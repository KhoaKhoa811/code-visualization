/** Shared state reconstruction for validated draft-3/draft-4 prefixes, not a trace validator. */
export function reconstructState(events, count, unsupportedEventMessage) {
  if (!Number.isInteger(count) || count < 0 || count > events.length) {
    throw new RangeError('Invalid playback cursor');
  }
  const bindings = {}, arrays = {};
  for (const event of events.slice(0, count)) {
    if (event.kind === 'VARIABLE_DECLARE') {
      bindings[event.variableId] = {name:event.variableName, value:event.value.value};
    } else if (event.kind === 'VARIABLE_WRITE') {
      bindings[event.variableId].value = event.value.value;
    } else if (event.kind === 'ARRAY_DECLARE') {
      bindings[event.variableId] = {name:event.variableName, arrayId:event.arrayId};
      arrays[event.arrayId] = event.values.map(value => value.value);
    } else if (event.kind === 'ARRAY_WRITE') {
      arrays[event.arrayId][event.index] = event.value.value;
    } else if (event.kind !== 'CONDITION' && event.kind !== 'ARRAY_READ') {
      throw new Error(unsupportedEventMessage);
    }
    for (const id of event.exitedVariableIds) delete bindings[id];
  }
  const selected = count === 0 ? null : events[count - 1];
  return {bindings, arrays, highlight:selected ? structuredClone(selected.source) : null};
}
