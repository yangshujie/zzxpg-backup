<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="140px">
      <el-form-item label="体系作战剖面名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入体系作战剖面名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="体系作战剖面类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择体系作战剖面类型" clearable style="width: 200px">
          <el-option
            v-for="item in profileTypeOptions"
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
    <el-table v-loading="loading" :data="profileList" @selection-change="handleSelectionChange" style="width: 100%">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="体系作战剖面名称" align="center" prop="profileName" min-width="180" />
      <el-table-column label="作战剖面类型" align="center" prop="profileTypeId" min-width="180">
        <template #default="scope">
          <dict-tag :options="profileTypeOptions" :value="scope.row.profileTypeId" />
        </template>
      </el-table-column>
      <el-table-column label="体系作战任务" align="center" prop="operationTask" min-width="150" />
      <el-table-column label="创建时间" align="center" prop="createTime" min-width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="体系作战对象" align="center" prop="operationTarget" min-width="150" />
      <el-table-column label="体系作战烈度" align="center" prop="operationIntensity" min-width="120" />
      <el-table-column label="体系作战要求" align="center" prop="operationRequirement" min-width="180" />
      <el-table-column label="蓝方最大作战半径" align="center" prop="blueMaxRadius" min-width="120" />
      <el-table-column label="蓝方反应时间" align="center" prop="blueReactionTime" min-width="120" />
      <el-table-column label="蓝方持续作战时间" align="center" prop="blueSustainTime" min-width="120" />
      <el-table-column label="蓝方对抗策略" align="center" prop="blueStrategy" min-width="150" />
    
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup name="CombatProfile">
import { selectList, deleteWithSub, saveOrUpdateCombatProfile, updateProfile, profileDetail } from "@/api/systemPlus/systemCooperation/combatProfile"
import { ref, reactive, onMounted, getCurrentInstance, toRefs } from 'vue'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()

const profileList = ref([])
const loading = ref(true)
const open = ref(false)

const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

// 作战剖面类型选项
const profileTypeOptions = ref([
  { value: '1', label: '航天侦察装备子体系' },
  { value: '2', label: '太空态势感知装备子体系' },
  { value: '3', label: '太空攻防装备子体系' },
  { value: '4', label: '航天发射装备子体系' },
  { value: '5', label: '航天测运控装备子体系' },
  { value: '6', label: '航天装备联合作战体系' }
])

// 表单数据
const form = ref({
  id: undefined,
  profileName: undefined,
  profileTypeId: undefined,
  operationTask: undefined,
  operationTarget: undefined,
  operationIntensity: undefined,
  operationRequirement: undefined,
  blueMaxRadius: undefined,
  blueReactionTime: undefined,
  blueSustainTime: undefined,
  blueStrategy: undefined
})

// 表单验证规则
const rules = ref({
  profileName: [{ required: true, message: "体系作战剖面名称不能为空", trigger: "blur" }],
  profileTypeId: [{ required: true, message: "作战剖面类型不能为空", trigger: "change" }],
  operationTask: [{ required: true, message: "体系作战任务不能为空", trigger: "blur" }]
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined
  }
})

const { queryParams } = toRefs(data)

/** 查询作战剖面列表 */
function getList() {
  loading.value = true
  selectList(queryParams.value).then(response => {
    profileList.value = response.data
    console.log("剖面列表",response.data);
    
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

/** 分页操作 */
function handlePagination(paginationData) {
  // 更新分页参数
  queryParams.value.pageNum = paginationData.page
  queryParams.value.pageSize = paginationData.limit
  // 调用获取列表
  getList()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  router.push({
    path: '/major/system-cooperation/detail',
    query: { mode: 'add' }
  })
}

/** 修改按钮操作 */
function handleUpdate(row) {
  const id = row.id || ids.value[0]
  if (id) {
    router.push({
      path: '/major/system-cooperation/detail',
      query: { 
        mode: 'edit',
        id: id 
      }
    })
  }
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    profileName: undefined,
    profileTypeId: undefined,
    operationTask: undefined,
    operationTarget: undefined,
    operationIntensity: undefined,
    operationRequirement: undefined,
    blueMaxRadius: undefined,
    blueReactionTime: undefined,
    blueSustainTime: undefined,
    blueStrategy: undefined
  }
  proxy.resetForm("profileRef")
}

/** 删除按钮操作 */
function handleDelete(row) {
  const ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除体系作战剖面编号为"' + ids + '"的数据项？').then(function() {
    return deleteWithSub(ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

onMounted(() => {
  getList()
})

</script>

<style scoped>
.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 20px;
}

.el-table {
  margin-top: 20px;
}

.dialog-footer {
  text-align: center;
}

.dialog-footer .el-button {
  margin: 0 10px;
}
</style>