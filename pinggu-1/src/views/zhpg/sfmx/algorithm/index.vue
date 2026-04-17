<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="算法名称" prop="algorithmName">
        <el-input
          v-model="queryParams.algorithmName"
          placeholder="请输入算法名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="算法类型" prop="algorithmType">
        <el-select v-model="queryParams.algorithmType" placeholder="请选择算法类型" clearable style="width: 180px">
          <el-option
            v-for="item in algorithmTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="装备类型" prop="equipmentType">
        <el-select v-model="queryParams.equipmentType" placeholder="6类装备域" clearable style="width: 180px">
          <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮区域 -->
    <div class="toolbar-row mb8">
      <div class="toolbar-btns">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleBatchDelete">删除</el-button>
        <el-button type="warning" plain icon="Download" @click="handleExport">导出</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      class="template-table"
      :data="algorithmList"
      row-key="id"
      table-layout="fixed"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="算法名称" prop="algorithmName" min-width="180" show-overflow-tooltip />
      <el-table-column label="算法类型" prop="algorithmType" min-width="130" show-overflow-tooltip />
      <el-table-column label="装备类型" prop="equipmentType" min-width="130" show-overflow-tooltip>
        <template #default="scope">{{ getEquipmentTypeLabel(scope.row.equipmentType) }}</template>
      </el-table-column>
      <el-table-column label="算法描述" prop="algorithmDesc" min-width="200" show-overflow-tooltip />
      <el-table-column label="代码包路径" prop="algorithmCodeUrl" min-width="200" show-overflow-tooltip />
      <el-table-column label="发布状态" prop="publishStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.publishStatus === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ scope.row.publishStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="createBy" width="100" show-overflow-tooltip />
      <el-table-column label="更新时间" width="170" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime || scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" align="center" fixed="right">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
            <el-button
              link type="info" size="small" icon="View"
              :disabled="!scope.row.algorithmCodeUrl"
              @click="handlePreview(scope.row)"
            >预览</el-button>
            <el-button
              link type="primary" size="small" icon="Download"
              :disabled="!scope.row.algorithmCodeUrl"
              @click="handleDownload(scope.row)"
            >下载</el-button>
            <el-button
              v-if="scope.row.publishStatus !== 'PUBLISHED'"
              link type="success" size="small" icon="Promotion" @click="handlePublish(scope.row)"
            >发布</el-button>
            <el-button
              v-else
              link type="warning" size="small" icon="RefreshLeft" @click="handleUnpublish(scope.row)"
            >撤回</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/修改对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="900px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="算法名称" prop="algorithmName">
              <el-input v-model="form.algorithmName" placeholder="请输入算法名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="算法类型" prop="algorithmType">
              <el-select v-model="form.algorithmType" placeholder="请选择算法类型" style="width: 100%">
                <el-option
                  v-for="item in algorithmTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="装备类型" prop="equipmentType">
              <el-select v-model="form.equipmentType" placeholder="6类装备之一" style="width: 100%">
                <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基准数据">
              <el-radio-group v-model="form.baseFlag">
                <el-radio-button :label="1">是</el-radio-button>
                <el-radio-button :label="0">否</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="算法代码包">
              <div class="code-upload-row">
                <el-upload
                  accept=".zip"
                  :auto-upload="false"
                  :limit="1"
                  :on-change="onZipChange"
                  :on-remove="onZipRemove"
                >
                  <el-button type="primary" plain size="small" icon="Upload">选择 zip</el-button>
                </el-upload>
                <span class="code-upload-tip">上传后路径为 algs/分类目录/算法名称.zip；请避免压缩包内主程序与算法名称不一致</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="存储路径">
              <el-input v-model="form.algorithmCodeUrl" placeholder="上传后自动回填，也可手填 MinIO 相对路径" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="算法描述">
          <el-input v-model="form.algorithmDesc" type="textarea" :rows="3" placeholder="请输入算法描述" />
        </el-form-item>

        <!-- 输入参数配置 -->
        <el-divider content-position="left">输入参数配置</el-divider>
        <div class="param-section">
          <el-table :data="inputParams" border size="small" style="width: 100%">
            <el-table-column label="参数名称" min-width="120">
              <template #default="scope">
                <el-input v-model="scope.row.paramField" placeholder="参数名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="参数类型" width="130">
              <template #default="scope">
                <el-select v-model="scope.row.paramType" placeholder="类型" size="small">
                  <el-option label="字符串" value="string" />
                  <el-option label="数值" value="number" />
                  <el-option label="数组" value="array" />
                  <el-option label="字典" value="dictionary" />
                  <el-option label="时间" value="date" />
                  <el-option label="遥测" value="tel" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="默认值" min-width="120">
              <template #default="scope">
                <el-input-number
                  v-if="scope.row.paramType === 'number'"
                  v-model="scope.row.defaultValue"
                  size="small"
                  controls-position="right"
                  style="width: 100%"
                />
                <el-input v-else v-model="scope.row.defaultValue" placeholder="默认值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70" align="center">
              <template #default="scope">
                <el-checkbox v-model="scope.row.requiredFlag" :true-label="1" :false-label="0" />
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="160">
              <template #default="scope">
                <el-input v-model="scope.row.paramDesc" placeholder="参数描述" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="70" align="center">
              <template #default="scope">
                <el-button link type="danger" size="small" icon="Minus" @click="removeParam('INPUT', scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt8" type="primary" plain size="small" icon="Plus" @click="addParam('INPUT')">添加输入参数</el-button>
        </div>

        <!-- 配置参数 -->
        <el-divider content-position="left">配置参数</el-divider>
        <div class="param-section">
          <el-table :data="configParams" border size="small" style="width: 100%">
            <el-table-column label="参数名称" min-width="120">
              <template #default="scope">
                <el-input v-model="scope.row.paramField" placeholder="参数名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="参数类型" width="130">
              <template #default="scope">
                <el-select v-model="scope.row.paramType" placeholder="类型" size="small">
                  <el-option label="字符串" value="string" />
                  <el-option label="数值" value="number" />
                  <el-option label="数组" value="array" />
                  <el-option label="字典" value="dictionary" />
                  <el-option label="时间" value="date" />
                  <el-option label="遥测" value="tel" />
                  <el-option label="数据模板" value="template" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="默认值" min-width="120">
              <template #default="scope">
                <el-input-number
                  v-if="scope.row.paramType === 'number'"
                  v-model="scope.row.defaultValue"
                  size="small"
                  controls-position="right"
                  style="width: 100%"
                />
                <el-input v-else v-model="scope.row.defaultValue" placeholder="默认值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70" align="center">
              <template #default="scope">
                <el-checkbox v-model="scope.row.requiredFlag" :true-label="1" :false-label="0" />
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="160">
              <template #default="scope">
                <el-input v-model="scope.row.paramDesc" placeholder="参数描述" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="70" align="center">
              <template #default="scope">
                <el-button link type="danger" size="small" icon="Minus" @click="removeParam('CONFIG', scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt8" type="primary" plain size="small" icon="Plus" @click="addParam('CONFIG')">添加配置参数</el-button>
        </div>

        <!-- 输出参数配置 -->
        <el-divider content-position="left">输出参数配置</el-divider>
        <div class="param-section">
          <el-table :data="outputParams" border size="small" style="width: 100%">
            <el-table-column label="参数名称" min-width="120">
              <template #default="scope">
                <el-input v-model="scope.row.paramField" placeholder="参数名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="参数类型" width="130">
              <template #default="scope">
                <el-select v-model="scope.row.paramType" placeholder="类型" size="small">
                  <el-option label="字符串" value="string" />
                  <el-option label="数值" value="number" />
                  <el-option label="数组" value="array" />
                  <el-option label="字典" value="dictionary" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="200">
              <template #default="scope">
                <el-input v-model="scope.row.paramDesc" placeholder="参数描述" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="70" align="center">
              <template #default="scope">
                <el-button link type="danger" size="small" icon="Minus" @click="removeParam('OUTPUT', scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt8" type="primary" plain size="small" icon="Plus" @click="addParam('OUTPUT')">添加输出参数</el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="960px" append-to-body destroy-on-close class="algo-preview-dialog">
      <pre class="algo-preview-pre"><code class="hljs language-python" v-html="previewHighlightedHtml"></code></pre>
    </el-dialog>
  </div>
