<template>
  <div class="import-workspace">
    <!-- Top System Header -->
    <header class="tactical-header">
      <div class="header-left">
        <el-icon class="breadcrumb-icon">
          <Files />
        </el-icon>
        <span class="system-title">ORBITAL CURATOR</span>
        <span class="divider"></span>
        <span class="page-title">导入一案三纲文件</span>
      </div>

      <div class="workflow-stepper">
        <div class="step active">01 导入一案三纲</div>
        <div class="step">02 匹配任务骨架</div>
        <div class="step">03 需求自动生成</div>
        <div class="step">04 完备性检查</div>
      </div>

      <div class="header-actions">
        <!-- Placeholder for global actions if any -->
      </div>
    </header>

    <main class="tactical-body">
      <!-- Left Sidebar: Upload & Lifecycle -->
      <aside class="analysis-sidebar">
        <div class="sidebar-section">
          <div class="label-tech">FILE INGESTION</div>
          <div class="upload-zone" :class="{ 'is-dragover': isDragover, 'has-file': fileInfo }"
            @dragover.prevent="isDragover = true" @dragleave.prevent="isDragover = false" @drop.prevent="handleDrop">

            <input type="file" ref="fileInput" class="hidden-input" @change="handleFileSelect"
              accept=".docx,.doc,.pdf,.txt">

            <template v-if="!fileInfo">
              <el-icon class="up-ico">
                <UploadFilled />
              </el-icon>
              <div class="up-text"><strong>点击上传</strong> 或拖拽文件</div>
              <div class="up-hint">DOCX, PDF, TXT (MAX 50MB)</div>
            </template>

            <template v-else>
              <div class="file-card">
                <el-icon class="f-ico">
                  <Document />
                </el-icon>
                <div class="f-meta">
                  <div class="f-name">{{ fileInfo.name }}</div>
                  <div class="f-size">{{ formatSize(fileInfo.size) }}</div>
                </div>
                <el-icon class="f-del" @click.stop="resetUpload">
                  <Close />
                </el-icon>
              </div>
            </template>
          </div>

          <div class="parsing-progress" v-if="fileInfo">
            <div class="track">
              <div class="bar" :style="{ width: progress + '%' }"></div>
            </div>
            <div class="stats">
              <span>{{ parsingMsg }}</span>
              <span>{{ progress }}%</span>
            </div>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="label-tech">ANALYSIS LIFECYCLE</div>
          <div class="status-list">
            <div v-for="s in statusSteps" :key="s.id" class="status-item" :class="s.state">
              <div class="s-dot"></div>
              <span class="s-text">{{ s.label }}</span>
            </div>
          </div>
        </div>

        <div class="sidebar-section info-box">
          <div class="label-tech">DOCUMENTATION</div>
          <p>系统自动识别一案三纲（方案、大纲、技术要求）中的评估目的、进入条件、评价准则及指标体系。</p>
          <div class="tech-link"><span>查看解析规范</span><el-icon>
              <ArrowRight />
            </el-icon></div>
        </div>
      </aside>

      <!-- Main Workspace: Result Preview -->
      <section class="analysis-workspace">
        <!-- Empty State -->
        <div v-if="!fileInfo" class="empty-placeholder">
          <div class="p-icon">
            <Memo />
          </div>
          <h3>暂未导入文件</h3>
          <p>请在左侧上传一案三纲文件，系统将自动解析并展示结构化内容。</p>
        </div>

        <!-- Shimmer State -->
        <div v-else-if="isLoading" class="shimmer-container">
          <div class="mhd">
            <div class="shimmer-line h30 w40"></div>
            <div class="shimmer-line h14 w20"></div>
          </div>
          <div class="sgrid">
            <div v-for="i in 3" :key="i" class="shimmer-card">
              <div class="shimmer-line h20 w30"></div>
              <div class="shimmer-line h60 w100"></div>
            </div>
          </div>
        </div>

        <!-- Result View -->
        <div v-else class="result-scroll">
          <div class="viewport-header">
            <div class="title-wrap">
              <h2>{{ parsedData.title }}</h2>
              <div class="meta">解析完成 · {{ parsedData.blocks }} 个结构块 · 识别字段 {{ parsedData.fields }} 个</div>
            </div>
            <div class="status-badge">
              <el-icon>
                <CircleCheckFilled />
              </el-icon> 解析结果已就绪
            </div>
          </div>

          <div class="dashboard-grid">
            <!-- 01 试验目的 -->
            <div class="sc-panel">
              <div class="corner tl"></div>
              <div class="corner tr"></div>
              <div class="sc-head">
                <el-icon class="s-ico purple">
                  <Aim />
                </el-icon>
                <div class="s-title">
                  <h3>试验目的</h3>
                  <span>评估对象与核心意图</span>
                </div>
                <div class="tag-extracted">已提取</div>
              </div>
              <div class="sc-content">
                <div class="kv-grid">
                  <div class="kv-row"><span class="k">评估类型</span><span class="v highlight">效能评估与定型鉴定</span></div>
                  <div class="kv-row"><span class="k">评估对象</span><span class="v">某型综合电子对抗系统 (ECM-P200)</span></div>
                  <div class="kv-row"><span class="k">主要目标</span><span class="v">干扰效能、侦察能力及响应时序综合效能鉴定评估</span></div>
                  <div class="kv-row"><span class="k">评估依据</span><span class="v">战术技术指标要求、GJB 1234.5-20XX、技术协议书</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 02 进入条件 -->
            <div class="sc-panel">
              <div class="corner tl"></div>
              <div class="corner tr"></div>
              <div class="sc-head">
                <el-icon class="s-ico cyan">
                  <Key />
                </el-icon>
                <div class="s-title">
                  <h3>进入条件</h3>
                  <span>试验启动前置要求</span>
                </div>
                <div class="tag-extracted">已提取</div>
              </div>
              <div class="sc-content">
                <div class="technical-text-block">
                  本试验评估须在以下条件全部满足后方可正式启动：受试装备已完成地面联调，天馈线系统安装稳固且驻波比符合要求；靶场地面信号模拟源完成校准，输出电平及调制特性满足侦察干扰测试门限；试验大纲、测试大纲和评价大纲均已通过专家评审并获得批准；参试人员完成电磁环境复杂度评估培训，具备复杂场景操作资质；天候条件良好，不影响微波链路传输；相关安全保密措施已落实到位，试验区域已完成非参试电磁信号清场。
                </div>
              </div>
            </div>

            <!-- 03 试验内容与时间 -->
            <div class="sc-panel full">
              <div class="corner tl"></div>
              <div class="corner tr"></div>
              <div class="sc-head">
                <el-icon class="s-ico green">
                  <Stopwatch />
                </el-icon>
                <div class="s-title">
                  <h3>试验内容与时间安排</h3>
                  <span>试验科目层级结构及对应时间范围</span>
                </div>
                <div class="tag-extracted">已提取 · 4大项 · 13子项</div>
              </div>
              <div class="sc-content">
                <div class="content-tree">
                  <div v-for="item in parsedData.contentTree" :key="item.id" class="major-item">
                    <div class="major-head" @click="toggleTreeItem(item.id)">
                      <el-icon class="arr" :class="{ open: treeOpen[item.id] }">
                        <CaretRight />
                      </el-icon>
                      <span class="label">{{ item.label }}</span>
                      <span class="meta">{{ item.childrenCount }} 子项 · {{ item.timeRange }}</span>
                    </div>
                    <el-collapse-transition>
                      <div v-show="treeOpen[item.id]" class="sub-items">
                        <div v-for="sub in item.children" :key="sub.id" class="sub-row">
                          <div class="sub-id">{{ sub.id }}</div>
                          <div class="sub-name">{{ sub.name }}</div>
                          <div class="sub-time">{{ sub.timeRange }}</div>
                          <div class="sub-days">{{ sub.duration }}</div>
                        </div>
                      </div>
                    </el-collapse-transition>
                  </div>
                </div>
              </div>
            </div>

            <!-- 04 考核指标 -->
            <div class="sc-panel full">
              <div class="corner tl"></div>
              <div class="corner tr"></div>
              <div class="sc-head">
                <el-icon class="s-ico pink">
                  <DataLine />
                </el-icon>
                <div class="s-title">
                  <h3>考核指标</h3>
                  <span>多层级指标体系 · 叶节点含设计要求与量纲</span>
                </div>
                <div class="tag-extracted">已提取 · 14 项</div>
              </div>
              <div class="sc-content">
                <div class="tactical-table-wrap">
                  <table class="tactical-table">
                    <thead>
                      <tr>
                        <th>一级指标</th>
                        <th>二级指标</th>
                        <th>具体指标</th>
                        <th>设计要求</th>
                        <th>量纲</th>
                        <th>对应试验项</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template v-for="(l1, idx1) in indicatorTable" :key="idx1">
                        <template v-for="(l2, idx2) in l1.children" :key="idx2">
                          <tr v-for="(l3, idx3) in l2.children" :key="idx3">
                            <td v-if="idx2 === 0 && idx3 === 0" :rowspan="l1.totalRow" class="l1-cell">{{ l1.name }}
                            </td>
                            <td v-if="idx3 === 0" :rowspan="l2.children.length" class="l2-cell">{{ l2.name }}</td>
                            <td class="l3-cell">{{ l3.name }}</td>
                            <td class="req-cell">{{ l3.req }}</td>
                            <td class="unit-cell">{{ l3.unit }}</td>
                            <td class="ref-cell">{{ l3.ref }}</td>
                          </tr>
                        </template>
                      </template>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- 05 评价准则 -->
            <div class="sc-panel full">
              <div class="corner tl"></div>
              <div class="corner tr"></div>
              <div class="sc-head">
                <el-icon class="s-ico orange">
                  <PieChart />
                </el-icon>
                <div class="s-title">
                  <h3>评价准则</h3>
                  <span>综合评价标准、等级划分与方法说明</span>
                </div>
                <div class="tag-extracted">已提取</div>
              </div>
              <div class="sc-content">
                <div class="eval-intro">
                  本次试验评估采用定量评价与定性评价相结合的综合评估方法，以战术技术指标为基准，以真实使用场景中的实测数据为核心依据。评价结论分为
                  <span class="stat pass">合格</span>、<span class="stat warn">基本合格</span>、<span
                    class="stat fail">不合格</span>
                  三个等级。凡关键否决指标未达标，综合结论直接判定为不合格。
                </div>

                <div class="grade-section">
                  <div class="grade-header"><el-icon>
                      <TrendCharts />
                    </el-icon> 各试验大项评价等级及评估分值</div>
                  <div class="grade-table-wrap">
                    <table class="grade-table">
                      <thead>
                        <tr>
                          <th style="width: 25%">试验大项</th>
                          <th>
                            <div class="g-cell pass">合格 (≥ 85)</div>
                          </th>
                          <th>
                            <div class="g-cell warn">基本合格 (70-84)</div>
                          </th>
                          <th>
                            <div class="g-cell fail">不合格 (&lt; 70)</div>
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="g in gradeList" :key="g.name">
                          <td class="row-label">{{ g.name }}</td>
                          <td>
                            <div class="cell-content">{{ g.pass }}</div>
                          </td>
                          <td>
                            <div class="cell-content">{{ g.warn }}</div>
                          </td>
                          <td>
                            <div class="cell-content">{{ g.fail }}</div>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <div class="method-section">
                  <div class="grade-header"><el-icon>
                      <Management />
                    </el-icon> 评价方法说明</div>
                  <div class="methods-grid">
                    <div class="method-card" v-for="m in methods" :key="m.label">
                      <span class="m-lbl">{{ m.label }}</span>
                      <p class="m-val">{{ m.val }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- Bottom Action Bar -->
    <footer class="tactical-footer">
      <div class="footer-left">
        <span v-if="fileInfo" class="active-file">当前处于待确认状态: <strong>{{ fileInfo.name }}</strong></span>
      </div>
      <div class="footer-right">
        <button class="btn ghost" :disabled="!isReady" @click="handleManualGenerate">
          <el-icon>
            <EditPen />
          </el-icon> 手动生成需求
        </button>
        <button class="btn primary-gradient" :disabled="!isReady" @click="proceedNext">
          <el-icon>
            <MagicStick />
          </el-icon> 匹配任务骨架推进
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Files, UploadFilled, Document, Close, ArrowRight, Memo,
  CircleCheckFilled, Aim, Key, Stopwatch, DataLine, PieChart,
  CaretRight, TrendCharts, Management, EditPen, MagicStick
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

// Design States
const isDragover = ref(false)
const fileInfo = ref(null)
const isLoading = ref(false)
const isReady = ref(false)
const progress = ref(0)
const parsingMsg = ref('等待文件上传...')

const treeOpen = ref({ A: true, B: true, C: true, D: true })

const toggleTreeItem = (id) => {
  treeOpen.value[id] = !treeOpen.value[id]
}

// 5-Stage Status steps
const statusSteps = ref([
  { id: 1, label: '文件结构化读取', state: 'waiting' },
  { id: 2, label: '语义特征结构识别', state: 'waiting' },
  { id: 3, label: '关键业务字段提取', state: 'waiting' },
  { id: 4, label: '三纲指标参数关联', state: 'waiting' },
  { id: 5, label: '解析完整度验证', state: 'waiting' }
])

// Mock Parsed Data (Ported from v4 HTML)
const parsedData = ref({
  title: '某型通信对抗装备综合效能评估实施方案',
  blocks: 5,
  fields: 38,
  contentTree: [
    {
      id: 'A', label: '侦察搜索性能试验', childrenCount: 3, timeRange: '2024-11-16 ~ 2025-01-05', children: [
        { id: 'A1', name: '全频段信号截获与识别测试', timeRange: '2024-11-16 ~ 12-05', duration: '20天' },
        { id: 'A2', name: '精细化参数提取与指纹解析', timeRange: '2024-12-06 ~ 12-20', duration: '15天' },
        { id: 'A3', name: '多源融合测向定位精度评估', timeRange: '2024-12-21 ~ 01-05', duration: '16天' }
      ]
    },
    {
      id: 'B', label: '干扰压制效能试验', childrenCount: 3, timeRange: '2025-01-06 ~ 2025-02-05', children: [
        { id: 'B1', name: '宽带压制干扰性能专项试验', timeRange: '01-06 ~ 01-18', duration: '13天' },
        { id: 'B2', name: '典型协议点对点欺骗干扰测试', timeRange: '01-19 ~ 01-28', duration: '10天' }
      ]
    }
  ]
})

const indicatorTable = [
  {
    name: '干扰效能',
    totalRow: 4,
    children: [
      {
        name: '干扰覆盖性', children: [
          { name: '压制干扰覆盖率', req: '≥ 80.0', unit: '%', ref: 'B1 压制干扰试验' },
          { name: '频段覆盖完整度', req: '≥ 95.0', unit: '%', ref: 'B1 压制干扰试验' }
        ]
      },
      {
        name: '干扰质量', children: [
          { name: 'J/S 有效干信比', req: '≥ 15', unit: 'dB', ref: 'B2 欺骗干扰测试' },
          { name: '响应时延 (Latency)', req: '≤ 0.5', unit: 's', ref: 'B2 欺骗干扰测试' }
        ]
      }
    ]
  },
  {
    name: '侦察能力',
    totalRow: 4,
    children: [
      {
        name: '信号识别项', children: [
          { name: '全频段截获率', req: '≥ 90', unit: '%', ref: 'A1 信号截获测试' },
          { name: '调制方式识别率', req: '≥ 85', unit: '%', ref: 'A1 信号截获测试' }
        ]
      },
      {
        name: '定位精度', children: [
          { name: '测向精度 RMSE', req: '≤ 2.0', unit: '°', ref: 'A3 定位精度评估' },
          { name: '多站融合定位 CEP', req: '≤ 500', unit: 'm', ref: 'A3 定位精度评估' }
        ]
      }
    ]
  }
]

const gradeList = [
  { name: 'A · 侦察搜索性能试验', pass: '截获率≥85%, RMSE≤2°', warn: '截获率≥70%, RMSE≤5°', fail: '截获率<70%, RMSE>5°' },
  { name: 'B · 干扰压制效能试验', pass: '覆盖率≥80%, J/S≥门限', threshold: '覆盖率≥65%, J/S接阈', fail: '覆盖率<65%, J/S失准' }
]

const methods = [
  { label: '定量评价', val: '以实测数据与设计指标的比值为基础，计算各叶节点指标的达标率；关键指标权重系数 1.5。' },
  { label: '综合评价', val: '采用层次分析法（AHP）建立评价模型：技术性能 40%、作战效能 35%、使用适用性 25%。' },
  { label: '否决项', val: '全色分辨率、无控定位精度、系统可用度三项为否决项。' }
]

// Logic Functions
const handleFileSelect = (e) => {
  const file = e.target.files[0]
  if (file) startAnalysis(file)
}

const handleDrop = (e) => {
  isDragover.value = false
  const file = e.dataTransfer.files[0]
  if (file) startAnalysis(file)
}

const startAnalysis = (file) => {
  fileInfo.value = file
  isLoading.value = true
  isReady.value = false
  progress.value = 0

  // Simulate 5nd stage parsing
  const steps = [
    { ms: 0, msg: '正在读取物理文件结构...' },
    { ms: 600, msg: '正在执行语义特征识别...' },
    { ms: 1400, msg: '正在提取关键业务字段...' },
    { ms: 2200, msg: '正在构建三纲指标关联网络...' },
    { ms: 2800, msg: '验证解析结果完整度...' }
  ]

  steps.forEach((s, i) => {
    setTimeout(() => {
      parsingMsg.value = s.msg
      statusSteps.value[i].state = 'active'
      if (i > 0) statusSteps.value[i - 1].state = 'done'
      progress.value = (i + 1) * 20
    }, s.ms)
  })

  setTimeout(() => {
    statusSteps.value[4].state = 'done'
    parsingMsg.value = '解析完成'
    isLoading.value = false
    isReady.value = true
    ElMessage.success('一案三纲文件解析成功')
  }, 3300)
}

const resetUpload = () => {
  fileInfo.value = null
  isReady.value = false
  statusSteps.value.forEach(s => s.state = 'waiting')
}

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const handleManualGenerate = () => {
  router.push('/major/requirement-sys/generate')
}

const proceedNext = () => {
  router.push('/major/requirement-sys/skeleton-match')
}
</script>

<style scoped lang="scss">
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;700&family=Inter:wght@400;600;700&display=swap');

// Design Tokens (Orbital Curator)
$surface: #10131a;
$surface-low: #191c22;
$surface-high: #272a31;
$surface-highest: #32353c;
$primary: #a4e6ff;
$primary-container: #00d1ff;
$success: #00ff9d;
$warn: #ffbd2e;
$danger: #ff3c5b;

.import-workspace {
  height: 100%;
  background: var(--bg-color);
  color: var(--text-color-primary);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Inter', sans-serif;
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
  box-shadow: var(--box-shadow-neon, 0 1px 12px rgba(0, 209, 255, 0.05));


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
    }

    .divider {
      width: 1px;
      height: 16px;
      background: var(--border-color);
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

      &.active {
        background: color-mix(in srgb, var(--primary-color) 10%, transparent);
        color: $primary-container;
        border-radius: 2px;
      }
    }
  }
}

