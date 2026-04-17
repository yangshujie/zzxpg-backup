<template>
  <div class="skeleton-builder">
    <div class="builder-header">
      <div class="left-info">
        <el-button :icon="ArrowLeft" circle @click="router.back()" />
        <h2 class="title">骨架构建编辑器 <span class="sk-name">{{ skeletonData.name || '未命名骨架' }}</span></h2>
      </div>
      <div class="actions">
        <el-button type="info" plain :icon="View">预览需求</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave">保存草稿</el-button>
        <el-button type="success" :icon="Position" @click="handleSubmit">提交入库</el-button>
      </div>
    </div>

    <!-- 6-Step Navigation -->
    <div class="builder-steps">
      <div v-for="(step, index) in steps" :key="index" class="step-item"
        :class="{ active: currentStep === index, completed: currentStep > index }" @click="currentStep = index">
        <div class="step-num">{{ index + 1 }}</div>
        <div class="step-label">{{ step }}</div>
        <div v-if="index < steps.length - 1" class="step-line"></div>
      </div>
    </div>

    <div class="builder-content">
      <!-- Step 1: Base Information -->
      <div v-if="currentStep === 0" class="step-pane base-info">
        <div class="pane-card">
          <div class="section-title">基本信息配置</div>
          <el-form :model="skeletonData" label-width="100px" class="cyber-form">
            <div class="form-grid">
              <el-form-item label="骨架名称">
                <el-input v-model="skeletonData.name" placeholder="请输入骨架名称" />
              </el-form-item>
              <el-form-item label="骨架编号">
                <el-input v-model="skeletonData.id" disabled />
              </el-form-item>
              <el-form-item label="适用领域">
                <el-select v-model="skeletonData.domain" placeholder="选择领域" style="width: 100%">
                  <el-option label="防卫领域" value="defend" />
                  <el-option label="感知领域" value="sense" />
                </el-select>
              </el-form-item>
              <el-form-item label="评估层级">
                <el-checkbox-group v-model="skeletonData.levels">
                  <el-checkbox-button label="体系级" />
                  <el-checkbox-button label="系统级" />
                  <el-checkbox-button label="装备级" />
                </el-checkbox-group>
              </el-form-item>
            </div>
            <el-form-item label="骨架描述">
              <el-input type="textarea" v-model="skeletonData.desc" :rows="3" placeholder="描述该骨架的适用场景与任务目标..." />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- Step 2: Indicator Tree -->
      <div v-if="currentStep === 1" class="step-pane tree-editor">
        <div class="tree-toolbar">
          <el-button type="primary" size="small" :icon="Plus">添加根目标</el-button>
          <div class="legend">
            <span class="l-item goal">目标</span>
            <span class="l-item indicator">指标</span>
          </div>
        </div>
        <div class="tree-container">
          <el-tree :data="treeData" draggable default-expand-all :expand-on-click-node="false" class="cyber-tree">
            <template #default="{ node, data }">
              <div class="custom-node" :class="data.type">
                <span class="node-icon"></span>
                <span class="node-label">{{ node.label }}</span>
                <span v-if="data.weight" class="node-w">w={{ data.weight }}</span>
                <div class="node-ops">
                  <el-button link type="primary" :icon="Plus" v-if="data.type === 'goal'" />
                  <el-button link type="info" :icon="Setting" />
                  <el-button link type="danger" :icon="Delete" />
                </div>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- Step 3: Operator Binding -->
      <div v-if="currentStep === 2" class="step-pane operator-binding">
        <div class="binding-layout">
          <div class="indicator-select">
            <div class="section-title">选择指标</div>
            <div v-for="idx in flattenedIndicators" :key="idx.label" class="idx-item"
              :class="{ active: selectedIdx === idx.label }" @click="selectedIdx = idx.label">
              {{ idx.label }}
            </div>
          </div>
          <div class="binding-details">
            <div class="section-title">算子与数据配置 - {{ selectedIdx }}</div>
            <div class="binding-content">
              <div class="binding-card">
                <div class="card-row">
                  <span class="label">默认算子</span>
                  <el-select v-model="bindingData.algo" size="default" style="flex: 1">
                    <el-option label="时序统计算子" value="t-stat" />
                    <el-option label="差值统计算子" value="d-stat" />
                  </el-select>
                </div>
                <div class="card-row">
                  <span class="label">评估准则</span>
                  <el-input v-model="bindingData.criteria" size="default" placeholder="如：实测值 >= 阈值" />
                </div>
                <div class="card-row">
                  <span class="label">数据需求</span>
                  <div class="tags-container">
                    <el-tag v-for="d in bindingData.dataReqs" :key="d" closable class="data-tag">{{ d }}</el-tag>
                    <el-button size="small" :icon="Plus" circle />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 4: Branch Rules -->
      <div v-if="currentStep === 3" class="step-pane branch-rules">
        <div class="branch-tabs">
          <div v-for="cat in branchCats" :key="cat.id" class="branch-tab"
            :class="{ active: activeBranchCat === cat.id, [cat.id]: true }" @click="activeBranchCat = cat.id">
            {{ cat.name }}
          </div>
        </div>
        <div class="rules-list">
          <div class="rule-toolbar">
            <el-button type="primary" size="small" :icon="Plus">新建分支规则</el-button>
          </div>
          <div v-for="rule in filteredRules" :key="rule.id" class="rule-item">
            <div class="rule-info">
              <div class="rule-code">{{ rule.id }}</div>
              <div class="rule-name">{{ rule.name }}</div>
              <div class="rule-cond">触发：<span class="cond-text">{{ rule.cond }}</span></div>
            </div>
            <div class="rule-actions">
              <el-tag v-for="act in rule.actions" :key="act" size="small" effect="plain">{{ act }}</el-tag>
            </div>
            <div class="rule-ops">
              <el-button link type="primary" :icon="Setting" />
              <el-button link type="danger" :icon="Delete" />
            </div>
          </div>
        </div>
      </div>

      <!-- Step 5: Weight Distribution -->
      <div v-if="currentStep === 4" class="step-pane weight-config">
        <div class="weight-card">
          <div class="section-title">目标层权重 (Σ 1.0)</div>
          <div v-for="item in treeData" :key="item.label" class="weight-row">
            <span class="label">{{ item.label }}</span>
            <el-slider v-model="item.weight" :max="1" :step="0.01" class="slider" />
            <el-input-number v-model="item.weight" :min="0" :max="1" :step="0.01" size="small" />
          </div>
        </div>
      </div>

      <!-- Step 6: Valid & Submit -->
      <div v-if="currentStep === 5" class="step-pane validation">
        <div class="valid-list">
          <div v-for="v in validations" :key="v.name" class="valid-item" :class="v.status">
            <el-icon class="status-ico">
              <CircleCheck v-if="v.status === 'pass'" />
              <Warning v-else />
            </el-icon>
            <div class="info">
              <div class="v-nm">{{ v.name }}</div>
              <div class="v-dt">{{ v.detail }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Weight Bar -->
    <div class="weight-summary" v-if="currentStep === 4 || currentStep === 1">
      <div class="w-label">层级权重归一化</div>
      <div class="w-bar">
        <div class="w-seg" style="width: 35%; background: #9855e0">G1 0.35</div>
        <div class="w-seg" style="width: 30%; background: #3478f0">G2 0.30</div>
        <div class="w-seg" style="width: 20%; background: #e89a08">G3 0.20</div>
        <div class="w-seg" style="width: 15%; background: #0faa78">G4 0.15</div>
      </div>
      <div class="w-total">Σ 1.00 <el-icon>
          <Check />
        </el-icon></div>
      <el-button type="primary" size="small" plain>自动归一化</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft, View, Check, Position, Plus, Delete, Setting, CircleCheck, Warning
} from '@element-plus/icons-vue'