</template>

<script setup name="AlgorithmManagement">
import {
  listAlgorithm,
  getAlgorithm,
  addAlgorithm,
  updateAlgorithm,
  delAlgorithm,
  publishAlgorithm,
  unpublishAlgorithm,
  uploadAlgorithmCode,
  previewAlgorithmCode
} from "@/api/zhpg/algorithm";
import { ZHPG_EQUIPMENT_TYPE_OPTIONS, getZhpgEquipmentTypeLabel } from "@/constants/zhpgIndicatorSystem";
const { proxy } = getCurrentInstance();

function escapeHtml(text) {
  if (text == null) return "";
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

const previewHighlightedHtml = computed(() => {
  const code = previewCode.value;
  if (code == null || code === "") return "";
  return escapeHtml(code);
});

const getEquipmentTypeLabel = getZhpgEquipmentTypeLabel;

const algorithmList = ref([]);
const loading = ref(false);
const showSearch = ref(true);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);
const dialogVisible = ref(false);
const dialogTitle = ref("");

const algorithmTypeOptions = [
  { label: "预处理算法", value: "预处理算法" },
  { label: "指标量化算法", value: "指标量化算法" },
  { label: "属性值计算方法", value: "属性值计算方法" },
  { label: "权重分配", value: "权重分配" },
  { label: "聚合传导", value: "聚合传导" },
  { label: "方案评价", value: "方案评价" },
  { label: "其它算法", value: "其它算法" },
];

