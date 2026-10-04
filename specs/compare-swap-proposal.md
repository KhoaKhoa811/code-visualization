# Conditional array compare-and-swap tracing

Status: design and contract approved, with runtime implementation approved after PR #8 merged. The bounded analyzer, instrumentation, recorder, source-plan collector and replay are implemented; current verification is recorded in the [runtime guide](../runner/analysis/CONDITIONAL_RECORDING.md). See also [contract status](../contracts/README.md). Milestone 1 remains incomplete. Proposed wording below records the design that led to these increments.

## Purpose and bounded source

Establish array comparisons, conditional execution and a temporary binding before composing sorting loops. This is one conditional swap, not a sorting algorithm or general if/else support.

```java
public class Main {
    public static void main(String[] args) {
        int[] values = {3, 1};
        if (values[0] > values[1]) {
            int temp = values[0];
            values[0] = values[1];
            values[1] = temp;
        }
    }
}
```

Proposed eligibility: one literal int-array declaration followed by one braced if without else; its condition compares two accesses of that array using `>`; its body contains exactly the three swap statements above. Allow renamed bindings, comments/formatting, array lengths 0–16, existing signed decimal int element/index literals, and any two literal indices L/R. Require the same resolved array binding and consistent L/R at all sites. Equal indices are allowed and yield false after two reads. Do not reject out-of-bounds indices as syntax errors: they are runtime-error acceptance cases.

Optional development output observations use the exact existing `System.out.print("FINAL=" + java.util.Arrays.toString(values));` and `System.err.print("PROBE");` forms after the if, with resolved JDK calls and the renamed array binding. These observe final state; they do not establish general output-call tracing.

Exclude variable/arithmetic indices, aliases, null initializers, other operators, else, nested conditions/loops, additional statements, calls in expressions, embedded increments and methods/recursion from this increment. These are tracing limitations, not compiler-invalid Java. Preserve the approved admitted output-only policy; this proposal does not implement general execution admission or fallback. Broader V1 requirements remain intact.

## Proposed observable steps

Every successful array read is its own step. Comparison and branch decision share one CONDITION step carrying both captured operands and the actual boolean. Do not add a second branch click or an atomic SWAP event. Scalar reads remain grouped with their consuming declaration/write, consistent with existing scalar steps.

| Step | Event and original highlight | Array after step | temp after step |
| --- | --- | --- | --- |
| 0 | No event/highlight | Absent | Absent |
| 1 | ARRAY_DECLARE: `int[] values = {3, 1}` | [3,1] | Absent |
| 2 | ARRAY_READ: left condition `values[0]`, value 3 | [3,1] | Absent |
| 3 | ARRAY_READ: right condition `values[1]`, value 1 | [3,1] | Absent |
| 4 | CONDITION: `values[0] > values[1]`, operands 3/1, true | [3,1] | Absent |
| 5 | ARRAY_READ: initializer `values[0]`, value 3 | [3,1] | Absent |
| 6 | VARIABLE_DECLARE: `int temp = values[0]` | [3,1] | 3 |
| 7 | ARRAY_READ: RHS `values[1]`, value 1 | [3,1] | 3 |
| 8 | ARRAY_WRITE: `values[0] = values[1]`, stored 1 | [1,1] | 3 |
| 9 | ARRAY_WRITE: `values[1] = temp`, stored 3; retire temp | [1,3] | Absent |

With [1,3] or equal values, stop normally after step 4 with false; no temp exists and no body events occur. The array remains available in the terminal playback snapshot. Main teardown is not added as a new click. Branch-local temp retirement is attached atomically to the last successful body write because that statement is immediately followed by branch exit in this bounded shape. It is not a general rule that every array write exits a scope.

Backward from step 9 restores [1,1] and temp=3. Backward from step 6 removes temp and selects its initializer read. Backward from step 4 selects the right operand read. Selected read/comparison details are transient playback attention, not invented program variables. Layout coordinates remain separate from recorded facts.

Use existing original-source identity, UTF-16 positions and exclusive ends. Each of the nine operations has its own static site, including separate sites for textually identical accesses. Highlight reads only at their access expressions, CONDITION at the binary expression excluding if/braces, declarations excluding the semicolon, and writes at the whole assignment excluding the semicolon. Never match sites by expression text alone.

## Proposed draft-4 contract

Draft-3's closed union cannot carry comparison operands; its validator also treats every CONDITION as a loop decision and permits retirement only on false. Do not loosen or reinterpret draft-3. Introduce a separately selected draft-4 for this new producer, retaining draft-1/2/3 schemas, fixtures, validation behavior and producer behavior. The original documentation task created no schema; the subsequent contract task added [draft-4](../contracts/run-result-v4.schema.json).

Retain the result envelope, one event per observable step, typed int/boolean values, source identity and safe-boundary event counts. Proposed draft-4 payload rules:

- Every event has `scopeId` for the lexical scope of its operation and `exitedVariableIds`. Declaration IDs and array-object IDs remain distinct. Example identities: array binding variable-1 in scope-main, array-1, temp variable-2 in scope-if-1. IDs are plan data, not names guessed by a renderer.
- CONDITION has `conditionRole: "IF"`, typed boolean `value`, and a `comparison` object with `operator: ">"`, typed-int `left`/`right`, and `leftReadSequence`/`rightReadSequence` referencing its two immediately preceding operand reads. It has scope-main, no target variable/array ID, and an empty exit list for either result. This first draft-4 producer does not migrate existing loop conditions.
- ARRAY_READ/ARRAY_WRITE retain arrayId, evaluated index and typed captured value. VARIABLE_DECLARE retains binding identity/name and its captured initializer value. ARRAY_DECLARE retains existing allocation/binding facts.
- Exit lists are empty except the final swap write, which carries scope-if-1 and exactly temp's ID. Validate that the binding is live and belongs to that scope; apply the committed write and retirement atomically. Missing, duplicate, foreign or repeated retirement is invalid. General nested-scope teardown is not defined here.

