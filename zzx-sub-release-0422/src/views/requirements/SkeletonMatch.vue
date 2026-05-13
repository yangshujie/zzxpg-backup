<template>
  <div class="skeleton-match-container">
    <!-- Top Tactical Bar -->
    <header class="tactical-header">
      <div class="header-left">
        <el-icon class="breadcrumb-icon"><Aim /></el-icon>
        <span class="system-title">ORBITAL CURATOR</span>
        <span class="divider"></span>
        <span class="page-title">任务骨架智能匹配结果</span>
      </div>
      
      <div class="workflow-stepper">
        <div class="step done">01 导入一案三纲</div>
        <div class="step active">02 匹配任务骨架</div>
        <div class="step">03 需求自动生成</div>
        <div class="step">04 完备性检查</div>
      </div>

      <div class="header-actions">
        <el-button class="ghost-btn" @click="reMatch">
          <el-icon><Refresh /></el-icon> 重新匹配
        </el-button>
        <el-button class="primary-gradient-btn" @click="confirmAndProceed">
          确认选定，生成需求 <el-icon><Right /></el-icon>
        </el-button>
      </div>
    </header>

    <main class="tactical-body">
      <!-- Left Sidebar: Recommendation List -->
      <aside class="recommend-sidebar">
        <div class="sidebar-head">
          <h3><el-icon><List /></el-icon> 智能推荐列表</h3>
          <p>基于知识图谱比对，按综合相关度排序</p>
        </div>
        
        <div class="skeleton-scroll">
          <div 
            v-for="(item, index) in skeletonList" 
            :key="item.id" 
            class="skeleton-card"
            :class="{ active: selectedId === item.id }"
            @click="selectedId = item.id"
          >
            <div class="rank-tag" :class="`rank-${index + 1}`">TOP {{ index + 1 }}</div>
            <div class="card-title">{{ item.name }}</div>
            <div class="tech-tags">
              <span v-for="tag in item.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            
            <div class="metric-group">
              <div class="metric-row">
                <span class="label">综合评分</span>
                <div class="progress-track">
                  <div class="progress-fill" :style="{ width: item.matchProgress + '%', background: getMatchColor(item.matchProgress) }"></div>
                </div>
                <span class="value" :style="{ color: getMatchColor(item.matchProgress) }">{{ item.matchProgress }}%</span>
              </div>
              <div class="metric-row">
                <span class="label">指标覆盖</span>
                <div class="progress-track">
                  <div class="progress-fill" :style="{ width: item.coverage + '%', background: '#00ff9d' }"></div>
                </div>
                <span class="value success">{{ item.coverage }}%</span>
              </div>
            </div>

            <div v-if="selectedId === item.id" class="selection-status">
              <el-icon><Select /></el-icon> 当前已选定
            </div>
          </div>
        </div>

        <div class="sidebar-foot">
          <button class="custom-build-btn" @click="goToManualBuild">
            <el-icon><Plus /></el-icon> 启用骨架构建器
          </button>
          <p class="hint">未找到匹配项？点击从零构建新骨架</p>
        </div>
      </aside>

      <!-- Main Viewport: Skeleton Details -->
      <section class="details-viewport" v-if="selectedData">
        <div class="viewport-header">
          <div class="title-wrap">
            <h2>{{ selectedData.name }}</h2>
            <div class="id-badge">ID: {{ selectedId }} · VER: v1.0.0</div>
          </div>
          <div class="status-badge">
            <span class="dot"></span> 匹配引擎已就绪
          </div>
        </div>

        <div class="scroll-container">
          <!-- Basic Info Section (Hierarchy Low) -->
          <div class="info-section">
            <div class="section-label">01 // 任务上下文概要</div>
            <div class="data-grid">
              <div class="data-item">
                <span class="label">适用领域</span>
                <span class="value bright">电子对抗 · 通信对抗</span>
              </div>
              <div class="data-item">
                <span class="label">装备体系</span>
                <span class="value">有源干扰机、侦察干扰一体化装备</span>
              </div>
              <div class="data-item">
                <span class="label">评估层级</span>
                <span class="value"><span class="pill">系统级</span><span class="pill">设备级</span></span>
              </div>
              <div class="data-item">
                <span class="label">任务类型</span>
                <span class="value"><span class="pill outline">效能评估</span><span class="pill outline">对比评估</span></span>
              </div>
              <div class="data-item full">
                <span class="label">核心评估目的</span>
                <span class="value">干扰效能、侦察能力及响应时序综合效能鉴定评估</span>
              </div>
            </div>
          </div>

          <!-- Indicator Tree Section -->
          <div class="tree-section">
            <div class="section-label">02 // 评估指标体系谱系 (TELEM-DYNAMICS)</div>
            
            <div class="tree-table-wrapper">
              <!-- Table Legend -->
              <div class="tree-legend">
                <span class="legend-title">NODES:</span>
                <span class="leg-item root">根节点</span>
                <span class="leg-item dim">维度节点</span>
                <span class="leg-item gen">泛化指标</span>
                <span class="leg-item spec">具体指标</span>
                <span class="spacer"></span>
                <span class="legend-title">OPERATORS:</span>
                <span class="leg-item calc">计算算子</span>
                <span class="leg-item norm">归一化</span>
              </div>

              <!-- Functional Header -->
              <div class="tree-header">
                <div class="col-name">节点标识与层级</div>
                <div class="col-type">类型描述</div>
                <div class="col-ops">算子配置</div>
                <div class="col-cond">激活阈值与条件</div>
              </div>

              <div class="tree-rows">
                <div 
                  v-for="node in flattenedTree" 
                  :key="node.id" 
                  class="t-row"
                  :class="[node.type, { expanded: node.expanded }]"
                  :style="{ paddingLeft: node.level * 24 + 'px' }"
                >
                  <div class="col-name">
                    <div class="indent-guides" v-for="i in node.level" :key="i"></div>
                    <el-icon 
                      v-if="node.hasChildren" 
                      class="toggle-icon" 
                      @click="toggleNode(node.id)"
                    >
                      <CaretRight v-if="!node.expanded" />
                      <CaretBottom v-else />
                    </el-icon>
                    <span v-else class="node-dot"></span>
                    <span class="node-label">{{ node.label }}</span>
                  </div>

                  <div class="col-type">
                    <span class="type-badge" :class="node.type">{{ node.typeText }}</span>
                  </div>

                  <div class="col-ops">
                    <template v-if="node.ops">
                      <div class="op-button-group">
                        <button class="tactical-op-btn calc" @click="showModal('calc', node)">
                          <span class="btn-tag">CALC</span> {{ node.ops.calc }}
                        </button>
                        <button class="tactical-op-btn norm" @click="showModal('norm', node)">
                          <span class="btn-tag">NORM</span> {{ node.ops.norm }}
                        </button>
                      </div>
                    </template>
                    <template v-else-if="node.aggregation">
                      <span class="agg-badge">{{ node.aggregation }}</span>
                    </template>
                  </div>

                  <div class="col-cond">
                    <button class="cond-btn" @click="showModal('cond', node)">
                      {{ node.condText || '查看激活逻辑' }} <el-icon><InfoFilled /></el-icon>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Evaluation Criteria Card -->
          <div class="criteria-section">
             <div class="section-label">03 // 关键评估准则 (CRITERIA-SET)</div>
             <div class="tactical-table-card">
               <table class="tactical-table">
                 <thead>
                   <tr>
                     <th>维度分类</th>
                     <th><span class="status-pill pass">合格 (NOMINAL)</span></th>
                     <th><span class="status-pill warn">基本合格 (THRESHOLD)</span></th>
                     <th><span class="status-pill fail">不合格 (CRITICAL)</span></th>
                   </tr>
                 </thead>
                 <tbody>
                   <tr v-for="row in selectedData.criteria" :key="row.name">
                     <td class="row-head">{{ row.name }}</td>
                     <td>{{ row.pass }} <div class="score-hint">≥ 85.00</div></td>
                     <td>{{ row.threshold }} <div class="score-hint">70.0 - 84.9</div></td>
                     <td>{{ row.fail }} <div class="score-hint">&lt; 70.00</div></td>
                   </tr>
                 </tbody>
               </table>
             </div>
          </div>

          <!-- Algorithms Section -->
          <div class="algo-section">
            <div class="section-label">04 // 预置算法引擎 (ALGO-ENGINE)</div>
            <div class="algo-grid">
              <div class="algo-tactical-card">
                <div class="algo-head">全局汇聚算法</div>
                <div class="algo-body">
                  <div class="algo-row"><span class="k">算法标识</span><span class="v code">HierarchicalAggregator</span></div>
                  <div class="algo-row"><span class="k">汇聚策略</span><span class="v">逐层加权汇聚，具体指标→泛化指标→维度→根节点</span></div>
                  <div class="algo-row"><span class="k">缺失处理</span><span class="v">缺失节点权重重分配至同级兄弟节点</span></div>
                </div>
              </div>
              <div class="algo-tactical-card">
                <div class="algo-head">性能加权算法</div>
                <div class="algo-body">
                  <div class="algo-row"><span class="k">算法标识</span><span class="v code">WeightedAvgAHP</span></div>
                  <div class="algo-row"><span class="k">权重来源</span><span class="v">AHP 层次分析法，结合领域专家意见</span></div>
                  <div class="algo-row"><span class="k">计算公式</span><span class="v code">S = Σ(wᵢ * sᵢ) / Σ(wᵢ)</span></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- Modal Overlay (Glassmorphism) -->
    <transition name="fade">
      <div class="tactical-overlay" v-if="activeModal" @click.self="activeModal = null">
        <div class="tactical-modal">
          <div class="modal-corner tl"></div>
          <div class="modal-corner tr"></div>
          <div class="modal-corner bl"></div>
          <div class="modal-corner br"></div>
          
          <div class="modal-head">
            <div class="head-left">
              <span class="modal-category">{{ modalTitlePrefix }}</span>
              <h3>{{ activeModal.nodeLabel }}</h3>
            </div>
            <button class="close-btn" @click="activeModal = null"><el-icon><Close /></el-icon></button>
          </div>

          <div class="modal-body">
            <div v-if="activeModal.type === 'cond'" class="modal-content">
              <div class="content-block">
                <label>规则说明</label>
                <p class="description">{{ activeModal.data.desc }}</p>
              </div>
              <div class="content-block">
                <label>激活约束序列</label>
                <div class="cond-list" v-if="activeModal.data.conds && activeModal.data.conds.length">
                  <div class="cond-item" v-for="(c, idx) in activeModal.data.conds" :key="idx">
                    <span class="index">C{{ idx + 1 }}</span>
                    <span class="field">{{ c.dim }}</span>
                    <span class="operator">{{ c.op }}</span>
                    <span class="target">{{ c.val }}</span>
                  </div>
                </div>
                <div class="cond-always" v-else>
                  <el-icon><Check /></el-icon> 无条件约束：父节点激活时此节点始终处于就绪状态
                </div>
              </div>
            </div>

            <div v-else-if="activeModal.type === 'calc'" class="modal-content">
               <div class="data-row">
                 <span class="m-lbl">计算算子标识</span>
                 <span class="m-val highlight">HierarchicalRatioCalculator</span>
               </div>
               <div class="data-row">
                 <span class="m-lbl">核心公式</span>
                 <span class="m-val code">Val = Σ(metric_i * wt_i) / Σ(wt_i)</span>
               </div>
               <div class="data-row">
                 <span class="m-lbl">数据词典映射</span>
                 <div class="mapping-list">
                    <div class="mapping-item">
                       <span class="source">jam_bandwidth</span>
                       <el-icon><Right /></el-icon>
                       <span class="target">TELEMETRY_X14_VAL</span>
                    </div>
                 </div>
               </div>
            </div>
            
            <div v-else-if="activeModal.type === 'norm'" class="modal-content">
               <div class="data-row">
                 <span class="m-lbl">归一化策略</span>
                 <span class="m-val highlight">S-Curve Positive Mapping</span>
               </div>
               <div class="data-row">
                 <span class="m-lbl">满分区间</span>
                 <span class="m-val code">[80.0%, 100.0%]</span>
               </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Aim, Refresh, Right, List, Select, Plus, 
  CaretRight, CaretBottom, InfoFilled, Close, Check
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