const router = useRouter()
const currentStep = ref(1)
const steps = ['填写基本信息', '构建核心指标树', '绑定算子与数据', '配置分支规则', '权重分配', '校验与提交']

// Step 3 Data
const selectedIdx = ref('时间占空比')
const bindingData = ref({
  algo: 't-stat',
  criteria: '实测值 >= 阈值',
  dataReqs: ['靶星终端在线记录', '中断状态记录']
})

// Step 4 Data
const activeBranchCat = ref('eval')
const branchCats = [
  { id: 'eval', name: '评估类型' },
  { id: 'goal', name: '评估目标' },
  { id: 'equip', name: '装备型号' },
  { id: 'scene', name: '场景条件' },
  { id: 'data', name: '数据条件' }
]

const branchRules = ref([
  { id: 'ET-001', cat: 'eval', name: '性能测试适配', cond: '评估类型 = 性能测试', actions: ['增加频率占空比', '删除计划符合性'] },
  { id: 'EG-001', cat: 'goal', name: '跳频对抗评估', cond: '包含 跳频对抗', actions: ['展开G3子树', '注入跳频算子'] },
  { id: 'EM-001', cat: 'equip', name: '卫星通信适配', cond: '装备 = 卫星干扰', actions: ['填充频段阈值', '替换卫星算子'] }
])

