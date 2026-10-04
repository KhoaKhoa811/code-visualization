# ADR 0009: Conditional comparison and swap steps

Status: accepted; contract implementation verified, and bounded runtime implementation approved after PR #8 merged. [Draft-4 checks](../../contracts/README.md) cover designed traces; the [runtime guide](../../runner/analysis/CONDITIONAL_RECORDING.md) records Java producer/collector verification separately.

For the first bounded conditional swap, propose two ARRAY_READ steps followed by one CONDITION carrying captured int operands and the boolean branch decision. The body records its own read/declaration/read/write/write operations. A taken branch has nine steps; a false branch has four. Attach branch-local temp retirement to the final successful body write, without a bookkeeping click or fabricated exit after failure.

Draft-3 assumes a loop-local index and retires it on false. Reusing that meaning for if would break existing consumers. Propose separately selected draft-4 with explicit IF condition role, captured comparison operands/read links, lexical scope on each event and the bounded write-associated exit rule. Preserve all older schemas and producers. This avoids duplicate comparison/branch clicks while retaining both runtime operands required for educational comparisons.

Alternatives: a separate COMPARE plus CONDITION would duplicate the same boolean decision in this bounded shape; grouping all reads and the decision in one event would hide already-established array-read steps. Generic scope events would require grouped-step semantics prematurely. These decisions cover this specific shape only, not arbitrary boolean expressions or nested-scope teardown.

The [proposal](../compare-swap-proposal.md) defines syntax, payloads, source sites, exceptions, limits, module responsibilities and acceptance criteria. The machine-readable contract preceded its separately approved producer. No sorting or frontend functionality is delivered by this decision.

Implementation boundary, 2026-10-04: conditional analysis and lowering use separate modules and immutable source-bound facts. The collector shares only an `ArrayTrace.Plan` interface (`accept` and `complete`) with the existing loop plan, so source-specific path validation does not alter older trace meanings. A separate recorder extension reuses bounded transport without changing older recorder templates. Replay applies accepted runtime facts; it does not execute comparisons or infer branch paths.
