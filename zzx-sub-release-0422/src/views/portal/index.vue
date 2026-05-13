<template>
    <div class="cyber-portal" @mousemove="handleMouseMove" @mouseenter="cursorVisible = true"
        @mouseleave="cursorVisible = false">
        <!-- Custom Cyber Cursor -->
        <div class="cyber-cursor" :class="{ 'is-active': isClicking, 'is-hidden': !cursorVisible }"
            :style="cursorStyle">
            <div class="crosshair-h"></div>
            <div class="crosshair-v"></div>
            <div class="crosshair-center"></div>
            <div class="crosshair-ring"></div>
        </div>

        <!-- 3D Background Grid -->
        <div class="perspective-grid">
            <div class="grid-plane"></div>
        </div>

        <!-- Background Atmospheric Glows -->
        <div class="ambient-glow glow-1"></div>
        <div class="ambient-glow glow-2"></div>

        <!-- Main Radar Container -->
        <div class="radar-container">

            <!-- Complex HUD Rings -->
            <div class="hud-system" :style="radarParallax">
                <div class="ring outer-ring">
                    <div class="tick-marks"></div>
                </div>
                <div class="ring dashed-ring"></div>
                <div class="ring inner-ring">
                    <div class="sector-scan"></div>
                </div>
                <div class="ring data-ring">
                    <svg viewBox="0 0 100 100" class="svg-ring">
                        <circle cx="50" cy="50" r="48" stroke="rgba(0, 242, 255, 0.3)" stroke-width="0.5" fill="none"
                            stroke-dasharray="2 4" />
                        <path d="M 50 2 A 48 48 0 0 1 98 50" stroke="#00f2ff" stroke-width="1.5" fill="none"
                            class="data-arc" />
                    </svg>
                </div>
            </div>

            <!-- Laser Pointer -->
            <div class="laser-beam-container" :style="beamStyle">
                <div class="laser-beam"></div>
                <div class="laser-hit" :style="{ opacity: distance > 80 ? 1 : 0 }"></div>
            </div>

            <!-- Central Core -->
            <div class="core-node">
                <div class="core-pulse-1"></div>
                <div class="core-pulse-2"></div>
            </div>

            <!-- Holographic Nodes (Sub-systems) -->
            <div class="hologram-nodes">
                <div v-for="(system, index) in systems" :key="index" class="hologram-card-wrapper"
                    :style="getNodePosition(index)" :class="{ 'wrapper-hovered': hoveredIndex === index }"
                    @mouseenter="onNodeHover(index)" @mouseleave="onNodeLeave">
                    <div class="hologram-card" :class="{ 'is-hovered': hoveredIndex === index }"
                        @click="navigateTo(system.path)" @mousedown="isClicking = true" @mouseup="isClicking = false">
                        <!-- Cyber Brackets -->
                        <div class="bracket tl"></div>
                        <div class="bracket tr"></div>
                        <div class="bracket bl"></div>
                        <div class="bracket br"></div>

                        <div class="card-glass">
                            <div class="icon-hexagon">
                                <el-icon :size="28">
                                    <component :is="system.icon" />
                                </el-icon>
                            </div>
                            <div class="card-info">
                                <div class="sys-id">NODE-0{{ index + 1 }}</div>
                                <h3 class="sys-name">{{ system.name }}</h3>
                                <div class="sys-status">
                                    <span class="status-dot"></span> ONLINE
                                </div>
                            </div>
                        </div>
                        <!-- Connection Line to center -->
                        <div class="tether-line"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- UI Overlays -->
        <div class="overlay-ui nav-header">
            <div class="brand">
                <span class="brand-text">INTELLIGENT EVALUATION</span>
                <span class="brand-sub">/// SYSTEM ARCHITECTURE v2.0</span>
            </div>
        </div>

        <div class="overlay-ui status-panel bottom-left">
            <div class="panel-line">SYS STAT: <span class="text-glow">NOMINAL</span></div>
            <div class="panel-line">NET LINK: <span class="text-glow">SECURE</span></div>
            <div class="panel-line mt-2">COORD X: {{ cursorX }}</div>
            <div class="panel-line">COORD Y: {{ cursorY }}</div>
        </div>

        <div class="overlay-ui datastream right-side">
            <div v-for="i in 10" :key="i" class="data-row" :style="{ animationDelay: `${Math.random() * 2}s` }">
                {{ generateRandomHex() }} : {{ generateRandomBinary() }}
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
    Connection,
    DataAnalysis,
    Filter,
    Search,
    Management,
    Coin
} from '@element-plus/icons-vue'

