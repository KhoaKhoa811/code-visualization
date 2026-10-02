/** Rebuild a selected prefix of validated draft-3 loop events. No source evaluation or mutable cached state. */
export function replayLoop(events, count) {
  if (!Number.isInteger(count) || count < 0 || count > events.length) throw new RangeError('Invalid playback cursor');
  const bindings = {}, arrays = {};
  for (const event of events.slice(0, count)) {
    if (event.kind === 'VARIABLE_DECLARE') bindings[event.variableId] = {name:event.variableName, value:event.value.value};
    else if (event.kind === 'VARIABLE_WRITE') bindings[event.variableId].value = event.value.value;
    else if (event.kind === 'ARRAY_DECLARE') {
      bindings[event.variableId] = {name:event.variableName, arrayId:event.arrayId};
      arrays[event.arrayId] = event.values.map(v => v.value);
    } else if (event.kind === 'ARRAY_WRITE') arrays[event.arrayId][event.index] = event.value.value;
    else if (event.kind !== 'CONDITION' && event.kind !== 'ARRAY_READ') throw new Error('Unsupported loop replay event');
    for (const id of event.exitedVariableIds) delete bindings[id];
  }
  const selected = count === 0 ? null : events[count - 1];
  return {bindings, arrays, highlight:selected ? structuredClone(selected.source) : null,
    condition:selected?.kind === 'CONDITION' ? selected.value.value : null,
    ...(selected?.kind === 'ARRAY_READ'
      ? {read:{arrayId:selected.arrayId,index:selected.index,value:selected.value.value}} : {})};
}
