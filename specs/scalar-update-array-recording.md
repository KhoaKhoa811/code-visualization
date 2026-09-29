# Scalar update before an array write

Status: scope specified and implementation/Docker verification approved on 2026-09-24. This increment composes the existing [integer assignment](integer-recording.md) and [combined scalar/array](combined-variable-array-recording.md) capabilities. It does not change their previously verified semantics. The [combined recording guide](../runner/analysis/COMBINED_RECORDING.md) describes the implementation.

## 1. Exact scope

Within the existing single-file public Main.main(String[]) convention, accept exactly these four recorded statements in this order:

```java
int x = 3;
int[] values = {5, 2};
x = 8;
values[0] = x;
```

The scalar is one non-final local int with a signed decimal int-literal initializer. The array is one non-final local int[] initialized with zero to 16 signed decimal int literals. The scalar assignment uses simple `=` with a signed decimal int literal; its target must resolve to the original scalar binding. The final assignment uses a signed decimal literal index, the original array binding as receiver, and a simple name resolving to the same scalar binding as RHS. Names and literal values may vary within the existing Java int rules. Java 21 compilation remains authoritative for program validity.

Retain comments, Unicode identifiers, LF/CRLF, tabs and multiline source mapping. Retain current entry/class restrictions, raw Unicode escape restrictions and source bounds. Empty arrays and invalid indices are eligible runtime-error cases, not syntax errors.

Optionally allow exactly the existing two trailing development probes:

```java
System.out.print("FINAL=" + x + "," + java.util.Arrays.toString(values));
System.err.print("PROBE");
```

Keep their existing exact syntax, resolved bindings/types/JDK declarations and System/java shadowing checks. These probes add no visualization steps and do not establish general library tracing. The four-statement form without probes is also eligible.

Excluded: extra declarations or assignments, different statement order, scalar assignment after the array write, arithmetic, variable indices, scalar-to-scalar assignment, array-to-scalar reads, compound assignment, increments, calls in recorded expressions, aliasing, null/length-based array creation, nested scopes, loops, methods and recursion. Preserve all three existing eligible shapes; do not route solely by statement count, since this four-statement form overlaps older shapes with output probes. Later V1 requirements remain planned.

## 2. Events, state and source highlighting

| Cursor | Event and original expression | Scalar | Array |
| --- | --- | --- | --- |
| 0 | None | Absent | Absent |
| 1 | VARIABLE_DECLARE: `int x = 3` | 3 | Absent |
| 2 | ARRAY_DECLARE: `int[] values = {5, 2}` | 3 | [5, 2] |
| 3 | VARIABLE_WRITE: `x = 8` | 8 | [5, 2] |
| 4 | ARRAY_WRITE: `values[0] = x` | 8 | [8, 2] |

Each event records a successful committed operation. Record the actual value after the scalar assignment; pass the current runtime scalar value into the array store. Never substitute either initializer or assignment literals for runtime capture. Evaluate the receiver, index and RHS once in Java order. Reading the scalar for the store adds no extra event. Assigning the same value still produces VARIABLE_WRITE because an observable assignment executed.

Highlight the whole original scalar assignment expression at cursor 3 and the whole array assignment at cursor 4. Preserve source hashes and original/generated site associations, using the existing UTF-16, one-based, exclusive-end ranges. Recorder guards and other synthetic bookkeeping have no separate original highlight or step.

Backward playback from cursor 4 restores [5, 2] while x stays 8. Moving to cursor 2 restores x=3 and retains [5, 2]. Moving to cursor 1 removes only the array binding/object; cursor 0 removes x. Test-only reconstruction must derive these states from recorded events without rerunning Java. Main scope teardown remains outside this experiment.

## 3. Failure and limit boundaries

A negative/out-of-bounds/empty-array write emits no ARRAY_WRITE. Retain exactly the first three events, including the committed scalar update. Compare original/generated exception type and message; stack frames may differ. No fabricated exception event or automatic fallback rerun is allowed.

Guard before each next observable operation. In particular, guard before the actual scalar assignment, not only inside a recorder call afterward.

| Event budget | Safe prefix | Next operation must not execute |
| --- | --- | --- |
| 1 | Scalar declaration; x=3 | Array initialization |
| 2 | Both declarations; x=3, array=[5,2] | Scalar assignment |
| 3 | Both declarations and scalar write; x=8, array=[5,2] | Array store |
| 4 or more | Four events on successful execution | No extra event is required for completion |

A budget of exactly four must allow normal completion. Timeouts, cancellation and transport failures retain only the validated prefix. Completion requires the recorder end marker and a trusted successful runner outcome; a three-event end marker cannot certify completion of this four-operation shape. Partial results retain explicit diagnostics and matching safe boundaries.

## 4. Contracts and implementation boundaries

