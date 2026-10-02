import java.util.*;
import java.util.regex.*;

/** Phase-aware validation of captured loop facts. Commits state only after an entire valid record. */
final class LoopTracePlan {
    private static final Pattern INT = Pattern.compile("\\{\"type\":\"int\",\"value\":(-?[0-9]{1,10})\\}");
    private final List<ArrayTrace.Operation> sites;
    private int phase;
    private Integer scalar, index;
    private int[] values;
    private final boolean reading;
    private final int addend, conditionPhase, arrayPhase, indexPhase;
    private final String indexId, arrayBindingId;
    private Integer pendingRead;
    LoopTracePlan(List<ArrayTrace.Operation> sites) {
        this(sites, null);
    }
    LoopTracePlan(List<ArrayTrace.Operation> sites, Integer addend) {
        this.sites = List.copyOf(sites);
        reading = addend != null; this.addend = reading ? addend : 0;
        arrayPhase = reading ? 0 : 1; indexPhase = reading ? 1 : 2; conditionPhase = reading ? 2 : 3;
        indexId = reading ? "variable-2" : "variable-3"; arrayBindingId = reading ? "variable-1" : "variable-2";
        var kinds = reading ? List.of("ARRAY_DECLARE", "VARIABLE_DECLARE", "CONDITION", "ARRAY_READ", "ARRAY_WRITE", "VARIABLE_WRITE")
            : List.of("VARIABLE_DECLARE", "ARRAY_DECLARE", "VARIABLE_DECLARE", "CONDITION", "ARRAY_WRITE", "VARIABLE_WRITE");
        if (!sites.stream().map(ArrayTrace.Operation::kind).toList().equals(kinds)
            || (!reading && !"variable-1".equals(sites.get(0).variableId())) || !arrayBindingId.equals(sites.get(arrayPhase).variableId())
            || !indexId.equals(sites.get(indexPhase).variableId()) || !indexId.equals(sites.get(5).variableId())
            || sites.stream().anyMatch(s -> s.source() == null)) throw new IllegalArgumentException("Invalid loop operation plan");
        for (int n = 0; n < conditionPhase; n++) Objects.requireNonNull(sites.get(n).variableName());
    }
    boolean complete() { return phase == 6; }
    void accept(String record, int sequence) {
        if (complete()) throw new IllegalArgumentException("Event after loop exit");
        var site = sites.get(phase);
        String prefix = "{\"sequence\":" + sequence + ",\"kind\":" + ArrayTrace.quote(site.kind()) + ",\"source\":" + site.source() + ",";
        if ((!reading && phase == 0) || phase == indexPhase) {
            prefix += "\"variableId\":" + ArrayTrace.quote(site.variableId()) + ",\"variableName\":" + ArrayTrace.quote(site.variableName()) + ",\"value\":";
            String suffix = ",\"scopeId\":\"" + (phase == indexPhase ? "scope-loop-1" : "scope-main") + "\",\"exitedVariableIds\":[]}";
            int actual = integerValue(extract(record, prefix, suffix));
            if (phase == indexPhase) index = actual; else scalar = actual;
            phase++; return;
        }
        if (phase == arrayPhase) {
            prefix += "\"arrayId\":\"array-1\",\"variableId\":" + ArrayTrace.quote(arrayBindingId) + ",\"variableName\":" + ArrayTrace.quote(site.variableName()) + ",\"values\":[";
            String list = extract(record, prefix, "],\"scopeId\":\"scope-main\",\"exitedVariableIds\":[]}");
            List<Integer> parsed = new ArrayList<>();
            for (int offset = 0; offset < list.length();) {
                var matcher = INT.matcher(list); matcher.region(offset, list.length());
                if (!matcher.lookingAt() || parsed.size() == ArrayTrace.ARRAY_LIMIT) throw new IllegalArgumentException("Invalid loop array values");
                parsed.add(integerValue(matcher.group())); offset = matcher.end();
                if (offset < list.length() && (list.charAt(offset++) != ',' || offset == list.length())) throw new IllegalArgumentException("Invalid loop array separator");
            }
            values = parsed.stream().mapToInt(Integer::intValue).toArray(); phase++; return;
        }
        if (phase == conditionPhase) {
            boolean actual = index < values.length;
            require(record.equals(prefix + "\"scopeId\":\"scope-loop-1\",\"value\":{\"type\":\"boolean\",\"value\":" + actual
                + "},\"exitedVariableIds\":" + (actual ? "[]" : "[" + ArrayTrace.quote(indexId) + "]") + "}"), "Condition/result/retirement disagrees with captured state");
            if (actual) phase++; else { phase = 6; index = null; }
        } else if (reading && phase == 3) {
            require(index >= 0 && index < values.length, "Invalid successful read index");
            int captured = values[index];
            require(record.equals(prefix + "\"arrayId\":\"array-1\",\"index\":" + index + ",\"value\":" + value(captured) + ",\"exitedVariableIds\":[]}"), "Read disagrees with captured array/index");
            pendingRead = captured; phase = 4;
        } else if (phase == 4) {
            require(index >= 0 && index < values.length, "Invalid successful store index");
            int expected = reading ? Objects.requireNonNull(pendingRead) + addend : scalar;
            require(record.equals(prefix + "\"arrayId\":\"array-1\",\"index\":" + index + ",\"value\":" + value(expected) + ",\"exitedVariableIds\":[]}"), "Store disagrees with captured bindings/read");
            values[index] = expected; pendingRead = null; phase = 5;
        } else {
            int updated = index + 1;
            require(record.equals(prefix + "\"variableId\":" + ArrayTrace.quote(indexId) + ",\"value\":" + value(updated) + ",\"exitedVariableIds\":[]}"), "Update disagrees with captured index");
            index = updated; phase = conditionPhase;
        }
    }
    private static String extract(String record, String prefix, String suffix) {
        require(record.startsWith(prefix) && record.endsWith(suffix) && record.length() >= prefix.length() + suffix.length(), "Malformed loop event/source/order");
        return record.substring(prefix.length(), record.length() - suffix.length());
    }
    private static int integerValue(String payload) {
        var matcher = INT.matcher(payload);
        require(matcher.matches(), "Invalid loop integer");
        int parsed = Integer.parseInt(matcher.group(1));
        require(value(parsed).equals(payload), "Noncanonical loop integer");
        return parsed;
    }
    private static String value(int value) { return "{\"type\":\"int\",\"value\":" + value + "}"; }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalArgumentException(message); }
}
