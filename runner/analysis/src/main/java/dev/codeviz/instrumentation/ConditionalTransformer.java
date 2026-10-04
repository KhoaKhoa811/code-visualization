package dev.codeviz.instrumentation;

import com.github.javaparser.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import dev.codeviz.analysis.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static dev.codeviz.instrumentation.ArrayTransformer.*;

/** Lowers the approved conditional shape, using captured values once and original-source facts. */
final class ConditionalTransformer {
    static Result generate(ArrayAnalyzer.Result analysis, byte[] original, int limit) throws IOException {
        var verified = new ArrayAnalyzer().analyze(original);
        if (!verified.eligible() || !Objects.equals(analysis.sites(),verified.sites()) || !analysis.bindings().equals(verified.bindings())
            || !analysis.scopes().equals(verified.scopes()) || !analysis.accesses().equals(verified.accesses())
            || !analysis.entry().equals(verified.entry()) || analysis.completeness()!=verified.completeness())
            throw new IllegalArgumentException("Inconsistent conditional facts");
        var copy=analysis.syntaxCopy().orElseThrow();
        if (!copy.equals(verified.syntaxCopy().orElseThrow())) throw new IllegalArgumentException("Inconsistent syntax copy");
        var f=analysis.sites().conditional();
        var parser=new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21).setTabSize(1));
        var main=copy.getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst();
        var old=main.getBody().orElseThrow();
        Set<String> names=new HashSet<>();copy.findAll(SimpleName.class).forEach(n->names.add(n.asString()));
        String helper=fresh(names,"__CodevizRecorder"), left=fresh(names,"__CodevizLeft"), right=fresh(names,"__CodevizRight"), decision=fresh(names,"__CodevizCondition");
        String initial=fresh(names,"__CodevizInitial"), target=fresh(names,"__CodevizTarget"), index=fresh(names,"__CodevizIndex"), rhs=fresh(names,"__CodevizRead");
        String a=f.array().name(),t=f.temporary().name(),l=Integer.toString(f.leftIndex()),r=Integer.toString(f.rightIndex());
        String guard=helper+".beforeOperation();\n";
        var source=f.sites().stream().map(s->literal(rangeJson(s))).toList();
        String lowered="{"+helper+".begin("+limit+");"+guard+old.getStatement(0)+"\n"
            +helper+".declare("+a+","+source.get(0)+","+literal(json(a))+");"+guard
            +"int "+left+"="+helper+".read("+a+","+l+","+source.get(1)+");"+guard
            +"int "+right+"="+helper+".read("+a+","+r+","+source.get(2)+");"+guard
            +"boolean "+decision+"="+left+">"+right+";"+helper+".condition("+left+","+right+","+decision+","+source.get(3)+");"
            +"if("+decision+"){"+guard+"int "+initial+"="+helper+".read("+a+","+l+","+source.get(4)+");"+guard
            +"int "+t+"="+initial+";"+helper+".tempDeclare("+t+","+source.get(5)+","+literal(json(t))+");"+guard
            // Capture the target reference/index before evaluating the original RHS access.
            +"int[] "+target+"="+a+"; int "+index+"="+l+"; int "+rhs+"="+helper+".read("+a+","+r+","+source.get(6)+");"+guard
            +helper+".write("+target+","+index+","+rhs+","+source.get(7)+");"+guard
            +helper+".finishWrite("+a+","+r+","+t+","+source.get(8)+");}";
        for(int n=2;n<old.getStatements().size();n++)lowered+=old.getStatement(n).toString();
        lowered+=helper+".end();}";
        main.setBody(parser.parseBlock(lowered).getResult().orElseThrow());
        main.addThrownException(new ClassOrInterfaceType(new ClassOrInterfaceType(new ClassOrInterfaceType(null,"java"),"io"),"IOException"));
        var recorder=parser.parse(resource("Recorder.java")).getResult().orElseThrow().getClassByName("Recorder").orElseThrow();
        recorder.getMethodsByName("event").stream().filter(m->m.getParameters().size()==4).findFirst().orElseThrow().setName("wireEvent");
        var members=parser.parse(resource("ConditionalRecorderMembers.java")).getResult().orElseThrow().getClassByName("ConditionalRecorderMembers").orElseThrow();
        members.getMembers().forEach(m->recorder.addMember(m.clone()));recorder.setName(helper);copy.addType(recorder);
        String generated=copy.toString();var output=new SourceSnapshot(generated.getBytes(StandardCharsets.UTF_8));
        var parsed=parser.parse(generated);if(!parsed.isSuccessful())throw new IllegalArgumentException("Generated syntax");
        var generatedMain=parsed.getResult().orElseThrow().getClassByName("Main").orElseThrow();
        var methods=List.of("declare","read","read","condition","read","tempDeclare","read","write","finishWrite");
        var kinds=List.of("ARRAY_DECLARE","ARRAY_READ","ARRAY_READ","CONDITION","ARRAY_READ","VARIABLE_DECLARE","ARRAY_READ","ARRAY_WRITE","ARRAY_WRITE");
        List<Site> sites=new ArrayList<>();
        for(int n=0;n<9;n++){
            final int site=n;
            var calls=generatedMain.findAll(MethodCallExpr.class).stream().filter(c->c.getScope().map(Object::toString).orElse("").equals(helper)
                &&c.getNameAsString().equals(methods.get(site))&&c.getArguments().stream().anyMatch(arg->arg.isStringLiteralExpr()
                &&arg.asStringLiteralExpr().asString().equals(rangeJson(f.sites().get(site))))).toList();
            if(calls.size()!=1)throw new IllegalArgumentException("Conditional site mapping");
            sites.add(new Site(kinds.get(n),f.sites().get(n),output.span(calls.getFirst().getRange().orElseThrow())));
        }
        return new Result(true,"",analysis.source().id(),output.id(),generated,helper,a,sites);
    }
    private static String literal(String value){return new StringLiteralExpr().setString(value).toString();}
    private static String fresh(Set<String> names,String base){String name=base;for(int n=1;!names.add(name);n++)name=base+n;return name;}
    private static String resource(String name)throws IOException{
        try(var in=Objects.requireNonNull(ConditionalTransformer.class.getResourceAsStream("/recorder/"+name))){return new String(in.readNBytes(MAX_GENERATED_BYTES+1),StandardCharsets.UTF_8);}
    }
}
