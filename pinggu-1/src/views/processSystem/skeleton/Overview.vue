<template>
  <div class="skeleton-overview">
    <div class="layout">
      <!-- Left Panel: Stats and Filters -->
      <div class="panel-left">
        <div class="stats-grid">
          <div class="stat-card" v-for="item in stats" :key="item.label">
            <div class="stat-val">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>

        <div class="section-title">领域筛选</div>
        <div class="domain-list">
          <div 
            v-for="domain in domains" 
            :key="domain.id" 
            class="domain-item" 
            :class="{ active: activeDomain === domain.id }"
            @click="activeDomain = domain.id"
          >
            <div class="domain-dot" :style="{ backgroundColor: domain.color, boxShadow: `0 0 8px ${domain.color}` }"></div>
            <span class="domain-name">{{ domain.name }}</span>
            <span class="domain-count">{{ domain.count }}</span>
          </div>
        </div>

        <div class="level-filter">
          <div class="section-title">评估层级</div>
          <el-checkbox-group v-model="selectedLevels" class="level-chips">
            <el-checkbox-button v-for="level in levels" :key="level" :label="level">{{ level }}</el-checkbox-button>
          </el-checkbox-group>
        </div>

        <div class="lib-section">
          <div class="section-title">公共资源库</div>
          <div class="lib-row" v-for="lib in libraries" :key="lib.name">
            <span class="lib-name"><el-icon><Management /></el-icon>{{ lib.name }}</span>
            <span class="lib-val">{{ lib.count }} 条</span>
          </div>
        </div>
      </div>

      <!-- Middle Panel: Relationship Graph -->
      <div class="panel-middle">
        <div class="graph-header">
          <div class="section-title">骨架关系图谱</div>
          <div class="graph-actions" v-if="graphFilter">
            <el-tag closable @close="graphFilter = null" size="small" type="primary">
              过滤: {{ graphFilter }}
            </el-tag>
          </div>
          <div class="graph-legend">
            <div class="legend-item"><span class="dot domain"></span>领域</div>
            <div class="legend-item"><span class="dot skeleton"></span>骨架</div>
            <div class="legend-item"><span class="dot indicator"></span>指标族</div>
          </div>
        </div>
        <div ref="graphRef" class="graph-container"></div>
        <div class="graph-controls">
          <el-button-group>
            <el-button size="small" :icon="ZoomIn"></el-button>
            <el-button size="small" :icon="ZoomOut"></el-button>
            <el-button size="small" :icon="Refresh"></el-button>
          </el-button-group>
        </div>
      </div>

      <!-- Right Panel: Skeleton List -->
      <div class="panel-right">
        <div class="right-header">
          <h3>骨架库列表</h3>
          <el-input 
            v-model="searchQuery" 
            placeholder="搜索骨架名称/编号..." 
            :prefix-icon="Search"
            clearable
            size="small"
          />
        </div>
        <div class="skeleton-list">
          <div 
            v-for="sk in filteredSkeletons" 
            :key="sk.id" 
            class="sk-card"
            :class="sk.domain"
            @click="handleViewDetail(sk)"
          >
            <div class="sk-top">
              <span class="sk-domain-tag" :class="sk.domain">{{ sk.domainName }}</span>
              <span class="sk-level-tag">{{ sk.level }}</span>
              <div class="sk-status" :class="{ draft: sk.status === '草稿' }"></div>
            </div>
            <div class="sk-name">{{ sk.name }}</div>
            <div class="sk-metrics">
              <div class="sk-metric">目标 <span class="n">{{ sk.goals }}</span></div>
              <div class="sk-metric">指标 <span class="n">{{ sk.indicators }}</span></div>
              <div class="sk-metric">规则 <span class="n">{{ sk.rules }}</span></div>
            </div>
            <div class="sk-branches">
              <span v-for="br in sk.branches" :key="br" class="sk-branch">{{ br }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import * as echarts from 'echarts'
import { 
  Management, Search, ZoomIn, ZoomOut, Refresh 
} from '@element-plus/icons-vue'

const activeDomain = ref('sense')
const selectedLevels = ref(['体系级', '系统级', '装备级'])
const searchQuery = ref('')
const graphFilter = ref(null)
const graphRef = ref(null)
let chart = null

const stats = [
  { label: '骨架总数', value: '42' },
  { label: '覆盖领域', value: '6' },
  { label: '指标节点', value: '286' },
  { label: '分支规则', value: '93' }
]

const domains = [
  { id: 'sense', name: '感知领域', color: '#3b82f6', count: 12 },
  { id: 'defend', name: '防卫领域', color: '#ef4444', count: 8 },
  { id: 'support', name: '支援领域', color: '#10b981', count: 6 },
  { id: 'ttc', name: '测运控领域', color: '#f59e0b', count: 5 },
  { id: 'launch', name: '发射领域', color: '#a855f7', count: 6 },
  { id: 'info', name: '信息领域', color: '#06b6d4', count: 5 }
]

const levels = ['体系级', '系统级', '装备级']

const libraries = [
  { name: '指标库', count: '1,248' },
  { name: '算法库', count: '326' },
  { name: '指标体系库', count: '38' },
  { name: '数据规格库', count: '582' }
]

const skeletons = ref([
  { 
    id: 'SK-001', 
    name: '通信对抗装备效能评估骨架', 
    domain: 'defend', 
    domainName: '防卫领域',
    level: '装备级', 
    status: '发布', 
    goals: 4, 
    indicators: 18, 
    rules: 3, 
    branches: ['跳频对抗', '环境适应性', '抗干扰模式']
  },
  { 
    id: 'SK-002', 
    name: '雷达侦察系统性能评估骨架', 
    domain: 'sense', 
    domainName: '感知领域',
    level: '系统级', 
    status: '发布', 
    goals: 5, 
    indicators: 22, 
    rules: 4, 
    branches: ['复杂背景', '多目标跟踪']
  },
  { 
    id: 'SK-003', 
    name: '电子防护能力综合评估骨架', 
    domain: 'defend', 
    domainName: '防卫领域',
    level: '体系级', 
    status: '草稿', 
    goals: 3, 
    indicators: 15, 
    rules: 2, 
    branches: ['协同防护']
  },
  { 
    id: 'SK-004', 
    name: '测控链路可靠性评估骨架', 
    domain: 'ttc', 
    domainName: '测运控领域',
    level: '装备级', 
    status: '发布', 
    goals: 4, 
    indicators: 12, 
    rules: 3, 
    branches: ['链路损耗', '遮挡分析']
  }
])

const filteredSkeletons = computed(() => {
  return skeletons.value.filter(sk => {
    const matchesDomain = activeDomain.value === 'all' || sk.domain === activeDomain.value
    const matchesLevel = selectedLevels.value.includes(sk.level)
    const matchesSearch = sk.name.includes(searchQuery.value) || sk.id.includes(searchQuery.value)
    
    let matchesGraph = true
    if (graphFilter.value) {
      const filter = graphFilter.value
      // Match by Domain
      if (['感知领域', '防卫领域', '支援领域', '测控领域', '发射领域', '信息领域'].includes(filter)) {
        const domainMap = { '感知领域': 'sense', '防卫领域': 'defend', '支援领域': 'support', '测控领域': 'ttc', '发射领域': 'launch', '信息领域': 'info' }
        matchesGraph = sk.domain === domainMap[filter]
      }
      // Match by Skeleton Name
      else if (filter.includes('骨架')) {
        matchesGraph = sk.name.includes(filter.replace('骨架', ''))
      } 
      // Match by Indicator Group
      else if (filter.includes('指标族')) {
        if (filter === '效能指标族') matchesGraph = ['SK-001', 'SK-003'].includes(sk.id)
        if (filter === '性能指标族') matchesGraph = ['SK-001', 'SK-002'].includes(sk.id)
        if (filter === '环境指标族') matchesGraph = ['SK-002', 'SK-003'].includes(sk.id)
        if (filter === '时延指标族') matchesGraph = ['SK-001', 'SK-004'].includes(sk.id)
      }
    }
    
    return matchesDomain && matchesLevel && matchesSearch && matchesGraph
  })
})

const initChart = () => {
  if (!graphRef.value) return
  chart = echarts.init(graphRef.value)
  
  const categories = [
    { name: '领域' },
    { name: '骨架' },
    { name: '指标族' }
  ]
  
  const nodes = [
    // Layer 1: Domains
    { name: '感知领域', category: 0, symbolSize: 50, itemStyle: { color: '#3b82f6' } },
    { name: '防卫领域', category: 0, symbolSize: 50, itemStyle: { color: '#ef4444' } },
    { name: '支援领域', category: 0, symbolSize: 50, itemStyle: { color: '#10b981' } },
    
    // Layer 2: Skeletons
    { name: '通信对抗骨架', category: 1, symbolSize: 35, itemStyle: { color: 'rgba(59, 130, 246, 0.7)' } },
    { name: '雷达侦察骨架', category: 1, symbolSize: 35, itemStyle: { color: 'rgba(239, 68, 68, 0.7)' } },
    { name: '电子防护骨架', category: 1, symbolSize: 35, itemStyle: { color: 'rgba(16, 185, 129, 0.7)' } },
    
    // Layer 3: Indicator Groups
    { name: '效能指标族', category: 2, symbolSize: 20 },
    { name: '性能指标族', category: 2, symbolSize: 20 },
    { name: '环境指标族', category: 2, symbolSize: 20 },
    { name: '时延指标族', category: 2, symbolSize: 20 }
  ]
  
  const links = [
    // Domain -> Skeleton
    { source: '感知领域', target: '通信对抗骨架' },
    { source: '感知领域', target: '雷达侦察骨架' },
    { source: '防卫领域', target: '电子防护骨架' },
    
    // Skeleton -> Indicator Group
    { source: '通信对抗骨架', target: '效能指标族' },
    { source: '通信对抗骨架', target: '性能指标族' },
    { source: '通信对抗骨架', target: '时延指标族' },
    { source: '雷达侦察骨架', target: '性能指标族' },
    { source: '雷达侦察骨架', target: '环境指标族' },
    { source: '电子防护骨架', target: '效能指标族' },
    { source: '电子防护骨架', target: '环境指标族' }
  ]

  const option = {
    tooltip: {},
    legend: [{
      data: categories.map(a => a.name),
      inactiveColor: '#4e5d7a',
      textStyle: { color: '#d6e0f0' }
    }],
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      categories: categories,
      roam: true,
      label: {
        show: true,
        position: 'right',
        color: '#d6e0f0'
      },
      force: {
        repulsion: 150,
        edgeLength: 100
      },
      lineStyle: {
        color: 'source',
        curveness: 0.1,
        width: 1
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3 }
      }
    }]
  }

  chart.setOption(option)
  
  chart.on('click', (params) => {
    if (params.dataType === 'node') {
      graphFilter.value = params.name
    }
  })
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', () => chart?.resize())
})

