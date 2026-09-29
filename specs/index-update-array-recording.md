# Index update before an array write

Status: implemented and Docker-verified on 2026-09-26 after user approval. This extends [variable-index recording](variable-index-array-recording.md); all five previously verified shapes remain supported. See the [implementation guide](../runner/analysis/INDEX_UPDATE_RECORDING.md).

## 1. Exact program shape

Accept exactly five recorded statements, in this order, under the existing single-file public Main.main(String[]) entry restrictions:

```java
int x = 8;
int[] values = {5, 2};
int i = 0;
i = 1;
values[i] = x;
```

Both scalar declarations are separate non-final local int bindings initialized with signed decimal int literals. The non-final int[] declaration contains zero to 16 signed decimal int literals. The fourth statement is a simple literal assignment resolving to the original index binding. The final store resolves the receiver, index and RHS to the array, index and value bindings respectively. Names and literals may vary; binding resolution must not depend on spelling or equal values. Java 21 compilation remains authoritative for validity.

Preserve current source/entry restrictions, raw Unicode escape policy, comments, Unicode identifiers, tabs, LF/CRLF and multiline formatting. Invalid index values and empty arrays remain eligible for runtime failure. An invalid initial index changed to a valid index must succeed; only the actual index at the store determines bounds behavior.

Optionally allow the existing exact final probes after the recorded region, with names adapted to the resolved bindings:

```java
System.out.print("FINAL=" + x + "," + i + "," + java.util.Arrays.toString(values));
System.err.print("PROBE");
```

Retain existing JDK method resolution, scalar/array reference checks and System/java shadowing restrictions. Probes add no observable operations or general library tracing.

This increment excludes updating x within this five-statement shape, multiple updates, alternative statement orders, extra variables, arithmetic, compound assignment, increments, expression indices, array reads as RHS, aliases, calls, nested scopes, loops and recursion. These are tracing eligibility limits, not Java syntax errors or removal of later V1 requirements. Preserve earlier scalar-update programs.

## 2. Events, source locations and replay

| Cursor | Event and original highlight | x | i | values |
| --- | --- | --- | --- | --- |
| 0 | None | Absent | Absent | Absent |
| 1 | VARIABLE_DECLARE: `int x = 8` | 8 | Absent | Absent |
| 2 | ARRAY_DECLARE: `int[] values = {5, 2}` | 8 | Absent | [5, 2] |
| 3 | VARIABLE_DECLARE: `int i = 0` | 8 | 0 | [5, 2] |
| 4 | VARIABLE_WRITE: `i = 1` | 8 | 1 | [5, 2] |
| 5 | ARRAY_WRITE: `values[i] = x` | 8 | 1 | [5, 8] |

Reuse draft-2 and the existing identities: variable-1 is x, variable-2 is the array binding, variable-3 is i, and array-1 is the array object. The scalar write updates variable-3, never variable-1, and does not create another binding. Display names do not determine identity.

Record committed runtime values. Preserve Java evaluation order and evaluate each expression once. The final generated store must use the actual index and value name expressions; do not substitute either initializer or assignment literals. Scalar reads add no events. Emit ARRAY_WRITE only after a successful store.

Maintain five distinct original/generated operation associations tied to the exact submitted source hash. Both VARIABLE_DECLARE sites must remain distinct. The fourth highlight covers the original index assignment using the existing UTF-16, one-based, exclusive-end location convention. Reject missing, stale, duplicated or swapped associations; event kind alone is insufficient.

Backward from cursor 5 to 4 restores [5,2] and retains i=1. Cursor 3 restores i=0; cursor 2 removes only i; cursor 1 removes the array binding/object; cursor 0 removes x. Verify every state without rerunning Java. Browser rendering remains outside this increment.

## 3. Failures and limits

An invalid updated index emits no ARRAY_WRITE and retains four valid events, including the committed index update. Compare original/generated exception type and message under the existing comparison policy; generated stack frames may differ. Never fabricate a store or automatically rerun the program.

Budget 1 stops before array initialization; budget 2 before index initialization; budget 3 before the index assignment; budget 4 before the array store. Budget 5 permits successful completion. Check generated guard placement before the actual mutation as well as resulting trace prefixes: a missing event alone does not prove the assignment was prevented.

Completion requires all five expected operations, the recorder end marker and a trusted successful runner outcome. Preserve safe prefixes and distinct failure, cancellation, timeout and limit outcomes. Retain existing source/trace byte limits, 32-event cap, 16-element bound, diagnostics, isolation and deadlines.