// ─── Design Tokens (From DESIGN.md) ───
// Surface: #10131a
// Container Low: #191c22
// Container High: #272a31
// Container Highest: #32353c
// Primary: #a4e6ff
// Primary Container: #00d1ff

const selectedId = ref('s1')
const activeModal = ref(null)

// ─── Mock Data ───
const skeletonList = ref([
  {
    id: 's1',
    name: '通信对抗装备综合效能评估骨架',
    tags: ['通信对抗', '效能评估', 'v1.0.0'],
    matchProgress: 94,
    coverage: 91,
    historySuccess: 88,
    criteria: [
      { name: '干扰效能', pass: '覆盖率≥80%, J/S≥门限', threshold: '覆盖率≥65%, J/S接阈', fail: '覆盖率<65%, J/S失准' },
      { name: '侦察能力', pass: '截获率≥85%, RMSE≤2°', threshold: '截获率≥70%, RMSE≤5°', fail: '截获率<70%, RMSE>5°' },
      { name: '响应时序', pass: '端到端≤3s, 响应≤0.5s', threshold: '端到端≤5s, 响应≤1s', fail: '端到端>5s, 响应>1s' }
    ]
  },
  {
    id: 's2',
    name: '通信干扰设备性能专项评估骨架',
    tags: ['通信对抗', '性能测试'],
    matchProgress: 79,
    coverage: 73,
    historySuccess: 82,
    criteria: []
  },
  {
    id: 's3',
    name: '侦察干扰一体化装备专项骨架',
    tags: ['空间对抗', '专项评估'],
    matchProgress: 65,
    coverage: 58,
    historySuccess: 76,
    criteria: []
  }
])

