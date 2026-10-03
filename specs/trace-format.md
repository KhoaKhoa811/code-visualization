# Recorded Trace Contract

2026-10-03 contract update: after PR #7 merged, the user approved continuing with the conditional contract. [Draft-4](../contracts/run-result-v4.schema.json) and its [validation guide](../contracts/README.md) now cover captured IF comparison operands/read links, lexical scopes and atomic branch-local retirement. Nine designed fixtures, 54 rejection cases, nine partial prefixes and source/replay checks passed alongside older suites. No Java conditional producer, source-plan collector or runtime comparison tests are implemented by this contract task. Earlier proposal-only statements below are historical.

2026-10-03 design proposal only: [conditional compare-and-swap](compare-swap-proposal.md) proposes literal-index array reads, one captured comparison/if decision, and a three-statement swap with a scoped temporary. True/false paths have nine/four steps. [ADR 0009](decisions/0009-conditional-comparison-trace.md) proposes separate draft-4 semantics; all existing contracts and runtime support remain unchanged. Design and implementation require further approval.

2026-10-01 runtime update: the [bounded read/addition loop](../runner/analysis/LOOP_READ_RECORDING.md) uses existing draft-3 ARRAY_READ and ARRAY_WRITE as separate steps. The read carries the actual RHS value and leaves the array unchanged; the write carries the committed native-int result. Collector validation checks condition/read/write/update order and consistency; replay never recomputes arithmetic. The fifteen-step example, limits between read and write, exact RHS/assignment source ranges and forward/backward prefixes passed the 80-case new runtime suite. All schemas and older fixtures remain unchanged; the guide records full regression-gate status.

2026-09-30 runtime update: the approved bounded loop producer now emits draft-3 conditions, declaration scopes and atomic index retirement. The collector validates condition/store/update order and consistency against captured bindings; replay applies recorded values/exits without rerunning Java. The 68 runtime loop cases, 34 prefix/postfix pairs and forward/backward checks passed. See [the implementation guide](../runner/analysis/LOOP_RECORDING.md) for current full-gate status. Draft-1/draft-2 remain unchanged. Earlier contract-only and pending-producer statements below are historical.

2026-09-29 contract update: user approved draft-3 schema, illustrative fixtures and validator work. [The contract guide](../contracts/README.md) records passing legacy checks, six new fixtures, 44 negative cases, twelve partial prefixes and fixture source/cursor checks. Draft-1/draft-2 schemas and original fixtures are unchanged. No Java loop implementation or Docker tests were performed. Earlier specification-only status below is historical; runtime implementation still requires approval.

2026-09-28 proposal: [the first classic for loop](for-loop-array-recording.md) needs recorded boolean CONDITION events and atomic scope-exit effects. [ADR 0007](decisions/0007-loop-condition-and-scope-draft-3.md) proposes draft-3 with declaration/condition scope IDs and an exitedVariableIds list on each event; the final false condition retires the loop index in the same click. One event remains one step, and both safe-boundary fields remain the accepted event count. Draft-1/draft-2 stay unchanged. This is documentation only: no draft-3 schema, validator or producer is implemented or verified.

2026-09-28 verified: [standalone index increments](index-increment-array-recording.md) reuses one VARIABLE_WRITE for either `i++` or `++i`, recording the committed variable value and highlighting the whole original unary expression. No separate read, addition or discarded-result event is proposed. Both forms retain the five-operation sequence and draft-2; embedded expression-result semantics and loop execution remain deferred. Implementation, exact source associations, committed-value capture and independent forward/backward reconstruction passed the 150-case gate, including 20 prefix/postfix pairs. Draft-2 remains unchanged.

2026-09-26 verified increment: [index addition](index-addition-array-recording.md) retains the existing five-event sequence when the index assignment is `i = i + decimal-int-literal`. One VARIABLE_WRITE captures the committed Java int result and highlights the whole assignment; no separate scalar-read or addition event is introduced. Replay consumes that recorded result without evaluating the expression. Wrapped values, safe prefixes and independent restoration are verified. This bounded rule does not settle stepping for arbitrary nested expressions. Draft-2 needs no new fields.

