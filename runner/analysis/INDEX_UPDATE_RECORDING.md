# Index-update array recording

The subsequent [standalone increment extension](INDEX_INCREMENT_RECORDING.md) adds i++ and ++i. Its expanded gate passed 150 cases/124 comparisons on 2026-09-28, plus result/replay and shared/manual checks. Counts and dated evidence below describe the earlier baseline.

The subsequent [index-addition increment](INDEX_ADDITION_RECORDING.md) adds `i = i + literal` and expands the shared gate to 110 cases/92 comparisons in two bounded fixture batches. The literal-update scope and 88-case evidence below describe the earlier baseline.

The [approved scope](../../specs/index-update-array-recording.md) adds one literal assignment to a separately declared index:

```java
int x = 8;
int[] values = {5, 2};
int i = 0;
i = 1;
values[i] = x;
```

Five events capture the three declarations, the committed index update and the successful array store. Final state is x=8, i=1, values=[5,8]. Stepping backward first restores the array, then i=0; x stays 8. Scalar reads add no steps. Reconstruction currently exists in tests; no browser renderer exists.

## Implementation

- [CombinedProbe.java](src/main/java/dev/codeviz/analysis/CombinedProbe.java) resolves the index assignment target to the original index declaration, while keeping the value and array bindings distinct. Existing immutable scalarWrite facts carry the assignment source and its resolved binding ID. IndexSites continues to describe the declaration and final index use.
- [ArrayTransformer.java](src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) checks the handoff, transforms only the syntax copy, guards before index mutation and records the committed value afterward. It preserves the actual index/RHS name expressions. Five source associations match helper method plus encoded original range, including the two declaration sites.
- [Recorder.java](../prototype/recording/Recorder.java) adds a binding-aware variableWrite overload. The existing two-argument overload still targets variable-1; the new index write explicitly targets declared variable-3. Shared counters, FIFO and limits remain unchanged.
- [ArrayTrace.java](../prototype/ArrayTrace.java) accepts the five-operation plan, requires variable-3 for the index update and checks the final store against the latest index and value bindings. Corrupt traces stop at their valid prefix. The collector never accepts a write to x as a substitute for the expected index update.
- [check-recordings.mjs](check-recordings.mjs) validates hashes and source associations, then checks all six cursor states forward and backward. It keeps value, array and index identities separate. Draft-2 needs no new fields.

An invalid updated index preserves four events without a store. An invalid initial index changed to a valid index succeeds. Budgets one through four stop before the next operation; exactly five allows completion. Generated guard placement is checked before the actual assignment, independently of prefix assertions.

## Verification

From the repository root, with Docker's Linux engine running:

```powershell
& ./runner/analysis/test-recording.ps1
```

The gate contains 88 automatic runtime cases and 74 original/generated comparisons: 66 previous cases plus 22 index-update cases. Fourteen intentionally trace-limited cases use prefix expectations instead of unlimited-run equivalence. The new cases cover changed names, equal/repeated values, int boundaries, Unicode/CRLF/multiline source ranges, helper collisions, absent probes, one/16-element arrays, invalid indices, empty arrays, invalid-to-valid index changes and budgets one through five.

[IndexUpdateAcceptance.java](src/test/java/dev/codeviz/analysis/IndexUpdateAcceptance.java) checks bindings, five sites, preservation, stale/missing/swapped facts and 27 unsupported forms inside the bounded analysis worker. Shared generation checks verify guards and actual name expressions for every indexed fixture. The former rejection of a literal index update is now a positive case; compound index updates remain rejected. All earlier runtime fixtures remain in the gate.

Collector checks add 32 corrupt-prefix cases and rejection of a trusted plan targeting the wrong scalar. Contract checks add a five-event positive case and six invalid mutations. Source-specific index/value agreement belongs to the collector; the general schema validator does not infer Java expressions. The gate retains original analyzer checks, both worker-limit probes, prior collector/contract checks and seven manual runtime regressions/four comparisons.

The fixture transport retains its 11 fields: the previously separate scalar-write and index-declaration fields are both populated for this shape. Whitelisted fixture names, 64-KiB source caps and the 1-MiB bundle/output bounds remain enforced. No dependency, schema version, API, isolation or orchestration change is needed.

Verified 2026-09-26: the complete gate exited 0. All 88 runtime cases/74 comparisons, 88 result checks, analyzer/transformation/collector/contract checks, both worker-limit probes and seven manual cases/four comparisons/seven result checks passed. All test-owned containers were verified removed. The initial attempt stopped after trusted build/contract checks because Docker was unavailable; Docker Desktop startup was approved before the successful run. The fixture bundle was 998854 bytes, within the unchanged 1-MiB bound. Separate output-only and simulated Docker-classification suites were not rerun because execution orchestration was unchanged.

Arithmetic, expression indices, multiple updates, updating x within this indexed shape, loops and arbitrary Java visualization remain outside this increment. Later V1 requirements remain planned.
