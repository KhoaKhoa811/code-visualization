# First classic for-loop recording

2026-09-29 contract update: user approved draft-3 schema, illustrative fixtures and validator work. [The contract guide](../contracts/README.md) records passing legacy checks, six new fixtures, 44 negative cases, twelve partial prefixes and fixture source/cursor checks. Draft-1/draft-2 schemas and original fixtures are unchanged. No Java loop implementation or Docker tests were performed. Earlier specification-only status below is historical; runtime implementation still requires approval.

Status: scope specified on 2026-09-28; machine-readable contract work approved and verified on 2026-09-29. Runtime loop implementation and Docker tests require separate approval. The last verified Java runtime baseline remains 150 automatic cases/124 comparisons. The existing [loop-step rules](java-support.md#approved-loop-step-rules) remain authoritative.

## 1. Bounded source shape

Use the existing ordinary Java 21 Main.java/Main.main entry convention:

```java
public class Main {
    public static void main(String[] args) {
        int x = 8;
        int[] values = {5, 2, 7};
        for (int i = 0; i < values.length; i++) {
            values[i] = x;
        }
    }
}
```

Accept exactly a scalar declaration, an array declaration, then one classic for loop. The loop has one non-final local int declaration initialized from an allowed signed decimal int literal; one condition `index < array.length`; one update, either `index++` or `++index`; and a braced body containing exactly one assignment `array[index] = scalar`. All references must resolve to those same declarations. Initial scalar/array literals, names and formatting may vary under the existing rules. Array lengths remain 0–16. The initial index may be any supported int literal, including negative values that fail at the first store or values at/above the length that skip the body.

Allow the existing style of optional exact development probes after the loop:

```java
System.out.print("FINAL=" + x + "," + java.util.Arrays.toString(values));
System.err.print("PROBE");
```

Validate actual JDK bindings and shadowing as in the current probes. Do not reference i after the loop; its Java scope has ended. These fixed test observations do not add general output-call tracing.

This increment does not add enhanced for, while, nested/multiple loops, break/continue, labels, alternate comparisons or bounds, missing header clauses, external index declarations, multiple initializers/updates, body declarations, unbraced bodies, decrement/compound updates, or embedded side effects. It also does not add array reads or a sorting algorithm to the loop body. These are later increments, not removal of the approved V1 loop/sorting requirements. Valid unsupported source remains subject to the existing execution-admission/output-only policy.

## 2. Runtime order and observable steps

Java performs initialization once, then repeats condition, body and update. A false condition ends normal iteration; a failed body does not execute its update. Preserve this order and evaluate each original expression once. This follows [JLS 14.14.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.14.1). The index belongs to the for-initializer scope, including its condition, update and body, as defined by [JLS 6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3).

The proposed example has 13 observable steps:

| Step | Original highlight | Recorded result | i after step | Array after step |
| --- | --- | --- | --- | --- |
| 0 | None | No bindings | Absent | Absent |
| 1 | `int x = 8` | Declare x=8 | Absent | Absent |
| 2 | `int[] values = {5, 2, 7}` | Declare array | Absent | [5,2,7] |
| 3 | `int i = 0` | Declare loop index | 0 | [5,2,7] |
| 4 | `i < values.length` | true | 0 | [5,2,7] |
| 5 | `values[i] = x` | Write index 0 | 0 | [8,2,7] |
| 6 | `i++` | Commit index update | 1 | [8,2,7] |
| 7 | `i < values.length` | true | 1 | [8,2,7] |
| 8 | `values[i] = x` | Write index 1 | 1 | [8,8,7] |
| 9 | `i++` | Commit index update | 2 | [8,8,7] |
| 10 | `i < values.length` | true | 2 | [8,8,7] |
| 11 | `values[i] = x` | Write index 2 | 2 | [8,8,8] |
| 12 | `i++` | Commit index update | 3 | [8,8,8] |
| 13 | `i < values.length` | false; leave loop scope | Absent | [8,8,8] |

x stays 8 after its declaration. Prefix update uses its actual `++i` highlight with the same committed values. Reading the scalar, index or array length inside these operations does not add a separate step. The entire condition is one operation, including its recorded boolean result. The result belongs to the selected condition highlight; it is not a synthetic user variable or a required operation-box diagram.

The index declaration executes once, not once per iteration. Each condition, body write and update reuses its static source site with a new event sequence number. There are six distinct operation sites, not 13 static sites. Use the existing exact-source hash and one-based UTF-16/exclusive-end coordinates. Highlights exclude semicolons, braces and the surrounding for header.

## 3. Draft-3 contract and scope handling

Draft-2 has a closed event union, int-only values and no scope-exit representation. Do not encode a condition as an integer assignment or silently extend draft-2. Use draft-3 as described in [ADR 0007](decisions/0007-loop-condition-and-scope-draft-3.md). Machine-readable schema/fixtures and validator changes passed the contract gate on 2026-09-29; a loop producer is not implemented yet.

Keep the result envelope, source identity, terminal outcomes and console rules. Draft-1/draft-2 producers and consumers retain their current behavior. For draft-3:

- Retain the existing array/scalar event payloads. Add required `scopeId` to declaration events, identifying the binding's scope, not array ownership.
- Add CONDITION with sequence, kind, source, scopeId and `value: {type: "boolean", value: true|false}`. It does not contain an arrayId or variableId. Boolean values here do not authorize general boolean-variable tracing.
- Add required `exitedVariableIds` to every draft-3 event as an array of binding IDs. This is an atomic post-operation effect, not another event or click. It is empty except on the final false condition in this scope, which contains exactly the index binding ID.
- For this single main invocation, use variable-1 for x, variable-2 for the array binding, variable-3 for i and array-1 for its object. Main declarations use scope-main; i and CONDITION use scope-loop-1. Scope-loop-1 has scope-main as its parent in the validated analysis/operation plan. These are runtime identities for this bounded invocation, not a name-based identity rule for future methods or recursion.

All events still correspond to one observable step. Both safe-boundary fields equal the number of accepted events. Publish the final false result and retirement of i together; no consumer may expose half that step. Validate the complete record before changing replay or collector state. A truncated/corrupt record cannot remove i or publish its false result. Unknown, duplicate, wrong-scope or already-retired exit IDs are invalid. Body/write/update references to a retired binding are invalid. Earlier declaration identities cannot be reused.

At step 13, remove i and retain x and the array. Backward to step 12 restores i=3 and the increment highlight; backward across step 11 restores the previous array; step 3 restores i=0; earlier cursors remove declarations independently. Rebuild state from captured prefixes, never by executing or predicting a loop. The last selected condition result must also clear or restore as the cursor changes.

On failure/limit/cancellation, playback ends at the last accepted operation snapshot. Do not fabricate a false condition or scope-exit record during exception unwinding; i may remain visible at that earlier snapshot even though the Java process has terminated. This is not a claim about live post-exception locals. On success, preserve x/array at the last recorded teaching snapshot; full main-frame teardown remains outside this prototype.

## 4. Analysis and instrumentation boundaries

The current analyzer recognizes fixed statement shapes. Its scope builder models blocks/methods, and enclosingScope deliberately returns no scope for an unmodeled ForStmt. Introduce explicit facts for the eligible for scope rather than mislabeling i as a main-block variable or weakening the guard for all unmodeled constructs.

Add a dedicated loop analysis result composed from the existing source, declaration, binding, access and increment facts. It must include all six operation spans; the for/header/body boundaries; main/loop scope relationship; resolved scalar/array/index identities and types; the less-than operator; the length receiver and its array type; and the chosen increment operator/operand. The array-length receiver must be the declared int[] and length must be its built-in int length, not an arbitrary similarly named field or method. Reject stale, absent, swapped or inconsistent facts before transformation. Do not overload the existing fixed scalarWrite/storeIndex fields with ambiguous loop meanings.

The transformer consumes those immutable facts and edits only the source-matched syntax copy. A suitable bounded lowering is a synthetic enclosing block containing the original index initialization and a while(true) loop. Inside that loop:

1. Check the recording budget before evaluating the original condition.
2. Evaluate the cloned condition once into a fresh synthetic boolean temporary.
3. Record that boolean and any normal-exit effect atomically; branch on the same temporary. False leaves the loop immediately.
4. Execute and record the original array store through the existing guarded write path.
5. Guard the index update, retain the original prefix/postfix statement once, then record the plain updated index.

Guard and record index initialization before the first condition. Keep the synthetic block around the lifted declaration so i cannot escape its original scope. Synthetic temporaries/branches/braces have no diagram bindings, user-source highlights or extra steps. Avoid helper/temporary collisions, including Unicode names. Do not put guard logic only inside a helper accepting an already evaluated condition; Java evaluates arguments first. Do not capture `i++` as the recorder value argument.

This lowering is limited to the stated shape. Its synthetic break does not implement user break/continue. Later control-flow support needs an explicit update/exit routing design; do not reuse this lowering blindly for it. Existing nine shapes keep their current paths and versions. No second Java parser, interpreter or execution service is needed.

## 5. Collector, replay and limits

The current ArrayTrace operation list consumes each source site once. Add a bounded loop plan and phase validator at that boundary, keeping transport/runner reuse and legacy fixed plans. The new phase sequence is:

```text
x declaration -> array declaration -> index declaration -> condition
condition(true) -> array write -> index update -> condition
condition(false + retire index) -> trace end
```

This is validation of recorded execution, not a precomputed event list. Do not unroll the loop from source literals or manufacture condition results. Accept each event only at its expected site/phase. Check actual array indices against the current captured index and written values against x; check a captured condition against those captured values and the recorded array length. Such consistency checks must reject contradictions rather than repair them. A normal end marker is valid only after the false-condition/retirement event; later data or another loop operation is invalid.

The general schema validator checks version/payload/identity/scope/retirement consistency. The plan-aware collector additionally checks original sites, exact control-flow order and source-specific operand relationships. Replay consumes recorded values and exit IDs; it must not infer scope exit by parsing source. Do not infer iterations from source coordinates, which repeat.

Retain existing caps: 32 events, 65536 collected trace bytes (recorder retains its termination reserve), 16 array elements, 64-KiB source/generated source, and current Docker/console/worker bounds. Do not raise limits merely to complete a long loop. A successful run with m body executions needs 4+3m events: three declarations, m condition/write/update triples and the final false condition. Zero iterations therefore still produce four events.

For the three-element example, budgets 1–12 stop before the next operation; 13 permits completion. Budget 3 stops before the first condition, 4 before the first store, 5 before the first increment, 12 before the final false condition (i is still present), and 13 includes atomic retirement. A nine-element array starting at zero completes in 31 events. A ten-element array starting at zero reaches the 32-event cap after its tenth store and stops before the next update; no false condition/exit is invented. A 16-element array remains eligible but may have a partial trace; starting at 7 permits nine iterations within 31 events.

For a negative initial index, condition may be true and the first store fails: retain the three declarations and true condition, with no write/update/normal-exit event. Starting at or above the array length skips the body. Int MIN/MAX initial values are useful boundary cases, but this bounded ascending traversal cannot reach increment overflow before exiting or failing; retain standalone overflow regression coverage without claiming loop overflow was exercised.

Preserve safe prefixes on truncated data, byte/event limits, cancellation and timeout. Stop at the first invalid record; never skip it and resume later. Original/generated comparisons apply to unrestricted successful/failed examples. Deliberately limited runs use expected-prefix and generated guard-placement assertions rather than comparisons to an unlimited original.

## 6. Acceptance and implementation order

Before enabling a producer, create draft-3 schema/examples and validator tests for CONDITION, booleans, declaration scope IDs, atomic exits, wrong/unknown/duplicate/retired IDs, sequence/source/boundary errors and cross-version rejection. Draft-1/draft-2 fixtures must remain unchanged and pass. Reject truthy strings/numbers as boolean values. Any validator keyword/type extension must be deliberate and tested; boolean schema type support was added and tested in the 2026-09-29 contract task; unsupported types/keywords still fail.

Then implement the bounded analyzer/transformer/recorder/collector/replay path and verify:

1. Both update forms on the 13-step example; exact per-step values, true/false results, six source associations, last-step retirement and reverse restoration.
2. Empty/one/multiple-element arrays, changed literals, equal/repeated values, different nonnegative starts, start equal to/greater than length, negative starts and int extremes. Compare original/generated stdout/stderr and exception type/message where appropriate.
3. Budgets 1–13 on the example, nine-iteration exact completion, 32-event truncation and 16-element eligibility/partial/success variants. Test byte truncation at the final atomic condition record and guard placement before condition/body/update.
4. Resolved for scope and bindings, built-in array length, immutable facts, stale sources, missing/swapped condition/update/body sites, wrong scope parents/types/identities/operators and original AST preservation.
5. Rejection of every excluded form, including nested/enhanced/while loops, break/continue, nonmatching bindings, field operands, expression indices, missing/multiple clauses, unsupported body shapes and shadowed development probes. Compiler errors remain distinct from valid tracing limitations.
6. Unicode/comments/tabs/CRLF, multiline headers, helper/temporary collisions, absent probes and repeated-site mapping. Count one condition evaluation per visit, one store per successful body and one increment per completed iteration; generated syntax checks and runtime comparisons must both contribute evidence.
7. Corrupt control-flow order, false result with omitted/wrong exits, true result with exits, stale index/store values, wrong recorded conditions, duplicate declarations, early end, records after exit and truncated records. No partially accepted event may change replay state.
8. Retain all 150 runtime cases/124 comparisons, current source/replay tests, analyzer/collector/contract guards, eight fixture-delivery negatives, both worker-limit probes and seven manual cases/four comparisons. Cancellation/timeouts and owned-container cleanup remain required.

Fixture delivery is already near a cap: current fresh batches are 998854/759159 bytes. Add bounded batches/workers if needed, preserving 1-MiB limits, fresh directories, allowlists and aggregate duplicate/missing checks; do not remove earlier cases. Select the actual new fixture count during implementation and record measured totals only after the gate passes.

Capture native Java/Node output explicitly in future test logs: the last PowerShell transcript omitted that output. Save the actual process exit and completion summaries. Interrupted attempts without a final exit are not full-gate evidence.

## 7. Status and next approval

The September 28 specification task reviewed requirements, stepping decisions, implementation boundaries and Java 21 references and changed documentation only. On September 29, the approved contract task added draft-3 schema/fixtures and validator checks. All contract checks passed; no Java/Docker tests ran and no runtime-loop code changed.

The contract gate is complete. Recommended next task, requiring confirmation: implement the bounded analyzer/transformer/recorder/collector/replay path and run Docker acceptance/regression checks for this loop scope. Milestone 1 and frontend/browser playback remain incomplete; sorting, broader loop forms and later V1 requirements remain planned.
