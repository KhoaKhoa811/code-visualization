package dev.codeviz.analysis;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import java.util.*;

/** Original-source facts for one conditional swap; distinct from the loop operation plan. */
public final class ConditionalProbe {
    public record Facts(ArrayAnalyzer.Binding array, ArrayAnalyzer.Binding temporary, List<SourceSnapshot.Span> sites,
                        int leftIndex, int rightIndex, String operator, String resultType, SourceSnapshot.Span branch) {
        public Facts { sites = List.copyOf(sites); }
    }
    static ArrayAnalyzer.Sites sites(SourceSnapshot source, NodeList<Statement> statements,
            List<ArrayAnalyzer.Binding> bindings, Map<ArrayAccessExpr, ArrayAnalyzer.Binding> targets) {
        try {
            var declaration = LoopProbe.declaration(statements.get(0).asExpressionStmt().getExpression(), "int[]");
            var array = declaration.getVariable(0);
            var initial = array.getInitializer().orElseThrow().asArrayInitializerExpr();
            if (initial.getValues().size() > 16 || !initial.getValues().stream().allMatch(ArrayAnalyzer::intLiteral)) return null;
            var branch = statements.get(1).asIfStmt();
            if (branch.getElseStmt().isPresent() || !branch.getThenStmt().isBlockStmt()) return null;
            var body = branch.getThenStmt().asBlockStmt();
            if (body.getStatements().size() != 3) return null;
            var condition = branch.getCondition().asBinaryExpr();
            if (condition.getOperator() != BinaryExpr.Operator.GREATER || !condition.calculateResolvedType().describe().equals("boolean")) return null;
            var left = condition.getLeft().asArrayAccessExpr(); var right = condition.getRight().asArrayAccessExpr();
            var tempDeclaration = LoopProbe.declaration(body.getStatement(0).asExpressionStmt().getExpression(), "int");
            var temp = tempDeclaration.getVariable(0); var initializer = temp.getInitializer().orElseThrow().asArrayAccessExpr();
            var first = body.getStatement(1).asExpressionStmt().getExpression().asAssignExpr();
            var last = body.getStatement(2).asExpressionStmt().getExpression().asAssignExpr();
            if (first.getOperator() != AssignExpr.Operator.ASSIGN || last.getOperator() != AssignExpr.Operator.ASSIGN
                || !LoopProbe.reference(last.getValue(), temp, "int")) return null;
            var firstTarget = first.getTarget().asArrayAccessExpr(); var firstRead = first.getValue().asArrayAccessExpr();
            var lastTarget = last.getTarget().asArrayAccessExpr();
            var ab = LoopProbe.binding(source, bindings, array); var tb = LoopProbe.binding(source, bindings, temp);
            if (ab.id().equals(tb.id()) || ab.scopeId().equals(tb.scopeId())) return null;
            for (var access : List.of(left, right, initializer, firstTarget, firstRead, lastTarget)) {
                if (!ab.equals(targets.get(access)) || !LoopProbe.reference(access.getName(), array, "int[]")
                    || !ArrayAnalyzer.intLiteral(access.getIndex()) || !access.calculateResolvedType().describe().equals("int")) return null;
            }
            int l = integer(left.getIndex()), r = integer(right.getIndex());
            if (integer(initializer.getIndex()) != l || integer(firstTarget.getIndex()) != l
                || integer(firstRead.getIndex()) != r || integer(lastTarget.getIndex()) != r) return null;
            if (statements.size() == 4 && !LoopProbe.probes(statements, null, array, bindings)) return null;
            var sites = List.of(span(source,declaration),span(source,left),span(source,right),span(source,condition),
                span(source,initializer),span(source,tempDeclaration),span(source,firstRead),span(source,first),span(source,last));
            var facts = new Facts(ab,tb,sites,l,r,"GREATER","boolean",span(source,body));
            return new ArrayAnalyzer.Sites(null,null,null,null,ArrayAnalyzer.ProbeKind.CONDITIONAL,null,null,null,null,null,null,null,null,null,facts);
        } catch (RuntimeException outsideShape) { return null; }
    }
    private static int integer(Expression e) { return Integer.parseInt(e.toString(new PrettyPrinterConfiguration().setPrintComments(false).setPrintJavadoc(false))); }
    private static SourceSnapshot.Span span(SourceSnapshot s, Node n) { return s.span(n.getRange().orElseThrow()); }
}
