# First complete bubble-sort trace

Status: design proposal, 2026-10-04. The user approved reviewing merged PR #9 and preparing this documentation, not a schema change or runtime implementation. Milestone 1 remains incomplete. Proposed decisions below require review before implementation.

## Purpose and existing evidence

Compose the verified [classic loops](for-loop-array-recording.md), [array reads](loop-array-read-proposal.md) and [conditional swaps](compare-swap-proposal.md) into one user-written sorting example. The [conditional implementation](../runner/analysis/CONDITIONAL_RECORDING.md) passed its full gate in PR #9. That proves the separate operations, not their composition inside nested loops.

Current code has explicit gaps: `ConditionalProbe` accepts literal indices and a single if; `LoopProbe` accepts a single bounded loop body. `ConditionalTracePlan` has one fixed four/nine-operation path and fixed temporary identity. `LoopTracePlan` has one loop index/lifetime. Draft-3 conditions are loop decisions, while draft-4 conditions are IF comparisons. The shared collector currently caps traces at 32 events. None of these facts establish nested sorting support.

## Proposed source shape

```java
public class Main {
    public static void main(String[] args) {
        int[] values = {3, 2, 1};
        for (int i = 0; i < values.length - 1; i++) {
            for (int j = 0; j < values.length - 1 - i; j++) {
                if (values[j] > values[j + 1]) {
                    int temp = values[j];
                    values[j] = values[j + 1];
                    values[j + 1] = temp;
                }
            }
        }
    }
}
```

Accept this exact structural family: one int-array literal, two braced classic loops starting at zero, the bounds shown with their original expression structure, and one braced if with the three swap statements. Resolve every array, index and temporary reference to its declaration. Allow renamed bindings, comments/formatting, signed decimal int elements, lengths 0–16, and independently either `i++`/`++i` and `j++`/`++j`. The increments are standalone updates; their expression results are discarded. Parenthesized equivalent expressions may be excluded initially with a clear tracing diagnostic rather than silently generalized.

Optional trailing development probes retain exactly `System.out.print("FINAL=" + java.util.Arrays.toString(values));` and `System.err.print("PROBE");`, with verified JDK bindings and the renamed array. They observe results, not general output-call tracing.

Exclude early-exit flags, else, break/continue, labels, arbitrary bounds/starts/operators, additional statements, aliases, helper methods, embedded increments, calls in expressions and alternate sorting algorithms from this increment. These remain tracing limitations, not invalid Java. Ordinary single-file Java authoring and the approved isolated output-only policy remain unchanged; this task does not build that admission service. Broader V1 requirements remain intact.

## Observable operations

One accepted event remains one Step. Array reads are separate operations; scalar/index reads, array length, subtraction and `j + 1` are grouped with their consuming operation. No extra arithmetic, loop-entry, scope-entry or swap click is proposed.

| Source operation | Event and capture |
| --- | --- |
| Array declaration | ARRAY_DECLARE with original values and object identity |
| Each executed index declaration | VARIABLE_DECLARE with zero and a fresh runtime binding |
| Each outer/inner loop condition | CONDITION with FOR role, captured int operands, `<` and the actual boolean used to branch |
| Each array access in the if condition | ARRAY_READ with evaluated index and actual value; left before right |
| If comparison | CONDITION with IF role, captured read operands, `>` and actual boolean used to branch |
| Temporary initializer access | Separate ARRAY_READ, executed only on true |
| Temporary declaration | VARIABLE_DECLARE consuming that captured value |
| Right-hand array access in first assignment | Separate ARRAY_READ |
| Each assignment | ARRAY_WRITE with the committed value and evaluated target index |
| Each index update | VARIABLE_WRITE with the committed index value and actual prefix/postfix source highlight |

The taken if contributes eight operations after its loop condition: two reads, IF decision, initializer read, declaration, RHS read and two writes. A false if contributes three. Do not reuse condition reads as body reads: the original source executes both. Preserve target-reference/index evaluation before the RHS; guards run before evaluating the next operation, never only inside a helper whose arguments already have side effects. Compute a condition once, record it, and branch on that same boolean.

There are 15 static operation sites. Repeated executions reuse the site/range and receive new sequence numbers. Highlights use the exact submitted source hash, one-based UTF-16 positions and exclusive ends. Declarations/assignments exclude semicolons; conditions exclude for/if syntax; array reads highlight the whole access including `j + 1`. Synthetic variables and control flow receive no user binding or source step.

