<template>
    <div class="robot-animation-container">
        <div class="robot-scene">
            <!-- Data Particles -->
            <div class="particles">
                <div class="particle p1"></div>
                <div class="particle p2"></div>
                <div class="particle p3"></div>
                <div class="particle p4"></div>
                <div class="particle p5"></div>
            </div>

            <!-- Central AI Core/Robot Head -->
            <div class="ai-core">
                <div class="core-ring outer"></div>
                <div class="core-ring inner">
                    <div class="eye"></div>
                    <div class="eye right"></div>
                </div>
                <!-- Scanning Beam -->
                <div class="scan-beam"></div>
            </div>

            <!-- Connecting Data Lines -->
            <div class="data-lines">
                <div class="line l1"></div>
                <div class="line l2"></div>
                <div class="line l3"></div>
            </div>

            <!-- Floating Text Grid -->
            <div class="matrix-grid">
                <div class="code-stream s1">0101101</div>
                <div class="code-stream s2">EXTRACTING</div>
                <div class="code-stream s3">ANALYZING...</div>
            </div>
        </div>

        <div class="status-text">
            <h3 class="glitch-text" data-text="正在深度解析需求语义...">正在深度解析需求语义...</h3>
            <p class="sub-text">语义抽取 / 参数规则生成</p>
            <el-progress :percentage="progress" :show-text="false" status="success" :stroke-width="4"
                class="custom-progress"></el-progress>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const progress = ref(0)
let timer = null

onMounted(() => {
    // Simulate progress bar filling up over 3 seconds
    timer = setInterval(() => {
        if (progress.value < 90) {
            // fast to 90
            progress.value += Math.random() * 50
            if (progress.value > 90) progress.value = 90
        } else if (progress.value < 98) {
            // slow down at the end
            progress.value += 1
        }
    }, 300)
})