Reuse draft-2 unchanged: scalar variable-1, array binding variable-2, object array-1. VARIABLE_WRITE refers to variable-1, with no new binding allocation. ARRAY_WRITE references array-1 and must match the latest recorded scalar value, not its declaration value. Existing versions and identities remain unchanged.

- Analysis: extend the existing combined eligibility adapter and immutable handoff with the scalar assignment site and resolved target binding. Reuse parsing, entry checks, diagnostics and bounds. Preserve both write sites distinctly; reject missing, inconsistent or stale facts.
- Transformation: reuse the copied-AST transformer and scalar assignment guard/capture. Preserve the actual assignment and runtime RHS. Return four original/generated site associations and preserve helper collision checks and generated-source bounds.
- Recorder and collector: reuse one FIFO and ordered counter. Extend explicit operation expectations to distinguish the existing three-operation shape from this four-operation shape. Validate scalar binding kind/identity and update the stored scalar state before validating the array store. Do not relax collection into accepting arbitrary operation order or premature completion.
- Reconstruction and contracts: extend existing tests for VARIABLE_WRITE interleaved with array events. Keep scalar bindings and array objects independent. No schema migration, new execution engine, dependency, API or frontend is proposed.

Keep the existing 64-KiB source/generated-source/trace bounds, 32-event and 16-element caps, worker restrictions and runner deadlines. Implementation must remain within those limits. An incompatible contract change would require a separately documented decision before producer/consumer changes.

## 5. Acceptance cases after implementation approval

| ID | Case | Required evidence |
| --- | --- | --- |
| UPDATE-01 | Exact example | Four events and all five states above; final output `FINAL=8,[8, 2]` |
| UPDATE-02 | Renamed bindings, changed literals, index 1 | Store the updated runtime scalar into only the selected element |
| UPDATE-03 | Negative, repeated and int-boundary values | Exact values; same-value scalar assignment still records a step |
| UPDATE-04 | Unicode names, supplementary comment, CRLF/tabs/multiline, helper collisions | Exact source ranges/hashes, four generated associations, unchanged original AST/source |
| UPDATE-05 | No probes; one-element and 16-element arrays | Correct states independent of probes and array length |
| UPDATE-06 | Negative/out-of-range index and empty array | Matching exceptions; three-event safe prefix including updated scalar |
| UPDATE-07 | Budgets 1, 2, 3 and exactly 4 | Guard placement before each next operation; exact partial states and normal completion at 4 |
| UPDATE-08 | Stale source; unresolved/wrong scalar target or RHS; missing binding/access/site facts | No generated source; distinguish tracing limitations/unresolved facts from syntax errors |
| UPDATE-09 | Excluded syntax and oversized inputs | Reject this capability without breaking legacy shape dispatch or claiming compiler-invalid Java solely from unsupported tracing |
| UPDATE-10 | Corrupt mixed streams | Reject wrong variable ID/kind, stale declaration value used by array write, wrong sites/names, duplicate identities, sequence gaps, invalid ints, skipped/reordered writes and premature end; never resume after corruption |
| UPDATE-11 | All forward/backward cursors and edited source | Restore scalar and array independently; correct original highlight; reject stale source without execution |
| UPDATE-12 | Regression baseline | Preserve all 31 automatic cases/27 comparisons, source/contract/reconstruction checks, analyzer/collector checks, both worker-limit probes and seven manual cases/four comparisons |

Analyze and transform submitted fixtures only inside the bounded worker. Compile and execute original/generated programs in the existing isolated Docker runner. Compare outputs, final probes and runtime errors for non-limited cases; check deliberately limited cases against explicit prefixes instead of unlimited behavior. Verify generated guard placement as well as trace prefixes, since a prefix alone cannot prove an unrecorded mutation did not occur. Validate result contracts and owned-container cleanup.

Completion requires the applicable acceptance and regression evidence in both memory files; this increment does not complete Milestone 1 or authorize the next capability.

## 6. Verification status

Implemented September 24; verification handoff recovered September 25. Observed Docker test output confirmed all 47 automatic runtime cases (16 scalar-update), 40 original/generated comparisons, all 47 result/reconstruction checks, analyzer/guard-placement checks, both worker-limit probes and shared contract/collector checks. New negative coverage includes 20 rejected input forms, unsafe handoffs, 23 collector corrupt prefixes and six contract mutations. Budgets one/two/three stop at the required boundaries; budget four completes. Failed array writes preserve the scalar update and three-event prefix.

All seven manual-regression artifacts were saved after their driver's behavior/comparison/cleanup assertions, covering four paired comparisons. Recovery verified their generated-source hashes and reran both result validators successfully (47 automatic and seven manual). The earlier process exit code was unavailable; no new Java/Docker runtime execution was performed on September 25. No browser tests exist yet. See memory/current-state.md for the evidence distinction and remaining milestone scope.
