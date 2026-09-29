import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Controlled-fixture feasibility harness, not an arbitrary-source execution service. */
public final class RunnerHarness {
    static final String IMAGE = "docker.io/library/eclipse-temurin@sha256:87f0a5e638d0bc739f2908adb4a0a697b585d8bee5e5a8de43c88e8d07b3786e";
    static final int OUTPUT_LIMIT = 1024 * 1024;
    enum Outcome { SUCCESS, COMPILE_ERROR, RUNTIME_FAILURE, TIMEOUT, CANCELLED, OUTPUT_LIMIT, TRACE_LIMIT, TRACE_ERROR, INFRASTRUCTURE_ERROR }
    record Result(String container, String sourceSha256, Outcome outcome, String stage,
                  Integer exitCode, String stdout, String stderr, int retainedBytes,
                  boolean truncated, boolean launched, boolean cleanupVerified,
                  long elapsedMillis, String detail) {}
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean cleanupUncertain;
    @FunctionalInterface
    interface ProcessStarter { Process start(List<String> argv) throws IOException; }
    private final ProcessStarter processes;
    RunnerHarness() { this(argv -> new ProcessBuilder(argv).start()); }
    // Trusted test seam only; submitted code cannot supply commands or a process launcher.
    RunnerHarness(ProcessStarter processes) { this.processes = processes; }

    Result run(byte[] source, AtomicBoolean cancel, AtomicBoolean launched) throws Exception {
        return run(source, cancel, launched, null);
    }

    Result run(byte[] source, AtomicBoolean cancel, AtomicBoolean launched, ArrayTrace trace) throws Exception {
        if (source.length > 65536) throw new IllegalArgumentException("Source exceeds 64 KiB");
        if (!busy.compareAndSet(false, true)) throw new IllegalStateException("Runner busy");
        try {
            if (cleanupUncertain) throw new IllegalStateException("Cleanup unresolved; admission blocked");
            return execute(source, cancel, launched, trace);
        } finally { busy.set(false); }
    }

