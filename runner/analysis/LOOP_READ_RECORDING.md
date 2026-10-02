# Array reads and addition in a classic loop

This increment implements the approved [source shape and fifteen-step sequence](../../specs/loop-array-read-proposal.md). An `int[]` declaration followed by one classic loop can use `values[i] = values[i] + signed-int-literal`, with `i++` or `++i`. The RHS must read the same array and index as the assignment target. The earlier [scalar-fill loop](LOOP_RECORDING.md) remains supported.

The read and write are separate operations. The read highlights only the RHS access and leaves the array unchanged. The write highlights the assignment and applies the captured result, including native Java int wraparound. Addition has no separate step. The example `{5,2,7}` plus one finishes at step 15 with `{6,3,8}`; the final false condition retires the index atomically.

## Implementation boundaries

- [LoopProbe](src/main/java/dev/codeviz/analysis/LoopProbe.java) supplies distinct immutable `ReadAddition` facts: original read/operand/operator/type ranges, resolved array/index identities and the literal addend. Scalar-fill facts keep their existing meaning; the read variant has no synthetic scalar prelude.
- [LoopTransformer](src/main/java/dev/codeviz/instrumentation/LoopTransformer.java) checks the handoff against the exact source in the isolated worker. It captures the assignment target reference/index before the RHS, captures the RHS value once, and reuses that temporary in the original native addition/store. Guards precede the condition, read, addition/store and increment. Fresh helper/temporary names avoid submitted-name collisions.
- [LoopReadRecorderMembers](../prototype/recording/LoopReadRecorderMembers.java) supplies only the new index-binding setup. Generated loop metadata uses array binding `variable-1`, index `variable-2`, object `array-1`, and the existing main/loop scopes. The old variant retains its IDs. The common [recorder](../prototype/recording/Recorder.java), wire transport and draft-3 schema are unchanged.
- [LoopTracePlan](../prototype/LoopTracePlan.java) validates condition/read/write/update order. It retains an accepted read for the pending store and rejects inconsistent results, identities, indices, source sites and exits before committing state. Checking arithmetic is validation of captured facts; it cannot manufacture a missing event.
- [loop-replay.mjs](../prototype/recording/loop-replay.mjs) applies validated recorded values and exits. Its optional `read` detail exists only for the selected ARRAY_READ event; conditions retain their existing nullable detail. Reads do not mutate state. A standalone prefix produces the same state as the corresponding cursor in a longer recording. Backward seeking never reruns Java or computes the addition.

The six static sites are array declaration, index declaration, condition, RHS read, whole assignment and increment. Failed negative-index reads stop after the first three events. A limit after a read preserves that read and the old array, without a write, increment or normal exit. Seven iterations complete in 31 events; eight iterations hit the 32-event cap after the eighth read. Sixteen elements starting at 9 complete in 31. All existing source, trace, array, worker and runner limits remain unchanged.

## Verification

From the repository root, with the existing pinned Java 21/Node/Docker setup:

```powershell
& ./runner/analysis/test-recording.ps1
```

Two new bounded analysis workers emit `read-post.txt` and `read-pre.txt`, 40 fixtures each. Both the worker output and each batch remain capped at one MiB. The host driver requires the two complete, correctly named batches with no duplicate or unknown cases before execution. Original and generated Java are compiled and executed only in restricted runner containers; trusted orchestration and pure trace checks run on the host.

The new cases cover both increment forms, budgets 1-15, negative/high starting indices, empty/one/multiple iterations, seven/eight/sixteen-element boundaries, zero/negative/extreme addends, wraparound, repeated values, renamed bindings, Unicode/comments/CRLF/tabs, temporary collisions and optional test probes. There are 80 new execution cases and 48 original/generated comparisons; the 32 intentionally limited cases use independent prefix expectations and guard-placement checks. Contract/hash/source/state checks cover every cursor, reverse restoration and 40 prefix/postfix pairs.

The complete gate retains the existing 218 automatic cases/164 comparisons and seven manual cases/four comparisons. It also runs collector corruption/truncation tests, contract tests, worker timeout/output limits and all three fixture-delivery suites. The checked-in [read contract examples](../../contracts/examples/README.md) are designed fixtures; actual captures remain under ignored `.results/loop-reads/`.

Verification on 2026-10-01: all 80 new runtime cases/48 original-generated comparisons and all 80 contract/hash/source/replay checks passed, including 40 prefix/postfix pairs and every forward/backward prefix. Both isolated analysis workers passed 39 excluded forms each and handoff/lowering checks. The read collector passed 59 corrupt records, 15 premature ends, atomic retry/byte-cap checks and 431 truncations; the new fixture-delivery suite passed ten rejection cases. All 68 earlier scalar-fill loop cases/40 comparisons and replay checks also passed. The wider run was interrupted during legacy cases, without a final exit record.

Recovery verification completed on 2026-10-02. Fresh isolated workers regenerated all six fixture batches; their sorted lines matched the October 1 batches exactly, including original/generated source and metadata. All 148 saved loop results matched those fresh fixtures, and all loop contract/source/replay checks passed again. The complete 150-case legacy group/124 original-generated comparisons, all 150 result checks, 20 earlier prefix/postfix pairs and seven manual cases/four comparisons then passed. Manual cancellation and blocked-pipe timeout checks passed. Total verified coverage is 298 automatic cases/212 original-generated comparisons plus seven manual cases/four comparisons. This combines matching October 1 loop executions with October 2 recovery checks; it is not a new uninterrupted run of the entire gate.

The first recovery attempt exited 1 at legacy `update-oob` because Docker execution identity could not be confirmed. A later management query showed one successful compiler execution for that container; the original query's missing evidence remains unexplained. One retry of the legacy/manual group passed with exit 0 and empty stderr. No production code, evidence checks or limits were changed to obtain that pass. The intermittent Docker evidence failure remains a runner reliability follow-up.

Ignored local evidence: `.results/loop-read-gate-20261001-1.out.log` records the completed loop groups; `.results/loop-read-recovery-20261002-1.*` records fresh workers, matching-fixture proof and the infrastructure failure; `.results/loop-read-recovery-20261002-2.*` records the successful legacy/manual retry and final exit 0. Fresh batches are under `.results/recovery-fixtures-2e784c05fe0446c3888074f64d41d0c3`, with sizes 998854/759159/495442/495368/578964/578860 bytes. Every completed run verified cleanup, and final Docker label queries found no remaining analysis/prototype containers. The earlier stopped container left by interruption was verified and removed during recovery.

This remains an engine prototype. Nested loops, other expressions, sorting, methods/recursion, broader structures, Spring Boot integration and the React interface are unfinished. This task does not claim general Java or LeetCode support, browser playback, or public deployment readiness.