/* ─── Body ─── */
.tactical-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ─── Sidebar ─── */
.analysis-sidebar {
  width: 300px;
  background: var(--sider-bg-color);
  flex-shrink: 0;
  border-right: 1px solid var(--border-color);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 32px;

  .sidebar-section {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .label-tech {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 11px;
    font-weight: 700;
    color: var(--text-color-secondary);
    letter-spacing: 2px;
  }
}

.upload-zone {
  border: 1.5px dashed color-mix(in srgb, var(--primary-color) 20%, transparent);
  // [MODIFIED] Use card bg
  background: var(--card-bg-color);
  border-radius: 8px;
  padding: 30px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: 0.3s;
  position: relative;

  &:hover,
  &.is-dragover {
    background: color-mix(in srgb, var(--primary-color) 5%, transparent);
    border-color: var(--primary-color);
    box-shadow: var(--box-shadow-neon, 0 0 16px rgba(0, 209, 255, 0.1));
  
  }

  &.has-file {
    border-style: solid;
    padding: 16px;
    background: color-mix(in srgb, var(--success-color) 2%, transparent);
  }

  .hidden-input {
    position: absolute;
    inset: 0;
    opacity: 0;
    cursor: pointer;
  }

  .up-ico {
    font-size: 32px;
    color: var(--primary-color);
  }

  .up-text {
    font-size: 13px;
    color: var(--text-color-secondary);

    strong {
      color: var(--primary-color);
    }
  }

  .up-hint {
    font-size: 10px;
    color: var(--text-color-secondary);
  }
}

.file-card {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;

  .f-ico {
    font-size: 24px;
    color: var(--primary-color);
  }

  .f-meta {
    flex: 1;
    min-width: 0;

    .f-name {
      font-size: 12px;
      font-weight: 700;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      color: var(--text-color-primary);
    }

    .f-size {
      font-size: 10px;
      color: var(--text-color-secondary);
      opacity: 0.5;
    }
  }

  .f-del {
    font-size: 16px;
    color: var(--text-color-secondary);
    cursor: pointer;

    &:hover {
      color: var(--danger-color);
    }
  }
}

.parsing-progress {
  margin-top: 8px;

  .track {
    height: 4px;
    border-radius: 2px;
    background: var(--border-color);
    overflow: hidden;

    .bar {
      height: 100%;
      background: linear-gradient(90deg, var(--primary-color), var(--success-color));
      transition: 0.3s;
    }
  }

  .stats {
    display: flex;
    justify-content: space-between;
    font-size: 10px;
    color: var(--text-color-secondary);
    margin-top: 6px;
  }
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .status-item {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 11.5px;
    color: var(--text-color-secondary);

    .s-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--border-color);
    }

    &.active {
      color: var(--primary-color);

      .s-dot {
        background: var(--primary-color);
        box-shadow: 0 0 8px var(--primary-color);
        animation: pulse-glow 1s infinite;
      }
    }

    &.done {
      color: var(--success-color);
      opacity: 1;

      .s-dot {
        background: var(--success-color);
      }
    }
  }
}

