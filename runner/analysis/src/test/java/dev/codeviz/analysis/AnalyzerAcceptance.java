package dev.codeviz.analysis;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.tools.*;

/** Runs only in the restricted Java 21 test container. No submitted class is executed. */
public final class AnalyzerAcceptance {
    private static final ArrayAnalyzer ANALYZER = new ArrayAnalyzer();
    private static int count;
    private static String original;
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("deadline-probe")) { Thread.sleep(120_000); return; }
        if (args.length == 1 && args[0].equals("output-probe")) {
            for (int i = 0; i < 2048; i++) System.out.print("x".repeat(1024));
            return;
        }
        check(Runtime.version().feature() == 21, "Java 21 resolution environment");
        if (args.length == 1 && args[0].equals("acceptance-conditional")) { ConditionalAcceptance.run(); return; }
        if (args.length == 1 && args[0].startsWith("acceptance-read-")) { LoopReadAcceptance.run(args[0].equals("acceptance-read-pre")); return; }
        if (args.length == 1 && args[0].startsWith("acceptance-loop-")) { LoopAcceptance.run(args[0].equals("acceptance-loop-pre")); return; }
        if (args.length == 1 && args[0].equals("acceptance-addition")) { TransformationAcceptance.runAddition(); return; }
        try (InputStream in = AnalyzerAcceptance.class.getResourceAsStream("/fixtures/Main.java")) {
            original = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
        test("original AST, types, bindings and recording-site slices", () -> {
            var result = analyze(original);
            check(result.eligible(), result.diagnostics().toString());
            var ast = result.syntaxCopy().orElseThrow();
            check(ast.getTypes().size() == 1 && ast.findAll(MethodDeclaration.class).size() == 1, "AST class and method");
            check(ast.findAll(VariableDeclarator.class).size() == 1 && ast.findAll(AssignExpr.class).size() == 1, "AST declaration and assignment");
            check(ast.findAll(ArrayAccessExpr.class).size() == 4, "AST accesses include development probe");
            check(result.entry().matches() && result.entry().parameterType().equals("java.lang.String[]"), "entry type");
            var array = result.bindings().stream().filter(b -> b.name().equals("values")).findFirst().orElseThrow();
            check(array.type().equals("int[]"), "array type");
            check(result.accesses().stream().allMatch(a -> a.bindingId().equals(array.id()) && a.arrayType().equals("int[]") && a.elementType().equals("int")), "resolved access facts");
            check(result.source().slice(result.sites().declaration()).equals("int[] values = {3, 1}"), "declaration source");
            check(result.source().slice(result.sites().read()).equals("values[1]"), "read source");
            check(result.source().slice(result.sites().write()).equals("values[0] = values[1]"), "write source");
            check(result.scopes().stream().anyMatch(s -> s.id().equals(array.scopeId()) && s.kind().equals("BlockStmt") && s.parentId() != null), "local scope containment");
        });
        test("renamed array and changed signed literals", () -> {
            String source = original.replace("values", "items").replace("{3, 1}", "{9, -4}");
            var result = analyze(source);
            check(result.eligible(), result.diagnostics().toString());
            check(result.bindings().stream().anyMatch(b -> b.name().equals("items") && b.type().equals("int[]")), "renamed binding");
            check(!result.source().id().equals(analyze(original).source().id()), "source identity changes");
            check(compiles(source), "renamed example compiles on Java 21");
        });
        test("tabs, CRLF, comments and supplementary UTF-16 characters", () -> {
            String source = minimal("int[] café = {3, 1};\n\t/* 😀 */ café[0] = café[1];").replace("\n", "\r\n");
            var result = analyze(source);
            check(result.eligible(), result.diagnostics().toString());
            check(source.equals(result.source().text()), "exact text preserved");
            var write = result.sites().write();
            int offset = source.indexOf("café[0]");
            int lineStart = source.lastIndexOf('\n', offset) + 1;
            check(write.startOffset() == offset && write.start().column() == offset - lineStart + 1, "UTF-16 start column");
            check(result.source().slice(write).equals("café[0] = café[1]"), "exact assignment range");
            check(write.end().column() - write.start().column() == "café[0] = café[1]".length(), "exclusive end");
            check(compiles(source), "Unicode/reformatted source compiles");
        });
        test("independent syntax copy and frozen semantic facts", () -> {
            var result = analyze(original);
            String before = result.syntaxCopy().orElseThrow().toString();
            var copy = result.syntaxCopy().orElseThrow();
            copy.findFirst(VariableDeclarator.class).orElseThrow().setName("changed");
            copy.getTypes().clear();
            check(before.equals(result.syntaxCopy().orElseThrow().toString()), "original AST unchanged");
            check(result.source().text().equals(original), "original text unchanged");
            check(result.bindings().stream().anyMatch(b -> b.name().equals("values")), "binding facts unchanged");
            boolean immutable = false;
            try { result.bindings().clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
            check(immutable, "facts immutable");
        });
        test("stale source fails the handoff", () -> {
            var source = analyze(original).source();
            source.requireSame(bytes(original));
            boolean rejected = false;
            try { source.requireSame(bytes(original + " ")); } catch (IllegalArgumentException e) { rejected = e.getMessage().equals("STALE_SOURCE"); }
            check(rejected, "edited source rejected");
        });
        test("same-named locals resolve to distinct declarations and scopes", () -> {
            String source = minimal("{ int[] a = {1}; a[0] = a[0]; } { int[] a = {2}; a[0] = a[0]; }");
            var result = analyze(source);
            var arrays = result.bindings().stream().filter(b -> b.name().equals("a")).toList();
            check(arrays.size() == 2 && !arrays.get(0).id().equals(arrays.get(1).id()) && !arrays.get(0).scopeId().equals(arrays.get(1).scopeId()), "distinct lexical bindings");
            for (var access : result.accesses()) {
                var declaration = arrays.stream().filter(b -> b.id().equals(access.bindingId())).findFirst().orElseThrow();
                var scope = result.scopes().stream().filter(s -> s.id().equals(declaration.scopeId())).findFirst().orElseThrow();
                check(access.span().startOffset() > scope.span().startOffset() && access.span().endOffset() < scope.span().endOffset(), "access resolves in its block");
            }
            check(result.accesses().size() == 4 && !result.eligible() && has(result, ArrayAnalyzer.Category.UNTESTED), "scope analysis does not expand instrumentation");
            check(compiles(source), "same-named block locals are legal Java");
        });
        test("field and local with the same name bind separately", () -> {
            String source = "public class Main { static int[] a={1}; public static void main(String[] args) { int[] a={2}; a[0]=a[0]; } static void other() { a[0]=a[0]; } }";
            var result = analyze(source);
            var arrays = result.bindings().stream().filter(b -> b.name().equals("a")).toList();
            check(arrays.size() == 2 && result.accesses().size() == 4, "field/local facts");
            check(result.accesses().stream().map(ArrayAnalyzer.Access::bindingId).distinct().count() == 2, "field/local references differ");
            check(!result.eligible() && compiles(source), "unsupported shape remains valid Java");
        });
        test("syntax diagnostics have reliable locations", () -> {
            String source = minimal("int[] a = {1, }; a[0] = ;");
            var result = analyze(source);
            check(!result.eligible() && has(result, ArrayAnalyzer.Category.SYNTAX), "syntax category");
            check(result.diagnostics().stream().anyMatch(d -> d.span() != null), "syntax location");
            check(result.syntaxCopy().isEmpty() && !compiles(source), "no recovered AST marked complete");
        });
        test("unresolved array is not reported as a syntax error", () -> {
            var result = analyze(minimal("int[] a = {1}; a[0] = missing[0];"));
            check(!result.eligible() && has(result, ArrayAnalyzer.Category.UNRESOLVED) && !has(result, ArrayAnalyzer.Category.SYNTAX), "unresolved category");
        });
        test("compiler-valid unsupported loop stays untested", () -> {
            String source = minimal("int[] a={1}; for(int i=0;i<1;i++) a[i]=a[i];");
            var result = analyze(source);
            check(!result.eligible() && has(result, ArrayAnalyzer.Category.UNTESTED) && !has(result, ArrayAnalyzer.Category.SYNTAX), "coverage is not validity");
            check(result.bindings().stream().filter(b -> b.name().equals("i")).allMatch(b -> b.scopeId() == null), "unmodeled loop scope is unknown, not the outer block");
            check(compiles(source), "loop compiler evidence");
        });
        test("missing, wrong and ambiguous entry conventions", () -> {
            for (String source : List.of("", "   \r\n", original.replace("static void main", "void main"), original.replace("String[] args", "int[] args"),
                original.replace("public class Main", "public class Other"), "public class Main {}",
                "public class Main { public static void main(String[] a) {} public static void main(String[] b) {} }")) {
                var result = analyze(source);
                check(!result.eligible() && has(result, ArrayAnalyzer.Category.ENTRY_CONVENTION), "entry diagnostic");
            }
        });
        test("signed int boundaries and array size", () -> {
            check(analyze(minimal("int[] a={-2147483648,2147483647}; a[0]=a[1];")).eligible(), "int limits");
            check(analyze(minimal("int[] a={}; a[0]=a[0];")).eligible(), "empty array can analyze despite runtime failure");
            check(!analyze(minimal("int[] a={2147483648}; a[0]=a[0];")).eligible(), "out of range not eligible");
            check(!analyze(minimal("int[] a={" + "1,".repeat(16) + "1}; a[0]=a[0];")).eligible(), "array count limit");
        });
        test("unverified Unicode escapes block mapping", () -> {
            var result = analyze(original + "// " + '\\' + "u0061");
            check(!result.eligible() && has(result, ArrayAnalyzer.Category.UNTESTED), "raw escapes untested");
        });
        test("malformed UTF-8 and source byte cap", () -> {
            check(has(ANALYZER.analyze(new byte[]{(byte)0xc3, 0x28}), ArrayAnalyzer.Category.INPUT), "bad encoding");
            check(has(ANALYZER.analyze(new byte[SourceSnapshot.MAX_BYTES + 1]), ArrayAnalyzer.Category.LIMIT), "source cap");
        });
        test("AST node cap", () -> {
            var result = analyze(minimal("int n=0;" + "n=1;".repeat(1000)));
            check(!result.eligible() && has(result, ArrayAnalyzer.Category.LIMIT), "AST cap");
        });
        test("diagnostic count and message caps", () -> {
            var result = analyze(minimal("int[] a={1};" + "a[0]=missing[0];".repeat(40)));
            check(result.diagnostics().size() == ArrayAnalyzer.MAX_DIAGNOSTICS && has(result, ArrayAnalyzer.Category.LIMIT), "diagnostic cap");
            check(result.diagnostics().stream().allMatch(d -> d.message().length() <= 240), "message cap");
        });
        test("bounded report never returns a truncated success", () -> {
            StringBuilder body = new StringBuilder();
            for (int i=0; i<250; i++) body.append("int variable").append(i).append("=0;");
            var result = analyze(minimal(body.toString()));
            boolean limited = false;
            try { AnalyzerMain.report(result); } catch (IllegalStateException e) { limited = e.getMessage().equals("REPORT_LIMIT"); }
            check(limited, "report size cap");
        });
        test("shadowed System cannot qualify as the development probe", () -> {
            var result = analyze(original.replace("values", "System"));
            check(!result.eligible(), "cannot resolve print on an int[]");
        });
        check(compiles(original), "original fixture Java compilation");
        System.out.println("PASS: " + count + " analyzer cases; source was never executed");
        TransformationAcceptance.run(original);
        System.out.println("REPORT_BEGIN");
        System.out.write(AnalyzerMain.report(analyze(original)));
    }
    private static String minimal(String body) { return "public class Main {\npublic static void main(String[] args) {\n" + body + "\n}\n}\n"; }
    private static byte[] bytes(String source) { return source.getBytes(StandardCharsets.UTF_8); }
    private static ArrayAnalyzer.Result analyze(String source) { return ANALYZER.analyze(bytes(source)); }
    private static boolean has(ArrayAnalyzer.Result r, ArrayAnalyzer.Category c) { return r.diagnostics().stream().anyMatch(d -> d.category() == c); }
    private static void check(boolean condition, String description) { if (!condition) throw new AssertionError(description); }
    private static void test(String name, Checked body) throws Exception { body.run(); count++; System.out.println("PASS " + name); }
    private interface Checked { void run() throws Exception; }
    private static boolean compiles(String source) throws Exception {
        Path directory = Files.createTempDirectory("analysis-compiler-");
        Path file = directory.resolve("Main.java");
        Files.writeString(file, source, StandardCharsets.UTF_8);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            return compiler.getTask(new StringWriter(), manager, diagnostic -> {},
                List.of("--release", "21", "-proc:none", "-d", directory.toString()), null,
                manager.getJavaFileObjects(file.toFile())).call();
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
