import java.io.IOException;
import java.time.Instant;
import java.util.*;

/** Confirms a completed process using bounded Docker management-channel events only. */
final class ExecEvidence {
    static int confirmTimed(String container, String command, int cliExit, String events, Instant cutoff) throws IOException {
        List<String> canonical = new ArrayList<>();
        for (String line : events.lines().toList()) {
            String[] row = line.split("\\|", -1);
            if (row.length != 5) throw new IOException("Invalid Docker execution evidence");
            long time = timestamp(row[4]);
            if (Instant.ofEpochSecond(0, time).isAfter(cutoff)) throw new IOException("Docker event after evidence cutoff");
            canonical.add(String.join("|", Arrays.copyOf(row, 4)));
        }
        return confirm(container, command, cliExit, String.join("\n", canonical));
    }

    /** Bounded aggregate diagnostics. Raw command/event text is never reflected to the caller. */
    static String describe(String container, String command, String events) {
        if (events == null) return "evidence=unavailable";
        boolean truncated = events.length() > 65536;
        String bounded = events.substring(0, Math.min(events.length(), 65536));
        List<String> lines = bounded.lines().limit(257).toList();
        truncated |= lines.size() > 256;
        int rows = 0, malformed = 0, creates = 0, completions = 0;
        long earliest = Long.MAX_VALUE, latest = Long.MIN_VALUE;
        Set<String> ids = new HashSet<>(); List<String> dies = new ArrayList<>();
        for (String line : lines.subList(0, Math.min(lines.size(), 256))) {
            rows++;
            String[] row = line.split("\\|", -1);
            try {
                if (row.length != 5 || !row[0].equals(container) || !row[2].matches("[a-f0-9]{64}")) throw new IOException();
                long time = timestamp(row[4]); earliest = Math.min(earliest, time); latest = Math.max(latest, time);
                if (row[1].equals("exec_create: " + command)) { creates++; ids.add(row[2]); }
                if (row[1].equals("exec_die")) dies.add(row[2]);
            } catch (IOException invalid) { malformed++; }
        }
        for (String id : dies) if (ids.contains(id)) completions++;
        return "rows=" + rows + ", matchedCreates=" + creates + ", matchedCompletions=" + completions + ", malformedRows=" + malformed
            + ", earliestEventNs=" + (earliest == Long.MAX_VALUE ? "unavailable" : earliest)
            + ", latestEventNs=" + (latest == Long.MIN_VALUE ? "unavailable" : latest) + ", summaryTruncated=" + truncated;
    }

    private static long timestamp(String text) throws IOException {
        if (!text.matches("[0-9]{1,19}")) throw new IOException("Invalid Docker event timestamp");
        try { long value = Long.parseLong(text); if (value <= 0) throw new NumberFormatException(); return value; }
        catch (NumberFormatException invalid) { throw new IOException("Invalid Docker event timestamp"); }
    }

    static int confirm(String container, String command, int cliExit, String events) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : events.lines().toList()) {
            String[] row = line.split("\\|", -1);
            if (row.length != 4 || !row[0].equals(container) || !row[2].matches("[a-f0-9]{64}")) {
                throw new IOException("Invalid Docker execution evidence");
            }
            rows.add(row);
        }
        List<String[]> created = rows.stream().filter(r -> r[1].equals("exec_create: " + command)).toList();
        if (created.size() != 1) throw new IOException("Missing or ambiguous Docker exec identity");
        String execId = created.getFirst()[2];
        List<String[]> finished = rows.stream().filter(r -> r[1].equals("exec_die") && r[2].equals(execId)).toList();
        if (finished.size() != 1) throw new IOException("No unique Docker process completion; execution outcome unknown");
        try {
            int processExit = Integer.parseInt(finished.getFirst()[3]);
            if (processExit < 0 || processExit > 255 || processExit != cliExit) {
                throw new IOException("Docker CLI and process completion disagree; execution outcome unknown");
            }
            return processExit;
        } catch (NumberFormatException invalid) {
            throw new IOException("Invalid Docker process exit code", invalid);
        }
    }
}
