# Java Support and Observable Steps

2026-10-03 contract update: after PR #7 merged, the user approved continuing with the conditional contract. [Draft-4](../contracts/run-result-v4.schema.json) and its [validation guide](../contracts/README.md) now cover captured IF comparison operands/read links, lexical scopes and atomic branch-local retirement. Nine designed fixtures, 54 rejection cases, nine partial prefixes and source/replay checks passed alongside older suites. No Java conditional producer, source-plan collector or runtime comparison tests are implemented by this contract task. Earlier proposal-only statements below are historical.

2026-10-03 design proposal only: [conditional compare-and-swap](compare-swap-proposal.md) proposes literal-index array reads, one captured comparison/if decision, and a three-statement swap with a scoped temporary. True/false paths have nine/four steps. [ADR 0009](decisions/0009-conditional-comparison-trace.md) proposes separate draft-4 semantics; all existing contracts and runtime support remain unchanged. Design and implementation require further approval.

2026-10-01 runtime update: [array reads inside the bounded classic loop](loop-array-read-proposal.md) add a separate RHS ARRAY_READ followed by ARRAY_WRITE for `values[i] = values[i] + signed-int-literal`. Addition stays inside the write step. The new source shape is an array declaration then the loop; existing scalar-fill support remains. Both increment forms passed 80 new Docker cases/48 original-generated comparisons and every source/hash/replay prefix check. Full regression-gate status is recorded in [the implementation guide](../runner/analysis/LOOP_READ_RECORDING.md).

2026-09-30 runtime update: the approved [bounded classic for-loop shape](for-loop-array-recording.md) now has an analyzer, transformer, draft-3 recorder, phase-aware collector and prefix replay. Both i++ and ++i passed the 68-case loop execution gate/40 original-generated comparisons, with exact source mapping and forward/backward state checks. Broader loops, sorting and general boolean-variable tracing remain outside this increment. Current verification details are in [the implementation guide](../runner/analysis/LOOP_RECORDING.md); older statements about pending loop implementation are historical.

2026-09-29 contract update: user approved draft-3 schema, illustrative fixtures and validator work. [The contract guide](../contracts/README.md) records passing legacy checks, six new fixtures, 44 negative cases, twelve partial prefixes and fixture source/cursor checks. Draft-1/draft-2 schemas and original fixtures are unchanged. No Java loop implementation or Docker tests were performed. Earlier specification-only status below is historical; runtime implementation still requires approval.

Status: Design draft with approved first-prototype scope, 2026-09-17. The user approved the initial types, entry convention, library targets, and loop coverage below, in addition to the simple stepping examples. Exact syntax/overload and trace details remain unfinished. The dated Verification Status section records implemented and runtime-verified increments; other entries remain planned. Proposed entries below are not an execution allowlist or a finalized V1 support matrix.

## Confirmed Direction

The 2026-09-18 clarification supersedes earlier language-subset restrictions below: users write normal single-file Java with imports, a class, static main, helper methods, and common standard-library calls. Keep the Main.java/Main.main starting convention. Java 21 compilation/runtime supplies ordinary behavior; the tables below organize tracing coverage rather than an authoring allowlist. Follow [ADR 0002](decisions/0002-java-execution-and-visualization-coverage.md).

Follow the [project requirements](../requirements/PROJECT_REQUIREMENTS.md), [architecture](architecture.md), and [frontend design](frontend-design.md). Use Java 21 as the backend/runner target. Expand verified visualization coverage incrementally while preserving real Java behavior. Execution remains subject to the documented isolation and policy restrictions.

Variables and one-dimensional arrays, including user-written sorting, come first. Lists, stacks, queues, maps, binary-search-tree sort, and heap sort remain V1 requirements. Supported user-defined methods and recursion must be delivered before tree/heap sorting. Supported Scanner input is required for the interactive console.

On 2026-09-22, the user confirmed broader array/string/map/set/tree/heap problem visualization as V2, after V1 completion. Sets, general tree algorithms beyond tree sort, broader heap/priority-queue use, and expanded array/string/library coverage belong to that roadmap. Both versions retain Main.java/Main.main; separate test inputs are V2, while interactive Scanner input remains V1. See Section 15 of the project requirements for scope and extensibility rules. Exact V2 subsets remain to be specified and verified.

Common standard-library support may extend beyond collection classes. Approval of this draft does not approve every method, add sets to the V1 diagram requirements, or remove existing exclusions. Concurrency, reflection, native calls, arbitrary external dependencies, and submitted-code file/network access remain outside the current direction. Mediated standard input remains permitted.

## Support Status and Meaning

