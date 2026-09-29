param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArguments)
$ErrorActionPreference = 'Stop'
$version = '3.9.16'
$expectedHash = 'ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3'
$toolsDir = Join-Path $PSScriptRoot '.tools'
$archive = Join-Path $toolsDir "apache-maven-$version-bin.zip"
$maven = Join-Path $toolsDir "apache-maven-$version/bin/mvn.cmd"
New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
if (-not (Test-Path -LiteralPath $archive)) {
    Invoke-WebRequest -UseBasicParsing -TimeoutSec 120 -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip" -OutFile $archive
}
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash.ToLowerInvariant() -ne $expectedHash) {
    throw "Maven archive checksum mismatch: $archive. No archive contents were executed."
}
if (-not (Test-Path -LiteralPath $maven)) {
    Expand-Archive -LiteralPath $archive -DestinationPath $toolsDir
}
& $maven --batch-mode --strict-checksums --no-transfer-progress "-Dmaven.repo.local=$toolsDir/repository" -f (Join-Path $PSScriptRoot 'pom.xml') @MavenArguments
if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
