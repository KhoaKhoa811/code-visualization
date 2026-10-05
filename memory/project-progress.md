# Project progress and development record

## Documentation alignment - 2026-10-05

User merged PR #11; fetch confirmed main 4f77bba. Approved documentation cleanup on docs/requirements-alignment before any new implementation. Corrected template wording and recorded-playback status, adopted-direction versus implementation status, obsolete parser/setup and future-collector statements, and stale analysis-guide next steps. Added root navigation and specs/documentation-alignment-review.md. Role definitions and narrow historical experiment semantics remain valid and unchanged.

Replaced the 715-line current-state.md with a concise active checkpoint. Preserved its exact prior bytes as memory/current-state-history-20261005.md; this progress file remains the chronological record. The next proposed task is still separately approved composable analysis, not sorting-specific draft-5. No code, schema, dependency, limit or product scope changed.

Checks passed: 62 Markdown files, 518 local links, balanced fences, Git whitespace and archive blob equality against main (1b715637aed2ad37ff8c43692350b90706253840). All changed files are Markdown. No Java, Docker or browser tests ran. Publication is pending; complete checks, commit/push/open this documentation PR, then record its commit/link in both active memory files. Do not start implementation or merge automatically.

## Composable Java tracing review - 2026-10-05

Recovered the October 4 uncommitted documentation task on docs/composable-java-tracing. PR #10 is merged; origin/main remains c649980. The user clarified that differently written supported Java should be visualizable without an algorithm-specific template, then approved the review and its continuation. No product-code changes or new tests were lost; the design files were intact, but the previous memory entry still pointed to the old draft-5 next step.

Added specs/composable-java-tracing.md and proposed ADR 0011. Review found fixed statement-position/count eligibility, fixed loop/swap collector phases, and draft-4 assumptions about two preceding reads and one temporary retired at a write. Recommend recursive typed statement/expression capability analysis, composed lowering and source-plan validation, while reusing source mapping, Docker isolation, transport and recorded-fact replay. Algorithms become acceptance programs. Arbitrary Java and immediate quicksort support are not promised.

Superseded the immediate bubble-sort-specific draft-5 sequence in the proposal/ADR and architecture, Java, trace and isolation notes. Preserve PR #10's source/checkpoints as acceptance material, existing runtime behavior and all V1 requirements. No schema/limit change is active. Revisit general capture dependencies, enclosing-local writes, scope/frame lifetimes, atomic bookkeeping and capability-based budgets before new runtime production.

Next proposed task, requiring separate approval: implement a new composable analysis result for supported int/int[] declarations, plain assignments, parentheses, int addition/subtraction, array length and nested array reads in any supported sequence. Keep legacy probes and runtime paths unchanged. Then add lowering/runtime and control-flow capabilities through separately verified increments. Methods/recursion, aliases and remaining Milestone 1/V1 work stay required.

