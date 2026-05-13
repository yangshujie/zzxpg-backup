<template>
  <div class="app-container" style="overflow: hidden;">
    <el-row :gutter="8">
      <el-col :span="14">
        <div class="map">
          <cesium-map @selectPoints="selectPoints" @clear="clearPoints"
            :type="formData.areaType === '点目标' ? 'Point' : 'Area'" :disabled="isViewMode" ref="cesiumRef" />
        </div>
      </el-col>
      <el-col :span="10">
        <el-form ref="configRef" :model="formData" :rules="rules" label-width="150px" status-icon
          style="height: calc(100vh - 130px - 2rem);" :disabled="isViewMode" class="req-form">
          <!-- 查看模式下的需求基本信息 -->
          <template v-if="isViewMode">
            <el-form-item label="需求ID" v-if="formData.taskId">
              <el-input v-model="formData.taskId" :disabled="true" />
            </el-form-item>
            <el-form-item label="提交时间" v-if="formData.submitTime">
              <el-input v-model="formatSubmitTime" :disabled="true" />
            </el-form-item>
            <el-form-item label="创建时间" v-if="formData.createTime">
              <el-input v-model="formatCreateTime" :disabled="true" />
            </el-form-item>
            <el-form-item label="修改时间" v-if="formData.updateTime">
              <el-input v-model="formatUpdateTime" :disabled="true" />
            </el-form-item>
            <el-form-item label="任务状态" v-if="formData.status !== undefined">
              <el-input v-model="formatTaskStatus" :disabled="true" />
            </el-form-item>
          </template>
          <template v-else-if='isFromHocDemands'>
            <el-form-item label="临机任务" prop="hocDemand">
              <el-select v-model="hocDemand">
                <el-option v-for="dict in hot_command_content" :key="dict.dictValue" :label="dict.label"
                  :value="parseInt(dict.value)" disabled />
              </el-select>
            </el-form-item>
          </template>
          <el-form-item label="任务类型" prop="taskType">
            <template v-if="isViewMode">
              <!-- 查看模式下只显示格式化的任务类型 -->
              <el-input v-model="formatTaskType" :disabled="true" />
            </template>
            <template v-else>
              <!-- 编辑模式下显示选择框 -->
              <el-select v-model="formData.taskType" placeholder="请选择任务类型">
                <el-option v-for="dict in task_type" :key="dict.value" :label="dict.label"
                  :value="parseInt(dict.value)" />
              </el-select>
            </template>
          </el-form-item>

          <!-- 查看模式下的额外显示字段 -->
          <template v-if="isViewMode">
            <el-form-item label="任务频次" v-if="formData.taskCount">
              <el-input v-model="formData.taskCount" :disabled="true" />
            </el-form-item>

            <el-form-item label="任务时间段" v-if="formatTaskTimeRange">
              <el-input v-model="formatTaskTimeRange" :disabled="true" />
            </el-form-item>
            <el-form-item label="卫星配置" v-if="formData.satelliteConfig">
              <div class="satellite-config-display">
                <div class="config-display-item">
                  <div class="config-detail">
                    <strong style="color: white !important;">载荷类型:</strong> {{
                      formatSatelliteTypes(formData.satelliteConfig.satelliteType) }}
                  </div>
                  <div class="config-detail">
                    <strong style="color: white !important;">卫星名称:</strong> {{
                      formatSatelliteNames(formData.satelliteConfig.satelliteName) }}
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="数传时效" v-if="formData.latestDeadline">
              <el-input v-model="formData.latestDeadline" :disabled="true" />
            </el-form-item>
            <el-form-item label="分辨率" v-if="formData.resolutionRange">
              <!-- <el-input v-model="formData.resolutionRange" :disabled="true" /> -->
              <el-input-number v-model="formData.resolutionRange[0]" controls-position="right" placeholder="分辨率（米）：10M"
                :min="0.1" />

              <div style="margin: 0px 10px 0px 10px;"><span style="color: #999;">~</span></div>
              <el-input-number v-model="formData.resolutionRange[1]" controls-position="right" placeholder="分辨率（米）：20M"
                :min="0.1" />
            </el-form-item>
            <el-form-item label="幅宽" v-if="formData.width">
              <!-- <el-input :value="formatWidth" :disabled="true" /> -->
              <el-input-number v-model="formData.width[0]" controls-position="right" placeholder="幅宽（公里）：10KM"
                :min="0.1" />

              <div style="margin: 0px 10px 0px 10px;"><span style="color: #999;">~</span></div>
              <el-input-number v-model="formData.width[1]" controls-position="right" placeholder="幅宽（公里）：20KM"
                :min="0.1" />
            </el-form-item>
            <!-- <el-form-item label="高度角限制" v-if="form.elevatingAngleLimit">
              <el-input v-model="form.elevatingAngleLimit" :disabled="true" />
            </el-form-item>
            <el-form-item label="太阳高度角限制" v-if="form.sunElevatingAngleLimit">
              <el-input v-model="form.sunElevatingAngleLimit" :disabled="true" />
            </el-form-item> -->
            <el-form-item label="优先级">
              <el-input v-model="displayPriority" :disabled="true" />
            </el-form-item>
            <el-form-item label="目标区域" v-if="formData.taskType === 0 && areaList && areaList.length > 0">
              <div class="target-area-display">
                <div class="area-type-info">
                  <span style="color: white !important;"><strong style="color: white !important;">区域类型:</strong> {{
                    formData.areaType }}</span>
                  <span style="margin-left: 20px;"><strong style="color: white !important;">坐标点数量:</strong> {{
                    validAreaPoints.length }}</span>
                  <el-button size="small" type="success" plain style="margin-left: 20px;"
                    @click="flyToAllTargets">查看全部</el-button>
                </div>
                <div class="area-points-list">
                  <div v-for="(point, index) in validAreaPoints" :key="index" class="area-point-item"
                    @click="flyToPoint(point, index)">
                    <div class="point-title">第{{ index + 1 }}个坐标点</div>
                    <div class="point-coords">
                      <span><strong style="color: white !important;">经度:</strong> {{ point.longitude }}</span>
                      <span><strong style="color: white !important;">纬度:</strong> {{ point.latitude }}</span>
                      <!-- <span><strong>高度:</strong> {{ point.height || 0 }}m</span> -->
                    </div>
                    <div class="point-action">
                      <el-button size="small" type="primary" plain>查看位置</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="目标卫星"
              v-if="formData.taskType === 1 && formData.targetSatelliteList && formData.targetSatelliteList.length > 0">
              <div class="target-satellite-display">
                <div class="satellite-type-info">
                  <span><strong>目标卫星数量:</strong> {{ formData.targetSatelliteList.length }}</span>
                </div>
                <div class="satellite-list">
                  <div v-for="(satellite, index) in formData.targetSatelliteList" :key="index" class="satellite-item">
                    <div class="satellite-name">{{ satellite }}</div>
                  </div>
                </div>
              </div>
            </el-form-item>

          </template>
          <template v-if="!isViewMode">
            <el-form-item label="侦察频次" prop="taskCount" v-show="formData.taskType === 0" style="width: 90%">
              <el-input v-model="formData.taskCount" type="number" :min="0" :step="1" placeholder="请输入侦察频次" />
            </el-form-item>
            <el-form-item label="任务时间段" prop="taskTime" style="width: 90%">
              <el-date-picker v-model="formData.taskTime" type="datetimerange" start-placeholder="开始时间"
                end-placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss" popper-class="noClear" />
            </el-form-item>
            <el-form-item label="卫星配置" prop="satelliteConfig">
              <div class="satellite-config-container">
                <div class="config-row">
                  <label>载荷类型：</label>
                  <el-select v-model="formData.satelliteConfig.satelliteType" multiple placeholder="请选择载荷类型"
                    @blur="handlePayloadTypeChange" clearable style="width: 100%; margin-right: 10px;">
                    <el-option v-for="dict in payload_type" :key="dict.value" :label="dict.label"
                      :value="parseInt(dict.value)" />
                  </el-select>
                </div>

                <div class="config-row">
                  <label>卫星名称：</label>
                  <el-select v-model="formData.satelliteConfig.satelliteName" multiple placeholder="请选择卫星" clearable
                    style="width: 100%; margin-right: 10px;">
                    <el-option v-for="(item, index) in satelliteList" :key="index" :label="item" :value="item" />
                  </el-select>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="数传时效" prop="latestDeadline">
              <el-date-picker v-model="formData.latestDeadline" type="datetime" placeholder="请选择数传文件下载的最晚时间"
                :shortcuts="shortcuts" />
            </el-form-item>
            <el-form-item label="分辨率（米）" prop="resolutionRange">
              <!-- <el-input-number v-model="formData.resolutionRange" controls-position="right" placeholder="分辨率（米）"
                :min="0.1" /> -->
              <el-input-number v-model="formData.resolutionRange[0]" controls-position="right" placeholder="分辨率（米）：10M"
                :min="0.1" />

              <div style="margin: 0px 10px 0px 10px;"><span style="color: #999;">~</span></div>
              <el-input-number v-model="formData.resolutionRange[1]" controls-position="right" placeholder="分辨率（米）：20M"
                :min="0.1" />
            </el-form-item>
            <el-form-item label="幅宽（公里）" prop="width">
              <el-input-number v-model="formData.width[0]" controls-position="right" placeholder="幅宽（公里）：10KM"
                :min="0.1" />

              <div style="margin: 0px 10px 0px 10px;"><span style="color: #999;">~</span></div>
              <el-input-number v-model="formData.width[1]" controls-position="right" placeholder="幅宽（公里）：20KM"
                :min="0.1" />

            </el-form-item>
            <!-- <el-form-item label="高度角" prop="elevatingAngleLimit">
              <el-input-number
                v-model="formData.elevatingAngleLimit"
                controls-position="right"
                placeholder="高度角"
                :min="0"
                :max="90"
              />
            </el-form-item>
            <el-form-item label="太阳高度角" prop="sunElevatingAngleLimit">
              <el-input-number
                v-model="formData.sunElevatingAngleLimit"
                controls-position="right"
                placeholder="太阳高度角"
                :min="0"
                :max="90"
              />
            </el-form-item> -->
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="formData.priority" placeholder="请选择任务优先级">
                <el-option v-for="dict in task_priority" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="formData.taskType === 0" label="目标类型" prop="configValue" style="height: 30px">
              <div style="display: flex; align-items: center; gap: 10px;">
                <el-radio-group v-model="formData.areaType" @change="handleChangeType">
                  <el-radio-button size="small" label="点目标"></el-radio-button>
                  <el-radio-button size="small" label="区域目标"></el-radio-button>
                </el-radio-group>

                <!-- 区域名称选择 -->
                <el-select v-model="formData.regionName" placeholder="请选择区域名称" clearable style="width: 200px;"
                  @change="handleRegionNameChange" :disabled="!formData.areaType"
                  :multiple="formData.areaType === '点目标'"
                  :collapse-tags="formData.areaType === '点目标'"
                  :collapse-tags-tooltip="formData.areaType === '点目标'">
                  <el-option v-for="(item, index) in regionNameOptions" :key="index" :label="item.label"
                    :value="item.value" />
                </el-select>
              </div>
            </el-form-item>

            <!-- 任务类型1的目标卫星选择 -->
            <el-form-item v-if="formData.taskType === 1" label="目标卫星" prop="targetSatelliteList">
              <el-select v-model="formData.targetSatelliteList" multiple placeholder="请选择目标卫星" clearable
                style="width: 100%;">
                <el-option v-for="(item, index) in satelliteList" :key="index" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <template v-if="formData.areaType === '点目标' && formData.taskType === 0">
              <el-form-item label="点目标">
                <div class="point-input-tips">
                  <span style="color: #409eff; font-size: 12px; margin-bottom: 8px; display: block;">
                    💡 操作提示：在地图上左键点击添加点目标
                    <el-button type="primary" size="small" @click="handleGetPoints"
                      style="margin-left: 10px;">批量点目标</el-button>
                  </span>
                </div>
                <div class="area-list-content">
                  <template v-for="(item, index) in areaList" :key="index">
                    <el-input-number v-model="item.longitude" controls-position="right" title="" autocomplete="off"
                      placeholder="经度" :min="-180" :max="180" style="width: 40%; margin: 2px 0" />
                    <el-divider direction="vertical" />
                    <el-input-number v-model="item.latitude" controls-position="right" title="" autocomplete="off"
                      placeholder="纬度" :min="-90" :max="90" style="width: 40%" />
                    <!-- <el-input-number
                      v-model="item.height"
                      controls-position="right"
                      title=""
                      autocomplete="off"
                      placeholder="高度"
                      :min="0"
                      :max="156546"
                      style="width: 25%"
                    /> -->
                    <el-button style="margin-left: 5px" size="small" type="success" v-if="index === areaList.length - 1"
                      @click="handlePlus">+</el-button>
                    <el-button style="margin-left: 5px" size="small" type="danger" v-else
                      @click="handleMinus(index)">-</el-button>
                  </template>
                </div>
              </el-form-item>
            </template>
            <template v-else-if="formData.areaType === '区域目标' && formData.taskType === 0">
              <el-form-item label="区域">
                <div class="point-input-tips">
                  <span style="color: #409eff; font-size: 12px; margin-bottom: 8px; display: block;">
                    💡 操作提示：在地图上左键点击添加区域目标，右键点击结束添加
                  </span>
                </div>
                <div class="area-list-content">
                  <template v-for="(item, index) in areaList" :key="index">
                    <el-input-number v-model="item.longitude" controls-position="right" title="" autocomplete="off"
                      placeholder="经度" :min="-180" :max="180" style="width: 40%; margin: 2px 0" />
                    <el-divider direction="vertical" />
                    <el-input-number v-model="item.latitude" controls-position="right" title="" autocomplete="off"
                      placeholder="纬度" :min="-90" :max="90" style="width: 40%" />
                    <!-- <el-divider direction="vertical" />
                    <el-input-number
                      v-model="item.height"
                      controls-position="right"
                      title=""
                      autocomplete="off"
                      placeholder="高度"
                      :min="0"
                      :max="156546"
                      style="width: 25%"
                    /> -->
                    <el-button style="margin-left: 5px" type="success" size="small"
                      v-if="index === areaList.length - 1 && !isViewMode" @click="handlePlus">+</el-button>
                    <el-button style="margin-left: 5px" type="danger" size="small" v-else-if="!isViewMode"
                      @click="handleMinus(index)">-</el-button>
                  </template>
                </div>
              </el-form-item>
            </template>

          </template>
        </el-form>
        <div class="action-button">
          <el-button v-if="isFromRequirementsView" @click="handleCancel" size="small">返回</el-button>
          <el-button v-else-if="isFromRequirements" @click="handleCancel" size="small">取消</el-button>

          <el-button v-if="!isViewMode" type="primary" size="small" @click="handleImportJson">导入表单</el-button>
          <el-button v-if="!isViewMode" type="success" size="small" :loading="disableRequest"
            @click="submit(configRef)">任务规划</el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 任务规划结果对话框已移动到可见性结果界面 -->
  </div>

  <!-- 文件导入输入框 -->
  <input ref="fileInputRef" type="file" accept=".json" style="display: none" @change="handleJsonFileChange" />

  <!-- 全局加载状态 -->
  <div v-if="loadingState.visible" class="global-loading-overlay">
    <div class="loading-content">
      <el-icon class="loading-icon">
        <Loading />
      </el-icon>
      <div class="loading-text">{{ loadingState.text }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted, computed, nextTick, watch, onMounted, getCurrentInstance } from "vue";
