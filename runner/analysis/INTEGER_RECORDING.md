# Integer variable recording

The subsequent [standalone increment extension](INDEX_INCREMENT_RECORDING.md) adds i++ and ++i. Its expanded gate passed 150 cases/124 comparisons on 2026-09-28, plus result/replay and shared/manual checks. Counts and dated evidence below describe the earlier baseline.

Subsequent [variable-index](VARIABLE_INDEX_RECORDING.md), [index-update](INDEX_UPDATE_RECORDING.md) and [index-addition](INDEX_ADDITION_RECORDING.md) increments capture two separate scalar declarations and index updates. The shared gate contains 110 cases/92 comparisons; see the latest guide and memory for verification status. The one-variable shape described here remains unchanged.

The integer increment extends the existing pipeline for one local int initialized from a signed decimal literal, followed by one literal assignment. It reuses source identity, AST preservation, helper-name collision checks, Docker execution and the bounded trace transport. A later [combined capability](COMBINED_RECORDING.md) handles one specific scalar/array program shape; arithmetic, loops and general multiple-variable programs remain unsupported.

```java
int x = 5;
x = 8;
```

Recording produces VARIABLE_DECLARE with actual x=5, then VARIABLE_WRITE with actual x=8. Each event highlights the corresponding original expression. Test-only backward reconstruction restores x=5 and then removes x. There is no browser renderer yet.

## Implementation

- [IntProbe.java](src/main/java/dev/codeviz/analysis/IntProbe.java) checks scalar eligibility and resolves the assignment target to the declared int binding. The existing analyzer shares parsing, scopes, limits and diagnostics, with an explicit INT_VARIABLE probe kind and scalar completeness status.
- [ArrayTransformer.java](src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) retains its historical class name and now has a scalar capability branch. It preserves the assignment, adds a limit guard before it and captures the actual variable after it. The original AST/source stay untouched; no scalar-read step is created for recorder bookkeeping.
- The shared [Recorder.java](../prototype/recording/Recorder.java) and [ArrayTrace.java](../prototype/ArrayTrace.java) retain their existing transport and array behavior. Scalar collection is an explicit mode with declaration/order/name/type checks; incompatible event kinds are rejected.
- [draft-2](../../contracts/run-result-v2.schema.json) adds scalar events without changing draft-1. The validator accepts both versions. The runtime result checker verifies exact source associations and forward/backward scalar states alongside arrays.

The current one-binding identity is variable-1. This does not implement aliases, multi-variable scope lifetimes, methods or recursive frames. The array and scalar probes are separate eligible program shapes.

## Verification command and cases

```powershell
& ./runner/analysis/test-recording.ps1
```

The September 23 gate covered 17 automatically generated cases: nine array cases plus eight scalar cases. Scalar fixtures cover x=5 then x=8, renamed negative values, Unicode/CRLF/multiline positions, helper collisions, int extremes, repeated values, no output probes, and a one-event trace budget. The deliberately limited case retains only initialization and stops before assignment. Fifteen original/generated pairs were compared; the two intentionally trace-limited runs were not unlimited behavior comparisons. The later combined and scalar-update increments expand this same gate to 47 cases/40 comparisons.

Generation also rejects final/uninitialized/wrong-type variables, unresolved/wrong-target assignments, compound assignment, increments, overflow literals and mixed array/scalar shapes. Scope/type/source identity facts must be available. The shared analyzer, collector, manual recorder and contract regression checks are part of the same command.

Local ignored evidence remains under `.results/automatic/`; integer cases use `int-` names, such as `int-success.original.java`, `int-success.instrumented.java` and `int-success.result.json`. Generated results use draft-2; earlier array cases remain draft-1. This guide describes the test gate; the memory files record completed runs and their evidence.

Verified 2026-09-23: the complete command passed. All 17 runtime cases, 15 paired comparisons and 17 result/reconstruction checks passed, alongside analyzer/generation, contract, collector and manual-recording regressions. Every owned container was verified removed. Success records two committed values, 5 then 8; int-limit retains only 5 with an explicit trace-limit outcome. There was no browser verification because no frontend exists.
