// Only installed for the array/read-addition loop. Existing transport and write helpers are reused.
class LoopReadRecorderMembers {
    static void variableDeclare(int actual, String source, String nameJson, String variableId) throws java.io.IOException {
        guard();
        if (!variableId.equals("variable-2") || known == null || !scalarBindings.isEmpty()) throw new IllegalStateException("Invalid loop index binding");
        scalarBindings.add(variableId);
        event("VARIABLE_DECLARE", source, "\"variableId\":\"" + variableId + "\",\"variableName\":" + nameJson + ",\"value\":" + value(actual), false);
    }
}