2026-09-26 index-update increment: [index update before an array write](index-update-array-recording.md) composes five draft-2 events: VARIABLE_DECLARE, ARRAY_DECLARE, VARIABLE_DECLARE, VARIABLE_WRITE and ARRAY_WRITE. The scalar write targets the existing index binding variable-3; value binding variable-1 is unchanged. The operation-plan collector checks the final store against the latest recorded index and value. No scalar-read event or schema field is added. Runtime verification status is recorded in the scope and memory.

2026-09-25 variable-index increment: [variable-index recording](variable-index-array-recording.md) reuses draft-2 for two distinct VARIABLE_DECLARE events around ARRAY_DECLARE, then ARRAY_WRITE. The value/index bindings have separate IDs and original sites. ARRAY_WRITE records the evaluated integer index and stored value; no scalar-read event is added. Operation-specific source associations and bounded binding state belong to analysis/collection, not new wire fields. Legacy IDs and versions remain unchanged.

2026-09-24 scalar-update increment: [scalar update before an array write](scalar-update-array-recording.md) composes existing draft-2 events as VARIABLE_DECLARE, ARRAY_DECLARE, VARIABLE_WRITE, ARRAY_WRITE. VARIABLE_WRITE changes the existing scalar binding; the array store captures its current value. No schema change is needed. The bounded collector checks the expected source sites, operation order and latest scalar value; these source-specific checks remain outside the general result-schema validator.

2026-09-24 increment: [combined scalar/array recording](combined-variable-array-recording.md) uses existing draft-2 fields for VARIABLE_DECLARE, ARRAY_DECLARE and ARRAY_WRITE. Scalar variable-1 and array binding variable-2 are distinct; array-1 is the object identity. Separate legacy array/scalar fixtures retain their prior versions and identities. This adds no new schema fields or general frame/alias semantics.

2026-09-23 increment: [integer variable recording](integer-recording.md) introduces draft-2 with VARIABLE_DECLARE and VARIABLE_WRITE while retaining draft-1 array compatibility. Both events use variableId, typed int value and original source; declaration also has variableName. They contain no arrayId. One committed operation is one step. Declare before write, reject duplicate identities and wrong binding kinds, and reconstruct backward from recorded facts. Full scope/frame lifecycles remain pending.

Status: Design draft, initially 2026-09-18, updated 2026-09-21. The user approved the example, conceptual event information, backward reconstruction, and separation of run results from events. A narrow draft-1 schema and controlled runtime experiment now exist, as described below; broader event kinds, lifecycle semantics and the full V1 protocol remain unfinished.

## Purpose and Boundaries

Update 2026-09-21: the [controlled array experiment](array-recording-experiment.md) now produces and validates these three event kinds from reviewed manual source pairs. It verifies bounded incremental collection and test-only prefix reconstruction. This is runtime evidence for that region, not an automatic transformer, full V1 protocol, or production playback engine. Canonical wire encoding and transport markers are documented in [ADR 0004](decisions/0004-controlled-array-trace-pipe.md); the event/result schema remains draft-1.

Update 2026-09-19: a narrow [draft-1 schema, validator, and four fixtures](../contracts/README.md) now exist. They make the current array examples mechanically checkable, not a finalized V1 protocol. The inline JSON illustrations below are not complete schema-valid payloads; use contracts/examples/ for complete fixture inputs. Draft-1 currently requires console and safePlaybackBoundary on all results.

The trace connects recorded Java execution to deterministic visualization. Follow the [requirements](../requirements/PROJECT_REQUIREMENTS.md), [architecture](architecture.md), [Java-support and stepping decisions](java-support.md), and [execution/visualization policy](decisions/0002-java-execution-and-visualization-coverage.md).

Runtime facts come from execution, not source-code guesses. Instrumentation produces records, the runner collects and validates them, and playback reconstructs state. The frontend/editor applies the original-source range selected by playback. The architect coordinates shared changes.

