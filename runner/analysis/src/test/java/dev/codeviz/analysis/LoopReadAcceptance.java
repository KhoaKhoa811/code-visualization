package dev.codeviz.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read/addition cases are analyzed and transformed only in the isolated worker. */
final class LoopReadAcceptance {
    record Example(String name, String source, int limit) {}
    static void run(boolean prefix) throws Exception {
        String op = prefix ? "++i" : "i++", group = prefix ? "read-pre-" : "read-post-";
        String base = "public class Main { public static void main(String[] args) { int[] values = {5, 2, 7}; for (int i = 0; i < values.length; " + op
            + ") { values[i] = values[i] + 1; } System.out.print(\"FINAL=\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";
        var analyzer = new ArrayAnalyzer(); var transformer = new ArrayTransformer();
        var facts = analyzer.analyze(bytes(base));
        check(facts.eligible(), facts.diagnostics().toString());
        var loop = facts.sites().loop(); var read = loop.readAddition();
        check(loop.scalar() == null && loop.scalarDeclaration() == null && loop.scalarReference() == null, "no synthetic scalar facts");
        check(read.resultType().equals("int") && read.addend() == 1 && read.arrayBindingId().equals(loop.array().id())
            && read.indexBindingId().equals(loop.index().id()), "resolved read/addition facts");
        check(facts.source().slice(read.read()).equals("values[i]") && facts.source().slice(read.expression()).equals("values[i] + 1")
            && read.read().startOffset() > loop.store().startOffset(), "RHS-only original range");
        check(facts.scopes().stream().anyMatch(s -> s.id().equals(loop.index().scopeId()) && s.kind().equals("ForStmt")
            && s.parentId().equals(loop.array().scopeId())), "loop scope parent");
        String frozen = facts.syntaxCopy().orElseThrow().toString();
        check(transformer.transform(facts, bytes(base), 32).ready(), "transform");
        facts.syntaxCopy().orElseThrow().getTypes().clear();
        check(frozen.equals(facts.syntaxCopy().orElseThrow().toString()), "immutable original AST");
        check(!transformer.transform(facts, bytes(base + " "), 32).ready(), "stale source");
        for (var component : LoopProbe.ReadAddition.class.getRecordComponents()) {
            Object bad = component.getType() == int.class ? 2 : component.getType() == String.class ? "WRONG" : null;
            var changed = LoopAcceptance.replaceRecord(read, component.getName(), bad);
            rejectFacts(transformer, facts, base, LoopAcceptance.replaceRecord(loop, "readAddition", changed));
        }
        for (var component : LoopProbe.Facts.class.getRecordComponents()) {
            if (component.getAccessor().invoke(loop) == null) continue;
            rejectFacts(transformer, facts, base, LoopAcceptance.replaceRecord(loop, component.getName(), component.getType() == String.class ? "WRONG" : null));
        }
        rejectFacts(transformer, facts, base, LoopAcceptance.replaceRecord(loop, "readAddition", LoopAcceptance.replaceRecord(read, "read", loop.store())));
        for (String field : List.of("bindings", "accesses", "scopes"))
            check(!transformer.transform(LoopAcceptance.with(facts, field, List.of()), bytes(base), 32).ready(), "missing " + field);
        List<String> excluded = new ArrayList<>();
        for (String rhs : List.of("values[i]", "values[i] - 1", "values[i] * 1", "1 + values[i]", "values[i] + values[i]", "values[i] + i",
            "values[i] + 1L", "values[i] + 0x1", "values[i] + 1_0", "values[i] + (1)", "values[i++] + 1", "values[0] + 1", "values[i]++", "++values[i]",
            "other[i] + 1", "values[i] + Math.abs(1)", "values[i] + 1 + 2", "(values[i]) + 1")) excluded.add(base.replace("values[i] + 1", rhs));
        for (String body : List.of("values[i] += 1;", "values[i]++;", "values[i] = values[i] + 1; break;", "continue;", "int x=1; values[i]=values[i]+1;",
            "values[i++]=values[i]+1;", "values[0]=values[i]+1;", "for(int j=0;j<1;j++){values[i]=values[i]+1;}")) excluded.add(base.replace("values[i] = values[i] + 1;", body));
        for (String condition : List.of("i <= values.length", "i < 3", "i++ < values.length", "(i < values.length)")) excluded.add(base.replace("i < values.length", condition));
        for (String update : List.of("i--", "i += 1", "i = i + 1", "values[i]++")) excluded.add(base.replace(op + ")", update + ")"));
        excluded.add(base.replace("int[] values", "int[] other = {1}; int[] values"));
        excluded.add(base.replace("values", "java")); excluded.add(base.replaceAll("\\bi\\b", "System"));
        excluded.add(base.replace("int[] values", "final int[] values"));
        excluded.add(base.replace("{5, 2, 7}", "{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}"));
        for (String source : excluded) {
            var rejected = analyzer.analyze(bytes(source));
            check(!rejected.eligible() && !transformer.transform(rejected, bytes(source), 32).ready(), "Excluded form accepted: " + source);
            check(rejected.diagnostics().size() <= ArrayAnalyzer.MAX_DIAGNOSTICS, "bounded diagnostics");
        }
        List<Example> examples = new ArrayList<>();
        examples.add(new Example("success", base, 32));
        examples.add(new Example("renamed", base.replace("values", "items").replaceAll("\\bi\\b", "position").replace("{5, 2, 7}", "{4, 7}"), 32));
        examples.add(new Example("equal", base.replace("{5, 2, 7}", "{1, 1, 1}").replace("int i = 0", "int i = 1"), 32));
        examples.add(new Example("repeat", base.replace("{5, 2, 7}", "{8, 8, 8}"), 32));
        examples.add(new Example("empty", base.replace("{5, 2, 7}", "{}"), 32));
        examples.add(new Example("one", base.replace("{5, 2, 7}", "{5}"), 32));
        for (var e : Map.of("start1", "1", "startlength", "3", "startabove", "4", "negative", "-1", "minindex", "-2147483648", "maxindex", "2147483647").entrySet())
            examples.add(new Example(e.getKey(), base.replace("int i = 0", "int i = " + e.getValue()), 32));
        examples.add(new Example("minvalue", base.replace("{5, 2, 7}", "{-2147483648, 0, 1}").replace("+ 1;", "+ -1;"), 32));
        examples.add(new Example("maxvalue", base.replace("{5, 2, 7}", "{2147483647, 0, 1}"), 32));
        examples.add(new Example("formatted", base.replace("values", "caf\u00e9").replaceAll("\\bi\\b", "pos\u00e9").replace(";", ";\r\n\t")
            .replace(" < ", " /* \ud83d\ude00 */ <\r\n ").replace(" + 1", " /* read */ +\r\n\t1"), 32));
        examples.add(new Example("collision", base.replace("values", "__CodevizRead").replaceAll("\\bi\\b", "__CodevizIndex").replace("args", "__CodevizTarget"), 32));
        examples.add(new Example("no-probe", base.substring(0, base.indexOf("System.out")) + "} }", 32));
        for (int size : List.of(7, 8, 16)) {
            String values = String.join(", ", java.util.stream.IntStream.range(0, size).mapToObj(Integer::toString).toList());
            String source = base.replace("{5, 2, 7}", "{" + values + "}");
            examples.add(new Example(size == 7 ? "seven" : size == 8 ? "eight" : "sixteen", source, 32));
            if (size == 16) examples.add(new Example("sixteen-start9", source.replace("int i = 0", "int i = 9"), 32));
        }
        for (var e : Map.of("negativeadd", "-9", "zeroadd", "0", "minadd", "-2147483648", "maxadd", "2147483647").entrySet())
            examples.add(new Example(e.getKey(), base.replace("+ 1;", "+ " + e.getValue() + ";"), 32));
        for (int limit = 1; limit <= 15; limit++) examples.add(new Example("limit" + limit, base, limit));
        check(examples.size() == 40, "fixture count");
        for (var example : examples) {
            var analysis = analyzer.analyze(bytes(example.source())); var f = analysis.sites() == null ? null : analysis.sites().loop();
            var generated = transformer.transform(analysis, bytes(example.source()), example.limit());
            check(generated.ready(), example.name() + " " + generated.diagnostic() + " " + analysis.diagnostics());
            assertLowering(generated, prefix);
            String metadata = "{\"name\":" + ArrayTransformer.json(group + example.name()) + ",\"sourceId\":" + ArrayTransformer.json(generated.originalId())
                + ",\"generatedId\":" + ArrayTransformer.json(generated.generatedId()) + ",\"addend\":" + f.readAddition().addend()
                + ",\"names\":[" + ArrayTransformer.json(f.array().name()) + "," + ArrayTransformer.json(f.index().name()) + "],\"sites\":["
                + String.join(",", generated.sites().stream().map(s -> "{\"kind\":" + ArrayTransformer.json(s.kind()) + ",\"original\":" + ArrayTransformer.rangeJson(s.original()) + ",\"generated\":" + ArrayTransformer.rangeJson(s.generated()) + "}").toList()) + "]}";
            System.out.println("READ_ARTIFACT " + group + example.name() + " " + encode(example.source()) + " " + encode(generated.generatedSource()) + " "
                + encode(f.array().name() + "\n" + f.index().name()) + " " + encode(String.join("\n", generated.sites().stream().map(s -> ArrayTransformer.rangeJson(s.original())).toList()))
                + " " + encode(metadata) + " " + f.readAddition().addend());
        }
        System.out.println("PASS: 40 " + group + "fixtures; frozen read facts, single evaluation, guards and " + excluded.size() + " rejected forms");
    }
    private static void assertLowering(ArrayTransformer.Result generated, boolean prefix) {
        var main = new JavaParser().parse(generated.generatedSource()).getResult().orElseThrow().getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst();
        var statements = main.getBody().orElseThrow().getStatements();
        check(statements.get(1).toString().contains("beforeOperation"), "array preguard");
        var lower = statements.get(4).asBlockStmt();
        check(lower.getStatement(0).toString().contains("beforeOperation"), "index preguard");
        var iteration = lower.getStatement(3).asWhileStmt().getBody().asBlockStmt();
        for (int n : List.of(0, 4, 8, 10)) check(iteration.getStatement(n).toString().contains("beforeOperation"), "condition/read/write/update guards");
        var target = declaration(iteration, 5); var index = declaration(iteration, 6); var read = declaration(iteration, 7);
        check(target.getInitializer().orElseThrow().isNameExpr() && index.getInitializer().orElseThrow().isNameExpr(), "capture LHS before RHS");
        check(read.getInitializer().orElseThrow().asMethodCallExpr().getNameAsString().equals("read"), "one captured RHS read");
        var write = iteration.getStatement(9).asExpressionStmt().getExpression().asMethodCallExpr();
        check(write.getArgument(0).toString().equals(target.getNameAsString()) && write.getArgument(1).toString().equals(index.getNameAsString()), "reuse captured LHS");
        check(write.getArgument(2).asBinaryExpr().getLeft().toString().equals(read.getNameAsString()), "reuse captured RHS in native addition");
        var unary = iteration.getStatement(11).asExpressionStmt().getExpression().asUnaryExpr();
        check(unary.getOperator() == (prefix ? UnaryExpr.Operator.PREFIX_INCREMENT : UnaryExpr.Operator.POSTFIX_INCREMENT), "preserved increment");
        check(main.findAll(ArrayAccessExpr.class).isEmpty() && main.findAll(ForStmt.class).isEmpty() && main.findAll(WhileStmt.class).size() == 1, "no duplicate user accesses");
        for (String method : List.of("read", "write", "condition", "variableWrite"))
            check(main.findAll(MethodCallExpr.class).stream().filter(c -> c.getNameAsString().equals(method)).count() == 1, "one " + method);
        check(generated.sites().stream().map(ArrayTransformer.Site::kind).toList().equals(List.of("ARRAY_DECLARE", "VARIABLE_DECLARE", "CONDITION", "ARRAY_READ", "ARRAY_WRITE", "VARIABLE_WRITE")), "six ordered sites");
    }
    private static com.github.javaparser.ast.body.VariableDeclarator declaration(BlockStmt b, int n) {
        return b.getStatement(n).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0);
    }
    private static void rejectFacts(ArrayTransformer t, ArrayAnalyzer.Result f, String source, LoopProbe.Facts bad) throws Exception {
        check(!t.transform(LoopAcceptance.with(f, "sites", LoopAcceptance.replaceRecord(f.sites(), "loop", bad)), bytes(source), 32).ready(), "inconsistent read facts accepted");
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static String encode(String value) { return Base64.getEncoder().encodeToString(bytes(value)); }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
