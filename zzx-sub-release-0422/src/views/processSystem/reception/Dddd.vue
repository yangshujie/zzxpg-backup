<template>
    <div class="designer-container">
        <div class="designer-header">
            <div class="title">表单可视化建模设计器 <span class="v-tag">v2.1</span></div>
            <div class="actions">
                <el-button-group>
                    <el-button icon="Check" plain>保存模板</el-button>
                    <el-button icon="Check" type="primary" @click="handleGenerate">生成</el-button>
                </el-button-group>
                <el-button icon="Close" circle style="margin-left: 12px;"></el-button>
            </div>
        </div>

        <div class="designer-body">
            <!-- Left Panel: Components -->
            <aside class="side-panel left-panel">
                <div class="panel-title">标准库组件</div>
                <div class="component-group" v-for="group in componentLibrary" :key="group.title">
                    <div class="group-label">{{ group.title }}</div>
                    <div class="component-items">
                        <div v-for="comp in group.items" :key="comp.type" class="comp-item" draggable="true"
                            @dragstart="onDragStart($event, comp, 'library')">
                            <el-icon>
                                <component :is="comp.icon" />
                            </el-icon>
                            <span>{{ comp.name }}</span>
                        </div>
                    </div>
                </div>
            </aside>

            <!-- Center: Canvas -->
            <main class="designer-canvas">
                <div class="canvas-header">
                    <div class="form-title-edit">
                        <el-input v-model="formTitle" size="large" class="title-input" />
                    </div>
                </div>
                <div class="canvas-content drop-zone" @dragover.prevent="onDragOverCanvas" @drop="onDropCanvas($event)">
                    <div v-if="canvasItems.length === 0" class="empty-placeholder">
                        <div class="icon-pulse">
                            <el-icon :size="48">
                                <Aim />
                            </el-icon>
                        </div>
                        <p>将组件拖拽至此处开始建模</p>
                    </div>

                    <!-- AI Generation Overlay -->
                    <div v-if="isGenerating" class="ai-generating-overlay">
                        <div class="ai-content">
                            <el-icon class="magic-icon" :size="60">
                                <MagicStick />
                            </el-icon>
                            <div class="ai-text">智能解析需求中...</div>
                            <div class="ai-subtext">正在从《{{ route.query.reqName }}》中提取指标与采集要素</div>
                            <div class="ai-progress-dots">
                                <span></span><span></span><span></span>
                            </div>
                        </div>
                    </div>

                    <div v-for="(item, index) in canvasItems" :key="item.key" class="canvas-item"
                        :class="{ active: activeItemIndex === index, 'drag-over': dragOverIndex === index }"
                        :style="{ width: `calc(${item.width}% - 15px)` }" @click="selectItem(index)" draggable="true"
                        @dragstart.stop="onDragStart($event, index, 'canvas')"
                        @dragover.prevent="onDragOverItem($event, index)" @dragleave="onDragLeaveItem($event, index)"
                        @drop.stop="onDropItem($event, index)">
                        <div class="item-label">{{ item.label }}</div>
                        <div class="item-visual">
                            <el-input v-if="item.type === 'input'" disabled placeholder="请输入内容..." />
                            <el-select v-if="item.type === 'select'" disabled style="width: 100%"
                                placeholder="请选择选项..." />
                            <el-date-picker v-if="item.type === 'date'" disabled style="width: 100%" />
                            <el-rate v-if="item.type === 'rate'" disabled />
                            <el-checkbox-group v-if="item.type === 'checkbox'" disabled>
                                <el-checkbox label="选项 1" />
                                <el-checkbox label="选项 2" />
                            </el-checkbox-group>
                            <el-radio-group v-if="item.type === 'radio'" disabled>
                                <el-radio label="选项 1" />
                                <el-radio label="选项 2" />
                            </el-radio-group>
                            <el-switch v-if="item.type === 'switch'" disabled />
                            <el-upload v-if="item.type === 'upload'" disabled />
                            <el-image v-if="item.type === 'image'" disabled />
                        </div>
                        <div v-if="item.indicatorNode" class="item-indicator">
                            <el-icon :size="12">
                                <Aim />
                            </el-icon>
                            <span>{{ item.indicatorNode }}</span>
                        </div>
                        <div class="item-actions">
                            <el-button icon="Delete" circle size="small" type="danger"
                                @click.stop="removeItem(index)"></el-button>
                        </div>
                    </div>
                </div>
            </main>

            <!-- Right Panel: Tabs -->
            <aside class="side-panel right-panel">
                <el-tabs v-model="rightTab" class="designer-tabs">
                    <el-tab-pane label="表单信息" name="info">
                        <div class="info-form">
                            <el-form :model="formInfo" label-position="top" size="small">
                                <el-form-item label="采集模板标识">
                                    <el-input v-model="formInfo.templateCode" placeholder="请输入模板编号" />
                                </el-form-item>
                                <el-form-item label="采集模板名称">
                                    <el-input v-model="formInfo.templateName" placeholder="请输入模板名称" />
                                </el-form-item>
                                <el-form-item label="评估任务标识">
                                    <el-input v-model="formInfo.taskCode" placeholder="请输入任务编号" />
                                </el-form-item>
                                <el-form-item label="评估任务名称">
                                    <el-input v-model="formInfo.taskName" placeholder="请输入任务名称" />
                                </el-form-item>
                                <el-form-item label="装备代号">
                                    <el-input v-model="formInfo.equipCode" placeholder="请输入装备代号" />
                                </el-form-item>
                                <el-form-item label="装备名称">
                                    <el-input v-model="formInfo.equipName" placeholder="请输入装备名称" />
                                </el-form-item>
                                <el-form-item label="开始时间">
                                    <el-date-picker v-model="formInfo.startTime" type="datetime" placeholder="请选择"
                                        style="width: 100%" />
                                </el-form-item>
                                <el-form-item label="结束时间">
                                    <el-date-picker v-model="formInfo.endTime" type="datetime" placeholder="请选择"
                                        style="width: 100%" />
                                </el-form-item>
                                <el-form-item label="试验阶段">
                                    <el-select v-model="formInfo.testPhase" placeholder="请选择" style="width: 100%">
                                        <el-option label="方案论证阶段" value="phase1" />
                                        <el-option label="初步设计阶段" value="phase2" />
                                        <el-option label="详细设计阶段" value="phase3" />
                                        <el-option label="定型试验阶段" value="phase4" />
                                    </el-select>
                                </el-form-item>
                                <el-form-item label="试验任务">
                                    <el-input v-model="formInfo.testTask" placeholder="请输入试验任务" />
                                </el-form-item>
                                <el-form-item label="所属系统">
                                    <el-input v-model="formInfo.systemName" placeholder="请输入所属系统名称" />
                                </el-form-item>
                                <el-form-item label="上报截止时间">
                                    <el-date-picker v-model="formInfo.deadline" type="datetime" placeholder="请选择"
                                        style="width: 100%" />
                                </el-form-item>
                            </el-form>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="属性" name="props">
                        <div v-if="activeItem" class="props-form">
                            <el-form label-position="top" size="small">
                                <el-form-item label="字段标识 (Key)">
                                    <el-input v-model="activeItem.key" />
                                </el-form-item>
                                <el-form-item label="标签展示">
                                    <el-input v-model="activeItem.label" />
                                </el-form-item>
                                <el-form-item label="必填项目">
                                    <el-switch v-model="activeItem.required" />
                                </el-form-item>
                                <el-form-item label="占位提示 (Placeholder)">
                                    <el-input v-model="activeItem.placeholder" />
                                </el-form-item>
                                <el-form-item label="组件宽度 (%)">
                                    <el-slider v-model="activeItem.width" :min="20" :max="100" :step="5" />
                                </el-form-item>
                                <el-form-item label="绑定指标节点">
                                    <div class="indicator-bind">
                                        <el-input v-model="activeItem.indicatorNode" placeholder="点击右侧按钮选择" readonly />
                                        <el-button :icon="Aim" circle size="small" class="indicator-btn"
                                            @click="openIndicatorLib" />
                                    </div>
                                </el-form-item>
                            </el-form>
                        </div>
                        <div v-else class="props-empty">
                            <el-empty description="在画布选中组件以配置属性" :image-size="60" />
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </aside>
        </div>

        <!-- Indicator Node Library Dialog -->
        <el-dialog v-model="indicatorDialogVisible" title="指标节点库" width="700px" class="indicator-dialog">
            <div class="indicator-search">
                <el-input v-model="indicatorSearch" placeholder="搜索指标节点名称/编号..." prefix-icon="Search" clearable />
            </div>
            <el-table :data="filteredIndicators" highlight-current-row @current-change="onIndicatorSelect"
                class="tech-table" max-height="400">
                <el-table-column prop="code" label="节点编号" width="130" />
                <el-table-column prop="name" label="指标名称" min-width="180" show-overflow-tooltip />
                <el-table-column prop="category" label="所属类别" width="120">
                    <template #default="{ row }">
                        <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="unit" label="单位" width="80" />
                <el-table-column prop="level" label="层级" width="80" />
            </el-table>
            <template #footer>
                <el-button @click="indicatorDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmIndicator">确认绑定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import {
    Aim, Edit, Select, Calendar, Star, Tickets,
    View, Check, Close, Delete, List, Grid,
    Switch, Upload, Picture, DocumentAdd, Histogram,
    Setting, Link, Phone, Location, MagicStick
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCollectForm } from '@/api/dataCollection'