The general validator checks field types, references, source identity, contiguous order, int bounds, read values against recorded array state, operand links/values, comparison consistency, binding lifetimes and atomic exits. The source-specific collector plan additionally enforces the nine-operation true path or four-operation false path, exact static sites, swap dependencies and the only permitted exit point. Neither may repair inconsistent records. Replay consumes captured boolean/value fields; it does not evaluate Java or decide whether to run the branch. Validation may check consistency without manufacturing facts.

Unknown versions must fail explicitly. A new machine-readable schema, illustrative fixtures, negative mutations and forward/backward prefix checks must pass before a draft-4 Java producer is implemented.

## Analysis, lowering and collection boundaries

Reuse the existing SourceSnapshot and analysis/instrumentation separation. Analysis supplies immutable resolved array/temp bindings, operand and store sites, literal indices, int/boolean types, branch scope and exact source hash. Add a dedicated conditional-plan variant rather than encoding it as the existing LoopProbe plan. Preserve the original AST and validate frozen facts against the original bytes inside the bounded analysis worker. Instrumentation works on a separate syntax representation; it does not introduce a competing resolver.

Capture each operand access once, compare those captured values once, record the result, and branch on that same result. The later temp initializer is a distinct original read and must execute again when the branch is taken. It must not be replaced with an earlier comparison operand merely because this example would yield the same value. Capture the swap RHS once and reuse it in the store. No extra array reads are allowed to obtain record values.

Keep budget guards before the relevant operation, including before evaluation of helper arguments. Preserve target-reference/index evaluation before RHS work for simple assignment; keep native access checks and exception behavior. The final write and its exit metadata must be validated/committed as one record. If trace emission fails after a Java mutation, retain only the earlier safe prefix and report incomplete visualization; do not invent the missing write or normal exit.

Reuse bounded FIFO transport and existing runner isolation. Add a conditional phase plan for collection and a version-aware replay path; do not retrofit false-if semantics into the old loop plan. Do not infer event values from syntax or precompute branches. Helper identities must avoid user-name collisions. These are responsibility boundaries, not authorization to launch specialist agents.

Java references reviewed for this proposal: operands evaluate left to right; a failed left operand prevents the right one; numerical `>` yields a boolean; simple array assignment has defined target/RHS evaluation and check order. See [JLS 15.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7), [15.10.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10.4), [15.20.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.1) and [15.26.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.1). The body executes only after a true condition; see [JLS 14.9.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.9.1). These references guide proposed lowering; runtime equivalence is not yet verified.

## Failure prefixes, limits and acceptance

Keep current source, 16-element array, 32-event, trace-byte, management, Docker and artifact limits. A true branch needs nine events and false needs four, so no limit increase is needed. Test budgets 1–9: a guard stops before the next operation, and exactly enough budget allows normal completion. Failed left access retains only the declaration; failed right access retains declaration plus left read. Neither emits a CONDITION or enters the body. A byte/corruption/transport failure retains only the last fully validated prefix. Never fabricate an exit during failure/unwinding.

Future implementation acceptance cases:

1. [3,1] true path, [1,3] false path, equal values, same index, negative values, int extremes, reversed/nonadjacent indices and arrays up to 16 elements. Compare actual recorded operands, result, intermediate array state and temp lifetime.
2. Empty/one-element arrays and negative/high indices: left failure prevents the right read; right failure preserves the left read. Compare original/generated exception type/message and final observable output through Docker, not synthetic success fixtures.
3. All event budgets and all forward/backward prefix positions, especially before comparison, after initializer read, between stores and across temp retirement. Limited runs use independently expected prefixes rather than expecting unrestricted original output.
4. Source rename/format/CRLF/tab/supplementary-Unicode cases, duplicate access text, helper-name collisions, stale/swapped/missing facts, unresolved bindings, wrong types and excluded syntax. Locations must identify the exact original site.
5. Contract/collector rejection: reversed or stale operand links, invented operands, non-boolean result, wrong comparison result, body after false, missing body on declared complete true, write before its read, stale temp value, bad exits, use after retirement and malformed/truncated final records. Each rejected event leaves reconstructed state unchanged.
6. Preserve existing versioned contract checks, automatic transformation comparisons and runner/manual recording regression gates. Add bounded fixture batches rather than growing current caps. No browser/sorting completion claim follows from this increment.

## Recommended delivery and approval points

First review/approve the nine-step semantics, false branch and temp retirement, and the proposed draft-4 fields. Then implement and verify the contract in a separate approved task. Next implement the bounded analyzer/lowering/recorder/collector/replay path and semantic comparisons. Nested sorting loops, aliasing, remaining Milestone 1 coverage and interactive worker behavior remain later increments before full application integration. Each task follows the existing memory/commit/push/PR workflow.

Original proposal verification was documentation-only. Subsequent contract verification: nine designed fixtures, 54 rejection cases, nine partial prefixes, atomic rejection, int extremes, exact source/hash checks and forward/backward fixture states passed alongside earlier suites. No Java runtime acceptance case or production playback implementation is delivered yet. Source-plan branch/swap ordering, exact exit placement and runtime capture remain the future collector/producer's responsibility.
