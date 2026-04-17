<template>
    <div class="import-page">
        <!-- 页面标题 -->
        <div class="page-header">
            <h2 class="title">导入一案三纲</h2>
            <p class="subtitle">导入试验任务书、试验大纲、技术协议等基础文件，系统将自动解析任务背景与评估目标</p>
        </div>

        <!-- 左右分栏主体 -->
        <div class="main-layout">
            <!-- 左侧：文件上传区 -->
            <div class="left-panel">
                <div class="panel-title">
                    <el-icon>
                        <Upload />
                    </el-icon>
                    <span>文件上传</span>
                </div>
                <el-upload class="cyber-upload" drag action="#" :auto-upload="false" :on-change="handleFileChange"
                    :show-file-list="false" multiple>
                    <div class="upload-body">
                        <el-icon class="upload-icon">
                            <Document />
                        </el-icon>
                        <p class="upload-text">拖拽文件到此处</p>
                        <p class="upload-subtext">或 <em>点击选择文件</em></p>
                        <p class="upload-hint">.doc .docx .pdf .txt，单文件 ≤ 50MB</p>
                    </div>
                </el-upload>

                <!-- 文件列表 -->
                <div v-if="fileList.length > 0" class="file-list">
                    <div class="file-list-title">已添加 {{ fileList.length }} 个文件</div>
                    <transition-group name="file-item-anim" tag="div">
                        <div class="file-item" v-for="(file, index) in fileList" :key="file.uid || index">
                            <el-icon class="file-ico">
                                <Document />
                            </el-icon>
                            <div class="file-meta">
                                <span class="file-name" :title="file.name">{{ file.name }}</span>
                                <span class="file-size">{{ formatSize(file.size) }}</span>
                            </div>
                            <el-icon class="file-del" @click.stop="removeFile(index)">
                                <CircleClose />
                            </el-icon>
                        </div>
                    </transition-group>
                </div>

                <!-- 解析按钮 -->
                <div class="parse-action" v-if="fileList.length > 0 && !isParsing && !showResults">
                    <el-button class="cyber-btn-parse" @click="startParsing">
                        <el-icon>
                            <Cpu />
                        </el-icon>
                        智能提取
                    </el-button>
                </div>

                <!-- 重新解析 -->
                <div class="parse-action" v-if="showResults">
                    <el-button class="cyber-btn-reparse" @click="reparse">
                        <el-icon>
                            <Refresh />
                        </el-icon>
                        重新解析
                    </el-button>
                </div>
            </div>

            <!-- 右侧：解析结果区 -->
            <div class="right-panel">
                <!-- 空状态 -->
                <div v-if="!isParsing && !showResults" class="empty-state">
                    <div class="empty-icon-wrap">
                        <el-icon class="empty-icon">
                            <Memo />
                        </el-icon>
                    </div>
                    <p class="empty-title">等待文件上传</p>
                    <p class="empty-desc">上传文件后点击"智能提取"，系统将自动解析并填充任务信息</p>
                </div>

                <!-- 解析动画 -->
                <div v-if="isParsing" class="parsing-state">
                    <RobotAnimation />
                </div>

                <!-- 解析结果表单 -->
                <div v-if="showResults" class="result-state">
                    <div class="result-header">
                        <span class="result-title">
                            <el-icon>
                                <CircleCheckFilled />
                            </el-icon>
                            结构化解析结果确认
                        </span>
                        <span class="parse-badge">解析成功</span>
                    </div>

                    <el-form :model="formData" label-position="top" class="cyber-form">
                        <div class="form-section-title">任务基本信息</div>
                        <div class="form-grid">
                            <el-form-item label="装备名称">
                                <el-input v-model="formData.equipName" placeholder="如: 某型雷达系统" />
                            </el-form-item>
                            <el-form-item label="型号">
                                <el-input v-model="formData.modelCode" placeholder="如: X-104A" />
                            </el-form-item>
                            <el-form-item label="评估层级">
                                <el-select v-model="formData.evalLevel" placeholder="请选择层级" style="width:100%">
                                    <el-option label="系统级" value="system" />
                                    <el-option label="分系统级" value="subsystem" />
                                    <el-option label="部件级" value="component" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="试验类型">
                                <el-select v-model="formData.testType" placeholder="请选择类型" style="width:100%">
                                    <el-option label="鉴定定型试验" value="type1" />
                                    <el-option label="科研试验" value="type2" />
                                    <el-option label="在役考核试验" value="type3" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="评估阶段">
                                <el-select v-model="formData.evalPhase" placeholder="请选择阶段" style="width:100%">
                                    <el-option label="方案设计阶段" value="p1" />
                                    <el-option label="初样阶段" value="p2" />
                                    <el-option label="正样阶段" value="p3" />
                                    <el-option label="试样阶段" value="p4" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="主要功能">
                                <el-input v-model="formData.functions" placeholder="如: 远程探测、抗干扰截获" />
                            </el-form-item>
                            <el-form-item label="关联单位">
                                <el-input v-model="formData.organization" placeholder="如: 第五研究院" />
                            </el-form-item>
                            <el-form-item label="试验周期">
                                <el-date-picker v-model="formData.testPeriod" type="daterange" range-separator="至"
                                    start-placeholder="开始日期" end-placeholder="结束日期" style="width:100%" />
                            </el-form-item>
                        </div>

                        <div class="form-section-title" style="margin-top:20px">评估目标与约束条件</div>
                        <el-form-item label="评估目标">
                            <el-input v-model="formData.evalTargets" type="textarea" :rows="4"
                                placeholder="提取的评估目标..." />
                        </el-form-item>
                        <el-form-item label="约束条件">
                            <el-input v-model="formData.constraints" type="textarea" :rows="3"
                                placeholder="场地、气候、资源约束等..." />
                        </el-form-item>
                    </el-form>

                    <!-- 底部操作 -->
                    <div class="bottom-actions">
                        <el-button class="btn-draft" @click="saveDraft">
                            <el-icon>
                                <CopyDocument />
                            </el-icon>
                            保存草稿
                        </el-button>
                        <el-button class="btn-next" @click="proceedNext">
                            下一步：生成评估需求
                            <el-icon>
                                <ArrowRight />
                            </el-icon>
                        </el-button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
    Document, Cpu, CircleClose, Upload, Memo, SuccessFilled,
    Refresh, CircleCheckFilled, CopyDocument, ArrowRight
} from '@element-plus/icons-vue'
import RobotAnimation from '@/components/RobotAnimation.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const fileList = ref([])
const isParsing = ref(false)
const showResults = ref(false)

