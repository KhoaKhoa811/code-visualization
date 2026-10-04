import java.util.*;
import java.util.regex.*;

/** Source-specific conditional plan. Validate complete canonical records before committing state. */
final class ConditionalTracePlan implements ArrayTrace.Plan {
    static final List<String> KINDS=List.of("ARRAY_DECLARE","ARRAY_READ","ARRAY_READ","CONDITION","ARRAY_READ","VARIABLE_DECLARE","ARRAY_READ","ARRAY_WRITE","ARRAY_WRITE");
    private static final Pattern INT=Pattern.compile("\\{\"type\":\"int\",\"value\":(-?[0-9]{1,10})\\}");
    private final List<ArrayTrace.Operation> sites;
    private final int left,right;
    private int phase;
    private boolean finished;
    private int[] values;
    private int leftValue,rightValue,temp;
    ConditionalTracePlan(List<ArrayTrace.Operation> sites,int left,int right){
        this.sites=List.copyOf(sites);this.left=left;this.right=right;
        if(!sites.stream().map(ArrayTrace.Operation::kind).toList().equals(KINDS)
            ||!"variable-1".equals(sites.get(0).variableId())||!"variable-2".equals(sites.get(5).variableId())
            ||sites.stream().anyMatch(s->s.source()==null)||sites.get(0).variableName()==null||sites.get(5).variableName()==null)
            throw new IllegalArgumentException("Invalid conditional plan");
    }
    public boolean complete(){return finished;}
    public void accept(String record,int sequence){
        require(!finished&&sequence==phase+1,"Event after conditional exit or sequence gap");
        var site=sites.get(phase);
        String prefix="{\"sequence\":"+sequence+",\"kind\":"+ArrayTrace.quote(site.kind())+",\"source\":"+site.source()+",";
        String suffix=",\"scopeId\":\""+(phase<4?"scope-main":"scope-if-1")+"\",\"exitedVariableIds\":"+(phase==8?"[\"variable-2\"]":"[]")+"}";
        if(phase==0){
            prefix+="\"arrayId\":\"array-1\",\"variableId\":\"variable-1\",\"variableName\":"+ArrayTrace.quote(site.variableName())+",\"values\":[";
            String tail="]"+suffix;
            require(record.startsWith(prefix)&&record.endsWith(tail),"Malformed conditional declaration");
            String list=record.substring(prefix.length(),record.length()-tail.length());List<Integer> parsed=new ArrayList<>();
            for(int offset=0;offset<list.length();){
                var m=INT.matcher(list);m.region(offset,list.length());
                require(m.lookingAt()&&parsed.size()<ArrayTrace.ARRAY_LIMIT,"Invalid array values");
                int value=Integer.parseInt(m.group(1));require(value(value).equals(m.group()),"Noncanonical integer");parsed.add(value);offset=m.end();
                if(offset<list.length())require(list.charAt(offset++)==','&&offset<list.length(),"Invalid array separator");
            }
            values=parsed.stream().mapToInt(Integer::intValue).toArray();phase++;return;
        }
        String payload;
        int index=-1,actual=0;
        if(phase==3){
            boolean comparison=leftValue>rightValue;
            payload="\"conditionRole\":\"IF\",\"value\":{\"type\":\"boolean\",\"value\":"+comparison+"},\"comparison\":{\"operator\":\">\",\"left\":"+value(leftValue)
                +",\"right\":"+value(rightValue)+",\"leftReadSequence\":2,\"rightReadSequence\":3}";
        }else if(phase==5){
            payload="\"variableId\":\"variable-2\",\"variableName\":"+ArrayTrace.quote(site.variableName())+",\"value\":"+value(temp);
        }else{
            index=(phase==1||phase==4||phase==7)?left:right;
            require(index>=0&&index<values.length,"Invalid successful array access");
            actual=phase==7?rightValue:phase==8?temp:values[index];
            payload="\"arrayId\":\"array-1\",\"index\":"+index+",\"value\":"+value(actual);
        }
        require(record.equals(prefix+payload+suffix),"Conditional order/source/value/scope mismatch");
        if(phase==1)leftValue=actual;
        if(phase==2||phase==6)rightValue=actual;
        if(phase==4)temp=actual;
        if(phase==7||phase==8)values[index]=actual;
        if(phase==8||(phase==3&&leftValue<=rightValue))finished=true;
        phase++;
    }
    private static String value(int n){return "{\"type\":\"int\",\"value\":"+n+"}";}
    private static void require(boolean valid,String message){if(!valid)throw new IllegalArgumentException(message);}
}
