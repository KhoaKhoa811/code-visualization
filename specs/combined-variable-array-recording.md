# Combined integer and array recording increment

Status: scope specified and implementation/Docker verification approved on 2026-09-24. This extends the [agreed teaching example](java-support.md) without changing its step semantics. See the [implementation guide](../runner/analysis/COMBINED_RECORDING.md); completed verification is recorded below.

## 1. Exact program shape

Inside the existing public Main.main(String[]) convention, accept these three statements in this order:

```java
int x = 3;
int[] values = {5, 2};
values[0] = x;
```

The first declaration is one non-final local int initialized with a signed decimal int literal. The second is one non-final local int[] initialized with zero to 16 signed decimal int literals. The final statement is a simple array assignment: its receiver resolves to that array declaration, its index is a signed decimal int literal, and its RHS is a simple name resolving to that scalar declaration. Both names may vary; Java 21 determines whether the program is valid. An empty array or out-of-range index is eligible for a runtime failure, not falsely classified as a syntax error.

Allow comments, whitespace, LF/CRLF, tabs, Unicode identifiers and multiline expressions under the existing source-mapping rules. Retain current entry/class restrictions, source limits and conservative rejection of raw Unicode escapes. Do not skip arbitrary additional statements.

For controlled comparison fixtures, optionally accept exactly these two trailing development probes with the chosen variable names:

```java
System.out.print("FINAL=" + x + "," + java.util.Arrays.toString(values));
System.err.print("PROBE");
```

Resolve the expected JDK System fields, PrintStream.print(String) calls and Arrays.toString(int[]) overload. These read-only output probes observe final scalar and full-array values, including empty and long arrays. They remain outside the recorded region and add no steps. Their acceptance does not establish general library-call tracing. The three-statement form without probes is also eligible.

Excluded from this increment: declaration reordering, additional variables/assignments, aliasing, null or length-based array construction, scalar assignment before/after the array write, array-to-scalar reads, variable indices, arithmetic, compound assignment, increments, calls in recorded expressions, nested scopes, loops, methods and recursion. Existing separately supported scalar/array shapes must remain supported; broader V1 requirements remain planned.

## 2. Observable steps and failures

| Cursor | Event/highlight | Scalar state | Array state |
| --- | --- | --- | --- |
| 0 | No event/highlight | Absent | Absent |
| 1 | VARIABLE_DECLARE; `int x = 3` | x = 3 | Absent |
| 2 | ARRAY_DECLARE; `int[] values = {5, 2}` | x = 3 | values references [5, 2] |
| 3 | ARRAY_WRITE; `values[0] = x` | x = 3 | values references [3, 2] |

Declarations record actual initialized values after initialization. The write records the actual stored value only after the array store succeeds. Evaluate target array, index and RHS in Java's order, once each; do not replace the RHS with its initializer literal. Reading x for this assignment adds no VARIABLE_READ or extra step, following the already agreed policy. Successful scalar state remains unchanged by the array write.

Failed negative/out-of-bounds/empty-array writes emit no successful ARRAY_WRITE. Preserve the two declaration events as a safe partial trace; compare the original/generated exception type and message. Do not fabricate an exception event outside the current contract. Generated stack frames may differ. No automatic fallback rerun is allowed.

Check recorder budgets before the next observable operation. An event budget of one retains only the scalar declaration and stops before array initialization. A budget of two retains both declarations and stops before the write. Timeouts, cancellation and transport failure retain only the validated prefix; normal completion requires both the recorder end marker and trusted successful runner outcome.

Backward reconstruction restores cursor 2's original array, then removes only the array binding/object at cursor 1, then removes the scalar at cursor 0. Seeking must not rerun Java. This is still test-only reconstruction, not a browser playback implementation. Scope teardown after main is outside the reviewed region.

## 3. Identities and contract compatibility

Use existing draft-2 event shapes: VARIABLE_DECLARE, ARRAY_DECLARE, ARRAY_WRITE. No new event field or trace version is expected. The two declaration events require distinct variable IDs; the array object has its own arrayId. For this one-main experiment, use variable-1 for the scalar, variable-2 for the array binding, and array-1 for the allocated array. IDs are run-local and independent of display names; they do not promise general frame/alias identity support.

Both source names must be retained, including canonical Unicode JSON escaping on the existing transport. Reject reused variable IDs, duplicate object allocation IDs, wrong names against the expected source binding, unknown objects, wrong binding kinds, sequence gaps, wrong source sites and invalid typed values. Never weaken existing validator rules to permit the combined trace.

Existing single-array results remain draft-1 with their current identities. Existing scalar results remain draft-2 with their current identity. Old stored results need no migration. Add mixed-stream contract/collector tests rather than assuming that separate array/scalar tests prove compatibility.

## 4. Implementation handoff and boundaries

