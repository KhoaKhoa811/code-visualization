# ADR 0010: Nested sorting conditions and runtime lifetimes

Status: proposed, 2026-10-04. Documentation approved after PR #9 merged; contract/runtime implementation requires separate confirmation.

The first complete [bubble-sort example](../bubble-sort-proposal.md) combines nested classic loops with repeated conditional swaps. Existing draft-3 loop and draft-4 IF contracts cannot express this composition without changing their meanings. A static declaration/source site also cannot identify successive j or temp lifetimes.

Propose separate draft-5 with FOR/IF condition roles, captured operands, explicit lexical and runtime scope identities with parent instances, static declaration identities, fresh dynamic variable identities and atomic binding/scope exit lists. Reentering a declaration creates new runtime identities. Conditions and writes retain one-event/one-Step semantics. No separate scope bookkeeping click, atomic SWAP or algorithm-specific trace event is introduced.

Keep source-specific ordering in a nested operation plan behind `ArrayTrace.Plan`; generic contract checks validate data/identity consistency. Replay applies accepted facts without recomputing comparisons or Java control flow. Preserve all older schemas and producers. This uses existing module boundaries while making repeated lifetime ownership explicit.

Alternatives rejected for this proposal: reopening old runtime IDs makes backward playback ambiguous; globally redefining scopeId breaks existing consumers; encoding bubble sort as a synthetic swap or sorting event hides user operations. A general interpreter/control-flow engine is unnecessary for the bounded source family.

The canonical reverse three-element trace needs 41 events, exceeding the current 32-event prototype cap. Propose an explicitly selected sorting-only 128-event profile, retaining 64-KiB trace/source caps and all isolation limits. Existing profiles remain unchanged. The implementation must demonstrate small complete traces fit the byte cap and larger runs stop safely. This proposal does not itself raise any limit.

Contract and designed replay fixtures should precede runtime composition. Methods/recursion, aliases, general conditions and browser integration remain separate work; this decision does not close Milestone 1.
