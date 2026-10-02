package dev.codeviz.analysis;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.nio.charset.CharacterCodingException;
import java.util.*;

/** Static facts for a bounded experiment; never executes source or generates trace events. */
public final class ArrayAnalyzer {
    public static final int MAX_NODES = 4096, MAX_DIAGNOSTICS = 16;
    public enum Category { SYNTAX, UNRESOLVED, UNTESTED, ENTRY_CONVENTION, INPUT, LIMIT, TOOL_FAILURE }
    public enum Completeness { COMPLETE_FOR_ARRAY_PROBE, COMPLETE_FOR_INT_PROBE, COMPLETE_FOR_COMBINED_PROBE, COMPLETE_FOR_LOOP_PROBE, PARTIAL, UNAVAILABLE }
    public enum ProbeKind { ARRAY, INT_VARIABLE, COMBINED, LOOP }
    public record Diagnostic(Category category, String message, SourceSnapshot.Span span) {}
    public record Scope(String id, String parentId, String kind, SourceSnapshot.Span span) {}
    public record Binding(String id, String name, String type, String scopeId, SourceSnapshot.Span span) {}
    public record Access(String bindingId, String arrayType, String elementType, String kind, SourceSnapshot.Span span) {}
    public record Entry(boolean matches, String className, String methodName, String parameterType, SourceSnapshot.Span span) {}
    public record IndexSites(SourceSnapshot.Span declaration, String bindingId, SourceSnapshot.Span reference) {}
    public record AdditionSites(SourceSnapshot.Span expression, SourceSnapshot.Span reference, SourceSnapshot.Span literal,
                                String bindingId, String operator, String resultType) {}
    public record IncrementSites(SourceSnapshot.Span expression, SourceSnapshot.Span operand,
                                 String bindingId, String operator, String resultType) {}
    public record Sites(SourceSnapshot.Span declaration, SourceSnapshot.Span read, SourceSnapshot.Span write,
                        String bindingId, ProbeKind kind, SourceSnapshot.Span arrayDeclaration, String arrayBindingId,
                        SourceSnapshot.Span scalarReference, SourceSnapshot.Span scalarWrite, String scalarWriteBindingId, IndexSites index, AdditionSites addition, IncrementSites increment, LoopProbe.Facts loop) {
        public Sites(SourceSnapshot.Span declaration, SourceSnapshot.Span read, SourceSnapshot.Span write, String bindingId, ProbeKind kind,
                     SourceSnapshot.Span arrayDeclaration, String arrayBindingId, SourceSnapshot.Span scalarReference,
                     SourceSnapshot.Span scalarWrite, String scalarWriteBindingId, IndexSites index, AdditionSites addition, IncrementSites increment) {
            this(declaration, read, write, bindingId, kind, arrayDeclaration, arrayBindingId, scalarReference, scalarWrite, scalarWriteBindingId, index, addition, increment, null);
        }
        public Sites(SourceSnapshot.Span declaration, SourceSnapshot.Span read, SourceSnapshot.Span write, String bindingId, ProbeKind kind,
                     SourceSnapshot.Span arrayDeclaration, String arrayBindingId, SourceSnapshot.Span scalarReference,
                     SourceSnapshot.Span scalarWrite, String scalarWriteBindingId, IndexSites index, AdditionSites addition) {
            this(declaration, read, write, bindingId, kind, arrayDeclaration, arrayBindingId, scalarReference, scalarWrite, scalarWriteBindingId, index, addition, null);
        }
        public Sites(SourceSnapshot.Span declaration, SourceSnapshot.Span read, SourceSnapshot.Span write, String bindingId, ProbeKind kind,
                     SourceSnapshot.Span arrayDeclaration, String arrayBindingId, SourceSnapshot.Span scalarReference,
                     SourceSnapshot.Span scalarWrite, String scalarWriteBindingId, IndexSites index) {
            this(declaration, read, write, bindingId, kind, arrayDeclaration, arrayBindingId, scalarReference, scalarWrite, scalarWriteBindingId, index, null);
        }
        public Sites(SourceSnapshot.Span declaration, SourceSnapshot.Span read, SourceSnapshot.Span write, String bindingId, ProbeKind kind,
                     SourceSnapshot.Span arrayDeclaration, String arrayBindingId, SourceSnapshot.Span scalarReference,
                     SourceSnapshot.Span scalarWrite, String scalarWriteBindingId) {
            this(declaration, read, write, bindingId, kind, arrayDeclaration, arrayBindingId, scalarReference, scalarWrite, scalarWriteBindingId, null);
        }
        public Sites(SourceSnapshot.Span declaration, SourceSnapshot.Span read, SourceSnapshot.Span write,
                     String bindingId, ProbeKind kind) { this(declaration, read, write, bindingId, kind, null, null, null, null, null); }
    }