import { getToken } from "@/utils/auth";
import CesiumMap from "./components/cesiumMap";
import useUserStore from "@/store/modules/user";
import { ElMessage, ElNotification } from "element-plus";
import { Loading } from '@element-plus/icons-vue';
// import { getSatelliteList } from '@/api/system3/satellite';
// import { getEquivalentSchemeExtractionScheme } from '@/api/system1/dxList';
// import { getRegionByName } from '@/api/system3/area';
import { useRoute, useRouter } from 'vue-router';
// import { useViewDataStore } from '@/store/modules/viewData'
// import { get100Points } from '@/api/system4/visible';
// import {
//   formatDateTime,
//   formatTimestamp,
//   formatFileDateTime,
//   getChinaTimestamp,
//   generateTaskId,
//   isDateBeforeTomorrow
// } from '@/utils/timeUtils'
// import tmpData from './data/[0701]空间感知成像需求构造数据.json'; // 不再需要

const { proxy } = getCurrentInstance();
// const { task_type, payload_type, task_priority} = proxy.useDict("task_type", "payload_type", "task_priority");
const { task_type, task_priority, hot_command_content } = proxy.useDict("task_type", "task_priority", "hot_command_content");
const payload_type = [
  {
    value: "1",
    label: "光学"
  },
  {
    value: "2",
    label: "SAR"
  },
  {
    value: "6",
    label: "电子"
  }];
