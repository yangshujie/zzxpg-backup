/**
 * 指标体系 / 指标体系模板 — 分类常量
 * 装备类型：不少于 6 类（与指标库 zhpg_equipment_type 一致）
 */

export const ZHPG_EQUIPMENT_TYPE_OPTIONS = [
  { value: 'space_recon', label: '航天侦察' },
  { value: 'space_domain_awareness', label: '太空态势感知' },
  { value: 'space_defense', label: '太空攻防' },
  { value: 'space_track_control', label: '航天测运控' },
  { value: 'space_launch', label: '航天发射' },
  { value: 'sea_based_space', label: '海基航天' },
  { value: '无', label: '无' }
]

/** 数据源分中心 source：与装备类型一致，不含“无” */
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
  SPACE_COMBAT: '太空攻防',
  COMM_COUNTER: '航天测运控',
  SEA_BASED: '海基航天',
  NAV_POSITION: '海基航天'
}

const LEGACY_EQUIPMENT_TYPE_VALUES = {
  space_recon: 'space_recon',
  space_domain_awareness: 'space_domain_awareness',
  space_defense: 'space_defense',
  space_track_control: 'space_track_control',
  space_launch: 'space_launch',
  sea_based_space: 'sea_based_space',
  SPACE_RECON: 'space_recon',
  SPACE_SITUATIONAL_AWARENESS: 'space_domain_awareness',
  SPACE_OFFENSE_DEFENSE: 'space_defense',
  SPACE_TTC: 'space_track_control',
  SPACE_LAUNCH: 'space_launch',
  SEA_BASED_SPACE: 'sea_based_space',
  SYSTEM_AGGREGATION: '无',
  SPACE_AWARENESS: 'space_domain_awareness',
  SPACE_AD: 'space_defense',
  SPACE_SA: 'space_domain_awareness',
  SPACE_COMBAT: 'space_defense',
  COMM_COUNTER: 'space_track_control',
  SEA_BASED: 'sea_based_space',
  NAV_POSITION: 'sea_based_space',
  航天侦察: 'space_recon',
  太空态势感知: 'space_domain_awareness',
  太空攻防: 'space_defense',
  航天测运控: 'space_track_control',
  航天发射: 'space_launch',
  海基航天: 'sea_based_space',
  无: '无'
}

export function normalizeZhpgEquipmentType(val, fallback = undefined) {
  if (val == null || val === '') return fallback
  const key = String(val).trim()
  if (!key) return fallback
  return LEGACY_EQUIPMENT_TYPE_VALUES[key] || key
}

export function getZhpgEquipmentTypeLabel(val) {
  if (val == null || val === '') return '—'
  const normalized = normalizeZhpgEquipmentType(val, val)
  const cur = ZHPG_EQUIPMENT_TYPE_OPTIONS.find(i => i.value === normalized)
  return cur ? cur.label : (LEGACY_EQUIPMENT_TYPE_LABELS[val] || normalized)
}