const formData = ref({
    equipName: '',
    modelCode: '',
    evalLevel: '',
    testType: '',
    evalPhase: '',
    functions: '',
    organization: '',
    testPeriod: [],
    evalTargets: '',
    constraints: ''
})

const handleFileChange = (uploadFile) => {
    const isNew = !fileList.value.some(f => f.name === uploadFile.name)
    if (isNew) fileList.value.push(uploadFile)
}

const removeFile = (index) => {
    fileList.value.splice(index, 1)
    if (fileList.value.length === 0) {
        showResults.value = false
    }
}

const formatSize = (bytes) => {
    if (!bytes || bytes === 0) return '未知大小'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const reparse = () => {
    showResults.value = false
    parseSteps.value.forEach(s => { s.done = false; s.active = false })
}

const startParsing = () => {
    isParsing.value = true
    showResults.value = false

    setTimeout(() => {
        isParsing.value = false
        showResults.value = true
        formData.value = {
            equipName: '新型相控阵雷达系统',
            modelCode: 'XK-2026A',
            evalLevel: 'system',
            testType: 'type1',
            evalPhase: 'p3',
            functions: '多目标跟踪、抗干扰探测、复杂电磁截获',
            organization: '中国科学院某研究所',
            testPeriod: [new Date(), new Date(Date.now() + 30 * 24 * 3600 * 1000)],
            evalTargets: '1. 验证目标发现在复杂干扰下的置信区间与距离限制。\n2. 评估高低温极限环境连贯开机工作的稳定性。\n3. 保障各接口协议完全通过军标验证。',
            constraints: '空间：海拔超过3000m地区不适用\n频段：限定在特定频率使用\n保障：需要独立电源供电车配组。'
        }
        ElMessage.success('智能提取完成，请核对信息。')
    }, 3100)
}

const saveDraft = () => {
    ElMessage.success('草稿已保存。')
}

const proceedNext = () => {
    ElMessage.success('数据已保存，正在跳转...')
    router.push('/major/requirement-sys/generate')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.import-page {
    padding: 24px;
    color: #e6edf3;
    display: flex;
    flex-direction: column;
    height: 100%;
    box-sizing: border-box;
}

.page-header {
    margin-bottom: 20px;
    flex-shrink: 0;

    .title {
        font-size: 22px;
        font-weight: 700;
        color: #fff;
        text-shadow: 0 0 12px rgba(0, 242, 255, 0.5);
        margin: 0 0 6px 0;
    }

    .subtitle {
        color: #6b8096;
        font-size: 13px;
        line-height: 1.6;
    }
}

.main-layout {
    display: flex;
    gap: 20px;
    flex: 1;
    min-height: 0;
    overflow: hidden;
}

/* ===== 左侧面板 ===== */
.left-panel {
    width: 320px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
    overflow-y: auto;
}

.panel-title {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #00f2ff;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 1px;

    .el-icon {
        font-size: 16px;
    }
}

:deep(.cyber-upload) {
    width: 100%;

    .el-upload {
        width: 100%;
    }

    .el-upload-dragger {
        width: 100%;
        background: rgba(6, 13, 23, 0.8);
        border: 1px dashed rgba(0, 242, 255, 0.4);
        border-radius: 8px;
        padding: 24px 16px;
        transition: all 0.3s ease;

        &:hover {
            border-color: #00f2ff;
            background: rgba(0, 242, 255, 0.04);
            box-shadow: 0 0 16px rgba(0, 242, 255, 0.15);
        }
    }
}

.upload-body {
    text-align: center;

    .upload-icon {
        font-size: 36px;
        color: #00f2ff;
        filter: drop-shadow(0 0 6px rgba(0, 242, 255, 0.7));
        margin-bottom: 10px;
    }

    .upload-text {
        color: #cdd9e5;
        font-size: 14px;
        margin: 0 0 4px;
    }

    .upload-subtext {
        color: #6b8096;
        font-size: 13px;
        margin: 0 0 8px;

        em {
            color: #00f2ff;
            font-style: normal;
            cursor: pointer;
        }
    }

    .upload-hint {
        color: #4a5568;
        font-size: 11px;
        margin: 0;
    }
}

.file-list {
    .file-list-title {
        color: #6b8096;
        font-size: 12px;
        margin-bottom: 8px;
        padding-left: 2px;
    }
}

.file-item {
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.02);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 6px;
    padding: 8px 12px;
    margin-bottom: 6px;
    transition: all 0.25s ease;
    gap: 10px;

    &:hover {
        border-color: rgba(0, 242, 255, 0.3);
        background: rgba(0, 242, 255, 0.04);

        .file-del {
            opacity: 1;
        }
    }

    .file-ico {
        color: #00f2ff;
        font-size: 16px;
        flex-shrink: 0;
    }

    .file-meta {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 2px;

        .file-name {
            color: #cdd9e5;
            font-size: 13px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .file-size {
            color: #4a5568;
            font-size: 11px;
        }
    }

    .file-del {
        color: #f85149;
        font-size: 16px;
        cursor: pointer;
        opacity: 0;
        transition: all 0.2s;
        flex-shrink: 0;

        &:hover {
            transform: scale(1.2);
        }
    }
}

.file-item-anim-enter-active,
.file-item-anim-leave-active {
    transition: all 0.25s ease;
}

.file-item-anim-enter-from {
    opacity: 0;
    transform: translateX(-10px);
}

.file-item-anim-leave-to {
    opacity: 0;
    transform: translateX(10px);
}

.parse-action {
    display: flex;
    justify-content: center;
}

.cyber-btn-parse {
    width: 100%;
    background: linear-gradient(135deg, rgba(0, 242, 255, 0.15), rgba(0, 136, 255, 0.15));
    border: 1px solid rgba(0, 242, 255, 0.5);
    color: #00f2ff;
    font-size: 14px;
    padding: 10px 0;
    letter-spacing: 2px;
    transition: all 0.3s;

    &:hover {
        background: linear-gradient(135deg, rgba(0, 242, 255, 0.25), rgba(0, 136, 255, 0.25));
        box-shadow: 0 0 20px rgba(0, 242, 255, 0.3);
        transform: translateY(-1px);
    }
}

.cyber-btn-reparse {
    width: 100%;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #6b8096;
    font-size: 13px;
    transition: all 0.3s;

    &:hover {
        border-color: rgba(0, 242, 255, 0.3);
        color: #00f2ff;
    }
}

/* ===== 右侧面板 ===== */
.right-panel {
    flex: 1;
    background: rgba(10, 16, 26, 0.7);
    border: 1px solid rgba(0, 242, 255, 0.12);
    border-radius: 10px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    position: relative;
}

/* 空状态 */
.empty-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;

    .empty-icon-wrap {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: rgba(0, 242, 255, 0.05);
        border: 1px solid rgba(0, 242, 255, 0.15);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 20px;
    }

    .empty-icon {
        font-size: 36px;
        color: rgba(0, 242, 255, 0.3);
    }

    .empty-title {
        color: #4a5568;
        font-size: 15px;
        margin: 0 0 10px;
    }

    .empty-desc {
        color: #3a4555;
        font-size: 13px;
        text-align: center;
        line-height: 1.7;
        max-width: 300px;
        margin: 0;
    }
}

/* 解析动画状态：RobotAnimation 撑满容器 */
.parsing-state {
    flex: 1;
    display: flex;
    align-items: stretch;
    overflow: hidden;
}

:deep(.robot-animation-container) {
    flex: 1;
    min-height: 0;
}

/* 解析结果状态 */
.result-state {
    flex: 1;
    padding: 24px;
    display: flex;
    flex-direction: column;
    animation: result-in 0.4s ease-out;
}

@keyframes result-in {
    from {
        opacity: 0;
        transform: translateY(8px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 1px solid rgba(0, 242, 255, 0.1);

    .result-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: #00f2ff;

        .el-icon {
            color: #3fb950;
            font-size: 18px;
        }
    }

    .parse-badge {
        background: rgba(63, 185, 80, 0.1);
        color: #3fb950;
        border: 1px solid rgba(63, 185, 80, 0.3);
        padding: 3px 10px;
        border-radius: 10px;
        font-size: 12px;
    }
}

.form-section-title {
    color: #6b8096;
    font-size: 12px;
    letter-spacing: 1.5px;
    text-transform: uppercase;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 8px;

    &::before {
        content: '';
        display: inline-block;
        width: 3px;
        height: 12px;
        background: #00f2ff;
        box-shadow: 0 0 6px #00f2ff;
        border-radius: 2px;
    }
}

.cyber-form {
    flex: 1;

    .form-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 0 24px;
    }

    :deep(.el-form-item__label) {
        color: #6b8096;
        font-size: 12px;
        font-weight: normal;
        padding-bottom: 4px;
    }

    :deep(.el-input__wrapper) {
        background: rgba(6, 13, 23, 0.9);
        box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;
        transition: all 0.25s;

        &:hover {
            box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset;
        }

        &.is-focus {
            box-shadow: 0 0 0 1px #00f2ff inset !important;
        }
    }

    :deep(.el-input__inner) {
        color: #cdd9e5;
        font-size: 13px;

        &::placeholder {
            color: #3a4555;
        }
    }

    :deep(.el-textarea__inner) {
        background: rgba(6, 13, 23, 0.9);
        box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;
        color: #cdd9e5;
        font-size: 13px;
        resize: none;

        &::placeholder {
            color: #3a4555;
        }

        &:focus {
            box-shadow: 0 0 0 1px #00f2ff inset;
        }
    }

    :deep(.el-select__wrapper) {
        background: rgba(6, 13, 23, 0.9);
        box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;

        &:hover {
            box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset;
        }
    }

    :deep(.el-date-editor.el-input__wrapper) {
        background: rgba(6, 13, 23, 0.9);
        box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;

        &:hover {
            box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset;
        }
    }
}

.bottom-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 20px;
    margin-top: 16px;
    border-top: 1px solid rgba(0, 242, 255, 0.08);

    .btn-draft {
        background: transparent;
        border: 1px solid rgba(255, 255, 255, 0.12);
        color: #8fa3b8;
        transition: all 0.3s;

        &:hover {
            border-color: rgba(255, 255, 255, 0.25);
            color: #fff;
        }
    }

    .btn-next {
        background: linear-gradient(90deg, #00c9ff 0%, #0072ff 100%);
        border: none;
        color: #ffffff;
        font-weight: 600;
        letter-spacing: 1px;
        padding: 8px 24px;
        box-shadow: 0 0 16px rgba(0, 114, 255, 0.4);
        transition: all 0.3s;

        &:hover {
            box-shadow: 0 0 28px rgba(0, 114, 255, 0.7);
            transform: translateY(-1px);
        }
    }
}

/* 滚动条 */
.left-panel::-webkit-scrollbar,
.right-panel::-webkit-scrollbar {
    width: 4px;
}

.left-panel::-webkit-scrollbar-track,
.right-panel::-webkit-scrollbar-track {
    background: transparent;
}

.left-panel::-webkit-scrollbar-thumb,
.right-panel::-webkit-scrollbar-thumb {
    background: rgba(0, 242, 255, 0.2);
    border-radius: 2px;
}
</style>
