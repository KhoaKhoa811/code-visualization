import {reconstructState} from './replay-state.mjs';

/** Draft-4 attention adapter; comparisons use recorded operands and decisions. */
export function replayConditional(events, count) {
  const state = reconstructState(events, count, 'Unsupported conditional event');
  const selected = count === 0 ? null : events[count - 1];
  return {...state,
    comparison:selected?.kind === 'CONDITION'
      ? {left:selected.comparison.left.value, right:selected.comparison.right.value, value:selected.value.value} : null,
    read:selected?.kind === 'ARRAY_READ'
      ? {arrayId:selected.arrayId, index:selected.index, value:selected.value.value} : null};
}
