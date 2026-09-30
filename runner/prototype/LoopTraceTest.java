import java.nio.charset.StandardCharsets;
import java.util.*;

/** Plan/order corruption and atomic-prefix tests without executing submitted code. */
public final class LoopTraceTest {
    private static final String RANGE = "{\"file\":\"Main.java\",\"start\":{\"line\":1,\"column\":1},\"end\":{\"line\":1,\"column\":2}}";
    private static final List<ArrayTrace.Operation> PLAN = List.of(
        new ArrayTrace.Operation("VARIABLE_DECLARE", RANGE, "variable-1", "x"), new ArrayTrace.Operation("ARRAY_DECLARE", RANGE, "variable-2", "values"),
        new ArrayTrace.Operation("VARIABLE_DECLARE", RANGE, "variable-3", "i"), new ArrayTrace.Operation("CONDITION", RANGE, null, null),
        new ArrayTrace.Operation("ARRAY_WRITE", RANGE, null, null), new ArrayTrace.Operation("VARIABLE_WRITE", RANGE, "variable-3", "i"));
    public static void main(String[] args) {
        var events = events();
        var complete = new ArrayTrace(new LoopTracePlan(PLAN)); feed(complete, String.join("\n", events) + "\n{\"transport\":\"end\"}\n"); complete.finish();
        check(complete.seal().complete(), "complete loop");
        int rejected = 0;
        for (int n = 0; n < events.size(); n++) {
            String valid = events.get(n);
            for (String bad : List.of(valid.replace("\"sequence\":" + (n + 1), "\"sequence\":99"), valid.replace(RANGE, RANGE.replace("Main.java", "Other.java")),
                valid.replace("\"exitedVariableIds\":", "\"wrongExits\":"))) { reject(events, n, bad); rejected++; }
        }
        for (var mutation : List.of(
            new Mutation(3, events.get(3).replace("true", "false")), new Mutation(3, events.get(3).replace("[]", "[\"variable-3\"]")),
            new Mutation(12, events.get(12).replace("false", "true")), new Mutation(12, events.get(12).replace("[\"variable-3\"]", "[]")),
            new Mutation(12, events.get(12).replace("variable-3", "variable-1")), new Mutation(12, events.get(12).replace("[\"variable-3\"]", "[\"variable-3\",\"variable-3\"]")),
            new Mutation(4, events.get(4).replace("\"index\":0", "\"index\":1")), new Mutation(4, events.get(4).replace("\"value\":8", "\"value\":7")),
            new Mutation(5, events.get(5).replace("\"value\":1", "\"value\":0")), new Mutation(5, events.get(5).replace("variable-3", "variable-1")),
            new Mutation(3, events.get(3).replace("scope-loop-1", "scope-main")), new Mutation(2, events.get(2).replace("scope-loop-1", "scope-main")),
            new Mutation(3, events.get(0).replace("\"sequence\":1", "\"sequence\":4")), new Mutation(3, events.get(4).replace("\"sequence\":5", "\"sequence\":4")),
            new Mutation(4, events.get(5).replace("\"sequence\":6", "\"sequence\":5")), new Mutation(12, events.get(12).replace("false", "\"false\"")))) {
            reject(events, mutation.index(), mutation.record()); rejected++;
        }
        for (int prefix = 0; prefix < 13; prefix++) {
            var trace = new ArrayTrace(new LoopTracePlan(PLAN)); feedPrefix(trace, events, prefix); feed(trace, "{\"transport\":\"end\"}\n"); trace.finish();
            var snapshot = trace.seal(); check(!snapshot.complete() && snapshot.events().size() == prefix && snapshot.problem().contains("Premature"), "early end");
        }
        var after = new ArrayTrace(new LoopTracePlan(PLAN)); feedPrefix(after, events, 13); feed(after, events.get(5).replace("\"sequence\":6", "\"sequence\":14") + "\n");
        check(after.seal().events().size() == 13 && !after.complete(), "no writes after retirement");
        String last = events.getLast() + "\n";
        for (int bytes = 1; bytes < last.length(); bytes++) {
            var truncated = new ArrayTrace(new LoopTracePlan(PLAN)); feedPrefix(truncated, events, 12); feed(truncated, last.substring(0, bytes)); truncated.finish();
            check(truncated.seal().events().size() == 12 && !truncated.complete(), "false+retirement atomic at byte " + bytes);
        }
        var byteLimit = new ArrayTrace(new LoopTracePlan(PLAN)); feedPrefix(byteLimit, events, 12); feed(byteLimit, "x".repeat(ArrayTrace.BYTE_LIMIT + 1));
        var snapshot = byteLimit.seal(); check(snapshot.events().size() == 12 && snapshot.problem().equals("Trace byte limit"), "byte cap preserves prefix");
        System.out.println("PASS loop collector: 13 steps, " + rejected + " corrupt records, 13 premature ends, retirement, byte cap and " + (last.length() - 1) + " atomic truncations");
    }
    record Mutation(int index, String record) {}
    private static void reject(List<String> events, int prefix, String bad) {
        var trace = new ArrayTrace(new LoopTracePlan(PLAN)); feedPrefix(trace, events, prefix); feed(trace, bad + "\n"); feed(trace, events.get(prefix) + "\n"); trace.finish();
        var snapshot = trace.seal(); check(!snapshot.complete() && snapshot.events().size() == prefix && !snapshot.problem().isEmpty(), "corrupt record accepted " + bad);
        // Direct plan retry proves a rejected record does not mutate phase/bindings, even before sealing.
        var plan = new LoopTracePlan(PLAN); for (int n = 0; n < prefix; n++) plan.accept(events.get(n), n + 1);
        boolean rejected = false; try { plan.accept(bad, prefix + 1); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "plan must reject"); plan.accept(events.get(prefix), prefix + 1);
    }
    private static List<String> events() {
        List<String> events = new ArrayList<>();
        events.add(event(1, "VARIABLE_DECLARE", "\"variableId\":\"variable-1\",\"variableName\":\"x\",\"value\":" + value(8) + ",\"scopeId\":\"scope-main\",\"exitedVariableIds\":[]"));
        events.add(event(2, "ARRAY_DECLARE", "\"arrayId\":\"array-1\",\"variableId\":\"variable-2\",\"variableName\":\"values\",\"values\":[" + value(5) + "," + value(2) + "," + value(7) + "],\"scopeId\":\"scope-main\",\"exitedVariableIds\":[]"));
        events.add(event(3, "VARIABLE_DECLARE", "\"variableId\":\"variable-3\",\"variableName\":\"i\",\"value\":" + value(0) + ",\"scopeId\":\"scope-loop-1\",\"exitedVariableIds\":[]"));
        for (int n = 0; n < 3; n++) {
            events.add(event(4 + 3*n, "CONDITION", "\"scopeId\":\"scope-loop-1\",\"value\":{\"type\":\"boolean\",\"value\":true},\"exitedVariableIds\":[]"));
            events.add(event(5 + 3*n, "ARRAY_WRITE", "\"arrayId\":\"array-1\",\"index\":" + n + ",\"value\":" + value(8) + ",\"exitedVariableIds\":[]"));
            events.add(event(6 + 3*n, "VARIABLE_WRITE", "\"variableId\":\"variable-3\",\"value\":" + value(n+1) + ",\"exitedVariableIds\":[]"));
        }
        events.add(event(13, "CONDITION", "\"scopeId\":\"scope-loop-1\",\"value\":{\"type\":\"boolean\",\"value\":false},\"exitedVariableIds\":[\"variable-3\"]"));
        return events;
    }
    private static String event(int seq, String kind, String payload) { return "{\"sequence\":" + seq + ",\"kind\":\"" + kind + "\",\"source\":" + RANGE + "," + payload + "}"; }
    private static String value(int n) { return "{\"type\":\"int\",\"value\":" + n + "}"; }
    private static void feedPrefix(ArrayTrace trace, List<String> events, int prefix) { for (int n = 0; n < prefix; n++) feed(trace, events.get(n) + "\n"); }
    private static void feed(ArrayTrace trace, String input) { var bytes = input.getBytes(StandardCharsets.UTF_8); trace.accept(bytes, bytes.length); }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
