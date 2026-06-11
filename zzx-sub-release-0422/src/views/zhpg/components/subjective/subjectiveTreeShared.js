/**
 * 主观赋权各子组件共用常量 / 帮助函数。
 *
 * 注意：6 个子组件保持与参考项目（原先评估项目的前端 / src/views/components/ahpScore*.vue）
 * 一致的字段命名 —— importance / weightTemp / weightTemp1 / expertWeight / bz / jz / expertList ——
 * 这样后端 SubjectiveWeightServiceImpl 才能按相同字段名取参数。
 */

/** 用于 AHP / 连环比率法的可选比值（1~9 及其倒数）。 */
export const RATIO_OPTIONS = [
  '1', '2', '3', '4', '5', '6', '7', '8', '9',
  '1/9', '1/8', '1/7', '1/6', '1/5', '1/4', '1/3', '1/2'
]

/** 深度优先：找出第一个有 children 的节点，fallback 到第一个根。 */
export function firstParentInTree(nodes) {
  if (!nodes || !nodes.length) return null
  const stack = [...nodes]
  while (stack.length) {
    const n = stack.shift()
    const ch = n && n.children
    if (Array.isArray(ch) && ch.length) return n
    if (Array.isArray(ch)) {
      for (const c of ch) stack.push(c)
    }
  }
  return nodes[0]
}

/** 判断给定数组键之和是否近似等于 1（容差 1e-4）。 */
export function sumIsOne(arr, key) {
  if (!Array.isArray(arr) || !arr.length) return false
  const sum = arr.reduce((s, c) => s + (Number(c?.[key]) || 0), 0)
  return Math.abs(sum - 1) < 1e-4
}

/** 把任意数值数组按正值归一；全为 0 时返回均分。 */
export function normalizeIfPositive(values) {
  const positives = values.map(v => (Number.isFinite(Number(v)) && Number(v) > 0 ? Number(v) : 0))
  const sum = positives.reduce((s, v) => s + v, 0)
  if (sum <= 0) {
    const n = positives.length || 1
    return positives.length ? positives.map(() => 1 / n) : []
  }
  return positives.map(v => v / sum)
}

/** 自顶向下递归刷新父节点的 isOk 标记。 */
export function refreshParentOkFlags(roots, predicate) {
  if (!Array.isArray(roots)) return
  const walk = (list) => {
    for (const n of list) {
      if (n && Array.isArray(n.children) && n.children.length) {
        n.isOk = !!predicate(n)
        walk(n.children)
      }
    }
  }
  walk(roots)
}

/** 主观赋权 6 种算法的下拉选项（value 直接落库到节点 weightAssignAlgorithm 字段）。 */
export const SUBJECTIVE_ALG_OPTIONS = [
  { value: '不校验', label: '重要程度归一法（按重要程度归一）' },
  { value: '相似度法', label: '相似度法（重要程度 + 参考权重）' },
  { value: '校验', label: '校验（多专家直接打分）' },
  { value: '理论证据法', label: '理论证据法（双专家 DST 合成）' },
  { value: '连环比率法', label: '连环比率法（相邻比值）' },
  { value: '层次分析', label: '层次分析法（AHP 互反矩阵 + CR 校验）' }
]

/** 由 value 反查 label。 */
export function getSubjectiveAlgLabel(value) {
  if (!value) return '重要程度归一法'
  const hit = SUBJECTIVE_ALG_OPTIONS.find(o => o.value === value)
  return hit ? hit.label : String(value)
}

/**
 * 通过算法管理表里的"权重分配"算法名，推断它对应的主观赋权 subtype（6 种中文之一）。
 * 返回 null 表示这是客观算法，不需要主观交互。
 *
 * 算法名 → subtype 的映射依据：
 *   - 看 d:/zzxpg/pgzzx/sql/zhpg_algorithm_data_kingbase.sql 里 algorithm_type='权重分配' 的种子数据。
 *   - 主观（需要专家输入参数）：层次分析法 / 基于模糊层次分析与相似度法 / 相对比较法 /
 *                              多维度模糊推理与层次分析 / 基于对数最小二乘的模糊层次分析法 /
 *                              理论证据法 / 连环比率法 / 德尔菲、专家调查法
 *   - 客观（无需专家，靠样本矩阵）：熵值法 / 主成分分析算法 / 因子分析法 / ...
 */
export function inferSubjectiveSubtype(algorithmName) {
  if (!algorithmName) return null
  const name = String(algorithmName).trim()
  if (!name) return null
  // 6 种中文 subtype 直接匹配（用户/旧数据可能直接存中文）
  if (SUBJECTIVE_ALG_OPTIONS.some(o => o.value === name)) return name
  // 算法管理表名 → subtype
  if (/连环比率/.test(name)) return '连环比率法'
  if (/理论证据/.test(name)) return '理论证据法'
  if (/相似度|模糊层次.*相似/.test(name)) return '相似度法'
  if (/德尔菲|Delphi|专家调查|专家.*打分/.test(name)) return '校验'
  if (/对数.*最小二乘.*层次/.test(name)) return '层次分析'
  if (/多维度.*层次/.test(name)) return '理论证据法'
  if (/相对比较/.test(name)) return '不校验'
  if (/层次分析|AHP/i.test(name)) return '层次分析'
  return null
}

/**
 * 把主观赋权算法语义刷到所有父节点的 `weightAssignAlgorithm`。
 * 没有算法管理表 ID 的临时/离线树也能直接按这个字段计算。
 */
export function stampWeightAssignAlgorithmOnParents(roots, algorithm) {
  if (!Array.isArray(roots) || !algorithm) return
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        n.weightAssignAlgorithm = algorithm
        delete n.subtype
        walk(n.children)
      }
    }
  }
  walk(roots)
}

/** 解析 "a/b" 形式比值；返回 NaN 表示无效。 */
export function parseRatio(text) {
  if (text == null || text === '') return NaN
  const s = String(text).trim()
  if (!s) return NaN
  const parts = s.split('/')
  if (parts.length === 1) return Number(parts[0])
  const a = Number(parts[0])
  const b = Number(parts[1])
  if (!Number.isFinite(a) || !Number.isFinite(b) || b === 0) return NaN
  return a / b
}
