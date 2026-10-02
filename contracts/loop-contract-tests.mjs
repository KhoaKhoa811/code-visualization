import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { createHash } from 'node:crypto';

/** Contract fixtures only: no Java execution or production playback implementation. */
export function loopContractTests({ fixtures, validate, checkSchema, structural, root }) {
  const success = fixtures['loop-success.json'];
  const failed = report => report.structural.length + report.semantic.length > 0;
  const expectValid = (value, label) => {
    const before = structuredClone(value);
    assert(!failed(validate(value)), label);
    assert.deepEqual(value, before, 'Validation must not mutate supplied events or retire caller state');
  };
  const readLoop = fixtures['loop-read-success.json'];
  expectValid(readLoop, 'two-binding read loop');
  expectValid(fixtures['loop-read-limit.json'], 'limit between read and write');
  assert.equal(readLoop.events.length, 15);
  const readSource = readFileSync(join(root, 'examples/sources/loop-read-success.java'));
  assert.equal(createHash('sha256').update(readSource).digest('hex'), readLoop.sourceId);
  for (const change of [r => { r.events[3].value.value = 6; }, r => { r.events[3].index = 1; },
    r => { r.events[3].arrayId = 'missing'; }, r => { r.events[3].exitedVariableIds = ['variable-2']; },
    r => { delete r.events[3].exitedVariableIds; }, r => { r.events[3].value.type = 'boolean'; }]) {
    const bad = structuredClone(readLoop); change(bad); const before = structuredClone(bad);
    assert(failed(validate(bad)), 'Invalid scoped read accepted'); assert.deepEqual(bad, before);
  }
  console.log('PASS draft-3 array-read fixtures: success, limit-before-write, source hash and six invalid reads');
  const append = (r, event) => {
    r.events.push({ ...structuredClone(event), sequence: r.events.length + 1 });
    r.safePlaybackBoundary = {lastSafeStep:r.events.length,lastSafeEventSequence:r.events.length};
  };
  const tests = [
    ['condition numeric true', 'structural', r => { r.events[3].value.value = 1; }],
    ['condition numeric false', 'structural', r => { r.events[12].value.value = 0; }],
    ['condition string true', 'structural', r => { r.events[3].value.value = 'true'; }],
    ['condition string false', 'structural', r => { r.events[12].value.value = 'false'; }],
    ['condition null', 'structural', r => { r.events[3].value.value = null; }],
    ['condition boxed object', 'structural', r => { r.events[3].value.value = {}; }],
    ['condition wrong type tag', 'structural', r => { r.events[3].value.type = 'int'; }],
    ['boolean scalar unsupported', 'structural', r => { r.events[0].value = {type:'boolean',value:true}; }],
    ['condition invented variable', 'structural', r => { r.events[3].variableId = 'variable-3'; }],
    ['condition invented object', 'structural', r => { r.events[3].arrayId = 'array-1'; }],
    ['missing condition scope', 'structural', r => { delete r.events[3].scopeId; }],
    ['missing scalar scope', 'structural', r => { delete r.events[2].scopeId; }],
    ['missing array scope', 'structural', r => { delete r.events[1].scopeId; }],
    ['empty scope', 'structural', r => { r.events[2].scopeId = ''; }],
    ['missing exit list', 'structural', r => { delete r.events[12].exitedVariableIds; }],
    ['null exit list', 'structural', r => { r.events[12].exitedVariableIds = null; }],
    ['invalid exit identity type', 'structural', r => { r.events[12].exitedVariableIds = [3]; }],
    ['empty exit identity', 'structural', r => { r.events[12].exitedVariableIds = ['']; }],
    ['unknown event field', 'structural', r => { r.events[3].color = 'green'; }],
    ['draft-3 relabeled draft-2', 'structural', r => { r.schemaVersion = 'draft-2'; }],
    ['draft-3 relabeled draft-1', 'structural', r => { r.schemaVersion = 'draft-1'; }],
    ['unknown version', 'structural', r => { r.schemaVersion = 'draft-4'; }],
    ['missing false-condition exit', 'semantic', r => { r.events[12].exitedVariableIds = []; }],
    ['true-condition exit', 'semantic', r => { r.events[3].exitedVariableIds = ['variable-3']; }],
    ['write with exit', 'semantic', r => { r.events[5].exitedVariableIds = ['variable-3']; }],
    ['unknown exit identity', 'semantic', r => { r.events[12].exitedVariableIds = ['unknown']; }],
    ['wrong-scope scalar exit', 'semantic', r => { r.events[12].exitedVariableIds = ['variable-1']; }],
    ['wrong-kind array exit', 'semantic', r => { r.events[12].exitedVariableIds = ['variable-2']; }],
    ['duplicate exit', 'semantic', r => { r.events[12].exitedVariableIds = ['variable-3','variable-3']; }],
    ['extra exit', 'semantic', r => { r.events[12].exitedVariableIds = ['variable-3','variable-1']; }],
    ['unknown condition scope', 'semantic', r => { r.events[3].scopeId = 'unknown'; }],
    ['condition main scope', 'semantic', r => { r.events[3].scopeId = 'scope-main'; }],
    ['index in wrong scope', 'semantic', r => { r.events[2].scopeId = 'scope-main'; }],
    ['retired scalar write', 'semantic', r => { append(r,r.events[5]); }],
    ['retired condition', 'semantic', r => { append(r,r.events[12]); }],
    ['retired ID reused elsewhere', 'semantic', r => { append(r,{...r.events[2],scopeId:'different'}); }],
    ['closed scope reopened', 'semantic', r => { append(r,{...r.events[2],variableId:'different'}); }],
    ['duplicate declaration', 'semantic', r => { r.events[2].variableId = 'variable-1'; }],
    ['sequence gap', 'semantic', r => { r.events[6].sequence = 8; }],
    ['backward source range', 'semantic', r => { r.events[3].source.end.column = 1; }],
    ['boundary splits retirement', 'semantic', r => { r.safePlaybackBoundary.lastSafeEventSequence = 12; }],
    ['step boundary mismatch', 'semantic', r => { r.safePlaybackBoundary.lastSafeStep = 12; }],
    ['complete open loop', 'semantic', r => { r.events.pop(); r.safePlaybackBoundary = {lastSafeStep:12,lastSafeEventSequence:12}; }],
    ['missing failure diagnostic', 'semantic', r => { r.executionOutcome = 'failed'; }]
  ];
  for (const [label, layer, mutate] of tests) {
    const value = structuredClone(success); mutate(value);
    const before = structuredClone(value), report = validate(value);
    assert(report[layer].length > 0, label + ': rejected at ' + layer);
    if (layer === 'semantic') assert.equal(report.structural.length, 0, label + ': wrong layer');
    assert.deepEqual(value, before, label + ': no input mutation on rejection');
  }
  // Every safe prefix can be delivered as partial, including before any condition exists.
  for (let n = 1; n <= 12; n++) {
    const value = structuredClone(fixtures['loop-limit.json']);
    value.events = value.events.slice(0,n);
    value.safePlaybackBoundary = {lastSafeStep:n,lastSafeEventSequence:n};
    expectValid(value, 'safe prefix ' + n);
  }
  const renamed = structuredClone(success);
  for (const e of renamed.events) if (e.scopeId) e.scopeId = 'renamed-' + e.scopeId;
  expectValid(renamed, 'scope identity is not a hard-coded display name');
  for (const name of ['loop-success.json','loop-prefix.json','loop-zero.json','loop-limit.json','loop-runtime-error.json','loop-output-only.json']) {
    expectValid(fixtures[name], name);
  }
  // Existing event shapes remain usable with the new required scope/exit fields.
  for (const version of ['draft-1','draft-2']) {
    const old = structuredClone(fixtures['array-success.json']); old.schemaVersion = version;
    expectValid(old, version + ' unchanged array contract');
    old.schemaVersion = 'draft-3';
    assert(failed(validate(old)), 'old trace cannot simply be relabeled');
    for (const e of old.events) { e.exitedVariableIds = []; if (e.kind === 'ARRAY_DECLARE') e.scopeId = 'scope-main'; }
    expectValid(old, 'draft-3 retained array read/write payloads');
  }
  validate(success); // Select draft-3 for direct trusted-schema checks.
  checkSchema({type:'boolean'});
  for (const value of [true,false]) assert.equal(structural({type:'boolean'},value).length,0);
  for (const value of [0,1,'true','false',null,{},[]]) assert(structural({type:'boolean'},value).length > 0);
  assert.throws(() => checkSchema({type:'number'}), /Unsupported schema type/);
  assert.throws(() => checkSchema({mysteryKeyword:true}), /Unsupported schema keyword/);
  assert.throws(() => JSON.parse(JSON.stringify(success).slice(0,-10)), SyntaxError);

  // Verify the design fixture's hashes, exact highlights and every cursor state.
  const sourceNames = {'loop-limit':'loop-success','loop-output-only':'loop-success'};
  for (const name of ['loop-success','loop-prefix','loop-zero','loop-limit','loop-runtime-error','loop-output-only']) {
    const result = fixtures[name + '.json'];
    const source = readFileSync(join(root,'examples/sources', (sourceNames[name] ?? name) + '.java'),'utf8');
    assert.equal(result.sourceId,createHash('sha256').update(source).digest('hex'));
    if (!result.events) continue;
    for (const event of result.events) {
      const {start,end} = event.source;
      assert.equal(start.line,end.line);
      const text = source.split('\n')[start.line-1].slice(start.column-1,end.column-1);
      const expected = event.kind === 'CONDITION' ? 'i < values.length'
        : event.kind === 'VARIABLE_WRITE' ? name === 'loop-prefix' ? '++i' : 'i++'
        : event.kind === 'ARRAY_WRITE' ? 'values[i] = x'
        : event.kind === 'ARRAY_DECLARE' ? name === 'loop-zero' ? 'int[] values = {}' : 'int[] values = {5, 2, 7}'
        : event.variableId === 'variable-1' ? 'int x = 8' : name === 'loop-runtime-error' ? 'int i = -1' : 'int i = 0';
      assert.equal(text,expected);
    }
    const at = count => {
      const bindings = {}, arrays = {};
      for (const e of result.events.slice(0,count)) {
        if (e.kind === 'VARIABLE_DECLARE' || e.kind === 'VARIABLE_WRITE') bindings[e.variableId] = e.value.value;
        if (e.kind === 'ARRAY_DECLARE') { bindings[e.variableId] = e.arrayId; arrays[e.arrayId] = e.values.map(v=>v.value); }
        if (e.kind === 'ARRAY_WRITE') arrays[e.arrayId][e.index] = e.value.value;
        for (const id of e.exitedVariableIds) delete bindings[id];
      }
      const last = result.events[count-1];
      return {bindings,arrays,condition:last?.kind === 'CONDITION' ? last.value.value : null,highlight:last?.source ?? null};
    };
    const expectedIndices = [undefined,undefined,undefined,0,0,0,1,1,1,2,2,2,3,undefined];
    const expectedStates = [];
    for (let count = 0; count <= result.events.length; count++) {
      const state = at(count);
      const zero = name === 'loop-zero', error = name === 'loop-runtime-error';
      const index = zero ? count === 3 ? 0 : undefined : error ? count >= 3 ? -1 : undefined : expectedIndices[count];
      const bindings = count ? {'variable-1':8} : {};
      if (count >= 2) bindings['variable-2'] = 'array-1';
      if (index !== undefined) bindings['variable-3'] = index;
      const array = zero ? [] : error || count < 5 ? [5,2,7] : count < 8 ? [8,2,7] : count < 11 ? [8,8,7] : [8,8,8];
      const condition = zero && count === 4 ? false : error && count === 4 ? true : !zero && !error && [4,7,10].includes(count) ? true : count === 13 ? false : null;
      const expected = {bindings,arrays:count>=2?{'array-1':array}:{},condition,highlight:count?result.events[count-1].source:null};
      expectedStates.push(expected);
      assert.deepEqual(state,expected, name + ' cursor ' + count);
    }
    for (let count = result.events.length; count >= 0; count--) assert.deepEqual(at(count),expectedStates[count],name + ' backward cursor ' + count);
  }
  const facts = name => fixtures[name].events.map(({source,...event})=>event);
  assert.deepEqual(facts('loop-success.json'),facts('loop-prefix.json'));
  console.log(`PASS: draft-3 six fixtures, ${tests.length} negative cases, 12 partial prefixes, boolean/type guards, scope retirement, source hashes/ranges and fixture cursor states`);
}
