import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

/** Incremental validator for the deliberately narrow, canonical experiment wire encoding. */
final class ArrayTrace {
    static final int BYTE_LIMIT = 65536, EVENT_LIMIT = 32, ARRAY_LIMIT = 16;
    enum Failure { LIMIT, INVALID }
    record Snapshot(List<String> events, boolean complete, String problem, int bytes) {}
    record Operation(String kind, String source, String variableId, String variableName) {}
    private static final Pattern EVENT = Pattern.compile(
        "\\{\"sequence\":([0-9]{1,3}),\"kind\":\"(ARRAY_DECLARE|ARRAY_READ|ARRAY_WRITE|VARIABLE_DECLARE|VARIABLE_WRITE)\",\"source\":(.*?),\"(arrayId|variableId)\":\"(array-1|variable-1|variable-3)\",(.*)\\}");
    private static final Pattern VALUE = Pattern.compile("\\{\"type\":\"int\",\"value\":(-?[0-9]{1,10})\\}");
    private static final Pattern ACCESS = Pattern.compile("\"index\":([0-9]{1,10}),\"value\":(.*)");
    private final Map<String, String> sources;
    private final String nameJson;
    private final boolean scalar;
    private final boolean combined;
    private final String arrayNameJson;
    private final List<String> operationOrder;
    private final List<Operation> expected;
    private final Map<String, Integer> scalarValues = new HashMap<>();
    private final List<String> events = new ArrayList<>();
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();
    private Consumer<Failure> stop = ignored -> {};
    private int[] values;
    private int bytes;
    private boolean ended, eof, sealed;
    private String problem = "";
    interface Plan { boolean complete(); void accept(String record, int sequence); }
    private Plan plan;

    ArrayTrace(Plan plan) {
        this(Map.of(), "");
        this.plan = Objects.requireNonNull(plan);
    }

    ArrayTrace(Map<String, String> sources) { this(sources, "values"); }
    ArrayTrace(Map<String, String> sources, String variableName) {
        this(sources, variableName, false);
    }
    ArrayTrace(Map<String, String> sources, String variableName, boolean scalar) {
        this(sources, variableName, scalar, null);
    }
    ArrayTrace(Map<String, String> sources, String scalarName, String arrayName) {
        this(sources, scalarName, false, Objects.requireNonNull(arrayName));
    }
    private ArrayTrace(Map<String, String> sources, String variableName, boolean scalar, String arrayName) {
        this(sources, variableName, scalar, arrayName, List.of());
    }
    ArrayTrace(List<Operation> expected) {
        this(Map.of(), "", false, "", expected);
        boolean update = operationOrder.equals(List.of("VARIABLE_DECLARE", "ARRAY_DECLARE", "VARIABLE_DECLARE", "VARIABLE_WRITE", "ARRAY_WRITE"));
        if ((!update && !operationOrder.equals(List.of("VARIABLE_DECLARE", "ARRAY_DECLARE", "VARIABLE_DECLARE", "ARRAY_WRITE")))
            || !expected.get(0).variableId().equals("variable-1") || !expected.get(1).variableId().equals("variable-2")
            || !expected.get(2).variableId().equals("variable-3")
            || (update && !"variable-3".equals(expected.get(3).variableId()))) throw new IllegalArgumentException("Invalid index operation plan");
    }
    private ArrayTrace(Map<String, String> sources, String variableName, boolean scalar, String arrayName, List<Operation> expected) {
        this.sources = Map.copyOf(sources);
        this.expected = List.copyOf(expected);
        this.scalar = scalar;
        this.combined = arrayName != null;
        this.operationOrder = !expected.isEmpty() ? expected.stream().map(Operation::kind).toList() : !combined ? List.of() : sources.containsKey("VARIABLE_WRITE")
            ? List.of("VARIABLE_DECLARE", "ARRAY_DECLARE", "VARIABLE_WRITE", "ARRAY_WRITE")
            : List.of("VARIABLE_DECLARE", "ARRAY_DECLARE", "ARRAY_WRITE");
        this.nameJson = quote(variableName);
        this.arrayNameJson = arrayName == null ? nameJson : quote(arrayName);
    }
    static String quote(String variableName) {
        StringBuilder name = new StringBuilder("\"");
        for (char c : variableName.toCharArray()) {
            if (c == '"' || c == '\\') name.append('\\').append(c);
            else if (c < 32 || c > 126) name.append(String.format("\\u%04x", (int)c));
            else name.append(c);
        }
        return name.append('"').toString();
    }
    synchronized void onFailure(Consumer<Failure> stop) { this.stop = stop; }
    synchronized int eventCount() { return events.size(); }
    synchronized boolean complete() { return ended && eof && problem.isEmpty(); }
    synchronized void accept(byte[] data, int count) {
        if (sealed || !problem.isEmpty()) return;
        for (int i = 0; i < count && problem.isEmpty(); i++) {
            if (bytes == BYTE_LIMIT) { fail(Failure.LIMIT, "Trace byte limit"); break; }
            bytes++;
            int b = data[i] & 255;
            if (ended) { fail(Failure.INVALID, "Data after trace end"); break; }
            if (b == '\n') {
                String record = line.toString(StandardCharsets.UTF_8);
                line.reset();
                record(record);
            } else if (b < 32 || b > 126) {
                fail(Failure.INVALID, "Invalid experiment trace encoding");
            } else {
                line.write(b);
            }
        }
    }