@keyframes pulse-glow {
  0% {
    transform: scale(1);
    opacity: 1;
  }

  50% {
    transform: scale(1.5);
    opacity: 0.5;
  }

  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.info-box {
  background: var(--card-bg-color);
  border-radius: 8px;
  padding: 16px;

  p {
    font-size: 11px;
    line-height: 1.6;
    color: var(--text-color-secondary);
    margin: 0;
  }

  .tech-link {
    margin-top: 12px;
    font-size: 11px;
    font-weight: 700;
    color: var(--primary-color);
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}

/* ─── Main Workspace ─── */
.analysis-workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  background-image: radial-gradient(circle at 10% 10%, color-mix(in srgb, var(--primary-color) 2%, transparent) 0%, transparent 40%);
}

.empty-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-color-secondary);

  .p-icon {
    font-size: 64px;
  }

  h3 {
    font-size: 20px;
    color: var(--text-color-primary);
  }

  p {
    font-size: 13px;
    max-width: 320px;
    text-align: center;
    line-height: 1.6;
    color: var(--text-color-secondary);
    opacity: 0.5;
  }
}

.result-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px 100px;
}

.viewport-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;

  h2 {
    font-size: 24px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: var(--text-color-primary);
  }

  .meta {
    font-size: 12px;
    color: var(--text-color-secondary);
    margin-top: 6px;
  }

  .status-badge {
    display: flex;
    align-items: center;
    gap: 8px;
    background: color-mix(in srgb, var(--success-color) 5%, transparent);
    border: 1px solid color-mix(in srgb, var(--success-color) 20%, transparent);
    padding: 6px 16px;
    border-radius: 20px;
    font-size: 12px;
    color: var(--success-color);
    font-weight: 700;
  }
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

