# Draft Contract Validation

2026-09-29: [run-result-v3.schema.json](run-result-v3.schema.json) and the validator implement the approved draft-3 contract for boolean conditions and atomic loop-scope exit. See [the first loop specification](../specs/for-loop-array-recording.md) and [ADR 0007](../specs/decisions/0007-loop-condition-and-scope-draft-3.md). That task covered contract validation only. The subsequent [runtime implementation](../runner/analysis/LOOP_RECORDING.md) now produces and validates real loop traces; its 68 loop cases passed on 2026-09-30. The six checked-in draft-3 examples remain designed contract fixtures, not runtime captures. Draft-1/draft-2 schemas and their four checked-in fixtures are byte-for-byte unchanged.

## Draft-3 scope and verification

The result envelope remains unchanged. Declaration events require scopeId; all events require exitedVariableIds, an array of binding IDs. CONDITION has scopeId and a typed boolean value, with no variableId or arrayId. General boolean variables remain unsupported. A false condition retires its one live int loop binding in the same event; true conditions and other operations have empty exit lists. One event remains one observable step.

Semantic validation checks known/live scope membership, exact false-condition retirement, duplicate/unknown/wrong-scope exits, writes after retirement, identity reuse and attempts to reopen a closed scope. A complete result cannot leave an observed condition scope open. Validation does not mutate the input, including when a record fails. Broader scope membership, nested loops, aliases and frames remain future work.

The schema validator has no submitted-source operation plan. It cannot prove a condition matches Java operands, enforce condition/body/update order, identify a body write that refers indirectly to a retired index, or verify runtime provenance. Those checks belong to the future plan-aware collector. Source hash/range checks and cursor-state reconstruction in [loop-contract-tests.mjs](loop-contract-tests.mjs) verify the designed fixtures; they are not a production playback reducer or a runtime-loop test.

Verified with `node contracts/validate.mjs --self-test` on 2026-09-29: all ten checked-in results, existing draft-1/draft-2 tests, six draft-3 fixtures, 44 draft-3 negative mutations, twelve partial prefixes, strict boolean/type guards, source hashes/ranges and forward/backward fixture states passed. Prefix/postfix fixture facts agree after removing source ranges. SHA-256 comparisons confirmed preservation of the two earlier schemas and four earlier fixtures. No Java, Docker or browser tests ran.

The custom checker now explicitly supports the JSON Schema boolean type. Other unsupported types and keywords still fail. Old versions reject draft-3 fields/events; simply changing an old trace's version does not make it valid draft-3. The new fixture sources and results are illustrative, not captured execution evidence; see [their guide](examples/README.md).

The variable-index increment adds a draft-2 self-test with two independent scalar declarations and six negative mutations. The schema already allows repeated event kinds with distinct binding IDs. Operation-specific original-site checks and index/value agreement with the intended scalar bindings remain collector responsibilities; the general validator does not infer submitted-source semantics.

The scalar-update increment adds a draft-2 self-test for VARIABLE_WRITE between ARRAY_DECLARE and ARRAY_WRITE, plus six negative mutations. No schema fields changed. Program-specific operation counts, latest-scalar store agreement and premature completion are checked by the bounded collector, not inferred by the general schema validator.

2026-09-24: the combined scalar/array experiment uses existing draft-2 fields unchanged. Its two binding IDs are distinct from each other and from the array object identity. Self-tests now include a mixed positive result and six mixed rejection mutations, alongside earlier scalar/array checks. The collector additionally checks expected source sites, names and operation order; the general schema validator has no submitted-source context.

2026-09-23: [run-result-v2.schema.json](run-result-v2.schema.json) adds draft-2 integer-variable declaration/write events. The original draft-1 schema remains unchanged. The validator explicitly supports both versions; scalar producers use draft-2 and existing array producers stay draft-1. See [ADR 0006](../specs/decisions/0006-scalar-trace-draft-2.md).

This is a narrow draft for the four [example results](examples/README.md), not the complete V1 protocol. See [trace-format.md](../specs/trace-format.md) for intended behavior.

