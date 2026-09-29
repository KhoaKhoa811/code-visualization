# Combined variable and array recording

The subsequent [standalone increment extension](INDEX_INCREMENT_RECORDING.md) adds i++ and ++i. Its expanded gate passed 150 cases/124 comparisons on 2026-09-28, plus result/replay and shared/manual checks. Counts and dated evidence below describe the earlier baseline.

Subsequent [variable-index](VARIABLE_INDEX_RECORDING.md), [index-update](INDEX_UPDATE_RECORDING.md) and [index-addition](INDEX_ADDITION_RECORDING.md) increments add a separate index scalar and its updates. The shared gate contains 110 cases/92 comparisons. The two combined shapes described below remain supported; see the latest guide and memory for verification status.

The [approved scope](../../specs/combined-variable-array-recording.md) adds one program shape to the existing pipeline:

```java
int x = 3;
int[] values = {5, 2};
values[0] = x;
```

It records three committed operations: x becomes 3, values becomes [5, 2], and values[0] becomes 3. There is no extra scalar-read event. Runtime Java supplies the recorded values. Original source remains unchanged; instrumentation edits a separate AST copy.

## Responsibilities

- [CombinedProbe.java](src/main/java/dev/codeviz/analysis/CombinedProbe.java) checks the exact syntax, resolves both bindings and the write's receiver/RHS, and supplies original source sites. The common analyzer retains entry, diagnostics, source and resource checks.
- [ArrayTransformer.java](src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) consumes the immutable handoff and generates recorder calls. It checks distinct binding facts and target-access facts, guards before the array initialization, and maps generated calls back to original expressions. Missing or stale facts prevent generation.
- [Recorder.java](../prototype/recording/Recorder.java) uses the existing pipe and event counter. It assigns variable-1 to the scalar, variable-2 to the array binding, and array-1 to the array object. The existing single-array and single-scalar paths retain their identities.
- [ArrayTrace.java](../prototype/ArrayTrace.java) validates the mixed operation order, binding kinds, names, identities, source sites and values. Its combined store must agree with the recorded scalar. Invalid records stop collection at the last valid prefix.
- [check-recordings.mjs](check-recordings.mjs) checks draft-2 results, source associations, and separate scalar/array states at every forward/backward cursor. This is test-only reconstruction; there is no frontend playback engine yet.

A failed array write retains both declarations. Event limit one stops before array initialization; limit two stops before the store. No successful write is invented. Normal completion requires the recorder end marker plus the runner's successful outcome.

## Fixed output probes

Optional development probes print the final scalar and full array, then a stderr marker. JavaParser 3.28.2 reported an ambiguous primitive-array overload when resolving Arrays.toString. For this exact probe syntax, the adapter checks the resolved int[] argument binding and looks up the exact JDK Arrays.toString(int[]) and PrintStream.print(String) declarations. It also resolves System.out/err and rejects local names shadowing System/java. This does not add general method-call tracing or replace javac as the Java validity authority. Original and generated probe programs are compiled and executed in Docker.

## Scalar update before the store

The [scalar-update scope](../../specs/scalar-update-array-recording.md) extends this capability with exactly one literal scalar assignment between the array declaration and array write:

```java
int x = 3;
int[] values = {5, 2};
x = 8;
values[0] = x;
```

CombinedProbe resolves the scalar assignment target to the existing binding and supplies its distinct source site. The common analyzer routes by declaration structure as well as statement count, preserving legacy four-statement programs with output probes. The transformer guards before the actual scalar assignment, preserves that assignment and records its committed runtime value. It then stores the current scalar value into the array. The existing Recorder already supports these operations; no recorder change or new trace version was needed.

The collector uses the expected source-site set to select the exact three- or four-operation order. VARIABLE_WRITE updates its scalar state; the final ARRAY_WRITE must match that latest value. It rejects premature completion and skipped/reordered operations. Test reconstruction changes only the scalar at step 3 and only the array at step 4. Backward movement restores each independently.

For the four-operation shape, a failed final array store retains three events. Budgets one/two/three stop before array initialization/scalar assignment/array store respectively. Exactly four events permit successful completion. The same-value scalar assignment remains an observable step.

## Verification

```powershell
& ./runner/analysis/test-recording.ps1
```

These combined increments contributed to the 47-case/40-comparison baseline: nine array, eight scalar, 14 original combined and 16 scalar-update cases. Seven deliberately trace-limited cases have explicit prefix expectations instead of unlimited comparisons. Both combined shapes cover renamed bindings, negative/repeated/boundary values, Unicode/CRLF/multiline mapping, helper collisions, absent probes, one/16-element arrays and negative/out-of-range/empty writes. The scalar-update cases additionally cover event budgets one through four. These cases remain in the expanded gate alongside analyzer, contract, collector, worker-limit and manual-recording regressions.

The worker additionally rejects 18 out-of-scope combined inputs and verifies stale/missing semantic facts. Collector tests include 15 corrupt combined prefixes; draft-2 semantic tests include six mixed negative cases. Results under `.results/automatic/mix-*` are local ignored evidence. The memory files record actual completed checks, not just this intended gate.

Scalar-update checks add 20 out-of-scope inputs, stale/missing/wrong binding/site facts, original preservation, and generated guard/capture ordering for every new fixture. Collector checks add 23 corrupt prefixes, including stale scalar values and three-event premature completion. Contract self-tests add one valid interleaved write and six negative mutations. Scalar-update runtime artifacts use `.results/automatic/update-*` names.

Arithmetic, variable indices, extra declarations/assignments, aliases, loops, methods, recursion and general Java tracing remain outside this increment. Later V1 requirements remain planned.