const selectedData = computed(() => skeletonList.value.find(s => s.id === selectedId.value))

// Tree Data Simulation
const indicatorTreeData = [
  {
    id: 'root',
    label: '通信对抗综合效能',
    type: 'root',
    typeText: '评估总纲',
    aggregation: '全局汇聚 · AHP权重',
    expanded: true,
    children: [
      {
        id: 'dim1',
        label: '干扰效能',
        type: 'dim',
        typeText: '核心维度',
        aggregation: '加权平均 · 等权重',
        expanded: true,
        children: [
          {
            id: 'gen11',
            label: '干扰覆盖性',
            type: 'gen',
            typeText: '泛化指标',
            expanded: true,
            children: [
              {
                id: 'spec111',
                label: '压制干扰覆盖率',
                type: 'spec',
                typeText: '量化指标',
                ops: { calc: '比值计算算子', norm: '线性归一化' },
                condText: '场景: 覆盖干扰'
              },
              {
                 id: 'spec112',
                 label: '频段覆盖完整度',
                 type: 'spec',
                 typeText: '量化指标',
                 ops: { calc: '分段计值算子', norm: '线性归一化' },
                 condText: '场景: 多频段'
              }
            ]
          }
        ]
      },
      {
        id: 'dim2',
        label: '侦察能力',
        type: 'dim',
        typeText: '核心维度',
        aggregation: '均值聚合',
        expanded: false,
        children: []
      }
    ]
  }
]

