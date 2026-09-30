import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Trusted host orchestration. Generated and original Java execute only in RunnerHarness containers. */
public final class LoopRecordingTest {
    static final List<String> VARIANTS = variants();
    static final Set<String> CASES = cases();
    private static List<String> variants() {
        var names = new ArrayList<>(List.of("success", "renamed", "equal", "repeat", "empty", "one", "start1", "startlength", "startabove", "negative", "minindex", "maxindex",
            "minvalue", "maxvalue", "formatted", "collision", "no-probe", "nine", "ten", "sixteen", "sixteen-start7"));
        for (int n = 1; n <= 13; n++) names.add("limit" + n);
        return List.copyOf(names);
    }
    private static Set<String> cases() {
        Set<String> result = new HashSet<>();
        for (String group : List.of("loop-post-", "loop-pre-")) for (String variant : VARIANTS) result.add(group + variant);
        return Set.copyOf(result);
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), results = root.resolve(".results/loops"); Files.createDirectories(results);
        var artifacts = loadBundles(Path.of(args[1]));
        int comparisons = 0;
        for (var fields : artifacts) {
            String name = fields[1], variant = name.substring(name.startsWith("loop-pre-") ? 9 : 10);
            String original = decode(fields[2]), generated = decode(fields[3]);
            String[] names = decode(fields[4]).split("\n"), ranges = decode(fields[5]).split("\n");
            check(names.length == 3 && ranges.length == 6 && original.getBytes(StandardCharsets.UTF_8).length <= 65536 && generated.getBytes(StandardCharsets.UTF_8).length <= 65536, "Malformed loop source/plan");
            var plan = List.of(new ArrayTrace.Operation("VARIABLE_DECLARE", ranges[0], "variable-1", names[0]),
                new ArrayTrace.Operation("ARRAY_DECLARE", ranges[1], "variable-2", names[1]), new ArrayTrace.Operation("VARIABLE_DECLARE", ranges[2], "variable-3", names[2]),
                new ArrayTrace.Operation("CONDITION", ranges[3], null, null), new ArrayTrace.Operation("ARRAY_WRITE", ranges[4], null, null),
                new ArrayTrace.Operation("VARIABLE_WRITE", ranges[5], "variable-3", names[2]));
            var trace = new ArrayTrace(new LoopTracePlan(plan)); var harness = new RunnerHarness();
            var actual = harness.run(generated.getBytes(StandardCharsets.UTF_8), new AtomicBoolean(), new AtomicBoolean(), trace);
            var captured = trace.seal();
            boolean limited = variant.equals("ten") || variant.equals("sixteen") || (variant.startsWith("limit") && !variant.equals("limit13"));
            var expected = limited ? RunnerHarness.Outcome.TRACE_LIMIT : Set.of("negative", "minindex").contains(variant) ? RunnerHarness.Outcome.RUNTIME_FAILURE : RunnerHarness.Outcome.SUCCESS;
            int count = limited ? variant.startsWith("limit") ? Integer.parseInt(variant.substring(5)) : 32
                : switch (variant) { case "negative", "minindex", "empty", "startlength", "startabove", "maxindex" -> 4;
                    case "one" -> 7; case "renamed", "equal", "start1" -> 10; case "nine", "sixteen-start7" -> 31; default -> 13; };
            check(actual.cleanupVerified() && actual.outcome() == expected, name + " execution/cleanup: " + actual.outcome() + " " + actual.detail() + " " + actual.stderr());
            check(captured.events().size() == count && captured.complete() == (expected == RunnerHarness.Outcome.SUCCESS), name + " trace prefix: " + captured);
            if (!limited) {
                var baseline = harness.run(original.getBytes(StandardCharsets.UTF_8), new AtomicBoolean(), new AtomicBoolean());
                check(baseline.cleanupVerified() && baseline.outcome() == expected && baseline.stdout().equals(actual.stdout()), name + " original behavior/cleanup");
                if (expected == RunnerHarness.Outcome.SUCCESS) check(baseline.stderr().equals(actual.stderr()), name + " stderr");
                else check(baseline.stderr().lines().findFirst().equals(actual.stderr().lines().findFirst()), name + " exception type/message");
                comparisons++;
            }
            String raw = "{\"runId\":" + json(actual.container()) + ",\"sourceId\":" + json(ArrayRecordingTest.sha(original))
                + ",\"instrumentedSourceId\":" + json(actual.sourceSha256()) + ",\"originalSource\":" + json(original) + ",\"instrumentedSource\":" + json(generated)
                + ",\"metadata\":" + decode(fields[6]) + ",\"outcome\":" + json(actual.outcome().name()) + ",\"stdout\":" + json(actual.stdout()) + ",\"stderr\":" + json(actual.stderr())
                + ",\"complete\":" + captured.complete() + ",\"problem\":" + json(captured.problem()) + ",\"events\":[" + String.join(",", captured.events()) + "]}";
            Files.writeString(results.resolve(name + ".raw.json"), raw, StandardCharsets.UTF_8);
            System.out.printf("PASS loop %-25s outcome=%s events=%d cleanup=true%n", name, expected, count);
        }
        check(comparisons == 40, "Missing original/generated loop comparisons");
        System.out.println("PASS 68 loop execution cases and 40 original/generated comparisons");
    }
    static List<String[]> loadBundles(Path directory) throws java.io.IOException {
        List<String[]> result = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String group : List.of("post", "pre")) {
            var file = directory.resolve("loop-" + group + ".txt");
            check(Files.size(file) <= 1048576, "Unbounded loop fixture batch");
            var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            check(lines.size() == 34, "Missing loop batch fixtures");
            for (String line : lines) {
                String[] fields = line.split(" ", -1);
                check(fields.length == 7 && fields[0].equals("LOOP_ARTIFACT") && CASES.contains(fields[1]) && fields[1].startsWith("loop-" + group + "-") && seen.add(fields[1]), "Invalid/duplicate loop fixture");
                result.add(fields);
            }
        }
        check(seen.equals(CASES), "Missing loop fixtures across batches"); return List.copyOf(result);
    }
    static String decode(String value) { return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8); }
    static String json(String value) { return ArrayRecordingTest.json(value); }
    static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