    public static final class Result {
        private final SourceSnapshot source;
        private final CompilationUnit original;
        private final List<Binding> bindings;
        private final List<Scope> scopes;
        private final List<Access> accesses;
        private final List<Diagnostic> diagnostics;
        private final Entry entry;
        private final Sites sites;
        private final Completeness completeness;
        private Result(SourceSnapshot source, CompilationUnit original, List<Binding> bindings,
                       List<Scope> scopes, List<Access> accesses, List<Diagnostic> diagnostics,
                       Entry entry, Sites sites, Completeness completeness) {
            this.source = source; this.original = original;
            this.bindings = List.copyOf(bindings); this.scopes = List.copyOf(scopes);
            this.accesses = List.copyOf(accesses); this.diagnostics = List.copyOf(diagnostics);
            this.entry = entry; this.sites = sites; this.completeness = completeness;
        }
        public SourceSnapshot source() { return source; }
        public List<Binding> bindings() { return bindings; }
        public List<Scope> scopes() { return scopes; }
        public List<Access> accesses() { return accesses; }
        public List<Diagnostic> diagnostics() { return diagnostics; }
        public Entry entry() { return entry; }
        public Sites sites() { return sites; }
        public Completeness completeness() { return completeness; }
        public boolean eligible() { return sites != null && diagnostics.isEmpty() && entry.matches(); }
        /** Syntax inspection only: resolver facts belong to the frozen original-source records. */
        public Optional<CompilationUnit> syntaxCopy() {
            if (original == null) return Optional.empty();
            CompilationUnit copy = original.clone();
            copy.walk(node -> node.removeData(Node.SYMBOL_RESOLVER_KEY));
            return Optional.of(copy);
        }
    }