const shortcuts = [
  {
    text: '明天',
    value: () => {
      const date = new Date()
      date.setDate(date.getDate() + 1)
      return date
    },
  },
  {
    text: '一周后',
    value: () => {
      const date = new Date()
      date.setDate(date.getDate() + 7)
      return date
    },
  },
]
const userStore = useUserStore();
const formData = ref({
  type: 0,
  areaType: "区域目标",
  inputType: "Draw",
  taskId: null, // 需求ID
  taskType: 0,
  taskCount: 1, // 侦察频次，默认为1
  taskTime: [],
  satelliteConfig: {
    satelliteType: [],
    satelliteName: []
  },
  resolutionRange: [5, 10],
  timeRange: [],
  satellite: [],
  payloadType: [],
  workMode: [],
  latestDeadline: null,
  width: [10, 20], // 修复：添加width字段初始化
  elevatingAngleLimit: null,
  sunElevatingAngleLimit: null,
  priority: "0",
  targetSatelliteList: [], // 目标卫星列表（任务类型1使用）
  submitTime: null, // 提交时间
  createTime: null, // 创建时间
  updateTime: null, // 修改时间
  status: null, // 任务状态
  regionName: null, // 区域名称选择
  regionCoordinate: null // 区域坐标信息
});
const rules = ref({
  taskType: [
    { required: true, message: '请选择任务类型', trigger: 'change' }
  ],
  taskTime: [
    { required: true, message: '请选择任务时间范围', trigger: 'change' }
  ],
  // satelliteConfig: [
  //   { 
  //     required: true, 
  //     validator: (rule, value, callback) => {
  //       if (!value || !value.satelliteType || value.satelliteType.length === 0) {
  //         callback(new Error('请选择载荷类型'));
  //       } else if (!value.satelliteName || value.satelliteName.length === 0) {
  //         callback(new Error('请选择卫星名称'));
  //       } else {
  //         callback();
  //       }
  //     }, 
  //     trigger: 'change' 
  //   }
  // ],
  // resolutionRange: [
  //   { required: true, message: '请输入分辨率', trigger: 'change' }
  // ],
  priority: [
    { required: true, message: '请选择任务优先级', trigger: 'change' }
  ],
  // targetSatelliteList: [
  //   { 
  //     validator: (rule, value, callback) => {
  //       // 只有任务类型为1（攻防任务）时才验证目标卫星列表
  //       if (formData.value.taskType === 1 && (!value || value.length === 0)) {
  //         callback(new Error('请选择目标卫星'));
  //       } else {
  //         callback();
  //       }
  //     }, 
  //     trigger: 'change' 
  //   }
  // ]
});
// const activeTab = ref(0); // 移除未使用的变量
const configRef = ref();
const disableRequest = ref(false);
const loadingState = ref({
  visible: false,
  text: '处理中...'
});
// const requestCheckRef = ref(); // 移除未使用的变量
// const TIME_INTERVAL = 10000; // 移除未使用的常量
// let intervalInstance = null; // 移除未使用的变量
const cesiumRef = ref();

const areaList = ref([{ longitude: "", latitude: "", height: "" }]);
// const fileInput = ref(); // 移除未使用的变量
// const currentReuqestResult = ref({}); // 移除未使用的变量

// 文件导入相关引用
const fileInputRef = ref();

const satelliteList = ref([]);
// 区域名称选项列表
const regionNameOptions = ref([]);

// 表单数据缓存键
const FORM_CACHE_KEY = 'req_add_form_cache';

// const subRequestSubmitTimes = ref(0)



// 任务规划结果已移动到可见性结果界面处理

const route = useRoute();
const router = useRouter();

const viewDataStore = useViewDataStore();
let isFirstLoad = true


// 表单数据缓存管理函数
const saveFormCache = () => {
  // 在add模式或从任务规划跳转时都保存缓存
  if (route.query.mode === "add" || route.query.from === "reqSubmission") {
    console.log('保存表单缓存数据:', areaList.value);
    const cachedData = {
      formData: formData.value,
      areaList: areaList.value
    }
    localStorage.setItem(FORM_CACHE_KEY, JSON.stringify(cachedData));
  }
};

const loadFormCache = () => {
  // 在add模式或从任务规划返回时都加载缓存
  if (route.query.mode === "add" || route.query.from === "visibleResult") {
    const cached = localStorage.getItem(FORM_CACHE_KEY);
    console.log("尝试加载缓存数据:", cached)
    try {
      const cachedData = JSON.parse(cached);
      console.log('表单缓存数据解析成功:', cachedData);
      const currentTaskId = formData.value.taskId;
      formData.value = { ...cachedData.formData, taskId: currentTaskId }
      areaList.value = [...cachedData.areaList]
      console.log("缓存数据加载完成，areaList:", areaList.value)
    } catch (e) {
      console.warn('表单缓存数据解析失败', e);
    }
  }
};

const clearFormCache = () => {
  localStorage.removeItem(FORM_CACHE_KEY);
};


onMounted(() => {
  handlePayloadTypeChange();
  if (route.query.mode == 'add' ) {
    loadFormCache();
  }
})

// 监听 areaList 的变化，自动更新地图绘制
watch(
  areaList,
  (newAreaList, oldAreaList) => {
    console.log("0", areaList.value, newAreaList, oldAreaList)
    // 确保地图组件已初始化
    if (!cesiumRef.value) {
      console.log('地图组件未初始化，跳过绘制');
      if (cesiumRef.value) {
        setTimeout(() => {
          drewLoadPointer(newAreaList, oldAreaList)
        }, 100);
      }
      return;
    }

    // 检查地图是否完全初始化（使用 cesiumMap 组件的 isInitialized 标志）
    console.log("1", areaList.value)
    if (!cesiumRef.value.isInitialized) {
      console.log('地图未完全初始化，跳过绘制');
      if (cesiumRef.value) {
        setTimeout(() => {
          drewLoadPointer(newAreaList, oldAreaList)
        }, 100);
      }
      return;
    }
    drewLoadPointer(newAreaList, oldAreaList)

  },
  {
    deep: true, // 深度监听对象内部属性变化
    flush: 'post' // 在 DOM 更新后执行
  }
)

// 监听 areaType 的变化，自动加载对应的区域名称选项
watch(
  () => formData.value.areaType,
  (newAreaType, oldAreaType) => {
    console.log('areaType 发生变化:', oldAreaType, '->', newAreaType);

    // 只有在areaType有值且发生变化时，才自动加载区域名称选项
    if (newAreaType && newAreaType !== oldAreaType) {
      handleRegionNameFocus();

      // 重置区域选择相关数据
      formData.value.regionName = null;
      formData.value.regionCoordinate = null;
      regionNameOptions.value = [];


    }
  },
  {
    immediate: true // 立即执行一次，确保页面初始化时加载默认选项
  }
)

function drewLoadPointer(newAreaList, oldAreaList) {
  try {
    // 过滤出有效的坐标点
    const validPoints = newAreaList.filter(point =>
      point.longitude !== "" &&
      point.longitude !== null &&
      point.longitude !== undefined &&
      point.latitude !== "" &&
      point.latitude !== null &&
      point.latitude !== undefined
    );


    // const isFirstLoad=!oldAreaList||oldAreaList.length===0;
    // 先清除地图上原有的点和区域
    if (!isFirstLoad) {

      console.log("isFirstLoad.value", isFirstLoad)
      cesiumRef.value.removePolygon("point");
      cesiumRef.value.removePolygon("area");
    } else {
      isFirstLoad = false
    }


    // 如果没有有效坐标点，直接返回
    if (validPoints.length === 0) {
      console.log('没有有效坐标点，清除地图绘制');
      return;
    }

    // 根据区域类型重新绘制
    if (formData.value.areaType === "点目标") {
      // 点目标：绘制所有有效点
      console.log('绘制点目标:', validPoints);
      cesiumRef.value.addPoint(validPoints, "point");
    } else if (formData.value.areaType === "区域目标") {
      // 区域目标：至少需要3个点才能绘制
      if (validPoints.length >= 3) {
        console.log('绘制区域目标:', validPoints);
        cesiumRef.value.addPolygon(validPoints, "area");
      } else {
        console.log('区域目标点数不足3个，无法绘制');
      }
    }
    console.log('绘制点目标_____addd:', areaList.value);
    if (route.query.mode === 'add') {
      // 使用防抖避免频繁保存
      clearTimeout(window.areaCacheTimer);
      window.areaCacheTimer = setTimeout(() => {
        saveFormCache();
      }, 500);
    }
  } catch (error) {
    console.error('地图绘制出错:', error);
    // 出错时不阻断程序运行
  }
}

