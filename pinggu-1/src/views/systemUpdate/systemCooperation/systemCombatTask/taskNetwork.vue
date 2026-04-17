<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="140px">
      <el-form-item label="任务网名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入任务网名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评估任务名称" prop="task">
        <el-input
          v-model="queryParams.task"
          placeholder="请输入评估任务名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="networkList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      @selection-change="handleSelectionChange"
    >
      <template #operationSlot="{ row }">
        <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        <el-button link type="info" icon="View" @click="handleDetail(row)">详情</el-button>
      </template>
    </DynamicTable>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 修改对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="networkRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="任务网名称" prop="networkName">
          <el-input v-model="form.networkName" placeholder="请输入任务网名称" />
        </el-form-item>
        <el-form-item label="所属评估任务" prop="evaluationTask">
          <el-input v-model="form.evaluationTask" placeholder="请输入所属评估任务" />
        </el-form-item>
        <el-form-item label="任务节点信息" prop="nodeInfo">
          <el-input v-model="form.nodeInfo" placeholder="请输入任务节点信息" />
        </el-form-item>
        <el-form-item label="任务边信息" prop="edgeInfo">
          <el-input v-model="form.edgeInfo" placeholder="请输入任务边信息" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务网详情弹窗 -->
    <el-dialog :title="detailTitle" v-model="detailOpen" width="1200px" append-to-body>
      <div class="task-network-detail">
        <div class="detail-container">
          <!-- 左侧树形结构 -->
          <div class="left-panel">
            <div class="panel-title">任务网结构</div>
            <div class="tree-container">
              <el-tree
                ref="detailTreeRef"
                :data="detailTreeData"
                :props="detailTreeProps"
                node-key="id"
                :expand-on-click-node="false"
                :highlight-current="true"
                @node-click="handleTreeNodeClick"
                class="detail-tree"
              >
                <template #default="{ node, data }">
                  <span class="tree-node-content">
                    <span class="node-label">{{ node.label }}</span>
                    <span v-if="data.type === 'task'" class="node-badge">T{{ data.id }}</span>
                    <span v-if="data.type === 'subtask'" class="node-badge">S{{ data.id }}</span>
                    <span v-if="data.type === 'edge'" class="edge-badge">E{{ data.id }}</span>
                  </span>
                </template>
              </el-tree>
            </div>
          </div>
          
          <!-- 右侧画布 -->
          <div class="right-panel">
            <div class="canvas-container">
              <div id="task-network-canvas" ref="taskNetworkCanvas" class="task-network-canvas"></div>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetail">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TaskNetwork">
// import { listTaskNetwork, getTaskNetwork, delTaskNetwork, updateTaskNetwork } from '@/api/systemPlus/systemCooperation/taskNetwork'
import DynamicTable from '@/components/DynamicTable/index.vue'
import { Graph, Shape } from '@antv/x6'
import { nextTick, getCurrentInstance, ref, reactive, toRefs } from 'vue'

const { proxy } = getCurrentInstance()

const networkList = ref([])
const open = ref(false)
const detailOpen = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")
const detailTitle = ref("")

