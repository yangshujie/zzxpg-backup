import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildResultAnalysisRoute,
  isResultAnalysisPath,
  normalizeRequirementIds,
  resolveFormalResultDecision
} from '../src/utils/zhpg/projectResultAnalysisEntry.js'

test('normalizeRequirementIds accepts common backend response shapes', () => {
  assert.deepEqual(normalizeRequirementIds({ data: [4, 10, 11] }), [4, 10, 11])
  assert.deepEqual(normalizeRequirementIds({ rows: [{ requirementId: 5 }, { id: 6 }] }), [5, 6])
  assert.deepEqual(normalizeRequirementIds(['7', null, undefined, 8]), [7, 8])
})

test('isResultAnalysisPath detects only the project result analysis entry', () => {
  assert.equal(isResultAnalysisPath('/major/program-mgmt/eval-result'), true)
  assert.equal(isResultAnalysisPath('/major/capability-resource/report'), false)
  assert.equal(isResultAnalysisPath('/process/reception-sys/flowRunner'), false)
})

test('resolveFormalResultDecision routes only formal results with template ids', () => {
  const decision = resolveFormalResultDecision({
    projectId: 101,
    requirementId: 4,
    context: {
      hasResult: true,
      evalResultId: 456,
      templateId: 8
    }
  })

  assert.equal(decision.type, 'route')
  assert.deepEqual(decision.route, {
    path: '/process/reception-sys/flowRunner',
    query: {
      projectId: 101,
      requirementId: 4,
      templateId: 8,
      evalResultId: 456,
      entry: 'resultAnalysis'
    }
  })
})

test('resolveFormalResultDecision blocks successful tasks without formal result ids', () => {
  const decision = resolveFormalResultDecision({
    projectId: 101,
    requirementId: 4,
    context: {
      hasResult: false,
      taskId: 123,
      templateId: 8
    }
  })

  assert.equal(decision.type, 'empty')
  assert.equal(decision.message, '当前需求暂无可查看的评估结果，请完成评估流程后再查看')
})

test('buildResultAnalysisRoute omits empty optional query values', () => {
  assert.deepEqual(buildResultAnalysisRoute({
    projectId: 101,
    requirementId: 4,
    templateId: 8,
    evalResultId: 456
  }), {
    path: '/process/reception-sys/flowRunner',
    query: {
      projectId: 101,
      requirementId: 4,
      templateId: 8,
      evalResultId: 456,
      entry: 'resultAnalysis'
    }
  })
})
