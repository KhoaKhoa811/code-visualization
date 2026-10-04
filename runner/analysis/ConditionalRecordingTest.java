import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Trusted driver; all submitted original/generated Java executes in RunnerHarness Docker containers. */
public final class ConditionalRecordingTest {
    static final List<String> CASES=cases();
    private static List<String> cases(){var names=new ArrayList<>(List.of("success","false","equal","same-index","empty","one","negative-left","negative-right","high-left","high-right","min-left","max-right","negative-values","extremes","reversed","nonadjacent","sixteen","renamed","formatted","collision","no-probe"));for(int n=1;n<=9;n++)names.add("limit"+n);return List.copyOf(names);}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),results=root.resolve(".results/conditionals");Files.createDirectories(results);
        var artifacts=load(Path.of(args[1]).resolve("conditional.txt"));int comparisons=0;
        for(var fields:artifacts){
            String name=fields[1],original=decode(fields[2]),generated=decode(fields[3]);
            String[] names=decode(fields[4]).split("\n"),ranges=decode(fields[5]).split("\n");
            List<ArrayTrace.Operation> sites=new ArrayList<>();for(int n=0;n<9;n++)sites.add(new ArrayTrace.Operation(ConditionalTracePlan.KINDS.get(n),ranges[n],n==0?"variable-1":n==5?"variable-2":null,n==0?names[0]:n==5?names[1]:null));
            var trace=new ArrayTrace(new ConditionalTracePlan(sites,Integer.parseInt(fields[7]),Integer.parseInt(fields[8])));var harness=new RunnerHarness();
            var actual=harness.run(generated.getBytes(StandardCharsets.UTF_8),new AtomicBoolean(),new AtomicBoolean(),trace);var captured=trace.seal();
            boolean limited=name.startsWith("limit")&&!name.equals("limit9"),failed=Set.of("empty","one","negative-left","negative-right","high-left","high-right","min-left","max-right").contains(name);
            var expected=limited?RunnerHarness.Outcome.TRACE_LIMIT:failed?RunnerHarness.Outcome.RUNTIME_FAILURE:RunnerHarness.Outcome.SUCCESS;
            int count=limited?Integer.parseInt(name.substring(5)):failed?Set.of("one","negative-right","high-right","max-right").contains(name)?2:1:Set.of("false","equal","same-index").contains(name)?4:9;
            check(actual.cleanupVerified()&&actual.outcome()==expected,name+": "+actual.outcome()+" "+actual.detail()+" "+actual.stderr());
            check(captured.events().size()==count&&captured.complete()==(expected==RunnerHarness.Outcome.SUCCESS),name+" trace "+captured);
            if(!limited){
                var baseline=harness.run(original.getBytes(StandardCharsets.UTF_8),new AtomicBoolean(),new AtomicBoolean());
                check(baseline.cleanupVerified()&&baseline.outcome()==expected&&baseline.stdout().equals(actual.stdout()),name+" original output/cleanup");
                check(failed?baseline.stderr().lines().findFirst().equals(actual.stderr().lines().findFirst()):baseline.stderr().equals(actual.stderr()),name+" original exception/stderr");comparisons++;
            }
            String raw="{\"runId\":"+json(actual.container())+",\"sourceId\":"+json(ArrayRecordingTest.sha(original))+",\"instrumentedSourceId\":"+json(actual.sourceSha256())
                +",\"originalSource\":"+json(original)+",\"instrumentedSource\":"+json(generated)+",\"metadata\":"+decode(fields[6])+",\"outcome\":"+json(actual.outcome().name())
                +",\"stdout\":"+json(actual.stdout())+",\"stderr\":"+json(actual.stderr())+",\"complete\":"+captured.complete()+",\"problem\":"+json(captured.problem())+",\"events\":["+String.join(",",captured.events())+"]}";
            Files.writeString(results.resolve(name+".raw.json"),raw,StandardCharsets.UTF_8);
            System.out.printf("PASS conditional %-18s outcome=%s events=%d cleanup=true%n",name,expected,count);
        }
        check(comparisons==22,"Missing original comparisons");System.out.println("PASS 30 conditional executions and 22 original/generated comparisons");
    }
    static List<String[]> load(Path file)throws Exception{
        check(Files.size(file)<=1048576,"Fixture batch cap");var lines=Files.readAllLines(file,StandardCharsets.UTF_8);check(lines.size()==30,"Fixture count");
        List<String[]> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(String line:lines){var f=line.split(" ",-1);check(f.length==9&&f[0].equals("CONDITIONAL_ARTIFACT")&&CASES.contains(f[1])&&seen.add(f[1]),"Invalid/duplicate fixture");
            for(int n=2;n<=6;n++)check(!decode(f[n]).isEmpty(),"Empty payload");
            check(Base64.getDecoder().decode(f[2]).length<=65536&&Base64.getDecoder().decode(f[3]).length<=65536,"Source cap");
            check(decode(f[4]).split("\n",-1).length==2&&decode(f[5]).split("\n",-1).length==9,"Invalid plan fields");
            Integer.parseInt(f[7]);Integer.parseInt(f[8]);result.add(f);
        }check(seen.equals(new HashSet<>(CASES)),"Missing cases");return List.copyOf(result);
    }
    static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
    static String json(String s){return ArrayRecordingTest.json(s);}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
