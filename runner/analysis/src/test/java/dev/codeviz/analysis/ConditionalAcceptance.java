package dev.codeviz.analysis;

import com.github.javaparser.*;
import com.github.javaparser.ast.expr.*;
import dev.codeviz.instrumentation.ArrayTransformer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Trusted acceptance and fixture export, run only in the isolated analysis worker. */
final class ConditionalAcceptance {
    record Example(String name,String source,int limit){}
    static String source(String values,int l,int r){return "public class Main { public static void main(String[] args) { int[] values = {"+values+"}; if (values["+l+"] > values["+r+"]) { int temp = values["+l+"]; values["+l+"] = values["+r+"]; values["+r+"] = temp; } System.out.print(\"FINAL=\" + java.util.Arrays.toString(values)); System.err.print(\"PROBE\"); } }";}
    static void run()throws Exception{
        String base=source("3, 1",0,1);var analyzer=new ArrayAnalyzer();var transformer=new ArrayTransformer();
        var facts=analyzer.analyze(bytes(base));check(facts.eligible(),facts.diagnostics().toString());
        var f=facts.sites().conditional();check(facts.completeness()==ArrayAnalyzer.Completeness.COMPLETE_FOR_CONDITIONAL_PROBE,"conditional completeness");
        check(f.sites().size()==9&&f.operator().equals("GREATER")&&f.resultType().equals("boolean"),"condition facts");
        check(facts.scopes().stream().anyMatch(s->s.id().equals(f.temporary().scopeId())&&s.parentId().equals(f.array().scopeId())),"branch scope parent");
        check(facts.accesses().size()==6&&facts.accesses().stream().allMatch(a->a.bindingId().equals(f.array().id())&&a.elementType().equals("int")),"resolved array accesses");
        String original=facts.syntaxCopy().orElseThrow().toString();
        check(!transformer.transform(facts,bytes(base+" "),32).ready(),"stale source");
        for(String field:List.of("bindings","scopes","accesses"))check(!transformer.transform(LoopAcceptance.with(facts,field,List.of()),bytes(base),32).ready(),"missing "+field);
        for(String field:List.of("leftIndex","rightIndex")){
            var bad=LoopAcceptance.replaceRecord(f,field,9);
            check(!transformer.transform(LoopAcceptance.with(facts,"sites",LoopAcceptance.replaceRecord(facts.sites(),"conditional",bad)),bytes(base),32).ready(),"altered "+field);
        }
        var swapped=new ArrayList<>(f.sites());Collections.swap(swapped,1,2);
        var badSites=LoopAcceptance.replaceRecord(f,"sites",swapped);
        check(!transformer.transform(LoopAcceptance.with(facts,"sites",LoopAcceptance.replaceRecord(facts.sites(),"conditional",badSites)),bytes(base),32).ready(),"swapped source sites");
        List<String> excluded=new ArrayList<>();
        for(String condition:List.of("values[0] < values[1]","values[0] >= values[1]","values[0] == values[1]","(values[0] > values[1])","values[0] > 1","values[0] > values[1] && true","values[0] > values[1]++","values[0] > values[1+0]"))excluded.add(base.replace("values[0] > values[1]",condition));
        for(String body:List.of("int temp=values[1]; values[0]=values[1]; values[1]=temp;","int temp=values[0]; values[1]=values[1]; values[1]=temp;","int temp=values[0]; values[0]=values[1]; values[0]=temp;","int temp=values[0]; values[0]=values[1]; values[1]=3;","int temp=values[0]; values[0]+=values[1]; values[1]=temp;","int temp=values[0]; values[0]=values[1]; values[1]=temp; System.out.print(temp);","int temp=values[0]; if(true) { values[0]=values[1]; } values[1]=temp;"))excluded.add(base.replace("int temp = values[0]; values[0] = values[1]; values[1] = temp;",body));
        excluded.add(base.replace("} System.out","} else {} System.out"));excluded.add(base.replace("int[] values","final int[] values"));
        excluded.add(base.replace("int temp","long temp"));excluded.add(base.replace("int temp","final int temp"));
        excluded.add(base.replace("{3, 1}","null"));excluded.add(base.replace("{3, 1}","{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}"));
        excluded.add(base.replace("values","java"));excluded.add(base.replace("temp","System"));
        excluded.add(base.replace("values[0] >","other[0] >"));excluded.add(base.replace("int[] values","int x=0; int[] values"));
        for(String s:excluded){var bad=analyzer.analyze(bytes(s));check(!bad.eligible()&&!transformer.transform(bad,bytes(s),32).ready(),"Excluded source accepted: "+s);}
        List<Example> examples=new ArrayList<>();
        examples.add(new Example("success",base,32));examples.add(new Example("false",source("1, 3",0,1),32));examples.add(new Example("equal",source("2, 2",0,1),32));
        examples.add(new Example("same-index",source("3",0,0),32));examples.add(new Example("empty",source("",0,1),32));examples.add(new Example("one",source("3",0,1),32));
        examples.add(new Example("negative-left",source("3, 1",-1,1),32));examples.add(new Example("negative-right",source("3, 1",0,-1),32));
        examples.add(new Example("high-left",source("3, 1",2,1),32));examples.add(new Example("high-right",source("3, 1",0,2),32));
        examples.add(new Example("min-left",source("3, 1",Integer.MIN_VALUE,1),32));examples.add(new Example("max-right",source("3, 1",0,Integer.MAX_VALUE),32));
        examples.add(new Example("negative-values",source("-1, -3",0,1),32));examples.add(new Example("extremes",source("2147483647, -2147483648",0,1),32));
        examples.add(new Example("reversed",source("1, 3",1,0),32));examples.add(new Example("nonadjacent",source("8, 4, 2",0,2),32));
        examples.add(new Example("sixteen",source("15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0",0,15),32));
        examples.add(new Example("renamed",base.replace("values","items").replace("temp","saved"),32));
        examples.add(new Example("formatted",base.replace("values","caf\u00e9").replace("temp","saved").replace(";",";\r\n\t").replace(" > "," /* \ud83d\ude00 */ >\r\n\t"),32));
        examples.add(new Example("collision",base.replace("values","__CodevizLeft").replace("temp","__CodevizRead").replace("args","__CodevizRecorder"),32));
        examples.add(new Example("no-probe",base.substring(0,base.indexOf("System.out"))+"} }",32));
        for(int n=1;n<=9;n++)examples.add(new Example("limit"+n,base,n));
        check(examples.size()==30,"fixture count");
        for(var e:examples){
            var analysis=analyzer.analyze(bytes(e.source()));var generated=transformer.transform(analysis,bytes(e.source()),e.limit());
            check(generated.ready(),e.name()+": "+generated.diagnostic()+" "+analysis.diagnostics());var cf=analysis.sites().conditional();
            var parsed=new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)).parse(generated.generatedSource()).getResult().orElseThrow();
            var main=parsed.getClassByName("Main").orElseThrow().getMethodsByName("main").getFirst();
            check(main.findAll(ArrayAccessExpr.class).isEmpty(),"duplicate array access");
            check(main.findAll(MethodCallExpr.class).stream().filter(c->c.getNameAsString().equals("read")).count()==4,"four original reads");
            check(main.findAll(MethodCallExpr.class).stream().filter(c->c.getNameAsString().equals("beforeOperation")).count()==9,"nine preguards");
            check(main.findAll(BinaryExpr.class).stream().filter(b->b.getOperator()==BinaryExpr.Operator.GREATER).count()==1,"one comparison");
            var branch=main.findAll(com.github.javaparser.ast.stmt.IfStmt.class).getFirst();
            var conditionCall=main.findAll(MethodCallExpr.class).stream().filter(c->c.getNameAsString().equals("condition")).findFirst().orElseThrow();
            check(branch.getCondition().equals(conditionCall.getArgument(2)),"branch reuses captured boolean");
            check(generated.sites().stream().map(ArrayTransformer.Site::original).toList().equals(cf.sites()),"nine exact source sites");
            String metadata="{\"sourceId\":"+ArrayTransformer.json(generated.originalId())+",\"generatedId\":"+ArrayTransformer.json(generated.generatedId())+",\"names\":["+ArrayTransformer.json(cf.array().name())+","+ArrayTransformer.json(cf.temporary().name())+"],\"sites\":["
                +String.join(",",generated.sites().stream().map(s->"{\"kind\":"+ArrayTransformer.json(s.kind())+",\"original\":"+ArrayTransformer.rangeJson(s.original())+",\"generated\":"+ArrayTransformer.rangeJson(s.generated())+"}").toList())+"]}";
            System.out.println("CONDITIONAL_ARTIFACT "+e.name()+" "+encode(e.source())+" "+encode(generated.generatedSource())+" "+encode(cf.array().name()+"\n"+cf.temporary().name())+" "+encode(String.join("\n",cf.sites().stream().map(ArrayTransformer::rangeJson).toList()))+" "+encode(metadata)+" "+cf.leftIndex()+" "+cf.rightIndex());
        }
        check(facts.syntaxCopy().orElseThrow().toString().equals(original),"original syntax unchanged");
        System.out.println("PASS: 30 conditional fixtures; immutable binding/scope/source facts and "+excluded.size()+" rejected forms");
    }
    private static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    private static String encode(String s){return Base64.getEncoder().encodeToString(bytes(s));}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