const router = useRouter()

// Cursor State
const cursorX = ref(0)
const cursorY = ref(0)
const cursorVisible = ref(false)
const isClicking = ref(false)

// Radar State
const centerX = ref(0)
const centerY = ref(0)
const hoveredIndex = ref(null)

// Nodes Data
const systems = [
    { name: '数据采集分发', path: '/process/reception-sys/overview', icon: Connection },
    { name: '汇总整理', path: '/process/analysis-sys/overview', icon: Management },
    { name: '数据预处理', path: '/process/process-sys/tasks', icon: Filter },
    { name: '数据挖掘', path: '/process/mining-sys/tasks', icon: DataAnalysis },
    { name: '资源管理', path: '/process/resource-sys/overview', icon: Coin },
    { name: '知识库与管理', path: '/process/knowledge-sys/ontology', icon: Search }
]

// Window Resize Handling
const updateCenter = () => {
    centerX.value = window.innerWidth / 2
    centerY.value = window.innerHeight / 2
}

onMounted(() => {
    updateCenter()
    window.addEventListener('resize', updateCenter)
    // Hide default cursor
    document.body.style.cursor = 'none'
})

onUnmounted(() => {
    window.removeEventListener('resize', updateCenter)
    document.body.style.cursor = 'auto'
})

// Mouse Interactions
const handleMouseMove = (e) => {
    cursorX.value = e.clientX
    cursorY.value = e.clientY
}

const onNodeHover = (index) => {
    hoveredIndex.value = index
    console.log(hoveredIndex.value, " hoveredIndex")
}

const onNodeLeave = () => {
    hoveredIndex.value = null
    console.log(hoveredIndex.value, " hoveredIndex")
}

const navigateTo = (path) => {
    // Reset cursor style before leaving
    document.body.style.cursor = 'auto'
    router.push(path)
}

// Computeds for Styles
const cursorStyle = computed(() => {
    return {
        transform: `translate3d(${cursorX.value}px, ${cursorY.value}px, 0)`
    }
})

const dx = computed(() => cursorX.value - centerX.value)
const dy = computed(() => cursorY.value - centerY.value)
const distance = computed(() => Math.sqrt(dx.value * dx.value + dy.value * dy.value))
const angle = computed(() => Math.atan2(dy.value, dx.value) * (180 / Math.PI))

const beamStyle = computed(() => {
    // Limit beam length to avoid going off screen unnecessarily, or stretch to cursor
    const length = Math.max(0, distance.value - 40) // -40 to start outside core
    return {
        height: '2px',
        width: `${length}px`,
        transform: `rotate(${angle.value}deg)`,
        opacity: cursorVisible.value ? (length > 10 ? 1 : 0) : 0
    }
})

const radarParallax = computed(() => {
    // Slight 3D tilt based on mouse position relative to center
    const limit = 15 // max rotation degrees
    const rotX = (dy.value / centerY.value) * -limit
    const rotY = (dx.value / centerX.value) * limit
    return {
        transform: `rotateX(${rotX}deg) rotateY(${rotY}deg)`
    }
})

// Node Positioning calculations
const getNodePosition = (index) => {
    const total = systems.length
    const radius = 320 // Orbit radius
    const nodeAngle = (index * (360 / total)) - 90 // -90 starts at 12 o'clock
    const radian = (nodeAngle * Math.PI) / 180

    const x = Math.cos(radian) * radius
    const y = Math.sin(radian) * radius

    return {
        transform: `translate(${x}px, ${y}px)`,
        '--node-angle': `${nodeAngle}deg`
    }
}

