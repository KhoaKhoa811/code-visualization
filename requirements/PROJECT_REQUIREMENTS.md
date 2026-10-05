# Java Code Visualization — V1 Requirements

## 1. Purpose

Build an educational website where a user writes Java code and watches its execution through visual representations of variables and data structures. For example, a variable appears as a labeled box, an array appears as indexed boxes, and a user-written sorting algorithm animates comparisons and changes step by step.

The frontend must use TypeScript, React, Vite, TanStack Query, Axios, and TanStack Router. The backend must use Java 21, Spring Boot, and Maven. Execution uses Docker Desktop with WSL2 and Linux runner containers for Windows-local V1. Development will be assisted by Codex in VS Code. The selected frontend baseline is Node.js 22, React/React DOM 19.3, and Vite 8. Exact patch versions and remaining dependencies must be pinned and installation-tested during setup.

The audience includes Java beginners and algorithm students. The primary learning outcome is understanding algorithm behavior by observing synchronized code and diagram changes. V1 runs locally on Windows; server deployment is a future extension. There is no fixed deadline. Establish correct variables and arrays before adding the other structure categories.

**Core principle: write code that can be expanded and updated.** Maintain clear module boundaries, explicit contracts, and testable behavior. Adding a supported data structure should normally require an adapter, renderer, and tests rather than changes throughout the system.

## 2. Requirement status

- **Confirmed:** Java-only V1; TypeScript, React, Vite, TanStack Query, Axios, and TanStack Router frontend; Java, Spring Boot, and Maven backend; Docker execution; code visualization, variables, arrays, lists, stacks, queues, maps, trees, heaps, and extensibility.
- **Confirmed playback behavior:** recorded diagram playback after execution terminates, using available validated facts.
- **Implementation baseline:** Monaco remains proposed; bounded source instrumentation and JavaParser/SymbolSolver are verified in the prototype. This does not establish general Java tracing or application integration.
- **Needs technical validation:** composable language coverage, broader instrumentation, library operation coverage, and tree/heap representation.

### Confirmed clarification decisions

- On 2026-10-04/05, the user clarified that differently written supported Java must not depend on recognizing an exact algorithm template, and merged the [composable tracing review](../specs/composable-java-tracing.md) in PR #11. Build coverage around statements, expressions and verified combinations; use bubble sort, quicksort and other programs as acceptance examples. This does not promise arbitrary Java or immediate quicksort support. Detailed implementation still requires approval.

- On 2026-09-22, the user confirmed finishing the existing V1 scope before implementing V2. V2 broadens visualization for Java solutions to array, string, map, set, tree, and heap problems, including the coverage gaps identified in the requirements review. Existing V1 array/string/collection requirements, tree sort, heap sort, methods/recursion, and live Scanner input remain unchanged. See Section 15 for the V2 roadmap; exact operations and acceptance cases require specification before implementation.
- Both V1 and V2 retain Main.java with Main.main. A separate test-input workflow is a V2 feature and still runs Main.java; this does not select a solution-class invocation model. V1 interactive Scanner input remains required. Input format, case management, and delivery semantics for V2 remain to be designed.

- On 2026-09-18, the user confirmed ordinary single-file Java authoring with imports, a class, static main, helper methods, and common standard-library calls. Java 21 compilation/runtime determines language behavior. Tracing coverage is separate: valid code beyond that coverage may execute in isolation and show output with a clear visualization limitation. Playback stops before missing facts could make state misleading. Existing execution restrictions and later V1 visualization requirements remain. See [ADR 0002](../specs/decisions/0002-java-execution-and-visualization-coverage.md); this supersedes blanket rejection solely for unsupported tracing.

