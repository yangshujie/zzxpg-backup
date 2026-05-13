<template>
  <div class="generate-req-page complete-req-page">
    <!-- Requirement Viewing Container -->
    <div class="stage-two-container">
      <!-- Loading Overlay -->
      <div v-if="isLoading" class="generating-overlay">
        <RobotAnimation />
        <div class="generating-text">正在加载需求详情...</div>
      </div>

      <!-- Main Two-Pane Layout -->
      <div v-else class="requirement-layout">
        <!-- ════ Left Pane: Requirement Elements (Read-only) ════ -->
        <div class="left-pane" :class="{ collapsed: isLeftPaneCollapsed }">
          <div class="lp-head">
            <el-icon class="lp-collapse-btn" @click="isLeftPaneCollapsed = !isLeftPaneCollapsed">
              <Expand v-if="isLeftPaneCollapsed" />
              <Fold v-else />
            </el-icon>
            <span class="lp-head-title" v-if="!isLeftPaneCollapsed">需求详情预览</span>
            <div class="lp-progress" v-if="!isLeftPaneCollapsed">
              <div class="lp-prog-bar">
                <div class="lp-prog-fill" :style="{ width: completionRate + '%' }"></div>
              </div>
              <span class="lp-prog-val">{{ completionRate }}%</span>
            </div>
          </div>

          <div class="lp-scroll" v-if="!isLeftPaneCollapsed">
            <!-- 待评装备 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--jam);"></span>待评装备</div>
              <div class="tag-editor readonly">
                <div class="tag" v-for="(tag, idx) in formData.equips" :key="idx"
                  style="background:var(--jam-bg);color:var(--jam);border:1px solid var(--jam-border);">
                  {{ tag }}
                </div>
                <div v-if="!formData.equips.length" class="empty-text">无记录</div>
              </div>
            </div>

            <!-- 评估目标 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent);"></span>评估目标</div>
              <div class="tag-editor readonly">
                <div class="tag" v-for="(tag, idx) in formData.targets" :key="idx"
                  style="background:rgba(47,129,247,.1);color:var(--accent);border:1px solid rgba(47,129,247,.28);">
                  {{ tag }}
                </div>
                <div v-if="!formData.targets.length" class="empty-text">无记录</div>
              </div>
            </div>

            <!-- 陪试装备 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:#58a6ff;"></span>陪试装备</div>
              <div class="tag-editor readonly">
                <div class="tag" v-for="(tag, idx) in formData.supportEquip" :key="idx"
                  style="background:rgba(88,166,255,.1);color:#58a6ff;border:1px solid rgba(88,166,255,.28);">
                  {{ tag }}
                </div>
                <div v-if="!formData.supportEquip.length" class="empty-text">无记录</div>
              </div>
            </div>

            <!-- 试验类型 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:#bc8cff;"></span>试验类型</div>
              <div class="tag-editor readonly">
                <div class="tag" v-for="(tag, idx) in formData.testType" :key="idx"
                  style="background:rgba(188,140,255,.1);color:#bc8cff;border:1px solid rgba(188,140,255,.28);">
                  {{ tag }}
                </div>
                <div v-if="!formData.testType.length" class="empty-text">无记录</div>
              </div>
            </div>

            <!-- 评估目的 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent2);"></span>评估目的</div>
              <div class="technical-text-block">{{ formData.purpose || '未填写' }}</div>
            </div>

            <div class="fg-divider"></div>

            <!-- 评估类型 & 级别 -->
            <div class="fg-row">
              <div class="fg half">
                <div class="fg-label">评估类型</div>
                <div class="pill-row">
                  <div v-for="opt in ['效能', '贡献率', '成熟度', '适用性']" :key="opt" class="pill readonly"
                    :class="{ on: formData.evalType === opt }">{{ opt }}</div>
                </div>
              </div>
              <div class="fg half">
                <div class="fg-label">评估级别</div>
                <div class="pill-row">
                  <div v-for="opt in ['装备级', '系统级', '体系级', '任务级']" :key="opt" class="pill readonly"
                    :class="{ 'on-b': formData.evalLevel === opt }">{{ opt }}</div>
                </div>
              </div>
            </div>

            <!-- 评估要求 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent3);"></span>评估要求</div>
              <div class="technical-text-block">{{ formData.evalReq || '未填写' }}</div>
            </div>

            <!-- 关联指标体系 -->
            <div class="fg">
              <div class="fg-label">关联指标体系</div>
              <div class="technical-text-block">{{ selectedIndicatorName || '未关联' }}</div>
            </div>
          </div>
        </div>

        <!-- ════ Right Pane: Indicator Visualization (Read-only) ════ -->
        <div class="right-pane">
          <IndicatorSystemEditor v-model:tree-data="indicatorTree" readonly :lib-collapsed="true" />
        </div>
      </div>

      <!-- Footer Actions -->
      <div v-if="!isLoading" class="stage-footer" :style="{ left: isLeftPaneCollapsed ? '40px' : '480px' }">
        <div class="footer-left">
          <span class="status-indicator">
            <el-icon>
              <InfoFilled />
            </el-icon> 需求预览模式：内容已锁定
          </span>
        </div>
        <div class="tb-actions">
          <el-button class="tb-btn tb-blue" @click="returnToEdit">↩ 返回编辑</el-button>
          <el-button class="tb-btn tb-send" @click="handleDistribute">确认并下发需求 →</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Expand, Fold, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import RobotAnimation from '@/components/RobotAnimation.vue'
