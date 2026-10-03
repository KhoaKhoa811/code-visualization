/** Draft-4 runtime-fact checks. Source-specific branch/swap order belongs to the future collector plan. */
export function conditionalSemantics(result) {
  const errors = [], arrays = new Map(), bindings = new Map(), retired = new Set(), closed = new Set();
  const accepted = new Map();
  const rootScope = result.events[0]?.scopeId;
  for (const [i, event] of result.events.entries()) {
    const problems = [];
    const {source, scopeId, exitedVariableIds: exits} = event;
    const declaration = event.kind === 'ARRAY_DECLARE' || event.kind === 'VARIABLE_DECLARE';
    if (event.sequence !== i + 1) problems.push('sequence must be contiguous from 1');
    if (source.end.line < source.start.line || (source.end.line === source.start.line && source.end.column <= source.start.column))
      problems.push('source range must be nonempty and forward');
    if (closed.has(scopeId)) problems.push('operation in a retired scope');
    if (declaration && (bindings.has(event.variableId) || retired.has(event.variableId))) problems.push('duplicate or retired binding identity');
    if (new Set(exits).size !== exits.length) problems.push('duplicate scope exit identity');
    if (exits.length) {
      const members = [...bindings].filter(([, binding]) => binding.scopeId === scopeId);
      if (event.kind !== 'ARRAY_WRITE' || scopeId === rootScope || exits.length !== 1 || members.length !== 1)
        problems.push('retirement requires one branch-local int on an array write');
      for (const id of exits) {
        const binding = bindings.get(id);
        if (!binding || binding.kind !== 'int' || binding.scopeId !== scopeId) problems.push('unknown, retired or wrong-scope exit');
      }
    }
    if (event.kind === 'ARRAY_DECLARE') {
      if (arrays.has(event.arrayId)) problems.push('duplicate array identity');
    } else if (event.kind === 'VARIABLE_WRITE') {
      const binding = bindings.get(event.variableId);
      if (!binding || binding.kind !== 'int' || binding.scopeId !== scopeId) problems.push('write requires a live int in its scope');
    } else if (event.kind === 'ARRAY_READ' || event.kind === 'ARRAY_WRITE') {
      const array = arrays.get(event.arrayId);
      if (!array) problems.push('array reference precedes declaration');
      else if (event.index < 0 || event.index >= array.length) problems.push('array index out of bounds');
      else if (event.kind === 'ARRAY_READ' && array[event.index] !== event.value.value) problems.push('read disagrees with recorded state');
    } else if (event.kind === 'CONDITION') {
      const comparison = event.comparison;
      const left = accepted.get(comparison.leftReadSequence), right = accepted.get(comparison.rightReadSequence);
      if (comparison.leftReadSequence !== event.sequence - 2 || comparison.rightReadSequence !== event.sequence - 1)
        problems.push('comparison must link its immediately preceding ordered reads');
      if (left?.kind !== 'ARRAY_READ' || right?.kind !== 'ARRAY_READ') problems.push('comparison operands must link accepted reads');
      else {
        if (left.scopeId !== scopeId || right.scopeId !== scopeId) problems.push('comparison and reads require the same scope');
        if (left.value.value !== comparison.left.value || right.value.value !== comparison.right.value)
          problems.push('comparison operands disagree with linked reads');
      }
      if (event.value.value !== (comparison.left.value > comparison.right.value)) problems.push('comparison result disagrees with operands');
    }
    // Never commit a write or retirement from a rejected event, even while gathering further diagnostics.
    if (problems.length) { errors.push(...problems.map(p => `event ${i}: ${p}`)); continue; }
    if (event.kind === 'ARRAY_DECLARE') {
      arrays.set(event.arrayId, event.values.map(v => v.value));
      bindings.set(event.variableId, {kind:'array', scopeId, value:event.arrayId});
    } else if (event.kind === 'VARIABLE_DECLARE') bindings.set(event.variableId, {kind:'int', scopeId, value:event.value.value});
    else if (event.kind === 'VARIABLE_WRITE') bindings.get(event.variableId).value = event.value.value;
    else if (event.kind === 'ARRAY_WRITE') arrays.get(event.arrayId)[event.index] = event.value.value;
    for (const id of exits) { bindings.delete(id); retired.add(id); }
    if (exits.length) closed.add(scopeId);
    accepted.set(event.sequence, event);
  }
  if (result.visualizationCoverage === 'complete' && [...bindings.values()].some(binding => binding.scopeId !== rootScope))
    errors.push('complete coverage requires recorded branch-local retirement');
  const boundary = result.safePlaybackBoundary;
  if (boundary.lastSafeStep !== result.events.length || boundary.lastSafeEventSequence !== result.events.length)
    errors.push('boundary must match delivered safe prefix');
  return errors;
}
