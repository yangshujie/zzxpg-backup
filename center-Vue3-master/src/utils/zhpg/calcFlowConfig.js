function clonePlain(value) {
  return JSON.parse(JSON.stringify(value || {}))
}

export function buildDesignModeCalcFlowConfig({ stages, runtimePolicy }) {
  const nextStages = clonePlain(stages)
  delete nextStages.weightCalc
  return {
    stages: nextStages,
    runtimePolicy: clonePlain(runtimePolicy)
  }
}
