import java.nio.charset.StandardCharsets;
import java.util.*;

/** Trusted collector checks using designed wire records; no submitted Java executes here. */
public final class ConditionalTraceTest {
    static final List<ArrayTrace.Operation> SITES=sites();
    static List<ArrayTrace.Operation> sites(){var result=new ArrayList<ArrayTrace.Operation>();for(int n=0;n<9;n++)result.add(new ArrayTrace.Operation(ConditionalTracePlan.KINDS.get(n),"{\"file\":\"Main.java\",\"start\":{\"line\":1,\"column\":"+(n+1)+"},\"end\":{\"line\":1,\"column\":"+(n+2)+"}}",n==0?"variable-1":n==5?"variable-2":null,n==0?"values":n==5?"temp":null));return List.copyOf(result);}
    static String value(int n){return "{\"type\":\"int\",\"value\":"+n+"}";}
    static List<String> records(boolean condition){
        int l=condition?3:1,r=condition?1:3;var result=new ArrayList<String>();
        for(int n=0;n<(condition?9:4);n++){
            String payload=switch(n){
                case 0->"\"arrayId\":\"array-1\",\"variableId\":\"variable-1\",\"variableName\":\"values\",\"values\":["+value(l)+","+value(r)+"]";
                case 3->"\"conditionRole\":\"IF\",\"value\":{\"type\":\"boolean\",\"value\":"+condition+"},\"comparison\":{\"operator\":\">\",\"left\":"+value(l)+",\"right\":"+value(r)+",\"leftReadSequence\":2,\"rightReadSequence\":3}";
                case 5->"\"variableId\":\"variable-2\",\"variableName\":\"temp\",\"value\":"+value(l);
                default->"\"arrayId\":\"array-1\",\"index\":"+(Set.of(1,4,7).contains(n)?0:1)+",\"value\":"+value(Set.of(1,4,8).contains(n)?l:r);
            };
            result.add("{\"sequence\":"+(n+1)+",\"kind\":"+ArrayTrace.quote(SITES.get(n).kind())+",\"source\":"+SITES.get(n).source()+","+payload+",\"scopeId\":\""+(n<4?"scope-main":"scope-if-1")+"\",\"exitedVariableIds\":"+(n==8?"[\"variable-2\"]":"[]")+"}");
        }return result;
    }
    public static void main(String[] args){
        var success=records(true);int corrupt=0;
        for(boolean decision:List.of(false,true)){
            var records=records(decision);var trace=new ArrayTrace(new ConditionalTracePlan(SITES,0,1));
            byte[] bytes=(String.join("\n",records)+"\n{\"transport\":\"end\"}\n").getBytes(StandardCharsets.UTF_8);
            for(byte b:bytes)trace.accept(new byte[]{b},1);trace.finish();check(trace.seal().complete(),"chunked complete");
        }
        for(int at=0;at<9;at++){
            String good=success.get(at);
            List<String> bad=new ArrayList<>(List.of(good.replace("\"sequence\":"+(at+1),"\"sequence\":99"),good.replace("Main.java","Wrong.java"),good.replace("scope-","wrong-scope-"),good.replace("\"value\":3","\"value\":99").replace("\"value\":1","\"value\":99")));
            if(at==0)bad.remove(3); // Initial values are runtime captures, not inferred from source literals.
            if(at==3){bad.add(good.replace("\"value\":true","\"value\":false"));bad.add(good.replace("leftReadSequence\":2","leftReadSequence\":3"));}
            if(at==8)bad.add(good.replace("[\"variable-2\"]","[]"));
            for(String record:bad){
                var plan=new ConditionalTracePlan(SITES,0,1);for(int n=0;n<at;n++)plan.accept(success.get(n),n+1);
                boolean rejected=false;try{plan.accept(record,at+1);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"corrupt record accepted at "+at);
                // A valid retry proves the failed event did not advance phase or mutate the pending state.
                plan.accept(good,at+1);for(int n=at+1;n<9;n++)plan.accept(success.get(n),n+1);check(plan.complete(),"atomic retry");corrupt++;
            }
        }
        for(int count=0;count<9;count++){
            var trace=new ArrayTrace(new ConditionalTracePlan(SITES,0,1));feed(trace,String.join("\n",success.subList(0,count))+(count==0?"":"\n")+"{\"transport\":\"end\"}\n");trace.finish();check(!trace.seal().complete(),"premature end");
        }
        String last=success.get(8)+"\n";
        for(int end=0;end<last.length();end++){
            var trace=new ArrayTrace(new ConditionalTracePlan(SITES,0,1));feed(trace,String.join("\n",success.subList(0,8))+"\n"+last.substring(0,end));trace.finish();check(trace.seal().events().size()==8&&!trace.seal().complete(),"truncated exit committed");
        }
        var falsePlan=new ConditionalTracePlan(SITES,0,1);var falseRecords=records(false);for(int n=0;n<4;n++)falsePlan.accept(falseRecords.get(n),n+1);
        boolean rejected=false;try{falsePlan.accept(success.get(4),5);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"body after false");
        var cap=new ArrayTrace(new ConditionalTracePlan(SITES,0,1));feed(cap,"x".repeat(65537));check(cap.seal().bytes()==65536&&!cap.seal().complete(),"byte cap");
        System.out.println("PASS conditional collector: "+corrupt+" corrupt records/atomic retries, 9 premature ends, false-branch rejection, byte cap and "+last.length()+" final-record truncations");
    }
    private static void feed(ArrayTrace trace,String s){byte[] bytes=s.getBytes(StandardCharsets.UTF_8);trace.accept(bytes,bytes.length);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
