import java.io.IOException;
import java.util.*;

/** Confirms a completed process using bounded Docker management-channel events only. */
final class ExecEvidence {
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
