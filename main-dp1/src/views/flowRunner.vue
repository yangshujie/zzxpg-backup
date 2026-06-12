<template>
  <div class="flow-runner-page">
    <!-- 顶部：任务上下文 + 上游流水线摘要 -->
    <!-- 统一导航控制栏：整合返回、标题、步骤条与操作按钮 -->
    <div class="runner-header-v2">
      <!-- 第一行：基础操作与任务信息 -->
      <div class="top-row">
        <div class="row-left">
          <el-button link icon="ArrowLeft" @click="handleExit" class="exit-btn">退出</el-button>
        </div>

        <div class="meta-info">
          <div class="meta-item">
            <span class="m-label">流程模板:</span>
            <span class="m-value">{{ templateDetail?.templateName || '—' }}</span>
          </div>
          <el-divider direction="vertical" />
          <div class="meta-item">
            <span class="m-label">指标体系:</span>
            <span class="m-value">{{ indicatorSystemDetail?.systemName || indicatorSystemDetail?.indicatorSystemName ||
              '—' }}</span>
          </div>
        </div>

        <div class="row-right">
          <el-button-group class="action-group">
            <el-button v-if="currentStepIdx > 0" size="small" icon="ArrowLeft" @click="prevStep">上一步</el-button>

            <el-button v-if="currentStepIdx < visibleSteps.length - 1" type="primary" size="small"
              :disabled="!canGoNext" @click="nextStep">
              下一步 <el-icon class="el-icon--right">
                <ArrowRight />
              </el-icon>
            </el-button>
            <el-button v-else type="success" size="small" :disabled="!canFinish" @click="handleFinish">完成并退出</el-button>
          </el-button-group>
        </div>
      </div>

      <!-- 第二行：流程进度条 -->
      <div class="bottom-row">
        <el-steps :active="currentStepIdx" finish-status="success" align-center>
          <el-step v-for="(s, idx) in visibleSteps" :key="s.key" :status="stepStatus(s.key)">
            <template #icon>
              <span>{{ idx + 1 }}</span>
            </template>
            <template #title>
              <span class="step-label" :class="{ 'is-active': currentStep === s.key }"
                @click="canReviewStep(idx) && goToStep(idx)">
                {{ s.label }}
              </span>
            </template>
          </el-step>
        </el-steps>
      </div>

      <!-- 第三行：分步状态汇总 (融合旧版风格) -->
      <div class="status-summary-bar">
        <div v-for="(s, idx) in visibleSteps" :key="'sum-' + s.key" class="status-summary-item"
          :class="[getStepState(s.key).status.toLowerCase(), { 'is-active': currentStep === s.key }]">
          <span class="s-label">{{ s.label }}:</span>
          <span class="s-status">{{ getStepStatusText(s.key) }}</span>
        </div>
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
            <el-col :span="12">
              <el-form-item label="超时时间(秒)">
                <el-input-number v-model="stageConfig.scheduleConfig.config.timeoutSeconds" :min="0" :max="3600"
                  style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="失败重试次数">
                <el-input-number v-model="stageConfig.scheduleConfig.config.retryTimes" :min="0" :max="10"
                  style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- Step 2: 权重计算 (仅 execute 模式) -->
      <div v-show="currentStep === 'weightCalc'" class="step-panel">
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px;">
          <template #title>
            本步会直接修改指标体系「{{ indicatorSystemDetail?.systemName || indicatorSystemDetail?.indicatorSystemName || '—'
            }}」本体；保存后会持久化回写，并影响后续所有使用该体系的评估任务。
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
          <el-tree :data="weightTreeData" :props="{ label: 'label', children: 'children' }" default-expand-all
            class="weight-tree">
            <template #default="{ data }">
              <span class="tree-row">
                <span class="tree-label">{{ data.label }}</span>
                <el-tag v-if="data.weight != null" size="small" effect="plain">权重 {{ ((data.weight || 0) *
                  100).toFixed(1) }}%</el-tag>
              </span>
            </template>
          </el-tree>
        </div>

        <!-- 树编辑器对话框 -->
        <el-dialog v-model="treeEditorVisible" title="指标树编辑" height="800px" fullscreen append-to-body destroy-on-close
          @opened="onTreeEditorOpened">
          <div class="tree-editor-wrap">
            <IndicatorTreeWorkbench ref="workbenchRef" v-model:tree-data="weightTreeData"
              v-model:selected-node="selectedTreeNode" variant="system" split-height="calc(100vh - 200px)"
              weight-tuning-mode="none" :system-id="effectiveIndicatorSystemId"
              :conduction-algorithm="indicatorSystemDetail?.conductionAlgorithm"
              :global-work-mode="indicatorSystemDetail?.workMode || ''"
              :preferred-root-label="indicatorSystemDetail?.systemName || indicatorSystemDetail?.indicatorSystemName || ''"
              :hide-leaf-calc-method="false" :show-objective-weight="false" :show-indicator-import="false" />
          </div>
          <template #footer>
            <el-button @click="cancelTreeEdits">取 消</el-button>
            <el-button type="primary" :loading="treeSaving" @click="confirmTreeEdits">保存到指标体系</el-button>
          </template>
        </el-dialog>

        <ZhpgWeightWorkbenchDialog v-model="weightTuningVisible" :system-id="effectiveIndicatorSystemId"
          :system-name="indicatorSystemDetail?.systemName || indicatorSystemDetail?.indicatorSystemName || ''"
          @updated="onWeightWorkbenchUpdated" />
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
              <el-form-item label="预处理批次">
                <el-input v-if="urlBatchId !== null" :model-value="batchDisplayName" disabled style="width: 100%" />
                <el-select v-else v-model="stageConfig.comprehensiveCalc.config.batchId"
                  :placeholder="loadingBatches ? '正在加载批次...' : (!requirementId ? '请选择预处理批次' : (batchOptions.length === 0 ? '无需预处理批次' : '请选择预处理批次'))"
                  :disabled="!!requirementId && batchOptions.length === 0 && !loadingBatches" :loading="loadingBatches"
                  @visible-change="handleBatchSelectVisibleChange" style="width: 100%" filterable clearable>
                  <el-option v-for="item in batchOptions" :key="item.id" :label="`${item.batchName} (ID: ${item.id})`"
                    :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="聚合策略来源">
                <el-select v-model="stageConfig.comprehensiveCalc.config.aggregationSource" style="width: 100%">
                  <el-option label="沿用运行时指标体系配置" value="INDICATOR_SYSTEM" />
                  <el-option label="流程覆盖指定" value="TEMPLATE_OVERRIDE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" v-show="stageConfig.comprehensiveCalc.config.aggregationSource === 'TEMPLATE_OVERRIDE'">
              <el-form-item label="覆盖聚合算法">
                <el-select v-model="stageConfig.comprehensiveCalc.config.overrideAggregationAlg" placeholder="请选择聚合算法"
                  filterable clearable style="width: 100%">
                  <el-option v-for="alg in aggregationAlgorithmList" :key="alg.id" :label="alg.algorithmName"
                    :value="alg.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="输出中间结果">
                <el-switch v-model="stageConfig.comprehensiveCalc.config.intermediateResultOutput" />
              </el-form-item>
            </el-col>
          </el-row>
          <!-- 评分等级配置（高颜值只读展示面板） -->
          <div class="score-level-display-panel"
            style="margin-top: 20px; border-top: 1px dashed var(--el-border-color-lighter); padding-top: 18px;">
            <div class="section-title"
              style="font-size: 13px; margin-bottom: 14px; color: var(--el-text-color-regular); font-weight: 600; display: flex; align-items: center; gap: 6px;">
              <el-icon style="color: var(--el-color-primary); font-size: 14px;">
                <Grid />
              </el-icon>
              <span>评估分级策略</span>
            </div>
            <div class="score-level-cards-wrapper">
              <el-row :gutter="12">
                <el-col v-for="(level, index) in sortedScoreLevels" :key="index" :xs="24" :sm="12" :md="6"
                  style="margin-bottom: 12px;">
                  <div class="score-level-premium-card" :style="{ borderColor: getLevelStyle(level.name).border }">
                    <div class="card-left-section">
                      <span class="level-badge" :style="{
                        backgroundColor: getLevelStyle(level.name).bg,
                        color: getLevelStyle(level.name).color
                      }">
                        {{ level.name }}
                      </span>
                    </div>
                    <div class="card-right-section">
                      <div class="threshold-label">分值要求</div>
                      <div class="threshold-value" :style="{ color: getLevelStyle(level.name).color }">
                        <span class="symbol">得分 ≥</span>
                        <span class="number">{{ level.threshold }}</span>
                        <span class="unit">分</span>
                      </div>
                    </div>
                    <!-- 炫光微修饰 -->
                    <div class="glow-layer"
                      :style="{ background: `radial-gradient(circle at 100% 0%, ${getLevelStyle(level.name).bg} 0%, transparent 70%)` }">
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
        </el-form>

        <!-- 执行控制 + 日志面板（仅执行模式） -->
        <div v-if="mode === 'execute'" class="calc-exec-panel">
          <el-alert v-if="isStepStale('comprehensiveCalc')" type="warning" :closable="false" show-icon
            style="margin-bottom: 12px;">
            <template #title>{{ stepStaleReason('comprehensiveCalc') || '当前计算结果已过期，请重新发起计算。' }}</template>
          </el-alert>
          <!-- 统一执行状态卡片 -->
          <div class="unified-exec-card-compact" :class="{ 'is-finished': calcFinished }">
            <div class="status-section">
              <div class="status-label-group">
                <span class="s-label">执行状态</span>
                <el-tag :type="calcTagType" effect="dark" round size="small">{{ calcStatusLabel || '未开始' }}</el-tag>
              </div>
              <div class="progress-wrap">
                <span class="progress-num">{{ Math.round(calcProgress) }}%</span>
                <el-progress :percentage="calcProgress"
                  :status="calcStatus === 'FAILED' ? 'exception' : (calcProgress >= 100 ? 'success' : '')"
                  :stroke-width="6" :show-text="false" />
              </div>
            </div>

            <div class="metrics-section" v-if="currentEvalResult">
              <el-divider direction="vertical" />
              <div class="metric-item">
                <span class="m-label">综合得分</span>
                <span class="m-value score">{{ mockCalcResult.score || '—' }}</span>
              </div>
              <div class="metric-item">
                <span class="m-label">评估等级</span>
                <el-tag :type="getGradeTagType(mockCalcResult.grade)" size="small" effect="plain">{{
                  mockCalcResult.grade ||
                  '—' }}</el-tag>
              </div>
              <div class="metric-item time">
                <span class="m-label">完成时间</span>
                <span class="m-value">{{ mockCalcResult.finishTime || '—' }}</span>
              </div>
            </div>

            <div class="action-section">
              <el-button v-if="currentStep === 'comprehensiveCalc'" type="warning" size="small" icon="Refresh"
                :loading="apiLoading" :disabled="isStartBtnDisabled" @click="startCalc" class="recalc-btn">
                {{ startBtnLabel }}
              </el-button>
              <el-button type="primary" link icon="List" @click="calcLogDrawerVisible = true"
                class="log-btn">过程日志</el-button>
            </div>
          </div>

          <div v-if="currentEvalResult" class="result-content-wrap">
            <div class="result-side-tree">
              <div class="panel-header">指标层级</div>
              <div class="tree-container">
                <el-tree :data="detailTreeData" :props="{ label: 'label', children: 'children' }" node-key="uid"
                  highlight-current default-expand-all @node-click="handleDetailNodeClick">
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
              <div class="result-detail-page" v-if="selectedDetailNode">
                <section class="detail-section">
                  <div class="detail-section-title">
                    <span class="section-title-main">评分明细</span>
                    <span class="section-title-sub">当前指标的得分表现</span>
                  </div>
                  <div class="detail-pane-content">
                    <div class="node-brief-info">
                      <div class="brief-item">
                        <span class="brief-label">指标名称：</span>
                        <span class="brief-value">{{ selectedDetailNode.label }}</span>
                      </div>
                      <div class="brief-item">
                        <span class="brief-label">得分：</span>
                        <span class="brief-value" :class="getScoreClass(selectedDetailNode.calculatedScore)">{{
                          selectedDetailNode.calculatedScore }}</span>
                      </div>
                    </div>

                    <!-- 专业分值仪表盘 -->
                    <div class="evaluation-dial-section" v-if="selectedDetailNode.calculatedScore !== undefined">
                      <div class="dial-wrapper">
                        <div class="dial-dial">
                          <el-progress type="dashboard" :percentage="selectedDetailNode.calculatedScore || 0"
                            :color="getScoreColor(selectedDetailNode.calculatedScore)" :width="150" :stroke-width="12">
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
                            <div class="tier-pointer"
                              :style="{ left: (selectedDetailNode.calculatedScore || 0) + '%' }">
                              <div class="pointer-tip"></div>
                              <div class="pointer-box">{{ selectedDetailNode.calculatedScore }}</div>
                            </div>
                          </div>
                        </div>
                        <div class="analysis-detail">
                          当前指标处于 <span class="status-text"
                            :style="{ color: getScoreColor(selectedDetailNode.calculatedScore) }">
                            {{ formatScoreToGrade(selectedDetailNode.calculatedScore) }}
                          </span> 状态。该分值代表参评对象在当前指标域下的综合能力表现水平。
                        </div>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="detail-section">
                  <div class="detail-section-title trace-heading">
                    <span class="section-title-main">结果溯源</span>
                    <span class="section-title-sub">计算来源、算法和连接关系</span>
                  </div>
                  <div class="trace-pane-content">
                    <!-- 情况 A: 根节点或中间节点 (展示权重分布与聚合逻辑) -->
                    <template v-if="selectedDetailNode.children?.length">
                      <div class="trace-section">
                        <div class="trace-title">子指标权重分布</div>
                        <el-table :data="selectedDetailNode.children" size="small" border class="trace-table"
                          show-summary :summary-method="getTraceTableSummary">
                          <el-table-column prop="label" label="指标名称" min-width="150" />
                          <el-table-column label="权重" width="100">
                            <template #default="{ row }">
                              <el-tag type="info" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
                            </template>
                          </el-table-column>
                          <el-table-column prop="calculatedScore" label="评估分值" width="100">
                            <template #default="{ row }">
                              <span :class="getScoreClass(row.calculatedScore)"
                                style="font-family: monospace; font-weight: bold;">
                                {{ row.calculatedScore != null ? row.calculatedScore : '—' }}
                              </span>
                            </template>
                          </el-table-column>
                          <el-table-column label="贡献分值" width="100">
                            <template #default="{ row }">
                              <span style="color: var(--border-color-hover); font-weight: bold;">
                                {{ row.weight != null && row.calculatedScore != null ? (row.weight *
                                  row.calculatedScore).toFixed(2) : '—' }}
                              </span>
                            </template>
                          </el-table-column>
                        </el-table>
                        <div class="total-score-hint">
                          <span class="label">本级汇总得分:</span>
                          <span class="value" :class="getScoreClass(selectedDetailNode.calculatedScore)">{{
                            selectedDetailNode.calculatedScore }}</span>
                          <span class="formula"> (Σ 子指标分值 × 权重)</span>
                        </div>
                      </div>

                      <div class="trace-section">
                        <div class="trace-title">权重分配算法溯源</div>
                        <div class="method-card">
                          <div class="method-card-v2">
                            <div class="m-header">
                              <div class="m-title-area">
                                <el-icon class="m-icon">
                                  <Setting />
                                </el-icon>
                                <span class="m-name">{{
                                  getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).algorithmName
                                }}</span>
                                <el-tag v-if="getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).algorithmCode"
                                  size="small" effect="plain" class="m-code-tag">
                                  {{ getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).algorithmCode }}
                                </el-tag>
                              </div>
                              <div class="m-meta" v-if="getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).author">
                                <span class="label">作者:</span>
                                <span class="value">{{ getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).author
                                }}</span>
                              </div>
                            </div>

                            <div class="m-body">
                              <div class="m-desc-label">算法逻辑描述</div>
                              <p class="m-description">
                                {{ getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).algorithmDescription ||
                                  '通过指标间的相关性或专家赋权逻辑进行权重分配。' }}
                              </p>
                            </div>

                            <div class="m-footer"
                              v-if="getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).algorithmCodeUrl">
                              <div class="m-path">
                                <el-icon>
                                  <Collection />
                                </el-icon>
                                <span class="path-text">{{
                                  getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).algorithmCodeUrl }}</span>
                              </div>
                              <el-button v-if="getAlgoInfo(selectedDetailNode.weightAssignAlgorithm).id" type="primary"
                                size="small" icon="View" class="m-preview-btn"
                                @click="handlePreviewAlgo(selectedDetailNode.weightAssignAlgorithm)">源码预览</el-button>
                            </div>
                          </div>

                          <div class="params-table-wrap"
                            v-if="selectedDetailNode.weightAssignAlgorithmParams && Object.keys(selectedDetailNode.weightAssignAlgorithmParams).length">
                            <div class="sub-trace-title">执行参数配置</div>
                            <el-descriptions :column="2" border size="small">
                              <el-descriptions-item v-for="(val, key) in selectedDetailNode.weightAssignAlgorithmParams"
                                :key="key" :label="key">
                                <code class="code-text">{{ val }}</code>
                              </el-descriptions-item>
                            </el-descriptions>
                          </div>
                        </div>
                      </div>

                      <div class="trace-section">
                        <div class="trace-title">聚合计算依据</div>
                        <div class="method-card">
                          <div class="method-card-v2 aggregation">
                            <div class="m-header">
                              <div class="m-title-area">
                                <el-icon class="m-icon">
                                  <Refresh />
                                </el-icon>
                                <span class="m-name">{{
                                  getAlgoInfo(selectedDetailNode.conductionAlgorithm).algorithmName ||
                                  '加权算术平均' }}</span>
                                <el-tag v-if="getAlgoInfo(selectedDetailNode.conductionAlgorithm).algorithmCode"
                                  size="small" effect="plain" class="m-code-tag">
                                  {{ getAlgoInfo(selectedDetailNode.conductionAlgorithm).algorithmCode }}
                                </el-tag>
                              </div>
                            </div>

                            <div class="m-body">
                              <div class="m-desc-label">聚合计算逻辑</div>
                              <p class="m-description">
                                {{ getAlgoInfo(selectedDetailNode.conductionAlgorithm).algorithmDescription ||
                                  '基于下级指标的权重与分值乘积进行线性累加，计算公式：Score = Σ(ChildScore_i * Weight_i)' }}
                              </p>
                            </div>

                            <div class="m-footer"
                              v-if="getAlgoInfo(selectedDetailNode.conductionAlgorithm).algorithmCodeUrl">
                              <div class="m-path">
                                <el-icon>
                                  <Collection />
                                </el-icon>
                                <span class="path-text">{{
                                  getAlgoInfo(selectedDetailNode.conductionAlgorithm).algorithmCodeUrl
                                }}</span>
                              </div>
                              <el-button v-if="getAlgoInfo(selectedDetailNode.conductionAlgorithm).id" type="primary"
                                size="small" icon="View" class="m-preview-btn"
                                @click="handlePreviewAlgo(selectedDetailNode.conductionAlgorithm)">源码预览</el-button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </template>

                    <!-- 情况 B: 叶子节点 (展示分值与计算链条) -->
                    <template v-else>
                      <div class="trace-section" v-if="selectedDetailNode.computeRule">
                        <div class="trace-title">计算链条配置 (Topological Flow)</div>
                        <div class="trace-flow-canvas"
                          :class="{ 'is-panning': traceFlowViewport.dragging, 'is-transforming': traceFlowViewport.transforming }"
                          :style="{ height: `${selectedTraceFlowGraph.viewportHeight}px` }"
                          @wheel.prevent="handleTraceFlowWheel" @mousedown="startTraceFlowPan"
                          @dblclick="resetTraceFlowViewport">
                          <div class="trace-flow-content" :style="{
                            width: `${selectedTraceFlowGraph.canvasWidth}px`,
                            height: `${selectedTraceFlowGraph.canvasHeight}px`,
                            transform: traceFlowTransform
                          }">
                            <svg class="trace-flow-lines" :width="selectedTraceFlowGraph.canvasWidth"
                              :height="selectedTraceFlowGraph.canvasHeight"
                              :viewBox="`0 0 ${selectedTraceFlowGraph.canvasWidth} ${selectedTraceFlowGraph.canvasHeight}`">
                              <defs>
                                <marker id="trace-flow-arrow" markerWidth="12" markerHeight="12" refX="10" refY="6"
                                  orient="auto" markerUnits="strokeWidth">
                                  <path d="M0,0 L0,12 L10,6 z" />
                                </marker>
                              </defs>
                              <path v-for="edge in selectedTraceFlowGraph.edges" :key="edge.key" class="trace-flow-edge"
                                :d="edge.path" />
                            </svg>

                            <div v-for="node in selectedTraceFlowGraph.nodes" :key="node.id" class="trace-flow-node"
                              :class="node.type"
                              :style="{ left: `${node.x}px`, top: `${node.y}px`, width: `${node.width}px`, height: `${node.height}px` }">
                              <div class="step-box" :class="node.type">
                                <div class="step-icon">
                                  <el-icon v-if="node.type === 'start'">
                                    <Clock />
                                  </el-icon>
                                  <el-icon v-else-if="node.type === 'algo'">
                                    <Setting />
                                  </el-icon>
                                  <el-icon v-else-if="node.type === 'result'">
                                    <Medal />
                                  </el-icon>
                                </div>
                                <div class="step-main">
                                  <div class="step-head">
                                    <span class="step-type">{{ node.type === 'start' ? '原始数据源' : node.type === 'algo' ?
                                      '算法处理' : '计算结果' }}</span>
                                  </div>
                                  <div class="step-body">
                                    <template v-if="node.type === 'start'">
                                      <div class="node-title">{{ node.name || node.value || '数据源' }}</div>
                                      <div class="source-meta-grid">
                                        <span v-if="node.source">分中心：{{ getZhpgEquipmentTypeLabel(node.source) }}</span>
                                        <span v-if="node.value">目录：<code>{{ node.value }}</code></span>
                                        <span v-if="node.taskStage">阶段：{{ node.taskStage }}</span>
                                        <span v-if="node.experimentalTask">任务：{{ node.experimentalTask }}</span>
                                      </div>
                                      <div v-if="formatDataSourceFields(node).length" class="field-chip-list">
                                        <el-tooltip v-for="field in formatDataSourceFields(node)" :key="field.key"
                                          :content="field.tooltip" placement="top">
                                          <el-tag size="small" effect="plain" type="success">{{ field.label }}</el-tag>
                                        </el-tooltip>
                                      </div>
                                      <div v-if="formatTimeRange(node)" class="source-desc">时间：{{ formatTimeRange(node)
                                      }}</div>
                                      <div v-if="node.description" class="source-desc">{{ node.description }}</div>
                                    </template>
                                    <template v-else-if="node.type === 'algo'">
                                      <div class="algo-name">算法：<strong>{{ getAlgorithmName(node) }}</strong></div>
                                      <div class="source-meta-grid">
                                        <span v-if="node.algoType">类型：{{ node.algoType }}</span>
                                        <span
                                          v-if="getAlgorithmFullInfo(node)?.algorithmCode">代码：<code>{{ getAlgorithmFullInfo(node).algorithmCode }}</code></span>
                                        <span v-if="getAlgorithmFullInfo(node)?.author">作者：{{
                                          getAlgorithmFullInfo(node).author }}</span>
                                      </div>
                                      <div class="algo-url"
                                        v-if="node.url || getAlgorithmFullInfo(node)?.algorithmCodeUrl">
                                        路径：<code>{{ node.url || getAlgorithmFullInfo(node)?.algorithmCodeUrl }}</code>
                                      </div>
                                      <div v-if="getAlgorithmFullInfo(node)?.algorithmDescription" class="source-desc">
                                        {{ getAlgorithmFullInfo(node).algorithmDescription }}</div>
                                      <div class="algo-params" v-if="getAlgorithmParams(node).length">
                                        <div class="param-tag-list">
                                          <el-tag v-for="p in getAlgorithmParams(node)" :key="p.field || p.name"
                                            size="small" effect="plain" type="info">
                                            {{ p.description || p.name || p.field }}: {{ p.defaultValue || '—' }}
                                          </el-tag>
                                        </div>
                                      </div>
                                      <el-button v-if="getAlgorithmPreviewId(node)" link type="primary" icon="View"
                                        size="small" class="inline-preview-btn"
                                        @click.stop="handlePreviewAlgo(getAlgorithmPreviewId(node))">算法预览</el-button>
                                    </template>
                                    <template v-else-if="node.type === 'result'">
                                      <div class="node-title">输出指标分值</div>
                                      <div class="result-label">当前得分：<strong>{{ selectedDetailNode.calculatedScore
                                      }}</strong></div>
                                      <div class="source-desc">连接链路执行完成后，将最终输出映射为该叶子指标的计算得分。</div>
                                    </template>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div class="trace-section" v-if="selectedPreprocessPreview.length">
                        <div class="trace-title">数据字段预处理对比</div>
                        <div class="preprocess-compare-panel">
                          <div class="preprocess-toolbar">
                            <div class="preprocess-toolbar-title">字段曲线预览</div>
                            <el-radio-group v-model="preprocessChartMode" size="small">
                              <el-radio-button label="overlay">叠加对比</el-radio-button>
                              <el-radio-button label="split">分离查看</el-radio-button>
                            </el-radio-group>
                          </div>
                          <div class="preprocess-card-grid">
                            <div v-for="field in selectedPreprocessPreview" :key="field.key"
                              class="preprocess-field-card">
                              <div class="field-card-head">
                                <div>
                                  <div class="field-name">{{ field.label }}</div>
                                  <div class="field-source">{{ field.sourceName }}</div>
                                </div>
                                <div class="field-card-actions">
                                  <el-tag size="small" effect="plain" type="info">{{ field.code }}</el-tag>
                                  <el-button v-if="field.hasData" link type="primary" size="small" class="full-data-btn"
                                    @click.stop="openPreprocessDataDetail(field)">
                                    查看全部数据
                                  </el-button>
                                </div>
                              </div>

                              <div class="curve-compare" v-loading="field.isFetching">
                                <template v-if="field.hasData">
                                  <div class="curve-legend" v-if="field.isNumeric">
                                    <span v-if="field.chartSampled" class="legend-note">图形已抽样，完整数据见明细</span>
                                    <span class="legend-item before">预处理前</span>
                                    <span class="legend-item after">预处理后</span>
                                  </div>

                                  <!-- 情况 1: 数值型数据，展示曲线图 -->
                                  <template v-if="field.isNumeric">
                                    <div v-if="preprocessChartMode === 'overlay'" class="curve-chart"
                                      :class="{ 'is-panning': preprocessChartViewport.dragging }"
                                      :data-chart-height="300" :data-chart-top="field.chart.top"
                                      :data-chart-bottom="field.chart.bottom"
                                      @wheel.prevent="handlePreprocessChartWheel" @mousedown="startPreprocessChartPan"
                                      @dblclick="resetPreprocessChartViewport">
                                      <svg viewBox="0 0 1200 300" class="axis-curve">
                                        <defs>
                                          <clipPath :id="`preprocess-chart-clip-${field.key}`">
                                            <rect :x="field.chart.left" :y="field.chart.top"
                                              :width="field.chart.right - field.chart.left"
                                              :height="field.chart.bottom - field.chart.top" />
                                          </clipPath>
                                          <linearGradient id="grad-before" x1="0%" y1="0%" x2="0%" y2="100%">
                                            <stop offset="0%" stop-color="var(--pre-chart-before)"
                                              stop-opacity="var(--pre-chart-before-area)" />
                                            <stop offset="100%" stop-color="var(--pre-chart-before)" stop-opacity="0" />
                                          </linearGradient>
                                          <linearGradient id="grad-after" x1="0%" y1="0%" x2="0%" y2="100%">
                                            <stop offset="0%" stop-color="var(--pre-chart-after)"
                                              stop-opacity="var(--pre-chart-after-area)" />
                                            <stop offset="100%" stop-color="var(--pre-chart-after)" stop-opacity="0" />
                                          </linearGradient>
                                        </defs>

                                        <!-- 固定层：网格、坐标轴与当前视口刻度 -->
                                        <g class="chart-fixed-y">
                                          <g class="curve-grid y-grid">
                                            <line v-for="tick in getPreprocessViewportYTicks(field.chart)"
                                              :key="`y-${tick.y}`" :x1="field.chart.left" :x2="field.chart.right"
                                              :y1="tick.y" :y2="tick.y" />
                                          </g>
                                          <g class="curve-grid x-grid">
                                            <line v-for="tick in getPreprocessViewportXTicks(field.chart)"
                                              :key="`xg-${tick.index}`" :x1="tick.x" :x2="tick.x" :y1="field.chart.top"
                                              :y2="field.chart.bottom" />
                                          </g>
                                          <g class="curve-axis">
                                            <line :x1="field.chart.left" :x2="field.chart.left" :y1="field.chart.top"
                                              :y2="field.chart.bottom" />
                                            <line :x1="field.chart.left" :x2="field.chart.right"
                                              :y1="field.chart.bottom" :y2="field.chart.bottom" />
                                          </g>
                                          <g class="curve-labels y-labels">
                                            <text v-for="tick in getPreprocessViewportYTicks(field.chart)"
                                              :key="`yl-${tick.y}`" :x="field.chart.left - 9" :y="tick.y + 4">{{
                                                tick.label }}</text>
                                          </g>
                                          <g class="curve-labels x-labels">
                                            <text v-for="tick in getPreprocessViewportXTicks(field.chart)"
                                              :key="`xl-${tick.index}`" :x="tick.x" :y="field.chart.bottom + 24">{{
                                                tick.label }}</text>
                                          </g>
                                        </g>

                                        <!-- 数据层：只水平缩放/平移，坐标轴保持固定 -->
                                        <g :clip-path="`url(#preprocess-chart-clip-${field.key})`">
                                          <g :transform="getPreprocessDataTransform(field.chart)">
                                            <polygon v-if="field.before.polyline" class="area-before"
                                              :points="`${field.before.polyline} ${field.chart.right},${field.chart.bottom} ${field.chart.left},${field.chart.bottom}`"
                                              fill="url(#grad-before)" />
                                            <polygon v-if="field.after.polyline" class="area-after"
                                              :points="`${field.after.polyline} ${field.chart.right},${field.chart.bottom} ${field.chart.left},${field.chart.bottom}`"
                                              fill="url(#grad-after)" />

                                            <polyline class="line-before" :points="field.before.polyline" />
                                            <polyline class="line-after" :points="field.after.polyline" />

                                            <g v-for="p in field.before.points" :key="`bp-${p.index}`"
                                              class="chart-node before">
                                              <ellipse :cx="p.x" :cy="p.y" :rx="getPreprocessPointRadius(3)"
                                                :ry="getPreprocessPointRadius(3)" />
                                              <title>{{ `预处理前\n值: ${p.value}\n时间: ${p.timestamp}` }}</title>
                                            </g>
                                            <g v-for="p in field.after.points" :key="`ap-${p.index}`"
                                              class="chart-node after">
                                              <ellipse :cx="p.x" :cy="p.y" :rx="getPreprocessPointRadius(3)"
                                                :ry="getPreprocessPointRadius(3)" />
                                              <title>{{ `预处理后\n值: ${p.value}\n时间: ${p.timestamp}` }}</title>
                                            </g>
                                          </g>
                                        </g>
                                      </svg>
                                    </div>

                                    <div v-else class="split-chart-grid">
                                      <div class="split-chart before"
                                        :class="{ 'is-panning': preprocessChartViewport.dragging }"
                                        :data-chart-height="160" :data-chart-top="field.beforeChart.top"
                                        :data-chart-bottom="field.beforeChart.bottom"
                                        @wheel.prevent="handlePreprocessChartWheel" @mousedown="startPreprocessChartPan"
                                        @dblclick="resetPreprocessChartViewport">
                                        <div class="split-chart-title">预处理前</div>
                                        <svg viewBox="0 0 1200 160" preserveAspectRatio="none"
                                          class="axis-curve compact">
                                          <defs>
                                            <clipPath :id="`preprocess-before-clip-${field.key}`">
                                              <rect :x="field.beforeChart.left" :y="field.beforeChart.top"
                                                :width="field.beforeChart.right - field.beforeChart.left"
                                                :height="field.beforeChart.bottom - field.beforeChart.top" />
                                            </clipPath>
                                          </defs>
                                          <g class="curve-grid">
                                            <line v-for="tick in getPreprocessViewportYTicks(field.beforeChart)"
                                              :key="`by-${tick.y}`" :x1="field.beforeChart.left"
                                              :x2="field.beforeChart.right" :y1="tick.y" :y2="tick.y" />
                                            <line v-for="tick in getPreprocessViewportXTicks(field.beforeChart)"
                                              :key="`bxg-${tick.index}`" :x1="tick.x" :x2="tick.x"
                                              :y1="field.beforeChart.top" :y2="field.beforeChart.bottom" />
                                          </g>
                                          <g class="curve-axis">
                                            <line :x1="field.beforeChart.left" :x2="field.beforeChart.left"
                                              :y1="field.beforeChart.top" :y2="field.beforeChart.bottom" />
                                            <line :x1="field.beforeChart.left" :x2="field.beforeChart.right"
                                              :y1="field.beforeChart.bottom" :y2="field.beforeChart.bottom" />
                                          </g>
                                          <g class="curve-labels y-labels">
                                            <text v-for="tick in getPreprocessViewportYTicks(field.beforeChart)"
                                              :key="`byl-${tick.y}`" :x="field.beforeChart.left - 9" :y="tick.y + 4">{{
                                                tick.label }}</text>
                                          </g>
                                          <g class="curve-labels x-labels compact-x-labels">
                                            <text v-for="tick in getPreprocessViewportXTicks(field.beforeChart, 4)"
                                              :key="`bxl-${tick.index}`" :x="tick.x"
                                              :y="field.beforeChart.bottom + 18">{{ tick.label }}</text>
                                          </g>
                                          <g :clip-path="`url(#preprocess-before-clip-${field.key})`">
                                            <g :transform="getPreprocessDataTransform(field.beforeChart)">
                                              <polyline class="line-before" :points="field.beforeChart.polyline" />
                                              <g v-for="p in field.beforeChart.points" :key="`bs-${p.index}`"
                                                class="chart-node before">
                                                <ellipse :cx="p.x" :cy="p.y" :rx="getPreprocessPointRadius(2.8)"
                                                  :ry="getPreprocessPointRadius(2.8)" />
                                                <title>{{ `预处理前\n值: ${p.value}\n时间: ${p.timestamp}` }}</title>
                                              </g>
                                            </g>
                                          </g>
                                        </svg>
                                      </div>
                                      <div class="split-chart after"
                                        :class="{ 'is-panning': preprocessChartViewport.dragging }"
                                        :data-chart-height="160" :data-chart-top="field.afterChart.top"
                                        :data-chart-bottom="field.afterChart.bottom"
                                        @wheel.prevent="handlePreprocessChartWheel" @mousedown="startPreprocessChartPan"
                                        @dblclick="resetPreprocessChartViewport">
                                        <div class="split-chart-title">预处理后</div>
                                        <svg viewBox="0 0 1200 160" preserveAspectRatio="none"
                                          class="axis-curve compact">
                                          <defs>
                                            <clipPath :id="`preprocess-after-clip-${field.key}`">
                                              <rect :x="field.afterChart.left" :y="field.afterChart.top"
                                                :width="field.afterChart.right - field.afterChart.left"
                                                :height="field.afterChart.bottom - field.afterChart.top" />
                                            </clipPath>
                                          </defs>
                                          <g class="curve-grid">
                                            <line v-for="tick in getPreprocessViewportYTicks(field.afterChart)"
                                              :key="`ay-${tick.y}`" :x1="field.afterChart.left"
                                              :x2="field.afterChart.right" :y1="tick.y" :y2="tick.y" />
                                            <line v-for="tick in getPreprocessViewportXTicks(field.afterChart)"
                                              :key="`axg-${tick.index}`" :x1="tick.x" :x2="tick.x"
                                              :y1="field.afterChart.top" :y2="field.afterChart.bottom" />
                                          </g>
                                          <g class="curve-axis">
                                            <line :x1="field.afterChart.left" :x2="field.afterChart.left"
                                              :y1="field.afterChart.top" :y2="field.afterChart.bottom" />
                                            <line :x1="field.afterChart.left" :x2="field.afterChart.right"
                                              :y1="field.afterChart.bottom" :y2="field.afterChart.bottom" />
                                          </g>
                                          <g class="curve-labels y-labels">
                                            <text v-for="tick in getPreprocessViewportYTicks(field.afterChart)"
                                              :key="`ayl-${tick.y}`" :x="field.afterChart.left - 9" :y="tick.y + 4">{{
                                                tick.label }}</text>
                                          </g>
                                          <g class="curve-labels x-labels compact-x-labels">
                                            <text v-for="tick in getPreprocessViewportXTicks(field.afterChart, 4)"
                                              :key="`axl-${tick.index}`" :x="tick.x"
                                              :y="field.afterChart.bottom + 18">{{ tick.label }}</text>
                                          </g>
                                          <g :clip-path="`url(#preprocess-after-clip-${field.key})`">
                                            <g :transform="getPreprocessDataTransform(field.afterChart)">
                                              <polyline class="line-after" :points="field.afterChart.polyline" />
                                              <g v-for="p in field.afterChart.points" :key="`as-${p.index}`"
                                                class="chart-node after">
                                                <ellipse :cx="p.x" :cy="p.y" :rx="getPreprocessPointRadius(2.8)"
                                                  :ry="getPreprocessPointRadius(2.8)" />
                                                <title>{{ `预处理后\n值: ${p.value}\n时间: ${p.timestamp}` }}</title>
                                              </g>
                                            </g>
                                          </g>
                                        </svg>
                                      </div>
                                    </div>
                                  </template>

                                  <!-- 情况 2: 非数值型数据（如时间戳），展示对比表 -->
                                  <div v-else class="non-numeric-compare">
                                    <div class="compare-table-grid">
                                      <div class="compare-col">
                                        <div class="col-title">预处理前 (预览 {{ field.beforeRecords.length }}/{{
                                          field.beforeTotal }})</div>
                                        <el-table :data="field.beforeRecords" size="small" border>
                                          <el-table-column label="序号" type="index" width="50" />
                                          <el-table-column prop="value" label="原始值" show-overflow-tooltip />
                                        </el-table>
                                      </div>
                                      <div class="compare-col">
                                        <div class="col-title">预处理后 (预览 {{ field.afterRecords.length }}/{{
                                          field.afterTotal }})</div>
                                        <el-table :data="field.afterRecords" size="small" border>
                                          <el-table-column label="序号" type="index" width="50" />
                                          <el-table-column prop="value" label="处理值" show-overflow-tooltip />
                                        </el-table>
                                      </div>
                                    </div>
                                    <div class="table-hint" v-if="field.beforeRecords.length">当前为预览数据，完整明细可从右上角查看</div>
                                    <el-empty v-else description="暂无明细数据" :image-size="40" />
                                  </div>
                                </template>
                                <el-empty v-else-if="!field.isFetching" description="暂无预处理对比数据" :image-size="80" />
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div class="trace-section" v-else-if="!selectedDetailNode.computeRule">
                        <el-empty description="该节点暂无详细计算规则配置" :image-size="60" />
                      </div>
                    </template>
                  </div>
                </section>
              </div>
              <el-empty v-else description="请点击左侧节点查看详情" :image-size="60" />
            </div>
          </div>

          <div v-else-if="calcRunning || calcLogs.length" class="running-log-area">
            <div class="log-title">计算过程日志</div>
            <div class="exec-log mini">
              <el-timeline>
                <el-timeline-item v-for="(log, idx) in calcLogs" :key="idx" :timestamp="log.time"
                  :type="log.type || 'primary'" placement="top">
                  <div class="log-content">
                    <strong>{{ log.stage }}</strong>
                    <span class="log-text">{{ log.text }}</span>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>
        </div>

        <el-alert v-else type="info" :closable="false" show-icon style="margin-top: 16px;">
          <template #title>独立设计模式：本步仅保存"综合分析计算"的策略配置，不会实际发起计算，也不产生日志。</template>
        </el-alert>

        <!-- 计算过程日志抽屉 -->
        <el-drawer v-model="calcLogDrawerVisible" title="综合分析计算 - 过程日志" size="580px" destroy-on-close append-to-body>
          <div class="drawer-log-content">
            <div class="log-status-header">
              <div class="status-left">
                <el-tag :type="calcTagType" effect="dark">{{ calcStatusLabel }}</el-tag>
                <span class="task-id">任务ID: {{ currentCalcTaskId || '—' }}</span>
              </div>
              <el-button link type="primary" icon="Refresh" @click="refreshCurrentTaskLogs">刷新日志</el-button>
            </div>
            <el-timeline v-if="calcLogs.length">
              <el-timeline-item v-for="(log, idx) in calcLogs" :key="idx" :timestamp="log.time"
                :type="log.type || 'primary'" placement="top" size="normal">
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
            <el-select v-model="stageConfig.reportOutput.config.reportTemplateId" placeholder="请选择报告模板" clearable
              @change="onReportTemplateChange" style="width: 360px;">
              <el-option v-for="t in reportTemplateList" :key="t.id" :label="t.templateName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="报告占位符">
            <el-table :data="stageConfig.reportOutput.config.placeholderMappings" size="small" border
              class="report-mapping-table" style="width: 100%; max-width: 980px;">
              <el-table-column label="占位符" min-width="320">
                <template #default="{ row }">
                  <div class="placeholder-cell">
                    <div class="placeholder-name">{{ row.label || getPlaceholderMeta(row.key).label }}</div>
                    <el-tag size="small" effect="plain">{{ row.key }}</el-tag>
                    <div class="placeholder-desc">{{ row.description || getPlaceholderMeta(row.key).description }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="映射类型" width="160">
                <template #default="{ row }">
                  <el-select v-model="row.mappingType" size="small" style="width: 100%"
                    @change="onReportMappingTypeChange(row)">
                    <el-option label="评估数据" value="AUTO_INDICATOR" />
                    <el-option label="任务属性" value="TASK_PROPERTY" />
                    <el-option label="静态文本" value="STATIC_TEXT" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="取值" min-width="260">
                <template #default="{ row }">
                  <el-select v-if="row.mappingType === 'AUTO_INDICATOR'" v-model="row.mappingValue" size="small"
                    popper-class="report-mapping-popper" style="width: 100%" @change="markReportConfigChanged">
                    <el-option-group v-for="grp in getAutoIndicatorGroups(row)" :key="grp.label" :label="grp.label">
                      <el-option v-for="opt in grp.options" :key="opt.value" :label="opt.label" :value="opt.value">
                        <div class="report-mapping-option">{{ opt.label }}</div>
                      </el-option>
                    </el-option-group>
                  </el-select>

                  <el-select v-else-if="row.mappingType === 'TASK_PROPERTY'" v-model="row.mappingValue" size="small"
                    style="width: 100%" @change="markReportConfigChanged">
                    <el-option v-for="opt in taskPropertyOptions" :key="opt.value" :label="opt.label"
                      :value="opt.value" />
                  </el-select>

                  <template v-else>
                    <div v-if="getPlaceholderUsage(row.key) === 'image'" class="image-upload-wrapper">
                      <ImageUpload v-model="row.mappingValue" action="/zhpg-api/zhpg/report/templates/uploadImage"
                        preview-prefix="/zhpg-api" :limit="1" :is-show-tip="false"
                        @update:modelValue="markReportConfigChanged" />
                    </div>
                    <el-input v-else v-model="row.mappingValue" size="small"
                      :placeholder="getStaticTextPlaceholder(row.key)" @change="markReportConfigChanged" />
                  </template>
                </template>
              </el-table-column>
            </el-table>
          </el-form-item>
        </el-form>

        <!-- 执行操作（仅执行模式） -->
        <div v-if="mode === 'execute'" class="report-exec-panel">
          <el-alert v-if="isStepStale('reportOutput')" type="warning" :closable="false" show-icon
            class="inline-state-alert">
            <template #title>{{ stepStaleReason('reportOutput') || '当前报告已过期，请重新生成。' }}</template>
          </el-alert>
          <el-button type="primary" icon="Document" :disabled="!canGenerateReport || reportGenerating"
            :loading="reportGenerating" @click="generateReport" style="margin-right: 8px;">
            {{ reportGenerated ? '再次生成报告' : '生成报告' }}
          </el-button>
          <el-button plain icon="View" :disabled="!canGenerateReport" :loading="reportHtmlPreviewLoading"
            @click="previewReportHtml">
            预览HTML
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
            <el-table-column prop="renderStatus" label="状态" width="170">
              <template #default="{ row }">
                <template v-if="row.renderStatus === 'PENDING'">
                  <div class="report-progress-bar">
                    <el-progress
                      :percentage="PROGRESS_MAP[pollingProgress[row.id]?.renderProgress]?.pct ?? 0"
                      :status="pollingProgress[row.id]?.renderProgress === 'FAILED' ? 'exception' : undefined"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="report-progress-text">
                      {{ PROGRESS_MAP[pollingProgress[row.id]?.renderProgress]?.label || '等待中...' }}
                    </span>
                  </div>
                </template>
                <template v-else-if="row.renderStatus === 'SUCCESS'">
                  <el-tag type="success" size="small">已生成</el-tag>
                </template>
                <template v-else-if="row.renderStatus === 'FAILED'">
                  <el-tooltip :content="row.errorMessage || '生成失败'" placement="top">
                    <el-tag type="danger" size="small">失败</el-tag>
                  </el-tooltip>
                </template>
                <template v-else>
                  <el-tag type="warning" size="small">{{ row.renderStatus }}</el-tag>
                </template>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="生成时间" width="180" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <div style="display: flex; align-items: center;">
                  <el-button link type="primary" :icon="Document" @click="openGeneratedReport(row)">预览</el-button>
                  <el-button v-if="row.renderStatus === 'FAILED'" link type="danger" icon="Refresh"
                    @click="retryGenerateReport(row)" style="margin-left: 4px;">重试</el-button>
                  <el-dropdown @command="(cmd) => handleReportDownloadCommand(cmd, row)" trigger="click"
                    style="margin-left: 12px">
                    <el-button link type="primary" icon="Download" style="display: flex; align-items: center;">
                      下载<el-icon class="el-icon--right"><arrow-down /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="word" :icon="Document">Word 报告</el-dropdown-item>
                        <el-dropdown-item command="pdf" :icon="DocumentChecked">PDF 报告</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-alert v-else type="info" :closable="false" show-icon style="margin-top: 16px;">
          <template #title>独立设计模式：本步仅保存"报告模板选择 + 占位符映射"，不会实际生成报告。</template>
        </el-alert>

        <el-dialog v-if="mode === 'execute'" v-model="reportPreviewVisible" title="报告预览" width="80%" append-to-body>
          <div class="report-preview-body" v-loading="reportPreviewLoading">
            <iframe v-if="reportPreviewUrl" :src="reportPreviewUrl" class="report-preview-frame" frameborder="0" />
            <el-empty v-else description="暂无可预览地址，可先下载 Word 报告" />
          </div>
        </el-dialog>

        <!-- 方向四: HTML 快速预览弹窗 -->
        <el-dialog v-if="mode === 'execute'" v-model="reportHtmlPreviewVisible" title="HTML 预览" width="80%" append-to-body top="5vh">
          <div class="report-html-preview-body" v-loading="reportHtmlPreviewLoading">
            <div class="report-html-preview-toolbar">
              <el-tag type="warning" size="small">HTML预览为快速渲染结果，最终正式报告以 DOCX/PDF 为准</el-tag>
              <el-button size="small" type="primary" :disabled="reportGenerating" :loading="reportGenerating"
                @click="generateReport" style="margin-left: 12px;">
                确认无误，生成正式报告
              </el-button>
            </div>
            <iframe v-if="reportHtmlPreviewContent" :srcdoc="reportHtmlPreviewContent" class="report-preview-frame" frameborder="0" />
            <el-empty v-else description="暂无预览内容" />
          </div>
        </el-dialog>
      </div>
    </div>

    <el-dialog v-model="preprocessDataDetailVisible" :title="`预处理字段完整数据 - ${preprocessDataDetailField?.label || ''}`"
      fullscreen append-to-body destroy-on-close class="preprocess-data-dialog">
      <div v-if="preprocessDataDetailField" class="preprocess-data-detail">
        <div class="detail-toolbar">
          <div class="detail-meta">
            <span>{{ preprocessDataDetailField.sourceName }}</span>
            <el-tag size="small" effect="plain">{{ preprocessDataDetailField.code }}</el-tag>
            <el-tag size="small" :type="preprocessDataDetailField.isNumeric ? 'success' : 'warning'" effect="plain">
              {{ preprocessDataDetailField.isNumeric ? '数值型' : '非数值型' }}
            </el-tag>
          </div>
          <el-input v-model="preprocessDataDetailKeyword" clearable placeholder="搜索序号、值或时间" class="detail-search" />
        </div>

        <div class="detail-table-grid">
          <section class="detail-table-panel">
            <div class="detail-table-title">
              <span>预处理前</span>
              <strong>{{ preprocessDetailBeforeRows.length }}</strong>
            </div>
            <el-table :data="preprocessDetailBeforePageRows" border height="calc(100vh - 260px)" size="small">
              <el-table-column prop="no" label="序号" width="80" fixed />
              <el-table-column prop="value" label="原始值" min-width="220" show-overflow-tooltip />
              <el-table-column prop="timestamp" label="时间/标记" min-width="180" show-overflow-tooltip />
            </el-table>
            <el-pagination v-model:current-page="preprocessDataDetailPage.before"
              v-model:page-size="preprocessDataDetailPage.pageSize" :page-sizes="preprocessDataDetailPageSizes"
              :total="preprocessDetailBeforeRows.length" layout="total, sizes, prev, pager, next" background small />
          </section>

          <section class="detail-table-panel">
            <div class="detail-table-title">
              <span>预处理后</span>
              <strong>{{ preprocessDetailAfterRows.length }}</strong>
            </div>
            <el-table :data="preprocessDetailAfterPageRows" border height="calc(100vh - 260px)" size="small">
              <el-table-column prop="no" label="序号" width="80" fixed />
              <el-table-column prop="value" label="处理值" min-width="220" show-overflow-tooltip />
              <el-table-column prop="timestamp" label="时间/标记" min-width="180" show-overflow-tooltip />
            </el-table>
            <el-pagination v-model:current-page="preprocessDataDetailPage.after"
              v-model:page-size="preprocessDataDetailPage.pageSize" :page-sizes="preprocessDataDetailPageSizes"
              :total="preprocessDetailAfterRows.length" layout="total, sizes, prev, pager, next" background small />
          </section>
        </div>
      </div>
    </el-dialog>

    <!-- 源码预览对话框 -->
    <el-dialog v-model="algoPreviewVisible" :title="`算法源码预览 - ${currentPreviewAlgoName}`" width="70%" append-to-body
      destroy-on-close class="algo-preview-dialog">
      <div v-loading="algoPreviewLoading" class="algo-preview-container">
        <pre v-if="algoPreviewContent" class="algo-preview-pre"><code class="algo-preview-code"
        v-html="algoPreviewHighlightedHtml"></code></pre>
        <el-empty v-else description="暂无源码内容" />
      </div>
      <template #footer>
        <el-button @click="algoPreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, ArrowDown, VideoPlay, Download, TrendCharts, Collection, DocumentChecked, DataAnalysis, Document, Medal, Setting, DataLine, InfoFilled, WarningFilled, List, Refresh, StarFilled, Clock } from '@element-plus/icons-vue'
import axios from 'axios'
import { getCalcFlow } from '@/api/zhpg/calcFlow'
import { getCalcTask, runCalcTask } from '@/api/zhpg/calcTask'
import { getCalcFlowExecution, initCalcFlowExecution, runCalcFlowExecution, saveCalcFlowExecutionConfig } from '@/api/zhpg/calcFlowExecution'
import { getIndicatorSystem, updateIndicatorSystem, selectIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import { getEvalResult, getEvalResultByTask, listEvalResult } from '@/api/zhpg/evalResult'
import { generateEvalReport, getEvalReportLinks, getEvalReportProgress, previewEvalReportHtml, listEvalReportsByResult } from '@/api/zhpg/evalReport'
import { listTemplates, getTemplate } from '@/api/zhpg/report'
import { listAllAlgorithm, previewAlgorithmCode } from '@/api/zhpg/algorithm'
import { listPreprocessBatch } from '@/api/zhpg/externalData'
import { highlightSourceCode } from '@/utils/zhpg/pythonSyntaxHighlight'
import {
  buildViewportYTicks,
  buildViewportXTicks,
  clampChartTranslateX,
  clampChartTranslateY,
  getChartDataTransform,
  indexToChartX as viewportIndexToChartX
} from '@/utils/zhpg/preprocessChartViewport'
import { getZhpgEquipmentTypeLabel } from '@/constants/zhpgIndicatorSystem'
import {
  TASK_PROPERTY_OPTIONS,
  createReportMapping,
  extractPlaceholdersFromHtml,
  getAutoIndicatorOptionGroups,
  getPlaceholderMeta,
  getPlaceholderUsage,
  getStaticTextPlaceholder,
  normalizeReportMappings,
} from '@/utils/zhpg/reportPlaceholders'
import ImageUpload from '@/components/ImageUpload'
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
import { parseIndicatorTreeToForest, serializeForestToIndicatorTree } from '@/utils/zhpg/zhpgIndicatorTreeJson'
import IndicatorTreeWorkbench from '@/views/zhpg/components/IndicatorTreeWorkbench.vue'
import ZhpgWeightWorkbenchDialog from '@/views/zhpg/components/ZhpgWeightWorkbenchDialog.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const props = defineProps({
  /** 可由父级直接传入，代替路由参数 */
  templateId: { type: [Number, String], default: null },
  indicatorSystemId: { type: [Number, String], default: null },
  requirementIdProp: { type: [Number, String], default: null },
  taskNameProp: { type: String, default: '' },
  batchIdProp: { type: [Number, String], default: null }
})

