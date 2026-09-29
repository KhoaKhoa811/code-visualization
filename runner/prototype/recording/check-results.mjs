import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const cases = ['success', 'changed', 'failed-read', 'failed-write', 'trace-limit', 'cancel', 'blocked-open'];
const hash = text => createHash('sha256').update(text, 'utf8').digest('hex');
function verifySource(raw) {
  assert.equal(hash(raw.originalSource), raw.sourceId, 'Trace detached from original source');
  assert.notEqual(raw.sourceId, raw.instrumentedSourceId, 'Original and generated identities conflated');
  const lines = raw.originalSource.split(/\r?\n/);
  for (const event of raw.events) {
    const { start, end } = event.source;
    assert.equal(event.source.file, 'Main.java');
    assert.equal(start.line, end.line);
    const text = lines[start.line - 1]?.slice(start.column - 1, end.column - 1);
    assert.ok(text, 'Invalid original-source coordinates');
    if (event.kind === 'ARRAY_DECLARE') assert.match(text, /^int\[\] values = \{-?\d+, -?\d+\}$/);
    if (event.kind === 'ARRAY_READ') assert.equal(text, `values[${event.index}]`);
    if (event.kind === 'ARRAY_WRITE') assert.match(text, new RegExp(`^values\\[${event.index}\\] = values\\[\\d+\\]$`));
  }
}

// Test-only prefix reconstruction. It reads captured facts and never evaluates Java source.
function stateAt(events, step) {
  const state = { arrays: {}, bindings: {}, highlight: null };
  for (const event of events.slice(0, step)) {
    if (event.kind === 'ARRAY_DECLARE') {
      state.arrays[event.arrayId] = event.values.map(v => v.value);
      state.bindings[event.variableId] = event.arrayId;
    } else if (event.kind === 'ARRAY_WRITE') state.arrays[event.arrayId][event.index] = event.value.value;
    state.highlight = event.source;
  }
  return state;
}

for (const name of cases) {
  const raw = JSON.parse(readFileSync(resolve(root, '.results', name + '.raw.json'), 'utf8'));
  verifySource(raw);
  const mappings = {
    SUCCESS: ['completed', null], RUNTIME_FAILURE: ['failed', 'runtime_error'],
    COMPILE_ERROR: ['failed', 'compilation_error'], INFRASTRUCTURE_ERROR: ['failed', 'engine_error'],
    TRACE_ERROR: ['failed', 'engine_error'], TRACE_LIMIT: ['limited', 'trace_limit'],
    TIMEOUT: ['limited', 'resource_limit'], OUTPUT_LIMIT: ['limited', 'resource_limit'],
    CANCELLED: ['cancelled', 'cancelled']
  };
  assert.ok(mappings[raw.outcome], 'Unmapped runner outcome');
  const [executionOutcome, category] = mappings[raw.outcome];
  const count = raw.events.length;
  const complete = raw.complete && executionOutcome === 'completed';
  const diagnostics = category ? [{ category, message: `Controlled experiment outcome: ${raw.outcome}`, source: null }] : [];
  if (!complete) diagnostics.push({ category: 'visualization_limitation',
    message: raw.problem || 'Only the validated recorded prefix is available.', source: null });
  const result = {
    runId: raw.runId, sourceId: raw.sourceId, schemaVersion: 'draft-1', executionOutcome,
    visualizationCoverage: count === 0 ? 'unavailable' : complete ? 'complete' : 'partial',
    diagnostics, events: count === 0 ? null : raw.events,
    safePlaybackBoundary: count === 0 ? null : { lastSafeStep: count, lastSafeEventSequence: count },
    console: { stdout: raw.stdout, stderr: raw.stderr }
  };
  const path = resolve(root, '.results', name + '.result.json');
  writeFileSync(path, JSON.stringify(result, null, 2) + '\n');
  const validation = spawnSync(process.execPath, [resolve(root, '../../../contracts/validate.mjs'), path],
    { encoding: 'utf8', timeout: 5000, maxBuffer: 65536 });
  assert.equal(validation.status, 0, validation.error?.message || validation.stdout + validation.stderr);

  const initial = name === 'changed' ? [9, -4] : [3, 1];
  const expectedStates = [undefined, initial, initial, [initial[1], initial[1]]];
  const saved = [];
  for (let step = 0; step <= count; step++) {
    const state = stateAt(raw.events, step);
    assert.deepEqual(state.arrays['array-1'], expectedStates[step]);
    assert.deepEqual(state.bindings, step === 0 ? {} : { 'variable-1': 'array-1' });
    assert.deepEqual(state.highlight, step === 0 ? null : raw.events[step - 1].source);
    saved.push(state);
  }
  for (let step = count; step >= 0; step--) assert.deepEqual(stateAt(raw.events, step), saved[step]);
  const stale = structuredClone(raw); stale.originalSource += '\n// edited';
  assert.throws(() => verifySource(stale), /detached/);
  if (name === 'changed') {
    assert.ok(raw.originalSource.includes('\r\n') && raw.originalSource.includes('🌱'));
    assert.equal(raw.events[0].source.start.column, 18, 'Source columns must count UTF-16 code units');
  }
  console.log(`PASS ${name}: draft-1 contract, source identity/ranges, ${count + 1} prefix positions, backward reconstruction`);
}
console.log('7/7 generated results validated. No Java execution occurs during reconstruction.');
