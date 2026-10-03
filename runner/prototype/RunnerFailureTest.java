import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Simulated Docker processes exercise the full harness; no submitted Java executes on the host. */
public final class RunnerFailureTest {
    static final String CONTAINER = "a".repeat(64), COMPILE = "b".repeat(64), RUN = "c".repeat(64);
    public static void main(String[] args) throws Exception {
        int cases = 0;
        for (String scenario : List.of("compile-docker", "run-docker", "launch-failure", "event-query-failure",
                "missing-evidence", "mismatched-exit", "spoofed-output", "wrong-container", "wrong-exec",
                "duplicate-evidence", "malformed-evidence", "compile-error", "success", "exit-1",
                "exit-125", "exit-126", "exit-127", "clock-behind", "clock-ahead", "clock-invalid",
                "clock-failure", "clock-launch-failure", "clock-empty", "timestamp-invalid", "timestamp-future",
                "duplicate-create", "clock-cancel", "events-cancel", "clock-overflow", "clock-timeout", "events-timeout", "shared-deadline")) {
            FakeDocker docker = new FakeDocker(scenario);
            RunnerHarness.Result result = new RunnerHarness(docker).run(
                "class Main {}".getBytes(StandardCharsets.UTF_8), docker.cancel, new AtomicBoolean());
            RunnerHarness.Outcome expected = List.of("success", "clock-behind", "clock-ahead").contains(scenario) ? RunnerHarness.Outcome.SUCCESS
                : scenario.endsWith("-cancel") ? RunnerHarness.Outcome.CANCELLED
                : scenario.endsWith("-timeout") || scenario.equals("shared-deadline") ? RunnerHarness.Outcome.TIMEOUT
                : scenario.equals("compile-error") ? RunnerHarness.Outcome.COMPILE_ERROR
                : scenario.startsWith("exit-") ? RunnerHarness.Outcome.RUNTIME_FAILURE
                : RunnerHarness.Outcome.INFRASTRUCTURE_ERROR;
            check(result.outcome() == expected, scenario + ": " + result.outcome() + " " + result.detail());
            check(result.cleanupVerified() && docker.removed, scenario + ": cleanup missing");
            check(docker.compiles <= 1 && docker.runs <= 1, scenario + ": submitted code retried");
            check(result.detail().length() < 1024 && !result.detail().contains("PRIVATE_MARKER"), scenario + ": unsafe diagnostic");
            if (expected == RunnerHarness.Outcome.CANCELLED || expected == RunnerHarness.Outcome.TIMEOUT)
                check(result.exitCode() == null, scenario + ": unverified exit exposed");
            if (scenario.equals("shared-deadline")) {
                check(result.elapsedMillis() >= 5000 && result.elapsedMillis() < 6500, "Execution deadline was reset for evidence");
                check(docker.eventQueries == 1, "Event query started after shared run deadline");
            }
            if (expected == RunnerHarness.Outcome.INFRASTRUCTURE_ERROR) {
                check(result.exitCode() == null, scenario + ": unverified exit exposed");
                check(!result.detail().isBlank(), scenario + ": missing infrastructure diagnostic");
            }
            if (scenario.startsWith("exit-")) {
                check(result.exitCode() == Integer.parseInt(scenario.substring(5)), "Exit code incorrectly reserved");
            }
            if (scenario.startsWith("compile-")) check(!docker.ran, "Program launched after compilation failed");
            cases++;
        }
        String summary = ExecEvidence.describe(CONTAINER, "java Main", "PRIVATE_MARKER\n".repeat(10000));
        check(summary.length() < 512 && summary.contains("summaryTruncated=true") && !summary.contains("PRIVATE_MARKER"), "Unbounded/raw summary");
        System.out.println(cases + "/" + cases + " simulated Docker classification cases passed.");
    }