Track each construct and resolved method overload separately. Class availability in the JDK does not imply analysis, instrumentation, or visualization support.

| Status | Meaning |
| --- | --- |
| Required capability | Product behavior required by confirmed requirements; exact coverage still needs specification |
| Proposed candidate | Suggested coverage awaiting method-level decisions and evidence |
| Specified, unverified | Exact semantics and acceptance cases agreed, but implementation checks have not passed |
| Verified | Relevant implementation, semantic-comparison, trace, and playback checks passed for a recorded version |
| Unhandled tracing | Ineligible for the unsupported transformation; original-source output-only execution remains possible under execution policy |
| Execution-policy exclusion | Rejected for execution independently of tracing coverage; distinguish this from invalid Java |

Track these dimensions independently:

- Execution support: accepted code retains Java behavior, and required results/side effects are captured.
- Visualization support: recorded facts produce the appropriate logical state and operation highlights.
- Internal algorithm tracing: intermediate operations inside a called implementation are actually recorded. This is additional coverage, not implied by accepting a library call.

The analysis role owns the initial instrumentation eligibility gate; instrumentation adds transformation-specific checks. Unresolved transformation facts prevent instrumentation, not automatically original-source execution. The runner must independently establish execution-policy admission and compile the chosen source. For tracing limitations, allow isolated output-only execution with a clear diagnostic. No silent partial instrumentation, guessed state, or automatic rerun after partial execution is permitted. Stop partial playback before an unrecorded change could invalidate state. Label valid-but-unhandled tracing as a product limitation, not a Java syntax error.

Show compiler diagnostics for compilation errors, exceptions and available safe recorded steps for runtime errors, and a separate explanation for visualization limitations. Include source locations when reliable; do not invent locations. Successful execution with unavailable visualization is a valid result, not a complete trace. Select output-only execution before running user code; a recording failure during execution does not authorize a second run.

## Staged Common-Library Proposal

The approved first-prototype scope below supersedes candidate wording in this staging table for the specific entries it selects. Other library candidates remain proposals.

This table organizes candidates by the existing delivery milestones. It does not require all optional helpers in Milestone 1 or authorize their implementation.

| Stage | Required focus and proposed library candidates | Decisions still needed |
| --- | --- | --- |
| Milestone 1: variables/arrays | Required scalar/array behavior and user-written sorting; candidate basic String operations, numeric wrappers, selected Math operations, and System.out.print/println overloads | Supported value types, operators, signatures, conversions, boxing/unboxing, return values, exceptions, and observable boundaries |
| Milestone 2: interactive console | Required supported Scanner input; candidate constructors over System.in and selected token/line reads | Exact constructors/reads, encoding/locale policy, buffering, invalid tokens, EOF, waiting, and limits |
| Milestone 3: collections | Required list/stack/queue/map operations; candidate List/ArrayList, LinkedList, Deque/ArrayDeque, and Map/HashMap | Exact constructors/methods, generic element/key/value types, aliases, ordering, nulls, empty states, and stack/queue presentation selection |
| Utilities after foundational coverage | Candidate selected Arrays and Collections methods | Choose methods by a concrete example; specify recorded result/mutation, errors, ordering, and logical-call stepping |
| V2, after V1 completion | Set/HashSet, broader map/string/array operations, general tree operations, and PriorityQueue/heap-problem support | Define exact operations, types, visualizations and representative acceptance cases; retain Main.java and existing V1 behavior |
| Later discussion, no commitment | Comparator, streams, and third-party libraries | Evaluate specific needs during feature design; this is not a blanket library-support commitment |

List, Map, and Deque are interface-level authoring candidates; concrete implementation types and method dispatch must be resolved under the agreed policy. Naming an interface does not authorize arbitrary implementations. Library calls must be identified through resolved types and signatures, not method names alone.

When implementing a tracing adapter or transformation for a method, document its resolved overload, types, effects, result, exceptions, aliasing, ordering, trace representation, step treatment, and acceptance fixtures. This is internal correctness work, not a requirement for users to approve every normal library call before execution. Prefer representative programs and investigate relevant Java 21 semantics as needed within approved work. No method-by-method research or runtime validation has been performed for this draft.

## Approved First-Prototype Scope

The user approved this capability scope on 2026-09-17. Approval establishes implementation targets, not verified support or permission to start implementation.

| Area | Approved target |
| --- | --- |
| Entry convention | One Main.java file containing class Main and public static void main(String[] args) |
| Values | int, boolean, and String |
| Arrays | One-dimensional int arrays, including aliases |
| Statements | Declarations, assignments, if/else, classic for, enhanced for over int arrays, and while |
| Loop control | break and continue in supported loops; exact supported forms remain to be specified |
| Expressions | Integer arithmetic, comparisons, boolean logic, array access, array length, and loop increments |
| Library targets | System.out.print/println for the selected value types; String.length() and String.equals(Object) |

