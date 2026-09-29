# ADR 0006: Version scalar events as draft-2

Status: implemented within the user-approved integer-variable increment, 2026-09-23.

## Decision

Keep contracts/run-result.schema.json as the unchanged draft-1 array contract. Add run-result-v2.schema.json for draft-2, preserving that envelope and array payloads while adding VARIABLE_DECLARE and VARIABLE_WRITE. The validator explicitly selects either trusted schema by version. Existing array producers continue emitting draft-1; the new scalar result producer emits draft-2.

## Rationale and consequences

The old event union is closed and an old consumer cannot interpret scalar events. Silently expanding the draft-1 meaning would misrepresent compatibility. Keeping two schemas preserves old traces and makes the new requirement explicit without adding a general migration framework. A draft-2 consumer can validate the existing array event shapes as well; mixed scalar/array execution is still not implemented by the current prototype.

Scalar binding identity is distinct from its display name and from an array object. The prototype uses one variable-1 binding in one main invocation. The typed int contract preserves all signed 32-bit values. Semantics require declaration before assignment and reject duplicate or wrong-kind binding IDs. Frame/scope lifecycle, more variables and additional primitive types require later specifications.

See [integer semantics](../integer-recording.md), [trace contract](../trace-format.md) and [validator guide](../../contracts/README.md). Contract tests cover existing draft-1 fixtures, draft-2 array compatibility, scalar success and invalid scalar payload/identity/order cases. Runtime evidence is recorded after the Docker gate passes.
