<template>
  <div class="auxiliary-calculation">
    <el-popover
      placement="bottom-end"
      :width="200"
      trigger="hover"
      popper-class="tech-popover"
    >
      <template #reference>
        <div class="calc-icon-wrapper">
          <el-icon class="calc-icon"><Management /></el-icon>
          <span class="calc-text">辅助计算</span>
        </div>
      </template>

      <div class="calc-menu">
        <div class="calc-menu-header">专用辅助计算分析</div>
        <div 
          v-for="item in calcItems" 
          :key="item.name" 
          class="calc-menu-item"
          @click="openCalcDialog(item)"
        >
          <span :class="{ 'highlight': item.highlight }">{{ item.name }}</span>
        </div>
      </div>
    </el-popover>

    <!-- 计算弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="currentCalc?.name"
      width="500px"
      append-to-body
      destroy-on-close
      class="tech-dialog"
    >
      <div v-if="!calculating && !result" class="calc-form">
        <div v-if="currentCalc?.name === '轨道机动'">
          <el-form label-position="top">
            <el-form-item label="半长轴 (km)">
              <el-input-number v-model="formData.semiMajorAxis" :precision="2" :step="100" />
            </el-form-item>
            <el-form-item label="离心率">
              <el-input-number v-model="formData.eccentricity" :precision="4" :max="1" :step="0.0001" />
            </el-form-item>
            <el-form-item label="机动量 (m/s)">
              <el-input-number v-model="formData.deltaV" :precision="2" :step="1" />
            </el-form-item>
          </el-form>
        </div>
        <div v-else-if="currentCalc?.name === '坐标转换'">
          <el-form label-position="top">
            <el-form-item label="转换为">
              <el-select v-model="formData.targetCoord">
                <el-option label="WGS84 -> ECEF" value="ecef" />
                <el-option label="ECEF -> WGS84" value="wgs84" />
              </el-select>
            </el-form-item>
            <el-form-item label="经度 (deg)">
              <el-input-number v-model="formData.lon" :precision="6" :min="-180" :max="180" />
            </el-form-item>
            <el-form-item label="纬度 (deg)">
              <el-input-number v-model="formData.lat" :precision="6" :min="-90" :max="90" />
            </el-form-item>
            <el-form-item label="高度 (m)">
              <el-input-number v-model="formData.alt" :precision="2" />
            </el-form-item>
          </el-form>
        </div>
        <div v-else>
          <p class="placeholder-text">该计算模块需要输入相关业务参数...</p>
          <el-form label-position="top">
            <el-form-item label="参数 A">
              <el-input v-model="formData.paramA" placeholder="请输入参数" />
            </el-form-item>
            <el-form-item label="参数 B">
              <el-input v-model="formData.paramB" placeholder="请输入参数" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div v-else-if="calculating" class="calculating-state">
        <el-icon class="is-loading loading-icon"><Loading /></el-icon>
        <p>正在分析计算中...</p>
      </div>

      <div v-else-if="result" class="calc-result">
        <div class="result-header">
          <el-icon class="success-icon"><CircleCheck /></el-icon>
          <span>计算完成</span>
        </div>
        <pre class="result-box">{{ resultText }}</pre>
      </div>

      <template #footer>
        <div v-if="!calculating">
          <el-button v-if="result" @click="resetCalc">重新计算</el-button>
          <el-button v-else @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" v-if="!result" @click="startCalculting">开始计算</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Management, Loading, CircleCheck } from '@element-plus/icons-vue'

const calcItems = [
  { name: '轨道机动', highlight: true },
  { name: '精确定轨' },
  { name: '覆盖效能' },
  { name: '链路容量' },
  { name: '导航精度' },
  { name: '任务规划' },
  { name: '坐标转换' },
  { name: '时间转换' },
  { name: '覆盖分析' }
]

const dialogVisible = ref(false)
const currentCalc = ref(null)
const calculating = ref(false)
const result = ref(false)
const resultText = ref('')

const formData = reactive({
  semiMajorAxis: 7000,
  eccentricity: 0.001,
  deltaV: 10,
  targetCoord: 'ecef',
  lon: 116.3,
  lat: 39.9,
  alt: 100,
  paramA: '',
  paramB: ''
})

const openCalcDialog = (item) => {
  currentCalc.value = item
  dialogVisible.value = true
  resetCalc()
}

const resetCalc = () => {
  result.value = false
  calculating.value = false
  resultText.value = ''
}

