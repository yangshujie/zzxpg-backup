<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">采集表单模板库</h3>
            </div>
            <div class="right">
                <el-input v-model="searchQuery" placeholder="搜索模板名称" clearable style="width: 240px; margin-right: 12px;"
                    prefix-icon="Search" />
                <el-button type="primary" icon="Plus" @click="handleCreate">新建模板</el-button>
            </div>
        </div>

        <div class="template-grid">
            <el-row :gutter="20">
                <el-col :span="6" v-for="item in templates" :key="item.id">
                    <el-card class="template-card" :body-style="{ padding: '0px' }">
                        <div class="card-preview">
                            <el-icon :size="48">
                                <Document />
                            </el-icon>
                            <div class="overlay">
                                <el-button circle icon="View" @click="handlePreview(item)"></el-button>
                                <el-button circle icon="Edit" @click="handleEdit(item)"></el-button>
                            </div>
                        </div>
                        <div class="card-footer">
                            <div class="title">{{ item.name }}</div>
                            <div class="info">
                                <span>字段: {{ item.fieldCount }}</span>
                                <span>使用: {{ item.useCount }}</span>
                            </div>
                            <div class="footer-meta mt-2">
                                <el-tag size="small" :type="item.status === '已发布' ? 'success' : 'info'" effect="plain">
                                    {{ item.status }}
                                </el-tag>
                                <span class="update-time">{{ item.updateTime }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- Template Preview Dialog -->
        <el-dialog v-model="previewVisible" :title="`表单预览 - ${previewTitle}`" width="800px"
            custom-class="preview-dialog">
            <div class="preview-form-container">
                <el-form label-position="top">
                    <div class="form-grid">
                        <div v-for="(field, index) in previewItems" :key="index"
                            :style="{ width: `calc(${field.width}% - 15px)` }" class="grid-item">
                            <el-form-item :label="field.label" :required="field.required" style="margin-bottom: 0;">
                                <el-input v-if="field.type === 'input'" placeholder="请输入..." disabled />
                                <el-select v-else-if="field.type === 'select'" style="width: 100%" placeholder="请选择..."
                                    disabled />
                                <el-date-picker v-else-if="field.type === 'date'" style="width: 100%" disabled />
                                <el-rate v-else-if="field.type === 'rate'" disabled />
                            </el-form-item>
                        </div>
                    </div>
                </el-form>
            </div>
            <template #footer>
                <div class="dialog-action">
                    <el-button @click="previewVisible = false">关闭</el-button>
                    <el-button type="primary" icon="Edit" @click="handleEditFromPreview">基于此模板修改</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, Document, View, Edit } from '@element-plus/icons-vue'

const router = useRouter()
const searchQuery = ref('')

const previewVisible = ref(false)
const previewTitle = ref('')
const previewItems = ref([])
const activePreviewItem = ref(null)

const templates = ref([
    { id: 1, name: '航天器平台健康状态采集表', fieldCount: 24, useCount: 156, status: '已发布', updateTime: '2024-03-12 10:30' },
    { id: 2, name: '星载探测载荷参数监控模板', fieldCount: 18, useCount: 89, status: '已发布', updateTime: '2024-03-11 15:45' },
    { id: 3, name: '星地/星间通信链路抖动测试表', fieldCount: 12, useCount: 342, status: '草稿', updateTime: '2024-03-13 09:20' },
    { id: 4, name: '武器装备体系作战效能评估问卷', fieldCount: 30, useCount: 45, status: '已发布', updateTime: '2024-03-10 11:12' },
    { id: 5, name: '星载计算机漏洞扫描结果导入模板', fieldCount: 15, useCount: 231, status: '已发布', updateTime: '2024-03-08 16:30' },
])

const handleCreate = () => {
    router.push('/process/reception-sys/designer')
}

const handlePreview = (item) => {
    activePreviewItem.value = item
    previewTitle.value = item.name
    // Mock template schema mapped to flex components
    previewItems.value = [
        { type: 'input', label: '填写人姓名', required: true, width: 50 },
        { type: 'date', label: '采集时间', required: true, width: 50 },
        { type: 'select', label: '测试节点等级', required: true, width: 100 },
        { type: 'rate', label: '系统综合评分', required: false, width: 100 },
        { type: 'input', label: '备注说明', required: false, width: 100 }
    ]
    previewVisible.value = true
}

const handleEdit = (item) => {
    router.push({ path: '/process/reception-sys/designer', query: { id: item.id } })
}

const handleEditFromPreview = () => {
    if (activePreviewItem.value) {
        handleEdit(activePreviewItem.value)
        previewVisible.value = false
    }
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

.template-card {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
    margin-bottom: 20px;
    transition: all 0.3s;
    overflow: hidden;

    &:hover {
        border-color: #00f2ff;
        transform: translateY(-5px);

        .card-preview .overlay {
            opacity: 1;
            pointer-events: auto;
        }
    }

    .card-preview {
        height: 140px;
        background: rgba(0, 242, 255, 0.05);
        display: flex;
        justify-content: center;
        align-items: center;
        position: relative;
        color: rgba(0, 242, 255, 0.3);

        .overlay {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.6);
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 15px;
            opacity: 0;
            transition: opacity 0.3s;
        }
    }

    .card-footer {
        padding: 15px;

        .title {
            color: #fff;
            font-size: 15px;
            font-weight: bold;
            margin-bottom: 8px;
        }

        .info {
            display: flex;
            justify-content: space-between;
            color: #8fa3b8;
            font-size: 12px;
        }

        .footer-meta {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-top: 1px solid rgba(255, 255, 255, 0.05);
            padding-top: 8px;

            .update-time {
                font-size: 11px;
                color: #5d6d7e;
            }
        }
    }
}

:deep(.preview-dialog) {
    background: #0d1a2b !important;
    border: 1px solid #00f2ff !important;

    .el-dialog__title {
        color: #00f2ff !important;
    }

    .preview-form-container {
        padding: 20px;
        background: rgba(16, 32, 53, 0.4);
        border: 1px solid rgba(0, 242, 255, 0.1);
        border-radius: 4px;

        .form-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 15px;

            .grid-item {
                box-sizing: border-box;
                background: rgba(255, 255, 255, 0.02);
                border: 1px dashed rgba(255, 255, 255, 0.1);
                padding: 10px 15px;
                border-radius: 4px;
            }
        }
    }

    .el-form-item__label {
        color: #fff !important;
    }
}
</style>