const route = useRoute()
const router = useRouter()
const isGenerating = ref(false)

const formTitle = ref('未命名数据采集模板')
const activeItemIndex = ref(-1)
const rightTab = ref('info')

const formInfo = ref({
    templateCode: '',
    templateName: '',
    taskCode: '',
    taskName: '',
    equipCode: '',
    equipName: '',
    startTime: '',
    endTime: '',
    testPhase: '',
    testTask: '',
    systemName: '',
    deadline: ''
})
const canvasItems = ref([
    { type: 'input', label: '采集人姓名', key: 'user_name', required: true, placeholder: '请输入...', width: 100 },
    { type: 'date', label: '采集时间', key: 'record_date', required: true, width: 50 },
    { type: 'rate', label: '系统性能评分', key: 'perf_score', required: false, width: 50 },
])

const loadFormData = async (id) => {
    try {
        const res = await getCollectForm(id)
        if (res.code === 200 || res.data) {
            const data = res.data
            formTitle.value = data.formName || '未命名数据采集模板'

            // Map formInfo
            formInfo.value = {
                templateCode: data.formCode || '',
                templateName: data.formName || '',
                taskCode: data.requireCode || '',
                taskName: data.requireName || '',
                equipCode: '', // Not in provided data
                equipName: data.issuingUnit || '',
                startTime: '',
                endTime: '',
                testPhase: '',
                testTask: '',
                systemName: data.issuingUnit || '',
                deadline: data.collectTime || ''
            }

            // Map canvasItems
            if (data.items && Array.isArray(data.items)) {
                canvasItems.value = data.items.map(item => ({
                    type: 'input', // Default type
                    label: item.relatedIndicator || item.dataItemName || '未标定字段',
                    key: item.fieldCode || `field_${item.id}`,
                    required: true,
                    placeholder: '请输入...',
                    width: 100,
                    indicatorNode: item.indicatorCode ? `${item.indicatorCode} - ${item.relatedIndicator || ''}` : ''
                }))
            }

            ElMessage.success('表单数据加载成功')
        }
    } catch (error) {
        console.error('Failed to load form data:', error)
        ElMessage.error('加载表单数据失败')
    }
}

