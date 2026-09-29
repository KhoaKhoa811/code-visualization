# Frontend Design

## Status and visual reference

Initial specification recorded on 2026-09-15 during Milestone 0. This describes intended behavior, not an implemented interface. See [requirements](../requirements/PROJECT_REQUIREMENTS.md) and [architecture](architecture.md) for product scope and system boundaries.

The user attached a screenshot showing a dark desktop workspace: a grid visualization canvas on the left, editor upper right, and terminal lower right. Use its layout, dark appearance, boxes, and connecting lines as the visual reference. Its algorithm-specific calculation boxes are illustrative, not required features. The image itself is attached in the conversation, not saved as a repository asset; this description preserves its intent without assuming a local image path.

## Confirmed layout

Use a dark-only V1 interface:

```text
+--------------------------+-----------------------------+
| Visualization             | Java editor                 |
|                           |                             |
| Boxes/nodes and links     +-----------------------------+
| from recorded execution   | Live program console        |
|                           | Output and Scanner input    |
+--------------------------+-----------------------------+
```

Provide Run/Cancel and playback controls: play/pause, forward/backward Step, restart, speed, and current step. Exact control placement, panel proportions, typography, spacing, and colors remain design details.

The screenshot's C++, login, Premium, problem list, and solution-submission controls do not define our scope. Run manages Java compilation automatically; the console is program input/output, not a host shell.

## Structure visibility

- Show only supported structures from the submitted code that exist at the current recorded playback position. Do not add unrelated demonstration structures because their renderers are available.
- A declaration in an unexecuted branch does not create a runtime object. Visibility follows the specified scope and object lifetimes.
- Preserve aliases: multiple references resolve to one object identity.
- Presentation of multiple simultaneous structures, filtering controls, and unreachable objects remains to be specified. The user has not selected a tab or visibility-toggle system.

## Dragging behavior

| Structure | Interaction | Preserved behavior |
| --- | --- | --- |
| Array | Drag the array as one group | Cells remain together in index order |
| List | Drag individual displayed element nodes | Logical order and values stay unchanged |
| Tree | Drag individual nodes | Parent/child relationships and values stay unchanged |
| Heap | Drag individual tree-view nodes | Heap index mapping and synchronized array values stay unchanged |

Existing connected lines follow moved elements. Dragging changes readability only: it must not create structures, draw program links, reorder data, modify values, or advance playback. Heap array views retain array-group dragging. Precise dragging rules for standalone variables, stacks, queues, and maps remain unspecified.

Keep positions in frontend layout state, separate from execution events. Use stable object identities where available and documented presentation identities for value elements; repeated values cannot identify a node. List order must remain clear even after visual rearrangement. Do not imply uncaptured implementation pointers for a logical list view.

## Editor and execution

The selected stack is TypeScript, React, Vite, TanStack Query, Axios, and TanStack Router. Monaco remains the proposed editor; diagram and terminal libraries have not been selected.

1. Capture the exact source version when the user clicks Run.
2. Display validation diagnostics or run status.
3. During execution, deliver live console output and accept supported Scanner input.
4. After termination, load the available trace, including valid partial traces for failed, cancelled, or limited runs.
5. Each Step advances one observable operation with synchronized source highlighting and diagram state.

Old traces must not silently highlight newly edited source. Live diagram stepping while execution is in progress is outside confirmed V1 behavior.

## Live console

- Display stdout/stderr incrementally, including prompts without a newline.
- Pressing Enter sends entered text to the active program for supported Scanner reads.
- Show input waiting and allow cancellation. Specify EOF, disconnect, invalid input, and input limits in the Java support and run API contracts.
- Display explicit terminal outcomes and available partial results.
- Replay uses recorded input-dependent state; it never resubmits input or asks the user to enter it again.

Console transport is confirmed in [ADR 0001](decisions/0001-live-console-websocket.md): native browser WebSocket with Spring WebSocket handlers for live output/input/EOF and notifications. Axios/TanStack Query handles HTTP run creation, cancellation, status, and completed results. Console transcript playback, Clear behavior, message schemas, acknowledgement, bounded buffering, connection recovery, and exact supported Scanner methods remain unresolved.

## Shared Error and Limitation Panel

Confirmed on 2026-09-18: use one shared panel for compilation errors, runtime errors, and visualization limitations. Follow the four approved cases in [the trace specification](trace-format.md). Reusing a panel does not merge their meanings: identify the category, explain the issue, and show execution outcome separately from visualization completeness.

- Compilation errors show the actual compiler diagnostic and reliable source location; this attempt has no playback.
- Runtime errors show the exception and available safe recorded steps. A failed operation must not appear successfully completed in the diagram.
- Visualization-unavailable results retain console output and explain the tracing limitation, including the construct and location when known. Do not label valid Java wrong or show a fabricated diagram.
- Partial visualization permits playback only through the safe recorded boundary and explains why it stops. Clearly distinguish the last recorded state from the program's final state.

Highlight the relevant original source when a reliable location exists. Keep run/source association intact after edits and across multiple attempts; never apply an old diagnostic to changed code silently. If no location is reliable, show the explanation without inventing a highlight. Error-location highlighting must not silently move the playback cursor or imply that a failed operation produced a recorded mutation.

Console output stays in the console. The panel does not automatically rerun code, resubmit input, or replace trusted backend outcomes. Execution-policy restrictions, cancellation, limits, and engine failures remain distinct when presented through this surface. Exact panel placement, dimensions, styling, message wording, and multiple-diagnostic interaction remain design details.

## State ownership

Under [ADR 0002](decisions/0002-java-execution-and-visualization-coverage.md), execution outcome and visualization completeness are separate. Show compiler errors, runtime exceptions, execution-policy restrictions, and visualization limitations distinctly, with locations when reliable. An admitted output-only run shows console output and an explanation that visualization is unavailable. Partial playback stops at a safe recorded boundary and explains missing coverage; it must not display guessed later state or cause an automatic rerun.

Keep editor/source state, server/run state, reconstructed playback state, and layout state separate. Axios supplies HTTP transport, TanStack Query manages server state, and TanStack Router manages navigation. The proposed reducer reconstructs values, references, frames, and highlights. Coordinates never belong in runtime trace events.

Select rendering tools for these interactions rather than adopting a full drawing editor with unrelated features.

## Proposed conveniences and open decisions

Resizable panels, canvas pan/zoom, Reset layout, and visibility toggles were suggested but not confirmed. Layout persistence across stepping, disappearing/reappearing objects, restart, new runs, and reloads remains open. Discuss these before treating them as requirements.

## Acceptance checks

- Dark desktop layout follows the reference intent without unrelated screenshot controls or calculation diagrams.
- A program containing only an array does not show unrelated list/tree/map examples. Structures appear at appropriate playback steps.
- Array dragging keeps cells together; list/tree/heap node dragging preserves logical order, relationships, values, and index mapping.
- Connected lines follow moved elements. Forward/backward replay remains correct after dragging, including aliases and repeated values.
- A prompt appears before input; supported Scanner input resumes execution and produces output.
- Diagram playback starts after termination and does not request recorded input again.

These supplement requirements AC-18 through AC-23. All browser checks remain planned; no frontend implementation exists yet.
