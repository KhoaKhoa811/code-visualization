package dev.codeviz.analysis;

import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Standalone increment checks inside the restricted analysis worker. */
final class IndexIncrementAcceptance {
    static List<IndexAcceptance.Example> run() throws Exception {
        List<IndexAcceptance.Example> examples = new ArrayList<>();
        for (boolean prefix : List.of(false, true)) {
            String operation = prefix ? "++i" : "i++", group = prefix ? "index-pre-" : "index-post-";
            String source = "public class Main { public static void main(String[] args) { int x = 8; int[] values = {5, 2}; int i = 0; " + operation
                + "; values[i] = x; System.out.print(\"FINAL=\" + x + \",\" + i + \",\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";
            var analyzer = new ArrayAnalyzer(); var transformer = new ArrayTransformer();
            var facts = analyzer.analyze(bytes(source));
            check(facts.eligible(), group + "eligibility: " + facts.diagnostics());
            var s = facts.sites(); var increment = s.increment();
            String operator = prefix ? "PREFIX_INCREMENT" : "POSTFIX_INCREMENT";
            check(increment != null && s.addition() == null && increment.operator().equals(operator)
                && increment.resultType().equals("int") && increment.bindingId().equals(s.index().bindingId())
                && increment.bindingId().equals(s.scalarWriteBindingId()), "resolved increment facts");
            check(facts.source().slice(increment.expression()).equals(operation)
                && facts.source().slice(increment.operand()).equals("i") && increment.expression().equals(s.scalarWrite()), "exact increment spans");
            String ast = facts.syntaxCopy().orElseThrow().toString();
            check(transformer.transform(facts, bytes(source), 32).ready(), "increment transformation");
            check(ast.equals(facts.syntaxCopy().orElseThrow().toString()) && facts.source().text().equals(source), "original preserved");
            check(transformer.transform(facts, bytes(source + " "), 32).diagnostic().equals("STALE_SOURCE"), "stale source rejected");
            for (var broken : Arrays.asList(null,
                new ArrayAnalyzer.IncrementSites(s.write(), increment.operand(), increment.bindingId(), operator, "int"),
                new ArrayAnalyzer.IncrementSites(increment.expression(), s.index().reference(), increment.bindingId(), operator, "int"),
                new ArrayAnalyzer.IncrementSites(increment.expression(), increment.operand(), s.bindingId(), operator, "int"),
                new ArrayAnalyzer.IncrementSites(increment.expression(), increment.operand(), increment.bindingId(), prefix ? "POSTFIX_INCREMENT" : "PREFIX_INCREMENT", "int"),
                new ArrayAnalyzer.IncrementSites(increment.expression(), increment.operand(), increment.bindingId(), "PREFIX_DECREMENT", "int"),
                new ArrayAnalyzer.IncrementSites(increment.expression(), increment.operand(), increment.bindingId(), operator, "long"))) {
                check(!transformer.transform(withSites(facts, sites(s, null, broken)), bytes(source), 32).ready(), "missing/swapped increment facts rejected");
            }
            var addition = new ArrayAnalyzer.AdditionSites(increment.expression(), increment.operand(), increment.operand(), increment.bindingId(), "PLUS", "int");
            check(!transformer.transform(withSites(facts, sites(s, addition, increment)), bytes(source), 32).ready(), "ambiguous facts rejected");
            for (String replacement : List.of("i = 1", "i = i + 1")) {
                String assigned = source.replace(operation, replacement);
                var assignedFacts = analyzer.analyze(bytes(assigned));
                check(assignedFacts.eligible(), "earlier assignment shape retained");
                check(!transformer.transform(withSites(assignedFacts, sites(assignedFacts.sites(), assignedFacts.sites().addition(), increment)), bytes(assigned), 32).ready(), "increment facts on assignment rejected");
            }
            var constructor = ArrayAnalyzer.Result.class.getDeclaredConstructors()[0]; constructor.setAccessible(true);
            var missingBinding = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(),
                facts.bindings().stream().filter(b -> !b.id().equals(increment.bindingId())).toList(), facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), s, facts.completeness());
            check(!transformer.transform(missingBinding, bytes(source), 32).ready(), "missing operand binding rejected");
            List<String> rejected = new ArrayList<>();
            for (String bad : List.of("x++", "++x", "missing++", "++missing", "i--", "--i", "(i)++", "++(i)", "values[0]++", "++values[0]",
                "Main.i++", "++Main.i", "i = i++", "i = ++i", "x = i++", "x = ++i", "i = i++ + 1", "i = ++i + 1", "System.out.print(i++)",
                "System.out.print(++i)", "i++; ++i", "for (; i < 1; i++) {}")) rejected.add(source.replace(operation, bad));
            for (String type : List.of("long", "Integer", "double", "byte", "final int")) rejected.add(source.replace("int i = 0", type + " i = 0"));
            rejected.add(source.replace("values[i]", "values[i++]"));
            rejected.add(source.replace("values[i]", "values[++i]"));
            rejected.add(source.replace("int i = 0; ", "").replace("public class Main {", "public class Main { static int i = 0;"));
            for (String bad : rejected) {
                var badFacts = analyzer.analyze(bytes(bad));
                check(!badFacts.eligible() && !transformer.transform(badFacts, bytes(bad), 32).ready(), "unsupported increment: " + bad);
                check(badFacts.diagnostics().size() <= ArrayAnalyzer.MAX_DIAGNOSTICS, "bounded diagnostics");
            }
            check(analyzer.analyze(bytes(source.replace(operation, "missing++"))).diagnostics().stream()
                .anyMatch(d -> d.category() == ArrayAnalyzer.Category.UNRESOLVED), "unresolved increment diagnostic");
            examples.addAll(List.of(example(group, "success", source, 32),
                example(group, "renamed", source.replace("x", "amount").replace("values", "items").replaceAll("\\bi\\b", "position")
                    .replace("= 8", "= -9").replace("{5, 2}", "{4, 7}").replace("int position = 0", "int position = -1"), 32),
                example(group, "equal", source.replace("= 8", "= 1").replace("int i = 0", "int i = 1").replace("{5, 2}", "{1, 1, 1}"), 32),
                example(group, "repeat", source.replace("{5, 2}", "{8, 8}"), 32),
                example(group, "negative", source.replace("int i = 0", "int i = -2"), 32),
                example(group, "oob", source.replace("int i = 0", "int i = 1"), 32),
                example(group, "empty", source.replace("{5, 2}", "{}"), 32),
                example(group, "overflow-max", source.replace("int i = 0", "int i = 2147483647"), 32),
                example(group, "overflow-min", source.replace("int i = 0", "int i = -2147483648"), 32),
                example(group, "invalid-initial", source.replace("int i = 0", "int i = -1"), 32),
                example(group, "first", source.replace("{5, 2}", "{5}").replace("int i = 0", "int i = -1"), 32),
                example(group, "sixteen", source.replace("{5, 2}", "{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}").replace("int i = 0", "int i = 14"), 32),
                example(group, "formatted", source.replace(operation, prefix ? "++ /* operand */\r\n i" : "i /* operand */\r\n ++")
                    .replace("values", "caf\u00e9").replaceAll("\\bi\\b", "pos\u00e9").replace("int pos\u00e9 = 0", "\r\n\t/* \ud83d\ude00 */ int pos\u00e9 = 0"), 32),
                example(group, "collision", source.replace("x", "__CodevizRecorder").replace("values", "__CodevizRecorder1").replaceAll("\\bi\\b", "__CodevizRecorder2").replace("args", "__CodevizRecorder3"), 32),
                example(group, "no-probe", source.substring(0, source.indexOf("System.out")) + "} }", 32),
                example(group, "limit1", source, 1), example(group, "limit2", source, 2), example(group, "limit3", source, 3),
                example(group, "limit4", source, 4), example(group, "limit5", source, 5)));
            System.out.println("PASS " + group + "analysis: resolved operator/binding/type/spans, immutable facts, preservation and " + rejected.size() + " rejected forms");
        }
        return List.copyOf(examples);
    }
    private static ArrayAnalyzer.Sites sites(ArrayAnalyzer.Sites s, ArrayAnalyzer.AdditionSites addition, ArrayAnalyzer.IncrementSites increment) {
        return new ArrayAnalyzer.Sites(s.declaration(), s.read(), s.write(), s.bindingId(), s.kind(), s.arrayDeclaration(), s.arrayBindingId(),
            s.scalarReference(), s.scalarWrite(), s.scalarWriteBindingId(), s.index(), addition, increment);
    }
    private static ArrayAnalyzer.Result withSites(ArrayAnalyzer.Result facts, ArrayAnalyzer.Sites sites) throws Exception {
        var constructor = ArrayAnalyzer.Result.class.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        return (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(), facts.bindings(),
            facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), sites, facts.completeness());
    }
    private static IndexAcceptance.Example example(String group, String name, String source, int limit) { return new IndexAcceptance.Example(group + name, source, limit); }
    private static byte[] bytes(String source) { return source.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
