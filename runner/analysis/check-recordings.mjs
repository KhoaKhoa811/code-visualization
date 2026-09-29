import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const root = dirname(fileURLToPath(import.meta.url));
const incrementVariants = ['success', 'renamed', 'equal', 'repeat', 'negative', 'oob', 'empty', 'overflow-max', 'overflow-min', 'invalid-initial', 'first', 'sixteen', 'formatted', 'collision', 'no-probe', 'limit1', 'limit2', 'limit3', 'limit4', 'limit5'];
// Independent expected initial and committed values, including Java int wraparound.
const incrementValues = variant => ({renamed:[-1,0],equal:[1,2],negative:[-2,-1],oob:[1,2],
  'overflow-max':[2147483647,-2147483648],'overflow-min':[-2147483648,-2147483647],
  'invalid-initial':[-1,0],first:[-1,0],sixteen:[14,15]}[variant] ?? [0,1]);
const additionVariants = ['success', 'renamed', 'equal', 'zero', 'step', 'negative', 'oob', 'empty', 'overflow-max', 'overflow-min', 'wrap-zero', 'invalid-initial', 'first', 'sixteen', 'formatted', 'collision', 'no-probe', 'limit1', 'limit2', 'limit3', 'limit4', 'limit5'];
// Reviewed expected values, not expression evaluation during replay: [initial, literal, committed].
const additionValues = variant => ({renamed:[1,-1,0],zero:[1,0,1],step:[-1,2,1],negative:[0,-1,-1],oob:[0,2,2],
  'overflow-max':[2147483647,1,-2147483648],'overflow-min':[-2147483648,-1,2147483647],
  'wrap-zero':[-2147483648,-2147483648,0],'invalid-initial':[-1,1,0],first:[1,-1,0],sixteen:[0,15,15]}[variant] ?? [0,1,1]);
const cases = ['success', 'renamed', 'formatted', 'collision', 'failed-read', 'failed-write', 'empty', 'int-bounds', 'trace-limit',
  'int-success', 'int-renamed', 'int-formatted', 'int-collision', 'int-bounds-scalar', 'int-repeat', 'int-no-probe', 'int-limit',
  'mix-success', 'mix-renamed', 'mix-bounds', 'mix-repeat', 'mix-formatted', 'mix-collision', 'mix-no-probe', 'mix-one', 'mix-sixteen', 'mix-negative', 'mix-oob', 'mix-empty', 'mix-limit1', 'mix-limit2',
  'update-success', 'update-renamed', 'update-bounds', 'update-repeat', 'update-formatted', 'update-collision', 'update-no-probe', 'update-one', 'update-sixteen', 'update-negative', 'update-oob', 'update-empty', 'update-limit1', 'update-limit2', 'update-limit3', 'update-limit4',
  'index-success', 'index-renamed', 'index-equal', 'index-minvalue', 'index-maxvalue', 'index-formatted', 'index-collision', 'index-no-probe', 'index-first', 'index-sixteen', 'index-negative', 'index-oob', 'index-minindex', 'index-maxindex', 'index-empty', 'index-limit1', 'index-limit2', 'index-limit3', 'index-limit4',
  ...['success', 'renamed', 'equal', 'minvalue', 'maxvalue', 'formatted', 'collision', 'no-probe', 'first', 'sixteen', 'negative', 'oob', 'minindex', 'maxindex', 'empty', 'repeat', 'invalid-initial', 'limit1', 'limit2', 'limit3', 'limit4', 'limit5'].map(n => 'index-update-' + n),
  ...additionVariants.map(n => 'index-add-' + n),
  ...['index-post-', 'index-pre-'].flatMap(group => incrementVariants.map(n => group + n))];
