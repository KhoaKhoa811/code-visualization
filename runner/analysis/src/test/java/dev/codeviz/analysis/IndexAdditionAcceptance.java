package dev.codeviz.analysis;

import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Trusted worker checks; neither source analysis nor submitted execution runs on the host. */
final class IndexAdditionAcceptance {
    static List<IndexAcceptance.Example> run() throws Exception {
        String source = "public class Main { public static void main(String[] args) { int x = 8; int[] values = {5, 2}; int i = 0; i = i + 1; values[i] = x; System.out.print(\"FINAL=\" + x + \",\" + i + \",\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";
        var analyzer = new ArrayAnalyzer(); var transformer = new ArrayTransformer();
        var facts = analyzer.analyze(bytes(source));
        check(facts.eligible(), "addition eligibility: " + facts.diagnostics());
        var s = facts.sites(); var a = s.addition();
        check(a != null && a.bindingId().equals(s.index().bindingId()) && a.bindingId().equals(s.scalarWriteBindingId())
            && a.operator().equals("PLUS") && a.resultType().equals("int"), "resolved addition facts");
        check(facts.source().slice(a.expression()).equals("i + 1") && facts.source().slice(a.reference()).equals("i")
            && facts.source().slice(a.literal()).equals("1"), "exact operand source spans");
        String ast = facts.syntaxCopy().orElseThrow().toString();
        var generated = transformer.transform(facts, bytes(source), 32);
        check(generated.ready() && generated.sites().size() == 5, "five addition sites");
        check(ast.equals(facts.syntaxCopy().orElseThrow().toString()) && source.equals(facts.source().text()), "original preserved");
        check(transformer.transform(facts, bytes(source + " "), 32).diagnostic().equals("STALE_SOURCE"), "stale addition source");
        var constructor = ArrayAnalyzer.Result.class.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        for (var broken : Arrays.asList(null,
            new ArrayAnalyzer.AdditionSites(s.scalarWrite(), a.reference(), a.literal(), a.bindingId(), "PLUS", "int"),
            new ArrayAnalyzer.AdditionSites(a.expression(), s.index().reference(), a.literal(), a.bindingId(), "PLUS", "int"),
            new ArrayAnalyzer.AdditionSites(a.expression(), a.reference(), a.reference(), a.bindingId(), "PLUS", "int"),
            new ArrayAnalyzer.AdditionSites(a.expression(), a.reference(), a.literal(), s.bindingId(), "PLUS", "int"),
            new ArrayAnalyzer.AdditionSites(a.expression(), a.reference(), a.literal(), a.bindingId(), "MINUS", "int"),
            new ArrayAnalyzer.AdditionSites(a.expression(), a.reference(), a.literal(), a.bindingId(), "PLUS", "long"))) {
            var sites = new ArrayAnalyzer.Sites(s.declaration(), s.read(), s.write(), s.bindingId(), s.kind(), s.arrayDeclaration(), s.arrayBindingId(),
                s.scalarReference(), s.scalarWrite(), s.scalarWriteBindingId(), s.index(), broken);
            var badFacts = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(), facts.bindings(),
                facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), sites, facts.completeness());
            check(!transformer.transform(badFacts, bytes(source), 32).ready(), "missing/swapped addition facts rejected");
        }
        var missingBinding = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(),
            facts.bindings().stream().filter(b -> !b.id().equals(a.bindingId())).toList(), facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), s, facts.completeness());
        check(!transformer.transform(missingBinding, bytes(source), 32).ready(), "missing resolved operand binding rejected");
        var rejected = List.of("i = x + 1", "x = i + 1", "i = missing + 1", "missing = i + 1", "i = 1 + i", "i = i - 1",
            "i = i * 1", "i = i / 1", "i = i + 1 + 1", "i = (i + 1)", "i = (i) + 1", "i = i + (1)", "i = (int)i + 1",
            "i = i + 1L", "i = i + 1.0", "i = i + \"1\"", "i = i + +1", "i = i + 0x1", "i = i + 01", "i = i + 1_0",
            "i = i + 2147483648", "i = i + -2147483649", "i = i + x", "i = i++ + 1", "i = ++i + 1",
            "i = i + Math.abs(1)", "i = i + values[0]", "i += 1", "i--", "i = i + 1; i = i + 1");
        for (String assignment : rejected) {
            String bad = source.replace("i = i + 1", assignment);
            var badFacts = analyzer.analyze(bytes(bad));
            check(!badFacts.eligible() && !transformer.transform(badFacts, bytes(bad), 32).ready(), "unsupported addition: " + assignment);
        }
        check(analyzer.analyze(bytes(source.replace("i = i + 1", "i = missing + 1"))).diagnostics().stream()
            .anyMatch(d -> d.category() == ArrayAnalyzer.Category.UNRESOLVED), "unresolved operand diagnostic");
        System.out.println("PASS index-addition analysis: int/binding/operator/operand spans, preservation, stale/missing/swapped facts and " + rejected.size() + " rejected forms");
        return List.of(example("success", source, 32),
            example("renamed", source.replace("x", "amount").replace("values", "items").replaceAll("\\bi\\b", "position")
                .replace("= 8", "= -9").replace("{5, 2}", "{4, 7}").replace("int position = 0", "int position = 1").replace("position + 1", "position + -1"), 32),
            example("equal", source.replace("= 8", "= 1"), 32),
            example("zero", source.replace("int i = 0", "int i = 1").replace("i + 1", "i + 0"), 32),
            example("step", source.replace("int i = 0", "int i = -1").replace("i + 1", "i + 2"), 32),
            example("negative", source.replace("i + 1", "i + -1"), 32), example("oob", source.replace("i + 1", "i + 2"), 32),
            example("empty", source.replace("{5, 2}", "{}"), 32),
            example("overflow-max", source.replace("int i = 0", "int i = 2147483647"), 32),
            example("overflow-min", source.replace("int i = 0", "int i = -2147483648").replace("i + 1", "i + -1"), 32),
            example("wrap-zero", source.replace("int i = 0", "int i = -2147483648").replace("i + 1", "i + -2147483648"), 32),
            example("invalid-initial", source.replace("int i = 0", "int i = -1"), 32),
            example("first", source.replace("{5, 2}", "{5}").replace("int i = 0", "int i = 1").replace("i + 1", "i + -1"), 32),
            example("sixteen", source.replace("{5, 2}", "{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}").replace("i + 1", "i + 15"), 32),
            example("formatted", source.replace("values", "caf\u00e9").replaceAll("\\bi\\b", "position").replace("int position = 0", "\r\n\t/* \ud83d\ude00 */ int position = 0")
                .replace("position = position + 1", "position\r\n = position /* operand */\r\n + 1"), 32),
            example("collision", source.replace("x", "__CodevizRecorder").replace("values", "__CodevizRecorder1").replaceAll("\\bi\\b", "__CodevizRecorder2").replace("args", "__CodevizRecorder3"), 32),
            example("no-probe", source.substring(0, source.indexOf("System.out")) + "} }", 32),
            example("limit1", source, 1), example("limit2", source, 2), example("limit3", source, 3), example("limit4", source, 4), example("limit5", source, 5));
    }
    private static IndexAcceptance.Example example(String name, String source, int limit) { return new IndexAcceptance.Example("index-add-" + name, source, limit); }
    private static byte[] bytes(String source) { return source.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