    private void record(String record) {
        if (record.equals("{\"transport\":\"end\"}")) {
            if (plan != null && !plan.complete()) { fail(Failure.INVALID, "Premature operation-plan completion"); return; }
            if (combined && events.size() != operationOrder.size()) { fail(Failure.INVALID, "Premature combined completion"); return; }
            ended = true; return;
        }
        if (record.equals("{\"transport\":\"limit\"}")) { fail(Failure.LIMIT, "Recorder event/byte/array limit"); return; }
        if (events.size() >= EVENT_LIMIT) { fail(Failure.LIMIT, "Collector event limit"); return; }
        try {
            if (plan != null) {
                plan.accept(record, events.size() + 1);
                events.add(record);
                return;
            }
            Matcher event = EVENT.matcher(record);
            if (!event.matches() || integer(event.group(1)) != events.size() + 1) {
                throw new IllegalArgumentException("Malformed event or sequence gap");
            }
            String kind = event.group(2), source = event.group(3), payload = event.group(6);
            boolean variableEvent = kind.startsWith("VARIABLE_");
            if (combined && (events.size() >= operationOrder.size() || !kind.equals(operationOrder.get(events.size()))))
                throw new IllegalArgumentException("Wrong combined operation order");
            Operation operation = expected.isEmpty() ? null : expected.get(events.size());
            String variableId = operation == null ? "variable-1" : operation.variableId();
            if (!event.group(4).equals(variableEvent ? "variableId" : "arrayId")
                || !event.group(5).equals(variableEvent ? variableId : "array-1")
                || (!combined && variableEvent != scalar)) throw new IllegalArgumentException("Wrong binding kind/identity");
            if (!source.equals(operation == null ? sources.get(kind) : operation.source())) throw new IllegalArgumentException("Original-source mismatch");
            if (variableEvent) {
                String prefix = kind.equals("VARIABLE_DECLARE") ? "\"variableName\":" + (operation == null ? nameJson : quote(operation.variableName())) + ",\"value\":" : "\"value\":";
                if ((kind.equals("VARIABLE_DECLARE") && scalarValues.containsKey(variableId)) || (kind.equals("VARIABLE_WRITE") && !scalarValues.containsKey(variableId))
                    || !payload.startsWith(prefix)) throw new IllegalArgumentException("Invalid scalar declaration/reference");
                Matcher value = VALUE.matcher(payload.substring(prefix.length()));
                if (!value.matches()) throw new IllegalArgumentException("Invalid scalar value");
                scalarValues.put(variableId, integer(value.group(1)));
            } else if (kind.equals("ARRAY_DECLARE")) {
                String prefix = "\"variableId\":\"" + (combined ? "variable-2" : "variable-1") + "\",\"variableName\":" + (operation == null ? arrayNameJson : quote(operation.variableName())) + ",\"values\":[";
                if (values != null || !payload.startsWith(prefix) || !payload.endsWith("]")) {
                    throw new IllegalArgumentException("Invalid declaration identity");
                }
                String list = payload.substring(prefix.length(), payload.length() - 1);
                List<Integer> captured = new ArrayList<>();
                for (int offset = 0; offset < list.length();) {
                    Matcher value = VALUE.matcher(list);
                    value.region(offset, list.length());
                    if (!value.lookingAt()) throw new IllegalArgumentException("Invalid int value");
                    if (captured.size() == ARRAY_LIMIT) { fail(Failure.LIMIT, "Array element limit"); return; }
                    captured.add(integer(value.group(1)));
                    offset = value.end();
                    if (offset < list.length()) {
                        if (list.charAt(offset++) != ',' || offset == list.length()) throw new IllegalArgumentException("Invalid value list");
                    }
                }
                values = captured.stream().mapToInt(Integer::intValue).toArray();
            } else {
                Matcher access = ACCESS.matcher(payload);
                if (values == null || !access.matches()) throw new IllegalArgumentException("Invalid array reference/access");
                int index = integer(access.group(1));
                Matcher value = VALUE.matcher(access.group(2));
                if (!value.matches() || index >= values.length) throw new IllegalArgumentException("Invalid access value/index");
                int captured = integer(value.group(1));
                if (combined && !Objects.equals(captured, scalarValues.get("variable-1"))) throw new IllegalArgumentException("Write disagrees with scalar binding");
                if (operation != null && !Objects.equals(index, scalarValues.get("variable-3"))) throw new IllegalArgumentException("Write disagrees with index binding");
                if (kind.equals("ARRAY_READ") && values[index] != captured) throw new IllegalArgumentException("Read disagrees with prefix state");
                if (kind.equals("ARRAY_WRITE")) values[index] = captured;
            }
            events.add(record);
        } catch (RuntimeException invalid) { fail(Failure.INVALID, invalid.getMessage()); }
    }

    synchronized void finish() {
        if (sealed) return;
        eof = true;
        if (line.size() != 0 && problem.isEmpty()) fail(Failure.INVALID, "Incomplete trailing record");
    }
    private static int integer(String text) {
        int value = Integer.parseInt(text);
        if (!Integer.toString(value).equals(text)) throw new IllegalArgumentException("Noncanonical integer");
        return value;
    }
    synchronized void transportFailure(String message) {
        if (!sealed && problem.isEmpty()) fail(Failure.INVALID, message);
    }
    private void fail(Failure failure, String message) {
        problem = message;
        stop.accept(failure);
    }
    synchronized Snapshot seal() {
        sealed = true;
        boolean complete = ended && eof && problem.isEmpty();
        String reason = problem.isEmpty() && !complete ? "No confirmed recorder completion" : problem;
        return new Snapshot(List.copyOf(events), complete, reason, bytes);
    }
}
