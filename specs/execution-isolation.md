# Execution Isolation and Runner Prototype

2026-10-04 superseding review: the [composable tracing direction](composable-java-tracing.md) replaces the sorting-only profile recommendation with capability/trace-budget selection independent of algorithm names. Reassess any larger event/record allowance after scope/bookkeeping semantics are specified. The earlier 128-event proposal remains inactive; all current limits are unchanged.

2026-10-04 proposal only: [the first bubble-sort trace](bubble-sort-proposal.md) needs more than the current 32 events to finish its reverse three-element example (41 events). It proposes a separately approved sorting-only 128-event profile with unchanged 64-KiB trace/source caps and all existing isolation limits. No limit changes are implemented by this documentation task; old producers retain their current profile. Runtime acceptance must prove the small examples fit both byte and event bounds and longer runs stop safely.

Status: Design draft with a bounded prototype, updated 2026-09-21. The approved [runner harness](../runner/prototype/README.md) implements the initial limits for controlled fixtures. Seven compilation/execution, timeout, cancellation, bounded-output, heap-exhaustion, and cleanup cases passed. This validates only the stated cases, not all safeguards or the full verification gates below. Numerical defaults remain prototype candidates.

## Scope and Requirements

Follow the [requirements](../requirements/PROJECT_REQUIREMENTS.md), [architecture](architecture.md), [Java support](java-support.md), and [ADR 0002](decisions/0002-java-execution-and-visualization-coverage.md). The first runner slice accepts one Main.java, compiles and executes it only in an isolated Java 21 Linux container, captures bounded output and diagnostics, reports an outcome, and cleans resources.

This slice is noninteractive and output-only. It proves execution management before adding source instrumentation. Close program stdin for this slice and state that input is unavailable; do not simulate input. Live Scanner/WebSocket interaction, variable/array tracing, and later V1 structures remain required increments. This draft does not authorize implementation, image downloads, Docker startup, or installation.

Ordinary Java compilation and tracing eligibility are separate. An admitted original-source run may complete with visualization unavailable. Compilation failure, execution restriction, runtime exception, engine failure, cancellation, and resource exhaustion must remain distinguishable. Compiler acceptance alone does not establish execution admission or isolation.

## Observed Local Prerequisites

Read-only checks on 2026-09-19 found:

| Item | Observation |
| --- | --- |
| Host java / javac | Oracle 21.0.12.1 available |
| Maven | mvn not found on PATH; not proof it is absent from the machine |
| Docker CLI | 29.8.0, build 88096ef |
| Docker Linux engine | Named pipe unavailable; no reachable engine or isolation verification |
| WSL | Default version 2; no distributions listed by the read-only query |

The first WSL query was denied inside the sandbox; an approved read-only retry outside it produced the observations above. A WSL1 configuration notice was also returned; it does not establish a WSL2 failure. Kernel/version, virtualization prerequisites, and Docker engine readiness still need checking. A separate user Linux distribution is not required for Docker commands from Windows. Host JDK availability does not validate the future runner image. Pin and verify a Java 21 image digest and toolchain during approved setup.

## Trust and Resource Boundaries

- The trusted driver owns container management, deadlines, collection, status, and cleanup. Submitted code never receives a Docker socket, host mounts, application secrets, or management credentials.
- Neither Spring Boot nor a host Java process compiles or executes submitted programs. Compilation is also resource-bounded and isolated.
- Use fixed driver-owned filenames, compiler options, entry command, and classpath. Do not interpolate submitted text into shell commands or accept user-selected host paths, container flags, compiler plugins, or annotation processors.
- Transfer bounded source into isolated storage through a controlled channel. The transfer mechanism is an implementation decision; it must not expose host paths. Keep image acquisition outside per-run execution and outside submitted-code control.
- Proposed container policy: non-root identity, read-only root filesystem, bounded private writable scratch space, no external network, dropped Linux capabilities, no-new-privileges, and default or stricter compatible syscall restrictions. Never use privileged mode to work around a Java startup failure.
- Explicitly configure resource limits; Docker availability/defaults alone are insufficient. Verify effective settings and behavior, not just command-line arguments.
- Existing source-policy exclusions remain. OS restrictions and Java feature policy are different: bounded scratch access does not enforce a blanket ban on all Java file APIs, and a process cap does not prove user-thread creation is rejected. Specify admission enforcement before accepting arbitrary submissions. Until then, restrict prototype verification to approved fixtures.
- Compiled class files and worker outputs remain untrusted. Do not load submitted classes in the trusted driver's JVM or traverse submitted paths during cleanup.

## Proposed Initial Limits

