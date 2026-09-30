package dev.codeviz.analysis;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.*;

/** Frozen facts for the single reviewed classic-for shape, separate from fixed-operation sites. */
public final class LoopProbe {
    public record Facts(ArrayAnalyzer.Binding scalar, ArrayAnalyzer.Binding array, ArrayAnalyzer.Binding index,
                        SourceSnapshot.Span loop, SourceSnapshot.Span scalarDeclaration, SourceSnapshot.Span arrayDeclaration,
                        SourceSnapshot.Span indexDeclaration, SourceSnapshot.Span condition, SourceSnapshot.Span store,
                        SourceSnapshot.Span update, String operator, SourceSnapshot.Span body,
                        SourceSnapshot.Span conditionIndex, SourceSnapshot.Span lengthReceiver, SourceSnapshot.Span storeIndex,
                        SourceSnapshot.Span scalarReference, SourceSnapshot.Span updateOperand, String comparison, String lengthType) {}

    static ArrayAnalyzer.Sites sites(SourceSnapshot source, NodeList<Statement> statements,
            List<ArrayAnalyzer.Binding> bindings, Map<ArrayAccessExpr, ArrayAnalyzer.Binding> targets) {
        try {
            var scalar = declaration(statements.get(0).asExpressionStmt().getExpression(), "int");
            var array = declaration(statements.get(1).asExpressionStmt().getExpression(), "int[]");
            var loop = statements.get(2).asForStmt();
            if (loop.getInitialization().size() != 1 || loop.getUpdate().size() != 1
                || !loop.getBody().isBlockStmt() || loop.getBody().asBlockStmt().getStatements().size() != 1) return null;
            var index = declaration(loop.getInitialization().get(0), "int");
            var x = scalar.getVariable(0); var a = array.getVariable(0); var i = index.getVariable(0);
            if (!ArrayAnalyzer.intLiteral(x.getInitializer().orElseThrow()) || !ArrayAnalyzer.intLiteral(i.getInitializer().orElseThrow())) return null;
            var init = a.getInitializer().orElseThrow().asArrayInitializerExpr();
            if (init.getValues().size() > 16 || !init.getValues().stream().allMatch(ArrayAnalyzer::intLiteral)) return null;
            var condition = loop.getCompare().orElseThrow().asBinaryExpr();
            var length = condition.getRight().asFieldAccessExpr();
            if (condition.getOperator() != BinaryExpr.Operator.LESS || !length.getNameAsString().equals("length")
                || !reference(condition.getLeft(), i, "int") || !reference(length.getScope(), a, "int[]")
                || !condition.calculateResolvedType().describe().equals("boolean")
                || !length.calculateResolvedType().describe().equals("int")) return null;
            var update = loop.getUpdate().get(0).asUnaryExpr();
            if ((update.getOperator() != UnaryExpr.Operator.POSTFIX_INCREMENT && update.getOperator() != UnaryExpr.Operator.PREFIX_INCREMENT)
                || !reference(update.getExpression(), i, "int") || !update.calculateResolvedType().describe().equals("int")) return null;
            var store = loop.getBody().asBlockStmt().getStatement(0).asExpressionStmt().getExpression().asAssignExpr();
            var access = store.getTarget().asArrayAccessExpr();
            if (store.getOperator() != AssignExpr.Operator.ASSIGN || !reference(access.getName(), a, "int[]")
                || !reference(access.getIndex(), i, "int") || !reference(store.getValue(), x, "int")) return null;
            var xb = binding(source, bindings, x); var ab = binding(source, bindings, a); var ib = binding(source, bindings, i);
            if (!ab.equals(targets.get(access)) || !xb.scopeId().equals(ab.scopeId()) || xb.scopeId().equals(ib.scopeId())
                || Set.of(xb.id(), ab.id(), ib.id()).size() != 3) return null;
            if (statements.size() == 5 && !probes(statements, x, a, bindings)) return null;
            var facts = new Facts(xb, ab, ib, span(source, loop), span(source, scalar), span(source, array), span(source, index),
                span(source, condition), span(source, store), span(source, update), update.getOperator().name(), span(source, loop.getBody()),
                span(source, condition.getLeft()), span(source, length.getScope()), span(source, access.getIndex()),
                span(source, store.getValue()), span(source, update.getExpression()), "LESS", "int");
            return new ArrayAnalyzer.Sites(null, null, null, null, ArrayAnalyzer.ProbeKind.LOOP, null, null, null, null, null, null, null, null, facts);
        } catch (RuntimeException outsideShape) { return null; }
    }
    private static VariableDeclarationExpr declaration(Expression expression, String type) {
        var d = expression.asVariableDeclarationExpr();
        if (d.getVariables().size() != 1 || !d.getModifiers().isEmpty() || !d.getAnnotations().isEmpty()
            || !d.getVariable(0).getType().asString().equals(type)) throw new IllegalArgumentException("Declaration shape");
        return d;
    }
    private static ArrayAnalyzer.Binding binding(SourceSnapshot source, List<ArrayAnalyzer.Binding> bindings, VariableDeclarator v) {
        return bindings.stream().filter(b -> b.span().equals(span(source, v)) && b.scopeId() != null
            && b.name().equals(v.getNameAsString()) && b.type().equals(v.getType().asString())).findFirst().orElseThrow();
    }
    private static boolean reference(Expression expression, VariableDeclarator v, String type) {
        var resolved = expression.asNameExpr().resolve();
        return resolved instanceof JavaParserVariableDeclaration local && local.getVariableDeclarator().getRange().equals(v.getRange())
            && resolved.getType().describe().equals(type);
    }
    private static boolean probes(NodeList<Statement> statements, VariableDeclarator x, VariableDeclarator a, List<ArrayAnalyzer.Binding> bindings) {
        if (bindings.stream().anyMatch(b -> b.name().equals("java") || b.name().equals("System"))) return false;
        var printer = new PrettyPrinterConfiguration().setPrintComments(false).setPrintJavadoc(false);
        String out = "System.out.print(\"FINAL=\" + " + x.getNameAsString() + " + \",\" + java.util.Arrays.toString(" + a.getNameAsString() + "));";
        if (!statements.get(3).toString(printer).equals(out) || !statements.get(4).toString(printer).equals("System.err.print(\"PROBE\");")) return false;
        for (int n = 3; n < 5; n++) {
            var field = statements.get(n).asExpressionStmt().getExpression().asMethodCallExpr().getScope().orElseThrow().asFieldAccessExpr().resolve().asField();
            if (!field.declaringType().getQualifiedName().equals("java.lang.System") || !field.getType().describe().equals("java.io.PrintStream")) return false;
        }
        var call = statements.get(3).findAll(MethodCallExpr.class).stream().filter(c -> c.getNameAsString().equals("toString")).findFirst().orElseThrow();
        if (!reference(call.getArgument(0), a, "int[]")) return false;
        for (NameExpr name : statements.get(3).findAll(NameExpr.class))
            if (name.getNameAsString().equals(x.getNameAsString()) && !reference(name, x, "int")) return false;
        var jdk = new ReflectionTypeSolver(true);
        return jdk.solveType("java.util.Arrays").getDeclaredMethods().stream().filter(m -> m.isStatic()
            && m.getQualifiedSignature().equals("java.util.Arrays.toString(int[])") && m.getReturnType().describe().equals("java.lang.String")).count() == 1
            && jdk.solveType("java.io.PrintStream").getDeclaredMethods().stream().filter(m -> !m.isStatic()
            && m.getQualifiedSignature().equals("java.io.PrintStream.print(java.lang.String)")).count() == 1;
    }
    private static SourceSnapshot.Span span(SourceSnapshot source, Node node) { return source.span(node.getRange().orElseThrow()); }
}
