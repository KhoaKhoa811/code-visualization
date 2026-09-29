import java.io.*;
import java.nio.charset.StandardCharsets;
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
                "exit-125", "exit-126", "exit-127")) {
            FakeDocker docker = new FakeDocker(scenario);
            RunnerHarness.Result result = new RunnerHarness(docker).run(
                "class Main {}".getBytes(StandardCharsets.UTF_8), new AtomicBoolean(), new AtomicBoolean());
            RunnerHarness.Outcome expected = scenario.equals("success") ? RunnerHarness.Outcome.SUCCESS
                : scenario.equals("compile-error") ? RunnerHarness.Outcome.COMPILE_ERROR
                : scenario.startsWith("exit-") ? RunnerHarness.Outcome.RUNTIME_FAILURE
                : RunnerHarness.Outcome.INFRASTRUCTURE_ERROR;
            check(result.outcome() == expected, scenario + ": " + result.outcome() + " " + result.detail());
            check(result.cleanupVerified() && docker.removed, scenario + ": cleanup missing");
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
        System.out.println(cases + "/" + cases + " simulated Docker classification cases passed.");
    }

    static final class FakeDocker implements RunnerHarness.ProcessStarter {
        final String scenario;
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
                case "exec" -> {
                    if (argv.get(2).equals("-i")) yield response(0, "", "");
                    check(argv.get(2).equals(CONTAINER), "exec must target returned immutable container ID");
                    String program = String.join(" ", argv.subList(3, argv.size()));
                    if (argv.get(3).equals("javac")) {
                        compileCommand = program;
                        yield response(scenario.startsWith("compile-") ? 1 : 0, "", "");
                    }
                    ran = true;
                    runCommand = program;
                    int exit = scenario.startsWith("exit-") ? Integer.parseInt(scenario.substring(5))
                        : scenario.equals("run-docker") || scenario.equals("mismatched-exit") ? 1
                        : scenario.equals("launch-failure") ? 126 : 0;
                    String spoof = scenario.equals("spoofed-output") ? event("exec_create: " + program, RUN, "")
                        + event("exec_die", RUN, "0") : "";
                    yield response(exit, spoof, "");
                }
                case "events" -> {
                    check(argv.contains("--filter=container=" + CONTAINER), "Evidence must be container-scoped");
                    check(argv.stream().anyMatch(a -> a.startsWith("--until=")), "Evidence query must terminate");
                    if (!ran) {
                        if (scenario.equals("compile-docker")) yield response(0, "", "");
                        yield response(0, event("exec_create: " + compileCommand, COMPILE, "")
                            + event("exec_die", COMPILE, scenario.equals("compile-error") ? "1" : "0"), "");
                    }
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
                    if (scenario.equals("malformed-evidence")) evidence = "invalid\n";
                    yield response(0, evidence, "");
                }
                default -> throw new IOException("Unexpected Docker operation: " + argv);
            };
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
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
