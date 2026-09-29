# ADR 0005: JavaParser and JavaSymbolSolver as the prototype candidate

Status: analysis-only setup approved and verified on 2026-09-22; bounded automatic array transformation separately approved and verified on 2026-09-23. JavaParser/SymbolSolver 3.28.2 remain pinned. Broader Java transformation remains unverified.

## Decision and rationale

Use JavaParser with JavaSymbolSolver as the first candidate for the bounded automatic array-analysis/instrumentation experiment. JavaParser provides AST construction and manipulation; JavaSymbolSolver provides declaration/type resolution. This fits the agreed separation between original-source analysis and instrumentation of a separate representation. JavaParser 3.28.2 is the researched dependency candidate; verify and pin the artifact and its dependencies during approved setup. Configure the Java 21 language level explicitly.

The recommendation is based on official documentation, not comparative benchmarks or demonstrated compatibility with this project. Eclipse JDT offers binding resolution and ASTRewrite without modifying the original AST. JDK compiler APIs offer parsing and semantic analysis but need a separately designed rewriting layer. Revisit those alternatives if the candidate fails acceptance checks; do not build parallel engines without a concrete need.

## Boundaries and verification gate

Analysis must preserve original source/hash/AST and provide resolved binding/type/scope facts, original ranges, entry-point facts, diagnostics, completeness and eligibility. Instrumentation consumes this handoff and modifies a separate representation. Runtime identities and values still come from recording actual execution. Library parsing support does not imply that our application can trace every supported Java construct.

The first gate covers the reviewed single-array declaration/assignment and renamed/reformatted variants. Verify declaration resolution, stale-source rejection, helper-name collisions, original representation preservation, UTF-16/exclusive-end ranges, CRLF/tabs/comments/Unicode, and original-versus-generated behavior in Docker. Node copying and lexical preservation must not be assumed to preserve independent or current resolver metadata without tests.

Keep analysis/transformation in the bounded worker direction described by the architecture. Configure a fixed Java 21 resolution environment; never execute or load submitted classes as a resolution shortcut. Specify analysis time, memory, diagnostics and generated-output bounds before implementation. Compilation and execution remain isolated. Existing trace schema, FIFO, recorder limits and V1/V2 scope are unchanged.

The next separately approved scope should establish reproducible Maven/dependency setup and a small analysis component, with controlled Docker checks before automatic recorder insertion. No Maven build/wrapper files were found during the approval handoff; this is a repository observation, not a current host-tool availability test.

## Approved setup outcome, 2026-09-22

The user subsequently approved Maven setup, the small analyzer and Docker tests. [runner/analysis](../../runner/analysis/README.md) now uses checksum-verified project-local Maven 3.9.16, pinned JavaParser/SymbolSolver 3.28.2 and explicit transitive/build-plugin versions. Eighteen analyzer cases and two worker-limit probes passed; all test containers were removed. This verifies the analysis-only part of the gate. Original/generated semantic comparison and helper-collision behavior remain pending for automatic transformation.

The in-process handoff keeps original AST/source private, returns immutable facts and offers a syntax-only clone. This avoids an AST wire protocol before any consumer needs one. Resolver-specific declaration adapters are required: local/field toAst() can return a containing declaration rather than its exact declarator. The tests verify binding identity against exact original declarations. See [analysis specification](../array-analysis-experiment.md) for scope, statuses and bounds.

## Automatic transformation follow-up

Follow-up outcome, 2026-09-23: the [automatic array experiment](../automatic-array-recording.md) now verifies syntax-copy modification, helper collision avoidance, original/generated mappings and runtime comparisons for the same reviewed region. The transformer consumes frozen binding/site facts rather than resolving the modified clone. It reuses the existing recorder and trace fields; original-byte identity is checked before edits. Nine runtime cases and eight original/generated comparisons passed, along with manual recorder regressions. This advances the narrow feasibility gate without selecting a general Java transformation strategy.

## Sources

- [JavaParser project and setup](https://github.com/javaparser/javaparser)
- [JavaParser 3.28.2 release](https://github.com/javaparser/javaparser/releases/tag/javaparser-parent-3.28.2)
- [JavaParser language configuration](https://github.com/javaparser/javaparser/blob/javaparser-parent-3.28.2/javaparser-core/src/main/java/com/github/javaparser/ParserConfiguration.java)
- [Eclipse JDT ASTParser](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ASTParser.html)
- [Eclipse JDT ASTRewrite](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/rewrite/ASTRewrite.html)
- [Java 21 JavacTask](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.compiler/com/sun/source/util/JavacTask.html)