// 添加查看模式相关的响应式变量
const isViewMode = computed(() => {
  return route.query.mode === 'view';
});

// 添加临机命令跳转过来的需求
const isFromHocDemands = computed(() => {
  return route.query.from === 'hocDemands';
})

const hocDemand = computed(() => {
  return Number(route.query.mission);
})

const isFromRequirementsView = computed(() => {
  return route.query.from === 'requirements' && route.query.mode === 'view';
});

// 修改现有的 isFromRequirements 计算属性
const isFromRequirements = computed(() => {
  return route.query.from === 'requirements' && !isViewMode.value;
});



// 格式化提交时间
const formatSubmitTime = computed(() => {
  return formatDateTime(formData.value.submitTime);
});

// 格式化创建时间
const formatCreateTime = computed(() => {
  return formatDateTime(formData.value.createTime);
});

// 格式化修改时间
const formatUpdateTime = computed(() => {
  return formatDateTime(formData.value.updateTime);
});

// 格式化任务状态
const formatTaskStatus = computed(() => {
  if (formData.value.status === null || formData.value.status === undefined) return '待处理';
  const statusMap = {
    0: '待处理',
    1: '处理中',
    2: '已完成',
    3: '已取消'
  };
  return statusMap[formData.value.status] || `状态${formData.value.status}`;
});

// 格式化任务类型
const formatTaskType = computed(() => {
  const taskTypeMap = {
    0: '侦察任务',
    1: '攻防任务',
    2: '打击任务'
  };
  return taskTypeMap[parseInt(formData.value.taskType)] || '未知类型';
});

// 格式化任务时间范围
const formatTaskTimeRange = computed(() => {
  if (!formData.value.taskTime || !Array.isArray(formData.value.taskTime) || formData.value.taskTime.length !== 2) return '';
  const startTime = formatDateTime(formData.value.taskTime[0]);
  const endTime = formatDateTime(formData.value.taskTime[1]);
  return `${startTime} ~ ${endTime}`;
});



// 格式化目标区域
const formatTargetArea = computed(() => {
  if (!areaList.value || areaList.value.length === 0) return '';

  const validPoints = areaList.value.filter(point =>
    point.longitude && point.latitude
  );

  if (validPoints.length === 0) return '';

  if (formData.value.areaType === '点目标') {
    if (validPoints.length === 1) {
      const point = validPoints[0];
      return `点目标: (${point.longitude}, ${point.latitude}, ${point.height || 0})`;
    } else {
      return `点目标: ${validPoints.length}个坐标点`;
    }
  } else {
    return `区域目标: ${validPoints.length}个坐标点`;
  }
});

// 优先级显示（直接显示数字，不格式化）
const displayPriority = computed(() => {
  return formData.value.priority?.toString() || '0';
});

// 格式化幅宽显示
const formatWidth = computed(() => {
  if (!formData.value.width || !Array.isArray(formData.value.width)) return '';
  if (formData.value.width.length === 2) {
    return `${formData.value.width[0]} ~ ${formData.value.width[1]} 公里`;
  }
  return formData.value.width.toString();
});

// 获取有效的区域坐标点
const validAreaPoints = computed(() => {
  if (!areaList.value || areaList.value.length === 0) return [];
  return areaList.value.filter(point =>
    point.longitude !== "" && point.longitude !== null && point.longitude !== undefined &&
    point.latitude !== "" && point.latitude !== null && point.latitude !== undefined
  );
});





// 格式化载荷类型
const formatSatelliteTypes = (types) => {
  if (!types || types.length === 0) return '无';
  return types.map(type =>
    payload_type.find(dict => parseInt(dict.value) === parseInt(type))?.label || type
  ).join(', ');
};

// 格式化卫星名称
const formatSatelliteNames = (names) => {
  if (!names || names.length === 0) return '无';
  return names.join(', ');
};

onMounted(() => {
  // getSiteList()
  // proxy.valueCode.level
  console.log(userStore.isOperator, "admin");

  // 如果是查看模式，从 store 加载数据
  if (isViewMode.value) {
    const viewData = viewDataStore.getViewData();
    if (viewData) {
      // 延迟加载数据，确保地图组件完全初始化
      setTimeout(() => {
        loadViewData(viewData);
      }, 1000);
    } else {
      console.warn('未找到查看数据');
      ElMessage.warning('数据已过期，请重新进入');
      router.push('/system4/ptPlan/requirements');
    }
  }

  // 如果是从equivalentPlan跳转过来的，获取需求提取数据
  if (route.query.from === 'equivalentPlan' && route.query.extractionId) {
    const extractionId = route.query.extractionId;
    console.log('从equivalentPlan跳转过来，extractionId:', extractionId);

    getEquivalentSchemeExtractionScheme(extractionId).then((response) => {
      if (response.code === 200) {
        console.log('获取需求提取数据成功:', response.data);
        // 将接口返回的数据赋值给formData.value
        if (response.data) {
          const data = response.data;

          // 处理基本表单数据
          formData.value = {
            ...formData.value, // 保留原有结构
            taskType: data.taskType,
            submitTime: data.submitTime,
            satelliteConfig: data.satelliteConfig || formData.value.satelliteConfig,
            width: data.width || [10, 20],
            priority: data.priority || "0",
            resolutionRange: data.resolutionRange || [5, 10],
            taskTime: data.taskTime ? [
              new Date(data.taskTime.convertStartTime),
              new Date(data.taskTime.convertEndTime)
            ] : []
          };

          // 处理目标区域数据
          if (data.targetAreaList && data.targetAreaList.length > 0) {
            const targetArea = data.targetAreaList[0];
            const targetType = parseInt(targetArea.targetType);

            // 根据targetType设置areaType
            if (targetType === 0) {
              formData.value.areaType = "点目标";
            } else {
              formData.value.areaType = "区域目标";
            }

            // 处理坐标数据
            if (targetArea.area && targetArea.area.length > 0) {
              areaList.value = targetArea.area.map(coord => ({
                longitude: coord[0],
                latitude: coord[1],
                height: coord[2] || 0
              }));

              // watch 会自动处理地图绘制
              console.log('加载需求提取数据，区域类型:', formData.value.areaType, '坐标数据:', areaList.value);
            }
          }

          ElMessage.success('需求提取数据加载成功');
        }
      } else {
        ElMessage.error(response.msg || '获取需求提取数据失败');
      }
    }).catch((error) => {
      console.error('获取需求提取数据失败:', error);
      ElMessage.error('获取需求提取数据失败');
    });
  }
});

function triggerFileInput() {
  fileInput.value.click();
}

const disabledDateFn = (time) => {
  return isDateBeforeTomorrow(time);
};

// 处理文件上传
function handleFileChange(event) {
  areaFile.value = event.target.files[0]; // 获取上传的文件
  console.log(areaFile.value, "file");
  if (areaFile.value) {
    const reader = new FileReader(); // 创建FileReader对象
    reader.onload = (e) => {
      const content = e.target.result; // 获取文件内容
      console.log(content, "content");
      const geojson = JSON.parse(content); // 解析GeoJSON
      console.log(geojson, "geojson");
      extractCoordinates(geojson); // 提取坐标信息
    };
    reader.readAsText(areaFile.value); // 以文本方式读取文件
  }
}

// 提取GeoJSON中的坐标信息
function extractCoordinates(geojson) {
  areaList.value = []; // 清空之前的坐标信息
  if (geojson.geometry.coordinates) {
    geojson.geometry.coordinates[0].forEach((point) => {
      areaList.value.push({ longitude: point[0], latitude: point[1], height: 0 });
    });
    // watch 会自动处理地图绘制，无需手动调用
    console.log(areaList.value, "areaList.value");
  } else {
    areaFile.value = null;
    ElMessage.info("Please check the file format!");
  }

  setTimeout(() => {
    document.getElementById("geojsonUpload").value = "";
    areaFile.value = null;
  }, 500);
}

