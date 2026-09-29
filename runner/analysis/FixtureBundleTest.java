import java.nio.file.*;
import java.util.*;

/** Tests trusted artifact delivery only; never compiles or executes fixture source. */
public final class FixtureBundleTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of(args[0]), "bundle-check-");
        Path first = directory.resolve("batch-0.txt"), second = directory.resolve("batch-1.txt");
        String a = "ARTIFACT a" + " eA==".repeat(9) + "\n", b = a.replace("ARTIFACT a", "ARTIFACT b");
        try {
            Files.writeString(first, a); Files.writeString(second, b);
            check(AutomaticRecordingTest.loadBundles(directory, Set.of("a", "b")).size() == 2, "valid batches");
            for (String bad : List.of(a, b + b, b.replace("ARTIFACT b", "ARTIFACT unknown"), "", "ARTIFACT b eA==\n")) {
                Files.writeString(second, bad); rejected(directory, Set.of("a", "b"));
            }
            Files.writeString(second, b); rejected(directory, Set.of("a", "b", "missing"));
            Files.write(second, new byte[1_048_577]); rejected(directory, Set.of("a", "b"));
            Files.delete(second); rejected(directory, Set.of("a", "b"));
            System.out.println("PASS fixture batches: valid delivery and 8 negative cases (duplicate, unknown, empty, malformed, missing, oversized)");
        } finally { Files.deleteIfExists(first); Files.deleteIfExists(second); Files.delete(directory); }
    }
    static void rejected(Path directory, Set<String> expected) throws Exception {
        boolean rejected = false;
        try { AutomaticRecordingTest.loadBundles(directory, expected); }
        catch (AssertionError | java.io.IOException failure) { rejected = true; }
        check(rejected, "Invalid batches accepted");
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
