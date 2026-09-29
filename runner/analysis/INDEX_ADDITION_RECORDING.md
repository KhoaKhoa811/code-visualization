# Index-addition array recording

The subsequent [standalone increment extension](INDEX_INCREMENT_RECORDING.md) adds i++ and ++i. Its expanded gate passed 150 cases/124 comparisons on 2026-09-28, plus result/replay and shared/manual checks. Counts and dated evidence below describe the earlier baseline.

The [approved scope](../../specs/index-addition-array-recording.md) extends the index assignment to `index = index + decimal-int-literal`:

```java
int x = 8;
int[] values = {5, 2};
int i = 0;
i = i + 1;
values[i] = x;
```

This still records five operations. Step 4 records the committed i value and highlights the entire assignment. Step 5 stores x using that current index. Scalar reads and addition add no separate steps. Backward reconstruction restores the previous index independently from x and the array, without evaluating the expression again.

## Implementation

- [CombinedProbe.java](src/main/java/dev/codeviz/analysis/CombinedProbe.java) accepts only a binary plus with the resolved index binding on the left and an existing supported int literal on the right. The assignment target, operand and final subscript must refer to the same local index. Both operand types and the result must be int. Other expression forms remain ineligible.
- [ArrayAnalyzer.java](src/main/java/dev/codeviz/analysis/ArrayAnalyzer.java) carries immutable AdditionSites: expression/reference/literal source spans, binding identity, operator and result type. Existing Sites constructors and literal-assignment facts remain compatible.
- [ArrayTransformer.java](src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) checks those facts against the source-matched syntax copy. It retains the actual RHS and the existing guard-before-assignment/capture-after-assignment sequence. No constant substitution, wider arithmetic or repeated evaluation is introduced.
- Existing recorder, FIFO, operation-plan collector and draft-2 capture the resulting int without new production fields or helpers. The collector checks the final store against the captured index; it does not calculate the source expression.
- [check-recordings.mjs](check-recordings.mjs) checks the whole original assignment highlight and five source associations. Independent expected fixture values include signed wraparound; replay itself only consumes recorded events.

Java int overflow remains ordinary wrapped arithmetic. A wrapped invalid index is recorded before the array store fails, leaving four valid events. A wrapped zero can produce a successful store. Budgets 1–4 stop before the next operation; exactly 5 allows completion. Adding zero still produces an assignment event.

## Bounded fixture delivery

Run the complete gate from the repository root:

```powershell
& ./runner/analysis/test-recording.ps1
```

The previous 88-case bundle was near the existing 1-MiB cap. [test.ps1](test.ps1) now generates two batches in separate restricted workers: the unchanged 88-case group and 22 addition cases. Each worker retains the existing 60-second deadline, 1-MiB output bound and isolation policy. Each saved batch is also limited to 1 MiB.

Each invocation creates a fresh `.results/fixtures-<run-id>/` directory containing fixed `batch-0.txt` and `batch-1.txt` files. A pre-existing directory is rejected. The full gate passes that exact directory to [AutomaticRecordingTest.java](AutomaticRecordingTest.java); there is no fallback to the old single bundle. Both batches are validated before executing any fixture, using the complete fixed fixture-name allowlist, 11-field records, duplicate/missing detection across batches and aggregate totals. Submitted source never becomes a host command or chosen path.

The test driver loads at most two capped batches, limiting combined input bytes to 2 MiB; decoded strings and object overhead use additional memory. This is a test-delivery change, not a larger per-worker output allowance. Existing source caps and runner/trace limits remain unchanged. [FixtureBundleTest.java](FixtureBundleTest.java) verifies valid delivery and eight rejection cases, including duplicates across batches, unknown names, empty/malformed/missing content and oversized files. Its temporary files are removed after the check.

## Acceptance coverage

The gate contains 110 automatic runtime cases and 92 original/generated comparisons. Eighteen deliberately trace-limited cases use prefix assertions instead of comparison against an unlimited original run. The 22 additions cover changed names, positive/zero/negative steps, equal scalar values, first/last positions, empty arrays, invalid computed indices, invalid initial indices becoming valid, formatting/helper collisions, absent probes and budgets 1–5.

Explicit boundary cases verify maximum int plus 1, minimum int plus -1 and minimum int plus minimum int. [IndexAdditionAcceptance.java](src/test/java/dev/codeviz/analysis/IndexAdditionAcceptance.java) checks resolved types/bindings/operators and operand spans, original preservation, stale/missing/swapped facts and 30 unsupported forms inside Docker. Shared generation checks verify one RHS evaluation and guards before mutation for every new fixture.

Contract checks accept wrapped values with the existing draft-2 schema. Collector checks accept two wrapped extremes as four-event prefixes and wrapped zero as a complete five-event trace. Existing corrupt-prefix rejection checks remain active. All 110 result checks cover source hashes, source associations and forward/backward reconstruction; prior analyzer, worker-limit, collector and seven manual runtime regressions/four comparisons remain in the gate.

Verified 2026-09-26: the complete gate exited 0. All 110 automatic cases/92 comparisons, 110 result checks, analyzer/transformation/collector/contract checks, eight batch-delivery negatives, both worker-limit probes and seven manual cases/four comparisons/seven result checks passed. All owned containers were verified removed. Fresh batch sizes were 998854 and 269639 bytes; neither cap was increased. Separate output-only and simulated Docker-classification suites were not rerun because runtime orchestration was unchanged.

The previous attempt was interrupted without recoverable completion. Recovery restarted Docker and removed one verified stopped test container before the fresh run; previous partial artifacts are not completion evidence. The fresh transcript is `.results/index-addition-gate-d9835c4863324bb99294089f88f1c8e6.log`, with fixtures under `.results/fixtures-88e0aac04c95468a871724e3d9ea3f37/`. Local `.results/acceptance.txt` and `.results/acceptance-addition.txt` hold worker evidence; `.results/automatic/` holds actual execution and replay artifacts.

General arithmetic, alternate operand order, compound assignment, increments, expression subscripts, multiple updates, loops and a browser renderer remain outside this increment. No runtime dependency, schema, API or runner isolation change is introduced.
