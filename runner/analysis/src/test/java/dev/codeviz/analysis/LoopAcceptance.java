package dev.codeviz.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Analysis and transformation assertions run only in the restricted worker. */
final class LoopAcceptance {
    record Example(String variant, String source, int limit) {}
    static void run(boolean prefix) throws Exception {
        String op = prefix ? "++i" : "i++", group = prefix ? "loop-pre-" : "loop-post-";
        String base = "public class Main { public static void main(String[] args) { int x = 8; int[] values = {5, 2, 7}; for (int i = 0; i < values.length; " + op
            + ") { values[i] = x; } System.out.print(\"FINAL=\" + x + \",\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";
        var analyzer = new ArrayAnalyzer(); var transformer = new ArrayTransformer();
        var facts = analyzer.analyze(bytes(base));
        check(facts.eligible(), facts.diagnostics().toString());
        var loop = facts.sites().loop();
        check(facts.sites().write() == null && facts.sites().scalarWrite() == null && facts.sites().index() == null, "dedicated loop facts");
        check(loop.index().type().equals("int") && loop.array().type().equals("int[]") && loop.scalar().type().equals("int"), "resolved types");
        check(facts.scopes().stream().anyMatch(s -> s.id().equals(loop.index().scopeId()) && s.kind().equals("ForStmt")
            && s.parentId().equals(loop.scalar().scopeId())), "for/main scope parent");
        check(facts.source().slice(loop.condition()).equals("i < values.length") && facts.source().slice(loop.update()).equals(op)
            && facts.source().slice(loop.body()).equals("{ values[i] = x; }"), "exact loop boundaries");
        String frozen = facts.syntaxCopy().orElseThrow().toString();
        check(transformer.transform(facts, bytes(base), 32).ready(), "loop transformation");
        check(frozen.equals(facts.syntaxCopy().orElseThrow().toString()), "original AST preserved");
        var changedCopy = facts.syntaxCopy().orElseThrow(); changedCopy.getTypes().clear();
        check(frozen.equals(facts.syntaxCopy().orElseThrow().toString()), "copy cannot mutate original");
        check(!transformer.transform(facts, bytes(base + " "), 32).ready(), "stale source");
        for (var component : LoopProbe.Facts.class.getRecordComponents()) {
            Object bad = component.getType() == String.class ? "WRONG" : null;
            var changed = replaceRecord(loop, component.getName(), bad);
            var sites = replaceRecord(facts.sites(), "loop", changed);
            check(!transformer.transform(with(facts, "sites", sites), bytes(base), 32).ready(), "missing/wrong loop fact " + component.getName());
        }
        for (String component : List.of("condition", "update", "store", "body"))
            check(!transformer.transform(with(facts, "sites", replaceRecord(facts.sites(), "loop", replaceRecord(loop, component, loop.scalarDeclaration()))), bytes(base), 32).ready(), "swapped site");
        for (String field : List.of("bindings", "accesses", "scopes")) {
            check(!transformer.transform(with(facts, field, List.of()), bytes(base), 32).ready(), "missing " + field);
        }
        var scopes = new ArrayList<>(facts.scopes());
        int loopScope = 0; while (!scopes.get(loopScope).id().equals(loop.index().scopeId())) loopScope++;
        scopes.set(loopScope, replaceRecord(scopes.get(loopScope), "parentId", "wrong-parent"));
        check(!transformer.transform(with(facts, "scopes", scopes), bytes(base), 32).ready(), "wrong scope parent");
        var bindings = new ArrayList<>(facts.bindings()); bindings.add(loop.index());
        check(!transformer.transform(with(facts, "bindings", bindings), bytes(base), 32).ready(), "duplicate binding");
        bindings = new ArrayList<>(facts.bindings());
        bindings.set(bindings.indexOf(loop.index()), replaceRecord(loop.index(), "type", "long"));
        check(!transformer.transform(with(facts, "bindings", bindings), bytes(base), 32).ready(), "wrong binding type");

        List<String> excluded = new ArrayList<>();
        for (String bad : List.of("i <= values.length", "i < 3", "x < values.length", "i < missing.length", "i < values.length()", "i < values[0]", "i++ < values.length", "", "(i < values.length)")) excluded.add(base.replace("i < values.length", bad));
        for (String bad : List.of("", "i--", "--i", "i += 1", "i = i + 1", "x++", "values[0]++", "(i)++", "i++, x++")) excluded.add(base.replace(op + ")", bad + ")"));
        for (String bad : List.of("", "int i = 0, j = 0", "i = 0", "long i = 0", "final int i = 0", "int i = x")) excluded.add(base.replace("int i = 0", bad));
        for (String bad : List.of("values[i] = x; break;", "continue;", "break;", "int j = 1; values[i] = x;", "values[i++] = x;", "values[i] = x++;", "values[i] = values[i];", "values[0] = x;", "values[x] = x;", "for(int j=0;j<1;j++){values[i]=x;}", "while(true){values[i]=x;}", "")) excluded.add(base.replace("values[i] = x;", bad));
        excluded.add(base.replace("{ values[i] = x; }", "values[i] = x;"));
        excluded.add(base.replace("for (int i = 0; i < values.length; " + op + ")", "for (int i : values)"));
        excluded.add(base.replace("for (", "label: for ("));
        excluded.add(base.replace("x", "System")); excluded.add(base.replace("values", "java"));
        excluded.add(base.replace("int x = 8", "final int x = 8"));
        excluded.add(base.replace("int[] values", "final int[] values"));
        excluded.add(base.replace("{5, 2, 7}", "{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}"));
        excluded.add(base.replace("for (int i = 0;", "int i = 0; for (;"));
        for (String source : excluded) {
            var rejected = analyzer.analyze(bytes(source));
            check(!rejected.eligible() && !transformer.transform(rejected, bytes(source), 32).ready(), "Excluded form accepted: " + source);
            check(rejected.diagnostics().size() <= ArrayAnalyzer.MAX_DIAGNOSTICS, "bounded diagnostics");
        }
        List<Example> examples = new ArrayList<>();
        examples.add(new Example("success", base, 32));
        examples.add(new Example("renamed", base.replace("x", "amount").replace("values", "items").replaceAll("\\bi\\b", "position").replace("= 8", "= -9").replace("{5, 2, 7}", "{4, 7}"), 32));
        examples.add(new Example("equal", base.replace("= 8", "= 1").replace("{5, 2, 7}", "{1, 1, 1}").replace("int i = 0", "int i = 1"), 32));
        examples.add(new Example("repeat", base.replace("{5, 2, 7}", "{8, 8, 8}"), 32));
        examples.add(new Example("empty", base.replace("{5, 2, 7}", "{}"), 32));
        examples.add(new Example("one", base.replace("{5, 2, 7}", "{5}"), 32));
        for (var e : Map.of("start1", "1", "startlength", "3", "startabove", "4", "negative", "-1", "minindex", "-2147483648", "maxindex", "2147483647").entrySet())
            examples.add(new Example(e.getKey(), base.replace("int i = 0", "int i = " + e.getValue()), 32));
        examples.add(new Example("minvalue", base.replace("= 8", "= -2147483648"), 32));
        examples.add(new Example("maxvalue", base.replace("= 8", "= 2147483647"), 32));
        examples.add(new Example("formatted", base.replace("values", "caf\u00e9").replaceAll("\\bi\\b", "pos\u00e9").replace(";", ";\r\n\t").replace(" < ", " /* \ud83d\ude00 */ <\r\n "), 32));
        examples.add(new Example("collision", base.replace("x", "__CodevizRecorder").replace("values", "__CodevizCondition").replaceAll("\\bi\\b", "__CodevizCondition1").replace("args", "__CodevizRecorder1"), 32));
        examples.add(new Example("no-probe", base.substring(0, base.indexOf("System.out")) + "} }", 32));
        for (int size : List.of(9, 10, 16)) {
            String values = String.join(", ", java.util.stream.IntStream.range(0, size).mapToObj(Integer::toString).toList());
            String source = base.replace("{5, 2, 7}", "{" + values + "}");
            examples.add(new Example(size == 9 ? "nine" : size == 10 ? "ten" : "sixteen", source, 32));
            if (size == 16) examples.add(new Example("sixteen-start7", source.replace("int i = 0", "int i = 7"), 32));
        }
        for (int limit = 1; limit <= 13; limit++) examples.add(new Example("limit" + limit, base, limit));
        check(examples.size() == 34, "fixture count");
        for (var example : examples) {
            var analysis = analyzer.analyze(bytes(example.source()));
            var generated = transformer.transform(analysis, bytes(example.source()), example.limit());
            check(generated.ready(), example.variant() + " " + generated.diagnostic() + " " + analysis.diagnostics());
            assertLowering(generated, analysis, prefix);
            var f = analysis.sites().loop();
            String metadata = "{\"name\":" + ArrayTransformer.json(group + example.variant()) + ",\"sourceId\":" + ArrayTransformer.json(generated.originalId())
                + ",\"generatedId\":" + ArrayTransformer.json(generated.generatedId()) + ",\"names\":[" + ArrayTransformer.json(f.scalar().name()) + "," + ArrayTransformer.json(f.array().name()) + "," + ArrayTransformer.json(f.index().name()) + "],\"sites\":["
                + String.join(",", generated.sites().stream().map(s -> "{\"kind\":" + ArrayTransformer.json(s.kind()) + ",\"original\":" + ArrayTransformer.rangeJson(s.original()) + ",\"generated\":" + ArrayTransformer.rangeJson(s.generated()) + "}").toList()) + "]}";
            System.out.println("LOOP_ARTIFACT " + group + example.variant() + " " + encode(example.source()) + " " + encode(generated.generatedSource()) + " "
                + encode(f.scalar().name() + "\n" + f.array().name() + "\n" + f.index().name()) + " "
                + encode(String.join("\n", generated.sites().stream().map(s -> ArrayTransformer.rangeJson(s.original())).toList())) + " " + encode(metadata));
        }
        System.out.println("PASS: 34 " + group + "fixtures; frozen facts, source/scope guards, lowering and " + excluded.size() + " rejected forms");
    }
    private static void assertLowering(ArrayTransformer.Result generated, ArrayAnalyzer.Result facts, boolean prefix) {
        var main = new JavaParser().parse(generated.generatedSource()).getResult().orElseThrow().getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst();
        var statements = main.getBody().orElseThrow().getStatements();
        check(statements.get(1).toString().contains("beforeOperation") && statements.get(4).toString().contains("beforeOperation"), "declaration preguards");
        var lower = statements.get(7).asBlockStmt();
        check(lower.getStatement(0).toString().contains("beforeOperation"), "index preguard");
        var iteration = lower.getStatement(3).asWhileStmt().getBody().asBlockStmt();
        for (int n : List.of(0, 4, 6)) check(iteration.getStatement(n).toString().contains("beforeOperation"), "guard before condition/store/update");
        var capture = iteration.getStatement(1).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0);
        check(capture.getInitializer().orElseThrow().isBinaryExpr(), "condition evaluated once into boolean");
        check(iteration.getStatement(2).asExpressionStmt().getExpression().asMethodCallExpr().getArgument(0).toString().equals(capture.getNameAsString()), "same condition recorded");
        check(iteration.getStatement(3).asIfStmt().getCondition().asUnaryExpr().getExpression().toString().equals(capture.getNameAsString()), "same condition branches");
        var unary = iteration.getStatement(7).asExpressionStmt().getExpression().asUnaryExpr();
        check(unary.getOperator() == (prefix ? UnaryExpr.Operator.PREFIX_INCREMENT : UnaryExpr.Operator.POSTFIX_INCREMENT), "original unary operator retained");
        check(iteration.getStatement(8).asExpressionStmt().getExpression().asMethodCallExpr().getArgument(0).isNameExpr(), "capture committed plain index");
        check(main.findAll(ForStmt.class).isEmpty() && main.findAll(WhileStmt.class).size() == 1 && generated.sites().size() == 6, "six static sites, one lowered loop");
        check(main.findAll(BinaryExpr.class).stream().filter(b -> b.getOperator() == BinaryExpr.Operator.LESS).count() == 1, "one condition expression");
        check(main.findAll(UnaryExpr.class).stream().filter(u -> u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT || u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT).count() == 1, "one increment expression");
        check(main.findAll(MethodCallExpr.class).stream().filter(c -> c.getNameAsString().equals("write")).count() == 1, "one store expression");
        if (facts.sites().loop().scalar().name().equals("__CodevizRecorder"))
            check(generated.helperName().equals("__CodevizRecorder2") && capture.getNameAsString().equals("__CodevizCondition2"), "fresh helper and temporary");
    }
    private static ArrayAnalyzer.Result with(ArrayAnalyzer.Result f, String field, Object value) throws Exception {
        var c = ArrayAnalyzer.Result.class.getDeclaredConstructors()[0]; c.setAccessible(true);
        return (ArrayAnalyzer.Result)c.newInstance(f.source(), f.syntaxCopy().orElseThrow(), field.equals("bindings") ? value : f.bindings(),
            field.equals("scopes") ? value : f.scopes(), field.equals("accesses") ? value : f.accesses(), f.diagnostics(), f.entry(), field.equals("sites") ? value : f.sites(), f.completeness());
    }
    @SuppressWarnings("unchecked") private static <T> T replaceRecord(T record, String field, Object value) throws Exception {
        var components = record.getClass().getRecordComponents(); Object[] args = new Object[components.length]; Class<?>[] types = new Class<?>[components.length];
        for (int n = 0; n < components.length; n++) { types[n] = components[n].getType(); args[n] = components[n].getName().equals(field) ? value : components[n].getAccessor().invoke(record); }
        return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static String encode(String value) { return Base64.getEncoder().encodeToString(bytes(value)); }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
