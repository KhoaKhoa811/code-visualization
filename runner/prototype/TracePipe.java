import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** One separate Docker exec reader; the runner still owns deadlines and whole-container cleanup. */
final class TracePipe {
    private final Process process;
    private final Thread reader, errors;
    private final ArrayTrace trace;
    TracePipe(RunnerHarness.ProcessStarter starter, String container, ArrayTrace trace) throws IOException {
        this.trace = trace;
        process = starter.start(List.of("docker", "exec", container, "cat", "/work/trace.pipe"));
        process.getOutputStream().close();
        reader = Thread.ofVirtual().start(() -> {
            try (InputStream input = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                for (int n; (n = input.read(buffer)) != -1;) trace.accept(buffer, n);
                trace.finish();
            } catch (IOException failure) { trace.transportFailure("Trace reader interrupted"); }
        });
        errors = Thread.ofVirtual().start(() -> {
            try (InputStream input = process.getErrorStream()) {
                // No unbounded diagnostic accumulation. Any stderr from fixed cat/CLI means transport trouble.
                if (input.read() != -1) trace.transportFailure("Docker trace reader reported an error");
                byte[] discard = new byte[4096];
                while (input.read(discard) != -1) { }
            } catch (IOException failure) { trace.transportFailure("Trace error stream interrupted"); }
        });
    }
    boolean done() { return !process.isAlive() && !reader.isAlive() && !errors.isAlive(); }
    boolean successful() { return done() && process.exitValue() == 0; }
    void close(long deadline) throws InterruptedException {
        while (!done() && System.nanoTime() < deadline) Thread.sleep(10);
        if (!done()) {
            trace.transportFailure("Trace collector shutdown unconfirmed");
            process.destroyForcibly();
        }
    }
}
