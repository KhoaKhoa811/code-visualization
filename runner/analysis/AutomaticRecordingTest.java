import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Trusted host driver; source analysis/generation and both program executions stay in Docker. */
public final class AutomaticRecordingTest {
    static final Set<String> CASES = cases();
    private static Set<String> cases() {
        var cases = new HashSet<>(Set.of("success", "renamed", "formatted", "collision", "failed-read", "failed-write", "empty", "int-bounds", "trace-limit",
        "int-success", "int-renamed", "int-formatted", "int-collision", "int-bounds-scalar", "int-repeat", "int-no-probe", "int-limit",
        "mix-success", "mix-renamed", "mix-bounds", "mix-repeat", "mix-formatted", "mix-collision", "mix-no-probe", "mix-one", "mix-sixteen", "mix-negative", "mix-oob", "mix-empty", "mix-limit1", "mix-limit2",
        "update-success", "update-renamed", "update-bounds", "update-repeat", "update-formatted", "update-collision", "update-no-probe", "update-one", "update-sixteen", "update-negative", "update-oob", "update-empty", "update-limit1", "update-limit2", "update-limit3", "update-limit4",
        "index-success", "index-renamed", "index-equal", "index-minvalue", "index-maxvalue", "index-formatted", "index-collision", "index-no-probe", "index-first", "index-sixteen", "index-negative", "index-oob", "index-minindex", "index-maxindex", "index-empty", "index-limit1", "index-limit2", "index-limit3", "index-limit4",
        "index-update-success", "index-update-renamed", "index-update-equal", "index-update-minvalue", "index-update-maxvalue", "index-update-formatted", "index-update-collision", "index-update-no-probe", "index-update-first", "index-update-sixteen", "index-update-negative", "index-update-oob", "index-update-minindex", "index-update-maxindex", "index-update-empty", "index-update-repeat", "index-update-invalid-initial", "index-update-limit1", "index-update-limit2", "index-update-limit3", "index-update-limit4", "index-update-limit5",
        "index-add-success", "index-add-renamed", "index-add-equal", "index-add-zero", "index-add-step", "index-add-negative", "index-add-oob", "index-add-empty", "index-add-overflow-max", "index-add-overflow-min", "index-add-wrap-zero", "index-add-invalid-initial", "index-add-first", "index-add-sixteen", "index-add-formatted", "index-add-collision", "index-add-no-probe", "index-add-limit1", "index-add-limit2", "index-add-limit3", "index-add-limit4", "index-add-limit5"));
        for (String group : List.of("index-post-", "index-pre-")) for (String variant : List.of("success", "renamed", "equal", "repeat", "negative", "oob", "empty",
            "overflow-max", "overflow-min", "invalid-initial", "first", "sixteen", "formatted", "collision", "no-probe", "limit1", "limit2", "limit3", "limit4", "limit5")) cases.add(group + variant);
        return Set.copyOf(cases);
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), results = root.resolve(".results/automatic");
        Files.createDirectories(results);
        List<String[]> artifacts = loadBundles(Path.of(args[1]), CASES);
        Set<String> seen = new HashSet<>();
        int comparisons = 0;
        RunnerHarness harness = new RunnerHarness();
        for (String[] fields : artifacts) {
            check(fields.length == 11 && fields[0].equals("ARTIFACT") && CASES.contains(fields[1]) && seen.add(fields[1]), "Invalid development artifact");
            String name = fields[1], original = decode(fields[2]), generated = decode(fields[3]), variable = decode(fields[4]);
            boolean scalar = !name.equals("int-bounds") && name.startsWith("int-");
            boolean update = name.startsWith("update-");
            boolean indexed = name.startsWith("index-");
            boolean increment = name.startsWith("index-post-") || name.startsWith("index-pre-");
            boolean indexUpdate = name.startsWith("index-update-") || name.startsWith("index-add-") || increment;
            boolean combined = name.startsWith("mix-") || update || indexed;
            Map<String, String> ranges = update ? Map.of("VARIABLE_DECLARE", decode(fields[5]), "ARRAY_DECLARE", decode(fields[6]), "VARIABLE_WRITE", decode(fields[9]), "ARRAY_WRITE", decode(fields[7]))
                : combined ? Map.of("VARIABLE_DECLARE", decode(fields[5]), "ARRAY_DECLARE", decode(fields[6]), "ARRAY_WRITE", decode(fields[7]))
                : scalar ? Map.of("VARIABLE_DECLARE", decode(fields[5]), "VARIABLE_WRITE", decode(fields[7]))
                : Map.of("ARRAY_DECLARE", decode(fields[5]), "ARRAY_READ", decode(fields[6]), "ARRAY_WRITE", decode(fields[7]));
            String metadata = decode(fields[8]);
            check(original.getBytes(StandardCharsets.UTF_8).length <= 65_536 && generated.getBytes(StandardCharsets.UTF_8).length <= 65_536, "Source cap");
            String[] names = variable.split("\n");
            List<ArrayTrace.Operation> plan = new ArrayList<>();
            if (indexed) {
                plan.add(new ArrayTrace.Operation("VARIABLE_DECLARE", decode(fields[5]), "variable-1", names[0]));
                plan.add(new ArrayTrace.Operation("ARRAY_DECLARE", decode(fields[6]), "variable-2", names[1]));
                plan.add(new ArrayTrace.Operation("VARIABLE_DECLARE", decode(fields[10]), "variable-3", names[2]));
                if (indexUpdate) plan.add(new ArrayTrace.Operation("VARIABLE_WRITE", decode(fields[9]), "variable-3", names[2]));
                plan.add(new ArrayTrace.Operation("ARRAY_WRITE", decode(fields[7]), null, null));
            }
            ArrayTrace trace = indexed ? new ArrayTrace(plan)
                : combined ? new ArrayTrace(ranges, names[0], names[1]) : new ArrayTrace(ranges, variable, scalar);
            RunnerHarness.Result actual = harness.run(generated.getBytes(StandardCharsets.UTF_8), new AtomicBoolean(), new AtomicBoolean(), trace);
            var captured = trace.seal();
            check(actual.cleanupVerified(), "Generated cleanup unconfirmed");
            RunnerHarness.Outcome expected = switch (name) {
                case "failed-read", "failed-write", "empty", "mix-negative", "mix-oob", "mix-empty", "update-negative", "update-oob", "update-empty" -> RunnerHarness.Outcome.RUNTIME_FAILURE;
                case "trace-limit", "int-limit", "mix-limit1", "mix-limit2", "update-limit1", "update-limit2", "update-limit3" -> RunnerHarness.Outcome.TRACE_LIMIT;
                case "index-negative", "index-oob", "index-minindex", "index-maxindex", "index-empty" -> RunnerHarness.Outcome.RUNTIME_FAILURE;
                case "index-limit1", "index-limit2", "index-limit3" -> RunnerHarness.Outcome.TRACE_LIMIT;
                case "index-update-negative", "index-update-oob", "index-update-minindex", "index-update-maxindex", "index-update-empty" -> RunnerHarness.Outcome.RUNTIME_FAILURE;
                case "index-update-limit1", "index-update-limit2", "index-update-limit3", "index-update-limit4" -> RunnerHarness.Outcome.TRACE_LIMIT;
                case "index-add-negative", "index-add-oob", "index-add-empty", "index-add-overflow-max", "index-add-overflow-min" -> RunnerHarness.Outcome.RUNTIME_FAILURE;
                case "index-add-limit1", "index-add-limit2", "index-add-limit3", "index-add-limit4" -> RunnerHarness.Outcome.TRACE_LIMIT;
                default -> RunnerHarness.Outcome.SUCCESS;
            };
            if (increment) {
                String variant = name.substring(name.startsWith("index-post-") ? 11 : 10);
                expected = Set.of("negative", "oob", "empty", "overflow-max", "overflow-min").contains(variant) ? RunnerHarness.Outcome.RUNTIME_FAILURE
                    : Set.of("limit1", "limit2", "limit3", "limit4").contains(variant) ? RunnerHarness.Outcome.TRACE_LIMIT : RunnerHarness.Outcome.SUCCESS;
            }
            check(actual.outcome() == expected, name + ": " + actual.outcome() + " " + actual.stderr() + " " + actual.detail());
            int events = indexUpdate ? expected == RunnerHarness.Outcome.SUCCESS ? 5 : expected == RunnerHarness.Outcome.TRACE_LIMIT ? Integer.parseInt(name.substring(name.length() - 1)) : 4
                : update || indexed ? expected == RunnerHarness.Outcome.SUCCESS ? 4 : name.endsWith("-limit1") ? 1 : name.endsWith("-limit2") ? 2 : 3
                : combined ? name.equals("mix-limit1") ? 1 : expected == RunnerHarness.Outcome.SUCCESS ? 3 : 2
                : scalar ? name.equals("int-limit") ? 1 : 2 : switch (name) {
                case "failed-read", "empty" -> 1;
                case "failed-write", "trace-limit" -> 2;
                default -> 3;
            };
            check(captured.events().size() == events, "Wrong event prefix: " + name + " " + captured);
            check(captured.complete() == (expected == RunnerHarness.Outcome.SUCCESS), "Wrong completion");
            if (expected != RunnerHarness.Outcome.TRACE_LIMIT) {
                RunnerHarness.Result baseline = harness.run(original.getBytes(StandardCharsets.UTF_8), new AtomicBoolean(), new AtomicBoolean());
                check(baseline.cleanupVerified() && baseline.outcome() == expected, "Original execution/cleanup failed");
                check(actual.stdout().equals(baseline.stdout()), "Output/final values changed: " + name);
                if (expected == RunnerHarness.Outcome.SUCCESS) check(actual.stderr().equals(baseline.stderr()), "stderr changed");
                else check(actual.stderr().lines().findFirst().equals(baseline.stderr().lines().findFirst()), "Exception type/message changed");
                comparisons++;
            }
            String raw = "{\"runId\":" + json(actual.container()) + ",\"sourceId\":" + json(ArrayRecordingTest.sha(original))
                + ",\"instrumentedSourceId\":" + json(actual.sourceSha256()) + ",\"originalSource\":" + json(original)
                + ",\"instrumentedSource\":" + json(generated) + ",\"metadata\":" + metadata
                + ",\"outcome\":" + json(actual.outcome().name()) + ",\"stdout\":" + json(actual.stdout())
                + ",\"stderr\":" + json(actual.stderr()) + ",\"complete\":" + captured.complete()
                + ",\"problem\":" + json(captured.problem()) + ",\"events\":[" + String.join(",", captured.events()) + "]}";
            Files.writeString(results.resolve(name + ".raw.json"), raw, StandardCharsets.UTF_8);
            Files.writeString(results.resolve(name + ".original.java"), original, StandardCharsets.UTF_8);
            Files.writeString(results.resolve(name + ".instrumented.java"), generated, StandardCharsets.UTF_8);
            System.out.printf("PASS automatic %-12s outcome=%s events=%d cleanup=true%n", name, expected, events);
        }
        check(seen.equals(CASES), "Missing development fixtures");
        check(comparisons == 124, "Missing original/generated comparisons");
        System.out.println("PASS " + seen.size() + " automatic recording cases and " + comparisons + " original/generated comparisons");
    }
    // Validate both fixed, bounded batches before any fixture execution. No stale default bundle.
    static List<String[]> loadBundles(Path directory, Set<String> expected) throws java.io.IOException {
        List<String[]> artifacts = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String filename : List.of("batch-0.txt", "batch-1.txt")) {
            Path file = directory.resolve(filename);
            check(Files.size(file) <= 1_048_576, "Unbounded development artifact bundle");
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            check(!lines.isEmpty(), "Empty fixture batch");
            for (String line : lines) {
                String[] fields = line.split(" ", -1);
                check(fields.length == 11 && fields[0].equals("ARTIFACT") && expected.contains(fields[1]) && seen.add(fields[1]), "Invalid/duplicate development artifact");
                artifacts.add(fields);
            }
        }
        check(seen.equals(expected), "Missing development fixtures across batches");
        return List.copyOf(artifacts);
    }
    static String decode(String value) { return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8); }
    static String json(String value) { return ArrayRecordingTest.json(value); }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