function selectPoints(e) {
  console.log(e, "parents");
  if (formData.value.areaType === "点目标") {
    // 点目标：可以选择多个点
    areaList.value = [];
    e.forEach((el) => {
      areaList.value.push({
        longitude: el[0],
        latitude: el[1],
        height: el[2] || 0 // 如果没有高度信息，默认为0
      });
    });

    // 如果没有选择点，至少保证一个空的点用于输入
    if (areaList.value.length === 0) {
      areaList.value.push({ longitude: "", latitude: "", height: "" });
    }
  } else {
    // 区域目标：取所有点，但至少保证三个点
    areaList.value = [];
    e.forEach((el) => {
      areaList.value.push({
        longitude: el[0],
        latitude: el[1],
        height: el[2] || 0 // 如果没有高度信息，默认为0
      });
    });

    // 如果点数少于3个，补充空的点
    while (areaList.value.length < 3) {
      areaList.value.push({ longitude: "", latitude: "", height: "" });
    }
  }
  console.log("选择后的arealist：", areaList.value)
}

function clearPoints() {
  if (formData.value.areaType === "点目标") {
    areaList.value = [{ longitude: "", latitude: "", height: "" }];
  } else {
    areaList.value = [
      { longitude: "", latitude: "", height: "" },
      { longitude: "", latitude: "", height: "" },
      { longitude: "", latitude: "", height: "" }
    ];
  }

  // watch 会自动清除地图上的点和区域（因为 areaList 变成空值）
}

function handlePlus() {
  areaList.value.push({ longitude: "", latitude: "", height: "" });
}

function handleMinus(index) {
  areaList.value.splice(index, 1);
}

const submit = async (formSubmitRef) => {

  console.log(formData.value, "formData.value");
  // 简化表单验证逻辑
  const formSubmitElement = formSubmitRef || configRef.value;
  console.log(formSubmitElement, "formSubmitElement");
  console.log(configRef.value, "configRef.value");

  // 如果表单元素存在，进行验证
  if (formSubmitElement) {
    try {
      await formSubmitElement.validate();
      console.log('表单验证通过');
    } catch (error) {
      console.log('表单验证失败:', error);
      return;
    }
  } else {
    console.log('表单元素不存在，跳过表单验证');
  }

  // 跳转到可见性结果界面，并传递表单数据
  const routeData = {
    path: '/system4/payloadMissionPlan/visibleResult',
    query: {
      from: 'reqSubmission',
      formData: JSON.stringify(formData.value),
      areaList: JSON.stringify(areaList.value)
    }
  };

  router.push(routeData);
};

async function handleChangeType() {
  // 根据区域类型重置 areaList，watch 会自动清除并重绘地图
  if (formData.value.areaType === "点目标") {
    // 点目标：初始保留一个经纬高对象，可以添加更多
    areaList.value = [{ longitude: "", latitude: "", height: "" }];
  } else {
    // 区域目标：至少需要三个经纬高对象
    areaList.value = [
      { longitude: "", latitude: "", height: "" },
      { longitude: "", latitude: "", height: "" },
      { longitude: "", latitude: "", height: "" }
    ];
  }
  nextTick(() => {
    console.log('当前区域类型:', formData.value.areaType);
    console.log('当前areaList:', areaList.value);
  });
}

/**
 * 处理区域名称选择框获取焦点事件
 */
async function handleRegionNameFocus() {
  if (!formData.value.areaType) {
    return;
  }

  try {
    // 根据区域类型设置regionType参数
    const regionType = formData.value.areaType === "点目标" ? 0 : 1;

    // 调用getRegionByName接口获取区域信息
    const response = await getRegionByName({ regionType });

    if (response.code === 200 && response.data) {
      // 处理返回的数据，生成选项列表
      regionNameOptions.value = response.data.map(item => ({
        label: item.regionName || '未知区域',
        value: item.regionName || 'unknown',
        regionCoordinate: item.regionCoordinate || ''
      }));

      console.log('区域名称选项加载成功:', regionNameOptions.value);
    } else {
      console.error('获取区域名称列表失败:', response.msg);
      ElMessage.error('获取区域名称列表失败');
    }
  } catch (error) {
    console.error('调用区域名称接口出错:', error);
    ElMessage.error('获取区域名称列表失败');
  }
}

/**
 * 处理区域名称选择变化
 */
async function handleRegionNameChange(regionName) {
  // 清空区域坐标选择
  formData.value.regionCoordinate = null;

  if (!regionName) {
    return;
  }

  try {
    // 清空现有的areaList
    areaList.value = [];
    
    // 根据选择模式处理
    if (Array.isArray(regionName)) {
      // 多选模式（点目标）
      regionName.forEach(region => {
        const selectedRegion = regionNameOptions.value.find(item => item.value === region);
        if (selectedRegion && selectedRegion.regionCoordinate) {
          // 解析regionCoordinate字符串为坐标数组
          const coordinatesArray = JSON.parse(selectedRegion.regionCoordinate);
          
          // 点目标：只取第一个坐标点
          if (coordinatesArray.length > 0) {
            const [longitude, latitude, height] = coordinatesArray[0];
            areaList.value.push({
              longitude: longitude || 0,
              latitude: latitude || 0,
              height: height || 0
            });
          }
        }
      });
    } else {
      // 单选模式（区域目标）
      const selectedRegion = regionNameOptions.value.find(item => item.value === regionName);
      
      if (selectedRegion && selectedRegion.regionCoordinate) {
        // 解析regionCoordinate字符串为坐标数组
        const coordinatesArray = JSON.parse(selectedRegion.regionCoordinate);
        
        // 区域目标：取所有坐标点
        coordinatesArray.forEach((coordinate, index) => {
          const [longitude, latitude, height] = coordinate;
          areaList.value.push({
            longitude: longitude || 0,
            latitude: latitude || 0,
            height: height || 0
          });
        });
      }
    }
    
    console.log('自动填充区域坐标成功:', areaList.value);
    
    // 在地图上绘制区域
    if (cesiumRef.value && areaList.value.length > 0) {
      if (formData.value.areaType === "点目标") {
        cesiumRef.value.addPoint(areaList.value);
      } else if (formData.value.areaType === "区域目标" && areaList.value.length >= 3) {
        cesiumRef.value.addPolygon(areaList.value);
      }
    }
    
  } catch (error) {
    console.error('解析区域坐标数据失败:', error);
    ElMessage.error('区域坐标数据格式错误');
  }
}



function hasThreeUniqueCoordinates(areaList) {
  const seenCoordinates = new Set();
  let uniqueCoordinatesCount = 0;

  for (const area of areaList) {
    const { longitude, latitude } = area;
    const coordinateString = `${longitude},${latitude}`;

    if (seenCoordinates.has(coordinateString)) {
      continue; // 如果坐标点已存在，继续遍历下一个
    }

    seenCoordinates.add(coordinateString);
    uniqueCoordinatesCount++;

    if (uniqueCoordinatesCount >= 3) {
      return true; // 如果找到了三个不一样的坐标点，立即返回 true
    }
  }

  return false; // 如果没有找到三个不一样的坐标点，返回 false
}

// 添加选项数据
const tagsOptions = ref({
  taskTypeOptions: [
    { label: '侦察任务', value: 0 },
    { label: '攻防任务', value: 1 },
    { label: '打击任务', value: 2 }
  ],
  satelliteOptions: [
    { label: '卫星A', value: 'SAT_A' },
    { label: '卫星B', value: 'SAT_B' },
    { label: '卫星C', value: 'SAT_C' }
  ],
  payloadTypeOptions: [
    { label: '光学', value: 0 },
    { label: 'SAR', value: 1 },
    { label: '红外', value: 2 }
  ],
  workModeOptions: [
    { label: '模式1', value: 0 },
    { label: '模式2', value: 1 },
    { label: '模式3', value: 2 }
  ]
});



// 原有的接口调用逻辑已移动到可见性结果界面

// 简化的目标区域验证函数（只做基本验证，不阻止接口调用）
function validateTargetArea() {
  console.log('--- 验证目标区域 ---');
  console.log('当前区域类型:', formData.value.areaType);
  console.log('当前任务类型:', formData.value.taskType);
  console.log('当前区域列表:', areaList.value);

  // 攻防任务（任务类型1）跳过目标区域验证
  if (formData.value.taskType === 1) {
    console.log('攻防任务，跳过目标区域验证');
    return { valid: true, message: "" };
  }

  // 简化验证逻辑 - 只要有areaList就通过，具体验证交给后端
  if (areaList.value && areaList.value.length > 0) {
    console.log('存在区域列表，验证通过');
    return { valid: true, message: "" };
  }

  // 最后的保底验证
  console.log('无区域数据，但仍允许提交（交给后端验证）');
  return { valid: true, message: "" };
}