const startCalculting = () => {
  calculating.value = true
  
  // 模拟计算延迟
  setTimeout(() => {
    calculating.value = false
    result.value = true
    generateMockResult()
  }, 1500)
}

const generateMockResult = () => {
  if (currentCalc.value.name === '轨道机动') {
    resultText.value = `机动后轨道参数：
半长轴: ${(formData.semiMajorAxis + Math.random() * 5).toFixed(3)} km
离心率: ${(formData.eccentricity + 0.0001).toFixed(6)}
近地点幅角: ${(Math.random() * 360).toFixed(2)} deg
升交点赤经: ${(Math.random() * 360).toFixed(2)} deg`
  } else if (currentCalc.value.name === '坐标转换') {
    resultText.value = `转换结果 (${formData.targetCoord.toUpperCase()}):
X: ${(Math.random() * 1000000).toFixed(3)} m
Y: ${(Math.random() * 1000000).toFixed(3)} m
Z: ${(Math.random() * 1000000).toFixed(3)} m`
  } else {
    resultText.value = `分析报告如下：
完成时间: ${new Date().toLocaleString()}
指标值: ${(Math.random() * 100).toFixed(2)}%
评估结论: 满足系统设计要求。`
  }
}
</script>

<style scoped lang="scss">
.auxiliary-calculation {
  display: inline-block;
}

.calc-icon-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 16px;
  background: rgba(0, 242, 255, 0.1);
  border: 1px solid rgba(0, 242, 255, 0.3);
  border-radius: 4px;
  transition: all 0.3s;
  color: #00f2ff;

  &:hover {
    background: rgba(0, 242, 255, 0.2);
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
  }

  .calc-icon {
    font-size: 18px;
  }

  .calc-text {
    font-size: 14px;
    font-weight: 600;
  }
}

.calc-menu {
  background: rgba(10, 21, 37, 0.95);
  padding: 10px 0;
  text-align: center;

  .calc-menu-header {
    font-size: 14px;
    color: #fff;
    padding: 8px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    margin-bottom: 8px;
  }

  .calc-menu-item {
    padding: 10px 20px;
    color: #8fa3b8;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 13px;

    &:hover {
      background: rgba(0, 242, 255, 0.1);
      color: #00f2ff;
    }

    .highlight {
      color: #ff4d4f;
      font-weight: bold;
    }
  }
}

.calculating-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  color: #00f2ff;

  .loading-icon {
    font-size: 40px;
    margin-bottom: 15px;
  }
}

.calc-result {
  .result-header {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #67c23a;
    font-size: 18px;
    font-weight: bold;
    margin-bottom: 20px;

    .success-icon {
      font-size: 24px;
    }
  }

  .result-box {
    background: rgba(0, 0, 0, 0.3);
    padding: 20px;
    border-radius: 8px;
    border: 1px solid rgba(0, 242, 255, 0.2);
    color: #00f2ff;
    font-family: monospace;
    line-height: 1.6;
    margin: 0;
  }
}

.placeholder-text {
  color: #8fa3b8;
  margin-bottom: 20px;
}
</style>

<style lang="scss">
.tech-popover {
  background: rgba(10, 21, 37, 0.98) !important;
  border: 1px solid rgba(0, 242, 255, 0.4) !important;
  padding: 0 !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5) !important;

  .el-popper__arrow::before {
    background: rgba(10, 21, 37, 0.98) !important;
    border-top: 1px solid rgba(0, 242, 255, 0.4) !important;
    border-right: 1px solid rgba(0, 242, 255, 0.4) !important;
  }
}

.tech-dialog {
  .el-dialog__header {
    background: rgba(10, 21, 37, 0.95);
    border-bottom: 1px solid rgba(0, 242, 255, 0.2);
    margin: 0;
    padding: 20px;
    
    .el-dialog__title {
      color: #00f2ff;
      font-weight: bold;
      letter-spacing: 1px;
    }
  }

  .el-dialog__body {
    background: #02060c;
    color: #fff;
    padding: 30px;
  }

  .el-dialog__footer {
    background: #02060c;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    padding: 20px;
  }

  .el-form-item__label {
    color: #8fa3b8 !important;
  }

  .el-input-number {
    width: 100%;
    
    .el-input__wrapper {
      background: rgba(255, 255, 255, 0.05) !important;
      box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.2) inset !important;
      
      input {
        color: #fff !important;
      }
    }
  }
  
  .el-select {
    width: 100%;
    .el-input__wrapper {
      background: rgba(255, 255, 255, 0.05) !important;
      box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.2) inset !important;
    }
  }
}
</style>
