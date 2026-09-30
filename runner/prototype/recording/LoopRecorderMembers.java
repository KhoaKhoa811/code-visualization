// Template members added only to the draft-3 recorder. Transport/bounds stay in Recorder.java.
class LoopRecorderMembers {
    private static void event(String kind, String source, String payload, boolean array) throws java.io.IOException {
        if (kind.endsWith("DECLARE")) {
            String scope = payload.contains("\"variableId\":\"variable-3\"") ? "scope-loop-1" : "scope-main";
            payload += ",\"scopeId\":\"" + scope + "\"";
        }
        wireEvent(kind, source, payload + ",\"exitedVariableIds\":[]", array);
    }
    static void condition(boolean actual, String source) throws java.io.IOException {
        guard();
        if (!scalarBindings.contains("variable-3")) throw new IllegalStateException("Loop binding missing");
        wireEvent("CONDITION", source, "\"scopeId\":\"scope-loop-1\",\"value\":{\"type\":\"boolean\",\"value\":" + actual
            + "},\"exitedVariableIds\":" + (actual ? "[]" : "[\"variable-3\"]"), false);
        if (!actual) scalarBindings.remove("variable-3");
    }
}
