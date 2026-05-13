<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="120px">
      <el-form-item label="策略名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入策略名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="装备等级" prop="equipmentLevel">
        <el-select v-model="queryParams.equipmentLevel" placeholder="请选择装备等级" clearable style="width: 200px">
          <el-option label="单装" value="0" />
          <el-option label="装备系统" value="1" />
          <el-option label="装备子系统" value="2" />
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

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="strategyList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无装备协同策略数据"
      @selection-change="handleSelectionChange"
    >
      <template #operationSlot="{ row }">
        <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
      </template>
    </DynamicTable>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="1200px" append-to-body class="strategy-dialog">
      <el-form ref="strategyRef" :model="form" :rules="rules" label-width="180px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="策略名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入策略名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="装备等级" prop="equipmentLevel">
              <el-select v-model="form.equipmentLevel" placeholder="请选择装备等级" style="width: 100%">
                <el-option label="单装" value="0" />
                <el-option label="装备系统" value="1" />
                <el-option label="装备子系统" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="战术详情" prop="tacticDetails">
              <el-input v-model="form.tacticDetails" placeholder="请输入战术详情" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 图形化配置区域 -->
        <div class="graphical-configuration">
          <h4>装备协同组合图形化配置</h4>
          <div class="configuration-container">
            <!-- 左侧组件区域 -->
            <div class="left-panel">
              <!-- 装备类型组件（从字典获取） -->
              <div class="component-section">
                <h5>装备列表</h5>
                <div class="equipment-components">
                  <div 
                    v-for="equip in equipmentList" 
                    :key="equip.id"
                    class="equipment-component" 
                    draggable="true" 
                    :data-type="'equipment'"
                    :data-id="equip.id"
                    :data-name="equip.name"
                    :data-equipment-type="equip.equipmentType"
                    @dragstart="onDragStart"
                  >
                    <div class="component-icon" :style="{ color: getEquipmentColor(equip.equipmentType) }">
                      {{ getEquipmentIcon(equip.equipmentType) }}
                    </div>
                    <span>{{ equip.name }}</span>
                  </div>
                </div>
              </div>
              
              <!-- 作战元活动组件 -->
              <div class="component-section">
                <h5>作战元活动组件</h5>
                <div class="combat-components">
                  <div 
                    v-for="(activity, key) in combatActivities" 
                    :key="key"
                    class="combat-component" 
                    draggable="true" 
                    :data-type="'activity'"
                    :data-activity-key="key"
                    :data-activity-name="activity.name"
                    :data-activity-value="activity.value"
                    @dragstart="onDragStart"
                  >
                    <div class="component-shape" :style="{ background: activity.color }">
                      {{ activity.name }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 右侧画布区域 -->
            <div class="right-panel">
              <div class="canvas-container">
                <div id="strategy-canvas" ref="strategyCanvas" class="strategy-canvas"></div>
              </div>
            </div>
          </div>
        </div>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="EquipmentCoordinationStrategy">
import { listEquipmentSynergyTactics, getEquipmentSynergyTactics, deleteEquipmentSynergyTactics, insertEquipmentSynergyTactics, updateEquipmentSynergyTactics } from '@/api/systemPlus/systemCooperation/equipment'
import DynamicTable from '@/components/DynamicTable/index.vue'
import { Graph, Shape } from '@antv/x6'
import { getCurrentInstance, ref, reactive, toRefs, onMounted, nextTick, watch } from 'vue'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()

const strategyList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")
const strategyCanvas = ref(null)
let strategyGraph = null

// 使用字典获取装备类型枚举
const { equipment_type: equipmentTypeDict } = useDict('equipment_type')

// 装备列表（从字典获取）
const equipmentList = ref([])

// 监听字典数据变化
watch(equipmentTypeDict, (newDict) => {
  if (newDict && newDict.length > 0) {
    equipmentList.value = newDict.map(item => ({
      id: item.value,
      name: item.label,
      equipmentType: parseInt(item.value)
    }))
  }
}, { immediate: true })

// 装备类型映射（用于图标和颜色）
const equipmentTypeMap = {
  0: { icon: '🛰️', color: '#409EFF', name: '光学卫星' },
  1: { icon: '📡', color: '#67C23A', name: 'SAR卫星' },
  2: { icon: '🛰️', color: '#E6A23C', name: '电子侦察卫星' },
  3: { icon: '🛰️', color: '#F56C6C', name: '海洋监视卫星' },
  4: { icon: '🛰️', color: '#909399', name: '导弹预警卫星' },
  5: { icon: '🛰️', color: '#9C27B0', name: '通信卫星' },
  6: { icon: '🛰️', color: '#1E3A8A', name: '导航卫星' },
  7: { icon: '🛰️', color: '#FF9800', name: '气象卫星' },
  8: { icon: '🛰️', color: '#4CAF50', name: '测地卫星' },
  9: { icon: '🏠', color: '#795548', name: '地面站' },
  10: { icon: '🚢', color: '#607D8B', name: '舰船' },
  11: { icon: '✈️', color: '#FF5722', name: '飞机' },
  12: { icon: '🚗', color: '#00BCD4', name: '车辆' },
  13: { icon: '🔫', color: '#9E9E9E', name: '单兵装备' },
  14: { icon: '🏢', color: '#3F51B5', name: '指挥中心' },
  15: { icon: '📊', color: '#FFC107', name: '数据中心' },
  16: { icon: '🔌', color: '#8BC34A', name: '通信设备' },
  17: { icon: '📡', color: '#CDDC39', name: '雷达设备' },
  18: { icon: '📷', color: '#FFEB3B', name: '光学设备' },
  19: { icon: '🔍', color: '#FF9800', name: '红外设备' },
  20: { icon: '📶', color: '#FF5722', name: '电子战设备' },
  default: { icon: '🔧', color: '#9E9E9E', name: '其他装备' }
}

// 作战元活动映射（改为圆形形状）
const combatActivities = {
  reconnaissance: { name: '侦察', value: 0, color: '#FF6B6B' },
  perception: { name: '感知', value: 1, color: '#4ECDC4' },
  strike: { name: '打击', value: 2, color: '#45B7D1' },
  interference: { name: '干扰', value: 3, color: '#96CEB4' },
  measurement: { name: '测控', value: 4, color: '#FECA57' },
  transmission: { name: '传输', value: 5, color: '#FF9FF3' },
  relay: { name: '中继', value: 6, color: '#54A0FF' },
  launch: { name: '发射', value: 7, color: '#5F27CD' }
}

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'id', label: 'ID', width: 120, showOverflowTooltip: true },
  { key: 'name', label: '装备协同组合运用策略名称', width: 220, showOverflowTooltip: true },
  { 
    key: 'equipmentLevel', 
    label: '装备等级', 
    width: 100,
    formatter: (row) => row.equipmentLevel !== undefined ? row.equipmentLevel : ''
  },
  { 
    key: 'tacticDetails', 
    label: '战术详情', 
    width: 200,
    showOverflowTooltip: true,
    formatter: (row) => {
      if (row.tacticDetails && Array.isArray(row.tacticDetails)) {
        return row.tacticDetails.map(detail => 
          `装备编组: ${detail.equipmentFormation?.join(', ') || '无'}, 元动作: ${detail.metaAction || '无'}`
        ).join('; ')
      }
      return '无'
    }
  },
  { key: 'createTime', label: '创建时间', width: 120 },
  {
    key: 'operation',
    label: '操作',
    width: 160,
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
    equipmentLevel: undefined
  },
  rules: {
    name: [{ required: true, message: "装备协同组合运用策略名称不能为空", trigger: "blur" }],
    equipmentLevel: [{ required: true, message: "装备等级不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 获取装备图标
function getEquipmentIcon(equipmentType) {
  return equipmentTypeMap[equipmentType]?.icon || equipmentTypeMap.default.icon
}

// 获取装备颜色
function getEquipmentColor(equipmentType) {
  return equipmentTypeMap[equipmentType]?.color || equipmentTypeMap.default.color
}

// 初始化装备列表（从字典获取）
const initEquipmentList = () => {
  if (equipmentTypeDict && equipmentTypeDict.length > 0) {
    equipmentList.value = equipmentTypeDict.map(item => ({
      id: item.value,
      name: item.label,
      equipmentType: parseInt(item.value)
    }))
  }
}

/** 查询列表 */
function getList() {
  loading.value = true
  listEquipmentSynergyTactics(queryParams.value).then(response => {
    strategyList.value = response.rows
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
function handleSelectionChange(selection) {}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增装备协同组合运用策略"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  // 直接使用行数据，避免不必要的接口调用
  form.value = { ...row }
  open.value = true
  title.value = "修改装备协同组合运用策略"
  console.log("修改装备协同组合运用策略",form.value)
  
  // 在对话框完全打开后加载画布数据
  nextTick(() => {
    setTimeout(() => {
      if (form.value.graphJSONEquipmentSynergy && strategyGraph) {
        try {
          const graphData = JSON.parse(form.value.graphJSONEquipmentSynergy)
          strategyGraph.fromJSON(graphData)
          console.log('画布数据加载成功')
        } catch (error) {
          console.error('解析graphJSONEquipmentSynergy失败:', error)
        }
      }
    }, 100) // 延迟100ms确保画布初始化完成
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除装备协同组合运用策略编号为"' + row.id + '"的数据项？').then(function() {
    return deleteEquipmentSynergyTactics(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["strategyRef"].validate(valid => {
    if (valid) {
      // 从画布构建 tacticDetails
      const tacticDetails = buildTacticDetailsFromGraph()
      
      // 保存画布状态到graphJSONEquipmentSynergy
      const graphJSONEquipmentSynergy = strategyGraph ? JSON.stringify(strategyGraph.toJSON()) : ''
      
      const submitData = {
        name: form.value.name,
        equipmentLevel: form.value.equipmentLevel,
        tacticDetails: tacticDetails,
        graphJSONEquipmentSynergy: graphJSONEquipmentSynergy
      }
      
      if (form.value.id != null) {
        submitData.id = form.value.id
        updateEquipmentSynergyTactics(submitData).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        insertEquipmentSynergyTactics(submitData).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
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
    name: null,
    equipmentLevel: null,
    tacticDetails: [],
    graphJSONEquipmentSynergy: '' // 新增字段
  }
  if (strategyGraph) {
    strategyGraph.clearCells()
  }
  proxy.resetForm("strategyRef")
}

/** 初始化图形画布 */
function initStrategyGraph() {
  if (strategyGraph) {
    strategyGraph.dispose()
  }
  
  strategyGraph = new Graph({
    container: strategyCanvas.value,
    grid: true,
    mousewheel: {
      enabled: true,
      zoomAtMousePosition: true,
      modifiers: 'ctrl',
      minScale: 0.5,
      maxScale: 3,
    },
    connecting: {
      router: {
        name: 'manhattan',
        args: { padding: 10 },
      },
      connector: {
        name: 'rounded',
        args: { radius: 8 },
      },
      anchor: 'center',
      connectionPoint: 'anchor',
      allowBlank: false,
      snap: { radius: 20 },
      createEdge() {
        return new Shape.Edge({
          attrs: {
            line: {
              stroke: '#5F95FF',
              strokeWidth: 3,
              targetMarker: { name: 'block', size: 12 },
            },
          },
          zIndex: 0,
        })
      },
      validateConnection({ sourceMagnet, targetMagnet, sourceView, targetView }) {
        // 获取源节点和目标节点的类型
        const sourceNode = sourceView?.cell
        const targetNode = targetView?.cell
        if (!sourceNode || !targetNode) return false
        
        const sourceType = sourceNode.getData()?.type
        const targetType = targetNode.getData()?.type
        
        // 允许：装备 → 元活动，元活动 → 元活动
        if (sourceType === 'equipment' && targetType === 'activity') return true
        if (sourceType === 'activity' && targetType === 'activity') return true
        
        return false
      },
    },
    highlighting: {
      magnetAdsorbed: {
        name: 'stroke',
        args: { attrs: { fill: '#5F95FF', stroke: '#5F95FF' } },
      },
    },
    resizing: true,
    rotating: true,
    selecting: {
      enabled: true,
      rubberband: true,
      showNodeSelectionBox: true,
    },
    snapline: true,
    keyboard: true,
    clipboard: true,
  })

  // 添加包围框工具
  strategyGraph.on('node:mouseenter', ({ node }) => {
    node.addTools({
      name: 'boundary',
      args: {
        padding: 8,
        attrs: {
          fill: '#7c68fc',
          stroke: '#9254de',
          strokeWidth: 2,
          fillOpacity: 0.2,
        },
      },
    })
  })

  strategyGraph.on('node:mouseleave', ({ node }) => {
    node.removeTools()
  })
}

/** 拖拽开始事件 */
function onDragStart(event) {
  const type = event.target.getAttribute('data-type')
  if (type === 'equipment') {
    const id = event.target.getAttribute('data-id')
    const name = event.target.getAttribute('data-name')
    const equipmentType = parseInt(event.target.getAttribute('data-equipment-type'))
    const data = {
      type: 'equipment',
      id: id,
      name: name,
      equipmentType: equipmentType
    }
    event.dataTransfer.setData('text/plain', JSON.stringify(data))
  } else if (type === 'activity') {
    const activityKey = event.target.getAttribute('data-activity-key')
    const activityName = event.target.getAttribute('data-activity-name')
    const activityValue = parseInt(event.target.getAttribute('data-activity-value'))
    const data = {
      type: 'activity',
      key: activityKey,
      name: activityName,
      value: activityValue
    }
    event.dataTransfer.setData('text/plain', JSON.stringify(data))
  }
  event.dataTransfer.effectAllowed = 'copy'
}

/** 画布拖放事件 */
function onCanvasDrop(event) {
  event.preventDefault()
  const rawData = event.dataTransfer.getData('text/plain')
  if (!rawData) return
  const data = JSON.parse(rawData)
  const rect = strategyCanvas.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  addComponentToCanvas(data, x, y)
}

/** 画布拖拽悬停事件 */
function onCanvasDragOver(event) {
  event.preventDefault()
  event.dataTransfer.dropEffect = 'copy'
}

/** 添加组件到画布 */
function addComponentToCanvas(data, x, y) {
  if (data.type === 'equipment') {
    // 装备节点：圆形，带图标和名称
    const color = getEquipmentColor(data.equipmentType)
    const icon = getEquipmentIcon(data.equipmentType)
    strategyGraph.addNode({
      id: `equip_${data.id}`,
      shape: 'circle',
      x: x - 30,
      y: y - 30,
      width: 60,
      height: 60,
      data: { type: 'equipment', id: data.id, name: data.name, equipmentType: data.equipmentType },
      attrs: {
        body: {
          fill: color,
          stroke: '#fff',
          strokeWidth: 2,
        },
        label: {
          text: `${icon}\n${data.name}`,
          fontSize: 10,
          fill: '#fff',
          fontWeight: 'bold',
          textAnchor: 'middle',
          textVerticalAnchor: 'middle',
        },
      },
      ports: {
        groups: {
          top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
          right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
          bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
          left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
        },
        items: [{ group: 'top' }, { group: 'right' }, { group: 'bottom' }, { group: 'left' }],
      },
    })
  } else if (data.type === 'activity') {
    // 元活动节点：圆形，带名称和背景色
    const activity = combatActivities[data.key]
    strategyGraph.addNode({
      id: `act_${Date.now()}_${Math.random()}`,
      shape: 'circle',
      x: x - 40,
      y: y - 40,
      width: 80,
      height: 80,
      data: { type: 'activity', key: data.key, name: data.name, value: data.value },
      attrs: {
        body: {
          fill: activity.color,
          stroke: '#fff',
          strokeWidth: 2,
        },
        label: {
          text: data.name,
          fontSize: 12,
          fill: '#fff',
          fontWeight: 'bold',
          textAnchor: 'middle',
          textVerticalAnchor: 'middle',
        },
      },
      ports: {
        groups: {
          top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
          right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
          bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
          left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#fff', fill: '#fff' } } },
        },
        items: [{ group: 'top' }, { group: 'right' }, { group: 'bottom' }, { group: 'left' }],
      },
    })
  }
}

/** 从画布构建 tacticDetails */
function buildTacticDetailsFromGraph() {
  if (!strategyGraph) return []
  
  const nodes = strategyGraph.getNodes()
  const edges = strategyGraph.getEdges()
  
  // 获取所有元活动节点
  const activityNodes = nodes.filter(node => node.getData()?.type === 'activity')
  const result = []
  
  for (const actNode of activityNodes) {
    const actData = actNode.getData()
    const metaAction = actData.value // 元活动类型值
    
    // 找出所有指向该元活动的边（来源节点）
    const incomingEdges = edges.filter(edge => edge.getTargetCellId() === actNode.id)
    const equipmentIds = []
    for (const edge of incomingEdges) {
      const sourceNode = strategyGraph.getCellById(edge.getSourceCellId())
      if (sourceNode && sourceNode.getData()?.type === 'equipment') {
        equipmentIds.push(sourceNode.getData().id)
      }
    }
    
    // 去重并保留顺序
    const uniqueEquipmentIds = [...new Set(equipmentIds)]
    if (uniqueEquipmentIds.length > 0) {
      result.push({
        equipmentFormation: uniqueEquipmentIds,
        metaAction: metaAction
      })
    }
  }
  
  return result
}

/** 对话框打开时初始化画布并加载装备列表 */
watch(open, async (newVal) => {
  if (newVal) {
    initEquipmentList() // 从字典初始化装备列表
    nextTick(() => {
      initStrategyGraph()
      strategyCanvas.value.addEventListener('dragover', onCanvasDragOver)
      strategyCanvas.value.addEventListener('drop', onCanvasDrop)
    })
  } else {
    if (strategyCanvas.value) {
      strategyCanvas.value.removeEventListener('dragover', onCanvasDragOver)
      strategyCanvas.value.removeEventListener('drop', onCanvasDrop)
    }
    if (strategyGraph) {
      strategyGraph.dispose()
      strategyGraph = null
    }
  }
})

/** 初始化 **/
getList()
</script>

<style scoped>
/* 原有样式保持不变，仅添加或修改必要部分 */
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
:deep(.el-table) {
  margin-top: 20px;
}
:deep(.el-table .cell) {
  text-align: center;
}
.graphical-configuration {
  margin-top: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 20px;
  background: #f8f9fa;
}
.graphical-configuration h4 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}
.configuration-container {
  display: flex;
  height: 500px;
  gap: 20px;
}
.left-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
  max-height: 100%;
  padding-right: 5px;
}

.component-section {
  background: white;
  border-radius: 4px;
  padding: 15px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.component-section h5 {
  margin: 0 0 15px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}
.equipment-components {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.equipment-component {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: grab;
  background: white;
  transition: all 0.3s;
}
.equipment-component:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}
.component-icon {
  font-size: 24px;
  margin-bottom: 5px;
}
.equipment-component span {
  font-size: 12px;
  color: #606266;
}
.combat-components {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.combat-component {
  cursor: grab;
  padding: 5px;
}
.component-shape {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%; /* 改为圆形 */
  color: white;
  font-size: 12px;
  font-weight: bold;
  transition: all 0.3s;
}
.component-shape:hover {
  transform: scale(1.05);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
}
.right-panel {
  flex: 1;
  background: white;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}
.canvas-container {
  width: 100%;
  height: 100%;
  position: relative;
}
.strategy-canvas {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
:deep(.strategy-dialog .el-dialog__body) {
  padding: 20px;
}
:deep(.strategy-dialog .el-form-item__label) {
  font-weight: 600;
}
:deep(.strategy-dialog .el-row) {
  margin-bottom: 20px;
}
:deep(.x6-widget-transform) {
  border: 2px solid #409EFF;
}
:deep(.x6-edge path) {
  stroke-width: 3px;
}
:deep(.x6-edge .x6-edge-target-marker) {
  stroke-width: 0;
  fill: #5F95FF;
}
:deep(.x6-node-selected) {
  stroke: #409EFF;
  stroke-width: 3px;
}
.equipment-component:active,
.combat-component:active {
  cursor: grabbing;
  opacity: 0.7;
}
@media (max-width: 1400px) {
  .configuration-container {
    height: 400px;
  }
  .left-panel {
    width: 250px;
  }
}
</style>