// 生成目标区域列表（按返回数据格式）
function generateTargetAreaList() {
  console.log(areaList.value, "areaList.value");
  const targetAreas = [];
  if (formData.value.areaType === "点目标") {
    // 点目标：支持多个点，但要过滤出有效的点
    const validPoints = areaList.value.filter(point =>
      point.longitude !== "" && point.longitude !== null && point.longitude !== undefined &&
      point.latitude !== "" && point.latitude !== null && point.latitude !== undefined
    );

    console.log("有效的点目标:", validPoints);

    if (validPoints.length > 0) {
      validPoints.forEach(point => {
        var item = {
          targetType: 0,
          area: [[parseFloat(point.longitude), parseFloat(point.latitude), parseFloat(point.height) || 0]]
        };
        targetAreas.push(item);
      });
    }
  } else if (formData.value.areaType === "区域目标") {
    // 区域目标：过滤出有效的点
    const validPoints = areaList.value.filter(point =>
      point.longitude !== "" && point.longitude !== null && point.longitude !== undefined &&
      point.latitude !== "" && point.latitude !== null && point.latitude !== undefined
    );

    console.log("有效的区域目标点:", validPoints);

    if (validPoints.length >= 3) {
      const coordinates = validPoints.map(point => [
        parseFloat(point.longitude),
        parseFloat(point.latitude),
        parseFloat(point.height) || 0
      ]);
      const item = {
        targetType: 1,
        area: coordinates
      };
      targetAreas.push(item);
    }
    console.log(targetAreas, "targetAreas");
  } else {
    const item = {
      targetType: 0,
      area: [[0, 0, 0]]
    };
    targetAreas.push(item);
  }

  console.log("最终生成的targetAreas:", targetAreas);
  return targetAreas;
}

function clearMapPoints() {
  cesiumRef.value.handleClear();
}

/** 查询参数列表 */
// function getSiteList(e) {
//   const params = {
//     Operator_id: useUserStore().id,
//     current_time: new Date().getTime(),
//     field_type: Number(e) - 1,
//   };
//   getCalibrationField(params)
//     .then((res) => {
//       if (res.code === 200) {
//         calibrationList.value = res.data;
//         calibrationList.value.forEach((item) => {
//           console.log(item, "遍历");
//           cesiumRef.value.addPolygon(item.field_area, item.field_id);
//           nextTick(() => {
//             cesiumRef.value.flyToCenter(item.field_area);
//           });
//         });
//       } else {
//         ElNotification.warning(res.msg);
//       }
//     })
//     .catch((e) => {
//       // ElNotification.error(e)
//     });
// }

function subRequestAccept() { }

// 原有的保存和任务规划逻辑已移动到可见性结果界面

// 添加加载查看数据的方法
function loadViewData(data) {
  console.log('加载查看数据:', data);
  console.log('卫星配置数据:', data.satelliteConfig);

  try {
    // 加载基本表单数据，处理字符串和数字格式
    formData.value = {
      ...formData.value,
      taskId: data.taskId,
      taskType: parseInt(data.taskType) || 0,
      taskCount: data.taskCount || 1,
      taskTime: data.taskTime ? [
        new Date(data.taskTime.convertStartTime),
        new Date(data.taskTime.convertEndTime)
      ] : [],
      satelliteConfig: data.satelliteConfig || {
        satelliteType: [],
        satelliteName: []
      },
      // resolutionRange: parseInt(data.resolutionRange) || null,
      resolutionRange: data.resolutionRange || [5, 10],
      width: data.width || [10, 20],
      latestDeadline: data.latestDeadline ? getChinaTimestamp(data.latestDeadline) : null,
      elevatingAngleLimit: parseInt(data.elevatingAngleLimit) || null,
      sunElevatingAngleLimit: parseInt(data.sunElevatingAngleLimit) || null,
      priority: parseInt(data.priority) || 0,
      targetSatelliteList: data.targetSatelliteList || [], // 目标卫星列表
      submitTime: data.submitTime,
      createTime: data.createTime,
      updateTime: data.updateTime,
      status: data.status,
      targetAreaList: data.targetAreaList || {}
    };

    console.log('加载后的formData.satelliteConfig:', formData.value.satelliteConfig);

    // 加载目标区域数据
    if (data.targetAreaList && data.targetAreaList.length > 0) {
      const targetArea = data.targetAreaList[0];
      const targetType = parseInt(targetArea.targetType);

      if (targetType === 0) {
        // 点目标：支持多个点
        formData.value.areaType = "点目标";
        if (targetArea.area && targetArea.area.length > 0) {
          areaList.value = targetArea.area.map(coord => ({
            longitude: coord[0],
            latitude: coord[1],
            height: coord[2] || 0
          }));
        }
      } else {
        // 区域目标
        formData.value.areaType = "区域目标";
        areaList.value = targetArea.area.map(coord => ({
          longitude: coord[0],
          latitude: coord[1],
          height: coord[2] || 0
        }));
      }

      // watch 会自动处理地图绘制
      // 查看模式下自动飞行到目标区域
      if (isViewMode.value) {
        setTimeout(() => {
          flyToAllTargets();
        }, 1500); // 等待地图元素添加完成
      }
    }
  } catch (error) {
    console.error('加载查看数据失败:', error);
    ElMessage.error('数据加载失败');
  }
}

// 修改 handleCancel 方法
const handleCancel = () => {
  if (isViewMode.value) {
    // 查看模式返回时清空数据和 store
    resetFormData();
    viewDataStore.clearViewData();
  }
  router.push('/system4/payloadMissionPlan/requirements');
};

// 飞行到指定坐标点
function flyToPoint(point, index) {
  if (!cesiumRef.value || !point.longitude || !point.latitude) return;

  console.log(`飞行到第${index + 1}个坐标点:`, point);

  // 如果是点目标且只有一个点，飞到该点
  if (formData.value.areaType === '点目标' && validAreaPoints.value.length === 1) {
    cesiumRef.value.flyToSinglePoint(point);
  } else {
    // 多个点的情况，飞到所有点的范围
    cesiumRef.value.flyToAllPoints(validAreaPoints.value, index);
  }
}

// 飞行查看所有目标区域
function flyToAllTargets() {
  if (!cesiumRef.value || validAreaPoints.value.length === 0) return;

  console.log('飞行查看所有目标区域:', validAreaPoints.value);

  if (validAreaPoints.value.length === 1) {
    cesiumRef.value.flyToSinglePoint(validAreaPoints.value[0]);
  } else {
    cesiumRef.value.flyToAllPoints(validAreaPoints.value);
  }
}

// 添加重置表单数据的方法
function resetFormData() {
  formData.value = {
    type: 0,
    areaType: "区域目标",
    inputType: "Draw",
    taskId: null,
    taskType: 0,
    taskCount: 1,
    taskTime: [],
    satelliteConfig: {
      satelliteType: [],
      satelliteName: []
    },
    resolutionRange: [5, 10],
    timeRange: [],
    satellite: [],
    payloadType: [],
    workMode: [],
    latestDeadline: null,
    width: [10, 20], // 修复：添加width字段初始化
    elevatingAngleLimit: null,
    sunElevatingAngleLimit: null,
    priority: 0,
    targetSatelliteList: [], // 目标卫星列表
    submitTime: null,
    createTime: null,
    updateTime: null,
    status: null
  };

  areaList.value = [
    { longitude: "", latitude: "", height: "" },
    { longitude: "", latitude: "", height: "" },
    { longitude: "", latitude: "", height: "" }
  ];

  // watch 会自动清空地图
}

onUnmounted(() => {
  // clearInterval(intervalInstance); // 移除未使用的变量
  // 页面离开时强制保存缓存，无论当前模式
  saveFormCache();
  console.log('页面离开，缓存数据已保存');
});

// 可见性计算结果已移动到可见性结果界面处理



// 原有的结果处理方法已移动到可见性结果界面

