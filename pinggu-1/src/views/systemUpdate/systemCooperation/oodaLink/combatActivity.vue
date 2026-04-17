<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="120px">
      <el-form-item label="作战活动名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入作战活动名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="作战活动类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择作战活动类型" clearable style="width: 200px">
          <el-option
            v-for="item in activityTypeOptions"
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

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="activityList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无作战活动数据"
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

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="1200px" append-to-body destroy-on-close>
      <el-form ref="activityRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作战活动名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入作战活动名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作战活动类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择作战活动类型" style="width: 100%" @change="onTypeChange">
                <el-option
                  v-for="item in activityTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="活动时间范围" prop="activityTimeRange">
              <el-date-picker
                v-model="form.activityTimeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="x"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联任务判据" prop="taskCriterionId">
              <el-select v-model="form.taskCriterionId" placeholder="请选择任务判据" style="width: 100%" filterable>
                <el-option
                  v-for="item in taskCriteriaOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="装备协同策略" prop="equipmentSynergyTacticid">
              <el-select v-model="form.equipmentSynergyTacticid" placeholder="请选择装备协同策略" style="width: 100%" filterable>
                <el-option
                  v-for="item in equipmentSynergyTacticOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 地面目标编辑器（仅 type == 0 显示） -->
        <div v-if="form.type == 0">
          <el-divider content-position="left">地面目标</el-divider>
          <el-form-item label="地面目标绘制">
            <div class="cesium-container">
              <SimpleCesiumViewer ref="cesiumViewer" @ready="onCesiumReady" />
            </div>
            <div class="ground-targets-toolbar">
              <el-button type="primary" size="small" @click="addPointTarget">添加点目标</el-button>
              <el-button type="success" size="small" @click="addAreaTarget">添加区域目标</el-button>
              <el-button type="danger" size="small" @click="clearAllTargets">清空所有目标</el-button>
            </div>
          </el-form-item>
          <el-form-item label="已添加目标">
            <el-table :data="groundTargetsList" border size="small">
              <el-table-column label="类型" prop="targetTypeLabel" width="100" />
              <el-table-column label="坐标信息" prop="coordinateSummary" />
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeGroundTarget($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-form-item>
        </div>

        <!-- 频率范围编辑器（type == 0 或 type == 4 显示） -->
        <div v-if="form.type == 0 || form.type == 4">
          <el-divider content-position="left">频率探测范围</el-divider>
          <el-form-item label="频率范围列表">
            <el-button type="primary" size="small" @click="addFrequencyRange">添加频率范围</el-button>
          </el-form-item>
          <el-form-item v-for="(freq, idx) in frequencyList" :key="idx" :label="`频率范围 ${idx+1}`">
            <el-row :gutter="10">
              <el-col :span="10">
                <el-input-number v-model="freq.frequencyMin" :precision="1" placeholder="最小频率 (MHz)" style="width: 100%" />
              </el-col>
              <el-col :span="10">
                <el-input-number v-model="freq.frequencyMax" :precision="1" placeholder="最大频率 (MHz)" style="width: 100%" />
              </el-col>
              <el-col :span="4">
                <el-button type="danger" size="small" @click="removeFrequencyRange(idx)">删除</el-button>
              </el-col>
            </el-row>
          </el-form-item>
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

<script setup name="CombatActivity">
import { ref, reactive, toRefs, getCurrentInstance, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listEquipmentActivity, getEquipmentActivity, deleteEquipmentActivity, insertEquipmentActivity, updateEquipmentActivity } from '@/api/systemPlus/systemCooperation/act'
import { listEquipmentSynergyTactics } from '@/api/systemPlus/systemCooperation/equipment'
import DynamicTable from '@/components/DynamicTable/index.vue'
import SimpleCesiumViewer from '@/components/SimpleCesiumViewer.vue'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()

// 列表数据
const activityList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")
const cesiumViewer = ref(null)

// 字典选项
const { subsystem_type: activityTypeOptions } = useDict('subsystem_type')

// 任务判据选项（实际应从接口获取）
const taskCriteriaOptions = ref([
  { value: 'TASK_CRITERION_001', label: '目标识别判据' },
  { value: 'TASK_CRITERION_002', label: '威胁评估判据' },
  { value: 'TASK_CRITERION_003', label: '作战效能判据' },
  { value: 'TASK_CRITERION_004', label: '装备状态判据' },
  { value: 'TASK_CRITERION_005', label: '环境条件判据' },
  { value: 'TASK_CRITERION_006', label: '协同作战判据' }
])

// 装备协同策略选项
const equipmentSynergyTacticOptions = ref([])

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'id', label: 'ID', width: 120, showOverflowTooltip: true },
  { key: 'name', label: '作战活动名称', width: 180, showOverflowTooltip: true },
  { 
    key: 'type', 
    label: '作战活动类型', 
    width: 150,
    formatter: (row) => {
      if (!activityTypeOptions.value || !Array.isArray(activityTypeOptions.value)) {
        return row.type !== undefined ? row.type : ''
      }
      const type = activityTypeOptions.value.find(item => item.value == row.type)
      return type ? type.label : (row.type !== undefined ? row.type : '')
    }
  },
  { key: 'activityStartTime', label: '活动开始时间', width: 180, formatter: (val) => val ? new Date(val).toLocaleString() : '-' },
  { key: 'activityEndTime', label: '活动结束时间', width: 180, formatter: (val) => val ? new Date(val).toLocaleString() : '-' },
  { key: 'activityTarget', label: '作战活动目标', width: 150, showOverflowTooltip: true },
  { 
    key: 'taskCriterionId', 
    label: '关联任务判据', 
    width: 150,
    formatter: (row) => row && row.taskCriterionId ? row.taskCriterionId : '无'
  },
  { 
    key: 'equipmentSynergyTacticid', 
    label: '装备协同策略', 
    width: 150,
    formatter: (row) => row && row.equipmentSynergyTacticid ? row.equipmentSynergyTacticid : '无'
  },
  { key: 'createTime', label: '创建时间', width: 180, formatter: (val) => val ? new Date(val).toLocaleString() : '-' },
  {
    key: 'operation',
    label: '操作',
    width: 160,
    customColumn: true,
    slotName: 'operationSlot'
  }
])

