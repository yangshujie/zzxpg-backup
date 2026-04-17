function coerceConductionMethod(raw) {
  if (raw == null || raw === '') return ''
  if (typeof raw === 'string') return raw
  if (typeof raw === 'object' && raw.name != null && raw.name !== '') return String(raw.name)
  return ''
}

export function buildNodeAlgoDialogState(node, globalConfig = {}) {
  return {
    weightAssignAlgorithm: node?.weightAssignAlgorithm || globalConfig.weightAssignAlgorithm || '',
    conductionAlgorithm: coerceConductionMethod(node?.conductionAlgorithm) ||
      coerceConductionMethod(globalConfig.conductionAlgorithm) ||
      ''
  }
}

export function applyNodeAlgoDialogState(node, dialogState = {}, globalConfig = {}) {
  if (!node) return

  const globalWeight = globalConfig.weightAssignAlgorithm || ''
  const selectedWeight = dialogState.weightAssignAlgorithm || ''
  if (selectedWeight && selectedWeight !== globalWeight) {
    node.weightAssignAlgorithm = selectedWeight
  } else {
    delete node.weightAssignAlgorithm
  }

  const globalConduction = coerceConductionMethod(globalConfig.conductionAlgorithm)
  const selectedConduction = coerceConductionMethod(dialogState.conductionAlgorithm)
  if (selectedConduction && selectedConduction !== globalConduction) {
    node.conductionAlgorithm = selectedConduction
  } else {
    delete node.conductionAlgorithm
  }
}