const equipmentTypeOptions = ZHPG_EQUIPMENT_TYPE_OPTIONS;

const pendingZipFile = ref(null);
const submitLoading = ref(false);
const previewVisible = ref(false);
const previewTitle = ref("");
const previewCode = ref("");

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  algorithmName: undefined,
  algorithmType: undefined,
  equipmentType: undefined,
  publishStatus: undefined,
});

const inputParams = ref([]);
const configParams = ref([]);
const outputParams = ref([]);

const initFormData = {
  id: undefined,
  algorithmName: "",
  algorithmType: "",
  equipmentType: "",
  algorithmDesc: "",
  algorithmCodeUrl: "",
  baseFlag: 0,
  publishStatus: "DRAFT",
  status: "ENABLED",
};
const form = ref({ ...initFormData });

const rules = {
  algorithmName: [{ required: true, message: "请输入算法名称", trigger: "blur" }],
  algorithmType: [{ required: true, message: "请选择算法类型", trigger: "change" }],
};

/** 查询列表 */
function getList() {
  loading.value = true;
  listAlgorithm(queryParams.value).then((res) => {
    algorithmList.value = res.rows;
    total.value = res.total;
    loading.value = false;
  });
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 多选 */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  multiple.value = !selection.length;
}

function onZipChange(uploadFile) {
  const raw = uploadFile.raw;
  if (!raw) return;
  const name = (raw.name || "").toLowerCase();
  if (!name.endsWith(".zip")) {
    proxy.$modal.msgWarning("请上传 .zip 格式的算法包");
    pendingZipFile.value = null;
    return;
  }
  pendingZipFile.value = raw;
}

function onZipRemove() {
  pendingZipFile.value = null;
}

/** 新增 */
function handleAdd() {
  reset();
  dialogTitle.value = "新增算法";
  dialogVisible.value = true;
}

/** 修改 */
function handleUpdate(row) {
  reset();
  const id = row.id || ids.value[0];
  getAlgorithm(id).then((res) => {
    const data = res.data;
    form.value = {
      id: data.id,
      algorithmName: data.algorithmName,
      algorithmType: data.algorithmType,
      equipmentType: data.equipmentType,
      algorithmDesc: data.algorithmDesc,
      algorithmCodeUrl: data.algorithmCodeUrl,
      baseFlag: data.baseFlag ?? 0,
      publishStatus: data.publishStatus,
      status: data.status,
    };
    pendingZipFile.value = null;
    // 按类别拆分参数
    const params = data.params || [];
    inputParams.value = params.filter((p) => p.paramCategory === "INPUT");
    configParams.value = params.filter((p) => p.paramCategory === "CONFIG");
    outputParams.value = params.filter((p) => p.paramCategory === "OUTPUT");
    dialogTitle.value = "修改算法";
    dialogVisible.value = true;
  });
}

