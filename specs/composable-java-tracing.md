# Review: tracing supported Java constructs

Status: direction adopted through merged PR #11, confirmed during the 2026-10-05 documentation review. The user wants differently written supported Java visualized without recognizing a named algorithm. The implementation increments and contract choices below remain proposals requiring separate approval. No composable analyzer, transformer, schema or limit change is implemented by this review.

## Conclusion

Treat algorithms as acceptance programs, not eligibility categories. Analyze and instrument supported declarations, expressions and statements wherever their approved composition is safe. Bubble sort, quicksort and an unnamed custom program should use the same language machinery when their constructs are supported. This does not promise arbitrary Java, every equivalent rewrite, or immediate quicksort support: recursive quicksort also needs methods, frames, arguments, returns and alias-safe array references.

The existing requirements already separate ordinary Java authoring from tracing coverage. Template prototypes established individual semantic behavior, but extending one exact recognizer per algorithm would scale poorly and reject harmless structural changes. Preserve that verified evidence while changing the future implementation direction. [ADR 0011](decisions/0011-composable-java-tracing.md) records the recommendation.

## Findings from the implementation

Reviewed baseline: merged main c649980, including PR #9 implementation and PR #10 design.

| Evidence | Current limitation | Reuse or adjustment |
| --- | --- | --- |
| [ArrayAnalyzer.eligibleSites](../runner/analysis/src/main/java/dev/codeviz/analysis/ArrayAnalyzer.java) | Chooses probes by statement count and fixed positions; result Sites holds shape-specific fields | Retain source/binding/type facts; add a separate recursive capability result, not more algorithm enum values |
| [LoopProbe](../runner/analysis/src/main/java/dev/codeviz/analysis/LoopProbe.java), [ConditionalProbe](../runner/analysis/src/main/java/dev/codeviz/analysis/ConditionalProbe.java) | One reviewed loop body or three-statement literal-index swap | Retain as legacy paths and regression examples; new eligibility checks node kinds, resolved types and valid composition |
| [ArrayTransformer](../runner/analysis/src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java), [ConditionalTransformer](../runner/analysis/src/main/java/dev/codeviz/instrumentation/ConditionalTransformer.java) | Dispatch/lowering follows fixed fact variants and ordered sites | Reuse safe mapping/capture patterns; introduce statement/expression lowering under an explicit new capability path |
| [LoopTracePlan](../runner/prototype/LoopTracePlan.java), [ConditionalTracePlan](../runner/prototype/ConditionalTracePlan.java) | Fixed phases/identities, one loop or swap; not a general source plan | Retain validation boundary; eventually build structured plans from source nodes, with generic sequence/branch/loop traversal |
| [Draft-4 semantic validation](../contracts/conditional-semantics.mjs) | Requires two immediately preceding reads; permits one branch-local int retired on a write; scalar writes require the binding's own scope | Revisit dependencies, multiple locals, writes to enclosing bindings and general scope exits before freezing the next schema |
| [Conditional replay](../runner/prototype/recording/conditional-replay.mjs) | Applies recorded event kinds rather than detecting algorithms, but only understands draft-4 | Preserve the operation-driven approach; version/extend reducers alongside new contracts |

The original AST is already richer than the supported tracing shapes. A parser replacement is not the demonstrated need. No new parser dependency, AST-to-trace interpreter, AI algorithm detector or special BubbleSort/QuickSort engine is proposed. Java remains responsible for execution; static facts guide instrumentation, not predicted runtime events.

## Intended pipeline and ownership

1. **Analyze original source.** Preserve exact source identity/ranges, AST, resolved declarations/types and lexical scopes. Produce located syntax, resolution and capability diagnostics separately. Parsing/resolution success does not establish Java compiler acceptance or execution admission.
2. **Check capabilities recursively.** Each statement/expression is supported only if its own form, types, children and context are supported. Inspect every branch, including unreachable-looking code; do not assume a runtime path makes unknown effects safe. Bounded analysis traversal and diagnostic limits still apply.
3. **Build a small typed source plan.** Represent source nodes and dependencies, not algorithms: a sequence of declarations/assignments initially; then structured blocks, branches and loops. Preserve static declaration/site IDs, lexical parents, expression result types, lvalue receiver/index structure and evaluation dependencies. No source-position guessing or precomputed iteration counts.
4. **Lower supported operations.** Statement handlers compose expression handlers. Keep the original Java operation or an equivalent tested lowering, capture each evaluated value once, and map observable operations to original expressions. Do not turn the plan into a Java interpreter.
5. **Execute and collect.** Reuse isolated compilation/execution and separate bounded trace transport. A source-plan validator checks allowed operation order, identities and recorded dependencies. It follows recorded branch decisions and verifies consistency; it does not manufacture missing operations or prove runtime provenance merely from a valid JSON result.
6. **Reconstruct and render.** Consume versioned accepted facts for forward/backward playback. Rendering depends on value/structure types, never a recognized algorithm name. Source edits invalidate association with old traces as before.

