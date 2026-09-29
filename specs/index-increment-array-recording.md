# Standalone index increments before an array write

Status: specification approved on 2026-09-26; implementation and Docker verification completed on 2026-09-28 for both standalone postfix `i++` and prefix `++i`. This extends [index addition](index-addition-array-recording.md); all seven verified program shapes remain supported.

## 1. Exact scope

Use the existing Main.java / public Main.main(String[]) convention and five recorded statements:

```java
int x = 8;
int[] values = {5, 2};
int i = 0;
i++;
values[i] = x;
```

The alternative replaces only `i++;` with `++i;`. Each program contains exactly one standalone increment, not both. Its operand must be a plain name resolving to the previously declared non-final local int index. Preserve existing declaration order, literal initializers, array size of zero to 16 elements and optional exact final stdout/stderr probes. Names, allowed initializer literals, comments, Unicode identifiers, tabs, line endings and multiline formatting may vary.

Only the fourth statement gains these two forms. This does not add increments to other program shapes, expressions, declarations, conditions or loop headers. Reject tracing for `x++`, `++x`, undeclared operands, fields, array-element increments, boxed/long operands, decrements, multiple increments and parenthesized operands. Keep existing literal assignments and index additions supported through their current paths.

Embedded forms remain outside this increment, including `x = i++`, `x = ++i`, `i = i++`, `values[i++] = x`, `values[++i] = x`, method arguments and arithmetic containing increments. These require preservation of the expression's consumed value and their own evaluation/step rules. They are tracing limitations where otherwise valid Java; ordinary compilation and execution-admission policy remain separate.

## 2. Semantics and instrumentation

Both forms add one to i and store the result. Postfix yields the previous value as its expression result; prefix yields the updated value. Standalone expression statements discard that result, so both forms leave the same variable state. Follow Java SE 21 [JLS 15.14.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.14.2) and [15.15.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.15.1).

Preserve Java int wraparound: initial 2147483647 becomes -2147483648; initial -2147483648 becomes -2147483647. Overflow does not become a checked arithmetic error. The later array store may fail because its updated index is invalid.

The generated sequence must guard before mutation, execute the original standalone increment exactly once, then record the committed variable value. Illustrative instrumentation order, using the existing helper interfaces:

```java
helper.beforeVariableWrite();
i++; // Or the original ++i; preserve its form.
helper.variableWrite(i, originalRange, "variable-3");
```

Never pass `i++` as the recorder's value argument: that would record the old expression result. Do not pass `++i` either; keep the source operation separate from recording and keep the guard before it. Do not replace the increment with a constant or execute another increment to obtain its value.

## 3. Events, highlighting and replay

Both forms produce the same five event kinds, with their own original source ranges:

| Cursor | Event and original highlight | x | i | values |
| --- | --- | --- | --- | --- |
| 0 | None | Absent | Absent | Absent |
| 1 | VARIABLE_DECLARE: `int x = 8` | 8 | Absent | Absent |
| 2 | ARRAY_DECLARE: `int[] values = {5, 2}` | 8 | Absent | [5, 2] |
| 3 | VARIABLE_DECLARE: `int i = 0` | 8 | 0 | [5, 2] |
| 4 | VARIABLE_WRITE: `i++` or `++i` | 8 | 1 | [5, 2] |
| 5 | ARRAY_WRITE: `values[i] = x` | 8 | 1 | [5, 8] |

One click records the increment's committed change. There is no separate scalar-read, arithmetic or discarded-expression-result step. Highlight the whole original unary expression, including `++` in its original position and excluding the semicolon. The source operation is an increment, not an invented assignment highlight.

Reuse draft-2: variable-1 is x, variable-2 is the array binding, variable-3 is i and array-1 is the object. The write targets variable-3; x remains unchanged. Keep five unique original/generated operation associations, source hashes and the existing UTF-16, one-based, exclusive-end range convention. Prefix/postfix distinction belongs in analysis/source facts, not a new trace field.

Backward from cursor 5 restores the array while retaining i=1. Cursor 3 restores i=0, then earlier cursors remove the index, array and value declarations independently. Replay consumes recorded values; it never executes an increment. These standalone rules are consistent with the existing loop-update stepping direction but do not implement loops or generalize embedded increment behavior.

## 4. Failures, limits and extensibility

A failed array store retains four events, including the committed index update, and emits no ARRAY_WRITE. Compare original/generated output and exception type/message under the existing policy. An initially invalid index of -1 becomes valid zero and can succeed. Empty arrays and an index advanced past the last element must fail safely.