/* ─── Tonal Panels ─── */
.sc-panel {
  background: var(--card-bg-color);
  // [MODIFIED] Use border color
  border: 1px solid var(--border-color);
  border-radius: 4px;
  position: relative;
  overflow: hidden;
  transition: 0.3s;

  &:hover {
    background: color-mix(in srgb, var(--card-bg-color) 95%, white); 
    border-color: var(--border-color-hover);
  }

  &.full {
    grid-column: span 2;
  }

  .corner {
    position: absolute;
    width: 8px;
    height: 8px;
    border: 2px solid var(--primary-color);

    &.tl {
      top: 0;
      left: 0;
      border-right: none;
      border-bottom: none;
    }

    &.tr {
      top: 0;
      right: 0;
      border-left: none;
      border-bottom: none;
    }
  }

  .sc-head {
    padding: 16px 20px;
    background: color-mix(in srgb, var(--card-bg-color) 80%, transparent);
    border-bottom: 1px solid var(--border-color);
    display: flex;
    align-items: center;
    gap: 12px;

    .s-ico {
      width: 40px;
      height: 40px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;

      &.purple {
        background: color-mix(in srgb, var(--info-color) 10%, transparent);
        color: var(--info-color);
      }
      &.cyan {
        background: color-mix(in srgb, var(--primary-color) 10%, transparent);
        color: var(--primary-color);
      }
      &.green {
        background: color-mix(in srgb, var(--success-color) 10%, transparent);
        color: var(--success-color);
      }
      &.pink {
        background: color-mix(in srgb, var(--danger-color) 10%, transparent);
        color: var(--danger-color);
      }
      &.orange {
        background: color-mix(in srgb, var(--warning-color) 10%, transparent);
        color: var(--warning-color);
      }
    }

    .s-title {
      flex: 1;

      h3 {
        font-size: 14px;
        font-weight: 700;
        margin: 0;
        color: var(--text-color-primary);
      }

      span {
        font-size: 11px;
        color: var(--text-color-secondary);
        margin-top: 2px;
      }
    }

    .tag-extracted {
      font-size: 10px;
      font-weight: 700;
      color: var(--success-color);
      background: color-mix(in srgb, var(--success-color) 10%, transparent);
      padding: 3px 10px;
      border-radius: 2px;
    }
  }

  .sc-content {
    padding: 24px;
  }
}

