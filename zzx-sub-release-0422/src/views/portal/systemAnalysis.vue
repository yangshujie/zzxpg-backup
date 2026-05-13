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
                <!-- Data Ring Removed for a cleaner look or modified -->
                <div class="ring data-ring">
                    <svg viewBox="0 0 100 100" class="svg-ring">
                        <circle cx="50" cy="50" r="48" stroke="rgba(0, 242, 255, 0.3)" stroke-width="0.5" fill="none"
                            stroke-dasharray="2 4" />
                        <path d="M 50 2 A 48 48 0 0 1 98 50" stroke="#00f2ff" stroke-width="1.5" fill="none"
                            class="data-arc" />
                    </svg>
                </div>
            </div>

            <!-- Central Core -->
            <div class="core-node">
                <div class="core-pulse-1"></div>
                <div class="core-pulse-2"></div>
                <div class="core-center">
                    <div class="core-text">MAIN<br>HUB</div>
                </div>
            </div>

            <!-- Holographic Nodes (Sub-systems) -->
            <div class="hologram-nodes">
                <div v-for="(system, index) in systems" :key="index" class="hologram-card-wrapper"
                    :style="getNodePosition(index)" :class="{ 'wrapper-hovered': hoveredIndex === index }"
                    @mouseenter="onNodeHover(index)" @mouseleave="onNodeLeave" @click="navigateTo(system.path)">
                    <div class="hologram-card" :class="{ 'is-hovered': hoveredIndex === index }"
                        @mousedown="isClicking = true" @mouseup="isClicking = false">
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
                <span class="brand-text">UNIFIED BATTLESPACE</span>
                <span class="brand-sub">/// COMMAND & CONTROL CENTER v3.0</span>
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
    Coin,
    Monitor,
    DataLine,
    Document
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

// Revised Nodes Data
const systems = [
    { name: '评估需求设计', path: '/major/requirement-sys', icon: Document },
    { name: '评估工程管理', path: '/major/program-mgmt/list', icon: Monitor }, 
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
}

