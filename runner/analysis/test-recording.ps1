$ErrorActionPreference = 'Stop'
& node (Join-Path $PSScriptRoot '../../contracts/validate.mjs') --self-test
if ($LASTEXITCODE -ne 0) { throw 'Trace contract self-tests failed' }
# This generates fresh fixtures in the bounded worker; no stale artifact fallback.
$fixtures = Join-Path $PSScriptRoot ('.results/fixtures-' + [Guid]::NewGuid().ToString('N'))
& (Join-Path $PSScriptRoot 'test.ps1') -FixtureDirectory $fixtures
$prototype = Join-Path $PSScriptRoot '../prototype'
$build = Join-Path $PSScriptRoot 'target/runner-tests'
New-Item -ItemType Directory -Force -Path $build | Out-Null
$trusted = @('RunnerHarness.java','ExecEvidence.java','TracePipe.java','ArrayTrace.java','LoopTracePlan.java','LoopTraceTest.java','LoopReadTraceTest.java','ArrayTraceTest.java','ArrayRecordingTest.java') |
    ForEach-Object { Join-Path $prototype $_ }
$trusted += Join-Path $PSScriptRoot 'AutomaticRecordingTest.java'
$trusted += Join-Path $PSScriptRoot 'FixtureBundleTest.java'
$trusted += Join-Path $PSScriptRoot 'LoopRecordingTest.java'
$trusted += Join-Path $PSScriptRoot 'LoopFixtureBundleTest.java'
$trusted += Join-Path $PSScriptRoot 'LoopReadRecordingTest.java'
$trusted += Join-Path $PSScriptRoot 'LoopReadFixtureBundleTest.java'
& javac --release 21 -encoding UTF-8 -d $build @trusted
if ($LASTEXITCODE -ne 0) { throw 'Trusted runner driver compilation failed' }
& java -cp $build ArrayTraceTest
if ($LASTEXITCODE -ne 0) { throw 'Shared collector regression failed' }
& java -cp $build LoopTraceTest
if ($LASTEXITCODE -ne 0) { throw 'Loop collector checks failed' }
& java -cp $build LoopReadTraceTest
if ($LASTEXITCODE -ne 0) { throw 'Loop read collector checks failed' }
& java -cp $build FixtureBundleTest (Join-Path $PSScriptRoot '.results')
if ($LASTEXITCODE -ne 0) { throw 'Fixture batch validation failed' }
& java -cp $build LoopFixtureBundleTest (Join-Path $PSScriptRoot '.results')
if ($LASTEXITCODE -ne 0) { throw 'Loop fixture batch validation failed' }
& java -cp $build LoopReadFixtureBundleTest (Join-Path $PSScriptRoot '.results')
if ($LASTEXITCODE -ne 0) { throw 'Loop read fixture batch validation failed' }
& java -cp $build LoopReadRecordingTest $PSScriptRoot $fixtures
if ($LASTEXITCODE -ne 0) { throw 'Loop read recording comparison failed' }
& node (Join-Path $PSScriptRoot 'check-loop-reads.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Loop read trace contract/reconstruction failed' }
& java -cp $build LoopRecordingTest $PSScriptRoot $fixtures
if ($LASTEXITCODE -ne 0) { throw 'Loop recording comparison failed' }
& node (Join-Path $PSScriptRoot 'check-loops.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Loop trace contract/reconstruction failed' }
& java -cp $build AutomaticRecordingTest $PSScriptRoot $fixtures
if ($LASTEXITCODE -ne 0) { throw 'Automatic recording comparison failed' }
& node (Join-Path $PSScriptRoot 'check-recordings.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Automatic trace contract/reconstruction failed' }
& java -cp $build ArrayRecordingTest $prototype
if ($LASTEXITCODE -ne 0) { throw 'Manual recording regression failed' }
& node (Join-Path $prototype 'recording/check-results.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Manual trace contract/reconstruction failed' }