/* ─── KV Grid ─── */
.kv-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .kv-row {
    display: flex;
    gap: 16px;
    font-size: 13px;

    .k {
      width: 70px;
      color: var(--text-color-secondary);
      font-size: 11px;
      font-weight: 700;
      margin-top: 2px;
    }

    .v {
      flex: 1;
      color: var(--text-color-primary);
      line-height: 1.6;

      &.highlight {
        color: var(--primary-color);
        font-weight: 700;
      }
    }
  }
}

.technical-text-block {
  font-size: 13px;
  line-height: 2;
  color: var(--text-color-primary);
  padding: 20px;
  background: var(--card-bg-color);
  border-left: 3px solid var(--primary-color);
  border-radius: 0 4px 4px 0;
}

/* ─── Content Tree ─── */
.content-tree {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .major-item {
    background: var(--card-bg-color);
    border-radius: 4px;
    border: 1px solid var(--border-color);

    .major-head {
      padding: 14px 20px;
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;

      &:hover {
        background: color-mix(in srgb, var(--primary-color) 5%, transparent);
      }

      .arr {
        transition: 0.3s;
        color: var(--primary-color);

        &.open {
          transform: rotate(90deg);
        }
      }

      .label {
        font-size: 14px;
        font-weight: 700;
        flex: 1;
        color: var(--text-color-primary);
      }

      .meta {
        font-size: 11px;
        color: var(--text-color-secondary);
      }
    }
  }

  .sub-items {
    padding: 12px 20px 20px 48px;
    display: flex;
    flex-direction: column;
    gap: 1px;
    background: rgba(0, 0, 0, 0.1);

    .sub-row {
      display: flex;
      align-items: center;
      padding: 10px 0;
      border-bottom: 1px solid var(--border-color);

      &:last-child {
        border-bottom: none;
      }

      .sub-id {
        width: 40px;
        font-size: 11px;
        font-weight: 700;
        color: $primary;
      }

      .sub-name {
        flex: 1;
        font-size: 13px;
        color: var(--text-color-primary);
        opacity: 0.8;
      }

      .sub-time {
        width: 140px;
        font-size: 11px;
        color: var(--warning-color);
        text-align: center;
      }

      .sub-days {
        width: 60px;
        font-size: 11px;
        color: var(--text-color-secondary);
        text-align: right;
      }
    }
  }
}