onMounted(() => {
    const { source, reqName, reqId, id } = route.query
    console.log("mount ", route.query)
    if (id) {
        loadFormData(id)
    } else if (source === 'requirement' && reqName) {
        generateFromRequirement(reqName, reqId)
    }
})

const generateFromRequirement = (name, id) => {
    isGenerating.value = true
    formTitle.value = `采集表单 - ${name}`

    // Simulate AI generation process
    setTimeout(() => {
        formInfo.value.taskName = name
        formInfo.value.taskCode = id || `TASK-${Date.now().toString().slice(-4)}`
        formInfo.value.templateName = `${name}采集模板`

        // Mock generated items based on requirement name
        canvasItems.value = [
            { type: 'input', label: '测试节点编号', key: 'node_id', required: true, placeholder: '自动对齐大纲节点...', width: 50, indicatorNode: 'IND-010 - 在轨可用度' },
            { type: 'input', label: '环境噪声等级(dBm)', key: 'noise_lvl', required: true, placeholder: '需上报原始采样值', width: 50, indicatorNode: 'IND-005 - 天线增益' },
            { type: 'textarea', label: '异常现象描述', key: 'error_desc', required: false, placeholder: '如有波形异常请记录', width: 100 },
            { type: 'upload', label: '原始数据文件上传', key: 'raw_data', required: true, width: 100 },
            { type: 'rate', label: '通信质量主观评价', key: 'quality_rate', required: false, width: 100, indicatorNode: 'IND-001 - 星地链路丢包率' }
        ]

        isGenerating.value = false
        ElMessage.success({
            message: '已基于评估需求智能生成采集表单模型',
            duration: 5000,
            showClose: true
        })
    }, 2500)
}