Console output and input remain separate from trace records. Diagrams play back after a terminal outcome; replay never reruns Java or resubmits input. Trace data contains no screen coordinates, colors, animation timing, or React component names.

## Agreed Example

```java
public class Main {
    public static void main(String[] args) {
        int[] values = {3, 1};
        values[0] = values[1];
    }
}
```

| Position | Recorded operation | Logical state after the operation | Highlight |
| --- | --- | --- | --- |
| Initial | Before the example declaration | No values binding or array yet | None |
| Step 1 | Create array and bind values | values references array-1 containing [3, 1] | Original declaration |
| Step 2 | Read array-1 at index 1; result is integer 1 | Array remains [3, 1] | Original values[1] expression and cell 1 |
| Step 3 | Write captured integer 1 to array-1 at index 0 | Array becomes [1, 1] | Original destination/write range and cell 0 |

The exact write-expression range remains a source-mapping decision. The read result is captured during Java execution; playback does not evaluate values[1] to recover it. Array creation and binding form one user-visible step for this literal declaration, even if several internal records are needed.

The example covers these three observable operations. Main-frame startup, scope exit, and run termination still need explicit bookkeeping placement; do not infer additional clicks or silently decide end-of-run object visibility from this table.

## Agreed Information per Operation

| Concept | Meaning |
| --- | --- |
| Execution order | Position in the ordered recording; preserve actual operation order |
| Original source range | Source file/version and expression location used for highlighting |
| Target identity | Stable variable binding or object identity affected by the operation |
| Recorded facts | Typed values, indices, results, or state changes sufficient to reconstruct the operation |

Run identity, schema version, and exact submitted-source identity belong to the associated result/trace context. Whether they are repeated on every event is a serialization decision. Event kind names and sequence-number bases are not finalized here.

Distinguish internal record order from the observable-step cursor. Multiple bookkeeping records may support one observable operation. A Step must not expose unexplained bookkeeping or skip agreed operations. Follow the approved classic-loop, while, break/continue, and grouped array-for-each rules in the Java-support specification; do not redefine them in a consumer.

### Stable Identities and Typed Values

The identifier array-1 is illustrative, not a required naming format. Variables have identities separate from their names; objects have identities separate from the variables referencing them. Another binding referring to array-1 shares the same array state rather than creating a copy.

Scope and recursive-frame identities remain required for later V1 support. Define them before those features are implemented. Do not identify bindings solely by display name or confuse static declarations with recursive runtime instances.

Values must distinguish primitive types, strings, null, and object references. Preserve Java numeric fidelity, including large integers and supported special floating-point values when tracing expands to them. The exact typed-value encoding remains unresolved; do not silently coerce unsupported numeric representations into inaccurate JavaScript numbers.

### Source Association

Every user-visible operation must retain its association with the exact submitted source. Editing source must not apply old ranges to new text. Analysis supplies original ranges; instrumentation preserves their associations; playback passes the selected range to the editor.

Specify line/column bases, coordinate units, end-boundary convention, Unicode, and CRLF handling before implementation. Synthetic-only records must not acquire fabricated user-source locations. Diagnostics without a reliable location must say so.

## State Reconstruction and Backward Playback

Reconstruct logical state from the initial state and an ordered prefix of recorded facts. In the example, moving backward from Step 3 to Step 2 restores [3, 1] and the read highlight. No Java execution occurs. Returning to Step 1 retains [3, 1] but selects the declaration highlight; returning before Step 1 removes the example binding and object from the reconstructed view.

This design does not require inverse Java execution or a previousValue field on every mutation. Exact record payloads remain to be specified. Optional snapshots may later optimize seeking only if they reproduce the same state as replay from the beginning.

Keep logical program state, operation highlights, and diagram positions separate. Dragging changes only presentation. Layout persistence across cursor changes remains a frontend decision, not a trace fact.

## Run Result Separate from Events

The run result describes the attempt and recording coverage; events describe observed operations. Proposed conceptual result contents are:

