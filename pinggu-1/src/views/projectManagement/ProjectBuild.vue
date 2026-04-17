<template>
  <div class="subpage-container project-build-page">
    <!-- 顶部返回和基础信息 -->
    <div class="detail-header">
      <el-button class="back-btn" @click="goBack" icon="ArrowLeft">返回列表</el-button>
      <div class="project-title-area">
        <h2 class="project-title">新建评估工程</h2>
        <el-tag type="primary" effect="dark" class="status-badge">构建中</el-tag>
        <el-tag effect="plain" class="type-badge">体系协同工程</el-tag>
      </div>

      <el-row class="basic-info-row">
        <el-col :span="6">
          <div class="info-item">
            <span class="label">当前步骤:</span>
            <span class="value">{{ processSteps[currentStep].title }}</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <span class="label">进度:</span>
            <div class="progress-wrap">
              <el-progress :percentage="progressPercentage" :color="customColors" class="tech-progress-mini" />
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <span class="label">创建人:</span>
            <span class="value">{{ userInfo.name }}</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <span class="label">创建时间:</span>
            <span class="value">{{ currentTime }}</span>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 步骤条 -->
    <el-card class="tech-card process-container">
      <template #header>
        <div class="card-title">
          <el-icon>
            <Connection />
          </el-icon> 评估工程构建流程
        </div>
      </template>

      <div class="process-steps-wrapper">
        <div class="process-steps">
          <div 
            v-for="(step, index) in processSteps" 
            :key="index"
            :class="['step-item', { active: currentStep === index, completed: currentStep > index }]"
          >
            <div class="step-number">{{ index + 1 }}</div>
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc">{{ step.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <el-divider class="cyber-divider" />

      <!-- 步骤内容展示区域 -->
      <div class="step-content-panel">
        <!-- 第一步：作战剖面构建 -->
        <div v-if="currentStep === 0" class="step-content">
          <h3 class="panel-header">作战剖面构建</h3>
          <el-form :model="form" :rules="rules" label-width="120px">
            <el-form-item label="体系作战剖面名称" prop="profileName">
              <el-input v-model="form.profileName" placeholder="请输入体系作战剖面名称" />
            </el-form-item>
            <el-form-item label="评估工程类型" prop="projectType">
              <el-select v-model="form.projectType" placeholder="请选择评估工程类型" style="width: 100%">
                <el-option
                  v-for="item in projectTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="评估超网络" prop="superNetwork">
              <el-select v-model="form.superNetwork" multiple placeholder="请选择评估超网络" style="width: 100%">
                <el-option
                  v-for="item in superNetworkOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第二步：超网络模型构建 -->
        <div v-if="currentStep === 1" class="step-content">
          <h3 class="panel-header">超网络模型构建</h3>
          <el-descriptions column="2" border class="tech-descriptions">
            <el-descriptions-item label="已选超网络">
              <el-tag v-for="network in form.superNetwork" :key="network" class="network-tag">
                {{ getNetworkLabel(network) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="模型状态">待构建</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 第三步：评估方案设计 -->
        <div v-if="currentStep === 2" class="step-content">
          <h3 class="panel-header">评估方案设计</h3>
          <p>评估方案设计内容待完善...</p>
        </div>

        <!-- 第四步：数据汇总 -->
        <div v-if="currentStep === 3" class="step-content">
          <h3 class="panel-header">数据汇总</h3>
          <p>数据汇总内容待完善...</p>
        </div>

        <!-- 第五步：网络计算 -->
        <div v-if="currentStep === 4" class="step-content">
          <h3 class="panel-header">网络计算</h3>
          <p>网络计算内容待完善...</p>
        </div>

        <!-- 第六步：结果分析 -->
        <div v-if="currentStep === 5" class="step-content">
          <h3 class="panel-header">结果分析</h3>
          <p>结果分析内容待完善...</p>
        </div>
      </div>

      <!-- 步骤导航按钮 -->
      <div class="step-navigation">
        <el-button @click="prevStep" v-if="currentStep > 0">上一步</el-button>
        <el-button type="primary" @click="nextStep" v-if="currentStep < processSteps.length - 1">下一步</el-button>
        <el-button type="success" @click="submitForm" v-if="currentStep === processSteps.length - 1">完成构建</el-button>
        <el-button @click="cancelBuild">取消构建</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 步骤数据
const processSteps = ref([
  { title: '作战剖面构建', desc: '定义体系作战剖面和评估类型' },
  { title: '超网络模型构建', desc: '构建评估所需的超网络模型' },
  { title: '评估方案设计', desc: '设计具体的评估方案和参数' },
  { title: '数据汇总', desc: '收集和整理评估所需数据' },
  { title: '网络计算', desc: '执行网络计算和分析' },
  { title: '结果分析', desc: '分析评估结果并生成报告' }
])

const currentStep = ref(0)

// 表单数据
const form = ref({
  profileName: '',
  projectType: '',
  superNetwork: []
})

// 用户信息
const userInfo = ref({
  name: '当前用户'
})

const currentTime = ref('')

// 选项数据
const projectTypeOptions = ref([
  { value: '1', label: '体系协同评估' },
  { value: '2', label: '作战效能评估' },
  { value: '3', label: '装备配置评估' }
])

const superNetworkOptions = ref([
  { value: '1', label: '体系作战剖面' },
  { value: '2', label: '装备体系力量结构网' },
  { value: '3', label: '体系作战任务网' },
  { value: '4', label: 'OODA网' },
  { value: '5', label: '指标网' },
  { value: '6', label: '算法网' }
])

// 计算属性
const progressPercentage = computed(() => {
  return Math.round((currentStep.value / (processSteps.value.length - 1)) * 100)
})

// 方法
const getNetworkLabel = (value) => {
  const network = superNetworkOptions.value.find(item => item.value === value)
  return network ? network.label : value
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const nextStep = () => {
  if (currentStep.value < processSteps.value.length - 1) {
    currentStep.value++
  }
}

const submitForm = () => {
  ElMessage.success('评估工程构建完成！')
  goBack()
}

const cancelBuild = () => {
  ElMessage.warning('已取消评估工程构建')
  goBack()
}

const goBack = () => {
  router.push('/project/index')
}

// 初始化
onMounted(() => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN')
})

// 样式配置
const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 }
]
</script>