- [run-result.schema.json](run-result.schema.json): JSON Schema 2020-12 structural contract, application version draft-1.
- [validate.mjs](validate.mjs): dependency-free Node checker for this schema's keywords, semantic checks, and negative cases.

From the repository root in PowerShell:

```powershell
node contracts/validate.mjs
node contracts/validate.mjs --self-test
node contracts/validate.mjs contracts/examples/array-success.json
```

Checked with Node 22.23.2 on 2026-09-19. No dependencies are installed. Explicit input paths are relative to the current directory; default fixtures/schema resolve relative to the script. Failures return a nonzero exit code.

## Rules and Scope

All result identity/version/outcome/coverage/diagnostic/event/boundary/console fields are required. Unknown properties are rejected. Events are ARRAY_DECLARE, ARRAY_READ, or ARRAY_WRITE with distinct payloads. Values are typed int within Java's signed 32-bit range. Source positions and sequence numbers are positive safe integers; indices are nonnegative integers.

Draft-2 additionally accepts VARIABLE_DECLARE with variableId, variableName and typed value, and VARIABLE_WRITE with variableId and typed value. Both have sequence/kind/source and no arrayId. Semantic checks require an earlier scalar declaration, prevent duplicate binding identities across kinds, and reject scalar writes to array bindings. One scalar event is one observable post-operation step. The self-test includes draft-2 scalar success, array compatibility and six scalar rejection mutations. Existing draft-1 tests remain.

Unavailable visualization requires null events/boundary and a diagnostic. Partial requires nonempty events, boundary, and a diagnostic. Complete requires nonempty events and a boundary. Zero-operation traces are deliberately outside this draft; empty allocated arrays are supported. This is a schema limitation, not permission to call a valid zero-operation program visualization-unavailable in the eventual app.

Each event is one observable step in this draft. Delivered events are exactly the safe prefix, so both boundary fields equal event count. Bookkeeping/grouping, records beyond a safe boundary, broader types, aliases, frames, and lifecycle events require future contract work. Complete coverage here describes the illustrative observable sequence, not a proven production lifecycle.

Execution outcome remains separate from visualization coverage. Semantic checks require matching diagnostics for failed/cancelled/limited outcomes. Compilation/admission failure requires failed execution and unavailable visualization. Completed execution cannot carry failure/limit diagnostics. These are draft consistency rules, not an exhaustive production lifecycle matrix.

## Validation Layers

The structural checker reads the schema and supports only its enumerated keywords: local named references/definitions, type, properties/required, additionalProperties:false, primitive constants/enums, oneOf, items, minItems/minLength, minimum/maximum, and metadata. Unsupported keywords, types, recursive references, and nonlocal reference forms fail explicitly. This is not a general JSON Schema implementation, metaschema validator, or independently certified standards validator. Use the trusted repository schema only; it is not a service for arbitrary schemas or unbounded input. A maintained standards validator can replace it during approved dependency setup.

Semantic checks cover ordered contiguous sequences, unique declarations, declared array references, valid indices, captured read-value consistency, forward source ranges, boundary agreement, and outcome/diagnostic combinations. They do not compile Java, prove runtime provenance, check source coordinates against actual text, authenticate source snapshots, or test a production playback reducer.

The self-test mutates fixture copies in memory. It checks missing fields, incorrect versions/payloads, unknown fields, invalid integers, coverage combinations, sequences, identities, indices, read values, boundaries, ranges, and outcomes. Positive edge cases cover both int bounds and an empty array. Unsupported schema keywords have a guard test. No fixture is modified by validation.

Keyword semantics were checked against official [JSON Schema validation](https://json-schema.org/draft/2020-12/json-schema-validation) and [core](https://json-schema.org/draft/2020-12/json-schema-core) documentation. JSON Schema dialect 2020-12 and application version draft-1 are distinct identifiers.

No Java execution, Docker setup, library installation, or application implementation was performed.
