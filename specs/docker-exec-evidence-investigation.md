# Docker execution-evidence investigation

Status: investigation completed 2026-10-03. No production runner fix is implemented. The October 2 trigger remains unconfirmed; a timestamp-cutoff failure mechanism is reproduced under a controlled condition.

## Incident and scope

During the loop-array-read recovery gate, legacy `update-oob` returned `INFRASTRUCTURE_ERROR` with `Missing or ambiguous Docker exec identity`. Cleanup passed. A later Docker management query showed one matching `javac` creation/completion pair with exit 0; the user program had not started in that failed run. One unchanged retry of the legacy/manual group passed. The original evidence-query response and its exact cutoff were not retained, so the later query cannot establish what the first query contained.

PR #4 was merged by the user. The investigation began from synced main `ff84c1f`, on `investigate/docker-exec-evidence`. Scope was source inspection, a bounded Docker probe and a proposed follow-up, not changing the runner's acceptance behavior.

## Existing behavior

[RunnerHarness.verifiedExec](../runner/prototype/RunnerHarness.java) waits for the Docker exec CLI, then requests container-scoped `exec_create` and `exec_die` events with `--since=0 --until=<host Instant.now()>`. [ExecEvidence](../runner/prototype/ExecEvidence.java) requires exactly one creation matching the command, one completion with the same exec ID, and agreement between process and CLI exit codes. Missing or ambiguous evidence remains an infrastructure failure. User stdout is never accepted as proof.

The query uses the Windows host's cutoff against daemon event timestamps. A cutoff preceding creation excludes the identity; a cutoff between creation and completion excludes completion. Docker documents timestamp filtering and a history limit of 256 events in its [events reference](https://docs.docker.com/reference/cli/docker/system/events/). Bounded historical queries therefore also depend on event retention. Neither clock skew nor retention loss was measured during the original failure.

## Probe and results

[ExecEvidenceProbe.java](../runner/prototype/diagnostics/ExecEvidenceProbe.java) runs only twenty uniquely numbered `/bin/echo` commands in one restricted container. Each command's CLI exit must be zero. The probe feeds management records into the unchanged `ExecEvidence.confirm`, comparing an immediate host cutoff, the same cutoff after 200 ms, and a refreshed cutoff. Raw command arguments and replies are retained under an explicitly selected ignored results directory. No submitted Java is executed on the host or by this diagnostic.

- Docker client/server: 29.8.0 on the investigation machine. This does not establish the version present during the earlier incident.
- Twenty normal probes passed all three evidence queries: zero natural failures reproduced.
- A controlled cutoff two seconds before the first command reproduced `Missing or ambiguous Docker exec identity`, while the current cutoff confirmed that same successful execution. No system clock was changed. This establishes sensitivity to an unsuitable cutoff, not the original incident's cause.
- In immediate successful queries, host cutoffs were 13.459564–17.229969 ms after recorded `exec_die` timestamps. This is an observed margin for these echo probes, not a clock-offset measurement or a Java-compilation timing result.
- The first attempt timed out while creating the container under a diagnostic-only five-second deadline; cleanup succeeded. The final probe uses the runner's existing ten-second creation deadline and five-second deadlines for other commands. The completed attempt exited 0 and verified cleanup. Final label inspection found no diagnostic containers.
- Source review and Git comparison confirm `RunnerHarness.java` and `ExecEvidence.java` were not changed. No application regression suite was rerun for this investigation.

Local raw evidence is under `runner/analysis/.results/evidence-probe-20261003-1` (startup timeout) and `evidence-probe-20261003-2` (completed probe). The latter includes `summary.json`, derived from the captured creation/completion pairs and cutoff arguments. These ignored captures are local evidence, not committed fixtures.

## Conclusion and proposed next task

The demonstrated weakness is reliance on a host-clock upper bound for daemon events. The precise original trigger is unknown. Clock movement/skew and event-query visibility remain hypotheses. History eviction is an architectural limitation, but the later successful query makes permanent eviction a weaker explanation for this incident. More ordinary repetitions would not recover the missing original response.

Recommended follow-up, requiring separate approval:

1. Add bounded failure diagnostics containing stage, container ID, cutoff, command classification and management evidence counts/timestamps. Keep diagnostics separate from user stdout; retain no secrets or submitted source. This distinguishes absent, duplicate, incomplete and contradictory evidence.
2. Evaluate a daemon-clock cutoff obtained after the execution command, inside the existing overall deadline, instead of assuming host/daemon clocks match. Prove skew behavior with controlled tests before adopting it. This removes one demonstrated dependency; it is not yet proof of a fix for the October 2 incident.
3. Preserve exact container/exec identity, command matching, exit agreement, output caps, cancellation and cleanup. An expired deadline or missing/ambiguous evidence must still fail conservatively. Never rerun submitted Java to repair evidence.
4. Test normal completion, stale cutoffs, missing/duplicate/malformed events, conflicting exits, daemon-query failure, timeout and cancellation. Do not add blanket retries or increase execution limits to make tests pass.

Direct Engine API execution creation/start/inspection can be evaluated later if historical-event reliability remains inadequate; it is not selected or implemented by this investigation. The first usable application slice remains a separate proposed task after runner reliability is addressed.

## Reproduce the diagnostic

From the repository root, with Java 21 and Docker Desktop's Linux engine ready and the pinned runner image already cached:

```powershell
$probeBuild = 'runner/prototype/.build/evidence-probe'
New-Item -ItemType Directory -Force -Path $probeBuild | Out-Null
$probeSources = @('RunnerHarness.java','ExecEvidence.java','ArrayTrace.java','LoopTracePlan.java','TracePipe.java','diagnostics/ExecEvidenceProbe.java') | ForEach-Object { Join-Path 'runner/prototype' $_ }
javac --release 21 -encoding UTF-8 -d $probeBuild @probeSources
if ($LASTEXITCODE -ne 0) { throw 'Diagnostic compilation failed' }
$probeResults = 'runner/analysis/.results/evidence-probe-' + [Guid]::NewGuid().ToString('N')
java -cp $probeBuild ExecEvidenceProbe $probeResults
if ($LASTEXITCODE -ne 0) { throw 'Diagnostic failed; inspect its captures' }
```

The documented trusted-source build was checked. The completed probe used the same probe and verifier sources, compiled against the unchanged runner class for its pinned image constant. The container has no network or host mounts, a read-only root, UID/GID 10001, dropped capabilities, no-new-privileges, 64 MiB memory/no swap, one CPU and 32 tasks. Its sleep entrypoint expires after 120 seconds; the probe also removes the container in its finally block. Each completed command capture is checked against 64 KiB. Capture files can temporarily exceed this post-completion check; this trusted diagnostic is not a hostile-output collector or a replacement for runner isolation tests.