- The frontend is dark-only for V1: visualization left, Java editor upper right, live console lower right. The user's screenshot is a layout/style reference, not a requirement to reproduce its operation boxes. Show only supported structures from submitted code which exist at the recorded step. Drag arrays as groups; list/tree/heap nodes may move individually without changing logical order or relationships. See `specs/frontend-design.md`.
- Diagrams support dragging existing boxes/nodes to improve readability. Dragging changes presentation coordinates only; values, identities, and connections remain determined by execution. Users cannot draw new program structures or edit program data through diagrams.
- V1 includes live console input through a documented subset of `Scanner` over `System.in`. Output appears during execution; the user types a line and presses Enter to send it to the running program. Diagram playback begins after execution terminates, using the available recorded trace. Replay must not request input again.
- Java 21 is the backend and runner JDK target. The user's earlier reference to "React 22" meant Node.js 22. On 2026-09-15, local `node --version` returned `v22.23.2`; retain it for initial setup. React and React DOM must use matching 19.3 patch versions with Vite 8. These are selected version lines, not an installed or tested dependency set. See the compatibility references in `specs/architecture.md`.
- The frontend/backend stack above was confirmed on 2026-09-15. Axios provides HTTP transport; TanStack Query manages server request state and caching; TanStack Router handles navigation. Keep execution playback state separate from server state. Styling, diagram-rendering tools, testing tools, and exact versions remain undecided. Stack selection does not imply installed dependencies or verified compatibility.
- Starter templates remain acceptable for V1, but they must not define permanent algorithm-specific tracing eligibility. User-defined methods and recursion remain required within documented coverage.
- Each Step click advances one observable operation and highlights its source expression. Several clicks may remain on the same source line.
- Tree support focuses on binary-search-tree sort, including construction and traversal. Heap support focuses on heap sort, including comparisons, swaps, and sift operations, with synchronized array and tree views.
- Use Docker Desktop with the WSL2 backend and Linux runner containers for Windows-local execution. Verify host prerequisites during setup and isolation during the prototype. Docker installation is not evidence that isolation requirements have been met.
- Broader Java analysis and server deployment are future directions. Preserve the analysis/execution, trace, playback, and rendering boundaries so these upgrades can reuse existing modules. Do not promise arbitrary Java support or zero future rewrites.

These decisions supersede conflicting proposed choices below. Exact node templates, supported method signatures, event details, resource limits, and dependency versions remain implementation specifications to validate.

Use the proposed baseline to plan and prototype. Record changes to technical decisions with reasons. Do not silently reduce the full V1 data-structure scope: milestones are implementation increments, not separate definitions of V1.

## 3. User experience

1. The user writes or edits a Java program in Monaco Editor.
2. The user selects Run.
3. The system validates the source and reports syntax errors or unsupported features with source locations.
4. An isolated runner compiles and executes admitted code, recording a bounded trace when tracing is eligible or showing output with an explicit visualization limitation otherwise.
5. The frontend displays the trace as synchronized source highlighting and data-structure visualizations.
6. The user can play, pause, move forward/backward one step, restart playback, and adjust playback speed.
7. Execution errors and resource limits appear as clear terminal run states, with available partial trace data.

Confirmed desktop layout: visualization left, Java editor upper right, and live console lower right, using a dark theme. Provide playback controls; exact placement remains a design detail. Handle empty, loading, running, completed, failed, cancelled, and limited states explicitly. Keep a trace associated with the exact source version that produced it; editing source must not silently remap old events to new lines. See `specs/frontend-design.md` for interactions and the screenshot reference.

During Run, the console displays stdout/stderr incrementally, including prompts without a trailing newline, and accepts line input for supported `Scanner` reads. Show when execution is waiting for input. Cancellation remains available while waiting. Define end-of-input behavior and input-wait limits before implementation. This console is program input/output, not a host shell.

During recorded playback, users may drag diagram elements for readability. Their connected arrows/lines follow the elements. Layout is separate from execution state and trace data; dragging must not alter values, relationships, or the playback cursor. The exact layout persistence policy remains to be specified.

## 4. Execution pipeline

The initial proposed pipeline is:

1. Monaco Editor produces Java source.
2. Backend parsing and semantic analysis validate supported syntax, types, and operations.
3. Instrumentation adds trace recording around supported operations.
4. An isolated worker compiles and runs the instrumented program.
5. The trace engine emits versioned events.
6. A frontend state reducer reconstructs execution state.
7. Structure-specific renderers animate that state.

AST means Abstract Syntax Tree. Tree-sitter builds a concrete syntax tree and does not execute Java or determine runtime values. It is optional for editor-side incremental analysis. Evaluate JavaParser or an equivalent Java-aware tool for backend parsing and symbol resolution; parsing alone does not replace Java compilation or runtime execution.

Source instrumentation is the recommended prototype direction. Alternatives include JDI-based tracing or a custom interpreter for a documented Java subset. Compare alternatives only as needed to resolve prototype findings; avoid building multiple complete engines.