- Run identity and exact source identity.
- Execution outcome, such as completion, failure, cancellation, or an enforced limit, with diagnostic provenance.
- Visualization coverage: complete, partial, or unavailable, with an explanation when needed.
- Trace schema/version and available records or a reference to them.
- A safe playback boundary and limitation information when capture is partial.
- Associated console results and diagnostics through their separate interfaces.

These are concepts, not approved JSON names, enums, routes, or storage choices. Execution success does not imply a complete visualization. A successful admitted output-only run can have unavailable visualization. A runtime exception can have usable recorded steps. Exact combinations, failure categories, and the definition of complete coverage for a failed run remain contract work.

### Partial and Unavailable Visualization

Follow ADR 0002: stop playback before uncaptured effects could invalidate reconstructed state. Preserve a usable safe prefix when available; do not guess later values, silently skip corrupt records, or reconnect to later events as if the missing changes never occurred. If no safe state can be established, visualization is unavailable.

An output-only run must not fabricate an empty successful recording. Distinguish no visualization from a valid trace with zero observable operations. Reaching the last playable step does not change the backend's execution outcome or prove recording completeness.

The runner selects an admitted original-source output-only path before executing user code when tracing ineligibility is known. Recorder failure during execution does not authorize automatic rerunning, input replay, or merging attempts. Exact safe-boundary encoding and validation rules remain unresolved.

## Approved Error and Visualization-Limitation Cases

The user approved these behaviors on 2026-09-18. They define observable results, not finalized diagnostic fields, status enums, exception events, or UI styling. Display explanations in the shared [frontend diagnostic panel](frontend-design.md), with source highlighting when a reliable location is available.

| Case | Execution and diagnostics | Available playback |
| --- | --- | --- |
| Compilation error | Show the compiler's diagnostic and source location; no user-program execution | No playback for this attempt |
| Runtime error | Show the actual exception and reliable failing source location | Allow safe recorded steps; never invent a successful failed operation |
| Visualization unavailable | Admitted valid source runs in isolation through the original-source path selected before execution; show output and explain the tracing limitation | No fabricated diagram or playable trace |
| Partial visualization | Show actual execution outcome separately from the explanation of incomplete recording | Allow only the safe recorded prefix; stop before missing facts could invalidate state |

### Compilation Error Example

```java
int x = ;
```

This fragment, placed inside the approved main wrapper, is a compiler-error case. Display the compiler's actual message and location rather than inventing exact wording. No user-program execution or trace playback occurs for this attempt. Any older run retained by the UI must remain clearly associated with its own source and outcome.

### Runtime Error Example

```java
int[] values = {3, 1};
int x = values[5];
```

For this fragment inside main, show the recorded array declaration and the ArrayIndexOutOfBoundsException at the failing access. Do not emit a successful array read or display x as having an assigned value. The array remains [3, 1] at the last safe state. Exact exception-event encoding and whether presenting the exception consumes a separate Step remain unresolved.

### Visualization Unavailable

When an admitted program exceeds tracing coverage, show its console output and a clear limitation explanation. Suggested wording:

> Visualization unavailable: this program uses a construct we cannot trace yet.

Name the construct and identify its original-source location when known. Do not blame valid Java or imply execution failed solely because tracing is unavailable. Original-source fallback is selected before execution; never automatically rerun a program that already started, duplicate input, or merge attempts. Existing isolation and execution restrictions remain in force.

### Partial Visualization

When a trustworthy prefix exists but later effects are not fully captured, allow playback only through the last safe step. Suggested wording:

> Visualization stops here because subsequent execution was not fully recorded.

Report the concrete cause when available. Show the actual execution outcome independently: incomplete visualization does not prove that the program failed or completed successfully. Do not present the last recorded state as the program's final state, invent later values, or resume across the missing effects. If there is no safe prefix, report visualization as unavailable instead.

Cancellation, resource limits, execution-policy rejection, and engine failures retain their existing distinct diagnostics. These four examples do not replace those outcomes or define every combination of execution result and visualization coverage. Machine-readable diagnostics and safe-boundary fields remain future schema work.

## Proposed Event and Result Shape

