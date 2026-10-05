# Array analysis prototype

Repository cleanup, 2026-10-05: existing analyzer/transformer and runner tests remain active. Loop/conditional replay shares state reconstruction, with an adapter compatibility check in `test-recording.ps1`. Root ignore rules replace redundant runner-local `.gitignore` files. See the [complete file review](../../specs/repository-file-review.md); no Java capability or runtime limit changed.

Current development direction: [composable Java tracing](../../specs/composable-java-tracing.md). The modules below are verified legacy experiments; their exact shapes are not permanent algorithm templates. The proposed composable analyzer is not implemented yet. See [current state](../../memory/current-state.md) before starting another task.

The prototype includes narrow [integer-variable](INTEGER_RECORDING.md), [combined scalar/array](COMBINED_RECORDING.md), [variable-index](VARIABLE_INDEX_RECORDING.md), [index-update](INDEX_UPDATE_RECORDING.md), [index-addition](INDEX_ADDITION_RECORDING.md), [standalone increments](INDEX_INCREMENT_RECORDING.md), [classic scalar-fill loops](LOOP_RECORDING.md), [loop read/addition](LOOP_READ_RECORDING.md) and [conditional compare-and-swap](CONDITIONAL_RECORDING.md) capabilities. Existing array class names remain for compatibility. The `test-recording.ps1` gate covers 150 legacy automatic fixtures, 68 scalar-fill loop fixtures, 80 read/addition loop fixtures and 30 conditional fixtures; this does not establish general Java support. The recording guides and memory distinguish implemented scope from completed verification.

This module reads original Java source and produces a bounded, in-process static-analysis handoff. A separate instrumentation package now consumes that handoff for [automatic array recording](AUTOMATIC_RECORDING.md). The analyzer itself does not insert recorder calls or execute source. The [analysis specification](../../specs/array-analysis-experiment.md) defines its exact scope. Existing manual recording remains under [prototype/recording](../prototype/recording/README.md).

## Build and test on Windows

Requires host Java/javac 21, PowerShell 5.1 or later, and a running Docker Desktop Linux engine. The existing pinned Temurin 21.0.12+8 linux/amd64 image must already be cached. The script fails if it is absent; it does not silently pull a replacement.

From the repository root:

```powershell
& ./runner/analysis/test.ps1
```

The script bootstraps Maven 3.9.16 under `.tools/` after checking its downloaded ZIP against a pinned SHA-512. It downloads build dependencies into `.tools/repository/`, builds only trusted analyzer/test code on the host, and runs analysis and selected Java-compiler checks inside Docker. It never changes global PATH. Dependencies and Maven need network access during initial setup; the worker does not have network access or host mounts.

This smaller script also checks automatic transformation and exports 328 reviewed source pairs: 150 earlier fixtures (nine array, eight scalar, 14 combined, 16 scalar-update, 19 variable-index, 22 index-update, 22 index-addition and 40 standalone increment), 68 scalar-fill loop fixtures, 80 read/addition loop fixtures and 30 conditional fixtures. It prints a fresh fixture directory containing seven batches, each capped at 1 MiB. To execute/compare those programs and validate their actual traces, run `test-recording.ps1` as documented in the recording guides.

Build alone, without source analysis or tests:

```powershell
& ./runner/analysis/maven.ps1 package
```

Maven's test phase is deliberately disabled here. A successful Maven build is not a passed analyzer suite; use `test.ps1`. This small prototype uses a plain Java acceptance driver rather than adding a test framework. The Docker image contains the analyzer JAR, trusted tests and reviewed source fixture. The analyzer CLI reads at most 64 KiB plus an overflow byte from stdin and prints a bounded review report. Invoke it only within a bounded worker; the script is the verified path.

## Files and responsibilities

| File | Purpose |
| --- | --- |
| [pom.xml](pom.xml) | Java 21 compilation, JavaParser/SymbolSolver 3.28.2, explicit transitive versions and build-plugin pins |
| [maven.ps1](maven.ps1) | Checksum-verified, project-local Maven bootstrap and invocation |
| [dependencies.txt](dependencies.txt) | Readable runtime dependency baseline; generated Maven tree is under target |
| [SourceSnapshot.java](src/main/java/dev/codeviz/analysis/SourceSnapshot.java) | Strict UTF-8 decoding, original SHA-256, stale-source guard and UTF-16/exclusive-end ranges |
| [ArrayAnalyzer.java](src/main/java/dev/codeviz/analysis/ArrayAnalyzer.java) | Original AST, binding/access/scope facts, entry convention and experimental eligibility/diagnostics |
| [AnalyzerMain.java](src/main/java/dev/codeviz/analysis/AnalyzerMain.java) | Bounded text report for review, not an API or trace format |
| [AnalyzerAcceptance.java](src/test/java/dev/codeviz/analysis/AnalyzerAcceptance.java) | Eighteen analyzer cases, compiler cross-checks and two trusted process-limit probes |
| [Dockerfile](Dockerfile) | Pinned Java 21 test image; no submitted source is compiled during image construction |
| [test.ps1](test.ps1) | Build, isolated worker configuration, deadlines, output limits, completion evidence and cleanup |

