export const ZHPG_WORK_MODE = {
  MAIN_BRANCH_COLLAB: '主分协同',
  INTERNAL_CIRCULATION: '内部流转'
}

export const ZHPG_WORK_MODE_OPTIONS = [
  { value: ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB, label: '主分协同' },
  { value: ZHPG_WORK_MODE.INTERNAL_CIRCULATION, label: '内部流转' }
]

export const ZHPG_MAIN_BRANCH_DEFAULT_DATASOURCE_NAME = '主分协同默认数据源'

export function getZhpgWorkModeLabel(val) {
  const item = ZHPG_WORK_MODE_OPTIONS.find(option => option.value === val)
  return item ? item.label : (val || '内部流转')
}

export function normalizeZhpgWorkMode(val, fallback = ZHPG_WORK_MODE.INTERNAL_CIRCULATION) {
  if (val == null || val === '') return fallback
  const raw = String(val).trim()
  if (!raw) return fallback
  if (raw === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB || raw === 'MAIN_BRANCH_COLLAB') {
    return ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB
  }
  if (raw === ZHPG_WORK_MODE.INTERNAL_CIRCULATION || raw === 'INTERNAL_CIRCULATION') {
    return ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  }
  return fallback
}

// ==================== 主分协同：构建阶段（仅两态；库值仍为 INITIAL_DRAFT / REFINED，历史 FINALIZED 视为已回传细化） ====================

export const ZHPG_BUILD_PHASE = {
  INITIAL_DRAFT: 'INITIAL_DRAFT',
  REFINED: 'REFINED'
}

export const ZHPG_BUILD_PHASE_OPTIONS = [
  { value: ZHPG_BUILD_PHASE.INITIAL_DRAFT, label: '初始粗建', type: 'info' },
  { value: ZHPG_BUILD_PHASE.REFINED, label: '已回传细化', type: 'success' }
]

/** 列表查询：与库字段一致；选「初始粗建」时后端会包含 SUBMITTED，选「已回传细化」时包含历史 FINALIZED */
export const ZHPG_BUILD_PHASE_QUERY_OPTIONS = [...ZHPG_BUILD_PHASE_OPTIONS]

export function getZhpgBuildPhaseLabel(val) {
  if (val == null || val === '') return ''
  if (val === 'SUBMITTED' || val === ZHPG_BUILD_PHASE.INITIAL_DRAFT) {
    return '初始粗建'
  }
  if (val === ZHPG_BUILD_PHASE.REFINED || val === 'FINALIZED') {
    return '已回传细化'
  }
  return ''
}

export function getZhpgBuildPhaseTagType(val) {
  if (val === 'SUBMITTED' || val === ZHPG_BUILD_PHASE.INITIAL_DRAFT) {
    return 'info'
  }
  if (val === ZHPG_BUILD_PHASE.REFINED || val === 'FINALIZED') {
    return 'success'
  }
  return 'info'
}

/** 主分协同模式下是否处于初始粗建阶段（底层节点可不配算法） */
export function isMainBranchInitialDraft(workMode, buildPhase) {
  return workMode === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB &&
    (!buildPhase ||
      buildPhase === ZHPG_BUILD_PHASE.INITIAL_DRAFT ||
      buildPhase === 'SUBMITTED')
}

/** 主分协同：已进入已回传细化（含历史定稿库值 FINALIZED） */
export function isMainBranchRefined(workMode, buildPhase) {
  return workMode === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB &&
    (buildPhase === ZHPG_BUILD_PHASE.REFINED || buildPhase === 'FINALIZED')
}

export function createZhpgMainBranchDefaultDataSource() {
  return {
    name: ZHPG_MAIN_BRANCH_DEFAULT_DATASOURCE_NAME,
    directory: '',
    fields: []
  }
}
