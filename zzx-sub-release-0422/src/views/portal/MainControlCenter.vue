<template>
  <div class="main-control-center">
    <!-- Header -->
    <header class="header">
      <div class="logo">
        <span class="x-icon">X</span>
        <span class="title">XXXX CENTER</span>
      </div>
      <div class="sys-time">
        <el-icon>
          <Monitor />
        </el-icon>
        <span class="time-label">系统运行正常</span>
        <span class="time-value">{{ currentTime }}</span>
        <div class="user-info">
          <el-icon>
            <User />
          </el-icon>
          <span>管理员</span>
        </div>
      </div>
    </header>

    <!-- Main Orbital System -->
    <div class="orbital-system-wrapper">


      <div class="orbital-system">
        <!-- Center Core Node -->
        <div class="core-node">
          <div class="core-earth"></div>
          <div class="core-rings">
            <div class="ring r1"></div>
            <div class="ring r2"></div>
          </div>
          <div class="core-label">
            <h4>XXXX 主中心</h4>
            <p>MAIN CONTROL</p>
          </div>
        </div>

        <!-- Orbiting Subsystems -->
        <div class="satellite-nodes">
          <div class="node " style="--x: -180px; --y: -120px; --delay: 0s;"
            @click="navigateTo('/major/requirement-sys/portal')">
            <div class="glass-sphere"><el-icon>
                <Menu />
              </el-icon></div>
            <div class="label-box">需求设计系统</div>
          </div>
          <div class="node " style="--x: -320px; --y: -10px; --delay: -1s;"
            @click="navigateTo('/major/capability-resource/indicator')">
            <div class="glass-sphere"><el-icon>
                <Connection />
              </el-icon></div>
            <div class="label-box">体系协同系统</div>
          </div>
          <div class="node " style="--x: -200px; --y: 110px; --delay: -2s;" @click="navigateTo('/indicator')">
            <div class="glass-sphere"><el-icon>
                <PieChart />
              </el-icon></div>
            <div class="label-box">能力展示系统</div>
          </div>
          <div class="node " style="--x: 120px; --y: 140px; --delay: -3s;" @click="navigateTo('/model')">
            <div class="glass-sphere"><el-icon>
                <DataLine />
              </el-icon></div>
            <div class="label-box">模型系统</div>
          </div>
          <div class="node " style="--x: 320px; --y: 20px; --delay: -1.5s;" @click="navigateTo('/compute')">
            <div class="glass-sphere"><el-icon>
                <Cpu />
              </el-icon></div>
            <div class="label-box">综合计算系统</div>
          </div>
          <div class="node " style="--x: 200px; --y: -100px; --delay: -0.5s;" @click="navigateTo('/data')">
            <div class="glass-sphere"><el-icon>
                <Coin />
              </el-icon></div>
            <div class="label-box">数据系统</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Core Functions Dashboard -->
    <div class="dashboard-section">
      <div class="section-title">
        <el-icon>
          <Odometer />
        </el-icon>
        <span class="cn">核心功能</span>
        <span class="en">CORE FUNCTIONS</span>
      </div>

      <!-- Functional Cards Row -->
      <div class="func-cards">
        <div class="func-card" @click="navigateTo('/project')">
          <div class="card-head">评估工程 <el-tag size="small" effect="dark">CORE</el-tag></div>
          <div class="card-body">
            <div class="progress-item"><span>评价指标</span><el-progress :percentage="60" color="#00f2ff"
                :show-text="false" /></div>
            <div class="progress-item"><span>评价模型</span><el-progress :percentage="30" color="#00f2ff"
                :show-text="false" /></div>
          </div>
        </div>
        <div class="func-card" @click="navigateTo('/evaluation')">
          <div class="card-head">体系评估 <el-tag size="small" effect="dark">CORE</el-tag></div>
          <div class="card-body chart-mockup wave-line"></div>
        </div>
        <div class="func-card" @click="navigateTo('/resource')">
          <div class="card-head">能力资源底座 <el-tag size="small" effect="dark">CORE</el-tag></div>
          <div class="card-body">
            <div class="progress-item"><span>资源使用负载</span><el-progress :percentage="85" color="#00f2ff"
                :show-text="false" /></div>
            <div class="progress-item"><span>网格并行利用率</span><el-progress :percentage="50" color="#00f2ff"
                :show-text="false" /></div>
          </div>
        </div>
        <div class="func-card" @click="navigateTo('/exhibition')">
          <div class="card-head">数字展览馆 <el-tag size="small" effect="dark">CORE</el-tag></div>
          <div class="card-body chart-mockup wave-area"></div>
        </div>
        <div class="func-card" @click="navigateTo('/process/process-sys/templates')">
          <div class="card-head">模板库管理 <el-tag size="small" effect="dark">CORE</el-tag></div>
          <div class="card-body dual-stat">
            <div class="left-stat">
              <div>总计资源</div>
              <div class="val">90 页</div>
              <div>分析资源</div>
              <div class="val">352 个库</div>
            </div>
            <div class="right-gauge"></div>
          </div>
        </div>
        <div class="func-card" @click="navigateTo('/system')">
          <div class="card-head">系统管理 <el-tag size="small" effect="dark">CORE</el-tag></div>
          <div class="card-body">
            <div class="progress-item"><span>系统负载</span><el-progress :percentage="22" color="#00f2ff"
                :show-text="false" /></div>
            <div class="progress-item"><span>系统健康</span><el-progress :percentage="98" color="#00f2ff"
                :show-text="false" /></div>
          </div>
        </div>
      </div>

      <!-- Stat Cards Row -->
      <div class="stat-cards">
        <div class="stat-card">
          <div class="val">1286</div>
          <div class="label">评估任务</div>
          <div class="bg-chart trend-up"></div>
        </div>
        <div class="stat-card">
          <div class="val">384</div>
          <div class="label">模型数量</div>
          <div class="bg-chart gauge-mini"></div>
        </div>
        <div class="stat-card">
          <div class="val">99%</div>
          <div class="label">系统可用率</div>
          <div class="bg-chart circle-progress"></div>
        </div>
        <div class="stat-card">
          <div class="val">56</div>
          <div class="label">在线用户</div>
          <div class="bg-chart heartbeat"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Monitor, User, Menu, Connection, PieChart,
  DataLine, Cpu, Coin, Odometer
} from '@element-plus/icons-vue'

