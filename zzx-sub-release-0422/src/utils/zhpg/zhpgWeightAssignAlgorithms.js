import { listAllAlgorithm } from '@/api/zhpg/algorithm'

/**
 * 权重分配算法下拉：算法管理中「权重分配」类算法。
 */
export async function fetchZhpgWeightAssignSelectOptions() {
  const res = await listAllAlgorithm({ algorithmType: '权重分配', status: 'ENABLED' })
  const rows = res.data
  const list = Array.isArray(rows) ? rows : []
  return list.map(a => ({
    value: String(a.id),
    label: a.algorithmName
  }))
}