The entry-point String[] parameter is part of the Java main signature; it does not expand diagram support to general String arrays. Define the permitted package/file arrangement and command-line argument behavior before implementation.

Library targets do not promise visualization for arbitrary objects or every print/println overload. Specify exact semantics before instrumenting them, including arguments, nulls, exceptions, and recording. Other ordinary calls may still run through the admitted original-source path. The first-prototype matrix describes tracing priorities, not blanket restrictions on compiler-valid Java.

### Loop Forms

Classic for with an integer loop variable is included:

```java
for (int i = 0; i < values.length; i++) {
    int value = values[i];
}
```

Enhanced for (for-each) over an int array is included:

```java
for (int value : values) {
    System.out.println(value);
}
```

These fragments assume values refers to an int array in the approved main wrapper. Collection for-each follows when collection coverage is specified and verified. The user's request for loop forms does not approve every Java loop variation, do/while, labels, arbitrary Iterable implementations, or iterator methods.

Preserve iteration order, loop-variable scope, element-read behavior, and the control-flow effects of break/continue. Specify their observable boundaries and source associations before implementation. Synthetic iterator/index bookkeeping introduced by instrumentation must not create invented user variables or unexplained steps.

Add acceptance fixtures for zero/multiple iterations, nested supported loops, early exit, skipped iterations, scope lifetime, and array reads, including backward replay. Exact labeled/unlabeled control coverage and event sequencing remain contract decisions; none of these checks has run.

### Following Increments

Scanner input, supported user-defined methods/recursion, and collections follow the initial variable/array prototype under the existing milestone plan. Numeric wrappers, additional Math/String methods, and Arrays/Collections utilities remain method-specific candidates. This initial scope does not remove later V1 requirements.

## Approved Loop-Step Rules

The user approved recording these rules after the loop-stepping and highlighting discussion. Recorded on 2026-09-18; these are design decisions, not verified implementation.

Highlighting temporarily marks the original code expression and relevant diagram element for the selected operation, using a background, border, or color. It guides attention without changing program data. A write step updates the diagram from the recorded mutation and highlights the affected cell; the highlight itself does not perform the mutation. Exact colors and styling remain undecided.

| Action | One observable Step shows |
| --- | --- |
| Initialize `int i = 0` | Create i with value 0 and highlight the declaration |
| Check `i < values.length` | Highlight the condition and show its recorded true/false result |
| Execute the loop body | Advance through each statement's observable operations under their agreed rules |
| Execute standalone loop update `i++` | Update i from the recorded value and highlight it |
| Execute `break` | Highlight break and leave the targeted supported loop |
| Execute `continue` | Highlight continue and skip the remaining body of the targeted supported loop |

For classic for, continue proceeds to the update expression and then the condition; those remain subsequent operations, not part of the continue click. For while, continue proceeds to the condition. A final false condition has its own step. Do not invent a false-condition event when break exits the loop instead.

For enhanced for over an int array, one iteration-entry step highlights the next element read and assigns its recorded value to the loop variable. The read and variable binding are intentionally grouped into this one observable operation. Body operations follow normally. Do not expose a synthetic index or synthetic termination comparison as user code. This grouping does not change the separate read/write steps agreed for ordinary array-to-array assignment.

Leaving a loop removes bindings whose scopes end at that boundary without an extra bookkeeping click. Bindings declared outside the loop remain. Precise bookkeeping event placement must support deterministic backward replay under the future trace contract.

These rules cover the simple loop forms discussed. Exact nested-expression decomposition, labeled control coverage, source ranges for for-each, and event schemas still require specification. Output-call stepping, including println, remains undecided. Do not generalize standalone i++ stepping to arbitrary increment expressions without specifying their evaluation and return-value semantics.

## Agreed Initial Stepping Example

The user accepted the following teaching-level step granularity on 2026-09-17. This fragment belongs inside the approved Main.main entry-point wrapper.

```java
int x = 3;
int[] values = {5, 2};
values[0] = x;
```

| Position | Highlight | Logical visualization after the step |
| --- | --- | --- |
| Initial | None | Neither example variable is displayed yet |
| Step 1 | `int x = 3` | Create variable x with value 3 |
| Step 2 | `int[] values = {5, 2}` | Create array values with indexed contents [5, 2] |
| Step 3 | `values[0] = x` | Change index 0 from 5 to 3 and highlight that cell; x remains 3 |