// 任务网详情相关变量
const detailTreeData = ref([])
const detailTreeProps = ref({
  children: 'children',
  label: 'label'
})
const taskNetworkGraph = ref(null)
const taskNetworkCanvas = ref(null)

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'networkName', label: '任务网名称', width: 200, showOverflowTooltip: true },
  { key: 'evaluationTask', label: '所属评估任务', width: 150, showOverflowTooltip: true },
  { key: 'nodeInfo', label: '任务节点信息', width: 180, showOverflowTooltip: true },
  { key: 'edgeInfo', label: '任务边信息', width: 180, showOverflowTooltip: true },
  { key: 'createTime', label: '创建时间', width: 120 },
  {
    key: 'operation',
    label: '操作',
    width: 240,
    customColumn: true,
    slotName: 'operationSlot'
  }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    task: undefined
  },
  rules: {
    networkName: [{ required: true, message: "任务网名称不能为空", trigger: "blur" }],
    evaluationTask: [{ required: true, message: "所属评估任务不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listTaskNetwork(queryParams.value).then(response => {
    networkList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  // 处理选中数据
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  getTaskNetwork(row.id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改任务网"
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除任务网编号为"' + row.id + '"的数据项？').then(function() {
    return delTaskNetwork(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 详情按钮操作 */
function handleDetail(row) {
  detailTitle.value = row.networkName + " - 任务网详情"
  detailOpen.value = true
  
  // 初始化树形结构数据
  initDetailTreeData()
  
  // 初始化画布
  nextTick(() => {
    initTaskNetworkGraph()
  })
}

/** 初始化任务网树形结构数据 */
function initDetailTreeData() {
  detailTreeData.value = [
    {
      id: 'root',
      label: '任务网根节点',
      type: 'root',
      children: [
        {
          id: 'tasks',
          label: '任务节点',
          type: 'task-group',
          children: [
            { id: 'T1', label: '任务T1', type: 'task' },
            { id: 'T2', label: '任务T2', type: 'task' },
            { id: 'T3', label: '任务T3', type: 'task' },
            { id: 'T4', label: '任务T4', type: 'task' }
          ]
        },
        {
          id: 'subtasks',
          label: '子任务节点',
          type: 'subtask-group',
          children: [
            { id: 'S1', label: '子任务S1', type: 'subtask' },
            { id: 'S2', label: '子任务S2', type: 'subtask' },
            { id: 'S3', label: '子任务S3', type: 'subtask' },
            { id: 'S4', label: '子任务S4', type: 'subtask' },
            { id: 'S5', label: '子任务S5', type: 'subtask' },
            { id: 'S6', label: '子任务S6', type: 'subtask' }
          ]
        },
        {
          id: 'edges',
          label: '任务边关系',
          type: 'edge-group',
          children: [
            { id: 'E1', label: 'T1→T2', type: 'edge' },
            { id: 'E2', label: 'T2→T3', type: 'edge' },
            { id: 'E3', label: 'T1→S1', type: 'edge' },
            { id: 'E4', label: 'T2→S2', type: 'edge' },
            { id: 'E5', label: 'T3→S3', type: 'edge' }
          ]
        }
      ]
    }
  ]
}

/** 初始化任务网画布 */
function initTaskNetworkGraph() {
  if (!taskNetworkCanvas.value) return
  
  // 销毁之前的图形实例
  if (taskNetworkGraph.value) {
    taskNetworkGraph.value.dispose()
  }
  
  // 创建图形实例
  taskNetworkGraph.value = new Graph({
    container: taskNetworkCanvas.value,
    width: 800,
    height: 600,
    grid: true,
    mousewheel: {
      enabled: true,
      zoomAtMousePosition: true,
      modifiers: 'ctrl',
      minScale: 0.5,
      maxScale: 3
    },
    connecting: {
      router: 'manhattan',
      connector: {
        name: 'rounded',
        args: { radius: 8 }
      },
      createEdge() {
        return new Shape.Edge({
          attrs: {
            line: {
              stroke: '#A2B1C3',
              strokeWidth: 2
            }
          }
        })
      }
    }
  })
  
  // 创建任务区域和子任务区域
  createTaskNetworkStructure()
}

/** 创建任务网络结构 */
function createTaskNetworkStructure() {
  // 创建统一的任务区域（黑色主题）
  const taskArea = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 50,
    y: 50,
    width: 500,
    height: 120,
    attrs: {
      body: {
        fill: 'rgba(30, 30, 30, 0.8)',
        stroke: '#434343',
        strokeWidth: 2,
        rx: 8,
        ry: 8
      },
      label: {
        text: '任务区域',
        fontSize: 14,
        fill: '#e8e8e8',
        fontWeight: 'bold'
      }
    }
  })
  
  // 创建统一的子任务区域（黑色主题）
  const subtaskArea = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 50,
    y: 200,
    width: 500,
    height: 200,
    attrs: {
      body: {
        fill: 'rgba(40, 40, 40, 0.8)',
        stroke: '#434343',
        strokeWidth: 2,
        rx: 8,
        ry: 8
      },
      label: {
        text: '子任务区域',
        fontSize: 14,
        fill: '#e8e8e8',
        fontWeight: 'bold'
      }
    }
  })
  
  // 创建任务节点（在统一的任务区域内）
  const task1 = taskNetworkGraph.value.addNode({
    shape: 'circle',
    x: 100,
    y: 80,
    width: 50,
    height: 50,
    attrs: {
      body: {
        fill: '#177ddc',
        stroke: '#0958d9'
      },
      label: {
        text: 'T1',
        fill: '#fff',
        fontSize: 14,
        fontWeight: 'bold'
      }
    }
  })
  
  const task2 = taskNetworkGraph.value.addNode({
    shape: 'circle',
    x: 200,
    y: 80,
    width: 50,
    height: 50,
    attrs: {
      body: {
        fill: '#177ddc',
        stroke: '#0958d9'
      },
      label: {
        text: 'T2',
        fill: '#fff',
        fontSize: 14,
        fontWeight: 'bold'
      }
    }
  })
  
  const task3 = taskNetworkGraph.value.addNode({
    shape: 'circle',
    x: 300,
    y: 80,
    width: 50,
    height: 50,
    attrs: {
      body: {
        fill: '#177ddc',
        stroke: '#0958d9'
      },
      label: {
        text: 'T3',
        fill: '#fff',
        fontSize: 14,
        fontWeight: 'bold'
      }
    }
  })
  
  const task4 = taskNetworkGraph.value.addNode({
    shape: 'circle',
    x: 400,
    y: 80,
    width: 50,
    height: 50,
    attrs: {
      body: {
        fill: '#177ddc',
        stroke: '#0958d9'
      },
      label: {
        text: 'T4',
        fill: '#fff',
        fontSize: 14,
        fontWeight: 'bold'
      }
    }
  })
  
  // 创建子任务节点（在统一的子任务区域内）
  const subtask1 = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 80,
    y: 250,
    width: 60,
    height: 40,
    attrs: {
      body: {
        fill: '#49aa19',
        stroke: '#389e0d',
        rx: 4,
        ry: 4
      },
      label: {
        text: 'S1',
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }
  })
  
  const subtask2 = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 160,
    y: 250,
    width: 60,
    height: 40,
    attrs: {
      body: {
        fill: '#49aa19',
        stroke: '#389e0d',
        rx: 4,
        ry: 4
      },
      label: {
        text: 'S2',
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }
  })
  
  const subtask3 = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 240,
    y: 250,
    width: 60,
    height: 40,
    attrs: {
      body: {
        fill: '#49aa19',
        stroke: '#389e0d',
        rx: 4,
        ry: 4
      },
      label: {
        text: 'S3',
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }
  })
  
  const subtask4 = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 320,
    y: 250,
    width: 60,
    height: 40,
    attrs: {
      body: {
        fill: '#49aa19',
        stroke: '#389e0d',
        rx: 4,
        ry: 4
      },
      label: {
        text: 'S4',
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }
  })
  
  const subtask5 = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 400,
    y: 250,
    width: 60,
    height: 40,
    attrs: {
      body: {
        fill: '#49aa19',
        stroke: '#389e0d',
        rx: 4,
        ry: 4
      },
      label: {
        text: 'S5',
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }
  })
  
  const subtask6 = taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 480,
    y: 250,
    width: 60,
    height: 40,
    attrs: {
      body: {
        fill: '#49aa19',
        stroke: '#389e0d',
        rx: 4,
        ry: 4
      },
      label: {
        text: 'S6',
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    }
  })
  
  // 创建连线（黑色主题）
  // 任务间连线
  taskNetworkGraph.value.addEdge({
    source: task1,
    target: task2,
    attrs: {
      line: {
        stroke: '#177ddc',
        strokeWidth: 2
      }
    }
  })
  
  taskNetworkGraph.value.addEdge({
    source: task2,
    target: task3,
    attrs: {
      line: {
        stroke: '#177ddc',
        strokeWidth: 2
      }
    }
  })
  
  taskNetworkGraph.value.addEdge({
    source: task3,
    target: task4,
    attrs: {
      line: {
        stroke: '#177ddc',
        strokeWidth: 2
      }
    }
  })
  
  // 任务与子任务连线（1对1对应关系）
  taskNetworkGraph.value.addEdge({
    source: task1,
    target: subtask1,
    attrs: {
      line: {
        stroke: '#49aa19',
        strokeWidth: 2,
        strokeDasharray: '5,2'
      }
    }
  })
  
  taskNetworkGraph.value.addEdge({
    source: task2,
    target: subtask2,
    attrs: {
      line: {
        stroke: '#49aa19',
        strokeWidth: 2,
        strokeDasharray: '5,2'
      }
    }
  })
  
  taskNetworkGraph.value.addEdge({
    source: task3,
    target: subtask3,
    attrs: {
      line: {
        stroke: '#49aa19',
        strokeWidth: 2,
        strokeDasharray: '5,2'
      }
    }
  })
  
  taskNetworkGraph.value.addEdge({
    source: task4,
    target: subtask4,
    attrs: {
      line: {
        stroke: '#49aa19',
        strokeWidth: 2,
        strokeDasharray: '5,2'
      }
    }
  })
  
  // 子任务间连线
  taskNetworkGraph.value.addEdge({
    source: subtask1,
    target: subtask2,
    attrs: {
      line: {
        stroke: '#722ed1',
        strokeWidth: 2,
        strokeDasharray: '3,3'
      }
    }
  })
  
  taskNetworkGraph.value.addEdge({
    source: subtask3,
    target: subtask4,
    attrs: {
      line: {
        stroke: '#722ed1',
        strokeWidth: 2,
        strokeDasharray: '3,3'
      }
    }
  })
  
  taskNetworkGraph.value.addEdge({
    source: subtask5,
    target: subtask6,
    attrs: {
      line: {
        stroke: '#722ed1',
        strokeWidth: 2,
        strokeDasharray: '3,3'
      }
    }
  })
}