    static final class FakeDocker implements RunnerHarness.ProcessStarter {
        final String scenario;
        final AtomicBoolean cancel = new AtomicBoolean();
        int compiles, runs, eventQueries;
        String compileCommand, runCommand;
        boolean ran, removed;
        FakeDocker(String scenario) { this.scenario = scenario; }
        public Process start(List<String> argv) throws IOException {
            String operation = argv.get(1);
            return switch (operation) {
                case "create" -> response(0, CONTAINER + "\n", "");
                case "start" -> response(0, "", "");
                case "rm" -> { removed = true; yield response(0, "", ""); }
                case "ps" -> response(0, "", "");
                case "info" -> {
                    check(argv.contains("--format={{.SystemTime}}"), "Must query daemon clock");
                    if (ran && scenario.equals("clock-cancel")) cancel.set(true);
                    if (ran && scenario.equals("clock-launch-failure")) throw new IOException("PRIVATE_MARKER");
                    if (ran && scenario.equals("clock-failure")) yield response(1, "", "PRIVATE_MARKER");
                    if (ran && scenario.equals("clock-invalid")) yield response(0, "PRIVATE_MARKER", "");
                    if (ran && scenario.equals("clock-empty")) yield response(0, "", "");
                    if (ran && scenario.equals("clock-overflow")) yield response(0, "PRIVATE_MARKER".repeat(6000), "");
                    if (ran && scenario.equals("clock-timeout")) yield delayed(10000);
                    if (ran && scenario.equals("shared-deadline")) yield delayed(2800);
                    yield response(0, cutoff().toString() + "\n", "");
                }
                case "exec" -> {
                    if (argv.get(2).equals("-i")) yield response(0, "", "");
                    check(argv.get(2).equals(CONTAINER), "exec must target returned immutable container ID");
                    String program = String.join(" ", argv.subList(3, argv.size()));
                    if (argv.get(3).equals("javac")) {
                        compiles++;
                        compileCommand = program;
                        yield response(scenario.startsWith("compile-") ? 1 : 0, "", "");
                    }
                    ran = true;
                    runs++;
                    if (scenario.equals("shared-deadline")) yield delayed(2800);
                    runCommand = program;
                    int exit = scenario.startsWith("exit-") ? Integer.parseInt(scenario.substring(5))
                        : scenario.equals("run-docker") || scenario.equals("mismatched-exit") ? 1
                        : scenario.equals("launch-failure") ? 126 : 0;
                    String spoof = scenario.equals("spoofed-output") ? event("exec_create: " + program, RUN, "")
                        + event("exec_die", RUN, "0") : "";
                    yield response(exit, spoof, "");
                }
                case "events" -> {
                    eventQueries++;
                    check(argv.contains("--filter=container=" + CONTAINER), "Evidence must be container-scoped");
                    check(argv.contains("--until=" + cutoff()), "Evidence cutoff must use daemon clock, independent of host clock");
                    if (!ran) {
                        if (scenario.equals("compile-docker")) yield response(0, "", "");
                        yield response(0, timed(event("exec_create: " + compileCommand, COMPILE, "")
                            + event("exec_die", COMPILE, scenario.equals("compile-error") ? "1" : "0")), "");
                    }
                    if (scenario.equals("events-cancel")) cancel.set(true);
                    if (scenario.equals("events-timeout")) yield delayed(10000);
                    if (scenario.equals("event-query-failure")) yield response(1, "", "engine unavailable");
                    if (List.of("run-docker", "missing-evidence", "spoofed-output").contains(scenario)) {
                        yield response(0, "", "");
                    }
                    String evidence = event("exec_create: " + runCommand, RUN, "");
                    if (!scenario.equals("launch-failure")) {
                        String code = scenario.startsWith("exit-") ? scenario.substring(5) : "0";
                        evidence += event("exec_die", scenario.equals("wrong-exec") ? COMPILE : RUN, code);
                    }
                    if (scenario.equals("wrong-container")) evidence = evidence.replace(CONTAINER, "d".repeat(64));
                    if (scenario.equals("duplicate-evidence")) evidence += event("exec_die", RUN, "0");
                    if (scenario.equals("duplicate-create")) evidence += event("exec_create: " + runCommand, RUN, "");
                    if (scenario.equals("malformed-evidence")) evidence = "invalid\n";
                    yield response(0, timed(evidence), "");
                }
                default -> throw new IOException("Unexpected Docker operation: " + argv);
            };
        }
        Instant cutoff() { return Instant.parse(scenario.equals("clock-behind") ? "1990-01-01T00:00:00Z" : "2040-01-01T00:00:00.123456789Z"); }
        String timed(String evidence) {
            Instant time = cutoff().plusSeconds(ran && scenario.equals("timestamp-future") ? 1 : -1);
            String stamp = ran && scenario.equals("timestamp-invalid") ? "PRIVATE_MARKER" : Long.toString(time.getEpochSecond() * 1_000_000_000L + time.getNano());
            return evidence.lines().map(line -> line + "|" + stamp + "\n").reduce("", String::concat);
        }
    }

    static String event(String action, String exec, String exit) {
        return CONTAINER + "|" + action + "|" + exec + "|" + exit + "\n";
    }
    static Process response(int exit, String out, String err) {
        return new Process() {
            final InputStream stdout = new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8));
            final InputStream stderr = new ByteArrayInputStream(err.getBytes(StandardCharsets.UTF_8));
            public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
            public InputStream getInputStream() { return stdout; }
            public InputStream getErrorStream() { return stderr; }
            public int waitFor() { return exit; }
            public boolean waitFor(long time, TimeUnit unit) { return true; }
            public int exitValue() { return exit; }
            public void destroy() {}
            public boolean isAlive() { return false; }
        };
    }
    static Process delayed(long millis) {
        return new Process() {
            final long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
            boolean destroyed;
            public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
            public InputStream getInputStream() { return InputStream.nullInputStream(); }
            public InputStream getErrorStream() { return InputStream.nullInputStream(); }
            public int waitFor() throws InterruptedException { while (isAlive()) Thread.sleep(10); return 0; }
            public int exitValue() { if (isAlive()) throw new IllegalThreadStateException(); return 0; }
            public void destroy() { destroyed = true; }
            public Process destroyForcibly() { destroy(); return this; }
            public boolean isAlive() { return !destroyed && System.nanoTime() < end; }
        };
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