For these literal initializers, declaration and initialization form one observable step. Internal identity/allocation bookkeeping does not create extra clicks. Reading scalar x in the assignment does not add a click in this initial policy.

For the separate example `values[0] = values[1]`, use two observable steps: read index 1, then write the captured value to index 0. Immediately after the read step, the array is unchanged and the read is highlighted. After the write step, the destination reflects the recorded value. Precise source-coordinate encoding remains trace-contract work.

Backward navigation restores the previous recorded program state without rerunning Java. From Step 3 to Step 2, the array returns to [5, 2]; moving back again removes the displayed array binding and leaves x = 3. Layout persistence during navigation remains a separate open decision.

These examples do not finalize rules for initializers containing calls or array reads, compound assignments, increment/decrement, short-circuiting, nested expressions, or methods/recursion. Do not hide separately observable operations inside a declaration by generalizing the literal-initializer example. Define comparisons, arithmetic visibility, scope/frame bookkeeping, exceptions, and initial/terminal boundaries in the future trace specification.

## Library Visualization Policy to Specify

For ordinary collection operations, propose logical changes such as an added list element or updated map entry rather than a diagram of undocumented implementation internals. Exact operation granularity still needs approval per supported method.

Arrays.sort and Collections.sort are candidates only. If later supported as logical operations, show captured before/after state without inventing internal comparisons or swaps. Internal algorithm visualization requires separately verified recording coverage. User-written sorting remains the first detailed tracing target.

Library exceptions and failed mutations must follow actual supported Java behavior and preserve valid partial traces. Do not promise rollback for a library method unless its specified behavior supports it. Aliased references must still observe one logical object. A method that performs callbacks or multiple externally visible mutations requires explicit capture/step decisions before eligibility.

## Required Specification Work Before Implementation

1. Complete the approved Main.main convention details and the exact first-slice syntax, operators, loop-control forms, and library overloads for the selected types.
2. Define scope/object/frame identities, aliasing, numeric representation, supported exceptions, and original source ranges.
3. Define observable events versus bookkeeping and committed-state semantics in `specs/trace-format.md`, with machine-readable examples under `contracts/`. These destinations are planned, not yet created.
4. Specify supported method calls and recursion, including parameters, returns, unwinding, and depth limits, before implementing those features.
5. Specify Scanner behavior jointly with run/WebSocket lifecycle, acknowledgements, wait detection, EOF, and disconnect policies. Input acceptance and Java consumption remain distinct.
6. Define concrete bounds and isolated execution checks in the planned `specs/execution-isolation.md`.
7. Create acceptance programs with expected intermediate states and original-versus-instrumented comparison criteria. Include sorting, aliases, side effects, failure paths, source edits, and replay without repeated input.

The first approved prototype should prove the smallest coherent variable/array subset. Expand through tested examples rather than assuming an entire standard-library class works. Preserve later V1 requirements while keeping each implementation milestone bounded.

## Verification Status

2026-10-02 completed verification: the [bounded loop read/addition implementation](../runner/analysis/LOOP_READ_RECORDING.md) has 80 new runtime cases, 48 original-generated comparisons, 40 prefix/postfix pairs and per-step forward/backward states. Read and write highlights are distinct; failed reads and limits preserve safe prefixes, and int wraparound retains Java semantics. Recovery proved all six freshly generated fixture batches match the October 1 evidence, revalidated all 148 completed loop results, and completed the 150-case legacy group plus seven manual cases. Total coverage is 298 automatic cases/212 comparisons and seven manual cases/four comparisons. One Docker evidence failure required a legacy-suite retry; the guide records that limitation and exact evidence. Later V1 work and the frontend remain incomplete. Older dated entries below describe their historical scope.

2026-09-28 specification only: [first classic for-loop recording](for-loop-array-recording.md) proposes one loop with a local int index, `index < array.length`, prefix/postfix increment and one scalar-to-array store. It follows the approved condition/update/final-false/scope-exit stepping rules. The example has 13 steps; the index disappears atomically with the last false condition. Proposed draft-3 condition and scope effects require contract work before a producer. No loop implementation or runtime tests were performed; the verified 150-case/124-comparison baseline below is unchanged. Enhanced for, while, nested loops, break/continue and sorting remain later required increments.

2026-09-28 verified: [standalone index increments](index-increment-array-recording.md) defines both `i++;` and `++i;` at the update position of the five-operation indexed shape. Each records one committed VARIABLE_WRITE and highlights its original unary expression. Capture the updated variable after executing the statement; do not record the discarded postfix result. Embedded increments and loop execution remain outside this increment. The full Docker gate exited 0: 150 automatic cases/124 original-generated comparisons, all 150 result/replay checks, 20 prefix/postfix comparisons and shared/manual regressions passed, with owned-container cleanup verified. Earlier dated entries below retain their historical scope and evidence.