The user approved adding this illustrative shape on 2026-09-18. Names, status strings, coordinate conventions, and serialization below remain proposals, not a finalized schema or generated execution evidence.

| Proposed event kind | Recorded facts |
| --- | --- |
| ARRAY_DECLARE | Variable identity/name, array identity, and typed initial contents |
| ARRAY_READ | Array identity, index, and typed value actually read |
| ARRAY_WRITE | Array identity, index, and typed value successfully written |

Each event has sequence and source fields. In this example, sequence starts at 1 and each event corresponds to one observable step. This does not require every future bookkeeping record to create a step. ARRAY_DECLARE combines allocation and binding for the literal declaration only; alias binding must not allocate a second array. Allocation/binding may later be represented by separate internal records while retaining one click.

The proposed result fields are runId, sourceId, schemaVersion, executionOutcome, visualizationCoverage, diagnostics, and events. sourceId identifies the exact submitted snapshot; it is not merely a filename. IDs below are illustrative. The schemaVersion string deliberately denotes a draft. This example has no diagnostics and includes no console payload; console delivery remains separate.

### Illustrative JSON

For this example only, source coordinates use one-based lines/columns, UTF-16 column units, and exclusive end positions. Lines refer to the Java code block above without Markdown fences. These conventions require approval and Unicode/CRLF fixtures before implementation. The write highlights the full assignment expression without its semicolon; the read highlights the right-hand array access.

```json
{
  "runId": "run-example-1",
  "sourceId": "source-snapshot-example-1",
  "schemaVersion": "draft-1",
  "executionOutcome": "completed",
  "visualizationCoverage": "complete",
  "diagnostics": [],
  "events": [
    {
      "sequence": 1,
      "kind": "ARRAY_DECLARE",
      "source": {
        "file": "Main.java",
        "start": { "line": 3, "column": 9 },
        "end": { "line": 3, "column": 31 }
      },
      "variableId": "variable-1",
      "variableName": "values",
      "arrayId": "array-1",
      "values": [
        { "type": "int", "value": 3 },
        { "type": "int", "value": 1 }
      ]
    },
    {
      "sequence": 2,
      "kind": "ARRAY_READ",
      "source": {
        "file": "Main.java",
        "start": { "line": 4, "column": 21 },
        "end": { "line": 4, "column": 30 }
      },
      "arrayId": "array-1",
      "index": 1,
      "value": { "type": "int", "value": 1 }
    },
    {
      "sequence": 3,
      "kind": "ARRAY_WRITE",
      "source": {
        "file": "Main.java",
        "start": { "line": 4, "column": 9 },
        "end": { "line": 4, "column": 30 }
      },
      "arrayId": "array-1",
      "index": 0,
      "value": { "type": "int", "value": 1 }
    }
  ]
}
```

Applying these events yields [3, 1], [3, 1], and [1, 1]. Replaying only the first two restores [3, 1]. ARRAY_READ records the captured value without changing the array. ARRAY_WRITE applies the recorded committed value; it does not reevaluate the source expression.

This deliberately simplified illustration omits frame/scope setup and teardown, which still need specification before a production complete trace can be claimed. The complete coverage label demonstrates the intended result shape for this example, not proof that the draft models all lifecycle records. It does not decide whether bindings remain visible after main exits.

Do not reuse this inline example as a complete schema-valid result. The draft-1 fixture contract additionally requires console and safePlaybackBoundary. It uses null for unavailable events/boundaries and excludes zero-operation recordings pending specification. Broader value encodings, method frames, aliases, and lifecycle compatibility remain unresolved.

## Diagnostics and Safe Playback Boundary

The user approved recording these concepts on 2026-09-18. A diagnostic explains what went wrong or cannot be visualized. A safe playback boundary identifies the last trustworthy observable step the frontend may display. Exact JSON field names and serialization below remain proposals.

| Proposed diagnostic field | Meaning |
| --- | --- |
| category | Distinguish compilation error, runtime error, visualization limitation, and other agreed diagnostic categories |
| message | Explain the concrete issue or limitation in plain language |
| source | Reliable original-source location when known; null in this proposed representation when unavailable |