/* ─── Tactical Table ─── */
.tactical-table-wrap {
  border-radius: 4px;
  overflow: hidden;
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
}

.tactical-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;

  th {
    background: color-mix(in srgb, var(--card-bg-color) 90%, transparent);
    // [MODIFIED] Use secondary text
    color: var(--text-color-secondary);
    font-size: 10px;
    font-weight: 700;
    text-transform: uppercase;
    padding: 12px 16px;
    text-align: left;
  }

  td {
    padding: 16px;
    border-bottom: 1px solid var(--border-color);
    line-height: 1.5;
    vertical-align: middle;
  }

  .l1-cell {
    background: color-mix(in srgb, var(--primary-color) 5%, transparent);
    color: var(--primary-color);
    font-weight: 700;
    text-align: center;
    writing-mode: vertical-lr;
  }

  .l2-cell {
    color: var(--info-color); 
    font-weight: 600;
    background: var(--card-bg-color);
    width: 120px;
  }

  .l3-cell {
    color: var(--text-color-primary);
    opacity: 0.9;
  }

  .req-cell {
    color: var(--primary-color);
    font-weight: 700;
    text-align: center;
    font-family: monospace;
    font-size: 13px;
    width: 100px;
  }

  .unit-cell {
    color: var(--text-color-secondary);
    text-align: center;
    width: 60px;
  }

  .ref-cell {
    color: var(--warning-color);
    font-size: 11px;
    width: 140px;
  }
}

