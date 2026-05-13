<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="100px">
      <el-form-item label="指标名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入指标名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指标类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择指标类型" clearable style="width: 200px">
          <el-option
            v-for="item in indicatorTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="indicatorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="指标名称" align="center" prop="name" />
      <el-table-column label="指标类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="indicatorTypeOptions" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="是否为底层指标" align="center" prop="isBase">
        <template #default="scope">
          <el-tag :type="scope.row.isBase ? 'success' : 'info'">
            {{ scope.row.isBase ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="指标关联算法" align="center" prop="algorithm" />
      <el-table-column label="创建人" align="center" prop="creator" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增或修改指标对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="indicatorRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="指标名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="指标类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择指标类型" style="width: 100%">
            <el-option
              v-for="item in indicatorTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="指标关联算法" prop="algorithm">
        
              <el-button @click="handleConfigAlgorithm">配置算法</el-button>
            
        </el-form-item>
        <el-form-item label="是否为底层指标" prop="isBase">
          <el-radio-group v-model="form.isBase">
            <el-radio :label="true">是</el-radio>
            <el-radio :label="false">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 配置算法对话框 -->
    <el-dialog title="配置算法" v-model="algorithmOpen" width="1200px" append-to-body>
      <div class="algorithm-config-container">
        <!-- 左侧节点面板 -->
        <div class="node-panel">
          <h4>节点库</h4>
          <div class="node-list">
            <div 
              class="node-item" 
              draggable="true" 
              data-type="data"
              @dragstart="handleDragStart"
            >
              <div class="node-icon data-node"></div>
              <span>数据</span>
            </div>
            <div 
              class="node-item" 
              draggable="true" 
              data-type="algorithm"
              @dragstart="handleDragStart"
            >
              <div class="node-icon algorithm-node"></div>
              <span>算法</span>
            </div>
            <div 
              class="node-item" 
              draggable="true" 
              data-type="result"
              @dragstart="handleDragStart"
            >
              <div class="node-icon result-node"></div>
              <span>结果</span>
            </div>
          </div>
        </div>
        
        <!-- 右侧画布 -->
        <div class="graph-container">
          <div id="graph-container" class="graph-canvas"></div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="saveAlgorithmConfig">保 存</el-button>
          <el-button @click="algorithmOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Indicator">
import { listIndicator, getIndicator, delIndicator, addIndicator, updateIndicator } from '@/api/systemPlus/capabilityResource/indicator'
import { Graph, Cell } from '@antv/x6'
// ELK will be loaded dynamically to avoid module import issues

const { proxy } = getCurrentInstance()

const indicatorList = ref([])
const open = ref(false)
const algorithmOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

// 图形化编辑器相关变量
const graph = ref(null)
const elk = ref(null)
const portIdToNodeIdMap = ref({})
const cells = ref([])

// 节点计数器
let nodeCounter = 1

// 指标类型选项
const indicatorTypeOptions = ref([
  { value: '1', label: '作战效能' },
  { value: '2', label: '装备体系配置合理性' },
  { value: '3', label: '装备体系运行稳定性' },
  { value: '4', label: '装备体系反应敏捷性' },
  { value: '5', label: '装备体系能力增长性' }
])

// 指标子体系选项（用于新增弹窗）
const indicatorSubSystemOptions = ref([
  { value: '1', label: '航天侦察装备子体系' },
  { value: '2', label: '太空态势感知装备子体系' },
  { value: '3', label: '太空攻防装备子体系' },
  { value: '4', label: '航天发射装备子体系' },
  { value: '5', label: '航天测运控装备子体系' },
  { value: '6', label: '航天装备联合作战体系' }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined
  },
  rules: {
    name: [{ required: true, message: "指标名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "指标类型不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询指标列表 */
function getList() {
  loading.value = true
  listIndicator(queryParams.value).then(response => {
    indicatorList.value = response.rows
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
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加指标"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getIndicator(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改指标"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["indicatorRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateIndicator(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addIndicator(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除指标编号为"' + ids + '"的数据项？').then(function() {
    return delIndicator(ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 配置算法按钮操作 */
function handleConfigAlgorithm() {
  algorithmOpen.value = true
  // 延迟初始化图形编辑器，确保DOM已渲染
  nextTick(() => {
    initGraphEditor()
  })
}

/** 初始化图形编辑器 */
function initGraphEditor() {
  // 注册自定义节点
  Graph.registerNode(
    'elk-node',
    {
      inherit: 'rect',
      attrs: {
        body: {
          fill: '#EFF4FF',
          stroke: '#5F95FF',
          strokeWidth: 2,
          rx: 6,
          ry: 6,
        },
        label: {
          text: '',
          fontSize: 12,
          fill: '#333',
          fontWeight: 'bold',
          refX: '50%',
          refY: '50%',
          textAnchor: 'middle',
          textVerticalAnchor: 'middle',
        },
      },
      ports: {
        groups: {
          in: {
            position: {
              name: 'left',
            },
            attrs: {
              portBody: {
                magnet: 'passive',
                r: 5,
                fill: '#5F95FF',
                stroke: '#5F95FF',
                strokeWidth: 2,
              },
            },
            markup: [
              {
                tagName: 'circle',
                selector: 'portBody',
              },
            ],
          },
          out: {
            position: {
              name: 'right',
            },
            attrs: {
              portBody: {
                magnet: 'active',
                r: 5,
                fill: '#5F95FF',
                stroke: '#5F95FF',
                strokeWidth: 2,
              },
            },
            markup: [
              {
                tagName: 'circle',
                selector: 'portBody',
              },
            ],
          },
        },
      },
    },
    true,
  )

  // 注册自定义边
  Graph.registerEdge(
    'elk-edge',
    {
      inherit: 'edge',
      attrs: {
        line: {
          stroke: '#A2B1C3',
          strokeWidth: 1,
          targetMarker: {
            name: 'block',
            width: 4,
            height: 4,
          },
        },
      },
    },
    true,
  )

  // 初始化图形实例
  graph.value = new Graph({
    container: document.getElementById('graph-container'),
    width: '100%',
    height: '100%',
    grid: false,
    //   size: 10,
    //   visible: true,
    // },
    interacting: {
      nodeMovable: true,
      edgeMovable: false,
      edgeLabelMovable: false,
      arrowheadMovable: false,
      vertexMovable: false,
      vertexAddable: false,
      vertexDeletable: false,
    },
    connecting: {
      anchor: 'center',
      connectionPoint: 'anchor',
      router: {
        name: 'manhattan',
        args: {
          step: 10,
        },
      },
      connector: {
        name: 'rounded',
        args: {
          radius: 8,
        },
      },
      allowBlank: false,
      snap: {
        radius: 20,
      },
      createEdge() {
        return graph.value.createEdge({
          shape: 'elk-edge',
          attrs: {
            line: {
              stroke: '#5F95FF',
              strokeWidth: 2,
              targetMarker: {
                name: 'block',
                width: 12,
                height: 8,
              },
            },
          },
          zIndex: 0,
        })
      },
      validateConnection({ sourceView, targetView, sourceMagnet, targetMagnet }) {
        // 不允许连接到自身
        if (sourceView === targetView) {
          return false
        }
        // 只允许从输出端口连接到输入端口
        if (sourceMagnet && targetMagnet) {
          const sourcePort = sourceMagnet.getAttribute('port-group')
          const targetPort = targetMagnet.getAttribute('port-group')
          return sourcePort === 'out' && targetPort === 'in'
        }
        return false
      },
    },
    highlighting: {
      magnetAdsorbed: {
        name: 'stroke',
        args: {
          attrs: {
            fill: '#5F95FF',
            stroke: '#5F95FF',
          },
        },
      },
    },
  })

  // 初始化ELK布局引擎 - 使用动态导入
  import('elkjs/lib/elk-api.js').then(module => {
    const ELK = module.default || module
    elk.value = new ELK()
  }).catch(error => {
    console.error('Failed to load ELK:', error)
    // 如果动态导入失败，尝试使用全局变量
    if (window.ELK) {
      elk.value = new window.ELK()
    }
  })

  // 启用拖拽放置
  setupDragAndDrop()
}

/** 设置拖拽放置功能 */
function setupDragAndDrop() {
  const container = document.getElementById('graph-container')
  
  container.addEventListener('dragover', (e) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'copy'
  })

  container.addEventListener('drop', (e) => {
    e.preventDefault()
    const type = e.dataTransfer.getData('type')
    const rect = container.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    
    addNodeToGraph(type, x, y)
  })
}

/** 处理拖拽开始 */
function handleDragStart(e) {
  e.dataTransfer.setData('type', e.target.dataset.type)
}

/** 添加节点到图形 */
function addNodeToGraph(type, x, y) {
  const nodeId = `node-${nodeCounter++}`
  const label = getNodeLabel(type)
  const color = getNodeColor(type)

  const node = graph.value.createNode({
    shape: 'elk-node',
    id: nodeId,
    x: x - 60,
    y: y - 30,
    width: 120,
    height: 60,
    attrs: {
      body: {
        fill: color,
      },
      label: {
        text: label,
      },
    },
    ports: {
      items: [
        { id: `${nodeId}-in`, group: 'in' },
        { id: `${nodeId}-out`, group: 'out' },
      ],
    },
  })

  graph.value.addNode(node)
}

/** 获取节点标签 */
function getNodeLabel(type) {
  const labels = {
    data: '数据节点',
    algorithm: '算法节点',
    result: '结果节点'
  }
  return labels[type] || '未知节点'
}

/** 获取节点颜色 */
function getNodeColor(type) {
  const colors = {
    data: '#E6F7FF',
    algorithm: '#F6FFED',
    result: '#FFF7E6'
  }
  return colors[type] || '#EFF4FF'
}

/** 保存算法配置 */
function saveAlgorithmConfig() {
  const data = graph.value.toJSON()
  proxy.$modal.msgSuccess('算法配置已保存')
  algorithmOpen.value = false
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    type: undefined,
    isBase: false,
    algorithm: undefined,
    creator: undefined
  }
  proxy.resetForm("indicatorRef")
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/v2/variables.scss' as *;
@use 'sass:color';

.app-container {
  padding: 20px;
}

.el-form-item {
  margin-bottom: 15px;
}

.mb8 {
  margin-bottom: 8px;
}

// 分页样式优化
:deep(.el-pagination) {
  margin-top: 20px;
  text-align: right;
  
  .btn-prev,
  .btn-next,
  .number {
    background: $card-bg-color;
    border: 1px solid $border-color;
    color: $text-color-primary;
    margin: 0 4px;
    border-radius: 4px;
    
    &:hover {
      border-color: $primary-color;
      color: $primary-color;
      box-shadow: $box-shadow-neon;
    }
    
    &.active {
      background: linear-gradient(90deg, $primary-color 0%, #{color.adjust($primary-color, $lightness: -15%)} 100%);
      border-color: $primary-color;
      color: #ffffff;
      box-shadow: $box-shadow-neon;
    }
    
    &.disabled {
      background: rgba(255, 255, 255, 0.05);
      border-color: $border-color;
      color: $text-color-placeholder;
      cursor: not-allowed;
    }
  }
  
  .el-pagination__jump {
    color: $text-color-primary;
    
    .el-input__wrapper {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid $border-color;
      color: $text-color-primary;
      
      &:hover {
        border-color: $primary-color;
      }
    }
  }
  
  .el-pagination__total {
    color: $text-color-secondary;
    margin-right: 10px;
  }
}

// 表格样式优化
.el-table {
  background: transparent;
  
  :deep(.el-table__header-wrapper) {
    th {
      background: rgba(0, 242, 255, 0.1);
      color: $primary-color;
      font-weight: bold;
      border-bottom: 1px solid $primary-color;
    }
  }
  
  :deep(.el-table__body-wrapper) {
    tr:hover > td {
      background: rgba(0, 242, 255, 0.05);
    }
    
    td {
      border-bottom: 1px solid $border-color;
      color: $text-color-primary;
    }
  }
}

// 按钮样式优化
.el-button {
  transition: all 0.3s;
  
  &.el-button--primary {
    background: linear-gradient(90deg, $primary-color 0%, darken($primary-color, 15%) 100%);
    border: none;
    
    &:hover {
      background: linear-gradient(90deg, #{color.adjust($primary-color, $lightness: 10%)} 0%, $primary-color 100%);
      box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
    }
  }
  
  &.el-button--default {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid $primary-color;
    color: $primary-color;
    
    &:hover {
      background: rgba(0, 242, 255, 0.2);
      box-shadow: 0 0 10px rgba(0, 242, 255, 0.3);
    }
  }
}

// 弹窗样式优化
.el-dialog {
  background: $card-bg-color;
  border: 1px solid $border-color;
  border-radius: 8px;
  
  .el-dialog__header {
    border-bottom: 1px solid $border-color;
    color: $text-color-primary;
  }
}

// 算法配置容器样式
.algorithm-config-container {
  display: flex;
  height: 600px;
  gap: 20px;
}

// 左侧节点面板样式
.node-panel {
  width: 200px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid $border-color;
  border-radius: 8px;
  padding: 15px;
  
  h4 {
    color: $primary-color;
    margin-bottom: 15px;
    text-align: center;
    font-size: 16px;
  }
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.node-item {
  display: flex;
  align-items: center;
  padding: 10px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid $border-color;
  border-radius: 6px;
  cursor: grab;
  transition: all 0.3s;
  
  &:hover {
    background: rgba(0, 242, 255, 0.1);
    border-color: $primary-color;
    transform: translateY(-2px);
  }
  
  .node-icon {
    width: 30px;
    height: 30px;
    border-radius: 4px;
    margin-right: 10px;
  }
  
  .data-node {
    background: linear-gradient(135deg, #E6F7FF 0%, #91D5FF 100%);
    border: 1px solid #91D5FF;
  }
  
  .algorithm-node {
    background: linear-gradient(135deg, #F6FFED 0%, #B7EB8F 100%);
    border: 1px solid #B7EB8F;
  }
  
  .result-node {
    background: linear-gradient(135deg, #FFF7E6 0%, #FFD591 100%);
    border: 1px solid #FFD591;
  }
  
  span {
    color: $text-color-primary;
    font-size: 14px;
  }
}

// 右侧画布容器样式
.graph-container {
  flex: 1;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid $border-color;
  border-radius: 8px;
  overflow: hidden;
}

.graph-canvas {
  width: 100%;
  height: 100%;
  background: rgba(224, 240, 240, 0.3);
}

// 对话框底部样式
.dialog-footer {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid $border-color;
  
  .el-button {
    margin: 0 10px;
  }
}

// 弹窗样式优化
.el-dialog {
  .el-dialog__header {
    .el-dialog__title {
      color: $text-color-primary;
      font-weight: bold;
    }
  }
  
  .el-dialog__body {
    color: $text-color-primary;
  }
  
  .el-dialog__footer {
    border-top: 1px solid $border-color;
  }
}
</style>