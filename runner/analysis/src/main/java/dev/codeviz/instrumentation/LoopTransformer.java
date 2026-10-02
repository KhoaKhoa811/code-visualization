package dev.codeviz.instrumentation;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import dev.codeviz.analysis.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static dev.codeviz.instrumentation.ArrayTransformer.*;

/** Loop lowering consumes dedicated frozen facts; no runtime values are predicted. */
final class LoopTransformer {
    static Result generate(ArrayAnalyzer.Result analysis, byte[] original, int limit) throws IOException {
        // Re-resolve in the isolated analysis worker before trusting externally supplied facts.
        // This also rejects omitted/duplicated/swapped records, including scope parent changes.
        var verified = new ArrayAnalyzer().analyze(original);
        if (!verified.eligible() || !Objects.equals(verified.sites(), analysis.sites())
            || !verified.bindings().equals(analysis.bindings()) || !verified.scopes().equals(analysis.scopes())
            || !verified.accesses().equals(analysis.accesses()) || !verified.entry().equals(analysis.entry())
            || verified.completeness() != analysis.completeness()) throw new IllegalArgumentException("Inconsistent loop facts");
        var copy = analysis.syntaxCopy().orElseThrow();
        if (!copy.equals(verified.syntaxCopy().orElseThrow())) throw new IllegalArgumentException("Inconsistent syntax copy");
        var facts = analysis.sites().loop();
        boolean reading = facts.readAddition() != null;
        String indexId = reading ? "variable-2" : "variable-3";
        var main = copy.getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst();
        var body = main.getBody().orElseThrow();
        var forLoop = body.getStatement(reading ? 1 : 2).asForStmt();
        Set<String> names = new HashSet<>(); copy.findAll(SimpleName.class).forEach(n -> names.add(n.asString()));
        String helper = fresh(names, "__CodevizRecorder"), temp = fresh(names, "__CodevizCondition");
        String x = reading ? null : facts.scalar().name(), a = facts.array().name(), i = facts.index().name();
        var lower = new BlockStmt();
        lower.addStatement(call(helper, "beforeOperation"));
        lower.addStatement(forLoop.getInitialization().get(0).clone());
        lower.addStatement(call(helper, "variableDeclare", new NameExpr(i), literal(rangeJson(facts.indexDeclaration())), literal(json(i)), literal(indexId)));
        var iteration = new BlockStmt();
        iteration.addStatement(call(helper, "beforeOperation"));
        iteration.addStatement(new VariableDeclarationExpr(new com.github.javaparser.ast.body.VariableDeclarator(
            PrimitiveType.booleanType(), temp, forLoop.getCompare().orElseThrow().clone())));
        iteration.addStatement(call(helper, "condition", new NameExpr(temp), literal(rangeJson(facts.condition()))));
        iteration.addStatement(new IfStmt(new UnaryExpr(new NameExpr(temp), UnaryExpr.Operator.LOGICAL_COMPLEMENT), new BreakStmt(), null));
        iteration.addStatement(call(helper, "beforeOperation"));
        if (reading) {
            String target = fresh(names, "__CodevizTarget"), targetIndex = fresh(names, "__CodevizIndex"), readValue = fresh(names, "__CodevizRead");
            iteration.addStatement(new VariableDeclarationExpr(new com.github.javaparser.ast.body.VariableDeclarator(new ArrayType(PrimitiveType.intType()), target, new NameExpr(a))));
            iteration.addStatement(new VariableDeclarationExpr(new com.github.javaparser.ast.body.VariableDeclarator(PrimitiveType.intType(), targetIndex, new NameExpr(i))));
            iteration.addStatement(new VariableDeclarationExpr(new com.github.javaparser.ast.body.VariableDeclarator(PrimitiveType.intType(), readValue,
                call(helper, "read", new NameExpr(a), new NameExpr(i), literal(rangeJson(facts.readAddition().read()))))));
            iteration.addStatement(call(helper, "beforeOperation"));
            var addition = forLoop.getBody().asBlockStmt().getStatement(0).asExpressionStmt().getExpression().asAssignExpr().getValue().asBinaryExpr().clone();
            addition.setLeft(new NameExpr(readValue));
            iteration.addStatement(call(helper, "write", new NameExpr(target), new NameExpr(targetIndex), addition, literal(rangeJson(facts.store()))));
        } else iteration.addStatement(call(helper, "write", new NameExpr(a), new NameExpr(i), new NameExpr(x), literal(rangeJson(facts.store()))));
        iteration.addStatement(call(helper, "beforeOperation"));
        iteration.addStatement(forLoop.getUpdate().get(0).clone());
        iteration.addStatement(call(helper, "variableWrite", new NameExpr(i), literal(rangeJson(facts.update())), literal(indexId)));
        lower.addStatement(new WhileStmt(new BooleanLiteralExpr(true), iteration));
        if (reading) {
            body.setStatement(1, lower);
            body.addStatement(1, call(helper, "declare", new NameExpr(a), literal(rangeJson(facts.arrayDeclaration())), literal(json(a))));
        } else {
            body.setStatement(2, lower);
            body.addStatement(2, call(helper, "declareCombined", new NameExpr(a), literal(rangeJson(facts.arrayDeclaration())), literal(json(a))));
            body.addStatement(1, call(helper, "beforeOperation"));
            body.addStatement(1, call(helper, "variableDeclare", new NameExpr(x), literal(rangeJson(facts.scalarDeclaration())), literal(json(x))));
        }
        body.addStatement(0, call(helper, "beforeOperation"));
        body.addStatement(0, call(helper, "begin", new IntegerLiteralExpr(Integer.toString(limit))));
        body.addStatement(call(helper, "end"));
        main.addThrownException(new ClassOrInterfaceType(new ClassOrInterfaceType(new ClassOrInterfaceType(null, "java"), "io"), "IOException"));
        var parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21).setTabSize(1));
        var recorder = parser.parse(resource("Recorder.java")).getResult().orElseThrow().getClassByName("Recorder").orElseThrow();
        recorder.getMethodsByName("event").stream().filter(m -> m.getParameters().size() == 4).findFirst().orElseThrow().setName("wireEvent");
        var additions = parser.parse(resource("LoopRecorderMembers.java")).getResult().orElseThrow().getClassByName("LoopRecorderMembers").orElseThrow();
        if (reading) {
            // Only trusted template identities change. Original source and legacy helpers stay intact.
            additions.findAll(StringLiteralExpr.class).forEach(s -> s.setString(s.asString().replace("variable-3", indexId)));
            recorder.getMethodsByName("variableDeclare").stream().filter(m -> m.getParameters().size() == 4).findFirst().orElseThrow().remove();
            var readMembers = parser.parse(resource("LoopReadRecorderMembers.java")).getResult().orElseThrow().getClassByName("LoopReadRecorderMembers").orElseThrow();
            readMembers.getMembers().forEach(m -> recorder.addMember(m.clone()));
        }
        additions.getMembers().forEach(m -> recorder.addMember(m.clone()));
        recorder.setName(helper); copy.addType(recorder);
        String generated = copy.toString();
        var output = new SourceSnapshot(generated.getBytes(StandardCharsets.UTF_8));
        var parsed = parser.parse(generated);
        if (!parsed.isSuccessful()) throw new IllegalArgumentException("Generated syntax");
        record Mapping(String method, String kind, SourceSnapshot.Span span) {}
        var mappings = reading ? List.of(new Mapping("declare", "ARRAY_DECLARE", facts.arrayDeclaration()),
            new Mapping("variableDeclare", "VARIABLE_DECLARE", facts.indexDeclaration()), new Mapping("condition", "CONDITION", facts.condition()),
            new Mapping("read", "ARRAY_READ", facts.readAddition().read()), new Mapping("write", "ARRAY_WRITE", facts.store()), new Mapping("variableWrite", "VARIABLE_WRITE", facts.update()))
            : List.of(new Mapping("variableDeclare", "VARIABLE_DECLARE", facts.scalarDeclaration()),
            new Mapping("declareCombined", "ARRAY_DECLARE", facts.arrayDeclaration()), new Mapping("variableDeclare", "VARIABLE_DECLARE", facts.indexDeclaration()),
            new Mapping("condition", "CONDITION", facts.condition()), new Mapping("write", "ARRAY_WRITE", facts.store()), new Mapping("variableWrite", "VARIABLE_WRITE", facts.update()));
        List<Site> sites = new ArrayList<>();
        for (var mapping : mappings) {
            var matches = parsed.getResult().orElseThrow().getClassByName("Main").orElseThrow().findAll(MethodCallExpr.class).stream()
                .filter(c -> c.getScope().map(Object::toString).orElse("").equals(helper) && c.getNameAsString().equals(mapping.method())
                    && c.getArguments().stream().anyMatch(arg -> arg.isStringLiteralExpr() && arg.asStringLiteralExpr().asString().equals(rangeJson(mapping.span())))).toList();
            if (matches.size() != 1) throw new IllegalArgumentException("Generated site mapping");
            sites.add(new Site(mapping.kind(), mapping.span(), output.span(matches.getFirst().getRange().orElseThrow())));
        }
        return new Result(true, "", analysis.source().id(), output.id(), generated, helper, reading ? a : x, sites);
    }
    private static String resource(String name) throws IOException {
        try (var in = Objects.requireNonNull(LoopTransformer.class.getResourceAsStream("/recorder/" + name))) {
            return new String(in.readNBytes(MAX_GENERATED_BYTES + 1), StandardCharsets.UTF_8);
        }
    }
    private static String fresh(Set<String> names, String base) { String name = base; for (int n = 1; !names.add(name); n++) name = base + n; return name; }
    private static StringLiteralExpr literal(String value) { return new StringLiteralExpr().setString(value); }
    private static MethodCallExpr call(String helper, String name, Expression... args) { return new MethodCallExpr(new NameExpr(helper), name, new NodeList<>(args)); }
}