<style scoped>
.project-build-page {
  padding: 20px;
}

.detail-header {
  background: var(--el-bg-color);
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid var(--el-border-color-light);
}

.back-btn {
  margin-bottom: 15px;
}

.project-title-area {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.project-title {
  margin: 0 15px 0 0;
  font-size: 24px;
  color: var(--el-text-color-primary);
}

.status-badge,
.type-badge {
  margin-right: 10px;
}

.basic-info-row {
  margin-top: 15px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item .label {
  margin-right: 8px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.info-item .value {
  color: var(--el-text-color-primary);
}

.progress-wrap {
  width: 120px;
}

.process-steps-wrapper {
  margin-bottom: 20px;
}

.process-steps {
  display: flex;
  justify-content: space-between;
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
}

.step-item {
  display: flex;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: -10px;
  top: 50%;
  width: 20px;
  height: 1px;
  background: var(--el-border-color-light);
}

.step-number {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 10px;
}

.step-item.active .step-number {
  background: var(--el-color-primary);
  color: white;
}

.step-item.completed .step-number {
  background: var(--el-color-success);
  color: white;
}

.step-content {
  flex: 1;
}

.step-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.step-item.active .step-title {
  color: var(--el-color-primary);
}

.step-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.step-content-panel {
  min-height: 300px;
  padding: 20px;
}

.panel-header {
  margin-bottom: 20px;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 10px;
}

.step-navigation {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-light);
}

.step-navigation .el-button {
  margin: 0 10px;
}

.network-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}
</style>