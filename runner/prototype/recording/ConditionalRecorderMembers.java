// Installed only for draft-4. Reuse the established bounded wire writer and array operations.
class ConditionalRecorderMembers {
    private static boolean branchScope, retiring;
    private static void event(String kind, String source, String payload, boolean array) throws java.io.IOException {
        wireEvent(kind, source, payload + ",\"scopeId\":\"" + (branchScope ? "scope-if-1" : "scope-main")
            + "\",\"exitedVariableIds\":" + (retiring ? "[\"variable-2\"]" : "[]"), array);
    }
    static void condition(int left, int right, boolean actual, String source) throws java.io.IOException {
        guard();
        if (branchScope || count != 3) throw new IllegalStateException("Conditional operand sequence");
        event("CONDITION", source, "\"conditionRole\":\"IF\",\"value\":{\"type\":\"boolean\",\"value\":" + actual
            + "},\"comparison\":{\"operator\":\">\",\"left\":" + value(left) + ",\"right\":" + value(right)
            + ",\"leftReadSequence\":2,\"rightReadSequence\":3}", false);
        branchScope = actual;
    }
    static void tempDeclare(int actual, String source, String nameJson) throws java.io.IOException {
        guard();
        if (!branchScope || !scalarBindings.add("variable-2")) throw new IllegalStateException("Conditional temp binding");
        event("VARIABLE_DECLARE", source, "\"variableId\":\"variable-2\",\"variableName\":" + nameJson + ",\"value\":" + value(actual), false);
    }
    static void finishWrite(int[] array, int index, int actual, String source) throws java.io.IOException {
        guard();
        if (!branchScope || !scalarBindings.contains("variable-2")) throw new IllegalStateException("Missing conditional temp");
        retiring = true;
        write(array, index, actual, source);
        scalarBindings.remove("variable-2");
        retiring = false;
        branchScope = false;
    }
}
