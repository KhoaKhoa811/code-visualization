# Variable-index array recording

The subsequent [standalone increment extension](INDEX_INCREMENT_RECORDING.md) adds i++ and ++i. Its expanded gate passed 150 cases/124 comparisons on 2026-09-28, plus result/replay and shared/manual checks. Counts and dated evidence below describe the earlier baseline.

Subsequent [index-update](INDEX_UPDATE_RECORDING.md) and [index-addition](INDEX_ADDITION_RECORDING.md) increments add index assignments before the store. The shared gate contains 110 cases/92 comparisons; the four-operation scope and September 25 evidence below remain historical baseline information. See the latest guide and memory for verification status.

The [approved scope](../../specs/variable-index-array-recording.md) adds this bounded shape:

```java
int x = 8;
int[] values = {5, 2};
int i = 1;
values[i] = x;
```

Four events capture the value declaration, array declaration, index declaration and successful store. Final state is x=8, i=1 and values=[5,8]. Reading either scalar adds no step. No frontend renderer exists yet.

## Implementation

- [CombinedProbe.java](src/main/java/dev/codeviz/analysis/CombinedProbe.java) resolves the value, array and index bindings separately. It checks the index declaration and its use, preserves existing shapes, and exports immutable IndexSites through the common analyzer handoff.
- [ArrayTransformer.java](src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) checks all binding/site facts, guards before index initialization, captures its actual value, and preserves the name expressions passed to the store. Source associations match both helper method and encoded original range. Repeated VARIABLE_DECLARE calls cannot overwrite one another's associations.
- [Recorder.java](../prototype/recording/Recorder.java) keeps bounded scalar binding identities and reuses the shared counter/FIFO. The value uses variable-1, the array binding variable-2, the index variable-3, and the object array-1. Legacy declaration entry points retain their IDs.
- [ArrayTrace.java](../prototype/ArrayTrace.java) accepts an ordered operation plan for this shape. It stores scalar values by binding ID and verifies declaration names, types, original sites, order, identities and completion. The store index must match variable-3; the stored value must match variable-1. Legacy constructors retain their validation paths.
- [check-recordings.mjs](check-recordings.mjs) matches events to sites by kind and original range, verifies generated associations, and reconstructs three independent bindings. Going backward removes the index before the array, then removes the value scalar. Reconstruction never reruns Java.

Invalid indices retain three declaration events and emit no store event. Budget one stops before array initialization, two before index initialization, and three before the store. Exactly four permits completion. Guard placement is checked in generated source as well as through expected trace prefixes.

## Verification

```powershell
& ./runner/analysis/test-recording.ps1
```

The shared gate covers 66 automatic cases and 56 original/generated comparisons. Ten intentionally limited cases use explicit prefix expectations. The 19 new index cases cover changed names/index/value, equal scalar values, int extremes, Unicode/CRLF/multiline mappings, helper collisions, absent probes, one/16-element arrays, invalid indices, empty arrays and budgets one through four.

[IndexAcceptance.java](src/test/java/dev/codeviz/analysis/IndexAcceptance.java) runs inside the bounded analysis worker. It checks three resolved bindings, repeated declaration sites, preservation, stale/missing/swapped facts and 21 unsupported inputs. Shared collector tests add 25 corrupt prefixes; contract self-tests add six negative mutations for two scalar declarations. Existing analyzer, worker-limit, collector, automatic and manual regressions remain in the gate. Local evidence uses `.results/automatic/index-*`; memory records actual completed results.

No dependency, schema version, API or runner orchestration changed. This does not implement scalar/index updates within the new shape, expression indices, arithmetic, loops or arbitrary Java visualization. Earlier scalar-update shapes remain supported; later V1 work remains planned.

Verified 2026-09-25: the complete command exited 0. All 66 automatic cases/56 comparisons, 66 result checks, analyzer/contract/collector and worker-limit checks, and seven manual cases/four comparisons/seven result checks passed. Every test-owned container was verified removed. The initial attempt stopped before worker execution because Docker was unavailable; Docker Desktop startup was approved before the successful run.