/** 树节点点击事件 */
function handleTreeNodeClick(data) {
  console.log('树节点点击:', data)
}

/** 关闭详情弹窗 */
function closeDetail() {
  detailOpen.value = false
  if (taskNetworkGraph.value) {
    taskNetworkGraph.value.dispose()
    taskNetworkGraph.value = null
  }
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["networkRef"].validate(valid => {
    if (valid) {
      updateTaskNetwork(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    }
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    networkName: null,
    evaluationTask: null,
    nodeInfo: null,
    edgeInfo: null
  }
  proxy.resetForm("networkRef")
}

/** 初始化 **/
getList()
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 20px;
}

.dialog-footer {
  text-align: center;
}

.dialog-footer .el-button {
  margin: 0 10px;
}

/* 动态表格样式 */
:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  text-align: center;
}

/* 任务网详情弹窗样式 - 黑色主题 */
.task-network-detail {
  height: 600px;
}

.detail-container {
  display: flex;
  height: 100%;
  gap: 20px;
}

.left-panel {
  width: 300px;
  border: 1px solid #434343;
  border-radius: 6px;
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
}

.panel-title {
  padding: 12px 16px;
  background: #262626;
  border-bottom: 1px solid #434343;
  font-weight: 600;
  font-size: 14px;
  color: #e8e8e8;
}

