export const FLOW_STEP_STATUS = {
  PENDING: 'pending',
  EDITING: 'editing',
  RUNNING: 'running',
  COMPLETED: 'completed',
  STALE: 'stale',
  FAILED: 'failed'
}

const EXECUTE_STEPS = ['scheduleConfig', 'weightCalc', 'comprehensiveCalc', 'reportOutput']
const DESIGN_STEPS = ['scheduleConfig', 'comprehensiveCalc', 'reportOutput']

function createStep(status = FLOW_STEP_STATUS.PENDING) {
  return {
    status,
    revision: 0,
    completedAt: '',
    staleReason: '',
    dataRef: {}
  }
}

export function createFlowStepState(mode = 'execute') {
  const keys = mode === 'design' ? DESIGN_STEPS : EXECUTE_STEPS
  return keys.reduce((state, key) => {
    state[key] = createStep()
    return state
  }, {})
}

export function markStepCompleted(state, key, dataRef = {}) {
  if (!state?.[key]) return
  state[key].status = FLOW_STEP_STATUS.COMPLETED
  state[key].revision += 1
  state[key].completedAt = new Date().toISOString()
  state[key].staleReason = ''
  state[key].dataRef = { ...(state[key].dataRef || {}), ...dataRef }
}

export function markStepRunning(state, key, dataRef = {}) {
  if (!state?.[key]) return
  state[key].status = FLOW_STEP_STATUS.RUNNING
  state[key].staleReason = ''
  state[key].dataRef = { ...(state[key].dataRef || {}), ...dataRef }
  if (key === 'comprehensiveCalc' && state.reportOutput) {
    markStepPending(state, 'reportOutput')
  }
}

export function markStepPending(state, key) {
  if (!state?.[key]) return
  state[key].status = FLOW_STEP_STATUS.PENDING
  state[key].staleReason = ''
}

export function markStepStale(state, key, reason = '') {
  if (!state?.[key]) return
  state[key].status = FLOW_STEP_STATUS.STALE
  state[key].staleReason = reason
}

export function markStepFailed(state, key, reason = '') {
  if (!state?.[key]) return
  state[key].status = FLOW_STEP_STATUS.FAILED
  state[key].staleReason = reason
}

export function restoreFlowStepState({ mode = 'execute', hasEvalResult = false, hasReports = false } = {}) {
  const state = createFlowStepState(mode)
  markStepCompleted(state, 'scheduleConfig')
  if (state.weightCalc) markStepCompleted(state, 'weightCalc')
  if (hasEvalResult) markStepCompleted(state, 'comprehensiveCalc')
  if (hasReports) markStepCompleted(state, 'reportOutput')
  return state
}