const openNodes = ref(new Set(['root', 'dim1', 'gen11']))

const flattenedTree = computed(() => {
  const result = []
  const walk = (nodes, level = 0) => {
    nodes.forEach(node => {
      result.push({
        ...node,
        level,
        hasChildren: node.children && node.children.length > 0,
        expanded: openNodes.value.has(node.id)
      })
      if (node.children && openNodes.value.has(node.id)) {
        walk(node.children, level + 1)
      }
    })
  }
  walk(indicatorTreeData)
  return result
})

const toggleNode = (id) => {
  if (openNodes.value.has(id)) {
    openNodes.value.delete(id)
  } else {
    openNodes.value.add(id)
  }
}

const getMatchColor = (p) => {
  if (p >= 90) return '#00f2ff'
  if (p >= 75) return '#00ff9d'
  return '#ffbd2e'
}

const modalTitlePrefix = computed(() => {
  if (!activeModal.value) return ''
  const sub = { cond: '激活规则', calc: '计算算子详情', norm: '归一化规则' }
  return sub[activeModal.value.type]
})

const showModal = (type, node) => {
  const mockDetails = {
    cond: {
      desc: '当前任务上下文定义了特定测试科目时激活。若解析到一案三纲中包含“宽带压制”关键字，此指标自动权重上调。',
      conds: [{ dim: '评估内容', op: '包含', val: '干扰效能测试' }]
    },
    calc: {},
    norm: {}
  }
  activeModal.value = {
    type,
    nodeId: node.id,
    nodeLabel: node.label,
    data: mockDetails[type]
  }
}

const reMatch = () => {
  ElMessage.success('匹配引擎已重启，正在重新计算知识图谱相似度...')
}

const confirmAndProceed = () => {
  ElMessage({
    message: '骨架确认成功，系统正在跳转至需求编辑器...',
    type: 'success',
    duration: 1500
  })
  setTimeout(() => {
    router.push({
      path: '/major/requirement-sys/generate',
      query: { mode: 'skeleton', sid: selectedId.value }
    })
  }, 1000)
}

const goToManualBuild = () => {
  router.push('/major/requirement-sys/manual-build')
}
</script>

<style scoped lang="scss">
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;700&family=Inter:wght@400;600;700&display=swap');

// Design Tokens Mapping
$surface: #10131a;
$surface-low: #191c22;
$surface-high: #272a31;
$surface-highest: #32353c;
$primary: #a4e6ff;
$primary-container: #00d1ff;
$success: #00ff9d;
$warn: #ffbd2e;
$danger: #ff3c5b;

.skeleton-match-container {
  height: 100%;
  background: var(--bg-color);
  color: var(--text-color-primary);
  display: flex;
  flex-direction: column;
  font-family: 'Inter', sans-serif;
  overflow: hidden;
}

