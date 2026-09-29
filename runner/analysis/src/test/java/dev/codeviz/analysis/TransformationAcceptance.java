package dev.codeviz.analysis;

import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Runs analysis/transformation in Docker; exports bounded reviewed fixtures for the existing runner. */
public final class TransformationAcceptance {
    private record Example(String name, String source, int limit) {}
    public static void run(String original) throws Exception {
        ArrayAnalyzer analyzer = new ArrayAnalyzer();
        ArrayTransformer transformer = new ArrayTransformer();
        var analysis = analyzer.analyze(bytes(original));
        String ast = analysis.syntaxCopy().orElseThrow().toString();
        var result = transformer.transform(analysis, bytes(original), 32);
        check(result.ready(), result.diagnostic());
        check(ast.equals(analysis.syntaxCopy().orElseThrow().toString()) && original.equals(analysis.source().text()), "original preservation");
        check(!result.originalId().equals(result.generatedId()), "different source identities");
        check(result.generatedId().equals(SourceSnapshot.hash(bytes(result.generatedSource()))), "generated hash");
        for (var site : result.sites()) {
            String generated = result.generatedSource().substring(site.generated().startOffset(), site.generated().endOffset());
            check(generated.startsWith(result.helperName() + "."), "generated call association");
            check(!analysis.source().slice(site.original()).isBlank(), "original site association");
        }
        check(transformer.transform(analysis, bytes(original + " "), 32).diagnostic().equals("STALE_SOURCE"), "stale source rejected");
        for (String unsupported : List.of(original.replace("values[1];", "missing[1];"),
                original.replace("values[0] = values[1];", "values[0] = values[1]++;"), "public class Main {}")) {
            var rejected = transformer.transform(analyzer.analyze(bytes(unsupported)), bytes(unsupported), 32);
            check(!rejected.ready() && rejected.generatedSource() == null, "incomplete/side-effecting inputs rejected");
        }
        check(!transformer.transform(analysis, bytes(original), 0).ready(), "event limit rejected");
        String large = original.replace("public class", "/*" + "x".repeat(62_000) + "*/\npublic class");
        var largeAnalysis = analyzer.analyze(bytes(large));
        check(largeAnalysis.eligible(), "large original remains eligible");
        check(transformer.transform(largeAnalysis, bytes(large), 32).diagnostic().equals("GENERATED_SOURCE_LIMIT"), "generated size rejected");
        System.out.println("PASS transformation guards: preservation, source hashes, site associations, stale/incomplete/side-effecting handoffs and limits");

        String renamed = original.replace("values", "items").replace("{3, 1}", "{9, -4}");
        String formatted = original.replace("values", "café").replace("{3, 1}", "{9, -4}")
            .replace("        int[]", "\t/* 😀 */ int[]")
            .replace("café[0] = café[1];", "café[\n 0] =\n café[\n 1];")
            .replace("\r\n", "\n").replace("\n", "\r\n");
        String collision = original.replace("values", "__CodevizRecorder").replace("String[] args", "String[] __CodevizRecorder1");
        List<Example> examples = new ArrayList<>(List.of(new Example("success", original, 32), new Example("renamed", renamed, 32),
            new Example("formatted", formatted, 32), new Example("collision", collision, 32),
            new Example("failed-read", original.replace("values[0] = values[1];", "values[0] = values[5];"), 32),
            new Example("failed-write", original.replace("values[0] = values[1];", "values[5] = values[1];"), 32),
            new Example("empty", original.replace("{3, 1}", "{}"), 32),
            new Example("int-bounds", original.replace("{3, 1}", "{-2147483648, 2147483647}"), 32),
            new Example("trace-limit", original, 2)));
        String integer = "public class Main { public static void main(String[] args) { int x = 5; x = 8; System.out.print(\"FINAL=\" + x); System.err.print(\"PROBE\"); } }";
        var integerFacts = analyzer.analyze(bytes(integer));
        check(integerFacts.eligible() && integerFacts.completeness() == ArrayAnalyzer.Completeness.COMPLETE_FOR_INT_PROBE, "scalar completeness");
        check(integerFacts.sites().read() == null && integerFacts.bindings().stream().anyMatch(b -> b.id().equals(integerFacts.sites().bindingId()) && b.type().equals("int")), "scalar resolved binding");
        String integerAst = integerFacts.syntaxCopy().orElseThrow().toString();
        var integerGenerated = transformer.transform(integerFacts, bytes(integer), 32);
        check(integerGenerated.ready() && integerAst.equals(integerFacts.syntaxCopy().orElseThrow().toString()), "scalar original preserved");
        check(transformer.transform(integerFacts, bytes(integer + " "), 32).diagnostic().equals("STALE_SOURCE"), "scalar stale guard");
        for (String bad : List.of(integer.replace("int x", "final int x"), integer.replace("x = 8", "missing = 8"),
            integer.replace("x = 8", "args = 8"), integer.replace("x = 8", "x += 8"), integer.replace("x = 8", "x = x++"),
            integer.replace("x = 8", "x = 2147483648"), integer.replace("int x = 5", "int x"),
            integer.replace("int x = 5", "long x = 5"), integer.replace("x = 8", "int[] a = {1}; x = 8"))) {
            var badFacts = analyzer.analyze(bytes(bad));
            check(!badFacts.eligible() && !transformer.transform(badFacts, bytes(bad), 32).ready(), "unsupported scalar rejected");
        }
        var unresolved = analyzer.analyze(bytes(integer.replace("x = 8", "missing = 8")));
        check(unresolved.diagnostics().stream().anyMatch(d -> d.category() == ArrayAnalyzer.Category.UNRESOLVED), "scalar unresolved diagnostic");
        examples.addAll(List.of(new Example("int-success", integer, 32),
            new Example("int-renamed", integer.replace("x", "counter").replace("= 5", "= -3").replace("= 8", "= -9"), 32),
            new Example("int-formatted", integer.replace("x", "café").replace("int café", "\r\n\t/* 😀 */ int café").replace("café = 8", "café\r\n = 8"), 32),
            new Example("int-collision", integer.replace("x", "__CodevizRecorder").replace("args", "__CodevizRecorder1"), 32),
            new Example("int-bounds-scalar", integer.replace("= 5", "= -2147483648").replace("= 8", "= 2147483647"), 32),
            new Example("int-repeat", integer.replace("= 8", "= 5"), 32),
            new Example("int-no-probe", "public class Main { public static void main(String[] args) { int x = 5; x = 8; } }", 32),
            new Example("int-limit", integer, 1)));
        System.out.println("PASS integer analysis/transformation: binding/type, preservation, stale source, nine unsupported forms and unresolved diagnostic");
        String mixed = "public class Main { public static void main(String[] args) { int x = 3; int[] values = {5, 2}; values[0] = x; System.out.print(\"FINAL=\" + x + \",\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";
        var mixedFacts = analyzer.analyze(bytes(mixed));
        check(mixedFacts.eligible() && mixedFacts.completeness() == ArrayAnalyzer.Completeness.COMPLETE_FOR_COMBINED_PROBE, "combined completeness: " + mixedFacts.diagnostics());
        check(!mixedFacts.sites().bindingId().equals(mixedFacts.sites().arrayBindingId()) && mixedFacts.sites().scalarReference() != null, "distinct resolved bindings");
        String mixedAst = mixedFacts.syntaxCopy().orElseThrow().toString();
        check(transformer.transform(mixedFacts, bytes(mixed), 32).ready(), "combined transform");
        check(mixedAst.equals(mixedFacts.syntaxCopy().orElseThrow().toString()), "combined original preserved");
        check(transformer.transform(mixedFacts, bytes(mixed + " "), 32).diagnostic().equals("STALE_SOURCE"), "combined stale rejected");
        // Fault injection stays in trusted test code; production facts remain privately constructed and immutable.
        var constructor = ArrayAnalyzer.Result.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        for (String missingId : List.of(mixedFacts.sites().bindingId(), mixedFacts.sites().arrayBindingId())) {
            var missingFacts = (ArrayAnalyzer.Result) constructor.newInstance(mixedFacts.source(), mixedFacts.syntaxCopy().orElseThrow(),
                mixedFacts.bindings().stream().filter(b -> !b.id().equals(missingId)).toList(), mixedFacts.scopes(), mixedFacts.accesses(),
                mixedFacts.diagnostics(), mixedFacts.entry(), mixedFacts.sites(), mixedFacts.completeness());
            check(!transformer.transform(missingFacts, bytes(mixed), 32).ready(), "combined missing binding rejected");
        }
        var missingAccess = (ArrayAnalyzer.Result) constructor.newInstance(mixedFacts.source(), mixedFacts.syntaxCopy().orElseThrow(),
            mixedFacts.bindings(), mixedFacts.scopes(), List.of(), mixedFacts.diagnostics(), mixedFacts.entry(), mixedFacts.sites(), mixedFacts.completeness());
        check(transformer.transform(missingAccess, bytes(mixed), 32).diagnostic().equals("INCONSISTENT_BINDING"), "combined missing target fact rejected");
        for (String bad : List.of(
            mixed.replace("int x = 3; int[] values = {5, 2};", "int[] values = {5, 2}; int x = 3;"),
            mixed.replace("int x = 3", "final int x = 3"), mixed.replace("int[] values", "final int[] values"),
            mixed.replace("int x = 3", "int x"), mixed.replace("int x = 3", "long x = 3"),
            mixed.replace("values[0] = x", "values[x] = x"), mixed.replace("values[0] = x", "values[0] = x++"),
            mixed.replace("values[0] = x", "values[0] = Math.abs(x)"), mixed.replace("values[0] = x", "values[0] = args"),
            mixed.replace("values[0] = x", "values[0] = values"), mixed.replace("values[0] = x", "values[0] = missing"),
            mixed.replace("values[0] = x", "missing[0] = x"), mixed.replace("values[0] = x", "x = 8; x = 9; values[0] = x"),
            mixed.replace("values[0] = x", "int[] alias = values; alias[0] = x"),
            mixed.replace("args", "java"), mixed.replace("values", "System"),
            mixed.replace("{5, 2}", "{" + String.join(",", Collections.nCopies(17, "1")) + "}"),
            mixed + " ".repeat(65_536))) {
            var badFacts = analyzer.analyze(bytes(bad));
            check(!badFacts.eligible() && !transformer.transform(badFacts, bytes(bad), 32).ready(), "combined unsupported rejected");
        }
        for (String bad : List.of(mixed.replace("values[0] = x", "values[0] = missing"), mixed.replace("values[0] = x", "missing[0] = x")))
            check(analyzer.analyze(bytes(bad)).diagnostics().stream().anyMatch(d -> d.category() == ArrayAnalyzer.Category.UNRESOLVED), "combined unresolved diagnostic");
        examples.addAll(List.of(new Example("mix-success", mixed, 32),
            new Example("mix-renamed", mixed.replace("x", "counter").replace("values", "items").replace("= 3", "= -7").replace("{5, 2}", "{9, -4}").replace("items[0]", "items[1]"), 32),
            new Example("mix-bounds", mixed.replace("= 3", "= -2147483648").replace("{5, 2}", "{2147483647, -2147483648}"), 32),
            new Example("mix-repeat", mixed.replace("{5, 2}", "{3, 3}"), 32),
            new Example("mix-formatted", mixed.replace("x", "compteur").replace("values", "caf\u00e9").replace("int compteur", "\r\n\t/* \ud83d\ude00 */ int compteur").replace("caf\u00e9[0] = compteur", "caf\u00e9[\r\n0] =\r\ncompteur"), 32),
            new Example("mix-collision", mixed.replace("x", "__CodevizRecorder").replace("values", "__CodevizRecorder1").replace("args", "__CodevizRecorder2"), 32),
            new Example("mix-no-probe", mixed.substring(0, mixed.indexOf("System.out")) + "} }", 32),
            new Example("mix-one", mixed.replace("{5, 2}", "{5}"), 32),
            new Example("mix-sixteen", mixed.replace("{5, 2}", "{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}"), 32),
            new Example("mix-negative", mixed.replace("values[0]", "values[-1]"), 32),
            new Example("mix-oob", mixed.replace("values[0]", "values[2]"), 32),
            new Example("mix-empty", mixed.replace("{5, 2}", "{}"), 32),
            new Example("mix-limit1", mixed, 1), new Example("mix-limit2", mixed, 2)));
        System.out.println("PASS combined analysis/transformation: distinct bindings, preservation, stale/missing facts, 18 unsupported forms and unresolved diagnostics");
        String update = mixed.replace("values[0] = x;", "x = 8; values[0] = x;");
        var updateFacts = analyzer.analyze(bytes(update));
        check(updateFacts.eligible() && updateFacts.sites().scalarWriteBindingId().equals(updateFacts.sites().bindingId()), "update target binding");
        String updateAst = updateFacts.syntaxCopy().orElseThrow().toString();
        var updateGenerated = transformer.transform(updateFacts, bytes(update), 32);
        check(updateGenerated.ready() && updateGenerated.sites().size() == 4, "four update sites");
        check(updateAst.equals(updateFacts.syntaxCopy().orElseThrow().toString()), "update original preserved");
        check(transformer.transform(updateFacts, bytes(update + " "), 32).diagnostic().equals("STALE_SOURCE"), "update stale rejected");
        var sites = updateFacts.sites();
        for (var brokenSites : List.of(
            new ArrayAnalyzer.Sites(sites.declaration(), null, sites.write(), sites.bindingId(), sites.kind(), sites.arrayDeclaration(), sites.arrayBindingId(), sites.scalarReference(), null, null),
            new ArrayAnalyzer.Sites(sites.declaration(), null, sites.write(), sites.bindingId(), sites.kind(), sites.arrayDeclaration(), sites.arrayBindingId(), sites.scalarReference(), sites.scalarWrite(), sites.arrayBindingId()),
            new ArrayAnalyzer.Sites(sites.declaration(), null, sites.write(), sites.bindingId(), sites.kind(), sites.arrayDeclaration(), sites.arrayBindingId(), sites.scalarReference(), sites.write(), sites.bindingId()))) {
            var broken = (ArrayAnalyzer.Result) constructor.newInstance(updateFacts.source(), updateFacts.syntaxCopy().orElseThrow(), updateFacts.bindings(),
                updateFacts.scopes(), updateFacts.accesses(), updateFacts.diagnostics(), updateFacts.entry(), brokenSites, updateFacts.completeness());
            check(!transformer.transform(broken, bytes(update), 32).ready(), "missing/wrong scalar update fact rejected");
        }
        for (String missingId : List.of(sites.bindingId(), sites.arrayBindingId())) {
            var broken = (ArrayAnalyzer.Result) constructor.newInstance(updateFacts.source(), updateFacts.syntaxCopy().orElseThrow(),
                updateFacts.bindings().stream().filter(b -> !b.id().equals(missingId)).toList(), updateFacts.scopes(), updateFacts.accesses(),
                updateFacts.diagnostics(), updateFacts.entry(), sites, updateFacts.completeness());
            check(!transformer.transform(broken, bytes(update), 32).ready(), "missing update binding rejected");
        }
        for (String bad : List.of(update.replace("x = 8;", "missing = 8;"), update.replace("x = 8;", "values = 8;"),
            update.replace("x = 8;", "args = 8;"), update.replace("x = 8;", "x += 8;"), update.replace("x = 8;", "x = x;"),
            update.replace("x = 8;", "x = x++;"), update.replace("x = 8;", "x = 4 + 4;"), update.replace("x = 8;", "x = Math.abs(8);"),
            update.replace("x = 8;", "x = values[0];"), update.replace("x = 8;", "x = 2147483648;"),
            update.replace("x = 8;", "x = 8; x = 9;"), update.replace("x = 8; values[0] = x;", "values[0] = x; x = 8;"),
            update.replace("values[0] = x;", "values[x] = x;"), update.replace("values[0] = x;", "values[0] = missing;"),
            update.replace("int x = 3", "final int x = 3"), update.replace("int[] values", "final int[] values"),
            update.replace("int x = 3", "long x = 3"), update.replace("int x = 3", "int x"),
            update.replace("{5, 2}", "{" + String.join(",", Collections.nCopies(17, "1")) + "}"), update + " ".repeat(65_536))) {
            var facts = analyzer.analyze(bytes(bad));
            check(!facts.eligible() && !transformer.transform(facts, bytes(bad), 32).ready(), "unsupported update rejected");
        }
        check(analyzer.analyze(bytes(update.replace("x = 8;", "missing = 8;"))).diagnostics().stream()
            .anyMatch(d -> d.category() == ArrayAnalyzer.Category.UNRESOLVED), "update unresolved diagnostic");
        examples.addAll(List.of(new Example("update-success", update, 32),
            new Example("update-renamed", update.replace("x", "counter").replace("values", "items").replace("= 3", "= -7").replace("= 8", "= -9").replace("{5, 2}", "{9, -4}").replace("items[0]", "items[1]"), 32),
            new Example("update-bounds", update.replace("= 3", "= -2147483648").replace("= 8", "= 2147483647").replace("{5, 2}", "{-2147483648, 2147483647}"), 32),
            new Example("update-repeat", update.replace("= 8", "= 3"), 32),
            new Example("update-formatted", update.replace("x", "compteur").replace("values", "caf\u00e9").replace("int compteur", "\r\n\t/* \ud83d\ude00 */ int compteur").replace("compteur = 8", "compteur\r\n = 8").replace("caf\u00e9[0]", "caf\u00e9[\r\n0]"), 32),
            new Example("update-collision", update.replace("x", "__CodevizRecorder").replace("values", "__CodevizRecorder1").replace("args", "__CodevizRecorder2"), 32),
            new Example("update-no-probe", update.substring(0, update.indexOf("System.out")) + "} }", 32),
            new Example("update-one", update.replace("{5, 2}", "{5}"), 32),
            new Example("update-sixteen", update.replace("{5, 2}", "{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}"), 32),
            new Example("update-negative", update.replace("values[0]", "values[-1]"), 32),
            new Example("update-oob", update.replace("values[0]", "values[2]"), 32),
            new Example("update-empty", update.replace("{5, 2}", "{}"), 32),
            new Example("update-limit1", update, 1), new Example("update-limit2", update, 2), new Example("update-limit3", update, 3),
            new Example("update-limit4", update, 4)));
        System.out.println("PASS scalar-update analysis: resolved target, four sites, stale/missing/wrong facts, preservation, 20 rejected forms");
        for (var example : IndexAcceptance.run()) examples.add(new Example(example.name(), example.source(), example.limit()));
        for (var example : IndexUpdateAcceptance.run()) examples.add(new Example(example.name(), example.source(), example.limit()));
        export(examples);
        System.out.println("PASS: 88 automatic transformation fixtures generated inside Docker (66 previous, 22 index-update)");
    }
    public static void runAddition() throws Exception {
        var examples = new ArrayList<>(IndexAdditionAcceptance.run());
        examples.addAll(IndexIncrementAcceptance.run());
        export(examples.stream().map(e -> new Example(e.name(), e.source(), e.limit())).toList());
        System.out.println("PASS: 62 indexed-update fixtures generated inside Docker (22 addition, 40 increment)");
    }
    private static void export(List<Example> examples) {
        var analyzer = new ArrayAnalyzer(); var transformer = new ArrayTransformer();
        for (Example example : examples) {
            var facts = analyzer.analyze(bytes(example.source()));
            var generated = transformer.transform(facts, bytes(example.source()), example.limit());
            check(generated.ready(), example.name() + ": " + generated.diagnostic() + " " + facts.diagnostics());
            if (facts.sites().scalarWrite() != null && facts.sites().index() == null) {
                var generatedMain = new com.github.javaparser.JavaParser().parse(generated.generatedSource()).getResult().orElseThrow()
                    .getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst().getBody().orElseThrow();
                var statements = generatedMain.getStatements();
                check(statements.get(3).toString().contains(".beforeOperation()") && statements.get(4).toString().contains("int[]"), "guard before array initialization");
                check(statements.get(6).toString().contains(".beforeVariableWrite()")
                    && statements.get(7).asExpressionStmt().getExpression().isAssignExpr()
                    && statements.get(8).toString().contains(".variableWrite(")
                    && statements.get(9).toString().contains(".write("), "guard, actual assignment, capture, array store order");
            }
            boolean combined = facts.sites().kind() == ArrayAnalyzer.ProbeKind.COMBINED;
            String arrayName = combined ? facts.bindings().stream().filter(b -> b.id().equals(facts.sites().arrayBindingId())).findFirst().orElseThrow().name() : null;
            String indexName = facts.sites().index() == null ? null : facts.bindings().stream().filter(b -> b.id().equals(facts.sites().index().bindingId())).findFirst().orElseThrow().name();
            if (indexName != null) {
                var statements = new com.github.javaparser.JavaParser().parse(generated.generatedSource()).getResult().orElseThrow()
                    .getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst().getBody().orElseThrow().getStatements();
                check(statements.get(3).toString().contains(".beforeOperation()") && statements.get(6).toString().contains(".beforeOperation()"), "index initialization guards");
                check(statements.get(7).asExpressionStmt().getExpression().isVariableDeclarationExpr()
                    && statements.get(8).toString().contains(".variableDeclare("), "index capture follows initialization");
                boolean indexUpdate = facts.sites().scalarWrite() != null;
                if (indexUpdate) {
                    check(statements.get(9).toString().contains(".beforeVariableWrite()"), "index update guarded before mutation");
                    var operation = statements.get(10).asExpressionStmt().getExpression();
                    if (facts.sites().increment() != null) {
                        var increment = operation.asUnaryExpr();
                        check(increment.getOperator().name().equals(facts.sites().increment().operator())
                            && increment.getExpression().asNameExpr().getNameAsString().equals(indexName), "original increment retained");
                        check(statements.stream().flatMap(st -> st.findAll(com.github.javaparser.ast.expr.UnaryExpr.class).stream())
                            .filter(u -> u.getOperator() == com.github.javaparser.ast.expr.UnaryExpr.Operator.PREFIX_INCREMENT
                                || u.getOperator() == com.github.javaparser.ast.expr.UnaryExpr.Operator.POSTFIX_INCREMENT).count() == 1, "exactly one increment evaluation");
                    } else {
                        var actualAssignment = operation.asAssignExpr();
                        check(actualAssignment.getTarget().asNameExpr().getNameAsString().equals(indexName), "actual index assignment retained");
                        if (facts.sites().addition() != null) {
                            var originalAssignment = facts.syntaxCopy().orElseThrow().getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst()
                                .getBody().orElseThrow().getStatement(3).asExpressionStmt().getExpression().asAssignExpr();
                            check(actualAssignment.getValue().equals(originalAssignment.getValue()), "original addition retained without substitution");
                            check(statements.stream().flatMap(st -> st.findAll(com.github.javaparser.ast.expr.BinaryExpr.class).stream())
                                .filter(b -> b.getLeft().isNameExpr() && b.getLeft().asNameExpr().getNameAsString().equals(indexName)).count() == 1, "one index addition evaluation");
                        }
                    }
                    var capture = statements.get(11).asExpressionStmt().getExpression().asMethodCallExpr();
                    check(capture.getNameAsString().equals("variableWrite") && capture.getArgument(0).asNameExpr().getNameAsString().equals(indexName)
                        && capture.getArgument(2).asStringLiteralExpr().asString().equals("variable-3"), "committed index value captured with its identity");
                }
                var write = statements.get(indexUpdate ? 12 : 9).asExpressionStmt().getExpression().asMethodCallExpr();
                check(write.getNameAsString().equals("write") && write.getArgument(1).isNameExpr() && write.getArgument(1).asNameExpr().getNameAsString().equals(indexName)
                    && write.getArgument(2).asNameExpr().getNameAsString().equals(generated.variableName()), "runtime index/RHS preserved");
                if (example.name().endsWith("-collision")) check(generated.helperName().equals("__CodevizRecorder4"), "index helper collision");
            }
            if (example.name().equals("mix-collision")) check(generated.helperName().equals("__CodevizRecorder3"), "combined helper collision");
            if (example.name().equals("collision")) check(generated.helperName().equals("__CodevizRecorder2"), "helper collision avoided");
            String metadata = "{\"name\":" + ArrayTransformer.json(example.name())
                + ",\"variableName\":" + ArrayTransformer.json(generated.variableName())
                + (combined ? ",\"arrayName\":" + ArrayTransformer.json(arrayName) : "")
                + (indexName == null ? "" : ",\"indexName\":" + ArrayTransformer.json(indexName))
                + ",\"sourceId\":" + ArrayTransformer.json(generated.originalId())
                + ",\"generatedId\":" + ArrayTransformer.json(generated.generatedId()) + ",\"sites\":["
                + String.join(",", generated.sites().stream().map(s -> "{\"kind\":" + ArrayTransformer.json(s.kind())
                    + ",\"original\":" + ArrayTransformer.rangeJson(s.original())
                    + ",\"generated\":" + ArrayTransformer.rangeJson(s.generated()) + "}").toList()) + "]}";
            // Fixed field count and base64 prevent source text from becoming a host command or path.
            System.out.println("ARTIFACT " + example.name() + " " + encode(example.source()) + " " + encode(generated.generatedSource())
                + " " + encode(generated.variableName() + (combined ? "\n" + arrayName : "") + (indexName == null ? "" : "\n" + indexName)) + " " + encode(ArrayTransformer.rangeJson(facts.sites().declaration()))
                + " " + encode(combined ? ArrayTransformer.rangeJson(facts.sites().arrayDeclaration()) : facts.sites().read() == null ? "null" : ArrayTransformer.rangeJson(facts.sites().read())) + " " + encode(ArrayTransformer.rangeJson(facts.sites().write()))
                + " " + encode(metadata) + " " + encode(facts.sites().scalarWrite() == null ? "null" : ArrayTransformer.rangeJson(facts.sites().scalarWrite()))
                + " " + encode(indexName == null ? "null" : ArrayTransformer.rangeJson(facts.sites().index().declaration())));
        }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static String encode(String value) { return Base64.getEncoder().encodeToString(bytes(value)); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
