# ADR 0009: Conditional comparison and swap steps

Status: proposed 2026-10-03; awaiting design approval. Documentation creation is approved, implementation is not.

For the first bounded conditional swap, propose two ARRAY_READ steps followed by one CONDITION carrying captured int operands and the boolean branch decision. The body records its own read/declaration/read/write/write operations. A taken branch has nine steps; a false branch has four. Attach branch-local temp retirement to the final successful body write, without a bookkeeping click or fabricated exit after failure.

Draft-3 assumes a loop-local index and retires it on false. Reusing that meaning for if would break existing consumers. Propose separately selected draft-4 with explicit IF condition role, captured comparison operands/read links, lexical scope on each event and the bounded write-associated exit rule. Preserve all older schemas and producers. This avoids duplicate comparison/branch clicks while retaining both runtime operands required for educational comparisons.

Alternatives: a separate COMPARE plus CONDITION would duplicate the same boolean decision in this bounded shape; grouping all reads and the decision in one event would hide already-established array-read steps. Generic scope events would require grouped-step semantics prematurely. These decisions cover this specific shape only, not arbitrary boolean expressions or nested-scope teardown.

The [proposal](../compare-swap-proposal.md) defines syntax, payloads, source sites, exceptions, limits, module responsibilities and acceptance criteria. Implement the machine-readable contract before its producer, subject to separate user approval. No sorting or frontend functionality is delivered by this decision.
