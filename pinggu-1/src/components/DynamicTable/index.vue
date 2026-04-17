<template>
  <el-table
    ref="elTableRef"
    :data="tableData"
    style="width: 100%"
    @selection-change="handleSelectionChange"
    :default-sort="sortConfig || undefined"
  >
    <!-- 多选列 -->
    <el-table-column
      v-if="showCheckbox"
      type="selection"
      width="55"
      align="center"
    />

    <!-- 展开列 -->
    <el-table-column
      v-if="showExpand"
      type="expand"
      width="55"
      align="center"
    >
      <template #default="props">
        <slot name="expandSlot" :row="props.row" :index="props.$index" />
      </template>
    </el-table-column>

    <!-- 序号列 -->
    <el-table-column
      v-if="!showIndex"
      type="index"
      label="序号"
      width="60"
      align="center"
    />

    <!-- 标准列 -->
    <el-table-column
      v-for="col in standardColumns"
      :key="col.key"
      :label="col.label"
      :prop="col.key"
      :min-width="col.width"
      align="center"
      :show-overflow-tooltip="col.showOverflowTooltip"
      :sortable="col.sortable ?? false"
      :sort-method="col.sortMethod"
    >
      <template #default="scope">
        <!-- 特殊处理 participationStatus -->
        <div v-if="col.key === 'participationStatus' ">
          <el-tag :type="scope.row[col.key] == 1 ? 'success' : 'info'" size="mini">
            {{ col.formatter ? col.formatter(scope.row[col.key]) : scope.row[col.key] }}
          </el-tag>
        </div>
        <div v-else-if=" col.key === 'tmscopeStatus' || col.key === 'radarStatus'">
          <el-tag :type="scope.row[col.key] == 1 ? 'danger' : 'success'" size="mini">
            {{ col.formatter ? col.formatter(scope.row[col.key]) : scope.row[col.key] }}
          </el-tag>
        </div>
         <!-- 选择框类型的格式化显示 -->
         <span v-else-if="col.component === 'select' && col.options">
          {{ getOptionLabel(col.options, scope.row[col.key]) }}
        </span>
        <!-- 其他列 -->
        <span v-else>
          {{ col.formatter ? col.formatter(scope.row[col.key]) : scope.row[col.key] }}
        </span>
      </template>
    </el-table-column>

    <!-- 自定义列 -->
    <el-table-column
      v-for="col in customColumns"
      :key="col.key"
      :label="col.label"
      :prop="col.key"
      :min-width="col.width"
      :fixed="col.fixed"
      align="center"
      :show-overflow-tooltip="col.showOverflowTooltip"
    >
      <template #default="scope">
        <slot :name="col.slotName" :row="scope.row" :index="scope.$index" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  tableData: Array,
  fieldConfig: Array,
  showIndex: Boolean,
  showCheckbox: Boolean,
  showExpand: Boolean,
  sortConfig: {
    type: Object,
    default: () => null
  }
});

const standardColumns = computed(() =>
  props.fieldConfig.filter((f) => !f.customColumn && !f.hidden)
);
const customColumns = computed(() =>
  props.fieldConfig.filter((f) => f.customColumn)
);

const emit = defineEmits(["selection-change"]);
const handleSelectionChange = (selection) => {
  emit("selection-change", selection);
};

const elTableRef = ref(null);

const tableRefClearSelection = () => {
  elTableRef.value?.clearSelection();
};

const getOptionLabel = (options, value) => {
  if (value === null || value === undefined) return '-';
  
  const option = options.find(opt => opt.value === value);
  return option ? option.label : value;
};
defineExpose({ tableRefClearSelection });
</script>

<style scoped></style>