const currentTime = ref('')
const router = useRouter()
let timer = null

const navigateTo = (path) => {
  if (path) {
    // Attempt standard navigation.
    router.push(path).catch((err) => {
      // In case route is not mapped yet in router/index.js
      if (err.name === 'NavigationDuplicated') return;
      ElMessage.warning(`页面模块未完全接入系统 [${path}]`)
    })
  } else {
    ElMessage.info('该模块正在建设中...')
  }
}

const updateTime = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dy = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}/${m}/${dy} ${h}:${min}:${s}`
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped lang="scss">
.main-control-center {
  width: 100%;
  height: 100vh;
  background: radial-gradient(circle at 50% 40%, #0c1a2e 0%, #030811 100%);
  color: #fff;
  font-family: 'Inter', system-ui, sans-serif;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 200%;
    height: 200%;
    background-image:
      repeating-radial-gradient(circle at center, transparent, transparent 100px, rgba(0, 242, 255, 0.02) 101px, rgba(0, 242, 255, 0.02) 102px);
    pointer-events: none;
    z-index: 0;
  }
}

.header {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;

    .x-icon {
      background: #00f2ff;
      color: #000;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 900;
      border-radius: 4px;
    }

    .title {
      font-size: 18px;
      font-weight: bold;
      letter-spacing: 2px;
      color: #b0c9e8;
    }
  }

  .sys-time {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 13px;
    color: #8fa3b8;

    .time-value {
      font-family: monospace;
      color: #00f2ff;
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-left: 20px;
      color: #fff;
    }
  }
}

.orbital-system-wrapper {
  flex: 1;
  position: relative;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .system-title {
    position: absolute;
    top: 10%;
    text-align: center;
    margin: 0;
    font-size: 32px;
    letter-spacing: 4px;
    text-shadow: 0 0 15px rgba(0, 242, 255, 0.5);

    .en-title {
      display: block;
      font-size: 14px;
      color: #00f2ff;
      letter-spacing: 6px;
      margin-top: 8px;
      opacity: 0.8;
    }
  }
}

.orbital-system {
  position: relative;
  width: 800px;
  height: 400px;
  perspective: 1000px;
  transform-style: preserve-3d;

  .core-node {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;

    .core-earth {
      width: 140px;
      height: 140px;
      border-radius: 50%;
      background: radial-gradient(circle at 30% 30%, rgba(48, 110, 204, 0.9) 0%, rgba(4, 21, 48, 0.9) 60%, rgba(2, 7, 18, 0.9) 100%);
      box-shadow: 0 0 30px rgba(0, 136, 255, 0.25), inset 0 0 25px rgba(0, 242, 255, 0.15);
      border: 1px solid rgba(0, 242, 255, 0.2);
      position: relative;
      z-index: 2;
    }

    .core-rings {
      position: absolute;
      top: 56%;
      left: 50%;
      width: 300px;
      height: 300px;
      transform: translate(-50%, -50%) rotateX(60deg);

      .ring {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        border-radius: 50%;
        border: 2px solid rgba(0, 242, 255, 0.1);
        border-left: 2px solid #00f2ff;
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.2);

        &.r1 {
          animation: rotateOrbit1 20s linear infinite;
        }

        &.r2 {
          border-right: 2px solid rgba(0, 242, 255, 0.6);
          border-left: 2px solid rgba(0, 242, 255, 0.1);
          animation: rotateOrbit2 30s linear infinite;
        }
      }
    }

    .core-label {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      z-index: 10;
      text-align: center;

      h4 {
        margin: 0;
        font-size: 16px;
        letter-spacing: 1px;
        white-space: nowrap;
      }

      p {
        margin: 4px 0 0;
        font-size: 10px;
        color: #00f2ff;
        white-space: nowrap;
      }
    }
  }

  .satellite-nodes {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;

    .node {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(calc(-50% + var(--x)), calc(-50% + var(--y)));
      animation: floatNode 4s ease-in-out infinite;
      animation-delay: var(--delay);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      transition: filter 0.3s ease;

      &:hover {
        filter: drop-shadow(0 0 15px rgba(0, 242, 255, 0.8)) brightness(1.2);
        z-index: 100;

        .label-box {
          color: #fff;
          border-color: #00f2ff;
        }
      }

      .glass-sphere {
        width: 70px;
        height: 70px;
        border-radius: 50%;
        background: rgba(16, 32, 53, 0.6);
        backdrop-filter: blur(10px);
        border: 2px solid rgba(0, 242, 255, 0.4);
        box-shadow: inset 0 -8px 20px rgba(0, 242, 255, 0.3), 0 12px 24px rgba(0, 0, 0, 0.6);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 42px;
        color: #00f2ff;
        position: relative;

        &::after {
          content: '';
          position: absolute;
          bottom: -24px;
          left: 50%;
          transform: translateX(-50%);
          width: 100px;
          height: 18px;
          border-radius: 50%;
          border-bottom: 3px solid #00f2ff;
          box-shadow: 0 12px 15px -5px rgba(0, 242, 255, 0.5);
        }
      }

      .label-box {
        background: rgba(4, 15, 30, 0.8);
        border: 1px solid rgba(0, 242, 255, 0.3);
        padding: 4px 12px;
        border-radius: 12px;
        font-size: 12px;
        color: #b0c9e8;
        box-shadow: 0 0 10px rgba(0, 242, 255, 0.1);
        backdrop-filter: blur(4px);
      }
    }
  }
}

.dashboard-section {
  position: relative;
  z-index: 10;
  padding: 0 40px 30px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;

    .el-icon {
      color: #00f2ff;
    }

    .cn {
      font-weight: bold;
      letter-spacing: 1px;
    }

    .en {
      color: #00f2ff;
      font-size: 12px;
      opacity: 0.8;
    }
  }

  .func-cards {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 16px;

    .func-card {
      background: rgba(16, 30, 48, 0.6);
      backdrop-filter: blur(12px);
      border: 1px solid rgba(0, 242, 255, 0.15);
      border-radius: 4px;
      padding: 12px;
      box-shadow: inset 0 0 20px rgba(0, 242, 255, 0.05);
      cursor: pointer;
      transition: all 0.3s ease;

      .card-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 13px;
        font-weight: bold;
        margin-bottom: 12px;

        .el-tag {
          background: rgba(0, 242, 255, 0.1);
          border: none;
          color: #00f2ff;
        }
      }

      .card-body {
        height: 60px;
        display: flex;
        flex-direction: column;
        justify-content: space-around;

        .progress-item {
          display: flex;
          align-items: center;
          gap: 10px;

          span {
            font-size: 11px;
            color: #8fa3b8;
            width: 60px;
            white-space: nowrap;
          }

          .el-progress {
            flex: 1;
          }

          :deep(.el-progress-bar__outer) {
            background: rgba(255, 255, 255, 0.05);
          }
        }

        /* Simulated micro-charts */
        &.chart-mockup {
          background-position: center bottom;
          background-repeat: no-repeat;
          background-size: contain;
          opacity: 0.7;

          &.wave-line {
            background-image: linear-gradient(rgba(0, 0, 0, 0) 80%, rgba(0, 242, 255, 0.5) 81%, rgba(0, 242, 255, 0.5) 85%, rgba(0, 0, 0, 0) 86%);
            border-radius: 50%;
          }

          &.wave-area {
            background: linear-gradient(to top, rgba(0, 242, 255, 0.2), transparent);
            clip-path: polygon(0 80%, 20% 60%, 40% 70%, 60% 30%, 80% 50%, 100% 20%, 100% 100%, 0 100%);
          }
        }

        &.dual-stat {
          flex-direction: row;
          align-items: center;
          justify-content: space-between;

          .left-stat {
            font-size: 10px;
            color: #8fa3b8;

            .val {
              font-size: 14px;
              font-weight: bold;
              color: #00f2ff;
              margin-bottom: 4px;
            }
          }

          .right-gauge {
            width: 50px;
            height: 50px;
            border: 4px solid rgba(0, 242, 255, 0.2);
            border-top-color: #00f2ff;
            border-radius: 50%;
            transform: rotate(45deg);
          }
        }
      }

      &:hover {
        border-color: rgba(0, 242, 255, 0.6);
        box-shadow: inset 0 0 20px rgba(0, 242, 255, 0.2), 0 0 15px rgba(0, 242, 255, 0.1);
      }
    }
  }

  .stat-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;

    .stat-card {
      background: linear-gradient(180deg, rgba(16, 30, 48, 0.4) 0%, rgba(6, 17, 33, 0.8) 100%);
      border: 1px solid rgba(0, 242, 255, 0.2);
      border-radius: 6px;
      padding: 16px 20px;
      position: relative;
      display: flex;
      flex-direction: column;
      justify-content: center;
      min-height: 80px;
      overflow: hidden;

      /* High-tech corners */
      &::before,
      &::after {
        content: '';
        position: absolute;
        width: 10px;
        height: 10px;
        border: 2px solid #00f2ff;
      }

      &::before {
        top: 0;
        left: 0;
        border-right: none;
        border-bottom: none;
      }

      &::after {
        bottom: 0;
        right: 0;
        border-left: none;
        border-top: none;
      }

      .val {
        font-size: 28px;
        font-weight: 900;
        color: #00f2ff;
        font-family: 'Arial Black', sans-serif;
        letter-spacing: 1px;
        z-index: 2;
      }

      .label {
        font-size: 13px;
        color: #b0c9e8;
        margin-top: 4px;
        z-index: 2;
      }

      .bg-chart {
        position: absolute;
        right: 20px;
        top: 50%;
        transform: translateY(-50%);
        width: 80px;
        height: 40px;
        opacity: 0.6;
        z-index: 1;

        &.trend-up {
          background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 50"><path d="M0,45 L20,38 L40,42 L70,20 L100,8" stroke="%2300f2ff" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>') no-repeat center;
          background-size: contain;
        }

        &.gauge-mini {
          width: 46px;
          height: 46px;
          border-radius: 50%;
          border: 4px solid rgba(0, 242, 255, 0.15);
          border-top-color: #00f2ff;
          border-right-color: #00f2ff;
          transform: translateY(-50%) rotate(-45deg);
        }

        &.circle-progress {
          width: 46px;
          height: 46px;
          border-radius: 50%;
          border: 5px solid #00f2ff;
          border-left-color: rgba(0, 242, 255, 0.2);
        }

        &.heartbeat {
          background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 50"><path d="M0,25 L20,25 L30,5 L40,45 L50,15 L60,35 L70,25 L100,25" stroke="%2300f2ff" stroke-width="3" fill="none" stroke-linejoin="miter"/></svg>') no-repeat center;
          background-size: contain;
          opacity: 0.9;
        }
      }
    }
  }
}

@keyframes rotateOrbit1 {
  0% {
    transform: scale(1) rotateZ(0deg);
  }

  100% {
    transform: scale(1) rotateZ(360deg);
  }
}

@keyframes rotateOrbit2 {
  0% {
    transform: scale(1.3) rotateY(15deg) rotateZ(0deg);
  }

  100% {
    transform: scale(1.3) rotateY(15deg) rotateZ(-360deg);
  }
}

@keyframes floatNode {

  0%,
  100% {
    transform: translate(calc(-50% + var(--x)), calc(-50% + var(--y))) translateY(0);
  }

  50% {
    transform: translate(calc(-50% + var(--x)), calc(-50% + var(--y))) translateY(-15px);
  }
}
</style>