// Data Stream Generators
const generateRandomHex = () => {
    return Array.from({ length: 4 }, () => Math.floor(Math.random() * 16).toString(16).toUpperCase()).join('')
}
const generateRandomBinary = () => {
    return Array.from({ length: 8 }, () => Math.round(Math.random())).join('')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

// --- Base Styles ---
.cyber-portal {
    width: 100%;
    height: 100vh;
    background-color: #010409;
    overflow: hidden;
    position: relative;
    font-family: 'Inter', 'Roboto Mono', monospace;
    color: #fff;
    perspective: 1200px; // For 3D environment
    cursor: none !important; // Hide default cursor entirely within this container
}

// --- Custom Cyber Cursor ---
.cyber-cursor {
    position: fixed;
    top: 0;
    left: 0;
    width: 40px;
    height: 40px;
    margin-left: -20px;
    margin-top: -20px;
    pointer-events: none !important;
    z-index: 10000;
    transition: transform 0.05s linear;

    &.is-hidden {
        opacity: 0;
    }

    .crosshair-h,
    .crosshair-v {
        position: absolute;
        background: $primary-color;
        box-shadow: 0 0 8px $primary-color;
    }

    .crosshair-h {
        top: 50%;
        left: 0;
        width: 100%;
        height: 1px;
        transform: translateY(-50%);
    }

    .crosshair-v {
        left: 50%;
        top: 0;
        height: 100%;
        width: 1px;
        transform: translateX(-50%);
    }

    .crosshair-center {
        position: absolute;
        top: 50%;
        left: 50%;
        width: 4px;
        height: 4px;
        background: #fff;
        border-radius: 50%;
        transform: translate(-50%, -50%);
        box-shadow: 0 0 10px #fff;
    }

    .crosshair-ring {
        position: absolute;
        top: 50%;
        left: 50%;
        width: 24px;
        height: 24px;
        border: 1px dashed rgba(0, 242, 255, 0.5);
        border-radius: 50%;
        transform: translate(-50%, -50%);
        transition: all 0.2s ease;
    }

    &.is-active .crosshair-ring {
        width: 36px;
        height: 36px;
        border: 1px solid $primary-color;
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.4) inset;
        animation: rotate 1s linear infinite;
    }
}

// --- 3D Grid Background ---
.perspective-grid {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    perspective: 1000px;
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1;

    .grid-plane {
        width: 200%;
        height: 200%;
        background-image:
            linear-gradient(rgba(0, 242, 255, 0.05) 1px, transparent 1px),
            linear-gradient(90deg, rgba(0, 242, 255, 0.05) 1px, transparent 1px);
        background-size: 60px 60px;
        transform: rotateX(60deg) translateY(-100px);
        transform-origin: center center;
        animation: gridMove 20s linear infinite;
        mask-image: radial-gradient(circle at center, black 20%, transparent 70%);
        -webkit-mask-image: radial-gradient(circle at center, black 20%, transparent 70%);
    }
}

// --- Ambient Glows ---
.ambient-glow {
    position: absolute;
    border-radius: 50%;
    filter: blur(120px);
    pointer-events: none;
    z-index: 2;

    &.glow-1 {
        top: -10%;
        left: -10%;
        width: 50vw;
        height: 50vw;
        background: rgba(0, 242, 255, 0.08);
    }

    &.glow-2 {
        bottom: -20%;
        right: -10%;
        width: 60vw;
        height: 60vw;
        background: rgba(30, 144, 255, 0.05); // Dodger blue tint
    }
}

// --- Radar Container & Parallax ---
.radar-container {
    position: relative;
    z-index: 10;
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    transform-style: preserve-3d;
    transition: transform 0.1s ease-out;
}

// --- Complex HUD Rings ---
.hud-system {
    position: absolute;
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1;
    pointer-events: none;

    .ring {
        position: absolute;
        border-radius: 50%;
    }

    .outer-ring {
        width: 760px;
        height: 760px;
        border: 1px solid rgba(0, 242, 255, 0.15);
        animation: rotate 120s linear infinite;

        &::before {
            content: '';
            position: absolute;
            top: -1px;
            left: 50%;
            width: 10px;
            height: 10px;
            background: $primary-color;
            box-shadow: 0 0 10px $primary-color;
            transform: translateX(-50%);
        }
    }

    .dashed-ring {
        width: 620px;
        height: 620px;
        border: 2px dashed rgba(0, 242, 255, 0.1);
        animation: rotate-reverse 90s linear infinite;
    }

    .inner-ring {
        width: 480px;
        height: 480px;
        border: 1px solid rgba(0, 242, 255, 0.05);
        background: radial-gradient(circle, rgba(0, 242, 255, 0.02) 0%, transparent 70%);
        overflow: hidden;

        .sector-scan {
            position: absolute;
            top: 50%;
            left: 50%;
            width: 50%;
            height: 50%;
            transform-origin: 0 0;
            background: conic-gradient(from 0deg, rgba(0, 242, 255, 0.15) 0deg, transparent 60deg);
            animation: rotate 4s linear infinite;
        }
    }

    .data-ring {
        width: 540px;
        height: 540px;
        animation: rotate 45s cubic-bezier(0.4, 0, 0.2, 1) infinite;

        .svg-ring {
            width: 100%;
            height: 100%;
        }
    }
}

