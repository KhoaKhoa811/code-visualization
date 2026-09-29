package dev.codeviz.analysis;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.*;

/** One resolved scalar declaration, one array declaration, and a scalar-to-array store. */
final class CombinedProbe {
    static ArrayAnalyzer.Sites sites(SourceSnapshot source, NodeList<Statement> statements,
            List<ArrayAnalyzer.Binding> bindings, Map<ArrayAccessExpr, ArrayAnalyzer.Binding> targets,
            ArrayAnalyzer.Diagnostics diagnostics) {
        boolean indexed = statements.get(2).isExpressionStmt()
            && statements.get(2).asExpressionStmt().getExpression().isVariableDeclarationExpr();
        boolean indexUpdate = indexed && (statements.size() == 5 || statements.size() == 7);
        boolean update = indexUpdate || (!indexed && (statements.size() == 4 || statements.size() == 6));
        int storeIndex = 2 + (indexed ? 1 : 0) + (update ? 1 : 0), probeIndex = storeIndex + 1;
        if (statements.size() != probeIndex && statements.size() != probeIndex + 2) return null;
        if (!statements.get(0).isExpressionStmt() || !statements.get(1).isExpressionStmt()
            || !(statements.get(0).asExpressionStmt().getExpression() instanceof VariableDeclarationExpr scalar)
            || !(statements.get(1).asExpressionStmt().getExpression() instanceof VariableDeclarationExpr array)
            || scalar.getVariables().size() != 1 || array.getVariables().size() != 1
            || !scalar.getModifiers().isEmpty() || !array.getModifiers().isEmpty()
            || !scalar.getAnnotations().isEmpty() || !array.getAnnotations().isEmpty()) return null;
        VariableDeclarator x = scalar.getVariable(0), a = array.getVariable(0);
        if (!x.getType().asString().equals("int") || !a.getType().asString().equals("int[]")
            || x.getInitializer().isEmpty() || !ArrayAnalyzer.intLiteral(x.getInitializer().orElseThrow())
            || !(a.getInitializer().orElse(null) instanceof ArrayInitializerExpr init)
            || init.getValues().size() > 16 || !init.getValues().stream().allMatch(ArrayAnalyzer::intLiteral)
            || !statements.get(storeIndex).isExpressionStmt()
            || !(statements.get(storeIndex).asExpressionStmt().getExpression() instanceof AssignExpr assign)
            || assign.getOperator() != AssignExpr.Operator.ASSIGN
            || !(assign.getTarget() instanceof ArrayAccessExpr left) || (!indexed && !ArrayAnalyzer.intLiteral(left.getIndex()))
            || !(assign.getValue() instanceof NameExpr right)) return null;
        try {
            var resolved = right.resolve();
            if (!(resolved instanceof JavaParserVariableDeclaration local)
                || !local.getVariableDeclarator().getRange().equals(x.getRange())
                || !resolved.getType().describe().equals("int")) return null;
            var scalarBinding = bindings.stream().filter(b -> b.span().equals(source.span(x.getRange().orElseThrow()))
                && b.type().equals("int") && b.scopeId() != null).findFirst().orElseThrow();
            Expression scalarWrite = null;
            ArrayAnalyzer.IndexSites indexSites = null;
            ArrayAnalyzer.AdditionSites additionSites = null;
            ArrayAnalyzer.IncrementSites incrementSites = null;
            String indexName = null;
            if (indexed) {
                var declaration = statements.get(2).asExpressionStmt().getExpression().asVariableDeclarationExpr();
                if (declaration.getVariables().size() != 1 || !declaration.getModifiers().isEmpty() || !declaration.getAnnotations().isEmpty()) return null;
                var index = declaration.getVariable(0);
                if (!index.getType().asString().equals("int") || index.getInitializer().isEmpty()
                    || !ArrayAnalyzer.intLiteral(index.getInitializer().orElseThrow()) || !(left.getIndex() instanceof NameExpr indexUse)) return null;
                var resolvedIndex = indexUse.resolve();
                if (!(resolvedIndex instanceof JavaParserVariableDeclaration indexLocal)
                    || !indexLocal.getVariableDeclarator().getRange().equals(index.getRange())
                    || !resolvedIndex.getType().describe().equals("int")) return null;
                var indexBinding = bindings.stream().filter(b -> b.span().equals(source.span(index.getRange().orElseThrow()))
                    && b.type().equals("int") && b.scopeId() != null).findFirst().orElseThrow();
                if (indexBinding.id().equals(scalarBinding.id())) return null;
                indexSites = new ArrayAnalyzer.IndexSites(source.span(declaration.getRange().orElseThrow()), indexBinding.id(), source.span(indexUse.getRange().orElseThrow()));
                indexName = indexBinding.name();
            }
            if (update) {
                int updateIndex = indexed ? 3 : 2;
                var expectedTarget = indexed ? statements.get(2).asExpressionStmt().getExpression().asVariableDeclarationExpr().getVariable(0) : x;
                if (!statements.get(updateIndex).isExpressionStmt()) return null;
                var operation = statements.get(updateIndex).asExpressionStmt().getExpression();
                if (operation instanceof UnaryExpr increment) {
                    if (!indexed || (increment.getOperator() != UnaryExpr.Operator.POSTFIX_INCREMENT
                        && increment.getOperator() != UnaryExpr.Operator.PREFIX_INCREMENT)
                        || !(increment.getExpression() instanceof NameExpr operand)) return null;
                    var operandBinding = operand.resolve();
                    if (!(operandBinding instanceof JavaParserVariableDeclaration operandLocal)
                        || !operandLocal.getVariableDeclarator().getRange().equals(expectedTarget.getRange())
                        || !operandBinding.getType().describe().equals("int")
                        || !increment.calculateResolvedType().describe().equals("int")) return null;
                    incrementSites = new ArrayAnalyzer.IncrementSites(source.span(increment.getRange().orElseThrow()),
                        source.span(operand.getRange().orElseThrow()), indexSites.bindingId(), increment.getOperator().name(), "int");
                    scalarWrite = increment;
                } else {
                    if (!(operation instanceof AssignExpr candidate)
                        || candidate.getOperator() != AssignExpr.Operator.ASSIGN
                        || !(candidate.getTarget() instanceof NameExpr target)) return null;
                    var targetBinding = target.resolve();
                    if (!(targetBinding instanceof JavaParserVariableDeclaration targetLocal)
                        || !targetLocal.getVariableDeclarator().getRange().equals(expectedTarget.getRange())
                        || !targetBinding.getType().describe().equals("int")) return null;
                    if (!ArrayAnalyzer.intLiteral(candidate.getValue())) {
                        if (!indexed || !(candidate.getValue() instanceof BinaryExpr addition)
                            || addition.getOperator() != BinaryExpr.Operator.PLUS
                            || !(addition.getLeft() instanceof NameExpr operand)
                            || !ArrayAnalyzer.intLiteral(addition.getRight())) return null;
                        var operandBinding = operand.resolve();
                        if (!(operandBinding instanceof JavaParserVariableDeclaration operandLocal)
                            || !operandLocal.getVariableDeclarator().getRange().equals(expectedTarget.getRange())
                            || !operandBinding.getType().describe().equals("int")
                            || !addition.getRight().calculateResolvedType().describe().equals("int")
                            || !addition.calculateResolvedType().describe().equals("int")) return null;
                        additionSites = new ArrayAnalyzer.AdditionSites(source.span(addition.getRange().orElseThrow()),
                            source.span(operand.getRange().orElseThrow()), source.span(addition.getRight().getRange().orElseThrow()),
                            indexSites.bindingId(), "PLUS", "int");
                    }
                    scalarWrite = candidate;
                }
            }
            var arrayBinding = targets.get(left);
            if (arrayBinding == null || !arrayBinding.type().equals("int[]") || arrayBinding.scopeId() == null
                || !arrayBinding.span().equals(source.span(a.getRange().orElseThrow()))
                || arrayBinding.id().equals(scalarBinding.id())) return null;
            if (statements.size() == probeIndex + 2) {
                // These are fixed development observations, not general overload resolution.
                // JavaParser 3.28.2 reports ambiguity among Arrays.toString primitive-array overloads.
                // Resolve the exact JDK declaration from the already resolved int[] argument instead.
                if (bindings.stream().anyMatch(b -> b.name().equals("java") || b.name().equals("System"))) return null;
                var printer = new PrettyPrinterConfiguration().setPrintComments(false).setPrintJavadoc(false);
                String out = "System.out.print(\"FINAL=\" + " + x.getNameAsString()
                    + (indexed ? " + \",\" + " + indexName : "")
                    + " + \",\" + java.util.Arrays.toString(" + a.getNameAsString() + "));";
                if (!statements.get(probeIndex).toString(printer).equals(out)
                    || !statements.get(probeIndex + 1).toString(printer).equals("System.err.print(\"PROBE\");")) return null;
                for (int i = probeIndex; i < probeIndex + 2; i++) {
                    var call = statements.get(i).asExpressionStmt().getExpression().asMethodCallExpr();
                    var field = call.getScope().orElseThrow().asFieldAccessExpr().resolve().asField();
                    if (!field.declaringType().getQualifiedName().equals("java.lang.System")
                        || !field.getType().describe().equals("java.io.PrintStream")) return null;
                }
                var toString = statements.get(probeIndex).findAll(MethodCallExpr.class).stream()
                    .filter(c -> c.getNameAsString().equals("toString")).findFirst().orElseThrow();
                var argument = toString.getArgument(0).asNameExpr().resolve();
                if (!(argument instanceof JavaParserVariableDeclaration actualArray)
                    || !actualArray.getVariableDeclarator().getRange().equals(a.getRange())
                    || !argument.getType().describe().equals("int[]")) return null;
                var jdk = new ReflectionTypeSolver(true);
                for (NameExpr name : statements.get(probeIndex).findAll(NameExpr.class)) {
                    if (!name.getNameAsString().equals(x.getNameAsString()) && !name.getNameAsString().equals(indexName)) continue;
                    var probeBinding = name.resolve();
                    var expected = name.getNameAsString().equals(x.getNameAsString()) ? scalarBinding.id() : indexSites.bindingId();
                    if (!(probeBinding instanceof JavaParserVariableDeclaration probeLocal)
                        || bindings.stream().noneMatch(b -> b.id().equals(expected)
                            && b.span().equals(source.span(probeLocal.getVariableDeclarator().getRange().orElseThrow())))) return null;
                }
                if (jdk.solveType("java.util.Arrays").getDeclaredMethods().stream().filter(m -> m.isStatic()
                    && m.getQualifiedSignature().equals("java.util.Arrays.toString(int[])")
                    && m.getReturnType().describe().equals("java.lang.String")).count() != 1
                    || jdk.solveType("java.io.PrintStream").getDeclaredMethods().stream().filter(m -> !m.isStatic()
                    && m.getQualifiedSignature().equals("java.io.PrintStream.print(java.lang.String)")).count() != 1) return null;
            }
            return new ArrayAnalyzer.Sites(source.span(scalar.getRange().orElseThrow()), null,
                source.span(assign.getRange().orElseThrow()), scalarBinding.id(), ArrayAnalyzer.ProbeKind.COMBINED,
                source.span(array.getRange().orElseThrow()), arrayBinding.id(), source.span(right.getRange().orElseThrow()),
                scalarWrite == null ? null : source.span(scalarWrite.getRange().orElseThrow()),
                scalarWrite == null ? null : indexed ? indexSites.bindingId() : scalarBinding.id(), indexSites, additionSites, incrementSites);
        } catch (RuntimeException failure) {
            diagnostics.add(ArrayAnalyzer.Category.UNRESOLVED, "Combined binding or probe call could not be resolved: " + failure.getClass().getSimpleName() + ": " + failure.getMessage(),
                source.span(assign.getRange().orElseThrow()));
            return null;
        }
    }
}
