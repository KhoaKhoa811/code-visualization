package dev.codeviz.analysis;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import java.util.List;

/** Eligibility adapter for a single int declaration followed by a literal assignment. */
final class IntProbe {
    static ArrayAnalyzer.Sites sites(SourceSnapshot source, NodeList<Statement> statements,
                                    VariableDeclarationExpr declaration, VariableDeclarator variable,
                                    List<ArrayAnalyzer.Binding> bindings, ArrayAnalyzer.Diagnostics diagnostics) {
        if (!declaration.getModifiers().isEmpty() || variable.getInitializer().isEmpty()
            || !ArrayAnalyzer.intLiteral(variable.getInitializer().orElseThrow())
            || !statements.get(1).isExpressionStmt()
            || !(statements.get(1).asExpressionStmt().getExpression() instanceof AssignExpr assignment)
            || assignment.getOperator() != AssignExpr.Operator.ASSIGN
            || !(assignment.getTarget() instanceof NameExpr target)
            || !ArrayAnalyzer.intLiteral(assignment.getValue())) return null;
        try {
            var resolved = target.resolve();
            if (!(resolved instanceof JavaParserVariableDeclaration local)
                || !local.getVariableDeclarator().getRange().equals(variable.getRange())
                || !resolved.getType().describe().equals("int")
                || !assignment.getValue().calculateResolvedType().describe().equals("int")) return null;
            var binding = bindings.stream().filter(b -> b.span().equals(source.span(variable.getRange().orElseThrow()))
                && b.type().equals("int") && b.scopeId() != null).findFirst().orElseThrow();
            if (statements.size() == 4) {
                var printer = new PrettyPrinterConfiguration().setPrintComments(false).setPrintJavadoc(false);
                String out = "System.out.print(\"FINAL=\" + " + variable.getNameAsString() + ");";
                if (!statements.get(2).toString(printer).equals(out)
                    || !statements.get(3).toString(printer).equals("System.err.print(\"PROBE\");")) return null;
                for (int i = 2; i < 4; i++) {
                    var call = statements.get(i).asExpressionStmt().getExpression().asMethodCallExpr();
                    if (!call.resolve().getQualifiedSignature().equals("java.io.PrintStream.print(java.lang.String)")
                        || !call.getScope().orElseThrow().asFieldAccessExpr().resolve().asField().declaringType().getQualifiedName().equals("java.lang.System")) return null;
                }
            }
            return new ArrayAnalyzer.Sites(source.span(declaration.getRange().orElseThrow()), null,
                source.span(assignment.getRange().orElseThrow()), binding.id(), ArrayAnalyzer.ProbeKind.INT_VARIABLE);
        } catch (RuntimeException unresolved) {
            diagnostics.add(ArrayAnalyzer.Category.UNRESOLVED, "Integer assignment binding or probe call could not be resolved",
                source.span(assignment.getRange().orElseThrow()));
            return null;
        }
    }
}