// 表单数据
const data = reactive({
  form: {
    id: null,
    name: '',
    type: null,
    activityTimeRange: [],
    activityTarget: null,
    taskCriterionId: '',
    equipmentSynergyTacticid: ''
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined
  },
  rules: {
    name: [{ required: true, message: "作战活动名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "作战活动类型不能为空", trigger: "change" }],
    activityTimeRange: [{ required: false, message: "请选择活动时间段", trigger: "change" }],
    taskCriterionId: [{ required: false, message: "请选择关联任务判据", trigger: "change" }],
    equipmentSynergyTacticid: [{ required: false, message: "请选择装备协同策略", trigger: "change" }]
  }
})
const { queryParams, form, rules } = toRefs(data)

// 地面目标列表（前端展示用）
const groundTargetsList = ref([])
// 频率范围列表
const frequencyList = ref([])

/** 加载装备协同策略列表 */
function loadEquipmentSynergyTactics() {
  listEquipmentSynergyTactics({ pageNum: 1, pageSize: 100 }).then(response => {
    if (response.code === 200 && response.rows ) {
      equipmentSynergyTacticOptions.value = response.rows.map(item => ({
        value: item.id ,
        label: item.name || '未命名策略'
      }))
      console.log('装备协同策略列表加载成功:', equipmentSynergyTacticOptions.value)
    } else {
      console.warn('装备协同策略列表加载失败或为空')
      equipmentSynergyTacticOptions.value = []
    }
  }).catch(error => {
    console.error('加载装备协同策略列表失败:', error)
    equipmentSynergyTacticOptions.value = []
  })
}

/** 查询列表 */
function getList() {
  loading.value = true
  listEquipmentActivity(queryParams.value).then(response => {
    activityList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 搜索按钮 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置查询 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选 */
function handleSelectionChange(selection) {
  // 可处理批量操作
}

/** 新增 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增作战活动"
}

/** 编辑 */
function handleUpdate(row) {
  reset()
  getEquipmentActivity(row.id).then(response => {
    const data = response.data
    
    // 将开始时间和结束时间合并为时间段
    const activityTimeRange = []
    if (data.activityStartTime) {
      activityTimeRange.push(data.activityStartTime)
    }
    if (data.activityEndTime) {
      activityTimeRange.push(data.activityEndTime)
    }
    
    form.value = {
      id: data.id,
      name: data.name,
      type: data.type,
      activityTimeRange: activityTimeRange,
      activityTarget: data.activityTarget,
      taskCriterionId: data.taskCriterionId,
      equipmentSynergyTacticid: data.equipmentSynergyTacticid || ''
    }
    
    // 解析 activityTarget
    const target = data.activityTarget
    console.log("target======",target)
    if (target) {
      // 解析地面目标
      if (target.groundTargets && Array.isArray(target.groundTargets)) {
        groundTargetsList.value = target.groundTargets.map(gt => {
          // 确保坐标格式正确
          let coordinate = gt.coordinate
          if (gt.targetType === 0 && typeof coordinate === 'object' && !Array.isArray(coordinate)) {
            // 点目标：单个坐标对象
            coordinate = {
              longitude: coordinate.longitude || 0,
              latitude: coordinate.latitude || 0,
              altitude: coordinate.altitude || 0
            }
          } else if (gt.targetType === 1 && Array.isArray(coordinate)) {
            // 区域目标：坐标数组
            coordinate = coordinate.map(c => ({
              longitude: c.longitude || 0,
              latitude: c.latitude || 0,
              altitude: c.altitude || 0
            }))
          }
          
          return {
            targetType: gt.targetType,
            coordinate: coordinate,
            targetTypeLabel: gt.targetType === 0 ? '点目标' : '区域目标',
            coordinateSummary: formatCoordinateSummary({ targetType: gt.targetType, coordinate: coordinate })
          }
        })
        // 等待地图组件渲染完成后重绘
        nextTick(() => redrawAllGroundTargets())
      }
      
      // 解析频率范围
      if (target.frequencyDetected && Array.isArray(target.frequencyDetected)) {
        frequencyList.value = target.frequencyDetected.map(f => ({
          frequencyMin: f.frequencyMin || null,
          frequencyMax: f.frequencyMax || null
        }))
      }
    }
    open.value = true
    title.value = "修改作战活动"
  })
}

/** 删除 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除作战活动编号为"' + row.id + '"的数据项？').then(() => {
    return deleteEquipmentActivity(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 提交表单 */
function submitForm() {
  proxy.$refs["activityRef"].validate(valid => {
    if (valid) {
      const submitData = { ...form.value }
      
      // 将时间段拆分为开始时间和结束时间
      if (Array.isArray(submitData.activityTimeRange) && submitData.activityTimeRange.length === 2) {
        submitData.activityStartTime = submitData.activityTimeRange[0]
        submitData.activityEndTime = submitData.activityTimeRange[1]
      } else {
        submitData.activityStartTime = null
        submitData.activityEndTime = null
      }
      
      // 移除时间段字段，只保留开始和结束时间
      delete submitData.activityTimeRange
      
      // 构建 activityTarget
      submitData.activityTarget = buildActivityTarget()
      
      // 调试信息：查看提交的数据
      console.log('提交数据:', JSON.stringify(submitData, null, 2))
      console.log('地面目标列表:', groundTargetsList.value)
      console.log('频率范围列表:', frequencyList.value)
      console.log('构建的activityTarget:', submitData.activityTarget)
      
      const action = form.value.id != null ? updateEquipmentActivity : insertEquipmentActivity
      action(submitData).then(() => {
        proxy.$modal.msgSuccess(form.value.id != null ? "修改成功" : "新增成功")
        open.value = false
        getList()
      }).catch((error) => {
        console.error('操作失败:', error)
        proxy.$modal.msgError("操作失败")
      })
    }
  })
}

/** 取消 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    name: '',
    type: null,
    activityStartTime: null,
    activityEndTime: null,
    activityTarget: null,
    taskCriterionId: '',
    equipmentSynergyTacticid: ''
  }
  groundTargetsList.value = []
  frequencyList.value = []
  if (cesiumViewer.value) {
    cesiumViewer.value.clear && cesiumViewer.value.clear()
  }
  proxy.resetForm("activityRef")
}

/** 类型变更时清空相关数据 */
function onTypeChange() {
  groundTargetsList.value = []
  frequencyList.value = []
  if (cesiumViewer.value) {
    cesiumViewer.value.clear && cesiumViewer.value.clear()
  }
}

/** Cesium 组件准备就绪 */
function onCesiumReady() {
  // 可以在此加载已有目标
  redrawAllGroundTargets()
}

/** 添加点目标（通过弹窗输入坐标） */
function addPointTarget() {
  ElMessageBox.prompt('请输入点目标坐标（经度,纬度,高度）', '添加点目标', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^-?\d+(\.\d+)?,\s*-?\d+(\.\d+)?,\s*-?\d+(\.\d+)?$/,
    inputErrorMessage: '格式错误，示例：116.4,39.9,500'
  }).then(({ value }) => {
    const [longitude, latitude, altitude] = value.split(',').map(Number)
    const pointTarget = {
      targetType: 0,
      coordinate: { 
        longitude: parseFloat(longitude.toFixed(1)), 
        latitude: parseFloat(latitude.toFixed(1)), 
        altitude: parseFloat(altitude.toFixed(1)) 
      },
      targetTypeLabel: '点目标',
      coordinateSummary: `经度 ${longitude.toFixed(1)}, 纬度 ${latitude.toFixed(1)}, 高度 ${altitude.toFixed(1)}m`
    }
    groundTargetsList.value.push(pointTarget)
    if (cesiumViewer.value && cesiumViewer.value.addPoint) {
      cesiumViewer.value.addPoint(longitude, latitude, altitude)
    } else {
      console.warn('Cesium组件未实现 addPoint 方法')
    }
  }).catch(() => {})
}

/** 添加区域目标（通过 JSON 输入点集） */
function addAreaTarget() {
  // 提供正确的JSON示例格式
  const jsonExample = `[
  {
    "longitude": 116.4,
    "latitude": 39.9,
    "altitude": 500.0
  },
  {
    "longitude": 121.5,
    "latitude": 31.2,
    "altitude": 300.0
  },
  {
    "longitude": 114.3,
    "latitude": 30.6,
    "altitude": 200.0
  }
]`
  
  ElMessageBox.prompt(`请输入区域目标点集（JSON数组格式，至少需要3个点）\n\n示例格式：\n${jsonExample}`, '添加区域目标', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '请粘贴或输入JSON格式的点集数据',
    inputValue: jsonExample,
    inputValidator: (val) => {
      if (!val || val.trim() === '') {
        return '请输入JSON数据'
      }
      
      try {
        const points = JSON.parse(val)
        
        // 验证是否为数组
        if (!Array.isArray(points)) {
          return '必须是JSON数组格式，请检查格式是否正确'
        }
        
        // 验证点数
        if (points.length < 3) {
          return `区域目标至少需要3个点，当前只有${points.length}个点`
        }
        
        // 验证每个点的格式
        for (let i = 0; i < points.length; i++) {
          const p = points[i]
          if (p.longitude === undefined || p.latitude === undefined) {
            return `第${i+1}个点缺少 longitude 或 latitude 字段`
          }
          
          // 验证数值类型
          if (typeof p.longitude !== 'number' || typeof p.latitude !== 'number') {
            return `第${i+1}个点的经度或纬度必须是数字类型`
          }
          
          // 验证数值范围
          if (p.longitude < -180 || p.longitude > 180) {
            return `第${i+1}个点的经度必须在-180到180之间`
          }
          if (p.latitude < -90 || p.latitude > 90) {
            return `第${i+1}个点的纬度必须在-90到90之间`
          }
        }
        
        return true
      } catch (error) {
        return `JSON格式错误：${error.message}`
      }
    }
  }).then(({ value }) => {
    const points = JSON.parse(value)
    const coordinate = points.map(p => ({
      longitude: parseFloat(p.longitude.toFixed(1)),
      latitude: parseFloat(p.latitude.toFixed(1)),
      altitude: parseFloat((p.altitude || 0).toFixed(1))
    }))
    const areaTarget = {
      targetType: 1,
      coordinate: coordinate,
      targetTypeLabel: '区域目标',
      coordinateSummary: `共 ${coordinate.length} 个点`
    }
    groundTargetsList.value.push(areaTarget)
    if (cesiumViewer.value && cesiumViewer.value.addPolygon) {
      cesiumViewer.value.addPolygon(coordinate)
    } else {
      console.warn('Cesium组件未实现 addPolygon 方法')
    }
  }).catch(() => {})
}

/** 移除地面目标 */
function removeGroundTarget(index) {
  groundTargetsList.value.splice(index, 1)
  redrawAllGroundTargets()
}

/** 清空所有地面目标 */
function clearAllTargets() {
  groundTargetsList.value = []
  if (cesiumViewer.value) {
    cesiumViewer.value.clear && cesiumViewer.value.clear()
  }
}

/** 重绘所有地面目标 */
function redrawAllGroundTargets() {
  if (!cesiumViewer.value) return
  cesiumViewer.value.clear && cesiumViewer.value.clear()
  groundTargetsList.value.forEach(target => {
    if (target.targetType === 0) {
      const c = target.coordinate
      cesiumViewer.value.addPoint && cesiumViewer.value.addPoint(c.longitude, c.latitude, c.altitude)
    } else if (target.targetType === 1) {
      cesiumViewer.value.addPolygon && cesiumViewer.value.addPolygon(target.coordinate)
    }
  })
}

/** 格式化坐标信息用于表格展示 */
function formatCoordinateSummary(target) {
  if (target.targetType === 0) {
    const c = target.coordinate
    return `经度 ${c.longitude}, 纬度 ${c.latitude}, 高度 ${c.altitude}m`
  } else {
    return `区域目标，共 ${target.coordinate.length} 个点`
  }
}

/** 添加频率范围 */
function addFrequencyRange() {
  frequencyList.value.push({ frequencyMin: null, frequencyMax: null })
}

/** 删除频率范围 */
function removeFrequencyRange(index) {
  frequencyList.value.splice(index, 1)
}

/** 构建 activityTarget 对象 */
function buildActivityTarget() {
  const type = form.value.type
  if (type == null) {
    console.log('buildActivityTarget: type为null，返回null')
    return null
  }

  const result = {}
  console.log('buildActivityTarget: 开始构建，type=', type, '类型:', typeof type)
  
  // 使用宽松比较，处理字符串和数字类型
  if (type == 0) {
    // 地面目标
    console.log('buildActivityTarget: 处理地面目标，groundTargetsList长度=', groundTargetsList.value.length)
    if (groundTargetsList.value.length > 0) {
      result.groundTargets = groundTargetsList.value.map(gt => {
        // 确保坐标格式与JSON示例一致
        let coordinate = gt.coordinate
        if (gt.targetType === 0) {
          // 点目标：确保是单个对象，不是数组
          coordinate = {
            longitude: parseFloat(coordinate.longitude.toFixed(1)),
            latitude: parseFloat(coordinate.latitude.toFixed(1)),
            altitude: parseFloat(coordinate.altitude.toFixed(1))
          }
        } else if (gt.targetType === 1) {
          // 区域目标：确保是数组，每个点都有正确的格式
          coordinate = coordinate.map(c => ({
            longitude: parseFloat(c.longitude.toFixed(1)),
            latitude: parseFloat(c.latitude.toFixed(1)),
            altitude: parseFloat(c.altitude.toFixed(1))
          }))
        }
        
        return {
          targetType: gt.targetType,
          coordinate: coordinate
        }
      })
      console.log('buildActivityTarget: 地面目标构建完成', result.groundTargets)
    }
    // 频率范围（过滤有效值）
    const validFreqs = frequencyList.value.filter(f => f.frequencyMin != null && f.frequencyMax != null)
    console.log('buildActivityTarget: 频率范围过滤后长度=', validFreqs.length)
    if (validFreqs.length > 0) {
      result.frequencyDetected = validFreqs.map(f => ({
        frequencyMin: parseFloat(f.frequencyMin.toFixed(1)),
        frequencyMax: parseFloat(f.frequencyMax.toFixed(1))
      }))
      console.log('buildActivityTarget: 频率范围构建完成', result.frequencyDetected)
    }
  } else if (type == 4) {
    const validFreqs = frequencyList.value.filter(f => f.frequencyMin != null && f.frequencyMax != null)
    if (validFreqs.length > 0) {
      result.frequencyDetected = validFreqs.map(f => ({
        frequencyMin: parseFloat(f.frequencyMin.toFixed(1)),
        frequencyMax: parseFloat(f.frequencyMax.toFixed(1))
      }))
    }
  } else {
    // 其他类型不包含任何目标信息
    console.log('buildActivityTarget: 其他类型，返回null')
    return null
  }
  
  // 只要有地面目标或频率范围，就返回result，即使只有一个字段
  if (result.groundTargets || result.frequencyDetected) {
    console.log('buildActivityTarget: 返回result', result)
    return result
  }
  
  console.log('buildActivityTarget: 没有地面目标或频率范围，返回null')
  return null
}

// 初始化加载
getList()
loadEquipmentSynergyTactics()
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

:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  text-align: center;
}

.cesium-container {
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

:deep(.simple-cesium-container) {
  height: 100%;
  min-height: 400px;
}

.ground-targets-toolbar {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-start;
}

:deep(.el-form-item__label) {
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-row) {
  margin-bottom: 20px;
}
</style>