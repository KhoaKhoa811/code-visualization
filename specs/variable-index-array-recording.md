# Variable-index array recording

Status: scope specified and implementation/Docker verification approved on 2026-09-25. This extends the [combined recording capabilities](combined-variable-array-recording.md), preserving the [scalar-update capability](scalar-update-array-recording.md). See the [implementation guide](../runner/analysis/VARIABLE_INDEX_RECORDING.md).

## 1. Exact program shape

Accept exactly four recorded statements in this order inside the existing public Main.main(String[]) convention:

```java
int x = 8;
int[] values = {5, 2};
int i = 1;
values[i] = x;
```

Both scalars are separate non-final local int variables initialized by signed decimal int literals. The array is a non-final local int[] initialized by zero to 16 signed decimal int literals. The final simple assignment must resolve its receiver to that array, its index name to the index binding, and its RHS name to the value binding. Names and literals may vary. Neither the names x/i nor their initializer values substitute for binding resolution. Java 21 compilation remains authoritative for program validity.

Retain current entry/class restrictions, source limits, raw Unicode escape restrictions and support for comments, Unicode identifiers, tabs, LF/CRLF and multiline expressions. Negative or out-of-range index values and empty arrays remain eligible for runtime failure; do not classify them as syntax errors.

Allow the four-statement form without probes, or exactly these two trailing development probes using the chosen names:

```java
System.out.print("FINAL=" + x + "," + i + "," + java.util.Arrays.toString(values));
System.err.print("PROBE");
```

Use the existing narrowly resolved JDK probe policy, including System/java shadowing rejection, and verify both scalar references and the array argument. The probes observe final values outside the recorded region and add no steps. This does not add general library tracing.

This increment excludes changes to either scalar after declaration, other declaration orders, extra variables/statements, using the value binding as the index or index binding as the RHS, expression indices such as i+1/i++, array reads as RHS, arithmetic, calls, aliases, null/length-based array construction, nested scopes, loops, methods and recursion. These are tracing limits, not claims that ordinary Java cannot execute such code. All four previously verified program shapes remain supported. Further combinations with scalar/index updates require a later explicit scope; later V1 requirements remain unchanged.

## 2. Observable states and source mapping

| Cursor | Event and highlight | Value scalar | Index scalar | Array |
| --- | --- | --- | --- | --- |
| 0 | None | Absent | Absent | Absent |
| 1 | VARIABLE_DECLARE: `int x = 8` | 8 | Absent | Absent |
| 2 | ARRAY_DECLARE: `int[] values = {5, 2}` | 8 | Absent | [5, 2] |
| 3 | VARIABLE_DECLARE: `int i = 1` | 8 | 1 | [5, 2] |
| 4 | ARRAY_WRITE: `values[i] = x` | 8 | 1 | [5, 8] |

Capture actual runtime declaration values. Evaluate the target array, index and RHS once in Java order, and emit ARRAY_WRITE only after the store succeeds. The event's index must be the actual evaluated integer, and its value must be the actual stored value. Do not replace i or x with constants in generated code. Scalar reads of i and x add no steps, continuing the existing scalar-read policy.

Keep four separate original/generated site associations tied to the exact source hash. The two VARIABLE_DECLARE events must retain their own identities and source ranges. A map keyed only by event kind or helper method name cannot represent this reliably. Use explicit operation identity/order and source/binding facts; reject missing, duplicated or swapped associations. Bookkeeping has no extra highlight. Source positions retain the existing UTF-16, one-based, exclusive-end convention.

Backward from cursor 4 restores [5,2] while retaining x=8 and i=1. Cursor 2 removes only i. Cursor 1 removes only the array binding/object. Cursor 0 removes x. Verify all states without rerunning Java. Test reconstruction remains separate from a future browser renderer; scope teardown is outside this region.

## 3. Identities, limits and failures

Reuse draft-2 without new event fields: value binding variable-1, array binding variable-2, index binding variable-3, array object array-1. These run-local identities are independent of display names. Preserve all existing producers' IDs and versions. The collector must track both scalar values separately and verify the store index against the index binding and the stored value against the value binding.

An invalid index emits no ARRAY_WRITE. Preserve exactly the three declarations, including the invalid int index value, as the safe prefix. Compare original/generated exception type and message; generated stack frames may differ. Never fabricate a successful write or rerun automatically. Failed negative indices need no negative-index ARRAY_WRITE payload because no store event is emitted.

Check budgets before the next observable operation. Budget one stops before array initialization; budget two stops before index initialization; budget three stops before array evaluation/store. Budget four allows successful completion after four events. Guard placement must be verified as well as trace prefixes. Completion requires the expected four operations, recorder end marker and trusted successful runner outcome. Preserve available validated prefixes and explicit outcomes for failure, cancellation, timeouts and transport limits.

Retain 64-KiB original/generated source and trace bounds, 32-event and 16-element caps, diagnostic limits and existing isolation/deadlines. No new runtime limit is needed for three bounded bindings and one array.