Budgets 1–4 stop before array initialization, index initialization, increment mutation and array store respectively. Exactly 5 permits completion. Verify generated guard placement as well as trace prefixes. Preserve existing cancellation, timeout, partial-trace, terminal-status and cleanup behavior.

Extend shared analysis with explicit immutable increment facts: prefix/postfix operator, whole-expression and operand ranges, resolved operand binding and int type. The current CombinedProbe and transformer assume an assignment node at this position; extend that boundary to distinguish assignment and standalone increment without weakening their existing checks. Increment and addition facts must not ambiguously describe the same operation.

Transform only the source-matched syntax copy. Verify complete, consistent increment facts before retaining the unary statement and inserting the existing guard/capture calls. Reject stale sources, missing bindings, swapped ranges/operators and unsupported operands. Preserve helper-name collision handling, actual final subscript/RHS names and the original AST.

Reuse recorder, FIFO, five-operation collector plan, schema and reconstruction. The collector checks the final array index against captured i and stored value against x; it is not an increment evaluator. No new dependency, runtime service, API, renderer or trace version is proposed.

Keep current source/trace/event/array/diagnostic and Docker limits. Retain all 110 previous fixtures and bounded fresh delivery. Current batches are 998854 and 269639 bytes, each capped at 1 MiB; place new cases in batches that fit, updating expected groups and aggregate allowlists consistently. Do not increase caps, consume stale batches or drop prior cases. Preserve duplicate/missing-case checks and aggregate comparison totals.

## 5. Acceptance cases

1. Run the example separately with postfix and prefix. Each must record five events, capture i=1 at step 4 and produce [5,8], matching the original program's stdout/stderr. Explicitly assert that postfix capture is the updated value, not the old expression result.
2. For both forms, vary initial values and names, equal x/i values, repeated array values, first/last valid positions and one/16-element arrays. Initial -1 must become zero and succeed where valid.
3. For both forms, initial 2147483647 wraps to -2147483648 and initial -2147483648 becomes -2147483647. Verify the captured update and four-event failure prefix. No fabricated successful store or arithmetic exception is allowed.
4. Check empty arrays, invalid negative/too-large computed indices and advancing off the last element. Compare original/generated exception behavior and console output. Test budgets 1–5 separately for both operators.
5. Verify renamed/Unicode operands, comments, tabs, CRLF/multiline source, helper collisions and absent optional probes. Prefix/postfix highlights must retain their actual operator placement and exact original/generated associations.
6. Resolve the operand to the index binding and int type. Reject wrong/unknown bindings, fields, array-element increments, unsupported types, final variables, decrements, parenthesized operands, duplicate statements, embedded forms and side-effecting combinations with bounded diagnostics. A supported standalone form must not make an embedded occurrence eligible accidentally.
7. Inject stale/missing/swapped increment facts, incorrect operator/type/binding and inconsistent assignment/addition/increment facts. Confirm rejection, original preservation, exactly one generated increment, guard before mutation and capture afterward with a plain variable argument.
8. Validate draft-2 results, all six successful cursor states and safe prefixes. Compare prefix/postfix event kinds and values independently of their different source hashes/ranges; backward stepping must leave x unchanged. Preserve collector rejection of wrong identities/sites/order, stale final indices and premature end markers.
9. Keep the 110-case/92-comparison baseline, existing analyzer/transformation/collector/contract checks, batch-delivery negatives, both worker-limit probes and seven manual cases/four comparisons. Previous standalone-increment rejection fixtures that now fit this exact shape must become explicit positive cases; retain adjacent unsupported-form coverage and all earlier runtime fixtures.
10. Verify fresh bounded batch delivery and removal of all owned containers. Record actual new case/comparison totals and full gate outcome only after execution. Preserve the saved-log recovery workflow; do not count interrupted runs as complete.

## 6. Verification status

Verified 2026-09-28: test-recording.ps1 exited 0. All 150 automatic cases/124 comparisons, 150 result/replay checks, 20 prefix/postfix fact comparisons, analyzer/transformation/collector/contract checks, eight batch-delivery negatives, both worker-limit probes and seven manual cases/four comparisons/seven result checks passed. All 110 prior runtime fixtures remain. Both operators passed 30 rejected-form checks and immutable-fact/guard/single-evaluation assertions. Every owned test container was verified removed. Two fresh batches contain 88/62 cases, sized 998854/759159 bytes, under unchanged caps. See [the implementation guide](../runner/analysis/INDEX_INCREMENT_RECORDING.md) and memory for evidence. Milestone 1 and browser playback remain incomplete.
