function coerceConductionMethod(raw) {
  if (raw == null || raw === '') return ''
  if (typeof raw === 'string' || typeof raw === 'number') return String(raw)
  if (typeof raw === 'object' && raw.id != null) return String(raw.id)
  if (typeof raw === 'object' && raw.name != null && raw.name !== '') return String(raw.name)
  return ''
}

export function buildNodeAlgoDialogState(node, globalConfig = {}) {
  return {
    weightAssignAlgorithm: String(node?.weightAssignAlgorithm || ''),
    weightAssignAlgorithmParams: node?.weightAssignAlgorithmParams || {},
    conductionAlgorithm: coerceConductionMethod(node?.conductionAlgorithm) || '',
    conductionAlgorithmParams: node?.conductionAlgorithmParams || {}
  }
}

export function applyNodeAlgoDialogState(node, dialogState = {}) {
  if (!node) return

  const selectedWeight = dialogState.weightAssignAlgorithm || ''
  if (selectedWeight) {
    // If it's a number string, store as number
    node.weightAssignAlgorithm = /^\d+$/.test(selectedWeight) ? Number(selectedWeight) : selectedWeight
    node.weightAssignAlgorithmParams = dialogState.weightAssignAlgorithmParams || {}
  } else {
    delete node.weightAssignAlgorithm
    delete node.weightAssignAlgorithmParams
  }

  const selectedConduction = coerceConductionMethod(dialogState.conductionAlgorithm)
  if (selectedConduction) {
    node.conductionAlgorithm = /^\d+$/.test(selectedConduction) ? Number(selectedConduction) : selectedConduction
    node.conductionAlgorithmParams = dialogState.conductionAlgorithmParams || {}
  } else {
    delete node.conductionAlgorithm
    delete node.conductionAlgorithmParams
  }
}