const onNodeLeave = () => {
    hoveredIndex.value = null
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
    // Made radius slightly larger since we have 8 items now
    const radius = 340
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
    perspective: 1200px;
    cursor: none !important;
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

@keyframes gridMove {
    0% {
        background-position: 0 0;
    }

    100% {
        background-position: 0 60px;
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
        width: 800px;
        height: 800px;
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
        width: 660px;
        height: 660px;
        border: 2px dashed rgba(0, 242, 255, 0.1);
        animation: rotate-reverse 90s linear infinite;
    }

    .inner-ring {
        width: 520px;
        height: 520px;
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
        width: 580px;
        height: 580px;
        animation: rotate 45s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        opacity: 0.6;

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
    width: 100px;
    height: 100px;
    display: flex;
    justify-content: center;
    align-items: center;

    .core-center {
        width: 70px;
        height: 70px;
        background: #000;
        border: 2px solid $primary-color;
        box-shadow: 0 0 25px rgba(0, 242, 255, 0.5), inset 0 0 15px rgba(0, 242, 255, 0.4);
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        position: relative;
        z-index: 2;
    }

    .core-text {
        font-size: 14px;
        font-weight: bold;
        color: $primary-color;
        text-align: center;
        line-height: 1.2;
        text-shadow: 0 0 5px $primary-color;
        letter-spacing: 2px;
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

@keyframes pulseOut {
    0% {
        transform: scale(1);
        opacity: 1;
    }

    100% {
        transform: scale(2.5);
        opacity: 0;
    }
}

// --- Laser Pointer - Removed for cleaner UI in new portal ---

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
        z-index: 60;

        .hologram-card {
            transform: translateZ(40px) scale(1.1);
            background: rgba(10, 25, 45, 0.9);
            border-color: rgba(0, 242, 255, 0.8);
            box-shadow: 0 0 30px rgba(0, 242, 255, 0.3), inset 0 0 20px rgba(0, 242, 255, 0.2);

            .tether-line {
                opacity: 1;
                background: linear-gradient(90deg, transparent, $primary-color);
                box-shadow: 0 0 10px $primary-color;
                height: 3px;
            }

            .icon-hexagon {
                color: #fff;
                border-color: #fff;
                background: rgba(0, 242, 255, 0.2);
                box-shadow: 0 0 20px $primary-color;
            }

            .sys-name {
                color: #fff;
                text-shadow: 0 0 10px $primary-color;
                transform: scale(1.05);
                transform-origin: left;
            }

            .bracket {
                border-color: #fff;
                box-shadow: 0 0 10px $primary-color;
                width: 20px;
                height: 20px;
            }

            .status-dot {
                box-shadow: 0 0 10px $success-color, 0 0 20px $success-color;
                background: #fff;
            }
        }
    }
}

.hologram-card {
    position: relative;
    width: 220px;
    padding: 15px;
    background: rgba(6, 17, 33, 0.7);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(0, 242, 255, 0.2);
    box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.8), inset 0 0 20px rgba(0, 242, 255, 0.05);
    cursor: pointer;
    transform: translateZ(0); // Trigger GPU
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    overflow: visible;

    // The Tether/Connection Line to center
    .tether-line {
        position: absolute;
        right: 100%;
        top: 50%;
        width: 140px; // Bridges gap to inner HUD rings
        height: 2px;
        background: linear-gradient(90deg, transparent, rgba(0, 242, 255, 0.3));
        transform-origin: right center;
        transform: rotate(calc(var(--node-angle) * -1 + 180deg)); // Point reverse to center
        opacity: 0.4;
        pointer-events: none;
        transition: all 0.3s ease;
    }

    // Corner Brackets
    .bracket {
        position: absolute;
        width: 15px;
        height: 15px;
        border: 2px solid $primary-color;
        opacity: 0.7;
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
    width: 48px;
    height: 48px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: $primary-color;
    border: 1px dashed rgba(0, 242, 255, 0.5);
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
    color: rgba(255, 255, 255, 0.5);
    letter-spacing: 2px;
}

.sys-name {
    margin: 0;
    font-size: 15px;
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
        transition: all 0.3s ease;
    }
}

// --- UI Overlays (Edges) ---
.overlay-ui {
    position: absolute;
    z-index: 100;
    pointer-events: none;
}

.nav-header {
    top: 40px;
    left: 40px;

    .brand {
        display: flex;
        flex-direction: column;

        .brand-text {
            font-size: 24px;
            font-weight: 900;
            letter-spacing: 8px;
            color: #fff;
            text-shadow: 0 0 15px rgba(0, 242, 255, 0.6);
        }

        .brand-sub {
            font-size: 11px;
            color: $primary-color;
            letter-spacing: 5px;
            margin-top: 8px;
            opacity: 0.9;
        }
    }
}

.status-panel {
    color: rgba(255, 255, 255, 0.7);
    font-size: 12px;
    letter-spacing: 1.5px;
    font-family: 'Roboto Mono', monospace;

    &.bottom-left {
        bottom: 40px;
        left: 40px;
    }

    .panel-line {
        margin-bottom: 6px;
    }

    .mt-2 {
        margin-top: 15px;
    }

    .text-glow {
        color: $success-color;
        text-shadow: 0 0 8px $success-color;
        font-weight: bold;
    }
}

.datastream {
    &.right-side {
        top: 40px;
        right: 40px;
        text-align: right;
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .data-row {
        font-size: 10px;
        color: rgba(0, 242, 255, 0.4);
        font-family: 'Roboto Mono', monospace;
        letter-spacing: 1px;
        animation: dataPulse 4s infinite;
    }
}

@keyframes rotate {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}

@keyframes rotate-reverse {
    0% {
        transform: rotate(360deg);
    }

    100% {
        transform: rotate(0deg);
    }
}

@keyframes blink {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.3;
    }
}

@keyframes dataPulse {

    0%,
    100% {
        opacity: 0.4;
    }

    50% {
        opacity: 0.8;
        color: rgba(0, 242, 255, 0.8);
    }
}
</style>