.tree-container {
  height: calc(100% - 49px);
  overflow-y: auto;
  padding: 8px;
}

.detail-tree {
  background: transparent;
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.node-label {
  flex: 1;
  font-size: 13px;
  color: #e8e8e8;
}

.node-badge {
  background: #177ddc;
  color: white;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.edge-badge {
  background: #49aa19;
  color: white;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.right-panel {
  flex: 1;
  border: 1px solid #434343;
  border-radius: 6px;
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
}

.canvas-container {
  height: 100%;
  padding: 10px;
}

.task-network-canvas {
  width: 100%;
  height: 100%;
  background: #141414;
  border: 1px solid #434343;
  border-radius: 4px;
}

/* 树形结构样式优化 - 黑色主题 */
:deep(.detail-tree .el-tree-node) {
  margin: 2px 0;
}

:deep(.detail-tree .el-tree-node__content) {
  height: 32px;
  border-radius: 4px;
  transition: background-color 0.2s;
  color: #e8e8e8;
}

:deep(.detail-tree .el-tree-node__content:hover) {
  background-color: #2a2a2a;
}

:deep(.detail-tree .el-tree-node.is-current > .el-tree-node__content) {
  background-color: #15395b;
  border: 1px solid #177ddc;
}

:deep(.detail-tree .el-tree-node__expand-icon) {
  color: #8c8c8c;
}

:deep(.detail-tree .el-tree-node__expand-icon:hover) {
  color: #e8e8e8;
}

/* 任务区域节点样式 */
:deep(.task-area) {
  fill: #e6f7ff;
  stroke: #91d5ff;
  stroke-width: 2;
}

:deep(.subtask-area) {
  fill: #fff7e6;
  stroke: #ffd591;
  stroke-width: 2;
  rx: 10;
  ry: 10;
}

:deep(.task-node) {
  fill: #1890ff;
  stroke: #096dd9;
}

:deep(.subtask-node) {
  fill: #fa8c16;
  stroke: #d46b08;
}

:deep(.task-label) {
  fill: #fff;
  font-size: 14px;
  font-weight: 500;
}

:deep(.subtask-label) {
  fill: #fff;
  font-size: 12px;
  font-weight: 500;
}

:deep(.task-edge) {
  stroke: #1890ff;
  stroke-width: 2;
}

:deep(.subtask-edge) {
  stroke: #fa8c16;
  stroke-width: 2;
  stroke-dasharray: 5,2;
}
</style>