/* ─── Evaluation Section ─── */
.eval-intro {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-color-primary);
  margin-bottom: 32px;
  padding: 20px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;

  .stat {
    font-weight: 700;
    margin: 0 4px;

    &.pass { color: var(--success-color); }
    &.warn { color: var(--warning-color); }
    &.fail { color: var(--danger-color); }
  }
}

.grade-header {
  font-size: 12px;
  font-weight: 700;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, color-mix(in srgb, var(--primary-color) 20%, transparent), transparent);
  }
}

.grade-table-wrap {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  padding: 1px;
  border: 1px solid var(--border-color);
}

.grade-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 1px;

  th {
    padding: 12px;
    font-size: 10px;
    color: var(--text-color-secondary);
    opacity: 0.5;
    text-align: center;
    // [MODIFIED] Use card bg
    background: var(--card-bg-color);
  }

  td {
    padding: 16px;
    text-align: center;
    background: var(--card-bg-color);
  }

  .row-label {
    text-align: left;
    font-size: 12px;
    font-weight: 700;
    width: 220px;
    color: var(--text-color-primary);
  }

  .g-cell {
    padding: 4px 12px;
    border-radius: 20px;
    display: inline-block;
    font-size: 10px;
    font-weight: 700;

    &.pass {
      background: color-mix(in srgb, var(--success-color) 10%, transparent);
      color: var(--success-color);
    }
    &.warn {
      background: color-mix(in srgb, var(--warning-color) 10%, transparent);
      color: var(--warning-color);
    }
    &.fail {
      background: color-mix(in srgb, var(--danger-color) 10%, transparent);
      color: var(--danger-color);
    }
  }

  .cell-content {
    font-size: 11px;
    color: var(--text-color-secondary);
    line-height: 1.6;
  }
}

