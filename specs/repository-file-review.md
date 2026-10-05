# Repository file review — 2026-10-05

The user expanded PR #12 cleanup to all repository files, including the runner. Reviewed the 175 tracked files at commit 424319e by ownership, build/test/recording references, current requirements and compatibility purpose. This inventory is not a security audit or a claim of new Java support.

## Decisions

- Keep the active Java analyzer/transformers, execution harness, trace collectors, schemas, fixtures and acceptance tests. Narrow probes still supply current supported behavior and regression proof; deleting them before a verified composable replacement would remove functionality.
- Keep the manual recording baseline and ExecEvidenceProbe diagnostic. The former checks recording independently of transformation; the latter has documented reproduction commands for the timestamp-cutoff failure. They are not dead application files.
- Extract duplicate validated-prefix state reconstruction from loop/conditional replay into replay-state.mjs. Keep both public adapters, error messages, selected details and their distinct absent-versus-null fields. No Java evaluation, schema semantics or scope model changes.
- Remove only runner/analysis/.gitignore and runner/prototype/.gitignore. Root **/target/, **/.tools/, **/.results/ and **/.build/ already cover every removed pattern. Before/after checks retain all five representative ignored paths. Keep .dockerignore: Docker build-context filtering has a different purpose.
- Local ignored build caches and recorded test evidence are not shipped source or obsolete product modules. They remain untouched, including the captured results used for this verification. No dependencies, toolchain pins or runtime limits changed.

## Verification

Before and after extraction: contract self-tests and all 178 affected captured-result checks passed (30 conditional, 68 loop, 80 read-loop), including exact hashes/highlights, forward/backward prefixes and retirement. These reuse October 4 execution captures; no Java or Docker execution was rerun or newly claimed. The first sandboxed baseline launch could not spawn Node (EPERM); its elevated rerun and post-refactor checks passed.

The new test-replay.mjs passed against both old and refactored adapters: output shape, cursor/error compatibility, comparisons/reads, temporary/index retirement, reverse snapshots and mutation isolation. Both existing PowerShell gates now invoke it. PowerShell syntax and ignore coverage checks passed. Source/contract byte preservation and final documentation/whitespace checks accompany delivery.

Final checks passed: 63 Markdown files, 695 local links, balanced fences and Git whitespace. All Java files, JSON schemas/fixtures, recorder templates, XML build pins, dependency baseline and Dockerfiles are unchanged against 424319e. Before/after logs remain ignored under runner/analysis/.results/refactor-before-*.log and refactor-after-*.log.

## File-by-file disposition

All baseline files are listed below. New files are the shared replay-state.mjs, test-replay.mjs and this review. The current task also updates guides and both active memory files. Direct-reference inspection found references for every non-fixture Java/JavaScript/PowerShell entry; references include documented commands and are supporting evidence rather than a complete call graph.

