export const VARIABLE_PATTERN = /\{\{\s*([a-zA-Z0-9_.]+)\s*}}/g
export const EACH_PATTERN = /\{\{#each\s+([a-zA-Z0-9_]+)\s*}}/g

export const FIXED_REPORT_PLACEHOLDERS = new Set([
  'ExperimentOverview',
  'CapabilityRadarChart',
  'CapabilityScoreTable',
  'OverallConclusionParagraph',
  'IndicatorSections',
  'IndicatorTree',
  'IndicatorTreeJson',
  'ImageUrl',
  'score',
  'grade',
  'KeyFindings',
  'ImprovementSuggestions',
  'FinalConclusion',
])

export const REPORT_PLACEHOLDER_META = {
  ReportTitle: { label: '报告标题', description: '封面或正文首页标题', mappingType: 'STATIC_TEXT', mappingValue: '试验评估报告' },
  SubTitle: { label: '报告副标题', description: '标题下方的补充说明', mappingType: 'STATIC_TEXT', mappingValue: '综合评估结果' },
  SectionOne: { label: '一级章节标题', description: '自由编辑的一级章节标题', mappingType: 'STATIC_TEXT', mappingValue: '' },
  SectionTwo: { label: '二级章节标题', description: '自由编辑的二级章节标题', mappingType: 'STATIC_TEXT', mappingValue: '' },
  SectionThree: { label: '三级章节标题', description: '自由编辑的三级章节标题', mappingType: 'STATIC_TEXT', mappingValue: '' },
  Content: { label: '正文内容', description: '普通文本段落', mappingType: 'STATIC_TEXT', mappingValue: '' },
  QuoteText: { label: '引用内容', description: '重点说明或结论摘录', mappingType: 'STATIC_TEXT', mappingValue: '' },
  MetricName: { label: '指标名称', description: '表格中的指标名称列', mappingType: 'STATIC_TEXT', mappingValue: '' },
  MetricValue: { label: '指标值', description: '表格中的指标值列', mappingType: 'STATIC_TEXT', mappingValue: '' },
  ImageUrl: { label: '图片地址', description: '自定义图片资源，支持本地上传或输入 URL 地址', mappingType: 'STATIC_TEXT', mappingValue: '' },
  ExperimentOverview: { label: '试验概况', description: '任务名称、对象、指标体系与时间等概览', mappingType: 'AUTO_INDICATOR', mappingValue: 'experiment_overview' },
  CapabilityRadarChart: { label: '能力雷达图', description: '综合能力雷达图，由报告生成阶段自动填充', mappingType: 'AUTO_INDICATOR', mappingValue: 'capability_radar_chart' },
  CapabilityScoreTable: { label: '指标汇总表', description: '指标节点、权重、得分与等级汇总', mappingType: 'AUTO_INDICATOR', mappingValue: 'indicator_summary_table' },
  OverallConclusionParagraph: { label: '综合评估段落', description: '根据综合得分和等级生成的总评段落', mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_conclusion' },
  IndicatorSections: { label: '指标章节循环', description: '按指标树递归生成章节、评价表和图表', mappingType: 'AUTO_INDICATOR', mappingValue: 'indicator_sections' },
  KeyFindings: { label: '关键发现', description: '从低分项和关键指标中提炼的发现', mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_conclusion' },
  ImprovementSuggestions: { label: '改进建议', description: '面向短板指标生成的建议', mappingType: 'AUTO_INDICATOR', mappingValue: 'improvement_suggestions' },
  FinalConclusion: { label: '最终结论', description: '报告末尾的综合结论', mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_conclusion' },
  score: { label: '综合得分', description: '本次评估综合得分', mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_score' },
  OverallScore: { label: '综合得分', description: '本次评估综合得分', mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_score' },
  grade: { label: '评估等级', description: '本次评估等级', mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_grade' },
  conclusion: { label: '评估结论', description: '本次评估结论', mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_conclusion' },
  IndicatorTable: { label: '指标汇总表', description: '指标节点、权重、得分与等级汇总', mappingType: 'AUTO_INDICATOR', mappingValue: 'indicator_summary_table' },
  IndicatorTree: { label: '指标树', description: '完整指标树结构', mappingType: 'AUTO_INDICATOR', mappingValue: 'indicator_tree' },
  IndicatorTreeJson: { label: '指标树', description: '完整指标树结构', mappingType: 'AUTO_INDICATOR', mappingValue: 'indicator_tree' },
  taskName: { label: '任务名称', description: '当前评估任务名称', mappingType: 'TASK_PROPERTY', mappingValue: 'taskName' },
  ExperimentName: { label: '试验名称', description: '当前试验或任务名称', mappingType: 'TASK_PROPERTY', mappingValue: 'taskName' },
  ExperimentTime: { label: '试验时间', description: '任务开始时间', mappingType: 'TASK_PROPERTY', mappingValue: 'startTime' },
  startTime: { label: '任务开始时间', description: '任务开始时间', mappingType: 'TASK_PROPERTY', mappingValue: 'startTime' },
  EvalTarget: { label: '评估对象', description: '当前评估对象', mappingType: 'TASK_PROPERTY', mappingValue: 'evaluateTarget' },
}

export const AUTO_INDICATOR_OPTION_GROUPS = [
  {
    label: '文本结果',
    options: [
      { label: '综合得分', value: 'overall_score', usage: 'text' },
      { label: '评估等级', value: 'overall_grade', usage: 'text' },
      { label: '评估结论', value: 'overall_conclusion', usage: 'text' },
      { label: '改进建议', value: 'improvement_suggestions', usage: 'text' },
      { label: '试验概况', value: 'experiment_overview', usage: 'text' },
    ],
  },
  {
    label: '表格',
    options: [
      { label: '指标汇总表', value: 'indicator_summary_table', usage: 'table' },
      { label: '指标树', value: 'indicator_tree', usage: 'table' },
    ],
  },
  {
    label: '图片',
    options: [
      { label: '能力雷达图', value: 'capability_radar_chart', usage: 'image' },
    ],
  },
  {
    label: '章节循环',
    options: [
      { label: '指标章节循环', value: 'indicator_sections', usage: 'section' },
    ],
  },
]

export function getAutoIndicatorOptionGroups(key) {
  const usage = getPlaceholderUsage(key)
  return AUTO_INDICATOR_OPTION_GROUPS
    .map(group => ({
      ...group,
      options: group.options.filter(option => !usage || option.usage === usage),
    }))
    .filter(group => group.options.length)
}

export const TASK_PROPERTY_OPTIONS = [
  { label: '任务名称', value: 'taskName' },
  { label: '评估对象', value: 'evaluateTarget' },
  { label: '任务开始时间', value: 'startTime' },
  { label: '指标体系名称', value: 'indicatorSystemName' },
]

export const LEGACY_AUTO_TO_SIMPLIFIED = {
  suggestion: 'improvement_suggestions',
  capability_score_table: 'indicator_summary_table',
  overall_conclusion_paragraph: 'overall_conclusion',
  key_findings: 'overall_conclusion',
  final_conclusion: 'overall_conclusion',
  indicator_nodes_flat: 'indicator_tree',
  indicator_hierarchy: 'indicator_tree',
  dimension_nodes: 'indicator_summary_table',
  all_leaf_nodes: 'indicator_summary_table',
  leaf_evaluation_details: 'indicator_summary_table',
  leaf_nodes_scores: 'indicator_summary_table',
  intermediate_nodes_scores: 'indicator_summary_table',
  node_count_stats: 'indicator_summary_table',
  eval_result_snapshot: 'overall_conclusion',
}

export function getPlaceholderMeta(key) {
  return REPORT_PLACEHOLDER_META[key] || {
    label: key,
    description: '自定义占位符',
    mappingType: 'STATIC_TEXT',
    mappingValue: '',
  }
}

export function extractPlaceholdersFromHtml(html) {
  const source = html || ''
  const set = new Set()
  const ordered = []
  const pattern = /\{\{#each\s+([a-zA-Z0-9_]+)\s*}}|\{\{\s*([a-zA-Z0-9_.]+)\s*}}/g
  let matched
  while ((matched = pattern.exec(source)) !== null) {
    const key = matched[1] || matched[2]
    if (!key || key.startsWith('item.') || set.has(key)) continue
    set.add(key)
    ordered.push(key)
  }
  return ordered
}

export function inferReportMapping(key) {
  const meta = getPlaceholderMeta(key)
  if (meta.mappingType !== 'STATIC_TEXT' || meta.mappingValue) {
    return { type: meta.mappingType, value: meta.mappingValue }
  }

  const keyLower = String(key || '').toLowerCase()
  if (keyLower.includes('score') || keyLower.includes('得分')) return { type: 'AUTO_INDICATOR', value: 'overall_score' }
  if (keyLower.includes('grade') || keyLower.includes('等级') || keyLower.includes('评级')) return { type: 'AUTO_INDICATOR', value: 'overall_grade' }
  if (keyLower.includes('suggestion') || keyLower.includes('advice') || keyLower.includes('建议')) return { type: 'AUTO_INDICATOR', value: 'improvement_suggestions' }
  if (keyLower.includes('conclusion') || keyLower.includes('结论')) return { type: 'AUTO_INDICATOR', value: 'overall_conclusion' }
  if (keyLower.includes('summary') || keyLower.includes('table') || keyLower.includes('汇总') || keyLower.includes('表格') || keyLower.includes('明细')) {
    return { type: 'AUTO_INDICATOR', value: 'indicator_summary_table' }
  }
  if (keyLower.includes('tree') || keyLower.includes('指标树')) return { type: 'AUTO_INDICATOR', value: 'indicator_tree' }
  if (keyLower.includes('task') || keyLower.includes('experiment') || keyLower.includes('任务') || keyLower.includes('试验')) {
    return { type: 'TASK_PROPERTY', value: 'taskName' }
  }
  return { type: 'STATIC_TEXT', value: '' }
}

export function getPlaceholderUsage(key) {
  const meta = getPlaceholderMeta(key)
  const value = meta.mappingValue || ''
  const normalizedKey = String(key || '').toLowerCase()
  if (value === 'capability_radar_chart' || key === 'ImageUrl' || normalizedKey.includes('image') || normalizedKey.includes('chart')) return 'image'
  if (value === 'indicator_summary_table' || value === 'indicator_tree' || normalizedKey.includes('table')) return 'table'
  if (value === 'indicator_sections') return 'section'
  if (meta.mappingType === 'AUTO_INDICATOR') return 'text'
  return ''
}

export function getStaticTextPlaceholder(key) {
  if (key === 'ImageUrl') return '请输入图片 URL，如 /profile/upload/demo.png 或 http(s)://...'
  return '请输入静态文本'
}

export function normalizeReportMappings(rows = []) {
  if (!Array.isArray(rows)) return []
  return rows.map((row) => {
    const next = { ...row }
    if (next.mappingType === 'INDICATOR_RESULT') {
      next.mappingType = 'AUTO_INDICATOR'
      next.mappingValue = 'indicator_summary_table'
    }
    if (next.mappingType === 'AUTO_INDICATOR' && next.mappingValue && LEGACY_AUTO_TO_SIMPLIFIED[next.mappingValue]) {
      next.mappingValue = LEGACY_AUTO_TO_SIMPLIFIED[next.mappingValue]
    }
    const meta = getPlaceholderMeta(next.key)
    const autoGroups = getAutoIndicatorOptionGroups(next.key)
    const autoValues = autoGroups.flatMap(group => group.options.map(option => option.value))
    if (next.mappingType === 'AUTO_INDICATOR' && autoValues.length && !autoValues.includes(next.mappingValue)) {
      next.mappingValue = meta.mappingType === 'AUTO_INDICATOR' && autoValues.includes(meta.mappingValue)
        ? meta.mappingValue
        : autoValues[0]
    }
    next.label = next.label || meta.label
    next.description = next.description || meta.description
    return next
  })
}

export function createReportMapping(key, existingRows = []) {
  const existed = existingRows.find(item => item.key === key)
  if (existed && (existed.mappingValue || existed.mappingType !== 'STATIC_TEXT')) {
    return normalizeReportMappings([existed])[0]
  }
  const meta = getPlaceholderMeta(key)
  const inferred = inferReportMapping(key)
  return normalizeReportMappings([{
    key,
    label: meta.label,
    description: meta.description,
    mappingType: inferred.type,
    mappingValue: inferred.value,
  }])[0]
}
