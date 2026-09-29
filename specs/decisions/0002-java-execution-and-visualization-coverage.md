# ADR 0002: Separate Java execution from visualization coverage

Status: Accepted direction on 2026-09-18; not implemented or runtime-verified.

## Context and Decision

The user wants ordinary single-file Java with imports, a class, static main, helper methods, and common standard-library calls. A manual method allowlist for visualization must not become the definition of valid Java or a prerequisite for ordinary authoring.

Use the Java 21 compiler/runtime for language and library behavior. Keep the approved Main.java/Main.main starting convention. Grow visualization coverage through representative programs, starting with variables and arrays. Existing V1 methods/recursion and structure requirements remain intact.

Separate execution admission, instrumentation eligibility, execution outcome, and visualization completeness. Valid source that exceeds tracing coverage may execute without instrumentation in the same isolated environment and show console output with an explicit visualization-unavailable explanation. Compilation errors show compiler diagnostics; runtime errors show the exception and any usable trace. A tracing limitation is not a Java error.

For a run with partial capture, playback may use only an accurate recorded prefix. Stop before an unrecorded change could invalidate reconstructed state; never resume with guessed state. Report the affected construct/source location when known and explain why recording stops. Exact message fields and safe-boundary representation belong in the future contracts.

## Constraints and Consequences

- Choose the uninstrumented output-only path before executing the user's program when tracing ineligibility is known. Do not silently rerun a program after partial execution, replay its input, merge recordings from different attempts, or duplicate side effects. Runtime recorder failures remain explicit failures/incomplete capture.
- Keep isolation, execution-policy restrictions, entry validation, compilation, resource limits, cancellation, and cleanup for both execution paths. Unresolved execution-policy checks must not be bypassed because tracing failed. Compiler acceptance alone does not authorize execution.
- Existing exclusions for threads/concurrency, reflection, native calls, third-party dependencies, and submitted-code file/network access remain. This decision is not a promise to run arbitrary Java or trace every library implementation.
- A successful output-only run may have completed execution and unavailable visualization. A partial trace does not become a complete visualization merely because the process exits successfully.
- Method-level semantic specifications and tests remain necessary for transformations/adapters we implement. They are internal tracing work, not a demand to preapprove every normal library call with the user.
- Live console transport remains ADR 0001. Fallback needs contract fixtures for compiler errors, runtime exceptions, unsupported tracing, unavailable locations, safe partial prefixes, and no duplicate execution/input. No such tests have run.

## Rationale

This replaces blanket rejection for tracing limitations with useful program output and honest diagnostics. It preserves the stronger rule that visualization must never invent execution facts. The first implementation of fallback need not offer partial instrumentation: an explicitly unavailable visualization is sufficient when no safe prefix can be captured.
