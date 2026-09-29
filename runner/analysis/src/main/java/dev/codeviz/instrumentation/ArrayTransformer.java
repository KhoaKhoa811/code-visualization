package dev.codeviz.instrumentation;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import dev.codeviz.analysis.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Consumes frozen analysis; only the syntax copy is edited. No Java execution here. */
public final class ArrayTransformer {
    public static final int MAX_GENERATED_BYTES = 65_536;
    public record Site(String kind, SourceSnapshot.Span original, SourceSnapshot.Span generated) {}
    public record Result(boolean ready, String diagnostic, String originalId, String generatedId,
                         String generatedSource, String helperName, String variableName, List<Site> sites) {
        public Result { sites = List.copyOf(sites); }
    }
    public Result transform(ArrayAnalyzer.Result analysis, byte[] original, int eventLimit) {
        if (eventLimit < 1 || eventLimit > 32) return rejected("EVENT_LIMIT", analysis);
        if (!analysis.eligible() || (analysis.completeness() != ArrayAnalyzer.Completeness.COMPLETE_FOR_ARRAY_PROBE
            && analysis.completeness() != ArrayAnalyzer.Completeness.COMPLETE_FOR_INT_PROBE
            && analysis.completeness() != ArrayAnalyzer.Completeness.COMPLETE_FOR_COMBINED_PROBE))
            return rejected("INELIGIBLE_ANALYSIS", analysis);
        try { analysis.source().requireSame(original); }
        catch (IllegalArgumentException e) { return rejected("STALE_SOURCE", analysis); }
        try { return generate(analysis, eventLimit); }
        catch (StackOverflowError e) { return rejected("TRANSFORMATION_LIMIT", analysis); }
        catch (Exception e) { return rejected("TRANSFORMATION_FAILURE:" + e.getClass().getSimpleName(), analysis); }
    }
    private Result generate(ArrayAnalyzer.Result analysis, int eventLimit) throws IOException {
        SourceSnapshot source = analysis.source();
        CompilationUnit copy = analysis.syntaxCopy().orElseThrow();
        MethodDeclaration main = copy.getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst();
        BlockStmt body = main.getBody().orElseThrow();
        var declaration = body.getStatement(0).asExpressionStmt().getExpression().asVariableDeclarationExpr();
        boolean combined = analysis.sites().kind() == ArrayAnalyzer.ProbeKind.COMBINED;
        boolean update = combined && analysis.sites().scalarWrite() != null;
        boolean indexed = combined && analysis.sites().index() != null;
        if (analysis.sites().addition() != null && !(indexed && update)) return rejected("INCONSISTENT_ADDITION", analysis);
        if (analysis.sites().increment() != null && (!(indexed && update) || analysis.sites().addition() != null))
            return rejected("INCONSISTENT_INCREMENT", analysis);
        int storeIndex = combined ? 2 + (update ? 1 : 0) + (indexed ? 1 : 0) : 1;
        var assignment = body.getStatement(storeIndex).asExpressionStmt().getExpression().asAssignExpr();
        boolean scalar = analysis.sites().kind() == ArrayAnalyzer.ProbeKind.INT_VARIABLE;
        if (!source.span(declaration.getRange().orElseThrow()).equals(analysis.sites().declaration())
            || !source.span(assignment.getRange().orElseThrow()).equals(analysis.sites().write()))
            return rejected("INCONSISTENT_SITES", analysis);
        VariableDeclarator variable = declaration.getVariable(0);
        var binding = analysis.bindings().stream().filter(b -> b.span().equals(source.span(variable.getRange().orElseThrow())))
            .filter(b -> b.id().equals(analysis.sites().bindingId()) && b.type().equals(scalar || combined ? "int" : "int[]") && b.scopeId() != null).findFirst().orElseThrow();
        if (!scalar && !combined) for (ArrayAccessExpr access : List.of(assignment.getTarget().asArrayAccessExpr(), assignment.getValue().asArrayAccessExpr())) {
            var span = source.span(access.getRange().orElseThrow());
            if (analysis.accesses().stream().noneMatch(a -> a.span().equals(span) && a.bindingId().equals(binding.id())
                    && a.arrayType().equals("int[]") && a.elementType().equals("int")))
                return rejected("INCONSISTENT_BINDING", analysis);
        }
        Set<String> names = new HashSet<>();
        copy.findAll(SimpleName.class).forEach(n -> names.add(n.asString()));
        String helper = "__CodevizRecorder";
        for (int suffix = 1; names.contains(helper); suffix++) helper = "__CodevizRecorder" + suffix;

        if (combined) {
            var arrayDeclaration = body.getStatement(1).asExpressionStmt().getExpression().asVariableDeclarationExpr();
            var array = arrayDeclaration.getVariable(0);
            var arrayBinding = analysis.bindings().stream().filter(b -> b.id().equals(analysis.sites().arrayBindingId())
                && b.type().equals("int[]") && b.scopeId() != null
                && b.span().equals(source.span(array.getRange().orElseThrow()))).findFirst().orElseThrow();
            var left = assignment.getTarget().asArrayAccessExpr();
            if (!source.span(arrayDeclaration.getRange().orElseThrow()).equals(analysis.sites().arrayDeclaration())
                || !source.span(assignment.getValue().getRange().orElseThrow()).equals(analysis.sites().scalarReference())
                || analysis.accesses().stream().noneMatch(a -> a.span().equals(source.span(left.getRange().orElseThrow()))
                    && a.bindingId().equals(arrayBinding.id()) && a.kind().equals("WRITE_TARGET")
                    && a.arrayType().equals("int[]") && a.elementType().equals("int"))) return rejected("INCONSISTENT_BINDING", analysis);
            if (update) {
                var operation = body.getStatement(indexed ? 3 : 2).asExpressionStmt().getExpression();
                if (!source.span(operation.getRange().orElseThrow()).equals(analysis.sites().scalarWrite())
                    || !(indexed ? analysis.sites().index().bindingId() : binding.id()).equals(analysis.sites().scalarWriteBindingId()))
                    return rejected("INCONSISTENT_BINDING", analysis);
                var additionFacts = analysis.sites().addition();
                var incrementFacts = analysis.sites().increment();
                if (operation instanceof UnaryExpr increment) {
                    if (!indexed || incrementFacts == null || additionFacts != null
                        || (increment.getOperator() != UnaryExpr.Operator.POSTFIX_INCREMENT && increment.getOperator() != UnaryExpr.Operator.PREFIX_INCREMENT)
                        || !increment.getOperator().name().equals(incrementFacts.operator()) || !"int".equals(incrementFacts.resultType())
                        || !analysis.sites().index().bindingId().equals(incrementFacts.bindingId())
                        || !source.span(increment.getRange().orElseThrow()).equals(incrementFacts.expression())
                        || !source.span(increment.getExpression().getRange().orElseThrow()).equals(incrementFacts.operand())
                        || !(increment.getExpression() instanceof NameExpr operand)
                        || !operand.getNameAsString().equals(body.getStatement(2).asExpressionStmt().getExpression()
                            .asVariableDeclarationExpr().getVariable(0).getNameAsString())) return rejected("INCONSISTENT_INCREMENT", analysis);
                } else {
                    if (incrementFacts != null) return rejected("INCONSISTENT_INCREMENT", analysis);
                    var scalarAssignment = operation.asAssignExpr();
                    if (scalarAssignment.getValue().isBinaryExpr()) {
                        var addition = scalarAssignment.getValue().asBinaryExpr();
                        if (!indexed || additionFacts == null || addition.getOperator() != BinaryExpr.Operator.PLUS
                            || !"PLUS".equals(additionFacts.operator()) || !"int".equals(additionFacts.resultType())
                            || !analysis.sites().index().bindingId().equals(additionFacts.bindingId())
                            || !source.span(addition.getRange().orElseThrow()).equals(additionFacts.expression())
                            || !source.span(addition.getLeft().getRange().orElseThrow()).equals(additionFacts.reference())
                            || !source.span(addition.getRight().getRange().orElseThrow()).equals(additionFacts.literal()))
                            return rejected("INCONSISTENT_ADDITION", analysis);
                        var indexName = body.getStatement(2).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0).getNameAsString();
                        if (!addition.getLeft().isNameExpr() || !addition.getLeft().asNameExpr().getNameAsString().equals(indexName)
                            || !scalarAssignment.getTarget().isNameExpr() || !scalarAssignment.getTarget().asNameExpr().getNameAsString().equals(indexName))
                            return rejected("INCONSISTENT_ADDITION", analysis);
                    } else if (additionFacts != null) return rejected("INCONSISTENT_ADDITION", analysis);
                }
            } else if (analysis.sites().scalarWriteBindingId() != null) return rejected("INCONSISTENT_BINDING", analysis);
            if (indexed) {
                var indexDeclaration = body.getStatement(2).asExpressionStmt().getExpression().asVariableDeclarationExpr();
                var indexVariable = indexDeclaration.getVariable(0);
                var indexFacts = analysis.sites().index();
                var indexBinding = analysis.bindings().stream().filter(b -> b.id().equals(indexFacts.bindingId()) && b.type().equals("int")
                    && b.scopeId() != null && b.span().equals(source.span(indexVariable.getRange().orElseThrow()))).findFirst().orElseThrow();
                if (indexBinding.id().equals(binding.id()) || indexBinding.id().equals(arrayBinding.id())
                    || !source.span(indexDeclaration.getRange().orElseThrow()).equals(indexFacts.declaration())
                    || !source.span(left.getIndex().getRange().orElseThrow()).equals(indexFacts.reference())) return rejected("INCONSISTENT_BINDING", analysis);
            }
            body.setStatement(storeIndex, new ExpressionStmt(call(helper, "write", left.getName().clone(), left.getIndex().clone(),
                assignment.getValue().clone(), literal(rangeJson(analysis.sites().write())))));
            if (update) {
                int updateIndex = indexed ? 3 : 2;
                var updatedVariable = indexed ? body.getStatement(2).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0) : variable;
                body.addStatement(updateIndex + 1, call(helper, "variableWrite", new NameExpr(updatedVariable.getNameAsString()),
                    literal(rangeJson(analysis.sites().scalarWrite())), literal(indexed ? "variable-3" : "variable-1")));
                body.addStatement(updateIndex, call(helper, "beforeVariableWrite"));
            }
            if (indexed) {
                var indexVariable = body.getStatement(2).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0);
                body.addStatement(3, call(helper, "variableDeclare", new NameExpr(indexVariable.getNameAsString()),
                    literal(rangeJson(analysis.sites().index().declaration())), literal(json(indexVariable.getNameAsString())), literal("variable-3")));
                body.addStatement(2, call(helper, "beforeOperation"));
            }
            body.addStatement(2, call(helper, "declareCombined", new NameExpr(array.getNameAsString()),
                literal(rangeJson(analysis.sites().arrayDeclaration())), literal(json(arrayBinding.name()))));
            body.addStatement(1, call(helper, "beforeOperation"));
            body.addStatement(1, call(helper, "variableDeclare", new NameExpr(variable.getNameAsString()),
                literal(rangeJson(analysis.sites().declaration())), literal(json(binding.name()))));
        } else if (scalar) {
            // Keep the actual assignment; record only after it commits. Guard before mutation.
            body.addStatement(2, call(helper, "variableWrite", new NameExpr(variable.getNameAsString()), literal(rangeJson(analysis.sites().write()))));
            body.addStatement(1, call(helper, "beforeVariableWrite"));
            body.addStatement(1, call(helper, "variableDeclare", new NameExpr(variable.getNameAsString()),
                literal(rangeJson(analysis.sites().declaration())), literal(json(binding.name()))));
        } else {
            var left = assignment.getTarget().asArrayAccessExpr();
            var right = assignment.getValue().asArrayAccessExpr();
            if (!source.span(right.getRange().orElseThrow()).equals(analysis.sites().read())) return rejected("INCONSISTENT_SITES", analysis);
            MethodCallExpr read = call(helper, "read", right.getName().clone(), right.getIndex().clone(), literal(rangeJson(analysis.sites().read())));
            MethodCallExpr write = call(helper, "write", left.getName().clone(), left.getIndex().clone(), read, literal(rangeJson(analysis.sites().write())));
            body.setStatement(1, new ExpressionStmt(write));
            body.addStatement(1, call(helper, "declare", new NameExpr(variable.getNameAsString()),
                literal(rangeJson(analysis.sites().declaration())), literal(json(binding.name()))));
        }
        body.addStatement(0, call(helper, "begin", new IntegerLiteralExpr(Integer.toString(eventLimit))));
        body.addStatement(call(helper, "end"));
        main.addThrownException(new ClassOrInterfaceType(new ClassOrInterfaceType(new ClassOrInterfaceType(null, "java"), "io"), "IOException"));

        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21).setTabSize(1));
        String recorder;
        try (InputStream in = Objects.requireNonNull(ArrayTransformer.class.getResourceAsStream("/recorder/Recorder.java"))) {
            recorder = new String(in.readNBytes(MAX_GENERATED_BYTES + 1), StandardCharsets.UTF_8);
        }
        var helperAst = parser.parse(recorder).getResult().orElseThrow().getClassByName("Recorder").orElseThrow();
        helperAst.setName(helper);
        copy.addType(helperAst);
        String generated = copy.toString();
        byte[] bytes = generated.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_GENERATED_BYTES) return rejected("GENERATED_SOURCE_LIMIT", analysis);
        SourceSnapshot output = new SourceSnapshot(bytes);
        var parsed = parser.parse(generated);
        if (!parsed.isSuccessful()) return rejected("GENERATED_SYNTAX_FAILURE", analysis);
        List<Site> sites = new ArrayList<>();
        Map<String, SourceSnapshot.Span> originalSites = update
            ? Map.of("variableDeclare", analysis.sites().declaration(), "declareCombined", analysis.sites().arrayDeclaration(),
                "variableWrite", analysis.sites().scalarWrite(), "write", analysis.sites().write())
            : combined
            ? Map.of("variableDeclare", analysis.sites().declaration(), "declareCombined", analysis.sites().arrayDeclaration(), "write", analysis.sites().write())
            : scalar
            ? Map.of("variableDeclare", analysis.sites().declaration(), "variableWrite", analysis.sites().write())
            : Map.of("declare", analysis.sites().declaration(), "read", analysis.sites().read(), "write", analysis.sites().write());
        record Mapping(String method, String kind, SourceSnapshot.Span original) {}
        List<Mapping> mappings = new ArrayList<>();
        originalSites.forEach((method, originalSpan) -> mappings.add(new Mapping(method,
            method.equals("declareCombined") ? "ARRAY_DECLARE" : method.startsWith("variable")
                ? method.equals("variableDeclare") ? "VARIABLE_DECLARE" : "VARIABLE_WRITE" : "ARRAY_" + method.toUpperCase(Locale.ROOT), originalSpan)));
        if (indexed) mappings.add(new Mapping("variableDeclare", "VARIABLE_DECLARE", analysis.sites().index().declaration()));
        Set<Mapping> matched = new HashSet<>();
        for (MethodCallExpr call : parsed.getResult().orElseThrow().getClassByName("Main").orElseThrow().findAll(MethodCallExpr.class)) {
            if (call.getScope().map(Object::toString).orElse("").equals(helper) && originalSites.containsKey(call.getNameAsString())) {
                var matches = mappings.stream().filter(m -> m.method().equals(call.getNameAsString()) && call.getArguments().stream()
                    .anyMatch(a -> a.isStringLiteralExpr() && a.asStringLiteralExpr().asString().equals(rangeJson(m.original())))).toList();
                if (matches.size() != 1 || !matched.add(matches.getFirst())) return rejected("GENERATED_SITE_FAILURE", analysis);
                var mapping = matches.getFirst();
                sites.add(new Site(mapping.kind(), mapping.original(),
                    output.span(call.getRange().orElseThrow())));
            }
        }
        if (sites.size() != (combined ? 3 + (update ? 1 : 0) + (indexed ? 1 : 0) : scalar ? 2 : 3)
            || matched.size() != mappings.size()) return rejected("GENERATED_SITE_FAILURE", analysis);
        return new Result(true, "", source.id(), output.id(), generated, helper, binding.name(), sites);
    }
    private static MethodCallExpr call(String helper, String name, Expression... arguments) {
        return new MethodCallExpr(new NameExpr(helper), name, new NodeList<>(arguments));
    }
    private static StringLiteralExpr literal(String value) { return new StringLiteralExpr().setString(value); }
    private static Result rejected(String reason, ArrayAnalyzer.Result analysis) {
        return new Result(false, reason, analysis.source() == null ? null : analysis.source().id(), null, null, null, null, List.of());
    }
    public static String rangeJson(SourceSnapshot.Span span) {
        return "{\"file\":\"Main.java\",\"start\":{\"line\":" + span.start().line() + ",\"column\":" + span.start().column()
            + "},\"end\":{\"line\":" + span.end().line() + ",\"column\":" + span.end().column() + "}}";
    }
    public static String json(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') result.append('\\').append(c);
            else if (c < 32 || c > 126) result.append(String.format("\\u%04x", (int)c));
            else result.append(c);
        }
        return result.append('"').toString();
    }
}
