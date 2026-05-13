export const RESULT_ANALYSIS_ENTRY_PATH = '/major/program-mgmt/eval-result'
export const FLOW_RUNNER_PATH = '/process/reception-sys/flowRunner'
export const EMPTY_RESULT_MESSAGE = '当前需求暂无可查看的评估结果，请完成评估流程后再查看'
export const INCOMPLETE_RESULT_CONTEXT_MESSAGE = '评估结果上下文不完整，无法打开结果分析'

export function isResultAnalysisPath(path) {
  return path === RESULT_ANALYSIS_ENTRY_PATH
}

function toNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function pickRequirementId(item) {
  if (item == null) return null
  if (typeof item === 'object') {
    return toNumber(item.requirementId ?? item.id)
  }
  return toNumber(item)
}

export function normalizeRequirementIds(response) {
  const source = response?.data ?? response?.rows ?? response
  if (!Array.isArray(source)) return []
  return source
    .map(pickRequirementId)
    .filter(id => id != null)
}

export function buildResultAnalysisRoute({ projectId, requirementId, templateId, evalResultId }) {
  const query = {
    projectId,
    requirementId,
    templateId,
    evalResultId,
    entry: 'resultAnalysis'
  }

  Object.keys(query).forEach(key => {
    if (query[key] == null || query[key] === '') delete query[key]
  })

  return {
    path: FLOW_RUNNER_PATH,
    query
  }
}

export function resolveFormalResultDecision({ projectId, requirementId, context }) {
  if (!context?.hasResult || !context?.evalResultId) {
    return {
      type: 'empty',
      message: EMPTY_RESULT_MESSAGE
    }
  }

  if (!context.templateId) {
    return {
      type: 'invalid',
      message: INCOMPLETE_RESULT_CONTEXT_MESSAGE
    }
  }

  return {
    type: 'route',
    route: buildResultAnalysisRoute({
      projectId,
      requirementId,
      templateId: context.templateId,
      evalResultId: context.evalResultId
    })
  }
}
