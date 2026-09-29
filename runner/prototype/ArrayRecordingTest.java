import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reviewed manual source pairs only. Trusted test driver runs on host; both Main variants run in Docker. */
public final class ArrayRecordingTest {
    record Example(String name, int first, int second, int target, int index, int limit, String fault) {}
    static final String LOOP = "if (args.length == 0) { while (true) { } }";
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), results = root.resolve("recording/.results");
        Files.createDirectories(results);
        String originalTemplate = Files.readString(root.resolve("recording/original/Main.java"));
        String instrumentedTemplate = Files.readString(root.resolve("recording/instrumented/Main.java.in"));
        String recorder = Files.readString(root.resolve("recording/Recorder.java"));
        RunnerHarness harness = new RunnerHarness();
        List<Example> examples = List.of(new Example("success", 3, 1, 0, 1, 32, ""),
            new Example("changed", 9, -4, 0, 1, 32, ""),
            new Example("failed-read", 3, 1, 0, 5, 32, ""),
            new Example("failed-write", 3, 1, 5, 1, 32, ""),
            new Example("trace-limit", 3, 1, 0, 1, 2, ""),
            new Example("cancel", 3, 1, 0, 1, 32, "after"),
            new Example("blocked-open", 3, 1, 0, 1, 32, "before"));
        for (Example example : examples) {
            String declaration = "int[] values = {" + example.first + ", " + example.second + "};";
            String assignment = "values[" + example.target + "] = values[" + example.index + "];";
            String original = originalTemplate.replace("int[] values = {3, 1};", declaration)
                .replace("values[0] = values[1];", assignment);
            // Changed case also proves CRLF source positions and UTF-16 columns after a supplementary character.
            if (example.name.equals("changed")) original = original.replace("        int[]", "        /* 🌱 */ int[]")
                .replace("\r\n", "\n").replace("\n", "\r\n");
            Map<String, String> sources = Map.of("ARRAY_DECLARE", range(original, 3, declaration.substring(0, declaration.length() - 1)),
                "ARRAY_READ", range(original, 4, "values[" + example.index + "]", true),
                "ARRAY_WRITE", range(original, 4, assignment.substring(0, assignment.length() - 1)));
            String instrumented = instrumentedTemplate.replace("@FIRST@", "" + example.first).replace("@SECOND@", "" + example.second)
                .replace("@TARGET@", "" + example.target).replace("@INDEX@", "" + example.index)
                .replace("@LIMIT@", "" + example.limit)
                .replace("@DECLARE_SOURCE@", json(sources.get("ARRAY_DECLARE")))
                .replace("@READ_SOURCE@", json(sources.get("ARRAY_READ")))
                .replace("@WRITE_SOURCE@", json(sources.get("ARRAY_WRITE")))
                .replace("@BEFORE_OPEN@", example.fault.equals("before") ? LOOP : "")
                .replace("@AFTER_DECLARE@", example.fault.equals("after") ? LOOP : "") + "\n" + recorder;
            ArrayTrace trace = new ArrayTrace(sources);
            AtomicBoolean cancel = new AtomicBoolean();
            Thread cancellation = example.name.equals("cancel") ? Thread.ofVirtual().start(() -> {
                try {
                    while (trace.eventCount() < 1) Thread.sleep(10);
                    cancel.set(true);
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }) : null;
            RunnerHarness.Result result;
            try { result = harness.run(instrumented.getBytes(StandardCharsets.UTF_8), cancel, new AtomicBoolean(), trace); }
            finally { if (cancellation != null) cancellation.interrupt(); }
            ArrayTrace.Snapshot snapshot = trace.seal();
            check(result.cleanupVerified(), "Container cleanup unconfirmed: " + result.container());
            RunnerHarness.Outcome expected = switch (example.name) {
                case "failed-read", "failed-write" -> RunnerHarness.Outcome.RUNTIME_FAILURE;
                case "trace-limit" -> RunnerHarness.Outcome.TRACE_LIMIT;
                case "cancel" -> RunnerHarness.Outcome.CANCELLED;
                case "blocked-open" -> RunnerHarness.Outcome.TIMEOUT;
                default -> RunnerHarness.Outcome.SUCCESS;
            };
            check(result.outcome() == expected, example.name + ": " + result.outcome() + " " + result.detail() + " " + result.stderr());
            int expectedEvents = switch (example.name) {
                case "failed-read", "cancel" -> 1;
                case "failed-write", "trace-limit" -> 2;
                case "blocked-open" -> 0;
                default -> 3;
            };
            check(snapshot.events().size() == expectedEvents, example.name + ": wrong safe prefix " + snapshot);
            check(snapshot.complete() == (expected == RunnerHarness.Outcome.SUCCESS), "Incorrect completion marker handling");
            check(snapshot.bytes() <= ArrayTrace.BYTE_LIMIT, "Unbounded trace capture");
            if (expected == RunnerHarness.Outcome.SUCCESS || expected == RunnerHarness.Outcome.RUNTIME_FAILURE) {
                RunnerHarness.Result baseline = harness.run(original.getBytes(StandardCharsets.UTF_8), new AtomicBoolean(), new AtomicBoolean());
                check(baseline.cleanupVerified(), "Original container cleanup failed");
                check(baseline.outcome() == result.outcome(), "Behavior outcome changed");
                check(baseline.stdout().equals(result.stdout()), "Final value/stdout probe changed");
                if (expected == RunnerHarness.Outcome.SUCCESS) {
                    check(baseline.stderr().equals(result.stderr()), "stderr changed");
                    check(result.stdout().equals("FINAL=" + example.second + "," + example.second), "Final value wrong");
                } else check(baseline.stderr().lines().findFirst().equals(result.stderr().lines().findFirst()), "Exception type/message changed");
            }
            String artifact = "{\"runId\":" + json(result.container()) + ",\"sourceId\":" + json(sha(original))
                + ",\"instrumentedSourceId\":" + json(result.sourceSha256()) + ",\"originalSource\":" + json(original)
                + ",\"outcome\":" + json(result.outcome().name()) + ",\"stdout\":" + json(result.stdout())
                + ",\"stderr\":" + json(result.stderr()) + ",\"complete\":" + snapshot.complete()
                + ",\"problem\":" + json(snapshot.problem()) + ",\"events\":[" + String.join(",", snapshot.events()) + "]}";
            Files.writeString(results.resolve(example.name + ".raw.json"), artifact);
            Files.writeString(results.resolve(example.name + ".original.java"), original);
            Files.writeString(results.resolve(example.name + ".instrumented.java"), instrumented);
            System.out.printf("PASS trace %-13s outcome=%-15s events=%d complete=%s cleanup=true%n", example.name,
                result.outcome(), snapshot.events().size(), snapshot.complete());
        }
        System.out.println("7/7 recording cases and 4 original/instrumented comparisons passed.");
    }
    static String range(String source, int line, String expression) { return range(source, line, expression, false); }
    static String range(String source, int line, String expression, boolean last) {
        String text = source.lines().toList().get(line - 1);
        int offset = last ? text.lastIndexOf(expression) : text.indexOf(expression);
        if (offset < 0) throw new IllegalArgumentException("Fixture source map does not match source");
        return "{\"file\":\"Main.java\",\"start\":{\"line\":" + line + ",\"column\":" + (offset + 1)
            + "},\"end\":{\"line\":" + line + ",\"column\":" + (offset + expression.length() + 1) + "}}";
    }
    static String sha(String source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
    }
    static String json(String value) {
        StringBuilder text = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') text.append('\\').append(c);
            else if (c < 32) text.append(String.format("\\u%04x", (int)c));
            else text.append(c);
        }
        return text.append('"').toString();
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