## 4. Implementation boundaries

- Analyzer: reuse CombinedProbe and the immutable handoff. Resolve the index declaration, assignment target and store reference to the same binding; keep the value binding separate. Extend operation facts deliberately and preserve existing shape dispatch.
- Transformer: consume complete source-matched facts, edit only the AST copy, guard the index assignment before mutation and record its committed value afterward. Preserve helper-name collision handling and operation-specific source associations.
- Recorder: add or adapt an explicit bounded binding-aware scalar-write path. The current legacy variableWrite entry targets variable-1; retain its behavior for existing callers while allowing the new write to variable-3.
- Collector: extend the expected operation plan to five operations, with scalar state keyed by binding ID. Require the final index to equal the latest variable-3 value and the stored value to equal variable-1. Reject wrong binding IDs, source ranges, order and premature completion; stop at the first corrupt prefix.
- Reconstruction and contracts: verify independent scalar updates and all six cursor states using existing draft-2 events. Keep execution facts separate from presentation.

No new parser, runner, dependency, schema, API, service or frontend is proposed. Inspect existing extension points before editing. If an incompatible contract change proves necessary, discuss and document it before implementation.

## 5. Acceptance cases

The implementation gate must cover:

1. The example above: five events and final x=8, i=1, values=[5,8], with unchanged stdout/stderr against the original.
2. Renamed bindings, equal scalar values, unchanged index assignments, signed int boundaries, comments, Unicode/CRLF/tabs/multiline formatting, helper collisions and optional probes omitted.
3. First/last valid positions and arrays of one and 16 elements; changed indices must prove the store uses the updated value, not its initializer.
4. Invalid initial index changed to valid succeeds. Valid initial index changed to negative, out-of-range or int-boundary invalid values fails; empty arrays fail. Each failed store retains four events and matches original exception behavior.
5. Budgets 1 through 4 retain the corresponding prefixes and budget 5 completes. Inspect generated guards before each operation, especially the actual index assignment.
6. Three distinct resolved bindings and five operation sites. Reject an update targeting x or another binding, unsupported operators/order and other out-of-scope forms with bounded diagnostics.
7. Stale source, missing or swapped binding/access/site facts are rejected. Preserve the original AST and source while transforming a copy.
8. Five exact original/generated source associations and submitted/generated hashes, including repeated declaration kinds and the new index-assignment highlight.
9. Collector corruption checks for wrong update binding/name where applicable, stale index at the store, wrong sources, duplicate/skipped/reordered events, invalid values and an end marker after only four operations. Preserve only the valid prefix.
10. Contract and forward/backward reconstruction checks for every result and all six successful cursor states; backward index updates must leave x and the array unchanged.
11. Preserve the verified 66 automatic cases/56 original-generated comparisons, existing analyzer/transformation/collector/contract checks, both worker-limit probes, and seven manual cases/four comparisons. Add new cases without silently replacing earlier coverage; intentionally limited runs use prefix assertions rather than unlimited-run equivalence.
12. Keep fixture/output bounds enforced and verify removal of all owned containers. Record actual new totals and final gate outcome only after execution; never treat a successful build as runtime verification.

## 6. Verification status

Fresh verification on 2026-09-26: `runner/analysis/test-recording.ps1` exited 0. All 88 automatic runtime cases (66 previous and 22 new), 74 original/generated comparisons, and 88 contract/hash/source/forward-backward result checks passed. Fourteen intentionally limited cases use prefix assertions instead of comparison with an unlimited original run. All six cursor states are checked for successful five-operation runs.

Analysis passed the original 18 cases and existing transformation checks, plus index-update target/site facts, original preservation, stale/missing/swapped handoffs, guard placement and 27 rejected forms. The former literal-index-update rejection now has explicit positive coverage; compound updates remain rejected. Collector checks passed 32 new corrupt prefixes and wrong-plan rejection; draft-2 self-tests passed six new negative mutations. Both worker-limit probes and all existing shared checks passed.

Seven manual runtime cases/four original-generated comparisons and seven result checks passed. Every test-owned container was verified removed. The initial attempt passed the trusted build/contracts but stopped because Docker was unavailable; Docker Desktop startup was approved before the successful gate. The bundle was 998854 bytes, below the unchanged 1-MiB cap. Separate output-only and simulated Docker-classification suites were not rerun because execution orchestration was unchanged.

No dependency, schema, API or isolation policy changed. Milestone 1 remains incomplete; no frontend or browser playback exists. Arithmetic, loops and broader indexed statement combinations require further scope and approval.