const hash = source => createHash('sha256').update(source, 'utf8').digest('hex');
function slice(source, range) {
  assert.equal(range.file, 'Main.java');
  const lines = source.split(/\r\n|\r|\n/);
  const { start, end } = range;
  assert.ok(start.line >= 1 && end.line <= lines.length && end.line >= start.line);
  assert.ok(start.column >= 1 && end.column >= 1);
  assert.ok(start.column <= lines[start.line - 1].length + 1 && end.column <= lines[end.line - 1].length + 1);
  if (start.line === end.line) return lines[start.line - 1].slice(start.column - 1, end.column - 1);
  return [lines[start.line - 1].slice(start.column - 1), ...lines.slice(start.line, end.line - 1), lines[end.line - 1].slice(0, end.column - 1)].join('\n');
}
function verify(raw) {
  assert.equal(hash(raw.originalSource), raw.sourceId, 'Stale original source');
  assert.equal(hash(raw.instrumentedSource), raw.instrumentedSourceId, 'Stale generated source');
  assert.equal(raw.metadata.sourceId, raw.sourceId);
  assert.equal(raw.metadata.generatedId, raw.instrumentedSourceId);
  for (const event of raw.events) {
    const matching = raw.metadata.sites.filter(s => s.kind === event.kind && JSON.stringify(s.original) === JSON.stringify(event.source));
    assert.equal(matching.length, 1, 'Missing or duplicate operation source association');
    const site = matching[0];
    assert.deepEqual(event.source, site.original);
    const text = slice(raw.originalSource, event.source).replace(/\/\*[\s\S]*?\*\//g, '').replace(/\s+/g, '');
    const variable = event.kind.startsWith('ARRAY_') ? raw.metadata.arrayName ?? raw.metadata.variableName
      : event.variableId === 'variable-3' ? raw.metadata.indexName : raw.metadata.variableName;
    if (event.kind === 'ARRAY_DECLARE') {
      assert.ok(text.startsWith(`int[]${variable}={`));
      assert.equal(event.variableName, variable);
    } else if (event.kind === 'VARIABLE_DECLARE') {
      assert.equal(text, `int${variable}=${event.value.value}`);
      assert.equal(event.variableName, variable);
    } else if (event.kind === 'VARIABLE_WRITE') {
      if (raw.metadata.name.startsWith('index-add-')) {
        const [, literal, committed] = additionValues(raw.metadata.name.slice('index-add-'.length));
        assert.equal(text, `${variable}=${variable}+${literal}`);
        assert.equal(event.value.value, committed);
      } else if (/^index-(post|pre)-/.test(raw.metadata.name)) {
        const prefix = raw.metadata.name.startsWith('index-pre-');
        assert.equal(text, prefix ? `++${variable}` : `${variable}++`);
        assert.equal(event.value.value, incrementValues(raw.metadata.name.slice(prefix ? 10 : 11))[1], 'Capture committed value, never discarded postfix result');
      } else assert.equal(text, `${variable}=${event.value.value}`);
    }
    else if (event.kind === 'ARRAY_READ') assert.equal(text, `${variable}[${event.index}]`);
    else if (raw.metadata.indexName) assert.equal(text, `${variable}[${raw.metadata.indexName}]=${raw.metadata.variableName}`);
    else assert.ok(text.startsWith(`${variable}[${event.index}]=`));
    assert.match(slice(raw.instrumentedSource, site.generated), /^__CodevizRecorder\d*\.(declare|declareCombined|read|write|variableDeclare|variableWrite)\(/);
  }
}
function stateAt(events, count) {
  let values;
  for (const event of events.slice(0, count)) {
    if (event.kind === 'ARRAY_DECLARE') values = event.values.map(v => v.value);
    else if (event.kind === 'VARIABLE_DECLARE' || event.kind === 'VARIABLE_WRITE') values = event.value.value;
    else if (event.kind === 'ARRAY_READ') assert.equal(values[event.index], event.value.value);
    else values[event.index] = event.value.value;
  }
  return { values, highlight: count === 0 ? null : events[count - 1].source };
}
function combinedStateAt(events, count) {
  const bindings = {}, arrays = {};
  for (const event of events.slice(0, count)) {
    if (event.kind === 'VARIABLE_DECLARE') bindings[event.variableId] = { name: event.variableName, value: event.value.value };
    else if (event.kind === 'VARIABLE_WRITE') bindings[event.variableId].value = event.value.value;
    else if (event.kind === 'ARRAY_DECLARE') {
      bindings[event.variableId] = { name: event.variableName, arrayId: event.arrayId };
      arrays[event.arrayId] = event.values.map(v => v.value);
    } else arrays[event.arrayId][event.index] = event.value.value;
  }
  return { bindings, arrays, highlight: count === 0 ? null : events[count - 1].source };
}
for (const name of cases) {
  const raw = JSON.parse(readFileSync(resolve(root, '.results/automatic', name + '.raw.json'), 'utf8'));
  const scalar = raw.events[0].kind === 'VARIABLE_DECLARE';
  verify(raw);
  const [executionOutcome, category] = {
    SUCCESS: ['completed', null], RUNTIME_FAILURE: ['failed', 'runtime_error'], TRACE_LIMIT: ['limited', 'trace_limit']
  }[raw.outcome];
  const diagnostics = category ? [{ category, message: `Automatic experiment: ${raw.outcome}`, source: null },
    { category: 'visualization_limitation', message: raw.problem || 'Only a safe prefix was recorded.', source: null }] : [];
  const count = raw.events.length;
  const result = { runId: raw.runId, sourceId: raw.sourceId, schemaVersion: scalar ? 'draft-2' : 'draft-1', executionOutcome,
    visualizationCoverage: raw.complete ? 'complete' : 'partial', diagnostics, events: raw.events,
    safePlaybackBoundary: { lastSafeStep: count, lastSafeEventSequence: count }, console: { stdout: raw.stdout, stderr: raw.stderr } };
  const path = resolve(root, '.results/automatic', name + '.result.json');
  writeFileSync(path, JSON.stringify(result, null, 2) + '\n');
  const checked = spawnSync(process.execPath, [resolve(root, '../../contracts/validate.mjs'), path], { encoding: 'utf8', timeout: 5000, maxBuffer: 65536 });
  assert.equal(checked.status, 0, checked.error?.message || checked.stdout + checked.stderr);
  if (name.startsWith('index-')) {
    const addition = name.startsWith('index-add-');
    const increment = /^index-(post|pre)-/.test(name);
    const update = name.startsWith('index-update-') || addition || increment;
    const variant = name.slice(increment ? name.startsWith('index-post-') ? 11 : 10 : addition ? 'index-add-'.length : update ? 'index-update-'.length : 'index-'.length);
    const x = variant === 'renamed' ? -9 : variant === 'equal' ? 1 : variant === 'minvalue' ? -2147483648 : variant === 'maxvalue' ? 2147483647 : 8;
    const index = increment ? incrementValues(variant)[1] : addition ? additionValues(variant)[2] : variant === 'renamed' || variant === 'first' ? 0 : variant === 'sixteen' ? 15 : variant === 'negative' ? -1
      : variant === 'oob' ? 2 : variant === 'minindex' ? -2147483648 : variant === 'maxindex' ? 2147483647 : 1;
    const oldIndex = increment ? incrementValues(variant)[0] : addition ? additionValues(variant)[0] : !update ? index : ['renamed', 'first', 'repeat'].includes(variant) ? 1 : variant === 'invalid-initial' ? -2147483648 : 0;
    const initial = increment && variant === 'equal' ? [1,1,1] : increment && variant === 'repeat' ? [8,8] : variant === 'renamed' ? [4, 7] : variant === 'first' ? [5] : variant === 'empty' ? []
      : variant === 'sixteen' ? Array.from({length:16}, (_, i) => i) : [5, 2];
    const total = update ? 5 : 4;
    const changed = [...initial]; if (count === total) changed[index] = x;
    assert.equal(raw.metadata.sites.length, total);
    const kinds = ['VARIABLE_DECLARE', 'ARRAY_DECLARE', 'VARIABLE_DECLARE', ...(update ? ['VARIABLE_WRITE'] : []), 'ARRAY_WRITE'];
    assert.deepEqual(raw.events.map(e => e.kind), kinds.slice(0, count));
    assert.deepEqual(raw.events.map(e => e.kind.startsWith('VARIABLE_') ? e.variableId : e.arrayId),
      ['variable-1', 'array-1', 'variable-3', ...(update ? ['variable-3'] : []), 'array-1'].slice(0, count));
    const states = [];
    for (let step = 0; step <= count; step++) {
      const bindings = step === 0 ? {} : {'variable-1':{name:raw.metadata.variableName,value:x}};
      if (step >= 2) bindings['variable-2'] = {name:raw.metadata.arrayName,arrayId:'array-1'};
      if (step >= 3) bindings['variable-3'] = {name:raw.metadata.indexName,value:update && step === 3 ? oldIndex : index};
      const state = combinedStateAt(raw.events, step);
      assert.deepEqual(state, {bindings,arrays:step < 2 ? {} : {'array-1':step === total ? changed : initial},highlight:step === 0 ? null : raw.events[step-1].source});
      states.push(state);
    }
    for (let step = count; step >= 0; step--) assert.deepEqual(combinedStateAt(raw.events, step), states[step]);
    if (count === total) assert.equal(raw.events[total - 1].index, index);
    if (raw.complete && variant !== 'no-probe') assert.equal(raw.stdout, `FINAL=${x},${index},[${changed.join(', ')}]`);
    assert.throws(() => verify({...raw,originalSource:raw.originalSource + ' '}), /Stale original/);
    if (variant === 'formatted') assert.ok(raw.originalSource.includes('\r\n') && raw.originalSource.includes('\u{1f600}'));
    const swapped = structuredClone(raw); [swapped.metadata.sites[0].original,swapped.metadata.sites[2].original] = [swapped.metadata.sites[2].original,swapped.metadata.sites[0].original];
    // Source-to-generated-call association must retain the exact encoded original range.
    for (const site of raw.metadata.sites) assert.ok(slice(raw.instrumentedSource, site.generated).includes(JSON.stringify(JSON.stringify(site.original)).slice(1,-1)));
    assert.ok(swapped.metadata.sites.some(site => !slice(swapped.instrumentedSource, site.generated).includes(JSON.stringify(JSON.stringify(site.original)).slice(1,-1))));
    console.log(`PASS automatic ${name}: two scalar identities, repeated-kind sites, actual index, independent forward/backward states`);
    continue;
  }
  if (name.startsWith('update-')) {
    const x = name === 'update-renamed' ? -7 : name === 'update-bounds' ? -2147483648 : 3;
    const updated = name === 'update-renamed' ? -9 : name === 'update-bounds' ? 2147483647 : name === 'update-repeat' ? 3 : 8;
    const initial = name === 'update-renamed' ? [9, -4] : name === 'update-bounds' ? [-2147483648, 2147483647]
      : name === 'update-empty' ? [] : name === 'update-one' ? [5]
      : name === 'update-sixteen' ? Array.from({length:16}, (_, i) => i) : [5, 2];
    const changed = [...initial]; changed[name === 'update-renamed' ? 1 : 0] = updated;
    assert.equal(raw.metadata.sites.length, 4);
    assert.deepEqual(raw.events.map(e => e.kind), ['VARIABLE_DECLARE', 'ARRAY_DECLARE', 'VARIABLE_WRITE', 'ARRAY_WRITE'].slice(0, count));
    const states = [];
    for (let step = 0; step <= count; step++) {
      const bindings = step === 0 ? {} : {'variable-1':{name:raw.metadata.variableName,value:step >= 3 ? updated : x}};
      if (step >= 2) bindings['variable-2'] = {name:raw.metadata.arrayName,arrayId:'array-1'};
      const expected = {bindings,arrays:step < 2 ? {} : {'array-1':step === 4 ? changed : initial},highlight:step === 0 ? null : raw.events[step - 1].source};
      const state = combinedStateAt(raw.events, step);
      assert.deepEqual(state, expected); states.push(state);
    }
    for (let step = count; step >= 0; step--) assert.deepEqual(combinedStateAt(raw.events, step), states[step]);
    if (raw.complete && name !== 'update-no-probe') assert.equal(raw.stdout, `FINAL=${updated},[${changed.join(', ')}]`);
    assert.throws(() => verify({...raw, originalSource:raw.originalSource + ' '}), /Stale original/);
    if (name === 'update-formatted') assert.ok(raw.originalSource.includes('\r\n') && raw.originalSource.includes('\u{1f600}'));
    console.log(`PASS automatic ${name}: draft-2, four sites, updated scalar and independent forward/backward states`);
    continue;
  }
  if (name.startsWith('mix-')) {
    const x = name === 'mix-renamed' ? -7 : name === 'mix-bounds' ? -2147483648 : 3;
    const initial = name === 'mix-renamed' ? [9, -4] : name === 'mix-bounds' ? [2147483647, -2147483648]
      : name === 'mix-repeat' ? [3, 3] : name === 'mix-empty' ? [] : name === 'mix-one' ? [5]
      : name === 'mix-sixteen' ? Array.from({length:16}, (_, i) => i) : [5, 2];
    const changed = [...initial]; changed[name === 'mix-renamed' ? 1 : 0] = x;
    assert.deepEqual(raw.events.map(e => e.kind), ['VARIABLE_DECLARE', 'ARRAY_DECLARE', 'ARRAY_WRITE'].slice(0, count));
    const states = [];
    for (let step = 0; step <= count; step++) {
      const expectedBindings = step === 0 ? {} : {'variable-1': {name:raw.metadata.variableName,value:x}};
      if (step >= 2) expectedBindings['variable-2'] = {name:raw.metadata.arrayName,arrayId:'array-1'};
      const expectedState = {bindings:expectedBindings,arrays:step < 2 ? {} : {'array-1':step === 3 ? changed : initial},
        highlight:step === 0 ? null : raw.events[step - 1].source};
      const state = combinedStateAt(raw.events, step);
      assert.deepEqual(state, expectedState); states.push(state);
    }
    for (let step = count; step >= 0; step--) assert.deepEqual(combinedStateAt(raw.events, step), states[step]);
    if (raw.complete && name !== 'mix-no-probe') assert.equal(raw.stdout, `FINAL=${x},[${changed.join(', ')}]`);
    assert.throws(() => verify({...raw, originalSource:raw.originalSource + ' '}), /Stale original/);
    if (name === 'mix-formatted') assert.ok(raw.originalSource.includes('\r\n') && raw.originalSource.includes('\u{1f600}'));
    console.log(`PASS automatic ${name}: draft-2, distinct identities, mappings, forward/backward states`);
    continue;
  }
  const initial = scalar ? name === 'int-renamed' ? -3 : name === 'int-bounds-scalar' ? -2147483648 : 5
    : ['renamed', 'formatted'].includes(name) ? [9, -4] : name === 'int-bounds' ? [-2147483648, 2147483647] : name === 'empty' ? [] : [3, 1];
  const final = name === 'int-renamed' ? -9 : name === 'int-bounds-scalar' ? 2147483647 : name === 'int-repeat' ? 5 : 8;
  const expected = scalar ? [undefined, initial, final] : [undefined, initial, initial, [initial[1], initial[1]]];
  assert.deepEqual(raw.events.map(e => e.kind), (scalar ? ['VARIABLE_DECLARE', 'VARIABLE_WRITE'] : ['ARRAY_DECLARE', 'ARRAY_READ', 'ARRAY_WRITE']).slice(0, count));
  const states = [];
  for (let step = 0; step <= count; step++) {
    const state = stateAt(raw.events, step);
    assert.deepEqual(state.values, expected[step]);
    states.push(state);
  }
  for (let step = count; step >= 0; step--) assert.deepEqual(stateAt(raw.events, step), states[step]);
  assert.throws(() => verify({ ...raw, originalSource: raw.originalSource + ' ' }), /Stale original/);
  if (name === 'formatted' || name === 'int-formatted') assert.ok(raw.originalSource.includes('\r\n') && raw.originalSource.includes('😀'));
  console.log(`PASS automatic ${name}: ${result.schemaVersion}, original/generated mappings, ordered events, forward/backward states`);
}
for (const variant of incrementVariants) {
  const read = group => JSON.parse(readFileSync(resolve(root, '.results/automatic', group + variant + '.raw.json'), 'utf8'));
  const post = read('index-post-'), pre = read('index-pre-');
  // Source hashes/ranges differ; execution facts and all reconstructed values must agree.
  const facts = raw => raw.events.map(({source, ...event}) => event);
  assert.deepEqual(facts(post), facts(pre), `Prefix/postfix execution facts: ${variant}`);
  assert.equal(post.outcome, pre.outcome);
  if (post.events.length >= 4) {
    const bad = structuredClone(post); bad.events[3].value.value = incrementValues(variant)[0];
    assert.throws(() => verify(bad), /Capture committed value/);
  }
}
console.log(`PASS ${cases.length} automatic results and 20 prefix/postfix pairs; reconstruction never reruns Java`);