Diagnostics belong to the run/source context already carried by the result. A missing location does not prevent showing the explanation. Never invent a location, treat valid Java as wrong because tracing failed, or infer execution failure solely from a visualization-limitation category. Full category names and compiler/exception detail fields remain schema work.

The proposed safePlaybackBoundary contains lastSafeStep and lastSafeEventSequence. The first limits user-visible navigation; the second identifies the inclusive recorded prefix needed to reconstruct that step. They happen to match in the simple example, but bookkeeping can make them differ. Before implementation, define and validate their mapping so a boundary cannot split the records required for an observable operation.

The boundary must come from validated capture/collection evidence under the shared contract, not a browser guess based on the last event received. Do not advance beyond it or resume at later events across missing effects. The last safe state is a recorded state, not a claim about the program's final state.

### Illustrative Partial-Result Metadata

Suppose the following two operations have been recorded reliably inside main:

```java
int[] values = {3, 1};
values[0] = 7;
```

Step 1 creates [3, 1]; Step 2 writes index 0 and produces [7, 1]. Assume a later operation cannot be captured safely. This is a hypothetical partial-capture scenario, not a claim that this fragment itself causes a tracing limitation.

```json
{
  "runId": "run-partial-example-1",
  "sourceId": "source-partial-example-1",
  "schemaVersion": "draft-1",
  "executionOutcome": "completed",
  "visualizationCoverage": "partial",
  "diagnostics": [
    {
      "category": "visualization_limitation",
      "message": "Visualization stops after step 2 because subsequent execution was not fully recorded.",
      "source": null
    }
  ],
  "safePlaybackBoundary": {
    "lastSafeStep": 2,
    "lastSafeEventSequence": 2
  }
}
```

This JSON is a metadata excerpt, not a complete serialized run result: its two trace events and console output are intentionally omitted. The sequence values assume one event per step solely for this illustration. completed is hypothetical and may be reported only when the trusted runner establishes that outcome; partial recording does not prove completion. The null diagnostic source avoids inventing a location for the unspecified later operation.

The UI permits forward/backward playback through Steps 1 and 2, displays the explanation, and does not claim [7, 1] is the final array. Existing restart behavior remains available; exact initial-position encoding is unresolved. The frontend must not infer that later unchanged-looking values make continued playback safe.

### Output-Only and Empty-Trace Distinction

For output-only execution, visualization is unavailable and there are no playable steps or safe-step claim. Show console output and the diagnostic. Draft-1 uses null for events and safePlaybackBoundary in this case. A genuinely recorded run with zero observable operations is separate and not yet modeled by this narrow schema; do not equate it with unavailable visualization or assign a fictitious Step 0 to mean both.

This proposal does not authorize running past recorder failures, silently executing an uninstrumented remainder, or rerunning after partial execution. Apply ADR 0002: use a safe prefix only when it exists; known tracing ineligibility can instead select an admitted output-only run before execution begins.

## Remaining Decisions Before Implementation

- Concrete event kinds, payloads, schema versioning, ordering rules, and observable-step grouping.
- Source-coordinate and typed-value encodings; scope, binding, object, and frame identities.
- Initial/final bookkeeping, mutation commit semantics, failure/exception events, and output-call stepping.
- Validate the proposed diagnostic/boundary fields, observable-step-to-event mapping, initial/empty/unavailable cases, malformed/incompatible record handling, and result-status combinations.
- Concrete trace bounds, transport, retention, and cancellation/termination interactions with the runner contract.
- Extend the existing narrow draft schema and fixtures under contracts/ when the remaining semantics are agreed; they do not yet cover full V1 behavior.

Use the agreed example as an initial fixture when contract work is approved. Subsequent fixtures should cover aliases, loops, errors, source edits, backward playback, and output-only results. This document does not finalize those schemas or authorize their implementation.

## Verification Status

The draft schema and validator now check the four synthetic fixtures plus positive/negative cases. This is structural and semantic fixture validation only. No Java compilation, trace production, production state-reducer tests, or browser checks have run. General V1 semantics remain unfinished.
