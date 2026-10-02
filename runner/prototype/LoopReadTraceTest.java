import java.nio.charset.StandardCharsets;
import java.util.*;

/** Malformed read/write order must never commit a partial fact. No submitted Java runs here. */
public final class LoopReadTraceTest {
    private static final String RANGE = "{\"file\":\"Main.java\",\"start\":{\"line\":1,\"column\":1},\"end\":{\"line\":1,\"column\":2}}";
    private static final List<ArrayTrace.Operation> PLAN = List.of(
        new ArrayTrace.Operation("ARRAY_DECLARE", RANGE, "variable-1", "values"), new ArrayTrace.Operation("VARIABLE_DECLARE", RANGE, "variable-2", "i"),
        new ArrayTrace.Operation("CONDITION", RANGE, null, null), new ArrayTrace.Operation("ARRAY_READ", RANGE, null, null),
        new ArrayTrace.Operation("ARRAY_WRITE", RANGE, null, null), new ArrayTrace.Operation("VARIABLE_WRITE", RANGE, "variable-2", "i"));
    public static void main(String[] args) {
        var events = events();
        var complete = trace(); feedPrefix(complete, events, 15); feed(complete, "{\"transport\":\"end\"}\n"); complete.finish();
        check(complete.seal().complete(), "15-step read loop");
        int rejected = 0;
        for (int n = 0; n < events.size(); n++) {
            String valid = events.get(n);
            for (String bad : List.of(valid.replace("\"sequence\":" + (n + 1), "\"sequence\":99"), valid.replace(RANGE, RANGE.replace("Main.java", "Other.java")),
                valid.replace("\"exitedVariableIds\":", "\"wrongExits\":"))) { reject(events, n, bad); rejected++; }
        }
        for (var mutation : List.of(
            new Mutation(3, events.get(3).replace("\"value\":5", "\"value\":6")), new Mutation(3, events.get(3).replace("\"index\":0", "\"index\":1")),
            new Mutation(3, events.get(3).replace("array-1", "array-2")), new Mutation(3, events.get(4).replace("\"sequence\":5", "\"sequence\":4")),
            new Mutation(4, events.get(3).replace("\"sequence\":4", "\"sequence\":5")), new Mutation(4, events.get(4).replace("\"value\":6", "\"value\":5")),
            new Mutation(8, events.get(8).replace("\"value\":3", "\"value\":6")), new Mutation(7, events.get(7).replace("\"value\":2", "\"value\":5")),
            new Mutation(2, events.get(2).replace("true", "false")), new Mutation(2, events.get(2).replace("[]", "[\"variable-2\"]")),
            new Mutation(14, events.get(14).replace("[\"variable-2\"]", "[]")), new Mutation(14, events.get(14).replace("variable-2", "variable-1")),
            new Mutation(5, events.get(5).replace("\"value\":1", "\"value\":0")), new Mutation(3, events.get(3).replace("[]", "[\"variable-2\"]")))) {
            reject(events, mutation.index(), mutation.record()); rejected++;
        }
        for (int prefix = 0; prefix < 15; prefix++) {
            var early = trace(); feedPrefix(early, events, prefix); feed(early, "{\"transport\":\"end\"}\n"); early.finish();
            check(!early.seal().complete() && early.seal().events().size() == prefix && early.seal().problem().contains("Premature"), "early end " + prefix);
        }
        int truncations = 0;
        for (int at : List.of(3, 14)) {
            String last = events.get(at) + "\n";
            for (int bytes = 1; bytes < last.length(); bytes++) {
                var truncated = trace(); feedPrefix(truncated, events, at); feed(truncated, last.substring(0, bytes)); truncated.finish();
                check(truncated.seal().events().size() == at && !truncated.complete(), "atomic read/retirement truncation"); truncations++;
            }
        }
        var after = trace(); feedPrefix(after, events, 15); feed(after, events.get(5).replace("\"sequence\":6", "\"sequence\":16") + "\n");
        check(!after.complete() && after.seal().events().size() == 15, "no update after retirement");
        var byteLimit = trace(); feedPrefix(byteLimit, events, 4); feed(byteLimit, "x".repeat(ArrayTrace.BYTE_LIMIT + 1));
        check(byteLimit.seal().events().size() == 4 && byteLimit.seal().problem().equals("Trace byte limit"), "byte cap after read");
        System.out.println("PASS loop-read collector: 15 steps, " + rejected + " corrupt records, 15 premature ends, atomic retries, byte cap and " + truncations + " truncations");
    }
    record Mutation(int index, String record) {}
    private static ArrayTrace trace() { return new ArrayTrace(new LoopTracePlan(PLAN, 1)); }
    private static void reject(List<String> events, int prefix, String bad) {
        var trace = trace(); feedPrefix(trace, events, prefix); feed(trace, bad + "\n"); feed(trace, events.get(prefix) + "\n"); trace.finish();
        check(!trace.seal().complete() && trace.seal().events().size() == prefix && !trace.seal().problem().isEmpty(), "bad record accepted");
        var plan = new LoopTracePlan(PLAN, 1); for (int n = 0; n < prefix; n++) plan.accept(events.get(n), n + 1);
        boolean rejected = false; try { plan.accept(bad, prefix + 1); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "plan must reject"); plan.accept(events.get(prefix), prefix + 1);
    }
    private static List<String> events() {
        List<String> events = new ArrayList<>(); int[] initial = {5, 2, 7};
        events.add(event(1, "ARRAY_DECLARE", "\"arrayId\":\"array-1\",\"variableId\":\"variable-1\",\"variableName\":\"values\",\"values\":[" + value(5) + "," + value(2) + "," + value(7) + "],\"scopeId\":\"scope-main\",\"exitedVariableIds\":[]"));
        events.add(event(2, "VARIABLE_DECLARE", "\"variableId\":\"variable-2\",\"variableName\":\"i\",\"value\":" + value(0) + ",\"scopeId\":\"scope-loop-1\",\"exitedVariableIds\":[]"));
        for (int n = 0; n < 3; n++) {
            events.add(event(3 + 4*n, "CONDITION", "\"scopeId\":\"scope-loop-1\",\"value\":{\"type\":\"boolean\",\"value\":true},\"exitedVariableIds\":[]"));
            events.add(event(4 + 4*n, "ARRAY_READ", "\"arrayId\":\"array-1\",\"index\":" + n + ",\"value\":" + value(initial[n]) + ",\"exitedVariableIds\":[]"));
            events.add(event(5 + 4*n, "ARRAY_WRITE", "\"arrayId\":\"array-1\",\"index\":" + n + ",\"value\":" + value(initial[n]+1) + ",\"exitedVariableIds\":[]"));
            events.add(event(6 + 4*n, "VARIABLE_WRITE", "\"variableId\":\"variable-2\",\"value\":" + value(n+1) + ",\"exitedVariableIds\":[]"));
        }
        events.add(event(15, "CONDITION", "\"scopeId\":\"scope-loop-1\",\"value\":{\"type\":\"boolean\",\"value\":false},\"exitedVariableIds\":[\"variable-2\"]"));
        return events;
    }
    private static String event(int seq, String kind, String payload) { return "{\"sequence\":" + seq + ",\"kind\":\"" + kind + "\",\"source\":" + RANGE + "," + payload + "}"; }
    private static String value(int n) { return "{\"type\":\"int\",\"value\":" + n + "}"; }
    private static void feedPrefix(ArrayTrace trace, List<String> events, int prefix) { for (int n = 0; n < prefix; n++) feed(trace, events.get(n) + "\n"); }
    private static void feed(ArrayTrace trace, String input) { var bytes = input.getBytes(StandardCharsets.UTF_8); trace.accept(bytes, bytes.length); }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