const emit = defineEmits(['exit', 'finished'])

// ==================== 入参 ====================
const templateId = computed(() => Number(props.templateId || route.query.templateId) || null)
const indicatorSystemId = computed(() => Number(props.indicatorSystemId || route.query.indicatorSystemId) || null)
const requirementId = computed(() => Number(props.requirementIdProp || route.query.requirementId) || null)
const initialTaskId = computed(() => Number(route.query.taskId) || null)
const initialEvalResultId = computed(() => Number(route.query.evalResultId) || null)
const initialExecutionId = computed(() => Number(route.query.executionId) || null)
const urlBatchId = computed(() => {
  const b = route.query.batchId || props.batchIdProp
  return b !== undefined && b !== null && b !== '' ? Number(b) : null
})
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

// ==================== 算法缓存 ====================
const algoMap = ref({})
async function fetchAlgoMap() {
  try {
    const res = await listAllAlgorithm()
    const list = Array.isArray(res) ? res : (res.data || res.rows || [])
    const map = {}
    list.forEach(a => {
      // 字段归一化，兼容后端可能返回的 snake_case 或 camelCase
      const normalized = {
        ...a,
        algorithmName: a.algorithmName || a.algorithm_name || a.name || `算法#${a.id}`,
        algorithmCode: a.algorithmCode || a.algorithm_code || a.code || '',
        algorithmDescription: a.algorithmDescription || a.algorithm_description || a.algorithmDesc || a.algorithm_desc || a.desc || '',
        author: a.author || a.create_by || a.createBy || '',
        algorithmCodeUrl: a.algorithmCodeUrl || a.algorithm_code_url || ''
      }
      map[String(a.id)] = normalized
    })
    algoMap.value = map
  } catch (e) {
    console.error('[FlowRunner] Fetch algo map failed:', e)
  }
}

