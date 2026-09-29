package dev.codeviz.analysis;

import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Trusted acceptance checks, run only in the bounded analysis worker. */
final class IndexAcceptance {
    record Example(String name, String source, int limit) {}
    static List<Example> run() throws Exception {
        String source = "public class Main { public static void main(String[] args) { int x = 8; int[] values = {5, 2}; int i = 1; values[i] = x; System.out.print(\"FINAL=\" + x + \",\" + i + \",\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";
        var analyzer = new ArrayAnalyzer(); var transformer = new ArrayTransformer();
        var facts = analyzer.analyze(bytes(source));
        check(facts.eligible() && facts.sites().index() != null, "index eligible: " + facts.diagnostics());
        var sites = facts.sites();
        check(Set.of(sites.bindingId(), sites.arrayBindingId(), sites.index().bindingId()).size() == 3, "three distinct bindings");
        String ast = facts.syntaxCopy().orElseThrow().toString();
        var generated = transformer.transform(facts, bytes(source), 32);
        check(generated.ready() && generated.sites().size() == 4, "four index sites");
        check(generated.sites().stream().filter(s -> s.kind().equals("VARIABLE_DECLARE")).map(ArrayTransformer.Site::original).distinct().count() == 2, "distinct declaration associations");
        check(ast.equals(facts.syntaxCopy().orElseThrow().toString()), "index original preserved");
        check(transformer.transform(facts, bytes(source + " "), 32).diagnostic().equals("STALE_SOURCE"), "index stale rejected");
        var constructor = ArrayAnalyzer.Result.class.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        for (String id : List.of(sites.bindingId(), sites.arrayBindingId(), sites.index().bindingId())) {
            var broken = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(), facts.bindings().stream().filter(b -> !b.id().equals(id)).toList(),
                facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), sites, facts.completeness());
            check(!transformer.transform(broken, bytes(source), 32).ready(), "missing index binding rejected");
        }
        for (var index : Arrays.asList(null, new ArrayAnalyzer.IndexSites(sites.index().declaration(), sites.bindingId(), sites.index().reference()),
            new ArrayAnalyzer.IndexSites(sites.declaration(), sites.index().bindingId(), sites.index().reference()),
            new ArrayAnalyzer.IndexSites(sites.index().declaration(), sites.index().bindingId(), sites.scalarReference()))) {
            var brokenSites = new ArrayAnalyzer.Sites(sites.declaration(), sites.read(), sites.write(), sites.bindingId(), sites.kind(), sites.arrayDeclaration(),
                sites.arrayBindingId(), sites.scalarReference(), null, null, index);
            var broken = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(), facts.bindings(),
                facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), brokenSites, facts.completeness());
            check(!transformer.transform(broken, bytes(source), 32).ready(), "missing/swapped index sites rejected");
        }
        var noAccess = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(), facts.bindings(), facts.scopes(),
            List.of(), facts.diagnostics(), facts.entry(), sites, facts.completeness());
        check(!transformer.transform(noAccess, bytes(source), 32).ready(), "missing index receiver fact rejected");
        List<String> invalid = List.of(source.replace("values[i]", "values[x]"), source.replace("values[i] = x", "values[i] = i"),
            source.replace("values[i]", "values[missing]"), source.replace("values[i] = x", "missing[i] = x"), source.replace("values[i] = x", "values[i] = missing"),
            source.replace("int i = 1", "long i = 1"), source.replace("int i = 1", "final int i = 1"), source.replace("int i = 1", "int i"),
            source.replace("int i = 1", "int i = 2147483648"), source.replace("values[i]", "values[i++]"), source.replace("values[i]", "values[i + 1]"),
            source.replace("values[i]", "values[Math.abs(i)]"), source.replace("values[i] = x", "i += 0; values[i] = x"),
            source.replace("values[i] = x", "x = 9; values[i] = x"), source.replace("values[i] = x", "values[i] = values[0]"),
            source.replace("int x = 8;", "int x = 8; int z = 0;"), source.replace("{5, 2}", "null"), source.replace("{5, 2}", "new int[2]"),
            source.replace("int[] values = {5, 2}; int i = 1;", "int i = 1; int[] values = {5, 2};"),
            source.replace("{5, 2}", "{" + String.join(",", Collections.nCopies(17, "1")) + "}"), source + " ".repeat(65536));
        for (String bad : invalid) {
            var badFacts = analyzer.analyze(bytes(bad));
            check(!badFacts.eligible() && !transformer.transform(badFacts, bytes(bad), 32).ready(), "index unsupported rejected");
        }
        check(analyzer.analyze(bytes(source.replace("values[i]", "values[missing]"))).diagnostics().stream()
            .anyMatch(d -> d.category() == ArrayAnalyzer.Category.UNRESOLVED), "unresolved index diagnostic");
        System.out.println("PASS variable-index analysis: three bindings, repeated-kind sites, stale/missing/swapped facts, original preservation, 21 rejected forms");
        return List.of(new Example("index-success", source, 32),
            new Example("index-renamed", source.replace("x", "amount").replace("values", "items").replace("int i = 1", "int position = 0").replace("[i]", "[position]").replace("+ i +", "+ position +").replace("= 8", "= -9").replace("{5, 2}", "{4, 7}"), 32),
            new Example("index-equal", source.replace("= 8", "= 1"), 32),
            new Example("index-minvalue", source.replace("= 8", "= -2147483648"), 32), new Example("index-maxvalue", source.replace("= 8", "= 2147483647"), 32),
            new Example("index-formatted", source.replace("values", "caf\u00e9").replace("int i = 1", "\r\n\t/* \ud83d\ude00 */ int position\r\n = 1").replace("[i]", "[\r\nposition]").replace("+ i +", "+ position +"), 32),
            new Example("index-collision", source.replace("x", "__CodevizRecorder").replace("values", "__CodevizRecorder1").replace("int i = 1", "int __CodevizRecorder2 = 1").replace("[i]", "[__CodevizRecorder2]").replace("+ i +", "+ __CodevizRecorder2 +").replace("args", "__CodevizRecorder3"), 32),
            new Example("index-no-probe", source.substring(0, source.indexOf("System.out")) + "} }", 32),
            new Example("index-first", source.replace("{5, 2}", "{5}").replace("i = 1", "i = 0"), 32),
            new Example("index-sixteen", source.replace("{5, 2}", "{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}").replace("i = 1", "i = 15"), 32),
            new Example("index-negative", source.replace("i = 1", "i = -1"), 32), new Example("index-oob", source.replace("i = 1", "i = 2"), 32),
            new Example("index-minindex", source.replace("i = 1", "i = -2147483648"), 32), new Example("index-maxindex", source.replace("i = 1", "i = 2147483647"), 32),
            new Example("index-empty", source.replace("{5, 2}", "{}"), 32), new Example("index-limit1", source, 1), new Example("index-limit2", source, 2),
            new Example("index-limit3", source, 3), new Example("index-limit4", source, 4));
    }
    private static byte[] bytes(String source) { return source.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
