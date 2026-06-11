// 评估任务构建分系统(RWGJ) 业务枚举 —— 在代码内维护，不写入 sys_dict，避免与 ruoyi-system 耦合

// 评估方法类型（本期 3 类；专项能力/关键因素后续扩展）
export const TEMPLATE_TYPE_OPTIONS = [
  { label: '作战效能评估', value: 'EQUIP_EFFECTIVENESS' },
  { label: '体系贡献率评估', value: 'SYSTEM_CONTRIBUTION' },
  { label: '作战任务满足度评估', value: 'TASK_SATISFACTION' }
]

// 模板分类
export const CLASSIFICATION_OPTIONS = [
  { label: '通用模板', value: 'GENERAL' },
  { label: '专用模板', value: 'SPECIFIC' }
]

// 模板状态
export const TEMPLATE_STATUS_OPTIONS = [
  { label: '已启用', value: 'PUBLISHED' },
  { label: '已停用', value: 'DISABLED' }
]

// 装备类型（6 类，沿用现有编码）
export const EQUIPMENT_TYPE_OPTIONS = [
  { label: '航天侦察', value: 'space_recon' },
  { label: '太空态势感知', value: 'space_domain_awareness' },
  { label: '太空攻防', value: 'space_defense' },
  { label: '航天测运控', value: 'space_track_control' },
  { label: '航天发射', value: 'space_launch' },
  { label: '海基航天', value: 'sea_based_space' }
]

// 评估粒度
export const CALC_GRANULARITY_OPTIONS = [
  { label: '体系级任务评估', value: 'SYSTEM_TASK' },
  { label: '演习演训任务评估', value: 'EXERCISE_TRAINING' },
  { label: '装备效能指标评估', value: 'EQUIP_EFFECTIVENESS' },
  { label: '性能指标评估', value: 'PERFORMANCE' }
]

export function getEnumLabel(options, value) {
  const hit = options.find(o => o.value === value)
  return hit ? hit.label : (value || '')
}

export function templateStatusTagType(status) {
  return status === 'PUBLISHED' ? 'success' : 'info'
}