// --- Center Core ---
.core-node {
    position: absolute;
    z-index: 30;
    width: 80px;
    height: 80px;
    display: flex;
    justify-content: center;
    align-items: center;

    .core-center {
        width: 50px;
        height: 50px;
        background: #000;
        border: 2px solid $primary-color;
        box-shadow: 0 0 20px rgba(0, 242, 255, 0.4), inset 0 0 15px rgba(0, 242, 255, 0.3);
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        position: relative;
        z-index: 2;
    }

    .core-text {
        font-size: 8px;
        font-weight: bold;
        color: $primary-color;
        text-align: center;
        line-height: 1.2;
        text-shadow: 0 0 5px $primary-color;
    }

    .core-pulse-1,
    .core-pulse-2 {
        position: absolute;
        width: 100%;
        height: 100%;
        border: 1px solid $primary-color;
        border-radius: 50%;
        z-index: 1;
    }

    .core-pulse-1 {
        animation: pulseOut 2s ease-out infinite;
    }

    .core-pulse-2 {
        animation: pulseOut 2s ease-out infinite 1s; // Delay
    }
}

// --- Laser Pointer ---
.laser-beam-container {
    position: absolute;
    left: 50%; // Center of screen
    top: 50%;
    transform-origin: 0 50%; // Rotate originating from center
    z-index: 25;
    pointer-events: none;
    display: flex;
    align-items: center;

    .laser-beam {
        width: 100%;
        height: 100%;
        background: linear-gradient(90deg, rgba(0, 242, 255, 0.8), rgba(0, 242, 255, 0.1));
        box-shadow: 0 0 8px $primary-color, 0 0 15px rgba(0, 242, 255, 0.4);
        border-radius: 2px;
    }

    .laser-hit {
        position: absolute;
        right: 0;
        width: 8px;
        height: 8px;
        background: #fff;
        border-radius: 50%;
        box-shadow: 0 0 15px #fff, 0 0 25px $primary-color;
        transform: translateX(50%);
        transition: opacity 0.2s;
    }
}

// --- Holographic Nodes ---
.hologram-nodes {
    position: absolute;
    width: 0;
    height: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 40;
}

.hologram-card-wrapper {
    position: absolute;
    display: flex;
    justify-content: center;
    align-items: center;
    width: 260px;
    height: 120px;
    z-index: 40;

    // Ensure the wrapper catches all mouse events stably
    pointer-events: auto;

    &.wrapper-hovered {
        // z-index: 60;

        .hologram-card {
            transform: translateZ(30px) scale(1.05);
            background: rgba(10, 25, 45, 0.8);
            border-color: rgba(0, 242, 255, 0.5);
            // box-shadow: 0 0 25px rgba(0, 242, 255, 0.2), inset 0 0 20px rgba(0, 242, 255, 0.1);

            .tether-line {
                opacity: 0.8;
                background: linear-gradient(90deg, transparent, $primary-color);
                box-shadow: 0 0 8px $primary-color;
            }

            .icon-hexagon {
                color: #fff;
                border-color: $primary-color;
                background: rgba(0, 242, 255, 0.1);
                box-shadow: 0 0 15px $primary-color;
            }

            .sys-name {
                color: #fff;
                text-shadow: 0 0 8px $primary-color;
            }

            .bracket {
                border-color: #fff;
                // box-shadow: 0 0 8px $primary-color;
            }

            .status-dot {
                box-shadow: 0 0 10px $success-color, 0 0 20px $success-color;
            }
        }
    }
}