const algoPreviewVisible = ref(false)
const algoPreviewLoading = ref(false)
const algoPreviewContent = ref('')
const currentPreviewAlgoName = ref('')
const algoPreviewHighlightedHtml = computed(() => highlightSourceCode(algoPreviewContent.value, currentPreviewAlgoName.value))

async function handlePreviewAlgo(algoOrId) {
  let id = ''
  let name = ''
  if (typeof algoOrId === 'object' && algoOrId !== null) {
    id = algoOrId.id
    name = algoOrId.algorithmName || algoOrId.algorithm_name
  } else {
    id = algoOrId
    const info = getAlgoInfo(id)
    name = info.algorithmName
  }

  if (!id) return

  currentPreviewAlgoName.value = name || `算法#${id}`
  algoPreviewContent.value = ''
  algoPreviewVisible.value = true
  algoPreviewLoading.value = true

  try {
    const res = await previewAlgorithmCode(id)
    // 假设返回的是字符串，或者是包含 data 字段的对象
    algoPreviewContent.value = typeof res === 'string' ? res : (res.data || res.msg || '无法解析源码内容')
  } catch (e) {
    ElMessage.error('获取源码失败: ' + (e.message || '网络错误'))
  } finally {
    algoPreviewLoading.value = false
  }
}

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

