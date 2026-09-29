import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RunnerHarnessTest {
    public static void main(String[] args) throws Exception {
        RunnerHarness harness = new RunnerHarness();
        String[] cases = {"success", "compile-error", "runtime-error", "timeout", "cancel", "output-limit", "heap-exhaustion"};
        RunnerHarness.Outcome[] expected = {RunnerHarness.Outcome.SUCCESS, RunnerHarness.Outcome.COMPILE_ERROR,
            RunnerHarness.Outcome.RUNTIME_FAILURE, RunnerHarness.Outcome.TIMEOUT, RunnerHarness.Outcome.CANCELLED,
            RunnerHarness.Outcome.OUTPUT_LIMIT, RunnerHarness.Outcome.RUNTIME_FAILURE};
        for (int i = 0; i < cases.length; i++) {
            String fixture = cases[i];
            AtomicBoolean cancel = new AtomicBoolean(), launched = new AtomicBoolean();
            Thread canceller = fixture.equals("cancel") ? Thread.ofVirtual().start(() -> {
                try {
                    while (!launched.get()) Thread.sleep(10);
                    Thread.sleep(1000);
                    cancel.set(true);
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }) : null;
            RunnerHarness.Result result;
            try {
                result = harness.run(Files.readAllBytes(Path.of(args[0], "fixtures", fixture, "Main.java")), cancel, launched);
            } finally { if (canceller != null) canceller.interrupt(); }
            check(result.cleanupVerified(), "Cleanup unconfirmed: " + result.container()
                + "; outcome=" + result.outcome() + "; stage=" + result.stage() + "; " + result.detail());
            check(result.outcome() == expected[i], fixture + ": " + result.outcome() + " " + result.detail() + " " + result.stderr());
            check(result.retainedBytes() <= RunnerHarness.OUTPUT_LIMIT, "Unbounded capture");
            check(result.elapsedMillis() < 36000, "Overall plus cleanup budget exceeded");
            check(result.launched() == !fixture.equals("compile-error"), "Incorrect launch stage");
            if (fixture.equals("timeout") || fixture.equals("cancel") || fixture.equals("output-limit")) {
                check(result.exitCode() == null, "Interrupted stage must not retain compiler exit code");
            }
            switch (fixture) {
                case "success" -> {
                    check(result.stdout().equals("ARRAY=1,1"), "stdout mismatch");
                    check(result.stderr().equals("STDERR"), "stderr mismatch");
                }
                case "compile-error" -> check(result.stderr().contains("Main.java:3"), "Missing compiler location");
                case "runtime-error" -> check(result.stderr().contains("IllegalStateException: expected"), "Missing exception");
                case "timeout" -> {
                    check(result.stage().equals("run"), "Wrong timeout stage");
                    check(result.elapsedMillis() >= 5000, "Deadline fired early");
                }
                case "cancel" -> check(result.stdout().equals("READY"), "Cancellation preceded main()");
                case "output-limit" -> check(result.truncated() && result.retainedBytes() == RunnerHarness.OUTPUT_LIMIT, "Overflow not bounded/marked");
                case "heap-exhaustion" -> check(result.stderr().contains("OutOfMemoryError: Java heap space"), "Expected controlled heap exhaustion");
            }
            System.out.printf("PASS %-16s outcome=%-15s stage=%-7s bytes=%d elapsed=%dms cleanup=true%n",
                fixture, result.outcome(), result.stage(), result.retainedBytes(), result.elapsedMillis());
        }
        System.out.println("7/7 controlled container cases passed.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