    public Result analyze(byte[] bytes) {
        SourceSnapshot source;
        try { source = new SourceSnapshot(bytes); }
        catch (CharacterCodingException e) { return failed(null, Category.INPUT, "Source must be valid UTF-8"); }
        catch (IllegalArgumentException e) { return failed(null, Category.LIMIT, "Source exceeds 64 KiB"); }
        // Raw escape-to-original mapping is outside this gate, including escapes in comments/strings.
        if (source.text().contains("\\u")) return failed(source, Category.UNTESTED, "Raw Unicode escapes are not verified for mapping");
        try { return analyze(source); }
        catch (StackOverflowError e) { return failed(source, Category.LIMIT, "Analysis stack exhausted"); }
        catch (RuntimeException e) { return failed(source, Category.TOOL_FAILURE, "Analysis failed: " + e.getClass().getSimpleName()); }
    }
    private Result failed(SourceSnapshot source, Category category, String message) {
        return new Result(source, null, List.of(), List.of(), List.of(),
            List.of(new Diagnostic(category, message, null)), new Entry(false, null, null, null, null),
            null, Completeness.UNAVAILABLE);
    }
    private Result analyze(SourceSnapshot source) {
        ParserConfiguration config = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21).setTabSize(1)
            .setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(true)));
        ParseResult<CompilationUnit> parsed = new JavaParser(config).parse(source.text());
        Diagnostics diagnostics = new Diagnostics();
        if (!parsed.isSuccessful()) {
            for (Problem problem : parsed.getProblems()) {
                SourceSnapshot.Span where = problem.getLocation().flatMap(TokenRange::toRange)
                    .flatMap(range -> safeSpan(source, range)).orElse(null);
                diagnostics.add(Category.SYNTAX, problem.getMessage(), where);
            }
            return new Result(source, null, List.of(), List.of(), List.of(), diagnostics.values,
                new Entry(false, null, null, null, null), null, Completeness.UNAVAILABLE);
        }
        CompilationUnit unit = parsed.getResult().orElseThrow();
        if (source.text().isBlank()) return failed(source, Category.ENTRY_CONVENTION, "Main.main is missing");
        List<Node> nodes = unit.stream().limit(MAX_NODES + 1L).toList();
        if (nodes.size() > MAX_NODES) return failed(source, Category.LIMIT, "AST exceeds 4096 nodes");

        Map<Node, String> scopeIds = new IdentityHashMap<>();
        List<Scope> scopes = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof CompilationUnit || node instanceof TypeDeclaration<?> || node instanceof MethodDeclaration || node instanceof BlockStmt
                || (node instanceof ForStmt && loopCandidate(node))) {
                SourceSnapshot.Span span = span(source, node);
                String id = "scope:" + node.getClass().getSimpleName() + ":" + span.startOffset();
                String parent = enclosingScope(node, scopeIds);
                scopeIds.put(node, id);
                scopes.add(new Scope(id, parent, node.getClass().getSimpleName(), span));
            }
        }
        List<Binding> bindings = new ArrayList<>();
        Map<Integer, Binding> byOffset = new HashMap<>();
        for (Node node : nodes) {
            if (!(node instanceof VariableDeclarator) && !(node instanceof Parameter)) continue;
            SourceSnapshot.Span range = span(source, node);
            try {
                String name, type;
                if (node instanceof VariableDeclarator variable) {
                    name = variable.getNameAsString(); type = variable.resolve().getType().describe();
                } else {
                    Parameter parameter = (Parameter) node;
                    name = parameter.getNameAsString(); type = parameter.resolve().getType().describe();
                }
                Binding binding = new Binding(source.id() + ":binding:" + range.startOffset(), name, type,
                    enclosingScope(node, scopeIds), range);
                bindings.add(binding); byOffset.put(range.startOffset(), binding);
            } catch (RuntimeException e) {
                diagnostics.add(Category.UNRESOLVED, "Declaration type could not be resolved", range);
            }
        }
        List<Access> accesses = new ArrayList<>();
        Map<ArrayAccessExpr, Binding> targets = new IdentityHashMap<>();
        for (Node node : nodes) {
            if (!(node instanceof ArrayAccessExpr access)) continue;
            try {
                if (!(access.getName() instanceof NameExpr name)) {
                    diagnostics.add(Category.UNTESTED, "Only direct named array accesses are investigated", span(source, access));
                    continue;
                }
                var resolved = name.resolve();
                // toAst() on a local returns the entire declaration expression, not its declarator.
                Node declaration = resolved instanceof JavaParserVariableDeclaration local ? local.getVariableDeclarator()
                    : resolved instanceof JavaParserFieldDeclaration field ? field.getVariableDeclarator()
                    : resolved.toAst().orElseThrow();
                Binding binding = byOffset.get(span(source, declaration).startOffset());
                if (binding == null) throw new IllegalStateException("Missing declaration fact");
                String arrayType = name.calculateResolvedType().describe();
                String elementType = access.calculateResolvedType().describe();
                String kind = access.getParentNode().filter(p -> p instanceof AssignExpr a && a.getTarget() == access)
                    .isPresent() ? "WRITE_TARGET" : "READ";
                accesses.add(new Access(binding.id(), arrayType, elementType, kind, span(source, access)));
                targets.put(access, binding);
            } catch (RuntimeException e) {
                diagnostics.add(Category.UNRESOLVED, "Array declaration or expression type could not be resolved", span(source, access));
            }
        }
        Entry entry = entry(source, unit, diagnostics);
        Sites sites = eligibleSites(source, unit, targets, entry, bindings, diagnostics);
        if (sites == null) diagnostics.add(Category.UNTESTED, "Source is outside the reviewed int/array probe shapes; Java validity is not determined", null);
        Completeness complete = diagnostics.values.isEmpty()
            ? sites.kind() == ProbeKind.ARRAY ? Completeness.COMPLETE_FOR_ARRAY_PROBE
                : sites.kind() == ProbeKind.COMBINED ? Completeness.COMPLETE_FOR_COMBINED_PROBE
                : sites.kind() == ProbeKind.LOOP ? Completeness.COMPLETE_FOR_LOOP_PROBE : Completeness.COMPLETE_FOR_INT_PROBE
            : Completeness.PARTIAL;
        return new Result(source, unit, bindings, scopes, accesses, diagnostics.values, entry,
            diagnostics.values.isEmpty() ? sites : null, complete);
    }
    private Entry entry(SourceSnapshot source, CompilationUnit unit, Diagnostics diagnostics) {
        List<MethodDeclaration> matches = unit.findAll(MethodDeclaration.class).stream().filter(method ->
            method.getNameAsString().equals("main") && method.isPublic() && method.isStatic()
                && method.getType().isVoidType() && method.getParameters().size() == 1
                && method.getParentNode().filter(p -> p instanceof ClassOrInterfaceDeclaration c
                    && !c.isInterface() && c.isPublic() && c.getNameAsString().equals("Main")
                    && c.getParentNode().orElse(null) == unit).isPresent()).toList();
        if (matches.size() == 1 && unit.getPackageDeclaration().isEmpty()) {
            MethodDeclaration method = matches.getFirst();
            try {
                Parameter p = method.getParameter(0);
                String type = p.resolve().getType().describe();
                if (type.equals("java.lang.String[]") && !p.isVarArgs() && method.getBody().isPresent())
                    return new Entry(true, "Main", "main", type, span(source, method));
            } catch (RuntimeException e) {
                diagnostics.add(Category.UNRESOLVED, "Entry parameter type could not be resolved", span(source, method));
            }
        }
        diagnostics.add(Category.ENTRY_CONVENTION, "Expected public Main with public static void main(String[] args)", null);
        return new Entry(false, null, null, null, null);
    }
    private Sites eligibleSites(SourceSnapshot source, CompilationUnit unit,
                                Map<ArrayAccessExpr, Binding> targets, Entry entry, List<Binding> bindings, Diagnostics diagnostics) {
        if (!entry.matches() || !unit.getImports().isEmpty() || unit.getTypes().size() != 1) return null;
        ClassOrInterfaceDeclaration type = unit.getClassByName("Main").orElseThrow();
        if (type.getMembers().size() != 1 || !type.getAnnotations().isEmpty() || !type.getTypeParameters().isEmpty()
            || !type.getExtendedTypes().isEmpty() || !type.getImplementedTypes().isEmpty()) return null;
        MethodDeclaration method = type.getMethodsByName("main").getFirst();
        if (!method.getAnnotations().isEmpty() || !method.getTypeParameters().isEmpty()
            || method.getModifiers().size() != 2 || !method.getParameter(0).getAnnotations().isEmpty()) return null;
        NodeList<Statement> statements = method.getBody().orElseThrow().getStatements();
        if ((statements.size() == 2 || statements.size() == 4) && statements.get(1).isForStmt())
            return LoopProbe.sites(source, statements, bindings, targets);
        if ((statements.size() == 3 || statements.size() == 5) && statements.get(2).isForStmt())
            return LoopProbe.sites(source, statements, bindings, targets);
        if (statements.size() >= 3 && statements.size() <= 7 && statements.get(1).isExpressionStmt()
            && statements.get(1).asExpressionStmt().getExpression().isVariableDeclarationExpr())
            return CombinedProbe.sites(source, statements, bindings, targets, diagnostics);
        if (statements.size() != 2 && statements.size() != 4) return null;
        if (!(statements.get(0) instanceof ExpressionStmt ds) || !(ds.getExpression() instanceof VariableDeclarationExpr de)
            || de.getVariables().size() != 1 || !de.getAnnotations().isEmpty()) return null;
        VariableDeclarator variable = de.getVariable(0);
        if (variable.getType().asString().equals("int")) return IntProbe.sites(source, statements, de, variable, bindings, diagnostics);
        if (!variable.getType().asString().equals("int[]") || !(variable.getInitializer().orElse(null) instanceof ArrayInitializerExpr init)
            || init.getValues().size() > 16 || !init.getValues().stream().allMatch(ArrayAnalyzer::intLiteral)) return null;
        if (!(statements.get(1) instanceof ExpressionStmt as) || !(as.getExpression() instanceof AssignExpr assign)
            || assign.getOperator() != AssignExpr.Operator.ASSIGN || !(assign.getTarget() instanceof ArrayAccessExpr left)
            || !(assign.getValue() instanceof ArrayAccessExpr right) || !intLiteral(left.getIndex()) || !intLiteral(right.getIndex())) return null;
        Binding target = targets.get(left), value = targets.get(right);
        if (target == null || value == null || !target.id().equals(value.id()) || !target.type().equals("int[]")
            || target.span().startOffset() != span(source, variable).startOffset()) return null;
        if (statements.size() == 4) {
            String name = variable.getNameAsString();
            // These reviewed development observations are outside the candidate recording region.
            String out = "System.out.print(\"FINAL=\" + " + name + "[0] + \",\" + " + name + "[1]);";
            if (!syntax(statements.get(2)).equals(out) || !syntax(statements.get(3)).equals("System.err.print(\"PROBE\");")) return null;
            for (ArrayAccessExpr access : statements.get(2).findAll(ArrayAccessExpr.class))
                if (!target.equals(targets.get(access))) return null;
            try {
                for (int i = 2; i < 4; i++) {
                    MethodCallExpr call = statements.get(i).asExpressionStmt().getExpression().asMethodCallExpr();
                    if (!call.resolve().getQualifiedSignature().equals("java.io.PrintStream.print(java.lang.String)")) return null;
                    FieldAccessExpr stream = call.getScope().orElseThrow().asFieldAccessExpr();
                    if (!stream.resolve().asField().declaringType().getQualifiedName().equals("java.lang.System")) return null;
                }
            } catch (RuntimeException e) { return null; }
        }
        return new Sites(span(source, de), span(source, right), span(source, assign), target.id(), ProbeKind.ARRAY);
    }
    static boolean intLiteral(Expression expression) {
        // Decimal signed literals only. No constant evaluation or assumed runtime values.
        String spelling = syntax(expression);
        if (!spelling.matches("-?(0|[1-9][0-9]*)")) return false;
        try { Integer.parseInt(spelling); return true; }
        catch (NumberFormatException e) { return false; }
    }
    private static String syntax(Node node) {
        return node.toString(new PrettyPrinterConfiguration().setPrintComments(false).setPrintJavadoc(false));
    }
    private static String enclosingScope(Node node, Map<Node, String> ids) {
        Node parent = node.getParentNode().orElse(null);
        while (parent != null) {
            if (ids.containsKey(parent)) return ids.get(parent);
            // Do not label unmodeled Java scopes as an enclosing block's scope.
            if (parent instanceof ForStmt || parent instanceof ForEachStmt || parent instanceof CatchClause
                || parent instanceof LambdaExpr || parent instanceof TryStmt || parent instanceof SwitchEntry
                || parent instanceof ConstructorDeclaration) return null;
            parent = parent.getParentNode().orElse(null);
        }
        return null;
    }
    private static boolean loopCandidate(Node node) {
        if (!(node.getParentNode().orElse(null) instanceof BlockStmt block)
            || !(block.getParentNode().orElse(null) instanceof MethodDeclaration)) return false;
        if ((block.getStatements().size() == 3 || block.getStatements().size() == 5) && block.getStatement(2) == node) return true;
        if ((block.getStatements().size() == 2 || block.getStatements().size() == 4) && block.getStatement(1) == node) {
            var body = ((ForStmt)node).getBody();
            if (!body.isBlockStmt() || body.asBlockStmt().getStatements().size() != 1) return false;
            var statement = body.asBlockStmt().getStatement(0);
            return statement.isExpressionStmt() && statement.asExpressionStmt().getExpression() instanceof AssignExpr assignment
                && assignment.getValue() instanceof BinaryExpr addition && addition.getOperator() == BinaryExpr.Operator.PLUS
                && addition.getLeft().isArrayAccessExpr();
        }
        return false;
    }
    private static SourceSnapshot.Span span(SourceSnapshot source, Node node) {
        return source.span(node.getRange().orElseThrow());
    }
    private static Optional<SourceSnapshot.Span> safeSpan(SourceSnapshot source, Range range) {
        try { return Optional.of(source.span(range)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }
    static final class Diagnostics {
        private final List<Diagnostic> values = new ArrayList<>();
        void add(Category category, String message, SourceSnapshot.Span span) {
            if (values.size() < MAX_DIAGNOSTICS) {
                values.add(new Diagnostic(category, message.substring(0, Math.min(message.length(), 240)), span));
            } else {
                values.set(MAX_DIAGNOSTICS - 1, new Diagnostic(Category.LIMIT, "Diagnostic limit reached; facts are incomplete", null));
            }
        }
    }
}