const filteredRules = computed(() => {
  return branchRules.value.filter(r => r.cat === activeBranchCat.value)
})

const flattenedIndicators = computed(() => {
  const result = []
  treeData.value.forEach(goal => {
    if (goal.children) {
      goal.children.forEach(idx => result.push(idx))
    }
  })
  return result
})

// Step 6 Data
const validations = ref([
  { name: '目标完整性', detail: '所有评估目标下均包含至少一个必选指标', status: 'pass' },
  { name: '算子绑定', detail: '指标 "最大连续中断时长" 未绑定算子', status: 'warn' },
  { name: '权重归一化', detail: '当前层级权重合计 1.00', status: 'pass' }
])

const skeletonData = ref({
  id: 'SKL-COMJAM-EQ-001',
  name: '通信对抗装备效能评估骨架',
  domain: 'defend',
  levels: ['装备级'],
  desc: '适用于通信对抗装备的通用评估模板。'
})

const treeData = ref([
  {
    label: 'G1 干扰效果评估',
    type: 'goal',
    weight: 0.35,
    children: [
      { label: '时间占空比', type: 'indicator', weight: 0.20 },
      { label: '最大连续中断时长', type: 'indicator', weight: 0.20 },
      { label: '干信比', type: 'indicator', weight: 0.15 }
    ]
  },
  {
    label: 'G2 响应速度与时序评估',
    type: 'goal',
    weight: 0.30,
    children: [
      { label: '目指接收时延', type: 'indicator', weight: 0.12 },
      { label: '目指引导精度偏差', type: 'indicator', weight: 0.12 }
    ]
  }
])

const handleSave = () => {
  console.log('Save as draft')
}

const handleSubmit = () => {
  console.log('Submit skeleton')
}
</script>

