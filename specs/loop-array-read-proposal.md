# Array read and addition inside a classic for loop

Status: design proposed 2026-09-30; user approved implementation on 2026-10-01. Bounded implementation and regression verification completed on 2026-10-02. The [recording guide](../runner/analysis/LOOP_READ_RECORDING.md) records scope, recovered runtime evidence and the Docker infrastructure retry. Earlier [scalar-fill loops](for-loop-array-recording.md) remain supported.

## Approved source shape

```java
public class Main {
    public static void main(String[] args) {
        int[] values = {5, 2, 7};
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] + 1;
        }
    }
}
```

Accept this natural two-statement main body, without requiring the unrelated scalar declaration used by the earlier scalar-fill example. Preserve that earlier shape and its identities unchanged. For the new shape, allow renamed bindings, existing signed decimal int literals, array lengths 0–16, literal initial indices, formatting variations, and either i++ or ++i. The RHS is exactly the same array/index read plus one signed decimal int literal; both accesses must resolve to the declared array and loop index. An optional unused scalar prelude is outside this increment.

Optional development observations after the loop are exactly `System.out.print("FINAL=" + java.util.Arrays.toString(values));` followed by `System.err.print("PROBE");`, with the resolved array name substituted. Validate JDK bindings and shadowing as in earlier probes. These are test observations, not general output-call tracing.

Other operators, compound assignments, array-element increments, multiple RHS reads, different source/target indices or arrays, aliases, method calls, extra body statements, nested loops, break/continue and sorting remain later work. Valid unhandled programs retain the approved execution-admission/output-only policy.

## Approved observable steps

The body assignment has two steps: ARRAY_READ highlights only the RHS `values[i]` and captures its old value without changing the array; ARRAY_WRITE highlights the whole assignment and captures the successfully stored result. Computing the addition introduces no separate event. This follows the existing distinction between array access and assignment, and the bounded arithmetic grouping in [index addition](index-addition-array-recording.md). It does not settle every future expression's stepping rules.

For the example above:

| Step | Highlight/result | i afterward | Array afterward |
| --- | --- | --- | --- |
| 1 | Declare values | Absent | [5,2,7] |
| 2 | Declare i=0 | 0 | [5,2,7] |
| 3 | Condition true | 0 | [5,2,7] |
| 4 | RHS values[i]: read 5 | 0 | [5,2,7] |
| 5 | Assignment: store 6 | 0 | [6,2,7] |
| 6 | Increment | 1 | [6,2,7] |
| 7 | Condition true | 1 | [6,2,7] |
| 8 | RHS values[i]: read 2 | 1 | [6,2,7] |
| 9 | Assignment: store 3 | 1 | [6,3,7] |
| 10 | Increment | 2 | [6,3,7] |
| 11 | Condition true | 2 | [6,3,7] |
| 12 | RHS values[i]: read 7 | 2 | [6,3,7] |
| 13 | Assignment: store 8 | 2 | [6,3,8] |
| 14 | Increment | 3 | [6,3,8] |
| 15 | Condition false; retire i | Absent | [6,3,8] |

Cursor 0 has no bindings. Reversing step 5 restores [5,2,7] and the read highlight/value; reversing step 4 changes attention without changing the array. Reversing step 15 restores i=3. Read/condition details belong only to their selected step and must not appear as extra program variables or persist on unrelated steps. This specifies playback state, not diagram styling or a frontend implementation.

## Baseline findings from the 2026-09-30 design review

These findings describe the earlier scalar-fill baseline and the required extension. Current implementation/verification status is in [the recording guide](../runner/analysis/LOOP_READ_RECORDING.md).