/* ─── Header ─── */
.tactical-header {
  height: 64px;
  background: var(--sider-bg-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  // Subtle glow instead of border
  box-shadow: 0 1px 12px rgba(0, 209, 255, 0.05);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .breadcrumb-icon {
      font-size: 20px;
      color: var(--primary-color);
    }
    
    .system-title {
      font-family: 'Space Grotesk', sans-serif;
      font-weight: 700;
      letter-spacing: 2px;
      font-size: 14px;
      color: var(--primary-color);
      opacity: 0.7;
    }

    .divider {
      width: 1px;
      height: 16px;
      background: var(--border-color);
      opacity: 0.5;
    }

    .page-title {
      font-weight: 700;
      font-size: 15px;
      color: var(--text-color-primary);
    }
  }

  .workflow-stepper {
    display: flex;
    gap: 1px;
    background: var(--border-color);
    padding: 2px;
    border-radius: 4px;

    .step {
      padding: 6px 16px;
      font-size: 11px;
      font-weight: 700;
      color: var(--text-color-secondary);
      letter-spacing: 1px;

      &.done { color: var(--success-color); }
      &.active {
        background: color-mix(in srgb, var(--primary-color) 10%, transparent);
        color: var(--primary-color);
        border-radius: 2px;
      }
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

/* ─── Common Buttons ─── */
.ghost-btn {
  background: transparent;
  border: 1px solid color-mix(in srgb, var(--primary-color) 20%, transparent);
  color: var(--primary-color);
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
  
  &:hover {
    background: color-mix(in srgb, var(--primary-color) 5%, transparent);
    border-color: var(--primary-color);
  }
}

.primary-gradient-btn {
  background: var(--primary-color);
  border: none;
  color: var(--text-color-primary);
  border-radius: 4px;
  padding: 8px 18px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--box-shadow-neon, 0 4px 12px rgba(0, 0, 0, 0.2));
  transition: 0.2s;
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px color-mix(in srgb, var(--primary-color) 40%, transparent);
    filter: brightness(1.1);
  }
}

/* ─── Main Content ─── */
.tactical-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.recommend-sidebar {
  width: 320px;
  background: var(--sider-bg-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  
  .sidebar-head {
    padding: 24px;
    background: linear-gradient(to bottom, color-mix(in srgb, var(--card-bg-color) 80%, transparent), transparent);

    h3 {
      font-size: 14px;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--primary-color);
    }

    p {
      font-size: 11px;
      color: var(--text-color-secondary);
      margin-top: 4px;
    }
  }

  .skeleton-scroll {
    flex: 1;
    overflow-y: auto;
    padding: 0 16px 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .sidebar-foot {
    padding: 16px;
    border-top: 1px solid var(--border-color);

    .custom-build-btn {
      width: 100%;
      background: color-mix(in srgb, var(--primary-color) 5%, transparent);
      border: 1px dashed color-mix(in srgb, var(--primary-color) 30%, transparent);
      border-radius: 4px;
      border-radius: 4px;
      padding: 12px 0;
      color: var(--primary-color);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      transition: 0.2s;

      &:hover {
        background: color-mix(in srgb, var(--primary-color) 10%, transparent);
        border-style: solid;
      }
    }

    .hint {
      text-align: center;
      font-size: 10px;
      color: var(--text-color-secondary);
      margin-top: 8px;
    }
  }
}

/* ─── Skeleton Card ─── */
.skeleton-card {
  background: var(--card-bg-color);
  border-radius: 4px;
  padding: 18px;
  cursor: pointer;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;

  &:hover {
    background: color-mix(in srgb, var(--card-bg-color) 90%, white);
    transform: translateX(4px);
  }

  &.active {
    background: color-mix(in srgb, var(--primary-color) 8%, transparent);
    border-color: color-mix(in srgb, var(--primary-color) 30%, transparent);
    
    &::after {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--primary-color);
      border-radius: 4px 0 0 4px;
    }
  }

  .rank-tag {
    position: absolute;
    top: 0;
    right: 0;
    font-family: 'Space Grotesk', sans-serif;
    font-size: 9px;
    font-weight: 700;
    padding: 3px 10px;
    border-radius: 0 4px 0 4px;
    
    &.rank-1 { background: var(--primary-color); 
      color: #0d1117;  }
    &.rank-2 { 
      border: 1px solid color-mix(in srgb, var(--primary-color) 40%, transparent); 
      color: var(--primary-color); 
    }
    &.rank-3 { 
      border: 1px solid var(--border-color); 
      color: var(--text-color-secondary); 
      opacity: 0.7;
    }
  }

  .card-title {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 10px;
    padding-right: 40px;
    color: var(--text-color-primary);
  }

  .tech-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 16px;

    .tag {
      font-size: 9px;
      padding: 2px 8px;
      background: color-mix(in srgb, var(--card-bg-color) 80%, transparent);
      border-radius: 2px;
      // [MODIFIED] Use secondary text
      color: var(--text-color-secondary);
      opacity: 0.8;
    }
  }

  .metric-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .metric-row {
    display: flex;
    align-items: center;
    gap: 8px;

    .label { font-size: 10px; color: var(--text-color-secondary);  width: 44px; }
    .progress-track {
      flex: 1;
      height: 3px;
      background: var(--border-color);
      border-radius: 2px;
      overflow: hidden;
      
      .progress-fill {
        height: 100%;
        border-radius: 2px;
      }
    }
    .value { font-size: 10px; font-weight: 700; width: 30px; text-align: right; }
    .value.success {
      color: var(--success-color);
    }
  }

  .selection-status {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--border-color);
    opacity: 0.3;
    font-size: 10px;
    font-weight: 700;
    color: var(--primary-color);
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

/* ─── Details Viewport ─── */
.details-viewport {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-image: radial-gradient(circle at 10% 10%, color-mix(in srgb, var(--primary-color) 2%, transparent) 0%, transparent 40%);
  
  .viewport-header {
    height: 80px;
    padding: 0 40px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;

    h2 {
      font-size: 20px;
      font-weight: 700;
      letter-spacing: -0.5px;
      color: var(--text-color-primary);
    }

    .id-badge {
      font-size: 11px;
      color: var(--text-color-secondary);
      margin-top: 4px;
      font-family: monospace;
    }

    .status-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      background: color-mix(in srgb, var(--success-color) 5%, transparent);
      border-radius: 20px;
      padding: 6px 16px;
      font-size: 12px;
      font-weight: 600;
      color: var(--success-color);

      .dot {
        width: 8px;
        height: 8px;
        background: var(--success-color);
        border-radius: 50%;
        box-shadow: 0 0 8px var(--success-color);
      }
    }
  }

  .scroll-container {
    flex: 1;
    overflow-y: auto;
    padding: 0 40px 40px;
  }
}

.section-label {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-color-secondary);
  opacity: 0.3;
  margin-bottom: 24px;
  margin-top: 40px;
  display: flex;
  align-items: center;
  gap: 16px;

  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: linear-gradient(to right, color-mix(in srgb, var(--border-color) 50%, transparent), transparent);
  }
}