## Designed example checkpoints

These are independently calculated expectations, not execution evidence. For `[3,2,1]`, the intended trace has 41 events:

| Event(s) | Meaning | State after the last event |
| --- | --- | --- |
| 1–5 | Array, i=0, outer true, first j=0, inner true | [3,2,1], i=0, j=0 |
| 6–8 | Read 3, read 2, IF true | Array unchanged |
| 9–11 | Initializer read, temp=3, RHS read 2 | temp=3 |
| 12 | First store | [2,2,1], temp=3 |
| 13 | Second store and temp/branch retirement | [2,3,1], no temp |
| 14–18 | j=1, inner true, read 3/read 1/IF true | [2,3,1] |
| 19–23 | New initializer read and temp=3 through final swap store/retirement | [2,1,3], no temp |
| 24–28 | j=2, inner false/retirement, i=1, outer true, new j=0 | [2,1,3], i=1, j=0 |
| 29–37 | Inner true, read 2/read 1/IF true, swap and retirement | [1,2,3], no temp |
| 38–41 | j=1, inner false/retirement, i=2, outer false/retirement | [1,2,3], no indices |

The compressed ranges above must be expanded and checked by the later contract task. In particular, initializer reads and declarations remain different events. Backward from the last store of each swap restores both its previous array element and the retired temporary. Backward across an inner false condition restores that specific j instance, never an earlier pass's j. The final teaching snapshot retains the array; no main-frame teardown click is added.

For length n >= 2, let C=n(n-1)/2 comparisons and S be the actual number of taken swaps. Expected event count is `4n - 1 + 5C + 5S`. Sorted/equal length-three inputs take 26 events; reverse length-three takes 41; reverse length-four takes 75. Empty and singleton inputs take three events: array declaration, i declaration, false outer condition with retirement. Counts are acceptance expectations only; the runtime producer must never synthesize events from a formula or sorted-output prediction.

## Proposed draft-5 identities and control flow

Use a separately selected draft-5; preserve draft-1 through draft-4 schemas, fixtures, validators and producers. Do not relabel old traces. Shared contracts describe operations and lifetimes, not a BUBBLE_SORT event or an algorithm name. See [ADR 0010](decisions/0010-nested-sorting-trace.md).

Keep the envelope, source identity, terminal outcomes and accepted-event safe boundaries. Proposed event additions:

- CONDITION uses a discriminated FOR/IF role. Both carry typed int left/right operands, the allowed operator and a real boolean. IF uses `>` and links to its two preceding accepted ARRAY_READ sequences. FOR uses `<` and has no read links: scalar reads and computed bounds are grouped into this event. The source-plan collector checks captured bounds against current recorded indices and array length; replay uses the captured result.
- Distinguish lexical `scopeId` from fresh runtime `scopeInstanceId`. Every event carries both plus `parentScopeInstanceId` (null only for main). Parent instances must be live ancestors with source-plan-compatible lexical parents. The bounded hierarchy is main, outer loop, inner loop, taken branch. A new inner loop instance is created for each outer iteration; a new branch instance for each taken if. Loop body repetitions alone do not recreate that loop's index scope.
- Each declaration carries a static `declarationId` tied to the original analyzed declaration and a fresh `variableId` for that dynamic binding. Later writes/exits use variableId. Reusing the same declarationId in a new valid activation is allowed; reusing a retired variableId or scopeInstanceId is not. Names are labels, never identity keys. Keep one array object identity throughout this example.
- Every event carries `exitedVariableIds` and `exitedScopeInstanceIds`. Both lists are empty except at the specified normal exits. The final successful branch write retires its temp and branch instance; the false inner condition retires its j and inner instance; the false outer condition retires i and the outer instance. Validate the entire record before publishing values or exits. No live descendant may survive its parent's retirement.

A new scope instance first appears on an accepted event: the outer/inner index declaration or the taken branch's initializer read. Parent IDs make ancestry explicit without extra scope-entry events. The IF condition itself belongs to the current inner-loop instance; false creates no branch instance. A limit before the first body read therefore exposes no branch scope. Declaration ownership, control-flow placement and legal first events must agree with the source plan; generic schema validation alone cannot prove Java execution order.

