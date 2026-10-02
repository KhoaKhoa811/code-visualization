# Classic for-loop recording

This guide records the scalar-fill increment and its 2026-09-30 evidence. The later [read/addition extension](LOOP_READ_RECORDING.md) adds a separate two-binding shape and 80 cases to the shared gate; it preserves the scalar-fill behavior below.

This prototype implements the bounded shape in [the loop specification](../../specs/for-loop-array-recording.md): a scalar, an int array, and one classic for loop that stores the scalar at each visited index. Both `i++` and `++i` are accepted. It is an execution/trace/replay prototype; no editor or diagram UI is included.

## Data flow

1. [LoopProbe.java](src/main/java/dev/codeviz/analysis/LoopProbe.java) resolves the scalar, array and index, the built-in array length, condition and update types, lexical scopes and original ranges. Dedicated immutable loop facts keep repeated operations separate from older fixed-operation plans.
2. [LoopTransformer.java](src/main/java/dev/codeviz/instrumentation/LoopTransformer.java) validates that handoff through the existing analyzer against the exact original bytes. It compares bindings, scopes, access facts, sites, completeness and syntax before modifying a clone. This bounded defensive re-analysis uses the same analysis implementation inside the isolated worker, not a separate resolver. Its cost remains subject to the existing worker limits.
3. Lowering creates a scoped index initialization and a guarded `while (true)`. Each original condition is evaluated once into a fresh boolean temporary, recorded, then used to branch. The guarded store uses the original bindings; the original unary increment executes once before its committed value is captured. Synthetic variables, guards and breaks are not observable steps.
4. [LoopRecorderMembers.java](../prototype/recording/LoopRecorderMembers.java) extends the generated helper only for draft-3. The generator renames the existing transport writer to `wireEvent` and inserts a metadata wrapper plus condition recording. [Recorder.java](../prototype/recording/Recorder.java) still owns the pipe, limits, encoding and successful store capture. Its source and legacy generated helpers remain unchanged.
5. [LoopTracePlan.java](../prototype/LoopTracePlan.java) validates declarations, then repeated condition/store/update phases through the existing bounded [ArrayTrace.java](../prototype/ArrayTrace.java) transport. It checks conditions against captured index/length, stores against captured bindings and increments against the previous index. It rejects contradictions; it never generates missing events. Only a complete false-condition record retires the index and permits completion.
6. [loop-replay.mjs](../prototype/recording/loop-replay.mjs) reconstructs a selected prefix from validated events. It applies recorded exits, restores retired bindings when seeking backward, and shows a boolean result only when a condition is selected. This prototype reducer expects validated draft-3 loop input and is not a general frontend playback engine.

The three-element example has six static sites and thirteen executed operations. At step 12 the index is 3; step 13 captures false and removes it. Seeking back to step 12 restores it. Failures and limits retain the last accepted snapshot without fabricating a normal exit.

## Verification command and scope

From the repository root with Java 21, Node and Docker Desktop's Linux engine available:

```powershell
& ./runner/analysis/test-recording.ps1
```

The gate preserves the 150 legacy runtime cases/124 original-generated comparisons and adds 68 loop cases/40 comparisons. The 28 intentionally limited loop cases are checked against expected prefixes, not compared to unlimited original execution. It also checks draft-1/2/3 contracts, analyzer handoffs, collector corruption, source hashes/ranges, replay states, both worker limits, eight existing fixture-delivery negatives and seven manual runtime cases/four comparisons.

Loop cases cover both increment forms; budgets 1–13; empty, one, three, nine, ten and sixteen elements; starts below, at and above bounds; int extremes; repeated/equal values; changed names/literals; Unicode/comments/CRLF/tabs; helper/temporary collisions; and absent probes. Source analysis/transformation and original/generated compilation/execution remain inside restricted Docker containers. Trusted test drivers and pure trace checks run on the host.

Fresh fixture delivery uses four explicitly named batches: the two unchanged legacy batches and `loop-post.txt`/`loop-pre.txt`, each containing 34 loop fixtures. Every worker/batch retains its one-MiB cap. The loop host driver checks both batches' names, counts and duplicates before executing any source. Event, array, source and runner limits are unchanged.

Verification on 2026-09-30: all 68 loop execution cases/40 original-generated comparisons and 68 contract/source/replay results passed, including 34 prefix/postfix pairs. The loop collector passed 55 corrupt-record checks, 13 premature ends, a byte-limit check and 223 final-record truncations. Both loop analysis workers passed fact/guard checks and 45 excluded forms each. A separate trusted fixture-delivery check passed valid 68-case delivery and ten rejection cases; it was then added to the gate script.

The full gate completed with exit 0 on 2026-09-30: all 150 legacy cases/124 original-generated comparisons, 150 legacy contract/replay results and 20 earlier prefix/postfix pairs passed. All seven manual cases/four comparisons and seven manual result checks passed, including cancellation and blocked-pipe timeout. Contract self-tests, both worker-limit probes and all existing analyzer/collector checks passed. Total automatic coverage is 218 cases/164 original-generated comparisons; intentionally limited cases use expected-prefix checks.

Native stdout/stderr and the actual final exit are captured under ignored `.results/loop-gate-20260930-1.out.log`, `.err.log` and `.exit.txt`; stderr is empty. The ten new fixture-delivery rejection tests passed separately because their gate hook was added after this run started. Fresh batch sizes were 998854, 759159, 495442 and 495368 bytes. Every test reported verified cleanup; final Docker queries found no containers with the analysis/prototype labels. The previous September 29 attempt was interrupted without a final exit and is not completion evidence.

Nested/multiple loops, other comparisons, enhanced for, while authoring, break/continue, body declarations, expression side effects and sorting remain outside this increment. Methods, recursion, broader structures and the frontend remain later required V1 work.
