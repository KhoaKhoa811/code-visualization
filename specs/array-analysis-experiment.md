# Bounded array analysis experiment

Historical experiment scope: Maven/parser setup and analysis-only verification in Docker, 2026-09-22. Bounded automatic instrumentation was subsequently implemented; see the [analysis guide](../runner/analysis/README.md). The exact shape below remains this legacy experiment's contract, not the general authoring policy or next development task. Future coverage follows [composable tracing](composable-java-tracing.md).

## Input and eligibility

Analyze the exact UTF-8 bytes of Main.java using JavaParser and JavaSymbolSolver 3.28.2, explicitly configured for Java 21, tab width one, and a JRE-only reflection type solver in the pinned Java 21 worker. Never compile or load submitted classes to resolve symbols. Java compilation remains authoritative; parser success does not prove Java validity or execution admission.

The eligible experimental region consists of one local int[] initialized with at most 16 signed int literals, followed by a simple assignment between two literal-indexed accesses to that same binding. Names, values, comments and whitespace may vary. The public Main class contains only public static void main(String[] args), without a package, imports, annotations or other members. Two optional trailing development probes must match the existing FINAL/stdout and PROBE/stderr expressions exactly after syntax normalization and binding resolution. They are explicitly outside the candidate recorded region. Eligibility applies only to this experiment, never to a whole-program trace.

Other shapes are untested for this gate, not necessarily invalid Java. Required unresolved bindings/types block eligibility and are analysis diagnostics, not compiler errors. Entry-convention failures have their own category. Parser syntax diagnostics are distinguished from tool failures and operational limits. Raw Unicode escape sequences are conservatively untested for source mapping in this slice; ordinary Unicode, tabs, LF and CRLF are tested. No source normalization or automatic fallback execution occurs.

## In-process handoff

The result retains exact source text and SHA-256, a private original AST, immutable declaration/access/scope facts, entry facts, classified bounded diagnostics, completeness and experimental eligibility. A syntax-only AST clone is available for inspection; consumers must use the frozen original facts, not resolver metadata on clones. No AST serialization or public API schema is introduced.

Binding identities derive from source identity and declaration offsets. Lexical scope identities derive from original containment. They are not runtime object or frame identities. Each array access links to its resolved declaration and records array/element types. Candidate declaration, RHS read and assignment ranges describe possible recording sites, not executed events or values. Before consuming a result, compare its original-byte hash with the current source and reject stale source.

Access facts follow syntax traversal, not execution order. Scope facts cover unit/type/method/block containment in this slice. Declarations separated from those scopes by unmodeled loop/catch/lambda/try/switch/constructor boundaries have unknown scope IDs. Partial analysis must not imply complete Java scope coverage.

All locations use Main.java, one-based lines and UTF-16 columns, and exclusive ends. Stored offsets allow exact source slicing. JavaParser inclusive ends are converted against the original text, with tab size one. Invalid coordinates cause a tool failure rather than guessed positions. Diagnostics have locations only when trustworthy.

## Bounds and ownership

Input is at most 64 KiB, AST traversal at most 4,096 nodes, diagnostics at most 16 with 240-character messages, and the text report at most 64 KiB. The whole report is bounded before writing, so overflow cannot masquerade as a complete report. Limit outcomes are not Java syntax errors. Parsing itself may allocate before the node count is known; process limits remain essential.

The test worker uses the existing pinned Temurin 21 image, UID/GID 10001, no network or host mounts, read-only root, dropped capabilities, no-new-privileges, 512 MiB memory/no swap, one CPU, 128 tasks, and 64 MiB private /tmp. The analyzer JVM gets 256 MiB heap and a 2 MiB stack. The test driver allows 60 seconds for the whole test process, retains at most 1 MiB combined console output, forcibly removes its unique container on timeout/failure, and verifies removal. This bounds the controlled suite, not an integrated per-request production API. Production cancellation/admission/restart recovery remain unfinished.

Trusted analyzer/test code builds on the host through a pinned project-local Maven distribution. Submitted Java is parsed and test-compiled only in Docker. Dependency downloads happen during trusted setup; the analysis worker has no network. No dependency installation changes global PATH or host Java configuration.

## Acceptance

Test original and renamed examples, exact AST structure, resolved int[]/int and String[] entry types, declaration identity across same-named scopes, candidate ranges, tabs/CRLF/comments/supplementary Unicode, stale-source rejection and clone preservation. Cover syntax errors, unresolved names, unsupported but compiler-valid code, entry mismatches, signed int bounds, malformed UTF-8, and input/node/diagnostic/report bounds. Compile representative valid and invalid fixtures with Java 21 inside Docker, without running them. This does not prove automatic transformation, Java compiler-equivalent analysis, or broader Java visualization.

Verification results belong in the implementation README and both memory files after tests run.

Verified 2026-09-22: 18 analyzer acceptance cases and two trusted worker-limit probes passed inside Docker; every test container was removed. See the [implementation guide](../runner/analysis/README.md) for exact evidence and limitations. Temporary console files may overshoot between driver polls; retained per-command files are truncated to a combined one-MiB prefix after stopping. This test driver is not the production collector.
