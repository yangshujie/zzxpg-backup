<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px" class="search-form">
      <el-form-item label="任务名称" prop="taskName">
        <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="等级" prop="grade">
        <el-select v-model="queryParams.grade" placeholder="请选择" clearable style="width: 100px">
          <el-option v-for="item in gradeOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="taskRunStatus">
        <el-select v-model="queryParams.taskRunStatus" placeholder="请选择" clearable style="width: 130px">
          <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-button type="primary" plain icon="VideoPlay" @click="openRunDialog">发起计算</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleBatchDelete">删除</el-button>
        <el-button type="warning" plain icon="Download" @click="handleExport">导出</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="任务名称" prop="taskName" min-width="200" show-overflow-tooltip />
      <el-table-column label="指标体系" prop="indicatorSystemName" min-width="160" show-overflow-tooltip />
      <el-table-column label="流程模板" prop="templateName" min-width="150" show-overflow-tooltip />
      <el-table-column label="综合得分" prop="score" width="90" align="center" />
      <el-table-column label="等级" prop="grade" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getGradeTagType(row.grade)" size="small">{{ formatGradeToChinese(row.grade) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="任务状态" prop="taskRunStatus" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTaskStatusTagType(row.taskRunStatus)" size="small">{{ getTaskStatusLabel(row.taskRunStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" prop="taskProgressPercent" width="180" align="center">
        <template #default="{ row }">
          <div class="progress-cell">
            <el-progress
              :percentage="row.taskProgressPercent || 0"
              :status="getProgressStatus(row.taskRunStatus)"
              :stroke-width="10"
              style="width: 112px; flex-shrink: 0;"
            />
            <el-button
              v-if="row.taskId"
              link
              type="primary"
              size="small"
              class="log-btn"
              @click="openTaskDetail(row)"
            >
              日志
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" min-width="168" align="center" />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-cell">
            <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 发起计算对话框 -->
    <el-dialog v-model="runDialogVisible" title="发起评估计算" width="560px" append-to-body :close-on-click-modal="false">
      <el-form ref="runFormRef" :model="runForm" :rules="runRules" label-width="100px">
        <el-form-item label="任务名称">
          <el-input v-model="runForm.taskName" placeholder="可选，不填则按模板名称自动生成" clearable />
        </el-form-item>
        <el-form-item label="流程模板" prop="calcFlowTemplateId">
          <el-select
            v-model="runForm.calcFlowTemplateId"
            placeholder="请选择已发布或测试中的流程模板"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="t in runnableTemplates"
              :key="t.id"
              :label="`${t.templateName}（${t.versionNo || '-'} / ${templateStatusLabel(t.status)}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="指标体系" prop="indicatorSystemId">
          <el-select
            v-model="runForm.indicatorSystemId"
            placeholder="请选择本次计算使用的指标体系"
            filterable
            clearable
            remote
            :remote-method="remoteIndicator"
            :loading="indicatorLoading"
            style="width: 100%"
          >
            <el-option
              v-for="s in indicatorOptions"
              :key="s.indicatorSystemId"
              :label="s.indicatorSystemName"
              :value="s.indicatorSystemId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="runLoading" @click="submitRun">发起计算</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="评估结果详情" width="1200px" append-to-body class="result-detail-dialog">
      <!-- 顶部概览卡片 -->
      <div class="overview-cards">
        <div class="overview-card total-score" :class="getScoreClass(currentRow.score)">
          <div class="card-icon">
            <el-icon :size="32"><TrendCharts /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-label">综合得分</div>
            <div class="card-value">{{ currentRow.score !== null ? currentRow.score : '—' }}</div>
            <div class="card-grade">
              <el-tag :type="getGradeTagType(currentRow.grade)" size="large" effect="dark">{{ formatGradeToChinese(currentRow.grade) }}</el-tag>
            </div>
          </div>
          <div class="card-bg-icon">
            <el-icon :size="80"><TrendCharts /></el-icon>
          </div>
        </div>

        <div class="overview-card indicator-system" v-if="indicatorTreeData.length > 0">
          <div class="card-icon">
            <el-icon :size="32"><Collection /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-label">指标体系</div>
            <div class="card-value text-ellipsis" :title="currentRow.indicatorSystemName">{{ currentRow.indicatorSystemName || '—' }}</div>
            <div class="card-tags">
              <el-tag size="small" type="info">{{ indicatorTreeData[0]?.children?.length || 0 }} 个一级指标</el-tag>
              <el-tag size="small" type="success">{{ getTotalNodeCount(indicatorTreeData) }} 个总节点</el-tag>
            </div>
          </div>
          <div class="card-bg-icon">
            <el-icon :size="80"><Collection /></el-icon>
          </div>
        </div>

        <div class="overview-card task-info">
          <div class="card-icon">
            <el-icon :size="32"><DocumentChecked /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-label">评估任务</div>
            <div class="card-value text-ellipsis" :title="currentRow.taskName">{{ currentRow.taskName || '—' }}</div>
            <div class="card-tags">
              <el-tag size="small" type="primary">{{ parseTime(currentRow.createTime) }}</el-tag>
            </div>
          </div>
          <div class="card-bg-icon">
            <el-icon :size="80"><DocumentChecked /></el-icon>
          </div>
        </div>

        <div class="overview-card quick-stats">
          <div class="card-icon">
            <el-icon :size="32"><DataAnalysis /></el-icon>
          </div>
          <div class="card-content">
            <div class="card-label">评估统计</div>
            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-value" :class="getScoreClass(currentRow.score)">{{ currentRow.dimensionList?.filter(d => Number(d.value) >= 90).length || 0 }}</span>
                <span class="stat-label">优秀指标</span>
              </div>
              <div class="stat-item">
                <span class="stat-value" :class="getScoreClass(currentRow.score)">{{ currentRow.dimensionList?.length || 0 }}</span>
                <span class="stat-label">评估维度</span>
              </div>
            </div>
          </div>
          <div class="card-bg-icon">
            <el-icon :size="80"><DataAnalysis /></el-icon>
          </div>
        </div>
      </div>

      <!-- 评分标准说明（从模板配置动态渲染） -->
      <div class="grade-standard-section">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            <span class="grade-standard-title">评分标准（模板配置）：</span>
            <span v-for="(lv, idx) in currentScoreLevels" :key="idx" class="grade-standard-item">
              <el-tag size="small" :type="gradeIndexToTagType(idx)">{{ lv.name }}</el-tag>
              ≥{{ lv.threshold }}分
            </span>
            <span class="grade-standard-item">
              <el-tag size="small" type="info">未达标</el-tag>
              {{ currentScoreLevels[currentScoreLevels.length - 1]?.threshold }}分以下
            </span>
          </template>
        </el-alert>
      </div>

      <!-- 左右布局：左侧树形 + 右侧详情 -->
      <div class="detail-main-layout">
        <div class="left-tree-panel">
          <div class="panel-header">
            <span class="panel-title">指标层级</span>
            <el-button link type="primary" size="small" @click="expandAllNodes">
              {{ isAllExpanded ? '收起全部' : '展开全部' }}
            </el-button>
          </div>
          <div class="tree-wrapper">
            <el-tree
              ref="indicatorTreeRef"
              :data="indicatorTreeData"
              :props="treeProps"
              node-key="uid"
              highlight-current
              :default-expanded-keys="defaultExpandedKeys"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="custom-tree-node" :class="{ 'is-leaf': !data.children || data.children.length === 0 }">
                  <span class="node-label" :title="node.label">{{ node.label }}</span>
                  <span class="node-score" v-if="data.calculatedScore !== undefined">
                    <el-tag :type="getScoreTagType(data.calculatedScore)" size="small" effect="plain">
                      {{ data.calculatedScore }}
                    </el-tag>
                  </span>
                </div>
              </template>
            </el-tree>
          </div>
        </div>

        <div class="right-detail-panel">
          <div class="panel-header">
            <span class="panel-title">评估详情</span>
            <span class="panel-subtitle" v-if="selectedNode?.label">{{ selectedNode.label }}</span>
          </div>

          <div class="detail-content" v-if="selectedNode">
            <div class="node-info-cards">
              <div class="info-card">
                <div class="info-icon"><el-icon><Document /></el-icon></div>
                <div class="info-content">
                  <div class="info-label">节点名称</div>
                  <div class="info-value" :title="selectedNode.label">{{ selectedNode.label || '—' }}</div>
                </div>
              </div>
              <div class="info-card">
                <div class="info-icon"><el-icon><Collection /></el-icon></div>
                <div class="info-content">
                  <div class="info-label">指标类型</div>
                  <div class="info-value">
                    <el-tag size="small" type="info">{{ selectedNode.indicatorType || '—' }}</el-tag>
                  </div>
                </div>
              </div>
              <div class="info-card score-card">
                <div class="info-icon"><el-icon><TrendCharts /></el-icon></div>
                <div class="info-content">
                  <div class="info-label">计算得分</div>
                  <div class="info-value score-value" :class="getScoreClass(selectedNode.calculatedScore)">
                    {{ selectedNode.calculatedScore !== undefined ? selectedNode.calculatedScore : '—' }}
                  </div>
                </div>
              </div>
              <div class="info-card grade-card">
                <div class="info-icon"><el-icon><Medal /></el-icon></div>
                <div class="info-content">
                  <div class="info-label">评级结果</div>
                  <div class="info-value">
                    <el-tag :type="getScoreTagType(selectedNode.calculatedScore)" size="large" effect="dark">
                      {{ formatScoreToGradeChinese(selectedNode.calculatedScore) }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>

            <!-- 得分可视化 -->
            <div class="score-visualization" v-if="selectedNode.calculatedScore !== undefined">
              <div class="viz-title">得分可视化</div>
              <div class="viz-content">
                <div class="progress-section">
                  <div class="progress-label">
                    <span>得分进度</span>
                    <span class="progress-score">{{ selectedNode.calculatedScore }} / 100</span>
                  </div>
                  <el-progress
                    :percentage="selectedNode.calculatedScore"
                    :status="getProgressStatusByScore(selectedNode.calculatedScore)"
                    :stroke-width="20"
                    :color="getScoreProgressColor(selectedNode.calculatedScore)"
                    class="score-progress"
                  />
                </div>
                <div class="gauge-section">
                  <div class="gauge-wrapper">
                    <el-progress
                      type="dashboard"
                      :percentage="selectedNode.calculatedScore"
                      :color="getScoreProgressColor(selectedNode.calculatedScore)"
                      :stroke-width="10"
                    />
                    <div class="gauge-label">{{ formatScoreToGradeChinese(selectedNode.calculatedScore) }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 子节点权重分配 -->
            <div class="children-weight-section" v-if="selectedNode.children && selectedNode.children.length > 0">
              <div class="section-header">
                <div class="section-title">子指标权重分配</div>
                <div class="weight-sum-info" :class="{ 'is-valid': Math.abs(getChildrenWeightSum(selectedNode.children) - 1) < 0.01 }">
                  权重之和：{{ (getChildrenWeightSum(selectedNode.children) * 100).toFixed(1) }}%
                </div>
              </div>
              <el-row :gutter="12" class="weight-cards-row">
                <el-col v-for="child in selectedNode.children" :key="child.uid" :xs="24" :sm="12" :md="8" :lg="8">
                  <div class="weight-card" :class="{ 'is-excellent': child.calculatedScore >= 90, 'is-good': child.calculatedScore >= 80 && child.calculatedScore < 90 }">
                    <div class="weight-card-header">
                      <div class="weight-card-title" :title="child.label">{{ child.label || '未命名' }}</div>
                      <el-tag :type="getScoreTagType(child.calculatedScore)" size="small" effect="dark">
                        {{ formatScoreToGradeChinese(child.calculatedScore) }}
                      </el-tag>
                    </div>
                    <div class="weight-card-body">
                      <div class="weight-value-row">
                        <div class="weight-item">
                          <div class="weight-label">权重占比</div>
                          <div class="weight-value">{{ formatWeight(child.weight) }}</div>
                        </div>
                        <div class="weight-item">
                          <div class="weight-label">计算得分</div>
                          <div class="score-value" :class="getScoreClass(child.calculatedScore)">
                            {{ child.calculatedScore !== undefined ? child.calculatedScore : '—' }}
                          </div>
                        </div>
                      </div>
                      <div class="weight-bar-section">
                        <div class="weight-bar-wrapper">
                          <div class="weight-bar-fill" :style="{ width: ((child.weight || 0) * 100) + '%' }"></div>
                        </div>
                        <span class="weight-percent">{{ ((child.weight || 0) * 100).toFixed(1) }}%</span>
                      </div>
                      <div class="score-bar-section">
                        <el-progress :percentage="child.calculatedScore || 0" :color="getScoreProgressColor(child.calculatedScore)" :stroke-width="6" :show-text="false" />
                        <span class="score-percent">{{ child.calculatedScore !== undefined ? child.calculatedScore + '分' : '—' }}</span>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <!-- 计算规则信息（叶子节点） -->
            <div class="compute-rule-section" v-if="(!selectedNode.children || selectedNode.children.length === 0) && hasComputeRuleData(selectedNode)">
              <div class="section-header">
                <div class="section-title"><el-icon><Setting /></el-icon><span>计算规则</span></div>
              </div>
              <div class="rule-content">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="算法类型">{{ selectedNode.computeRule?.algorithmName || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="算法编码">{{ selectedNode.computeRule?.algorithmCode || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="算法描述" :span="2" v-if="selectedNode.computeRule?.description">{{ selectedNode.computeRule.description }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </div>

            <!-- 原始值信息（叶子节点） -->
            <div class="raw-value-section" v-if="(!selectedNode.children || selectedNode.children.length === 0) && hasRawValueData(selectedNode)">
              <div class="section-header">
                <div class="section-title"><el-icon><DataLine /></el-icon><span>指标数值信息</span></div>
              </div>
              <div class="value-content">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="原始值"><span class="highlight-value">{{ selectedNode.rawValue }}</span></el-descriptions-item>
                  <el-descriptions-item label="标准化值"><span class="highlight-value">{{ selectedNode.normalizedValue !== undefined ? selectedNode.normalizedValue : '—' }}</span></el-descriptions-item>
                  <el-descriptions-item label="单位">{{ selectedNode.unit || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="阈值范围">{{ selectedNode.threshold || '—' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
          </div>

          <el-empty v-else description="请点击左侧指标节点查看详情" />
        </div>
      </div>

      <!-- 评估结论 -->
      <div class="conclusion-section" v-if="currentRow.conclusion || currentRow.suggestion">
        <div class="section-title">评估结论与建议</div>
        <el-row :gutter="20">
          <el-col :span="12" v-if="currentRow.conclusion">
            <el-card shadow="hover" class="conclusion-card">
              <template #header>
                <div class="card-header"><el-icon><InfoFilled /></el-icon><span>关键结论</span></div>
              </template>
              <div class="conclusion-content">{{ currentRow.conclusion }}</div>
            </el-card>
          </el-col>
          <el-col :span="12" v-if="currentRow.suggestion">
            <el-card shadow="hover" class="suggestion-card">
              <template #header>
                <div class="card-header"><el-icon><WarningFilled /></el-icon><span>整改建议</span></div>
              </template>
              <div class="suggestion-content">{{ currentRow.suggestion }}</div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 报告文件 -->
      <div class="report-section" v-if="currentRow.reportUrl">
        <div class="section-title">评估报告</div>
        <div class="report-actions">
          <el-button type="primary" :icon="View" @click="openReportPreviewV2(currentRow)">预览报告</el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="viewDialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 报告预览弹窗 -->
    <el-dialog
      v-model="reportPreviewVisible"
      title="报告预览"
      width="1100px"
      append-to-body
      destroy-on-close
      class="report-preview-dialog"
      @closed="closeReportPreview"
    >
      <div class="report-preview-body" v-loading="reportPreviewLoading">
        <iframe v-if="reportPreviewUrl" :src="reportPreviewUrl" class="report-preview-frame" frameborder="0" />
        <el-empty v-else description="暂无可预览的 PDF 报告" />
      </div>
      <template #footer>
        <el-dropdown trigger="click" @command="handleReportDownloadCommand">
          <el-button type="primary" :icon="Download">
            下载<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="pdf">下载PDF</el-dropdown-item>
              <el-dropdown-item command="word">下载Word</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button @click="reportPreviewVisible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 任务执行日志抽屉 -->
    <el-drawer v-model="taskDetailVisible" title="任务执行日志" size="600px" destroy-on-close>
      <template v-if="taskDetail">
        <el-descriptions :column="1" border size="small" class="mb12">
          <el-descriptions-item label="任务ID">{{ taskDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ taskDetail.taskName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getTaskStatusTagType(taskDetail.runStatus)" size="small">{{ getTaskStatusLabel(taskDetail.runStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前阶段">{{ taskDetail.currentStage || '—' }}</el-descriptions-item>
          <el-descriptions-item label="进度">
            <el-progress :percentage="taskDetail.progressPercent || 0" :status="getProgressStatus(taskDetail.runStatus)" :stroke-width="12" style="width: 200px; display: inline-block;" />
          </el-descriptions-item>
          <el-descriptions-item label="跟踪号">{{ taskDetail.logTraceId || '—' }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-section-title">阶段执行日志</div>
        <el-timeline v-if="taskDetail.stageLogs && taskDetail.stageLogs.length">
          <el-timeline-item
            v-for="log in taskDetail.stageLogs"
            :key="log.id"
            :timestamp="parseTime(log.finishTime || log.beginTime)"
            :type="getTimelineItemType(log.executeStatus)"
            placement="top"
          >
            <div class="stage-log-item">
              <div class="stage-header">
                <strong>{{ log.stageName }}</strong>
                <el-tag :type="getStageStatusType(log.executeStatus)" size="small" class="ml8">{{ getStageStatusLabel(log.executeStatus) }}</el-tag>
              </div>
              <div v-if="log.outputSummary" class="stage-output">
                <div class="output-label">输出摘要：</div>
                <div class="output-content">{{ log.outputSummary }}</div>
              </div>
              <div v-if="log.errorMessage" class="stage-error">
                <div class="error-label">错误信息：</div>
                <div class="error-content">{{ log.errorMessage }}</div>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无阶段日志" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { View, Download, ArrowDown, InfoFilled, WarningFilled, TrendCharts, Collection, DocumentChecked, DataAnalysis, Document, Medal, Setting, DataLine } from '@element-plus/icons-vue'
import {
  listEvalResult,
  getEvalResult,
  getEvalResultReportPreviewUrl,
  getEvalResultReportLinks,
  delEvalResult,
} from '@/api/zhpg/evalResult'
import { listCalcFlow } from '@/api/zhpg/calcFlow'
import { getCalcTask, runCalcTask } from '@/api/zhpg/calcTask'
import { selectIndicatorSystem, getIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import { parseIndicatorTreeToForest } from '@/utils/zhpgIndicatorTreeJson'

const { proxy } = getCurrentInstance()

// ==================== 列表状态 ====================
const loading = ref(false)
const showSearch = ref(true)
const tableData = ref([])
const total = ref(0)
const ids = ref([])
const multiple = ref(true)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  taskName: '',
  grade: '',
  taskRunStatus: ''
})

// ==================== 评分等级工具 ====================
const DEFAULT_SCORE_LEVELS = [
  { name: '优秀', threshold: 90 },
  { name: '良好', threshold: 75 },
  { name: '及格', threshold: 60 },
  { name: '差', threshold: 40 },
  { name: '未达标', threshold: 0 }
]

function sortLevels(levels) {
  return [...levels].sort((a, b) => (Number(b.threshold) || 0) - (Number(a.threshold) || 0))
}

function extractScoreLevelsFromTemplate(tpl) {
  if (!tpl?.configJson) return []
  try {
    const cfg = typeof tpl.configJson === 'string' ? JSON.parse(tpl.configJson) : tpl.configJson
    return cfg?.stages?.comprehensiveCalc?.config?.scoreLevels || []
  } catch { return [] }
}

function gradeIndexToTagType(idx) {
  const types = ['success', 'warning', '', 'danger', 'info']
  return types[Math.min(idx, types.length - 1)]
}

/** 搜索过滤下拉：聚合所有已加载模板的等级名，兼容旧 A/B/C/D */
const gradeOptions = computed(() => {
  const names = new Set(['A', 'B', 'C', 'D'])
  allTemplates.value.forEach(t => {
    extractScoreLevelsFromTemplate(t).forEach(l => l.name && names.add(l.name))
  })
  return Array.from(names)
})

const taskStatusOptions = [
  { label: '待执行', value: 'PENDING' },
  { label: '已分发', value: 'DISPATCHED' },
  { label: '执行中', value: 'RUNNING' },
  { label: '执行成功', value: 'SUCCESS' },
  { label: '执行失败', value: 'FAILED' },
  { label: '已终止', value: 'TERMINATED' },
  { label: '等待中', value: 'WAITING' },
  { label: '已暂停', value: 'PAUSED' }
]

// ==================== 发起计算 ====================
const runDialogVisible = ref(false)
const runFormRef = ref()
const runLoading = ref(false)
const runForm = reactive({
  taskName: '',
  calcFlowTemplateId: undefined,
  indicatorSystemId: undefined
})
const runRules = {
  calcFlowTemplateId: [{ required: true, message: '请选择流程模板', trigger: 'change' }],
  indicatorSystemId: [{ required: true, message: '请选择指标体系', trigger: 'change' }]
}

const allTemplates = ref([])
const runnableTemplates = computed(() =>
  allTemplates.value.filter(t => t.status === 'PUBLISHED' || t.status === 'TESTING')
)

const indicatorOptions = ref([])
const indicatorLoading = ref(false)

function templateStatusLabel(status) {
  const map = { PUBLISHED: '已发布', TESTING: '测试中', DRAFT: '草稿', DISABLED: '已停用' }
  return map[status] || status
}

async function loadTemplates() {
  const res = await listCalcFlow({ pageNum: 1, pageSize: 500 })
  allTemplates.value = res.rows || []
}

async function remoteIndicator(keyword) {
  indicatorLoading.value = true
  try {
    const res = await selectIndicatorSystem(keyword)
    indicatorOptions.value = Array.isArray(res.data) ? res.data : []
  } finally {
    indicatorLoading.value = false
  }
}

function openRunDialog() {
  runForm.taskName = ''
  runForm.calcFlowTemplateId = undefined
  runForm.indicatorSystemId = undefined
  runFormRef.value?.resetFields()
  runDialogVisible.value = true
}

async function submitRun() {
  await runFormRef.value?.validate()
  runLoading.value = true
  try {
    const body = {
      calcFlowTemplateId: runForm.calcFlowTemplateId,
      taskName: runForm.taskName || undefined,
      indicatorSystemId: runForm.indicatorSystemId
    }
    const res = await runCalcTask(body)
    const tid = res.data && res.data.id
    ElMessage.success(tid ? `${res.msg || '发起成功'}（任务ID ${tid}）` : (res.msg || '发起成功'))
    runDialogVisible.value = false
    getList()
  } catch (e) {
    ElMessage.error(e?.message || '发起失败')
  } finally {
    runLoading.value = false
  }
}

// ==================== 查看详情 ====================
const viewDialogVisible = ref(false)
const currentRow = ref({})
/** 当前详情对应的评分等级（按阈值降序），供详情弹窗内所有评级函数使用 */
const currentScoreLevels = computed(() => {
  const raw = currentRow.value?.scoreLevels
  if (Array.isArray(raw) && raw.length > 0) return sortLevels(raw)
  return DEFAULT_SCORE_LEVELS
})
const indicatorTreeRef = ref(null)
const indicatorTreeData = ref([])
const selectedNode = ref(null)
const defaultExpandedKeys = ref([])
const isAllExpanded = ref(true)
const treeProps = { label: 'label', children: 'children' }

async function handleView(row) {
  viewDialogVisible.value = true
  currentRow.value = { ...row }
  indicatorTreeData.value = []
  selectedNode.value = null
  try {
    const res = await getEvalResult(row.id)
    if (res && res.data) {
      const data = { ...res.data }
      data.score = normalizeApiScore(data.score)
      if (Array.isArray(data.dimensionList)) {
        data.dimensionList = data.dimensionList.map(d => ({ ...d, value: normalizeApiScore(d.value) }))
      }
      const levels = Array.isArray(data.scoreLevels) && data.scoreLevels.length > 0
        ? data.scoreLevels : DEFAULT_SCORE_LEVELS
      data.grade = computeGradeByScore(data.score, levels)
      currentRow.value = data
    }
  } catch { /* 保留列表行数据 */ }
  await nextTick()
  await loadIndicatorSystemData()
}

async function loadIndicatorSystemData() {
  if (!currentRow.value.indicatorSystemId) {
    buildTreeFromDimensions()
    return
  }
  try {
    const res = await getIndicatorSystem(currentRow.value.indicatorSystemId)
    if (res && res.data && res.data.indicatorTree) {
      const treeData = parseIndicatorTreeToForest(res.data.indicatorTree)
      mergeCalculationResults(treeData)
      indicatorTreeData.value = treeData
      if (treeData.length > 0) {
        defaultExpandedKeys.value = [treeData[0].uid]
        selectedNode.value = treeData[0]
      }
    } else {
      buildTreeFromDimensions()
    }
  } catch {
    buildTreeFromDimensions()
  }
}

function buildTreeFromDimensions() {
  if (!currentRow.value.dimensionList || currentRow.value.dimensionList.length === 0) {
    indicatorTreeData.value = []
    return
  }
  const rootUid = 'root_' + Date.now()
  const rootNode = {
    uid: rootUid,
    label: currentRow.value.indicatorSystemName || '指标体系',
    indicatorType: '综合',
    weight: 1,
    calculatedScore: currentRow.value.score,
    children: currentRow.value.dimensionList.map((dim, index) => ({
      uid: 'dim_' + index,
      label: dim.label,
      indicatorType: '维度',
      weight: 0,
      calculatedScore: dim.value,
      children: []
    }))
  }
  indicatorTreeData.value = [rootNode]
  defaultExpandedKeys.value = [rootUid]
  selectedNode.value = rootNode
}

function mergeCalculationResults(treeData) {
  const assignScores = (nodes) => {
    nodes.forEach(node => {
      if (currentRow.value.dimensionList) {
        const matchedDim = currentRow.value.dimensionList.find(dim => dim.label === node.label)
        if (matchedDim) {
          node.calculatedScore = Number(matchedDim.value)
        } else {
          node.calculatedScore = estimateNodeScore()
        }
      } else {
        node.calculatedScore = estimateNodeScore()
      }
      if (node.children && node.children.length > 0) assignScores(node.children)
    })
  }
  assignScores(treeData)
  if (treeData.length > 0 && currentRow.value.score) {
    treeData[0].calculatedScore = Number(currentRow.value.score)
  }
}

function estimateNodeScore() {
  const base = currentRow.value.score ? Number(currentRow.value.score) : 70
  return Math.max(0, Math.min(100, Math.round((base + (Math.random() - 0.5) * 20) * 100) / 100))
}

function expandAllNodes() {
  if (!indicatorTreeRef.value) return
  const allUids = []
  const traverse = (nodes) => nodes.forEach(n => { allUids.push(n.uid); if (n.children?.length) traverse(n.children) })
  traverse(indicatorTreeData.value)
  defaultExpandedKeys.value = isAllExpanded.value
    ? (indicatorTreeData.value.length > 0 ? [indicatorTreeData.value[0].uid] : [])
    : allUids
  isAllExpanded.value = !isAllExpanded.value
}

function handleNodeClick(data) { selectedNode.value = data }

function getTotalNodeCount(treeData) {
  let count = 0
  const traverse = (nodes) => nodes.forEach(n => { count++; if (n.children?.length) traverse(n.children) })
  traverse(treeData)
  return count
}

function getChildrenWeightSum(children) {
  return (children || []).reduce((sum, c) => sum + (Number(c.weight) || 0), 0)
}

function hasComputeRuleData(node) {
  return node?.computeRule && (node.computeRule.algorithmName || node.computeRule.algorithmCode || node.computeRule.description)
}

function hasRawValueData(node) {
  return node && (node.rawValue !== undefined || node.normalizedValue !== undefined || node.unit || node.threshold)
}

// ==================== 任务执行日志抽屉 ====================
const taskDetailVisible = ref(false)
const taskDetail = ref(null)

async function openTaskDetail(row) {
  if (!row.taskId) { ElMessage.warning('无关联计算任务'); return }
  taskDetail.value = null
  taskDetailVisible.value = true
  try {
    const res = await getCalcTask(row.taskId)
    if (res && res.data) taskDetail.value = res.data
  } catch {
    ElMessage.error('获取任务详情失败')
  }
}

// ==================== 报告预览 ====================
const reportPreviewVisible = ref(false)
const reportPreviewLoading = ref(false)
const reportPreviewUrl = ref('')
const reportPreviewRow = ref(null)
const reportLinkCache = new Map()

async function fetchReportLinks(row) {
  if (!row?.id) return null
  if (reportLinkCache.has(row.id)) return reportLinkCache.get(row.id)
  const res = await getEvalResultReportLinks(row.id)
  const links = res?.data || {}
  reportLinkCache.set(row.id, links)
  return links
}

function closeReportPreview() {
  reportPreviewUrl.value = ''
  reportPreviewRow.value = null
  reportPreviewLoading.value = false
}

async function openReportPreviewV2(row) {
  if (!row?.id) return
  reportPreviewRow.value = row
  reportPreviewVisible.value = true
  reportPreviewLoading.value = true
  reportPreviewUrl.value = ''
  try {
    const links = await fetchReportLinks(row)
    const url = links?.previewUrl
    if (!url) { ElMessage.warning('未返回PDF预览地址'); reportPreviewVisible.value = false; return }
    reportPreviewUrl.value = url
  } catch (e) {
    reportPreviewVisible.value = false
    ElMessage.error(e?.message || '获取预览地址失败')
  } finally {
    reportPreviewLoading.value = false
  }
}

function handleReportDownloadCommand(format) {
  if (reportPreviewRow.value) openReportDownloadV2(reportPreviewRow.value, format)
}

async function openReportDownloadV2(row, format = 'pdf') {
  if (!row?.id) return
  try {
    const links = await fetchReportLinks(row)
    const targetUrl = format === 'word' ? links?.wordUrl : links?.pdfUrl
    if (!targetUrl) { ElMessage.warning(format === 'word' ? '未生成Word报告' : '未生成PDF报告'); return }
    const ext = format === 'word' ? 'docx' : 'pdf'
    const cleanUrl = (targetUrl || '').split('?')[0]
    const fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1) || `eval_result_${row.id}.${ext}`
    proxy.download('file/downloadReport', { url: targetUrl }, fileName)
  } catch (e) {
    ElMessage.error(e?.message || '下载报告失败')
  }
}

// ==================== 辅助函数 ====================
function getGradeTagType(grade) {
  // 兼容旧 A/B/C/D 格式
  const abcdMap = { A: 'success', B: 'warning', C: '', D: 'danger' }
  if (grade in abcdMap) return abcdMap[grade]
  // 新格式：从 currentScoreLevels 按名称找到索引，映射颜色
  const idx = currentScoreLevels.value.findIndex(l => l.name === grade)
  if (idx >= 0) return gradeIndexToTagType(idx)
  // 兜底：常用中文等级名静态映射
  const cnMap = { '优秀': 'success', '良好': 'warning', '及格': '', '不及格': 'danger', '差': 'danger', '较差': 'danger', '一般': '' }
  return cnMap[grade] ?? 'info'
}

function formatGradeToChinese(grade) {
  return { A: '优秀', B: '良好', C: '一般', D: '较差' }[grade] || grade || '—'
}

/** 将原始得分转为等级名称，使用当前行模板配置的 scoreLevels */
function formatScoreToGradeChinese(score) {
  if (score == null) return '—'
  const n = Number(score)
  for (const lv of currentScoreLevels.value) {
    if (n >= (Number(lv.threshold) || 0)) return lv.name
  }
  return '—'
}

function getTaskStatusTagType(status) {
  return { SUCCESS: 'success', RUNNING: 'primary', DISPATCHED: 'warning', PENDING: 'info', FAILED: 'danger', TERMINATED: 'info', WAITING: 'info', PAUSED: 'warning' }[status] || 'info'
}

function getTaskStatusLabel(status) {
  return { SUCCESS: '执行成功', RUNNING: '执行中', DISPATCHED: '已分发', PENDING: '待执行', FAILED: '执行失败', TERMINATED: '已终止', WAITING: '等待中', PAUSED: '已暂停' }[status] || status || '—'
}

function getProgressStatus(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'exception'
  return ''
}

function getProgressStatusByScore(score) {
  if (score == null) return ''
  const n = Number(score)
  const levels = currentScoreLevels.value
  if (levels.length > 0 && n >= (Number(levels[0].threshold) || 0)) return 'success'
  if (levels.length > 0 && n < (Number(levels[levels.length - 1].threshold) || 0)) return 'exception'
  return ''
}

function getScoreTagType(score) {
  if (score == null) return 'info'
  const n = Number(score)
  const idx = currentScoreLevels.value.findIndex(lv => n >= (Number(lv.threshold) || 0))
  if (idx === -1) return 'info'
  return gradeIndexToTagType(idx)
}

function getScoreClass(score) {
  if (score == null) return ''
  const n = Number(score)
  const idx = currentScoreLevels.value.findIndex(lv => n >= (Number(lv.threshold) || 0))
  const classes = ['score-excellent', 'score-good', 'score-normal', 'score-poor', 'score-bad']
  if (idx === -1) return 'score-bad'
  return classes[Math.min(idx, classes.length - 1)]
}

function getScoreProgressColor(score) {
  if (score == null) return '#909399'
  const n = Number(score)
  const idx = currentScoreLevels.value.findIndex(lv => n >= (Number(lv.threshold) || 0))
  const colors = ['#67C23A', '#E6A23C', '#409EFF', '#F56C6C', '#909399']
  if (idx === -1) return '#909399'
  return colors[Math.min(idx, colors.length - 1)]
}

function formatWeight(weight) {
  if (weight == null) return '—'
  const n = Number(weight)
  return isNaN(n) ? String(weight) : (n * 100).toFixed(1) + '%'
}

function parseTime(time) {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return time
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' }).replace(/\//g, '-')
}

function getStageStatusType(status) {
  return { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'primary', PENDING: 'info', SKIPPED: 'warning' }[status] || 'info'
}

function getStageStatusLabel(status) {
  return { SUCCESS: '成功', FAILED: '失败', RUNNING: '执行中', PENDING: '待执行', SKIPPED: '已跳过' }[status] || status
}

function getTimelineItemType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'primary'
  return ''
}

// ==================== 列表操作 ====================
function handleQuery() { queryParams.value.pageNum = 1; getList() }

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value = { pageNum: 1, pageSize: 10, taskName: '', grade: '', taskRunStatus: '' }
  getList()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

async function handleDelete(row) {
  try {
    await proxy.$modal.confirm(`确认删除该评估结果？`)
    await delEvalResult(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch { /* 取消 */ }
}

function handleBatchDelete() {
  if (ids.value.length === 0) { ElMessage.warning('请先选择要删除的结果'); return }
  proxy.$modal.confirm(`确认删除选中的 ${ids.value.length} 条结果？`).then(async () => {
    try {
      await delEvalResult(ids.value.join(','))
      ElMessage.success('删除成功')
      getList()
    } catch { ElMessage.error('删除失败') }
  }).catch(() => {})
}

function handleExport() {
  proxy.download('/zhpg/evalResult/export', { ...queryParams.value }, `评估结果_${new Date().getTime()}.xlsx`)
}

// ==================== 动态轮询 ====================
const NON_TERMINAL = ['PENDING', 'DISPATCHED', 'RUNNING']
let pollTimer = null

function hasRunningTasks() { return tableData.value.some(r => NON_TERMINAL.includes(r.taskRunStatus)) }

function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

/** 将算法服务返回的 0-1 小数得分转换为百分制 */
function normalizeApiScore(score) {
  if (score == null) return score
  const n = Number(score)
  if (isNaN(n)) return score
  return Number((n * 100).toFixed(2))
}

/** 根据百分制得分和等级配置计算等级名称 */
function computeGradeByScore(score, levels) {
  if (score == null) return null
  const n = Number(score)
  const sorted = [...(levels || DEFAULT_SCORE_LEVELS)].sort((a, b) => (Number(b.threshold) || 0) - (Number(a.threshold) || 0))
  for (const lv of sorted) {
    if (n >= (Number(lv.threshold) || 0)) return lv.name
  }
  return null
}

function normalizeRows(rows) {
  return (rows || []).map(row => {
    const score = normalizeApiScore(row.score)
    const levels = Array.isArray(row.scoreLevels) && row.scoreLevels.length > 0
      ? row.scoreLevels : DEFAULT_SCORE_LEVELS
    return { ...row, score, grade: computeGradeByScore(score, levels) }
  })
}

async function silentRefresh() {
  try {
    const res = await listEvalResult(queryParams.value)
    tableData.value = normalizeRows(res.rows)
    total.value = res.total || 0
  } catch { /* 静默 */ }
  if (!hasRunningTasks()) stopPolling()
}

function startPolling() { stopPolling(); pollTimer = setInterval(silentRefresh, 3000) }

async function getList() {
  loading.value = true
  try {
    const res = await listEvalResult(queryParams.value)
    tableData.value = normalizeRows(res.rows)
    total.value = res.total || 0
    if (hasRunningTasks()) startPolling()
  } catch { ElMessage.error('获取列表失败') } finally { loading.value = false }
}

onMounted(async () => {
  await loadTemplates()
  await remoteIndicator('')
  getList()
})

onUnmounted(stopPolling)
</script>

<style scoped>
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.search-form :deep(.el-form-item) {
  margin-bottom: 12px;
  margin-right: 12px;
}
.text-muted { color: var(--el-text-color-secondary); font-size: 12px; }
.mb12 { margin-bottom: 12px; }
.ml8 { margin-left: 8px; }

/* 表格行高 */
:deep(.el-table .el-table__row td) {
  padding: 10px 0;
}
:deep(.el-table th.el-table__cell) {
  padding: 10px 0;
  font-size: 13px;
}

/* 进度列 */
.progress-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}
.log-btn {
  flex-shrink: 0;
  font-size: 13px;
  padding: 4px 6px;
}
.progress-cell :deep(.el-progress-bar__outer) {
  height: 10px !important;
}
.progress-cell :deep(.el-progress__text) {
  font-size: 13px !important;
}

/* 操作列 */
.action-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}
.action-cell .el-button {
  padding: 4px 6px;
  font-size: 13px;
}
.action-cell .el-divider--vertical {
  margin: 0 2px;
  height: 12px;
}

/* ========== 报告预览 ========== */
.report-preview-body { height: 70vh; min-height: 520px; }
.report-preview-frame { width: 100%; height: 100%; border: 0; background: #f5f7fa; }
.report-actions { display: flex; gap: 12px; padding: 16px; background: var(--el-fill-color-light); border-radius: 4px; }

/* ========== 概览卡片 ========== */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
@media (max-width: 1200px) { .overview-cards { grid-template-columns: repeat(2, 1fr); } }

.overview-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
  transition: all 0.3s ease;
}
.overview-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); transform: translateY(-2px); }
.overview-card .card-icon {
  display: flex; align-items: center; justify-content: center;
  width: 56px; height: 56px; border-radius: 12px;
  background: var(--el-color-primary-light-9); color: var(--el-color-primary); flex-shrink: 0;
}
.overview-card.total-score .card-icon { background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%); color: #10b981; }
.overview-card.total-score.score-good .card-icon { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); color: #f59e0b; }
.overview-card.total-score.score-normal .card-icon { background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); color: #3b82f6; }
.overview-card.total-score.score-poor .card-icon,
.overview-card.total-score.score-bad .card-icon { background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%); color: #ef4444; }
.overview-card.indicator-system .card-icon { background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%); color: #8b5cf6; }
.overview-card.task-info .card-icon { background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); color: #0ea5e9; }
.overview-card.quick-stats .card-icon { background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%); color: #f97316; }
.overview-card .card-content { flex: 1; min-width: 0; }
.overview-card .card-label { font-size: 13px; color: var(--el-text-color-secondary); margin-bottom: 4px; }
.overview-card .card-value { font-size: 24px; font-weight: 700; color: var(--el-text-color-primary); margin-bottom: 8px; line-height: 1.2; }
.overview-card .card-value.text-ellipsis { font-size: 15px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.overview-card .card-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.overview-card .card-grade { margin-top: 4px; }
.overview-card .card-bg-icon { position: absolute; right: -10px; bottom: -10px; opacity: 0.05; pointer-events: none; }
.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.stat-item { display: flex; flex-direction: column; align-items: flex-start; gap: 2px; }
.stat-item .stat-value { font-size: 20px; font-weight: 700; }
.stat-item .stat-label { font-size: 12px; color: var(--el-text-color-secondary); }

/* ========== 分数色 ========== */
.score-excellent { color: #67C23A; }
.score-good { color: #E6A23C; }
.score-normal { color: #409EFF; }
.score-poor { color: #F56C6C; }
.score-bad { color: #909399; }

/* ========== 评分标准 ========== */
.grade-standard-section { margin-bottom: 20px; }
.grade-standard-title { font-weight: 600; margin-right: 8px; }
.grade-standard-item { margin-right: 16px; font-size: 13px; }
.grade-standard-item .el-tag { margin-right: 4px; }

/* ========== 主布局 ========== */
.detail-main-layout { display: flex; gap: 16px; height: 500px; margin-bottom: 20px; }
.left-tree-panel, .right-detail-panel {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.left-tree-panel { width: 320px; flex-shrink: 0; }
.right-detail-panel { flex: 1; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--el-bg-color); border-bottom: 1px solid var(--el-border-color-light); }
.panel-title { font-weight: 600; font-size: 14px; color: var(--el-text-color-primary); }
.panel-subtitle { font-size: 13px; color: var(--el-text-color-secondary); margin-left: 8px; }
.tree-wrapper { flex: 1; overflow-y: auto; padding: 8px; }
.custom-tree-node { display: flex; align-items: center; justify-content: space-between; flex: 1; padding-right: 8px; }
.custom-tree-node .node-label { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 180px; }
.custom-tree-node .node-score { margin-left: 8px; }
.custom-tree-node.is-leaf .node-label { color: var(--el-color-primary); }
.detail-content { flex: 1; overflow-y: auto; padding: 16px; }

/* ========== 节点信息卡片 ========== */
.node-info-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
@media (max-width: 768px) { .node-info-cards { grid-template-columns: repeat(2, 1fr); } }
.info-card { display: flex; align-items: center; gap: 12px; background: var(--el-bg-color); border-radius: 10px; padding: 16px; border: 1px solid var(--el-border-color-light); transition: all 0.3s ease; }
.info-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); transform: translateY(-2px); }
.info-card .info-icon { display: flex; align-items: center; justify-content: center; width: 40px; height: 40px; border-radius: 10px; background: var(--el-fill-color-light); color: var(--el-text-color-secondary); flex-shrink: 0; }
.info-card .info-content { flex: 1; min-width: 0; }
.info-card .info-label { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 4px; }
.info-card .info-value { font-size: 14px; font-weight: 500; color: var(--el-text-color-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.info-card .info-value.score-value { font-size: 22px; font-weight: 700; }
.info-card.score-card .info-icon { background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%); color: #10b981; }
.info-card.grade-card .info-icon { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); color: #f59e0b; }

/* ========== 得分可视化 ========== */
.score-visualization { background: var(--el-bg-color); border-radius: 8px; padding: 16px; margin-bottom: 20px; border: 1px solid var(--el-border-color-light); }
.viz-title { font-weight: 600; font-size: 14px; margin-bottom: 16px; color: var(--el-text-color-primary); }
.viz-content { display: flex; gap: 24px; align-items: center; }
.progress-section { flex: 1; }
.progress-label { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }
.progress-score { font-weight: 600; color: var(--el-color-primary); }
.gauge-section { width: 150px; display: flex; justify-content: center; }
.gauge-wrapper { position: relative; text-align: center; }
.gauge-label { margin-top: 8px; font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); }

/* ========== 子指标权重卡片 ========== */
.children-weight-section { background: var(--el-bg-color); border-radius: 8px; padding: 16px; margin-bottom: 20px; border: 1px solid var(--el-border-color-light); }
.children-weight-section .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.children-weight-section .section-title { font-weight: 600; font-size: 14px; color: var(--el-text-color-primary); padding-left: 8px; border-left: 4px solid var(--el-color-primary); }
.weight-sum-info { font-size: 13px; padding: 6px 12px; border-radius: 6px; background: var(--el-fill-color-light); color: var(--el-text-color-secondary); }
.weight-sum-info.is-valid { background: var(--el-color-success-light-9); color: var(--el-color-success); }
.weight-card { background: var(--el-fill-color-light); border-radius: 10px; padding: 14px; margin-bottom: 12px; border: 2px solid transparent; transition: all 0.3s ease; }
.weight-card:hover { border-color: var(--el-color-primary-light-5); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.weight-card.is-excellent { background: linear-gradient(135deg, #ecfdf5 0%, #f0fdf4 100%); border-color: #86efac; }
.weight-card.is-good { background: linear-gradient(135deg, #fffbeb 0%, #fefce8 100%); border-color: #fcd34d; }
.weight-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.weight-card-title { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; margin-right: 8px; }
.weight-card-body { display: flex; flex-direction: column; gap: 12px; }
.weight-value-row { display: flex; gap: 16px; }
.weight-item { flex: 1; }
.weight-item .weight-label { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 4px; }
.weight-item .weight-value { font-size: 18px; font-weight: 700; color: var(--el-color-primary); }
.weight-item .score-value { font-size: 18px; font-weight: 700; }
.weight-bar-section { display: flex; align-items: center; gap: 8px; }
.weight-bar-wrapper { flex: 1; height: 8px; background: var(--el-border-color-lighter); border-radius: 4px; overflow: hidden; }
.weight-bar-fill { height: 100%; background: linear-gradient(90deg, var(--el-color-primary) 0%, var(--el-color-primary-light-3) 100%); border-radius: 4px; transition: width 0.5s ease; }
.weight-percent { width: 50px; text-align: right; font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); }
.score-bar-section { display: flex; align-items: center; gap: 8px; }
.score-bar-section .el-progress { flex: 1; }
.score-percent { width: 60px; text-align: right; font-size: 12px; color: var(--el-text-color-secondary); }

/* ========== 计算规则 / 原始值 ========== */
.compute-rule-section, .raw-value-section { background: var(--el-bg-color); border-radius: 8px; padding: 16px; margin-bottom: 20px; border: 1px solid var(--el-border-color-light); }
.compute-rule-section .section-header, .raw-value-section .section-header { display: flex; align-items: center; margin-bottom: 12px; }
.compute-rule-section .section-title, .raw-value-section .section-title { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 14px; color: var(--el-text-color-primary); padding-left: 8px; border-left: 4px solid var(--el-color-primary); }
.highlight-value { font-size: 16px; font-weight: 600; color: var(--el-color-primary); }

/* ========== 结论建议 ========== */
.section-title { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 12px; padding-left: 8px; border-left: 4px solid var(--el-color-primary); }
.conclusion-section, .report-section { margin-bottom: 20px; }
.conclusion-card :deep(.el-card__header), .suggestion-card :deep(.el-card__header) { padding: 12px 16px; }
.conclusion-card .card-header, .suggestion-card .card-header { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.conclusion-card .card-header { color: var(--el-color-primary); }
.suggestion-card .card-header { color: var(--el-color-warning); }
.conclusion-content, .suggestion-content { font-size: 13px; line-height: 1.6; color: var(--el-text-color-regular); white-space: pre-wrap; }

/* ========== 任务日志抽屉 ========== */
.drawer-section-title { font-weight: 600; margin-bottom: 12px; color: var(--el-text-color-primary); }
.stage-log-item { background: var(--el-fill-color-light); padding: 12px; border-radius: 4px; }
.stage-header { display: flex; align-items: center; margin-bottom: 8px; }
.stage-output, .stage-error { margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--el-border-color-light); }
.output-label, .error-label { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 4px; }
.output-content { font-size: 13px; color: var(--el-text-color-primary); word-break: break-all; }
.error-content { font-size: 13px; color: var(--el-color-danger); word-break: break-all; }

/* ========== 详情弹窗 ========== */
:deep(.result-detail-dialog .el-dialog__body) { padding: 20px; max-height: calc(90vh - 120px); overflow-y: auto; }
:deep(.el-tree-node__content) { height: 32px; }
:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) { background-color: var(--el-color-primary-light-9); }
:deep(.el-timeline-item__content) { margin-left: 8px; }
</style>
