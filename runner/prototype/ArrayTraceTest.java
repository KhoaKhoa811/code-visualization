import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stream-fault tests require no Docker and never run submitted code. */
public final class ArrayTraceTest {
    static final String SOURCE = "{\"file\":\"Main.java\",\"start\":{\"line\":3,\"column\":9},\"end\":{\"line\":3,\"column\":31}}";
    static final Map<String, String> MAP = Map.of("ARRAY_DECLARE", SOURCE, "ARRAY_READ", SOURCE, "ARRAY_WRITE", SOURCE);
    static final String DECLARE = event(1, "ARRAY_DECLARE", "\"variableId\":\"variable-1\",\"variableName\":\"values\",\"values\":[" + value(3) + "," + value(1) + "]");
    static final String READ = event(2, "ARRAY_READ", "\"index\":1,\"value\":" + value(1));
    static final String WRITE = event(3, "ARRAY_WRITE", "\"index\":0,\"value\":" + value(1));
    static final String END = "{\"transport\":\"end\"}\n";
    public static void main(String[] args) {
        ArrayTrace good = new ArrayTrace(MAP);
        byte[] bytes = (DECLARE + READ + WRITE + END).getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) good.accept(new byte[]{b}, 1);
        good.finish();
        check(good.seal().complete(), "Chunked complete records rejected");
        int rejected = 0;
        for (String bad : List.of("{bad}\n", READ.substring(0, READ.length() - 1),
                READ.replace("\"sequence\":2", "\"sequence\":4"), READ.replace("\"value\":1", "\"value\":7"),
                READ.replace("\"index\":1", "\"index\":5"), READ.replace("array-1", "unknown"),
                READ.replace("\"line\":3", "\"line\":9"), READ.replace("\"value\":1", "\"value\":01"),
                READ.replace("\"value\":1", "\"value\":2147483648"), DECLARE)) {
            ArrayTrace trace = new ArrayTrace(MAP);
            send(trace, DECLARE + bad);
            trace.finish();
            send(trace, READ + WRITE + END); // Never resume beyond a corrupt/incomplete record.
            ArrayTrace.Snapshot result = trace.seal();
            check(result.events().size() == 1 && !result.complete(), "Invalid stream accepted: " + bad);
            rejected++;
        }
        ArrayTrace missing = new ArrayTrace(MAP);
        send(missing, DECLARE + READ); missing.finish();
        check(!missing.seal().complete(), "Missing completion accepted");
        ArrayTrace byteLimit = new ArrayTrace(MAP);
        send(byteLimit, DECLARE + "x".repeat(ArrayTrace.BYTE_LIMIT));
        check(byteLimit.seal().bytes() == ArrayTrace.BYTE_LIMIT && byteLimit.eventCount() == 1, "Byte cap failed");
        ArrayTrace eventLimit = new ArrayTrace(MAP);
        send(eventLimit, DECLARE);
        for (int i = 2; i <= 33; i++) send(eventLimit, event(i, "ARRAY_READ", "\"index\":1,\"value\":" + value(1)));
        check(eventLimit.seal().events().size() == 32, "Event cap failed");
        ArrayTrace arrayLimit = new ArrayTrace(MAP);
        send(arrayLimit, DECLARE.replace(value(3) + "," + value(1), String.join(",", Collections.nCopies(17, value(0)))));
        check(arrayLimit.seal().events().isEmpty(), "Array cap failed");
        for (String contents : List.of("", value(Integer.MIN_VALUE) + "," + value(Integer.MAX_VALUE))) {
            ArrayTrace edge = new ArrayTrace(MAP);
            send(edge, DECLARE.replace(value(3) + "," + value(1), contents) + END); edge.finish();
            check(edge.seal().complete(), "Empty array/int bounds rejected");
        }
        String scalarDeclaration = "{\"sequence\":1,\"kind\":\"VARIABLE_DECLARE\",\"source\":" + SOURCE
            + ",\"variableId\":\"variable-1\",\"variableName\":\"x\",\"value\":" + value(5) + "}\n";
        String scalarWrite = "{\"sequence\":2,\"kind\":\"VARIABLE_WRITE\",\"source\":" + SOURCE
            + ",\"variableId\":\"variable-1\",\"value\":" + value(8) + "}\n";
        Map<String, String> scalarMap = Map.of("VARIABLE_DECLARE", SOURCE, "VARIABLE_WRITE", SOURCE);
        ArrayTrace scalarGood = new ArrayTrace(scalarMap, "x", true);
        send(scalarGood, scalarDeclaration + scalarWrite + END); scalarGood.finish();
        check(scalarGood.seal().complete(), "Scalar stream rejected");
        for (String bad : List.of(scalarWrite.replace("variable-1", "unknown"), scalarWrite.replace("\"value\":8", "\"value\":2147483648"),
            scalarWrite.replace("\"value\":8", "\"value\":08"), scalarDeclaration.replace("\"sequence\":1", "\"sequence\":2"),
            scalarWrite.replace("\"sequence\":2", "\"sequence\":4"))) {
            ArrayTrace scalarBad = new ArrayTrace(scalarMap, "x", true);
            send(scalarBad, scalarDeclaration + bad + scalarWrite + END); scalarBad.finish();
            check(scalarBad.seal().events().size() == 1 && !scalarBad.complete(), "Scalar invalid prefix accepted");
        }
        ArrayTrace scalarBefore = new ArrayTrace(scalarMap, "x", true);
        send(scalarBefore, scalarWrite.replace("\"sequence\":2", "\"sequence\":1"));
        check(scalarBefore.eventCount() == 0, "Scalar write before declaration accepted");
        ArrayTrace wrongKind = new ArrayTrace(MAP);
        send(wrongKind, scalarDeclaration); check(wrongKind.eventCount() == 0, "Scalar accepted on array collector");
        Map<String, String> mixedMap = Map.of("VARIABLE_DECLARE", SOURCE, "ARRAY_DECLARE", SOURCE, "ARRAY_WRITE", SOURCE);
        String mixedArray = DECLARE.replace("\"sequence\":1", "\"sequence\":2").replace("variable-1", "variable-2");
        String mixedWrite = WRITE.replace("\"value\":1", "\"value\":5");
        ArrayTrace mixedGood = new ArrayTrace(mixedMap, "x", "values");
        for (byte b : (scalarDeclaration + mixedArray + mixedWrite + END).getBytes(StandardCharsets.UTF_8)) mixedGood.accept(new byte[]{b}, 1);
        mixedGood.finish(); check(mixedGood.seal().complete(), "Combined chunked stream rejected");
        List<String> badArrays = List.of(mixedArray.replace("variable-2", "variable-1"), mixedArray.replace("values", "other"),
            mixedArray.replace("array-1", "array-2"), mixedArray.replace("\"line\":3", "\"line\":4"),
            mixedArray.replace("\"sequence\":2", "\"sequence\":3"), mixedArray.replace("\"value\":3", "\"value\":2147483648"),
            scalarDeclaration.replace("\"sequence\":1", "\"sequence\":2"), END);
        for (String bad : badArrays) {
            ArrayTrace trace = new ArrayTrace(mixedMap, "x", "values");
            send(trace, scalarDeclaration + bad + mixedArray + mixedWrite + END); trace.finish();
            check(trace.seal().events().size() == 1 && !trace.complete(), "Combined invalid declaration resumed");
        }
        List<String> badWrites = List.of(mixedWrite.replace("array-1", "array-2"), mixedWrite.replace("ARRAY_WRITE", "VARIABLE_WRITE"),
            mixedWrite.replace("\"value\":5", "\"value\":2147483648"), mixedWrite.replace("\"value\":5", "\"value\":3"),
            mixedWrite.replace("\"sequence\":3", "\"sequence\":4"), mixedWrite.replace("\"line\":3", "\"line\":4"),
            mixedArray.replace("\"sequence\":2", "\"sequence\":3"));
        for (String bad : badWrites) {
            ArrayTrace trace = new ArrayTrace(mixedMap, "x", "values");
            send(trace, scalarDeclaration + mixedArray + bad + mixedWrite + END); trace.finish();
            check(trace.seal().events().size() == 2 && !trace.complete(), "Combined invalid write resumed");
        }
        System.out.println("PASS combined collector: chunked success and 15 corrupt prefixes; distinct names/identities, order, types, source and stored scalar value");
        Map<String, String> updateMap = Map.of("VARIABLE_DECLARE", SOURCE, "ARRAY_DECLARE", SOURCE, "VARIABLE_WRITE", SOURCE, "ARRAY_WRITE", SOURCE);
        String updateScalar = scalarWrite.replace("\"sequence\":2", "\"sequence\":3");
        String updateArray = mixedWrite.replace("\"sequence\":3", "\"sequence\":4").replace("\"value\":5", "\"value\":8");
        ArrayTrace updateGood = new ArrayTrace(updateMap, "x", "values");
        for (byte b : (scalarDeclaration + mixedArray + updateScalar + updateArray + END).getBytes(StandardCharsets.UTF_8)) updateGood.accept(new byte[]{b}, 1);
        updateGood.finish(); check(updateGood.seal().complete(), "Scalar-update stream rejected");
        for (String bad : List.of(updateScalar.replace("variable-1", "variable-2"), updateScalar.replace("variable-1", "unknown"),
            updateScalar.replace("VARIABLE_WRITE", "ARRAY_WRITE"), updateScalar.replace("\"line\":3", "\"line\":4"),
            updateScalar.replace("\"value\":8", "\"value\":2147483648"), updateScalar.replace("\"sequence\":3", "\"sequence\":4"),
            scalarDeclaration.replace("\"sequence\":1", "\"sequence\":3"), mixedWrite, END)) {
            ArrayTrace trace = new ArrayTrace(updateMap, "x", "values");
            send(trace, scalarDeclaration + mixedArray + bad + updateScalar + updateArray + END); trace.finish();
            check(trace.seal().events().size() == 2 && !trace.complete(), "Invalid scalar update resumed");
        }
        for (String bad : List.of(updateArray.replace("\"value\":8", "\"value\":5"), updateArray.replace("array-1", "array-2"),
            updateArray.replace("\"line\":3", "\"line\":4"), updateArray.replace("\"sequence\":4", "\"sequence\":5"),
            updateScalar.replace("\"sequence\":3", "\"sequence\":4"), END)) {
            ArrayTrace trace = new ArrayTrace(updateMap, "x", "values");
            send(trace, scalarDeclaration + mixedArray + updateScalar + bad + updateArray + END); trace.finish();
            check(trace.seal().events().size() == 3 && !trace.complete(), "Stale scalar or invalid final store accepted");
        }
        for (String bad : badArrays) {
            ArrayTrace trace = new ArrayTrace(updateMap, "x", "values");
            send(trace, scalarDeclaration + bad + mixedArray + updateScalar + updateArray + END); trace.finish();
            check(trace.seal().events().size() == 1 && !trace.complete(), "Update declaration corruption resumed");
        }
        System.out.println("PASS scalar-update collector: chunked success and 23 corrupt prefixes, including stale scalar value and premature completion");
        String indexSource = SOURCE.replace("\"line\":3", "\"line\":4");
        var indexPlan = List.of(new ArrayTrace.Operation("VARIABLE_DECLARE", SOURCE, "variable-1", "x"),
            new ArrayTrace.Operation("ARRAY_DECLARE", SOURCE, "variable-2", "values"),
            new ArrayTrace.Operation("VARIABLE_DECLARE", indexSource, "variable-3", "i"),
            new ArrayTrace.Operation("ARRAY_WRITE", SOURCE, null, null));
        String indexDeclare = scalarDeclaration.replace("\"sequence\":1", "\"sequence\":3").replace("variable-1", "variable-3")
            .replace("\"x\"", "\"i\"").replace("\"value\":5", "\"value\":1").replace(SOURCE, indexSource);
        String indexWrite = mixedWrite.replace("\"sequence\":3", "\"sequence\":4").replace("\"index\":0", "\"index\":1");
        ArrayTrace indexGood = new ArrayTrace(indexPlan);
        for (byte b : (scalarDeclaration + mixedArray + indexDeclare + indexWrite + END).getBytes(StandardCharsets.UTF_8)) indexGood.accept(new byte[]{b}, 1);
        indexGood.finish(); check(indexGood.seal().complete(), "Index stream rejected");
        for (String bad : List.of(indexDeclare.replace("variable-3", "variable-1"), indexDeclare.replace("variable-3", "variable-2"),
            indexDeclare.replace("\"i\"", "\"x\""), indexDeclare.replace(indexSource, SOURCE), indexDeclare.replace("VARIABLE_DECLARE", "VARIABLE_WRITE"),
            indexDeclare.replace("\"value\":1", "\"value\":2147483648"), indexDeclare.replace("\"sequence\":3", "\"sequence\":4"), indexWrite, END)) {
            ArrayTrace trace = new ArrayTrace(indexPlan);
            send(trace, scalarDeclaration + mixedArray + bad + indexDeclare + indexWrite + END); trace.finish();
            check(trace.seal().events().size() == 2 && !trace.complete(), "Invalid index declaration resumed");
        }
        for (String bad : List.of(indexWrite.replace("\"index\":1", "\"index\":0"), indexWrite.replace("\"value\":5", "\"value\":1"),
            indexWrite.replace("array-1", "unknown"), indexWrite.replace(SOURCE, indexSource), indexWrite.replace("\"sequence\":4", "\"sequence\":5"),
            indexDeclare.replace("\"sequence\":3", "\"sequence\":4"), END)) {
            ArrayTrace trace = new ArrayTrace(indexPlan);
            send(trace, scalarDeclaration + mixedArray + indexDeclare + bad + indexWrite + END); trace.finish();
            check(trace.seal().events().size() == 3 && !trace.complete(), "Invalid index/value store resumed");
        }
        for (String bad : badArrays) {
            ArrayTrace trace = new ArrayTrace(indexPlan);
            send(trace, scalarDeclaration + bad + mixedArray + indexDeclare + indexWrite + END); trace.finish();
            check(trace.seal().events().size() == 1 && !trace.complete(), "Index array corruption resumed");
        }
        ArrayTrace swappedIndex = new ArrayTrace(indexPlan);
        send(swappedIndex, scalarDeclaration.replace(SOURCE, indexSource) + mixedArray + indexDeclare + indexWrite + END);
        check(swappedIndex.eventCount() == 0, "Swapped first declaration source accepted");
        System.out.println("PASS variable-index collector: chunked success and 25 corrupt prefixes; independent index/value bindings and declaration sites");
        String indexUpdateSource = SOURCE.replace("\"line\":3", "\"line\":5");
        var indexUpdatePlan = new ArrayList<>(indexPlan);
        indexUpdatePlan.add(3, new ArrayTrace.Operation("VARIABLE_WRITE", indexUpdateSource, "variable-3", "i"));
        String oldIndex = indexDeclare.replace("\"value\":1", "\"value\":0");
        String indexUpdate = scalarWrite.replace("\"sequence\":2", "\"sequence\":4").replace("variable-1", "variable-3")
            .replace("\"value\":8", "\"value\":1").replace(SOURCE, indexUpdateSource);
        String finalIndexStore = indexWrite.replace("\"sequence\":4", "\"sequence\":5");
        ArrayTrace indexUpdateGood = new ArrayTrace(indexUpdatePlan);
        for (byte b : (scalarDeclaration + mixedArray + oldIndex + indexUpdate + finalIndexStore + END).getBytes(StandardCharsets.UTF_8)) indexUpdateGood.accept(new byte[]{b}, 1);
        indexUpdateGood.finish(); check(indexUpdateGood.seal().complete(), "Index-update chunked stream rejected");
        int updateRejected = 0;
        for (String bad : List.of(indexUpdate.replace("variable-3", "variable-1"), indexUpdate.replace("variable-3", "variable-2"),
            indexUpdate.replace("variable-3", "unknown"), indexUpdate.replace(indexUpdateSource, indexSource), indexUpdate.replace(indexUpdateSource, SOURCE),
            indexUpdate.replace("\"value\":1", "\"value\":2147483648"), indexUpdate.replace("\"value\":1", "\"value\":01"),
            indexUpdate.replace("\"sequence\":4", "\"sequence\":5"), indexUpdate.replace("VARIABLE_WRITE", "VARIABLE_DECLARE"),
            oldIndex.replace("\"sequence\":3", "\"sequence\":4"), indexWrite, END)) {
            ArrayTrace trace = new ArrayTrace(indexUpdatePlan);
            send(trace, scalarDeclaration + mixedArray + oldIndex + bad + indexUpdate + finalIndexStore + END); trace.finish();
            check(trace.eventCount() == 3 && !trace.complete(), "Invalid index update resumed"); updateRejected++;
        }
        for (String bad : List.of(finalIndexStore.replace("\"index\":1", "\"index\":0"), finalIndexStore.replace("\"value\":5", "\"value\":1"),
            finalIndexStore.replace("array-1", "unknown"), finalIndexStore.replace(SOURCE, indexUpdateSource),
            finalIndexStore.replace("\"sequence\":5", "\"sequence\":6"), indexUpdate.replace("\"sequence\":4", "\"sequence\":5"), END)) {
            ArrayTrace trace = new ArrayTrace(indexUpdatePlan);
            send(trace, scalarDeclaration + mixedArray + oldIndex + indexUpdate + bad + finalIndexStore + END); trace.finish();
            check(trace.eventCount() == 4 && !trace.complete(), "Stale index or invalid final store resumed"); updateRejected++;
        }
        for (String bad : List.of(oldIndex.replace("variable-3", "variable-1"), oldIndex.replace(indexSource, indexUpdateSource),
            oldIndex.replace("\"i\"", "\"x\""), oldIndex.replace("VARIABLE_DECLARE", "VARIABLE_WRITE"), END)) {
            ArrayTrace trace = new ArrayTrace(indexUpdatePlan);
            send(trace, scalarDeclaration + mixedArray + bad + oldIndex + indexUpdate + finalIndexStore + END); trace.finish();
            check(trace.eventCount() == 2 && !trace.complete(), "Missing/wrong index declaration resumed"); updateRejected++;
        }
        for (String bad : badArrays) {
            ArrayTrace trace = new ArrayTrace(indexUpdatePlan);
            send(trace, scalarDeclaration + bad + mixedArray + oldIndex + indexUpdate + finalIndexStore + END); trace.finish();
            check(trace.eventCount() == 1 && !trace.complete(), "Index-update array corruption resumed"); updateRejected++;
        }
        var wrongPlan = new ArrayList<>(indexUpdatePlan);
        wrongPlan.set(3, new ArrayTrace.Operation("VARIABLE_WRITE", indexUpdateSource, "variable-1", "x"));
        boolean planRejected = false;
        try { new ArrayTrace(wrongPlan); } catch (IllegalArgumentException expected) { planRejected = true; }
        check(planRejected, "Wrong update target in trusted operation plan accepted");
        System.out.println("PASS index-update collector: chunked success, " + updateRejected + " corrupt prefixes and wrong-plan rejection; latest index and independent value");
        for (int wrapped : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, 0}) {
            ArrayTrace trace = new ArrayTrace(indexUpdatePlan);
            String prior = oldIndex.replace("\"value\":0", "\"value\":" + Integer.MIN_VALUE);
            String captured = indexUpdate.replace("\"value\":1", "\"value\":" + wrapped);
            send(trace, scalarDeclaration + mixedArray + prior + captured
                + (wrapped == 0 ? finalIndexStore.replace("\"index\":1", "\"index\":0") + END : ""));
            trace.finish();
            check(trace.eventCount() == (wrapped == 0 ? 5 : 4) && trace.complete() == (wrapped == 0), "Wrapped int capture or safe prefix rejected");
        }
        System.out.println("PASS addition collector: wrapped int extremes preserve four-event prefixes; wrapped zero permits a store");
        System.out.println("PASS trace stream: chunking, " + rejected + " array corrupt-prefix cases, scalar success/7 rejection cases, missing end, byte/event/array caps, empty array/int bounds");
    }
    static String value(int value) { return "{\"type\":\"int\",\"value\":" + value + "}"; }
    static String event(int sequence, String kind, String payload) {
        return "{\"sequence\":" + sequence + ",\"kind\":\"" + kind + "\",\"source\":" + SOURCE
            + ",\"arrayId\":\"array-1\"," + payload + "}\n";
    }
    static void send(ArrayTrace trace, String records) {
        byte[] bytes = records.getBytes(StandardCharsets.UTF_8); trace.accept(bytes, bytes.length);
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
