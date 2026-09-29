# Integer variable recording experiment

Approved 2026-09-23: extend the current analyzer, copied-source instrumentation, recorder, validation and test-only reconstruction for `int x = 5; x = 8;`.

## Java and step semantics

The eligible shape has one non-final local primitive int with a signed decimal int-literal initializer, followed by one simple assignment of another signed decimal int literal to the same resolved declaration. Use the established public Main.main(String[]) convention. Names, whitespace, comments and source locations may vary. Optional trailing development probes are `System.out.print("FINAL=" + x);` and `System.err.print("PROBE");`, resolved to the JDK calls. No other source statements are silently omitted.

Initialization and declaration form one VARIABLE_DECLARE step after initialization succeeds. VARIABLE_WRITE follows the successful assignment. Reading the variable to record its resulting value is bookkeeping, not a scalar-read step. Initial playback has no variable; step one has x=5; step two has x=8. Backward playback restores x=5, then removes the binding, using recorded facts only.

The transformer preserves the original assignment in its separate syntax copy. A recorder-budget guard runs before the assignment; the post-assignment recorder reads the actual variable value once. If a limit stops the next operation, retain the preceding safe prefix. No event is predicted from the literal in the AST. Recorder I/O failures may interrupt the run and never authorize automatic rerunning.

Mixed scalar/array programs, multiple declarations/assignments, expressions, increment/compound assignment, uninitialized/final variables, branches, loops and scope-exit/frame lifecycles remain outside this narrow gate. Existing array support is preserved. This is an increment toward V1, not a reduction of its scope.

## Contract and compatibility

Keep the existing draft-1 array schema unchanged. Add draft-2 in contracts/run-result-v2.schema.json: the same envelope and array events plus VARIABLE_DECLARE(sequence, kind, source, variableId, variableName, value) and VARIABLE_WRITE(sequence, kind, source, variableId, value). Values use the existing tagged signed int encoding. Scalar events contain no arrayId. Names are display labels; variableId identifies a binding within one run. The current one-variable recorder uses variable-1, without promising multi-frame identity handling.

Each event remains one step. Sequence numbers are contiguous; a declaration precedes writes; duplicate variable IDs, writes to an array binding, invalid int values and unexplained partial traces are rejected. Old producers remain draft-1; scalar producers use draft-2 because old consumers cannot interpret new event kinds. Explicitly support both versions in the validator. No conversion of old stored artifacts is required.

## Analysis and transformation

Reuse source identity, parser configuration, diagnostic categories, bounds and entry validation. Add an explicit probe kind and resolved assignment binding to the handoff; a scalar probe has no array-read site. Preserve the array completeness status and add a scalar-specific status. The transformer consumes those facts, verifies stale source and binding/range consistency, and retains original/generated source associations. Both prototype classes keep their existing names for compatibility; capability handling is explicit.

## Bounds and verification

Use existing source/generated-source, worker, trace, event and diagnostic caps. Extend the same recorder/collector transport without changing execution isolation. Test x=5 then x=8, renamed/Unicode variables, CRLF/multiline ranges, helper collisions, repeated/negative/int-boundary values, no-probe source and an event limit retaining only initialization. Reject unresolved/wrong-target/final/side-effecting/unsupported shapes and stale handoffs. Compare original/generated execution in Docker and validate intermediate/backward states and original highlights. Rerun array automatic/manual regressions and contract negative tests. No browser renderer exists yet.

Verified 2026-09-23: test-recording.ps1 passed 17 automatic runtime cases (nine array, eight scalar), 15 original/generated comparisons, all 17 contract/source-mapping/reconstruction checks, the original 18 analyzer cases, scalar analysis/transformation guards, two worker-limit probes, collector checks including seven scalar rejection cases, and seven manual recording cases/four comparisons. Contract tests preserved the old fixtures/18 negative mutations and added draft-2 scalar/array compatibility plus six scalar negative mutations. All owned containers were removed. The separate output-only runner and simulated Docker-classification suites were not rerun; orchestration was unchanged.
