import { readFileSync, readdirSync } from 'node:fs';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { isDeepStrictEqual } from 'node:util';
import assert from 'node:assert/strict';
import { loopContractTests } from './loop-contract-tests.mjs';
import { conditionalSemantics } from './conditional-semantics.mjs';
import { conditionalContractTests } from './conditional-contract-tests.mjs';

const root = dirname(fileURLToPath(import.meta.url));
const schemas = {
  'draft-1': JSON.parse(readFileSync(join(root, 'run-result.schema.json'), 'utf8')),
  'draft-2': JSON.parse(readFileSync(join(root, 'run-result-v2.schema.json'), 'utf8')),
  'draft-3': JSON.parse(readFileSync(join(root, 'run-result-v3.schema.json'), 'utf8')),
  'draft-4': JSON.parse(readFileSync(join(root, 'run-result-v4.schema.json'), 'utf8'))
};
let schema = schemas['draft-1']; // Synchronous validation selects the matching trusted contract.
const supported = new Set(['$schema', '$defs', '$ref', 'title', 'description', 'type',
  'properties', 'required', 'additionalProperties', 'const', 'enum', 'oneOf',
  'items', 'minItems', 'minLength', 'minimum', 'maximum']);
const object = v => v !== null && typeof v === 'object' && !Array.isArray(v);

