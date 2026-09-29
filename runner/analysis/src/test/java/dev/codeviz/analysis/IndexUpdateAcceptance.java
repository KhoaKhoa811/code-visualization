package dev.codeviz.analysis;

import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Trusted checks for the approved five-operation shape; executed in the analysis worker. */
final class IndexUpdateAcceptance {
    static List<IndexAcceptance.Example> run() throws Exception {
        String source = "public class Main { public static void main(String[] args) { int x = 8; int[] values = {5, 2}; int i = 0; i = 1; values[i] = x; System.out.print(\"FINAL=\" + x + \",\" + i + \",\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";
        var analyzer = new ArrayAnalyzer(); var transformer = new ArrayTransformer();
        var facts = analyzer.analyze(bytes(source));
        check(facts.eligible(), "index update eligible: " + facts.diagnostics());
        var s = facts.sites();
        check(Set.of(s.bindingId(), s.arrayBindingId(), s.index().bindingId()).size() == 3
            && s.scalarWriteBindingId().equals(s.index().bindingId()), "update resolves original index binding");
        String ast = facts.syntaxCopy().orElseThrow().toString();
        var generated = transformer.transform(facts, bytes(source), 32);
        check(generated.ready() && generated.sites().size() == 5, "five distinct index-update sites");
        check(generated.sites().stream().map(ArrayTransformer.Site::original).distinct().count() == 5, "unique original operations");
        check(ast.equals(facts.syntaxCopy().orElseThrow().toString()) && facts.source().text().equals(source), "original preserved");
        check(transformer.transform(facts, bytes(source + " "), 32).diagnostic().equals("STALE_SOURCE"), "stale source rejected");
        var constructor = ArrayAnalyzer.Result.class.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        for (String id : List.of(s.bindingId(), s.arrayBindingId(), s.index().bindingId())) {
            var broken = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(),
                facts.bindings().stream().filter(b -> !b.id().equals(id)).toList(), facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), s, facts.completeness());
            check(!transformer.transform(broken, bytes(source), 32).ready(), "missing binding rejected");
        }
        for (var brokenSites : List.of(
            sites(s, null, null, s.index()), sites(s, s.scalarWrite(), s.bindingId(), s.index()),
            sites(s, s.scalarWrite(), s.arrayBindingId(), s.index()), sites(s, s.write(), s.scalarWriteBindingId(), s.index()),
            sites(s, s.scalarWrite(), s.scalarWriteBindingId(), null),
            sites(s, s.scalarWrite(), s.scalarWriteBindingId(), new ArrayAnalyzer.IndexSites(s.declaration(), s.index().bindingId(), s.index().reference())),
            sites(s, s.scalarWrite(), s.scalarWriteBindingId(), new ArrayAnalyzer.IndexSites(s.index().declaration(), s.bindingId(), s.index().reference())),
            sites(s, s.scalarWrite(), s.scalarWriteBindingId(), new ArrayAnalyzer.IndexSites(s.index().declaration(), s.index().bindingId(), s.scalarReference())))) {
            var broken = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(), facts.bindings(),
                facts.scopes(), facts.accesses(), facts.diagnostics(), facts.entry(), brokenSites, facts.completeness());
            check(!transformer.transform(broken, bytes(source), 32).ready(), "missing/swapped update facts rejected");
        }
        var noAccess = (ArrayAnalyzer.Result) constructor.newInstance(facts.source(), facts.syntaxCopy().orElseThrow(), facts.bindings(),
            facts.scopes(), List.of(), facts.diagnostics(), facts.entry(), s, facts.completeness());
        check(!transformer.transform(noAccess, bytes(source), 32).ready(), "missing receiver fact rejected");
        List<String> invalid = new ArrayList<>();
        for (String assignment : List.of("x = 1", "missing = 1", "values = 1", "args = 1", "i += 1", "i--", "i = i", "i = x",
            "i = i++", "i = 0 + 1", "i = Math.abs(1)", "i = values[0]", "i = 2147483648", "i = 1; i = 0"))
            invalid.add(source.replace("i = 1;", assignment + ";"));
        invalid.addAll(List.of(source.replace("i = 1; values[i] = x;", "values[i] = x; i = 1;"),
            source.replace("int i = 0", "final int i = 0"), source.replace("int i = 0", "long i = 0"), source.replace("int i = 0", "int i"),
            source.replace("values[i]", "values[x]"), source.replace("values[i]", "values[i++]"), source.replace("values[i] = x", "values[i] = i"),
            source.replace("values[i] = x", "values[i] = missing"), source.replace("values[i] = x", "missing[i] = x"),
            source.replace("args", "java"), source.replace("values", "System"),
            source.replace("{5, 2}", "{" + String.join(",", Collections.nCopies(17, "1")) + "}"), source + " ".repeat(65536)));
        for (String bad : invalid) {
            var rejected = analyzer.analyze(bytes(bad));
            check(!rejected.eligible() && !transformer.transform(rejected, bytes(bad), 32).ready(), "unsupported index update rejected: " + bad.substring(0, Math.min(220, bad.length())));
        }
        check(analyzer.analyze(bytes(source.replace("i = 1;", "missing = 1;"))).diagnostics().stream()
            .anyMatch(d -> d.category() == ArrayAnalyzer.Category.UNRESOLVED), "unresolved update diagnostic");
        // The prior four-operation rejection becomes this increment's explicit positive case.
        String newlySupported = source.replace("int i = 0; i = 1;", "int i = 1; i = 0;");
        check(analyzer.analyze(bytes(newlySupported)).eligible(), "previously unsupported literal index update now supported");
        System.out.println("PASS index-update analysis: index target, five sites, preservation, stale/missing/swapped facts and " + invalid.size() + " rejected forms");
        return List.of(example("success", source, 32),
            example("renamed", source.replace("x", "amount").replace("values", "items").replaceAll("\\bi\\b", "position")
                .replace("= 8", "= -9").replace("{5, 2}", "{4, 7}").replace("int position = 0; position = 1;", "int position = 1; position = 0;"), 32),
            example("equal", source.replace("= 8", "= 1"), 32),
            example("minvalue", source.replace("= 8", "= -2147483648"), 32), example("maxvalue", source.replace("= 8", "= 2147483647"), 32),
            example("formatted", source.replace("values", "caf\u00e9").replaceAll("\\bi\\b", "position")
                .replace("int position = 0", "\r\n\t/* \ud83d\ude00 */ int position\r\n = 0").replace("position = 1;", "position\r\n = 1;").replace("[position]", "[\r\nposition]"), 32),
            example("collision", source.replace("x", "__CodevizRecorder").replace("values", "__CodevizRecorder1")
                .replaceAll("\\bi\\b", "__CodevizRecorder2").replace("args", "__CodevizRecorder3"), 32),
            example("no-probe", source.substring(0, source.indexOf("System.out")) + "} }", 32),
            example("first", source.replace("{5, 2}", "{5}").replace("int i = 0; i = 1;", "int i = 1; i = 0;"), 32),
            example("sixteen", source.replace("{5, 2}", "{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}").replace("i = 1;", "i = 15;"), 32),
            example("negative", source.replace("i = 1;", "i = -1;"), 32), example("oob", source.replace("i = 1;", "i = 2;"), 32),
            example("minindex", source.replace("i = 1;", "i = -2147483648;"), 32), example("maxindex", source.replace("i = 1;", "i = 2147483647;"), 32),
            example("empty", source.replace("{5, 2}", "{}"), 32), example("repeat", source.replace("int i = 0;", "int i = 1;"), 32),
            example("invalid-initial", source.replace("int i = 0;", "int i = -2147483648;"), 32),
            example("limit1", source, 1), example("limit2", source, 2), example("limit3", source, 3), example("limit4", source, 4), example("limit5", source, 5));
    }
    private static ArrayAnalyzer.Sites sites(ArrayAnalyzer.Sites s, SourceSnapshot.Span write, String id, ArrayAnalyzer.IndexSites index) {
        return new ArrayAnalyzer.Sites(s.declaration(), s.read(), s.write(), s.bindingId(), s.kind(), s.arrayDeclaration(), s.arrayBindingId(), s.scalarReference(), write, id, index);
    }
    private static IndexAcceptance.Example example(String name, String source, int limit) { return new IndexAcceptance.Example("index-update-" + name, source, limit); }
    private static byte[] bytes(String source) { return source.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
