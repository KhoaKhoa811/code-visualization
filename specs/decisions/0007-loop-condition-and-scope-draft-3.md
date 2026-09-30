# ADR 0007: Loop conditions and atomic scope exit in draft-3

Status: proposed 2026-09-28; accepted and implemented at the contract layer with user approval on 2026-09-29. The subsequent approved runtime implementation passed 68 loop cases/40 original-generated comparisons and source/replay checks on 2026-09-30. See [the implementation guide](../../runner/analysis/LOOP_RECORDING.md) for full regression status.

## Context

The first classic for loop needs a recorded boolean condition, repeated visits to the same operation sites and removal of its loop-local index on normal exit. The approved stepping rules give the final false condition one click and forbid a separate scope-bookkeeping click. Draft-2 has neither condition events nor scope effects, and its event union is closed.

## Decision

Add a separately selected draft-3 contract, preserving the result envelope and existing versions. Add CONDITION with a typed boolean result and scopeId; add scopeId to declaration payloads; and attach exitedVariableIds to each draft-3 event. The exit list is empty except on the final false condition, where the index binding is retired atomically with the condition result. One record remains one step, so both safe-boundary fields continue matching accepted event count.

Retain draft-1/draft-2 schemas, fixtures and producer behavior. Do not relabel old recordings as draft-3. Define and test the new machine-readable contract before implementing a loop producer. Existing consumers must reject an unsupported version rather than reinterpret conditions as writes or ignore unknown lifecycle data.

## Reasons and consequences

An atomic scope effect avoids a trace ending between the false condition and its required index removal. It also avoids introducing general event grouping solely for this bounded case. A separate SCOPE_EXIT record would require grouped-step and safe-boundary semantics now; leaving i visible would conflict with the approved normal loop-exit behavior.

Replay applies captured values and recorded exits together. Collector validation rejects the whole record before mutation if any field is inconsistent. Failure/limit snapshots retain only earlier observed state; no synthetic normal exit is appended during unwinding. Broader method frames, nested scopes, break/continue and exception teardown remain future specifications, not promises that this model already handles them.

Use a bounded loop phase plan in the collector instead of expanding a fixed list based on predicted iteration counts. Source analysis supplies scope/binding/site facts; instrumentation captures Java execution; collection validates order and consistency; replay never executes Java. This preserves the existing module boundaries without building a general control-flow interpreter.

The bounded implementation defensively validates loop handoffs by invoking the existing analyzer on the exact original bytes inside the isolated worker and comparing frozen records and syntax. It does not maintain a competing resolver or resolve a transformed clone. This adds analysis cost under the unchanged worker deadline. Draft-3 helper members are appended only for loop generation, reusing the existing recorder's transport writer; this preserves legacy helper output and fixture-batch capacity instead of enlarging every old artifact. New loop fixtures use two additional bounded workers/batches.

Exact syntax, payload ownership, identities, step table, limits and acceptance cases are defined in [the first loop specification](../for-loop-array-recording.md). The [draft-3 contract](../../contracts/run-result-v3.schema.json) is implemented with six illustrative fixtures, 44 negative mutations, twelve partial prefixes and source/cursor-state checks. Draft-1/draft-2 regression tests passed and their schemas/fixtures are unchanged. The September 29 contract task ran no Java/Docker tests; the subsequent runtime implementation now provides captured execution evidence as described above.
