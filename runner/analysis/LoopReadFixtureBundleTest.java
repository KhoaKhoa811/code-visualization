import java.nio.file.*;
import java.util.*;

/** Reject corrupt loop fixture delivery before any submitted-source execution. */
public final class LoopReadFixtureBundleTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of(args[0]), "read-bundle-check-");
        Path post = directory.resolve("read-post.txt"), pre = directory.resolve("read-pre.txt");
        var postLines = lines("post"); var preLines = lines("pre");
        int negatives = 0;
        try {
            Files.write(post, postLines); Files.write(pre, preLines);
            check(LoopReadRecordingTest.loadBundles(directory).size() == 80, "valid loop batches");
            for (String bad : List.of(preLines.get(1), preLines.getFirst().replace("read-pre-success", "read-pre-unknown"),
                preLines.getFirst().replace("read-pre-success", "read-post-success"), "READ_ARTIFACT read-pre-success eA==",
                preLines.getFirst().replace("READ_ARTIFACT", "ARTIFACT"))) {
                var changed = new ArrayList<>(preLines); changed.set(0, bad); Files.write(pre, changed);
                rejected(directory); negatives++;
            }
            Files.write(pre, preLines.subList(1, preLines.size())); rejected(directory); negatives++;
            var extra = new ArrayList<>(preLines); extra.add(preLines.getFirst()); Files.write(pre, extra); rejected(directory); negatives++;
            Files.writeString(pre, ""); rejected(directory); negatives++;
            Files.write(pre, new byte[1_048_577]); rejected(directory); negatives++;
            Files.delete(pre); rejected(directory); negatives++;
            System.out.println("PASS loop fixture batches: valid 80-case delivery and " + negatives + " rejection cases");
        } finally { Files.deleteIfExists(post); Files.deleteIfExists(pre); Files.delete(directory); }
    }
    private static List<String> lines(String form) {
        return LoopReadRecordingTest.VARIANTS.stream().map(v -> "READ_ARTIFACT read-" + form + "-" + v + " eA==".repeat(5) + " 1").toList();
    }
    private static void rejected(Path directory) throws Exception {
        boolean rejected = false;
        try { LoopReadRecordingTest.loadBundles(directory); } catch (AssertionError | java.io.IOException expected) { rejected = true; }
        check(rejected, "Invalid loop batch accepted");
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
