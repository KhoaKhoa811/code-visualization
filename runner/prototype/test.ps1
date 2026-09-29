$ErrorActionPreference = 'Stop'
$buildDir = Join-Path $PSScriptRoot '.build'
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
# Compile ONLY trusted driver/test files on the host. Fixture Main.java files stay in Docker.
$trustedFiles = @('RunnerHarness.java','ExecEvidence.java','RunnerFailureTest.java','RunnerHarnessTest.java','ArrayTrace.java','TracePipe.java','ArrayTraceTest.java','ArrayRecordingTest.java') | ForEach-Object { Join-Path $PSScriptRoot $_ }
& javac --release 21 -encoding UTF-8 -d $buildDir @trustedFiles
if ($LASTEXITCODE -ne 0) { throw 'Trusted harness compilation failed' }
& java -cp $buildDir RunnerFailureTest
if ($LASTEXITCODE -ne 0) { throw 'Runner failure classification tests failed' }
& java -cp $buildDir RunnerHarnessTest $PSScriptRoot
if ($LASTEXITCODE -ne 0) { throw 'Runner acceptance cases failed' }
& java -cp $buildDir ArrayTraceTest
if ($LASTEXITCODE -ne 0) { throw 'Trace stream checks failed' }
& java -cp $buildDir ArrayRecordingTest $PSScriptRoot
if ($LASTEXITCODE -ne 0) { throw 'Array recording cases failed' }
& node (Join-Path $PSScriptRoot 'recording/check-results.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Recorded contract/reconstruction checks failed' }
