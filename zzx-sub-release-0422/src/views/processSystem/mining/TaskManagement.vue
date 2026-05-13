<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">数据分析挖掘任务管理</h3>
            </div>
            <div class="right">
                <el-button type="primary" icon="Plus" @click="openConstruction">构建新挖掘任务</el-button>
            </div>
        </div>

        <!-- Task List -->
        <div class="task-list mt-4">
            <el-table :data="tasks" v-loading="loading" class="tech-table">
                <el-table-column prop="id" label="任务ID" width="100" />
                <el-table-column prop="name" label="任务名称" show-overflow-tooltip />
                <el-table-column prop="type" label="挖掘类型">
                    <template #default="{ row }">
                        <el-tag effect="plain" size="small">{{ row.type }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="当前阶段">
                    <template #default="{ row }">
                        <el-steps :active="row.step" simple style="background: transparent; padding: 0;">
                            <el-step title="准备" />
                            <el-step title="训练" />
                            <el-step title="预测" />
                        </el-steps>
                    </template>
                </el-table-column>
                <el-table-column prop="progress" label="总进度" width="180">
                    <template #default="{ row }">
                        <el-progress :percentage="row.progress" :color="progressColors" />
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" icon="View" @click="openResults(row)">查看结果</el-button>
                        <el-button link type="primary" icon="VideoPlay"
                            :disabled="row.progress === 100">继续执行</el-button>
                        <el-button link type="danger" icon="Delete">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- Guided Construction Wizard Dialog -->
        <el-dialog v-model="wizardVisible" title="数据分析挖掘任务构建向导" width="1000px" custom-class="mining-wizard-dialog"
            :close-on-click-modal="false" destroy-on-close>
            <div class="wizard-steps-container">
                <el-steps :active="activeStep" finish-status="success" align-center>
                    <el-step title="数据准备" />
                    <el-step title="数据预处理" />
                    <el-step title="特征提取" />
                    <el-step title="模型训练" />
                    <el-step title="模型预测" />
                    <el-step title="结果输出" />
                </el-steps>

                <div class="step-content mt-6">
                    <!-- Step 0: Data Prep -->
                    <div v-if="activeStep === 0" class="step-pane animate-fade-in">
                        <div class="pane-header">阶段 1: 数据准备</div>
                        <el-form :model="taskForm" label-position="top">
                            <el-form-item label="任务名称">
                                <el-input v-model="taskForm.name" placeholder="输入任务名称..." />
                            </el-form-item>
                            <el-form-item label="任务类型">
                                <el-select v-model="taskForm.type" placeholder="选择挖掘目标" style="width: 100%">
                                    <el-option-group label="基础挖掘与寻优">
                                        <el-option label="装备能力短板分析" value="gap" />
                                        <el-option label="评估方法优化" value="method" />
                                        <el-option label="作战运用模式择优" value="mode" />
                                    </el-option-group>
                                    <el-option-group label="影响因素分析 (基于历史与单次评估)">
                                        <el-option label="装备试验影响因素分析" value="factors_equip" />
                                        <el-option label="体系试验影响因素分析" value="factors_sys" />
                                        <el-option label="演习演训影响因素分析" value="factors_drill" />
                                    </el-option-group>
                                    <el-option-group label="数据与模型优化">
                                        <el-option label="数据源可信性评估" value="credibility" />
                                        <el-option label="评估算法模型优化改进" value="model_opt" />
                                    </el-option-group>
                                </el-select>
                            </el-form-item>
                            <el-row :gutter="20">
                                <el-col :span="12">
                                    <el-form-item label="输入历史数据">
                                        <el-select v-model="taskForm.historyData" multiple placeholder="选择历史评估或试验数据"
                                            style="width: 100%">
                                            <el-option label="2025年度演习演训归档库" value="hist1" />
                                            <el-option label="历年装备试验参数集" value="hist2" />
                                            <el-option label="体系对抗仿真历史库" value="hist3" />
                                        </el-select>
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="输入单次评估结果">
                                        <el-select v-model="taskForm.singleEvalData" multiple placeholder="选择单次评估产出"
                                            style="width: 100%">
                                            <el-option label="本次红蓝对抗火力打击评估报告" value="single1" />
                                            <el-option label="A型雷达单次抗干扰评估输出" value="single2" />
                                        </el-select>
                                    </el-form-item>
                                </el-col>
                            </el-row>
                        </el-form>
                    </div>

                    <!-- Step 1: Preprocessing -->
                    <div v-if="activeStep === 1" class="step-pane animate-fade-in">
                        <div class="pane-header">阶段 2: 数据分析挖掘预处理</div>
                        <div class="helper-text">为任务类型 [{{ taskForm.type }}] 自动匹配的预处理组件：</div>
                        <el-checkbox-group v-model="taskForm.preprocess">
                            <el-card shadow="never" class="op-card mb-2">
                                <el-checkbox label="missing_fill">缺失值插值补偿 (针对时序空缺)</el-checkbox>
                            </el-card>
                            <el-card shadow="never" class="op-card mb-2">
                                <el-checkbox label="denoise">高斯平滑降噪 (消除噪声干扰)</el-checkbox>
                            </el-card>
                            <el-card shadow="never" class="op-card mb-2">
                                <el-checkbox label="normalization">Min-Max 标准化 (映射至 [0, 1])</el-checkbox>
                            </el-card>
                        </el-checkbox-group>
                    </div>

                    <!-- Step 2: Feature Extraction -->
                    <div v-if="activeStep === 2" class="step-pane animate-fade-in">
                        <div class="pane-header">阶段 3: 特征提取</div>
                        <el-transfer v-model="taskForm.features" :data="availableFeatures" :titles="['原始字段', '选定特征']"
                            filterable />
                    </div>

                    <!-- Step 3: Training -->
                    <div v-if="activeStep === 3" class="step-pane animate-fade-in">
                        <div class="pane-header">阶段 4: 模型训练</div>
                        <el-alert title="提示: 为该任务类型推荐使用 XGBoost 或卷积神经网络 (CNN)" type="info" show-icon
                            :closable="false" />
                        <el-form :model="taskForm" label-position="top" class="mt-4">
                            <el-form-item label="基础模型选择">
                                <el-radio-group v-model="taskForm.model">
                                    <el-radio-button label="xgboost">XGBoost V2.0</el-radio-button>
                                    <el-radio-button label="random_forest">随机森林</el-radio-button>
                                    <el-radio-button label="cnn">CNN (深度学习)</el-radio-button>
                                </el-radio-group>
                            </el-form-item>
                            <el-row :gutter="20">
                                <el-col :span="12">
                                    <el-form-item label="训练集比例 (%)">
                                        <el-slider v-model="taskForm.split" :min="10" :max="90" />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="最大迭代次数 (Epochs)">
                                        <el-input-number v-model="taskForm.epochs" :min="1" :max="1000" />
                                    </el-form-item>
                                </el-col>
                            </el-row>
                        </el-form>
                    </div>

                    <!-- Higher steps would follow similar logic... -->
                    <div v-if="activeStep >= 4" class="step-pane animate-fade-in">
                        <div class="pane-header">阶段 {{ activeStep + 1 }}: 配置总结</div>
                        <el-result icon="success" title="向导配置已完成" sub-title="模型预测与结果汇总阶段将根据以上配置自动并行执行">
                            <template #extra>
                                <div class="config-summary">
                                    <div>任务指向: {{ taskForm.type }}</div>
                                    <div>输入历史数据: {{ taskForm.historyData.length }} 项</div>
                                    <div>输入单次结果: {{ taskForm.singleEvalData.length }} 项</div>
                                    <div>选中预处理算子: {{ taskForm.preprocess.length }} 个</div>
                                    <div>选中特征维度: {{ taskForm.features.length }} 项</div>
                                    <div>核心挖掘模型: {{ taskForm.model }}</div>
                                </div>
                            </template>
                        </el-result>
                    </div>
                </div>
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="wizardVisible = false">取消</el-button>
                    <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
                    <el-button type="primary" v-if="activeStep < 5" @click="activeStep++">下一步</el-button>
                    <el-button type="success" v-else @click="handleSubmit">启动挖掘任务</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- Analysis Results Drawer -->
        <el-drawer v-model="resultsVisible" :title="`分析挖掘报告 - ${activeTaskTitle}`" size="65%"
            custom-class="mining-result-drawer" destroy-on-close>
            <div class="result-container" v-if="activeTaskResult">
                <el-alert title="分析任务已成功完成" type="success" show-icon :closable="false" class="mb-4" />

                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-card class="result-card h-100">
                            <template #header>
                                <div class="card-title">特征重要性 (Feature Importance)</div>
                            </template>
                            <div class="chart-mock">
                                <div class="bar-chart">
                                    <div class="bar-row" v-for="feat in activeTaskResult.features" :key="feat.name">
                                        <div class="bar-label">{{ feat.name }}</div>
                                        <div class="bar-track">
                                            <div class="bar-fill"
                                                :style="{ width: feat.score + '%', background: feat.color }">
                                            </div>
                                        </div>
                                        <div class="bar-value">{{ feat.score.toFixed(2) }}</div>
                                    </div>
                                </div>
                            </div>
                        </el-card>
                    </el-col>
                    <el-col :span="12">
                        <el-card class="result-card h-100">
                            <template #header>
                                <div class="card-title">模型性能指标</div>
                            </template>
                            <el-descriptions :column="1" border class="tech-desc">
                                <el-descriptions-item label="核心算法">XGBoost V2.0</el-descriptions-item>
                                <el-descriptions-item label="训练集准确率">98.5%</el-descriptions-item>
                                <el-descriptions-item label="验证集准确率"><span
                                        style="color: #67c23a; font-weight: bold;">95.2%</span></el-descriptions-item>
                                <el-descriptions-item label="F1-Score">0.94</el-descriptions-item>
                                <el-descriptions-item label="推理时延">12ms / inference</el-descriptions-item>
                            </el-descriptions>
                        </el-card>
                    </el-col>
                </el-row>

                <el-card class="result-card mt-4">
                    <template #header>
                        <div class="card-title">核心规律/短板分析结论提取</div>
                    </template>
                    <div class="conclusion-box">
                        <p v-for="(text, index) in activeTaskResult.conclusions" :key="index">
                            <el-icon color="#e6a23c" class="mr-2">
                                <WarningFilled />
                            </el-icon>
                            {{ text }}
                        </p>
                    </div>
                </el-card>

                <div class="action-bar mt-6" style="text-align: right;">
                    <el-button type="primary" icon="Download">下载完整分析报告 (PDF)</el-button>
                    <el-button icon="Cpu">一键发布模型至库</el-button>
                </div>
            </div>
            <div v-else class="empty-result">
                <el-empty description="暂无分析结果或任务未执行完毕" />
            </div>
        </el-drawer>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Plus, Search, View, VideoPlay, Delete, WarningFilled, Download, Cpu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const wizardVisible = ref(false)
const activeStep = ref(0)

const progressColors = [
    { color: '#f56c6c', percentage: 20 },
    { color: '#e6a23c', percentage: 40 },
    { color: '#5cb87a', percentage: 60 },
    { color: '#1989fa', percentage: 80 },
    { color: '#6f7ad3', percentage: 100 },
]

const resultsVisible = ref(false)
const activeTaskTitle = ref('')
const activeTaskResult = ref(null)

const tasks = ref([
    { id: 'MT-001', name: '低轨互联网星座星间链路抗截获敏感度分析', type: '装备能力短板分析', step: 2, progress: 65 },
    { id: 'MT-002', name: '基于发射场历史数据的靶场评估偏差修正模型', type: '评估方法优化', step: 3, progress: 100 },
    { id: 'MT-003', name: '红蓝双方天基体系测控资源分配效能挖掘', type: '作战运用模式择优', step: 1, progress: 30 },
])

const openResults = (row) => {
    activeTaskTitle.value = row.name
    resultsVisible.value = true

    // Mock result generation based on task progress
    if (row.progress === 100) {
        activeTaskResult.value = {
            features: [
                { name: '电离层总电子含量(TEC)', score: 85, color: '#f56c6c' },
                { name: '发射场风切变指数', score: 62, color: '#e6a23c' },
                { name: '入轨段轨道高度面', score: 45, color: '#409eff' },
                { name: '姿轨控三轴姿态角速度', score: 30, color: '#67c23a' }
            ],
            conclusions: [
                '在太阳耀斑爆发等空间天气下，TEC特征权重异常升高，高度指示星地测控链路的穿透损耗呈非线性剧变。',
                '发射场低空风切变对箭体姿态控制的绝对干预阈值较小，当前伺服机构响应速率冗余度不足。',
                '星历跟踪数据分布存在长尾效应，建议在下一阶段采集更多"近地轨道大气拖曳"导致的微扰相关数据。'
            ]
        }
    } else {
        activeTaskResult.value = null
    }
}

const taskForm = reactive({
    name: '',
    type: '',
    historyData: [],
    singleEvalData: [],
    preprocess: [],
    features: [],
    model: 'xgboost',
    split: 80,
    epochs: 100
})

const availableFeatures = Array.from({ length: 15 }).map((_, i) => ({
    key: i,
    label: `特征_${i + 1}`,
    disabled: i % 4 === 0
}))

const openConstruction = () => {
    activeStep.value = 0
    Object.assign(taskForm, {
        name: '', type: '', historyData: [], singleEvalData: [], preprocess: [], features: [], model: 'xgboost', split: 80, epochs: 100
    })
    wizardVisible.value = true
}

const handleSubmit = () => {
    ElMessage.success('数据挖掘任务已提交后端集群执行')
    wizardVisible.value = false
    tasks.value.unshift({
        id: `MT-00${tasks.value.length + 1}`,
        name: taskForm.name || '新建数据挖掘任务',
        type: taskForm.type,
        step: 0,
        progress: 0
    })
}
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .page-title {
        margin: 0;
        font-size: 24px;
        font-weight: bold;
        color: #fff;
        border-left: 4px solid #00f2ff;
        padding-left: 12px;
    }
}

