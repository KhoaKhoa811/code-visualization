import assert from 'node:assert/strict';
import {readFileSync, writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {dirname, resolve} from 'node:path';
import {replayLoop} from '../prototype/recording/loop-replay.mjs';

const root = dirname(fileURLToPath(import.meta.url));
const variants = ['success','renamed','equal','repeat','empty','one','start1','startlength','startabove','negative','minindex','maxindex',
  'minvalue','maxvalue','formatted','collision','no-probe','seven','eight','sixteen','sixteen-start9','negativeadd','zeroadd','minadd','maxadd',
  ...Array.from({length:15}, (_, n) => `limit${n+1}`)];
const hash = source => createHash('sha256').update(source, 'utf8').digest('hex');
function slice(source, range) {
  assert.equal(range.file, 'Main.java');
  const lines = source.split(/\r\n|\r|\n/), {start, end} = range;
  assert.ok(start.line >= 1 && end.line <= lines.length && start.line <= end.line);
  assert.ok(start.column >= 1 && start.column <= lines[start.line-1].length+1 && end.column >= 1 && end.column <= lines[end.line-1].length+1);
  return start.line === end.line ? lines[start.line-1].slice(start.column-1, end.column-1)
    : [lines[start.line-1].slice(start.column-1), ...lines.slice(start.line, end.line-1), lines[end.line-1].slice(0,end.column-1)].join('\n');
}
const clean = text => text.replace(/\/\*[\s\S]*?\*\//g,'').replace(/\s+/g,'');
// Test oracle only. Production replay applies captured values without evaluating arithmetic.
const intSum = (a,b) => Number(BigInt.asIntN(32, BigInt(a) + BigInt(b)));
function verify(raw, variant, prefix) {
  assert.equal(hash(raw.originalSource), raw.sourceId, 'Stale source');
  assert.equal(hash(raw.instrumentedSource), raw.instrumentedSourceId, 'Stale generated source');
  assert.equal(raw.metadata.sourceId, raw.sourceId); assert.equal(raw.metadata.generatedId, raw.instrumentedSourceId);
  assert.equal(raw.metadata.sites.length, 6);
  const [arrayName, indexName] = raw.metadata.names;
  const addend = ({minvalue:-1,negativeadd:-9,zeroadd:0,minadd:-2147483648,maxadd:2147483647})[variant] ?? 1;
  assert.equal(raw.metadata.addend, addend);
  const start = ({equal:1,start1:1,startlength:3,startabove:4,negative:-1,minindex:-2147483648,maxindex:2147483647,'sixteen-start9':9})[variant] ?? 0;
  const initial = variant === 'renamed' ? [4,7] : variant === 'equal' ? [1,1,1] : variant === 'repeat' ? [8,8,8]
    : variant === 'minvalue' ? [-2147483648,0,1] : variant === 'maxvalue' ? [2147483647,0,1]
    : variant === 'empty' ? [] : variant === 'one' ? [5] : ['seven','eight','sixteen','sixteen-start9'].includes(variant)
    ? Array.from({length:variant === 'seven' ? 7 : variant === 'eight' ? 8 : 16},(_,n)=>n) : [5,2,7];
  const expectedText = [`int[]${arrayName}={${initial.join(',')}}`, `int${indexName}=${start}`, `${indexName}<${arrayName}.length`,
    `${arrayName}[${indexName}]`, `${arrayName}[${indexName}]=${arrayName}[${indexName}]+${addend}`, prefix ? `++${indexName}` : `${indexName}++`];
  raw.metadata.sites.forEach((site, n) => {
    assert.equal(clean(slice(raw.originalSource,site.original)), expectedText[n]);
    assert.match(slice(raw.instrumentedSource,site.generated), /^__CodevizRecorder\d*\.(variableDeclare|declare|condition|read|write|variableWrite)\(/);
  });
  const readStart = raw.metadata.sites[3].original.start, writeStart = raw.metadata.sites[4].original.start;
  assert.ok(readStart.line > writeStart.line || (readStart.line === writeStart.line && readStart.column > writeStart.column), 'Read highlights RHS, not LHS');
  const count = raw.events.length;
  const limited = variant === 'eight' || variant === 'sixteen' || (/^limit/.test(variant) && variant !== 'limit15');
  const failed = variant === 'negative' || variant === 'minindex';
  const expectedCount = limited ? /^limit/.test(variant) ? Number(variant.slice(5)) : 32 : failed ? 3 : 3 + 4*Math.max(0,initial.length-start);
  assert.equal(count, expectedCount); assert.equal(raw.complete, !limited && !failed);
  assert.equal(raw.outcome, limited ? 'TRACE_LIMIT' : failed ? 'RUNTIME_FAILURE' : 'SUCCESS');
  for (let step = 0; step <= count; step++) {
    const bindings = {}, arrays = {};
    if (step >= 1) {
      bindings['variable-1'] = {name:arrayName,arrayId:'array-1'};
      const values = [...initial], stores = Math.max(0, Math.floor((step-1)/4));
      for (let n = 0; n < stores; n++) values[start+n] = intSum(initial[start+n],addend);
      arrays['array-1'] = values;
    }
    const finalFalse = raw.complete && step === count;
    const index = start + Math.max(0,Math.floor((step-2)/4));
    if (step >= 2 && !finalFalse) bindings['variable-2'] = {name:indexName,value:index};
    const siteIndex = step === 0 ? null : step <= 2 ? step-1 : 2 + ((step-3)%4);
    const highlight = step === 0 ? null : raw.metadata.sites[siteIndex].original;
    const expected = {bindings, arrays, highlight, condition:siteIndex === 2 ? !finalFalse : null,
      ...(siteIndex === 3 ? {read:{arrayId:'array-1',index,value:initial[index]}} : {})};
    assert.deepEqual(replayLoop(raw.events, step), expected, `Forward state ${step}`);
    replayLoop(raw.events, count);
    assert.deepEqual(replayLoop(raw.events, step), expected, `Backward state ${step}`);
    assert.deepEqual(replayLoop(raw.events.slice(0,step), step), expected, `Standalone prefix ${step}`);
    if (step > 0) {
      const event = raw.events[step-1];
      assert.equal(event.sequence, step); assert.equal(event.kind, raw.metadata.sites[siteIndex].kind);
      assert.deepEqual(event.source, highlight); assert.deepEqual(event.exitedVariableIds, finalFalse ? ['variable-2'] : []);
    }
  }
  const unchanged = structuredClone(raw.events), state = replayLoop(raw.events,count);
  if (state.arrays['array-1']) state.arrays['array-1'][0] = 123;
  if (state.highlight) state.highlight.start.line = 999;
  if (state.read) state.read.value = 999;
  assert.deepEqual(raw.events, unchanged, 'Replay must not mutate trace');
  assert.throws(() => replayLoop(raw.events,-1), RangeError); assert.throws(() => replayLoop(raw.events,count+1), RangeError);
  if (raw.complete) {
    const final = [...initial]; for (let n = start; n < final.length; n++) final[n] = intSum(final[n],addend);
    assert.equal(raw.stdout, variant === 'no-probe' ? '' : `FINAL=[${final.join(', ')}]`);
    assert.equal(raw.stderr, variant === 'no-probe' ? '' : 'PROBE');
  }
  if (variant === 'formatted') assert.ok(raw.originalSource.includes('\r\n') && raw.originalSource.includes('\u{1f600}'));
}
for (const prefix of [false,true]) for (const variant of variants) {
  const name = `read-${prefix ? 'pre' : 'post'}-${variant}`;
  const raw = JSON.parse(readFileSync(resolve(root,'.results/loop-reads',name+'.raw.json'),'utf8'));
  verify(raw,variant,prefix);
  const [executionOutcome, category] = {SUCCESS:['completed',null],RUNTIME_FAILURE:['failed','runtime_error'],TRACE_LIMIT:['limited','trace_limit']}[raw.outcome];
  const count = raw.events.length;
  const result = {runId:raw.runId,sourceId:raw.sourceId,schemaVersion:'draft-3',executionOutcome,
    visualizationCoverage:raw.complete ? 'complete' : 'partial', diagnostics:category ? [{category,message:`Loop read experiment: ${raw.outcome}`,source:null},
      {category:'visualization_limitation',message:raw.problem || 'Only a safe prefix was recorded.',source:null}] : [],
    events:raw.events,safePlaybackBoundary:{lastSafeStep:count,lastSafeEventSequence:count},console:{stdout:raw.stdout,stderr:raw.stderr}};
  const path = resolve(root,'.results/loop-reads',name+'.result.json'); writeFileSync(path,JSON.stringify(result,null,2)+'\n');
  const checked = spawnSync(process.execPath,[resolve(root,'../../contracts/validate.mjs'),path],{encoding:'utf8',timeout:5000,maxBuffer:65536});
  assert.equal(checked.status,0,checked.error?.message || checked.stdout+checked.stderr);
  assert.throws(() => verify({...raw,originalSource:raw.originalSource+' '},variant,prefix), /Stale source/);
  console.log(`PASS ${name}: draft-3, hashes, RHS ranges, read/write states and reverse restoration`);
}
for (const variant of variants) {
  const read = form => JSON.parse(readFileSync(resolve(root,'.results/loop-reads',`read-${form}-${variant}.raw.json`),'utf8'));
  const facts = raw => raw.events.map(({source,...fact})=>fact);
  assert.deepEqual(facts(read('post')),facts(read('pre')));
}
console.log('PASS 80 loop-read results and 40 prefix/postfix pairs; all replay prefixes use captured facts');
