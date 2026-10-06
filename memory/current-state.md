# Current session handoff

Updated 2026-10-05. This file is the active checkpoint, not a chronological log. Requirements and specifications take precedence. See [progress history](project-progress.md) and the [unchanged prior handoff](current-state-history-20261005.md) for dated evidence; their old next steps are historical.

## Current task

The user expanded the cleanup to all files, including runner code: keep/refactor useful components and remove confirmed obsolete files. This continues on docs/requirements-alignment in open PR #12. Reviewed all 175 tracked files at 424319e. Retained active source, schemas, fixtures, legacy compatibility tests and documented diagnostics. Extracted shared loop/conditional replay state reconstruction with unchanged adapters; added a compatibility test to both PowerShell gates. Removed only two redundant runner-local .gitignore files; root patterns preserve exclusions.

Before/after contract checks and all 178 affected captured-result checks passed (30 conditional, 68 loop, 80 read-loop), plus adapter error/shape/mutation/retirement tests and PowerShell syntax. These reuse October 4 traces; no new Java/Docker execution or language support is claimed. Final checks passed: 63 Markdown files/695 local links, fences/whitespace, and unchanged Java/schema/fixture/recorder/build-pin/Dockerfile bytes against 424319e. Publication: runner cleanup commit 81974f0 is pushed and PR #12 is updated with the full scope. The approved cleanup is complete; final handoff notes follow on the same branch. Do not start another task or merge automatically.

## Product direction

- Educational Java visualization, Windows-local V1, Java 21 with Docker isolation. Ordinary Main.java/Main.main authoring; execution admission and tracing eligibility stay separate.
- Trace supported statements/expressions and verified contexts, not algorithm names or exact whole-program templates. Starter examples remain allowed. No arbitrary-Java guarantee.
- One Step advances an observable operation using runtime facts and original-source highlighting. Recorded playback starts after execution terminates. Live Scanner input remains required but unfinished.
- Variables/arrays and user-written sorting first; then collections and tree/heap sorting. Methods/recursion remain required. Full V1/V2 scope and selected frontend/backend stack remain in [requirements](../requirements/PROJECT_REQUIREMENTS.md).

## Verified implementation

Milestone 1 is active and incomplete. Existing shape-specific analyzers/transformers, Docker runner, draft-1 through draft-4 collectors and prototype replay are verified only for their documented coverage. No Spring Boot/frontend application integration exists.

Latest full gate: October 4, 2026, exit 0; 328 automatic cases/234 original-generated comparisons and seven manual cases/four comparisons. Source/hash/replay, collectors, fixture delivery, worker limits and cleanup passed. See [conditional guide](../runner/analysis/CONDITIONAL_RECORDING.md). The cleanup reruns affected replay checks on those captures, not the full Java execution gate.

PR #9 implemented conditional swaps; PR #10 proposed bubble-sort composition; PR #11 adopted the revised [composable tracing direction](../specs/composable-java-tracing.md). All three are merged. Draft-5, composable analysis/runtime and the previously proposed larger event budget are not implemented. Existing producers retain their current limits.

## Next proposed task — approval required

Implement a separate composable analysis result for supported int/int[] declarations, plain assignments, parentheses, int addition/subtraction, array length and nested array reads in any supported sequence. Keep legacy probes and runtime paths intact. Confirm concrete analysis bounds and acceptance cases first; the [review](../specs/composable-java-tracing.md) defines the proposed boundary.

Later tasks add lowering/runtime, blocks/conditions, loops and method/frame/reference support. Revisit capture dependencies, scope exits and atomic bookkeeping before new trace contracts. Do not resume the superseded sorting-specific draft-5 task.

## Delivery and verification rules

Discuss and obtain confirmation before new work. Each approved task updates this file and project-progress.md, commits/pushes its branch, and creates or updates its PR. Do not merge PRs. Preserve unrelated changes, exact source bytes and old schemas/fixtures; keep generated artifacts out of Git.

Documentation cleanup findings are in [the alignment review](../specs/documentation-alignment-review.md). Verified 62 Markdown files, 518 local links, balanced fences and Git whitespace. The archive blob exactly matches the prior main handoff (1b715637aed2ad37ff8c43692350b90706253840). That first cleanup portion changed only Markdown; the runner extension above separately verifies replay behavior.

See [repository file review](../specs/repository-file-review.md) for every keep/refactor/remove decision. Earlier documentation cleanup commits were 6db0b85 and 424319e; the runner extension is published in the same PR #12.
