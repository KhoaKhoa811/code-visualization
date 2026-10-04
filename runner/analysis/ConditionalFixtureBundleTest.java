import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Reject damaged/missing fixture delivery before any program execution. */
public final class ConditionalFixtureBundleTest {
    public static void main(String[] args)throws Exception{
        Path original=Path.of(args[0]).resolve("conditional.txt"),directory=Files.createTempDirectory(Path.of(args[1]),"conditional-delivery-");
        var lines=Files.readAllLines(original,StandardCharsets.UTF_8);ConditionalRecordingTest.load(original);
        List<List<String>> cases=new ArrayList<>();
        cases.add(lines.subList(1,lines.size()));
        var duplicate=new ArrayList<>(lines);duplicate.set(1,duplicate.getFirst());cases.add(duplicate);
        for(int variant=0;variant<6;variant++){
            var altered=new ArrayList<>(lines);var fields=altered.getFirst().split(" ",-1);
            switch(variant){case 0->fields[0]="WRONG";case 1->fields[1]="unknown";case 2->fields[2]="%%%";case 3->fields[4]=Base64.getEncoder().encodeToString("one-name".getBytes(StandardCharsets.UTF_8));case 4->fields[7]="2147483648";case 5->fields[3]=Base64.getEncoder().encodeToString("x".repeat(65537).getBytes(StandardCharsets.UTF_8));}
            altered.set(0,String.join(" ",fields));cases.add(altered);
        }
        for(int n=0;n<cases.size();n++){
            Path file=directory.resolve("bad-"+n+".txt");Files.write(file,cases.get(n),StandardCharsets.UTF_8);
            boolean rejected=false;try{ConditionalRecordingTest.load(file);}catch(Exception|AssertionError expected){rejected=true;}
            if(!rejected)throw new AssertionError("Invalid bundle accepted: "+n);
        }
        System.out.println("PASS conditional fixture delivery: "+cases.size()+" rejection cases");
    }
}