Partial traces may end with an open branch before temp declaration, with temp live between stores, or with either loop index live. Do not invent normal exits during exceptions, cancellation, trace limits or corrupt/truncated records. These are snapshots at the last accepted operation, not live locals after process termination. Completion requires the final outer false condition, no live descendant scopes, and the existing trusted normal-end evidence. Reject premature end, missing loop/body operations and body after false.

This is a bounded lifetime model for a single main invocation, not a finished frame/recursion design. Later frame semantics must preserve these identity distinctions and may require another deliberate version.

## Analysis, recording and replay boundaries

Add immutable sorting facts with both loop headers/bodies, the IF body, 15 sites, resolved declarations/accesses, lexical parent relationships, bound/index expression shapes and update forms. Keep old fact variants unchanged. Validate exact source bytes and stale/missing/swapped facts before lowering.

Use a dedicated composition module consuming these facts, reusing tested source mapping, name allocation and operation capture where their semantics match. A nested source plan implements the existing `ArrayTrace.Plan` boundary and tracks outer/inner/branch phases, current runtime identities and pending captured reads. Never stretch the old fixed single-swap plan to accept arbitrary paths. Keep source analysis, lowering, recorder transport, validation and reconstruction separate. No second interpreter or general compiler framework is required.

Generate original Java operations with guards at all observable boundaries. Record actual evaluated indices/values/operands; never reevaluate an access or update for logging. Preserve the native arithmetic/assignment behavior and the same captured boolean for control flow. Collector consistency checks may calculate expected values for rejection; the producer and replay must not manufacture missing runtime facts. Replay reconstructs accepted prefixes and restores identities, array mutations, exits and selected attention without executing Java. No coordinates, colors, timings or component names enter the trace.

## Limits requiring separate implementation approval

The existing 32-event cap cannot complete the reverse length-three example. Propose a sorting-only profile of at most 128 events, retaining the 64-KiB source/generated-source and trace-byte caps, 16-element array cap and all current execution/worker deadlines, memory, CPU, process, output and cleanup limits. This is a proposed profile, not an active setting or permission to raise global defaults. Existing producers must keep their current 32-event behavior.

Recorder and collector must select the same trusted profile; submitted Java cannot choose larger limits. Byte limits still win if reached first. Prove length-three/four examples fit both caps with actual encoded traces before calling them complete. Longer arrays remain eligible but may yield a valid partial trace; reverse length-sixteen would need 1263 events and must stop under this profile. Keep fixture batches below 1 MiB by splitting batches, not raising the bound. Any inability to fit the canonical small examples needs a reviewed follow-up decision, not silent limit increases.

## Acceptance and delivery increments

1. **Contract first:** separately approve draft-5 schemas, designed complete/partial traces, validators and prefix-state expectations. Include scope/declaration reentry, distinct dynamic IDs, captured FOR/IF operands and atomic exits. Check all 41 example positions and every backward transition. Retain old-version byte and semantic compatibility.
2. **Bounded runtime composition:** separately approve analysis, lowering, recorder, source-plan collector, sorting-only event profile and prototype replay. Compare original/generated Docker execution for empty/singleton, sorted, reverse, duplicates, negative values/int extremes, length-four, renamed/formatted/collision sources, and all four prefix/postfix update combinations. Trace every executed original read once and use actual array indices. Check intermediate states, not only sorted output.
3. **Failure and limit proof within that runtime increment:** test event budgets before each canonical operation, byte-limit prefixes, cancellation/timeouts, malformed/truncated records and cleanup. Reject stale/incorrect operands/read links, wrong index instances, duplicate/reused IDs, invalid ancestry, use after retirement, wrong scope exits, swapped source sites and premature completion. The approved zero-based bounds cannot cause ordinary array-bounds failure; retain existing invalid-index regression cases and use malformed-trace tests without claiming they are executions of this source family. Verify broader excluded syntax receives tracing diagnostics.
4. **Milestone review after proof:** retain all existing 328 automatic and seven manual cases and their comparisons/replay checks. Review aliasing, remaining scalar/array syntax and side effects before declaring Milestone 1 complete. Browser sorting verification waits for the frontend; successful prototype sorting alone does not complete V1 or authorize app integration.

This documentation task changes no Java, schema, dependency, runtime limit or application behavior. Its checks are source/spec consistency, designed-count arithmetic, paths/fences, whitespace and a documentation-only diff. Runtime and browser acceptance remain unrun for sorting.