.tech-table {
    background: rgba(16, 32, 53, 0.4) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
    border-radius: 8px;

    :deep(tr) {
        background: transparent !important;
        color: #fff;
    }

    :deep(th) {
        background: rgba(0, 242, 255, 0.05) !important;
        color: #00f2ff !important;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1) !important;
    }

    :deep(td) {
        border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important;
    }
}

.wizard-steps-container {
    padding: 20px 0;
}

.step-pane {
    min-height: 400px;
    padding: 20px;
    background: rgba(0, 242, 255, 0.02);
    border: 1px solid rgba(0, 242, 255, 0.1);
    border-radius: 8px;

    .pane-header {
        font-size: 18px;
        color: #00f2ff;
        margin-bottom: 20px;
        font-weight: bold;
        border-bottom: 1px solid rgba(0, 242, 255, 0.2);
        padding-bottom: 10px;
    }

    .helper-text {
        margin-bottom: 15px;
        color: rgba(255, 255, 255, 0.6);
        font-size: 14px;
    }
}

.op-card {
    background: rgba(255, 255, 255, 0.03) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;

    :deep(.el-checkbox__label) {
        color: #fff;
    }
}

.config-summary {
    text-align: left;
    margin-top: 20px;
    font-size: 14px;
    color: #8fa3b8;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.animate-fade-in {
    animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

:deep(.mining-wizard-dialog) {
    background: #0d1a2b !important;
    border: 1px solid #00f2ff !important;

    .el-dialog__title {
        color: #00f2ff !important;
    }

    .el-form-item__label {
        color: #fff !important;
    }

    .el-input__inner,
    .el-textarea__inner {
        background: rgba(255, 255, 255, 0.05) !important;
        color: #fff !important;
    }

    .el-transfer-panel {
        background: rgba(255, 255, 255, 0.05) !important;
        color: #fff !important;
        border-color: rgba(0, 242, 255, 0.2) !important;
    }

    .el-transfer-panel__header {
        background: rgba(0, 242, 255, 0.1) !important;

        .el-checkbox__label {
            color: #00f2ff !important;
        }
    }
}

:deep(.mining-result-drawer) {
    background: #061121 !important;
    border-left: 1px solid #00f2ff;

    .el-drawer__header {
        color: #00f2ff;
        font-weight: bold;
        margin-bottom: 0;
        border-bottom: 1px solid rgba(0, 242, 255, 0.2);
        padding-bottom: 16px;
    }

    .result-card {
        background: rgba(16, 32, 53, 0.4);
        border: 1px solid rgba(0, 242, 255, 0.1);
        color: #fff;

        .el-card__header {
            border-bottom: 1px solid rgba(255, 255, 255, 0.05);
            color: #00f2ff;
            font-weight: bold;
        }
    }

    .chart-mock {
        padding: 10px 0;

        .bar-chart {
            display: flex;
            flex-direction: column;
            gap: 12px;

            .bar-row {
                display: flex;
                align-items: center;
                gap: 10px;

                .bar-label {
                    width: 120px;
                    font-size: 13px;
                    text-align: right;
                    color: #8fa3b8;
                }

                .bar-track {
                    flex: 1;
                    height: 12px;
                    background: rgba(255, 255, 255, 0.05);
                    border-radius: 6px;
                    overflow: hidden;

                    .bar-fill {
                        height: 100%;
                        border-radius: 6px;
                    }
                }

                .bar-value {
                    width: 40px;
                    font-size: 13px;
                    color: #fff;
                    font-family: 'Roboto Mono', monospace;
                }
            }
        }
    }

    .tech-desc {
        :deep(.el-descriptions__label) {
            background: rgba(0, 242, 255, 0.05) !important;
            color: #8fa3b8;
            border-color: rgba(255, 255, 255, 0.05);
        }

        :deep(.el-descriptions__content) {
            background: transparent !important;
            color: #fff;
            border-color: rgba(255, 255, 255, 0.05);
        }
    }

    .conclusion-box {
        p {
            margin-top: 0;
            margin-bottom: 12px;
            line-height: 1.6;
            color: #e4e7ed;
            background: rgba(255, 255, 255, 0.02);
            padding: 12px;
            border-radius: 4px;
            border-left: 3px solid #e6a23c;
            display: flex;
            align-items: flex-start;
        }
    }

    .empty-result {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
    }
}
</style>