Instrumentation must preserve program semantics: evaluation order, side effects, short-circuit behavior, scope, exceptions, and return values. It must never evaluate an expression twice to capture its value.

Example semantic risk:

```java
values[i++] = calculate();
```

A correct transformation increments `i` once and calls `calculate()` once. If faithful instrumentation is unavailable, reject that transformation and report the tracing limitation. The original program may use the isolated output-only path if execution admission succeeds; never produce misleading visualization or silently rerun after partial execution.

## 5. Java support policy

V1 accepts ordinary single-file Java under the entry convention and execution restrictions. Visualization supports an incrementally verified subset; compiler acceptance does not imply full visualization or unrestricted execution.

The first engine milestone should support:

- A documented single-file entry-point convention, such as `Main.main`.
- Primitive values and strings, with an explicit supported-type matrix.
- Variable declarations and assignments.
- One-dimensional arrays, indexing, reads, and writes.
- Supported arithmetic and comparison expressions.
- `if`/`else`, `for`, and `while`.
- User-written sorting logic over arrays.

Subsequent V1 increments must add the supported collection methods and node operations needed for lists, stacks, queues, maps, trees, and heaps. User-defined methods and recursion are required in V1. Specify arguments, return values, frame lifetimes, recursive calls, and depth limits before implementation; deliver this support before the tree/heap sorting milestone.

Before implementation of each feature, document its exact supported syntax and semantics in `specs/java-support.md`. Define behavior for null values, numeric types, scope exit, aliasing, method frames, exceptions, and unsupported calls. Do not silently approximate unsupported Java behavior.

V1 includes a documented subset of `Scanner` methods for interactive standard input. Specify supported constructors and reads, token/line behavior, invalid input, end-of-input, blocking, encoding, and exceptions in `specs/java-support.md`. Use actual Java semantics; do not emulate reads with different behavior. Exact method coverage remains unresolved.

Initially exclude concurrency, reflection, native calls, external dependencies, file/network access, and arbitrary library internals. These are proposed scope boundaries, not a security mechanism. Mediated standard input is permitted and does not permit submitted code to access the network or host files.

Use JDK 21 for the backend and runner, with Java release target 21. This does not imply support for every Java 21 language feature. Pin the JDK distribution/patch and compatible dependency versions during setup. Document them and include reproducible build commands for Windows/PowerShell and the runner environment.

## 6. Data-structure visualization requirements

| Structure | Required logical visualization | Required behavior |
| --- | --- | --- |
| Variable | Named box containing type/value | Creation, assignment, and scope exit |
| Array | Indexed sequence of boxes | Allocation, access/write highlighting, length, and shared references |
| List | Ordered sequence of elements | Supported reads, additions, replacements, and removals |
| Stack | Vertical elements with top marker | Push, pop, peek, and empty state |
| Queue | Sequence with front/back markers | Enqueue, dequeue, peek, and empty state |
| Map | Key–value entries | Put/update, get, remove, and documented ordering behavior |
| Tree | Binary search tree nodes connected by edges | Tree-sort construction, node/link changes, comparisons, and traversal highlights |
| Heap | Synchronized tree and array views | Heap-sort construction, extraction, comparisons, swaps, and sift operations captured from user code |

Detection must rely on resolved supported types and explicit conventions. Do not assume arbitrary objects are recognizable data structures.

- A `Deque` can act as a stack or queue. Support an explicit display mode or documented annotation/convention when intent is ambiguous.
- Define a supported tree node class or mapping convention. Do not assume every class with `left` and `right` fields is a tree.
- For a heap, distinguish logical priority-queue operations from actual internal heap layout. Do not invent a layout from unspecified iteration order.
- Clearly label logical views; do not imply they display Java implementation internals unless those internals are actually captured.
- Render nulls, empty structures, repeated values, and shared references correctly. Handle cycles safely in object traversal.

## 7. Meaning of a visualization step

A step represents a documented observable operation, not necessarily one source line.

The Step button advances exactly one observable operation and synchronizes the diagram with the highlighted source expression. Multiple operations on one line require multiple clicks. Internal bookkeeping events must not create unexplained extra clicks; define their mapping to observable steps in the trace specification. Grouped animations must not skip observable operations during manual stepping.

Candidate event kinds include declaration, assignment, array read/write, comparison, supported collection operation, node/link change, scope entry/exit, method entry/exit, exception, and run termination.

