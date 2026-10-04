import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {replayConditional} from '../prototype/recording/conditional-replay.mjs';
const root=dirname(fileURLToPath(import.meta.url));
const names=['success','false','equal','same-index','empty','one','negative-left','negative-right','high-left','high-right','min-left','max-right','negative-values','extremes','reversed','nonadjacent','sixteen','renamed','formatted','collision','no-probe',...Array.from({length:9},(_,n)=>'limit'+(n+1))];
const hash=s=>createHash('sha256').update(s).digest('hex');
function slice(source,range){const lines=source.split('\n');const {start,end}=range;return start.line===end.line?lines[start.line-1].slice(start.column-1,end.column-1):[lines[start.line-1].slice(start.column-1),...lines.slice(start.line,end.line-1),lines[end.line-1].slice(0,end.column-1)].join('\n');}
const clean=s=>s.replace(/\/\*[\s\S]*?\*\//g,'').replace(/\s+/g,'');
for(const name of names){
 const raw=JSON.parse(readFileSync(resolve(root,'.results/conditionals',name+'.raw.json'),'utf8'));
 assert.equal(hash(raw.originalSource),raw.sourceId);assert.equal(hash(raw.instrumentedSource),raw.instrumentedSourceId);
 assert.equal(raw.metadata.sourceId,raw.sourceId);assert.equal(raw.metadata.generatedId,raw.instrumentedSourceId);
 const initial=({false:[1,3],equal:[2,2],'same-index':[3],empty:[],one:[3],'negative-values':[-1,-3],extremes:[2147483647,-2147483648],reversed:[1,3],nonadjacent:[8,4,2],sixteen:Array.from({length:16},(_,n)=>15-n)})[name]??[3,1];
 const l=({'negative-left':-1,'high-left':2,'min-left':-2147483648,reversed:1})[name]??0;
 const r=({'same-index':0,'negative-right':-1,'high-right':2,'max-right':2147483647,reversed:0,nonadjacent:2,sixteen:15})[name]??1;
 const [a,t]=raw.metadata.names,sites=raw.metadata.sites;
 assert.deepEqual([a,t],name==='renamed'?['items','saved']:name==='formatted'?['caf\u00e9','saved']:name==='collision'?['__CodevizLeft','__CodevizRead']:['values','temp']);
 const texts=[`int[]${a}={${initial.join(',')}}`,`${a}[${l}]`,`${a}[${r}]`,`${a}[${l}]>${a}[${r}]`,`${a}[${l}]`,`int${t}=${a}[${l}]`,`${a}[${r}]`,`${a}[${l}]=${a}[${r}]`,`${a}[${r}]=${t}`];
 assert.equal(sites.length,9);sites.forEach((s,n)=>{assert.equal(clean(slice(raw.originalSource,s.original)),texts[n]);assert.match(slice(raw.instrumentedSource,s.generated),/^__CodevizRecorder\d*\.(declare|read|condition|tempDeclare|write|finishWrite)\(/);});
 const limited=name.startsWith('limit')&&name!=='limit9',failed=l<0||l>=initial.length||r<0||r>=initial.length;
 const fullCount=failed?(l<0||l>=initial.length?1:2):initial[l]>initial[r]?9:4;
 const count=limited?Number(name.slice(5)):fullCount;
 assert.equal(raw.events.length,count);assert.equal(raw.complete,!limited&&!failed);assert.equal(raw.outcome,limited?'TRACE_LIMIT':failed?'RUNTIME_FAILURE':'SUCCESS');
 for(let step=0;step<=count;step++){
   const bindings={},arrays={},values=[...initial];
   if(step>=8)values[l]=initial[r];if(step===9)values[r]=initial[l];
   if(step){bindings['variable-1']={name:a,arrayId:'array-1'};arrays['array-1']=values;}
   if(step>=6&&step<9)bindings['variable-2']={name:t,value:initial[l]};
   const readIndex=[2,5].includes(step)?l:[3,7].includes(step)?r:null;
   const expected={bindings,arrays,highlight:step?sites[step-1].original:null,comparison:step===4?{left:initial[l],right:initial[r],value:initial[l]>initial[r]}:null,read:readIndex===null?null:{arrayId:'array-1',index:readIndex,value:initial[readIndex]}};
   assert.deepEqual(replayConditional(raw.events,step),expected,`${name} forward ${step}`);
   replayConditional(raw.events,count);assert.deepEqual(replayConditional(raw.events,step),expected,`${name} backward ${step}`);
   assert.deepEqual(replayConditional(raw.events.slice(0,step),step),expected,`${name} independent prefix ${step}`);
   if(step){assert.equal(raw.events[step-1].sequence,step);assert.equal(raw.events[step-1].kind,sites[step-1].kind);assert.deepEqual(raw.events[step-1].source,sites[step-1].original);assert.deepEqual(raw.events[step-1].exitedVariableIds,step===9?['variable-2']:[]);}
 }
 const copy=structuredClone(raw.events),state=replayConditional(raw.events,count);if(state.arrays['array-1'])state.arrays['array-1'][0]=99;if(state.highlight)state.highlight.start.line=999;assert.deepEqual(raw.events,copy);
 assert.throws(()=>replayConditional(raw.events,-1),RangeError);assert.throws(()=>replayConditional(raw.events,count+1),RangeError);
 if(raw.complete){const final=[...initial];if(initial[l]>initial[r]){final[l]=initial[r];final[r]=initial[l];}assert.equal(raw.stdout,name==='no-probe'?'':`FINAL=[${final.join(', ')}]`);assert.equal(raw.stderr,name==='no-probe'?'':'PROBE');}
 const [executionOutcome,category]=limited?['limited','trace_limit']:failed?['failed','runtime_error']:['completed',null];
 const result={runId:raw.runId,sourceId:raw.sourceId,schemaVersion:'draft-4',executionOutcome,visualizationCoverage:raw.complete?'complete':'partial',diagnostics:category?[{category,message:raw.problem||raw.outcome,source:null}]:[],events:raw.events,safePlaybackBoundary:{lastSafeStep:count,lastSafeEventSequence:count},console:{stdout:raw.stdout,stderr:raw.stderr}};
 const file=resolve(root,'.results/conditionals',name+'.result.json');writeFileSync(file,JSON.stringify(result,null,2)+'\n');
 const checked=spawnSync(process.execPath,[resolve(root,'../../contracts/validate.mjs'),file],{encoding:'utf8',timeout:5000,maxBuffer:65536});assert.equal(checked.status,0,checked.error?.message||checked.stdout+checked.stderr);
 console.log(`PASS ${name}: draft-4, source hashes/sites and all forward/backward prefixes`);
}
console.log('PASS 30 conditional runtime results; no Java execution during reconstruction');