import IndicatorSystemEditor from './IndicatorSystemEditor.vue'
import { getEvaluationRequirement, analysisSendTree, broadcastIndicatorTree } from '@/api/requirements'
import { getIndicatorSystem } from '@/api/xiaoyang'

const router = useRouter()
const route = useRoute()

// ─── UI State ───
const isLeftPaneCollapsed = ref(false)
const isLoading = ref(true)
const completionRate = ref(0)
const requirementId = ref(null)
const indicatorTree = ref([])
const selectedIndicatorName = ref('')

// ─── Data ───
const formData = ref({
  equips: [],
  targets: [],
  purpose: '',
  evalType: '效能',
  evalLevel: '系统级',
  evalReq: '',
  supportEquip: [],
  testType: []
})

// ─── Methods ───
const loadRequirement = async (id) => {
  if (!id) return
  isLoading.value = true
  try {
    const res = await getEvaluationRequirement(id)
    if (res.code === 200 && res.data) {
      const data = res.data
      requirementId.value = data.requirementId || data.id

      // Form Mapping
      formData.value.equips = data.equipList || []
      formData.value.supportEquip = data.supportEquip || []
      formData.value.targets = data.evaluationGoal || []
      formData.value.purpose = data.evaluationPurpose || ''
      formData.value.testType = data.testType || []
      formData.value.evalReq = data.evaluationRequirement || ''

      // Select indicator name if any
      if (data.indicatorSystemLabel) {
        selectedIndicatorName.value = data.indicatorSystemLabel
      } else {
        // Fallback or fetch labels if needed
        selectedIndicatorName.value = '已配置指标体系'
      }

      // Mapping levels/types
      const levelMap = { '1': '装备级', '2': '系统级', '3': '体系级', '4': '任务级' }
      formData.value.evalLevel = levelMap[data.evaluationLevel] || '系统级'
      const typeMap = { '1': '效能', '2': '贡献率', '3': '成熟度', '4': '适用性' }
      formData.value.evalType = typeMap[data.evaluationType] || '效能'

      // Tree Data
      if (data.treeData) {
        indicatorTree.value = [data.treeData]
      } else if (data.indicatorSystem) {
        try {
          indicatorTree.value = JSON.parse(data.indicatorSystem)
        } catch (e) { console.error(e) }
      }

      completionRate.value = data.progress || 100
    }
  } catch (err) {
    ElMessage.error('详情加载失败')
  } finally {
    isLoading.value = false
  }
}

const returnToEdit = () => {
  router.push({
    path: '/major/requirement-sys/generate',
    query: { id: requirementId.value }
  })
}

const handleDistribute = async () => {
  try {
    const res = await broadcastIndicatorTree(requirementId.value)
    if (res) {
      ElMessage.success('需求已下发')
      router.push({
        path: '/process/reception-sys/forms',
        query: { id: requirementId.value }
      })
    }
  } catch (err) {
    ElMessage.error('下发失败')
  }
}

onMounted(() => {
  const id = route.query.id
  if (id) {
    loadRequirement(id)
  } else {
    ElMessage.warning('未找到指定的需求ID')
    router.back()
  }
})
</script>

<style scoped lang="scss">
@import './_requirement-styles.scss'; // Reuse common workspace styles

.complete-req-page {
  --jam: var(--warning-color, #d29922);
  --jam-bg: color-mix(in srgb, var(--warning-color, #d29922) 10%, transparent);
  --jam-border: color-mix(in srgb, var(--warning-color, #d29922) 30%, transparent);
  
  --accent: var(--info-color, #2f81f7);
  --accent-bg: color-mix(in srgb, var(--info-color, #2f81f7) 10%, transparent);
  --accent-border: color-mix(in srgb, var(--info-color, #2f81f7) 30%, transparent);
  
  --accent2: var(--success-color, #3fb950);
  --accent3: var(--primary-color, #bc8cff); // Fallback to purple/pink if needed

  .tag-editor.readonly {
    padding: 8px 0;
    cursor: default;

    .tag {
      padding-right: 8px;
      // Inline styles in template will use the CSS variables defined above
    }
  }


  .technical-text-block {
    // [MODIFIED] Use CSS variables
    background: var(--card-bg-color, rgba(255, 255, 255, 0.03));
    border: 1px solid var(--border-color, #30363d);
    padding: 12px;
    border-radius: 4px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--text-color-primary, #e6edf3);
    min-height: 60px;
    white-space: pre-wrap;
  }

  .pill.readonly {
    cursor: default;
    opacity: 0.6;
    background: var(--card-bg-color, rgba(255,255,255,0.05));
    border: 1px solid var(--border-color, rgba(255,255,255,0.1));
    color: var(--text-color-secondary, #8b949e);

    &.on,
    &.on-b {
      opacity: 1;
      border-style: solid;
      background: var(--primary-color, #2f81f7);
      color: #fff; // Or use a dark text color variable if light theme
      border-color: var(--primary-color, #2f81f7);
    }
  }

  .status-indicator {
    font-size: 12px;
    color: var(--text-color-secondary, #8b949e);
    display: flex;
    align-items: center;
    gap: 6px;

    .el-icon {
      color: var(--info-color, #58a6ff);
      font-size: 14px;
    }
  }
  .empty-text {
    font-size: 12px;
    color: var(--text-color-placeholder, #484f58);
    font-style: italic;
  }
}

// Adjustments for readonly preview
.fg-label .req {
  display: none;
}
</style>
