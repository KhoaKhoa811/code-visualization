import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {join} from 'node:path';
import {createHash} from 'node:crypto';
import {conditionalSemantics} from './conditional-semantics.mjs';

/** Designed contract fixtures and test-only prefix reconstruction; no Java or product renderer. */
export function conditionalContractTests({fixtures, validate, root}) {
  const success = fixtures['conditional-success.json'];
  const failed = report => report.structural.length + report.semantic.length > 0;
  const check = (result, label) => {
    const before = structuredClone(result), report = validate(result);
    assert(!failed(report), label + ': ' + JSON.stringify(report));
    assert.deepEqual(result, before, 'Validation mutated caller data');
  };
  const tests = [
    ['unknown version','structural',r=>{r.schemaVersion='draft-99';}],
    ['old version','structural',r=>{r.schemaVersion='draft-3';}],
    ['missing scope','structural',r=>{delete r.events[1].scopeId;}],
    ['empty scope','structural',r=>{r.events[1].scopeId='';}],
    ['missing exit list','structural',r=>{delete r.events[8].exitedVariableIds;}],
    ['bad exit type','structural',r=>{r.events[8].exitedVariableIds=[1];}],
    ['missing condition role','structural',r=>{delete r.events[3].conditionRole;}],
    ['loop role','structural',r=>{r.events[3].conditionRole='LOOP';}],
    ['missing comparison','structural',r=>{delete r.events[3].comparison;}],
    ['wrong operator','structural',r=>{r.events[3].comparison.operator='<';}],
    ['missing operand','structural',r=>{delete r.events[3].comparison.left;}],
    ['wrong operand type','structural',r=>{r.events[3].comparison.left={type:'boolean',value:true};}],
    ['operand overflow','structural',r=>{r.events[3].comparison.left.value=2147483648;}],
    ['fractional operand','structural',r=>{r.events[3].comparison.right.value=1.5;}],
    ['numeric boolean','structural',r=>{r.events[3].value.value=1;}],
    ['string boolean','structural',r=>{r.events[3].value.value='true';}],
    ['null boolean','structural',r=>{r.events[3].value.value=null;}],
    ['wrong boolean tag','structural',r=>{r.events[3].value.type='int';}],
    ['unsafe sequence','structural',r=>{r.events[3].comparison.leftReadSequence=9007199254740992;}],
    ['fractional link','structural',r=>{r.events[3].comparison.leftReadSequence=2.5;}],
    ['zero link','structural',r=>{r.events[3].comparison.leftReadSequence=0;}],
    ['unknown comparison field','structural',r=>{r.events[3].comparison.color='red';}],
    ['condition target','structural',r=>{r.events[3].arrayId='array-1';}],
    ['sequence gap','semantic',r=>{r.events[2].sequence=9;}],
    ['wrong boundary','semantic',r=>{r.safePlaybackBoundary.lastSafeStep=8;}],
    ['split exit boundary','semantic',r=>{r.safePlaybackBoundary.lastSafeEventSequence=8;}],
    ['backward source','semantic',r=>{r.events[3].source.end.column=1;}],
    ['reversed links','semantic',r=>{Object.assign(r.events[3].comparison,{leftReadSequence:3,rightReadSequence:2});}],
    ['stale link','semantic',r=>{r.events[3].comparison.leftReadSequence=1;}],
    ['future link','semantic',r=>{r.events[3].comparison.rightReadSequence=5;}],
    ['invented operand','semantic',r=>{r.events[3].comparison.left.value=9;}],
    ['wrong result','semantic',r=>{r.events[3].value.value=false;}],
    ['cross-scope operand','semantic',r=>{r.events[2].scopeId='other';}],
    ['non-read operand','semantic',r=>{r.events[2].kind='ARRAY_WRITE';}],
    ['unknown array','semantic',r=>{r.events[1].arrayId='missing';}],
    ['bad read index','semantic',r=>{r.events[1].index=2;}],
    ['false read value','semantic',r=>{r.events[1].value.value=99;}],
    ['bad store index','semantic',r=>{r.events[7].index=2;}],
    ['duplicate binding','semantic',r=>{r.events[5].variableId='variable-1';}],
    ['missing retirement','semantic',r=>{r.events[8].exitedVariableIds=[];}],
    ['unknown retirement','semantic',r=>{r.events[8].exitedVariableIds=['missing'];}],
    ['array retirement','semantic',r=>{r.events[8].exitedVariableIds=['variable-1'];}],
    ['duplicate retirement','semantic',r=>{r.events[8].exitedVariableIds=['variable-2','variable-2'];}],
    ['wrong-scope retirement','semantic',r=>{r.events[8].scopeId='other';}],
    ['root retirement','semantic',r=>{r.events[5].scopeId=r.events[8].scopeId='scope-main';}],
    ['read retirement','semantic',r=>{r.events[6].exitedVariableIds=['variable-2'];}],
    ['true condition retirement','semantic',r=>{r.events[3].exitedVariableIds=['variable-1'];}],
    ['write after scope close','semantic',r=>{append(r,r.events[8]);}],
    ['reopen scope','semantic',r=>{append(r,{...r.events[5],variableId:'another'});}],
    ['reuse retired identity','semantic',r=>{append(r,{...r.events[5],scopeId:'other'});}],
    ['retired scalar write','semantic',r=>{append(r,{kind:'VARIABLE_WRITE',source:r.events[8].source,scopeId:'scope-main',variableId:'variable-2',value:{type:'int',value:4},exitedVariableIds:[]});}],
    ['unexplained failure','semantic',r=>{r.executionOutcome='failed';}],
  ];
  function append(r,event) {
    r.events.push({...structuredClone(event),sequence:r.events.length+1});
    r.safePlaybackBoundary={lastSafeStep:r.events.length,lastSafeEventSequence:r.events.length};
  }
  for (const [label,layer,mutate] of tests) {
    const result=structuredClone(success); mutate(result);
    const before=structuredClone(result),report=validate(result);
    assert(report[layer].length>0,label+' was not rejected at '+layer);
    if(layer==='semantic')assert.equal(report.structural.length,0,label+' wrong layer');
    assert.deepEqual(result,before,label+' mutated caller');
  }
  // False does not retire a loop index or the array binding.
  const wrongFalse=structuredClone(fixtures['conditional-false.json']);
  wrongFalse.events[3].exitedVariableIds=['variable-1'];
  assert(failed(validate(wrongFalse)));
  const relabeledLoop=structuredClone(fixtures['loop-success.json']);
  relabeledLoop.schemaVersion='draft-4';
  assert(validate(relabeledLoop).structural.length>0,'Old loop cannot be relabeled as a conditional trace');
  for(let count=1;count<=9;count++) {
    const partial=structuredClone(fixtures['conditional-limit.json']);
    partial.events=structuredClone(success.events.slice(0,count));
    partial.safePlaybackBoundary={lastSafeStep:count,lastSafeEventSequence:count};
    check(partial,'safe prefix '+count);
  }
  // Rejected write/exit must not alter internal state used to check the following read.
  const atomic=structuredClone(fixtures['conditional-limit.json']);
  append(atomic,{...success.events[8],index:0,value:{type:'int',value:999},exitedVariableIds:['missing']});
  append(atomic,{...success.events[6],index:0,value:{type:'int',value:1}});
  const errors=conditionalSemantics(atomic);
  assert(errors.some(e=>e.startsWith('event 8:')));
  assert(!errors.some(e=>e.startsWith('event 9:')),'Rejected event changed array or scope state');
  const renamed=structuredClone(success);
  for(const e of renamed.events){e.scopeId='renamed-'+e.scopeId;if(e.variableId)e.variableId='renamed-'+e.variableId;if(e.arrayId)e.arrayId='renamed-'+e.arrayId;e.exitedVariableIds=e.exitedVariableIds.map(id=>'renamed-'+id);}
  check(renamed,'identities are not display names');
  const extremes=structuredClone(success);
  extremes.events[0].values=[{type:'int',value:2147483647},{type:'int',value:-2147483648}];
  for(const index of [1,4,5,8])extremes.events[index].value.value=2147483647;
  for(const index of [2,6,7])extremes.events[index].value.value=-2147483648;
  extremes.events[3].comparison.left.value=2147483647;extremes.events[3].comparison.right.value=-2147483648;
  check(extremes,'comparison of int extremes without subtraction overflow');

  const sourceNames={'conditional-limit':'conditional-success','conditional-cancel':'conditional-success','conditional-output-only':'conditional-success'};
  const names=Object.keys(fixtures).filter(n=>n.startsWith('conditional-'));
  for(const name of names){
    const result=fixtures[name],stem=name.slice(0,-5),source=readFileSync(join(root,'examples/sources',(sourceNames[stem]??stem)+'.java'),'utf8');
    check(result,name);
    assert.equal(result.sourceId,createHash('sha256').update(source).digest('hex'));
    const lines=source.split('\n'),same=stem==='conditional-same-index';
    const expectedHighlights=[lines[2].trim().slice(0,-1),'values[0]',same?'values[0]':'values[1]',same?'values[0] > values[0]':'values[0] > values[1]','values[0]','int temp = values[0]','values[1]','values[0] = values[1]','values[1] = temp'];
    for(const [i,e]of(result.events??[]).entries()){
      assert.equal(e.source.start.line,[3,4,4,4,5,5,6,6,7][i],name+' operation site '+i);
      assert.equal(e.source.start.line,e.source.end.line);
      assert.equal(lines[e.source.start.line-1].slice(e.source.start.column-1,e.source.end.column-1),expectedHighlights[i],name+' highlight '+i);
    }
    if(result.events?.length>=4){
      assert(result.events[1].source.start.column<result.events[2].source.start.column,'Operand source order');
      assert.equal(result.events[1].source.start.column,result.events[3].source.start.column);
      assert.equal(result.events[2].source.end.column,result.events[3].source.end.column);
    }
    if(!result.events)continue;
    const initial=stem==='conditional-false'?[1,3]:stem==='conditional-equal'?[2,2]:stem==='conditional-left-error'?[]:['conditional-same-index','conditional-right-error'].includes(stem)?[3]:[3,1];
    const snapshots=[];
    for(let count=0;count<=result.events.length;count++){
      const bindings=count?{'variable-1':'array-1'}:{};
      if(count>=6&&count<9)bindings['variable-2']=3;
      const array=count===9?[1,3]:count===8?[1,1]:initial;
      const selected=result.events[count-1];
      const expected={bindings,arrays:count?{'array-1':array}:{},highlight:selected?.source??null,comparison:count===4?{left:initial[0],right:same?initial[0]:initial[1],value:stem==='conditional-success'||['conditional-limit','conditional-cancel'].includes(stem)}:null};
      snapshots.push(expected);assert.deepEqual(replay(result.events,count),expected,name+' cursor '+count);
    }
    for(let count=result.events.length;count>=0;count--)assert.deepEqual(replay(result.events,count),snapshots[count],name+' backward '+count);
  }
  assert.throws(()=>JSON.parse(JSON.stringify(success).slice(0,-1)),SyntaxError);
  console.log(`PASS draft-4: ${names.length} designed fixtures, ${tests.length+2} rejection cases, 9 partial prefixes, atomic rejection, int extremes, source hashes/highlights and forward/backward fixture states`);
}

function replay(events,count){
  const bindings={},arrays={};
  for(const e of events.slice(0,count)){
    if(e.kind==='ARRAY_DECLARE'){bindings[e.variableId]=e.arrayId;arrays[e.arrayId]=e.values.map(v=>v.value);}
    if(e.kind==='VARIABLE_DECLARE'||e.kind==='VARIABLE_WRITE')bindings[e.variableId]=e.value.value;
    if(e.kind==='ARRAY_WRITE')arrays[e.arrayId][e.index]=e.value.value;
    for(const id of e.exitedVariableIds)delete bindings[id];
  }
  const selected=events[count-1];
  return {bindings,arrays,highlight:selected?.source??null,comparison:selected?.kind==='CONDITION'?{left:selected.comparison.left.value,right:selected.comparison.right.value,value:selected.value.value}:null};
}
