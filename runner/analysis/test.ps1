param([string]$FixtureDirectory)
$ErrorActionPreference = 'Stop'
$resultsDir = Join-Path $PSScriptRoot '.results'
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
$docker = (Get-Command docker -ErrorAction Stop).Source
$runId = [Guid]::NewGuid().ToString('N')
if (-not $FixtureDirectory) { $FixtureDirectory = Join-Path $resultsDir "fixtures-$runId" }
if (Test-Path -LiteralPath $FixtureDirectory) { throw 'Fixture directory must be fresh' }
New-Item -ItemType Directory -Path $FixtureDirectory | Out-Null
$captureIndex = 0

function Quote-Argument([string]$Value) {
    # Windows native argv quoting; values never become shell syntax.
    '"' + [regex]::Replace([regex]::Replace($Value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
}
function Read-Bounded([string]$Path, [int]$Maximum) {
    $stream = [IO.File]::Open($Path, 'Open', 'Read', 'ReadWrite')
    try {
        $buffer = New-Object byte[] $Maximum
        $count = $stream.Read($buffer, 0, $buffer.Length)
        return [Text.Encoding]::UTF8.GetString($buffer, 0, $count)
    } finally { $stream.Dispose() }
}
function Invoke-Docker([string[]]$Arguments, [int]$Seconds = 15) {
    $script:captureIndex++
    $outPath = Join-Path $resultsDir "$runId-$script:captureIndex.out"
    $errPath = Join-Path $resultsDir "$runId-$script:captureIndex.err"
    $quoted = ($Arguments | ForEach-Object { Quote-Argument $_ }) -join ' '
    $process = Start-Process -FilePath $docker -ArgumentList $quoted -WindowStyle Hidden -PassThru -RedirectStandardOutput $outPath -RedirectStandardError $errPath
    # Cache the native handle before a short command exits (Windows PowerShell 5.1).
    $null = $process.Handle
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $failure = $null
    try {
        while (-not $process.HasExited) {
            $size = (Get-Item -LiteralPath $outPath).Length + (Get-Item -LiteralPath $errPath).Length
            if ($size -gt 1048576) { $failure = 'OUTPUT_LIMIT'; break }
            if ($timer.Elapsed.TotalSeconds -ge $Seconds) { $failure = 'TIMEOUT'; break }
            Start-Sleep -Milliseconds 50
            $process.Refresh()
        }
        if ($failure) { $process.Kill() }
        if (-not $process.WaitForExit(5000)) { throw 'Docker CLI did not stop' }
        $process.Refresh()
        $size = (Get-Item -LiteralPath $outPath).Length + (Get-Item -LiteralPath $errPath).Length
        if ($size -gt 1048576 -and -not $failure) { $failure = 'OUTPUT_LIMIT' }
        $stdout = Read-Bounded $outPath 1048576
        $remaining = [Math]::Max(0, 1048576 - [Text.Encoding]::UTF8.GetByteCount($stdout))
        $stderr = Read-Bounded $errPath $remaining
        if ($failure) { throw $failure }
        if ($process.ExitCode -ne 0) { throw "Docker CLI failed ($($process.ExitCode)): $stderr $stdout" }
        return $stdout.TrimEnd()
    } finally {
        if (-not $process.HasExited) { $process.Kill(); $null = $process.WaitForExit(5000) }
        $process.Dispose()
        # Polling can overshoot on disk; retain only a combined 1 MiB prefix after stopping.
        $budget = 1048576L
        foreach ($path in @($outPath, $errPath)) {
            $file = [IO.File]::Open($path, 'Open', 'Write', 'ReadWrite')
            try {
                $retained = [Math]::Min($file.Length, $budget)
                $file.SetLength($retained)
                $budget -= $retained
            } finally { $file.Dispose() }
        }
    }
}

# Build only trusted Java. Surefire is disabled; analysis and fixture compilation stay in Docker.
& (Join-Path $PSScriptRoot 'maven.ps1') package dependency:tree '-DoutputFile=target/dependencies.txt'
$base = 'docker.io/library/eclipse-temurin@sha256:87f0a5e638d0bc739f2908adb4a0a697b585d8bee5e5a8de43c88e8d07b3786e'
$null = Invoke-Docker @('image', 'inspect', $base)
$imageFile = Join-Path $resultsDir "$runId-image-id.txt"
$build = Invoke-Docker @('build', '--pull=false', '--network=none', '--platform=linux/amd64', '--iidfile', $imageFile, $PSScriptRoot) 120
$imageId = (Get-Content -LiteralPath $imageFile -Raw).Trim()
if ($imageId -notmatch '^sha256:[a-f0-9]{64}$') { throw 'Invalid built image identity' }

foreach ($mode in @('acceptance', 'acceptance-addition', 'deadline-probe', 'output-probe')) {
    $name = "codeviz-analysis-$runId-$mode"
    try {
        $create = @('create', '--pull=never', '--platform=linux/amd64', "--name=$name", "--label=codeviz.analysis=$runId",
            '--user=10001:10001', '--network=none', '--read-only', '--cap-drop=ALL', '--security-opt=no-new-privileges=true',
            '--memory=512m', '--memory-swap=512m', '--cpus=1', '--pids-limit=128',
            '--tmpfs=/tmp:rw,nosuid,nodev,noexec,size=64m,uid=10001,gid=10001,mode=0700', '--log-driver=none', $imageId, $mode)
        $containerId = Invoke-Docker $create
        if ($containerId -notmatch '^[a-f0-9]{64}$') { throw 'Invalid test container identity' }
        $inspection = Invoke-Docker @('inspect', $containerId)
        $inspection | Set-Content -LiteralPath (Join-Path $resultsDir "$mode-container.json") -Encoding UTF8
        $config = ($inspection | ConvertFrom-Json)[0]
        if ($config.Config.User -ne '10001:10001' -or $config.HostConfig.NetworkMode -ne 'none' -or
            -not $config.HostConfig.ReadonlyRootfs -or $config.HostConfig.Memory -ne 536870912 -or
            $config.HostConfig.MemorySwap -ne 536870912 -or $config.HostConfig.PidsLimit -ne 128 -or
            $config.HostConfig.NanoCpus -ne 1000000000 -or $config.HostConfig.CapDrop -notcontains 'ALL' -or
            $config.HostConfig.SecurityOpt -notcontains 'no-new-privileges=true' -or
            @($config.Mounts | Where-Object { $_.Type -eq 'bind' }).Count -ne 0) { throw 'Unexpected worker isolation configuration' }
        if ($mode -in @('acceptance', 'acceptance-addition')) {
            $output = Invoke-Docker @('start', '--attach', $containerId) 60
            $state = (Invoke-Docker @('inspect', $containerId) | ConvertFrom-Json)[0].State
            $marker = if ($mode -eq 'acceptance') { 'PASS: 18 analyzer cases' } else { 'PASS: 62 indexed-update fixtures' }
            if ($state.Running -or $state.ExitCode -ne 0 -or $state.OOMKilled -or $output -notmatch $marker) { throw 'No confirmed successful test completion' }
            $output | Set-Content -LiteralPath (Join-Path $resultsDir "$mode.txt") -Encoding UTF8
            $parts = $output -split 'REPORT_BEGIN\r?\n', 2
            if ($mode -eq 'acceptance' -and $parts.Count -ne 2) { throw 'Missing bounded analysis report' }
            $lines = $parts[0] -split '\r?\n'
            $artifacts = @($lines | Where-Object { $_.StartsWith('ARTIFACT ') })
            $expectedCount = if ($mode -eq 'acceptance') { 88 } else { 62 }
            $batch = if ($mode -eq 'acceptance') { 'batch-0.txt' } else { 'batch-1.txt' }
            if ($artifacts.Count -ne $expectedCount) { throw 'Missing automatic transformation fixtures' }
            [IO.File]::WriteAllLines((Join-Path $FixtureDirectory $batch), $artifacts, (New-Object Text.UTF8Encoding $false))
            if ((Get-Item -LiteralPath (Join-Path $FixtureDirectory $batch)).Length -gt 1048576) { throw 'Fixture batch exceeds cap' }
            Write-Output ($lines | Where-Object { -not $_.StartsWith('ARTIFACT ') })
            if ($mode -eq 'acceptance') { $parts[1] | Set-Content -LiteralPath (Join-Path $resultsDir 'original-analysis.txt') -Encoding UTF8 }
        } else {
            $expected = if ($mode -eq 'deadline-probe') { 'TIMEOUT' } else { 'OUTPUT_LIMIT' }
            $seconds = if ($mode -eq 'deadline-probe') { 2 } else { 15 }
            $observed = $null
            try { $null = Invoke-Docker @('start', '--attach', $containerId) $seconds }
            catch { $observed = $_.Exception.Message }
            if ($observed -ne $expected) { throw "Expected $expected, observed: $observed" }
            Write-Output "PASS worker $expected enforcement"
        }
    } finally {
        $null = Invoke-Docker @('rm', '--force', $name) 5
        $remaining = Invoke-Docker @('ps', '-a', '--filter', "name=^/$name$", '--format', '{{.ID}}') 5
        if ($remaining) { throw "Cleanup unconfirmed for $name" }
        Write-Output "PASS removed $name"
    }
}
Write-Output 'PASS analyzer suite and two driver limit probes; every owned container removed'
Write-Output "Fresh bounded fixture batches: $FixtureDirectory"