These values are starting candidates for small teaching programs. Tune only from measured prototype evidence and record material changes. They are not current guarantees or full V1 defaults.

| Resource | Initial proposal | Enforcement intent |
| --- | --- | --- |
| Active runs | 1, with no waiting queue initially | Reject excess requests explicitly before allocating a worker |
| Source | 64 KiB encoded bytes | Trusted admission check before transfer |
| Compilation wall time | 15 seconds | Driver deadline covers the full compilation stage |
| Program wall time | 5 seconds | Driver deadline begins at program launch |
| Overall run wall time | 30 seconds | Admission through startup, transfer, compilation, execution, and collection; no image pull in this budget |
| Container memory | 512 MiB with no swap | Enforced container budget, including JVM native memory and scratch accounting where applicable |
| CPU | 1 CPU allocation | Container quota; wall-time deadline remains independent |
| Processes/threads | 128 tasks | Container process limit; verify JVM can initialize under it |
| Writable scratch | 64 MiB total | Private bounded storage shared by source, generated classes, and JVM temporary files |
| stdout + stderr | 1 MiB combined per run | Incremental trusted byte counting across compiler/runtime output; retain only bounded output and terminate on overflow |
| Cleanup observation | Proposed 5-second bounded attempt | Report failure if removal cannot be confirmed; block new admission pending recovery |

The overall deadline overrides longer remaining stage time. Cleanup has its own bounded attempt; do not promise physical termination within five seconds if the engine is unreachable. JVM heap limits must leave native-memory headroom and cannot substitute for the container memory cap. Measure startup, compilation, threads, scratch use, and output collector memory before accepting these defaults.

Container log storage, diagnostic accumulation, output transport buffers, and retained results must also be bounded. Propose retaining only the most recent result in this first slice and releasing it when replaced; final retention policy remains open. Do not introduce unbounded Docker logging or buffer all output before checking its limit.

Trace byte/event/record limits, traversal/collection bounds, recursion depth, and analysis/transformation budgets require concrete values before their respective features are introduced. They are not silently unlimited in the meantime; the first slice does not run those components.

## Proposed Run Lifecycle

1. Validate source size, entry convention, execution-policy admission, capacity, and driver configuration. Bind a run identity to the submitted source snapshot.
2. Confirm a pinned local image and reachable compatible Linux engine. Missing prerequisites produce an infrastructure diagnostic; never fall back to host execution or pull images silently.
3. Create uniquely identified private run resources with the approved restrictions. Keep a trusted ownership record sufficient for startup-failure cleanup and recovery.
4. Transfer source and compile with fixed Java 21 settings. Collect bounded compiler diagnostics. Compilation errors prevent program launch.
5. Launch the compiled entry point with closed stdin for the first slice. Capture stdout/stderr incrementally, including text without a newline. Preserve ordering within each stream; do not promise a total cross-stream order not supplied by the transport.
6. Monitor deadlines, resource outcomes, cancellation, and collector errors. The trusted driver determines the outcome from evidence. A generic process exit code does not by itself identify OOM, timeout, or a Java exception.
7. Stop any remaining worker processes, remove owned resources, and report the result plus cleanup state. Keep execution outcome separate from visualization coverage. Never report an incomplete capture as a complete trace.

Submitted output is text, not a control channel. Console strings such as fake trace JSON or success messages cannot set trusted status. Exact API/enums and compiler/runtime diagnostic mapping remain contract work; do not force every runner failure into an inaccurate existing fixture category.

Prototype update (2026-09-21): [ADR 0003](decisions/0003-runner-execution-evidence.md) records the implemented classification fix. Compiler/program outcomes now require correlated Docker management events and agreement with the CLI exit code; missing evidence reports infrastructure uncertainty rather than a source error. The evidence query shares stage/overall deadlines, is capped at three seconds and 64 KiB, and does not read submitted console output as status. Seventeen simulated classification cases and seven controlled container cases passed; this does not complete the engine-loss or recovery gates.

## Failure, Cancellation, and Cleanup

| Situation | Required behavior |
| --- | --- |
| Missing engine/image or worker startup failure | Infrastructure diagnostic, no host fallback, cleanup partially allocated resources |
| Compilation error | Preserve compiler diagnostics; do not launch the program |
| Runtime exception/nonzero exit | Preserve bounded output and available reliable exception information; no invented source location |
| Time/memory/process/storage/output limit | Report the specific cause when evidence identifies it, terminate the entire environment, and mark truncated data explicitly |
| Cancellation | Driver stops the entire environment, including descendants; repeated cancellation/removal is safe |
| Collector failure | Do not claim complete output/recording; stop the run as required by policy and report the operational failure |
| Engine disconnect or failed cleanup | Report uncertainty and cleanup failure; stop admission until owned resources are reconciled |
| Interrupted orchestration | Recover only verified project-owned resources from trusted ownership records; never blanket-delete unrelated containers |

