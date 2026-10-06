import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {replayLoop} from './loop-replay.mjs';
import {replayConditional} from './conditional-replay.mjs';

const fixture = name => JSON.parse(readFileSync(new URL(`../../../contracts/examples/${name}.json`, import.meta.url), 'utf8')).events;
const loop = fixture('loop-success');
const conditional = fixture('conditional-success');

for (const [replay, events, message] of [
  [replayLoop, loop, 'Unsupported loop replay event'],
  [replayConditional, conditional, 'Unsupported conditional event']
]) {
  const original = structuredClone(events);
  for (const cursor of [-1, 0.5, NaN, Infinity, events.length + 1]) {
    assert.throws(() => replay(events, cursor), {name:'RangeError', message:'Invalid playback cursor'});
  }
  assert.throws(() => replay([{kind:'UNKNOWN', exitedVariableIds:[]}], 1), {message});
  const state = replay(events, events.length);
  state.arrays['array-1'][0] = 99;
  state.highlight.start.line = 999;
  for (const binding of Object.values(state.bindings)) binding.name = 'changed';
  assert.deepEqual(events, original, 'returned state must not mutate captured facts');
  assert.notDeepEqual(replay(events, events.length), state, 'calls must rebuild independent state');
}

assert.deepEqual(replayLoop(loop, 0), {bindings:{}, arrays:{}, highlight:null, condition:null});
assert.deepEqual(replayConditional(conditional, 0), {bindings:{}, arrays:{}, highlight:null, comparison:null, read:null});
assert.equal(Object.hasOwn(replayLoop(loop, 1), 'read'), false, 'loop omits unselected read');
assert.equal(replayConditional(conditional, 1).read, null, 'conditional keeps null read');
assert.deepEqual(replayConditional(conditional, 2).read, {arrayId:'array-1', index:0, value:3});
assert.deepEqual(replayConditional(conditional, 4).comparison, {left:3, right:1, value:true});
assert.deepEqual(replayConditional(conditional, 8).arrays['array-1'], [1,1]);
assert.equal(replayConditional(conditional, 8).bindings['variable-2'].value, 3);
assert.deepEqual(replayConditional(conditional, 9).arrays['array-1'], [1,3]);
assert.equal(Object.hasOwn(replayConditional(conditional, 9).bindings, 'variable-2'), false);
assert.deepEqual(replayLoop(loop, loop.length).arrays['array-1'], [8,8,8]);
assert.equal(Object.hasOwn(replayLoop(loop, loop.length).bindings, 'variable-3'), false);
console.log('PASS replay adapter compatibility: output shapes, errors, captured comparisons/reads, retirement, reverse snapshots and mutation isolation');