    private Result execute(byte[] source, AtomicBoolean cancel, AtomicBoolean launchSignal, ArrayTrace trace) throws Exception {
        long start = System.nanoTime(), deadline = start + TimeUnit.SECONDS.toNanos(30);
        String name = "codeviz-prototype-" + UUID.randomUUID();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        AtomicReference<Outcome> cause = new AtomicReference<>();
        if (trace != null) trace.onFailure(failure -> cause.compareAndSet(null,
            failure == ArrayTrace.Failure.LIMIT ? Outcome.TRACE_LIMIT : Outcome.TRACE_ERROR));
        TracePipe pipe = null;
        Capture output = new Capture(OUTPUT_LIMIT, () -> cause.compareAndSet(null, Outcome.OUTPUT_LIMIT));
        String stage = "create", detail = "";
        Integer exit = null;
        boolean launched = false, attempted = false, cleaned;
        try {
            attempted = true;
            Command created = command(deadline, 10, cancel, cause, null, null,
                "create", "--pull=never", "--platform=linux/amd64", "--name=" + name,
                "--label=codeviz.prototype=" + name, "--user=10001:10001", "--network=none",
                "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges=true",
                "--memory=512m", "--memory-swap=512m", "--cpus=1", "--pids-limit=128",
                "--tmpfs=/work:rw,nosuid,nodev,noexec,size=56m,uid=10001,gid=10001,mode=0700",
                "--tmpfs=/tmp:rw,nosuid,nodev,noexec,size=8m,uid=10001,gid=10001,mode=0700",
                "--log-driver=none", "--workdir=/work", "--entrypoint=/bin/sleep", IMAGE, "30");
            checked(created);
            String containerId = created.stdout.strip();
            if (!containerId.matches("[a-f0-9]{64}")) throw new IOException("Invalid Docker container identity");
            stage = "start";
            checked(command(deadline, 5, cancel, cause, null, null, "start", name));
            stage = "transfer";
            // Source travels on stdin, never in shell syntax or a Windows command-line argument.
            checked(command(deadline, 5, cancel, cause, null, source,
                "exec", "-i", name, "/bin/sh", "-c", "cat > /work/Main.java"));
            if (trace != null) checked(command(deadline, 5, cancel, cause, null, null,
                "exec", name, "mkfifo", "-m", "600", "/work/trace.pipe"));
            stage = "compile";
            exit = verifiedExec(deadline, 15, cancel, cause, output, containerId,
                "javac", "-J-Xmx256m", "-J-XX:ActiveProcessorCount=1", "-proc:none",
                "--release", "21", "-d", "/work", "/work/Main.java");
            if (exit != 0) cause.compareAndSet(null, Outcome.COMPILE_ERROR);
            if (cause.get() == null) {
                stage = "run";
                exit = null;
                long runDeadline = Math.min(deadline, System.nanoTime() + TimeUnit.SECONDS.toNanos(5));
                if (trace != null) pipe = new TracePipe(processes, containerId, trace);
                launched = true;
                // Signal is launch request, not proof main() has started. Tests also check fixture output.
                launchSignal.set(true);
                exit = verifiedExec(runDeadline, 5, cancel, cause, output, containerId,
                    "java", "-Xmx64m", "-XX:ActiveProcessorCount=1", "-XX:-UsePerfData",
                    "-cp", "/work", "Main");
                if (pipe != null) {
                    while (!pipe.done() && cause.get() == null) {
                        if (cancel.get()) cause.compareAndSet(null, Outcome.CANCELLED);
                        if (System.nanoTime() >= runDeadline) cause.compareAndSet(null, Outcome.TIMEOUT);
                        if (cause.get() == null) Thread.sleep(10);
                    }
                    if (!pipe.successful()) cause.compareAndSet(null, Outcome.TRACE_ERROR);
                    if (exit == 0 && !trace.complete()) cause.compareAndSet(null, Outcome.TRACE_ERROR);
                }
                cause.compareAndSet(null, exit == 0 ? Outcome.SUCCESS : Outcome.RUNTIME_FAILURE);
            }
        } catch (Exception failure) {
            cause.compareAndSet(null, Outcome.INFRASTRUCTURE_ERROR);
            detail = failure.toString();
        } finally {
            // Removing the environment kills all its processes, including an abandoned docker exec.
            long cleanupDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            cleaned = !attempted || cleanup(name, cleanupDeadline);
            if (pipe != null) pipe.close(cleanupDeadline);
            if (!cleaned) cleanupUncertain = true;
        }
        return new Result(name, hash, cause.get(), stage, exit, output.out(), output.err(),
            output.size(), output.overflow, launched, cleaned,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), detail);
    }

    private record Command(int exit, String stdout, String stderr) {}

    private int verifiedExec(long overallDeadline, int seconds, AtomicBoolean cancel,
            AtomicReference<Outcome> cause, Capture output, String containerId, String... program) throws Exception {
        long deadline = Math.min(overallDeadline, System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds));
        List<String> argv = new ArrayList<>(List.of("exec", containerId));
        argv.addAll(List.of(program));
        Command result = command(deadline, seconds, cancel, cause, output, null, argv.toArray(String[]::new));
        // Separate management channel. Neither console text nor a file writable by Main is evidence.
        Command evidence = command(deadline, 3, cancel, cause, null, null,
            "events", "--since=0", "--until=" + Instant.now(), "--filter=type=container",
            "--filter=container=" + containerId, "--filter=event=exec_create", "--filter=event=exec_die",
            "--format={{.Actor.ID}}|{{.Action}}|{{.Actor.Attributes.execID}}|{{.Actor.Attributes.exitCode}}");
        checked(evidence);
        return ExecEvidence.confirm(containerId, String.join(" ", program), result.exit, evidence.stdout);
    }

    private static void checked(Command result) throws IOException {
        if (result.exit != 0) throw new IOException("Docker command failed: " + result.stderr);
    }

    private Command command(long overallDeadline, int seconds, AtomicBoolean cancel,
            AtomicReference<Outcome> cause, Capture userOutput, byte[] input, String... args) throws Exception {
        if (cancel.get()) cause.compareAndSet(null, Outcome.CANCELLED);
        if (cause.get() != null) throw new IOException("Run stopped: " + cause.get());
        long deadline = Math.min(overallDeadline, System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds));
        List<String> argv = new ArrayList<>(List.of("docker"));
        argv.addAll(List.of(args));
        Process process = processes.start(List.copyOf(argv));
        Capture capture = userOutput != null ? userOutput : new Capture(65536,
            () -> cause.compareAndSet(null, Outcome.INFRASTRUCTURE_ERROR));
        AtomicReference<IOException> ioFailure = new AtomicReference<>();
        Thread out = drain(process.getInputStream(), capture, false, ioFailure);
        Thread err = drain(process.getErrorStream(), capture, true, ioFailure);
        Thread writer = Thread.ofVirtual().start(() -> {
            try (OutputStream stream = process.getOutputStream()) {
                if (input != null) stream.write(input);
            } catch (IOException e) { ioFailure.compareAndSet(null, e); }
        });
        try {
            while (true) {
                if (cancel.get()) cause.compareAndSet(null, Outcome.CANCELLED);
                if (ioFailure.get() != null) cause.compareAndSet(null, Outcome.INFRASTRUCTURE_ERROR);
                if (System.nanoTime() >= deadline) cause.compareAndSet(null, Outcome.TIMEOUT);
                if (cause.get() != null) throw new IOException("Run stopped: " + cause.get());
                if (!process.isAlive() && !out.isAlive() && !err.isAlive() && !writer.isAlive()) break;
                Thread.sleep(10);
            }
            return new Command(process.exitValue(), capture.out(), capture.err());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            // Daemon virtual collectors do not hold the host alive on a broken Docker connection.
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        }
    }

    private static Thread drain(InputStream stream, Capture capture, boolean stderr,
                                AtomicReference<IOException> failure) {
        return Thread.ofVirtual().start(() -> {
            try (stream) {
                byte[] buffer = new byte[8192];
                for (int count; (count = stream.read(buffer)) != -1;) capture.append(buffer, count, stderr);
            } catch (IOException e) { failure.compareAndSet(null, e); }
        });
    }

    private boolean cleanup(String name, long deadline) {
        try {
            AtomicReference<Outcome> cause = new AtomicReference<>();
            AtomicBoolean cancel = new AtomicBoolean();
            // Exact random name generated by this invocation only; never remove by a broad filter.
            command(deadline, 5, cancel, cause, null, null, "rm", "--force", name);
            Command remaining = command(deadline, 5, cancel, cause, null, null,
                "ps", "-aq", "--filter", "name=^/" + name + "$", "--no-trunc");
            return remaining.exit == 0 && remaining.stdout.isBlank();
        } catch (Exception e) { return false; }
    }

    private static final class Capture {
        final int limit;
        final Runnable exceeded;
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        volatile boolean overflow;
        Capture(int limit, Runnable exceeded) { this.limit = limit; this.exceeded = exceeded; }
        synchronized void append(byte[] bytes, int count, boolean error) {
            int accepted = Math.min(count, limit - size());
            (error ? stderr : stdout).write(bytes, 0, accepted);
            if (accepted < count) { overflow = true; exceeded.run(); }
        }
        synchronized int size() { return stdout.size() + stderr.size(); }
        synchronized String out() { return stdout.toString(StandardCharsets.UTF_8); }
        synchronized String err() { return stderr.toString(StandardCharsets.UTF_8); }
    }
}