const handleGenerate = () => {
    ElMessage.success('表单生成并发布成功')
    router.push('/process/reception-sys/forms')
}

const activeItem = computed(() => {
    if (activeItemIndex.value >= 0) {
        return canvasItems.value[activeItemIndex.value]
    }
    return null
})

const componentLibrary = [
    {
        title: '基础输入',
        items: [
            { name: '单行输入', type: 'input', icon: Edit },
            { name: '多行文本', type: 'textarea', icon: Tickets },
            { name: '数字输入', type: 'number', icon: Histogram },
        ]
    },
    {
        title: '选择类',
        items: [
            { name: '下拉选择', type: 'select', icon: Select },
            { name: '单选按鈕', type: 'radio', icon: List },
            { name: '多选复选框', type: 'checkbox', icon: Grid },
            { name: '开关按鈕', type: 'switch', icon: Switch },
        ]
    },
    {
        title: '日期时间',
        items: [
            { name: '日期选择', type: 'date', icon: Calendar },
            { name: '日期范围', type: 'daterange', icon: Calendar },
            { name: '时间选择', type: 'time', icon: Calendar },
        ]
    },
    {
        title: '高级组件',
        items: [
            { name: '评分组件', type: 'rate', icon: Star },
            { name: '文件上传', type: 'upload', icon: Upload },
            { name: '图片上传', type: 'image', icon: Picture },
        ]
    },
]

// Drag and Drop State
const draggedType = ref(null) // 'library' or 'canvas'
const draggedData = ref(null)
const dragOverIndex = ref(-1)

const onDragStart = (e, data, type) => {
    draggedType.value = type
    draggedData.value = data
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', 'drag') // Required for Firefox
}

const onDragOverCanvas = (e) => {
    e.preventDefault()
}

const onDropCanvas = (e) => {
    e.preventDefault()
    if (draggedType.value === 'library' && draggedData.value) {
        const comp = draggedData.value
        canvasItems.value.push({
            type: comp.type,
            label: comp.name,
            key: `${comp.type}_${Date.now()}`,
            required: false,
            placeholder: '请输入...',
            width: 100
        })
        activeItemIndex.value = canvasItems.value.length - 1
    }
    clearDrag()
}

const onDragOverItem = (e, index) => {
    e.preventDefault()
    dragOverIndex.value = index
}

const onDragLeaveItem = (e, index) => {
    if (dragOverIndex.value === index) {
        dragOverIndex.value = -1
    }
}

const onDropItem = (e, index) => {
    e.preventDefault()
    e.stopPropagation() // Prevent drop on canvas

    if (draggedType.value === 'library' && draggedData.value) {
        const comp = draggedData.value
        const newItem = {
            type: comp.type,
            label: comp.name,
            key: `${comp.type}_${Date.now()}`,
            required: false,
            placeholder: '请输入...',
            width: 100
        }
        canvasItems.value.splice(index, 0, newItem)
        activeItemIndex.value = index
    } else if (draggedType.value === 'canvas' && draggedData.value !== null) {
        const sourceIndex = draggedData.value
        if (sourceIndex !== index) {
            const items = [...canvasItems.value]
            const [removed] = items.splice(sourceIndex, 1)
            // If dragging down, the target index shifts after removal
            const targetPos = sourceIndex < index ? index : index
            items.splice(targetPos, 0, removed)
            canvasItems.value = items
            activeItemIndex.value = targetPos
        }
    }
    clearDrag()
}

const clearDrag = () => {
    draggedType.value = null
    draggedData.value = null
    dragOverIndex.value = -1
}

const selectItem = (index) => {
    activeItemIndex.value = index
    rightTab.value = 'props'
}

// Indicator Node Library
const indicatorDialogVisible = ref(false)
const indicatorSearch = ref('')
const selectedIndicator = ref(null)

const indicatorNodes = ref([
])

const filteredIndicators = computed(() => {
    if (!indicatorSearch.value) return indicatorNodes.value
    const q = indicatorSearch.value.toLowerCase()
    return indicatorNodes.value.filter(n => n.name.toLowerCase().includes(q) || n.code.toLowerCase().includes(q))
})

