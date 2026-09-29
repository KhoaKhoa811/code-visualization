# Controlled array recorder

This experiment produces real ARRAY_DECLARE, ARRAY_READ and ARRAY_WRITE records from manually instrumented Java inside Docker. It does not parse or transform arbitrary Java. The original records [3,1], an unchanged array after reading index 1, then [1,1] after writing index 0.

## Files and responsibilities

| File | Responsibility |
| --- | --- |
| original/Main.java | Reviewed original algorithm plus a development-only final-value/console probe |
| instrumented/Main.java.in | Manually written instrumentation template for the reviewed cases |
| Recorder.java | Reads actual runtime facts and writes/flushes events through the private pipe; compiled only inside Docker |
| ../ArrayTrace.java | Bounds and validates incoming records; retains the safe prefix |
| ../TracePipe.java | Reads the separate Docker pipe stream, outside program console output |
| ../ArrayRecordingTest.java | Fills explicit fixture tokens/source ranges, runs paired programs in Docker, checks behavior and records evidence |
| ../ArrayTraceTest.java | Injects transport corruption and limits without executing submitted code |
| check-results.mjs | Assembles draft-1 results, invokes the existing validator, checks source identity/ranges and test-only forward/backward reconstruction |

Recorder helpers preserve a single read and record successful operations afterward. Runtime facts are not copied from expected JSON fixtures. Fixed binding/object names are scoped to this one-array experiment. Source ranges come from explicit fixture expressions, not an AST or generated-line guesses. The changed-values case also checks CRLF and UTF-16 columns after a supplementary Unicode character.

## Run and inspect

From the repository root, with Java 21, Node 22 and the pinned Docker image available:

```powershell
& ./runner/prototype/test.ps1
```

Only explicitly listed trusted driver/test files compile on the host. Recorder.java and both Main variants compile/run only in restricted containers. The script checks existing runner cases before the array experiment.

Generated evidence is retained in ignored `.results/`: each case has original/generated source snapshots, a raw runner/trace artifact, and a readable `.result.json` conforming to draft-1. For example, `.results/success.result.json` contains the three actual events. `sourceId` hashes original UTF-8 bytes; the raw artifact separately records the generated source hash. Original source changes cannot reuse that association silently. Results are local test artifacts, not an application API or durable run store.

The reader starts before Java. Each flushed complete event is validated before retention. Collector limits are 64 KiB, 32 events and 16 array elements; the partial test lowers the recorder event cap to two. Trace failures/limits are distinct runner outcomes. Cancellation/deadlines retain their existing precedence. Cleanup removes the entire container and bounds collector shutdown with the same cleanup observation budget.

## Verification on 2026-09-21

- All seven prior Docker cases and seventeen simulated Docker classification cases passed.
- Stream checks passed for split records, ten corrupt/incomplete prefixes, missing completion, byte/event/array caps, empty arrays and int bounds.
- Seven recording cases passed: success, changed values, failed read, failed write, trace limit, cancellation and blocked reader with no writer.
- Four original/instrumented comparisons passed. Normal cases match stdout/final probes and stderr; exception cases match stdout and exception type/message, without requiring generated stack frames to match.
- All seven generated results passed existing contract validation, original-source checks and forward/backward prefix reconstruction. No Java runs occur during reconstruction.
- Every test-owned container was verified absent afterward.

This is a three-operation region, not full lifecycle tracing. Development probes are outside that region. Arrays/aliases in general, scalar variables, side-effecting expressions, automatic transformation, UI, admission, durable recovery and hostile-recorder integrity remain unfinished. See the [experiment specification](../../../specs/array-recording-experiment.md).