<style scoped lang="scss">
.skeleton-builder {
  height: 100%;
  background: #060911;
  color: #dde4f2;
  display: flex;
  flex-direction: column;

  .builder-header {
    height: 56px;
    padding: 0 20px;
    background: #0a0e19;
    border-bottom: 1px solid rgba(50, 85, 155, 0.15);
    display: flex;
    align-items: center;
    justify-content: space-between;

    .left-info {
      display: flex;
      align-items: center;
      gap: 15px;

      .title {
        font-size: 16px;
        font-weight: bold;
      }

      .sk-name {
        color: #3478f0;
        margin-left: 10px;
      }

      .ver-tag {
        background: rgba(52, 120, 240, 0.1);
        border: 1px solid rgba(52, 120, 240, 0.2);
      }
    }
  }

  .builder-steps {
    height: 60px;
    background: #0e1323;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 50px;
    padding: 0 40px;
    border-bottom: 1px solid rgba(50, 85, 155, 0.05);

    .step-item {
      display: flex;
      align-items: center;
      position: relative;
      cursor: pointer;
      transition: all 0.3s;

      .step-num {
        width: 26px;
        height: 26px;
        border-radius: 50%;
        background: #182039;
        color: #5a6a85;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        font-weight: bold;
        z-index: 2;
        transition: all 0.3s;
        border: 1px solid rgba(90, 106, 133, 0.2);
      }

      .step-label {
        font-size: 13px;
        margin-left: 10px;
        color: #5a6a85;
        z-index: 2;
        transition: all 0.3s;
        white-space: nowrap;
      }

      .step-line {
        position: absolute;
        left: calc(100% + 10px);
        top: 50%;
        width: 30px;
        height: 1px;
        background: rgba(90, 106, 133, 0.2);
        z-index: 1;
        transition: all 0.5s;
      }

      &:hover {
        .step-label {
          color: #fff;
        }

        .step-num {
          border-color: #3478f0;
          color: #3478f0;
        }
      }

      &.active {
        .step-num {
          background: #3478f0;
          color: #fff;
          border-color: #3478f0;
          box-shadow: 0 0 15px rgba(52, 120, 240, 0.4);
        }

        .step-label {
          color: #fff;
          font-weight: bold;
        }
      }

      &.completed {
        .step-num {
          background: #0faa78;
          color: #fff;
          border-color: #0faa78;
        }

        .step-label {
          color: #0faa78;
        }

        .step-line {
          background: #0faa78;
          height: 2px;
        }
      }
    }
  }

  .builder-content {
    flex: 1;
    overflow-y: auto;
    padding: 30px;

    .step-pane {
      max-width: 1000px;
      margin: 0 auto;
    }

    .placeholder {
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;

      .empty-state {
        text-align: center;

        .icon {
          font-size: 60px;
          color: #182039;
          margin-bottom: 20px;
        }

        h3 {
          font-size: 20px;
          color: #5a6a85;
          margin-bottom: 10px;
        }

        p {
          color: #3c4b62;
        }
      }
    }
  }

  // Indicator Tree Editor
  .tree-editor {
    .tree-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;

      .legend {
        display: flex;
        gap: 15px;

        .l-item {
          font-size: 11px;
          display: flex;
          align-items: center;
          gap: 5px;

          &::before {
            content: '';
            width: 8px;
            height: 8px;
            border-radius: 2px;
          }

          &.goal::before {
            background: #9855e0;
          }

          &.indicator::before {
            background: #3478f0;
          }
        }
      }
    }

    .tree-container {
      background: #0a0e19;
      border: 1px solid rgba(50, 85, 155, 0.15);
      border-radius: 8px;
      padding: 10px;
    }

    .custom-node {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 13px;
      padding: 5px 0;

      .node-icon {
        width: 12px;
        height: 4px;
        border-radius: 2px;
      }

      &.goal .node-icon {
        background: #9855e0;
      }

      &.indicator .node-icon {
        background: #3478f0;
      }

      .node-label {
        flex: 1;
      }

      .node-w {
        font-family: 'JetBrains Mono', monospace;
        color: #5a6a85;
        font-size: 11px;
        margin-right: 20px;
      }

      .node-ops {
        display: none;
        gap: 5px;
      }

      &:hover .node-ops {
        display: flex;
      }
    }
  }

  .weight-summary {
    height: 50px;
    background: #0a0e19;
    border-top: 1px solid rgba(50, 85, 155, 0.15);
    padding: 0 40px;
    display: flex;
    align-items: center;
    gap: 20px;

    .w-label {
      font-size: 12px;
      color: #5a6a85;
    }

    .w-bar {
      flex: 1;
      height: 16px;
      background: #060911;
      border-radius: 8px;
      overflow: hidden;
      display: flex;

      .w-seg {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 9px;
        font-weight: bold;
        color: #fff;
      }
    }

    .w-total {
      font-size: 13px;
      color: #0faa78;
      font-weight: bold;
      display: flex;
      align-items: center;
      gap: 5px;
    }
  }

  // Step 1 Refinement
  .base-info {
    .pane-card {
      background: #0a0e19;
      border: 1px solid rgba(50, 85, 155, 0.15);
      border-radius: 12px;
      padding: 30px;

      .section-title {
        margin-bottom: 25px;
        padding-left: 0;
      }
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0 40px;
    }
  }

  // Operator Binding
  .operator-binding {
    .binding-layout {
      display: grid;
      grid-template-columns: 240px 1fr;
      gap: 20px;
      height: 540px;

      .indicator-select {
        background: #0a0e19;
        border: 1px solid rgba(50, 85, 155, 0.15);
        border-radius: 8px;
        overflow-y: auto;

        .section-title {
          font-size: 11px;
          padding: 15px;
          background: rgba(255, 255, 255, 0.02);
          margin: 0;
        }

        .idx-item {
          padding: 12px 20px;
          cursor: pointer;
          font-size: 13px;
          color: #8494b2;
          border-bottom: 1px solid rgba(255, 255, 255, 0.02);
          transition: all 0.2s;

          &:hover {
            background: rgba(52, 120, 240, 0.05);
            color: #fff;
          }

          &.active {
            color: #3478f0;
            background: rgba(52, 120, 240, 0.1);
            font-weight: bold;
            border-left: 3px solid #3478f0;
          }
        }
      }

      .binding-details {
        background: #0a0e19;
        border: 1px solid rgba(50, 85, 155, 0.15);
        border-radius: 8px;
        display: flex;
        flex-direction: column;

        .section-title {
          font-size: 11px;
          padding: 15px;
          background: rgba(255, 255, 255, 0.02);
          margin: 0;
        }

        .binding-content {
          flex: 1;
          padding: 30px;

          .binding-card {
            background: #111a33;
            border: 1px solid rgba(80, 140, 255, 0.08);
            border-radius: 12px;
            padding: 30px;

            .card-row {
              display: flex;
              align-items: center;
              gap: 20px;
              margin-bottom: 25px;

              .label {
                width: 100px;
                font-size: 13px;
                color: #8494b2;
              }

              .data-tag {
                margin-right: 8px;
                background: #060a13;
                border-color: rgba(52, 120, 240, 0.2);
              }
            }
          }
        }
      }
    }
  }

  // Branch Rules
  .branch-rules {
    display: grid;
    grid-template-columns: 160px 1fr;
    gap: 20px;
    height: 540px;

    .branch-tabs {
      background: #0a0e19;
      border: 1px solid rgba(50, 85, 155, 0.15);
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      gap: 5px;
      padding: 10px;

      .branch-tab {
        padding: 12px 15px;
        border-radius: 6px;
        border: 1px solid transparent;
        font-size: 13px;
        color: #8494b2;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: rgba(255, 255, 255, 0.05);
        }

        &.active {
          color: #fff;
          border-color: rgba(52, 120, 240, 0.3);
        }

        &.eval.active {
          background: rgba(52, 120, 240, 0.2);
          box-shadow: 0 0 10px rgba(52, 120, 240, 0.1);
        }

        &.goal.active {
          background: rgba(152, 85, 224, 0.2);
        }

        &.equip.active {
          background: rgba(15, 170, 120, 0.2);
        }

        &.scene.active {
          background: rgba(232, 154, 8, 0.2);
        }

        &.data.active {
          background: rgba(6, 182, 212, 0.2);
        }
      }
    }

    .rules-list {
      background: #0a0e19;
      border: 1px solid rgba(50, 85, 155, 0.15);
      border-radius: 8px;
      padding: 20px;
      overflow-y: auto;

      .rule-toolbar {
        margin-bottom: 15px;
        display: flex;
        justify-content: flex-end;
      }
    }

    .rule-item {
      background: #111a33;
      border: 1px solid rgba(80, 140, 255, 0.08);
      border-radius: 10px;
      padding: 18px;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
      gap: 25px;
      transition: all 0.2s;

      &:hover {
        border-color: rgba(52, 120, 240, 0.4);
        background: #15203d;
      }

      .rule-info {
        flex: 1;

        .rule-code {
          font-family: 'JetBrains Mono', monospace;
          font-size: 11px;
          color: #4e5d7a;
        }

        .rule-name {
          font-size: 15px;
          font-weight: bold;
          margin: 6px 0;
          color: #fff;
        }

        .rule-cond {
          font-size: 12px;
          color: #8494b2;

          .cond-text {
            color: #e89a08;
            font-weight: bold;
          }
        }
      }

      .rule-actions {
        flex: 1.2;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .rule-ops {
        opacity: 0.6;
        transition: opacity 0.2s;

        &:hover {
          opacity: 1;
        }
      }
    }
  }

  // Weight Config
  .weight-config {
    .weight-card {
      background: #0a0e19;
      border: 1px solid rgba(50, 85, 155, 0.15);
      padding: 30px;
      border-radius: 12px;

      .weight-row {
        display: flex;
        align-items: center;
        gap: 20px;
        margin-bottom: 20px;

        .label {
          width: 150px;
          font-size: 13px;
        }

        .slider {
          flex: 1;
        }
      }
    }
  }

  // Validation
  .validation {
    .valid-list {
      max-width: 800px;
      margin: 0 auto;

      .valid-item {
        display: flex;
        align-items: center;
        gap: 20px;
        background: #0a0e19;
        padding: 20px 30px;
        border-radius: 10px;
        margin-bottom: 15px;
        border: 1px solid rgba(80, 140, 255, 0.05);
        border-left: 4px solid #182039;
        transition: transform 0.2s;

        &:hover {
          transform: translateX(5px);
          background: #0e1323;
        }

        .status-ico {
          font-size: 24px;
        }

        &.pass {
          border-left-color: #0faa78;

          .status-ico {
            color: #0faa78;
          }
        }

        &.warn {
          border-left-color: #e89a08;

          .status-ico {
            color: #e89a08;
          }
        }

        .info {
          flex: 1;

          .v-nm {
            font-size: 15px;
            font-weight: bold;
            margin-bottom: 4px;
            color: #fff;
          }

          .v-dt {
            font-size: 12px;
            color: #8494b2;
          }
        }
      }
    }
  }
}