For a user-written sorting algorithm, preserve comparisons and writes in execution order. A comparison event records operands and result. A swap may consist of several assignments. A presentation layer may group operations for animation, but must preserve the original event sequence and allow correct stepping.

A call such as `Arrays.sort(values)` does not automatically expose its internal algorithm. Visualize it only through verified logical-operation capture or actual internal tracing. Otherwise report the limitation and allow the original program's isolated output-only path under execution policy. Do not fabricate internal steps or continue playback across uncaptured mutations.

Define whether each event represents state before or after the operation. Recommended baseline: successful mutation events represent committed state after that operation; failed operations produce an exception event without a successful mutation event.

## 8. Trace contract and state model

Create a versioned, machine-readable trace contract under `contracts/`, with semantics documented in `specs/trace-format.md`.

A trace must carry:

- Schema version, run identity, and source version/hash.
- Ordered event sequence and terminal run status.
- Source file and range for user-visible operations.
- Stable object identities and variable identities distinct from display names.
- Scope and call-frame identities where applicable.
- Typed values that distinguish primitives, strings, null, and object references.
- Sufficient state changes to reconstruct every supported playback step.
- Diagnostics and explicit limit/truncation information.

Define a numeric serialization policy that preserves Java values, including `long` values outside JavaScript's safe integer range and supported floating-point special values.

Illustrative event shape; finalize the schema before using it as an API contract:

```json
{
  "schemaVersion": 1,
  "runId": "run-1",
  "step": 12,
  "kind": "ARRAY_WRITE",
  "source": { "file": "Main.java", "line": 8, "column": 5 },
  "frameId": "frame-1",
  "targetId": "object-3",
  "index": 2,
  "previousValue": { "type": "int", "value": 7 },
  "value": { "type": "int", "value": 4 }
}
```

Trace events must not contain screen coordinates, colors, animation durations, or React component names. Keep runtime facts independent from presentation.

Aliasing must work correctly:

```java
int[] a = {1, 2};
int[] b = a;
b[0] = 9;
```

`a` and `b` refer to one array object. Both views must reflect `[9, 2]`.

Use an initial state plus ordered events, with optional periodic snapshots for seeking. Backward playback reconstructs recorded state; it does not execute Java backward. Replaying the same trace must reconstruct the same state.

## 9. Architecture and extensibility

Use one repository with these boundaries:

| Module | Responsibility |
| --- | --- |
| `frontend/` | React/TypeScript editor, playback, state reducer, renderer registry |
| `backend/` | Spring Boot API, validation orchestration, run lifecycle, trace delivery |
| `runner/` | Isolated compilation/execution and trace production |
| `contracts/` | Trace and API schemas plus representative examples |

Recommended extension points:

- Trace engine interface: source/run request to trace result.
- Structure adapters: supported runtime objects to logical structure state/events.
- State reducer: trace events to playback state.
- Renderer registry: logical structure kind to visual component.

Avoid a single large service or React component that handles parsing, execution, state, and rendering. Prefer composition and clear interfaces over speculative abstraction. Do not introduce microservices, a message broker, a database, authentication, or streaming unless a current requirement justifies them.

The first noninteractive tracing prototype may return a bounded completed trace. The end-to-end V1 API must support a run handle, incremental console output, input submission, cancellation, and terminal trace retrieval. Live console communication is required; live diagram-trace playback is deferred. On 2026-09-16, the user confirmed native browser WebSocket and Spring WebSocket handlers for console output/input/EOF and notifications, with Axios/TanStack Query over HTTP for run creation, cancellation, status, and completed results. See [the transport decision](../specs/decisions/0001-live-console-websocket.md). Message contracts, buffering, acknowledgements, and disconnect behavior remain to be specified and verified; no broker or durable job system is introduced.

## 10. Execution isolation and resource limits

Submitted source is untrusted input. Never compile or execute it inside the Spring Boot application JVM.

- Compile and execute within a dedicated isolated worker environment.
- Use an unprivileged identity and restrict filesystem and network access.
- Do not expose application secrets, host mounts, or container-management sockets to submitted code.
- Enforce compilation/execution timeout, memory, process/thread, output, and trace-size limits.
- Bound object traversal depth, collection size, and total serialized data.
- Terminate the entire run environment on timeout/cancellation and clean temporary resources.
- Return explicit reasons when a limit is reached; never present a truncated run as complete.
- Keep tracing data separate from user stdout so printed text cannot masquerade as trace events.
- Limit concurrent runs and isolate one run's files/data from another.
- Bound input line size, total input bytes, pending input, and input-wait duration. Define how waiting affects execution timeouts while retaining an overall run bound. Handle disconnects, EOF, cancellation, and writes to terminated processes explicitly. Interactive input must never leave an unlimited orphaned worker.

