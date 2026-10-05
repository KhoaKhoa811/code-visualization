# Documentation alignment review — 2026-10-05

Scope: user-approved review/refactor of prior files after PR #11 merged. Baseline main is 4f77bba. This is documentation cleanup, not implementation of the next capability.

## Findings addressed

| Area | Correction |
| --- | --- |
| Requirements and repository instructions | Clarify construct-based tracing, starter templates versus permanent eligibility, confirmed recorded playback, and bounded parser/instrumentation evidence |
| Architecture and ADR 0011 | Distinguish adopted direction from proposed implementation, existing prototype contracts/transport from unfinished application integration |
| Array-analysis and parser setup documents | Remove obsolete instructions to repeat setup or treat automatic recording as still wholly future |
| Conditional contract/proposal | Point to the implemented source-plan collector and PR #9 runtime proof instead of calling it future work |
| Analysis guide | Separate September 22 analysis-only evidence from later automatic recording; preserve actual commands and limits |
| Current memory | Replace the mixed 715-line handoff with a concise active checkpoint; preserve its exact prior contents in a same-directory archive so relative links still resolve |
| Navigation | Add a root reading guide separating product requirements, current status, specifications and historical evidence |

## Reviewed and intentionally preserved

The `.agents/` role definitions already separate analysis, instrumentation, orchestration and replay; their template/entry references describe conventions, not algorithm recognition. No role registration or configuration change is needed. No agents were launched.

Older experiment/recording guides and ADRs preserve the precise verified shapes and their dated test results. Rewriting those eligibility rules to describe unimplemented general support would be incorrect. Frontend, console, V1/V2 and method/recursion requirements remain unchanged. Selected dependency versions are not upgraded or re-researched in this task.

The bubble-sort proposal and ADR 0010 already state their supersession by ADR 0011; their source/checkpoint examples remain useful acceptance material. The proposed 128-event profile and draft-5 are not active. Existing Java, schemas, fixtures, test drivers and runtime limits remain unchanged.

## Verification and next boundary

Verified 62 Markdown files and 518 local links, balanced fences, Git whitespace and documentation-only changed paths. The archived handoff's Git blob matches main exactly: `1b715637aed2ad37ff8c43692350b90706253840`. No Java, Docker or browser tests ran; prior runtime evidence remains dated October 4. Both active memory files record publication separately.

The next proposed implementation remains the composable straight-line analyzer described in [the tracing review](composable-java-tracing.md). This cleanup does not authorize starting it.
