import java.util.*;
import java.util.regex.*;

/** Phase-aware validation of captured loop facts. Commits state only after an entire valid record. */
final class LoopTracePlan {
    private static final Pattern INT = Pattern.compile("\\{\"type\":\"int\",\"value\":(-?[0-9]{1,10})\\}");
    private final List<ArrayTrace.Operation> sites;
    private int phase;
    private Integer scalar, index;
    private int[] values;
    LoopTracePlan(List<ArrayTrace.Operation> sites) {
        this.sites = List.copyOf(sites);
        if (!sites.stream().map(ArrayTrace.Operation::kind).toList().equals(List.of("VARIABLE_DECLARE", "ARRAY_DECLARE", "VARIABLE_DECLARE", "CONDITION", "ARRAY_WRITE", "VARIABLE_WRITE"))
            || !"variable-1".equals(sites.get(0).variableId()) || !"variable-2".equals(sites.get(1).variableId())
            || !"variable-3".equals(sites.get(2).variableId()) || !"variable-3".equals(sites.get(5).variableId())
            || sites.stream().anyMatch(s -> s.source() == null)) throw new IllegalArgumentException("Invalid loop operation plan");
        for (int n = 0; n < 3; n++) Objects.requireNonNull(sites.get(n).variableName());
    }
    boolean complete() { return phase == 6; }
    void accept(String record, int sequence) {
        if (complete()) throw new IllegalArgumentException("Event after loop exit");
        var site = sites.get(phase);
        String prefix = "{\"sequence\":" + sequence + ",\"kind\":" + ArrayTrace.quote(site.kind()) + ",\"source\":" + site.source() + ",";
        if (phase == 0 || phase == 2) {
            prefix += "\"variableId\":" + ArrayTrace.quote(site.variableId()) + ",\"variableName\":" + ArrayTrace.quote(site.variableName()) + ",\"value\":";
            String suffix = ",\"scopeId\":\"" + (phase == 0 ? "scope-main" : "scope-loop-1") + "\",\"exitedVariableIds\":[]}";
            int actual = integerValue(extract(record, prefix, suffix));
            if (phase == 0) scalar = actual; else index = actual;
            phase++; return;
        }
        if (phase == 1) {
            prefix += "\"arrayId\":\"array-1\",\"variableId\":\"variable-2\",\"variableName\":" + ArrayTrace.quote(site.variableName()) + ",\"values\":[";
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
        if (phase == 3) {
            boolean actual = index < values.length;
            require(record.equals(prefix + "\"scopeId\":\"scope-loop-1\",\"value\":{\"type\":\"boolean\",\"value\":" + actual
                + "},\"exitedVariableIds\":" + (actual ? "[]" : "[\"variable-3\"]") + "}"), "Condition/result/retirement disagrees with captured state");
            if (actual) phase = 4; else { phase = 6; index = null; }
        } else if (phase == 4) {
            require(index >= 0 && index < values.length, "Invalid successful store index");
            require(record.equals(prefix + "\"arrayId\":\"array-1\",\"index\":" + index + ",\"value\":" + value(scalar) + ",\"exitedVariableIds\":[]}"), "Store disagrees with captured bindings");
            values[index] = scalar; phase = 5;
        } else {
            int updated = index + 1;
            require(record.equals(prefix + "\"variableId\":\"variable-3\",\"value\":" + value(updated) + ",\"exitedVariableIds\":[]}"), "Update disagrees with captured index");
            index = updated; phase = 3;
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
