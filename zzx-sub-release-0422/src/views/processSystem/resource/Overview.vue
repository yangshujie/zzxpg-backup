<template>
    <div class="resource-container">
        <!-- Main Tabs -->
        <el-tabs v-model="activeTab" class="main-tabs">
            <!-- Tab 1: Data Catalog Management -->
            <el-tab-pane label="数据目录管理" name="catalog">
                <el-container class="catalog-container">
                    <!-- Left: Database & Table Tree -->
                    <el-aside width="280px" class="catalog-aside">
                        <div class="aside-header">
                            <span class="title">资源树</span>
                            <el-button type="primary" link icon="Plus" @click="openSourceConfig">配置数据源</el-button>
                        </div>
                        <div class="aside-search">
                            <el-input v-model="treeSearch" placeholder="搜索库/表..." prefix-icon="Search" clearable size="small" />
                        </div>
                        <div class="tree-wrapper">
                            <el-tree 
                                :data="dbTreeData" 
                                :props="treeProps" 
                                :filter-node-method="filterNode"
                                icon-class="FolderOpened"
                                highlight-current
                                @node-click="handleNodeClick"
                                ref="treeRef"
                                class="cyber-tree"
                            >
                                <template #default="{ node, data }">
                                    <span class="custom-tree-node">
                                        <el-icon v-if="data.type === 'db'"><Coin /></el-icon>
                                        <el-icon v-else><Memo /></el-icon>
                                        <span class="label">{{ node.label }}</span>
                                        <span class="actions" v-if="data.type === 'table'">
                                            <el-icon @click.stop="handleTableEdit(data)"><Edit /></el-icon>
                                            <el-icon @click.stop="handleTableDelete(data)"><Delete /></el-icon>
                                        </span>
                                    </span>
                                </template>
                            </el-tree>
                        </div>
                    </el-aside>

                    <!-- Right: Details (Fields & Data) -->
                    <el-main class="catalog-main">
                        <div v-if="selectedTable" class="table-details">
                            <div class="detail-header">
                                <div class="table-info">
                                    <span class="tag">TABLE</span>
                                    <h3 class="table-name">{{ selectedTable.label }}</h3>
                                    <span class="db-path">{{ selectedTable.dbName }}</span>
                                </div>
                                <div class="header-actions">
                                    <el-button type="primary" plain icon="Plus" @click="addColumn">新增字段</el-button>
                                </div>
                            </div>

                            <el-tabs v-model="detailTab" class="detail-tabs">
                                <el-tab-pane label="字段管理" name="fields">
                                    <el-table :data="tableColumns" border stripe class="cyber-table small-table">
                                        <el-table-column label="字段名" prop="name">
                                            <template #default="{ row }">
                                                <el-input v-model="row.name" size="small" />
                                            </template>
                                        </el-table-column>
                                        <el-table-column label="数据类型" prop="type" width="150">
                                            <template #default="{ row }">
                                                <el-select v-model="row.type" size="small">
                                                    <el-option label="VARCHAR" value="varchar" />
                                                    <el-option label="INT" value="int" />
                                                    <el-option label="DATETIME" value="datetime" />
                                                    <el-option label="FLOAT" value="float" />
                                                </el-select>
                                            </template>
                                        </el-table-column>
                                        <el-table-column label="描述" prop="comment">
                                            <template #default="{ row }">
                                                <el-input v-model="row.comment" size="small" />
                                            </template>
                                        </el-table-column>
                                        <el-table-column label="操作" width="80" align="center">
                                            <template #default="{ $index }">
                                                <el-button icon="Delete" link type="danger" @click="deleteColumn($index)" />
                                            </template>
                                        </el-table-column>
                                    </el-table>
                                </el-tab-pane>
                                <el-tab-pane label="数据预览" name="data">
                                    <el-table :data="tablePreviewData" border stripe class="cyber-table small-table">
                                        <el-table-column v-for="col in tableColumns" :key="col.id" :label="col.name" :prop="col.name" show-overflow-tooltip />
                                    </el-table>
                                </el-tab-pane>
                            </el-tabs>
                        </div>
                        <div v-else class="empty-state">
                            <el-empty description="请从左侧选择一个数据表查看详情" />
                        </div>
                    </el-main>
                </el-container>
            </el-tab-pane>

            <!-- Tab 2: Data Resource Overview (ECharts Graph) -->
            <el-tab-pane label="数据资源概览" name="summary">
                <div class="summary-container">
                    <div class="summary-toolbar">
                        <div class="filter-group">
                            <el-select v-model="filterType" placeholder="数据类型" clearable style="width: 150px">
                                <el-option label="基础信息" value="base" />
                                <el-option label="评估结果" value="result" />
                                <el-option label="过程参数" value="param" />
                            </el-select>
                            <el-input v-model="summarySearch" placeholder="搜索资源名称..." prefix-icon="Search" clearable style="width: 250px; margin-left: 12px;" />
                        </div>
                        <div class="legend">
                            <span class="dot db"></span> 数据库
                            <span class="dot table"></span> 数据表
                        </div>
                    </div>
                    
                    <div class="graph-viewport">
                        <div ref="chartRef" class="echarts-container"></div>
                        <!-- Empty overlay if no data or searching and no results -->
                        <div v-if="dbTreeData.length === 0" class="graph-empty">
                            <el-empty description="暂无图谱数据" />
                        </div>
                    </div>
                </div>
            </el-tab-pane>
        </el-tabs>

        <!-- Source Config Dialog -->
        <el-dialog v-model="sourceVisible" title="配置数据源" width="500px" class="cyber-dialog">
            <el-form :model="sourceForm" label-position="top">
                <el-form-item label="数据源名称">
                    <el-input v-model="sourceForm.name" placeholder="如：评估任务主库" />
                </el-form-item>
                <el-form-item label="连接地址">
                    <el-input v-model="sourceForm.url" placeholder="mysql://192.168.1.100:3306" />
                </el-form-item>
                <el-form-item label="类型">
                    <el-select v-model="sourceForm.type" style="width: 100%">
                        <el-option label="MySQL" value="mysql" />
                        <el-option label="PostgreSQL" value="pgsql" />
                        <el-option label="MongoDB" value="mongo" />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="sourceVisible = false">取消</el-button>
                <el-button type="primary" @click="saveSource">确认添加</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { 
    Plus, Search, Coin, Memo, Edit, Delete, 
    FolderOpened, Connection
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const activeTab = ref('catalog')
const detailTab = ref('fields')
const treeSearch = ref('')
const treeRef = ref(null)
const chartRef = ref(null)
let myChart = null

// Mock Tree Data
const dbTreeData = ref([
    {
        id: 'db1',
        label: 'EMC_CORE_DB (电磁核心库)',
        type: 'db',
        category: 'base',
        children: [
            { id: 't1', label: 'TBL_REC_NOISE', type: 'table', dbName: 'EMC_CORE_DB', category: 'param' },
            { id: 't2', label: 'TBL_BER_STATS', type: 'table', dbName: 'EMC_CORE_DB', category: 'result' },
            { id: 't3', label: 'TBL_PWR_STABILITY', type: 'table', dbName: 'EMC_CORE_DB', category: 'param' }
        ]
    },
    {
        id: 'db2',
        label: 'PLATFORM_SYS (平台指标库)',
        type: 'db',
        category: 'base',
        children: [
            { id: 't4', label: 'TBL_ATT_PRECISION', type: 'table', dbName: 'PLATFORM_SYS', category: 'param' },
            { id: 't5', label: 'TBL_SOLAR_PANEL', type: 'table', dbName: 'PLATFORM_SYS', category: 'result' },
            { id: 't6', label: 'TBL_ORBIT_DATA', type: 'table', dbName: 'PLATFORM_SYS', category: 'param' }
        ]
    },
    {
        id: 'db3',
        label: 'MISSION_ANALYSIS (任务分析库)',
        type: 'db',
        category: 'result',
        children: [
            { id: 't7', label: 'MISSION_REPORT_SUMMARY', type: 'table', dbName: 'MISSION_ANALYSIS', category: 'result' },
            { id: 't8', label: 'EVAL_METRICS_LOG', type: 'table', dbName: 'MISSION_ANALYSIS', category: 'result' }
        ]
    },
    {
        id: 'db4',
        label: 'TELEMETRY_DATALAKE (测控遥测湖)',
        type: 'db',
        category: 'param',
        children: [
            { id: 't9', label: 'RAW_TM_PACKETS', type: 'table', dbName: 'TELEMETRY_DATALAKE', category: 'param' },
            { id: 't10', label: 'PARSED_TM_CHANNELS', type: 'table', dbName: 'TELEMETRY_DATALAKE', category: 'param' }
        ]
    }
])

// Table-to-Table Relationships
const tableToTableLinks = [
    { source: 't1', target: 't2', label: '计算输入' },
    { source: 't9', target: 't10', label: '解析流' },
    { source: 't10', target: 't2', label: '关联分析' },
    { source: 't6', target: 't4', label: '动力学计算' },
    { source: 't5', target: 't8', label: '效能评估' },
    { source: 't7', target: 't8', label: '指标关联' }
]

const treeProps = {
    children: 'children',
    label: 'label'
}

const selectedTable = ref(null)
const tableColumns = ref([])
const tablePreviewData = ref([])

const handleNodeClick = (data) => {
    if (data.type === 'table') {
        selectedTable.value = data
        // Mock columns and data
        tableColumns.value = [
            { id: 1, name: 'ID', type: 'int', comment: '主键' },
            { id: 2, name: 'timestamp', type: 'datetime', comment: '采集时间' },
            { id: 3, name: 'value', type: 'float', comment: '实测数值' },
            { id: 4, name: 'unit', type: 'varchar', comment: '量纲' },
            { id: 5, name: 'sensor_id', type: 'varchar', comment: '传感器编号' }
        ]
        tablePreviewData.value = [
            { ID: 1, timestamp: '2026-03-16 10:00:01', value: 12.5, unit: 'dBm', sensor_id: 'S-77' },
            { ID: 2, timestamp: '2026-03-16 10:00:05', value: 13.1, unit: 'dBm', sensor_id: 'S-77' },
            { ID: 3, timestamp: '2026-03-16 10:00:10', value: 12.8, unit: 'dBm', sensor_id: 'S-77' },
            { ID: 4, timestamp: '2026-03-16 10:00:15', value: 12.9, unit: 'dBm', sensor_id: 'S-77' },
            { ID: 5, timestamp: '2026-03-16 10:00:20', value: 13.0, unit: 'dBm', sensor_id: 'S-77' }
        ]
    }
}

const addColumn = () => {
    tableColumns.value.push({ id: Date.now(), name: 'new_col', type: 'varchar', comment: '' })
}

const deleteColumn = (index) => {
    tableColumns.value.splice(index, 1)
}

const handleTableEdit = (data) => {
    ElMessage.info(`启动表 [${data.label}] 的元数据编辑`)
}

const handleTableDelete = (data) => {
    ElMessageBox.confirm(`确定删除数据表 [${data.label}] 吗？此操作不可逆。`, '警告', {
        type: 'warning'
    }).then(() => {
        ElMessage.success('删除成功')
    })
}

// Tree Filtering
watch(treeSearch, (val) => {
    treeRef.value.filter(val)
})

const filterNode = (value, data) => {
    if (!value) return true
    return data.label.includes(value)
}

// Source Config
const sourceVisible = ref(false)
const sourceForm = ref({ name: '', url: '', type: 'mysql' })
const openSourceConfig = () => { sourceVisible.value = true }
const saveSource = () => {
    ElMessage.success('数据源配置成功')
    sourceVisible.value = false
}

// Summary Tab State
const filterType = ref('')
const summarySearch = ref('')

const initChart = () => {
    if (!chartRef.value) return
    myChart = echarts.init(chartRef.value)
    updateChart()
}

const updateChart = () => {
    if (!myChart) return

    const nodes = []
    const links = []
    
    dbTreeData.value.forEach(db => {
        // DB Node
        nodes.push({
            id: db.id,
            name: db.label,
            symbolSize: 55,
            itemStyle: { 
                color: '#00f2ff', 
                shadowBlur: 15, 
                shadowColor: '#00f2ff',
                borderColor: 'rgba(0, 242, 255, 0.5)',
                borderWidth: 2
            },
            category: 0,
            value: db.type
        })
        
        db.children?.forEach(table => {
            // Table Node
            nodes.push({
                id: table.id,
                name: table.label,
                symbolSize: 35,
                itemStyle: { 
                    color: '#67c23a', 
                    shadowBlur: 10, 
                    shadowColor: '#67c23a',
                    borderColor: 'rgba(103, 194, 58, 0.5)',
                    borderWidth: 1
                },
                category: 1,
                value: table.type
            })
            
            // Link DB -> Table (Parent-Child)
            links.push({
                source: db.id,
                target: table.id,
                lineStyle: { color: 'rgba(0, 242, 255, 0.15)', width: 1, type: 'dashed' },
                symbol: ['', '']
            })
        })
    })

    // Table-to-Table Links (Semantic/Data flow)
    tableToTableLinks.forEach(link => {
        links.push({
            source: link.source,
            target: link.target,
            label: { show: true, formatter: link.label, fontSize: 10, color: '#8fa3b8' },
            lineStyle: { color: 'rgba(255, 255, 255, 0.3)', width: 2, type: 'solid', curveness: 0.2 },
            symbol: ['', 'arrow'],
            symbolSize: [0, 8]
        })
    })

    const option = {
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item', formatter: '{c}: {b}' },
        animationDurationUpdate: 1500,
        animationEasingUpdate: 'quinticInOut',
        series: [{
            type: 'graph',
            layout: 'force',
            data: nodes,
            links: links,
            edgeSymbol: ['none', 'arrow'],
            edgeSymbolSize: [0, 5],
            categories: [{ name: '数据库' }, { name: '数据表' }],
            roam: true,
            label: {
                show: true,
                position: 'bottom',
                color: '#e6edf3',
                fontSize: 11,
                fontFamily: 'Roboto Mono',
                formatter: (params) => params.data.type === 'db' ? `{b}` : params.name
            },
            force: {
                repulsion: 1200,
                edgeLength: [100, 200],
                gravity: 0.05
            },
            lineStyle: { opacity: 0.8, width: 1, curveness: 0 },
            emphasis: {
                focus: 'adjacency',
                lineStyle: { width: 4, color: '#00f2ff' }
            }
        }]
    }

    myChart.setOption(option)
}

watch(activeTab, (val) => {
    if (val === 'summary') {
        nextTick(() => {
            if (!myChart) {
                initChart()
            } else {
                myChart.resize()
                updateChart()
            }
        })
    }
})

onMounted(() => {
    window.addEventListener('resize', () => myChart?.resize())
})

onUnmounted(() => {
    myChart?.dispose()
})
</script>

<style scoped lang="scss">
.resource-container {
    height: 100%;
    background: #050a14;
    padding: 10px 20px;
    display: flex;
    flex-direction: column;
}

.main-tabs {
    height: 100%;
    display: flex;
    flex-direction: column;
    :deep(.el-tabs__content) { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
    :deep(.el-tab-pane) { height: 100%; display: flex; flex-direction: column; }
}

/* Catalog Management Styles */
.catalog-container {
    flex: 1;
    overflow: hidden;
    gap: 20px;
    padding: 10px 0;
}

.catalog-aside {
    background: rgba(16, 32, 53, 0.4);
    border: 1px solid rgba(0, 242, 255, 0.1);
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    padding: 15px;

    .aside-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
        .title { color: #00f2ff; font-weight: bold; font-size: 15px; }
    }
    
    .aside-search { margin-bottom: 12px; }
    .tree-wrapper { flex: 1; overflow-y: auto; }
}

.cyber-tree {
    background: transparent !important;
    color: #e6edf3;
    --el-tree-node-hover-bg-color: rgba(0, 242, 255, 0.05);
    
    :deep(.el-tree-node__content) { height: 32px; }
    :deep(.el-tree-node.is-current > .el-tree-node__content) {
        background-color: rgba(0, 242, 255, 0.1) !important;
        color: #00f2ff;
    }
}

.custom-tree-node {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    .label { font-size: 13px; }
    .actions {
        margin-left: auto;
        display: none;
        gap: 8px;
        color: #8fa3b8;
        .el-icon:hover { color: #00f2ff; }
    }
    &:hover .actions { display: flex; }
}

.catalog-main {
    background: rgba(16, 32, 53, 0.2);
    border: 1px solid rgba(0, 242, 255, 0.1);
    border-radius: 8px;
    padding: 24px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.table-details {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;
    
    .table-info {
        .tag {
            font-size: 10px;
            background: rgba(0, 242, 255, 0.2);
            padding: 2px 6px;
            border-radius: 4px;
            color: #00f2ff;
        }
        .table-name { margin: 8px 0 4px; font-size: 22px; color: #fff; }
        .db-path { font-size: 12px; color: #8fa3b8; }
    }
}

.detail-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;
    :deep(.el-tabs__content) { flex: 1; overflow-y: auto; padding-top: 15px; }
}

/* Summary Tab Styles (ECharts) */
.summary-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 20px;
}

.summary-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    background: rgba(16, 32, 53, 0.4);
    padding: 12px 20px;
    border-radius: 8px;
    border: 1px solid rgba(0, 242, 255, 0.1);
}

.legend {
    display: flex;
    gap: 20px;
    font-size: 12px;
    color: #8fa3b8;
    .dot {
        width: 10px; height: 10px; border-radius: 50%; display: inline-block;
        &.db { background: #00f2ff; box-shadow: 0 0 5px #00f2ff; }
        &.table { background: #67c23a; box-shadow: 0 0 5px #67c23a; }
    }
}

.graph-viewport {
    flex: 1;
    background: radial-gradient(circle at center, #0a1b2d 0%, #050a14 100%);
    border: 1px solid rgba(0, 242, 255, 0.1);
    border-radius: 12px;
    position: relative;
    overflow: hidden;
}

.echarts-container {
    width: 100%;
    height: 100%;
}

.graph-empty {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.2);
}

/* Table Aesthetics */
.cyber-table {
    background: transparent !important;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: rgba(0, 242, 255, 0.05);
    --el-table-border-color: rgba(255, 255, 255, 0.05);
    
    :deep(th.el-table__cell) { color: #00f2ff !important; font-weight: bold; }
    :deep(.el-input__wrapper) { background: rgba(0, 0, 0, 0.3) !important; box-shadow: none !important; border: 1px solid rgba(0, 242, 255, 0.2); }
}

.small-table { font-size: 13px; }

.empty-state {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
}
</style>