const handlePayloadTypeChange = async () => {
  // 获取当前选中的载荷类型
  const currentValue = formData.value.satelliteConfig.satelliteType;

  if (currentValue && currentValue.length > 0) {
    var params = {
      participationStatus: 1,
      payloadType: currentValue.toString(),
    }
  } else {
    var params = {
      participationStatus: 1,
      payloadType: '',
    }
  }

  console.log('请求载荷类型参数:', params);

  try {
    // 先获取第一页数据，了解总数
    const firstPageParams = {
      ...params,
      pageNum: 1,
      pageSize: 10
    };

    const firstResponse = await getSatelliteList(firstPageParams);

    if (firstResponse.code === 200) {
      let allSatellites = [...firstResponse.rows];
      const total = firstResponse.total;
      const pageSize = 10;
      const totalPages = Math.ceil(total / pageSize);

      console.log(`总数据量: ${total}, 总页数: ${totalPages}`);

      // 如果总页数大于1，继续获取剩余页面
      if (totalPages > 1) {
        const remainingPages = [];

        // 创建剩余页面的请求
        for (let page = 2; page <= totalPages; page++) {
          remainingPages.push(
            getSatelliteList({
              ...params,
              pageNum: page,
              pageSize: pageSize
            })
          );
        }

        // 并发请求所有剩余页面
        const remainingResponses = await Promise.all(remainingPages);

        // 合并所有页面的数据
        remainingResponses.forEach(response => {
          if (response.code === 200) {
            allSatellites = allSatellites.concat(response.rows);
          }
        });
      }

      // 提取所有卫星名称并去重
      satelliteList.value = [...new Set(allSatellites.map(item => item.satelliteName))];
      console.log(`更新卫星列表，共${satelliteList.value.length}个卫星:`, satelliteList.value);
    } else {
      console.error('获取卫星列表失败:', firstResponse.msg);
      satelliteList.value = [];
    }
  } catch (err) {
    console.error('获取卫星列表失败:', err.msg || err.message);
    satelliteList.value = [];
  }
};

const handleGetPoints = async () => {
  get100Points().then(res => {
    if (res.code === 200) {
      areaList.value = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  })
}

// 监听表单数据变化，自动保存缓存
watch(formData, (newValue) => {
  if (route.query.mode === 'add') {
    // 使用防抖避免频繁保存
    clearTimeout(window.reqCacheTimer);
    window.reqCacheTimer = setTimeout(() => {
      saveFormCache();
    }, 500);
  }
}, { deep: true });
// 监听表区域列表数据变化，自动保存缓存
// watch(areaList, (newValue) => {
//   if (route.query.mode==='add') {
//     // 使用防抖避免频繁保存
//     clearTimeout(window.areaCacheTimer);
//     window.areaCacheTimer = setTimeout(() => {
//       saveFormCache();
//     }, 500);
//   }
// }, { deep: true });

// 导入JSON文件功能
const handleImportJson = () => {
  // 触发文件选择器
  fileInputRef.value?.click();
};

const handleJsonFileChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;

  // 检查文件类型
  if (!file.name.endsWith('.json')) {
    ElMessage.error('请选择JSON格式的文件');
    return;
  }

  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const jsonData = JSON.parse(e.target.result);
      importFormData(jsonData);
    } catch (error) {
      console.error('JSON文件解析失败:', error);
      ElMessage.error('JSON文件格式错误，请检查文件内容');
    }
  };
  
  reader.onerror = () => {
    ElMessage.error('文件读取失败');
  };
  
  reader.readAsText(file);
  
  // 重置文件输入框，允许重复选择同一文件
  event.target.value = '';
};

const importFormData = (jsonData) => {
  try {
    // 验证JSON数据结构
    if (!jsonData || typeof jsonData !== 'object') {
      throw new Error('无效的JSON数据');
    }

    // 导入基本表单数据
    if (jsonData.taskType !== undefined) {
      formData.value.taskType = parseInt(jsonData.taskType) || 0;
    }
    if (jsonData.taskCount !== undefined) {
      formData.value.taskCount = parseInt(jsonData.taskCount) || 1;
    }
    if (jsonData.priority !== undefined) {
      formData.value.priority = jsonData.priority?.toString() || "0";
    }
    if (jsonData.latestDeadline !== undefined) {
      // 处理时间戳格式的日期
      if (typeof jsonData.latestDeadline === 'string' && !isNaN(jsonData.latestDeadline)) {
        formData.value.latestDeadline = new Date(parseInt(jsonData.latestDeadline)).toISOString();
      } else {
        formData.value.latestDeadline = jsonData.latestDeadline;
      }
    }
    if (jsonData.elevatingAngleLimit !== undefined) {
      formData.value.elevatingAngleLimit = jsonData.elevatingAngleLimit;
    }
    if (jsonData.sunElevatingAngleLimit !== undefined) {
      formData.value.sunElevatingAngleLimit = jsonData.sunElevatingAngleLimit;
    }
    if (jsonData.width !== undefined) {
      // 当width为null时转换为空数组
      formData.value.width = jsonData.width === null ? [null,null] : jsonData.width;
    }
    if (jsonData.resolutionRange !== undefined) {
      // 当resolutionRange为null时转换为空数组，否则确保是有效的数组
      if (jsonData.resolutionRange === null) {
        formData.value.resolutionRange = [null,null];
      } else if (Array.isArray(jsonData.resolutionRange) && jsonData.resolutionRange.length >= 2) {
        formData.value.resolutionRange = jsonData.resolutionRange;
      } else {
        formData.value.resolutionRange = [5, 10]; // 使用默认值
        console.warn('导入的resolutionRange格式无效，使用默认值');
      }
    }

    // 导入卫星配置
    if (jsonData.satelliteConfig) {
      if (jsonData.satelliteConfig.satelliteType) {
        formData.value.satelliteConfig.satelliteType = Array.isArray(jsonData.satelliteConfig.satelliteType) 
          ? jsonData.satelliteConfig.satelliteType.map(item => parseInt(item) || item)
          : [];
      } else {
        formData.value.satelliteConfig.satelliteType = [];
      }
      if (jsonData.satelliteConfig.satelliteName) {
        formData.value.satelliteConfig.satelliteName = Array.isArray(jsonData.satelliteConfig.satelliteName)
          ? jsonData.satelliteConfig.satelliteName
          : [];
      } else {
        formData.value.satelliteConfig.satelliteName = [];
      }
    }


    // 导入目标卫星列表
    if (jsonData.targetSatelliteNameList !== undefined) {
      // 当targetSatelliteNameList为null时转换为空数组
      formData.value.targetSatelliteList = jsonData.targetSatelliteNameList === null ? [] : jsonData.targetSatelliteNameList;
    }

    // 导入等效配置列表
    if (jsonData.equivalentConfigList && Array.isArray(jsonData.equivalentConfigList)) {
      console.log('导入的等效配置数据:', jsonData.equivalentConfigList);
    }

    // 导入任务时间范围
    if (jsonData.taskTime && jsonData.taskTime.startTime && jsonData.taskTime.endTime) {
      formData.value.taskTime = [
        new Date(parseInt(jsonData.taskTime.startTime)).toISOString(),
        new Date(parseInt(jsonData.taskTime.endTime)).toISOString()
      ];
    }

    // 导入目标区域列表
    if (jsonData.targetAreaList !== undefined) {
      if (jsonData.targetAreaList === null) {
        formData.value.targetAreaList = [];
      } else if (Array.isArray(jsonData.targetAreaList)) {
        // 转换目标区域数据格式
        formData.value.targetAreaList = jsonData.targetAreaList.map(area => ({
          targetType: area.targetType?.toString() || "0",
          area: area.area || [],
          visibleTimeRange: area.visibleTimeRange || null
        }));
        console.log('导入的目标区域数据:', formData.value.targetAreaList);
        
        // 将targetAreaList映射到areaList用于界面显示
        if (formData.value.targetAreaList.length > 0) {
          // 分离点目标和区域目标
          const pointTargets = formData.value.targetAreaList.filter(area => area.targetType === "0");
          const areaTargets = formData.value.targetAreaList.filter(area => area.targetType === "1");
          
          // 处理点目标：导入全部点目标列表
          if (pointTargets.length > 0) {
            formData.value.areaType = "点目标";
            
            // 将所有点目标的坐标合并到areaList
            areaList.value = [];
            pointTargets.forEach(target => {
              if (target.area && Array.isArray(target.area)) {
                target.area.forEach(coord => {
                  areaList.value.push({
                    longitude: coord[0] || "",
                    latitude: coord[1] || "",
                    height: coord[2] || ""
                  });
                });
              }
            });
            
            // 确保至少有一个点
            if (areaList.value.length === 0) {
              areaList.value = [{ longitude: "", latitude: "", height: "" }];
            }
          }
          // 处理区域目标：只导入第一个区域目标
          else if (areaTargets.length > 0) {
            const firstArea = areaTargets[0];
            formData.value.areaType = "区域目标";
            
            if (firstArea.area && Array.isArray(firstArea.area)) {
              // 将坐标数据映射到areaList
              areaList.value = firstArea.area.map(coord => ({
                longitude: coord[0] || "",
                latitude: coord[1] || "",
                height: coord[2] || ""
              }));
              
              // 区域目标需要至少3个点，补充空点
              if (areaList.value.length < 3) {
                while (areaList.value.length < 3) {
                  areaList.value.push({ longitude: "", latitude: "", height: "" });
                }
              }
            }
          }
          
          console.log('映射后的areaList:', areaList.value);
        }
      } else {
        console.warn('导入的targetAreaList格式无效，使用默认空数组');
        formData.value.targetAreaList = [];
      }
    }

    // 处理其他可能为null的数组字段
    if (jsonData.payloadType !== undefined) {
      formData.value.payloadType = jsonData.payloadType === null ? [] : jsonData.payloadType;
    }
    if (jsonData.workMode !== undefined) {
      formData.value.workMode = jsonData.workMode === null ? [] : jsonData.workMode;
    }
    if (jsonData.timeRange !== undefined) {
      formData.value.timeRange = jsonData.timeRange === null ? [] : jsonData.timeRange;
    }

    ElMessage.success('表单数据导入成功');
    
    // 保存缓存
    saveFormCache();
    
  } catch (error) {
    console.error('导入表单数据失败:', error);
    ElMessage.error('导入表单数据失败: ' + error.message);
  }
};
</script>

