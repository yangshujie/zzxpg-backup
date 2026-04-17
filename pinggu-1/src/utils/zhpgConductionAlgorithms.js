import { listAllAlgorithm } from '@/api/zhpg/algorithm'
import { ZHPG_CONDUCTION_METHOD_OPTIONS } from '@/constants/zhpgIndicatorSystemAlgorithms'

/**
 * 传导方法下拉：算法管理中「聚合传导」类算法。
 */
export async function fetchZhpgConductionMethodSelectOptions() {
  const res = await listAllAlgorithm({ algorithmType: '聚合传导', status: 'ENABLED' })
  const rows = res.data
  const list = Array.isArray(rows) ? rows : []
  return list.map(a => ({
    value: a.algorithmName,
    label: a.algorithmName
  }))
}
