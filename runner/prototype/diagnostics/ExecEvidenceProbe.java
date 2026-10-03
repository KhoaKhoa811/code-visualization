import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Trusted diagnostic: executes only numbered echo commands in one restricted container. */
public final class ExecEvidenceProbe {
    private static Path directory;
    private static int capture;
    record Result(int exit, String out, String err) {}
    public static void main(String[] args) throws Exception {
        directory = Path.of(args[0]); Files.createDirectories(directory);
        String name = "codeviz-evidence-probe-" + UUID.randomUUID();
        String id = null;
        int failures = 0;
        try {
            id = checked("create", "--pull=never", "--platform=linux/amd64", "--name=" + name, "--label=codeviz.evidence-probe=" + name,
                "--user=10001:10001", "--network=none", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges=true",
                "--memory=64m", "--memory-swap=64m", "--cpus=1", "--pids-limit=32", "--log-driver=none",
                "--entrypoint=/bin/sleep", RunnerHarness.IMAGE, "120").strip();
            if (!id.matches("[a-f0-9]{64}")) throw new IllegalStateException("Invalid container ID");
            checked("start", id);
            System.out.println("container=" + id);
            System.out.println("daemon=" + checked("info", "--format={{.SystemTime}}").strip() + " hostAfter=" + Instant.now());
            for (int n = 0; n < 20; n++) {
                Instant before = Instant.now();
                var execution = run("exec", id, "/bin/echo", "probe-" + n);
                if (execution.exit != 0) throw new IllegalStateException(execution.err);
                Instant cutoff = Instant.now();
                String immediate = events(id, cutoff);
                boolean first = valid(id, n, immediate);
                Thread.sleep(200);
                String sameCutoff = events(id, cutoff);
                String refreshed = events(id, Instant.now());
                boolean same = valid(id, n, sameCutoff), fresh = valid(id, n, refreshed);
                if (!first) failures++;
                System.out.printf("probe=%d before=%s cutoff=%s immediate=%s sameCutoffLater=%s refreshed=%s%n", n, before, cutoff, first, same, fresh);
                if (!fresh) throw new IllegalStateException("No complete later evidence for probe " + n);
                if (n == 0) {
                    // Controlled stale cutoff: demonstrate classification without changing any host/daemon clock.
                    String stale = events(id, before.minusSeconds(2));
                    if (valid(id, n, stale)) throw new IllegalStateException("Stale cutoff unexpectedly contains execution");
                    System.out.println("CONTROL stale-cutoff rejects successful execution; current cutoff confirms the same exec");
                }
            }
            System.out.println("SUMMARY probes=20 immediateFailures=" + failures);
        } finally {
            checked("rm", "--force", id == null ? name : id);
            if (!checked("ps", "-a", "--filter=name=^/" + name + "$", "--format={{.ID}}").isBlank()) throw new IllegalStateException("Cleanup unconfirmed");
            System.out.println("CLEANUP verified");
        }
    }
    private static String events(String id, Instant cutoff) throws Exception {
        return checked("events", "--since=0", "--until=" + cutoff, "--filter=type=container", "--filter=container=" + id,
            "--filter=event=exec_create", "--filter=event=exec_die",
            "--format={{.Actor.ID}}|{{.Action}}|{{.Actor.Attributes.execID}}|{{.Actor.Attributes.exitCode}}|{{.TimeNano}}");
    }
    private static boolean valid(String id, int n, String rows) {
        String canonical = String.join("\n", rows.lines().map(line -> line.substring(0, line.lastIndexOf('|'))).toList());
        try { ExecEvidence.confirm(id, "/bin/echo probe-" + n, 0, canonical); return true; }
        catch (java.io.IOException rejected) { System.out.println("REJECT " + rejected.getMessage()); return false; }
    }
    private static String checked(String... args) throws Exception {
        var result = run(args);
        if (result.exit != 0) throw new IllegalStateException("Docker CLI failed: " + result.err);
        return result.out;
    }
    private static Result run(String... args) throws Exception {
        int number = ++capture;
        var out = directory.resolve(number + ".out"); var err = directory.resolve(number + ".err");
        var argv = new ArrayList<>(List.of("docker")); argv.addAll(List.of(args));
        Files.writeString(directory.resolve(number + ".command"), String.join("\n", argv));
        var process = new ProcessBuilder(argv).redirectOutput(out.toFile()).redirectError(err.toFile()).start();
        try {
            if (!process.waitFor(args[0].equals("create") ? 10 : 5, TimeUnit.SECONDS)) throw new IllegalStateException("Diagnostic command timeout: " + args[0]);
            if (Files.size(out) + Files.size(err) > 65536) throw new IllegalStateException("Diagnostic capture cap");
            return new Result(process.exitValue(), Files.readString(out), Files.readString(err));
        } finally { if (process.isAlive()) { process.destroyForcibly(); process.waitFor(2, TimeUnit.SECONDS); } }
    }
}