The analysis engineer owns original semantic facts and capability diagnostics; the instrumentation/trace engineer owns lowering and capture. Execution management and replay retain their existing boundaries. The role documents already describe these responsibilities; no role registration or delegation changes are needed.

## Small first implementation proposal: composable analysis only

Recommend this as the next separately approved task, ahead of new runtime or draft-5 work. Add a new immutable analysis result alongside existing probes. Do not route submitted programs to a new recorder yet.

Retain the reviewed Main.java/Main.main entry setup for this first increment. Within its main body, propose any bounded-length sequence of these supported constructs, rather than an exact number/order of statements:

| Construct | Initial analysis coverage |
| --- | --- |
| Local int declaration | One explicitly typed, initialized local per statement; initializer is a supported int expression |
| Local int-array declaration | One fresh int[] literal with existing signed decimal int elements and 0–16 element bound |
| Assignment statement | Plain `=` to a resolved local int or supported array element, with a supported int RHS |
| Int expressions | Existing signed decimal int literals, resolved local int references, parentheses, int `+`/`-`, array length and array element reads |
| Array element | Receiver is a resolved supported local int[]; index is a supported int expression, including `j + 1` or another array read |

Allow multiple independently allocated arrays and int locals, renamed bindings, independent extra declarations and different valid statement arrangements. Do not require a special trailing output probe. Keep source-size, AST/node, diagnostic and array bounds; define/check recursion depth for the recursive analysis before implementation rather than relying on the host stack. The result states completeness for this capability set, not whole-Java support or trace availability.

Example main-body fragments for the same proposed analyzer:

```java
int[] a = {3, 1};
int j = 0;
int temp = a[j];
a[j] = a[j + 1];
a[j + 1] = temp;
```

```java
int j = 0;
int padding = 7;
int[] data = {3, 1};
int saved = data[(j)];
data[j] = data[(j + 1)];
data[j + 1] = saved;
padding = padding + 1;
```

Both should be analyzable by the same construct handlers; neither requires a swap recognizer. The second has additional behavior and must record that behavior when runtime support later exists. Coverage is about supported operations, not proving two programs equivalent.

Initially diagnose nested blocks, if/else, loops, uninitialized or multi-declarator locals, calls, reference assignments/aliases, other types/operators and embedded side effects as not yet covered by this new path. Existing legacy paths retain their previously verified scope. Do not confuse a limitation in the new analysis path with a regression in old tracing support. Top-level forms beyond the reviewed entry setup remain separate coverage work; do not claim an ordinary valid import is a Java syntax error.

Analysis acceptance: resolve every reference/type and nested expression; distinguish multiple declaration identities; preserve exact ranges and original bytes/AST; detect missing/stale facts; accept unrelated extra supported statements; reject unsupported descendants with their location/reason instead of silently skipping them. Test differently ordered/renamed/formatted programs, repeated access text, multiple arrays, boundary values and analysis limits in Docker. No runtime values, object IDs or invocation instances may be fabricated by this static result.

## Following increments, not authorized implementation

| Increment | Capability outcome and necessary proof |
| --- | --- |
| Straight-line lowering and runtime | Instrument the analyzed sequences and nested expressions; settle captured-value dependencies, allocation/binding identities, exact read/write stepping and compatible trace version first |
| Blocks and conditions | Compose statements in nested blocks and both branches; allow reads/writes of accessible enclosing locals, variable-index comparisons and repeated locals; specify normal/empty-block exits and partial traces |
| Classic loops | Reuse the same statement body machinery for loops and nested loops; specify headers/updates and runtime activations, with fresh bindings on reentry |
| Other required control flow | Add while/enhanced-for and supported break/continue according to existing V1 stepping rules; preserve short-circuit/exception semantics when adding expressions |
| Methods, references and recursion | Specify frames and call/return semantics early; implement argument/return evaluation, recursive activations, alias/object identity and failure/limits before tree/heap sorting |

