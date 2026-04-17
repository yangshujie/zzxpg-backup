<template>
  <div
    class="stage-node"
    :class="{
      'is-first': isFirst,
      'is-last': isLast,
      'active': isActive
    }"
    @click="handleClick"
  >
    <div class="node-content">
      <div class="node-icon">
        <el-icon :size="24">
          <component :is="getNodeIcon(index)" />
        </el-icon>
      </div>
      <div class="node-info">
        <div class="node-name">{{ stage.name }}</div>
        <div class="node-index">{{ index + 1 }} / {{ totalCount }}</div>
      </div>
      <div class="drag-handle">
        <el-icon><Rank /></el-icon>
      </div>
    </div>
    
    <!-- 连接点指示器 -->
    <div class="connection-point connection-point-left"></div>
    <div class="connection-point connection-point-right"></div>
    
    <!-- 步骤指示器 -->
    <div class="step-indicator" v-if="!isLast">
      <div class="step-line"></div>
      <div class="step-arrow">
        <el-icon><ArrowDown /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Rank, ArrowDown, Search, Operation, Aim, Checked } from '@element-plus/icons-vue'

const props = defineProps({
  stage: {
    type: Object,
    required: true
  },
  index: {
    type: Number,
    required: true
  },
  isFirst: {
    type: Boolean,
    default: false
  },
  isLast: {
    type: Boolean,
    default: false
  },
  totalCount: {
    type: Number,
    default: 0
  },
  isActive: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

// 根据任务名称或索引返回对应图标
const getNodeIcon = (index) => {
  const icons = {
    0: Search,      // 侦察阶段
    1: Operation,   // 指挥决策
    2: Aim,         // 火力打击
    3: Checked      // 效果评估
  }
  return icons[index] || Rank
}

const handleClick = () => {
  emit('click', props.stage)
}
</script>

<style lang="scss" scoped>
.stage-node {
  position: relative;
  width: 280px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  }

  &.active {
    box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.5), 0 8px 24px rgba(102, 126, 234, 0.4);
    transform: scale(1.02);
  }

  &.is-first {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }

  &.is-last {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }
}

.node-content {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  color: white;
  position: relative;
  z-index: 1;
}

.node-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.node-info {
  flex: 1;
}

.node-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.node-index {
  font-size: 12px;
  opacity: 0.8;
}

.drag-handle {
  cursor: grab;
  opacity: 0.7;
  transition: opacity 0.3s;

  &:hover {
    opacity: 1;
  }

  &:active {
    cursor: grabbing;
  }
}

/* 连接点 */
.connection-point {
  position: absolute;
  width: 12px;
  height: 12px;
  background: white;
  border: 2px solid #667eea;
  border-radius: 50%;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
}

.connection-point-left {
  left: -6px;
}

.connection-point-right {
  right: -6px;
}

/* 步骤指示器 */
.step-indicator {
  position: absolute;
  bottom: -40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
}

.step-line {
  width: 2px;
  height: 30px;
  background: linear-gradient(to bottom, #667eea, #764ba2);
}

.step-arrow {
  color: #667eea;
  font-size: 14px;
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

/* 最后一个节点不显示指示器 */
.stage-node.is-last .step-indicator {
  display: none;
}
</style>