- [Draft-3](../contracts/run-result-v3.schema.json) already includes ARRAY_READ with arrayId, index, typed int value and exitedVariableIds. The [validator](../contracts/validate.mjs) checks reads against captured array state. No schema version or field change is proposed; prove compatibility with new contract fixtures before enabling a producer.
- [LoopProbe](../runner/analysis/src/main/java/dev/codeviz/analysis/LoopProbe.java) currently requires the scalar/array/loop prelude and a scalar RHS. Add a distinct body-fact variant and the two-declaration operation plan. Keep read access, write target, addition operand/operator/type and source spans explicit; do not reinterpret scalarReference as an array read.
- [ArrayAnalyzer](../runner/analysis/src/main/java/dev/codeviz/analysis/ArrayAnalyzer.java) currently recognizes the candidate for scope only in the earlier three/five-statement main body. Extend dispatch and scope recognition specifically to the new two/four-statement shape, retaining guards across all other unmodeled scopes.
- [LoopTransformer](../runner/analysis/src/main/java/dev/codeviz/instrumentation/LoopTransformer.java) currently emits one guarded store per iteration. Add separate guarded read and write operations, retaining exact-source validation, scoped index initialization and collision-free temporaries.
- [Recorder](../runner/prototype/recording/Recorder.java) already has a guarded read that captures the actual value. Reuse its transport, but adapt loop-only binding setup: current loop helpers assume scalar variable-1, array variable-2 and index variable-3. The new shape proposes array binding variable-1 and index variable-2, with array-1, scope-main and scope-loop-1. Carry these bindings explicitly in the new plan; preserve old IDs and old generated helpers.
- [LoopTracePlan](../runner/prototype/LoopTracePlan.java) currently checks condition -> scalar store -> update. A new body variant needs condition -> read -> write -> update. Validate read index/value against captured state, retain that accepted read for the pending write, and verify the recorded write against Java int addition with the analyzed literal. Reject contradictory records atomically; never repair or generate them.
- [Loop replay](../runner/prototype/recording/loop-replay.mjs) currently rejects ARRAY_READ. Add non-mutating read handling and selected read details, using only captured facts. Replay must never recalculate the addition.

## Evaluation, failures and limits

Java simple array assignment evaluates the target reference/index before its RHS; preserve that ordering. Capture the RHS array element once and reuse that captured value in the original int addition. Preserve int overflow rather than widening or clamping. References: [JLS 15.26.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.1), [15.10.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10.4), and [15.18.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.2), reviewed on 2026-09-30.

A suitable lowering retains the evaluated target reference/index in synthetic temporaries, guards and captures the RHS read, then guards the addition/store and records the committed result. No second array read may be added for recording. Budget checks must precede the relevant operation, not exist only inside a helper whose arguments have already been evaluated. Synthetic temporaries create no user steps or diagram bindings.

A failed RHS access emits no successful read, write, update or normal exit. With initial index -1, the condition is true and the trace stops after three events when the read fails. Empty arrays with start 0, or starts at/above length, produce three events ending with false. A limit after a successful read preserves that read and the unchanged array; it cannot fabricate a write or index retirement.

Keep the existing 32-event, trace-byte, source, array and Docker limits. A successful run with m iterations has 3+4m events. The example completes at 15; budgets 1–14 stop at exact prefixes. Seven iterations complete at 31 events. Eight iterations hit 32 after the eighth read, before its write; sixteen elements starting at 9 also complete seven iterations at 31. Add bounded worker batches if needed rather than raising the one-MiB artifact cap.

## Implementation acceptance

1. Exact fifteen-step example with both update forms; six static sites, RHS-only read highlights and whole-assignment write highlights; independent forward/backward states and no duplicate evaluations.
2. Zero/one/multiple iterations, signed addends including zero, repeated values, int wraparound, negative and high initial indices, seven/eight/sixteen-element limit boundaries, and event budgets 1–15.
3. Original/generated Docker comparisons for output, final values and exception type/message; limited runs use independently expected prefixes and guard-placement checks.
4. Read/store binding/type/source handoff checks, stale/swapped/missing facts, formatting/Unicode/collision cases and rejection of excluded expression shapes.
5. Collector rejection of skipped/duplicate/reordered reads, stale values/indices, wrong arithmetic results, write-before-read and events after retirement; truncated records preserve whole-event prefixes. Draft-3 read fixtures and replay tests must pass without changing old schemas/fixtures.
6. Retain all 218 earlier automatic cases/164 comparisons, seven manual cases/four comparisons, existing contract/collector/worker/delivery checks and cleanup. The original design-only review ran none of those runtime tests; implementation verification is tracked separately.

User confirmed this two-step body rule and natural two-statement source shape on 2026-10-01. Frontend/API integration remains separate required work; completing this extension will not itself create a usable app UI.