Documentation checks passed: 10 files, 198 local links, balanced fences, source/spec/role consistency and Git whitespace. The runner/contracts/frontend/backend diff is empty. No Java, Docker or browser tests ran; no new tracing coverage is claimed. Publication: review commit 61b6e54 pushed; [PR #11](https://github.com/KhoaKhoa811/code-visualization/pull/11) is open against main. These publication notes follow on the same branch. The approved documentation task is complete; await review and separate implementation approval. Do not start implementation or merge automatically.

## Bubble-sort design proposal - 2026-10-04

User reported PR #9 merged and approved a documentation-only design for the first complete bubble-sort example. Fetch confirmed origin/main a9f2af4 includes handoff 1c41c57; clean branch docs/bubble-sort-proposal starts there. No runtime, schema, dependency or limit change is authorized by this task.

Added specs/bubble-sort-proposal.md and proposed ADR 0010. The design composes two zero-based classic loops with variable-index conditional swaps, 15 static source sites and an expected 41-event reverse three-element trace. Proposed draft-5 distinguishes FOR/IF conditions, lexical scopes and runtime scope instances/parents, static declaration identities and fresh dynamic binding IDs. Repeated j/temp lifetimes cannot reuse retired IDs. Scope/binding exits stay atomic with existing observable operations. Separate source-plan validation and recorded-prefix replay remain required.

Proposed sorting-only cap: 128 events with unchanged 64-KiB trace/source, 16-element and execution-isolation limits; existing producers retain 32 events. These are design choices awaiting review, not implemented settings. Acceptance covers all four prefix/postfix combinations, complete small examples, limited larger arrays, exact highlights, all prefix/backward states, dynamic lifetime rejection and existing regression retention. Milestone 1 remains incomplete; aliasing, broader syntax/side effects and browser integration remain later work.

Updated Java-support, trace-format and execution-isolation notes. Documentation checks passed: seven files, 170 local links, balanced fences, 15 designed sites, canonical intermediate checkpoints and seven event-count cases. Git whitespace passed; the runner/contracts/frontend/backend diff is empty. These arithmetic checks are design expectations, not runtime evidence. No Java, Docker or browser tests were run for sorting. Publication: design commit 8002049 pushed; [PR #10](https://github.com/KhoaKhoa811/code-visualization/pull/10) is open against main. These publication notes follow on the same branch. The approved documentation task is complete. Next task recommendation is separately approved draft-5 contract/fixture work after design review. Do not implement or merge automatically.

## Conditional recording verified - 2026-10-04

The approved bounded compare-and-swap implementation is complete on feat/conditional-recording, based on merged PR #8 (d3fefdb). It adds separate conditional analysis/lowering, draft-4 recording, source-plan validation and prototype replay. True/false branches produce nine/four observable steps with original-expression highlights; final successful write retires the temporary atomically. No product code changed during October 4 recovery. Guide: runner/analysis/CONDITIONAL_RECORDING.md.

Fresh full runner/analysis/test-recording.ps1 gate passed (exit 0): 328 automatic cases/234 original-generated comparisons (30/22 conditional, 80/48 read loops, 68/40 scalar-fill loops, 150/124 earlier cases), plus seven manual cases/four comparisons. All result contract/source/replay checks, collectors, fixture-delivery tests and worker deadline/output probes passed. New checks include 25 excluded source forms, 38 corrupt records with atomic retries, nine premature endings, 246 final-record truncations and eight delivery rejection cases. Final Docker label inspections found no test containers.

Evidence is ignored locally under runner/analysis/.results/conditional-gate-20261004-1.* (empty stderr; exit 0), with fresh fixtures in .results/fixtures-9e0cef200a104f249c13148377aba607. All seven batches are below 1 MiB; conditional.txt is 529628 bytes. Forty earlier schemas/fixtures/Java sources/recorder templates are byte-identical; nine documents/198 local links/fences, PowerShell syntax and Git whitespace passed. The compatibility helper initially hit sandbox EPERM; its elevated read-only rerun passed. October 3's interrupted full gate was not counted as successful.

Updated the proposal, ADR 0009, Java/trace specifications and guides. Publication: implementation commit d594ff6 pushed on feat/conditional-recording; [PR #9](https://github.com/KhoaKhoa811/code-visualization/pull/9) is open against main. These final publication notes follow on the same branch. The approved task is complete; await review/merge and discussion of the next task. Milestone 1 remains incomplete: nested sorting loops, aliases and broader required syntax/types remain future increments. Spring Boot, frontend and live Scanner integration remain unfinished. Do not merge or begin another task automatically.

## Conditional verification recovery - 2026-10-04

The user requested memory/Git recovery and continuation of the already approved conditional-recording task. No current-file.md exists; memory/current-state.md is the handoff. Git still has the uncommitted implementation on feat/conditional-recording. The October 3 full gate ended after eight conditional cases without a final exit file; no Java/Docker CLI test process remains, so that gate is incomplete, not passed. Its earlier focused 30-case evidence remains recorded separately.

Docker 29.8.0 is available. Initial prototype/analysis label inspections found no leftover containers. Started a fresh full runner/analysis/test-recording.ps1 gate with output/error/exit capture under runner/analysis/.results/conditional-gate-20261004-1.*. This regenerates fixtures and checks all 328 automatic cases plus seven manual cases. No product code changed during recovery. Wait for the exit, then finish documentation, both memories, commit/push and a PR. Do not merge or begin another task.

Checkpoint: the fresh gate has passed all 30 conditional cases/22 comparisons, 80 read-loop cases/48 comparisons and 68 scalar-fill loop cases/40 comparisons, including their source/contract/replay checks. Collector, delivery and worker-limit tests passed. The 150 earlier automatic cases and seven manual cases are still running. Separate delivery checks passed: 40 compatibility files byte-identical, nine documents/198 local links/fences, PowerShell syntax and Git whitespace. Final gate exit and publication remain pending.

## Conditional recording implementation in progress - 2026-10-03

User merged PR #8 and approved the bounded Java conditional implementation. Fetch confirmed origin/main d3fefdb; active branch feat/conditional-recording. Added dedicated conditional analysis facts/lowering, draft-4 recorder additions, phase-aware collector through a small shared ArrayTrace.Plan interface, prototype replay and acceptance/delivery checks. Earlier schemas and recorder templates are unchanged. The implementation guide is runner/analysis/CONDITIONAL_RECORDING.md.

Focused verification passed: 30 isolated analyzer/transformation fixtures with 25 rejected forms; 30 Docker executions and 22 original/generated comparisons; all 30 draft-4/source/replay result checks. Collector checks passed 38 corrupt records with atomic retries, nine premature endings, false-branch rejection, trace byte cap and 246 final-record truncations. An initial trusted collector-test compile used the wrong existing EOF method name; corrected to finish() before these passes. Focused fixture batch: .results/conditional-dev-1/conditional.txt (529628 bytes).

Full recording gate is now active, with fresh workers/fixtures, the new 30 cases, all 298 older automatic cases and seven manual regressions. Logs: runner/analysis/.results/conditional-gate-20261003-1.out.log, .err.log, .exit.txt (written only when finished). Wait for its final exit and resolve any failures before claiming completion or publishing. Eight fixture-delivery rejection cases are included in this gate. No implementation commit or PR yet.

Milestone 1 remains incomplete. Nested sorting loops, aliases and broader required syntax/types remain future increments. No Spring Boot/frontend, interactive Scanner or arbitrary-source execution service was added. Complete only this approved task, update both memories and publish its PR; do not merge or start the next task automatically.

## Draft-4 conditional contract verified - 2026-10-03

Publication: implementation commit 6b9ebcd pushed; [PR #8](https://github.com/KhoaKhoa811/code-visualization/pull/8) is open against main. These final handoff notes follow on the same branch. The approved contract task is complete; the Java producer/collector task still needs confirmation. This supersedes the publication-pending checkpoint below. Do not merge automatically.

User reported merging PR #7 and approved continuing. Fetch confirmed origin/main at 4eac858; created feat/conditional-trace-contract from that merge. Scope is the next contract increment only, not Java instrumentation or app integration.

Added run-result-v4.schema.json, isolated conditional semantic checks, nine designed JSON fixtures with six exact Java source snapshots, and conditional contract/reconstruction self-tests. CONDITION uses IF role, captured typed operands and ordered links to the two preceding accepted reads. All events carry lexical scope; a write can retire one live branch-local int atomically. False conditions do not retire loop bindings. Generic validation checks data consistency; the future source-specific collector must enforce exact branch/swap paths, initializer/write dependencies and final-statement exit placement.

node contracts/validate.mjs --self-test passed all 21 fixtures and existing suites, plus 54 draft-4 rejection cases, all nine partial prefixes, atomic rejection, int extremes, renamed identities, source hashes/highlights and forward/backward fixture states. Draft-1/2/3 schemas, old fixtures and runtime producers remain unchanged; the previous unknown-version test now uses draft-99 and a separate rejection prevents relabeling old loops as draft-4. No Java/Docker tests, new runtime producer, streaming collector or frontend reducer were delivered. Milestone 1 remains incomplete.

Updated the proposal, ADR 0009, Java/trace specs and contract/fixture guides. Final checks passed: 20 earlier schemas/fixtures/source snapshots are byte-identical to origin/main; six documentation files, 114 local links, fences and Git whitespace passed. The first compatibility-check launch hit sandbox EPERM; the approved elevated read-only rerun passed. Publication is pending. Next discuss approval for the bounded conditional analyzer/instrumentation/recording/collector/replay implementation; do not start it or merge automatically.

## Conditional compare-and-swap proposal - 2026-10-03

Publication: proposal commit 6597609 pushed; [PR #7](https://github.com/KhoaKhoa811/code-visualization/pull/7) is open against main. These final publication notes follow on the same branch. The documentation task is complete; design approval and contract implementation remain pending. Do not merge or begin implementation automatically. This supersedes the publication-pending checkpoint below.

Confirmed PR #6 merged at a11f81f; the working tree was clean. The read-only review found no frontend/backend application and identified remaining Milestone 1 sorting/aliasing work. Revised the initial app-integration recommendation: complete tracing feasibility before Milestone 2 integration. User then approved a documentation proposal, both memory updates and a documentation PR, not Java/contract implementation.

Created specs/compare-swap-proposal.md and proposed ADR 0009 on docs/compare-swap-proposal from origin/main. Recommend one literal-index array comparison with > and a braced three-statement swap. Two operand reads precede one comparison/branch CONDITION; true path has nine steps, false has four. A branch-local temp retires atomically with the final successful write. Draft-3 assumes loop-condition retirement, so propose separately selected draft-4 with IF role, captured operands/read links, lexical scope and bounded write-associated retirement. Earlier schemas/producers remain unchanged.

The proposal includes exact source highlights, failure/limit prefixes, handoff responsibilities and contract/runtime acceptance criteria. Java 21 specification references were reviewed. No schemas, runtime code or committed tests changed; no Java/Docker tests ran. Documentation checks passed for four specification files, 46 local links, fences, the 0–9 step table and source expressions; Git whitespace checks passed. Publication is pending. Next obtain approval of the proposed steps/contract before implementing contract fixtures, then the bounded Java path. Nested loops/sorting, aliasing and remaining Milestone 1 coverage stay unfinished; live Scanner and other V1 requirements remain intact.

## Docker execution-evidence fix verified - 2026-10-03

User approved the runner follow-up. Implemented on fix/docker-exec-evidence: daemon SystemTime supplies the event cutoff after exec; event timestamps are validated; failures provide bounded metadata/counts without raw management text. Exact container/command/exec/exit checks, stage/overall budgets, output bounds, cancellation and cleanup remain enforced. No retries or submitted-code reruns. The original October 2 trigger is still unconfirmed; bounded Docker history and daemon clock movement remain limitations.

Fresh runner/prototype/test.ps1 passed (exit 0, empty stderr): 32 simulated lifecycle cases, seven actual Docker runner cases, stream/collector checks, seven manual recording cases, four original/instrumented comparisons and all seven generated contract/source/forward-backward replay checks. Added simulations cover host/daemon clock differences, malformed/unavailable clocks, management output overflow, invalid/future timestamps, duplicate creation, cancellation, management timeouts and the shared execution deadline. Diagnostic truncation/privacy also passed. Final Docker label inspections found no prototype/probe containers. Evidence: runner/analysis/.results/evidence-fix-gate-20261003-1.out.log, .err.log and .exit.txt.

The broader 298 automatic transformation cases were not rerun for this runner-only change; their October 1/2 evidence remains historical. No Java support, trace schema or frontend/backend integration was added. Milestone 1 remains incomplete. See specs/docker-exec-evidence-fix.md and the updated ADR 0003.

Fetch confirmed investigation PR #5 merged into origin/main at 6653796. Published implementation commit 7f96801 and opened [PR #6](https://github.com/KhoaKhoa811/code-visualization/pull/6) from fix/docker-exec-evidence to main. The PR is open and unmerged; these final handoff notes follow on the same branch. The approved task is complete. Do not merge or start the next task without confirmation.

## Docker execution-evidence investigation - 2026-10-03

Published investigation commit 4e9cd38 and opened [PR #5](https://github.com/KhoaKhoa811/code-visualization/pull/5) from investigate/docker-exec-evidence to main. The PR is open and unmerged. These final publication notes follow on the same branch. Investigation is complete; the proposed runner behavior change still requires user approval.

Confirmed PR #4 merged via fetch; synced main to ff84c1f and created investigate/docker-exec-evidence. User approved investigation only: inspect the runner, perform focused reproduction, propose a fix and update both memories. Production RunnerHarness/ExecEvidence are unchanged. Added a reproducible trusted diagnostic and specs/docker-exec-evidence-investigation.md.

Twenty uniquely numbered echo executions in a restricted Docker container passed immediate, delayed-same-cutoff and refreshed-cutoff evidence queries. A deliberately stale cutoff reproduced the exact missing-identity error for a successful command; this proves host-cutoff sensitivity but NOT the October 2 trigger. Original raw query/cutoff were not retained. Observed successful cutoff margins: 13.459564–17.229969 ms after daemon completion timestamps. Docker client/server 29.8.0. First probe startup exceeded a diagnostic-only five-second creation deadline; cleanup passed. Aligning that deadline with the runner's existing ten seconds produced exit 0; final inspection found no diagnostic containers. Captures are under runner/analysis/.results/evidence-probe-20261003-1 and -2. No application regression gate ran or was needed for this investigation.

Recommended next task, not yet approved: bounded failure diagnostics plus evaluation of a daemon-clock cutoff, tested for skew and evidence failures while retaining exact exec identity/exit checks, limits and cleanup. No blanket retries and no rerun of submitted Java. Original incident cause remains unconfirmed; do not claim a production fix. Publication is recorded above; obtain confirmation for the proposed runner change. First usable app integration remains a later proposed task.

## Loop array-read implementation verified - 2026-10-02

Published implementation commit 99378a4 on feat/loop-array-read and opened [PR #4](https://github.com/KhoaKhoa811/code-visualization/pull/4) against main. The PR is open and unmerged. These publication notes follow in a documentation commit on the same branch. The approved task is complete; discuss the next task before making further changes. The Docker execution-evidence intermittency remains an explicitly recorded follow-up.

Completed the approved bounded `values[i] = values[i] + signed-int-literal` loop extension on feat/loop-array-read. Both i++ and ++i record separate RHS read and whole-assignment write steps; the three-element example has 15 events. Dedicated immutable read facts, guarded single-evaluation lowering, draft-3 recording, phase-aware validation and forward/backward prefix replay are implemented. Schemas, earlier fixtures, original recorder templates and existing limits remain unchanged. No product code changed during recovery.

Verification is complete using matching evidence: October 1 supplied 80 new loop cases/48 comparisons and 68 scalar-fill cases/40 comparisons. October 2 regenerated all six fixture batches, proved exact sorted-line equality and matched all 148 saved loop results to fresh original/generated source and metadata. All their contract/hash/source/replay checks passed again. The full 150-case legacy suite/124 comparisons and seven manual cases/four comparisons then passed, including cancellation/blocked-pipe timeout. Total: 298 automatic cases/212 comparisons plus seven manual cases/four comparisons. Worker limits, collectors, delivery tests and contract self-tests passed. Final Docker label queries found no remaining analysis/prototype containers.

Recovery attempt 1 failed on legacy update-oob with missing Docker exec identity; later management evidence showed one successful compiler execution. Cause remains unconfirmed. One unchanged legacy/manual retry passed (exit 0, empty stderr). Keep this as a runner reliability follow-up; do not claim the first attempt passed or a new uninterrupted full-gate run. Evidence: runner/analysis/.results/loop-read-gate-20261001-1.out.log and loop-read-recovery-20261002-1.* / -2.*; the implementation guide records details. Both memory files and specification status are updated; publication is recorded above.

Milestone 1 remains incomplete. No frontend, Spring Boot integration, sorting, general loop support or broader V1 structures were added. After publication, discuss and confirm the next task; do not merge or begin another feature automatically.

## Loop array-read verification recovery - 2026-10-02

Recovery checkpoint: fresh workers, contracts, collector/delivery checks, all six fixture-batch comparisons and all 148 saved loop result/replay checks passed. Recovery attempt 1 exited 1 on legacy `update-oob`: INFRASTRUCTURE_ERROR, "Missing or ambiguous Docker exec identity". Cleanup passed. A later Docker management query showed a unique successful compiler execution for that container; the cause of the earlier missing evidence is unconfirmed. No harness checks were weakened and no product code changed. One retry of the full 150-case legacy suite plus manual cases is active under `.results/loop-read-recovery-20261002-2.*`, using fresh `.results/recovery-fixtures-2e784c05fe0446c3888074f64d41d0c3`. A repeated failure needs investigation, not unlimited retries.

User approved finishing verification and publishing the existing feat/loop-array-read task. October 1 logs confirm all 80 new read/addition cases/48 comparisons and 68 older loop cases/40 comparisons, with their replay checks. The wider run stopped during legacy index-update cases; stderr is empty but no final exit exists. No Java test process remained. Restarted Docker and removed the verified stopped prototype container 1113e074cc08 left by that interruption.

Recovery gate is running with native output/error/exit capture under runner/analysis/.results/loop-read-recovery-20261002-1.*. It regenerates all six bounded fixture batches and requires exact sorted-line equality with October 1 fixtures before reusing the 148 completed loop runs. It also ties each saved loop result to the fresh original/generated source and metadata, rechecks all loop replay results, and reruns the entire 150-case legacy group plus seven manual cases. Worker, contract, collector and delivery checks run again. No product code changed during recovery. Wait for the final exit, then update verification status, both memories, commit/push and create the PR; do not merge.

## Loop array-read implementation started - 2026-10-01

Latest checkpoint: all 80 new runtime cases/48 original-generated comparisons and all 80 draft-3/hash/source/replay checks passed, including 40 prefix/postfix pairs and every forward/backward prefix. Collector: 59 corrupt records, 15 premature ends, atomic retries, byte cap, 431 truncations. New fixture delivery: ten rejection cases. Full gate has moved to earlier scalar-fill loops and then legacy/manual cases; wait for its final exit before claiming completion or publishing. New batch sizes: 578964/578860 bytes; four older batches remain 998854/759159/495442/495368 bytes.

Checkpoint: new contract fixtures/self-tests, trusted Maven compilation, both isolated 40-fixture analysis workers (39 rejected forms each), collector corruption/truncation tests and fixture-delivery suites passed. Full gate is active: `.results/loop-read-gate-20261001-1.out.log`, `.err.log`, `.exit.txt` under runner/analysis; the exit file is created only on completion. Fresh fixtures: `.results/fixtures-6ed26f505e3b4678b77ccdf7f76e2681`. Early new runtime cases passed, including the fifteen-step example and three-event negative-index failures. Remaining new runs, replay checks and the complete legacy/manual regression gate are not yet confirmed. No commit or PR for this implementation yet.

User confirmed implementation after merging PR #3 and deleting its branch. Fetched main at 00a36ca and created feat/loop-array-read. Approved scope: natural array declaration + classic loop, RHS same array/index plus signed int literal, separate read/write steps, both increment forms, draft-3 compatibility and all prior regressions. Update both memories and publish a new PR after verification; do not merge. Work is in progress and no new runtime support is verified yet.

## Interrupted push recovered - 2026-10-01

Read both memories, the design proposal/ADR and Git state at the user's request. The proposal commit 605a48e was already remote; only documentation commit b56e6e7 (both memory files) remained local. Verified PR #3 was open/unmerged, checked the pending commit for whitespace errors, and successfully pushed b56e6e7 to docs/loop-array-read-proposal. No implementation code was missing or changed, and no runtime tests were needed or run. This recovery note follows on the same PR.

Current decision remains pending: confirm the proposed ARRAY_READ then ARRAY_WRITE stepping for values[i] = values[i] + literal before implementation. The prototype baseline and prior test evidence remain unchanged. Do not merge PR #3 or start implementation without confirmation.

## Loop array-read design review - 2026-09-30

User approved review and a design-only proposal after asking whether the actual app exists. Clarified that the engine prototype performs real analysis/instrumentation/Docker execution/replay, while the React UI and Spring Boot app are not built. Reviewed requirements, current loop facts/lowering/recorder/collector/replay, draft-3 schema and semantic validator. Draft-3 already represents ARRAY_READ; the specialized loop collector and reducer currently do not consume it.

Created proposed specs/loop-array-read-proposal.md and ADR 0008 on docs/loop-array-read-proposal. Recommend natural array-declaration + loop source, with body values[i] = values[i] + signed-int-literal, i++ or ++i. Propose separate RHS read and whole-assignment write steps, grouping addition with the write. Example {5,2,7} plus 1 has fifteen steps and ends [6,3,8]; normal false retires the index. Explicit plan identities keep this two-declaration shape separate from the existing scalar-fill variant. No schema change anticipated, subject to contract tests. Java evaluation/overflow references reviewed; runtime acceptance cases are proposed, not run.

These are proposals awaiting user confirmation, not newly supported code. No runtime, schema, fixture or application files changed; no Java/Docker tests ran. The verified baseline remains the 218 automatic cases/164 comparisons from the prior task. Documentation paths/fences, the fifteen-step table and Git whitespace checks passed. Published design commit 605a48e and opened [PR #3](https://github.com/KhoaKhoa811/code-visualization/pull/3) against main; final publication notes follow on the same branch.

Delivery update 2026-10-01: remote inspection showed PR #2 already merged and its feature branch deleted. Fetched origin/main (4588659), verified it includes f1eee95 and that this proposal differs by only four documentation/memory files. No merge was performed by this task. PR #3 is open; confirm the proposed stepping/source scope before implementation, and obtain separate approval before merging.

## Bounded classic for-loop verified - 2026-09-30

Completed the approved implementation on feat/classic-for-recording. LoopProbe supplies dedicated immutable binding/type/scope/site facts; LoopTransformer validates the exact-source handoff and lowers one classic for into guarded native operations. Loop-only helper members share the existing recorder transport, emit draft-3 conditions and retire the index atomically on final false. LoopTracePlan validates repeated condition/store/update phases; the prototype reducer reconstructs forward/backward prefixes from recorded values. Both i++ and ++i are supported for the specified single-array-store loop. Legacy recorder source, schemas and checked-in fixtures remain unchanged.

Fresh full gate `runner/analysis/test-recording.ps1` completed with exit 0: 68 loop cases/40 original-generated comparisons; 150 legacy cases/124 comparisons; all 218 contract/hash/source/replay checks; 34 new and 20 earlier prefix/postfix pairs; seven manual cases/four comparisons and seven manual result checks, including cancellation and blocked-open timeout. Existing analyzer/collector/contract guards, eight fixture-delivery negatives and both worker limits passed. Loop analysis workers each passed 45 rejected forms; loop collector passed 55 corrupt records, 13 premature ends and 223 atomic final-record truncations. Ten new loop fixture-delivery negatives passed separately; their future gate hook was added after the running script had loaded. No production code changed after the gate build.

Evidence: runner/analysis/.results/loop-gate-20260930-1.out.log, .err.log (empty), .exit.txt (0); fresh fixtures in .results/fixtures-23f8f7919e6e455eacfaccbc4bdec223. Four batch sizes: 998854/759159/495442/495368 bytes, each below one MiB. Every runtime/worker cleanup was verified; final Docker label queries found no analysis/prototype containers. Changed documentation paths/fences, PowerShell syntax and staged whitespace checks passed. The September 29 interrupted attempt is not a passed gate.

Updated the loop implementation guide, Java/trace specifications, ADR 0007 and related guides. Published implementation commit 79ed082 on feat/classic-for-recording and opened [PR #2](https://github.com/KhoaKhoa811/code-visualization/pull/2) against main. The PR is open, not merged. The branch includes the two earlier publication-note commits from PR #1 because it began at that handoff tip. These final publication notes are committed/pushed as a documentation follow-up on the same PR.

Milestone 1 remains incomplete: this is a bounded prototype without frontend/browser playback, sorting or general loop support. Later V1 requirements remain intact. The approved loop task is complete; next discuss and obtain confirmation for the next bounded increment. Do not merge or start another feature without confirmation.

## Loop implementation recovery - 2026-09-30

Latest checkpoint: all 68 loop executions/40 original-generated comparisons and all 68 draft-3 contract/hash/source/replay results passed, including 34 prefix/postfix pairs. The active full gate has moved on to the 150 legacy automatic cases; manual checks and final exit still remain. Implementation/contract guides and specifications now distinguish this runtime evidence from the older designed fixtures. No publication yet.

User approved the bounded classic-for implementation and Docker gate after repository setup, and explicitly requested resuming the interrupted work today. Active branch: feat/classic-for-recording, based on docs/repository-handoff. The loop work is uncommitted and unpublished; PR #1 remains the earlier handoff PR. Earlier entries saying loop implementation awaits approval are superseded.

Implemented but not fully verified: dedicated LoopProbe facts and candidate ForStmt scopes; LoopTransformer lowering; draft-3 recorder extension sharing the existing bounded transport; LoopTracePlan validation; prefix replay; 68 planned loop execution fixtures and contract/state checks. Legacy recorder source and schemas are unchanged. Trusted Maven compilation and a restricted Docker worker with 34 postfix-loop fixtures, fact/guard checks and 45 rejected forms passed on September 29. Full gate attempt loop-gate-20260929-1 has stdout through the two legacy analyzer workers, but no final exit file. Treat it as interrupted, not passed. Docker is stopped at recovery; no current Java test process was found.

Continue the approved task: inspect/finish tests, run a fresh full gate with explicit native stdout/stderr and exit capture, verify owned-container cleanup, update both memories/specification status, commit/push this branch and create its PR. Do not merge PRs or start another feature. Milestone 1 and frontend remain incomplete.

Recovery verification underway: Docker restarted; no containers remained for interrupted analysis run ddcb6edf27b0436098fe59260ffe22cc. Fresh full gate logs are .results/loop-gate-20260930-1.out.log / .err.log / .exit.txt (the exit file is written only on completion). Fresh fixtures: .results/fixtures-23f8f7919e6e455eacfaccbc4bdec223. Both loop analyzer workers passed, each with 34 fixtures and 45 rejected forms. Collector checks passed 55 corrupt records, 13 premature ends and 223 final-record truncations. A separate trusted LoopFixtureBundleTest passed valid 68-fixture delivery and ten rejection cases; added it to the gate for future runs after the active script had already started. Legacy batches remain 998854/759159 bytes; new loop batches are 495442/495368 bytes, all below one MiB. Runtime comparisons and replay checks are still in progress; no overall pass is claimed yet.

## Project baseline pushed to main - 2026-09-29

User clarified that the current files should go directly to main before a PR. Completed that request: main on https://github.com/KhoaKhoa811/code-visualization now contains baseline commit 40d4d36 (111 tracked project/instruction/source/contract/specification files). Initial commit c30d3e4 is its parent. Generated tools, builds and results are ignored; .gitattributes preserves exact source bytes. The earlier plan to review the entire baseline in a PR is superseded by this explicit direct-main request.

Created docs/repository-handoff from the published main for the follow-up PR, containing the final publication notes in both memory files. That PR reviews these notes, not baseline code already on main. Opened [PR #1](https://github.com/KhoaKhoa811/code-visualization/pull/1) from docs/repository-handoff to main. The PR is open and has not been merged. The previous chore/project-baseline branch also points to the baseline commit; it contains no extra code beyond main.

Git author is configured locally as KhoaKhoa811 / KhoaKhoa811@users.noreply.github.com. AGENTS.md records the standing task-branch push/PR workflow; merging PRs and starting the loop implementation still require separate approval. Existing contract/runtime verification records remain unchanged; no new Docker tests ran for repository setup.


## Git baseline preparation - 2026-09-29

Remote confirmed by user: https://github.com/KhoaKhoa811/code-visualization.git. Remote inspection returned no branches. User approved repository-local author KhoaKhoa811 / KhoaKhoa811@users.noreply.github.com. Created empty main root commit c30d3e4 and branch chore/project-baseline so the first PR can show the whole project. Added .gitignore for generated/local files and .gitattributes to preserve exact source bytes used by contract hashes. Publication is in progress; no push or PR success is claimed in this entry. Earlier missing-destination/author notes are superseded.

The standing workflow is recorded in AGENTS.md: after each approved task, update both memory files, commit, push the task branch and create/update its PR. Do not merge without separate authorization. No loop implementation or new Docker tests have started.


## Git delivery workflow requested - 2026-09-29

User requested creating Git, pushing the current project and opening a PR before further feature work. They also authorized pushing task branches and creating a PR after every subsequent approved task (or updating the same task's existing PR); merging remains unauthorized. Recorded this workflow in AGENTS.md and added root ignore rules for local tools, generated builds/results, environment files and credentials.

Initialized an empty local Git repository on main. There was no existing repository, remote or configured commit author, and GitHub CLI was not found on PATH. Asked the user for the remote destination (or owner/name/visibility for a new repository) and repository-local author name/email. These answers are pending. No commit, push or PR has been completed; do not claim publication. Prepare a reviewable baseline branch against the selected remote's actual base once its state is known. If the remote is new and empty, establish a minimal base before opening the baseline PR rather than pushing all project files directly to the PR base.

No loop implementation started. The contract review and its tests remain the latest verified work. Continue the requested Git setup/publication before discussing further feature work, and maintain both memory files.


## Draft-3 interruption/compatibility review - 2026-09-29

At the user's request, reviewed the saved work after a usage-limit concern against the loop specification, ADR 0007, schema, validator and fixture documentation. The contract implementation is present and complete within its approved scope. Reran node contracts/validate.mjs --self-test: exit 0, all ten fixtures and existing regression groups passed, including six draft-3 fixtures, 44 rejection cases and twelve partial prefixes. Rechecked SHA-256 against the pre-change values: both legacy schemas and four legacy fixtures remain byte-for-byte unchanged. No compatibility conflict was found in the reviewed contract work.

Draft-3 lifecycle checks are version-specific; draft-1/draft-2 retain their earlier behavior. The examples are designed contract traces, not Java runtime evidence. Condition truth/source-specific control-flow order and a loop-aware collector still require the future implementation. September 28 specification-only paragraphs remain explicitly historical beneath the September 29 contract status; do not interpret them as missing current schema work. No code/specification changes or Java/Docker tests occurred during this review. This entry records review results only and does not authorize loop implementation. Next remains the previously proposed bounded loop implementation and Docker gate, awaiting confirmation. Maintain both memory files.


## Draft-3 loop contract complete - 2026-09-29

User approved schema, example traces and validation tests after the loop specification. Added [run-result-v3.schema.json](../contracts/run-result-v3.schema.json), six illustrative loop results and four source snapshots under contracts/examples, plus loop-contract-tests.mjs and draft-3 support in validate.mjs. Accepted ADR 0007 at the contract layer. CONDITION records a typed boolean and scopeId; declarations require scopeId; every event carries exitedVariableIds. A false condition retires its one live int loop binding atomically; other events cannot retire bindings in this narrow contract.

Verified node contracts/validate.mjs --self-test (Node v22.23.2), exit 0: all ten checked-in fixtures, prior draft-1/draft-2 regression groups, six draft-3 results, 44 negative mutations, twelve partial prefixes, boolean/type guards, source hashes/ranges and independently expected fixture cursor states forward/backward passed. Prefix/postfix fixture facts match apart from their source ranges. Validator inputs remain unchanged on success and rejection. Checked unknown/wrong/duplicate/retired IDs, closed-scope reuse, missing exits, wrong booleans, cross-version rejection, sequences, ranges, boundaries and incomplete scope closure. SHA-256 checks proved both prior schemas and four prior fixture files byte-for-byte unchanged.

These fixtures describe expected events, not captured Java execution. They cover three iterations, prefix update, zero iterations, a limit before final false, failed first store and unavailable visualization. Source snapshots are not compiled or executed. Generic contract validation checks payloads and lifecycle consistency; Java condition truth, exact condition/body/update order and indirect index use belong to the future plan-aware collector. The test-only fixture reconstruction is not a frontend reducer. No Java, Docker or browser tests ran, and no dependency or runner code changed.

Updated contract/fixture guides, ADR 0007, loop specification, java-support.md and trace-format.md. Documentation links/fences checked. The last verified Java runtime baseline remains 150 automatic cases/124 comparisons plus shared/manual checks from September 28. Milestone 1 and frontend remain incomplete. Next proposed task, requiring confirmation: implement the first bounded for-loop analyzer/transformer/recorder/collector/replay path and run Docker acceptance/regression checks. Preserve older shapes/versions, 32-event and other limits, fresh bounded fixture delivery and explicit native-output logging. Always update both memory files. Earlier specification-only entries are historical and superseded for the contract layer only.


## First classic for-loop specification prepared - 2026-09-28

User approved reviewing the loop boundaries and writing specifications only. Created [for-loop-array-recording.md](../specs/for-loop-array-recording.md) and [proposed ADR 0007](../specs/decisions/0007-loop-condition-and-scope-draft-3.md); linked them from java-support.md, trace-format.md and contracts/README.md. No application code, machine-readable schema, fixtures or dependencies changed. No Java/Docker tests ran. Documentation paths/fences and the 13-step table were checked.

Proposed first shape: int x=8; int[] values={5,2,7}; for (int i=0; i<values.length; i++) { values[i]=x; }, also permitting ++i, allowed literal/name/format variations and exact optional probes without i after the loop. Six static sites produce 13 runtime steps in this example. Each condition records its actual boolean; the last false condition removes i without an extra click. Backward stepping restores i and array state independently. Broader loop forms, break/continue, nested loops and sorting remain later V1 increments.

Review found three extension points: ArrayAnalyzer deliberately leaves ForStmt scopes unmodeled; the transformer assumes fixed statements; ArrayTrace consumes a fixed operation list. The specification proposes explicit loop/scope/binding/site facts, scoped lowering with one evaluation per original operation, and a bounded loop phase plan that validates recorded events instead of predicting/unrolling them. Existing nine shapes remain supported.

Draft-2 cannot represent conditions or scope exit. Proposed draft-3 retains the result envelope, adds CONDITION with a typed boolean and scopeId, scopeId on declarations and atomic exitedVariableIds on each event. Only the false condition retires the index in this bounded shape. One event remains one step; old versions remain unchanged. ADR 0007 is proposed, not implemented or separately approved for code. Full method/frame teardown and exception unwinding remain outside this increment; partial playback retains the last safe snapshot without fabricated exits.

Retain the 32-event/trace/source/array/Docker caps. A successful m-iteration loop needs 4+3m events: nine iterations fit in 31, while a ten-iteration traversal stops at 32 before its tenth update. Specified zero iterations, failures, budgets 1-13, repeated source mappings, corrupted conditions/scope effects, original/generated comparisons and retention of all existing regressions. Future fixture delivery may need another bounded batch; never raise caps or drop prior cases. Future gate logs must explicitly capture native Java/Node output, as the previous transcript omitted it.

Next proposed task, requiring confirmation: implement draft-3 schema, representative fixtures and validator checks while preserving draft-1/draft-2. After that contract gate, discuss loop implementation and Docker verification. The last verified runtime baseline remains 150 automatic cases/124 original-generated comparisons plus shared/manual checks from the completed increment below. Milestone 1 and frontend/browser playback remain incomplete. Always update both memory files.


Logging note: PowerShell Start-Transcript omitted native Java/Node output. Successful exit 0 and final native summaries were observed through execution session 60189 and saved in runner/analysis/.results/index-increment-completion-20260928.md. The transcript alone is not complete runtime evidence; future logging must explicitly capture native output.

## Standalone prefix/postfix increments complete - 2026-09-28

Implemented and verified the approved fourth-statement extension: either i++; or ++i; in the five-operation indexed array shape. Both retain the original unary statement exactly once, guard before mutation, then capture the updated plain index as VARIABLE_WRITE to variable-3. The highlight is the whole original unary expression, excluding the semicolon. Example: i=0 becomes 1, then values[i]=x produces [5,8]. Backward stepping restores the array and then the index independently. Guide: [INDEX_INCREMENT_RECORDING.md](../runner/analysis/INDEX_INCREMENT_RECORDING.md); scope: [index-increment-array-recording.md](../specs/index-increment-array-recording.md).

Fresh runner/analysis/test-recording.ps1 exited 0. All 150 automatic runtime cases (110 prior + 40 new), 124 original/generated comparisons, 150 contract/hash/source/forward-backward result checks and 20 prefix/postfix fact comparisons passed. Twenty-six deliberately limited cases use prefix assertions. Both operators passed int wraparound, negative/out-of-bounds/empty stores, invalid-to-valid indices, first/last positions, equal/repeated values, Unicode/comments/tabs/CRLF, helper collisions, absent probes and budgets 1-5. Postfix capture is explicitly checked against recording the old value. Failed stores retain four events, including the committed update.

Passed the original 18 analyzer checks and all earlier transformation guards; new checks verify resolved operator/binding/type/ranges, original preservation, stale/missing/swapped/ambiguous facts, exactly one increment, guard/capture order and 30 rejected forms per operator. Collector/contract regressions, eight fixture-delivery negatives, both worker-limit probes and their cleanup passed. Seven manual runtime cases/four comparisons and seven result checks passed, including cancellation and blocked-FIFO timeout. Every owned test container was verified removed. Separate output-only/classification suites were not rerun because runtime orchestration did not change.

Evidence: runner/analysis/.results/index-increment-gate-73c7187d08174021b22b90319f50aecd.log; fresh fixtures: runner/analysis/.results/fixtures-748ca338165444bf87159c7043612f41. The two batches contain 88/62 cases and are 998854/759159 bytes, below unchanged 1-MiB bounds. Docker was stopped initially; restarted it under existing authorization before the successful full gate. No interrupted test run or orphan recovery was needed in this session.

Implementation adds immutable IncrementSites and extends CombinedProbe/ArrayTransformer at the existing update boundary. Recorder, collector, trace schema, dependencies, API and isolation policy are unchanged. Updated guides/specification status and checked documentation links/fences. All nine bounded program shapes remain supported. Milestone 1 and frontend/browser playback remain incomplete. Embedded increments, decrements, other operand types/bindings, multiple updates and loops/sorting remain future work. Discuss and confirm the next bounded task before acting; always update both memory files. Earlier underway/specification-only entries below are historical and superseded by this completion record.


## Standalone increments implementation underway - 2026-09-28

Restored both memory files and relevant specifications after user approval to continue implementing. Added explicit immutable IncrementSites, narrow standalone prefix/postfix resolution, and transformer checks preserving the original unary statement with a guard before mutation and plain-variable capture afterward. Added 40 planned runtime cases (20 per operator), unsupported-form/fact checks and paired replay/source assertions. Existing 110 fixtures remain; planned gate totals are 150 cases/124 original-generated comparisons, with batches of 88 and 62 under unchanged limits. Recorder, collector, schema and dependencies are unchanged. The full gate is running: transcript runner/analysis/.results/index-increment-gate-73c7187d08174021b22b90319f50aecd.log, fixtures .results/fixtures-748ca338165444bf87159c7043612f41. Analysis, transformation, collector, contract, eight delivery negatives and both worker-limit probes passed. Batches are 998854/759159 bytes. Runtime comparisons and final replay/manual checks remain pending; the last complete baseline remains 110/92. Keep both memory files updated with actual outcomes. Milestone 1 and frontend remain incomplete.


Last updated: 2026-09-26.

September 26 standalone increment scope specified: user requested both i++ and ++i and approved documentation only. Added [the specification](../specs/index-increment-array-recording.md), Java-support/trace links and memory notes. Both forms occupy the fourth statement of the existing indexed shape, capture one committed variable-3 update and preserve their exact original highlight. Instrumentation must guard, retain the original unary statement, then record plain i; never capture the discarded postfix result or increment twice. Embedded forms, decrements and loops remain outside this scope.

Specified overflow, independent replay, failure prefixes, budgets 1–5, resolved operator/operand/binding/type/source facts and ten acceptance groups preserving the 110-case/92-comparison baseline and shared/manual regressions. Retain bounded fresh batches and duplicate/missing checks. Reviewed existing code/stepping and Java SE 21 semantics; documentation links/fences checked. No implementation or runtime tests. Implementation of both forms and Docker verification require separate confirmation; Milestone 1 and frontend remain unfinished. Keep both memory files updated.

September 26 index-addition increment complete: recovered memory and finished the approved task. The indexed five-operation shape now accepts index=index+decimal-int-literal, with a single RHS evaluation, Java int wraparound, whole-assignment highlight and independent backward restoration. Reused the recorder/draft-2/operation plan; added immutable addition facts and narrow analyzer/transformer checks. [The guide](../runner/analysis/INDEX_ADDITION_RECORDING.md) explains implementation and bounded batches.

Fresh full gate exited 0: 110 automatic runtime cases/92 original-generated comparisons, 110 contract/source/replay checks, earlier analyzer/transformer guards plus 30 rejected addition forms, new binding/type/operator/span/preservation/guard checks, collector/contract wraparound coverage, eight fixture-batch negatives and both worker-limit probes passed. Seven manual runtime cases/four comparisons/seven result checks passed. All test-owned containers were verified removed. Eighteen intentionally limited runs use safe-prefix expectations. Max+1 and min+-1 overflow produce four-event failure prefixes; min+min wraps to index zero and succeeds. Two fresh batches (88/22 cases, 998854/269639 bytes) retain all prior cases under the existing per-batch/per-worker 1-MiB caps.

The earlier interrupted run had no recoverable completion and left one stopped project container. Recovery restarted Docker, verified and removed that container, then ran the full fresh gate with saved transcript runner/analysis/.results/index-addition-gate-d9835c4863324bb99294089f88f1c8e6.log and fixtures .results/fixtures-88e0aac04c95468a871724e3d9ea3f37. No dependency, recorder, schema, API or runtime isolation change. Separate output-only/classification suites were not rerun because runtime orchestration was unchanged. Documentation checks passed. Milestone 1 and browser frontend remain unfinished; further arithmetic/loops require discussion and approval. This completion supersedes pending entries below; maintain both memory files.

September 26 index-addition recovery: reread memory/specifications at the user's request. The interrupted run had generated both batches and left artifacts through the prior 88 cases, without addition runtime results or recoverable final status. Docker was stopped; restarted it under existing approval, verified and removed one stopped test-owned container. Started a fresh full gate with transcript .results/index-addition-gate-d9835c4863324bb99294089f88f1c8e6.log. Fresh build/contracts, both analysis batches, 30 addition rejection cases and fact/guard checks, worker limits/cleanup, collector/wraparound and eight batch-delivery negative checks passed. Runtime/final verification remains pending. Added [the implementation guide](../runner/analysis/INDEX_ADDITION_RECORDING.md); do not mark the increment complete yet.

September 26 index-addition implementation underway: user approved implementation and Docker tests, including bounded batches. Added resolved addition facts and transformer checks while retaining the actual Java expression. Added 22 arithmetic fixtures and batch-delivery validation; planned full gate 110 automatic cases/92 comparisons plus shared/manual regressions. No schema/recorder/dependency change. Tests have not completed; last verified baseline remains 88/74. Both memory files must record the final outcome.

September 26 index-addition scope specified: user approved documentation only. Added [the specification](../specs/index-addition-array-recording.md), plus Java-support/trace links, for index=index+decimal-int-literal in the five-operation shape. The assignment records one committed variable-3 write, preserves Java int overflow, evaluates the RHS once and highlights the whole original assignment. Backward replay restores the old index; invalid computed indices retain four events. Ten acceptance groups cover bindings/source facts, overflow, limits, replay and preservation of the existing 88 cases/74 comparisons and shared/manual checks.

The 998854-byte fixture bundle is close to its unchanged 1-MiB cap. Proposed implementation includes bounded fresh batches if needed, retaining every prior case and aggregate duplicate/missing-case checks. No implementation or runtime tests occurred; reviewed current code, stepping rules and Java SE 21 JLS addition semantics, and checked documentation links/fences. Implementation and Docker tests await separate approval. Milestone 1 and frontend remain unfinished; both memory files must stay updated.

September 26 index-update increment complete: implemented the approved five-step example (declare x=8, values=[5,2], i=0; assign i=1; store values[i]=x). Step 4 updates variable-3 independently from x; step 5 uses the actual updated index. Backward replay restores the array and index separately. [The implementation guide](../runner/analysis/INDEX_UPDATE_RECORDING.md) explains the shared analyzer, transformer, binding-aware recorder and operation-plan collector. No schema, dependency, API, isolation or orchestration changes.

Fresh runner/analysis/test-recording.ps1 exited 0: all 88 automatic runtime cases/74 original-generated comparisons, 88 result/source/reconstruction checks, original analyzer/transformation checks, new five-site/binding/guard/preservation checks and 27 rejected inputs passed. Collector checks passed 32 new corrupt prefixes and wrong-plan rejection; draft-2 checks added six negative cases. Both worker-limit probes and seven manual runtime cases/four comparisons/seven result checks passed. All owned containers were verified removed. Invalid updated indices retain four events; invalid initial indices changed to valid succeed; budgets 1–4 retain exact prefixes and 5 completes. Fourteen intentionally limited cases are excluded from unlimited-run comparisons.

The initial attempt passed trusted build/contracts but found Docker stopped; startup was approved and the full rerun passed. The 88-fixture bundle was 998854 bytes under the unchanged 1-MiB cap. Earlier literal-index-update rejection was replaced with explicit positive coverage, while compound updates remain rejected and all previous runtime cases remain. Separate output-only/classification suites were not rerun because orchestration was unchanged. Documentation links/fences checked. Milestone 1 and frontend remain unfinished; the next increment requires discussion and confirmation. These completed results supersede pending entries below; keep both memory files updated.

September 26 index-update implementation underway: user approved the code changes and Docker gate. Shared analysis/transformation now accepts a literal update to the separately declared index; recording/collection targets variable-3 and retains value variable-1. Added 22 fixtures, planned 88 total automatic cases/74 comparisons, and focused source/binding/guard/contract/corrupt-prefix checks. Verification is pending; do not treat this as completed. Last complete baseline remains 66/56. No schema, dependency, API or isolation policy change.

September 26 index-update scope specified: user approved documentation only. Added [the five-step scope and acceptance cases](../specs/index-update-array-recording.md), linked from java-support.md. The example declares x=8, values=[5,2], i=0, updates i=1, then stores x at values[i]. The update records variable-3 independently from x; replay restores the old index. Specified source mapping, failed-store prefixes, budgets 1–5, binding-aware recorder/collector changes and twelve acceptance groups preserving the 66-case/56-comparison baseline and shared/manual checks. Existing draft-2 is sufficient. No code changes or runtime tests; implementation and Docker verification await separate confirmation. This supersedes the pending scope discussion below. Milestone 1 remains incomplete.

September 26 restoration: reviewed the saved memory, variable-index specification and implementation guide. No code changes or tests ran. The verified baseline remains 66 automatic cases/56 comparisons plus the shared and seven manual regressions. The next proposed discussion is a literal index update (i=0; i=1;) before values[i]=x, producing five steps. Scope specification and implementation remain unapproved; continue discussing and confirming each task first.

September 25 variable-index increment completed: the [new guide](../runner/analysis/VARIABLE_INDEX_RECORDING.md) explains the implementation. The program declares x=8, values=[5,2], i=1, then stores x at index i, producing [5,8] in four steps. Two scalar declaration events retain separate identities and highlights; backward reconstruction removes only the matching binding.

Fresh complete gate exited 0: 66 automatic runtime cases, 56 original/generated comparisons, all 66 result/source/reconstruction checks, original and new analyzer guards, both worker-limit probes, contract/collector checks and seven manual recording cases/four comparisons/seven result checks passed. New index checks include 21 rejected input forms, 25 corrupt prefixes and six contract negatives. All owned containers were verified removed. Invalid indices preserve three declarations; budgets 1/2/3 preserve their exact prefixes and budget 4 completes. The separate output-only/classification suites were not rerun because orchestration was unchanged.

Reused parser, transformer, FIFO, runner and draft-2. Added bounded binding-aware scalar capture/collection and operation-specific source mapping; no dependency or schema version changed. The first attempt found Docker stopped; startup was approved before the successful run. Milestone 1 and the frontend remain unfinished. Index/scalar updates within the new shape, arithmetic and loops require further confirmed scope. No next implementation is authorized. Earlier pending entries below are historical.

September 25 variable-index implementation underway: approved code changes add a separate index binding, repeated-kind source associations and bounded binding-aware recording/collection. [The guide](../runner/analysis/VARIABLE_INDEX_RECORDING.md) explains the files. Docker was initially stopped, then started with approval. Analysis generated all 66 fixtures; binding/source/guard checks, 21 unsupported index inputs, worker limits, contract tests and 25 new collector corrupt-prefix checks passed. Runtime comparisons and remaining regressions are still running; the increment is not yet marked complete. Existing draft-2 and runner orchestration remain unchanged.

September 25 variable-index scope specified: created [scope and acceptance cases](../specs/variable-index-array-recording.md) under documentation-only approval. Proposed program declares x=8, values=[5,2], i=1, then writes values[i]=x, ending at [5,8]. Four steps retain two independent scalar bindings and their distinct declaration highlights. The specification covers runtime index/value capture, backward states, failures, limits and 12 acceptance groups. Existing kind-keyed source maps and the single scalar slot must be extended within the current pipeline. Implementation and Docker tests remain pending confirmation; no runtime tests ran for this task and the verified 47-case/40-comparison baseline is unchanged.

September 25 recovery: the scalar-update increment is complete. The four-step example records x=3, array=[5,2], x=8, then array=[8,2], with independent backward restoration. September 24 output confirmed 47 automatic runtime cases, 40 original/generated comparisons, all 47 reconstruction checks and the shared analyzer/contract/collector/worker-limit checks. Seven manual artifacts were saved after their driver's behavior and cleanup assertions; their generated-source hashes were checked during recovery. Both result checkers passed again on September 25 (47 automatic/seven manual), without rerunning Java or Docker. The previous process exit code was no longer available; this recovery combines observed runtime output with saved artifacts and fresh result validation. The earlier pending notes below are historical.

No new feature was implemented during recovery. Specifications and memory now reflect the completed increment. A possible next scope is a variable-index array write with a separate int index, followed later by arithmetic and loops. Further scope/design/implementation requires confirmation; Milestone 1 and the frontend remain unfinished.

September 24 scalar-update implementation underway: user approved implementation and Docker tests for the four-step program. Extended the existing analyzer/transformer/collector and test reconstruction; reused recorder operations and draft-2 unchanged. All 47 source pairs were generated in Docker; analyzer/guard-placement, worker-limit, contract and collector checks passed. New checks include 20 rejected source forms, 23 corrupt prefixes and six contract negatives. Runtime comparisons and remaining regressions are still running; completion is not yet claimed. See the [updated guide](../runner/analysis/COMBINED_RECORDING.md).

September 24 next scope specified: under documentation-only approval, created [scalar update before an array write](../specs/scalar-update-array-recording.md). The example adds x=8 between array creation and the array store, producing four steps and final array [8,2]. It defines exact syntax, distinct identities, backward states, three-event failed-write prefixes, budgets 1/2/3 and successful completion at budget 4. Twelve acceptance groups include all earlier regressions. This task changed documentation only; implementation and Docker tests remain pending confirmation. The verified baseline is still 31 automatic cases/27 comparisons from the completed three-operation combined increment.

September 24 combined increment completed and verified: [the implementation guide](../runner/analysis/COMBINED_RECORDING.md) explains the new code. The approved example now records x=3, then values=[5,2], then values=[3,2], with separate scalar/array identities and three source highlights. Backward reconstruction removes the array while retaining x, then removes x, using recorded facts only.

Tests passed: 31 automatic runtime cases (14 new combined), 27 original/generated comparisons, all 31 contract/source/reconstruction checks, shared collector/contract regressions, two worker-limit probes, seven manual recording cases/four comparisons and seven manual result checks. Failed combined writes retain two events; trace budgets one/two retain one/two. Fifteen mixed corrupt-prefix tests and six mixed contract-negative tests passed. A final analyzer-only Docker run passed additional missing-fact/probe-shadowing guards and 18 out-of-scope combined inputs. All owned test containers were removed. The separate output-only/classification suites were not rerun; their orchestration code was unchanged.

The same parser, transformer, runner and FIFO transport are reused; no dependency/schema change. A fixed test-output probe required exact JDK declaration lookup because JavaParser found Arrays.toString overloads ambiguous. General library tracing remains unsupported. Milestone 1, arithmetic/loops/sorting and the frontend remain unfinished. No next task is approved; discuss the next bounded capability first.

September 24 implementation underway: user approved the combined increment and Docker tests. Added [combined eligibility and recording](../runner/analysis/COMBINED_RECORDING.md), distinct scalar/array IDs and independent test reconstruction. Existing draft-2 accommodates the events unchanged. Initial analyzer tests found an Arrays.toString overload-resolution ambiguity; a bounded exact-JDK probe lookup fixes this without adding general library tracing. Analysis generated 31 fixtures; worker limits and shared collector/contract checks passed. Runtime comparisons and a final expanded analyzer check remain in progress; no completion claimed yet.

September 24 specification completed: the user approved documentation-only work for [combined variable/array recording](../specs/combined-variable-array-recording.md). The document defines exact syntax, three steps, two distinct variable identities, draft-2 compatibility, failed-write/limit behavior, backward reconstruction, implementation ownership and 13 acceptance groups. No code changed or runtime tests ran. Implementation and Docker verification remain pending explicit approval; the September 23 verified baseline is unchanged.

September 24 restoration: reviewed the saved handoff and relevant requirements/specifications. No code changes or tests ran; the last verified baseline remains September 23. Recommended next discussion is defining scope and acceptance cases for the earlier combined scalar/array teaching example. Neither that new specification work nor implementation is approved yet.

End-of-day checkpoint: the user requested stopping after the completed integer-variable increment. The verified baseline is 17 automatic runtime cases, 15 original/generated comparisons, 17 result/reconstruction checks and the relevant shared regressions, with all owned containers removed. This save changed documentation only; no tests were rerun. Tomorrow's proposed discussion is combining scalar variables with array operations after reviewing the current example. That next implementation is not approved. See current-state.md for the continuation handoff.

This document consolidates our work so far for review and tracking. [current-state.md](current-state.md) holds the latest session handoff and detailed conversation history. [Project requirements](../requirements/PROJECT_REQUIREMENTS.md) define product intent; specifications define intended behavior. This progress record does not override either or authorize the next task.

## 1. Current position

Recovery check, September 23: after the user reported a usage-limit interruption, the saved analyzer acceptance log and analysis report were read again. The 18-case success report remains present. The prior session's completed command output recorded both limit probes and container cleanup before saving these memory files. No implementation loss is evidenced for that run. This was a read-only evidence check plus memory maintenance, not a new test run or a live Docker-state check.

We have a working, controlled Java execution and array-recording prototype. We do not yet have the complete visualization application.

We can run reviewed Java examples in Docker, capture output/errors, enforce tested limits, and remove their containers. A manually instrumented array example also produces actual declaration/read/write events. Tests validate those events and reconstruct earlier states without rerunning Java.

JavaParser + JavaSymbolSolver 3.28.2 power a small, tested array analyzer. A separate transformer now consumes its facts and automatically inserts recorder calls into a copied program for the reviewed declaration/assignment region. Docker comparisons verify actual recorded behavior; broader Java transformation remains unfinished.

The scalar increment records one primitive int declaration and one literal assignment: int x=5; x=8; has states absent, 5, 8. The combined capability records scalar declaration, array declaration and a scalar-to-array store, optionally with one literal scalar assignment before that store. A separate bounded variable-index shape now captures a second scalar declaration and values[i]=x. These shapes reuse the same pipeline with explicit bindings and source associations. General mixed programs, index updates, arithmetic and loops remain pending.

| Area | Current status |
| --- | --- |
| Product scope, stack, UI direction, working rules | Recorded; detailed specifications remain incomplete |
| Seven specialist roles | Written and reviewed as documents; not automatically registered agents |
| Trace/result contract | Narrow draft-1 schema, synthetic examples, validator and generated array results |
| Docker Java execution | Implemented and tested with controlled fixtures |
| Docker versus Java failure classification | Implemented; simulated failures and normal container cases tested |
| Array recording | Manual fixtures and automatically generated copies of the narrow array region verified |
| Forward/backward reconstruction | Verified by test code; no production playback engine or UI |
| Java parser/resolver | Maven setup and bounded array analyzer implemented; 18 Docker analyzer cases and two driver-limit probes passed |
| Automatic instrumentation | Narrow declaration/read/write transformation verified; broader expressions/control flow pending |
| Integer variable recording | Literal initialization/assignment verified; eight scalar cases added, draft-2 events, test-only backward reconstruction |
| Spring Boot application/API | Not implemented |
| React editor, diagrams and console UI | Not implemented |
| Interactive Scanner transport | Required and designed at a high level; current runner stdin is closed |
| General execution admission and durable recovery | Unfinished |

Milestone 0 specifications and Milestone 1 feasibility work are both incomplete. Passing the small prototype does not complete either milestone.

## 2. Confirmed product direction

The project is an educational Java visualization application. Users write normal Java and inspect recorded execution through synchronized source highlighting and diagrams. Runtime values come from actual execution, not AI guesses.

The original source remains unchanged. Instrumentation will generate a separate copy containing recorder calls. Diagram playback starts after execution terminates and uses only validated recorded facts. One Step advances one observable operation, which is not necessarily an entire source line.

The confirmed frontend direction is a dark workspace: visualization on the left, Java editor at upper right, console at lower right. Users may drag existing diagrams to improve readability without changing program values or relationships. Arrays move as groups; supported list/tree/heap nodes may move individually. Only structures present at the recorded position should appear. The reference screenshot is in the conversation; it is not a implemented frontend or a saved repository image asset.

| Technology or convention | Decision and implementation status |
| --- | --- |
| Frontend | TypeScript, React, Vite, TanStack Query, Axios, TanStack Router; not installed as an application |
| Recorded frontend baseline | Node 22; React/React DOM 19.3 and Vite 8 selected in earlier planning, with exact patches/install compatibility still unverified |
| Backend | Java 21 and Spring Boot selected; Maven 3.9.16 set up locally for the analyzer, no Spring Boot application yet |
| Execution | Windows-local V1 using Docker Desktop, WSL2 and Linux containers |
| Source entry point | Main.java with Main.main in both V1 and V2 |
| Console transport | Native browser WebSocket and Spring WebSocket handlers for live I/O; HTTP with Axios/TanStack Query for run operations; not implemented |
| Editor | Monaco is the proposed implementation, not a completed integration |
| Parser/resolver | JavaParser + JavaSymbolSolver 3.28.2 and transitive dependencies pinned; narrow analysis gate verified |

Version entries above record our decisions and prior observations, not fresh compatibility checks performed for this document.

### V1 and V2 boundary

Complete V1 before implementing V2. V1 includes variables/basic String support, one-dimensional arrays and sorting, lists, stacks, queues, maps, methods/recursion, interactive Scanner input, binary-search-tree sort and heap sort within specified subsets.

On 2026-09-22, we assigned broader array/string/map/set/tree/heap problem coverage to V2. This includes set visualization, broader string operations, general tree problems beyond tree sort, and heap/priority-queue problems beyond heap sort. Exact types, methods, examples and acceptance cases still need design.

Separate test inputs are also V2, while keeping Main.java. This does not replace V1 live Scanner input or select a Solution-class invocation workflow.

Maintain separate analysis, instrumentation, recording, execution/input, trace, reconstruction and rendering responsibilities. Extend through focused capabilities and tests, preserve V1 behavior, and version incompatible contracts. Do not implement V2 prematurely or promise that every future change will avoid refactoring.

## 3. Development history

### Foundation and roles: September 15–17

We reviewed the project goal and recorded requirements, architecture, frontend design and working agreements. We distinguished the system architecture specification from the architect agent-role document supplied as a reference.

We created seven focused role documents and reviewed their ownership boundaries. Java analysis and instrumentation are separate responsibilities. Monaco/editor integration belongs to the frontend/editor role; trace reconstruction and rendering belong to visualization/playback. The reviewer identifies findings and evidence gaps; review does not automatically authorize fixes.

We confirmed the technology stack and clarified that the earlier “React 22” request referred to Node 22. We recorded the initial Java scope, library candidates, methods/recursion requirements and staged delivery priorities.

### Java behavior and trace design: September 18

We clarified ordinary single-file Java authoring. Valid Java outside tracing coverage may run output-only if independently admitted. Unsupported tracing must not be reported as a Java syntax error. A started program must not silently rerun to obtain output or a different trace.

We recorded observable-step rules, source highlighting, exception handling and safe partial playback. For array-to-array assignment, the read and write are separate steps. We distinguished runtime events from the overall run result and created four synthetic result examples.

### Contract validation and Docker feasibility: September 19

We added the draft-1 JSON schema and a dependency-free Node validator. The validator checks the limited schema vocabulary and relevant array semantics; it is not a general JSON Schema implementation.

We documented execution restrictions and candidate limits, checked local prerequisites, downloaded a pinned Java 21 image with approval, and passed a restricted-container smoke test. Runtime probes checked identity, filesystem restrictions and effective resource settings. The smoke test compiled/reran a small array program inside Docker and verified cleanup.

### Repeatable execution and recording: September 21

We built the trusted Java runner harness and seven controlled fixtures. An initial attempt found the Docker Linux engine unavailable. After user-approved Docker Desktop startup, the cases passed.

A review found that Docker command failures could be misclassified as Java failures. We added management-event evidence checking and simulated regression cases. We then built the separate trace pipe, bounded collector, container-only recorder, manual source pairs and reconstruction checks.

We clarified the implementation with the user: recorder calls are currently added manually to a separate copy. The system does not yet capture every source line or automatically understand arbitrary code.

### Scope review and parser choice: September 22

We reviewed the broader algorithm-problem target, recorded V2 expansion while preserving V1, and retained Main.java for both versions. We reviewed analysis/instrumentation handoffs and researched JavaParser, Eclipse JDT and the JDK compiler API.

The user approved JavaParser + JavaSymbolSolver as the prototype candidate. We recorded the decision. Setup, dependency downloads, analyzer implementation and tests have been proposed but not authorized. The user requested this consolidated progress document before proceeding.

## 4. What the execution runner does

### Integer-variable increment completed on September 23

After the array task, the user approved recording int initialization and assignment. [IntProbe.java](../runner/analysis/src/main/java/dev/codeviz/analysis/IntProbe.java) checks the narrow scalar shape and resolves the assignment to the declared binding. The existing transformer preserves the assignment, adds a recorder-budget guard before it, and records the actual value afterward. Original source/AST and source mappings remain preserved.

The shared recorder and collector support VARIABLE_DECLARE and VARIABLE_WRITE. [ADR 0006](../specs/decisions/0006-scalar-trace-draft-2.md) records why these use [draft-2](../contracts/run-result-v2.schema.json): old consumers have a closed array-only event union. Draft-1 files/producers remain unchanged; the validator accepts both versions. Variable identity is separate from the display name and array identity. This one-variable gate does not implement frames or multi-variable lifetimes.

The combined command passed 17 automatic runtime cases, 15 original/generated comparisons and 17 contract/range/reconstruction checks. The eight scalar cases cover normal/renamed/Unicode/formatted input, helper collision, int extremes, repeated values, no probes and a one-event trace limit. Analyzer/transformation, schema/collector, two worker-limit probes and all manual recorder regressions also passed. Every owned container was removed. See the [integer guide](../runner/analysis/INTEGER_RECORDING.md) and [semantics](../specs/integer-recording.md). Local original/generated sources and result JSON are under runner/analysis/.results/automatic/int-*.

### Automatic recorder insertion added on September 23

The user approved replacing manual recorder insertion for the same small array example. [ArrayTransformer.java](../runner/analysis/src/main/java/dev/codeviz/instrumentation/ArrayTransformer.java) checks the analyzed source identity and binding/site facts, modifies a syntax-only AST copy, avoids helper-name collisions and returns generated source with original/generated source associations. It leaves original source/AST unchanged and rejects unsupported or stale inputs. See the [guide](../runner/analysis/AUTOMATIC_RECORDING.md) and [specification](../specs/automatic-array-recording.md).

The recorder and collector now accept the actual analyzed variable display name, including Unicode through canonical JSON escapes, while preserving manual-fixture defaults. No trace-schema change was needed. The runtime recorder still supplies actual values; AST analysis does not predict events.

`test-recording.ps1` passed nine automatic runtime cases, eight original/generated comparisons and nine contract/source-range/reconstruction checks. Cases include renamed arrays, changed values, multiline/CRLF/Unicode formatting, helper collisions, failed reads/writes, empty arrays, integer limits and a trace limit. All 18 analyzer cases, transformation guard checks and two worker-limit probes passed. Shared collector checks and the seven manual recording cases/four paired comparisons also passed. All owned test containers were removed. The separate output-only runner/classification suites were not rerun.

The review artifacts are under runner/analysis/.results/automatic, with original/generated Java and raw/validated trace JSON for each case. These are ignored local evidence. This finishes the approved small automatic-recording task, not Milestone 1 or full Java instrumentation.

### Array analyzer added on September 22

After this progress document was created, the user approved Maven setup, the small analyzer and Docker tests. The [analyzer guide](../runner/analysis/README.md) explains the implementation and commands; its [specification](../specs/array-analysis-experiment.md) defines exact coverage and bounds.

The analyzer preserves original source/hash/AST, resolves declaration identity and int[]/int types, identifies the Main.main signature, and locates three possible recording sites. It does not generate events or change code. Immutable facts and syntax-only AST copies keep later instrumentation separate from original analysis. Source edits fail the handoff guard.

| New file | Purpose |
| --- | --- |
| [pom.xml](../runner/analysis/pom.xml) and [maven.ps1](../runner/analysis/maven.ps1) | Pinned parser/build dependencies and checksum-verified project-local Maven 3.9.16 |
| [SourceSnapshot.java](../runner/analysis/src/main/java/dev/codeviz/analysis/SourceSnapshot.java) | UTF-8 source, SHA-256, stale-source checks and UTF-16 ranges |
| [ArrayAnalyzer.java](../runner/analysis/src/main/java/dev/codeviz/analysis/ArrayAnalyzer.java) | Static AST/binding/type/scope/entry facts and classified diagnostics |
| [AnalyzerMain.java](../runner/analysis/src/main/java/dev/codeviz/analysis/AnalyzerMain.java) | Bounded human-readable report |
| [AnalyzerAcceptance.java](../runner/analysis/src/test/java/dev/codeviz/analysis/AnalyzerAcceptance.java) | Eighteen focused analysis tests and trusted worker-limit probes |
| [test.ps1](../runner/analysis/test.ps1) and [Dockerfile](../runner/analysis/Dockerfile) | Isolated Java 21 verification, resource limits, completion checks and cleanup |

All 18 analyzer cases and both driver-limit probes passed. The tests also compiled selected valid/invalid fixtures inside Docker without executing them. Every final test container was removed. Parsing eligibility covers only the small array region; unsupported shapes, unknown scopes and unverified raw Unicode escapes remain explicit limitations. Generated evidence is ignored under runner/analysis/.results; the existing manual runner/recording code was unchanged and its earlier suite was not rerun.

### Existing execution runner

The runner accepts a bounded source snapshot, creates a uniquely named restricted container, transfers Main.java, compiles it, and starts it only if compilation succeeds. It captures stdout/stderr concurrently, including output without a newline. It monitors cancellation and limits, removes the environment, and separately reports whether cleanup was confirmed.

Submitted Java and recorder code compile/run only in Docker. The host compiles the trusted harness/test driver, not submitted Main.java files.

| File | Purpose |
| --- | --- |
| [RunnerHarness.java](../runner/prototype/RunnerHarness.java) | Container lifecycle, compilation/execution, deadlines, bounded console capture, outcomes and cleanup |
| [ExecEvidence.java](../runner/prototype/ExecEvidence.java) | Checks correlated Docker execution/completion events before attributing a result to Java |
| [RunnerHarnessTest.java](../runner/prototype/RunnerHarnessTest.java) | Runs the seven controlled execution fixtures and checks outcomes/cleanup |
| [RunnerFailureTest.java](../runner/prototype/RunnerFailureTest.java) | Simulates Docker responses to verify infrastructure-versus-program classification |
| [test.ps1](../runner/prototype/test.ps1) | Builds explicit trusted files and runs the approved test suites |
| [Runner guide](../runner/prototype/README.md) | Commands, implementation boundaries and evidence |

The execution fixtures are success, compile error, runtime error, timeout, cancellation, output overflow and controlled JVM heap exhaustion. A JVM heap failure is not proof that Docker cgroup OOM enforcement was tested.

### Implemented prototype bounds

| Resource | Prototype setting |
| --- | --- |
| Source | 64 KiB, including appended recorder source in the recording experiment |
| Compilation | 15 seconds |
| Program stage | 5 seconds; evidence/trace collection shares the stage deadline |
| Overall orchestration | 30 seconds, excluding the separate cleanup observation budget |
| Cleanup observation | 5 seconds; unconfirmed removal is reported |
| Container memory/CPU/tasks | 512 MiB, no swap, one CPU, 128 tasks |
| Private writable scratch | 56 MiB /work plus 8 MiB /tmp |
| JVM heaps | Program 64 MiB; compiler 256 MiB |
| Combined compiler/program stdout and stderr | 1 MiB retained bytes |
| Trace | 64 KiB, 32 events, 16 array elements |

Containers use a non-root identity, no network, read-only root, dropped capabilities, no-new-privileges and no host mounts. Docker logging is disabled. These are prototype controls with specific recorded checks, not a complete hostile-code safety certification.

## 5. What the array-recording experiment does

The reviewed source region is:

```java
int[] values = {3, 1};
values[0] = values[1];
```

Its separate manual instrumentation uses recorder helpers to perform and report the operations. The read helper captures and returns the actual value once. The write helper stores that value, then records the successful write. Failed accesses do not emit successful events.

| Step | Event | Recorded fact | Reconstructed array |
| --- | --- | --- | --- |
| 0 | None | Before declaration | No binding/object |
| 1 | ARRAY_DECLARE | Actual initial elements | [3, 1] |
| 2 | ARRAY_READ | Read index 1, obtaining 1 | [3, 1] |
| 3 | ARRAY_WRITE | Write 1 into index 0 | [1, 1] |

Events flow through a private named pipe, separate from console output. The collector validates complete records incrementally and retains only the safe prefix. A backward step reconstructs an earlier prefix; it does not execute Java backward or ask for input again.

| File | Purpose |
| --- | --- |
| [Original Main.java](../runner/prototype/recording/original/Main.java) | Reviewed source with development-only output/final-value probes |
| [Manual template](../runner/prototype/recording/instrumented/Main.java.in) | Instrumented counterpart with explicit fixture substitutions; not an automatic transformer |
| [Recorder.java](../runner/prototype/recording/Recorder.java) | Container-only runtime recorder |
| [TracePipe.java](../runner/prototype/TracePipe.java) | Separate Docker pipe reader |
| [ArrayTrace.java](../runner/prototype/ArrayTrace.java) | Bounded validation and safe-prefix retention for the narrow wire format |
| [ArrayTraceTest.java](../runner/prototype/ArrayTraceTest.java) | Stream corruption, framing and limit checks |
| [ArrayRecordingTest.java](../runner/prototype/ArrayRecordingTest.java) | Docker execution of manual pairs and behavior comparisons |
| [check-results.mjs](../runner/prototype/recording/check-results.mjs) | Draft-result assembly, contract validation, source checks and test-only reconstruction |
| [Recording guide](../runner/prototype/recording/README.md) | Detailed responsibilities, commands and limitations |

Generated evidence resides in `runner/prototype/recording/.results/`: original/instrumented source snapshots, raw run artifacts and readable result JSON. These are ignored local test artifacts and can be regenerated; they are not a durable application store. The saved [success result](../runner/prototype/recording/.results/success.result.json) currently contains the three actual events.

The experiment traces one reviewed operation region. Test probes and full method/scope lifecycle are outside that region. Its complete-coverage label does not establish general Java tracing or production lifecycle semantics.

## 6. Verification record

These are historical results from the recorded development sessions, not tests rerun while creating this document. Suites overlap; the rows should not be added together as independent feature totals.

| Verification | Recorded result | What it establishes |
| --- | --- | --- |
| Contract checks, September 19 | Four synthetic fixtures; 18 negative mutations; three positive edge cases; unsupported-keyword guard passed | Narrow result shape and semantic consistency |
| Docker smoke test, September 19 | Passed, including restriction probes and cleanup | Pinned Java image and small isolated compile/run feasibility |
| Execution harness, September 21 | 7/7 controlled cases passed | Output/errors, timeout, cancellation, overflow, controlled heap failure and cleanup |
| Docker classification, September 21 | 17/17 simulated cases passed | Missing/conflicting management evidence does not become a claimed Java result |
| Trace stream checks, September 21 | Chunking, ten corrupt/incomplete prefixes, missing end, three resource caps and value edge cases passed | Bounded safe-prefix collection and rejection behavior |
| Recording cases, September 21 | 7/7 passed | Success, changed values, failed read/write, trace limit, cancellation and blocked reader |
| Original/instrumented comparisons, September 21 | Four pairs passed | Normal output/final probes/stderr and exception type/message preservation for reviewed pairs |
| Generated result checks, September 21 | 7/7 passed | Draft-1 validation, source hash/ranges, forward/backward reconstruction and stale-source rejection |
| Array analyzer, September 22 | 18/18 cases passed in Docker | AST, bindings/types/scopes, source ranges, preservation, diagnostics and analysis limits for the narrow gate |
| Analyzer driver, September 22 | Deadline and output-limit probes passed; all containers removed | Controlled test-worker limits and cleanup, not production admission/recovery |
| Automatic array recording, September 23 | 9 runtime cases, 8 paired comparisons and 9 contract/mapping/reconstruction checks passed | Automatic recorder insertion preserves reviewed behavior and original source highlights |
| Shared regressions, September 23 | 18 analyzer cases, transformation guards, two worker-limit probes, collector checks and 7 manual recording cases/4 comparisons passed | Existing narrow analyzer/manual recorder behavior preserved; all owned containers removed |
| Integer increment, September 23 | Combined 17 runtime cases/15 paired comparisons/17 result checks passed, including 8 scalar cases | Literal int declaration/assignment records actual committed values and reconstructs backward; arrays preserved |

The changed initializer produced [9,-4], [9,-4], [-4,-4], showing that values come from runtime recording rather than the original expected fixture. That case also checked CRLF and UTF-16 source coordinates after a supplementary Unicode character.

A failed read retained one event. A failed write retained two. The lowered trace cap retained two; cancellation retained one; the blocked reader/no-writer timeout retained none. All containers in the successful suites were verified absent afterward.

No real engine-disconnection injection, full orphan-recovery proof, Docker memory/process/scratch exhaustion suite, browser verification or general-admission verification has passed. Simulated errors are not substitutes for those checks.

## 7. Specifications, roles and decisions

### Main documents

| Document | Purpose |
| --- | --- |
| [AGENTS.md](../AGENTS.md) | Working rules, user confirmation, ownership, correctness and verification requirements |
| [Project requirements](../requirements/PROJECT_REQUIREMENTS.md) | Product scope, V1 milestones, V2 roadmap and acceptance cases |
| [Architecture](../specs/architecture.md) | Component boundaries and proposed system design |
| [Frontend design](../specs/frontend-design.md) | Workspace, console, playback, highlighting and dragging behavior |
| [Java support](../specs/java-support.md) | Staged language/library support and operation semantics |
| [Trace format](../specs/trace-format.md) | Recorded facts, source association, safe boundaries and playback rules |
| [Execution isolation](../specs/execution-isolation.md) | Restrictions, limits, lifecycle and remaining verification gates |
| [Array experiment](../specs/array-recording-experiment.md) | Exact manual experiment scope and recorded evidence |
| [Contract guide](../contracts/README.md) | Schema and validator usage/limitations |
| [Synthetic examples](../contracts/examples/README.md) | Four hand-authored scenarios and their source snapshots |

The schema and synthetic examples describe expected behavior; they do not themselves execute Java. The generated recording artifacts provide the separate runtime evidence. The Node validator is deliberately limited to the repository schema and is not a general standards validator.

### Specialist role documents

| Role | Responsibility |
| --- | --- |
| [Architect](../.agents/architect.md) | Coordinate architecture, contracts, milestones and task handoffs |
| [Java analysis](../.agents/java-analysis-engineer.md) | Original AST, declarations, references, types, scopes, ranges and diagnostics |
| [Instrumentation/trace](../.agents/instrumentation-trace-engineer.md) | Transform a separate representation and record faithful runtime facts |
| [Backend/runner](../.agents/backend-runner-engineer.md) | APIs, run orchestration, isolation, console/trace collection and cleanup |
| [Frontend/editor](../.agents/frontend-editor-engineer.md) | Editor/workspace and source/console integration |
| [Visualization/playback](../.agents/visualization-playback-engineer.md) | State reconstruction, navigation, renderers and presentation-only dragging |
| [Reviewer](../.agents/reviewer.md) | Review correctness, consistency and verification evidence |

These Markdown files describe responsibilities. Their existence does not register or execute agents, and no agent should be launched without applicable authorization.

### Architectural decision records

| Decision | Current meaning |
| --- | --- |
| [ADR 0001](../specs/decisions/0001-live-console-websocket.md) | WebSocket selected for live console communication; integration pending |
| [ADR 0002](../specs/decisions/0002-java-execution-and-visualization-coverage.md) | Separate Java execution from tracing coverage; preserve honest output-only/partial outcomes |
| [ADR 0003](../specs/decisions/0003-runner-execution-evidence.md) | Use correlated Docker management events to confirm prototype process outcomes |
| [ADR 0004](../specs/decisions/0004-controlled-array-trace-pipe.md) | Separate bounded pipe for the manual array trace |
| [ADR 0005](../specs/decisions/0005-javaparser-prototype-candidate.md) | JavaParser + JavaSymbolSolver 3.28.2 pinned and verified for narrow analysis and automatic array transformation |
| [ADR 0006](../specs/decisions/0006-scalar-trace-draft-2.md) | Scalar events use draft-2; draft-1 arrays remain supported without changing stored traces |

## 8. Outstanding work and next approval

Maven/parser setup, automatic array recording, literal int declaration/assignment recording, scalar-to-array stores with or without a preceding scalar update, and the separate variable-index shape are completed for their narrow tested scopes. Next discuss a bounded follow-on capability toward index updates, arithmetic and loops. Further scope and implementation require approval and explicit semantics.

The current transformation preserves original source/AST, rejects stale or incomplete analysis, avoids helper-name collisions and has behavior comparisons for the narrow supported shape. Loops, aliases, side-effecting expressions and other broader transformations remain unimplemented.

The user approved and completed both the setup/analyzer scope and the subsequent narrow automatic-recording scope. No further implementation task is approved.

The subsequently approved literal-int and bounded combined increments are complete. This does not authorize arithmetic, broader mixed programs, loops or another implementation task.

- [x] Record architecture, specialist roles and working agreements.
- [x] Create the narrow trace contract and validation fixtures.
- [x] Prove controlled Docker execution and cleanup.
- [x] Improve Docker-versus-Java error classification.
- [x] Prove manual array recording and test-only reconstruction.
- [x] Record V1/V2 scope and approve a parser candidate.
- [x] Set up and verify the parser/resolver and analysis handoff for the narrow array experiment.
- [x] Automatically insert recorder calls into a separate source copy for the reviewed array region.
- [x] Record one int variable's literal initialization and assignment, with compatible trace versioning and backward reconstruction tests.
- [ ] Extend verified scalar/array/control-flow/aliasing/sorting support.
- [ ] Implement Spring Boot APIs, editor, playback/renderers and interactive console.
- [ ] Deliver methods/recursion, collections, tree sort and heap sort.
- [ ] Complete execution admission, recovery, remaining specifications and V1 verification.
- [ ] Begin the confirmed V2 expansion only after V1 completion.

## 9. How to keep this record useful

Update this document after material approved work: record the date, concrete change, evidence, limitations and next pending step. Keep decisions separate from implementation, and simulations separate from real execution. Add links to detailed evidence rather than copying the entire conversation. Keep current-state.md focused on continuation context; neither memory document overrides requirements or specifications.

The user explicitly requested maintaining both this document and current-state.md after material discussions and approved work. This document was initially created through documentation-only review on 2026-09-22, then updated after the separately approved and verified analyzer task above. The latest analyzer results do not imply that older test suites were rerun.