// Deliberately limited to this repository's trusted schema, not a general validator.
function checkSchema(s, refs = new Set()) {
  if (!object(s)) throw Error('Only object schemas are supported');
  for (const key of Object.keys(s)) {
    if (!supported.has(key)) throw Error('Unsupported schema keyword: ' + key);
  }
  if (s.$ref) {
    if (!/^#\/\$defs\/[a-zA-Z]+$/.test(s.$ref)) throw Error('Only local named definitions supported');
    const name = s.$ref.split('/').at(-1);
    if (!schema.$defs[name]) throw Error('Missing definition: ' + name);
    if (refs.has(name)) throw Error('Recursive references are not supported');
    checkSchema(schema.$defs[name], new Set([...refs, name]));
  }
  if (s.type && !['object', 'array', 'string', 'integer', 'boolean', 'null'].includes(s.type)) {
    throw Error('Unsupported schema type: ' + s.type);
  }
  if ('additionalProperties' in s && s.additionalProperties !== false) {
    throw Error('Only additionalProperties:false is supported');
  }
  for (const child of Object.values(s.properties ?? {})) checkSchema(child, refs);
  for (const child of Object.values(s.$defs ?? {})) checkSchema(child, refs);
  for (const child of s.oneOf ?? []) checkSchema(child, refs);
  if (s.items) checkSchema(s.items, refs);
}

function structural(s, value, path = '$') {
  const errors = [];
  const fail = message => errors.push(path + ': ' + message);
  if (s.$ref) errors.push(...structural(schema.$defs[s.$ref.split('/').at(-1)], value, path));
  if (s.type) {
    const valid = s.type === 'object' ? object(value)
      : s.type === 'array' ? Array.isArray(value)
      : s.type === 'integer' ? Number.isInteger(value)
      : s.type === 'null' ? value === null : typeof value === s.type;
    if (!valid) { fail('expected ' + s.type); return errors; }
  }
  if ('const' in s && !isDeepStrictEqual(value, s.const)) fail('wrong constant');
  if (s.enum && !s.enum.some(v => isDeepStrictEqual(v, value))) fail('unknown enum value');
  if (s.oneOf && s.oneOf.filter(branch => structural(branch, value, path).length === 0).length !== 1) {
    fail('must match exactly one variant');
  }
  if (typeof value === 'string' && [...value].length < (s.minLength ?? 0)) fail('string too short');
  if (typeof value === 'number') {
    if (value < s.minimum) fail('below minimum');
    if (value > s.maximum) fail('above maximum');
  }
  if (Array.isArray(value)) {
    if (value.length < (s.minItems ?? 0)) fail('too few items');
    if (s.items) value.forEach((v, i) => errors.push(...structural(s.items, v, path + '[' + i + ']')));
  }
  if (object(value)) {
    for (const key of s.required ?? []) if (!Object.hasOwn(value, key)) fail('missing ' + key);
    for (const [key, v] of Object.entries(value)) {
      if (Object.hasOwn(s.properties ?? {}, key)) {
        errors.push(...structural(s.properties[key], v, path + '.' + key));
      } else if (s.additionalProperties === false) fail('unexpected field ' + key);
    }
  }
  return errors;
}

function semantic(result) {
  const errors = [], arrays = new Map(), variables = new Map();
  const scoped = result.schemaVersion === 'draft-3';
  const scopes = new Map(), retired = new Set(), closedScopes = new Set(), conditionScopes = new Set();
  const range = (s, label) => {
    if (s && (s.end.line < s.start.line ||
        (s.end.line === s.start.line && s.end.column <= s.start.column))) {
      errors.push(label + ': source range must be nonempty and forward');
    }
  };
  result.diagnostics.forEach((d, i) => range(d.source, 'diagnostic ' + i));
  const categories = new Set(result.diagnostics.map(d => d.category));
  const reasons = { failed: ['compilation_error', 'runtime_error', 'engine_error', 'execution_policy'],
    limited: ['trace_limit', 'resource_limit'], cancelled: ['cancelled'] };
  if (reasons[result.executionOutcome] &&
      !reasons[result.executionOutcome].some(c => categories.has(c))) {
    errors.push('execution outcome lacks a matching diagnostic');
  }
  if (result.executionOutcome === 'completed' &&
      [...categories].some(c => c !== 'visualization_limitation')) {
    errors.push('completed execution contradicts diagnostic category');
  }
  if (categories.has('compilation_error') || categories.has('execution_policy')) {
    if (result.executionOutcome !== 'failed' || result.visualizationCoverage !== 'unavailable') {
      errors.push('compilation/admission failure cannot have execution playback');
    }
  }
  if (result.events === null) return errors;
  if (result.schemaVersion === 'draft-4') return [...errors, ...conditionalSemantics(result)];
  result.events.forEach((e, i) => {
    range(e.source, 'event ' + i);
    if (e.sequence !== i + 1) errors.push('event sequence must be contiguous from 1');
    if (scoped) {
      // Validate the entire lifecycle effect before applying any part of this record.
      const problems = [];
      const declaration = e.kind === 'ARRAY_DECLARE' || e.kind === 'VARIABLE_DECLARE';
      if (declaration && (variables.has(e.variableId) || retired.has(e.variableId))) problems.push('duplicate or retired declaration identity');
      if (declaration && closedScopes.has(e.scopeId)) problems.push('declaration reopens a retired scope');
      if (new Set(e.exitedVariableIds).size !== e.exitedVariableIds.length) problems.push('duplicate scope exit identity');
      if (e.kind === 'CONDITION') {
        const members = [...variables.keys()].filter(id => scopes.get(id) === e.scopeId);
        if (closedScopes.has(e.scopeId) || members.length !== 1 || variables.get(members[0]) !== 'int') {
          problems.push('condition requires one live int binding in its loop scope');
        }
        if (e.value.value) {
          if (e.exitedVariableIds.length) problems.push('true condition cannot retire bindings');
        } else if (e.exitedVariableIds.length !== 1 || e.exitedVariableIds[0] !== members[0]) {
          problems.push('false condition must atomically retire its loop binding');
        }
      } else if (e.exitedVariableIds.length) problems.push('only a false condition may retire bindings in this draft');
      for (const id of e.exitedVariableIds) {
        if (!variables.has(id) || retired.has(id) || scopes.get(id) !== e.scopeId) problems.push('unknown, retired or wrong-scope exit identity');
      }
      if (problems.length) { errors.push(...problems.map(p => 'event ' + i + ': ' + p)); return; }
    }
    if (e.kind === 'ARRAY_DECLARE') {
      if (arrays.has(e.arrayId) || variables.has(e.variableId)) errors.push('duplicate declaration identity');
      arrays.set(e.arrayId, e.values.map(v => v.value));
      variables.set(e.variableId, 'array');
    } else if (e.kind === 'VARIABLE_DECLARE') {
      if (variables.has(e.variableId)) errors.push('duplicate declaration identity');
      variables.set(e.variableId, 'int');
    } else if (e.kind === 'VARIABLE_WRITE') {
      if (variables.get(e.variableId) !== 'int') errors.push('scalar write must refer to a declared int binding');
    } else if (e.kind === 'CONDITION') {
      conditionScopes.add(e.scopeId);
    } else {
      const a = arrays.get(e.arrayId);
      if (!a) { errors.push('array reference precedes declaration'); return; }
      if (e.index >= a.length) { errors.push('array index out of bounds'); return; }
      if (e.kind === 'ARRAY_READ' && a[e.index] !== e.value.value) errors.push('read value disagrees with state');
      if (e.kind === 'ARRAY_WRITE') a[e.index] = e.value.value;
    }
    if (scoped) {
      if (e.kind === 'ARRAY_DECLARE' || e.kind === 'VARIABLE_DECLARE') scopes.set(e.variableId, e.scopeId);
      for (const id of e.exitedVariableIds) { variables.delete(id); retired.add(id); }
      if (e.kind === 'CONDITION' && !e.value.value) closedScopes.add(e.scopeId);
    }
  });
  if (scoped && result.visualizationCoverage === 'complete' && [...conditionScopes].some(id => !closedScopes.has(id))) {
    errors.push('complete loop coverage requires a recorded false condition and scope retirement');
  }
  const b = result.safePlaybackBoundary;
  // This draft has one event per step and delivers only the safe prefix.
  if (b.lastSafeStep !== result.events.length || b.lastSafeEventSequence !== result.events.length) {
    errors.push('boundary must match the delivered safe prefix in this draft');
  }
  return errors;
}

function validate(value) {
  schema = schemas[value?.schemaVersion] ?? schemas['draft-1'];
  const shape = structural(schema, value);
  return { structural: shape, semantic: shape.length ? [] : semantic(value) };
}
const invalid = report => report.structural.length + report.semantic.length > 0;
const read = path => JSON.parse(readFileSync(path, 'utf8'));

function selfTest(fixtures) {
  const success = fixtures['array-success.json'];
  const tests = [
    ['missing identity', 'structural', success, r => { delete r.runId; }],
    ['wrong version', 'structural', success, r => { r.schemaVersion = 'v99'; }],
    ['unknown field', 'structural', success, r => { r.events[0].color = 'red'; }],
    ['integer overflow', 'structural', success, r => { r.events[0].values[0].value = 2147483648; }],
    ['fractional integer', 'structural', success, r => { r.events[0].values[0].value = 1.5; }],
    ['wrong event payload', 'structural', success, r => { delete r.events[1].index; }],
    ['unavailable with events', 'structural', success, r => { r.visualizationCoverage = 'unavailable'; }],
    ['partial without explanation', 'structural', success, r => { r.visualizationCoverage = 'partial'; }],
    ['sequence gap', 'semantic', success, r => { r.events[1].sequence = 4; }],
    ['unknown object', 'semantic', success, r => { r.events[1].arrayId = 'unknown'; }],
    ['index out of bounds', 'semantic', success, r => { r.events[1].index = 2; }],
    ['wrong read', 'semantic', success, r => { r.events[1].value.value = 99; }],
    ['wrong boundary', 'semantic', success, r => { r.safePlaybackBoundary.lastSafeStep = 1; }],
    ['backward range', 'semantic', success, r => { r.events[0].source.end.column = 1; }],
    ['duplicate object', 'semantic', success, r => {
      r.events[1] = structuredClone(r.events[0]); r.events[1].sequence = 2;
    }],
    ['completed exception', 'semantic', fixtures['runtime-error.json'], r => { r.executionOutcome = 'completed'; }],
    ['unexplained limit', 'semantic', success, r => { r.executionOutcome = 'limited'; }],
    ['compilation with playback', 'semantic', fixtures['runtime-error.json'], r => {
      r.diagnostics[0].category = 'compilation_error';
    }]
  ];
  for (const [name, layer, original, mutate] of tests) {
    const value = structuredClone(original); mutate(value);
    const report = validate(value);
    assert(report[layer].length > 0, name + ' was not rejected by ' + layer);
    if (layer === 'semantic') assert.equal(report.structural.length, 0, name + ' failed wrong layer');
  }
  // Valid edge cases must not be over-rejected.
  for (const number of [-2147483648, 2147483647]) {
    const r = structuredClone(success); r.events[0].values[0].value = number;
    assert(!invalid(validate(r)), 'valid Java int bound rejected');
  }
  const emptyArray = structuredClone(success);
  emptyArray.events = [emptyArray.events[0]]; emptyArray.events[0].values = [];
  emptyArray.safePlaybackBoundary = { lastSafeStep: 1, lastSafeEventSequence: 1 };
  assert(!invalid(validate(emptyArray)), 'empty array rejected');
  assert.throws(() => checkSchema({ mysteryKeyword: true }), /Unsupported schema keyword/);
  const scalar = structuredClone(success);
  scalar.schemaVersion = 'draft-2';
  scalar.events = [
    {sequence:1,kind:'VARIABLE_DECLARE',source:success.events[0].source,variableId:'v',variableName:'x',value:{type:'int',value:5}},
    {sequence:2,kind:'VARIABLE_WRITE',source:success.events[1].source,variableId:'v',value:{type:'int',value:8}}
  ];
  scalar.safePlaybackBoundary = {lastSafeStep:2,lastSafeEventSequence:2};
  assert(!invalid(validate(scalar)), 'draft-2 scalar rejected');
  const newArray = structuredClone(success); newArray.schemaVersion = 'draft-2';
  assert(!invalid(validate(newArray)), 'draft-2 array compatibility rejected');
  for (const mutate of [
    r => { r.schemaVersion = 'draft-1'; },
    r => { r.events[1].variableId = 'unknown'; },
    r => { r.events[1] = {...r.events[0],sequence:2}; },
    r => { r.events[0].value.value = 2147483648; },
    r => { r.events[1].arrayId = 'array-1'; },
    r => { r.events[0] = structuredClone(success.events[0]); r.events[1].variableId = r.events[0].variableId; }
  ]) { const changed = structuredClone(scalar); mutate(changed); assert(invalid(validate(changed)), 'Invalid scalar accepted'); }
  console.log('PASS: draft-2 scalar and array compatibility; 6 scalar negative cases');
  const mixed = structuredClone(newArray);
  mixed.events = [structuredClone(scalar.events[0]), {...structuredClone(success.events[0]), sequence:2, variableId:'array-binding'},
    {...structuredClone(success.events[2]), sequence:3}];
  assert(!invalid(validate(mixed)), 'draft-2 mixed bindings rejected');
  for (const mutate of [
    r => { r.events[1].variableId = r.events[0].variableId; },
    r => { r.events[2].arrayId = 'unknown'; },
    r => { r.events[2] = {...structuredClone(scalar.events[1]), sequence:3, variableId:'array-binding'}; },
    r => { r.events[2] = {...structuredClone(r.events[1]), sequence:3, variableId:'another-binding'}; },
    r => { r.events[2].sequence = 4; },
    r => { r.events[0].value.value = 2147483648; }
  ]) { const changed = structuredClone(mixed); mutate(changed); assert(invalid(validate(changed)), 'Invalid mixed trace accepted'); }
  console.log('PASS: draft-2 mixed bindings and 6 mixed negative cases');
  const updated = structuredClone(mixed);
  updated.events.splice(2, 0, {...structuredClone(scalar.events[1]), sequence:3});
  updated.events[3].sequence = 4;
  updated.events[3].value.value = 8;
  updated.safePlaybackBoundary = {lastSafeStep:4,lastSafeEventSequence:4};
  assert(!invalid(validate(updated)), 'Interleaved scalar write rejected');
  for (const mutate of [
    r => { r.events[2].variableId = 'array-binding'; },
    r => { r.events[2].variableId = 'unknown'; },
    r => { r.events[2].value.value = 2147483648; },
    r => { r.events[2].arrayId = 'array-1'; },
    r => { r.events[3].sequence = 5; },
    r => { r.safePlaybackBoundary.lastSafeStep = 3; }
  ]) { const changed = structuredClone(updated); mutate(changed); assert(invalid(validate(changed)), 'Invalid interleaved scalar write accepted'); }
  console.log('PASS: interleaved scalar write and 6 negative cases');
  const indexed = structuredClone(updated);
  indexed.events[2] = {...structuredClone(indexed.events[0]),sequence:3,variableId:'index-binding',variableName:'i',value:{type:'int',value:1}};
  indexed.events[3].index = 1;
  assert(!invalid(validate(indexed)), 'Two distinct scalar declarations rejected');
  for (const mutate of [
    r => { r.events[2].variableId = r.events[0].variableId; },
    r => { r.events[2].variableId = r.events[1].variableId; },
    r => { r.events[2].value.value = 2147483648; },
    r => { r.events[2].source.end.column = 0; },
    r => { r.events[3].arrayId = 'unknown'; },
    r => { r.events[2].sequence = 4; }
  ]) { const changed = structuredClone(indexed); mutate(changed); assert(invalid(validate(changed)), 'Invalid two-scalar trace accepted'); }
  console.log('PASS: two scalar declarations and 6 negative cases');
  const indexUpdated = structuredClone(indexed);
  indexUpdated.events[2].value.value = 0;
  indexUpdated.events.splice(3, 0, {...structuredClone(scalar.events[1]), sequence:4, variableId:'index-binding', value:{type:'int',value:1}});
  indexUpdated.events[4].sequence = 5;
  indexUpdated.safePlaybackBoundary = {lastSafeStep:5,lastSafeEventSequence:5};
  assert(!invalid(validate(indexUpdated)), 'Independent index write rejected');
  for (const mutate of [
    r => { r.events[3].variableId = 'array-binding'; },
    r => { r.events[3].variableId = 'unknown'; },
    r => { r.events[3].value.value = 2147483648; },
    r => { r.events[3].source.end.column = 0; },
    r => { r.events[3].sequence = 5; },
    r => { r.safePlaybackBoundary.lastSafeStep = 4; }
  ]) { const changed = structuredClone(indexUpdated); mutate(changed); assert(invalid(validate(changed)), 'Invalid index update accepted'); }
  // Source-specific target and final-index agreement belong to the operation-plan collector.
  console.log('PASS: independent index write and 6 negative cases');
  const wrapped = structuredClone(indexUpdated);
  wrapped.events[2].value.value = -2147483648;
  wrapped.events[3].value.value = 0;
  wrapped.events[4].index = 0;
  assert(!invalid(validate(wrapped)), 'Wrapped index result rejected');
  console.log('PASS: recorded wrapped-int index value uses existing draft-2');
  console.log('PASS: ' + tests.length + ' negative cases, 3 positive edge cases, unsupported-keyword guard');
  loopContractTests({fixtures, validate, checkSchema, structural, root});
  conditionalContractTests({fixtures, validate, root});
}

try {
  for (const candidate of Object.values(schemas)) { schema = candidate; checkSchema(schema); }
  const args = process.argv.slice(2);
  const test = args.includes('--self-test');
  const files = args.filter(a => a !== '--self-test');
  if (files.some(a => a.startsWith('--'))) throw Error('Unknown option');
  const fixturePaths = readdirSync(join(root, 'examples')).filter(n => n.endsWith('.json')).sort();
  const fixtures = Object.fromEntries(fixturePaths.map(n => [n, read(join(root, 'examples', n))]));
  const inputs = files.length ? files.map(p => [p, read(resolve(p))]) : Object.entries(fixtures);
  let failures = 0;
  for (const [name, value] of inputs) {
    const report = validate(value);
    if (invalid(report)) {
      failures++; console.error('FAIL: ' + name, report);
    } else console.log('PASS: ' + name);
  }
  if (test) selfTest(fixtures);
  process.exitCode = failures ? 1 : 0;
} catch (error) {
  console.error(error.message); process.exitCode = 1;
}
