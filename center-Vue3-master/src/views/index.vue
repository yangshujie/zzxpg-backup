<template>
  <div class="app-container mission-page home-dashboard">
    <section class="page-hero">
      <div class="page-hero__body">
        <div>
          <span class="page-hero__eyebrow">
            <el-icon><DataLine /></el-icon>
            Mission Intelligence Hub
          </span>
          <h1 class="page-hero__title">通信对抗试验任务综合评估平台</h1>
          <p class="page-hero__description">
            以任务阶段、指标体系、算法完备性和报告生成链路为主线，统一展示任务设计、分中心填报、主中心汇聚与评估发布的进展状态。
            首页聚焦总览、阻断项和协同流程，作为整个前端的视觉基线。
          </p>
          <div class="page-hero__actions">
            <el-button type="primary" @click="goTarget('/system-cooperation/combat-profile')">进入协同设计</el-button>
            <el-button @click="goTarget('/zhpg/report/manage')">查看评估报告</el-button>
          </div>
        </div>

        <div class="page-hero__panel">
          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">综合完成度</h3>
                <p class="panel-subtitle">当前版本 V0.20，聚焦流程协同与指标闭环</p>
              </div>
              <span class="status-chip status-chip--success">可发布</span>
            </div>
            <div class="hero-kpi-grid">
              <div v-for="item in heroStats" :key="item.label" class="hero-kpi">
                <div class="hero-kpi__value">{{ item.value }}</div>
                <div class="hero-kpi__label">{{ item.label }}</div>
              </div>
            </div>
          </div>

          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">优化优先级</h3>
                <p class="panel-subtitle">按影响范围和交付风险排序</p>
              </div>
              <el-icon class="hero-side-icon"><Timer /></el-icon>
            </div>
            <div class="score-bars">
              <div v-for="item in priorityBars" :key="item.label" class="score-bar">
                <div class="score-bar__head">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}%</strong>
                </div>
                <div class="score-bar__track">
                  <div :class="['score-bar__fill', item.tone && `score-bar__fill--${item.tone}`]" :style="{ width: `${item.value}%` }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <div v-for="item in metricCards" :key="item.label" class="metric-card">
        <div class="metric-card__label">{{ item.label }}</div>
        <div class="metric-card__value">{{ item.value }}</div>
        <div class="metric-card__trend">{{ item.desc }}</div>
      </div>
    </section>

    <section class="page-grid-2">
      <div class="stack-grid">
        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">四维评估成熟度</h3>
              <p class="panel-subtitle">参考需求完成度分析页，统一成卡片化数据视图</p>
            </div>
            <span class="status-chip status-chip--info">分析视图</span>
          </div>
          <div class="dimension-grid">
            <article v-for="item in dimensions" :key="item.name" class="dimension-card">
              <div class="dimension-card__head">
                <div>
                  <div class="dimension-card__name">{{ item.name }}</div>
                  <div class="dimension-card__meta">{{ item.meta }}</div>
                </div>
                <div class="dimension-card__score">{{ item.score }}</div>
              </div>
              <div class="mini-stat-grid">
                <div v-for="metric in item.metrics" :key="metric.label" class="mini-stat">
                  <div class="mini-stat__value">{{ metric.value }}</div>
                  <div class="mini-stat__label">{{ metric.label }}</div>
                </div>
              </div>
              <div class="score-bars">
                <div v-for="bar in item.bars" :key="bar.label" class="score-bar">
                  <div class="score-bar__head">
                    <span>{{ bar.label }}</span>
                    <strong>{{ bar.value }}%</strong>
                  </div>
                  <div class="score-bar__track">
                    <div :class="['score-bar__fill', bar.tone && `score-bar__fill--${bar.tone}`]" :style="{ width: `${bar.value}%` }"></div>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </div>

        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">阻断项与修复建议</h3>
              <p class="panel-subtitle">将问题清单上移，保证首页即可看到发布风险</p>
            </div>
            <span class="status-chip status-chip--danger">3 项阻断</span>
          </div>
          <div class="insight-list">
            <div v-for="item in blockers" :key="item.title" class="insight-item">
              <span class="insight-item__dot" :style="{ background: item.color }"></span>
              <div>
                <div class="insight-item__title">{{ item.title }}</div>
                <div class="insight-item__text">{{ item.text }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="stack-grid">
        <div class="panel-card side-summary">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">任务态势快照</h3>
              <p class="panel-subtitle">对应参考图右侧态势侧栏</p>
            </div>
            <el-icon class="hero-side-icon"><Histogram /></el-icon>
          </div>
          <div class="mini-stat-grid">
            <div v-for="item in sideStats" :key="item.label" class="mini-stat">
              <div class="mini-stat__value">{{ item.value }}</div>
              <div class="mini-stat__label">{{ item.label }}</div>
            </div>
          </div>
          <div class="score-bars">
            <div v-for="item in sideDistribution" :key="item.label" class="score-bar">
              <div class="score-bar__head">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}%</strong>
              </div>
              <div class="score-bar__track">
                <div :class="['score-bar__fill', item.tone && `score-bar__fill--${item.tone}`]" :style="{ width: `${item.value}%` }"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">关键标签</h3>
              <p class="panel-subtitle">界面语义从“后台管理”改为“任务协同与评估”</p>
            </div>
            <span class="status-chip status-chip--warning">风格基线</span>
          </div>
          <div class="tag-cloud">
            <span v-for="item in tagCloud" :key="item" class="tag-cloud__item">{{ item }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-heading">
        <div>
          <h3 class="panel-title">协同流程</h3>
          <p class="panel-subtitle">参考流程页，将节点状态、动作按钮和完成情况放在统一链路上展示</p>
        </div>
        <span class="status-chip status-chip--success">流程在线</span>
      </div>
      <div class="workflow-strip">
        <div
          v-for="item in workflow"
          :key="item.title"
          :class="['workflow-node', item.state && `workflow-node--${item.state}`]"
        >
          <div class="workflow-node__index">{{ item.index }}</div>
          <div class="workflow-node__title">{{ item.title }}</div>
          <div class="workflow-node__desc">{{ item.desc }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup name="Index">
import { DataLine, Histogram, Timer } from '@element-plus/icons-vue'

const heroStats = [
  { label: '需求成熟度', value: '78%' },
  { label: '算法完备度', value: '82分' },
  { label: '问题待闭环', value: '15项' },
  { label: '分中心在线', value: '6/6' }
]

const priorityBars = [
  { label: '阻断问题修复', value: 86, tone: 'danger' },
  { label: '告警统一收敛', value: 72, tone: 'warning' },
  { label: '建议项视觉统一', value: 64 }
]

const metricCards = [
  { label: '任务设计包', value: '12', desc: '本周新增 3 个，全部进入评估链路' },
  { label: '指标节点', value: '74', desc: '主指标 18 个，派生指标 56 个' },
  { label: '任务网络', value: '28', desc: '支持主任务与子任务关联展示' },
  { label: '报告生成', value: '9', desc: '当前可追踪到分中心回填进度' }
]

const dimensions = [
  {
    name: '数据完备度',
    meta: '数据源 / 样本 / 标签 / 关联',
    score: 74,
    metrics: [
      { label: '结构', value: 78 },
      { label: '逻辑', value: 65 },
      { label: '内容', value: 80 }
    ],
    bars: [
      { label: '阻断项', value: 28, tone: 'danger' },
      { label: '告警项', value: 42, tone: 'warning' },
      { label: '建议项', value: 65 }
    ]
  },
  {
    name: '方法完备度',
    meta: '方法定义 / 适用边界 / 约束 / 依赖',
    score: 71,
    metrics: [
      { label: '结构', value: 75 },
      { label: '逻辑', value: 62 },
      { label: '内容', value: 76 }
    ],
    bars: [
      { label: '阻断项', value: 32, tone: 'danger' },
      { label: '告警项', value: 48, tone: 'warning' },
      { label: '建议项', value: 53 }
    ]
  },
  {
    name: '评价体系完备度',
    meta: '指标 / 权重 / 规则 / 覆盖',
    score: 85,
    metrics: [
      { label: '结构', value: 90 },
      { label: '逻辑', value: 80 },
      { label: '内容', value: 87 }
    ],
    bars: [
      { label: '阻断项', value: 12, tone: 'danger' },
      { label: '告警项', value: 26, tone: 'warning' },
      { label: '建议项', value: 58 }
    ]
  }
]

const blockers = [
  {
    title: '方法选择与评价方法存在映射断裂',
    text: '当前任务设计中的评价方法与指标体系缺少显式映射，无法在发布前形成完整闭环，需要补齐约束链路。',
    color: '#fb7185'
  },
  {
    title: '任务阶段配置与流程状态未统一',
    text: '协同设计页、任务编排页与报告页对“阶段”使用了不同语义，导致右侧进度栏和流程节点不能一致表达。',
    color: '#fbbf24'
  },
  {
    title: '图形画布视觉风格与业务信息脱节',
    text: 'X6 画布已经承载流程和任务网，但配色、节点信息和左侧条件面板仍偏通用后台样式，需要统一成任务作战视图。',
    color: '#60a5fa'
  }
]

const sideStats = [
  { label: '待办修复', value: 11 },
  { label: '流程节点', value: 4 },
  { label: '模型算法', value: 23 },
  { label: '在线数据源', value: 7 }
]

const sideDistribution = [
  { label: '结构', value: 78 },
  { label: '逻辑', value: 65, tone: 'danger' },
  { label: '内容', value: 80 }
]

const workflow = [
  { index: '01', title: '设计下发', desc: '完成体系任务设计包下发', state: 'done' },
  { index: '02', title: '分中心填写', desc: '各分中心按模板回填场景与指标', state: 'active' },
  { index: '03', title: '上报主中心', desc: '汇总任务网、判据和算法信息' },
  { index: '04', title: '汇集整合', desc: '形成综合评估结果并发布' }
]

const tagCloud = [
  '深色科技风',
  '指标看板',
  '流程画布',
  '任务网络',
  '阻断问题',
  '分中心协同',
  '综合评估',
  '发布闭环'
]

function goTarget(url) {
  window.open(url, '_self')
}
</script>

<style scoped lang="scss">
.home-dashboard {
  .hero-side-icon {
    font-size: 20px;
    color: var(--mc-primary);
  }

  .dimension-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .dimension-card {
    padding: 18px;
    border-radius: 18px;
    border: 1px solid rgba(86, 122, 173, 0.22);
    background: var(--mc-soft-block-bg);
  }

  .dimension-card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
  }

  .dimension-card__name {
    font-size: 16px;
    font-weight: 600;
    color: var(--mc-text-primary);
  }

  .dimension-card__meta {
    margin-top: 6px;
    font-size: 12px;
    color: var(--mc-text-tertiary);
  }

  .dimension-card__score {
    font-size: 30px;
    font-weight: 700;
    color: var(--mc-primary);
  }

  .side-summary {
    min-height: 100%;
  }
}

@media (max-width: 1280px) {
  .home-dashboard .dimension-grid {
    grid-template-columns: 1fr;
  }
}
</style>
