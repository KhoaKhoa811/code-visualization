# ADR 0008: Separate array-read and write steps in loop assignments

Status: proposed 2026-09-30; review/documentation authorized, implementation and stepping decision awaiting confirmation.

For the bounded `values[i] = values[i] + decimal-int-literal` loop body, propose one ARRAY_READ step followed by one ARRAY_WRITE step. The read highlights the RHS access and leaves the array unchanged. The write highlights the whole assignment and applies its recorded result; addition has no separate event in this increment.

This makes the input value visible before mutation, reuses existing draft-3 event shapes, and agrees with earlier array-access and bounded-arithmetic stepping. Combining everything into one write would hide the read; introducing an arithmetic event would expand the contract without a current requirement for that extra click.

Accept an array declaration followed by the loop without an unused scalar declaration. Introduce a distinct body-fact/operation-plan variant and explicit binding identities; preserve the earlier scalar-fill variant. Reuse the analyzer, lowering, recorder transport and collector/replay boundaries. This is not a general expression interpreter or arbitrary loop capability.

The [full proposal](../loop-array-read-proposal.md) defines the fifteen-step example, source scope, Java evaluation references, guard placement, failure/limit prefixes and proposed acceptance cases. No schema or runtime code changes are part of this decision draft.
