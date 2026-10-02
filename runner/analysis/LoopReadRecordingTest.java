import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Trusted host orchestration. Generated and original Java execute only in RunnerHarness containers. */
public final class LoopReadRecordingTest {
    static final List<String> VARIANTS = variants();
    static final Set<String> CASES = cases();
    private static List<String> variants() {
        var names = new ArrayList<>(List.of("success", "renamed", "equal", "repeat", "empty", "one", "start1", "startlength", "startabove", "negative", "minindex", "maxindex",
            "minvalue", "maxvalue", "formatted", "collision", "no-probe", "seven", "eight", "sixteen", "sixteen-start9", "negativeadd", "zeroadd", "minadd", "maxadd"));
        for (int n = 1; n <= 15; n++) names.add("limit" + n);
        return List.copyOf(names);
    }
    private static Set<String> cases() {
        Set<String> result = new HashSet<>();
        for (String group : List.of("read-post-", "read-pre-")) for (String variant : VARIANTS) result.add(group + variant);
        return Set.copyOf(result);
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), results = root.resolve(".results/loop-reads"); Files.createDirectories(results);
        var artifacts = loadBundles(Path.of(args[1]));
        int comparisons = 0;
        for (var fields : artifacts) {
            String name = fields[1], variant = name.substring(name.startsWith("read-pre-") ? 9 : 10);
            String original = decode(fields[2]), generated = decode(fields[3]);
            String[] names = decode(fields[4]).split("\n"), ranges = decode(fields[5]).split("\n");
            check(names.length == 2 && ranges.length == 6 && original.getBytes(StandardCharsets.UTF_8).length <= 65536 && generated.getBytes(StandardCharsets.UTF_8).length <= 65536, "Malformed loop source/plan");
            var plan = List.of(new ArrayTrace.Operation("ARRAY_DECLARE", ranges[0], "variable-1", names[0]),
                new ArrayTrace.Operation("VARIABLE_DECLARE", ranges[1], "variable-2", names[1]), new ArrayTrace.Operation("CONDITION", ranges[2], null, null),
                new ArrayTrace.Operation("ARRAY_READ", ranges[3], null, null), new ArrayTrace.Operation("ARRAY_WRITE", ranges[4], null, null),
                new ArrayTrace.Operation("VARIABLE_WRITE", ranges[5], "variable-2", names[1]));
            var trace = new ArrayTrace(new LoopTracePlan(plan, Integer.valueOf(fields[7]))); var harness = new RunnerHarness();
            var actual = harness.run(generated.getBytes(StandardCharsets.UTF_8), new AtomicBoolean(), new AtomicBoolean(), trace);
            var captured = trace.seal();
            boolean limited = variant.equals("eight") || variant.equals("sixteen") || (variant.startsWith("limit") && !variant.equals("limit15"));
            var expected = limited ? RunnerHarness.Outcome.TRACE_LIMIT : Set.of("negative", "minindex").contains(variant) ? RunnerHarness.Outcome.RUNTIME_FAILURE : RunnerHarness.Outcome.SUCCESS;
            int count = limited ? variant.startsWith("limit") ? Integer.parseInt(variant.substring(5)) : 32
                : switch (variant) { case "negative", "minindex", "empty", "startlength", "startabove", "maxindex" -> 3;
                    case "one" -> 7; case "renamed", "equal", "start1" -> 11; case "seven", "sixteen-start9" -> 31; default -> 15; };
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
            System.out.printf("PASS loop-read %-25s outcome=%s events=%d cleanup=true%n", name, expected, count);
        }
        check(comparisons == 48, "Missing original/generated loop comparisons");
        System.out.println("PASS 80 loop-read execution cases and 48 original/generated comparisons");
    }
    static List<String[]> loadBundles(Path directory) throws java.io.IOException {
        List<String[]> result = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String group : List.of("post", "pre")) {
            var file = directory.resolve("read-" + group + ".txt");
            check(Files.size(file) <= 1048576, "Unbounded loop fixture batch");
            var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            check(lines.size() == 40, "Missing loop batch fixtures");
            for (String line : lines) {
                String[] fields = line.split(" ", -1);
                check(fields.length == 8 && fields[0].equals("READ_ARTIFACT") && CASES.contains(fields[1]) && fields[1].startsWith("read-" + group + "-") && seen.add(fields[1]), "Invalid/duplicate loop fixture");
                result.add(fields);
            }
        }
        check(seen.equals(CASES), "Missing loop fixtures across batches"); return List.copyOf(result);
    }
    static String decode(String value) { return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8); }
    static String json(String value) { return ArrayRecordingTest.json(value); }
    static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