const handleViewDetail = (sk) => {
  console.log('View detail:', sk)
}
</script>

<style scoped lang="scss">
.skeleton-overview {
  height: 100%;
  background: #060a13;
  color: #d6e0f0;
  overflow: hidden;

  .layout {
    display: grid;
    grid-template-columns: 280px 1fr 320px;
    height: 100%;
  }

  // Common Panels
  .panel-left, .panel-right {
    background: #0d1326;
    border-right: 1px solid rgba(80, 140, 255, 0.08);
    display: flex;
    flex-direction: column;
  }
  .panel-right {
    border-right: none;
    border-left: 1px solid rgba(80, 140, 255, 0.08);
  }

  .panel-middle {
    background: #0a0f1e;
    position: relative;
    display: flex;
    flex-direction: column;
  }

  // Left Panel
  .stats-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    padding: 20px;
    border-bottom: 1px solid rgba(80, 140, 255, 0.08);

    .stat-card {
      background: #111a33;
      padding: 12px;
      border-radius: 8px;
      border: 1px solid rgba(80, 140, 255, 0.05);

      .stat-val {
        font-family: 'JetBrains Mono', monospace;
        font-size: 24px;
        font-weight: bold;
        color: #fff;
      }
      .stat-label {
        font-size: 11px;
        color: #4e5d7a;
        margin-top: 4px;
      }
    }
  }

  .section-title {
    padding: 16px 20px 8px;
    font-size: 12px;
    font-weight: bold;
    color: #4e5d7a;
    text-transform: uppercase;
    letter-spacing: 1.5px;
  }

  .domain-list {
    padding: 0 12px;
    flex: 1;
    overflow-y: auto;

    .domain-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 15px;
      border-radius: 8px;
      cursor: pointer;
      margin-bottom: 4px;
      transition: all 0.2s;

      &:hover { background: rgba(59, 130, 246, 0.05); }
      &.active {
        background: rgba(59, 130, 246, 0.1);
        border: 1px solid rgba(59, 130, 246, 0.2);
      }

      .domain-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
      }
      .domain-name { flex: 1; font-size: 14px; }
      .domain-count {
        font-family: 'JetBrains Mono', monospace;
        font-size: 11px;
        color: #4e5d7a;
      }
    }
  }

  .level-filter {
    padding: 15px 20px;
    border-top: 1px solid rgba(80, 140, 255, 0.08);
  }

  .lib-section {
    padding: 15px 20px;
    border-top: 1px solid rgba(80, 140, 255, 0.08);

    .lib-row {
      display: flex;
      justify-content: space-between;
      padding: 6px 0;
      font-size: 12px;
      color: #8494b2;

      .lib-name {
        display: flex;
        align-items: center;
        gap: 6px;
        .el-icon { font-size: 14px; color: #3b82f6; }
      }
      .lib-val { font-family: 'JetBrains Mono', monospace; color: #4e5d7a; }
    }
  }

  // Middle Panel: Graph
  .graph-header {
    padding: 0 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: absolute;
    top: 0; left: 0; right: 0;
    z-index: 10;
    pointer-events: none;

    .section-title { padding-left: 0; pointer-events: auto; }
    .graph-actions {
      pointer-events: auto;
      margin-left: 10px;
    }
    .graph-legend {
      display: flex;
      gap: 15px;
      pointer-events: auto;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 11px;
        color: #8494b2;

        .dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          &.domain { background: #3b82f6; box-shadow: 0 0 8px rgba(59, 130, 246, 0.5); }
          &.skeleton { background: rgba(59, 130, 246, 0.7); border: 1px solid rgba(59, 130, 246, 0.4); }
          &.indicator { background: #4e5d7a; }
        }
      }
    }
  }

  .graph-container {
    flex: 1;
    width: 100%;
  }

  .graph-controls {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(13, 19, 38, 0.8);
    backdrop-filter: blur(8px);
    padding: 5px;
    border-radius: 8px;
    border: 1px solid rgba(80, 140, 255, 0.08);

    .el-button {
      background: transparent;
      border: none;
      color: #8494b2;
      &:hover { color: #3b82f6; }
    }
  }

  // Right Panel: List
  .right-header {
    padding: 20px;
    border-bottom: 1px solid rgba(80, 140, 255, 0.08);

    h3 { font-size: 15px; margin-bottom: 12px; }
    :deep(.el-input__wrapper) {
      background-color: #060a13;
      box-shadow: 0 0 0 1px rgba(80, 140, 255, 0.1) inset;
    }
  }

  .skeleton-list {
    flex: 1;
    overflow-y: auto;
    padding: 15px;

    .sk-card {
      background: #111a33;
      border: 1px solid rgba(80, 140, 255, 0.08);
      border-radius: 10px;
      padding: 15px;
      margin-bottom: 12px;
      cursor: pointer;
      position: relative;
      transition: all 0.2s;

      &:hover {
        border-color: rgba(59, 130, 246, 0.4);
        background: #15203d;
      }

      &::before {
        content: '';
        position: absolute;
        left: 0; top: 0; bottom: 0;
        width: 3px;
        border-radius: 3px 0 0 3px;
      }

      &.defend::before { background: #ef4444; }
      &.sense::before { background: #3b82f6; }
      &.ttc::before { background: #f59e0b; }

      .sk-top {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 10px;

        .sk-domain-tag {
          font-size: 10px;
          padding: 2px 8px;
          border-radius: 4px;
          &.defend { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
          &.sense { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
          &.ttc { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
        }
        .sk-level-tag {
          font-size: 10px;
          color: #4e5d7a;
          border: 1px solid rgba(80, 140, 255, 0.1);
          padding: 1px 6px;
          border-radius: 4px;
        }
        .sk-status {
          margin-left: auto;
          width: 8px; height: 8px;
          border-radius: 50%;
          background: #10b981;
          box-shadow: 0 0 8px #10b981;
          &.draft { background: #f59e0b; box-shadow: 0 0 8px #f59e0b; }
        }
      }

      .sk-name {
        font-size: 14px;
        font-weight: bold;
        margin-bottom: 12px;
        line-height: 1.4;
      }

      .sk-metrics {
        display: flex;
        gap: 15px;
        margin-bottom: 12px;

        .sk-metric {
          font-size: 11px;
          color: #4e5d7a;
          display: flex;
          align-items: center;
          gap: 4px;
          .n { color: #8494b2; font-family: 'JetBrains Mono', monospace; font-weight: bold; }
        }
      }

      .sk-branches {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .sk-branch {
          font-size: 10px;
          background: #060a13;
          padding: 2px 8px;
          border-radius: 10px;
          color: #5a6a85;
          border: 1px solid rgba(80, 140, 255, 0.05);
        }
      }
    }
  }
}

// Global Element Plus Overrides
:deep(.el-checkbox-button__inner) {
  background: transparent !important;
  border: 1px solid rgba(80, 140, 255, 0.1) !important;
  color: #4e5d7a !important;
  font-size: 11px !important;
  padding: 8px 12px !important;
  margin-right: 5px;
  border-radius: 4px !important;
}
:deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  border-color: #3b82f6 !important;
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.1) !important;
}
</style>
