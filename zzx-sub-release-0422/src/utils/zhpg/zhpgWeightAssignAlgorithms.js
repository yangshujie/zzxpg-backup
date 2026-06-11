import { listAllAlgorithm } from '@/api/zhpg/algorithm'
import { inferSubjectiveSubtype } from '@/views/zhpg/components/subjective/subjectiveTreeShared'

/**
 * 权重分配算法下拉：算法管理中「权重分配」类算法。
 *
 * 返回字段：
 *   value           — 算法管理表主键（字符串 ID）
 *   label           — 算法名称
 *   algorithmName   — 同 label，便于 caller 显式区分
 *   subtype         — 推断出的主观赋权 subtype（中文，6 种之一）；为 null 表示客观算法
 *   isSubjective    — 便利 boolean
 */
export async function fetchZhpgWeightAssignSelectOptions() {
  const res = await listAllAlgorithm({ algorithmType: '权重分配', status: 'ENABLED' })
  const rows = res.data
  const list = Array.isArray(rows) ? rows : []
  return list.map(a => {
    const subtype = inferSubjectiveSubtype(a.algorithmName)
    return {
      value: String(a.id),
      label: a.algorithmName,
      algorithmName: a.algorithmName,
      subtype,
      isSubjective: !!subtype
    }
  })
}

/**
 * 在已加载的 weightAssignOptions 数组中，按 value（算法 ID）找到对应 subtype。
 * 若 raw 直接是 6 种中文之一，直接返回。
 */
export function resolveSubtypeFromOptions(raw, options) {
  if (raw == null || raw === '') return null
  const s = String(raw)
  if (Array.isArray(options)) {
    const hit = options.find(o => String(o.value) === s)
    if (hit && hit.subtype) return hit.subtype
  }
  // 兜底：直接当算法名做模糊推断
  return inferSubjectiveSubtype(s)
}