onUnmounted(() => {
    if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.robot-animation-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: radial-gradient(circle at center, rgba(0, 242, 255, 0.05) 0%, transparent 70%);
}

.robot-scene {
    position: relative;
    width: 300px;
    height: 300px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* --- Central Core --- */
.ai-core {
    position: relative;
    width: 120px;
    height: 120px;
    border-radius: 50%;
    z-index: 10;
    box-shadow: 0 0 30px rgba(0, 242, 255, 0.2);
    animation: float 4s ease-in-out infinite;

    .core-ring {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        border-radius: 50%;
        border: 2px solid transparent;
    }

    .outer {
        border-top-color: $primary-color;
        border-bottom-color: $primary-color;
        animation: spin 3s linear infinite;
        box-shadow: inset 0 0 15px rgba(0, 242, 255, 0.5);
    }

    .inner {
        margin: 15px;
        background: rgba(10, 25, 47, 0.8);
        border: 1px solid rgba(0, 242, 255, 0.3);
        display: flex;
        align-items: center;
        justify-content: space-evenly;
        overflow: hidden;

        /* Robot Eyes */
        .eye {
            width: 25px;
            height: 10px;
            background: $success-color;
            border-radius: 10px;
            box-shadow: 0 0 10px $success-color;
            animation: blink 4s infinite, pulse-eye 2s infinite alternate;
        }
    }

    /* Scanning beam from bottom */
    .scan-beam {
        position: absolute;
        bottom: -150px;
        left: 50%;
        transform: translateX(-50%);
        width: 150px;
        height: 150px;
        background: linear-gradient(to bottom, rgba(0, 242, 255, 0.3), transparent);
        clip-path: polygon(50% 0, 100% 100%, 0 100%);
        animation: scan 2s ease-in-out infinite alternate;
        opacity: 0.6;
        pointer-events: none;
    }
}

/* --- Particles --- */
.particles {
    position: absolute;
    width: 100%;
    height: 100%;

    .particle {
        position: absolute;
        width: 6px;
        height: 6px;
        background: $primary-color;
        border-radius: 50%;
        box-shadow: 0 0 10px $primary-color;
    }

    .p1 {
        top: 20%;
        left: 20%;
        animation: orbit 4s linear infinite;
    }

    .p2 {
        top: 80%;
        left: 30%;
        animation: orbit 3s linear infinite reverse;
    }

    .p3 {
        top: 30%;
        right: 20%;
        animation: orbit 5s linear infinite;
    }

    .p4 {
        bottom: 20%;
        right: 25%;
        animation: orbit 6s linear infinite reverse;
    }

    .p5 {
        top: 10%;
        left: 50%;
        animation: float-up 3s ease-in-out infinite alternate;
    }
}

/* --- Data Lines --- */
.data-lines {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    border: 1px dashed rgba(0, 242, 255, 0.2);
    animation: spin 10s linear infinite reverse;

    .line {
        position: absolute;
        background: $primary-color;
        border-radius: 5px;
        box-shadow: 0 0 8px $primary-color;
    }

    .l1 {
        top: 50%;
        left: -20px;
        width: 40px;
        height: 2px;
    }

    .l2 {
        top: -20px;
        left: 50%;
        width: 2px;
        height: 40px;
    }

    .l3 {
        top: 50%;
        right: -20px;
        width: 40px;
        height: 2px;
    }
}

/* --- Matrix Grid --- */
.matrix-grid {
    position: absolute;
    width: 100%;
    height: 100%;
    overflow: hidden;
    z-index: 1;

    .code-stream {
        position: absolute;
        color: rgba(0, 242, 255, 0.4);
        font-family: monospace;
        font-size: 10px;
        writing-mode: vertical-rl;
        animation: fall linear infinite;
    }

    .s1 {
        left: 10%;
        animation-duration: 3s;
    }

    .s2 {
        left: 50%;
        top: -50px;
        animation-duration: 5s;
        animation-delay: 1s;
    }

    .s3 {
        right: 15%;
        top: -20px;
        animation-duration: 4s;
        animation-delay: 0.5s;
        font-size: 14px;
    }
}

/* --- Status Text --- */
.status-text {
    margin-top: 50px;
    text-align: center;
    width: 300px;

    h3 {
        margin: 0 0 10px 0;
        color: $text-color-primary;
        font-weight: 400;
        letter-spacing: 1px;
    }

    .sub-text {
        font-size: 12px;
        color: $primary-color;
        margin-bottom: 20px;
        animation: pulse-text 2s infinite alternate;
    }
}

.custom-progress {
    :deep(.el-progress-bar__inner) {
        background-color: $success-color;
        background-image: linear-gradient(45deg,
                rgba(255, 255, 255, 0.15) 25%,
                transparent 25%,
                transparent 50%,
                rgba(255, 255, 255, 0.15) 50%,
                rgba(255, 255, 255, 0.15) 75%,
                transparent 75%,
                transparent);
        background-size: 1rem 1rem;
        animation: progress-stripe 1s linear infinite;
    }
}

/* --- Animations --- */
@keyframes spin {
    100% {
        transform: rotate(360deg);
    }
}

@keyframes float {

    0%,
    100% {
        transform: translateY(0);
    }

    50% {
        transform: translateY(-15px);
    }
}

@keyframes float-up {
    0% {
        transform: translateY(10px);
        opacity: 0.2;
    }

    100% {
        transform: translateY(-10px);
        opacity: 1;
    }
}

@keyframes blink {

    0%,
    96%,
    98% {
        transform: scaleY(1);
    }

    97%,
    99% {
        transform: scaleY(0.1);
    }
}

@keyframes pulse-eye {
    from {
        box-shadow: 0 0 5px $success-color;
        background: rgba(0, 255, 0, 0.6);
    }

    to {
        box-shadow: 0 0 20px $success-color;
        background: $success-color;
    }
}

@keyframes scan {
    from {
        transform: translateX(-50%) rotate(-15deg);
    }

    to {
        transform: translateX(-50%) rotate(15deg);
    }
}

@keyframes orbit {
    from {
        transform: rotate(0deg) translateX(50px) rotate(0deg);
    }

    to {
        transform: rotate(360deg) translateX(50px) rotate(-360deg);
    }
}

@keyframes fall {
    from {
        transform: translateY(-100%);
        opacity: 1;
    }

    to {
        transform: translateY(300px);
        opacity: 0;
    }
}

@keyframes pulse-text {
    from {
        opacity: 0.5;
    }

    to {
        opacity: 1;
    }
}

@keyframes progress-stripe {
    from {
        background-position: 1rem 0;
    }

    to {
        background-position: 0 0;
    }
}
</style>