/* ─── Info Grid ─── */
.data-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1px;
  background: var(--border-color);
  border-radius: 4px;
  overflow: hidden;

  .data-item {
    background: var(--sider-bg-color);
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    &.full { grid-column: span 2; }

    .label {
      font-size: 11px;
      font-weight: 700;
      color: var(--text-color-secondary);
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .value {
      font-size: 14px;
      color: var(--text-color-primary);
      opacity: 0.8;
      
      &.bright { color: var(--primary-color); 
        font-weight: 600; 
        opacity: 1; }

      .pill {
        display: inline-block;
        padding: 4px 12px;
        background: var(--card-bg-color);
        border-radius: 2px;
        font-size: 11px;
        color: var(--text-color-primary);
        margin-right: 8px;

        &.outline {
          background: transparent;
          border: 1px solid color-mix(in srgb, var(--primary-color) 30%, transparent);
          color: var(--primary-color);
        }
      }
    }
  }
}

/* ─── Tree Table ─── */
.tree-table-wrapper {
  background: var(--sider-bg-color);
  border-radius: 4px;
  overflow: hidden;
  box-shadow: var(--box-shadow-base, 0 8px 24px rgba(0, 0, 0, 0.2));
  border: 1px solid var(--border-color);
  opacity: 0.9;
}

