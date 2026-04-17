/**
 * 指标体系 / 指标体系模板 — 分类常量
 * 体系类型：一能三性（装备作战效能、作战适用性、体系适用性、在役适用性）
 * 装备类型：不少于 6 类（与指标库 zhpg_equipment_type 一致）
 */

export const ZHPG_SYSTEM_TYPE_OPTIONS = [
  { value: '装备作战效能', label: '装备作战效能' },
  { value: '作战适用性', label: '作战适用性' },
  { value: '体系适用性', label: '体系适用性' },
  { value: '在役适用性', label: '在役适用性' }
]

/** 历史落库编码 → 展示名（查询与列表兼容旧数据） */
const LEGACY_SYSTEM_TYPE_LABELS = {
  EQUIPMENT_COMBAT_EFFECTIVENESS: '装备作战效能',
  COMBAT_APPLICABILITY: '作战适用性',
  SYSTEM_APPLICABILITY: '体系适用性',
  SERVICE_APPLICABILITY: '在役适用性',
  COMBAT_EFFECTIVENESS: '装备作战效能',
  SYSTEM_CONTRIBUTION: '体系适用性',
  CAPABILITY_MATURITY: '在役适用性'
}

export function getZhpgSystemTypeLabel(val) {
  if (val == null || val === '') return '—'
  const cur = ZHPG_SYSTEM_TYPE_OPTIONS.find(i => i.value === val)
  if (cur) return cur.label
  return LEGACY_SYSTEM_TYPE_LABELS[val] || val
}

export const ZHPG_EQUIPMENT_TYPE_OPTIONS = [
  { value: 'space_recon', label: '航天侦察' },
  { value: 'space_domain_awareness', label: '太空态势感知' },
  { value: 'space_defense', label: '太空攻防' },
  { value: 'space_track_control', label: '航天测运控' },
  { value: 'space_launch', label: '航天发射' },
  { value: 'sea_based_space', label: '海基航天' },
  { value: '无', label: '无' }
]

/** 数据源分中心 source：与装备类型一致，不含「无」 */
export const ZHPG_DATA_SOURCE_CENTER_OPTIONS = ZHPG_EQUIPMENT_TYPE_OPTIONS.filter(o => o.value !== '无')

const LEGACY_EQUIPMENT_TYPE_LABELS = {
  space_recon: '航天侦察',
  space_domain_awareness: '太空态势感知',
  space_defense: '太空攻防',
  space_track_control: '航天测运控',
  space_launch: '航天发射',
  sea_based_space: '海基航天',
  SPACE_RECON: '航天侦察',
  SPACE_SITUATIONAL_AWARENESS: '太空态势感知',
  SPACE_OFFENSE_DEFENSE: '太空攻防',
  SPACE_TTC: '航天测运控',
  SPACE_LAUNCH: '航天发射',
  SEA_BASED_SPACE: '海基航天',
  SYSTEM_AGGREGATION: '无',
  SPACE_AWARENESS: '太空态势感知',
  SPACE_AD: '太空攻防',
  SPACE_SA: '太空态势感知',
  SEA_BASED: '海基航天'
}

export function getZhpgEquipmentTypeLabel(val) {
  if (val == null || val === '') return '—'
  const cur = ZHPG_EQUIPMENT_TYPE_OPTIONS.find(i => i.value === val)
  return cur ? cur.label : (LEGACY_EQUIPMENT_TYPE_LABELS[val] || val)
}
