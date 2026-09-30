import assert from 'node:assert/strict';
import {readFileSync, writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {dirname, resolve} from 'node:path';
import {replayLoop} from '../prototype/recording/loop-replay.mjs';

const root = dirname(fileURLToPath(import.meta.url));
const variants = ['success','renamed','equal','repeat','empty','one','start1','startlength','startabove','negative','minindex','maxindex',
  'minvalue','maxvalue','formatted','collision','no-probe','nine','ten','sixteen','sixteen-start7', ...Array.from({length:13}, (_, n) => `limit${n+1}`)];
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
function verify(raw, variant, prefix) {
  assert.equal(hash(raw.originalSource), raw.sourceId, 'Stale source');
  assert.equal(hash(raw.instrumentedSource), raw.instrumentedSourceId, 'Stale generated source');
  assert.equal(raw.metadata.sourceId, raw.sourceId); assert.equal(raw.metadata.generatedId, raw.instrumentedSourceId);
  assert.equal(raw.metadata.sites.length, 6);
  const [xName, arrayName, indexName] = raw.metadata.names;
  // Independent fixture expectations; replay reads captured facts only.
  const x = ({renamed:-9, equal:1, minvalue:-2147483648, maxvalue:2147483647})[variant] ?? 8;
  const start = ({equal:1,start1:1,startlength:3,startabove:4,negative:-1,minindex:-2147483648,maxindex:2147483647,'sixteen-start7':7})[variant] ?? 0;
  const initial = variant === 'renamed' ? [4,7] : variant === 'equal' ? [1,1,1] : variant === 'repeat' ? [8,8,8]
    : variant === 'empty' ? [] : variant === 'one' ? [5] : ['nine','ten','sixteen','sixteen-start7'].includes(variant)
    ? Array.from({length:variant === 'nine' ? 9 : variant === 'ten' ? 10 : 16},(_,n)=>n) : [5,2,7];
  const expectedText = [`int${xName}=${x}`, `int[]${arrayName}={${initial.join(',')}}`, `int${indexName}=${start}`,
    `${indexName}<${arrayName}.length`, `${arrayName}[${indexName}]=${xName}`, prefix ? `++${indexName}` : `${indexName}++`];
  raw.metadata.sites.forEach((site, n) => {
    assert.equal(clean(slice(raw.originalSource,site.original)), expectedText[n]);
    assert.match(slice(raw.instrumentedSource,site.generated), /^__CodevizRecorder\d*\.(variableDeclare|declareCombined|condition|write|variableWrite)\(/);
  });
  const count = raw.events.length;
  const limited = variant === 'ten' || variant === 'sixteen' || (/^limit/.test(variant) && variant !== 'limit13');
  const failed = variant === 'negative' || variant === 'minindex';
  const naturalCount = failed ? 4 : 4 + 3 * Math.max(0, initial.length - start);
  const expectedCount = limited ? /^limit/.test(variant) ? Number(variant.slice(5)) : 32 : naturalCount;
  assert.equal(count, expectedCount); assert.equal(raw.complete, !limited && !failed);
  assert.equal(raw.outcome, limited ? 'TRACE_LIMIT' : failed ? 'RUNTIME_FAILURE' : 'SUCCESS');
  for (let step = 0; step <= count; step++) {
    const bindings = {}, arrays = {};
    if (step >= 1) bindings['variable-1'] = {name:xName,value:x};
    if (step >= 2) {
      bindings['variable-2'] = {name:arrayName,arrayId:'array-1'};
      const values = [...initial];
      const stores = Math.max(0, Math.floor((step - 2)/3));
      for (let n = 0; n < stores; n++) values[start+n] = x;
      arrays['array-1'] = values;
    }
    const finalFalse = raw.complete && step === count;
    if (step >= 3 && !finalFalse) bindings['variable-3'] = {name:indexName,value:start + Math.max(0,Math.floor((step-3)/3))};
    const siteIndex = step === 0 ? null : step <= 3 ? step-1 : 3 + ((step-4)%3);
    const highlight = step === 0 ? null : raw.metadata.sites[siteIndex].original;
    const condition = siteIndex === 3 ? !finalFalse : null;
    const expected = {bindings, arrays, highlight, condition};
    assert.deepEqual(replayLoop(raw.events, step), expected, `Forward state ${step}`);
    // Select the same cursor after visiting later cursors; no stale false result or retired binding.
    replayLoop(raw.events, count);
    assert.deepEqual(replayLoop(raw.events, step), expected, `Backward state ${step}`);
    if (step > 0) {
      const event = raw.events[step-1];
      assert.equal(event.sequence, step);
      assert.equal(event.kind, raw.metadata.sites[siteIndex].kind);
      assert.deepEqual(event.source, highlight);
      assert.deepEqual(event.exitedVariableIds, finalFalse ? ['variable-3'] : []);
    }
  }
  const unchanged = structuredClone(raw.events);
  const mutableState = replayLoop(raw.events, count); if (mutableState.arrays['array-1']) mutableState.arrays['array-1'][0] = 123;
  if (mutableState.highlight) mutableState.highlight.start.line = 999;
  assert.deepEqual(raw.events, unchanged, 'Replay must not mutate trace');
  assert.throws(() => replayLoop(raw.events,-1), RangeError);
  assert.throws(() => replayLoop(raw.events,count+1), RangeError);
  if (raw.complete) {
    const final = [...initial]; for (let n = start; n < final.length; n++) final[n] = x;
    assert.equal(raw.stdout, variant === 'no-probe' ? '' : `FINAL=${x},[${final.join(', ')}]`);
    assert.equal(raw.stderr, variant === 'no-probe' ? '' : 'PROBE');
  }
  if (variant === 'formatted') assert.ok(raw.originalSource.includes('\r\n') && raw.originalSource.includes('\u{1f600}'));
}
for (const prefix of [false,true]) for (const variant of variants) {
  const name = `loop-${prefix ? 'pre' : 'post'}-${variant}`;
  const raw = JSON.parse(readFileSync(resolve(root,'.results/loops',name+'.raw.json'),'utf8'));
  verify(raw,variant,prefix);
  const [executionOutcome, category] = {SUCCESS:['completed',null],RUNTIME_FAILURE:['failed','runtime_error'],TRACE_LIMIT:['limited','trace_limit']}[raw.outcome];
  const count = raw.events.length;
  const result = {runId:raw.runId,sourceId:raw.sourceId,schemaVersion:'draft-3',executionOutcome,
    visualizationCoverage:raw.complete ? 'complete' : 'partial', diagnostics:category ? [{category,message:`Loop experiment: ${raw.outcome}`,source:null},
      {category:'visualization_limitation',message:raw.problem || 'Only a safe prefix was recorded.',source:null}] : [],
    events:raw.events,safePlaybackBoundary:{lastSafeStep:count,lastSafeEventSequence:count},console:{stdout:raw.stdout,stderr:raw.stderr}};
  const path = resolve(root,'.results/loops',name+'.result.json'); writeFileSync(path,JSON.stringify(result,null,2)+'\n');
  const checked = spawnSync(process.execPath,[resolve(root,'../../contracts/validate.mjs'),path],{encoding:'utf8',timeout:5000,maxBuffer:65536});
  assert.equal(checked.status,0,checked.error?.message || checked.stdout+checked.stderr);
  assert.throws(() => verify({...raw,originalSource:raw.originalSource+' '},variant,prefix), /Stale source/);
  console.log(`PASS ${name}: draft-3, hashes, six sites, per-step values, retirement and reverse restoration`);
}
for (const variant of variants) {
  const read = form => JSON.parse(readFileSync(resolve(root,'.results/loops',`loop-${form}-${variant}.raw.json`),'utf8'));
  const facts = raw => raw.events.map(({source,...fact})=>fact);
  assert.deepEqual(facts(read('post')),facts(read('pre')));
}
console.log('PASS 68 loop results and 34 prefix/postfix pairs; replay uses recorded prefixes only');
