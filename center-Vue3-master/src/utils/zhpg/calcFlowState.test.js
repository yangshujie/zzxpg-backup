import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createFlowStepState,
  markStepCompleted,
  markStepRunning,
  markStepStale,
  restoreFlowStepState
} from './calcFlowState.js'

test('marks calculation and report stale when weight step changes after a result exists', () => {
  const state = restoreFlowStepState({
    mode: 'execute',
    hasEvalResult: true,
    hasReports: true
  })

  markStepCompleted(state, 'weightCalc')
  markStepStale(state, 'comprehensiveCalc', '指标体系已修改，请重新计算')
  markStepStale(state, 'reportOutput', '上游计算结果已过期，请重新生成报告')

  assert.equal(state.weightCalc.status, 'completed')
  assert.equal(state.comprehensiveCalc.status, 'stale')
  assert.equal(state.reportOutput.status, 'stale')
  assert.equal(state.comprehensiveCalc.staleReason, '指标体系已修改，请重新计算')
})

test('resets report to pending when a new calculation starts', () => {
  const state = restoreFlowStepState({
    mode: 'execute',
    hasEvalResult: true,
    hasReports: true
  })

  markStepRunning(state, 'comprehensiveCalc')

  assert.equal(state.comprehensiveCalc.status, 'running')
  assert.equal(state.reportOutput.status, 'pending')
})

test('restores completed steps from historical result and reports', () => {
  const state = restoreFlowStepState({
    mode: 'execute',
    hasEvalResult: true,
    hasReports: true
  })

  assert.equal(state.scheduleConfig.status, 'completed')
  assert.equal(state.weightCalc.status, 'completed')
  assert.equal(state.comprehensiveCalc.status, 'completed')
  assert.equal(state.reportOutput.status, 'completed')
})

test('creates design-mode state without execute-only weight step', () => {
  const state = createFlowStepState('design')

  assert.equal(state.scheduleConfig.status, 'pending')
  assert.equal(state.weightCalc, undefined)
  assert.equal(state.comprehensiveCalc.status, 'pending')
  assert.equal(state.reportOutput.status, 'pending')
})