Select and document concrete limits during the runner prototype. Source validation and a separate JVM process alone are not sufficient isolation for public deployment. A local prototype must not be presented as ready for public untrusted execution before isolation has been verified.

## 11. Codex repository organization

Use the following path map when scaffolding the repository:

| Path | Purpose |
| --- | --- |
| `requirements/PROJECT_REQUIREMENTS.md` | This document; original project intent and baseline |
| `AGENTS.md` | Shared project instructions and context-loading rules |
| `.codex/agents/architect.toml` | Architecture and contract review role |
| `.codex/agents/trace-engineer.toml` | Java semantics, instrumentation, and runner role |
| `.codex/agents/frontend-engineer.toml` | Editor, playback, and rendering role |
| `.codex/agents/reviewer.toml` | Correctness, isolation, and regression review role |
| `specs/v1-scope.md` | Complete V1 boundaries and milestone mapping |
| `specs/java-support.md` | Supported syntax, types, and library methods |
| `specs/trace-format.md` | Event semantics and compatibility policy |
| `specs/visualization-rules.md` | Structure detection, display modes, animations |
| `specs/execution-isolation.md` | Isolation design and enforced limits |
| `specs/acceptance-cases.md` | Programs and expected outcomes |
| `specs/decisions/` | Accepted architectural decisions and rationale |
| `memory/current-state.md` | Actual implemented and verified state |
| `memory/next-steps.md` | Immediate prioritized work |
| `memory/known-issues.md` | Verified limitations and unresolved defects |
| `backend/AGENTS.md` | Backend-specific conventions |
| `frontend/AGENTS.md` | Frontend-specific conventions |
| `runner/AGENTS.md` | Execution and tracing invariants |
| `contracts/` | Machine-readable contracts |

`AGENTS.md` contains persistent instructions. Custom agents define focused responsibilities; an arbitrary directory of Markdown personas does not automatically register Codex agents. Verify custom-agent configuration against the installed Codex version before generating configuration. If unsupported, use documented role instructions without claiming automatic agent registration.

Specs describe intended behavior. Memory describes actual progress and must not override specs. Keep memory concise and move accepted architectural decisions into `specs/decisions/`. Do not claim incomplete work is finished. Optional reusable skills may be added later when repeatable workflows emerge.

## 12. Instructions for Codex development

When creating the actual `AGENTS.md`, incorporate these rules:

1. Read this document, relevant specs, and concise current-state notes before changing code.
2. Inspect existing repository instructions and implementation first. Preserve unrelated user changes.
3. Maintain Spring Boot and React as the required stack.
4. Keep changes scoped to the active milestone; do not silently drop later V1 requirements.
5. Preserve Java semantics and object identity. Distinguish invalid Java, execution-policy restrictions, and unsupported tracing. Allow admitted output-only execution for tracing limitations and report visualization completeness honestly.
6. Keep execution traces independent from UI rendering.
7. Keep submitted code isolated from the backend application.
8. Define or update contracts before implementing incompatible producer/consumer changes.
9. Add meaningful tests for semantic transformations, trace replay, adapters, and isolation limits.
10. Record material decisions and update memory after verified progress.
11. Report what changed, what was tested, and remaining limitations.
12. Do not claim tests passed unless they were actually run successfully.
13. Use focused agents only when the environment supports them and the task benefits from delegation; multiple agents are not required for every change.
14. Prefer maintainable, working increments over generating the entire application in one pass.

## 13. Implementation milestones

### Milestone 0 — Repository and specifications

Create the instruction/spec/memory structure. Record the Java support matrix, candidate trace schema, architecture decisions, and acceptance cases. Verify agent configuration compatibility. Document build/runtime versions without generating unnecessary application features.

### Milestone 1 — Trace feasibility prototype

Prove scalar declarations/assignments, arrays, conditions, loops, comparisons, and a user-written sorting algorithm. Check evaluation order and aliasing. Establish a real compilation/execution boundary and resource limits. Use findings to confirm or revise source instrumentation before expanding the engine.