Proposed race rule: the driver serializes lifecycle decisions; the first committed terminal cause remains authoritative. A committed cancellation/limit cannot later become success merely because a process exits. Cleanup failure is separately visible and does not erase the execution cause. Exact implementation synchronization requires tests.

Do not automatically rerun failed or partially executed code, replay input, or merge attempts. On driver restart, inspect owned resources before accepting new runs. Define and verify orphan recovery before claiming reliable local operation. A controller-side timeout alone cannot guarantee orphan termination if the controller crashes; the prototype must demonstrate the chosen recovery/watchdog arrangement or report that limitation explicitly.

## Required Later Increments

### Trace Production

Prototype increment (2026-09-21): the approved [array experiment](array-recording-experiment.md) implements a separate private FIFO reader with 64 KiB trace-byte, 32-event and 16-element bounds. Complete records are validated incrementally; TRACE_LIMIT and TRACE_ERROR terminate the controlled run while preserving only its validated prefix. Pipe operations share existing stage/overall deadlines; reader shutdown shares the cleanup observation budget. Source size remains 64 KiB including appended recorder source in this experiment. Broader traversal, recursion and transformation bounds remain prerequisites for their own increments.

Select trace transport distinct from stdout/stderr with incremental bounded collection; normal-exit-only flushing is insufficient. Preserve validated records only through a safe reconstruction boundary under [trace-format.md](trace-format.md). Missing mutation records must never be guessed. Recorder checks supplement trusted hard limits. A recorder in the submitted JVM is not tamper-proof; the output-only path still needs independent execution admission.

### Interactive Console

Keep [ADR 0001](decisions/0001-live-console-websocket.md) for browser/backend communication. Before interactive execution, specify exact line/aggregate/queue limits, acknowledgement/deduplication, EOF, wait detection, wait-versus-compute budgets, an overall bound, and disconnect/reconnect policy. Preserve Scanner behavior; input acceptance is not consumption. Cancellation must work while a read is blocked. The noninteractive five-second execution budget is not automatically the interactive policy.

## Verification Gates

The table defines the wider verification plan. The harness README records the narrower cases actually passed; unlisted scenarios remain unverified. Execute further cases only in an approved runner-verification task with bounded fixtures. The prerequisite observations above are historical; later Docker startup/smoke-test evidence is recorded in session memory.

| Case | Evidence required |
| --- | --- |
| Small Main.java prints a value | Compilation and execution in the restricted container; exact expected output and cleanup |
| Syntax error and runtime exception | Correct stage-specific diagnostics, no inappropriate launch or rerun |
| Infinite loop and cancellation | Bounded response, entire-environment termination, no surviving worker |
| Excess output, including no-newline output | Incremental bounded capture, explicit overflow, bounded collector/log storage |
| Memory, scratch, and process-limit fixtures | Effective limits demonstrated without exhausting the host; Java startup still succeeds for normal fixtures |
| Forbidden network and host-resource probes | Submitted environment cannot reach prohibited external/host resources or management interfaces |
| Cross-run isolation | New run sees no prior run files/input/output; stale handles cannot affect another run |
| Partial startup and repeated cleanup | Owned resources removed safely; idempotence and unrelated resources preserved |
| Controller interruption and engine loss | Recovery policy demonstrated; unknown cleanup is reported and admission remains blocked |
| Success/cancel/timeout race | One consistent execution outcome with independently accurate cleanup state |

Before the trace increment, add original-versus-instrumented semantic comparisons and safe-prefix collection tests. Before console UI integration, prove prompt/input/EOF/wait/cancel behavior through the worker. Existing fixture/schema checks do not substitute for any of these runtime gates.

## Open Decisions and References

Remaining choices include pinned worker image, exact source/result transfer, trusted driver implementation, effective JVM/container settings, process-policy enforcement, bounded logging/storage, and watchdog/recovery design. Confirm concrete choices and implementation scope before coding. Docker/WSL startup and setup need separate approval.

Official documentation reviewed during prerequisite inspection:

- [Docker Desktop WSL backend](https://docs.docker.com/desktop/features/wsl/)
- [Docker resource constraints](https://docs.docker.com/engine/containers/resource_constraints/)
- [Docker container runtime controls](https://docs.docker.com/engine/containers/run/)

Windows-local verification does not establish public hostile-code deployment readiness.