- Analysis: reuse the parser, entry gate, diagnostics and immutable original facts. Add an explicit combined capability and two resolved binding descriptors. Resolve both receiver and RHS uses to those declarations; preserve each original operation range. Do not use names or initializer values as substitutes for resolution/runtime facts.
- Transformation: consume the complete, source-matched handoff, edit a syntax-only copy, avoid helper-name collisions, and return original/generated site associations. Synthetic bookkeeping has no invented original location. Enforce the existing 64-KiB generated-source cap including helpers.
- Recorder: support both live bindings and one array in this capability. Preserve legacy single-binding entry paths/defaults. Guard before initialization/write as specified, then capture committed actual values. Keep one ordered event counter and the existing FIFO transport.
- Collector/reconstruction: track binding kind/identity separately from array object state and validate the expected combined sequence. The current mutually exclusive scalar flag is not sufficient. Keep transport framing, bounds and cleanup ownership in their existing layers; do not build a second execution harness or parser.
- Contract: extend semantic tests and examples using existing draft-2 shapes; do not change draft-1 or add speculative fields. Record a separate architectural decision only if implementation reveals an incompatible contract change.

Reuse existing 64-KiB source/trace bounds, 32-event/16-element caps, analysis/diagnostic limits, worker restrictions and runner deadlines. No new dependency, service, API, UI or installation is proposed.

## 5. Acceptance cases to run after implementation approval

| ID | Case | Required evidence |
| --- | --- | --- |
| MIX-01 | Exact example above | Three events/states as specified; final probe `FINAL=3,[3, 2]`; x stays 3 |
| MIX-02 | Rename both bindings; change scalar/initial array values and use index 1 | Names and resolved IDs remain correct; actual scalar value replaces only index 1 |
| MIX-03 | Negative values, repeated values, int minimum/maximum | Exact typed values, unchanged non-target elements; no overflow/coercion in replay |
| MIX-04 | Unicode names, supplementary-character comment, CRLF/tabs/multiline sites | Exact original UTF-16/exclusive-end highlights and generated-call associations |
| MIX-05 | Names collide with generated helper candidates | Choose an unused helper name; original source/AST and behavior remain unchanged |
| MIX-06 | No output probes; one-element and 16-element arrays | Correct declaration snapshots and write states; no dependency on probe statements |
| MIX-07 | Negative/out-of-bounds index and empty array | Original/generated exception type/message agree; exactly two safe declaration events |
| MIX-08 | Event budgets one and two | Stop before next operation; one/two events with explicit limit and partial coverage |
| MIX-09 | Stale source, unresolved RHS/receiver, wrong type/binding, missing facts | No generated source from an unsafe handoff; diagnostic distinguishes unresolved/unsupported coverage from syntax errors |
| MIX-10 | Reordered/additional statements, variable index, RHS increment/call, alias, final/uninitialized declaration, oversized array/source | Reject outside this capability; preserve previously supported separate shapes |
| MIX-11 | Mixed trace corruption | Reject duplicate scalar/array binding IDs, unknown object, wrong kind/name/source, invalid value and sequence gap; never resume beyond corruption |
| MIX-12 | Every forward/backward cursor and source edits after recording | Exact independent scalar/array states, correct highlight and stale-source rejection without execution |
| MIX-13 | Existing regression baseline | Preserve 17 automatic cases/15 paired comparisons, analyzer/contract/collector checks, two worker-limit probes and seven manual recording cases/four comparisons |

For admitted normal and runtime-error fixtures, compile and execute original/generated source separately inside the existing restricted Docker runner with identical inputs. Compare output/final probes and exception type/message; deliberately limited runs are not compared as unlimited successes. Validate every generated result against draft-2 and verify all owned containers are removed. Analysis/transformation also stay inside the bounded worker; host builds remain trusted driver code only.

Completion requires all applicable cases and regression checks passing with evidence recorded in both memory files. Do not advance to arithmetic/loops solely because this increment is complete.

## 6. Implementation and verification status

Verified 2026-09-24: test-recording.ps1 passed all 31 automatic cases (14 combined), 27 original/generated comparisons, 31 schema/source/reconstruction checks, shared contract/collector checks, both worker-limit probes, and seven manual recording regressions/four comparisons. All test-owned containers were verified removed. A final test.ps1 run also passed the added missing-binding/access-fact and probe-shadowing guards, all 18 original analyzer cases and both worker-limit probes. The combined collector rejects 15 corrupt prefixes; the contract self-test adds six mixed negative cases. No frontend/browser test exists yet.

The fixed Arrays.toString output probe uses exact resolved JDK declaration lookup after JavaParser reported ambiguity among primitive-array overloads; source shape, argument binding/type and System stream checks remain enforced. This is limited to the development observations described in section 1. No dependency or trace-version change was needed.
