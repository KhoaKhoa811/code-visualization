# Automatic array recording

The [classic for-loop extension](LOOP_RECORDING.md) adds 68 loop cases and draft-3 condition/scope recording alongside the 150 legacy cases. Its guide records current verification and limitations; dated counts below describe earlier increments.

The subsequent [standalone increment extension](INDEX_INCREMENT_RECORDING.md) adds i++ and ++i. Its expanded gate passed 150 cases/124 comparisons on 2026-09-28, plus result/replay and shared/manual checks. Counts and dated evidence below describe the earlier baseline.

Subsequent increments added [integer recording](INTEGER_RECORDING.md), [combined scalar/array recording](COMBINED_RECORDING.md), [variable-index recording](VARIABLE_INDEX_RECORDING.md), [index-update recording](INDEX_UPDATE_RECORDING.md) and [index-addition recording](INDEX_ADDITION_RECORDING.md). The same gate now generates 110 fixtures and compares 92 original/generated pairs. The nine original array cases and their draft-1 contract remain unchanged. The original array-only evidence below is historical; current results are recorded in memory.

This experiment connects the existing analyzer to source instrumentation and the existing Docker runner. The user writes only the original Main.java. For the reviewed shape, the transformer inserts recorder calls automatically into a separate source copy. See the [specification](../../specs/automatic-array-recording.md) for exact semantics and limits.

## Run the complete verification

From the repository root, with host Java 21, Node and Docker Desktop's Linux engine available:

```powershell
& ./runner/analysis/test-recording.ps1
```

This command builds trusted code with the pinned local Maven setup, runs analysis/transformation tests inside Docker, exports reviewed source pairs, and compares their execution through the existing restricted runner. It also checks draft-1 trace results, original/generated source ranges, forward/backward reconstruction and the manual recording regression cases. It does not parse or execute submitted Java on the host.

The smaller `test.ps1` now runs analyzer and transformation generation checks, including two driver-limit probes. It does not execute the exported source pairs; use `test-recording.ps1` for that proof. Neither command creates a frontend, API or general-source submission service.

## Files and data flow

1. [ArrayTransformer.java](src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) consumes a complete, source-matched analysis result. It verifies original sites/bindings, changes a syntax-only AST copy, chooses a helper name absent from original identifiers, appends the trusted recorder, and returns original/generated hashes and recording-site associations.
2. [TransformationAcceptance.java](src/test/java/dev/codeviz/analysis/TransformationAcceptance.java) checks rejection/preservation rules and generates nine reviewed fixtures in the isolated worker. Its base64 artifact lines are bounded development transport, not a public API or an AST serialization format.
3. [AutomaticRecordingTest.java](AutomaticRecordingTest.java) is a trusted host driver. It decodes only the fixed named test artifacts, invokes the existing RunnerHarness for generated/original source, verifies terminal outcomes and cleanup, and saves runtime evidence.
4. [check-recordings.mjs](check-recordings.mjs) validates generated draft-1 results and exact intermediate states using captured facts. It checks both source hashes and source-range associations, rejects stale source, and seeks backward without rerunning Java.

The shared recorder adds an overload accepting the resolved variable name as canonical JSON. The shared collector accepts an expected variable name, defaulting to `values` for old manual fixtures. Unicode names are ASCII-escaped on the existing transport and decoded normally by JSON consumers. Runtime variable/object identities remain `variable-1` and `array-1` for this one-binding/one-object experiment; they are not general alias or frame identity support.

## Example

Original source region:

```java
int[] values = {3, 1};
values[0] = values[1];
```

The generated copy opens recording, retains the array declaration, records the initialized array, and replaces the assignment with a write helper containing a read helper. Both successful operation events use original source ranges. The actual runtime states remain `[3,1]`, `[3,1]`, `[1,1]`.

Generation never substitutes expected array values into a trace. Values are read/stored by the running program. Array/index arguments preserve the supported expression's evaluation order. Side-effecting indices or RHS calls are rejected; no claim about arbitrary expression transformation is made.

## Cases and limits

| Case | Expected behavior |
| --- | --- |
| success | Three events; final `[1,1]`; unchanged output |
| renamed | `items` with `[9,-4]`; three events and final `[-4,-4]` |
| formatted | Unicode name, supplementary-character comment, CRLF/tabs/multiline expressions; original ranges remain correct |
| collision | Existing `__CodevizRecorder` and `__CodevizRecorder1` identifiers cause helper suffix 2 |
| failed-read | Declaration only; same exception type/message as original |
| failed-write | Declaration and RHS read only; same exception type/message as original |
| empty | Empty declaration followed by a failing read; one safe event |
| int-bounds | Exact signed int extremes in trace; three events |
| trace-limit | Lowered two-event budget; safe prefix and explicit trace limit |

Additional generation checks cover source/AST preservation, hash associations, original/generated call mappings, stale/ineligible handoffs, rejected side-effecting expressions, event-budget validation and 64-KiB generated-source overflow. Eight original/generated pairs are compared; the deliberately limited run is not expected to finish like its unlimited original.

Existing execution, FIFO, event and array caps apply. Generation uses the bounded analyzer worker. No tracing implementation can claim completeness beyond the reviewed declaration/assignment region and explicitly excluded development output probes. Recorder failures and hard limits may interrupt execution; no automatic fallback run occurs. Generated stack-frame locations differ from original locations; exception comparisons check type/message rather than full stack-trace equality.

Local ignored evidence is under `.results/automatic/`: each case has original source, generated source, raw captured data and validated result JSON. Fresh `.results/fixtures-<run-id>/batch-0.txt` and `batch-1.txt` contain the worker-generated fixtures; the old single bundle is no longer consumed. These are review artifacts, not durable application storage. `.results/acceptance.txt` and `.results/acceptance-addition.txt` contain static-analysis/transformation evidence, not proof that the exported programs ran.

## Verified result, 2026-09-23

`test-recording.ps1` passed end to end: 18 analyzer cases, transformation guard checks and nine generated fixtures, two analyzer-worker limit probes, collector stream regressions, nine automatic recording cases, eight original/generated comparisons, and nine draft-1/source-mapping/reconstruction checks. The seven manual recording cases, four manual original/generated comparisons and seven manual result checks also passed. All test-owned containers were verified removed. The separate seven-case output-only runner suite and simulated Docker classification suite were not rerun; execution orchestration was unchanged.

The evidence establishes automatic recording only for this reviewed array region. No scalar/control-flow/sorting expansion, API, UI, general submission admission or durable recovery was added.
