import java.nio.file.*;
import java.util.*;

/** Reject corrupt loop fixture delivery before any submitted-source execution. */
public final class LoopFixtureBundleTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of(args[0]), "loop-bundle-check-");
        Path post = directory.resolve("loop-post.txt"), pre = directory.resolve("loop-pre.txt");
        var postLines = lines("post"); var preLines = lines("pre");
        int negatives = 0;
        try {
            Files.write(post, postLines); Files.write(pre, preLines);
            check(LoopRecordingTest.loadBundles(directory).size() == 68, "valid loop batches");
            for (String bad : List.of(preLines.get(1), preLines.getFirst().replace("loop-pre-success", "loop-pre-unknown"),
                preLines.getFirst().replace("loop-pre-success", "loop-post-success"), "LOOP_ARTIFACT loop-pre-success eA==",
                preLines.getFirst().replace("LOOP_ARTIFACT", "ARTIFACT"))) {
                var changed = new ArrayList<>(preLines); changed.set(0, bad); Files.write(pre, changed);
                rejected(directory); negatives++;
            }
            Files.write(pre, preLines.subList(1, preLines.size())); rejected(directory); negatives++;
            var extra = new ArrayList<>(preLines); extra.add(preLines.getFirst()); Files.write(pre, extra); rejected(directory); negatives++;
            Files.writeString(pre, ""); rejected(directory); negatives++;
            Files.write(pre, new byte[1_048_577]); rejected(directory); negatives++;
            Files.delete(pre); rejected(directory); negatives++;
            System.out.println("PASS loop fixture batches: valid 68-case delivery and " + negatives + " rejection cases");
        } finally { Files.deleteIfExists(post); Files.deleteIfExists(pre); Files.delete(directory); }
    }
    private static List<String> lines(String form) {
        return LoopRecordingTest.VARIANTS.stream().map(v -> "LOOP_ARTIFACT loop-" + form + "-" + v + " eA==".repeat(5)).toList();
    }
    private static void rejected(Path directory) throws Exception {
        boolean rejected = false;
        try { LoopRecordingTest.loadBundles(directory); } catch (AssertionError | java.io.IOException expected) { rejected = true; }
        check(rejected, "Invalid loop batch accepted");
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