const openIndicatorLib = () => {
    indicatorSearch.value = ''
    selectedIndicator.value = null
    indicatorDialogVisible.value = true
}

const onIndicatorSelect = (row) => {
    selectedIndicator.value = row
}

const confirmIndicator = () => {
    if (selectedIndicator.value && activeItem.value) {
        activeItem.value.indicatorNode = `${selectedIndicator.value.code} - ${selectedIndicator.value.name}`
    }
    indicatorDialogVisible.value = false
}

const removeItem = (index) => {
    canvasItems.value.splice(index, 1)
    if (activeItemIndex.value === index) {
        activeItemIndex.value = -1
    }
}
</script>

<style scoped lang="scss">
.designer-container {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.designer-header {
    height: 50px;
    background: rgba(16, 32, 53, 0.9);
    border-bottom: 1px solid rgba(0, 242, 255, 0.1);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;

    .title {
        font-size: 14px;
        color: #fff;
        font-weight: bold;
        display: flex;
        align-items: center;
        gap: 10px;

        .v-tag {
            font-size: 10px;
            padding: 1px 6px;
            background: rgba(0, 242, 255, 0.2);
            color: #00f2ff;
            border-radius: 4px;
        }
    }
}

.designer-body {
    flex: 1;
    display: flex;
    overflow: hidden;
}

.side-panel {
    width: 260px;
    background: rgba(10, 21, 37, 0.8);
    border: 1px solid rgba(0, 242, 255, 0.05);
    display: flex;
    flex-direction: column;

    .panel-title {
        padding: 15px;
        font-size: 13px;
        color: #00f2ff;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        background: rgba(0, 242, 255, 0.05);
    }
}

.left-panel {
    .component-group {
        padding: 15px;

        .group-label {
            font-size: 11px;
            color: rgba(255, 255, 255, 0.4);
            margin-bottom: 15px;
            letter-spacing: 1px;
        }

        .component-items {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;

            .comp-item {
                background: rgba(255, 255, 255, 0.03);
                border: 1px solid rgba(255, 255, 255, 0.1);
                padding: 10px;
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 8px;
                font-size: 12px;
                color: #8fa3b8;
                cursor: move;
                transition: all 0.2s;

                &:hover {
                    background: rgba(0, 242, 255, 0.1);
                    border-color: #00f2ff;
                    color: #00f2ff;
                    transform: translateY(-2px);
                }

                .el-icon {
                    font-size: 18px;
                }
            }
        }
    }
}

.designer-canvas {
    flex: 1;
    background: #050a14;
    background-image:
        linear-gradient(rgba(0, 242, 255, 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0, 242, 255, 0.03) 1px, transparent 1px);
    background-size: 20px 20px;
    padding: 40px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    align-items: center;

    .canvas-header {
        width: 100%;
        max-width: 800px;
        margin-bottom: 20px;

        .title-input :deep(.el-input__wrapper) {
            background: transparent !important;
            box-shadow: none !important;
            border-bottom: 1px dashed rgba(0, 242, 255, 0.3);
            padding: 0;
            border-radius: 0;

            input {
                text-align: center;
                font-size: 20px;
                color: #fff;
                font-weight: bold;
            }
        }
    }
}

.canvas-content {
    width: 100%;
    max-width: 800px;
    min-height: 500px;
    background: rgba(16, 32, 53, 0.8);
    border: 1px solid rgba(0, 242, 255, 0.1);
    padding: 30px;
    position: relative;
    display: flex;
    flex-wrap: wrap;
    align-content: flex-start;
    gap: 15px;

    .empty-placeholder {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        text-align: center;
        color: rgba(255, 255, 255, 0.2);

        .icon-pulse {
            animation: pulse 2s infinite;
            margin-bottom: 15px;
        }
    }
}

.canvas-item {
    position: relative;
    background: rgba(255, 255, 255, 0.02);
    border: 1px dashed rgba(255, 255, 255, 0.1);
    padding: 15px;
    cursor: pointer;
    transition: all 0.2s;
    box-sizing: border-box;

    &:hover {
        border-color: rgba(0, 242, 255, 0.3);
    }

    &.active {
        border: 1px solid #00f2ff;
        background: rgba(0, 242, 255, 0.05);
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.1);

        .item-actions {
            opacity: 1;
        }
    }

    &.drag-over {
        border-left: 3px solid #67c23a;
        margin-left: 10px; // Visual gap indication
        transition: border 0.1s, margin 0.1s;
    }

    .item-label {
        font-size: 13px;
        margin-bottom: 10px;
        color: #8fa3b8;
    }

    .item-indicator {
        display: flex;
        align-items: center;
        gap: 4px;
        margin-top: 8px;
        padding: 3px 8px;
        background: rgba(0, 242, 255, 0.08);
        border: 1px solid rgba(0, 242, 255, 0.2);
        border-radius: 4px;
        font-size: 11px;
        color: #00f2ff;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }

    .item-actions {
        position: absolute;
        right: -45px;
        top: 0;
        opacity: 0;
        transition: opacity 0.2s;
    }
}

.right-panel {
    overflow-y: auto;
}

.props-form {
    padding: 20px;
}

:deep(.designer-tabs) {
    height: 100%;
    display: flex;
    flex-direction: column;

    .el-tabs__header {
        margin: 0;
        background: rgba(0, 242, 255, 0.05);
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);

        .el-tabs__nav-wrap::after {
            display: none;
        }

        .el-tabs__item {
            color: #8fa3b8;
            font-size: 13px;
            height: 42px;

            &.is-active {
                color: #00f2ff;
            }

            &:hover {
                color: #00f2ff;
            }
        }

        .el-tabs__active-bar {
            background-color: #00f2ff;
        }
    }

    .el-tabs__content {
        flex: 1;
        overflow-y: auto;
        padding: 0;
    }
}