.tree-legend {
  padding: 12px 20px;
  background: var(--card-bg-color);
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;

  .legend-title { font-size: 9px; font-weight: 700; color: var(--text-color-secondary);  letter-spacing: 1px; }

  .leg-item {
    font-size: 10px;
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--text-color-primary);
    opacity: 0.8;

    &::before {
       content: '';
       width: 6px;
       height: 6px;
       border-radius: 50%;
    }

    &.root::before { background: var(--warning-color); }
    &.dim::before { background: #b898ff; } /* Keep purple distinct */
    &.gen::before { background: #7ad0ff; } /* Keep light blue distinct */
    &.spec::before { background: var(--success-color); }
    
    &.calc::before { 
      content: 'C'; 
      border-radius: 2px; 
      width: 12px; 
      height: 12px; 
      display: flex; 
      align-items: center; 
      justify-content: center; 
      background: var(--primary-color); 
      font-size: 8px; 
      line-height: 1; 
      color: #0d1117; /* Dark text for contrast */
    }
    &.norm::before { 
      content: 'N'; 
      border-radius: 2px; 
      width: 12px; 
      height: 12px; 
      display: flex; 
      align-items: center; 
      justify-content: center; 
      background: #b898ff; 
      font-size: 8px; 
      line-height: 1; 
      color: #0d1117;
    }
  }

  .spacer { flex: 1; }
}

.tree-header {
  display: flex;
  background: var(--card-bg-color);
  padding: 10px 0;
  border-bottom: 2px solid var(--border-color);

  div {
    font-size: 10px;
    font-weight: 700;
    color: var(--text-color-secondary);
    padding: 0 20px;
    letter-spacing: 1px;
    text-transform: uppercase;
  }
}

.col-name { flex: 1.5; display: flex; align-items: center; }
.col-type { width: 140px; text-align: center; }
.col-ops { width: 280px; }
.col-cond { width: 180px; text-align: right; }

.t-row {
  display: flex;
  min-height: 48px;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  transition: 0.1s;

  &:hover { background: color-mix(in srgb, var(--primary-color) 2%, transparent); 
    opacity: 1; }

    &.root { 
    background: color-mix(in srgb, var(--warning-color) 2%, transparent); 
    .node-label { font-weight: 700; color: var(--warning-color); } 
  }
  &.dim { 
    .node-label { font-weight: 600; color: #b898ff; } 
  }
  
  .col-name {
    .toggle-icon {
      cursor: pointer;
      margin-right: 8px;
      font-size: 12px;
      // [MODIFIED] Use secondary text
      color: var(--text-color-secondary);
      opacity: 0.5;
      &:hover { color: var(--text-color-primary); opacity: 1; }
    }
    .node-dot {
      width: 4px;
      height: 4px;
      border-radius: 50%;
      background: var(--success-color);
      margin-right: 12px;
      margin-left: 4px;
    }
    .node-label { font-size: 13px; color: var(--text-color-primary); }
  }

  .col-type {
    display: flex;
    justify-content: center;
    .type-badge {
       font-size: 9px;
       font-weight: 700;
       padding: 2px 8px;
       border-radius: 2px;
       // [MODIFIED] Use card bg
       background: var(--card-bg-color);
       color: var(--text-color-primary);

       &.root { background: color-mix(in srgb, var(--warning-color) 10%, transparent); color: var(--warning-color); border: 1px solid color-mix(in srgb, var(--warning-color) 20%, transparent); }
       &.dim { background: rgba(#b898ff, 0.1); color: #b898ff; border: 1px solid rgba(#b898ff, 0.2); }
       &.gen { background: rgba(#7ad0ff, 0.1); color: #7ad0ff; border: 1px solid rgba(#7ad0ff, 0.2); }
       &.spec { background: color-mix(in srgb, var(--success-color) 10%, transparent); color: var(--success-color); border: 1px solid color-mix(in srgb, var(--success-color) 20%, transparent); }
    }
  }

  .col-ops {
    display: flex;
    gap: 8px;
    padding: 0 16px;

    .agg-badge {
      font-size: 10px;
      // [MODIFIED] Use secondary text
      color: var(--text-color-secondary);
      opacity: 0.6;
      // [MODIFIED] Use card bg
      background: var(--card-bg-color);
      padding: 4px 10px;
      border-radius: 20px;
    }

    .op-button-group {
      display: flex;
      gap: 6px;
    }

    .tactical-op-btn {
      font-family: inherit;
      border: none;
      border-radius: 2px;
      padding: 4px 10px;
      font-size: 10px;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
      // [MODIFIED] Use card bg
      background: var(--card-bg-color);
      color: var(--text-color-primary);
      transition: 0.2s;
      border: 1px solid transparent;

      .btn-tag {
        font-size: 8px;
        font-weight: 900;
        padding: 1px 4px;
        border-radius: 1px;
        background: #000;
        color: #fff;
      }

      &.calc { 
        border-color: color-mix(in srgb, var(--primary-color) 20%, transparent);
        .btn-tag { color: var(--primary-color); background: transparent; border: 1px solid var(--primary-color); }
        &:hover { background: color-mix(in srgb, var(--primary-color) 5%, transparent); border-color: var(--primary-color); }
      }
      &.norm { 
        border-color: rgba(#b898ff, 0.2);
        .btn-tag { color: #b898ff; background: transparent; border: 1px solid #b898ff; }
        &:hover { background: rgba(#b898ff, 0.05); border-color: #b898ff; }
      }
    }
  }

  .col-cond {
    padding: 0 20px;
    .cond-btn {
      background: transparent;
      border: 1px solid var(--border-color);
      opacity: 0.3;
      color: var(--text-color-secondary);
      padding: 4px 12px;
      font-size: 10px;
      border-radius: 2px;
      cursor: pointer;
      white-space: nowrap;
      
      &:hover {
        background: var(--card-bg-color);
        color: var(--text-color-primary);
        border-color: var(--text-color-secondary);
        opacity: 1;
      }
    }
  }
}

/* ─── Tactical Table ─── */
.tactical-table-card {
  background: var(--sider-bg-color);
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.tactical-table {
  width: 100%;
  border-collapse: collapse;

  th {
    background: var(--card-bg-color);
     padding: 16px 24px;
     text-align: center;
     font-size: 10px;
     // [MODIFIED] Use secondary text
     color: var(--text-color-secondary);
     opacity: 0.6;
     letter-spacing: 1px;
     text-transform: uppercase;
     // [MODIFIED] Use border color
     border-bottom: 2px solid var(--border-color);
     &:first-child { text-align: left; }
  }

  td {
    padding: 20px 24px;
    text-align: center;
    border-bottom: 1px solid var(--border-color);
    // opacity: 0.3;
    font-size: 12px;
    color: var(--text-color-primary);

    &.row-head { text-align: left; font-weight: 700; color: var(--text-color-primary); opacity: 1; width: 140px; }

.score-hint {
  font-size: 10px;
  // [MODIFIED] Use secondary text
  color: var(--text-color-secondary);
  opacity: 0.5;
  margin-top: 6px;
  font-family: monospace;
}
  }
}

.status-pill {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 700;
  
  &.pass { background: color-mix(in srgb, var(--success-color) 10%, transparent); color: var(--success-color); }
  &.warn { background: color-mix(in srgb, var(--warning-color) 10%, transparent); color: var(--warning-color); }
  &.fail { background: color-mix(in srgb, var(--danger-color) 10%, transparent); color: var(--danger-color); }
}

/* ─── Modal & Overlays ─── */
.tactical-overlay {
  position: fixed;
  inset: 0;
  // [MODIFIED] Use darker overlay
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: var(--backdrop-filter);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tactical-modal {
  width: 500px;
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  box-shadow: var(--box-shadow-base, 0 40px 120px rgba(0, 0, 0, 0.8));
  position: relative;
  border-radius: 2px;

  .modal-corner {
    position: absolute;
    width: 6px;
    height: 6px;
    border: 2px solid var(--primary-color);
    
    &.tl { top: -1px; left: -1px; border-right: none; border-bottom: none; }
    &.tr { top: -1px; right: -1px; border-left: none; border-bottom: none; }
    &.bl { bottom: -1px; left: -1px; border-right: none; border-top: none; }
    &.br { bottom: -1px; right: -1px; border-left: none; border-top: none; }
  }

  .modal-head {
    padding: 24px;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .modal-category {
      font-size: 10px;
      font-weight: 900;
      color: var(--primary-color);
      text-transform: uppercase;
      letter-spacing: 2px;
      display: block;
      margin-bottom: 4px;
    }

    h3 { font-size: 18px; font-weight: 700; color: #fff; }

    .close-btn {
      background: transparent;
      border: none;
      color: var(--text-color-secondary);
        opacity: 0.5;
        font-size: 20px;
        cursor: pointer;
        &:hover { 
          color: var(--text-color-primary); 
          opacity: 1; 
        }
    }
  }

  .modal-body {
    padding: 24px;
    
    label {
      font-size: 9px;
      font-weight: 900;
      color: var(--text-color-secondary);
        opacity: 0.4;
        text-transform: uppercase;
        letter-spacing: 1.5px;
        display: block;
        margin-bottom: 12px;
    }

    .description {
      font-size: 13px;
      line-height: 1.8;
      color: var(--text-color-primary);
      padding: 16px;
      background: rgba(0, 0, 0, 0.2);
        border-radius: 4px;
        border-left: 2px solid var(--primary-color);
    }

    .content-block { margin-bottom: 24px; }

    .cond-item {
      display: flex;
      align-items: center;
      gap: 12px;
      background: color-mix(in srgb, var(--warning-color) 5%, transparent);
        border: 1px solid color-mix(in srgb, var(--warning-color) 10%, transparent);
        padding: 10px 16px;
      border-radius: 4px;
      font-size: 12px;

      .index { font-weight: 900; color: var(--warning-color); opacity: 0.7; }
        .field { font-weight: 700; color: var(--text-color-primary); }
        .operator { color: var(--warning-color); font-weight: 900; font-family: monospace; }
        .target { color: var(--text-color-secondary); opacity: 0.8; }
      }

    .cond-always {
      padding: 16px;
      background: color-mix(in srgb, var(--success-color) 5%, transparent);
        color: var(--success-color);
      font-size: 12px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .data-row {
      margin-bottom: 16px;
      display: flex;
      flex-direction: column;
      gap: 6px;

      .m-lbl { font-size: 10px; color: var(--text-color-secondary);  }
      .m-val {
        font-size: 14px;
        &.highlight { color: var(--primary-color);  font-weight: 700; }
        &.code { font-family: monospace;  background: #0d1117; 
          color: var(--text-color-primary);  padding: 4px 10px; border-radius: 4px; }
      }
    }

    .mapping-item {
       display: flex;
       align-items: center;
       gap: 12px;
       font-family: monospace;
       background: rgba(0, 0, 0, 0.2);
       padding: 8px 12px;
       border-radius: 4px;
       font-size: 12px;

       .source { color: var(--warning-color); }
         .target { color: var(--primary-color); }
    }
  }
}

/* ─── Algorithm Cards ─── */
.algo-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.algo-tactical-card {
  background: var(--sider-bg-color);
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  
  .algo-head {
    padding: 12px 20px;
    // [MODIFIED] Use card bg
    background: var(--card-bg-color);
    // [MODIFIED] Use border color
    border-bottom: 2px solid var(--border-color);
    font-size: 11px;
    font-weight: 700;
    color: var(--primary-color);
    letter-spacing: 0.5px;
  }

  .algo-body {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .algo-row {
     display: flex;
     align-items: flex-start;
     gap: 12px;
     font-size: 12px;

     .k { color: var(--text-color-secondary);  width: 60px; flex-shrink: 0; font-size: 10px; font-weight: 700; text-transform: uppercase; }
     .v { color: var(--text-color-primary);  line-height: 1.5; }
     
     .code { 
       font-family: monospace; 
       background: rgba(0, 0, 0, 0.2); 
       color: var(--primary-color); 
       padding: 2px 8px; 
       border-radius: 2px;
       font-size: 11px;
     }
  }
}

/* Animations */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

::-webkit-scrollbar { width: 4px; height: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { 
  // [MODIFIED] Use border color
  background: color-mix(in srgb, var(--border-color) 50%, transparent); 
  border-radius: 10px; 
}
::-webkit-scrollbar-thumb:hover { 
  // [MODIFIED] Use primary color
  background: var(--primary-color); 
}

</style>
