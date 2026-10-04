/** Reconstruct a validated draft-4 prefix. Values/branch decisions come only from recorded events. */
export function replayConditional(events,count){
  if(!Number.isInteger(count)||count<0||count>events.length)throw new RangeError('Invalid playback cursor');
  const bindings={},arrays={};
  for(const e of events.slice(0,count)){
    if(e.kind==='ARRAY_DECLARE'){bindings[e.variableId]={name:e.variableName,arrayId:e.arrayId};arrays[e.arrayId]=e.values.map(v=>v.value);}
    else if(e.kind==='VARIABLE_DECLARE')bindings[e.variableId]={name:e.variableName,value:e.value.value};
    else if(e.kind==='VARIABLE_WRITE')bindings[e.variableId].value=e.value.value;
    else if(e.kind==='ARRAY_WRITE')arrays[e.arrayId][e.index]=e.value.value;
    else if(e.kind!=='ARRAY_READ'&&e.kind!=='CONDITION')throw new Error('Unsupported conditional event');
    for(const id of e.exitedVariableIds)delete bindings[id];
  }
  const selected=events[count-1];
  return {bindings,arrays,highlight:selected?structuredClone(selected.source):null,
    comparison:selected?.kind==='CONDITION'?{left:selected.comparison.left.value,right:selected.comparison.right.value,value:selected.value.value}:null,
    read:selected?.kind==='ARRAY_READ'?{arrayId:selected.arrayId,index:selected.index,value:selected.value.value}:null};
}
