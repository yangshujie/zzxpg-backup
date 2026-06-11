function normalizeAlgorithmValue(value) {
  if (value == null || value === '') return undefined
  const text = String(value)
  return /^\d+$/.test(text) ? Number(text) : value
}

function hasChildren(node) {
  return Array.isArray(node?.children) && node.children.length > 0
}

/**
 * Sync global weight/conduction algorithm config to every non-leaf node.
 * The algorithm id/name is the single source of truth; subtype is not written.
 */
export function syncGlobalAlgorithmsToParentNodes(nodes, config = {}) {
  const {
    weightAssignAlgorithm,
    weightAssignAlgorithmParams,
    conductionAlgorithm,
    conductionAlgorithmParams
  } = config

  for (const node of nodes || []) {
    if (!hasChildren(node)) continue

    if (weightAssignAlgorithm) {
      node.weightAssignAlgorithm = normalizeAlgorithmValue(weightAssignAlgorithm)
      node.weightAssignAlgorithmParams = { ...(weightAssignAlgorithmParams || {}) }
      delete node.subtype
    }

    if (conductionAlgorithm) {
      node.conductionAlgorithm = normalizeAlgorithmValue(conductionAlgorithm)
      node.conductionAlgorithmParams = { ...(conductionAlgorithmParams || {}) }
    }

    syncGlobalAlgorithmsToParentNodes(node.children, config)
  }
}

export function removeWeightAssignSubtypeFromParentNodes(nodes) {
  for (const node of nodes || []) {
    if (!hasChildren(node)) continue
    delete node.subtype
    removeWeightAssignSubtypeFromParentNodes(node.children)
  }
}