<style lang="scss" scoped>
.map {
  position: relative;
  width: 100%;
  height: calc(100vh - 150px - 2rem);
  border: 2px solid rgb(176, 194, 255);
  border-radius: 4px;
}

.action-button {
  position: absolute;
  width: fit-content;
  height: 40px;
  line-height: 40px;
  right: 0px;
  bottom: 0px;
}

a:hover {
  text-decoration: underline;
}

.area-list-content {
  position: relative;
  width: 100%;
  height: calc(100vh - 670px - 2rem);
  overflow-y: auto;
}

.upload-btn {
  display: inline-block;
  padding: 10px 20px;
  margin: 10px;
  background-color: #007bff;
  color: white;
  text-align: center;
  cursor: pointer;
  border-radius: 4px;
}

.file-input {
  display: none;
  /* 隐藏原生的文件上传输入 */
}

.satellite-config-container {
  width: 90%;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
}

.satellite-config-item {
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background-color: #fafafa;

  &:last-child {
    margin-bottom: 0;
  }
}

.config-header {
  font-weight: bold;
  margin-bottom: 10px;
  color: #409eff;
}

.config-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  color: white !important;

  label {
    min-width: 80px;
    margin-right: 10px;
    font-size: 14px;
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.noClear .el-picker-panel__footer .el-button.el-picker-panel__link-btn.el-button--text.el-button--mini {
  display: none;
}

.point-input-container {
  display: flex;
  align-items: center;
  gap: 0;
  width: 100%;
}

.point-input {
  width: 100%;
}

.satellite-config-display {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid rgba(33, 233, 252, 0.2);
  border-radius: 4px;
  padding: 10px;
  background-color: rgba(8, 135, 247, 0.05);
}

.config-display-item {
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid rgba(33, 233, 252, 0.2);
  border-radius: 4px;
  background-color: rgba(20, 64, 102, 0.1);

  &:last-child {
    margin-bottom: 0;
  }
}

.config-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #409eff;
  font-size: 14px;
}

.config-detail {
  margin-bottom: 5px;
  font-size: 13px;
  line-height: 1.5;
  color: white !important;

  &:last-child {
    margin-bottom: 0;
  }

  strong {
    color: #303133;
    min-width: 80px;
    display: inline-block;
  }
}

.target-area-display {
  border: 1px solid rgba(33, 233, 252, 0.2);
  border-radius: 4px;
  padding: 15px;
  background-color: rgba(20, 64, 102, 0.05);
}

.area-type-info {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;

  span {
    color: #303133;
  }

  strong {
    color: #409eff;
  }
}

.area-points-list {
  max-height: 400px;
  overflow-y: auto;
}

.area-point-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 15px;
  margin-bottom: 10px;
  border: 1px solid rgba(33, 233, 252, 0.2);
  border-radius: 4px;
  background-color: rgba(20, 64, 102, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
    transform: translateY(-2px);
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.point-title {
  font-weight: bold;
  color: #409eff;
  font-size: 14px;
  min-width: 120px;
}

.point-coords {
  flex: 1;
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #606266;

  span {
    white-space: nowrap;
  }

  strong {
    color: #303133;
  }
}

.point-action {
  min-width: 80px;
  text-align: right;
}

// 表单整体背景样式
.req-form {
  position: absolute;
  background: rgba(20, 64, 102, 0.1) !important;
  // background: rgba(0, 133, 249, 0.927) !important;
  overflow-y: hidden;
  // height: 100%;

  // 调整表单项的背景色
  .el-form-item {
    background: rgba(20, 64, 102, 0.05);
    margin-bottom: 8px;
    border: 1px solid rgba(33, 233, 252, 0.1);
  }

  .el-input {
    width: 100%;
  }

  .el-select {
    width: 100%;
  }

  .el-input-number {
    width: calc((100% - 115px)/2);
  }

  .el-date-picker {
    width: 100%;
  }


}

// 可见性结果对话框样式
.visibility-result-container {
  max-height: 70vh;
  overflow-y: auto;

  .result-section {
    margin-bottom: 30px;

    .section-title {
      color: #409eff;
      font-size: 18px;
      font-weight: bold;
      margin-bottom: 15px;
      border-bottom: 2px solid #e4e7ed;
      padding-bottom: 8px;
    }
  }

  .target-area-info {
    background-color: #f8f9fa;
    padding: 15px;
    border-radius: 6px;
    border: 1px solid #e9ecef;

    .area-item {
      margin-bottom: 15px;

      &:last-child {
        margin-bottom: 0;
      }

      p {
        margin: 0 0 8px 0;
        font-weight: 600;
        color: #495057;
      }
    }

    .coordinates-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .coord-item {
        background-color: #e9ecef;
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 12px;
        font-family: monospace;
        color: #495057;
      }
    }
  }

  .visibility-results {
    .result-item {
      background-color: #fafbfc;
      border: 1px solid #e1e4e8;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;

      &:last-child {
        margin-bottom: 0;
      }

      .result-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;

        h4 {
          margin: 0;
          color: #24292e;
          font-size: 16px;
          font-weight: 600;
        }
      }

      .strip-coordinates {
        margin-top: 15px;
        background-color: #f6f8fa;
        padding: 15px;
        border-radius: 6px;
        border: 1px solid #e1e4e8;

        p {
          margin: 0 0 10px 0;
          font-weight: 600;
          color: #24292e;
        }

        .coordinates-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 8px;

          .coord-item {
            background-color: white;
            padding: 6px 10px;
            border-radius: 4px;
            font-size: 12px;
            font-family: monospace;
            color: #586069;
            border: 1px solid #e1e4e8;
          }
        }
      }
    }
  }


}

// 任务规划结果弹窗样式
.el-dialog {
  .el-dialog__body {
    max-height: 80vh;
    overflow-y: auto;
  }

  .el-form {
    .el-form-item {
      margin-bottom: 4px !important;
    }
  }
}

// 全局加载状态样式
.global-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(2px);

  .loading-content {
    background: #35444d47;
    padding: 30px 40px;
    text-align: center;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
    border: 1px solid #00e3d0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;

    .loading-icon {
      font-size: 40px;
      color: #00e3d0;
      animation: spin 1s linear infinite;
      margin-bottom: 0;
      display: block;
    }

    .loading-text {
      font-size: 16px;
      color: #00e3d0;
      font-weight: 500;
      min-width: 150px;
      text-align: left;
    }
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

// 目标卫星显示样式
.target-satellite-display {
  border: 1px solid rgba(33, 233, 252, 0.2);
  border-radius: 4px;
  padding: 15px;
  background-color: rgba(20, 64, 102, 0.05);
}

.satellite-type-info {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;

  span {
    color: #303133;
  }

  strong {
    color: #409eff;
  }
}

.satellite-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.satellite-item {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid rgba(33, 233, 252, 0.2);
  border-radius: 4px;
  background-color: rgba(20, 64, 102, 0.1);
  transition: all 0.3s ease;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
  }
}

.satellite-name {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}
</style>