## What the result means

For the existing original example, the AST contains one class, one method, one array declaration and one assignment. Four array-access expressions include two accesses in the trailing development output probe. Every array access resolves to the same `values` declaration with array type `int[]` and element type `int`. The entry parameter resolves to `java.lang.String[]`.

Three candidate recording ranges identify `int[] values = {3, 1}`, the RHS `values[1]`, and the assignment `values[0] = values[1]`. These are static source sites, not executed operations. Access-list order follows AST traversal and must not be interpreted as runtime evaluation order. Actual events still come from the separate recording experiment.

The result keeps a private original AST and immutable semantic records. `syntaxCopy()` supplies a clone with symbol-resolver metadata removed; do not resolve or transform that clone under an assumption that its metadata is current. Frozen binding facts are keyed to the exact original SHA-256 and declaration offsets. Call `source().requireSame(currentBytes)` before consuming the handoff. The implemented transformers perform source/fact and helper-collision checks for their bounded shapes. Every future composable lowering must retain those checks.

`COMPLETE_FOR_ARRAY_PROBE` and `eligible=true` mean only that the reviewed experimental region has the required analysis facts. They do not establish compiler acceptance, execution admission, complete program tracing or successful execution. The trailing output probes are outside that region. Other forms receive partial/unavailable results with explicit diagnostics. Unit/type/method/block scope containment and the candidate classic-for scope are modeled; scope IDs are unknown across other unmodeled lexical boundaries rather than incorrectly assigned to an outer block. The loop path uses `COMPLETE_FOR_LOOP_PROBE` and dedicated loop facts.

## Limits and verification

Input: 64 KiB. AST traversal: 4,096 nodes. Diagnostics: 16 with 240-character messages. Review report: 64 KiB. Arrays in the eligible shape: at most 16 decimal signed-int literals. Raw Unicode escapes are not yet eligible for mapping. There is no source normalization, runtime-value inference, user-class loading, or automatic fallback execution.

Worker: UID/GID 10001, no network, read-only root, no host mounts, dropped capabilities, no-new-privileges, 512 MiB memory/no swap, one CPU, 128 tasks, 64 MiB private /tmp. JVM: 256 MiB heap and 2 MiB stack. Each of the seven acceptance workers has a 60-second process deadline. A separate trusted sleeping test verifies a lowered two-second deadline; another writes two MiB to verify the one-MiB console limit. The script polls temporary output files, so they can briefly exceed the limit; after stopping it retains at most one MiB combined per Docker command. This is a controlled test driver, not a production hostile-output collector or durable worker service.

Verified on 2026-09-22: all 18 analyzer cases passed, plus both process-limit probes. Tests covered original/renamed examples, AST shape, int[]/int/String[] resolution, same-named locals and field/local bindings, exact source slices, CRLF/tabs/comments/supplementary Unicode, original preservation, stale-source rejection, syntax/unresolved/untested/entry diagnostics, int boundaries, malformed UTF-8 and source/node/diagnostic/report caps. Selected valid and invalid fixtures were compiled with Java 21 inside Docker using `-proc:none`; none were executed. Docker inspection confirmed configured restrictions and successful test exit; all three final test containers were verified removed.

The test work found and corrected JavaParser's local/field `toAst()` declaration-wrapper behavior and a PowerShell 5.1 short-process exit-code issue. Repeated builds recreate the plain JAR before shading dependencies. Packaging uses the classpath; Maven reports overlapping module descriptors/manifests/license resources from dependencies. This is not a JPMS module build.

The September 22 analysis-only gate generated ignored local reports and did not run automatic recording. Later tasks added bounded automatic transformation and the full recording gate described at the top of this guide. Those historical analysis results are not the current total capability. Frontend, server integration, arbitrary-Java analysis and public-deployment readiness remain unimplemented/unverified.

Version/checksum sources: [Apache Maven downloads](https://maven.apache.org/download.cgi), [Maven 3.9.16 ZIP checksum](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip.sha512), [JavaParser 3.28.2](https://github.com/javaparser/javaparser/releases/tag/javaparser-parent-3.28.2). Runtime dependencies are pinned in the POM; the baseline file is a review aid, not a checksum lock for every transitive artifact.
