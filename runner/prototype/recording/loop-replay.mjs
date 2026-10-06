import {reconstructState} from './replay-state.mjs';

/** Draft-3 attention adapter; state changes come only from validated captured events. */
export function replayLoop(events, count) {
  const state = reconstructState(events, count, 'Unsupported loop replay event');
  const selected = count === 0 ? null : events[count - 1];
  return {...state,
    condition:selected?.kind === 'CONDITION' ? selected.value.value : null,
    ...(selected?.kind === 'ARRAY_READ'
      ? {read:{arrayId:selected.arrayId, index:selected.index, value:selected.value.value}} : {})};
}