/** 提交 */
function submitForm() {
  proxy.$refs.formRef.validate((valid) => {
    if (!valid) return;
    const run = async () => {
      submitLoading.value = true;
      try {
        if (pendingZipFile.value) {
          const res = await uploadAlgorithmCode(
            form.value.algorithmType,
            form.value.algorithmName,
            pendingZipFile.value
          );
          const url = res && res.data != null ? String(res.data) : "";
          form.value.algorithmCodeUrl = url;
          if (!url) {
            proxy.$modal.msgError("上传成功但未返回路径，请检查文件服务配置");
            return;
          }
        }
        const allParams = [
          ...inputParams.value.map((p, i) => ({ ...p, paramCategory: "INPUT", sortOrder: i + 1 })),
          ...configParams.value.map((p, i) => ({ ...p, paramCategory: "CONFIG", sortOrder: i + 1 })),
          ...outputParams.value.map((p, i) => ({ ...p, paramCategory: "OUTPUT", sortOrder: i + 1 })),
        ];
        const data = { ...form.value, params: allParams };
        if (form.value.id) {
          await updateAlgorithm(data);
          proxy.$modal.msgSuccess("修改成功");
        } else {
          await addAlgorithm(data);
          proxy.$modal.msgSuccess("新增成功");
        }
        dialogVisible.value = false;
        pendingZipFile.value = null;
        getList();
      } catch (e) {
        proxy.$modal.msgError(e.msg || e.message || "保存失败");
      } finally {
        submitLoading.value = false;
      }
    };
    run();
  });
}

function handlePreview(row) {
  previewTitle.value = row.algorithmName + " — 源码预览";
  previewCode.value = "加载中…";
  previewVisible.value = true;
  previewAlgorithmCode(row.id).then((res) => {
    const d = res.data != null && res.data !== "" ? String(res.data) : "";
    if (d) {
      previewCode.value = d;
      return;
    }
    // 兼容旧接口：曾误用 success(String)，源码落在 msg 里
    const m = res.msg != null ? String(res.msg) : "";
    previewCode.value = m && m !== "预览成功" && m !== "操作成功" ? m : "";
  }).catch(() => {
    previewCode.value = "";
  });
}

function handleDownload(row) {
  const url = row.algorithmCodeUrl;
  if (!url) return;
  const parts = url.split("/");
  const fileName = parts[parts.length - 1] || "algorithm.zip";
  proxy.download("file/downloadReport", { url }, fileName);
}

/** 删除 */
function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value;
  proxy.$modal.confirm('确认删除选中的算法？').then(() => {
    return delAlgorithm(delIds.join(","));
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

function handleBatchDelete() {
  if (ids.value.length === 0) {
    proxy.$modal.msgWarning("请先选择要删除的算法");
    return;
  }
  handleDelete({});
}

/** 发布 */
function handlePublish(row) {
  proxy.$modal.confirm('确认发布算法"' + row.algorithmName + '"？').then(() => {
    return publishAlgorithm(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("发布成功");
  }).catch(() => {});
}

/** 撤回发布 */
function handleUnpublish(row) {
  proxy.$modal.confirm('确认撤回算法"' + row.algorithmName + '"的发布？').then(() => {
    return unpublishAlgorithm(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("撤回成功");
  }).catch(() => {});
}

/** 导出 */
function handleExport() {
  proxy.download("/zhpg/algorithm/export", { ...queryParams.value }, `算法列表_${new Date().getTime()}.xlsx`);
}

/** 添加参数行 */
function addParam(category) {
  const newParam = { paramField: "", paramType: "string", defaultValue: "", paramDesc: "", requiredFlag: 0 };
  if (category === "INPUT") inputParams.value.push(newParam);
  else if (category === "CONFIG") configParams.value.push(newParam);
  else outputParams.value.push(newParam);
}

/** 移除参数行 */
function removeParam(category, index) {
  if (category === "INPUT") inputParams.value.splice(index, 1);
  else if (category === "CONFIG") configParams.value.splice(index, 1);
  else outputParams.value.splice(index, 1);
}

/** 重置表单 */
function reset() {
  form.value = { ...initFormData };
  inputParams.value = [];
  configParams.value = [];
  outputParams.value = [];
  pendingZipFile.value = null;
  proxy.resetForm("formRef");
}

/** 取消 */
function cancel() {
  dialogVisible.value = false;
  reset();
}

getList();
</script>

<style lang="scss" scoped>
.param-section {
  padding: 0 20px 10px 20px;
}
.mt8 {
  margin-top: 8px;
}
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-ops {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}
.code-upload-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.code-upload-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.algo-preview-pre {
  margin: 0;
  max-height: 65vh;
  overflow: auto;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-lighter);
  :deep(code.hljs) {
    display: block;
    padding: 14px 16px;
    font-family: ui-monospace, "Cascadia Code", "Source Code Pro", Consolas, monospace;
    font-size: 13px;
    line-height: 1.55;
    white-space: pre;
    overflow-x: auto;
    tab-size: 4;
  }
}
</style>