function getStepStatusText(key) {
  const state = getStepState(key)
  const status = state.status

  if (status === FLOW_STEP_STATUS.COMPLETED) return '已完成'
  if (status === FLOW_STEP_STATUS.RUNNING) return '正在执行'
  if (status === FLOW_STEP_STATUS.FAILED) return '执行失败'
  if (status === FLOW_STEP_STATUS.STALE) return '需更新'

  // 待处理状态根据步骤不同显示不同文本
  if (key === 'scheduleConfig') return '待配置'
  if (key === 'weightCalc') return '待加载'
  if (key === 'comprehensiveCalc') return '待计算'
  if (key === 'reportOutput') return '待生成'

  return '未开始'
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
  weightCalc: {
    config: {}
  },
  comprehensiveCalc: {
    config: {
      algorithmChainMode: 'SERIAL',
      nullDataPolicy: 'ZERO_FILL',
      intermediateResultOutput: false,
      batchId: null,
      aggregationSource: 'INDICATOR_SYSTEM',
      overrideAggregationAlg: null,
      scoreLevels: [
        { name: '优秀', threshold: 90 },
        { name: '良好', threshold: 75 },
        { name: '及格', threshold: 60 }
      ]
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

function normalizeReportOutputConfig() {
  const current = stageConfig.reportOutput.config || {}
  const nextTemplateId = current.reportTemplateId || null
  const nextMappings = normalizeReportMappings(current.placeholderMappings || [])
  const hasTemplateChanged = current.reportTemplateId !== nextTemplateId
  const hasMappingsChanged = JSON.stringify(current.placeholderMappings || []) !== JSON.stringify(nextMappings)
  if (hasTemplateChanged || hasMappingsChanged) {
    stageConfig.reportOutput.config = {
      reportTemplateId: nextTemplateId,
      placeholderMappings: nextMappings
    }
  }
}

function buildRuntimeConfigJson() {
  normalizeReportOutputConfig()
  if (stageConfig.scheduleConfig?.config) {
    stageConfig.scheduleConfig.config.misfireStrategy = 'DO_NOTHING'
  }
  return JSON.stringify({
    stages: {
      scheduleConfig: {
        stageCode: 'SCHEDULE_CONFIG',
        stageName: '调度策略选配',
        stageOrder: 1,
        enabled: true,
        config: JSON.parse(JSON.stringify(stageConfig.scheduleConfig.config || {}))
      },
      weightCalc: {
        stageCode: 'WEIGHT_CALC',
        stageName: '权重计算',
        stageOrder: 2,
        enabled: true,
        config: JSON.parse(JSON.stringify(stageConfig.weightCalc.config || {}))
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
    runtimePolicy: {
      blockStrategy: stageConfig.scheduleConfig.config.blockStrategy,
      misfireStrategy: 'DO_NOTHING'
    }
  })
}

function serializeStepState() {
  return JSON.stringify(JSON.parse(JSON.stringify(flowStepState)))
}

function applyRuntimeConfigJson(configJson) {
  if (!configJson) return
  try {
    const cfg = typeof configJson === 'string' ? JSON.parse(configJson) : configJson
    if (cfg.runtimePolicy) {
      runtimePolicy.value = cfg.runtimePolicy
    } else {
      runtimePolicy.value = {}
    }
    if (cfg.stages) {
      Object.keys(cfg.stages).forEach(key => {
        if (stageConfig[key] && cfg.stages[key]?.config) {
          Object.assign(stageConfig[key].config, cfg.stages[key].config)
        }
      })
    }

    // 兼容并归一化运行时调度策略
    const blockStrategy = cfg.stages?.scheduleConfig?.config?.blockStrategy || cfg.runtimePolicy?.blockStrategy || 'SERIAL_EXECUTION'
    const misfireStrategy = cfg.stages?.scheduleConfig?.config?.misfireStrategy || cfg.runtimePolicy?.misfireStrategy || 'DO_NOTHING'

    stageConfig.scheduleConfig.config.blockStrategy = blockStrategy
    stageConfig.scheduleConfig.config.misfireStrategy = misfireStrategy
    runtimePolicy.value.blockStrategy = blockStrategy
    runtimePolicy.value.misfireStrategy = misfireStrategy

    if (urlBatchId.value !== null) {
      stageConfig.comprehensiveCalc.config.batchId = urlBatchId.value
    } else if (mode.value === 'execute' && !initialExecutionId.value) {
      stageConfig.comprehensiveCalc.config.batchId = null
    }
    normalizeReportOutputConfig()
    const reportCfg = stageConfig.reportOutput.config
    if (reportCfg.reportTemplateId) {
      onReportTemplateChange(reportCfg.reportTemplateId, { silent: true })
    }
  } catch (e) {
    console.warn('解析 configJson 失败', e)
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

const batchOptions = ref([])
const loadingBatches = ref(false)

const batchDisplayName = computed(() => {
  if (urlBatchId.value !== null) {
    const found = batchOptions.value.find(item => item.id === urlBatchId.value)
    if (found) {
      return `${found.batchName} (ID: ${found.id})`
    }
    return `${urlBatchId.value} (URL已指定，不可编辑)`
  }
  return ''
})

async function fetchBatchOptions() {
  if (!requirementId.value) {
    batchOptions.value = []
    return
  }
  loadingBatches.value = true
  try {
    const res = await listPreprocessBatch({
      requirementCode: String(requirementId.value),
      pageNum: 1,
      pageSize: 100
    })
    const dataObj = res?.data || res
    batchOptions.value = dataObj?.records || dataObj?.rows || (Array.isArray(dataObj) ? dataObj : [])
  } catch (e) {
    console.error('获取预处理批次列表失败:', e)
  } finally {
    loadingBatches.value = false
  }
}

function handleBatchSelectVisibleChange(visible) {
  if (visible) {
    fetchBatchOptions()
  }
}

watch(requirementId, () => {
  fetchBatchOptions()
}, { immediate: true })

watch(urlBatchId, (newVal) => {
  if (newVal !== null) {
    stageConfig.comprehensiveCalc.config.batchId = newVal
  }
}, { immediate: true })

const routeStrategyOptions = [
  { label: '第一个', value: 'FIRST' },
  { label: '轮询', value: 'ROUND' },
  { label: '随机', value: 'RANDOM' },
  { label: '一致性哈希', value: 'CONSISTENT_HASH' },
  { label: '故障转移', value: 'FAILOVER' }
]


const aggregationAlgorithmList = computed(() => {
  return Object.values(algoMap.value).filter(alg => alg.algorithmType === '聚合传导')
})

function addScoreLevel() {
  if (!stageConfig.comprehensiveCalc.config.scoreLevels) {
    stageConfig.comprehensiveCalc.config.scoreLevels = []
  }
  stageConfig.comprehensiveCalc.config.scoreLevels.push({
    name: '',
    threshold: 0
  })
}

function removeScoreLevel(index) {
  if (stageConfig.comprehensiveCalc.config.scoreLevels) {
    stageConfig.comprehensiveCalc.config.scoreLevels.splice(index, 1)
  }
}

const sortedScoreLevels = computed(() => {
  const levels = stageConfig.comprehensiveCalc?.config?.scoreLevels || []
  return [...levels].sort((a, b) => {
    const tA = Number(a.threshold) || 0
    const tB = Number(b.threshold) || 0
    return tB - tA
  })
})

function getLevelStyle(name) {
  if (!name) return { bg: 'rgba(148, 163, 184, 0.08)', color: '#64748b', border: 'rgba(148, 163, 184, 0.2)' }
  const n = name.trim()
  if (n.includes('优') || n.toLowerCase().includes('excellent') || n.toLowerCase().includes('a')) {
    return {
      bg: 'rgba(16, 185, 129, 0.08)',
      color: '#10b981',
      border: 'rgba(16, 185, 129, 0.24)'
    }
  }
  if (n.includes('良') || n.toLowerCase().includes('good') || n.toLowerCase().includes('b')) {
    return {
      bg: 'rgba(59, 130, 246, 0.08)',
      color: '#3b82f6',
      border: 'rgba(59, 130, 246, 0.24)'
    }
  }
  if (n.includes('及格') || n.includes('中') || n.includes('合格') || n.toLowerCase().includes('pass') || n.toLowerCase().includes('c')) {
    return {
      bg: 'rgba(245, 158, 11, 0.08)',
      color: '#f59e0b',
      border: 'rgba(245, 158, 11, 0.24)'
    }
  }
  return {
    bg: 'rgba(239, 68, 68, 0.08)',
    color: '#ef4444',
    border: 'rgba(239, 68, 68, 0.24)'
  }
}


// ==================== Step 2 树数据 ====================
const weightTreeData = ref([])
const selectedTreeNode = ref(null)
const treeEditorVisible = ref(false)
const weightTuningVisible = ref(false)
const workbenchRef = ref(null)
const originalTreeSnapshot = ref(null)
const treeSaving = ref(false)

const hasUnsavedTreeChanges = computed(() => {
  if (!originalTreeSnapshot.value) return false
  return JSON.stringify(weightTreeData.value || []) !== JSON.stringify(originalTreeSnapshot.value || [])
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
// 全屏弹窗打开瞬间 G6 容器尺寸为 0，需在动画结束（@opened）后重绘并 fit，否则图形区为空
function onTreeEditorOpened() {
  nextTick(() => workbenchRef.value?.renderGraph?.({ fitView: true }))
}
async function openWeightTuning() {
  if (!effectiveIndicatorSystemId.value) {
    ElMessage.warning('缺少指标体系 ID，无法进行权重调优')
    return
  }
  // 工作台按已保存的指标树加载/计算；若有未保存的树编辑，先落库再打开，避免覆盖丢失
  if (hasUnsavedTreeChanges.value) {
    try {
      await ElMessageBox.confirm(
        '当前指标树存在未保存修改。权重调优会基于已保存的指标树进行，是否先保存当前修改？',
        '保存当前修改',
        {
          confirmButtonText: '保存并继续',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
    const saved = await confirmTreeEdits({ keepDialogOpen: true, silent: true })
    if (!saved) return
  }
  weightTuningVisible.value = true
}
// 工作台完成「一键计算权重」或「保存手动微调」后回写带权重树：刷新预览 + 标记步骤 + 存草稿
async function onWeightWorkbenchUpdated(treeWeightJson) {
  if (treeWeightJson) {
    const nextTree = parseIndicatorTreeToForest(treeWeightJson)
    if (nextTree.length) {
      weightTreeData.value = nextTree
      originalTreeSnapshot.value = JSON.parse(JSON.stringify(nextTree))
    }
    indicatorSystemDetail.value = {
      ...(indicatorSystemDetail.value || {}),
      indicatorTreeWeight: treeWeightJson
    }
  } else {
    await loadIndicatorSystem()
  }
  markStepCompleted(flowStepState, 'weightCalc')
  markCalculationStale()
  await saveExecutionDraft({ silent: true })
  await nextTick()
  workbenchRef.value?.renderGraph?.()
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

// 客观/主观/手动赋权已统一到「权重分配调优」工作台（ZhpgWeightWorkbenchDialog · computeWeightsSmart）；
// 指标树编辑器仅负责结构编辑，不再内嵌独立的客观赋权入口。

// ==================== Step 3: 计算执行 ====================
const calcStatus = ref('')
const calcRunning = computed(() => ['PENDING', 'DISPATCHED', 'RUNNING'].includes(calcStatus.value))
const calcFinished = computed(() => calcStatus.value === 'SUCCESS')
const apiLoading = ref(false)
const isStartBtnDisabled = computed(() => {
  return apiLoading.value
})
const startBtnLabel = computed(() => {
  if (calcStatus.value) return '重新计算'
  return '发起计算'
})
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
  if (urlBatchId.value === null && !stageConfig.comprehensiveCalc.config.batchId) {
    if (batchOptions.value && batchOptions.value.length > 0) {
      ElMessage.warning('请先选择预处理批次后再进行计算！')
      return
    }
  }

  if (calcRunning.value && currentCalcTaskId.value) {
    try {
      await ElMessageBox.confirm(`当前计算状态为「${calcStatusLabel.value || '进行中'}」，重新计算将发起新的计算任务，是否确认？`, '确认重新计算', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }

  apiLoading.value = true
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
        batchId: stageConfig.comprehensiveCalc.config.batchId || undefined,
        runtimeConfigJson: buildRuntimeConfigJson(),
        skipWeightLog: true
      })
    }
    const task = res?.data || res
    currentCalcTaskId.value = task?.id || null
    applyTaskDetail(task)
    if (currentCalcTaskId.value && (calcStatus.value === 'RUNNING' || calcStatus.value === 'PENDING' || calcStatus.value === 'DISPATCHED')) {
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
  } finally {
    apiLoading.value = false
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
  const oldStatus = calcStatus.value
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
  if (calcStatus.value !== oldStatus || calcFinished.value || calcStatus.value === 'FAILED') {
    scheduleSaveExecutionDraft()
  }
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
    time: formatTime(log.finishTime || log.endTime || log.beginTime || log.startTime || log.createTime)
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
const taskPropertyOptions = TASK_PROPERTY_OPTIONS
const reportGenerating = ref(false)
const reportInstances = ref([])
const reportGenerated = computed(() => reportInstances.value.length > 0)
const canGenerateReport = computed(() => calcFinished.value && !calcStale.value && !!currentEvalResult.value?.id && !!stageConfig.reportOutput.config.reportTemplateId)

// 方向四: 异步生成进度轮询
const reportPollTimers = ref({})
const pollingProgress = ref({})

const reportPreviewVisible = ref(false)
const reportPreviewLoading = ref(false)
const reportPreviewUrl = ref('')
const currentReportLinks = ref(null)

// 方向四: HTML 预览
const reportHtmlPreviewVisible = ref(false)
const reportHtmlPreviewContent = ref('')
const reportHtmlPreviewLoading = ref(false)

const PROGRESS_MAP = {
  PENDING:             { label: '等待生成',     pct: 0 },
  HTML_RENDERING:      { label: '生成图表',     pct: 20 },
  CHART_GENERATING:    { label: '生成图表',     pct: 35 },
  DOCX_CONVERTING:     { label: '生成DOCX',     pct: 55 },
  PDF_CONVERTING:      { label: '转换PDF',      pct: 75 },
  UPLOADING:           { label: '上传文件',     pct: 90 },
  DONE:                { label: '完成',          pct: 100 },
  FAILED:              { label: '生成失败',     pct: -1 }
}
const STANDARD_REPORT_TEMPLATE_CODE = 'COMM_COUNTERMEASURE_STANDARD_REPORT'

function getAutoIndicatorGroups(row) {
  return getAutoIndicatorOptionGroups(row?.key)
}

function getDefaultAutoIndicatorValue(row, inferredValue = '') {
  const options = getAutoIndicatorGroups(row).flatMap(group => group.options)
  return options.some(option => option.value === inferredValue) ? inferredValue : options[0]?.value || ''
}
const STANDARD_REPORT_TEMPLATE_NAME = '通信对抗试验评估报告（标准版）'

async function generateReport() {
  if (!canGenerateReport.value) return
  normalizeReportOutputConfig()
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
      startPollingReport(report.id)
    }
    markStepCompleted(flowStepState, 'reportOutput', { reportId: report?.id })
    await saveExecutionDraft({ silent: true, status: 'REPORT_READY', currentStep: 'reportOutput' })
    ElMessage.success(`后台生成已启动${report?.generationNo ? `（第 ${report.generationNo} 版）` : ''}`)
  } catch (e) {
    ElMessage.error(e?.message || '报告生成启动失败')
  } finally {
    reportGenerating.value = false
  }
}

async function retryGenerateReport(row) {
  if (!currentEvalResult.value?.id) return
  reportGenerating.value = true
  try {
    let mappings, fields
    try {
      const parsed = JSON.parse(row.mappingJson || '{}')
      mappings = parsed.mappings || stageConfig.reportOutput.config.placeholderMappings
      fields = parsed.fields || buildReportFields()
    } catch {
      mappings = stageConfig.reportOutput.config.placeholderMappings
      fields = buildReportFields()
    }
    const res = await generateEvalReport(currentEvalResult.value.id, {
      reportTemplateId: row.reportTemplateId || stageConfig.reportOutput.config.reportTemplateId,
      mappings,
      fields
    })
    const report = res?.data || res
    if (report?.id) {
      await loadReportInstances()
      startPollingReport(report.id)
    }
    ElMessage.success('已重新发起报告生成')
  } catch (e) {
    ElMessage.error(e?.message || '重试失败')
  } finally {
    reportGenerating.value = false
  }
}

function startPollingReport(reportId) {
  stopPollingReport(reportId)
  const poll = async () => {
    try {
      const res = await getEvalReportProgress(reportId)
      const data = res?.data || {}
      pollingProgress.value[reportId] = {
        renderProgress: data.renderProgress,
        renderProgressDetail: data.renderProgressDetail,
        renderStatus: data.renderStatus,
        errorMessage: data.errorMessage
      }
      pollingProgress.value = { ...pollingProgress.value }
      if (data.renderStatus === 'SUCCESS' || data.renderProgress === 'DONE') {
        stopPollingReport(reportId)
        await loadReportInstances()
        ElMessage.success('报告已生成完成')
      } else if (data.renderStatus === 'FAILED' || data.renderProgress === 'FAILED') {
        stopPollingReport(reportId)
        await loadReportInstances()
        ElMessage.error(data.errorMessage || '报告生成失败')
      }
    } catch { /* 轮询失败继续重试 */ }
  }
  poll()
  reportPollTimers.value[reportId] = setInterval(poll, 2000)
}

function stopPollingReport(reportId) {
  if (reportPollTimers.value[reportId]) {
    clearInterval(reportPollTimers.value[reportId])
    delete reportPollTimers.value[reportId]
  }
}

async function previewReportHtml() {
  if (!currentEvalResult.value?.id || !stageConfig.reportOutput.config.reportTemplateId) {
    ElMessage.warning('请先选择报告模板')
    return
  }
  reportHtmlPreviewLoading.value = true
  try {
    const res = await previewEvalReportHtml(currentEvalResult.value.id, {
      reportTemplateId: stageConfig.reportOutput.config.reportTemplateId,
      mappings: stageConfig.reportOutput.config.placeholderMappings,
      fields: buildReportFields()
    })
    const data = res?.data || {}
    if (data.reportUrl) {
      reportHtmlPreviewContent.value = data.reportUrl
      reportHtmlPreviewVisible.value = true
      ElMessage.success('预览渲染完成')
    } else {
      ElMessage.warning('预览渲染为空')
    }
  } catch (e) {
    ElMessage.error(e?.message || '预览渲染失败')
  } finally {
    reportHtmlPreviewLoading.value = false
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

    const existingMappings = stageConfig.reportOutput.config.placeholderMappings || []
    stageConfig.reportOutput.config.placeholderMappings = placeholders.map(key => createReportMapping(key, existingMappings))
  } catch (e) {
    if (!options.silent) ElMessage.error('获取模板占位符失败')
  }
}

function onReportMappingTypeChange(row) {
  if (row.mappingType === 'AUTO_INDICATOR') {
    row.mappingValue = getDefaultAutoIndicatorValue(row, createReportMapping(row.key).mappingValue)
  } else if (row.mappingType === 'TASK_PROPERTY') {
    const inferred = createReportMapping(row.key)
    row.mappingValue = inferred.mappingType === 'TASK_PROPERTY' ? inferred.mappingValue : 'taskName'
  } else {
    row.mappingValue = ''
  }
  markReportConfigChanged()
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
    const resolved = resolveReportField(row, base)
    if (resolved !== undefined) {
      base[row.key] = resolved
    }
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
    if (row.mappingValue === 'overall_conclusion') return sanitizeNarrative(base.conclusion) || undefined
    if (row.mappingValue === 'suggestion') return sanitizeNarrative(base.suggestion) || undefined
    if (row.mappingValue === 'indicator_summary_table') return buildIndicatorSummaryTableHtml()
    if (row.mappingValue === 'indicator_tree') return buildIndicatorTreeHtml()
    if (row.mappingValue === 'eval_result_snapshot') return ''
    if (row.mappingValue === 'experiment_overview') return buildExperimentOverviewText(base)
    if (row.mappingValue === 'capability_radar_chart') return undefined
    if (row.mappingValue === 'capability_score_table') return buildIndicatorSummaryTableHtml()
    if (row.mappingValue === 'indicator_sections') return undefined
    if (row.mappingValue === 'overall_conclusion_paragraph') return sanitizeNarrative(base.conclusion) || `本次评估综合得分为 ${base.score ?? '未计算'}，评估等级为 ${base.grade || '未评定'}。`
    if (row.mappingValue === 'key_findings') return buildKeyFindingsHtml(base)
    if (row.mappingValue === 'improvement_suggestions') return buildImprovementSuggestionsHtml(base)
    if (row.mappingValue === 'final_conclusion') return sanitizeNarrative(base.conclusion) || `综上，本次评估结果为${base.grade || '未评定'}，综合得分为 ${base.score ?? '未计算'}。`
  }

  return base[row.key] ?? row.mappingValue ?? ''
}

function buildExperimentOverviewText(base) {
  const name = cleanTaskName(base.taskName) || '当前试验任务'
  const target = currentTaskDetail.value?.evaluateTarget || currentEvalResult.value?.evaluateTarget || '当前评估对象'
  const systemName = cleanIndicatorSystemName(base.indicatorSystemName) || '当前指标体系'
  const startTime = currentTaskDetail.value?.startTime ? formatTime(currentTaskDetail.value.startTime) : currentEvalResult.value?.startTime ? formatTime(currentEvalResult.value.startTime) : '未记录'
  const rows = [
    ['试验名称', name],
    ['试验时间', startTime],
    ['评估对象', target],
    ['指标体系', systemName],
    ['综合得分', base.score ?? '未计算'],
    ['评定等级', base.grade || '未评定']
  ]
  return `<table border="1" cellspacing="0" cellpadding="6" style="width:100%;border-collapse:collapse;border:1px solid #dcdfe6;font-family:Arial,'Microsoft YaHei',sans-serif;font-size:13px;">${rows.map(([label, value]) => `<tr><th style="width:24%;border:1px solid #dcdfe6;text-align:left;background:#f5f7fa;padding:8px;">${escapeHtml(label)}</th><td style="border:1px solid #dcdfe6;padding:8px;">${escapeHtml(String(value ?? '-'))}</td></tr>`).join('')}</table>`
}

function buildKeyFindingsHtml(base) {
  const scoreText = base.score ?? '未计算'
  const gradeText = base.grade || '未评定'
  const dimensions = selectPreviewRadarDimensions()
  const dimensionItems = dimensions.slice(0, 5).map(item => `<li>${escapeHtml(item.label || item.name || '未命名能力域')}得分 ${escapeHtml(String(item.score ?? item.value ?? '-'))}，评定为 ${escapeHtml(formatTone(item.tone))}。</li>`).join('')
  return `<ul><li>综合得分：${escapeHtml(String(scoreText))}</li><li>评定结果：${escapeHtml(String(gradeText))}</li>${dimensionItems || '<li>指标明细见后续章节。</li>'}</ul>`
}

function buildImprovementSuggestionsHtml(base) {
  const suggestion = sanitizeNarrative(base.suggestion)
  if (suggestion) return `<p>${escapeHtml(suggestion)}</p>`
  const lowItems = selectPreviewRadarDimensions()
    .filter(item => Number(item.score ?? item.value) < 75)
    .slice(0, 3)
    .map(item => `<li>针对${escapeHtml(item.label || item.name || '低分能力域')}开展专项改进，补充试验样本并跟踪复测结果。</li>`)
    .join('')
  return `<ol><li>优先复核低分能力域的数据来源、计算规则和权重配置，确认评估输入与实际试验记录一致。</li>${lowItems}<li>形成整改闭环后重新生成评估报告，保留前后版本对比，支撑后续决策复盘。</li></ol>`
}

function buildIndicatorSummaryTableHtml() {
  if (!detailTreeData.value || !detailTreeData.value.length) return '<p style="color: #909399; font-style: italic;">暂无评估指标数据</p>'

  const paths = []
  const collect = (node, path = []) => {
    const next = [...path, node]
    if (node.children && node.children.length) node.children.forEach(child => collect(child, next))
    else paths.push(next)
  }
  detailTreeData.value.forEach(node => collect(node))
  if (!paths.length) return '<p style="color: #909399; font-style: italic;">暂无评估指标数据</p>'
  const maxDepth = Math.min(5, Math.max(...paths.map(path => path.length)))
  const levelNames = ['一级指标', '二级指标', '三级指标', '四级指标', '五级指标']

  let html = '<table border="1" cellpadding="6" cellspacing="0" style="border-collapse:collapse;width:100%;max-width:100%;border:1px solid #dcdfe6;font-family:Arial,Microsoft YaHei,sans-serif;font-size:13px;text-align:center;margin-bottom:20px;">'
  html += '<thead><tr style="background-color:#f5f7fa;font-weight:bold;color:#303133;">'
  for (let i = 0; i < maxDepth; i++) html += `<th style="border:1px solid #dcdfe6;padding:8px;">${levelNames[i]}</th>`
  html += '<th style="border:1px solid #dcdfe6;padding:8px;">指标得分</th><th style="border:1px solid #dcdfe6;padding:8px;">评定</th><th style="border:1px solid #dcdfe6;padding:8px;">实测值</th><th style="border:1px solid #dcdfe6;padding:8px;">参考阈值</th></tr></thead><tbody>'

  paths.forEach((path, rowIndex) => {
    const leaf = path[path.length - 1]
    html += '<tr>'
    for (let depth = 0; depth < maxDepth; depth++) {
      const node = path[depth]
      if (!node) {
        html += '<td style="border:1px solid #dcdfe6;padding:8px;">-</td>'
        continue
      }
      if (rowIndex > 0 && paths[rowIndex - 1][depth] === node) continue
      let rowspan = 1
      for (let next = rowIndex + 1; next < paths.length; next++) {
        if (paths[next][depth] === node) rowspan++
        else break
      }
      html += `<td${rowspan > 1 ? ` rowspan="${rowspan}"` : ''} style="border:1px solid #dcdfe6;padding:8px;text-align:left;vertical-align:middle;">${escapeHtml(node.label || node.name || '未命名指标')}</td>`
    }
    const score = leaf.score ?? leaf.calculatedScore ?? leaf.calculatedValue ?? '-'
    const evalValue = leaf.evalValue ?? leaf.calculatedValue ?? leaf.metricValue ?? leaf.value ?? '-'
    const referenceValue = leaf.referenceValue ?? leaf.threshold ?? leaf.valueMax ?? leaf.valueMin ?? '-'
    html += `<td style="border:1px solid #dcdfe6;padding:8px;">${escapeHtml(String(score))}</td>`
    html += `<td style="border:1px solid #dcdfe6;padding:8px;">${escapeHtml(formatTone(leaf.tone || leaf.grade || leaf.status))}</td>`
    html += `<td style="border:1px solid #dcdfe6;padding:8px;">${escapeHtml(formatValueWithUnit(evalValue, leaf.unit))}</td>`
    html += `<td style="border:1px solid #dcdfe6;padding:8px;">${escapeHtml(formatValueWithUnit(referenceValue, leaf.unit))}</td>`
    html += '</tr>'
  })
  html += '</tbody></table>'
  return html
}

function cleanTaskName(raw) {
  return String(raw || '').trim().replace(/-\d{10,}$/, '').trim()
}

function cleanIndicatorSystemName(raw) {
  return String(raw || '').trim().replace(/[（(][^）)]*[）)]/g, '').replace(/\s+/g, ' ').trim()
}

function sanitizeNarrative(raw) {
  const cleaned = String(raw || '')
    .trim()
    .replace(/\broute\s*=\s*[^,，。;；]+[,，。;；]?\s*/gi, '')
    .replace(/\bblock\s*=\s*[^,，。;；]+[,，。;；]?\s*/gi, '')
    .replace(/\bmisfire\s*=\s*[^,，。;；]+[,，。;；]?\s*/gi, '')
    .replace(/^.*\s+completed on\s+.*score\s*=\s*[^,，。;；]+[,，。;；]?\s*grade\s*=\s*[^,，。;；]+\.?$/i, '')
    .replace(/^Result is stable\.?$/i, '')
    .replace(/^Review low-score indicators and mapping\.?$/i, '')
    .replace(/[,，;；]\s*$/, '')
    .trim()
  return cleaned || ''
}

function escapeHtml(value) {
  return String(value ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;')
}

function formatTone(tone) {
  const text = String(tone || '').trim().toLowerCase()
  if (!text) return '-'
  if (text === 'excellent') return '优秀'
  if (text === 'good') return '良好'
  if (text === 'pass' || text === 'qualified') return '合格'
  if (text === 'risk') return '风险'
  return tone
}

function formatValueWithUnit(value, unit) {
  if (value === undefined || value === null || value === '') return '-'
  return `${value}${unit || ''}`
}

function selectPreviewRadarDimensions() {
  return (detailTreeData.value || [])
    .map(node => ({
      label: node.label || node.name,
      score: node.score ?? node.calculatedScore ?? node.calculatedValue,
      value: node.value,
      tone: node.tone || node.grade || node.status
    }))
    .filter(item => item.label && (item.score !== undefined || item.value !== undefined))
    .sort((a, b) => Number(b.score ?? b.value ?? 0) - Number(a.score ?? a.value ?? 0))
    .slice(0, 8)
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
    proxy.download('downloadReport', { url }, fileName)
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
    intermediateResultOutput: false,
    batchId: urlBatchId.value !== null ? urlBatchId.value : null
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
    await applyDefaultStandardReportTemplate()
  } finally {
    initializingContext.value = false
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  fetchAlgoMap()
  await initializeRunnerContext(true)
})

onBeforeUnmount(() => {
  clearTimeout(draftSaveTimer)
  saveExecutionDraft({ silent: true })
  clearCalcPolling()
  stopTraceFlowPan()
  stopPreprocessChartPan()
  Object.keys(reportPollTimers.value).forEach(id => {
    clearInterval(reportPollTimers.value[id])
  })
  reportPollTimers.value = {}
  if (traceFlowWheelFrame != null) {
    window.cancelAnimationFrame(traceFlowWheelFrame)
    traceFlowWheelFrame = null
  }
  window.clearTimeout(traceFlowWheelIdleTimer)
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
  await applyDefaultStandardReportTemplate()
}

function mockReportTemplates() {
  return [
    { id: 'mock-standard', templateCode: STANDARD_REPORT_TEMPLATE_CODE, templateName: `(模拟)${STANDARD_REPORT_TEMPLATE_NAME}` },
    { id: 'mock-2', templateName: '(模拟)作战效能评估报告模板 v2.1' }
  ]
}

async function applyDefaultStandardReportTemplate() {
  const reportCfg = stageConfig.reportOutput.config
  if (reportCfg.reportTemplateId || !Array.isArray(reportTemplateList.value) || !reportTemplateList.value.length) return
  const standard = reportTemplateList.value.find(item =>
    item?.templateCode === STANDARD_REPORT_TEMPLATE_CODE || item?.templateName === STANDARD_REPORT_TEMPLATE_NAME
  )
  if (!standard?.id) return
  reportCfg.reportTemplateId = standard.id
  if (String(standard.id).startsWith('mock-')) return
  await onReportTemplateChange(standard.id, { silent: true })
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

// ==================== 结果溯源：预处理对比 (外部服务) ====================
const huageUrl = "/huage-api"
const preprocessDataMap = reactive({}) // 缓存获取到的预处理数据
const preprocessFetchingFields = reactive(new Set()) // 记录正在获取中的字段
const PREPROCESS_PREVIEW_LIMIT = 10
const PREPROCESS_CHART_POINT_LIMIT = 800
const preprocessDataDetailVisible = ref(false)
const preprocessDataDetailField = ref(null)
const preprocessDataDetailKeyword = ref('')
const preprocessDataDetailPageSizes = [20, 50, 100, 200]
const preprocessDataDetailPage = reactive({
  before: 1,
  after: 1,
  pageSize: 50
})

async function fetchPreprocessData(tableName, fieldName, indicatorCode) {
  const reqCode = String(requirementId.value || '')
  if (!reqCode || !tableName || !fieldName) return

  const cacheKey = `${reqCode}|${tableName}|${fieldName}|${indicatorCode}`
  if (preprocessDataMap[cacheKey] || preprocessFetchingFields.has(cacheKey)) return

  preprocessFetchingFields.add(cacheKey)
  try {
    const res = await axios.post(`${huageUrl}/table-data/field-mapping-data/condition-level`, {
      batchId: urlBatchId.value,
      fieldName,
      indicatorCode,
      requirementCode: reqCode,
      tableName
    })

    if (res.data?.code === 200 && Array.isArray(res.data?.data)) {
      // 匹配当前指标的数据
      const matched = res.data.data.find(d => d.indicatorCode === indicatorCode) || res.data.data[0]
      if (matched) {
        preprocessDataMap[cacheKey] = matched
      }
    }
  } catch (e) {
    console.error('[Huage] Fetch preprocess data failed:', e)
  } finally {
    preprocessFetchingFields.delete(cacheKey)
  }
}

function handleViewDetail() {
  buildDetailTree()
}

function openPreprocessDataDetail(field) {
  preprocessDataDetailField.value = field
  preprocessDataDetailKeyword.value = ''
  preprocessDataDetailPage.before = 1
  preprocessDataDetailPage.after = 1
  preprocessDataDetailVisible.value = true
}

function formatPreprocessRecordValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch (e) {
      return String(value)
    }
  }
  return String(value)
}

function normalizePreprocessRows(records = [], keyword = '') {
  const query = String(keyword || '').trim().toLowerCase()
  return records.map((record, index) => {
    const value = formatPreprocessRecordValue(record?.value ?? record)
    const timestamp = formatPreprocessRecordValue(record?.timestamp || record?.time || record?.date || record?.label || '')
    return {
      no: index + 1,
      value,
      timestamp
    }
  }).filter(row => {
    if (!query) return true
    return `${row.no} ${row.value} ${row.timestamp}`.toLowerCase().includes(query)
  })
}

function paginatePreprocessRows(rows = [], page = 1, pageSize = 50) {
  const safePageSize = Math.max(1, Number(pageSize) || 50)
  const safePage = Math.max(1, Number(page) || 1)
  const start = (safePage - 1) * safePageSize
  return rows.slice(start, start + safePageSize)
}

const preprocessDetailBeforeRows = computed(() =>
  normalizePreprocessRows(preprocessDataDetailField.value?.fullBeforeRecords || [], preprocessDataDetailKeyword.value)
)
const preprocessDetailAfterRows = computed(() =>
  normalizePreprocessRows(preprocessDataDetailField.value?.fullAfterRecords || [], preprocessDataDetailKeyword.value)
)
const preprocessDetailBeforePageRows = computed(() =>
  paginatePreprocessRows(preprocessDetailBeforeRows.value, preprocessDataDetailPage.before, preprocessDataDetailPage.pageSize)
)
const preprocessDetailAfterPageRows = computed(() =>
  paginatePreprocessRows(preprocessDetailAfterRows.value, preprocessDataDetailPage.after, preprocessDataDetailPage.pageSize)
)

watch(preprocessDataDetailKeyword, () => {
  preprocessDataDetailPage.before = 1
  preprocessDataDetailPage.after = 1
})

watch(() => preprocessDataDetailPage.pageSize, () => {
  preprocessDataDetailPage.before = 1
  preprocessDataDetailPage.after = 1
})

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

async function handleDetailNodeClick(data) {
  selectedDetailNode.value = data

  // 如果是叶子节点且有计算规则，尝试加载真实的预处理数据
  if (!data.children?.length && data.computeRule) {
    await nextTick() // 等待 trace graph 构建完成
    const nodes = selectedTraceFlowGraph.value.nodes || []
    const startNodes = nodes.filter(n => n.type === 'start')

    startNodes.forEach(node => {
      const fields = formatDataSourceFields(node)
      fields.forEach(field => {
        const fieldCode = field.tooltip?.includes('/')
          ? field.tooltip.split('/').pop().trim()
          : field.label

        fetchPreprocessData(node.value, fieldCode, data.label)
      })
    })
  }
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

const selectedTraceFlowGraph = computed(() => buildTraceFlowGraph(selectedDetailNode.value?.computeRule))
const selectedPreprocessPreview = computed(() => buildPreprocessPreview(selectedTraceFlowGraph.value.nodes))
const preprocessChartMode = ref('overlay')
const preprocessChartViewport = reactive({
  scale: 1,
  x: 0,
  y: 0,
  dragging: false,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0,
  chartWidth: 1200
})
const traceFlowViewport = reactive({
  scale: 1,
  x: 0,
  y: 0,
  dragging: false,
  transforming: false,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0
})
let traceFlowWheelFrame = null
let traceFlowWheelIdleTimer = null
let pendingTraceFlowWheel = null

const traceFlowTransform = computed(() => {
  const x = Number(traceFlowViewport.x.toFixed(2))
  const y = Number(traceFlowViewport.y.toFixed(2))
  const scale = Number(traceFlowViewport.scale.toFixed(3))
  return `translate(${x}px, ${y}px) scale(${scale})`
})

function getPreprocessDataTransform(chart) {
  return getChartDataTransform(preprocessChartViewport, chart)
}

function getPreprocessPointRadius(radius) {
  const scale = Math.max(1, Number(preprocessChartViewport.scale) || 1)
  return Number((radius / scale).toFixed(3))
}

watch(() => selectedDetailNode.value?.uid, () => {
  resetTraceFlowViewport()
  resetPreprocessChartViewport()
})

function buildTraceFlowGraph(computeRule) {
  const rawNodes = Array.isArray(computeRule?.method?.node) ? computeRule.method.node.filter(Boolean) : []
  const rawEdges = Array.isArray(computeRule?.method?.lineList) ? computeRule.method.lineList.filter(Boolean) : []
  if (!rawNodes.length) return { nodes: [], edges: [], canvasWidth: 720, canvasHeight: 220, viewportHeight: 320 }

  const cardSize = {
    start: { width: 300, height: 214 },
    algo: { width: 320, height: 210 },
    result: { width: 250, height: 150 }
  }
  const byId = new Map(rawNodes.map(n => [String(n.id), n]))
  const edges = rawEdges
    .map((edge, idx) => ({
      sourceId: String(edge.sourceId ?? edge.source ?? ''),
      targetId: String(edge.targetId ?? edge.target ?? ''),
      idx
    }))
    .filter(edge => edge.sourceId && edge.targetId && byId.has(edge.sourceId) && byId.has(edge.targetId))

  if (!edges.length && rawNodes.length > 1) {
    for (let i = 0; i < rawNodes.length - 1; i++) {
      edges.push({ sourceId: String(rawNodes[i].id), targetId: String(rawNodes[i + 1].id), idx: i })
    }
  }

  const layout = autoLayoutTraceNodes(rawNodes, edges, cardSize)

  const nodes = rawNodes.map(raw => {
    const type = normalizeTraceNodeType(raw.type)
    const size = cardSize[type] || cardSize.algo
    const pos = layout[String(raw.id)] || { x: 24, y: 24 }
    return {
      ...raw,
      type,
      id: String(raw.id),
      x: pos.x,
      y: pos.y,
      width: size.width,
      height: size.height
    }
  })
  const nodeById = new Map(nodes.map(n => [n.id, n]))
  const canvasWidth = Math.max(760, ...nodes.map(n => n.x + n.width + 56))
  const canvasHeight = Math.max(260, ...nodes.map(n => n.y + n.height + 44))
  const viewEdges = edges.map(edge => {
    const source = nodeById.get(edge.sourceId)
    const target = nodeById.get(edge.targetId)
    if (!source || !target) return null
    const startX = source.x + source.width
    const startY = source.y + source.height / 2
    const endX = target.x
    const endY = target.y + target.height / 2
    const dx = Math.max(72, endX - startX)
    const handle = dx * 0.5
    return {
      key: `${edge.sourceId}-${edge.targetId}-${edge.idx}`,
      path: `M ${startX + 8} ${startY} C ${startX + handle} ${startY}, ${endX - handle} ${endY}, ${endX - 10} ${endY}`
    }
  }).filter(edge => edge?.path)

  const viewportHeight = Math.min(Math.max(canvasHeight, 320), 520)
  return { nodes, edges: viewEdges, canvasWidth, canvasHeight, viewportHeight }
}

function clampTraceFlowScale(scale) {
  return Math.min(1.8, Math.max(0.35, scale))
}

function handleTraceFlowWheel(event) {
  const rect = event.currentTarget.getBoundingClientRect()
  const pointerX = event.clientX - rect.left
  const pointerY = event.clientY - rect.top
  pendingTraceFlowWheel = {
    pointerX,
    pointerY,
    deltaY: (pendingTraceFlowWheel?.deltaY || 0) + event.deltaY
  }
  traceFlowViewport.transforming = true
  if (traceFlowWheelFrame == null) {
    traceFlowWheelFrame = window.requestAnimationFrame(flushTraceFlowWheel)
  }
  window.clearTimeout(traceFlowWheelIdleTimer)
  traceFlowWheelIdleTimer = window.setTimeout(finishTraceFlowTransform, 140)
}

function flushTraceFlowWheel() {
  traceFlowWheelFrame = null
  const pending = pendingTraceFlowWheel
  pendingTraceFlowWheel = null
  if (!pending) return

  const oldScale = traceFlowViewport.scale
  const wheelFactor = Math.exp(-pending.deltaY * 0.0018)
  const nextScale = clampTraceFlowScale(Number((oldScale * wheelFactor).toFixed(3)))
  if (nextScale === oldScale) return

  const contentX = (pending.pointerX - traceFlowViewport.x) / oldScale
  const contentY = (pending.pointerY - traceFlowViewport.y) / oldScale
  traceFlowViewport.scale = nextScale
  traceFlowViewport.x = Math.round((pending.pointerX - contentX * nextScale) * 100) / 100
  traceFlowViewport.y = Math.round((pending.pointerY - contentY * nextScale) * 100) / 100
}

function finishTraceFlowTransform() {
  traceFlowViewport.x = Math.round(traceFlowViewport.x)
  traceFlowViewport.y = Math.round(traceFlowViewport.y)
  traceFlowViewport.scale = Number(traceFlowViewport.scale.toFixed(3))
  traceFlowViewport.transforming = false
}

function startTraceFlowPan(event) {
  if (event.button !== 0) return
  if (event.target?.closest?.('.inline-preview-btn, .el-button, .el-tag')) return
  traceFlowViewport.dragging = true
  traceFlowViewport.transforming = true
  traceFlowViewport.startX = event.clientX
  traceFlowViewport.startY = event.clientY
  traceFlowViewport.originX = traceFlowViewport.x
  traceFlowViewport.originY = traceFlowViewport.y
  window.addEventListener('mousemove', moveTraceFlowPan)
  window.addEventListener('mouseup', stopTraceFlowPan)
}

function moveTraceFlowPan(event) {
  if (!traceFlowViewport.dragging) return
  traceFlowViewport.x = Math.round((traceFlowViewport.originX + event.clientX - traceFlowViewport.startX) * 100) / 100
  traceFlowViewport.y = Math.round((traceFlowViewport.originY + event.clientY - traceFlowViewport.startY) * 100) / 100
}

function stopTraceFlowPan() {
  if (traceFlowViewport.dragging) {
    finishTraceFlowTransform()
  }
  traceFlowViewport.dragging = false
  window.removeEventListener('mousemove', moveTraceFlowPan)
  window.removeEventListener('mouseup', stopTraceFlowPan)
}

function resetTraceFlowViewport() {
  pendingTraceFlowWheel = null
  if (traceFlowWheelFrame != null) {
    window.cancelAnimationFrame(traceFlowWheelFrame)
    traceFlowWheelFrame = null
  }
  window.clearTimeout(traceFlowWheelIdleTimer)
  traceFlowViewport.scale = 1
  traceFlowViewport.x = 0
  traceFlowViewport.y = 0
  traceFlowViewport.transforming = false
}

function clampPreprocessChartScale(scale) {
  return Math.min(4, Math.max(1, scale))
}

function handlePreprocessChartWheel(event) {
  const rect = event.currentTarget.getBoundingClientRect()
  const chartHeight = Number(event.currentTarget.dataset.chartHeight || 300)
  const plotTop = Number(event.currentTarget.dataset.chartTop || 18)
  const plotBottom = Number(event.currentTarget.dataset.chartBottom || chartHeight - 38)
  const pointerX = ((event.clientX - rect.left) / Math.max(1, rect.width)) * 1200
  const pointerY = ((event.clientY - rect.top) / Math.max(1, rect.height)) * chartHeight
  const plotLeft = 48
  const plotRight = 1184
  const plotWidth = plotRight - plotLeft
  const plotHeight = plotBottom - plotTop
  const oldScale = preprocessChartViewport.scale
  const nextScale = clampPreprocessChartScale(Number((oldScale * Math.exp(-event.deltaY * 0.0016)).toFixed(3)))
  if (nextScale === oldScale) return

  const anchorX = Math.min(plotRight, Math.max(plotLeft, pointerX))
  const anchorY = Math.min(plotBottom, Math.max(plotTop, pointerY))
  const sourceX = (anchorX - plotLeft * (1 - oldScale) - preprocessChartViewport.x) / oldScale
  const sourceY = (anchorY - plotBottom * (1 - oldScale) - preprocessChartViewport.y) / oldScale
  const nextX = anchorX - sourceX * nextScale - plotLeft * (1 - nextScale)
  const nextY = anchorY - sourceY * nextScale - plotBottom * (1 - nextScale)
  const chartBounds = { left: plotLeft, right: plotRight, top: plotTop, bottom: plotBottom || plotTop + plotHeight }

  preprocessChartViewport.scale = nextScale
  preprocessChartViewport.x = clampChartTranslateX(Math.round(nextX * 100) / 100, chartBounds, nextScale)
  preprocessChartViewport.y = clampChartTranslateY(Math.round(nextY * 100) / 100, chartBounds, nextScale)
}

function startPreprocessChartPan(event) {
  if (event.button !== 0) return
  const rect = event.currentTarget.getBoundingClientRect()
  preprocessChartViewport.dragging = true
  preprocessChartViewport.startX = event.clientX
  preprocessChartViewport.startY = event.clientY
  preprocessChartViewport.originX = preprocessChartViewport.x
  preprocessChartViewport.originY = preprocessChartViewport.y
  preprocessChartViewport.chartWidth = Math.max(1, rect.width)
  preprocessChartViewport.chartHeight = Math.max(1, rect.height)
  preprocessChartViewport.chartTop = Number(event.currentTarget.dataset.chartTop || 18)
  preprocessChartViewport.chartBottom = Number(event.currentTarget.dataset.chartBottom || Number(event.currentTarget.dataset.chartHeight || 300) - 38)
  window.addEventListener('mousemove', movePreprocessChartPan)
  window.addEventListener('mouseup', stopPreprocessChartPan)
}

function movePreprocessChartPan(event) {
  if (!preprocessChartViewport.dragging) return
  const svgWidth = 1200
  const rectWidth = Math.max(1, preprocessChartViewport.chartWidth)
  const chartHeight = Number(preprocessChartViewport.chartBottom || 246) + 38
  const rectHeight = Math.max(1, preprocessChartViewport.chartHeight)
  const deltaX = ((event.clientX - preprocessChartViewport.startX) / rectWidth) * svgWidth
  const deltaY = ((event.clientY - preprocessChartViewport.startY) / rectHeight) * chartHeight
  const nextX = Math.round((preprocessChartViewport.originX + deltaX) * 100) / 100
  const nextY = Math.round((preprocessChartViewport.originY + deltaY) * 100) / 100
  const chartBounds = {
    left: 48,
    right: 1184,
    top: Number(preprocessChartViewport.chartTop || 18),
    bottom: Number(preprocessChartViewport.chartBottom || 262)
  }
  preprocessChartViewport.x = clampChartTranslateX(nextX, chartBounds, preprocessChartViewport.scale)
  preprocessChartViewport.y = clampChartTranslateY(nextY, chartBounds, preprocessChartViewport.scale)
}

function stopPreprocessChartPan() {
  preprocessChartViewport.dragging = false
  window.removeEventListener('mousemove', movePreprocessChartPan)
  window.removeEventListener('mouseup', stopPreprocessChartPan)
}

function resetPreprocessChartViewport() {
  preprocessChartViewport.scale = 1
  preprocessChartViewport.x = 0
  preprocessChartViewport.y = 0
  preprocessChartViewport.dragging = false
}

function getPreprocessViewportXTicks(chart, maxTicks = 5) {
  return buildViewportXTicks(chart, preprocessChartViewport, maxTicks)
}

function getPreprocessViewportYTicks(chart, maxTicks = 5) {
  return buildViewportYTicks(chart, preprocessChartViewport, maxTicks)
}

function normalizeTraceNodeType(type) {
  if (type === 'start' || type === 'algo' || type === 'result') return type
  if (type === 'data') return 'start'
  if (type === 'algorithm') return 'algo'
  return 'algo'
}

function autoLayoutTraceNodes(nodes, edges, cardSize) {
  const ids = nodes.map(n => String(n.id))
  const incoming = new Map(ids.map(id => [id, 0]))
  const outgoing = new Map(ids.map(id => [id, []]))
  edges.forEach(edge => {
    incoming.set(edge.targetId, (incoming.get(edge.targetId) || 0) + 1)
    outgoing.get(edge.sourceId)?.push(edge.targetId)
  })
  const level = new Map()
  const queue = ids.filter(id => (incoming.get(id) || 0) === 0)
  if (!queue.length && ids.length) queue.push(ids[0])
  queue.forEach(id => level.set(id, 0))
  while (queue.length) {
    const id = queue.shift()
    const nextLevel = (level.get(id) || 0) + 1
    for (const targetId of outgoing.get(id) || []) {
      if (!level.has(targetId) || nextLevel > level.get(targetId)) {
        level.set(targetId, nextLevel)
        queue.push(targetId)
      }
    }
  }
  ids.forEach((id, idx) => {
    if (!level.has(id)) level.set(id, idx)
  })

  const grouped = {}
  ids.forEach(id => {
    const l = level.get(id) || 0
    if (!grouped[l]) grouped[l] = []
    grouped[l].push(id)
  })
  const byId = new Map(nodes.map(n => [String(n.id), n]))
  const out = {}
  const groupMetrics = Object.entries(grouped).map(([levelKey, group]) => {
    const totalHeight = group.reduce((sum, id) => {
      const type = normalizeTraceNodeType(byId.get(id)?.type)
      const size = cardSize[type] || cardSize.algo
      return sum + size.height
    }, 0) + Math.max(0, group.length - 1) * 34
    return { levelKey, group, totalHeight }
  })
  const innerHeight = Math.max(260, ...groupMetrics.map(item => item.totalHeight + 48))
  groupMetrics.forEach(({ levelKey, group, totalHeight }) => {
    const col = Number(levelKey)
    let y = Math.max(24, (innerHeight - totalHeight) / 2)
    group.forEach((id, row) => {
      const type = normalizeTraceNodeType(byId.get(id)?.type)
      const size = cardSize[type] || cardSize.algo
      out[id] = {
        x: 28 + col * 390,
        y
      }
      y += size.height + 34
    })
  })
  return out
}

function formatDataSourceFields(node) {
  const fields = Array.isArray(node?.fields)
    ? node.fields
    : (node?.field != null ? [node.field] : [])
  return fields.map((field, idx) => {
    if (field && typeof field === 'object' && !Array.isArray(field)) {
      const entries = Object.entries(field)
      const [code, label] = entries[0] || ['', '']
      return {
        key: `${code || label}-${idx}`,
        label: String(label || code || '字段'),
        tooltip: code && label ? `${label} / ${code}` : String(label || code || '字段')
      }
    }
    const value = String(field || '').trim()
    return { key: `${value}-${idx}`, label: value || '字段', tooltip: value || '字段' }
  }).filter(field => field.label)
}

function buildPreprocessPreview(nodes = []) {
  const sourceNodes = nodes.filter(node => node.type === 'start')
  const fields = sourceNodes.flatMap((node, sourceIndex) => {
    const sourceName = node.name || node.value || `数据源${sourceIndex + 1}`
    const tableName = node.value || ''
    return formatDataSourceFields(node).map((field, fieldIndex) => ({
      ...field,
      sourceName,
      tableName,
      sourceIndex,
      fieldIndex
    }))
  })

  const reqCode = String(requirementId.value || '')
  const indicatorCode = selectedDetailNode.value?.label || ''

  return fields.slice(0, 4).map((field, idx) => {
    const code = field.tooltip?.includes('/')
      ? field.tooltip.split('/').pop().trim()
      : field.label

    const cacheKey = `${reqCode}|${field.tableName}|${code}|${indicatorCode}`
    const remoteData = preprocessDataMap[cacheKey]
    const isFetching = preprocessFetchingFields.has(cacheKey)

    let beforeValues = []
    let afterValues = []
    let beforeRecords = []
    let afterRecords = []
    let isNumeric = true
    let hasData = false

    if (remoteData) {
      hasData = true
      // 提取远程数据
      const bList = remoteData.beforePreprocessList?.[0]?.records || []
      const aList = remoteData.afterPreprocessList?.[0]?.records || []

      beforeRecords = bList
      afterRecords = aList

      // 判断是否为数值型
      const allValues = [...bList, ...aList].map(r => r.value)
      isNumeric = allValues.length > 0 && allValues.every(v => v !== null && v !== '' && !isNaN(Number(v)))

      if (isNumeric) {
        beforeValues = bList.map(r => Number(r.value))
        afterValues = aList.map(r => Number(r.value))
      }
    }

    const chartBeforeRecords = samplePreprocessRecords(beforeRecords, PREPROCESS_CHART_POINT_LIMIT)
    const chartAfterRecords = samplePreprocessRecords(afterRecords, PREPROCESS_CHART_POINT_LIMIT)
    const chartSampled = chartBeforeRecords.length < beforeRecords.length || chartAfterRecords.length < afterRecords.length
    const chart = buildPreprocessChart(chartBeforeRecords, chartAfterRecords)
    const beforeChart = buildSinglePreprocessChart(chartBeforeRecords, 160)
    const afterChart = buildSinglePreprocessChart(chartAfterRecords, 160)

    return {
      key: `${field.key}-${idx}`,
      label: field.label,
      code,
      sourceName: field.sourceName,
      isNumeric,
      isFetching,
      hasData,
      beforeRecords: beforeRecords.slice(0, PREPROCESS_PREVIEW_LIMIT),
      afterRecords: afterRecords.slice(0, PREPROCESS_PREVIEW_LIMIT),
      fullBeforeRecords: beforeRecords,
      fullAfterRecords: afterRecords,
      beforeTotal: beforeRecords.length,
      afterTotal: afterRecords.length,
      hasMoreRecords: beforeRecords.length > PREPROCESS_PREVIEW_LIMIT || afterRecords.length > PREPROCESS_PREVIEW_LIMIT,
      chartSampled,
      chart,
      before: chart.before,
      after: chart.after,
      beforeChart,
      afterChart
    }
  })
}

function buildPreprocessChart(beforeRecords = [], afterRecords = [], height = 300) {
  const width = 1200
  const left = 48
  const right = 1184
  const top = 18
  const bottom = height - 38

  const beforeValues = beforeRecords.map(r => Number(r.value))
  const afterValues = afterRecords.map(r => Number(r.value))
  const values = [...beforeValues, ...afterValues]

  const { min, max, yTicks } = buildChartScale(values, top, bottom)
  const sampleCount = Math.max(beforeRecords.length, afterRecords.length)

  const beforePoints = toChartPoints(beforeRecords, min, max, left, right, top, bottom)
  const afterPoints = toChartPoints(afterRecords, min, max, left, right, top, bottom)
  const records = Array.from({ length: sampleCount }, (_, index) => beforeRecords[index] || afterRecords[index] || null)

  return {
    left,
    right,
    top,
    bottom,
    min,
    max,
    yTicks,
    records,
    sampleCount,
    before: {
      polyline: beforePoints.map(p => `${p.x},${p.y}`).join(' '),
      points: beforePoints
    },
    after: {
      polyline: afterPoints.map(p => `${p.x},${p.y}`).join(' '),
      points: afterPoints
    }
  }
}

function samplePreprocessRecords(records = [], limit = PREPROCESS_CHART_POINT_LIMIT) {
  if (!Array.isArray(records) || records.length <= limit) return records
  if (limit <= 2) return [records[0], records[records.length - 1]].filter(Boolean)

  const lastIndex = records.length - 1
  const step = lastIndex / (limit - 1)
  const indexes = new Set([0, lastIndex])
  for (let i = 1; i < limit - 1; i += 1) {
    indexes.add(Math.round(i * step))
  }
  return Array.from(indexes)
    .sort((a, b) => a - b)
    .map(index => records[index])
    .filter(Boolean)
}

function buildSinglePreprocessChart(records = [], height = 160) {
  const left = 48
  const right = 1184
  const top = 14
  const bottom = height - 24
  const values = records.map(r => Number(r.value))
  const { min, max, yTicks } = buildChartScale(values, top, bottom)
  const points = toChartPoints(records, min, max, left, right, top, bottom)
  return {
    left,
    right,
    top,
    bottom,
    min,
    max,
    yTicks,
    records,
    sampleCount: records.length,
    polyline: points.map(p => `${p.x},${p.y}`).join(' '),
    points
  }
}

function buildChartScale(values = [], top, bottom) {
  const safeValues = values.length ? values : [0, 1]
  const minRaw = Math.min(...safeValues)
  const maxRaw = Math.max(...safeValues)
  const padding = Math.max(4, (maxRaw - minRaw) * 0.12)
  const min = Math.floor((minRaw - padding) / 10) * 10
  const max = Math.ceil((maxRaw + padding) / 10) * 10
  const ticks = [max, max - (max - min) / 4, (max + min) / 2, min + (max - min) / 4, min]
  const yTicks = ticks.map(value => ({
    value,
    label: Number(value.toFixed(0)),
    y: valueToChartY(value, min, max, top, bottom)
  }))
  return { min, max, yTicks }
}

function toChartPoints(records = [], min, max, left, right, top, bottom) {
  if (!records.length) return []
  return records.map((r, idx) => {
    const value = Number(r.value)
    const x = indexToChartX(idx, records.length, left, right)
    const y = valueToChartY(value, min, max, top, bottom)
    return {
      x,
      y,
      value,
      timestamp: r.timestamp || r.time || '-',
      index: idx
    }
  })
}

function indexToChartX(index, count, left, right) {
  return viewportIndexToChartX(index, count, left, right)
}

function valueToChartY(value, min, max, top, bottom) {
  const range = max - min || 1
  return Number((bottom - ((value - min) / range) * (bottom - top)).toFixed(2))
}

function formatTimeRange(node) {
  const start = String(node?.taskTimeStart || '').trim()
  const end = String(node?.taskTimeEnd || '').trim()
  if (start && end) return `${start} 至 ${end}`
  return start || end || ''
}

function getAlgorithmPreviewId(node) {
  if (!node) return ''
  return node.algorithmId || node.value || ''
}

function getAlgorithmName(rule) {
  if (!rule) return '—'
  const fullInfo = getAlgorithmFullInfo(rule)
  if (fullInfo?.algorithmName) return fullInfo.algorithmName
  if (rule.algorithmName) return rule.algorithmName
  if (rule.url) {
    const parts = rule.url.split('/')
    const file = parts[parts.length - 1]
    return file.replace('.zip', '').split('-')[0] // 去掉可能的哈希后缀
  }
  return rule.method?.name || '规则基础算法'
}

function getAlgorithmParams(rule) {
  if (!rule) return []
  // 检查 config 字段，有些是在 computeRule 下直接有配置
  const config = rule.config || rule.params
  if (!config) return []
  return Array.isArray(config) ? config : []
}

function getAlgoInfo(id) {
  if (!id) return { algorithmName: '系统默认', algorithmDescription: '未指定特定算法' }
  const targetId = String(id)
  return algoMap.value[targetId] || { algorithmName: `算法#${id}`, algorithmDescription: '数据加载中或未找到详细说明' }
}

function getAlgorithmFullInfo(node) {
  if (!node) return null
  const id = node.algorithmId || node.value || node.id
  return algoMap.value[String(id)] || null
}

function getTraceTableSummary(param) {
  const { columns, data } = param
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (column.label === '权重') {
      const values = data.map(item => Number(item.weight))
      const total = values.reduce((prev, curr) => prev + curr, 0)
      sums[index] = (total * 100).toFixed(2) + '%'
      return
    }
    if (column.label === '贡献分值') {
      const values = data.map(item => Number(item.weight) * Number(item.calculatedScore))
      const total = values.reduce((prev, curr) => prev + curr, 0)
      sums[index] = total.toFixed(2)
      return
    }
    sums[index] = ''
  })
  return sums
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
  if (s >= 90) return '#00f2ff' // 优秀 - 电光青
  if (s >= 75) return '#00d1ff' // 良好 - 苍穹蓝
  if (s >= 60) return '#ffd700' // 及格 - 战术金
  return '#ff4d4f' // 未达标 - 警示红
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
        {
          uid: genUid(), label: '任务完成度', weight: 0.4, children: [
            { uid: genUid(), label: '响应时间', weight: 0.5 },
            { uid: genUid(), label: '成功率', weight: 0.5 }
          ]
        },
        {
          uid: genUid(), label: '装备可靠性', weight: 0.35, children: [
            { uid: genUid(), label: 'MTBF', weight: 1.0 }
          ]
        },
        {
          uid: genUid(), label: '资源消耗', weight: 0.25, children: [
            { uid: genUid(), label: '能耗', weight: 0.6 },
            { uid: genUid(), label: '弹药消耗', weight: 0.4 }
          ]
        }
      ]
    }
  ]
}
</script>

<style scoped lang="scss">
/* 评分等级高级只读卡片 */
.score-level-premium-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
    background: var(--el-fill-color-light);
  }

  .card-left-section {
    z-index: 2;

    .level-badge {
      font-size: 13px;
      font-weight: 600;
      padding: 5px 12px;
      border-radius: 6px;
      letter-spacing: 0.5px;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    }
  }

  .card-right-section {
    z-index: 2;
    text-align: right;

    .threshold-label {
      font-size: 11px;
      color: var(--el-text-color-secondary);
      margin-bottom: 2px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .threshold-value {
      font-family: 'Outfit', 'Inter', system-ui, -apple-system, sans-serif;
      font-size: 14px;
      font-weight: 700;
      display: flex;
      align-items: baseline;
      gap: 2px;
      justify-content: flex-end;

      .symbol {
        font-size: 11px;
        opacity: 0.85;
        font-weight: 500;
      }

      .number {
        font-size: 18px;
        line-height: 1;
      }

      .unit {
        font-size: 11px;
        opacity: 0.85;
        font-weight: 500;
        margin-left: 1px;
      }
    }
  }

  .glow-layer {
    position: absolute;
    top: 0;
    right: 0;
    width: 100px;
    height: 100px;
    opacity: 0.6;
    pointer-events: none;
    z-index: 1;
    transition: opacity 0.3s;
  }

  &:hover .glow-layer {
    opacity: 0.8;
  }
}


/* =============================================================================
   Celestial Command Interface - Flow Runner Theme
   ============================================================================= */

.flow-runner-page {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  --runner-soft-bg: rgba(var(--sider-bg-color-rgb), 0.48);
  --runner-accent-soft-bg: rgba(var(--primary-color-rgb), 0.06);
  --runner-accent-hover-bg: rgba(var(--primary-color-rgb), 0.1);
  --runner-accent-border: rgba(var(--border-color-hover-rgb), 0.18);
  --runner-table-row-bg: rgba(var(--sider-bg-color-rgb), 0.42);
  --runner-table-row-hover-bg: rgba(var(--primary-color-rgb), 0.08);
  --runner-table-border: rgba(var(--border-color-rgb), 0.86);
  --runner-header-bg: rgba(var(--header-bg-color-rgb), 0.96);
  --runner-header-bg-strong: rgba(var(--sider-bg-color-rgb), 0.98);
  --runner-header-muted: rgba(var(--text-color-secondary-rgb), 0.72);
  --runner-header-subtle: rgba(var(--text-color-placeholder-rgb), 0.38);
  --runner-header-shadow: 0 4px 20px rgba(var(--bg-color-rgb), 0.16);
  --trace-panel-bg: rgba(var(--card-bg-color-rgb), 0.96);
  --trace-muted-panel-bg: rgba(var(--sider-bg-color-rgb), 0.72);
  --trace-card-border: rgba(var(--border-color-rgb), 0.92);
  --trace-code-bg: rgba(var(--warning-color-rgb), 0.12);
  --trace-code-border: rgba(var(--warning-color-rgb), 0.28);
  --trace-code-text: #b45309;
  --trace-score-excellent: var(--primary-color);
  --trace-score-good: var(--info-color);
  --trace-score-pass: var(--warning-color);
  --trace-score-fail: var(--danger-color);
  --log-card-bg: rgba(var(--card-bg-color-rgb), 0.98);
  --log-card-hover-bg: rgba(var(--primary-color-rgb), 0.05);
  --log-card-border: rgba(var(--border-color-rgb), 0.9);
  --log-header-bg: rgba(var(--primary-color-rgb), 0.06);
  --log-timeline-line: rgba(var(--primary-color-rgb), 0.18);
  --log-timestamp-color: var(--text-color-secondary);
  --runner-panel-divider: rgba(var(--border-color-rgb), 0.78);
  --runner-exec-card-bg: rgba(var(--card-bg-color-rgb), 0.98);
  --runner-exec-card-border: rgba(var(--border-color-rgb), 0.92);
  --runner-exec-card-shadow: 0 10px 24px rgba(var(--bg-color-rgb), 0.08);
  --runner-exec-finished-bg: linear-gradient(90deg, rgba(var(--primary-color-rgb), 0.08), rgba(var(--card-bg-color-rgb), 0.98));
  --runner-exec-accent: var(--primary-color);
  --runner-exec-label: var(--text-color-secondary);
  --runner-exec-value: var(--text-color-primary);
  --runner-exec-muted: var(--text-color-secondary);
  --runner-exec-divider: rgba(var(--border-color-rgb), 0.72);
  --runner-exec-log-hover: rgba(var(--primary-color-rgb), 0.08);
  // Replaced hardcoded colors with theme variables
  background-color: var(--bg-color);
  color: var(--text-color-primary);
  overflow: hidden;
  font-family: 'Inter', -apple-system, sans-serif;
}

:global(html.dark) .flow-runner-page {
  --runner-panel-divider: rgba(255, 255, 255, 0.1);
  --runner-exec-card-bg: rgba(255, 255, 255, 0.03);
  --runner-exec-card-border: rgba(255, 255, 255, 0.1);
  --runner-exec-card-shadow: none;
  --runner-exec-finished-bg: linear-gradient(90deg, rgba(var(--primary-color-rgb), 0.03), rgba(255, 255, 255, 0.02));
  --runner-exec-accent: var(--primary-color);
  --runner-exec-label: rgba(255, 255, 255, 0.4);
  --runner-exec-value: #fff;
  --runner-exec-muted: rgba(255, 255, 255, 0.7);
  --runner-exec-divider: rgba(255, 255, 255, 0.1);
  --runner-exec-log-hover: rgba(var(--primary-color-rgb), 0.05);
}

/* 顶部：任务上下文 */
/* 最终极致优化版标题栏样式 */
.runner-header-v2 {
  background: linear-gradient(to bottom, var(--runner-header-bg), var(--runner-header-bg-strong));
  backdrop-filter: blur(12px) saturate(180%);
  border-bottom: 1px solid var(--runner-table-border);
  z-index: 1000;
  box-shadow: var(--runner-header-shadow);

  .top-row {
    height: 56px;
    display: flex;
    align-items: center;
    padding: 0 24px;
    border-bottom: 1px solid rgba(var(--border-color-rgb), 0.42);
    position: relative;

    .row-left {
      flex: 1;

      .exit-btn {
        color: var(--text-color-secondary);
        font-size: 14px;
        font-weight: 500;
        transition: all 0.3s;

        &:hover {
          color: var(--text-color-primary);
          transform: translateX(-2px);
        }
      }
    }

    .meta-info {
      flex: 2;
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 32px;
      font-size: 14px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 10px;

        .m-label {
          color: var(--runner-header-muted);
          font-weight: 400;
        }

        .m-value {
          color: var(--border-color-hover);
          font-weight: 600;
          letter-spacing: 0.5px;
          text-shadow: var(--box-shadow-neon);
        }
      }

      .el-divider {
        background-color: var(--runner-table-border);
        height: 14px;
      }
    }

    .row-right {
      flex: 1;
      display: flex;
      justify-content: flex-end;

      .action-group {
        background: rgba(var(--sider-bg-color-rgb), 0.56);
        padding: 4px;
        border-radius: 8px;
        border: 1px solid var(--runner-table-border);
        display: flex;
        gap: 4px;
        box-shadow: inset 0 1px 1px rgba(var(--bg-color-rgb), 0.08);

        :deep(.el-button) {
          border: none;
          background: transparent;
          font-weight: 600;
          height: 28px;

          &.el-button--primary {
            background: linear-gradient(135deg, #00d2ff 0%, #3a7bd5 100%);
            color: #fff;
            box-shadow: 0 2px 6px rgba(0, 210, 255, 0.3);

            &:hover {
              opacity: 0.9;
              transform: translateY(-1px);
            }
          }

          &.el-button--warning {
            background: rgba(255, 170, 0, 0.15);
            color: #ffaa00;
            border: 1px solid rgba(255, 170, 0, 0.3);

            &:hover {
              background: rgba(255, 170, 0, 0.25);
            }
          }

          &.el-button--success {
            background: linear-gradient(135deg, #00b09b 0%, #96c93d 100%);
            color: #fff;
          }

          &:not(.el-button--primary):not(.el-button--warning):not(.el-button--success) {
            color: var(--runner-header-muted);

            &:hover {
              color: var(--text-color-primary);
              background: var(--runner-accent-soft-bg);
            }
          }
        }
      }
    }
  }

  .bottom-row {
    height: 80px;
    display: flex;
    align-items: center;
    padding: 0 12%;
    background: linear-gradient(to bottom, var(--runner-accent-soft-bg), transparent);

    :deep(.el-steps) {
      flex: 1;

      .el-step__head {
        .el-step__icon {
          width: 32px;
          height: 32px;
          font-size: 14px;
          background: var(--sider-bg-color);
          border-width: 2px;
          transition: all 0.3s;
        }

        .el-step__line {
          top: 16px;
          /* 32px icon / 2 = 16px center */
          background-color: var(--runner-table-border);
          height: 1px;
        }
      }

      .el-step__title {
        font-size: 14px;
        line-height: 24px;
        margin-top: 8px;
        font-weight: 600;
        letter-spacing: 0.5px;
      }

      /* 状态颜色细化 */
      .el-step.is-process {
        .el-step__icon {
          border-color: var(--border-color-hover);
          color: var(--border-color-hover);
          box-shadow: var(--box-shadow-neon);
          transform: scale(1.1);
        }

        .el-step__title {
          color: var(--border-color-hover) !important;
        }
      }

      .el-step.is-success {
        .el-step__icon {
          border-color: var(--border-color-hover);
          color: var(--border-color-hover);
        }

        .el-step__line-inner {
          border-color: var(--border-color-hover);
        }
      }
    }

    .step-label {
      cursor: pointer;
      color: var(--runner-header-muted);
      transition: all 0.3s;

      &:hover {
        color: var(--border-color-hover);
        transform: translateY(-1px);
      }

      &.is-active {
        color: var(--border-color-hover);
        text-shadow: var(--box-shadow-neon);
      }
    }
  }

  /* 底部状态汇总条：复刻旧版“标签感” */
  .status-summary-bar {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    padding: 10px 0 16px;
    background: transparent;

    .status-summary-item {
      display: flex;
      align-items: center;
      height: 24px;
      padding: 0 12px;
      border-radius: 4px;
      font-size: 12px;
      background: rgba(var(--sider-bg-color-rgb), 0.56);
      border: 1px solid var(--runner-table-border);
      color: var(--runner-header-muted);
      transition: all 0.3s;
      white-space: nowrap;

      .s-label {
        margin-right: 6px;
        opacity: 0.7;
      }

      .s-status {
        font-weight: 600;
      }

      /* 不同状态的标签风格 */
      &.completed {
        border-color: var(--runner-accent-border);
        color: var(--border-color-hover);
        background: var(--runner-accent-soft-bg);
      }

      &.running,
      &.is-active {
        border-color: var(--border-color-hover);
        color: var(--text-color-primary);
        background: var(--runner-accent-hover-bg);
        box-shadow: var(--box-shadow-neon);

        .s-label {
          opacity: 1;
        }
      }

      &.failed,
      &.stale {
        border-color: rgba(255, 77, 79, 0.3);
        color: #ff4d4f;
        background: rgba(255, 77, 79, 0.05);
      }

      &.pending {
        border-color: var(--runner-table-border);
        color: var(--runner-header-subtle);
      }
    }
  }
}

/* 移除旧的单行工具栏样式 */
.runner-toolbar {
  display: none;
}

/* 移除旧的区块样式 */
.runner-header,
.runner-context,
.runner-steps {
  display: none;
}

.runner-body {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: var(--bg-color);

  /* 自定义滚动条 */
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(164, 230, 255, 0.15);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.step-panel {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  flex: 1;
  animation: panelFadeIn 0.4s ease;
}

.placeholder-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  line-height: 1.35;
  min-height: 54px;
  justify-content: center;
}

.placeholder-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.placeholder-desc {
  font-size: 12px;
  color: var(--text-color-secondary);
}

.report-mapping-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.report-mapping-table :deep(.el-select),
.report-mapping-table :deep(.el-input) {
  width: 100%;
}

:global(.report-mapping-popper .el-select-group__title) {
  height: 28px;
  line-height: 28px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

:global(.report-mapping-popper .el-select-dropdown__item) {
  height: auto;
  min-height: 38px;
  padding: 0 12px;
  font-size: 13px;
  line-height: 20px;
}

:global(.report-mapping-popper .report-mapping-option) {
  display: flex;
  align-items: center;
  min-height: 38px;
  width: 100%;
  font-size: 13px;
}

@keyframes panelFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:deep(.tech-card),
:deep(.el-card) {
  background: var(--card-bg-color) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 4px !important;
  margin-bottom: 24px;
  box-shadow: var(--box-shadow-base) !important;

  .el-card__header {
    border-bottom: 1px solid var(--runner-table-border);
    padding: 12px 20px;

    .card-header {
      color: var(--border-color-hover);
      font-size: 15px;
      font-weight: bold;
      display: flex;
      align-items: center;
      gap: 10px;

      &::before {
        content: '';
        width: 3px;
        height: 14px;
        background: var(--border-color-hover);
        border-radius: 2px;
      }
    }
  }
}

/* 战术表单、输入框 */
:deep(.el-form) {
  .el-form-item__label {
    color: var(--text-color-secondary);
  }
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  background-color: var(--sider-bg-color) !important;
  box-shadow: 0 0 0 1px var(--runner-accent-border) inset !important;
  border-radius: 2px;

  .el-input__inner {
    color: var(--text-color-primary) !important;
  }
}

:deep(.el-input-number__increase),
:deep(.el-input-number__decrease) {
  background-color: var(--header-bg-color) !important;
  border-color: rgba(0, 242, 255, 0.1) !important;
  color: var(--text-color-secondary) !important;
}

.weight-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.tree-preview {
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 20px;

  .preview-title {
    color: var(--border-color-hover);
    font-size: 14px;
    font-weight: bold;
    margin-bottom: 16px;
    border-bottom: 1px solid var(--runner-table-border);
    padding-bottom: 10px;
  }
}

:deep(.el-tree) {
  background: transparent !important;
  color: var(--text-color-primary) !important;

  .el-tree-node__content:hover {
    background-color: rgba(0, 242, 255, 0.08) !important;
  }

  .el-tree-node.is-current>.el-tree-node__content {
    background-color: rgba(0, 242, 255, 0.15) !important;
    color: var(--border-color-hover) !important;
  }

  .tree-row {
    display: flex;
    justify-content: space-between;
    width: 100%;
    padding-right: 20px;
    font-size: 13px;
  }
}

/* 执行控制面板 */
.calc-exec-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--runner-panel-divider);
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* 极致紧凑版执行状态卡片 */
.unified-exec-card-compact {
  background: var(--runner-exec-card-bg);
  border: 1px solid var(--runner-exec-card-border);
  border-radius: 8px;
  padding: 12px 20px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 32px;
  position: relative;
  box-shadow: var(--runner-exec-card-shadow);
  transition: all 0.3s;

  &.is-finished {
    background: var(--runner-exec-finished-bg);
    border-left: 3px solid var(--runner-exec-accent);
  }

  .status-section {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 180px;

    .status-label-group {
      display: flex;
      align-items: center;
      gap: 10px;

      .s-label {
        font-size: 11px;
        color: var(--runner-exec-label);
        text-transform: uppercase;
      }
    }

    .progress-wrap {
      display: flex;
      align-items: center;
      gap: 12px;

      .progress-num {
        font-size: 14px;
        font-weight: bold;
        color: var(--border-color-hover);
        min-width: 40px;
      }

      :deep(.el-progress) {
        flex: 1;
      }
    }
  }

  .metrics-section {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 40px;

    .el-divider {
      height: 30px;
      border-color: var(--runner-exec-divider);
    }

    .metric-item {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .m-label {
        font-size: 11px;
        color: var(--runner-exec-label);
      }

      .m-value {
        font-size: 14px;
        font-weight: 600;
        color: var(--runner-exec-value);

        &.score {
          color: var(--runner-exec-accent);
          font-size: 16px;
        }
      }

      &.time {
        .m-value {
          font-size: 12px;
          font-weight: normal;
          color: var(--runner-exec-muted);
        }
      }
    }
  }

  .action-section {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;

    .recalc-btn {
      min-width: 88px;
      font-weight: 600;
    }

    .log-btn {
      font-size: 13px;
      padding: 4px 8px;

      &:hover {
        background: var(--runner-exec-log-hover);
      }
    }
  }
}

/* 结果明细布局 */
.result-content-wrap {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 500px;
  margin-top: 12px;
}

.result-side-tree {
  width: 300px;
  background: var(--sider-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  display: flex;
  flex-direction: column;

  .panel-header {
    padding: 14px 20px;
    background: rgba(0, 242, 255, 0.05);
    color: var(--border-color-hover);
    font-weight: bold;
    font-size: 14px;
    border-bottom: 1px solid var(--border-color);
  }

  .tree-container {
    padding: 10px;
    flex: 1;
    overflow-y: auto;
  }
}

.result-main-pane {
  flex: 1;
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.result-detail-page {
  flex: 1;
  min-height: 100%;
  overflow-y: auto;
  padding: 20px 24px 28px;
}

.detail-section {
  margin-bottom: 26px;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 46px;
  padding: 10px 16px 10px 18px;
  margin-bottom: 16px;
  color: var(--text-color-primary);
  background:
    linear-gradient(90deg, rgba(var(--border-color-rgb), 0.14), rgba(var(--card-bg-color-rgb), 0.56) 54%, transparent);
  border: 1px solid var(--runner-table-border);
  border-left: 0;
  border-radius: 0 8px 8px 0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.42);

  &::before {
    content: '';
    align-self: stretch;
    width: 5px;
    min-height: 26px;
    background: var(--border-color-hover);
    border-radius: 999px;
    box-shadow: 0 0 12px rgba(var(--border-color-rgb), 0.28);
  }

  &.trace-heading {
    background:
      linear-gradient(90deg, rgba(0, 198, 255, 0.14), rgba(var(--card-bg-color-rgb), 0.58) 54%, transparent);

    &::before {
      background: #00bcd4;
      box-shadow: 0 0 12px rgba(0, 188, 212, 0.26);
    }
  }
}

.section-title-main {
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0;
}

.section-title-sub {
  color: var(--text-color-secondary);
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.detail-pane-content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  align-items: start;
}

.node-brief-info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.brief-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  background: rgba(var(--sider-bg-color-rgb), 0.52);
  border: 1px solid var(--runner-table-border);
  border-radius: 6px;
}

.brief-label {
  color: var(--text-color-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.brief-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-color-primary);
  font-weight: 700;
}

/* 评分详情 */
.evaluation-dial-section {
  display: flex;
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 0;
  align-items: center;
  gap: 28px;
  min-width: 0;
}

.dial-inner-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .dial-score-num {
    font-size: 32px;
    font-weight: 900;
    color: var(--text-color-primary);
    line-height: 1;
    text-shadow: var(--box-shadow-neon);
  }

  .dial-score-unit {
    color: var(--text-color-secondary);
    font-size: 14px;
    margin-top: 4px;
  }

  :deep(.el-divider) {
    border-top-color: rgba(255, 255, 255, 0.1) !important;
    margin: 12px 0 !important;
  }

  // ... existing code ...

  .dial-grade-label {
    font-size: 18px;
    font-weight: 800;
    letter-spacing: 1px;
  }

  :deep(.el-progress--dashboard) {
    .el-progress__text {
      top: 50% !important;
      left: 50% !important;
      transform: translate(-50%, -50%) !important;
      width: 100%;
      margin: 0 !important;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
    }

    svg>path:first-child {
      stroke: rgba(255, 255, 255, 0.05) !important;
    }
  }
}

.performance-analysis {
  flex: 1;

  .analysis-title {
    color: var(--text-color-secondary);
    font-size: 14px;
    margin-bottom: 40px;
    text-transform: uppercase;
    letter-spacing: 1px;
  }

  .tier-scale {
    position: relative;
    height: 12px;
    margin-bottom: 40px;
    margin-top: 30px;
  }

  .tier-markers {
    position: absolute;
    top: -24px;
    left: 0;
    width: 100%;
    pointer-events: none;

    span {
      position: absolute;
      font-size: 11px;
      font-family: 'Space Grotesk', monospace;
      color: var(--text-color-placeholder);
      transform: translateX(-50%);
      font-weight: 600;
    }
  }

  .tier-bar {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    background: var(--sider-bg-color);
    border-radius: 6px;
    overflow: visible;
    border: 1px solid rgba(255, 255, 255, 0.03);
  }

  .tier {
    height: 100%;
    border-right: 1px solid rgba(11, 14, 20, 0.4);

    &:last-of-type {
      border-right: none;
    }
  }

  .tier.fail {
    background: #451a1a; // Keeping specific semantic colors for status bars
    border-radius: 6px 0 0 6px;
  }

  .tier.pass {
    background: #453a1a;
  }

  .tier.good {
    background: #1a452a;
  }

  .tier.excellent {
    background: #1a4545;
    border-radius: 0 6px 6px 0;
  }

  .tier-pointer {
    position: absolute;
    top: -4px;
    height: 20px;
    width: 2px;
    background: var(--text-color-primary);
    box-shadow: var(--box-shadow-neon);
    z-index: 10;
    transition: left 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
    transform: translateX(-50%);
  }

  .pointer-tip {
    position: absolute;
    top: -6px;
    left: 50%;
    width: 8px;
    height: 8px;
    background: var(--text-color-primary);
    transform: translateX(-50%) rotate(45deg);
    box-shadow: var(--box-shadow-neon);
  }

  .pointer-box {
    position: absolute;
    bottom: -32px;
    left: 50%;
    transform: translateX(-50%);
    padding: 2px 8px;
    background: var(--border-color-hover);
    color: var(--bg-color);
    font-size: 13px;
    font-weight: 900;
    font-family: 'Space Grotesk', monospace;
    border-radius: 2px;
    white-space: nowrap;
    box-shadow: var(--box-shadow-neon);

    &::after {
      content: '';
      position: absolute;
      top: -4px;
      left: 50%;
      transform: translateX(-50%);
      border-left: 4px solid transparent;
      border-right: 4px solid transparent;
      border-bottom: 4px solid var(--border-color-hover);
    }
  }

  .analysis-detail {
    background: rgba(0, 242, 255, 0.03);
    border-left: 3px solid var(--border-color-hover);
    color: var(--text-color-secondary);
    font-size: 13px;
    padding: 15px;
  }
}

/* 战术表格 */
:deep(.el-table) {
  background: transparent !important;
  --el-table-bg-color: transparent !important;
  --el-table-tr-bg-color: var(--runner-table-row-bg) !important;
  --el-table-header-bg-color: var(--runner-accent-soft-bg) !important;
  --el-table-header-text-color: var(--border-color-hover) !important;
  --el-table-text-color: var(--text-color-primary) !important;
  --el-table-row-hover-bg-color: var(--runner-table-row-hover-bg) !important;
  --el-table-border-color: var(--runner-table-border) !important;
  color: var(--text-color-primary) !important;

  th.el-table__cell {
    color: var(--text-color-secondary) !important;
    border-bottom: 1px solid var(--border-color) !important;
    font-weight: bold;
  }

  td.el-table__cell {
    border-bottom: 1px solid var(--runner-table-border) !important;
    font-size: 13px;
  }

  .el-table__row:hover>td {
    background-color: var(--runner-table-row-hover-bg) !important;
  }
}

/* 底部操作栏已移除 */


/* 按钮渐变样式 */
:deep(.el-button--primary:not(.is-link)) {
  background: linear-gradient(135deg, #a4e6ff 0%, #00d1ff 100%) !important;
  border: none !important;
  color: #050a14 !important;
  font-weight: bold !important;
  box-shadow: 0 0 15px rgba(0, 209, 255, 0.2) !important;

  &:hover {
    opacity: 0.9;
    box-shadow: 0 0 20px rgba(0, 209, 255, 0.4) !important;
  }

  &:disabled {
    background: var(--text-color-placeholder) !important;
    color: var(--text-color-secondary) !important;
    box-shadow: none !important;
  }
}

:deep(.el-button--success:not(.is-link)) {
  background: linear-gradient(135deg, #2ff801 0%, #10b981 100%) !important;
  border: none !important;
  color: #050a14 !important;
}

:deep(.el-drawer) {
  background-color: var(--sider-bg-color) !important;

  .el-drawer__header {
    margin-bottom: 0;
    border-bottom: 1px solid var(--border-color);
    padding: 20px;

    span {
      color: var(--border-color-hover);
      font-weight: bold;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .el-drawer__close-btn {
      color: var(--text-color-secondary);

      &:hover {
        color: var(--text-color-primary);
      }
    }
  }

  .drawer-log-content {
    background: var(--bg-color);
    padding: 24px;
    height: 100%;
    overflow-y: auto;
  }
}

:deep(.el-timeline) {
  .el-timeline-item__node {
    background-color: var(--bg-color);
    border: 2px solid var(--border-color-hover);
    box-shadow: var(--box-shadow-neon);
  }

  .el-timeline-item__tail {
    border-left-color: var(--log-timeline-line);
  }

  .el-timeline-item__timestamp {
    color: var(--log-timestamp-color);
    font-family: 'Space Grotesk', monospace;
    font-size: 11px;
  }

  .log-item-detail {
    background: var(--log-card-bg);
    padding: 15px;
    border-radius: 4px;
    border: 1px solid var(--log-card-border);
    border-left: 3px solid var(--border-color-hover);
    box-shadow: 0 8px 22px rgba(var(--bg-color-rgb), 0.06);

    &:hover {
      background: var(--log-card-hover-bg);
      border-color: rgba(var(--border-color-hover-rgb), 0.28);
    }

    .item-stage {
      color: var(--text-color-primary);
      font-size: 14px;
      font-weight: bold;
      margin-bottom: 5px;
    }

    .item-text {
      color: var(--text-color-secondary);
      font-size: 13px;
      line-height: 1.6;
    }
  }

  .log-content {
    background: var(--log-card-bg);
    padding: 12px 14px;
    border: 1px solid var(--log-card-border);
    border-left: 3px solid var(--border-color-hover);
    border-radius: 4px;
    box-shadow: 0 8px 22px rgba(var(--bg-color-rgb), 0.05);

    strong {
      display: block;
      color: var(--text-color-primary);
      margin-bottom: 4px;
    }

    .log-text {
      color: var(--text-color-secondary);
      line-height: 1.6;
    }
  }
}

/* 结果溯源样式 */
.trace-pane-content {
  padding: 0;
  color: var(--text-color-primary);

  .trace-section {
    margin-bottom: 30px;

    .trace-title {
      font-size: 15px;
      font-weight: 600;
      color: var(--border-color-hover);
      margin-bottom: 15px;
      padding-left: 10px;
      border-left: 4px solid var(--border-color-hover);
    }

    .sub-trace-title {
      font-size: 13px;
      font-weight: 500;
      color: var(--text-color-secondary);
      margin: 15px 0 10px 0;
    }
  }

  .method-card {
    background: rgba(0, 242, 255, 0.03);
    border: 1px solid rgba(0, 242, 255, 0.1);
    border-radius: 8px;
    padding: 20px;

    .method-info {
      display: flex;
      flex-wrap: wrap;
      gap: 20px;
      margin-bottom: 20px;

      .m-item {
        .m-label {
          color: var(--text-color-secondary);
          margin-right: 8px;
          font-size: 13px;
        }

        .m-value {
          color: var(--text-color-primary);
          font-weight: 500;
        }

        code.m-value {
          background: rgba(0, 0, 0, 0.3);
          padding: 2px 6px;
          border-radius: 4px;
          font-family: monospace;
          color: #ff9d00;
        }
      }
    }
  }

  .aggregation-info {
    display: flex;
    align-items: center;
    gap: 10px;
    background: rgba(255, 157, 0, 0.05);
    border: 1px solid rgba(255, 157, 0, 0.1);
    padding: 15px;
    border-radius: 6px;
    color: var(--text-color-secondary);
    font-size: 13px;

    .el-icon {
      font-size: 18px;
      color: #ff9d00;
    }

    strong {
      color: var(--text-color-primary);
    }
  }

  .code-text {
    font-family: monospace;
    color: var(--border-color-hover);
  }

  .method-card-v2 {
    background: rgba(var(--card-bg-color-rgb), 0.72);
    border-radius: 8px;
    border: 1px solid var(--runner-accent-border);
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      border-color: rgba(var(--border-color-hover-rgb), 0.3);
      background: rgba(var(--card-bg-color-rgb), 0.9);
    }

    &.aggregation {
      border-left: 4px solid var(--border-color-hover);
    }

    .m-header {
      padding: 15px 20px;
      background: var(--runner-accent-soft-bg);
      border-bottom: 1px solid var(--runner-table-border);
      display: flex;
      justify-content: space-between;
      align-items: center;

      .m-title-area {
        display: flex;
        align-items: center;
        gap: 12px;

        .m-icon {
          font-size: 18px;
          color: var(--border-color-hover);
        }

        .m-name {
          font-size: 16px;
          font-weight: bold;
          color: var(--text-color-primary);
        }

        .m-code-tag {
          background: var(--runner-accent-soft-bg);
          border-color: var(--runner-accent-border);
          color: var(--border-color-hover);
        }
      }

      .m-meta {
        font-size: 12px;
        color: var(--text-color-placeholder);

        .value {
          color: var(--text-color-secondary);
          margin-left: 5px;
          font-weight: 600;
        }
      }
    }

    .m-body {
      padding: 20px;

      .m-desc-label {
        font-size: 12px;
        color: var(--text-color-secondary);
        margin-bottom: 8px;
        text-transform: uppercase;
        letter-spacing: 1px;
        font-weight: 600;
      }

      .m-description {
        margin: 0;
        font-size: 14px;
        line-height: 1.6;
        color: var(--text-color-primary);
        font-weight: 500;
        white-space: pre-line;
      }
    }

    .m-footer {
      padding: 12px 20px;
      background: rgba(var(--sider-bg-color-rgb), 0.72);
      border-top: 1px solid var(--runner-table-border);
      display: flex;
      justify-content: space-between;
      align-items: center;

      .m-path {
        display: flex;
        align-items: center;
        gap: 8px;
        color: var(--text-color-secondary);
        font-size: 12px;
        font-weight: 500;
        flex: 1;
        overflow: hidden;

        .path-text {
          font-family: monospace;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .m-preview-btn {
        margin-left: 20px;
        background: linear-gradient(135deg, #00f2ff 0%, #00d1ff 100%);
        border: none;
        color: #001a1d;
        font-weight: bold;

        &:hover {
          opacity: 0.9;
          transform: translateY(-1px);
        }
      }
    }
  }

  .params-table-wrap {
    margin-top: 15px;
    padding: 0 20px;

    .sub-trace-title {
      font-size: 12px;
      color: var(--text-color-placeholder);
      margin-bottom: 8px;
    }
  }

  .total-score-hint {
    margin-top: 15px;
    padding: 12px 20px;
    background: rgba(0, 242, 255, 0.05);
    border-radius: 4px;
    border-left: 4px solid var(--border-color-hover);
    display: flex;
    align-items: center;
    gap: 10px;

    .label {
      font-size: 14px;
      color: var(--text-color-secondary);
    }

    .value {
      font-size: 20px;
      font-weight: bold;
      font-family: 'Space Grotesk', monospace;

      &.is-excellent {
        color: #00f2ff;
      }

      &.is-good {
        color: #00d1ff;
      }

      &.is-pass {
        color: #ffd700;
      }

      &.is-fail {
        color: #ff4d4f;
      }
    }

    .formula {
      font-size: 12px;
      color: var(--text-color-placeholder);
      font-style: italic;
    }
  }

  .trace-table {
    --el-table-border-color: var(--runner-table-border);
    --el-table-header-bg-color: var(--runner-accent-soft-bg);
  }

  .no-params-hint {
    color: var(--text-color-placeholder);
    font-style: italic;
    font-size: 12px;
    text-align: center;
    padding: 10px;
  }

  /* 源码预览样式 */
  .algo-preview-container {
    max-height: 500px;
    overflow-y: auto;
    background: #0d1117;
    padding: 20px;
    border-radius: 6px;
    border: 1px solid var(--border-color);

    .code-block {
      margin: 0;
      color: #e6edf3;
      font-family: 'Fira Code', 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.5;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }

  /* 计算链条样式 */
  .trace-flow-canvas {
    position: relative;
    min-height: 260px;
    overflow: hidden;
    background:
      linear-gradient(180deg, rgba(var(--card-bg-color-rgb), 0.76), rgba(var(--sider-bg-color-rgb), 0.36)),
      linear-gradient(90deg, rgba(var(--border-color-rgb), 0.16) 1px, transparent 1px) 0 0/40px 40px,
      linear-gradient(180deg, rgba(var(--border-color-rgb), 0.14) 1px, transparent 1px) 0 0/40px 40px;
    border: 1px solid var(--trace-card-border);
    border-radius: 10px;
    padding: 0;
    box-shadow: inset 0 1px 0 rgba(var(--border-color-rgb), 0.18);
    cursor: grab;
    user-select: none;
    touch-action: none;

    &.is-panning {
      cursor: grabbing;
    }

    &.is-transforming {
      .trace-flow-content {
        will-change: transform;
      }

      .step-box {
        box-shadow: none;
      }
    }
  }

  .trace-flow-content {
    position: absolute;
    top: 0;
    left: 0;
    transform-origin: 0 0;
    backface-visibility: hidden;
  }

  .trace-flow-lines {
    position: absolute;
    top: 0;
    left: 0;
    pointer-events: none;
    overflow: visible;

    marker path {
      fill: var(--border-color-hover);
    }
  }

  .trace-flow-edge {
    fill: none;
    stroke: var(--border-color-hover);
    stroke-width: 2.2;
    stroke-linecap: round;
    marker-end: url(#trace-flow-arrow);
    opacity: 0.78;
    filter: drop-shadow(0 1px 2px rgba(var(--bg-color-rgb), 0.28));
  }

  .trace-flow-node {
    position: absolute;
    z-index: 2;

    .step-box {
      width: 100%;
      height: 100%;
      display: flex;
      gap: 12px;
      background: rgba(var(--card-bg-color-rgb), 0.96);
      border: 1px solid var(--trace-card-border);
      padding: 14px 16px;
      border-radius: 8px;
      position: relative;
      overflow: hidden;
      box-shadow: 0 10px 24px rgba(var(--bg-color-rgb), 0.08);
      transition: all 0.2s ease;

      &:hover {
        background: rgba(var(--card-bg-color-rgb), 1);
        border-color: rgba(var(--border-color-hover-rgb), 0.34);
        transform: translateY(-1px);
      }

      &.start {
        border-left: 4px solid #10b981;
      }

      &.algo {
        border-left: 4px solid #00d1ff;
      }

      &.result {
        border-left: 4px solid var(--warning-color);
      }
    }

    .step-icon {
      width: 36px;
      height: 36px;
      flex: 0 0 36px;
      background: rgba(var(--primary-color-rgb), 0.08);
      border: 1px solid rgba(var(--border-color-rgb), 0.72);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      color: var(--border-color-hover);
    }

    .step-main {
      min-width: 0;
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .step-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .step-type {
        font-size: 12px;
        font-weight: 700;
        color: var(--text-color-secondary);
      }
    }

    .step-body {
      font-size: 13px;
      color: var(--text-color-primary);
      min-height: 0;
      overflow: hidden;

      code {
        background: var(--trace-code-bg);
        border: 1px solid var(--trace-code-border);
        padding: 1px 5px;
        border-radius: 4px;
        font-family: monospace;
        color: var(--trace-code-text);
        font-weight: 600;
      }
    }

    .node-title,
    .algo-name,
    .result-label {
      font-weight: 700;
      color: var(--text-color-primary);
      margin-bottom: 8px;
      line-height: 1.35;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .source-meta-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 5px;
      color: var(--text-color-secondary);
      font-size: 12px;
      line-height: 1.45;

      span {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .field-chip-list,
    .param-tag-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 8px;
      max-height: 54px;
      overflow: hidden;

      :deep(.el-tag) {
        max-width: 100%;
      }

      :deep(.el-tag__content) {
        max-width: 220px;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .source-desc,
    .algo-url {
      margin-top: 7px;
      color: var(--text-color-secondary);
      font-size: 12px;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .inline-preview-btn {
      margin-top: 6px;
      padding: 0;
      font-size: 12px;
      font-weight: 600;
    }
  }

  .preprocess-compare-panel {
    background: var(--pre-chart-panel-bg);
    border: 1px solid var(--pre-chart-border);
    border-radius: 6px;
    padding: 14px;
    box-shadow: 0 1px 2px rgba(var(--bg-color-rgb), 0.04);
  }

  .preprocess-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 14px;
    margin-bottom: 12px;

    :deep(.el-radio-button__inner) {
      padding: 6px 12px;
      font-size: 12px;
    }
  }

  .preprocess-toolbar-title {
    color: var(--pre-chart-title);
    font-size: 14px;
    font-weight: 800;
    letter-spacing: 0;
  }

  .preprocess-card-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .preprocess-field-card {
    min-width: 0;
    padding: 12px 12px 14px;
    background: var(--pre-chart-card-bg);
    border: 1px solid var(--pre-chart-border);
    border-radius: 6px;
  }

  .field-card-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 10px;

    .field-name {
      color: var(--pre-chart-title);
      font-size: 13px;
      font-weight: 800;
      line-height: 1.35;
    }

    .field-source {
      margin-top: 3px;
      color: var(--pre-chart-subtitle);
      font-size: 12px;
      line-height: 1.35;
    }
  }

  .field-card-actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 6px;
    max-width: 42%;

    :deep(.el-tag) {
      max-width: 100%;
    }

    :deep(.el-tag__content) {
      max-width: 220px;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .full-data-btn {
    padding: 0;
    font-size: 12px;
    font-weight: 700;
  }

  .curve-compare {
    display: grid;
    gap: 6px;
    padding: 6px 8px 8px;
    border-radius: 4px;
    border: 1px solid var(--pre-chart-border);
    background: var(--pre-chart-plot-bg);
  }

  .curve-legend {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 16px;
    padding-right: 2px;
  }

  .legend-note {
    margin-right: auto;
    color: var(--pre-chart-subtitle);
    font-size: 12px;
    font-weight: 600;
  }

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--pre-chart-label);
    font-size: 13px;
    font-weight: 700;

    &::before {
      content: '';
      width: 18px;
      height: 2px;
      border-radius: 0;
    }

    &.before {
      color: var(--pre-chart-before);

      &::before {
        background: var(--pre-chart-before);
      }
    }

    &.after {
      color: var(--pre-chart-after);

      &::before {
        background: var(--pre-chart-after);
      }
    }
  }

  .curve-chart {
    width: 100%;
    height: clamp(240px, 26vw, 300px);
    min-height: 240px;
    border-radius: 3px;
    overflow: hidden;
    cursor: zoom-in;
    user-select: none;
    touch-action: none;
    background: var(--pre-chart-highlight), var(--pre-chart-plot-bg);

    &.is-panning {
      cursor: grabbing;
    }
  }

  .axis-curve {
    width: 100%;
    height: 100%;
    overflow: visible;

    .curve-grid line {
      stroke: var(--pre-chart-grid);
      stroke-width: 1;
      stroke-dasharray: 2 5;
      opacity: 1;
    }

    .curve-axis line {
      stroke: var(--pre-chart-axis);
      stroke-width: 1;
      stroke-linecap: square;
    }

    .curve-labels text {
      fill: var(--pre-chart-subtitle);
      font-size: 12px;
      font-family: "DIN Alternate", "Roboto Mono", "Consolas", monospace;
      pointer-events: none;
    }

    .y-labels text {
      text-anchor: end;
    }

    .x-labels text {
      text-anchor: middle;
    }

    .zoomable-labels text {
      vector-effect: non-scaling-stroke;
      /* 缩放时保持文字大小不变 */
      transform-box: fill-box;
      font-size: 11px;
    }

    .area-before,
    .area-after {
      pointer-events: none;
    }

    .line-before {
      stroke: var(--pre-chart-before);
      stroke-width: 1.8;
      fill: none;
      stroke-linecap: square;
      stroke-linejoin: round;
      vector-effect: non-scaling-stroke;
    }

    .line-after {
      stroke: var(--pre-chart-after);
      stroke-width: 1.8;
      fill: none;
      stroke-linecap: square;
      stroke-linejoin: round;
      vector-effect: non-scaling-stroke;
    }

    .chart-node {
      ellipse {
        stroke-width: 1.6;
        fill: var(--pre-chart-node-fill);
        cursor: pointer;
        vector-effect: non-scaling-stroke;
        transition: stroke-width 0.16s ease;
      }

      &.before ellipse {
        stroke: var(--pre-chart-before);
      }

      &.after ellipse {
        stroke: var(--pre-chart-after);
      }

      &:hover ellipse {
        stroke-width: 2.4;
      }
    }

    &.compact {
      border-radius: 3px;
    }
  }

  .split-chart-grid {
    display: grid;
    gap: 8px;
  }

  .split-chart {
    min-width: 0;
    padding: 8px 10px 6px;
    border-radius: 4px;
    border: 1px solid var(--pre-chart-border);
    background: var(--pre-chart-plot-bg);
    overflow: hidden;
    cursor: zoom-in;
    user-select: none;
    touch-action: none;

    &.is-panning {
      cursor: grabbing;
    }

    &.before {
      border-left: 3px solid var(--pre-chart-before);
    }

    &.after {
      border-left: 3px solid var(--pre-chart-after);
    }
  }

  .split-chart-title {
    color: var(--pre-chart-title);
    font-size: 12px;
    font-weight: 800;
    margin-bottom: 4px;
  }

  .flow-chain {
    padding-left: 20px;

    .flow-step-item {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
    }

    .step-connector {
      margin: 10px 0 10px 25px;
      color: var(--border-color-hover);
      opacity: 0.5;
      font-size: 20px;
    }

    .step-box {
      display: flex;
      gap: 15px;
      background: var(--trace-panel-bg);
      border: 1px solid var(--trace-card-border);
      padding: 15px 20px;
      border-radius: 8px;
      min-width: 400px;
      position: relative;
      transition: all 0.3s ease;

      &:hover {
        background: var(--runner-accent-soft-bg);
        border-color: rgba(var(--border-color-hover-rgb), 0.32);
      }

      &.start {
        border-left: 4px solid #10b981;
      }

      &.algo {
        border-left: 4px solid #00d1ff;
      }

      &.result {
        border-left: 4px solid var(--warning-color);
        background: rgba(var(--warning-color-rgb), 0.08);
      }

      .step-icon {
        width: 40px;
        height: 40px;
        background: rgba(var(--primary-color-rgb), 0.08);
        border: 1px solid rgba(var(--border-color-rgb), 0.72);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        color: var(--border-color-hover);
      }

      .step-main {
        flex: 1;

        .step-head {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .step-type {
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
            color: var(--text-color-secondary);
          }

          .step-id {
            font-family: monospace;
            font-size: 11px;
            color: var(--text-color-secondary);
            opacity: 0.9;
          }
        }

        .step-body {
          font-size: 13px;
          color: var(--text-color-primary);

          code {
            background: var(--trace-code-bg);
            border: 1px solid var(--trace-code-border);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: monospace;
            color: var(--trace-code-text);
            font-weight: 600;
          }

          strong {
            color: var(--border-color-hover);
          }

          .algo-url {
            font-size: 11px;
            margin-top: 5px;
            opacity: 0.6;
          }

          .param-tag-list {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-top: 10px;
          }
        }
      }
    }
  }
}

.log-status-header {
  background: var(--log-header-bg) !important;
  border: 1px solid var(--log-card-border) !important;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 30px;

  .task-id {
    color: var(--border-color-hover);
    font-family: monospace;
  }
}

.report-preview-body {
  height: 70vh;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.report-preview-frame {
  width: 100%;
  height: 100%;
  border: 0;
  background: #f5f7fa;
}

/* 方向四: 报告进度条 */
.report-progress-bar {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
}
.report-progress-text {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
}

/* 方向四: HTML 预览弹窗 */
.report-html-preview-body {
  height: 75vh;
  display: flex;
  flex-direction: column;
}
.report-html-preview-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  flex-shrink: 0;
}

/* 源码预览弹窗：减少全局半透明主题穿透，并复用算法管理的代码阅读样式 */
:deep(.algo-preview-dialog.el-dialog) {
  background: #1e1e1e !important;
  border: 1px solid #2d2d2d !important;
}

:deep(.algo-preview-dialog .el-dialog__header) {
  background: #252526 !important;
  border-bottom: 1px solid #2d2d2d !important;
}

:deep(.algo-preview-dialog .el-dialog__title) {
  color: #d4d4d4 !important;
}

:deep(.algo-preview-dialog .el-dialog__body) {
  background: #1e1e1e !important;
  padding: 0 !important;
}

:deep(.algo-preview-dialog .el-dialog__footer) {
  background: #252526 !important;
  border-top: 1px solid #2d2d2d !important;
}

.algo-preview-container {
  max-height: 70vh;
  overflow: auto;
  background: #1e1e1e;
}

.algo-preview-pre {
  margin: 0;
  background: #1e1e1e;
  border-radius: 0;

  :deep(code.algo-preview-code) {
    display: block;
    padding: 16px 18px;
    font-family: "JetBrains Mono", "Cascadia Code", "Source Code Pro", Consolas, "Courier New", monospace;
    font-size: 13px;
    line-height: 1.6;
    color: #d4d4d4;
    background: transparent;
    white-space: pre;
    tab-size: 4;

    .tk-keyword {
      color: #c586c0;
    }

    .tk-builtin {
      color: #4ec9b0;
    }

    .tk-string {
      color: #ce9178;
    }

    .tk-comment {
      color: #6a9955;
      font-style: italic;
    }

    .tk-number {
      color: #b5cea8;
    }

    .tk-function {
      color: #dcdcaa;
    }

    .tk-class {
      color: #4ec9b0;
    }

    .tk-decorator {
      color: #dcdcaa;
    }

    .tk-preprocessor {
      color: #9cdcfe;
    }
  }
}

/* 全局表格战术覆盖 */
:deep(.el-table) {
  background-color: transparent !important;
  --el-table-border-color: var(--runner-table-border);

  tr {
    background-color: var(--runner-table-row-bg) !important;
  }

  .el-table__header-wrapper th {
    background-color: var(--runner-accent-soft-bg) !important;
    color: var(--border-color-hover) !important;
    border-bottom: 1px solid var(--border-color) !important;
  }

  .el-table__row {
    background-color: var(--runner-table-row-bg) !important;

    &:hover>td {
      background-color: var(--runner-table-row-hover-bg) !important;
    }
  }

  td.el-table__cell {
    border-bottom: 1px solid var(--runner-table-border) !important;
  }
}

.image-upload-wrapper {
  padding: 5px 0;

  :deep(.el-upload--picture-card) {
    width: 80px;
    height: 80px;
    line-height: 80px;
  }

  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 80px;
    height: 80px;
  }

  :deep(.el-upload-list--picture-card .el-upload-list__item-actions) {
    font-size: 16px;
  }
}

.non-numeric-compare {
  margin-top: 10px;

  .compare-table-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .compare-col {
    .col-title {
      font-size: 13px;
      font-weight: bold;
      margin-bottom: 8px;
      color: var(--el-text-color-primary);
      display: flex;
      align-items: center;

      &::before {
        content: '';
        width: 3px;
        height: 12px;
        background: var(--el-color-primary);
        margin-right: 6px;
        border-radius: 2px;
      }
    }
  }

  .table-hint {
    margin-top: 10px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    text-align: center;
  }
}

:deep(.preprocess-data-dialog.el-dialog) {
  background: var(--bg-color);
}

:deep(.preprocess-data-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(var(--border-color-rgb), 0.72);
  margin-right: 0;
  padding: 16px 22px;
}

:deep(.preprocess-data-dialog .el-dialog__body) {
  padding: 18px 22px 22px;
  background: var(--bg-color);
}

.preprocess-data-detail {
  min-height: calc(100vh - 90px);
  color: var(--text-color-primary);
}

.detail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: var(--text-color-primary);
  font-size: 14px;
  font-weight: 700;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.detail-search {
  width: min(360px, 42vw);
}

.detail-table-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.detail-table-panel {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--pre-chart-border);
  border-radius: 6px;
  background: var(--pre-chart-card-bg);

  :deep(.el-pagination) {
    justify-content: flex-end;
    margin-top: 12px;
  }
}

.detail-table-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: var(--pre-chart-title);
  font-size: 13px;
  font-weight: 800;

  strong {
    color: var(--border-color-hover);
    font-size: 12px;
  }
}

@media (max-width: 900px) {

  .detail-toolbar,
  .detail-table-grid {
    grid-template-columns: 1fr;
  }

  .detail-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-search {
    width: 100%;
  }
}
</style>