.info-form {
    padding: 16px;
    overflow-y: auto;

    :deep(.el-form-item__label) {
        color: #8fa3b8 !important;
        font-size: 12px;
        margin-bottom: 2px;
    }

    :deep(.el-form-item) {
        margin-bottom: 12px;
    }
}

.props-empty {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
}

@keyframes pulse {
    0% {
        opacity: 0.2;
        transform: scale(1);
    }

    50% {
        opacity: 0.5;
        transform: scale(1.1);
    }

    100% {
        opacity: 0.2;
        transform: scale(1);
    }
}

.indicator-bind {
    display: flex;
    gap: 6px;
    align-items: center;

    .el-input {
        flex: 1;
    }

    .indicator-btn {
        border-color: #00f2ff !important;
        color: #00f2ff !important;
        background: rgba(0, 242, 255, 0.1) !important;
        flex-shrink: 0;

        &:hover {
            background: rgba(0, 242, 255, 0.25) !important;
            box-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
        }
    }
}

:deep(.indicator-dialog) {
    background: #0d1a2b !important;
    border: 1px solid rgba(0, 242, 255, 0.2) !important;

    .el-dialog__title {
        color: #00f2ff !important;
    }

    .el-dialog__header {
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
    }

    .indicator-search {
        margin-bottom: 16px;
    }
}

.ai-generating-overlay {
    position: absolute;
    inset: 0;
    background: rgba(10, 21, 37, 0.95);
    z-index: 100;
    display: flex;
    justify-content: center;
    align-items: center;
    backdrop-filter: blur(8px);

    .ai-content {
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;
    }

    .magic-icon {
        color: #00f2ff;
        animation: spin-pulse 2s infinite ease-in-out;
        filter: drop-shadow(0 0 15px rgba(0, 242, 255, 0.6));
    }

    .ai-text {
        font-size: 20px;
        font-weight: bold;
        color: #fff;
        letter-spacing: 2px;
    }

    .ai-subtext {
        font-size: 14px;
        color: #8fa3b8;
    }

    .ai-progress-dots {
        display: flex;
        gap: 8px;

        span {
            width: 8px;
            height: 8px;
            background: #00f2ff;
            border-radius: 50%;
            animation: dot-pulse 1.5s infinite ease-in-out;

            &:nth-child(2) {
                animation-delay: 0.2s;
            }

            &:nth-child(3) {
                animation-delay: 0.4s;
            }
        }
    }
}

@keyframes spin-pulse {
    0% {
        transform: rotate(0deg) scale(1);
    }

    50% {
        transform: rotate(180deg) scale(1.2);
    }

    100% {
        transform: rotate(360deg) scale(1);
    }
}

@keyframes dot-pulse {

    0%,
    100% {
        opacity: 0.3;
        transform: scale(0.8);
    }

    50% {
        opacity: 1;
        transform: scale(1.2);
    }
}
</style>
