# Docker execution-evidence cutoff and diagnostics

Status: implemented and verified 2026-10-03 within the approved runner task. Follow-up to the [investigation](docker-exec-evidence-investigation.md), within [ADR 0003](decisions/0003-runner-execution-evidence.md).

## Behavior

After the compiler/program Docker exec command returns, `RunnerHarness` obtains `docker info --format={{.SystemTime}}` from the same configured daemon. It parses a positive, UTC RFC3339 timestamp and uses that instant as the event query's upper bound. Host wall-clock time no longer determines which daemon events are eligible. The existing monotonic stage/overall deadlines remain authoritative: clock lookup and event query each have a three-second maximum and share the original execution budget. Expired commands are rejected before launch.

Events include `TimeNano`. The verifier rejects malformed/nonpositive timestamps and events later than the chosen cutoff, then retains the existing exact container, command, unique exec identity, unique completion and exit-agreement checks. No fallback trusts console output, guesses an exit, retries the query or reruns submitted Java. Existing cancellation, timeout and output-limit outcomes remain authoritative; cleanup is independent.

Failures expose bounded metadata in the internal result detail: compile/run stage, evidence phase, validated container ID, parsed daemon cutoff if available, observed CLI statuses, fixed reason, aggregate row/create/completion counts, malformed-row count, timestamp range and truncation flag. Raw management rows, stderr, exception messages and source are excluded from this evidence diagnostic. This does not change ordinary compiler/program stdout/stderr. Management capture remains bounded to 64 KiB; diagnostic summarization reads at most that many characters and 256 rows.

## Verification

`runner/prototype/test.ps1` compiles only trusted harness/tests on the host. Submitted fixture Java compiles and runs only in Docker.

The simulated suite adds ahead/behind daemon clocks, invalid/empty/unavailable clock replies, launcher failure, oversized clock output, invalid/future event timestamps, duplicate creation, cancellation in both management phases, their timeouts and a shared execution deadline. It checks cleanup, no execution retries, diagnostic bounds/privacy and no exposed unverified exits. Earlier missing/conflicting identity, output-spoofing and exit-code cases remain covered. A separate oversized diagnostic input checks summary truncation.

Fresh `runner/prototype/test.ps1` passed with exit 0 and empty stderr: 32 simulated lifecycle cases, seven real Docker runner cases, stream/collector checks, seven manual recording cases, four original/instrumented comparisons and all seven generated contract/source/forward-backward replay checks. Final prototype/probe label queries returned no containers. Local ignored evidence: `runner/analysis/.results/evidence-fix-gate-20261003-1.out.log`, `.err.log`, `.exit.txt`. The broader 298 automatic transformation cases were not rerun for this runner-only change; their earlier verification remains historical.

## Limits

This removes the demonstrated dependence on the host clock; it does not establish the cause of the October 2 incident. Event retention remains bounded by Docker history. Clock movement within the daemon, unavailable/incompatible management replies and missing history can still produce conservative infrastructure failures. There is no claim of general event-delivery reliability or public deployment readiness.

The historical `ExecEvidenceProbe` deliberately retains host-cutoff queries to reproduce the investigated mechanism. Its four-column verifier entry point remains available; the runner uses the new timestamp-aware entry point. No trace schema, Java transformation, replay or application integration changes are included.