### Milestone 2 — End-to-end editor and playback

Integrate Monaco, Spring Boot run API, worker, recorded trace, state reducer, and variable/array renderers. Deliver synchronized source highlighting, playback controls, console/errors, and limit handling.

Include live console output and supported `Scanner` input, waiting/cancellation/EOF handling, and draggable diagram layouts. Prove an interactive worker round trip before UI integration; diagram playback still starts only after termination.

Verify variables and arrays before expanding to other structures. Then add and verify supported user-defined methods and recursion, including call-frame playback, before Milestone 4.

### Milestone 3 — Collection visualization

Add documented list, stack, queue, and map adapters, operations, renderers, and acceptance cases. Support explicit stack/queue view selection where needed.

### Milestone 4 — Tree and heap visualization

Implement binary-search-tree sort using the agreed node template and heap sort using an array-backed heap convention. Show tree construction and traversal, and heap comparisons, swaps, and sift operations. Synchronize heap array/tree views, including the active heap boundary and sorted suffix. Capture algorithm steps from executed user code; do not infer library internals. Other tree/heap algorithms are future extensions.

### Milestone 5 — V1 completion

Verify the full V1 support matrix, cross-module tests, failure states, resource cleanup, and development instructions. Update documentation and known limitations. V1 completion requires all V1 structure categories within their documented supported scope; the V2 roadmap below does not expand this completion gate.

## 14. Acceptance cases

| ID | Case | Expected result |
| --- | --- | --- |
| AC-01 | `int x = 5; x = 8;` | A named value box shows 5, then 8 |
| AC-02 | `int[] a = {3, 1, 2}; a[1] = 9;` | Three indexed boxes; only index 1 changes |
| AC-03 | Array loop doubles each element | Trace shows each supported read/write in order; final state is correct |
| AC-04 | User-written bubble sort on `{3, 1, 2}` | Comparisons and writes are visible; final state is `{1, 2, 3}` |
| AC-05 | Two variables reference the same array | One shared object identity; mutation is visible through both references |
| AC-06 | Same variable name in different supported scopes/frames | Distinct identities and correct lifetimes |
| AC-07 | Supported expression with side effects | Same result and side-effect count as the uninstrumented program |
| AC-08 | Forward/backward/restart playback | Exact state restored without rerunning user code |
| AC-09 | Compilation error, execution-policy restriction, or tracing limitation | Distinct diagnostic with source location when available; only tracing limitations permit admitted output-only execution; no fabricated successful trace |
| AC-10 | Array out-of-bounds or other supported runtime error | Correct partial trace and error location; no false successful write |
| AC-11 | Infinite loop/excessive output/oversized trace | Enforced limit, explicit terminal status, worker cleanup |
| AC-12 | Supported list/stack/queue/map operations | Correct contents, markers, operation ordering, and empty states |
| AC-13 | Binary-search-tree sort with supported node template | Correct construction, edges, traversal highlights, and sorted output without duplicating shared nodes |
| AC-14 | User-written heap sort | Recorded comparisons, swaps, and sift operations; synchronized array/tree views, active heap boundary, and correct sorted output |
| AC-15 | Edit source after execution | Old trace remains tied to original source or is clearly invalidated |
| AC-16 | Supported method calls and recursion | Correct arguments, return values, distinct frames, scope lifetimes, and backward playback across calls; explicit depth-limit outcome |
| AC-17 | Multiple observable operations on one source line | Each Step click advances one operation with matching source-expression highlighting and diagram state |
| AC-18 | Drag an existing diagram node during playback | Only position changes; connected lines follow; values, identities, references, and cursor remain unchanged |
| AC-19 | Print a prompt, read an integer through supported `Scanner`, print twice its value | Prompt appears before input even without newline; Enter sends input; execution resumes and output is correct; recorded diagram playback follows termination |
| AC-20 | Wait for input, then cancel, disconnect, reach a wait limit, or send EOF | Documented outcome, available partial trace, bounded resource usage, and worker cleanup; EOF and invalid tokens follow supported Java semantics |
| AC-21 | Replay a run that consumed console input | Forward/backward steps restore input-dependent state without sending input to a worker or asking for input again |
| AC-22 | Program uses only certain supported structures | Only structures from that program which exist at the playback position appear; no unrelated demonstration structures |
| AC-23 | Drag arrays and individual list/tree/heap nodes | Arrays move as groups; individual nodes move visually without changing logical order, values, relationships, or heap index mapping |
| AC-24 | Valid admitted single-file Java exceeds tracing coverage | Execute the original program once in isolation; show output and a clear visualization-unavailable diagnostic; distinguish execution success from visualization completeness |
| AC-25 | Missing recording could invalidate later diagram state | Stop playback at the last safe recorded boundary, explain incomplete capture, and do not guess later values, automatically rerun the program, or resubmit input |