| Baseline file | Disposition and purpose |
| --- | --- |
| [.agents/architect.md](../.agents/architect.md) | Keep: role boundaries; descriptive, not executable registration |
| [.agents/backend-runner-engineer.md](../.agents/backend-runner-engineer.md) | Keep: role boundaries; descriptive, not executable registration |
| [.agents/frontend-editor-engineer.md](../.agents/frontend-editor-engineer.md) | Keep: role boundaries; descriptive, not executable registration |
| [.agents/instrumentation-trace-engineer.md](../.agents/instrumentation-trace-engineer.md) | Keep: role boundaries; descriptive, not executable registration |
| [.agents/java-analysis-engineer.md](../.agents/java-analysis-engineer.md) | Keep: role boundaries; descriptive, not executable registration |
| [.agents/reviewer.md](../.agents/reviewer.md) | Keep: role boundaries; descriptive, not executable registration |
| [.agents/visualization-playback-engineer.md](../.agents/visualization-playback-engineer.md) | Keep: role boundaries; descriptive, not executable registration |
| [.gitattributes](../.gitattributes) | Keep: exact source/fixture bytes and hashes |
| [.gitignore](../.gitignore) | Keep: authoritative generated-output/credential exclusions |
| [AGENTS.md](../AGENTS.md) | Keep: requirements, current guidance or dated verification/history |
| [README.md](../README.md) | Keep: requirements, current guidance or dated verification/history |
| [contracts/README.md](../contracts/README.md) | Keep: requirements, current guidance or dated verification/history |
| [contracts/conditional-contract-tests.mjs](../contracts/conditional-contract-tests.mjs) | Keep: active versioned schemas/validation and regression checks |
| [contracts/conditional-semantics.mjs](../contracts/conditional-semantics.mjs) | Keep: active versioned schemas/validation and regression checks |
| [contracts/examples/README.md](../contracts/examples/README.md) | Keep: requirements, current guidance or dated verification/history |
| [contracts/examples/array-success.json](../contracts/examples/array-success.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-cancel.json](../contracts/examples/conditional-cancel.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-equal.json](../contracts/examples/conditional-equal.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-false.json](../contracts/examples/conditional-false.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-left-error.json](../contracts/examples/conditional-left-error.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-limit.json](../contracts/examples/conditional-limit.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-output-only.json](../contracts/examples/conditional-output-only.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-right-error.json](../contracts/examples/conditional-right-error.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-same-index.json](../contracts/examples/conditional-same-index.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/conditional-success.json](../contracts/examples/conditional-success.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-limit.json](../contracts/examples/loop-limit.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-output-only.json](../contracts/examples/loop-output-only.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-prefix.json](../contracts/examples/loop-prefix.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-read-limit.json](../contracts/examples/loop-read-limit.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-read-success.json](../contracts/examples/loop-read-success.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-runtime-error.json](../contracts/examples/loop-runtime-error.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-success.json](../contracts/examples/loop-success.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/loop-zero.json](../contracts/examples/loop-zero.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/output-only.json](../contracts/examples/output-only.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/partial-visualization.json](../contracts/examples/partial-visualization.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/runtime-error.json](../contracts/examples/runtime-error.json) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/conditional-equal.java](../contracts/examples/sources/conditional-equal.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/conditional-false.java](../contracts/examples/sources/conditional-false.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/conditional-left-error.java](../contracts/examples/sources/conditional-left-error.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/conditional-right-error.java](../contracts/examples/sources/conditional-right-error.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/conditional-same-index.java](../contracts/examples/sources/conditional-same-index.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/conditional-success.java](../contracts/examples/sources/conditional-success.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/loop-prefix.java](../contracts/examples/sources/loop-prefix.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/loop-read-success.java](../contracts/examples/sources/loop-read-success.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/loop-runtime-error.java](../contracts/examples/sources/loop-runtime-error.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/loop-success.java](../contracts/examples/sources/loop-success.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/examples/sources/loop-zero.java](../contracts/examples/sources/loop-zero.java) | Keep: designed compatibility fixtures and exact source snapshots |
| [contracts/loop-contract-tests.mjs](../contracts/loop-contract-tests.mjs) | Keep: active versioned schemas/validation and regression checks |
| [contracts/run-result-v2.schema.json](../contracts/run-result-v2.schema.json) | Keep: active versioned schemas/validation and regression checks |
| [contracts/run-result-v3.schema.json](../contracts/run-result-v3.schema.json) | Keep: active versioned schemas/validation and regression checks |
| [contracts/run-result-v4.schema.json](../contracts/run-result-v4.schema.json) | Keep: active versioned schemas/validation and regression checks |
| [contracts/run-result.schema.json](../contracts/run-result.schema.json) | Keep: active versioned schemas/validation and regression checks |
| [contracts/validate.mjs](../contracts/validate.mjs) | Keep: active versioned schemas/validation and regression checks |
| [memory/current-state-history-20261005.md](../memory/current-state-history-20261005.md) | Keep: requirements, current guidance or dated verification/history |
| [memory/current-state.md](../memory/current-state.md) | Keep: requirements, current guidance or dated verification/history |
| [memory/project-progress.md](../memory/project-progress.md) | Keep: requirements, current guidance or dated verification/history |
| [requirements/PROJECT_REQUIREMENTS.md](../requirements/PROJECT_REQUIREMENTS.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/.dockerignore](../runner/analysis/.dockerignore) | Keep: pinned build/worker inputs and restricted image context |
| `runner/analysis/.gitignore` | Remove: all patterns already covered by root .gitignore |
| [runner/analysis/AUTOMATIC_RECORDING.md](../runner/analysis/AUTOMATIC_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/AutomaticRecordingTest.java](../runner/analysis/AutomaticRecordingTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/COMBINED_RECORDING.md](../runner/analysis/COMBINED_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/CONDITIONAL_RECORDING.md](../runner/analysis/CONDITIONAL_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/ConditionalFixtureBundleTest.java](../runner/analysis/ConditionalFixtureBundleTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/ConditionalRecordingTest.java](../runner/analysis/ConditionalRecordingTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/Dockerfile](../runner/analysis/Dockerfile) | Keep: pinned build/worker inputs and restricted image context |
| [runner/analysis/FixtureBundleTest.java](../runner/analysis/FixtureBundleTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/INDEX_ADDITION_RECORDING.md](../runner/analysis/INDEX_ADDITION_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/INDEX_INCREMENT_RECORDING.md](../runner/analysis/INDEX_INCREMENT_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/INDEX_UPDATE_RECORDING.md](../runner/analysis/INDEX_UPDATE_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/INTEGER_RECORDING.md](../runner/analysis/INTEGER_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/LOOP_READ_RECORDING.md](../runner/analysis/LOOP_READ_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/LOOP_RECORDING.md](../runner/analysis/LOOP_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/LoopFixtureBundleTest.java](../runner/analysis/LoopFixtureBundleTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/LoopReadFixtureBundleTest.java](../runner/analysis/LoopReadFixtureBundleTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/LoopReadRecordingTest.java](../runner/analysis/LoopReadRecordingTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/LoopRecordingTest.java](../runner/analysis/LoopRecordingTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/README.md](../runner/analysis/README.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/VARIABLE_INDEX_RECORDING.md](../runner/analysis/VARIABLE_INDEX_RECORDING.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/analysis/check-conditionals.mjs](../runner/analysis/check-conditionals.mjs) | Keep: actual captured-result source/contract/replay checks |
| [runner/analysis/check-loop-reads.mjs](../runner/analysis/check-loop-reads.mjs) | Keep: actual captured-result source/contract/replay checks |
| [runner/analysis/check-loops.mjs](../runner/analysis/check-loops.mjs) | Keep: actual captured-result source/contract/replay checks |
| [runner/analysis/check-recordings.mjs](../runner/analysis/check-recordings.mjs) | Keep: actual captured-result source/contract/replay checks |
| [runner/analysis/dependencies.txt](../runner/analysis/dependencies.txt) | Keep: pinned build/worker inputs and restricted image context |
| [runner/analysis/maven.ps1](../runner/analysis/maven.ps1) | Keep: reproducible build/test orchestration |
| [runner/analysis/pom.xml](../runner/analysis/pom.xml) | Keep: pinned build/worker inputs and restricted image context |
| [runner/analysis/src/main/java/dev/codeviz/analysis/AnalyzerMain.java](../runner/analysis/src/main/java/dev/codeviz/analysis/AnalyzerMain.java) | Keep: active source facts/legacy capability analysis; no replacement yet |
| [runner/analysis/src/main/java/dev/codeviz/analysis/ArrayAnalyzer.java](../runner/analysis/src/main/java/dev/codeviz/analysis/ArrayAnalyzer.java) | Keep: active source facts/legacy capability analysis; no replacement yet |
| [runner/analysis/src/main/java/dev/codeviz/analysis/CombinedProbe.java](../runner/analysis/src/main/java/dev/codeviz/analysis/CombinedProbe.java) | Keep: active source facts/legacy capability analysis; no replacement yet |
| [runner/analysis/src/main/java/dev/codeviz/analysis/ConditionalProbe.java](../runner/analysis/src/main/java/dev/codeviz/analysis/ConditionalProbe.java) | Keep: active source facts/legacy capability analysis; no replacement yet |
| [runner/analysis/src/main/java/dev/codeviz/analysis/IntProbe.java](../runner/analysis/src/main/java/dev/codeviz/analysis/IntProbe.java) | Keep: active source facts/legacy capability analysis; no replacement yet |
| [runner/analysis/src/main/java/dev/codeviz/analysis/LoopProbe.java](../runner/analysis/src/main/java/dev/codeviz/analysis/LoopProbe.java) | Keep: active source facts/legacy capability analysis; no replacement yet |
| [runner/analysis/src/main/java/dev/codeviz/analysis/SourceSnapshot.java](../runner/analysis/src/main/java/dev/codeviz/analysis/SourceSnapshot.java) | Keep: active source facts/legacy capability analysis; no replacement yet |
| [runner/analysis/src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java](../runner/analysis/src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) | Keep: active guarded lowering and original-source mapping |
| [runner/analysis/src/main/java/dev/codeviz/instrumentation/ConditionalTransformer.java](../runner/analysis/src/main/java/dev/codeviz/instrumentation/ConditionalTransformer.java) | Keep: active guarded lowering and original-source mapping |
| [runner/analysis/src/main/java/dev/codeviz/instrumentation/LoopTransformer.java](../runner/analysis/src/main/java/dev/codeviz/instrumentation/LoopTransformer.java) | Keep: active guarded lowering and original-source mapping |
| [runner/analysis/src/test/java/dev/codeviz/analysis/AnalyzerAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/AnalyzerAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/ConditionalAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/ConditionalAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/IndexAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/IndexAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/IndexAdditionAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/IndexAdditionAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/IndexIncrementAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/IndexIncrementAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/IndexUpdateAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/IndexUpdateAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/LoopAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/LoopAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/LoopReadAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/LoopReadAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/src/test/java/dev/codeviz/analysis/TransformationAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/TransformationAcceptance.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/analysis/test-recording.ps1](../runner/analysis/test-recording.ps1) | Keep/update: run replay compatibility check before existing gate |
| [runner/analysis/test.ps1](../runner/analysis/test.ps1) | Keep: reproducible build/test orchestration |
| `runner/prototype/.gitignore` | Remove: all patterns already covered by root .gitignore |
| [runner/prototype/ArrayRecordingTest.java](../runner/prototype/ArrayRecordingTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/prototype/ArrayTrace.java](../runner/prototype/ArrayTrace.java) | Keep: isolated execution, evidence, pipe or plan validation |
| [runner/prototype/ArrayTraceTest.java](../runner/prototype/ArrayTraceTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/prototype/ConditionalTracePlan.java](../runner/prototype/ConditionalTracePlan.java) | Keep: isolated execution, evidence, pipe or plan validation |
| [runner/prototype/ConditionalTraceTest.java](../runner/prototype/ConditionalTraceTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/prototype/ExecEvidence.java](../runner/prototype/ExecEvidence.java) | Keep: isolated execution, evidence, pipe or plan validation |
| [runner/prototype/LoopReadTraceTest.java](../runner/prototype/LoopReadTraceTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/prototype/LoopTracePlan.java](../runner/prototype/LoopTracePlan.java) | Keep: isolated execution, evidence, pipe or plan validation |
| [runner/prototype/LoopTraceTest.java](../runner/prototype/LoopTraceTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/prototype/README.md](../runner/prototype/README.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/prototype/RunnerFailureTest.java](../runner/prototype/RunnerFailureTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/prototype/RunnerHarness.java](../runner/prototype/RunnerHarness.java) | Keep: isolated execution, evidence, pipe or plan validation |
| [runner/prototype/RunnerHarnessTest.java](../runner/prototype/RunnerHarnessTest.java) | Keep: acceptance/semantic/collector/delivery evidence used by gates |
| [runner/prototype/TracePipe.java](../runner/prototype/TracePipe.java) | Keep: isolated execution, evidence, pipe or plan validation |
| [runner/prototype/diagnostics/ExecEvidenceProbe.java](../runner/prototype/diagnostics/ExecEvidenceProbe.java) | Keep: documented reproduction for Docker evidence-cutoff regression |
| [runner/prototype/fixtures/cancel/Main.java](../runner/prototype/fixtures/cancel/Main.java) | Keep: runner isolation/failure acceptance source, executed only in Docker |
| [runner/prototype/fixtures/compile-error/Main.java](../runner/prototype/fixtures/compile-error/Main.java) | Keep: runner isolation/failure acceptance source, executed only in Docker |
| [runner/prototype/fixtures/heap-exhaustion/Main.java](../runner/prototype/fixtures/heap-exhaustion/Main.java) | Keep: runner isolation/failure acceptance source, executed only in Docker |
| [runner/prototype/fixtures/output-limit/Main.java](../runner/prototype/fixtures/output-limit/Main.java) | Keep: runner isolation/failure acceptance source, executed only in Docker |
| [runner/prototype/fixtures/runtime-error/Main.java](../runner/prototype/fixtures/runtime-error/Main.java) | Keep: runner isolation/failure acceptance source, executed only in Docker |
| [runner/prototype/fixtures/success/Main.java](../runner/prototype/fixtures/success/Main.java) | Keep: runner isolation/failure acceptance source, executed only in Docker |
| [runner/prototype/fixtures/timeout/Main.java](../runner/prototype/fixtures/timeout/Main.java) | Keep: runner isolation/failure acceptance source, executed only in Docker |
| [runner/prototype/recording/ConditionalRecorderMembers.java](../runner/prototype/recording/ConditionalRecorderMembers.java) | Keep: recorder resource, manual baseline or result verification |
| [runner/prototype/recording/LoopReadRecorderMembers.java](../runner/prototype/recording/LoopReadRecorderMembers.java) | Keep: recorder resource, manual baseline or result verification |
| [runner/prototype/recording/LoopRecorderMembers.java](../runner/prototype/recording/LoopRecorderMembers.java) | Keep: recorder resource, manual baseline or result verification |
| [runner/prototype/recording/README.md](../runner/prototype/recording/README.md) | Keep: requirements, current guidance or dated verification/history |
| [runner/prototype/recording/Recorder.java](../runner/prototype/recording/Recorder.java) | Keep: recorder resource, manual baseline or result verification |
| [runner/prototype/recording/check-results.mjs](../runner/prototype/recording/check-results.mjs) | Keep: recorder resource, manual baseline or result verification |
| [runner/prototype/recording/conditional-replay.mjs](../runner/prototype/recording/conditional-replay.mjs) | Refactor: retain version-specific output; share reconstruction |
| [runner/prototype/recording/instrumented/Main.java.in](../runner/prototype/recording/instrumented/Main.java.in) | Keep: recorder resource, manual baseline or result verification |
| [runner/prototype/recording/loop-replay.mjs](../runner/prototype/recording/loop-replay.mjs) | Refactor: retain version-specific output; share reconstruction |
| [runner/prototype/recording/original/Main.java](../runner/prototype/recording/original/Main.java) | Keep: recorder resource, manual baseline or result verification |
| [runner/prototype/test.ps1](../runner/prototype/test.ps1) | Keep/update: run replay compatibility check before existing gate |
| [specs/architecture.md](../specs/architecture.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/array-analysis-experiment.md](../specs/array-analysis-experiment.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/array-recording-experiment.md](../specs/array-recording-experiment.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/automatic-array-recording.md](../specs/automatic-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/bubble-sort-proposal.md](../specs/bubble-sort-proposal.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/combined-variable-array-recording.md](../specs/combined-variable-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/compare-swap-proposal.md](../specs/compare-swap-proposal.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/composable-java-tracing.md](../specs/composable-java-tracing.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0001-live-console-websocket.md](../specs/decisions/0001-live-console-websocket.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0002-java-execution-and-visualization-coverage.md](../specs/decisions/0002-java-execution-and-visualization-coverage.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0003-runner-execution-evidence.md](../specs/decisions/0003-runner-execution-evidence.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0004-controlled-array-trace-pipe.md](../specs/decisions/0004-controlled-array-trace-pipe.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0005-javaparser-prototype-candidate.md](../specs/decisions/0005-javaparser-prototype-candidate.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0006-scalar-trace-draft-2.md](../specs/decisions/0006-scalar-trace-draft-2.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0007-loop-condition-and-scope-draft-3.md](../specs/decisions/0007-loop-condition-and-scope-draft-3.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0008-loop-array-read-stepping.md](../specs/decisions/0008-loop-array-read-stepping.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0009-conditional-comparison-trace.md](../specs/decisions/0009-conditional-comparison-trace.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0010-nested-sorting-trace.md](../specs/decisions/0010-nested-sorting-trace.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/decisions/0011-composable-java-tracing.md](../specs/decisions/0011-composable-java-tracing.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/docker-exec-evidence-fix.md](../specs/docker-exec-evidence-fix.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/docker-exec-evidence-investigation.md](../specs/docker-exec-evidence-investigation.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/documentation-alignment-review.md](../specs/documentation-alignment-review.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/execution-isolation.md](../specs/execution-isolation.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/for-loop-array-recording.md](../specs/for-loop-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/frontend-design.md](../specs/frontend-design.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/index-addition-array-recording.md](../specs/index-addition-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/index-increment-array-recording.md](../specs/index-increment-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/index-update-array-recording.md](../specs/index-update-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/integer-recording.md](../specs/integer-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/java-support.md](../specs/java-support.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/loop-array-read-proposal.md](../specs/loop-array-read-proposal.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/scalar-update-array-recording.md](../specs/scalar-update-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/trace-format.md](../specs/trace-format.md) | Keep: requirements, current guidance or dated verification/history |
| [specs/variable-index-array-recording.md](../specs/variable-index-array-recording.md) | Keep: requirements, current guidance or dated verification/history |