.hologram-card {
    position: relative;
    width: 220px;
    padding: 15px;
    background: rgba(6, 17, 33, 0.6);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(0, 242, 255, 0.1);
    box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.8), inset 0 0 20px rgba(0, 242, 255, 0.05);
    cursor: pointer;
    transform: translateZ(0); // Trigger GPU
    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    overflow: visible;

    // The Tether/Connection Line to center
    .tether-line {
        position: absolute;
        right: 100%;
        top: 50%;
        width: 120px; // Bridges gap to inner HUD rings
        height: 2px;
        background: linear-gradient(90deg, transparent, rgba(0, 242, 255, 0.2));
        transform-origin: right center;
        transform: rotate(calc(var(--node-angle) * -1 + 180deg)); // Point reverse to center
        opacity: 0.3;
        pointer-events: none;
        transition: all 0.3s ease;
    }

    // Corner Brackets
    .bracket {
        position: absolute;
        width: 15px;
        height: 15px;
        border: 2px solid $primary-color;
        opacity: 0.5;
        transition: all 0.3s ease;

        &.tl {
            top: -2px;
            left: -2px;
            border-right: none;
            border-bottom: none;
        }

        &.tr {
            top: -2px;
            right: -2px;
            border-left: none;
            border-bottom: none;
        }

        &.bl {
            bottom: -2px;
            left: -2px;
            border-right: none;
            border-top: none;
        }

        &.br {
            bottom: -2px;
            right: -2px;
            border-left: none;
            border-top: none;
        }
    }

    .card-glass {
        display: flex;
        align-items: center;
        gap: 15px;
        position: relative;
        z-index: 2;
    }
}

// Inner Card Content
.icon-hexagon {
    width: 46px;
    height: 46px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: $primary-color;
    border: 1px dashed rgba(0, 242, 255, 0.3);
    transform: rotate(45deg);
    transition: all 0.3s ease;
    flex-shrink: 0;

    .el-icon {
        transform: rotate(-45deg); // Counter-rotate icon
    }
}

.card-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.sys-id {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.4);
    letter-spacing: 2px;
}

.sys-name {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: $text-color-primary;
    transition: all 0.3s ease;
}

.sys-status {
    font-size: 10px;
    color: $success-color;
    display: flex;
    align-items: center;
    gap: 5px;
    letter-spacing: 1px;

    .status-dot {
        width: 6px;
        height: 6px;
        background: $success-color;
        border-radius: 50%;
        animation: blink 2s infinite;
    }
}

// --- UI Overlays (Edges) ---
.overlay-ui {
    position: absolute;
    z-index: 100;
    pointer-events: none;
}

.nav-header {
    top: 30px;
    left: 30px;

    .brand {
        display: flex;
        flex-direction: column;

        .brand-text {
            font-size: 20px;
            font-weight: 900;
            letter-spacing: 8px;
            color: #fff;
            text-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
        }

        .brand-sub {
            font-size: 10px;
            color: $primary-color;
            letter-spacing: 4px;
            margin-top: 5px;
            opacity: 0.8;
        }
    }
}

.status-panel {
    color: rgba(255, 255, 255, 0.6);
    font-size: 11px;
    letter-spacing: 1px;

    &.bottom-left {
        bottom: 30px;
        left: 30px;
    }

    .panel-line {
        margin-bottom: 4px;
    }

    .mt-2 {
        margin-top: 15px;
    }

    .text-glow {
        color: $primary-color;
        text-shadow: 0 0 5px $primary-color;
    }
}

.datastream {
    &.right-side {
        top: 50%;
        right: 30px;
        transform: translateY(-50%);
        text-align: right;
    }

    .data-row {
        font-size: 10px;
        color: rgba(0, 242, 255, 0.3);
        margin-bottom: 6px;
        font-family: 'Roboto Mono', monospace;
        animation: pulse 2s infinite alternate;
    }
}

// --- Keyframes ---
@keyframes rotate {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}

@keyframes rotate-reverse {
    from {
        transform: rotate(360deg);
    }

    to {
        transform: rotate(0deg);
    }
}

@keyframes pulseOut {
    0% {
        transform: scale(1);
        opacity: 0.8;
        border-width: 2px;
    }

    100% {
        transform: scale(3);
        opacity: 0;
        border-width: 0px;
    }
}

@keyframes blink {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.4;
    }
}

@keyframes gridMove {
    from {
        background-position: 0 0;
    }

    to {
        background-position: 0 60px;
    }
}

@keyframes pulse {
    from {
        opacity: 0.2;
    }

    to {
        opacity: 0.7;
    }
}
</style>
