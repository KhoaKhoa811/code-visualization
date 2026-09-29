# Index addition before an array write

Status: implemented and Docker-verified on 2026-09-26 after approval of the scope, implementation and bounded fixture batches. This extends [literal index updates](index-update-array-recording.md); all six earlier verified program shapes remain supported. See the [implementation guide](../runner/analysis/INDEX_ADDITION_RECORDING.md).

## 1. Supported expression and program shape

Keep the existing Main.java / public Main.main(String[]) entry convention and five recorded statements:

```java
int x = 8;
int[] values = {5, 2};
int i = 0;
i = i + 1;
values[i] = x;
```

The only new expression form is `index = index + literal`. The assignment target and left operand must resolve to the same previously declared local int index. The right operand uses the existing decimal-int literal policy: zero or decimal digits without leading zeros, optionally preceded by unary minus, within -2147483648 through 2147483647. Thus `i = i + 0`, `i = i + 2` and `i = i + -1` use the same supported form. Unary plus, suffixes, underscores, alternate bases and parenthesized wrappers are outside this increment. These restrictions define tracing eligibility, not Java language validity.

Resolve the addition's operands and result as int. Do not accept `i = x + 1`, `x = i + 1`, `i = 1 + i`, other binary operators, chained additions, casts, calls, boxed/long/string operands, compound assignments, increments or embedded side effects. This does not add arithmetic to declarations, array subscripts or other existing shapes. The final store still has the form `values[i] = x`.

Retain the previous declaration order, non-final local declarations, literal initializers, zero-to-16-element array limit, source restrictions and optional exact final stdout/stderr probes. Names, allowed literals, comments, Unicode identifiers, tabs, CRLF and multiline formatting may vary. No extra statements, aliases, nested scopes, loops or methods are added. Later V1 requirements remain unchanged.

## 2. Java semantics and recording

Java evaluates int addition with 32-bit signed arithmetic. Overflow wraps; it does not throw an arithmetic exception. For example, 2147483647 + 1 becomes -2147483648, and -2147483648 + -1 becomes 2147483647. Preserve Java's operand order and execute the original expression once. These rules follow [Java SE 21 JLS 15.18.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.2) and [15.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7).

Keep the real assignment in the generated program. Check the event budget before evaluating its RHS or mutating i; afterward capture the committed i value. Do not calculate a replacement from the initializer, evaluate the RHS again for recording, widen the submitted expression to long, clamp overflow or use checked addition. Runtime events remain authoritative.

The assignment is one observable operation. Reading i and adding the literal produce no separate read/arithmetic event in this increment. Highlight the entire original assignment expression, excluding its semicolon. This is a deliberately bounded stepping rule, not a decision about arbitrary nested expressions or future comparison events.

| Cursor | Event and highlight | x | i | values |
| --- | --- | --- | --- | --- |
| 0 | None | Absent | Absent | Absent |
| 1 | VARIABLE_DECLARE: `int x = 8` | 8 | Absent | Absent |
| 2 | ARRAY_DECLARE: `int[] values = {5, 2}` | 8 | Absent | [5, 2] |
| 3 | VARIABLE_DECLARE: `int i = 0` | 8 | 0 | [5, 2] |
| 4 | VARIABLE_WRITE: `i = i + 1` | 8 | 1 | [5, 2] |
| 5 | ARRAY_WRITE: `values[i] = x` | 8 | 1 | [5, 8] |

Reuse draft-2, variable-1 for x, variable-2 for the array binding, variable-3 for i and array-1 for the array object. Even adding zero records a committed assignment. Five original/generated operation associations remain tied to source hashes and existing UTF-16, one-based, exclusive-end ranges. Additional analysis spans for the RHS and operand are facts, not extra playback steps.

Backward from cursor 5 restores [5,2]; cursor 3 restores i=0. Earlier cursors remove the declarations independently. Replay uses recorded values, never recalculates Java arithmetic and never reruns the program.

## 3. Failures and resource bounds

The addition may produce an invalid array index. Record that updated int value at step 4, then retain exactly four events when the store throws. Emit no successful ARRAY_WRITE. Distinguish overflow during addition from the later array-bounds exception; preserve original/generated exception type and message under the existing comparison policy. An initially invalid index that becomes valid must succeed.

Budgets 1, 2, 3 and 4 stop before array initialization, index initialization, the arithmetic assignment and the array store respectively. Budget 5 permits completion. Check generated guard placement before the actual RHS, not only trace lengths. Existing cancellation, timeout, transport, terminal-status and cleanup rules remain in force.

Retain existing source/trace/event/array/diagnostic and Docker limits. The last verified fixture bundle is 998854 bytes against a 1048576-byte cap. Preserve every earlier case. If the expanded gate exceeds the cap, generate and consume bounded fixture batches; keep each worker output and bundle bounded, with fresh artifacts, fixed fixture-name allowlists, duplicate/missing-case rejection across batches and aggregate comparison totals. Do not raise the cap or silently truncate/drop cases. This is test-driver delivery work, not a new execution service or protocol feature.

## 4. Existing implementation boundaries