// Global Component Overrides
:deep(.cyber-form) {
  .el-form-item__label {
    color: #8494b2;
    font-size: 13px;
  }

  .el-input__wrapper,
  .el-textarea__inner {
    background-color: #060a13 !important;
    box-shadow: 0 0 0 1px rgba(80, 140, 255, 0.1) inset !important;
    border: none !important;

    .el-input__inner,
    .el-textarea__inner {
      color: #fff !important;
    }
  }
}

:deep(.el-checkbox-button__inner) {
  background: transparent !important;
  border: 1px solid rgba(80, 140, 255, 0.1) !important;
  color: #8494b2 !important;
  border-radius: 4px !important;
  margin-right: 8px;
  font-size: 12px !important;
}

:deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  background: rgba(52, 120, 240, 0.1) !important;
  color: #3478f0 !important;
  border-color: #3478f0 !important;
  box-shadow: -1px 0 0 0 #3478f0 inset !important;
}

:deep(.cyber-tree) {
  background: transparent;
  color: #dde4f2;

  .el-tree-node__content {
    height: 36px;

    &:hover {
      background: rgba(52, 120, 240, 0.05);
    }
  }

  .el-tree-node.is-current>.el-tree-node__content {
    background: rgba(52, 120, 240, 0.1) !important;
  }
}
</style>