2026-09-26 verified: [index addition before an array write](index-addition-array-recording.md) supports `i = i + decimal-int-literal` in the five-operation indexed shape. The assignment is one committed VARIABLE_WRITE with a whole-assignment highlight; target and RHS reference resolve to the same index binding. Java int wraparound and independent backward restoration are verified. The fresh gate exited 0: 110 automatic cases/92 comparisons, all 110 result/source/reconstruction checks, analyzer/collector/contract/batch/worker-limit checks and seven manual regressions. Two bounded fixture batches retain the 1-MiB caps. No general arithmetic, alternate operand order, compound assignment, increments or loops are added. Entries below record earlier verified increments and their then-current limitations.

2026-09-26 verified: [index update before an array write](index-update-array-recording.md) supports five operations: declare value, declare array, declare index, assign a literal to that index, then store using its current value. The index write targets variable-3 and has an independent highlight/backward step; variable-1 remains unchanged. The full Docker gate exited 0: 88 automatic cases/74 comparisons, all result/source/reconstruction checks, analyzer/collector/contract/worker-limit checks and seven manual regressions. Failed stores retain four events; budgets 1–4 stop before the next operation and budget 5 completes. Arithmetic, expression indices, multiple updates and updating the value scalar within this indexed shape remain unsupported. Earlier verified shapes remain supported; the entries below describe their historical scope.

2026-09-25 verified: [variable-index array recording](variable-index-array-recording.md) supports a separately declared int index in `values[i] = x`. The bounded shape declares the value scalar, array and index scalar, then performs one store. Two scalar declarations remain separate observable operations and bindings; scalar reads add no extra step. Repeated event kinds retain distinct source associations. The full gate passed 66 automatic cases/56 comparisons, all result/source/backward checks, analyzer/collector/contract/worker-limit checks and seven manual regressions. This does not support index updates, expression indices or arithmetic in the new shape; earlier scalar-update shapes remain supported.

2026-09-25 recovered verification: [scalar update before an array write](scalar-update-array-recording.md) implements four operations: declare int, declare int[], assign a literal to the same int, then store that current value at a literal array index. September 24 runtime evidence covers 47 automatic cases/40 comparisons; September 25 saved-result validation passed all 47 automatic and seven manual results without rerunning Java. Draft-2, independent backward states, failed-write prefixes and budgets one through four are verified for this exact shape. Variable indices, arithmetic and loops remain outside current implementation. See the scope document for the recovered-evidence details.

2026-09-24: the [combined integer/array increment](combined-variable-array-recording.md) implements and verifies the earlier three-step teaching example. Fourteen combined runtime cases passed alongside the 17 existing automatic cases, with 27 total original/generated comparisons and all 31 source/contract/reconstruction checks. The reviewed shape has exactly one literal int declaration, one literal int[] declaration and a literal-index store from that scalar. Existing separate scalar/array shapes remain supported; general mixed programs, arithmetic and loops remain unimplemented. See the scope document for failures, limits, binding identities and probe restrictions.

The approved [integer-variable experiment](integer-recording.md) defines literal int initialization/assignment recording before implementation. It adds two post-operation steps and keeps mixed programs, arithmetic and loops outside this increment.

The approved [automatic array recording experiment](automatic-array-recording.md) extends the existing manual declaration/read/write experiment through a checked analysis handoff and separate AST copy. Its exact transformation rules and tests do not expand the eligible Java shape.

The separately approved [array analysis experiment](array-analysis-experiment.md) specifies the narrow analysis-only handoff, eligibility, diagnostics, source coordinates and resource bounds before implementation. It does not add runtime tracing coverage or authorize automatic recorder insertion.

The approved [controlled array recording experiment](array-recording-experiment.md) defines the exact declaration/read/write instrumentation semantics and bounds for its manually paired fixtures. It does not expand the general verified Java subset. Its own verification section will record results after implementation.

This file remains a documentation draft with approved capability scope, simple stepping examples, and loop-step rules. The controlled array experiment has runtime evidence for its reviewed manual pairs. The separate analyzer and automatic transformer now have AST/resolution/source-mapping and runtime comparison evidence for the same narrow array region. No broader automatic transformation or browser visualization exists. Exact broader syntax/overload coverage and trace encodings require further specification; output-call stepping and more complex expression boundaries remain unresolved.
