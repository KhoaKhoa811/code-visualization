# Java Code Visualization

An educational Java application under development: write ordinary single-file Java and replay captured operations with synchronized source highlights and data-structure diagrams.

## Current status

Milestone 1 is active and incomplete. The repository contains bounded Java analysis, instrumentation, isolated Docker execution, trace validation and prototype replay. Spring Boot/frontend integration, live Scanner input and complete sorting playback are unfinished.

The latest full runtime gate, on October 4, 2026, passed 328 automatic cases/234 original-generated comparisons plus seven manual cases/four comparisons. These prove the documented prototype shapes, not arbitrary Java. See the [conditional recording guide](runner/analysis/CONDITIONAL_RECORDING.md) for evidence and limitations.

The adopted direction is [composable Java tracing](specs/composable-java-tracing.md): support statements/expressions and verified combinations, using algorithms as acceptance programs rather than exact templates. Its new analysis/runtime path is not implemented. Draft-1 through draft-4 remain current; draft-5 and larger budgets are unimplemented proposals under reconsideration.

## Reading order

1. [Requirements](requirements/PROJECT_REQUIREMENTS.md): product intent and confirmed decisions.
2. [Current state](memory/current-state.md): active checkpoint, approval boundary and next proposed task.
3. [Architecture](specs/architecture.md), [Java support](specs/java-support.md) and [trace semantics](specs/trace-format.md): intended behavior and versioned increments.
4. [Analysis/recording guide](runner/analysis/README.md), [runner guide](runner/prototype/README.md) and [contracts](contracts/README.md): implemented scope and reproducible checks.
5. [Progress history](memory/project-progress.md): dated evidence and prior decisions. Historical next steps are not current instructions.

Follow [AGENTS.md](AGENTS.md). New implementation work requires explicit user approval. Older experiment specifications intentionally preserve their narrow eligibility and historical verification; consult current status before applying their former next steps.
