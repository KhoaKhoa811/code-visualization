# Automatic recording for the bounded array example

Approved implementation scope: 2026-09-23. Consume the [array analysis handoff](array-analysis-experiment.md), generate a separate instrumented source, and compare execution/trace results in Docker. This does not add loops, aliasing, user methods or broader V1 coverage.

## Transformation contract

Accept only a complete, eligible analysis result whose SHA-256 matches the exact supplied original bytes. Recheck that the declaration, RHS access and assignment in the syntax copy match the frozen original ranges and resolved binding. Reject stale, unresolved or untested inputs with a transformation diagnostic and no generated source. Successful generation is not compiler acceptance or execution admission.

Modify a syntax-only AST copy without resolving it or modifying original analysis. Generate a unique top-level helper name by checking all original SimpleName nodes; append a numeric suffix when needed. Copy the trusted recorder resource, rename its class and append it to the derived compilation unit. Add qualified java.io.IOException to the entry method's throws clause for the helper's checked failures. The eligible shape has no user callers or override relationship for main. Original and generated compilation remain separate checks.

Insert begin before the declaration, declare after successful initialization, and replace the single assignment with write(array, index, read(array, index, readSource), writeSource). Java evaluates the write arguments left-to-right, so the original target array/index precede the RHS access. The supported indices are signed decimal literals and both accesses resolve to the same non-null local int[]; side-effecting, null, aliased and more complex expressions remain ineligible. Reads happen once; mutation happens once and successful events follow the corresponding operation. Failed reads/writes emit no successful event for the failed operation. Retain the reviewed trailing output probes and emit the transport end marker only after normal completion.

The result includes readiness/diagnostic, original and generated hashes, generated source, chosen helper name, actual variable display name and three recording-site associations. Each association has the original UTF-16/exclusive-end range and the generated recorder-call range. Generated bookkeeping/helper code has no invented original location. Whole-source printing may change formatting in the generated copy; original bytes and AST remain untouched.

## Recorder and transport compatibility

Reuse the controlled FIFO recorder and collector with one variable/object per run. Extend declaration naming to the analyzed variable name while retaining the old values default for manual fixtures. Canonical JSON escapes non-ASCII UTF-16 units, quotes and backslashes, so the existing ASCII transport can carry Unicode identifiers without lossy conversion. This uses existing draft-1 fields and is not an incompatible trace version change.

Retain the existing 64 KiB trace, 32-event and 16-element caps, source identity, trusted terminal classification and safe-prefix rules. The generated source, including helper, is at most 64 KiB to fit the runner admission bound; reject overflow before execution. Generation runs with analysis in the existing 60-second/512-MiB test worker. Temporary development artifact bundles use bounded test stdout, not a new application API. No host mounts, container sockets, runtime source parsing on the host, or automatic fallback rerun are added.

## Verification gate

Verify original/copy preservation, hashes, stale and incomplete handoff rejection, helper-name collisions, generated source limit, and original/generated site associations. Generate cases inside Docker for original, renamed/changed, CRLF/tabs/Unicode/multiline, failed read, failed write, empty array and int-boundary programs. Compile and compare original/generated output and exception type/message through the existing restricted runner. Generated line numbers and helper stack frames need not match original stack traces. Compare exact ordered events and every reconstructed prefix, including backward steps, using recorded data only.

Include a lowered recorder event limit and retain the manual recorder regression cases for cancellation, blocked pipe and malformed stream handling. The transformed supported expressions have no user-defined side effects; reject increment/call expressions rather than claiming side-effect preservation for unimplemented forms. In-process recording is still not tamper-proof, and general admission/recovery remain unfinished.

## Verified result, 2026-09-23

The [complete test command](../runner/analysis/AUTOMATIC_RECORDING.md) passed: 18 analyzer cases, transformation guards/nine fixture generations, two analysis-worker limit probes, nine runtime recording cases and eight original/generated comparisons, all nine contract/mapping/reconstruction checks, and the existing collector/manual recording regression checks. The manual suite retained seven cases and four behavior comparisons. All owned test containers were verified removed. Unsupported Java shapes remain excluded; milestone completion is not implied.
