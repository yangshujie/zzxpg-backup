/**
 * 指标值类型（成本型 / 效益型 / 区间效益型），与装备领域「指标类型」字典区分
 */
export const ZHPG_VALUE_CATEGORY_OPTIONS = [
  { value: '成本型', label: '成本型' },
  { value: '效益型', label: '效益型' },
  { value: '区间效益型', label: '区间效益型' }
]

export function getZhpgValueCategoryLabel(val) {
  if (val == null || val === '') return '—'
  const item = ZHPG_VALUE_CATEGORY_OPTIONS.find(o => o.value === val)
  if (item) return item.label
  const legacy = {
    COST: '成本型',
    BENEFIT: '效益型',
    INTERVAL_BENEFIT: '区间效益型'
  }
  return legacy[val] || String(val)
}
