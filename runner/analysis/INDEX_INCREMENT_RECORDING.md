# Standalone index increment recording

Logging note: PowerShell Start-Transcript omitted native Java/Node output. Successful exit 0 and final native summaries were observed through execution session 60189 and saved in runner/analysis/.results/index-increment-completion-20260928.md. The transcript alone is not complete runtime evidence; future logging must explicitly capture native output.

This extends [index addition](INDEX_ADDITION_RECORDING.md) with either standalone postfix `i++` or prefix `++i` at the index-update position. The exact scope and acceptance cases are in [the increment specification](../../specs/index-increment-array-recording.md). Both forms retain the earlier program shapes, Main.java convention and isolated Java 21 execution.

```java
int x = 8;
int[] values = {5, 2};
int i = 0;
i++; // Alternatively: ++i;
values[i] = x;
```

The five operations declare x, declare the array, declare i, record the committed i=1, and store x at index 1. One VARIABLE_WRITE highlights the original `i++` or `++i` expression, excluding the semicolon. Going backward first restores the array and then restores i=0, without rerunning Java.

## Analysis and transformation

`CombinedProbe` accepts a unary expression only as the entire fourth statement. Its operand must resolve to the separately declared non-final local int index. Immutable `IncrementSites` retains the operator, expression/operand ranges, binding identity and resolved type. Existing assignment and addition facts remain distinct.

`ArrayTransformer` checks those facts against the source-matched syntax copy. Missing or inconsistent facts, including simultaneous addition/increment facts, reject transformation. It retains the original operation once:

```java
helper.beforeVariableWrite();
i++; // The original prefix or postfix statement.
helper.variableWrite(i, originalRange, "variable-3");
```

Capturing plain i afterward is essential: passing `i++` to the recorder would record the old expression result. The guard runs before mutation. The final array store receives the actual updated index and original value binding. The original AST remains unchanged.

Java int wraparound remains native. An invalid store preserves four events, including the committed update. Budgets 1–4 stop before the next operation; exactly 5 permits completion. Recorder, FIFO, collector, draft-2, source identities and runtime limits are unchanged. The collector validates the final index against the captured value; it does not evaluate increments.

## Verification

Run from the repository root with Docker's Linux engine available:

```powershell
& ./runner/analysis/test-recording.ps1
```

The gate now defines 150 automatic cases and 124 original/generated comparisons. The 26 intentionally limited cases use expected prefixes instead of unlimited-original comparisons. All 110 earlier fixtures remain, with 20 new cases per operator: success, renamed variables, equal scalar values, repeated array values, negative/too-large indices, empty arrays, both int extremes, invalid-to-valid index, first/last valid positions, one/16-element arrays, Unicode/comments/tabs/CRLF, helper collisions, absent probes and budgets 1–5.

Trusted analysis checks cover operator/binding/type/ranges, original preservation, stale/missing/swapped facts, inconsistent addition/increment facts, 30 rejected forms per operator, exactly one increment and guard/capture order. Result checks validate source mappings, hashes, draft-2, every intermediate state forward/backward, updated postfix capture and equivalent execution facts for all 20 prefix/postfix pairs.

Fixture generation retains two restricted workers and two fresh batches: 88 prior cases, then 22 addition plus 40 increment cases. The existing `acceptance-addition` worker name is retained; it now covers both kinds of indexed update. Each worker/batch stays within the existing 1-MiB bound. Complete allowlists, duplicate/missing checks, eight delivery negatives, both worker-limit probes and seven manual runtime regressions remain required.

Verified 2026-09-28: the full gate exited 0. All 150 automatic cases/124 comparisons, 150 result checks, 20 prefix/postfix pairs, analyzer/transformation/collector/contract checks, eight delivery negatives, both worker-limit probes and seven manual cases/four comparisons/seven result checks passed. All owned containers were verified removed. Batch sizes are 998854/759159 bytes; neither cap changed. Separate output-only/classification suites were not rerun because runtime orchestration was unchanged. The saved transcript is .results/index-increment-gate-73c7187d08174021b22b90319f50aecd.log, with fresh fixtures in .results/fixtures-748ca338165444bf87159c7043612f41. See [current state](../../memory/current-state.md) for the handoff.

## Limits

This does not support embedded increments, such as `values[i++]`, `i = i++`, arguments or arithmetic containing increments. Decrements, increments to other bindings, fields/array elements, other numeric types, parenthesized operands, multiple updates and loops remain outside this bounded extension. Milestone 1, sorting and the frontend remain incomplete.