Bubble sort becomes a composition acceptance case once the necessary loop/condition/array constructs are covered. Rewrites using while, an early-exit flag, helper methods, or recursion become eligible only when those constructs and their interactions are implemented. Quicksort is a later acceptance case for the necessary method/control-flow/reference capabilities, not a separate recognizer or newly promised immediate feature.

Composition has semantic constraints. An operator supported in an assignment may need additional work in a short-circuit condition, method argument or embedded update. Maintain a documented capability/context matrix and fail eligibility explicitly where a combination has not been verified. Do not claim every combination merely because individual AST nodes are known.

## Contract questions to resolve before new runtime production

Pause immediate implementation of the bubble-sort-specific draft-5 from PR #10. Its lexical/runtime identity distinction remains useful, but its final-store retirement rule, two-read IF assumption and sorting-only profile must not become general Java rules.

- **Dependencies:** a condition may compare scalars, computed operands, one array access or several nested accesses. Link captures to source-plan operands without requiring exactly the preceding two events. Define evaluation order and single-use capture ownership before selecting fields.
- **Scope and declaration instances:** keep static declarations separate from dynamic variable/scope IDs. Writes to an enclosing live binding from an inner scope must be legal. Multiple locals, empty blocks and branch bodies ending without an array write need explicit normal exit semantics.
- **Steps and bookkeeping:** one Step remains one observable operation, but scope/frame bookkeeping may require multiple transport records per step. Decide atomic grouping, prefix completeness and end-of-run handling before a schema. Never attach all exits to an arbitrary final write or expose a half-committed group. Preserve old versions' one-event/one-step interpretation.
- **Failures and references:** record only successful operations; stop at the last complete safe group. Preserve pending captures/exception order without invented exits. Distinguish array allocation from reference binding before alias support; do not allocate a second object for an alias.
- **Frames:** identify static methods, dynamic calls, argument/return capture and exceptional unwind separately from lexical scopes. Specify before recursive production, without claiming the current single-main plans already solve it.
- **Limits:** use a trusted capability/trace-budget profile, not an algorithm-name switch. The proposed 128-event allowance is not approved or active. Reassess required records, step counts, byte caps and bookkeeping limits with the new contract; never raise old profiles silently.

If the revised trace cannot preserve an existing version's meanings, introduce a new version deliberately. Draft-5 is still unimplemented, so refine its proposal before freezing a schema. Contract tests should include unrelated programs and multiple statement arrangements, not only one sorting trace. Runtime compatibility and semantic comparisons must follow; do not infer successful instrumentation from contract fixtures.

## Preservation and completion criteria

Keep the current parser/version pins, SourceSnapshot behavior, immutable source ownership, trusted worker/runner isolation, separate trace transport, legacy source plans and existing replay fixtures. Some probe-specific code will eventually be replaced; extensibility does not mean no refactoring. Introduce the new path beside verified behavior, migrate supported cases only after comparisons, and retain historical trace readers/fixtures. Do not delete old paths during the first analyzer task.

Future runtime acceptance must compare original/instrumented values, output, exceptions and side-effect counts inside Docker; test source locations and every selected intermediate/reverse state. Include several differently written programs using the same capabilities. Unsupported syntax must produce a located limitation, not guessed visualization. Execution admission remains independent; partial playback cannot continue past missing facts that invalidate state. The output-only service is still unfinished, not supplied by this review.

Retain all 328 automatic/seven manual regression cases when shared runtime code changes. Browser acceptance remains required when the frontend exists. Current Milestone 1 requirements, including aliasing/side effects and user-written sorting, remain incomplete; all later V1 collections, methods/recursion, Scanner and tree/heap requirements remain.

This review is documentation only. Validation checks consistency with source/requirements/role boundaries, local links, fences, whitespace and the absence of product-code changes. No new Java capability, schema, algorithm execution or browser behavior is verified here.