For instrumentation tests, compare original and instrumented execution for supported programs, including final values, output, exception behavior, and side-effect counts. Test the reducer against expected intermediate states, not only final state. Include a browser-level test of the sorting example and meaningful worker-limit/isolation checks.

## 15. Deferred features

### Confirmed V2 roadmap

Complete V1 before implementing V2. The confirmed V2 direction is broader visualization of Java solutions to algorithm problems while retaining ordinary Main.java/Main.main authoring. This is a roadmap commitment, not a promise to visualize every Java program or every library implementation.

| V2 area | Expansion beyond existing V1 scope |
| --- | --- |
| Arrays | Broader array-problem coverage and representations beyond the initial one-dimensional subset; select exact types and matrix cases during V2 specification |
| Strings | Broader string-problem operations and suitable character/string visualizations; specify required methods and representations |
| Maps and sets | Broader map-problem coverage plus set visualization, including a defined Set/HashSet subset |
| Trees | General tree-problem coverage beyond binary-search-tree sort, with defined node conventions and operations |
| Heaps | General heap/priority-queue problem coverage beyond heap sort, including a defined PriorityQueue subset; never infer implementation layout from iteration order |
| Separate test inputs | Provide test inputs separately from source while still executing Main.java/Main.main; define format, delivery, and case handling before implementation |

Design V1 with explicit boundaries between analysis, instrumentation, recording, execution/input handling, trace reconstruction, and rendering. Add capabilities through focused operations/adapters/renderers and tests where possible. Avoid hard-coding a particular sample or sorting algorithm into shared contracts. Preserve verified V1 behavior, version incompatible contracts deliberately, and keep test-input handling separate from source transformation. Extensibility does not justify implementing V2 early or promise that no future refactoring will be needed.

These additions do not defer existing V1 features, including basic String support, maps, user-defined methods/recursion, live Scanner input, tree sort, or heap sort. Exact V2 library methods, algorithm examples, data types, and acceptance cases remain design work.

### Other deferred features

Multiple programming languages, AI-generated explanations, collaboration, authentication/accounts, saved cloud projects, algorithm complexity analysis, arbitrary third-party libraries, and public deployment are not required for the initial implementation. Future additions must reuse or deliberately version the established boundaries.

AI may assist development, but runtime visualization must be driven by verified execution data rather than an LLM guessing what the program does.

## 16. Historical initial setup prompt

The prompt below records repository initialization. It is not the current task or permission to restart Milestone 0. Resume from [current-state.md](../memory/current-state.md) and the user's approved scope.

```text
Read requirements/PROJECT_REQUIREMENTS.md and inspect this repository and any existing
AGENTS.md instructions. Start with Milestone 0 only: create the project
AGENTS.md, compatible focused agent definitions, specs, and concise memory
files described in the requirements. Distinguish confirmed requirements
from proposed technical decisions, and record unresolved choices.

Preserve existing files and unrelated changes. Do not implement the full
application yet. Plan the smallest Milestone 1 prototype that can validate
Java tracing correctness. Make extensibility, execution isolation, and
trace-contract consistency explicit in the instructions. Report the files
created and the next concrete implementation step.
```

## 17. Reference documentation

These references informed the architecture discussion. Recheck version-dependent configuration during implementation.

- [Codex AGENTS.md guidance](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
- [Codex custom agents](https://learn.chatgpt.com/docs/agent-configuration/subagents)
- [Monaco Editor](https://github.com/microsoft/monaco-editor)
- [Tree-sitter](https://tree-sitter.github.io/tree-sitter/)
- [JavaParser](https://javaparser.org/)
- [Java Debug Interface](https://docs.oracle.com/en/java/javase/22/docs/api/jdk.jdi/module-summary.html)
- [Java PriorityQueue contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html)
- [Docker Desktop WSL2 backend](https://docs.docker.com/desktop/features/wsl/)
