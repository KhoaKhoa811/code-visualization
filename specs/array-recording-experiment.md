# Controlled array recording experiment

Status: implemented controlled experiment, 2026-09-21. Verification below applies only to the reviewed manual source pairs.

## Scope and semantics

Pair a reviewed original Main.java with a manually instrumented copy. This experiment has no parser or automatic transformation. Both programs compile and run only in the existing restricted Java 21 containers. Test probes outside the recorded region observe final values/output; they are not additional visual steps.

The recorded region declares one non-null int array named values with at most 16 elements, then performs one array-to-array assignment with literal indices. ARRAY_DECLARE follows successful initialization and captures its actual elements. ARRAY_READ captures the value once after successful access and returns that value. ARRAY_WRITE stores that captured value, then records the successful write. A failed read/write emits no successful event for that operation. Limit checks precede the next operation; a failure between mutation and event emission can still leave only the earlier safe prefix.

These rules apply only to the reviewed expressions. They do not establish handling for arbitrary side effects, alias declarations, loops, methods, recursion, null arrays, or concurrency. The scalar return value used by instrumentation is synthetic and does not add a visual step.

Use existing draft-1 event payloads, typed int values, one-based UTF-16 coordinates, exclusive ends, and one event per step. Retain exact original bytes and SHA-256 separately from the instrumented source's hash. The fixture-specific source map must match original text, including CRLF handling; it is not produced by an AST. Variable/object IDs are scoped to one run and one binding/object in this experiment.

## Transport and limits

Create a mode-0600 named pipe at /work/trace.pipe in private container scratch. Start a separate Docker exec reader before starting Java. Recorder output does not share program stdout/stderr and uses no host mounts or network. Pipe opening, execution and collection share the runner's stage/overall deadlines. Container cleanup terminates blocked readers/writers.

Use UTF-8 JSON lines, flushing each record immediately. Retain at most 64 KiB of trace bytes and 32 events; bound each array snapshot to 16 elements. Tests can lower the event limit. The trusted collector independently checks framing, the narrow recorder encoding, source association, identities, sequence, indices and read values before accepting a record. Stop at the first bad/incomplete record, never recover by skipping it. Limits and transport failures terminate the run; no automatic fallback or rerun occurs.

Transport-only end/limit markers are not schema events or visual steps. An end marker supports normal completion only together with the trusted runner outcome and successful collection. Missing completion, exceptions, limits or cancellation allow only validated prefixes. A zero-event prefix is unavailable under draft-1. The in-process recorder and its pipe are not tamper-proof; execution remains limited to reviewed fixtures.

## Result and acceptance

Assemble draft-1 results separately from collection and validate them with contracts/validate.mjs. A complete result means the reviewed observable region was recorded; frame/scope teardown and final binding visibility remain outside this experiment. Reconstruction tests rebuild each prefix and seek backward without executing Java again. This is not a production playback reducer or UI.

Verify original/instrumented output, final-value probes, and exception type/message; generated stack frames need not match original coordinates. Test changed initializer values, failed read/write, lowered trace cap, cancellation, blocked pipe opening, malformed/incomplete transport, source mismatch, and forward/backward states. Keep the existing runner regression cases. Explicit paired comparison runs are development tests, not application fallback behavior.

## Verified results

The runner script passed seven existing container cases, seventeen simulated Docker classification cases, stream validation checks, seven recording cases, and four original/instrumented comparisons. Recording cases cover success, changed initializer/CRLF/UTF-16 source mapping, failed read, failed write, a two-event trace cap, cancellation after the declaration, and a reader waiting for a writer that never opens the pipe. Every container was verified removed. All seven generated draft-1 results passed the existing contract validator and test-only forward/backward reconstruction. Corruption checks include ten malformed/incomplete/semantically invalid prefixes, byte/event/array caps, missing completion, empty arrays, and int bounds. Source edits fail identity validation.

Success records [3,1], [3,1], then [1,1]. The changed initializer records [9,-4], [9,-4], then [-4,-4]. A failed read retains only declaration; a failed write retains declaration plus the successful RHS read. Trace-cap interruption retains two events; cancellation retains one; the blocked-reader case has no playable events. Exception comparisons check type/message and stdout; normal comparisons additionally check final-value probes and stderr.

No automatic AST transformation, general Java support, scalar-variable recording, UI, production reducer, admission enforcement, or restart recovery was added. Runtime recorder writes use a trusted helper for this reviewed experiment, not permission for arbitrary submitted file access. Semantics of side-effecting expressions and full lifecycle coverage remain unverified. See [implementation guide](../runner/prototype/recording/README.md) and [ADR 0004](decisions/0004-controlled-array-trace-pipe.md).