- Analysis: extend CombinedProbe's assignment eligibility through explicit resolved addition facts. Record the operator, operand/reference range, referenced index binding and int result type in the immutable, source-matched handoff. Keep the current literal-assignment path intact; do not weaken intLiteral globally to admit arbitrary expressions.
- Transformation: validate the addition facts against the syntax copy and original source. Preserve the actual RHS and guard-before-assignment/record-after-assignment order. Reject missing, wrong-binding, wrong-operator or stale facts. Reuse the existing five source associations and helper-collision handling.
- Recorder and collector: reuse the binding-aware variableWrite and five-operation plan. Capture actual i, then check the final array index against its latest recorded value and the stored value against x. The collector is not a Java expression evaluator; arithmetic correctness is established by semantic transformation checks and original/generated execution comparisons.
- Contracts and replay: existing int values and VARIABLE_WRITE represent wrapped results. Verify all six states and independent restoration. No operand-display fields, new schema version, dependency, API or renderer are needed.

This preserves analysis, instrumentation, collection and rendering boundaries without building a general arithmetic interpreter. Broader expressions require their own verified semantics.

## 5. Acceptance cases

1. The example produces five events and [5,8], with unchanged stdout/stderr. Confirm the RHS remains an addition using the resolved index, not an inserted constant.
2. Vary initial i and the right literal independently: positive steps, zero, negative steps, first/last valid positions, and initially invalid indices becoming valid. Cover unchanged assignments and distinct/equal x/i values.
3. Explicit overflow cases: initial 2147483647 plus 1 records -2147483648 before a failing store; initial -2147483648 plus -1 records 2147483647 before a failing store; initial -2147483648 plus -2147483648 records 0 and succeeds for a nonempty array. Use decimal literals in source, not unapproved field references. Verify the recorded wrapped value, not only the exception.
4. Empty arrays and negative/out-of-range computed indices retain four events. Compare original/generated exceptions and available console output. Test budgets 1–5 and guards before evaluation/mutation.
5. Renamed bindings, comments, Unicode/CRLF/tabs/multiline addition, helper collisions, missing optional probes and one/16-element arrays preserve values and exact original/generated source associations.
6. Resolve assignment target and RHS reference to the same index binding and int type. Reject unresolved names, use of x as the operand/target, alternate operand order/operators, multiple additions, parenthesized forms, overflowed literals, widening, string addition, calls and side effects with bounded diagnostics. Valid but unsupported forms remain tracing limitations.
7. Reject stale source and missing/swapped operand, binding, type, operator or source facts. Verify the original AST/source is unchanged and the generated copy contains one RHS evaluation and a post-commit capture.
8. Preserve collector rejection of wrong binding/source/order, stale final index, wrong stored x value, invalid typed payloads and premature completion. Add wrapped-int positive coverage without teaching the collector to calculate expressions.
9. Validate contracts, hashes, five source associations and all six forward/backward states. The fourth highlight covers the entire original addition assignment. No sixth arithmetic step or fabricated operand visualization appears.
10. Retain the 88-case/74-comparison baseline, earlier analyzer/transformation/collector/contract checks, both worker-limit probes and seven manual cases/four comparisons. Update historical arithmetic rejection fixtures only when they become explicit supported positive cases; retain neighboring rejection coverage. Verify bounded batch delivery if needed, enforce source/output caps and confirm all owned containers are removed.

## 6. Verification status

Fresh verification on 2026-09-26: `runner/analysis/test-recording.ps1` exited 0. Passed 110 automatic runtime cases (88 earlier and 22 addition), 92 original/generated comparisons and all 110 contract/hash/source/forward-backward result checks. Eighteen intentionally limited cases use prefix assertions instead of unlimited-run equivalence. Wrapped negative/positive extremes retain four events before failed stores; wrapped zero succeeds. Budgets 1–4 preserve their expected prefixes and exactly 5 completes.

The original 18 analyzer checks and all earlier transformation checks passed. New analysis checks cover resolved int/binding/operator/operand spans, preservation, stale/missing/swapped facts and 30 rejected forms. Shared generation checks verify guards, preservation of the actual RHS and one addition evaluation. Existing collector/contract regressions and new wrapped-value positives passed, as did both worker-limit probes and all four analysis-worker cleanup checks.

Fresh fixture batches contain 88 and 22 cases, sized 998854 and 269639 bytes respectively, below their unchanged 1-MiB limits. Batch-delivery tests passed valid delivery and eight negative cases. The complete fixed allowlist is validated before execution; duplicates and missing cases across batches fail. Seven manual runtime cases/four comparisons and seven result checks passed. All test-owned containers were verified removed. Separate output-only and simulated Docker-classification suites were not rerun because runtime orchestration was unchanged.

An earlier run was interrupted without a recoverable final status. On recovery, Docker was restarted and one stopped test container was ownership-verified, removed and checked absent before the fresh gate. The earlier partial artifacts are not counted as completed verification. The fresh transcript is `.results/index-addition-gate-d9835c4863324bb99294089f88f1c8e6.log` under runner/analysis; its fixture directory is `.results/fixtures-88e0aac04c95468a871724e3d9ea3f37`.

No dependency, recorder, trace schema, API or runner isolation policy changed. Milestone 1 remains incomplete; browser playback does not exist. Broader arithmetic, expression indices, increments and loops require a later confirmed scope.
