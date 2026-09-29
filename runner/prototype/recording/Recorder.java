// Compiled only inside Docker, appended to the reviewed instrumented Main.java.
final class Recorder {
    private static java.io.OutputStream pipe;
    private static int count, bytes, limit;
    private static int[] known;
    private static final java.util.Set<String> scalarBindings = new java.util.HashSet<>();
    static void begin(int eventLimit) throws java.io.IOException {
        if (eventLimit < 1 || eventLimit > 32) throw new IllegalArgumentException("Invalid experiment event limit");
        limit = eventLimit;
        pipe = new java.io.FileOutputStream("/work/trace.pipe");
    }
    private static void guard() throws java.io.IOException {
        if (count >= limit) limited();
    }
    private static void limited() throws java.io.IOException {
        pipe.write("{\"transport\":\"limit\"}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        pipe.flush();
        pipe.close();
        throw new java.io.IOException("Experiment recording limit reached");
    }
    private static String value(int n) { return "{\"type\":\"int\",\"value\":" + n + "}"; }
    private static void event(String kind, String source, String payload) throws java.io.IOException {
        event(kind, source, payload, true);
    }
    private static void event(String kind, String source, String payload, boolean array) throws java.io.IOException {
        String record = "{\"sequence\":" + (count + 1) + ",\"kind\":\"" + kind
            + "\",\"source\":" + source + "," + (array ? "\"arrayId\":\"array-1\"," : "") + payload + "}\n";
        byte[] data = record.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes + data.length > 65500) limited(); // Reserve space for transport termination.
        pipe.write(data);
        pipe.flush();
        bytes += data.length;
        count++;
    }
    static void declare(int[] array, String source) throws java.io.IOException {
        declare(array, source, "\"values\"");
    }
    // The generator supplies a canonical JSON string for the resolved display name.
    static void declare(int[] array, String source, String nameJson) throws java.io.IOException {
        if (!scalarBindings.isEmpty()) throw new IllegalStateException("Use combined declaration for a second binding");
        declare(array, source, nameJson, "variable-1");
    }
    static void declareCombined(int[] array, String source, String nameJson) throws java.io.IOException {
        if (!scalarBindings.contains("variable-1")) throw new IllegalStateException("Scalar binding missing");
        declare(array, source, nameJson, "variable-2");
    }
    static void beforeOperation() throws java.io.IOException { guard(); }
    private static void declare(int[] array, String source, String nameJson, String variableId) throws java.io.IOException {
        guard();
        if (array.length > 16) limited();
        if (known != null) throw new IllegalStateException("One array per experiment");
        known = array;
        StringBuilder elements = new StringBuilder();
        for (int n : array) { if (!elements.isEmpty()) elements.append(','); elements.append(value(n)); }
        event("ARRAY_DECLARE", source, "\"variableId\":\"" + variableId + "\",\"variableName\":" + nameJson + ",\"values\":[" + elements + "]");
    }
    static int read(int[] array, int index, String source) throws java.io.IOException {
        guard();
        if (array != known) throw new IllegalStateException("Unknown array");
        int result = array[index];
        event("ARRAY_READ", source, "\"index\":" + index + ",\"value\":" + value(result));
        return result;
    }
    static void write(int[] array, int index, int value, String source) throws java.io.IOException {
        guard();
        if (array != known) throw new IllegalStateException("Unknown array");
        array[index] = value;
        event("ARRAY_WRITE", source, "\"index\":" + index + ",\"value\":" + value(value));
    }
    static void end() throws java.io.IOException {
        pipe.write("{\"transport\":\"end\"}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        pipe.flush();
        pipe.close();
    }
    static void variableDeclare(int actual, String source, String nameJson) throws java.io.IOException {
        variableDeclare(actual, source, nameJson, "variable-1");
    }
    static void variableDeclare(int actual, String source, String nameJson, String variableId) throws java.io.IOException {
        guard();
        if (!(variableId.equals("variable-1") && known == null && scalarBindings.isEmpty())
            && !(variableId.equals("variable-3") && known != null && scalarBindings.contains("variable-1")))
            throw new IllegalStateException("Invalid scalar binding");
        if (!scalarBindings.add(variableId)) throw new IllegalStateException("Duplicate scalar binding");
        event("VARIABLE_DECLARE", source, "\"variableId\":\"" + variableId + "\",\"variableName\":" + nameJson + ",\"value\":" + value(actual), false);
    }
    static void beforeVariableWrite() throws java.io.IOException { guard(); }
    static void variableWrite(int actual, String source) throws java.io.IOException {
        variableWrite(actual, source, "variable-1");
    }
    static void variableWrite(int actual, String source, String variableId) throws java.io.IOException {
        guard();
        if (!scalarBindings.contains(variableId)) throw new IllegalStateException("Unknown variable");
        event("VARIABLE_WRITE", source, "\"variableId\":\"" + variableId + "\",\"value\":" + value(actual), false);
    }
}