.method-section {
  margin-top: 32px;
}

.methods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  .method-card {
    background: var(--card-bg-color);
    padding: 16px 20px;
    border-radius: 4px;
    border: 1px solid var(--border-color);

    .m-lbl {
      font-size: 10px;
      font-weight: 900;
      color: var(--primary-color);
      margin-bottom: 8px;
      display: block;
    }

    .m-val {
      font-size: 12px;
      color: var(--text-color-secondary);
      line-height: 1.6;
      margin: 0;
    }
  }
}

/* ─── Footer ─── */
.tactical-footer {
  height: 72px;
  background: color-mix(in srgb, var(--sider-bg-color) 95%, transparent);
  backdrop-filter: var(--backdrop-filter);
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.active-file {
  font-size: 12px;
  color: var(--text-color-secondary);

  strong {
    color: var(--primary-color);
  }
}

.btn {
  padding: 10px 24px;
  border-radius: 4px;
  border: none;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 8px;

  &:disabled {
    opacity: 0.2;
    cursor: not-allowed;
  }

  &.ghost {
    background: transparent;
    border: 1px solid color-mix(in srgb, var(--primary-color) 20%, transparent);
    color: var(--primary-color);
    &:hover:not(:disabled) {
      background: color-mix(in srgb, var(--primary-color) 5%, transparent);
    }
  }

  &.primary-gradient {
    background: var(--primary-color); 
    color: var(--primary-text);
    box-shadow: var(--box-shadow-neon, 0 4px 12px rgba(0, 0, 0, 0.2));

    &:hover:not(:disabled) {
      box-shadow: 0 6px 16px color-mix(in srgb, var(--primary-color) 40%, transparent);
      transform: translateY(-1px);
      filter: brightness(1.1);
    }
  }
}

/* ─── Shimmer Animation ─── */
.shimmer-line {
  background: linear-gradient(90deg, 
    color-mix(in srgb, var(--border-color) 30%, transparent) 25%, 
    color-mix(in srgb, var(--border-color) 50%, transparent) 50%, 
    color-mix(in srgb, var(--border-color) 30%, transparent) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite linear;
  border-radius: 4px;
  margin-bottom: 12px;

  &.h30 {
    height: 30px;
  }

  &.h20 {
    height: 20px;
  }

  &.h14 {
    height: 14px;
  }

  &.h60 {
    height: 60px;
  }

  &.w100 {
    width: 100%;
  }

  &.w40 {
    width: 40%;
  }

  &.w30 {
    width: 30%;
  }

  &.w20 {
    width: 20%;
  }
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

/* Scrollbar Customization */
::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--border-color) 50%, transparent);
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--primary-color);
}
</style>