## 4. Implementation boundaries

- Analysis: reuse parsing, entry checks, scopes and diagnostics. Supply three distinct resolved binding descriptors and ordered operation sites, including both scalar declarations and the receiver/index/RHS uses. Preserve legacy dispatch when statement counts overlap.
- Transformation: consume complete source-matched facts, edit only a syntax copy, preserve actual name expressions and helper-collision handling, and associate each generated call with its own operation. Do not infer associations from initializer values or event kind alone.
- Recorder: extend scalar capture to accept explicit bounded binding identities rather than one global scalar slot. Reuse FIFO transport, limits and the shared event counter. Keep legacy entry points or deliberately adapt their callers while preserving their verified output.
- Collector: use expected operation/site/binding descriptors and bounded scalar state keyed by binding identity. Enforce order, names, types, distinct declarations, original ranges and latest value/index agreement. Never resume past a corrupt prefix. Preserve legacy validation strictness.
- Reconstruction/contracts: apply repeated VARIABLE_DECLARE events to independent bindings; test that moving backward removes only the matching declaration. Draft-2 already expresses this sequence, but add mixed multi-scalar semantic cases and regressions. Do not add presentation fields or a second execution engine.

These are internal analysis/instrumentation/collection changes. No dependency, API, service, frontend or general Java-language engine is proposed. If an incompatible trace change becomes necessary, document and review it before modifying producer/consumer contracts.

## 5. Acceptance cases after implementation approval

| ID | Case | Required evidence |
| --- | --- | --- |
| INDEX-01 | Exact example | Four events/five states; final probe `FINAL=8,1,[5, 8]` |
| INDEX-02 | Rename all bindings; vary x, i and array literals | Resolve by binding; write only the selected element using actual runtime values |
| INDEX-03 | Unequal x/i values; equal values; negative/min/max scalar values | No conflation of scalar roles or int coercion; equal values still have distinct identities |
| INDEX-04 | First/last valid indices; one/16-element arrays; absent probes | Correct index and full snapshot with no dependency on probes |
| INDEX-05 | Negative index, index equal to length, int-min/int-max indices, empty array | Matching runtime exceptions; exactly three safe declaration events |
| INDEX-06 | Budgets 1, 2, 3 and exactly 4 | Guard before next operation; exact partial states; normal completion at four |
| INDEX-07 | Unicode names, supplementary comment, CRLF/tabs/multiline, one-line program, helper collisions | Four correct source associations, including two distinct VARIABLE_DECLARE sites; unchanged original AST/source |
| INDEX-08 | Stale source; unresolved/wrong-type/wrong-role receiver/index/RHS; missing/swapped binding/site facts | No generated source from unsafe facts; distinguish tracing limitations from Java syntax errors |
| INDEX-09 | Excluded syntax and oversized source/array | Reject this capability while preserving all earlier eligible shapes, including overlapping statement counts |
| INDEX-10 | Corrupt multi-scalar stream | Reject duplicate IDs, wrong scalar kind/name/site, swapped declaration sites, unknown object, index/value taken from wrong binding, invalid ints, sequence gaps, skipped/reordered operations and premature end; retain only valid prefix |
| INDEX-11 | Forward/backward at every cursor; source edited after recording | Independent value/index/array restoration and correct highlight; stale-source rejection without execution |
| INDEX-12 | Existing baseline | Preserve 47 automatic runtime cases/40 comparisons, all source/contract/reconstruction checks, analyzer/collector guards, both worker-limit probes and seven manual cases/four comparisons |

Analyze/transform inside the bounded worker. Compile and execute original/generated admitted fixtures separately in the isolated Docker runner. Compare non-limited outputs/final probes and runtime exception type/message. Check intentionally limited cases against explicit prefixes instead of unlimited execution. Validate draft-2 results and owned-container cleanup. Update both memory files with actual results and any remaining limitations.

This increment does not establish arbitrary variable-index support or complete Milestone 1.

## 6. Verification status

Verified 2026-09-25: runner/analysis/test-recording.ps1 exited 0. All 66 automatic runtime cases (19 variable-index), 56 original/generated comparisons and 66 schema/source/reconstruction checks passed. The bounded worker passed index binding/site checks, stale/missing/swapped handoffs, guard placement, 21 unsupported inputs, legacy analyzer/transformation checks and both worker-limit probes. Shared collector checks include 25 new corrupt prefixes; draft-2 tests include six new negative mutations. Seven manual recording cases/four comparisons and all seven result checks also passed. Every test-owned container was verified removed.

The main example records x=8, values=[5,2], i=1, then values=[5,8]. Invalid indices retain three declarations. Budgets one/two/three preserve the required prefix; budget four completes. Repeated declaration highlights and backward restoration are checked without Java reruns. The fixture bundle remains under the existing 1-MiB worker-output cap. No dependency, schema version or runner isolation change was required. No frontend/browser test exists yet; broader Java support remains outside this increment.
