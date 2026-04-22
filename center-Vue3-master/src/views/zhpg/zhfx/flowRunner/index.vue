<template>
  <div class="flow-runner-page">
    <!-- 顶部：任务上下文 + 上游流水线摘要 -->
    <div class="runner-header">
      <div class="header-left">
        <el-button link icon="ArrowLeft" @click="handleExit">返回</el-button>
        <div class="header-title">
          <h2>评估计算 · 流程执行</h2>
          <span class="header-sub">{{ taskName || '未命名任务' }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-tag v-if="mode === 'execute'" type="success" effect="dark">执行模式</el-tag>
        <el-tag v-else type="info" effect="dark">设计模式</el-tag>
      </div>
    </div>

    <div class="runner-context" v-if="mode === 'execute'">
      <el-descriptions :column="3" size="small" border>
        <el-descriptions-item label="流程模板">{{ templateDetail?.templateName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="指标体系">{{ indicatorSystemDetail?.systemName || indicatorSystemDetail?.indicatorSystemName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ taskTypeLabel }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 步骤条 -->
    <div class="runner-steps">
      <el-steps :active="currentStepIdx" finish-status="success" align-center>
        <el-step
          v-for="(s, idx) in visibleSteps"
          :key="s.key"
          :status="stepStatus(s.key)"
        >
          <template #title>
            <button
              class="step-title-btn"
              :class="{ 'is-clickable': canReviewStep(idx), 'is-current': currentStep === s.key }"
              type="button"
              :disabled="!canReviewStep(idx)"
              @click="goToStep(idx)"
            >
              {{ s.label }}
            </button>
          </template>
          <template #description>
            <span class="step-desc">{{ stepDescription(s.key) }}</span>
          </template>
        </el-step>
      </el-steps>
      <div class="step-state-strip">
        <span v-for="s in visibleSteps" :key="s.key" class="state-pill" :class="stepStateClass(s.key)">
          {{ s.label }}：{{ stepDescription(s.key) }}
        </span>
      </div>
    </div>

    <!-- 步骤内容 -->
    <div class="runner-body">
      <!-- Step 1: 调度策略 -->
      <div v-show="currentStep === 'scheduleConfig'" class="step-panel">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
          <template #title>该步配置任务的调度与执行策略，若仅使用默认值可直接点击"下一步"。</template>
        </el-alert>
        <el-form label-width="130px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="路由策略">
                <el-select v-model="stageConfig.scheduleConfig.config.routeStrategy" style="width: 100%">
                  <el-option v-for="o in routeStrategyOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="阻塞策略">
                <el-select v-model="stageConfig.scheduleConfig.config.blockStrategy" style="width: 100%">
                  <el-option label="单机串行" value="SERIAL_EXECUTION" />
                  <el-option label="丢弃后续调度" value="DISCARD_LATER" />
                  <el-option label="覆盖之前调度" value="COVER_EARLY" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="超时时间(秒)">
                <el-input-number v-model="stageConfig.scheduleConfig.config.timeoutSeconds" :min="0" :max="3600" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="失败重试次数">
                <el-input-number v-model="stageConfig.scheduleConfig.config.retryTimes" :min="0" :max="10" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="错过调度">
                <el-select v-model="stageConfig.scheduleConfig.config.misfireStrategy" style="width: 100%">
                  <el-option label="忽略" value="DO_NOTHING" />
                  <el-option label="立即补触发" value="FIRE_ONCE_NOW" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- Step 2: 权重计算 (仅 execute 模式) -->
      <div v-show="currentStep === 'weightCalc'" class="step-panel">
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px;">
          <template #title>
            本步会直接修改指标体系「{{ indicatorSystemDetail?.systemName || indicatorSystemDetail?.indicatorSystemName || '—' }}」本体；保存后会持久化回写，并影响后续所有使用该体系的评估任务。
          </template>
        </el-alert>
        <div class="weight-actions">
          <el-button type="primary" icon="Edit" @click="openTreeEditor">打开指标树编辑器</el-button>
          <el-button type="primary" plain icon="Operation" @click="openWeightTuning">权重分配调优</el-button>
          <el-tag v-if="hasUnsavedTreeChanges" type="warning" effect="plain">有未保存修改</el-tag>
          <el-tag v-else type="success" effect="plain">已同步</el-tag>
        </div>
        <div class="tree-preview">
          <div class="preview-title">指标树预览（节点 {{ flatNodeCount }} 个 · 权重和 {{ totalWeightPct }}%）</div>
          <el-tree
            :data="weightTreeData"
            :props="{ label: 'label', children: 'children' }"
            default-expand-all
            class="weight-tree"
          >
            <template #default="{ data }">
              <span class="tree-row">
                <span class="tree-label">{{ data.label }}</span>
                <el-tag v-if="data.weight != null" size="small" effect="plain">权重 {{ ((data.weight || 0) * 100).toFixed(1) }}%</el-tag>
              </span>
            </template>
          </el-tree>
        </div>

        <!-- 树编辑器对话框 -->
        <el-dialog
          v-model="treeEditorVisible"
          title="指标树编辑（本次计算覆盖）"
          fullscreen
          append-to-body
          destroy-on-close
        >
          <div class="tree-editor-wrap">
            <IndicatorTreeWorkbench
              ref="workbenchRef"
              v-model:tree-data="weightTreeData"
              v-model:selected-node="selectedTreeNode"
              variant="system"
              fill-parent-height
              :weight-tuning-mode="'full'"
              :weight-assign-algorithm="indicatorSystemDetail?.weightAssignAlgorithm"
              :conduction-algorithm="indicatorSystemDetail?.conductionAlgorithm"
              show-objective-weight
              :objective-weight-loading="objectiveWeightLoading"
              :objective-weight-disabled="!effectiveIndicatorSystemId || !weightTreeData?.length"
              @run-objective-weight="runObjectiveWeight"
            />
          </div>
          <template #footer>
            <el-button @click="cancelTreeEdits">取 消</el-button>
            <el-button type="primary" :loading="treeSaving" @click="confirmTreeEdits">保存到指标体系</el-button>
          </template>
        </el-dialog>

        <ZhpgWeightTuningDialog
          v-model="weightTuningVisible"
          :tree-data="weightTreeData"
          show-objective-weight
          :objective-weight-loading="objectiveWeightLoading"
          :objective-weight-disabled="!effectiveIndicatorSystemId || !weightTreeData?.length"
          @run-objective-weight="runObjectiveWeight"
        />

        <el-dialog
          v-model="objectiveWeightProgressVisible"
          title="权重赋权计算"
          width="520px"
          append-to-body
          align-center
          :close-on-click-modal="false"
          :close-on-press-escape="false"
          :show-close="objectiveWeightDone"
          destroy-on-close
          @closed="onObjectiveWeightDialogClosed"
        >
          <div class="objective-weight-progress">
            <el-steps direction="vertical" :active="objectiveWeightStepsActive" finish-status="success">
              <el-step title="校验与准备" description="确认指标体系已保存，并解析当前指标树结构" />
              <el-step title="组装层级数据" description="按父子层级生成用于赋权的样本矩阵" />
              <el-step title="权重赋权计算" description="调用算法服务计算权重，可能需要等待一段时间" />
              <el-step title="回写与刷新" description="保存带权重的指标树快照，并刷新当前向导" />
            </el-steps>
            <div v-if="objectiveWeightLoading" class="objective-weight-status">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在计算，请稍候，勿关闭本窗口</span>
            </div>
          </div>
          <template v-if="objectiveWeightDone" #footer>
            <el-button type="primary" @click="closeObjectiveWeightProgressDialog">知道了</el-button>
          </template>
        </el-dialog>
      </div>

      <!-- Step 3: 综合分析计算 -->
      <div v-show="currentStep === 'comprehensiveCalc'" class="step-panel">
        <el-form label-width="130px">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="执行方式">
                <el-select v-model="stageConfig.comprehensiveCalc.config.algorithmChainMode" style="width: 100%">
                  <el-option label="串行执行" value="SERIAL" />
                  <el-option label="并行后聚合" value="PARALLEL" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="空数据处理">
                <el-select v-model="stageConfig.comprehensiveCalc.config.nullDataPolicy" style="width: 100%">
                  <el-option label="补零参与计算" value="ZERO_FILL" />
                  <el-option label="终止计算" value="TERMINATE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="输出中间结果">
                <el-switch v-model="stageConfig.comprehensiveCalc.config.intermediateResultOutput" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 执行控制 + 日志面板（仅执行模式） -->
        <div v-if="mode === 'execute'" class="calc-exec-panel">
          <el-alert
            v-if="isStepStale('comprehensiveCalc')"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          >
            <template #title>{{ stepStaleReason('comprehensiveCalc') || '当前计算结果已过期，请重新发起计算。' }}</template>
          </el-alert>
          <!-- 统一执行状态卡片 -->
          <div class="unified-exec-card" :class="{ 'is-finished': calcFinished }">
            <div class="card-main">
              <div class="exec-action-area">
                <el-button
                  type="primary"
                  size="large"
                  icon="VideoPlay"
                  :disabled="calcRunning"
                  @click="startCalc"
                  class="run-btn"
                >
                  {{ calcFinished ? '重新发起计算' : '发起计算' }}
                </el-button>
              </div>

              <div class="status-tracker">
                <div class="tracker-item">
                  <span class="tracker-label">当前状态</span>
                  <el-tag :type="calcTagType" effect="dark" round size="small">{{ calcStatusLabel || '未开始' }}</el-tag>
                </div>
                <div class="tracker-item progress-item">
                  <span class="tracker-label">进度</span>
                  <div class="progress-container">
                    <el-progress
                      :percentage="calcProgress"
                      :status="calcStatus === 'FAILED' ? 'exception' : (calcProgress >= 100 ? 'success' : '')"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="progress-num">{{ Math.round(calcProgress) }}%</span>
                  </div>
                </div>
              </div>

              <div class="card-actions">
                <el-button 
                  v-if="calcRunning || calcLogs.length"
                  link 
                  type="primary" 
                  icon="List" 
                  @click="calcLogDrawerVisible = true"
                >
                  过程日志
                </el-button>
              </div>
            </div>
            
            <div v-if="currentEvalResult" class="card-summary-strip">
              <div class="summary-item score">
                <div class="item-icon"><el-icon><StarFilled /></el-icon></div>
                <div class="item-content">
                  <span class="label">综合得分</span>
                  <span class="value">{{ mockCalcResult.score || '—' }}</span>
                </div>
              </div>
              <div class="summary-item grade">
                <div class="item-icon"><el-icon><Medal /></el-icon></div>
                <div class="item-content">
                  <span class="label">评估等级</span>
                  <el-tag :type="getGradeTagType(mockCalcResult.grade)" size="small" effect="plain">{{ mockCalcResult.grade || '—' }}</el-tag>
                </div>
              </div>
              <div class="summary-item time">
                <div class="item-icon"><el-icon><Clock /></el-icon></div>
                <div class="item-content">
                  <span class="label">完成时间</span>
                  <span class="value">{{ mockCalcResult.finishTime || '—' }}</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="currentEvalResult" class="result-content-wrap">
            <div class="result-side-tree">
              <div class="panel-header">指标层级</div>
              <div class="tree-container">
                <el-tree
                  :data="detailTreeData"
                  :props="{ label: 'label', children: 'children' }"
                  node-key="uid"
                  highlight-current
                  default-expand-all
                  @node-click="handleDetailNodeClick"
                >
                  <template #default="{ node, data }">
                    <div class="custom-tree-node">
                      <span class="node-label" :title="node.label">{{ node.label }}</span>
                      <span class="node-score" v-if="data.calculatedScore !== undefined">
                        {{ data.calculatedScore }}
                      </span>
                    </div>
                  </template>
                </el-tree>
              </div>
            </div>

            <div class="result-main-pane">
              <el-tabs v-model="activeResultTab" class="result-tabs">
                <el-tab-pane label="评分明细" name="details">
                  <div class="detail-pane-content" v-if="selectedDetailNode">
                    <div class="node-brief-info">
                      <div class="brief-item">
                        <span class="brief-label">指标名称：</span>
                        <span class="brief-value">{{ selectedDetailNode.label }}</span>
                      </div>
                      <div class="brief-item">
                        <span class="brief-label">得分：</span>
                        <span class="brief-value" :class="getScoreClass(selectedDetailNode.calculatedScore)">{{ selectedDetailNode.calculatedScore }}</span>
                      </div>
                    </div>

                    <!-- 专业分值仪表盘 -->
                    <div class="evaluation-dial-section" v-if="selectedDetailNode.calculatedScore !== undefined">
                      <div class="dial-wrapper">
                        <div class="dial-dial">
                          <el-progress 
                            type="dashboard" 
                            :percentage="selectedDetailNode.calculatedScore || 0" 
                            :color="getScoreColor(selectedDetailNode.calculatedScore)" 
                            :width="150" 
                            :stroke-width="12"
                          >
                            <template #default="{ percentage }">
                              <div class="dial-inner-content">
                                <span class="dial-score-num">{{ percentage }}</span>
                                <span class="dial-score-unit">分</span>
                                <el-divider style="margin: 8px 0; width: 40px; min-width: 40px;" />
                                <span class="dial-grade-label" :style="{ color: getScoreColor(percentage) }">
                                  {{ formatScoreToGrade(percentage) }}
                                </span>
                              </div>
                            </template>
                          </el-progress>
                        </div>
                      </div>

                      <div class="performance-analysis">
                        <div class="analysis-title">评估得分表现</div>
                        <div class="tier-scale">
                          <div class="tier-markers">
                            <span style="left: 0%">0</span>
                            <span style="left: 60%">60</span>
                            <span style="left: 75%">75</span>
                            <span style="left: 90%">90</span>
                            <span style="right: 0%">100</span>
                          </div>
                          <div class="tier-bar">
                            <div class="tier fail" style="width: 60%" title="0-60 不及格"></div>
                            <div class="tier pass" style="width: 15%" title="60-75 及格"></div>
                            <div class="tier good" style="width: 15%" title="75-90 良好"></div>
                            <div class="tier excellent" style="width: 10%" title="90-100 优秀"></div>
                            <div class="tier-pointer" :style="{ left: (selectedDetailNode.calculatedScore || 0) + '%' }">
                              <div class="pointer-tip"></div>
                              <div class="pointer-box">{{ selectedDetailNode.calculatedScore }}</div>
                            </div>
                          </div>
                        </div>
                        <div class="analysis-detail">
                          当前指标处于 <span class="status-text" :style="{ color: getScoreColor(selectedDetailNode.calculatedScore) }">
                            {{ formatScoreToGrade(selectedDetailNode.calculatedScore) }}
                          </span> 状态。该分值代表参评对象在当前指标域下的综合能力表现水平。
                        </div>
                      </div>
                    </div>

                    <div v-if="selectedDetailNode.children?.length" class="sub-indicators">
                      <div class="sub-title">下级指标分布</div>
                      <div class="sub-grid">
                        <div v-for="child in selectedDetailNode.children" :key="child.uid" class="sub-card-mini">
                          <div class="sub-card-head">
                            <span class="sub-name">{{ child.label }}</span>
                            <span class="sub-val" :class="getScoreClass(child.calculatedScore)">{{ child.calculatedScore }}分</span>
                          </div>
                          <el-progress :percentage="child.calculatedScore || 0" :color="getScoreColor(child.calculatedScore)" :show-text="false" :stroke-width="3" />
                        </div>
                      </div>
                    </div>
                  </div>
                  <el-empty v-else description="请点击左侧节点查看详情" :image-size="60" />
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>

          <div v-else-if="calcRunning || calcLogs.length" class="running-log-area">
            <div class="log-title">计算过程日志</div>
            <div class="exec-log mini">
              <el-timeline>
                <el-timeline-item
                  v-for="(log, idx) in calcLogs"
                  :key="idx"
                  :timestamp="log.time"
                  :type="log.type || 'primary'"
                  placement="top"
                >
                  <div class="log-content">
                    <strong>{{ log.stage }}</strong>
                    <span class="log-text">{{ log.text }}</span>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>
        </div>

        <el-alert
          v-else
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 16px;"
        >
          <template #title>独立设计模式：本步仅保存"综合分析计算"的策略配置，不会实际发起计算，也不产生日志。</template>
        </el-alert>

        <!-- 计算过程日志抽屉 -->
        <el-drawer
          v-model="calcLogDrawerVisible"
          title="综合分析计算 - 过程日志"
          size="580px"
          destroy-on-close
          append-to-body
        >
          <div class="drawer-log-content">
            <div class="log-status-header">
              <div class="status-left">
                <el-tag :type="calcTagType" effect="dark">{{ calcStatusLabel }}</el-tag>
                <span class="task-id">任务ID: {{ currentCalcTaskId || '—' }}</span>
              </div>
              <el-button link type="primary" icon="Refresh" @click="refreshCurrentTaskLogs">刷新日志</el-button>
            </div>
            <el-timeline v-if="calcLogs.length">
              <el-timeline-item
                v-for="(log, idx) in calcLogs"
                :key="idx"
                :timestamp="log.time"
                :type="log.type || 'primary'"
                placement="top"
                size="normal"
              >
                <div class="log-item-detail">
                  <div class="item-stage">{{ log.stage }}</div>
                  <div class="item-text">{{ log.text }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无过程日志" />
          </div>
        </el-drawer>
      </div>

      <!-- Step 4: 报告匹配 -->
      <div v-show="currentStep === 'reportOutput'" class="step-panel">
        <el-form label-width="130px">
          <el-form-item label="报告模板">
            <el-select
              v-model="stageConfig.reportOutput.config.reportTemplateId"
              placeholder="请选择报告模板"
              clearable
              @change="onReportTemplateChange"
              style="width: 360px;"
            >
              <el-option v-for="t in reportTemplateList" :key="t.id" :label="t.templateName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="占位符映射">
            <el-table :data="stageConfig.reportOutput.config.placeholderMappings" size="small" border style="width: 720px;">
              <el-table-column prop="key" label="占位符" width="200" />
              <el-table-column label="映射类型" width="180">
                <template #default="{ row }">
                  <el-select v-model="row.mappingType" size="small" style="width: 100%" @change="markReportConfigChanged">
                    <el-option label="评估数据" value="AUTO_INDICATOR" />
                    <el-option label="任务属性" value="TASK_PROPERTY" />
                    <el-option label="静态文本" value="STATIC_TEXT" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="映射值">
                <template #default="{ row }">
                  <el-select v-if="row.mappingType === 'AUTO_INDICATOR'" v-model="row.mappingValue" size="small" style="width: 100%" @change="markReportConfigChanged">
                    <el-option label="综合得分" value="overall_score" />
                    <el-option label="评估等级" value="overall_grade" />
                    <el-option label="评估结论" value="overall_conclusion" />
                    <el-option label="评估建议" value="suggestion" />
                    <el-option label="指标汇总表(图文表格)" value="indicator_summary_table" />
                    <el-option label="指标树(图文层级)" value="indicator_tree" />
                    <el-option label="评估结果快照(JSON)" value="eval_result_snapshot" />
                  </el-select>
                  
                  <el-select v-else-if="row.mappingType === 'TASK_PROPERTY'" v-model="row.mappingValue" size="small" style="width: 100%" @change="markReportConfigChanged">
                    <el-option label="任务名称" value="taskName" />
                    <el-option label="试验时间/任务发生时间" value="startTime" />
                    <el-option label="评估对象" value="evaluateTarget" />
                    <el-option label="指标体系名称" value="indicatorSystemName" />
                  </el-select>

                  <el-input v-else v-model="row.mappingValue" size="small" placeholder="—" @change="markReportConfigChanged" />
                </template>
              </el-table-column>
            </el-table>
          </el-form-item>
        </el-form>

        <!-- 执行操作（仅执行模式） -->
        <div v-if="mode === 'execute'" class="report-exec-panel">
          <el-alert
            v-if="isStepStale('reportOutput')"
            type="warning"
            :closable="false"
            show-icon
            class="inline-state-alert"
          >
            <template #title>{{ stepStaleReason('reportOutput') || '当前报告已过期，请重新生成。' }}</template>
          </el-alert>
          <el-button
            type="primary"
            icon="Document"
            :disabled="!canGenerateReport || reportGenerating"
            :loading="reportGenerating"
            @click="generateReport"
          >
            {{ reportGenerated ? '再次生成报告' : '生成报告' }}
          </el-button>
          <span v-if="!calcFinished" class="hint-text">请先在「综合分析计算」完成计算</span>
          <span v-else-if="calcStale" class="hint-text">指标体系已修改，请重新计算后再生成报告</span>
          <span v-else-if="!currentEvalResult" class="hint-text">未获取到本次计算结果</span>
        </div>

        <div v-if="mode === 'execute'" class="report-history">
          <div class="history-title">报告生成历史</div>
          <el-table :data="reportInstances" size="small" border>
            <el-table-column prop="generationNo" label="版本" width="80">
              <template #default="{ row }">第 {{ row.generationNo }} 版</template>
            </el-table-column>
            <el-table-column prop="reportTemplateName" label="报告模板" min-width="180" />
            <el-table-column prop="renderStatus" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.renderStatus === 'SUCCESS' ? 'success' : 'warning'" size="small">{{ row.renderStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="生成时间" width="180" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Document" @click="openGeneratedReport(row)">预览</el-button>
                <el-dropdown @command="(cmd) => handleReportDownloadCommand(cmd, row)" trigger="click" style="margin-left: 12px">
                  <el-button link type="primary" icon="Download">
                    下载<el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="word" :icon="Document">Word 报告</el-dropdown-item>
                      <el-dropdown-item command="pdf" :icon="DocumentChecked">PDF 报告</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-alert
          v-else
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 16px;"
        >
          <template #title>独立设计模式：本步仅保存"报告模板选择 + 占位符映射"，不会实际生成报告。</template>
        </el-alert>

        <el-dialog v-if="mode === 'execute'" v-model="reportPreviewVisible" title="报告预览" width="80%" append-to-body>
          <div class="report-preview-body" v-loading="reportPreviewLoading">
            <iframe v-if="reportPreviewUrl" :src="reportPreviewUrl" class="report-preview-frame" frameborder="0" />
            <el-empty v-else description="暂无可预览地址，可先下载 Word 报告" />
          </div>
        </el-dialog>
      </div>
    </div>

    <!-- 底部操作区 -->
    <div class="runner-footer">
      <el-button @click="handleExit">退出</el-button>
      <div class="footer-right">
        <el-button :disabled="currentStepIdx === 0" @click="prevStep">上一步</el-button>
        <el-button
          v-if="currentStepIdx < visibleSteps.length - 1"
          type="primary"
          :disabled="!canGoNext"
          @click="nextStep"
        >下一步</el-button>
        <el-button
          v-else
          type="success"
          :disabled="!canFinish"
          @click="handleFinish"
        >完成</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, ArrowRight, ArrowDown, Download, TrendCharts, Collection, DocumentChecked, DataAnalysis, Document, Medal, Setting, DataLine, InfoFilled, WarningFilled, List, Refresh, StarFilled, Clock } from '@element-plus/icons-vue'
import { getCalcFlow } from '@/api/zhpg/calcFlow'
import { getCalcTask, runCalcTask } from '@/api/zhpg/calcTask'
import { getCalcFlowExecution, initCalcFlowExecution, runCalcFlowExecution, saveCalcFlowExecutionConfig } from '@/api/zhpg/calcFlowExecution'
import { getIndicatorSystem, objectiveWeightIndicatorSystem, updateIndicatorSystem, selectIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import { getEvalResult, getEvalResultByTask, listEvalResult } from '@/api/zhpg/evalResult'
import { generateEvalReport, getEvalReportLinks, listEvalReportsByResult } from '@/api/zhpg/evalReport'
import { listTemplates, getTemplate } from '@/api/zhpg/report'
import {
  createFlowStepState,
  markStepCompleted,
  markStepFailed,
  markStepPending,
  markStepRunning,
  markStepStale,
  restoreFlowStepState,
  FLOW_STEP_STATUS
} from '@/utils/zhpg/calcFlowState'
import { parseIndicatorTreeToForest, serializeForestToIndicatorTree } from '@/utils/zhpgIndicatorTreeJson'
import IndicatorTreeWorkbench from '@/views/zhpg/components/IndicatorTreeWorkbench.vue'
import ZhpgWeightTuningDialog from '@/views/zhpg/components/ZhpgWeightTuningDialog.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const props = defineProps({
  /** 可由父级直接传入，代替路由参数 */
  templateId: { type: [Number, String], default: null },
  indicatorSystemId: { type: [Number, String], default: null },
  requirementIdProp: { type: [Number, String], default: null },
  taskNameProp: { type: String, default: '' }
})

const emit = defineEmits(['exit', 'finished'])

// ==================== 入参 ====================
const templateId = computed(() => Number(props.templateId || route.query.templateId) || null)
const indicatorSystemId = computed(() => Number(props.indicatorSystemId || route.query.indicatorSystemId) || null)
const requirementId = computed(() => Number(props.requirementIdProp || route.query.requirementId) || null)
const initialTaskId = computed(() => Number(route.query.taskId) || null)
const initialEvalResultId = computed(() => Number(route.query.evalResultId) || null)
const initialExecutionId = computed(() => Number(route.query.executionId) || null)
const taskName = ref(props.taskNameProp || route.query.taskName || '')
const currentExecution = ref(null)
const routeContextKey = computed(() => JSON.stringify({
  templateId: templateId.value,
  indicatorSystemId: indicatorSystemId.value,
  requirementId: requirementId.value,
  executionId: initialExecutionId.value,
  taskId: initialTaskId.value,
  evalResultId: initialEvalResultId.value,
  taskName: props.taskNameProp || route.query.taskName || ''
}))
const initializingContext = ref(false)
const effectiveIndicatorSystemId = computed(() =>
  Number(indicatorSystemId.value || currentExecution.value?.indicatorSystemId || indicatorSystemDetail.value?.id || indicatorSystemDetail.value?.indicatorSystemId) || null
)
const mode = computed(() => ((effectiveIndicatorSystemId.value || requirementId.value) ? 'execute' : 'design'))
const executionReady = ref(false)
let draftSaveTimer = null

// ==================== 模板 / 体系信息 ====================
const templateDetail = ref(null)
const indicatorSystemDetail = ref(null)
const taskTypeLabelMap = {
  PERFORMANCE: '性能指标评估',
  EQUIP_EFFECTIVENESS: '装备作战效能评估',
  SYSTEM_CONTRIBUTION: '体系贡献率评估',
  TASK_SATISFACTION: '作战任务满足度评估',
  SYSTEM_TASK: '体系级任务评估',
  EXERCISE_TRAINING: '演习演训任务评估'
}
const taskTypeLabel = computed(() => taskTypeLabelMap[templateDetail.value?.taskType] || '—')

// ==================== 步骤定义 ====================
const allSteps = [
  { key: 'scheduleConfig', label: '调度策略选配' },
  { key: 'weightCalc', label: '权重计算', executeOnly: true },
  { key: 'comprehensiveCalc', label: '综合分析计算' },
  { key: 'reportOutput', label: '报告匹配' }
]
const visibleSteps = computed(() =>
  allSteps.filter(s => !s.executeOnly || mode.value === 'execute')
)
const currentStepIdx = ref(0)
const currentStep = computed(() => visibleSteps.value[currentStepIdx.value]?.key)
const visitedSteps = reactive(new Set(['scheduleConfig']))
const flowStepState = reactive(createFlowStepState(mode.value))

function replaceFlowStepState(nextState) {
  Object.keys(flowStepState).forEach(key => delete flowStepState[key])
  Object.entries(nextState || {}).forEach(([key, value]) => {
    flowStepState[key] = value
  })
}

function getStepState(key) {
  return flowStepState[key] || { status: FLOW_STEP_STATUS.PENDING, staleReason: '' }
}

function isStepCompleted(key) {
  return getStepState(key).status === FLOW_STEP_STATUS.COMPLETED
}

function isStepActionable(key) {
  const status = getStepState(key).status
  return [FLOW_STEP_STATUS.COMPLETED, FLOW_STEP_STATUS.STALE, FLOW_STEP_STATUS.FAILED, FLOW_STEP_STATUS.RUNNING].includes(status)
}

function isStepStale(key) {
  return getStepState(key).status === FLOW_STEP_STATUS.STALE
}

function stepStaleReason(key) {
  return getStepState(key).staleReason || ''
}

function stepStatus(key) {
  const status = getStepState(key).status
  if (status === FLOW_STEP_STATUS.COMPLETED) return 'success'
  if (status === FLOW_STEP_STATUS.STALE || status === FLOW_STEP_STATUS.FAILED) return 'error'
  if (status === FLOW_STEP_STATUS.RUNNING) return 'process'
  if (currentStep.value === key) return 'process'
  return 'wait'
}

function stepStateClass(key) {
  const status = getStepState(key).status
  return {
    [status]: true,
    current: currentStep.value === key
  }
}

function canReviewStep(idx) {
  const step = visibleSteps.value[idx]
  if (!step) return false
  return idx === currentStepIdx.value || visitedSteps.has(step.key) || isStepActionable(step.key) || canEnterStepByDependency(step.key)
}

function canEnterStepByDependency(key) {
  if (key === 'scheduleConfig') return true
  if (key === 'weightCalc') return mode.value === 'execute' && isStepCompleted('scheduleConfig')
  if (key === 'comprehensiveCalc') {
    if (mode.value === 'design') return isStepCompleted('scheduleConfig')
    return isStepCompleted('weightCalc')
  }
  if (key === 'reportOutput') {
    if (mode.value === 'design') return isStepCompleted('comprehensiveCalc')
    return calcFinished.value || isStepActionable('reportOutput')
  }
  return false
}

function stepDescription(key) {
  const status = getStepState(key).status
  if (status === FLOW_STEP_STATUS.STALE) return getStepState(key).staleReason || '需重新处理'
  if (status === FLOW_STEP_STATUS.FAILED) return getStepState(key).staleReason || '执行失败'
  if (status === FLOW_STEP_STATUS.RUNNING) {
    return key === 'comprehensiveCalc' ? `执行中 ${calcProgress.value}%` : '处理中'
  }
  if (key === 'scheduleConfig') {
    return isStepCompleted(key) ? '策略已确认' : '待配置'
  }
  if (key === 'weightCalc') {
    if (hasUnsavedTreeChanges.value) return '有未保存修改'
    if (isStepCompleted(key)) return weightTreeData.value?.length ? `已确认，节点 ${flatNodeCount.value} 个` : '已确认'
    return weightTreeData.value?.length ? `待确认，节点 ${flatNodeCount.value} 个` : '待加载'
  }
  if (key === 'comprehensiveCalc') {
    if (calcFinished.value) return `已完成，得分 ${mockCalcResult.score || '-'}`
    return '待计算'
  }
  if (key === 'reportOutput') {
    if (reportGenerated.value) return `已生成 ${reportInstances.value.length} 版`
    return '待匹配'
  }
  return ''
}

const stageConfig = reactive({
  scheduleConfig: {
    config: {
      routeStrategy: 'FIRST',
      blockStrategy: 'SERIAL_EXECUTION',
      misfireStrategy: 'DO_NOTHING',
      timeoutSeconds: 300,
      retryTimes: 0
    }
  },
  comprehensiveCalc: {
    config: {
      algorithmChainMode: 'SERIAL',
      nullDataPolicy: 'ZERO_FILL',
      intermediateResultOutput: false
    }
  },
  reportOutput: {
    config: {
      reportTemplateId: null,
      placeholderMappings: []
    }
  }
})
const runtimePolicy = ref({})

function buildRuntimeConfigJson() {
  return JSON.stringify({
    stages: {
      scheduleConfig: {
        stageCode: 'SCHEDULE_CONFIG',
        stageName: '调度策略选配',
        stageOrder: 1,
        enabled: true,
        config: JSON.parse(JSON.stringify(stageConfig.scheduleConfig.config || {}))
      },
      comprehensiveCalc: {
        stageCode: 'COMPREHENSIVE_CALC',
        stageName: '综合分析计算',
        stageOrder: 3,
        enabled: true,
        config: JSON.parse(JSON.stringify(stageConfig.comprehensiveCalc.config || {}))
      },
      reportOutput: {
        stageCode: 'REPORT_OUTPUT',
        stageName: '报告匹配',
        stageOrder: 4,
        enabled: true,
        config: JSON.parse(JSON.stringify(stageConfig.reportOutput.config || {}))
      }
    },
    runtimePolicy: runtimePolicy.value || {}
  })
}

function serializeStepState() {
  return JSON.stringify(JSON.parse(JSON.stringify(flowStepState)))
}

function applyRuntimeConfigJson(configJson) {
  if (!configJson) return
  try {
    const cfg = typeof configJson === 'string' ? JSON.parse(configJson) : configJson
    if (cfg.runtimePolicy) runtimePolicy.value = cfg.runtimePolicy
    if (cfg.stages) {
      Object.keys(cfg.stages).forEach(key => {
        if (stageConfig[key] && cfg.stages[key]?.config) {
          Object.assign(stageConfig[key].config, cfg.stages[key].config)
        }
      })
    }
    const reportCfg = stageConfig.reportOutput.config
    if (reportCfg.reportTemplateId && (!reportCfg.placeholderMappings || !reportCfg.placeholderMappings.length)) {
      onReportTemplateChange(reportCfg.reportTemplateId, { silent: true })
    }
  } catch {
    // ignore malformed config and keep current preset
  }
}

function applyStepStateJson(stepStateJson) {
  if (!stepStateJson) return
  try {
    const state = typeof stepStateJson === 'string' ? JSON.parse(stepStateJson) : stepStateJson
    replaceFlowStepState(state)
  } catch {
    // ignore malformed state
  }
}

async function initOrLoadExecution() {
  // 自动化改进：如果只有 requirementId 而没有 indicatorSystemId，自动查找关联的体系
  if (requirementId.value && !indicatorSystemId.value && !currentExecution.value?.indicatorSystemId && !indicatorSystemDetail.value?.id) {
    try {
      const selectRes = await selectIndicatorSystem({ requirementId: requirementId.value })
      const list = selectRes.data || selectRes.rows || selectRes || []
      const found = Array.isArray(list) ? list[0] : null
      if (found) {
        const foundId = found.indicatorSystemId || found.id
        // 挂载完整指标体系（这也会顺便解构出原始的指标树）
        await loadIndicatorSystem(foundId)
        console.log(`[FlowRunner] 已根据需求ID ${requirementId.value} 自动匹配指标体系:`, foundId)
      }
    } catch (e) {
      console.warn('[FlowRunner] 自动匹配指标体系失败:', e)
    }
  }

  if (mode.value !== 'execute' || !templateId.value || (!effectiveIndicatorSystemId.value && !requirementId.value)) {
    executionReady.value = true
    initializingContext.value = false
    return
  }
  try {
    let res
    if (initialExecutionId.value) {
      res = await getCalcFlowExecution(initialExecutionId.value)
    } else {
      res = await initCalcFlowExecution({
        templateId: templateId.value,
        indicatorSystemId: effectiveIndicatorSystemId.value || undefined,
        requirementId: requirementId.value || undefined,
        executionName: taskName.value || undefined
      })
    }
    const execution = res?.data || res
    if (execution?.id) {
      currentExecution.value = execution
      applyRuntimeConfigJson(execution.runtimeConfigJson)
      applyStepStateJson(execution.stepStateJson)
      if (execution.currentStep) {
        const idx = visibleSteps.value.findIndex(step => step.key === execution.currentStep)
        if (idx >= 0) currentStepIdx.value = idx
      }
      if (execution.indicatorSystemId && !indicatorSystemDetail.value) {
        await loadIndicatorSystem(execution.indicatorSystemId)
      }
      if (execution.calcTaskId && !initialTaskId.value) {
        currentCalcTaskId.value = execution.calcTaskId
        try {
          const taskRes = await getCalcTask(execution.calcTaskId)
          applyTaskDetail(taskRes?.data || taskRes)
          if (!execution.evalResultId && calcFinished.value) {
            await loadEvalResultForTask(execution.calcTaskId)
          }
        } catch {
          // ignore task restore failure
        }
      }
      if (execution.evalResultId && !initialEvalResultId.value) {
        await loadEvalResultById(execution.evalResultId)
      }
    }
  } catch (e) {
    ElMessage.warning(e?.message || '流程执行草稿加载失败，将使用模板预设')
  } finally {
    executionReady.value = true
  }
}

function scheduleSaveExecutionDraft() {
  if (!executionReady.value || !currentExecution.value?.id) return
  clearTimeout(draftSaveTimer)
  draftSaveTimer = setTimeout(() => {
    saveExecutionDraft({ silent: true })
  }, 1200)
}

async function saveExecutionDraft(options = {}) {
  if (mode.value !== 'execute' || !currentExecution.value?.id) return true
  try {
    const res = await saveCalcFlowExecutionConfig(currentExecution.value.id, {
      runtimeConfigJson: buildRuntimeConfigJson(),
      stepStateJson: serializeStepState(),
      currentStep: options.currentStep || currentStep.value,
      status: options.status,
      calcTaskId: currentCalcTaskId.value,
      evalResultId: currentEvalResult.value?.id,
      latestReportId: reportInstances.value?.[0]?.id
    })
    currentExecution.value = res?.data || currentExecution.value
    return true
  } catch (e) {
    if (!options.silent) {
      ElMessage.error(e?.message || '保存流程执行配置失败')
    }
    return false
  }
}

watch(stageConfig, () => {
  scheduleSaveExecutionDraft()
}, { deep: true })

const routeStrategyOptions = [
  { label: '第一个', value: 'FIRST' },
  { label: '轮询', value: 'ROUND' },
  { label: '随机', value: 'RANDOM' },
  { label: '一致性哈希', value: 'CONSISTENT_HASH' },
  { label: '故障转移', value: 'FAILOVER' }
]

// ==================== Step 2 树数据 ====================
const weightTreeData = ref([])
const selectedTreeNode = ref(null)
const treeEditorVisible = ref(false)
const weightTuningVisible = ref(false)
const workbenchRef = ref(null)
const originalTreeSnapshot = ref(null)
const treeSaving = ref(false)
const objectiveWeightLoading = ref(false)
const objectiveWeightProgressVisible = ref(false)
const objectiveWeightStep = ref(0)
const objectiveWeightDone = ref(false)
let objectiveWeightStepTimers = []

const hasUnsavedTreeChanges = computed(() => {
  if (!originalTreeSnapshot.value) return false
  return JSON.stringify(weightTreeData.value || []) !== JSON.stringify(originalTreeSnapshot.value || [])
})
const objectiveWeightStepsActive = computed(() => {
  if (objectiveWeightDone.value) return 4
  return objectiveWeightStep.value
})

const flatNodeCount = computed(() => countNodes(weightTreeData.value))
const totalWeightPct = computed(() => {
  const roots = weightTreeData.value
  if (!roots.length) return '0.0'
  const sum = roots.reduce((acc, n) => acc + (Number(n.weight) || 0), 0)
  return (sum * 100).toFixed(1)
})
function countNodes(list) {
  let n = 0
  const walk = arr => {
    arr.forEach(it => {
      n++
      if (it.children?.length) walk(it.children)
    })
  }
  walk(list || [])
  return n
}

function openTreeEditor() { treeEditorVisible.value = true }
function openWeightTuning() {
  weightTuningVisible.value = true
}
async function confirmTreeEdits(options = {}) {
  const currentIndicatorSystemId = effectiveIndicatorSystemId.value
  if (!currentIndicatorSystemId) {
    ElMessage.warning('缺少指标体系 ID，无法保存')
    return false
  }
  if (!weightTreeData.value?.length) {
    ElMessage.warning('指标树为空，无法保存')
    return false
  }

  treeSaving.value = true
  try {
    const detail = indicatorSystemDetail.value || {}
    const serialized = serializeForestToIndicatorTree(weightTreeData.value, {
      systemName: detail.systemName || detail.indicatorSystemName,
      workMode: detail.workMode
    })
    const payload = {
      ...detail,
      id: detail.id || detail.indicatorSystemId || currentIndicatorSystemId,
      indicatorTree: serialized
    }
    await updateIndicatorSystem(payload)
    indicatorSystemDetail.value = { ...detail, indicatorTree: serialized }
    originalTreeSnapshot.value = JSON.parse(JSON.stringify(weightTreeData.value))
    markStepCompleted(flowStepState, 'weightCalc')
    markCalculationStale()
    await saveExecutionDraft({ silent: true })
    if (!options.keepDialogOpen) {
      treeEditorVisible.value = false
    }
    if (!options.silent) {
      ElMessage.success('指标树已保存到指标体系')
    }
    return true
  } catch (e) {
    ElMessage.error('保存指标体系失败，请稍后重试')
    return false
  } finally {
    treeSaving.value = false
  }
}
function cancelTreeEdits() {
  if (originalTreeSnapshot.value) {
    weightTreeData.value = JSON.parse(JSON.stringify(originalTreeSnapshot.value))
  }
  treeEditorVisible.value = false
}
function resetWeightOverride() {
  if (!originalTreeSnapshot.value) return
  weightTreeData.value = JSON.parse(JSON.stringify(originalTreeSnapshot.value))
  ElMessage.success('已恢复为最近一次保存状态')
}

function clearObjectiveWeightStepTimers() {
  objectiveWeightStepTimers.forEach(id => clearTimeout(id))
  objectiveWeightStepTimers = []
}
function closeObjectiveWeightProgressDialog() {
  objectiveWeightProgressVisible.value = false
}
function onObjectiveWeightDialogClosed() {
  clearObjectiveWeightStepTimers()
  objectiveWeightLoading.value = false
  objectiveWeightDone.value = false
  objectiveWeightStep.value = 0
}
async function runObjectiveWeight() {
  const currentIndicatorSystemId = effectiveIndicatorSystemId.value
  if (!currentIndicatorSystemId) {
    ElMessage.warning('缺少指标体系 ID，无法进行权重赋权')
    return
  }
  if (!weightTreeData.value?.length) {
    ElMessage.warning('指标树为空')
    return
  }

  try {
    if (hasUnsavedTreeChanges.value) {
      await ElMessageBox.confirm(
        '当前指标树存在未保存修改。权重赋权会基于已保存的指标树计算，是否先保存当前修改？',
        '保存当前修改',
        {
          confirmButtonText: '保存并继续',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      const saved = await confirmTreeEdits({ keepDialogOpen: true, silent: true })
      if (!saved) return
    }

    await ElMessageBox.confirm(
      '权重赋权会重新计算并覆盖当前指标树权重。请确认是否继续？',
      '确认权重赋权',
      {
        confirmButtonText: '继续计算',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  clearObjectiveWeightStepTimers()
  objectiveWeightStep.value = 0
  objectiveWeightDone.value = false
  objectiveWeightProgressVisible.value = true
  objectiveWeightLoading.value = true

  objectiveWeightStepTimers.push(setTimeout(() => { objectiveWeightStep.value = 1 }, 320))
  objectiveWeightStepTimers.push(setTimeout(() => { objectiveWeightStep.value = 2 }, 880))

  try {
    const res = await objectiveWeightIndicatorSystem(currentIndicatorSystemId, { persist: true, mockSampleRows: 8 })
    clearObjectiveWeightStepTimers()
    objectiveWeightStep.value = 3
    const payload = res?.data || {}
    const weightedTreeJson = payload.indicatorTreeWeight || payload.indicatorTree
    if (weightedTreeJson) {
      const nextTree = parseIndicatorTreeToForest(weightedTreeJson)
      if (nextTree.length) {
        weightTreeData.value = nextTree
        originalTreeSnapshot.value = JSON.parse(JSON.stringify(nextTree))
      }
      indicatorSystemDetail.value = {
        ...(indicatorSystemDetail.value || {}),
        indicatorTreeWeight: payload.indicatorTreeWeight,
        indicatorTree: payload.indicatorTree || indicatorSystemDetail.value?.indicatorTree
      }
      markStepCompleted(flowStepState, 'weightCalc')
      markCalculationStale()
      await saveExecutionDraft({ silent: true })
      await nextTick()
      workbenchRef.value?.renderGraph?.()
    } else {
      await loadIndicatorSystem()
    }
    objectiveWeightLoading.value = false
    objectiveWeightDone.value = true
    ElMessage.success(res?.msg || '权重赋权完成')
    if (payload.hint) ElMessage.info(String(payload.hint))
  } catch {
    clearObjectiveWeightStepTimers()
    objectiveWeightProgressVisible.value = false
    objectiveWeightLoading.value = false
    objectiveWeightStep.value = 0
    objectiveWeightDone.value = false
    ElMessage.error('权重赋权计算失败')
  }
}

// ==================== Step 3: 计算执行 ====================
const calcStatus = ref('')
const calcRunning = computed(() => ['PENDING', 'DISPATCHED', 'RUNNING'].includes(calcStatus.value))
const calcFinished = computed(() => calcStatus.value === 'SUCCESS')
const calcProgress = ref(0)
const calcLogs = ref([])
const calcStale = ref(false)
const currentCalcTaskId = ref(null)
const currentTaskDetail = ref(null)
const currentEvalResult = ref(null)
const mockCalcResult = reactive({ score: 0, grade: '', finishTime: '' })
const calcLogDrawerVisible = ref(false)

const calcStatusLabel = computed(() => {
  return { PENDING: '待执行', DISPATCHED: '已分发', RUNNING: '执行中', SUCCESS: '已完成', FAILED: '失败' }[calcStatus.value] || ''
})
const calcTagType = computed(() => {
  return { PENDING: 'info', DISPATCHED: 'warning', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger' }[calcStatus.value] || 'info'
})

let calcTimer = null
async function startCalc() {
  if (!(await ensureTreeSavedBeforeLeave())) return
  if (!templateId.value || (!effectiveIndicatorSystemId.value && !requirementId.value)) {
    ElMessage.warning('缺少流程模板或指标体系，无法发起计算')
    return
  }
  clearCalcPolling()
  calcStatus.value = 'PENDING'
  calcProgress.value = 0
  calcLogs.value = []
  calcStale.value = false
  currentEvalResult.value = null
  reportInstances.value = []
  markStepRunning(flowStepState, 'comprehensiveCalc')
  try {
    let res
    if (currentExecution.value?.id) {
      await saveExecutionDraft({ silent: false })
      res = await runCalcFlowExecution(currentExecution.value.id, {
        taskName: taskName.value || undefined,
        runtimeConfigJson: buildRuntimeConfigJson(),
        stepStateJson: serializeStepState(),
        currentStep: 'comprehensiveCalc',
        skipWeightLog: true
      })
    } else {
      res = await runCalcTask({
        calcFlowTemplateId: templateId.value,
        taskName: taskName.value || undefined,
        indicatorSystemId: effectiveIndicatorSystemId.value || undefined,
        requirementId: requirementId.value || undefined,
        runtimeConfigJson: buildRuntimeConfigJson(),
        skipWeightLog: true
      })
    }
    const task = res?.data || res
    currentCalcTaskId.value = task?.id || null
    applyTaskDetail(task)
    if (currentCalcTaskId.value && calcRunning.value) {
      startCalcPolling(currentCalcTaskId.value)
    } else if (calcFinished.value) {
      await loadEvalResultForTask(currentCalcTaskId.value)
    }
    ElMessage.success(currentCalcTaskId.value ? `计算任务已发起（任务ID ${currentCalcTaskId.value}）` : '计算任务已发起')
  } catch (e) {
    calcStatus.value = 'FAILED'
    calcProgress.value = 0
    markStepFailed(flowStepState, 'comprehensiveCalc', e?.message || '计算任务发起失败')
    calcLogs.value.push({ stage: '发起失败', text: e?.message || '计算任务发起失败', type: 'danger', time: new Date().toLocaleTimeString() })
    ElMessage.error(e?.message || '计算任务发起失败')
  }
}

function startCalcPolling(taskId) {
  clearCalcPolling()
  calcTimer = setInterval(async () => {
    try {
      const res = await getCalcTask(taskId)
      const task = res?.data || res
      applyTaskDetail(task)
      if (!calcRunning.value) {
        clearCalcPolling()
        if (calcFinished.value) {
          await loadEvalResultForTask(taskId)
        }
      }
    } catch (e) {
      clearCalcPolling()
      calcStatus.value = 'FAILED'
      markStepFailed(flowStepState, 'comprehensiveCalc', e?.message || '获取计算状态失败')
      calcLogs.value.push({ stage: '轮询失败', text: e?.message || '获取计算状态失败', type: 'danger', time: new Date().toLocaleTimeString() })
    }
  }, 2500)
}

function clearCalcPolling() {
  if (calcTimer) {
    clearInterval(calcTimer)
    calcTimer = null
  }
}

function applyTaskDetail(task) {
  if (!task) return
  currentTaskDetail.value = task
  currentCalcTaskId.value = task.id || currentCalcTaskId.value
  calcStatus.value = task.runStatus || task.status || calcStatus.value
  calcProgress.value = Number(task.progressPercent ?? task.progress ?? calcProgress.value ?? 0)
  calcLogs.value = mapTaskLogs(task)
  if (calcFinished.value) {
    markStepCompleted(flowStepState, 'comprehensiveCalc', { taskId: currentCalcTaskId.value })
  } else if (calcRunning.value) {
    markStepRunning(flowStepState, 'comprehensiveCalc', { taskId: currentCalcTaskId.value })
  } else if (calcStatus.value === 'FAILED') {
    markStepFailed(flowStepState, 'comprehensiveCalc', task.currentStage || '计算任务失败')
  }
  scheduleSaveExecutionDraft()
}

async function refreshCurrentTaskLogs() {
  if (!currentCalcTaskId.value) return
  try {
    const res = await getCalcTask(currentCalcTaskId.value)
    applyTaskDetail(res?.data || res)
    ElMessage.success('日志已刷新')
  } catch (e) {
    ElMessage.warning('刷新日志失败')
  }
}

function mapTaskLogs(task) {
  let logs = Array.isArray(task?.stageLogs) ? task.stageLogs : []
  
  // 如果当前在综合计算步骤，过滤掉之前的权重计算日志，保持界面简洁
  if (currentStep.value === 'comprehensiveCalc') {
    logs = logs.filter(log => log.stageCode !== 'WEIGHT_CALC')
  }

  if (!logs.length) {
    return calcStatus.value
      ? [{ stage: calcStatusLabel.value || '计算任务', text: task?.currentStage || '等待执行器回写', type: calcTagType.value, time: formatTime(task?.updateTime || task?.startTime) }]
      : []
  }
  return logs.map(log => ({
    stage: log.stageName || log.stageCode || '阶段',
    text: log.inputSummary || log.outputSummary || log.message || log.executeMessage || log.executeStatus || '',
    type: stageLogType(log.executeStatus),
    time: formatTime(log.endTime || log.startTime || log.createTime)
  }))
}

function stageLogType(status) {
  return { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning', PENDING: 'primary', SKIPPED: 'info' }[status] || 'primary'
}

async function loadEvalResultForTask(taskId) {
  if (!taskId) return
  try {
    const res = await getEvalResultByTask(taskId)
    const data = res?.data || res
    await applyHistoricalEvalResult(data)
  } catch (e) {
    currentEvalResult.value = null
    ElMessage.warning(e?.message || '计算完成，但暂未获取到评估结果')
  }
}

async function loadEvalResultById(evalResultId) {
  if (!evalResultId) return false
  try {
    const res = await getEvalResult(evalResultId)
    const data = res?.data || res
    await applyHistoricalEvalResult(data)
    if (data?.taskId) {
      try {
        const taskRes = await getCalcTask(data.taskId)
        applyTaskDetail(taskRes?.data || taskRes)
      } catch {
        calcStatus.value = data.taskRunStatus || 'SUCCESS'
        calcProgress.value = data.taskProgressPercent ?? 100
      }
    }
    return true
  } catch {
    return false
  }
}

async function loadLatestEvalResultForContext() {
  if (!templateId.value || !effectiveIndicatorSystemId.value) return false
  try {
    const res = await listEvalResult({
      pageNum: 1,
      pageSize: 1,
      templateId: templateId.value,
      indicatorSystemId: effectiveIndicatorSystemId.value
    })
    const row = Array.isArray(res?.rows) ? res.rows[0] : null
    if (!row?.id) return false
    return loadEvalResultById(row.id)
  } catch {
    return false
  }
}

async function restoreHistoricalFlowState() {
  let restored = false
  if (initialEvalResultId.value) {
    restored = await loadEvalResultById(initialEvalResultId.value)
  } else if (initialTaskId.value) {
    try {
      const taskRes = await getCalcTask(initialTaskId.value)
      applyTaskDetail(taskRes?.data || taskRes)
      await loadEvalResultForTask(initialTaskId.value)
      restored = !!currentEvalResult.value
    } catch {
      restored = false
    }
  } else {
    restored = await loadLatestEvalResultForContext()
  }
  if (restored) {
    hydrateCompletedStepsFromHistory()
  }
}

function shouldRestoreHistoricalFlowState() {
  if (currentEvalResult.value) return false
  if (initialEvalResultId.value || initialTaskId.value) return true
  if (!currentExecution.value?.id) return true
  return currentExecution.value.status === 'DRAFT'
    && !currentExecution.value.calcTaskId
    && !currentExecution.value.evalResultId
}

async function applyHistoricalEvalResult(data) {
  if (!data) return
  currentEvalResult.value = data
  currentCalcTaskId.value = data.taskId || currentCalcTaskId.value
  taskName.value = taskName.value || data.taskName || ''
  mockCalcResult.score = data.score ?? ''
  mockCalcResult.grade = data.grade || ''
  mockCalcResult.finishTime = formatTime(data.updateTime || data.createTime || currentTaskDetail.value?.endTime)
  calcStatus.value = data.taskRunStatus || calcStatus.value || 'SUCCESS'
  calcProgress.value = data.taskProgressPercent ?? calcProgress.value ?? 100
  if (!calcLogs.value.length || (calcLogs.value.length === 1 && calcLogs.value[0].stage === '历史结果')) {
    calcLogs.value = [{ stage: '评估结果', text: `已加载评估结果 ${data.resultCode || data.id}`, type: 'success', time: mockCalcResult.finishTime }]
  }
  calcStale.value = false
  markStepCompleted(flowStepState, 'comprehensiveCalc', { taskId: data.taskId, evalResultId: data.id })
  buildDetailTree() // 自动构建评分树
  await loadReportInstances()
  await saveExecutionDraft({ silent: true, status: 'SUCCESS', currentStep: reportInstances.value.length ? 'reportOutput' : 'comprehensiveCalc' })
}

function hydrateCompletedStepsFromHistory() {
  replaceFlowStepState(restoreFlowStepState({
    mode: mode.value,
    hasEvalResult: !!currentEvalResult.value,
    hasReports: reportInstances.value.length > 0
  }))
  visitedSteps.add('scheduleConfig')
  if (mode.value === 'execute') visitedSteps.add('weightCalc')
  visitedSteps.add('comprehensiveCalc')
  if (reportInstances.value.length) {
    visitedSteps.add('reportOutput')
  }
  const targetKey = reportInstances.value.length ? 'reportOutput' : 'comprehensiveCalc'
  const idx = visibleSteps.value.findIndex(step => step.key === targetKey)
  if (idx >= 0) currentStepIdx.value = idx
}

function markCalculationStale() {
  if (!currentEvalResult.value && !calcFinished.value) return
  calcStale.value = true
  markStepStale(flowStepState, 'comprehensiveCalc', '结果需重新计算')
  if (reportGenerated.value) {
    markStepStale(flowStepState, 'reportOutput', '上游计算结果已过期，需重新生成')
  } else {
    markStepPending(flowStepState, 'reportOutput')
  }
}

// ==================== Step 4: 报告生成 ====================
const reportTemplateList = ref([])
const reportGenerating = ref(false)
const reportInstances = ref([])
const reportGenerated = computed(() => reportInstances.value.length > 0)
const canGenerateReport = computed(() => calcFinished.value && !calcStale.value && !!currentEvalResult.value?.id && !!stageConfig.reportOutput.config.reportTemplateId)
const reportPreviewVisible = ref(false)
const reportPreviewLoading = ref(false)
const reportPreviewUrl = ref('')
const currentReportLinks = ref(null)

async function generateReport() {
  if (!canGenerateReport.value) return
  reportGenerating.value = true
  try {
    const res = await generateEvalReport(currentEvalResult.value.id, {
      reportTemplateId: stageConfig.reportOutput.config.reportTemplateId,
      mappings: stageConfig.reportOutput.config.placeholderMappings,
      fields: buildReportFields()
    })
    const report = res?.data || res
    if (report?.id) {
      await loadReportInstances()
    }
    reportGenerating.value = false
    markStepCompleted(flowStepState, 'reportOutput', { reportId: report?.id })
    await saveExecutionDraft({ silent: true, status: 'REPORT_READY', currentStep: 'reportOutput' })
    ElMessage.success(`报告已生成${report?.generationNo ? `（第 ${report.generationNo} 版）` : ''}`)
  } catch (e) {
    ElMessage.error(e?.message || '报告生成失败')
  } finally {
    reportGenerating.value = false
  }
}

async function loadReportInstances() {
  if (!currentEvalResult.value?.id) {
    reportInstances.value = []
    markStepPending(flowStepState, 'reportOutput')
    return
  }
  try {
    const res = await listEvalReportsByResult(currentEvalResult.value.id)
    reportInstances.value = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    if (reportInstances.value.length && !calcStale.value) {
      markStepCompleted(flowStepState, 'reportOutput', {
        reportIds: reportInstances.value.map(item => item.id).filter(Boolean)
      })
    } else if (!reportInstances.value.length) {
      markStepPending(flowStepState, 'reportOutput')
    }
  } catch {
    reportInstances.value = []
    markStepPending(flowStepState, 'reportOutput')
  }
}

const VARIABLE_PATTERN = /\{\{\s*([a-zA-Z0-9_.]+)\s*}}/g
function extractPlaceholdersFromHtml(html) {
  const source = html || ''
  const set = new Set()
  let matched
  while ((matched = VARIABLE_PATTERN.exec(source)) !== null) {
    const key = matched[1]
    if (!key || key.startsWith('item.')) continue
    set.add(key)
  }
  return Array.from(set)
}

async function onReportTemplateChange(id, options = {}) {
  if (!options.silent) {
    markReportConfigChanged()
  }
  if (!id) {
    stageConfig.reportOutput.config.placeholderMappings = []
    return
  }
  try {
    const res = await getTemplate(id)
    const data = res.data || {}
    const placeholders = data.placeholders || extractPlaceholdersFromHtml(data.htmlContent)
    
    // 智能合并：保留已有映射，仅补充缺失的
    const existingMappings = stageConfig.reportOutput.config.placeholderMappings || []
    const nextMappings = placeholders.map(key => {
      const exist = existingMappings.find(m => m.key === key)
      if (exist && (exist.mappingValue || exist.mappingType !== 'STATIC_TEXT')) {
        return exist
      }
      // 智能推断映射类型
      if (key === 'taskName' || key === 'ExperimentName') return { key, mappingType: 'TASK_PROPERTY', mappingValue: 'taskName' }
      if (key === 'ExperimentTime' || key === 'startTime') return { key, mappingType: 'TASK_PROPERTY', mappingValue: 'startTime' }
      if (key === 'EvalTarget') return { key, mappingType: 'TASK_PROPERTY', mappingValue: 'evaluateTarget' }
      if (key === 'ReportTitle') return { key, mappingType: 'STATIC_TEXT', mappingValue: '试验评估报告' }
      if (key === 'SubTitle') return { key, mappingType: 'STATIC_TEXT', mappingValue: '综合评估结果' }
      if (key === 'score' || key === 'OverallScore') return { key, mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_score' }
      if (key === 'grade') return { key, mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_grade' }
      if (key === 'conclusion') return { key, mappingType: 'AUTO_INDICATOR', mappingValue: 'overall_conclusion' }
      if (key === 'EvalSnapshot') return { key, mappingType: 'AUTO_INDICATOR', mappingValue: 'eval_result_snapshot' }
      if (key === 'IndicatorTable') return { key, mappingType: 'AUTO_INDICATOR', mappingValue: 'indicator_summary_table' }
      if (key === 'IndicatorTreeJson' || key === 'IndicatorTree') return { key, mappingType: 'AUTO_INDICATOR', mappingValue: 'indicator_tree' }
      return { key, mappingType: 'STATIC_TEXT', mappingValue: '' }
    })
    stageConfig.reportOutput.config.placeholderMappings = nextMappings
  } catch (e) {
    if (!options.silent) ElMessage.error('获取模板占位符失败')
  }
}

function markReportConfigChanged() {
  if (!reportGenerated.value) return
  markStepStale(flowStepState, 'reportOutput', '报告匹配规则已修改，需重新生成')
}

function buildReportFields() {
  const base = {
    taskName: currentEvalResult.value?.taskName || taskName.value,
    score: currentEvalResult.value?.score ?? mockCalcResult.score,
    grade: currentEvalResult.value?.grade || mockCalcResult.grade,
    conclusion: currentEvalResult.value?.conclusion,
    suggestion: currentEvalResult.value?.suggestion,
    indicatorSystemName: currentEvalResult.value?.indicatorSystemName || indicatorSystemDetail.value?.systemName || indicatorSystemDetail.value?.indicatorSystemName,
    dimensions: currentEvalResult.value?.dimensionList || []
  }
  stageConfig.reportOutput.config.placeholderMappings.forEach(row => {
    base[row.key] = resolveReportField(row, base)
  })
  return base
}

function resolveReportField(row, base) {
  if (!row) return ''
  if (row.mappingType === 'STATIC_TEXT') return row.mappingValue || ''
  
  if (row.mappingType === 'TASK_PROPERTY') {
    if (row.mappingValue === 'taskName') return base.taskName || ''
    if (row.mappingValue === 'indicatorSystemName') return base.indicatorSystemName || ''
    if (row.mappingValue === 'startTime') return currentTaskDetail.value?.startTime ? formatTime(currentTaskDetail.value.startTime) : currentEvalResult.value?.startTime ? formatTime(currentEvalResult.value.startTime) : '未记录'
    if (row.mappingValue === 'evaluateTarget') return currentTaskDetail.value?.evaluateTarget || currentEvalResult.value?.evaluateTarget || '当前评估对象'
    return base[row.mappingValue] ?? base[row.key] ?? ''
  }

  if (row.mappingType === 'AUTO_INDICATOR') {
    if (row.mappingValue === 'overall_score') return base.score
    if (row.mappingValue === 'overall_grade') return base.grade
    if (row.mappingValue === 'overall_conclusion') return base.conclusion || '暂无明确结论'
    if (row.mappingValue === 'suggestion') return base.suggestion || '暂无建议'
    if (row.mappingValue === 'indicator_summary_table') return buildIndicatorSummaryTableHtml()
    if (row.mappingValue === 'indicator_tree') return buildIndicatorTreeHtml()
    if (row.mappingValue === 'eval_result_snapshot') return JSON.stringify(base.dimensions || [], null, 2)
  }

  return base[row.key] ?? row.mappingValue ?? ''
}

function buildIndicatorSummaryTableHtml() {
  if (!detailTreeData.value || !detailTreeData.value.length) return '<p style="color: #909399; font-style: italic;">暂无评估指标数据</p>'
  
  let html = '<table border="1" cellpadding="8" cellspacing="0" style="border-collapse: collapse; width: 100%; max-width: 100%; border: 1px solid #dcdfe6; font-family: Helvetica Neue, Arial, sans-serif; font-size: 14px; text-align: center; margin-bottom: 20px;">'
  html += '<tr style="background-color: #f5f7fa; font-weight: bold; color: #303133;">'
  html += '<th style="border: 1px solid #dcdfe6; padding: 10px;">指标结构层级</th>'
  html += '<th style="border: 1px solid #dcdfe6; padding: 10px;">指标名称</th>'
  html += '<th style="border: 1px solid #dcdfe6; padding: 10px;">评估结果(得分)</th>'
  html += '<th style="border: 1px solid #dcdfe6; padding: 10px;">下发权重</th>'
  html += '</tr>'
  
  const drawNode = (nodes, level) => {
    let result = ''
    nodes.forEach(node => {
      // 样式控制：不同层级背景和缩进
      const indentHtml = '<span style="display:inline-block; width:' + (level * 20) + 'px;"></span>'
      const isLeaf = !(node.children && node.children.length > 0)
      const bgColor = level === 0 ? '#fdfdfd' : '#ffffff'
      const fw = level === 0 ? 'bold' : 'normal'
      const weightTxt = typeof node.weight === 'number' ? (node.weight * 100).toFixed(1) + '%' : '-'
      const scoreTxt = node.calculatedScore !== undefined ? '<span style="color: #409EFF; font-weight: bold;">' + node.calculatedScore + '</span>' : '<span style="color:#C0C4CC;">未计算</span>'
      
      result += `<tr style="background-color: ${bgColor}; font-weight: ${fw};">`
      result += `<td style="border: 1px solid #dcdfe6; text-align: left; padding-left: 10px;">${indentHtml}L${level+1}</td>`
      result += `<td style="border: 1px solid #dcdfe6; text-align: left; padding-left: 10px;">${indentHtml}${isLeaf ? '📄 ' : '📁 '}${node.label}</td>`
      result += `<td style="border: 1px solid #dcdfe6;">${scoreTxt}</td>`
      result += `<td style="border: 1px solid #dcdfe6; color: #606266;">${weightTxt}</td>`
      result += `</tr>`
      
      if (!isLeaf) {
        result += drawNode(node.children, level + 1)
      }
    })
    return result
  }
  
  html += drawNode(detailTreeData.value, 0)
  html += '</table>'
  return html
}

function buildIndicatorTreeHtml() {
  if (!detailTreeData.value || !detailTreeData.value.length) return '<p style="color: #909399; font-style: italic;">暂无指标树数据</p>'
  
  let html = '<div style="font-family: Helvetica Neue, Arial, sans-serif; font-size: 14px; line-height: 1.8; color: #303133; background: #fbfcfd; padding: 15px; border-radius: 6px; border: 1px solid #e4e7ed;">'
  
  const getScoreTag = (score) => {
    if (score === undefined || score === null) return ''
    let color = '#F56C6C' // 默认不及格红
    if (score >= 90) color = '#67C23A'      // 优秀绿
    else if (score >= 75) color = '#409EFF' // 良好蓝
    else if (score >= 60) color = '#E6A23C' // 及格黄
    return ` <span style="display:inline-block; margin-left: 10px; padding: 0 8px; font-size: 12px; color: #fff; background-color: ${color}; border-radius: 12px; line-height: 20px;">得 ${score} 分</span>`
  }

  const drawNode = (nodes, level) => {
    let result = ''
    nodes.forEach((node, index) => {
      const isLast = index === nodes.length - 1
      const indentChar = '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
      const prefix = level === 0 ? '🔹 ' : (isLast ? '└─ ' : '├─ ')
      const indent = indentChar.repeat(level)
      const fw = level === 0 ? 'font-weight: bold; font-size: 15px;' : 'font-weight: normal;'
      
      result += `<div style="margin-bottom: 6px; ${fw}">${indent}<span style="color: #909399;">${prefix}</span>${node.label}${getScoreTag(node.calculatedScore)}</div>`
      if (node.children && node.children.length > 0) {
        result += drawNode(node.children, level + 1)
      }
    })
    return result
  }
  
  html += drawNode(detailTreeData.value, 0)
  html += '</div>'
  return html
}

async function openGeneratedReport(row) {
  if (!row?.id) return
  reportPreviewVisible.value = true
  reportPreviewLoading.value = true
  reportPreviewUrl.value = ''
  try {
    const res = await getEvalReportLinks(row.id)
    const links = res?.data || {}
    currentReportLinks.value = links
    
    // 优先使用预览服务地址
    reportPreviewUrl.value = links.previewUrl || ''
    
    if (!reportPreviewUrl.value) {
      if (links.pdfUrl) {
        // 如果预览服务不可用，但存在 pdf 文件，提示使用 pdf 下载预览
        ElMessage.warning('文件预览服务异常，建议直接下载 PDF 报告查看')
      } else {
        ElMessage.warning('暂无可预览地址，可下载 Word 报告查看')
      }
    }
  } catch (e) {
    reportPreviewVisible.value = false
    ElMessage.error(e?.message || '获取报告链接失败')
  } finally {
    reportPreviewLoading.value = false
  }
}

function handleReportDownloadCommand(command, row) {
  downloadGeneratedReport(row, command)
}

async function downloadGeneratedReport(row, format = 'word') {
  if (!row?.id) return
  try {
    const res = await getEvalReportLinks(row.id)
    const links = res?.data || {}
    let url = ''
    let fileName = ''
    
    if (format === 'pdf') {
      url = links.pdfUrl || links.reportUrl
      fileName = `${row.reportCode || `eval_report_${row.id}`}.pdf`
    } else {
      url = links.wordUrl || links.reportUrl
      fileName = `${row.reportCode || `eval_report_${row.id}`}.docx`
    }

    if (!url) {
      ElMessage.warning(`暂无${format === 'pdf' ? ' PDF ' : ' Word '}报告可下载`)
      return
    }
    proxy.download('file/downloadReport', { url }, fileName)
  } catch (e) {
    ElMessage.error(e?.message || '下载报告失败')
  }
}

// ==================== 步骤流转 ====================
const canGoNext = computed(() => {
  if (mode.value === 'design') return true
  // 执行模式下，Step 3 必须完成计算后才能走到 Step 4
  if (currentStep.value === 'comprehensiveCalc') return calcFinished.value && !calcStale.value
  return true
})
const canFinish = computed(() => {
  if (mode.value === 'design') return true
  return isStepCompleted('reportOutput')
})

async function ensureTreeSavedBeforeLeave() {
  if (currentStep.value !== 'weightCalc' || !hasUnsavedTreeChanges.value) return true
  try {
    await ElMessageBox.confirm(
      '当前指标树修改尚未保存到指标体系。离开本步前需要先保存，是否保存并继续？',
      '未保存的指标树修改',
      {
        confirmButtonText: '保存并继续',
        cancelButtonText: '留在本步',
        type: 'warning'
      }
    )
  } catch {
    return false
  }
  return confirmTreeEdits({ keepDialogOpen: true, silent: true })
}

async function nextStep() {
  if (!(await ensureTreeSavedBeforeLeave())) return
  markStepCompleted(flowStepState, currentStep.value)
  if (currentStepIdx.value < visibleSteps.value.length - 1) {
    currentStepIdx.value++
    visitedSteps.add(currentStep.value)
  }
  await saveExecutionDraft({ silent: true })
}
async function prevStep() {
  if (!(await ensureTreeSavedBeforeLeave())) return
  if (currentStepIdx.value > 0) {
    currentStepIdx.value--
    visitedSteps.add(currentStep.value)
  }
  await saveExecutionDraft({ silent: true })
}
async function handleExit() {
  if (!(await ensureTreeSavedBeforeLeave())) return
  await saveExecutionDraft({ silent: true })
  emit('exit')
  if (route.name) router.back()
}
async function goToStep(idx) {
  if (idx === currentStepIdx.value || !canReviewStep(idx)) return
  if (!(await ensureTreeSavedBeforeLeave())) return
  currentStepIdx.value = idx
  visitedSteps.add(currentStep.value)
  await saveExecutionDraft({ silent: true })
}
async function handleFinish() {
  markStepCompleted(flowStepState, currentStep.value)
  await saveExecutionDraft({ silent: true, status: 'REPORT_READY', currentStep: currentStep.value })
  ElMessage.success('流程执行完毕')
  emit('finished', {
    templateId: templateId.value,
    indicatorSystemId: effectiveIndicatorSystemId.value,
    taskId: currentCalcTaskId.value,
    evalResult: currentEvalResult.value,
    reports: reportInstances.value
  })
  setTimeout(() => router.back(), 600)
}

function resetRunnerState() {
  const routeMode = (indicatorSystemId.value || requirementId.value) ? 'execute' : 'design'
  clearTimeout(draftSaveTimer)
  draftSaveTimer = null
  clearCalcPolling()
  clearObjectiveWeightStepTimers()

  executionReady.value = false
  currentExecution.value = null
  taskName.value = props.taskNameProp || route.query.taskName || ''

  templateDetail.value = null
  indicatorSystemDetail.value = null
  runtimePolicy.value = {}
  Object.assign(stageConfig.scheduleConfig.config, {
    routeStrategy: 'FIRST',
    blockStrategy: 'SERIAL_EXECUTION',
    misfireStrategy: 'DO_NOTHING',
    timeoutSeconds: 300,
    retryTimes: 0
  })
  Object.assign(stageConfig.comprehensiveCalc.config, {
    algorithmChainMode: 'SERIAL',
    nullDataPolicy: 'ZERO_FILL',
    intermediateResultOutput: false
  })
  Object.assign(stageConfig.reportOutput.config, {
    reportTemplateId: null,
    placeholderMappings: []
  })

  visitedSteps.clear()
  visitedSteps.add('scheduleConfig')
  currentStepIdx.value = 0
  replaceFlowStepState(createFlowStepState(routeMode))

  weightTreeData.value = []
  selectedTreeNode.value = null
  originalTreeSnapshot.value = null
  treeEditorVisible.value = false
  weightTuningVisible.value = false
  treeSaving.value = false
  objectiveWeightLoading.value = false
  objectiveWeightProgressVisible.value = false
  objectiveWeightStep.value = 0
  objectiveWeightDone.value = false

  calcStatus.value = ''
  calcProgress.value = 0
  calcLogs.value = []
  calcStale.value = false
  currentCalcTaskId.value = null
  currentTaskDetail.value = null
  currentEvalResult.value = null
  mockCalcResult.score = 0
  mockCalcResult.grade = ''
  mockCalcResult.finishTime = ''

  reportGenerating.value = false
  reportInstances.value = []
  reportPreviewVisible.value = false
  reportPreviewLoading.value = false
  reportPreviewUrl.value = ''
  currentReportLinks.value = null

  activeResultTab.value = 'details'
  detailTreeData.value = []
  selectedDetailNode.value = null
}

async function initializeRunnerContext(force = false) {
  if (initializingContext.value && !force) return
  initializingContext.value = true
  try {
    if (currentExecution.value?.id && executionReady.value) {
      await saveExecutionDraft({ silent: true })
    }
    resetRunnerState()
    if (templateId.value) await loadTemplate()
    await loadReportTemplates()
    await initOrLoadExecution()
    if (effectiveIndicatorSystemId.value && !indicatorSystemDetail.value) {
      await loadIndicatorSystem()
    }
    if (shouldRestoreHistoricalFlowState()) {
      await restoreHistoricalFlowState()
    }
  } finally {
    initializingContext.value = false
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  await initializeRunnerContext(true)
})

onBeforeUnmount(() => {
  clearTimeout(draftSaveTimer)
  saveExecutionDraft({ silent: true })
  clearCalcPolling()
  clearObjectiveWeightStepTimers()
})

watch(routeContextKey, async (nextKey, prevKey) => {
  if (!prevKey || nextKey === prevKey) return
  await initializeRunnerContext(true)
})

async function loadTemplate() {
  try {
    const res = await getCalcFlow(templateId.value)
    const data = res.data || res
    templateDetail.value = data
    if (data?.configJson) {
      applyRuntimeConfigJson(data.configJson)
    }
  } catch (e) {
    // 测试页面允许失败回退
    templateDetail.value = { templateName: `(模拟)模板 ${templateId.value}`, taskType: 'EQUIP_EFFECTIVENESS' }
  }
}

async function loadIndicatorSystem(targetIndicatorSystemId = effectiveIndicatorSystemId.value) {
  if (!targetIndicatorSystemId) return
  try {
    const res = await getIndicatorSystem(targetIndicatorSystemId)
    const data = res.data || res
    indicatorSystemDetail.value = data
    const tree = parseIndicatorTreeToForest(data?.indicatorTreeWeight || data?.indicatorTree || data?.treeData)
    if (Array.isArray(tree) && tree.length) {
      weightTreeData.value = tree
    } else {
      weightTreeData.value = buildMockTree(data?.systemName || data?.indicatorSystemName)
    }
    originalTreeSnapshot.value = JSON.parse(JSON.stringify(weightTreeData.value))
  } catch (e) {
    indicatorSystemDetail.value = {
      id: targetIndicatorSystemId,
      systemName: `(模拟)指标体系 ${targetIndicatorSystemId}`
    }
    weightTreeData.value = buildMockTree(indicatorSystemDetail.value.systemName)
    originalTreeSnapshot.value = JSON.parse(JSON.stringify(weightTreeData.value))
  }
}

async function loadReportTemplates() {
  try {
    const res = await listTemplates()
    const list = Array.isArray(res) ? res : (res.data || res.rows || [])
    reportTemplateList.value = list.length ? list : mockReportTemplates()
  } catch {
    reportTemplateList.value = mockReportTemplates()
  }
}

function mockReportTemplates() {
  return [
    { id: 'mock-1', templateName: '(模拟)装备综合评估报告模板 v1.0' },
    { id: 'mock-2', templateName: '(模拟)作战效能评估报告模板 v2.1' }
  ]
}

function formatTime(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString()
}

// ==================== 评分详情查看 ====================
const activeResultTab = ref('details')
const detailTreeData = ref([])
const selectedDetailNode = ref(null)

function handleViewDetail() {
  buildDetailTree()
}

function buildDetailTree() {
  const result = currentEvalResult.value
  if (!result) return
  activeResultTab.value = 'details' // 自动切回明细页
  // 如果有原始指标树，尝试合并得分
  if (weightTreeData.value.length > 0) {
    const tree = JSON.parse(JSON.stringify(weightTreeData.value))
    mergeScoresIntoTree(tree, result.dimensionList)
    detailTreeData.value = tree
    if (tree.length > 0) selectedDetailNode.value = tree[0]
  } else {
    // 否则基于 dimensionList 构建两层树
    const root = {
      uid: 'root_detail',
      label: indicatorSystemDetail.value?.systemName || indicatorSystemDetail.value?.indicatorSystemName || '综合评估',
      calculatedScore: result.score,
      children: (result.dimensionList || []).map((d, idx) => ({
        uid: `dim_${idx}`,
        label: d.label,
        calculatedScore: Number(d.value),
        weight: 0
      }))
    }
    detailTreeData.value = [root]
    selectedDetailNode.value = root
  }
}

function mergeScoresIntoTree(nodes, dimensions) {
  if (!nodes || !nodes.length) return
  nodes.forEach(node => {
    // 优先匹配 dimensionList
    const dim = dimensions?.find(d => d.label === node.label)
    if (dim) {
      node.calculatedScore = Number(dim.value)
    } else if (node.uid === 'root' || nodes.length === 1 && node === nodes[0]) {
      node.calculatedScore = currentEvalResult.value?.score
    }
    if (node.children?.length) {
      mergeScoresIntoTree(node.children, dimensions)
    }
  })
}

function handleDetailNodeClick(data) {
  selectedDetailNode.value = data
}

function getGradeTagType(grade) {
  if (grade === '优秀' || grade === 'A') return 'success'
  if (grade === '良好' || grade === 'B') return 'primary'
  if (grade === '及格' || grade === 'C') return 'warning'
  if (grade === '差' || grade === 'D') return 'danger'
  return 'info'
}

function getScoreClass(score) {
  const s = Number(score)
  if (isNaN(s)) return ''
  if (s >= 90) return 'is-excellent'
  if (s >= 75) return 'is-good'
  if (s >= 60) return 'is-pass'
  return 'is-fail'
}

function getScoreTagType(score) {
  const s = Number(score)
  if (isNaN(s)) return 'info'
  if (s >= 90) return 'success'
  if (s >= 75) return 'primary'
  if (s >= 60) return 'warning'
  return 'danger'
}

function getScoreColor(score) {
  const s = Number(score)
  if (s >= 90) return '#67C23A'
  if (s >= 75) return '#409EFF'
  if (s >= 60) return '#E6A23C'
  return '#F56C6C'
}

function formatScoreToGrade(score) {
  const s = Number(score)
  if (isNaN(s)) return '—'
  if (s >= 90) return '优秀'
  if (s >= 75) return '良好'
  if (s >= 60) return '及格'
  return '未达标'
}

function buildMockTree(rootName) {
  let uid = 0
  const genUid = () => `mock_${++uid}`
  return [
    {
      uid: genUid(),
      label: rootName || '综合评估',
      weight: 1,
      children: [
        { uid: genUid(), label: '任务完成度', weight: 0.4, children: [
          { uid: genUid(), label: '响应时间', weight: 0.5 },
          { uid: genUid(), label: '成功率', weight: 0.5 }
        ]},
        { uid: genUid(), label: '装备可靠性', weight: 0.35, children: [
          { uid: genUid(), label: 'MTBF', weight: 1.0 }
        ]},
        { uid: genUid(), label: '资源消耗', weight: 0.25, children: [
          { uid: genUid(), label: '能耗', weight: 0.6 },
          { uid: genUid(), label: '弹药消耗', weight: 0.4 }
        ]}
      ]
    }
  ]
}
</script>

<style scoped>
.flow-runner-page {
  padding: 16px 20px 20px;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 100px);
}
.runner-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.header-title h2 { margin: 0; font-size: 18px; font-weight: 600; color: #303133; }
.header-sub { font-size: 13px; color: #909399; }
.runner-context { margin-bottom: 18px; }
.runner-steps {
  padding: 18px 40px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 18px;
}
.step-title-btn {
  border: 0;
  background: transparent;
  padding: 0;
  color: inherit;
  font: inherit;
  cursor: default;
}
.step-title-btn.is-clickable {
  cursor: pointer;
}
.step-title-btn.is-clickable:hover,
.step-title-btn.is-current {
  color: #409eff;
}
.step-title-btn:disabled {
  color: inherit;
}
.step-desc {
  font-size: 12px;
  color: #909399;
}
.step-state-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.state-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 12px;
  color: #606266;
  background: #fff;
  border: 1px solid #dcdfe6;
}
.state-pill.success {
  color: #67c23a;
  border-color: #b3e19d;
  background: #f0f9eb;
}
.state-pill.process {
  color: #409eff;
  border-color: #a0cfff;
  background: #ecf5ff;
}
.state-pill.completed {
  color: #67c23a;
  border-color: #b3e19d;
  background: #f0f9eb;
}
.state-pill.running {
  color: #409eff;
  border-color: #a0cfff;
  background: #ecf5ff;
}
.state-pill.stale {
  color: #e6a23c;
  border-color: #f3d19e;
  background: #fdf6ec;
}
.state-pill.failed {
  color: #f56c6c;
  border-color: #fab6b6;
  background: #fef0f0;
}
.state-pill.current {
  font-weight: 600;
}
.runner-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.step-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 24px;
}
.calc-exec-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.runner-footer {
  display: flex;
  justify-content: space-between;
  padding: 12px 0 0;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
}
.footer-right { display: flex; gap: 8px; }

.weight-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.tree-preview {
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 12px 16px;
  background: #fafbfc;
}
.preview-title { font-size: 13px; color: #606266; margin-bottom: 10px; font-weight: 500; }
.weight-tree { background: transparent; }
.tree-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 16px;
}
.tree-label { font-size: 13px; }
.tree-editor-wrap {
  height: calc(100vh - 150px);
  display: flex;
  flex-direction: column;
}
.objective-weight-progress { padding: 4px 8px 0; }
.objective-weight-status {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 13px;
}

.calc-exec-panel {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #dcdfe6;
}
.exec-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.calc-result-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.calc-result-summary.is-stale {
  padding: 10px;
  border: 1px solid #f3d19e;
  border-radius: 6px;
  background: #fdf6ec;
}
.summary-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
}
.summary-label {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}
.exec-log {
  background: #fafbfc;
  border-radius: 6px;
  padding: 16px;
  max-height: 360px;
  overflow-y: auto;
}
.log-title { font-size: 13px; font-weight: 500; color: #606266; margin-bottom: 12px; }
.log-content { font-size: 13px; }
.log-text { color: #606266; margin-left: 8px; }

.report-exec-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #dcdfe6;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.inline-state-alert {
  width: 100%;
}
.hint-text { font-size: 12px; color: #e6a23c; }
.report-history {
  margin-top: 16px;
}
.history-title {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 10px;
}
.report-preview-body { height: 70vh; min-height: 520px; }
.report-preview-frame { width: 100%; height: 100%; border: 0; background: #f5f7fa; }

/* 综合分析计算步骤 - 不需要旧样式，已由下方新样式接管 */

/* 深度重构的分值表现 UI */
.evaluation-dial-section {
  display: flex;
  align-items: center;
  gap: 48px;
  padding: 24px 32px;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 16px;
  margin-bottom: 28px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.dial-wrapper { flex-shrink: 0; }
.dial-dial { position: relative; }
.dial-inner-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-top: -5px;
}
.dial-score-num { font-size: 42px; font-weight: 900; color: #0f172a; line-height: 1; }
.dial-score-unit { font-size: 14px; color: #64748b; margin-top: 2px; }
.dial-grade-label { font-size: 18px; font-weight: 800; }

.performance-analysis { flex: 1; min-width: 0; }
.analysis-title { font-size: 16px; font-weight: 700; color: #1e293b; margin-bottom: 40px; }

.tier-scale { position: relative; height: 16px; margin-bottom: 32px; }
.tier-markers { position: absolute; top: -28px; left: 0; width: 100%; }
.tier-markers span { position: absolute; font-size: 12px; color: #94a3b8; font-weight: 600; transform: translateX(-50%); }

.tier-bar { position: relative; width: 100%; height: 100%; display: flex; border-radius: 8px; overflow: visible; background: #f1f5f9; }
.tier-bar .tier { height: 100%; border-right: 2px solid #fff; }
.tier.fail { background-color: #fcdada; border-radius: 8px 0 0 8px; }
.tier.pass { background-color: #fee2e2; }
.tier.good { background-color: #dcfce7; }
.tier.excellent { background-color: #bbf7d0; border-radius: 0 8px 8px 0; border-right: none; }

.tier-pointer { position: absolute; top: -8px; height: 32px; width: 3px; background: #0f172a; transition: left 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); z-index: 10; border-radius: 2px; }
.pointer-tip { position: absolute; top: -6px; left: -2px; width: 7px; height: 7px; background: #0f172a; transform: rotate(45deg); }
.pointer-box { position: absolute; bottom: -34px; left: 50%; transform: translateX(-50%); padding: 3px 10px; border-radius: 6px; background: #1e293b; color: #fff; font-size: 13px; font-weight: 800; box-shadow: 0 2px 4px rgba(0,0,0,0.2); }

.analysis-detail { font-size: 14px; color: #475569; line-height: 1.8; background: #f8fafc; padding: 12px 16px; border-radius: 8px; border-left: 4px solid #cbd5e1; }
.analysis-detail .status-text { font-weight: 800; margin: 0 4px; }

.node-brief-info { display: flex; gap: 32px; margin-bottom: 24px; }

.sub-indicators { margin-bottom: 20px; }
.sub-title { font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 16px; border-left: 4px solid #10b981; padding-left: 12px; }
.sub-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; padding-bottom: 10px; }
.sub-card-mini { padding: 12px 16px; background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; transition: transform 0.2s; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.sub-card-mini:hover { transform: translateY(-2px); border-color: #cbd5e1; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
.sub-card-head { display: flex; justify-content: space-between; margin-bottom: 8px; }
.sub-name { font-size: 13px; font-weight: 600; color: #334155; }
.sub-val { font-size: 13px; font-weight: 800; }

/* 统一执行状态卡片 */
.unified-exec-card {
  background: #ffffff;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 20px -5px rgba(0, 0, 0, 0.05);
}
.unified-exec-card.is-finished {
  border-bottom: 3px solid #67C23A;
}
.card-main {
  display: flex;
  align-items: center;
  gap: 24px;
}
.run-btn {
  min-width: 120px;
  font-weight: 600;
}
.status-tracker {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 32px;
}
.tracker-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tracker-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.progress-item {
  flex: 1;
  max-width: 450px;
}
.progress-container {
  display: flex;
  align-items: center;
  gap: 12px;
}
.progress-container :deep(.el-progress) {
  flex: 1;
}
.progress-num {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  min-width: 36px;
}
.card-actions {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.card-summary-strip {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #f1f5f9;
  display: flex;
  gap: 40px;
  justify-content: flex-start;
}
.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.summary-item .item-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.summary-item.score .item-icon { background: #f0fdf4; color: #10b981; }
.summary-item.grade .item-icon { background: #eff6ff; color: #3b82f6; }
.summary-item.time .item-icon { background: #f8fafc; color: #64748b; }

.summary-item .item-content {
  display: flex;
  flex-direction: column;
}
.summary-item .label {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 1px;
}
.summary-item .value {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.result-content-wrap {
  display: flex;
  gap: 24px;
  height: clamp(560px, 60vh, 800px);
  margin-top: 0;
}

.result-side-tree {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fbfcfd;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}
.result-side-tree .panel-header {
  padding: 14px 16px;
  font-weight: 700;
  color: #1e293b;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;
  flex-shrink: 0;
}
.result-side-tree .tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px 8px;
}
.result-side-tree .custom-tree-node {
  display: flex;
  justify-content: space-between;
  width: 100%;
  font-size: 13px;
  padding-right: 8px;
}
.result-side-tree .node-score {
  font-family: 'PingFang SC', 'Helvetica Neue', Arial;
  font-weight: 700;
  color: #f56c6c;
}

.result-main-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.result-tabs { flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.result-tabs :deep(.el-tabs__header) { margin-bottom: 0; flex-shrink: 0; }
.result-tabs :deep(.el-tabs__item) { font-size: 14px; font-weight: 600; }
.result-tabs :deep(.el-tabs__content) { flex: 1; overflow-y: auto; padding: 16px 4px; min-height: 0; }
.log-status-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
}
.status-left { display: flex; align-items: center; gap: 12px; }
.task-id { font-size: 13px; color: #94a3b8; font-family: monospace; }
.log-item-detail { padding: 2px 0 8px; }
.item-stage { font-weight: 700; color: #1e293b; margin-bottom: 6px; font-size: 14px; }
.item-text { color: #475569; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }
</style>
