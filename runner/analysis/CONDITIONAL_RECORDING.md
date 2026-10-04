# Conditional compare-and-swap recording

Status: implemented and verified, 2026-10-04. The user approved this runtime increment after merging contract PR #8. The full regression gate passed. This is a bounded Milestone 1 prototype, not sorting or app integration.

## Scope and behavior

The [approved design](../../specs/compare-swap-proposal.md) defines one int-array literal followed by a braced if comparing two literal-index reads with `>`, and exactly three body statements: read the left element into a local int temporary, assign the right element to the left, then assign the temporary to the right. Bindings may be renamed; array values/indices are signed decimal ints, arrays have 0–16 elements, and formatting/comments are preserved through original-source mappings. Existing reviewed final-output probes are optional. No variable/arithmetic indices, else, nested conditions/loops, aliases, arbitrary calls or methods are newly supported.

There are nine observable operations on true and four on false. Each original array read executes once, including the body's distinct initializer read. Captured operands feed one native comparison, and the recorded boolean also controls the branch. Successful writes capture stored values; the final write retires the temporary atomically. False never declares the temporary or retires the array binding. Playback uses the exact original source snapshot and shows no synthetic helper variables.

## Implementation boundaries

- `ConditionalProbe` adds a distinct immutable fact variant to the existing analyzer. It resolves array and temporary bindings, branch scope, types, literal indices and nine original operation sites. The existing `LoopProbe` declaration/reference/probe helpers are reused within the analysis package; loop facts retain their meanings.
- `ConditionalTransformer` validates the exact original bytes and rechecks frozen facts in the isolated analysis worker before lowering a syntax copy. It creates collision-free helper/local names, preserves the native comparison and target-before-RHS evaluation, and places guards before all nine operations. Generated helper calls map back to distinct original sites even when the source text repeats.
- `ConditionalRecorderMembers` augments the unchanged base recorder only for this producer. It adds draft-4 scopes, comparison operands/read links and the final exit list, reusing bounded FIFO transport and existing array operations. No older recorder template or schema changes.
- `ArrayTrace.Plan` is the minimal shared interface for loop and conditional validation (`accept` and `complete`). Existing loop behavior stays in `LoopTracePlan`. `ConditionalTracePlan` enforces the true/false path, literal indices, exact source sites, captured initializer/store values, comparison agreement and the only permitted retirement position. A rejected record changes neither phase nor array state. A normal end marker is accepted only at a complete path.
- `conditional-replay.mjs` reconstructs validated event prefixes. It applies recorded mutations/exits and selects recorded comparison/read details; it never evaluates Java. This is a prototype reducer without React integration.

The 64-KiB source/trace, 32-event, 16-element array, worker resource/deadline and one-MiB fixture batch bounds remain. Limited runs stop before the next guarded operation. Failed left/right condition accesses retain one/two events respectively and emit no successful comparison or body. Trace corruption/truncation retains only accepted records; exceptions do not fabricate normal scope exits. Ineligibility remains distinct from invalid Java and does not implement general output-only execution admission.

## Reproducible verification

From the repository root, using the existing pinned Java 21/Maven setup, Node 22 and Docker Desktop Linux engine:

```powershell
& ./runner/analysis/test-recording.ps1
```

The script builds trusted Java on the host, runs source analysis/transformation in bounded Docker workers, and executes submitted originals/generated programs only in isolated runner containers. It adds the conditional batch to the existing regression gate. Build success alone is not a passed runtime suite.

The new worker exports 30 reviewed fixtures: true/false/equal/same-index branches, left/right bounds failures, extreme indices/values, negative values, reversed/nonadjacent indices, 16 elements, renamed/formatted/collision/no-probe sources, and budgets 1–9. Eight limited cases check safe prefixes; the other 22 compare native original/generated outcomes, stdout/stderr and exception type/message. Generated structure checks ensure four original reads, one comparison, reuse of its boolean, nine guards, immutable facts and nine source mappings. Twenty-five excluded forms are checked.

Collector tests cover 38 malformed records with atomic valid retries, nine premature end points, extra body after false, byte cap and all 246 incomplete final-record byte prefixes. Fixture delivery tests reject eight malformed/missing/oversized variants before execution. Runtime result checks cover draft-4, exact hashes, nine static highlights and every forward/backward/standalone prefix against independent expected states. No runtime data is inferred from the UI.

Verified on October 4: the fresh full `test-recording.ps1` gate exited 0, with 328 automatic executions and 234 original/generated comparisons (30/22 conditional, 80/48 read loops, 68/40 scalar-fill loops and 150/124 earlier cases), plus seven manual cases/four comparisons. All result contract/source/replay checks, collector and fixture-delivery checks, and analysis-worker deadline/output-limit probes passed. Final Docker label inspection found no prototype or analysis containers. Earlier schemas, fixtures, Java example sources and recorder templates remain byte-identical across 40 compatibility files; documentation links/fences, PowerShell syntax and Git whitespace passed.

Ignored local evidence: `.results/conditional-gate-20261004-1.out.log`, `.err.log` (empty) and `.exit.txt` (0), with fresh fixtures in `.results/fixtures-9e0cef200a104f249c13148377aba607`. All seven fixture batches remain below 1 MiB; the conditional batch is 529628 bytes. October 3's interrupted broader gate was not counted as a pass; its focused 30-case proof preceded this complete rerun.

## Remaining work

General conditions, else, nested sorting loops, aliases, remaining Milestone 1 syntax/types, live Scanner, methods/recursion, Spring Boot and frontend integration remain unfinished. This increment does not certify arbitrary Java execution, hostile-source admission or public deployment readiness.
