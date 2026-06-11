<template>
  <div class="app-container criterion-designer-container" :class="themeStore.currentThemeName">
    <!-- 头部栏 -->
    <div class="designer-header mb16">
      <div class="header-left">
        <el-button icon="Back" circle @click="handleBack" />
        <div class="title-area ml12">
          <h2 class="page-title">设计评估准则</h2>
          <div class="page-desc set-switcher-row">
            <span>当前准则集：<span class="highlight-text">{{ currentCriterionSetName }}</span></span>
            <span class="set-switcher-divider">|</span>
            <span>关联指标体系：<span class="highlight-text">{{ systemName || '加载中...' }}</span></span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button type="success" icon="Check" :loading="saveLoading" @click="handleBatchSave">保存准则集配置</el-button>
        <el-button icon="Close" @click="handleBack">取消</el-button>
      </div>
    </div>

    <!-- 主布局 -->
    <div class="designer-main">
      <!-- 左侧：指标树 -->
      <aside class="tree-panel-wrapper">
        <el-card class="box-card full-height-card" shadow="never">
          <template #header>
            <div class="card-header-inner">
              <span class="card-title">指标结构树</span>
              <el-tag size="small" type="info">共 {{ nodeCount }} 个节点</el-tag>
            </div>
          </template>
          <div class="tree-search-wrapper mb12">
            <el-input
              v-model="filterText"
              placeholder="输入关键字过滤指标..."
              clearable
              prefix-icon="Search"
            />
          </div>
          <el-scrollbar class="tree-scrollbar">
            <el-tree
              ref="treeRef"
              :data="treeData"
              node-key="uid"
              :props="treeProps"
              highlight-current
              default-expand-all
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              @current-change="handleNodeClick"
              class="custom-indicator-tree"
            >
              <template #default="{ node, data }">
                <div class="custom-tree-node" :class="{ 'is-leaf-node': !data.children || !data.children.length }">
                  <span class="node-icon">
                    <el-icon v-if="!data.children || !data.children.length"><Document /></el-icon>
                    <el-icon v-else><FolderOpened /></el-icon>
                  </span>
                  <span class="node-label">{{ data.indicatorName || data.label }}</span>
                  <span class="node-tags-area">
                    <el-tag v-if="hasCriterion(data.uid)" size="small" type="success" effect="light" class="ml4 node-tag">
                      已设
                    </el-tag>
                  </span>
                </div>
              </template>
            </el-tree>
          </el-scrollbar>
        </el-card>
      </aside>

      <!-- 右侧：属性表单 -->
      <section class="form-panel-wrapper">
        <el-card class="box-card full-height-card scrollable-card" shadow="never">
          <template #header>
            <div class="card-header-inner">
              <span class="card-title">评估准则设计面板</span>
              <el-tag v-if="activeNode" :type="activeNode.children?.length ? 'warning' : 'primary'" size="small">
                {{ activeNode.children?.length ? '中间节点' : '叶子节点' }}
              </el-tag>
            </div>
          </template>

          <div v-if="!activeNode" class="empty-state">
            <el-empty description="请在左侧选择需要配置评估准则的指标节点" />
          </div>

          <div v-else class="form-content">
            <!-- 指标基础信息展示 -->
            <div class="info-banner mb16">
              <div class="banner-item">
                <span class="label">当前指标：</span>
                <span class="value font-semibold">{{ activeNode.indicatorName || activeNode.label }}</span>
              </div>
              <div class="banner-item">
                <span class="label">层级：</span>
                <span class="value">{{ activeNode.level }} 级</span>
              </div>
              <div class="banner-item">
                <span class="label">层级类型：</span>
                <span class="value">{{ activeNode.children?.length ? '聚合节点' : '底层计算项' }}</span>
              </div>
            </div>

            <el-form :model="ruleForm" label-position="top" class="criterion-form">
              <!-- 第一板块：同源参数极值设置 -->
              <div class="form-section">
                <h3 class="section-title">1. 指标值与极值同源设置</h3>
                <el-row :gutter="20">
                  <el-col :span="8">
                    <el-form-item label="指标量化方向 (valueCategory)">
                      <el-select v-model="ruleForm.valueCategory" placeholder="请选择极值量化方向" style="width: 100%">
                        <el-option
                          v-for="item in VALUE_CATEGORY_OPTIONS"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="物理最佳值 (bestValue)">
                      <el-input-number
                        v-model="ruleForm.bestValue"
                        :precision="4"
                        placeholder="量化评分注入最佳值"
                        style="width: 100%"
                        controls-position="right"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="物理最差值 (worstValue)">
                      <el-input-number
                        v-model="ruleForm.worstValue"
                        :precision="4"
                        placeholder="量化评分注入最差值"
                        style="width: 100%"
                        controls-position="right"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <div class="form-tip">
                  提示：最佳值与最差值直接注入叶子节点的指标量化算法配置中（同源SSOT控制），实现量化模型与评分界限统一。
                </div>
              </div>

              <!-- 第二板块：一票否决控制 -->
              <div class="form-section mt16">
                <h3 class="section-title">2. 强制约束（一票否决控制）</h3>
                <el-row :gutter="20" align="middle">
                  <el-col :span="6">
                    <el-form-item label="是否属于一票否决指标">
                      <el-switch
                        v-model="ruleForm.isMandatory"
                        :active-value="1"
                        :inactive-value="0"
                        active-text="是"
                        inactive-text="否"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="18" v-if="ruleForm.isMandatory === 1">
                    <el-form-item label="触发否决后，对所属分支的惩罚动作">
                      <el-radio-group v-model="ruleForm.vetoAction">
                        <el-radio value="DIRECT_ZERO">分数抹零惩罚 (DIRECT_ZERO)</el-radio>
                        <el-radio value="DIRECT_FAIL">等级判为限制等级 (DIRECT_FAIL)</el-radio>
                        <el-radio value="DOWNGRADE">扣减分数并限制等级 (DOWNGRADE)</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20" align="middle" v-if="ruleForm.isMandatory === 1" class="mt8">
                  <!-- DOWNGRADE 对应的扣分输入 -->
                  <el-col :span="12" v-if="ruleForm.vetoAction === 'DOWNGRADE'">
                    <el-form-item label="扣减分值 (分)" required>
                      <el-input-number
                        v-model="ruleForm.vetoScoreDeduct"
                        :min="0"
                        :max="100"
                        placeholder="请输入扣分"
                        style="width: 100%"
                        controls-position="right"
                      />
                    </el-form-item>
                  </el-col>
                  
                  <!-- DIRECT_FAIL 或 DOWNGRADE 对应的限制最高等级下拉框 -->
                  <el-col :span="12" v-if="ruleForm.vetoAction === 'DIRECT_FAIL' || ruleForm.vetoAction === 'DOWNGRADE'">
                    <el-form-item label="最高限制等级" required>
                      <el-select v-model="ruleForm.vetoLevelLimit" placeholder="请选择限制最高等级" style="width: 100%">
                        <el-option
                          v-for="lvl in vetoLevelOptions"
                          :key="lvl"
                          :label="lvl"
                          :value="lvl"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <div class="form-tip red-tip" v-if="ruleForm.isMandatory === 1">
                  注意：一票否决动作将直接施加在所属的第一级子系统节点（分支子项）上。
                </div>
              </div>

              <!-- 第三板块：规则配置 -->
              <div class="form-section mt16">
                <div class="section-header">
                  <h3 class="section-title">3. 评估准则规则设计</h3>
                  <el-button
                    v-if="hasCriterion(activeNode.uid)"
                    type="danger"
                    plain
                    size="small"
                    icon="Delete"
                    @click="handleRemoveCriterion"
                  >
                    清除当前准则
                  </el-button>
                </div>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="规则类型 (ruleType)" required>
                      <el-select v-model="ruleForm.ruleType" placeholder="请选择规则类型" style="width: 100%" popper-class="criterion-select-popper" @change="handleRuleTypeChange">
                        <el-option-group
                          v-for="group in RULE_TYPE_GROUPS"
                          :key="group.label"
                          :label="group.label"
                        >
                          <el-option
                            v-for="item in group.options"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                          />
                        </el-option-group>
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <!-- 规则表单交互区域 -->
                <div class="rule-detail-card mt12">
                  <!-- 2. LEVEL_MAP 等级映射 -->
                  <div v-if="ruleForm.ruleType === 'LEVEL_MAP'" class="rule-box">
                    <div class="rule-box-header mb12">
                      <h4 class="rule-title">多级区间定级配置</h4>
                      <el-button type="primary" size="small" plain icon="Plus" @click="addLevelTier">添加定级区间</el-button>
                    </div>
                    <el-table :data="levelMapForm.tiers" border size="small" style="width: 100%">
                      <el-table-column label="等级名称 (name)" min-width="100">
                        <template #default="scope">
                          <el-input v-model="scope.row.name" placeholder="优秀/良好/合格..." size="small" />
                        </template>
                      </el-table-column>
                      <el-table-column label="评分下限 (>= min)" min-width="110">
                        <template #default="scope">
                          <el-input-number
                            v-model="scope.row.valueMin"
                            :min="0"
                            :max="100"
                            size="small"
                            controls-position="right"
                            style="width: 100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column label="评分上限 (<= max)" min-width="110">
                        <template #default="scope">
                          <el-input-number
                            v-model="scope.row.valueMax"
                            :min="0"
                            :max="100"
                            size="small"
                            controls-position="right"
                            style="width: 100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column label="单项结论文案 (conclusion)" min-width="180">
                        <template #default="scope">
                          <el-input v-model="scope.row.conclusion" placeholder="选填，定制文本" size="small" />
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="70" align="center">
                        <template #default="scope">
                          <el-button
                            type="danger"
                            icon="Delete"
                            size="small"
                            circle
                            @click="removeLevelTier(scope.$index)"
                          />
                        </template>
                      </el-table-column>
                    </el-table>
                    <div class="form-tip">
                      提示：区间判断包含下限和上限。按表格由上至下匹配，第一个命中的区间为生效等级。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与使用指南</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：多级区间定级将底层的评分数值 <code>[0 ~ 100]</code> 划分为若干个连续区间，映射为离散的评估等级。</p>
                        <p><strong>匹配逻辑</strong>：系统按表格<strong>从上至下</strong>的顺序依次判断。对于输入的指标得分 <code>Score</code>，若满足 <code>valueMin &le; Score &le; valueMax</code>，则立即判定为该等级并结束匹配。</p>
                        <p><strong>使用建议</strong>：请确保各区间的范围能够完全覆盖 <code>[0, 100]</code> 区间，避免出现未命中任何区间的情况。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 3. SHORT_BOARD_COMP 短板均值补偿 -->
                  <div v-else-if="ruleForm.ruleType === 'SHORT_BOARD_COMP'" class="rule-box">
                    <h4 class="rule-title">短板均值补偿配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="罚分系数 (lambda)" required>
                          <el-input-number
                            v-model="shortBoardForm.lambda"
                            :precision="2"
                            :step="0.1"
                            :min="0"
                            :max="5"
                            placeholder="如 0.5"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：短板均值补偿用于聚合节点评分，防止子项评分的严重缺陷被高分均值所掩盖。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：若下属子节点的最低得分 <code>S<sub>min</sub></code> 低于 60 分合格线，系统会对当前聚合指标的分值进行惩罚性扣减。</p>
                        <p><strong>扣分公式</strong>：设最低得分为 <code>S<sub>min</sub></code>，罚分系数为 <code>&lambda; (lambda)</code>：</p>
                        <div class="intro-formula">
                          扣减分数 = &lambda; &times; (60 - S<sub>min</sub>)
                        </div>
                        <p><strong>使用建议</strong>：罚分系数 &lambda; 设为 0.5 ~ 1.0 时较温和，设为 1.0 以上时属于重度惩罚。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 4. CONDITION 条件判定 -->
                  <div v-else-if="ruleForm.ruleType === 'CONDITION'" class="rule-box">
                    <div class="rule-box-header mb12">
                      <h4 class="rule-title">条件判定规则配置</h4>
                      <el-button type="primary" size="small" plain icon="Plus" @click="addConditionRow">添加条件</el-button>
                    </div>
                    
                    <!-- 条件逻辑关系 -->
                    <el-form-item label="条件逻辑关系" style="margin-bottom: 12px;">
                      <el-radio-group v-model="conditionForm.relation">
                        <el-radio value="AND">满足以下所有条件 (AND)</el-radio>
                        <el-radio value="OR">满足以下任意条件 (OR)</el-radio>
                      </el-radio-group>
                    </el-form-item>

                    <!-- 条件行列表 -->
                    <el-table :data="conditionForm.conditions" border size="small" style="width: 100%">
                      <el-table-column label="判定参数" min-width="120">
                        <template #default="scope">
                          <el-select v-model="scope.row.field" size="small" placeholder="请选择">
                            <el-option label="指标物理值 (value)" value="value" />
                            <el-option label="指标计算得分 (score)" value="score" />
                          </el-select>
                        </template>
                      </el-table-column>
                      <el-table-column label="运算符" min-width="100">
                        <template #default="scope">
                          <el-select v-model="scope.row.operator" size="small" placeholder="请选择">
                            <el-option label="大于 (>)" value=">" />
                            <el-option label="大于等于 (>=)" value=">=" />
                            <el-option label="小于 (<)" value="<" />
                            <el-option label="小于等于 (<=)" value="<=" />
                            <el-option label="等于 (==)" value="==" />
                            <el-option label="不等于 (!=)" value="!=" />
                          </el-select>
                        </template>
                      </el-table-column>
                      <el-table-column label="阈值" min-width="120">
                        <template #default="scope">
                          <el-input-number
                            v-model="scope.row.threshold"
                            size="small"
                            :precision="4"
                            controls-position="right"
                            style="width: 100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="70" align="center">
                        <template #default="scope">
                          <el-button
                            type="danger"
                            icon="Delete"
                            size="small"
                            circle
                            @click="removeConditionRow(scope.$index)"
                          />
                        </template>
                      </el-table-column>
                    </el-table>

                    <!-- 触发干预动作 -->
                    <div style="margin-top: 16px; border-top: 1px dashed var(--el-border-color-lighter); padding-top: 16px;">
                      <h4 class="rule-title mb12">触发干预动作</h4>
                      <el-row :gutter="20">
                        <el-col :span="12">
                          <el-form-item label="干预类型" required>
                            <el-select v-model="conditionForm.action.type" placeholder="请选择干预动作" style="width: 100%">
                              <el-option label="重置指标得分 (SET_SCORE)" value="SET_SCORE" />
                              <el-option label="扣减指标得分 (DEDUCT_SCORE)" value="DEDUCT_SCORE" />
                              <el-option label="设定指标等级 (SET_GRADE)" value="SET_GRADE" />
                            </el-select>
                          </el-form-item>
                        </el-col>
                        <el-col :span="12">
                          <!-- 重置得分 / 扣减得分 -->
                          <el-form-item
                            v-if="conditionForm.action.type === 'SET_SCORE' || conditionForm.action.type === 'DEDUCT_SCORE'"
                            label="设定的分值 (分)"
                            required
                          >
                            <el-input-number
                              v-model="conditionForm.action.value"
                              :min="0"
                              :max="100"
                              style="width: 100%"
                              controls-position="right"
                            />
                          </el-form-item>
                          
                          <!-- 限制等级 -->
                          <el-form-item
                            v-else-if="conditionForm.action.type === 'SET_GRADE'"
                            label="设定的等级"
                            required
                          >
                            <el-select v-model="conditionForm.action.value" placeholder="请选择限制等级" style="width: 100%">
                              <el-option
                                v-for="lvl in vetoLevelOptions"
                                :key="lvl"
                                :label="lvl"
                                :value="lvl"
                              />
                            </el-select>
                          </el-form-item>
                        </el-col>
                      </el-row>
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与使用指南</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：条件判定是多参数组合的逻辑判断规则。支持使用逻辑关系 <code>AND</code> (所有条件均满足) 或 <code>OR</code> (任意条件满足) 组合判定行。</p>
                        <p><strong>干预动作</strong>：当满足条件时，将触发特定的干预：</p>
                        <ul>
                          <li><code>重置指标得分 (SET_SCORE)</code>：指标本级计算分数直接替换为设定值。</li>
                          <li><code>扣减指标得分 (DEDUCT_SCORE)</code>：在计算分值的基础上扣减指定分数。</li>
                          <li><code>设定指标等级 (SET_GRADE)</code>：强行将评定等级指定为对应的目标等级。</li>
                        </ul>
                      </div>
                    </div>
                  </div>

                  <!-- 5. TREND_STABILITY 稳定性分析 -->
                  <div v-else-if="ruleForm.ruleType === 'TREND_STABILITY'" class="rule-box">
                    <h4 class="rule-title">稳定性分析配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="8">
                        <el-form-item label="调节时间上限 (秒, settlingTimeMax)" required>
                          <el-input-number
                            v-model="trendStabilityForm.settlingTimeMax"
                            :precision="2"
                            :step="0.5"
                            :min="0.1"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="超调量上限 (%, overshootMax)" required>
                          <el-input-number
                            v-model="trendStabilityForm.overshootMax"
                            :precision="2"
                            :step="1"
                            :min="0"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="目标带范围 (%, targetRangePercent)" required>
                          <el-input-number
                            v-model="trendStabilityForm.targetRangePercent"
                            :precision="2"
                            :step="0.5"
                            :min="0"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：系统在动态响应过程中，超调量和调节时间是反映控制稳定性的核心指标。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：稳定性分析旨在评估时序信号在受扰或阶跃响应后的动态控制品质。主要提取三个核心特征进行超限判定：</p>
                        <ul>
                          <li><strong>超调量 (&sigma;%)</strong>：最大响应值偏离稳态值的百分比。</li>
                          <div class="intro-formula">
                            &sigma;% = [ (y<sub>max</sub> - y<sub>ss</sub>) / y<sub>ss</sub> ] &times; 100%
                          </div>
                          <li><strong>调节时间 (t<sub>s</sub>)</strong>：响应曲线进入并保持在稳态值周围误差带（由目标范围百分比决定，通常为 &plusmn;2% 或 &plusmn;5%）内所需的最小时间。</li>
                        </ul>
                        <p><strong>使用建议</strong>：若系统实测值超过设定的上限，则判定指标不达标。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 6. DYNAMIC_ENVELOPE 动态包络 -->
                  <div v-else-if="ruleForm.ruleType === 'DYNAMIC_ENVELOPE'" class="rule-box">
                    <div class="rule-box-header mb12">
                      <h4 class="rule-title">动态包络工况边界配置</h4>
                      <el-button type="primary" size="small" plain icon="Plus" @click="addEnvelopeRow">添加插值限制点</el-button>
                    </div>
                    <el-row :gutter="20" class="mb12">
                      <el-col :span="12">
                        <el-form-item label="自变量 X 物理量名称" required>
                          <el-input v-model="dynamicEnvelopeForm.xVariable" placeholder="例如 height" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="自变量 Y 物理量名称" required>
                          <el-input v-model="dynamicEnvelopeForm.yVariable" placeholder="例如 velocity" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    
                    <el-table :data="dynamicEnvelopeForm.lookupTable" border size="small" style="width: 100%">
                      <el-table-column label="自变量 X 值" min-width="120">
                        <template #default="scope">
                          <el-input-number
                            v-model="scope.row.x"
                            size="small"
                            :precision="2"
                            controls-position="right"
                            style="width: 100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column label="自变量 Y 值" min-width="120">
                        <template #default="scope">
                          <el-input-number
                            v-model="scope.row.y"
                            size="small"
                            :precision="2"
                            controls-position="right"
                            style="width: 100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column label="限制阈值 (limit)" min-width="120">
                        <template #default="scope">
                          <el-input-number
                            v-model="scope.row.limit"
                            size="small"
                            :precision="2"
                            controls-position="right"
                            style="width: 100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="70" align="center">
                        <template #default="scope">
                          <el-button
                            type="danger"
                            icon="Delete"
                            size="small"
                            circle
                            @click="removeEnvelopeRow(scope.$index)"
                          />
                        </template>
                      </el-table-column>
                    </el-table>
                    <div class="form-tip">
                      提示：通过在二元自变量（X、Y）坐标系下配置多个插值限制点，算法将对实时工况进行双线性插值求出包络边界。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：用于评估多维动态工况边界的安全性。从插值限制表中读取多个离散控制点 <code>(x<sub>i</sub>, y<sub>i</sub>)</code> 的阈值限制 <code>Limit<sub>i</sub></code>。</p>
                        <p><strong>双线性插值</strong>：根据实时测得的自变量（如高度 <code>height</code>，速度 <code>velocity</code>）进行二维线性插值计算出动态边界：</p>
                        <div class="intro-formula">
                          Limit<sub>dynamic</sub> = f(x<sub>real</sub>, y<sub>real</sub>)
                        </div>
                        <p><strong>判定规则</strong>：实际测量值 <code>Value<sub>real</sub></code> 必须处于安全边界内（低于限值边界），否则判定越界触发惩罚。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 7. RELATIVE_COMPARE 相对比较 -->
                  <div v-else-if="ruleForm.ruleType === 'RELATIVE_COMPARE'" class="rule-box">
                    <h4 class="rule-title">相对比较基准配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="8">
                        <el-form-item label="基线基准数据源" required>
                          <el-input v-model="relativeCompareForm.baselineSource" placeholder="例如 MODEL_B" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="比较算法类型" required>
                          <el-select v-model="relativeCompareForm.compareType" placeholder="请选择" style="width: 100%">
                            <el-option label="百分比比例改善 (PERCENTAGE)" value="PERCENTAGE" />
                            <el-option label="绝对差值改善 (ABSOLUTE)" value="ABSOLUTE" />
                          </el-select>
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="目标提升额度阈值" required>
                          <el-input-number
                            v-model="relativeCompareForm.targetImprovement"
                            :precision="2"
                            :step="5"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：用于评估当前试验表现相较于历史型号或仿真模型（基准）的改进百分比或绝对增量。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：用于衡量新状态相较于基准模型（如旧型号型号、仿真基准等）的改善幅度。</p>
                        <p><strong>计算公式</strong>（设测试值为 <code>X<sub>test</sub></code>，基准值为 <code>X<sub>base</sub></code>）：</p>
                        <ul>
                          <li><strong>百分比改善 (PERCENTAGE)</strong>:
                            <div class="intro-formula">&eta; = [ (X<sub>test</sub> - X<sub>base</sub>) / X<sub>base</sub> ] &times; 100%</div>
                          </li>
                           <li><strong>绝对值改善 (ABSOLUTE)</strong>:
                            <div class="intro-formula">&Delta; = X<sub>test</sub> - X<sub>base</sub></div>
                          </li>
                        </ul>
                        <p><strong>使用建议</strong>：配置目标提升额度阈值。当提升值小于设定阈值时判定不达标。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 8. ROBUSTNESS 鲁棒性评估 -->
                  <div v-else-if="ruleForm.ruleType === 'ROBUSTNESS'" class="rule-box">
                    <h4 class="rule-title">鲁棒性评估配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="外部扰动源参数名 (disturbanceSource)" required>
                          <el-input v-model="robustnessForm.disturbanceSource" placeholder="例如 wind / temperature" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="性能衰退斜率极限 (slopeLimit)" required>
                          <el-input-number
                            v-model="robustnessForm.slopeLimit"
                            :precision="4"
                            :step="0.05"
                            :min="0"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：系统鲁棒性以指标在受到特定外部扰动时的衰退变化斜率来度量，衰退斜率越小鲁棒性越强。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：评估指标在存在外部扰动（如风速、温度）时的抗干扰能力。通过对“扰动量-指标值”数据集 <code>{(J<sub>j</sub>, P<sub>j</sub>)}</code> 进行回归分析计算衰退斜率。</p>
                        <p><strong>数学模型</strong>：系统响应对扰动的衰退敏感斜率 <code>K</code> 计算如下：</p>
                        <div class="intro-formula">
                          K = dP / dJ &approx; &Delta;P / &Delta;J
                        </div>
                        <p><strong>使用建议</strong>：要求斜率绝对值满足 <code>|K| &le; slopeLimit</code>。斜率越小，说明系统在干扰下的性能衰退越慢，鲁棒性越强。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 9. PROBABILITY_CONFIDENCE 概率与置信度 -->
                  <div v-else-if="ruleForm.ruleType === 'PROBABILITY_CONFIDENCE'" class="rule-box">
                    <h4 class="rule-title">小样本概率置信度配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="置信水平 (1 - α, confidenceLevel)" required>
                          <el-input-number
                            v-model="probabilityConfidenceForm.confidenceLevel"
                            :precision="2"
                            :step="0.05"
                            :min="0.5"
                            :max="0.99"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="合格的置信下限阈值" required>
                          <el-input-number
                            v-model="probabilityConfidenceForm.lowerLimitThreshold"
                            :precision="4"
                            :step="0.05"
                            :min="0"
                            :max="1"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：主要针对小样本试验，通过统计置信度公式（如 Clopper-Pearson 算法）评估在指定置信水平下的成功率置信下限。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：针对小样本（如低频次高价值发射）研制阶段，利用统计学置信区间来估算系统在给定置信水平时的成功率下限。</p>
                        <p><strong>数学模型</strong>：在给定的置信水平 <code>1 - &alpha;</code>（如 0.90 表示 90% 置信水平）下，通过 <strong>Clopper-Pearson</strong> 算法计算成功率的双侧置信区间下限 <code>p<sub>lower</sub></code>。</p>
                        <p><strong>使用建议</strong>：若计算出的置信下限 <code>p<sub>lower</sub></code> 低于设定的最低合格置信下限阈值，则判定指标不达标。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 10. INTERVAL_OVERLAP 区间重合度 -->
                  <div v-else-if="ruleForm.ruleType === 'INTERVAL_OVERLAP'" class="rule-box">
                    <h4 class="rule-title">区间重合度配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="8">
                        <el-form-item label="合格区间下限 (requiredMin)" required>
                          <el-input-number
                            v-model="intervalOverlapForm.requiredMin"
                            :precision="2"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="合格区间上限 (requiredMax)" required>
                          <el-input-number
                            v-model="intervalOverlapForm.requiredMax"
                            :precision="2"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="最小重合百分比限制 (overlapRatioLimit)" required>
                          <el-input-number
                            v-model="intervalOverlapForm.overlapRatioLimit"
                            :precision="2"
                            :step="0.05"
                            :min="0"
                            :max="1"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：实际测得指标变化范围与设定的标准合格区间发生重合时，重叠比例必须达到设定的最低界限。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：评估参数实际波动区间与标准合格区间的重合匹配度。</p>
                        <p><strong>数学模型</strong>（设测得区间为 <code>I<sub>1</sub> = [a<sub>1</sub>, b<sub>1</sub>]</code>，合格区间为 <code>I<sub>2</sub> = [a<sub>2</sub>, b<sub>2</sub>]</code>）：</p>
                        <div class="intro-formula">
                          Overlap = Length(I<sub>1</sub> &cap; I<sub>2</sub>) / Length(I<sub>1</sub>)
                        </div>
                        <p><strong>使用建议</strong>：配置合格区间范围和最小重合比率限制。当区间重叠占比 <code>Overlap</code> 低于该限制时判定不达标。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 11. MONOTONICITY 单调性 -->
                  <div v-else-if="ruleForm.ruleType === 'MONOTONICITY'" class="rule-box">
                    <h4 class="rule-title">单调性配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="期望单调变化方向" required>
                          <el-select v-model="monotonicityForm.monotonicDirection" style="width: 100%">
                            <el-option label="单调递增 (INCREASING)" value="INCREASING" />
                            <el-option label="单调递减 (DECREASING)" value="DECREASING" />
                          </el-select>
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="最小单调比率极限" required>
                          <el-input-number
                            v-model="monotonicityForm.ratioLimit"
                            :precision="2"
                            :step="0.05"
                            :min="0.5"
                            :max="1.0"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：评估随工况输入线性增大时，响应输出在整个区间是否具备严格单调性（满足方向的采样点占比不低于限制）。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与公式说明</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：验证系统响应随工况单向连续变化时的递增或递减趋势。</p>
                        <p><strong>计算逻辑</strong>：统计实际采样数据对在单向变化区间内，满足递增条件 <code>(y<sub>i+1</sub> &gt; y<sub>i</sub>)</code> 或递减条件 <code>(y<sub>i+1</sub> &lt; y<sub>i</sub>)</code> 的点对占比 <code>Ratio<sub>mono</sub></code>。</p>
                        <p><strong>使用建议</strong>：要求该占比不低于设定的最小单调占比限制（如 0.95 表示至少有 95% 的数据对符合单调趋势）。</p>
                      </div>
                    </div>
                  </div>

                  <!-- 12. VETO_CONTROL 否决控制 -->
                  <div v-else-if="ruleForm.ruleType === 'VETO_CONTROL'" class="rule-box">
                    <h4 class="rule-title">否决控制配置</h4>
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="一票否决触发分数阈值" required>
                          <el-input-number
                            v-model="vetoControlForm.vetoScoreThreshold"
                            :min="0"
                            :max="100"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="触发后惩罚动作" required>
                          <el-select v-model="vetoControlForm.vetoAction" style="width: 100%">
                            <el-option label="分数抹零惩罚 (DIRECT_ZERO)" value="DIRECT_ZERO" />
                            <el-option label="限制最高等级 (DIRECT_FAIL)" value="DIRECT_FAIL" />
                            <el-option label="扣分并限制等级 (DOWNGRADE)" value="DOWNGRADE" />
                          </el-select>
                        </el-form-item>
                      </el-col>
                    </el-row>
                    
                    <el-row :gutter="20" class="mt8" v-if="vetoControlForm.vetoAction === 'DOWNGRADE' || vetoControlForm.vetoAction === 'DIRECT_FAIL'">
                      <el-col :span="12" v-if="vetoControlForm.vetoAction === 'DOWNGRADE'">
                        <el-form-item label="扣减分值 (分)" required>
                          <el-input-number
                            v-model="vetoControlForm.vetoScoreDeduct"
                            :min="0"
                            :max="100"
                            style="width: 100%"
                            controls-position="right"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="限制最高等级" required>
                          <el-select v-model="vetoControlForm.vetoLevelLimit" style="width: 100%">
                            <el-option
                              v-for="lvl in vetoLevelOptions"
                              :key="lvl"
                              :label="lvl"
                              :value="lvl"
                            />
                          </el-select>
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="form-tip">
                      提示：当指标得分低于设定的触发分数阈值时，自动激活全局一票否决控制。
                    </div>
                    <div class="formula-intro-panel mt12">
                      <div class="intro-title"><el-icon><InfoFilled /></el-icon> 规则原理与使用指南</div>
                      <div class="intro-content">
                        <p><strong>基本原理</strong>：实现硬性指标的一票否决惩罚机制。当指标的原始得分低于设定的否决触发分数阈值时，直接处罚整个分支节点。</p>
                        <p><strong>处罚动作</strong>：</p>
                        <ul>
                          <li><code>分数抹零惩罚 (DIRECT_ZERO)</code>：直接将节点最终评分强制判定为 0 分。</li>
                          <li><code>限制最高等级 (DIRECT_FAIL)</code>：强制当前分支的最高最终评级上限为指定等级（如最高评定为及格）。</li>
                          <li><code>扣分并限制等级 (DOWNGRADE)</code>：扣除指定分值且限制最高等级上限。</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                  
                  <div v-else class="no-rule-tip">
                    请选择上述规则类型以开始定义本节点的判定准则。
                  </div>
                </div>
              </div>

              <!-- 第四板块：结论文案模板 -->
              <div class="form-section mt16" v-if="ruleForm.ruleType">
                <h3 class="section-title">4. 后置评估结论文案模板</h3>
                <el-form-item label="结论文案模板 (conclusionTemplate)">
                  <el-input
                    v-model="ruleForm.conclusionTemplate"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入评估结论文案模板"
                  />
                </el-form-item>
                <div class="form-tip" style="line-height: 1.8;">
                  <strong>文案占位符（计算时自动替换，请使用单花括号）：</strong><br />
                  <code>{name}</code>：指标名称 &nbsp;|&nbsp; 
                  <code>{score}</code>：得分 &nbsp;|&nbsp; 
                  <code>{grade}</code>：等级 &nbsp;|&nbsp; 
                  <code>{label}</code>：指标标识
                  <div style="margin-top: 8px; border-top: 1px dashed #dcdfe6; padding-top: 8px;">
                    <strong>与评估报告映射说明：</strong><br />
                    • 根节点结论文案 ➜ 自动替换报告模板中的 <code>{{FinalConclusion}}</code>（最终结论）<br />
                    • 子节点结论文案 ➜ 自动渲染进报告对应指标章节的分析文本中
                  </div>
                </div>
              </div>
            </el-form>
          </div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useThemeStore } from '@/store/theme'
import { Document, FolderOpened, Search, InfoFilled } from '@element-plus/icons-vue'
import {
  getSetInfo,
  getCriteriaBySet,
  batchSaveCriteria,
  listSets,
  addSet,
  removeSets
} from '@/api/zhpg/evalCriterion'
import { getIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import { parseIndicatorTreeToForest } from '@/utils/zhpg/zhpgIndicatorTreeJson'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()

// 状态定义
const setId = ref(null)
const criterionSet = ref({})
const systemName = ref('')
const indicatorSystemId = ref(null)
const loading = ref(false)
const saveLoading = ref(false)
const isInitializing = ref(false)
const criterionSetList = ref([])

const currentCriterionSetName = computed(() => {
  const currentSet = criterionSetList.value.find(s => String(s.id) === String(setId.value))
  return currentSet ? currentSet.setName : '加载中...'
})

const treeData = ref([])
const activeNode = ref(null)
const filterText = ref('')
const treeRef = ref(null)
const nodeCount = ref(0)

// 全局暂存 Map：key = indicatorId (uid), value = EvalCriterion 结构对象
const evalCriteriaMap = ref({})

// 树属性定义
const treeProps = {
  label: (data) => data.indicatorName || data.label || '',
  children: 'children'
}

// 选项常量
const VALUE_CATEGORY_OPTIONS = [
  { value: '效益型', label: '效益型（越大越好）' },
  { value: '成本型', label: '成本型（越小越好）' },
  { value: '区间效益型', label: '区间效益型（区间内好）' }
]

const RULE_TYPE_GROUPS = [
  {
    label: '数据层准则（应用于指标物理值/原始试验数据）',
    options: [
      { value: 'TREND_STABILITY', label: '稳定性分析 (TREND_STABILITY)' },
      { value: 'DYNAMIC_ENVELOPE', label: '动态包络 (DYNAMIC_ENVELOPE)' },
      { value: 'ROBUSTNESS', label: '鲁棒性评估 (ROBUSTNESS)' },
      { value: 'RELATIVE_COMPARE', label: '相对比较 (RELATIVE_COMPARE)' },
      { value: 'PROBABILITY_CONFIDENCE', label: '概率与置信度 (PROBABILITY_CONFIDENCE)' },
      { value: 'INTERVAL_OVERLAP', label: '区间重合度 (INTERVAL_OVERLAP)' },
      { value: 'MONOTONICITY', label: '单调性 (MONOTONICITY)' }
    ]
  },
  {
    label: '评估层准则（作用于指标量化评估值/得分与等级）',
    options: [
      { value: 'LEVEL_MAP', label: '等级映射 (LEVEL_MAP)' },
      { value: 'SHORT_BOARD_COMP', label: '短板均值补偿 (SHORT_BOARD_COMP)' },
      { value: 'CONDITION', label: '逻辑条件判定 (CONDITION)' },
      { value: 'VETO_CONTROL', label: '一票否决控制 (VETO_CONTROL)' }
    ]
  }
]

const RULE_TYPE_OPTIONS = [
  { value: 'TREND_STABILITY', label: '稳定性分析 (TREND_STABILITY)' },
  { value: 'DYNAMIC_ENVELOPE', label: '动态包络 (DYNAMIC_ENVELOPE)' },
  { value: 'ROBUSTNESS', label: '鲁棒性评估 (ROBUSTNESS)' },
  { value: 'RELATIVE_COMPARE', label: '相对比较 (RELATIVE_COMPARE)' },
  { value: 'PROBABILITY_CONFIDENCE', label: '概率与置信度 (PROBABILITY_CONFIDENCE)' },
  { value: 'INTERVAL_OVERLAP', label: '区间重合度 (INTERVAL_OVERLAP)' },
  { value: 'MONOTONICITY', label: '单调性 (MONOTONICITY)' },
  { value: 'LEVEL_MAP', label: '等级映射 (LEVEL_MAP)' },
  { value: 'SHORT_BOARD_COMP', label: '短板均值补偿 (SHORT_BOARD_COMP)' },
  { value: 'CONDITION', label: '逻辑条件判定 (CONDITION)' },
  { value: 'VETO_CONTROL', label: '一票否决控制 (VETO_CONTROL)' }
]

// 右侧通用基础表单（双向绑定右侧交互）
const ruleForm = reactive({
  id: undefined,
  valueCategory: '效益型',
  bestValue: undefined,
  worstValue: undefined,
  isMandatory: 0,
  vetoAction: 'DIRECT_ZERO',
  vetoScoreDeduct: 20,
  vetoLevelLimit: '不及格',
  ruleType: undefined,
  conclusionTemplate: undefined
})

// 一票否决限制的等级候选列表
const vetoLevelOptions = computed(() => {
  if (ruleForm.ruleType === 'LEVEL_MAP' && levelMapForm.tiers && levelMapForm.tiers.length > 0) {
    return levelMapForm.tiers.map(t => t.name).filter(Boolean)
  }
  return ['优秀', '良好', '及格', '不及格']
})



const levelMapForm = reactive({
  tiers: []
})

const shortBoardForm = reactive({
  lambda: 0.5
})

const conditionForm = reactive({
  relation: 'AND',
  conditions: [],
  action: {
    type: 'SET_SCORE',
    value: 0
  }
})

function addConditionRow() {
  conditionForm.conditions.push({
    field: 'value',
    operator: '>=',
    threshold: 0
  })
}

function removeConditionRow(index) {
  conditionForm.conditions.splice(index, 1)
}

// 监听干预类型变化，自动初始化其 value 对应的值类型，避免脏数据残留或展示异常
watch(
  () => conditionForm.action.type,
  (newType) => {
    if (isInitializing.value) return
    if (newType === 'SET_GRADE') {
      conditionForm.action.value = vetoLevelOptions.value[0] || '及格'
    } else {
      conditionForm.action.value = 0
    }
  }
)

// 稳定性分析 (TREND_STABILITY)
const trendStabilityForm = reactive({
  settlingTimeMax: 3.0,
  overshootMax: 5.0,
  targetRangePercent: 2.0
})

// 动态包络 (DYNAMIC_ENVELOPE)
const dynamicEnvelopeForm = reactive({
  xVariable: 'height',
  yVariable: 'velocity',
  lookupTable: []
})

function addEnvelopeRow() {
  dynamicEnvelopeForm.lookupTable.push({
    x: 10000,
    y: 340,
    limit: 80
  })
}

function removeEnvelopeRow(index) {
  dynamicEnvelopeForm.lookupTable.splice(index, 1)
}

// 相对比较 (RELATIVE_COMPARE)
const relativeCompareForm = reactive({
  baselineSource: 'MODEL_B',
  compareType: 'PERCENTAGE',
  targetImprovement: 20.0
})

// 鲁棒性评估 (ROBUSTNESS)
const robustnessForm = reactive({
  disturbanceSource: 'wind',
  slopeLimit: 0.15
})

// 概率与置信度 (PROBABILITY_CONFIDENCE)
const probabilityConfidenceForm = reactive({
  confidenceLevel: 0.90,
  lowerLimitThreshold: 0.80
})

// 区间重合度 (INTERVAL_OVERLAP)
const intervalOverlapForm = reactive({
  requiredMin: 60.0,
  requiredMax: 100.0,
  overlapRatioLimit: 0.95
})

// 单调性 (MONOTONICITY)
const monotonicityForm = reactive({
  monotonicDirection: 'INCREASING',
  ratioLimit: 0.95
})

// 否决控制 (VETO_CONTROL)
const vetoControlForm = reactive({
  vetoScoreThreshold: 60.0,
  vetoAction: 'DIRECT_ZERO',
  vetoScoreDeduct: 20,
  vetoLevelLimit: '不及格'
})

// 监听否决控制处罚动作变化，联动更新其对应字段
watch(
  () => vetoControlForm.vetoAction,
  (newAction) => {
    if (isInitializing.value) return
    if (newAction === 'DOWNGRADE') {
      vetoControlForm.vetoScoreDeduct = 20
      vetoControlForm.vetoLevelLimit = '不及格'
    } else if (newAction === 'DIRECT_FAIL') {
      vetoControlForm.vetoLevelLimit = '不及格'
    }
  }
)

// 侦听搜索框
watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

// 侦听右侧表单与规则修改，实现左侧树节点标记实时回显亮起
watch(
  () => [
    ruleForm.ruleType,
    ruleForm.isMandatory,
    ruleForm.valueCategory,
    ruleForm.bestValue,
    ruleForm.worstValue,
    ruleForm.vetoAction,
    ruleForm.vetoScoreDeduct,
    ruleForm.vetoLevelLimit,
    ruleForm.conclusionTemplate,
    levelMapForm.tiers,
    shortBoardForm.lambda,
    conditionForm.relation,
    conditionForm.conditions,
    conditionForm.action,
    trendStabilityForm,
    dynamicEnvelopeForm,
    relativeCompareForm,
    robustnessForm,
    probabilityConfidenceForm,
    intervalOverlapForm,
    monotonicityForm,
    vetoControlForm
  ],
  () => {
    if (isInitializing.value) return
    if (activeNode.value) {
      if (ruleForm.ruleType) {
        saveActiveNodeToMap()
      } else {
        delete evalCriteriaMap.value[String(activeNode.value.uid)]
      }
    }
  },
  { deep: true }
)

// 用于跟踪上一次的多级区间等级名称列表
let lastTiersNames = []

// 侦听等级映射的区间名称及个数变化，实现一票否决限制等级的自动同步联动
watch(
  () => levelMapForm.tiers,
  (newTiers) => {
    if (!newTiers) return
    const newNames = newTiers.map(t => t.name || '')
    
    // 如果新旧名称数组完全一致，说明是切换节点引起的初始化加载，不需要同步
    if (JSON.stringify(newNames) === JSON.stringify(lastTiersNames)) {
      return
    }

    // 1. 等级重命名联动：若修改了某一个已有的等级名称，且该等级刚好是一票否决的当前限制等级，则自动更新
    if (lastTiersNames.length === newTiers.length) {
      let renamedIndex = -1
      for (let i = 0; i < newTiers.length; i++) {
        if (newTiers[i].name !== lastTiersNames[i]) {
          renamedIndex = i
          break
        }
      }
      if (renamedIndex !== -1) {
        const oldName = lastTiersNames[renamedIndex]
        const newName = newTiers[renamedIndex].name
        if (ruleForm.vetoLevelLimit === oldName && newName) {
          ruleForm.vetoLevelLimit = newName
        }
      }
    } else {
      // 2. 等级减少/删除联动：若删除了某行，导致当前一票否决限制等级不存在了，则重置为剩下的最后一档（最差一档）
      const validNames = newNames.filter(Boolean)
      if (ruleForm.vetoLevelLimit && !validNames.includes(ruleForm.vetoLevelLimit)) {
        if (validNames.length > 0) {
          ruleForm.vetoLevelLimit = validNames[validNames.length - 1]
        } else {
          ruleForm.vetoLevelLimit = '不及格'
        }
      }
    }

    // 更新上一次的名称快照
    lastTiersNames = newTiers.map(t => t.name || '')
  },
  { deep: true }
)

onMounted(() => {
  const querySetId = route.query.setId
  if (!querySetId) {
    ElMessage.error('缺失必要的参数 setId')
    handleBack()
    return
  }
  setId.value = Number(querySetId)
  loadAllData()
})

// 加载全部设计数据
async function loadAllData() {
  loading.value = true
  try {
    // 1. 加载准则集详情
    const setRes = await getSetInfo(setId.value)
    criterionSet.value = setRes.data || {}
    indicatorSystemId.value = criterionSet.value.indicatorSystemId

    // 2. 加载关联指标体系详情 & 同体系下所有准则集
    if (indicatorSystemId.value) {
      const [sysRes, setsRes] = await Promise.all([
        getIndicatorSystem(indicatorSystemId.value),
        listSets({ indicatorSystemId: indicatorSystemId.value })
      ])
      systemName.value = sysRes.data?.systemName || ''
      criterionSetList.value = setsRes.rows || []

      // 解析树，加载 level 属性
      const forest = parseIndicatorTreeToForest(sysRes.data?.indicatorTreeWeight || sysRes.data?.indicatorTree)
      assignNodeLevels(forest, 1)
      treeData.value = forest
      nodeCount.value = countTreeNodes(forest)
    }

    // 收集树上所有节点的真实无损 UID，构建精度丢失映射关系
    const uids = collectUids(treeData.value)
    const lossyUidMap = {}
    uids.forEach(uid => {
      // 强转成 Number 再转回 String，人工模拟 JS 精度丢失截断的结果
      const lossy = String(Number(uid))
      lossyUidMap[lossy] = uid
    })

    // 3. 加载已有准则规则明细
    const detailRes = await getCriteriaBySet(setId.value)
    const list = detailRes.data || []
    const setCodePrefix = (criterionSet.value.setCode || '') + '_'
    const map = {}
    list.forEach(item => {
      let exactIndicatorId = String(item.indicatorId)
      
      // 优先从具有字符串保护的 criterionCode 中提取精确原始的 indicatorId 自愈大数精度丢失
      if (item.criterionCode && setCodePrefix && item.criterionCode.startsWith(setCodePrefix)) {
        const extractedId = item.criterionCode.substring(setCodePrefix.length)
        if (extractedId && extractedId.trim() !== '') {
          exactIndicatorId = extractedId
        }
      } else {
        // 降级使用精度丢失映射
        const origId = String(item.indicatorId)
        exactIndicatorId = lossyUidMap[origId] || origId
      }
      
      // 自愈修正明细对象属性，确保后续二次保存时自动订正为正确的无损 ID
      item.indicatorId = exactIndicatorId
      
      map[exactIndicatorId] = item
    })
    evalCriteriaMap.value = map

    // 自动选中高亮第一个已配有准则的节点（若均未配置，则默认选中第一个根节点）
    nextTick(() => {
      if (treeData.value && treeData.value.length > 0) {
        const firstNode = findFirstConfiguredNode(treeData.value)
        const targetNode = firstNode || treeData.value[0]
        if (targetNode) {
          treeRef.value?.setCurrentKey(targetNode.uid)
          handleNodeClick(targetNode)
        }
      }
    })
  } catch (error) {
    console.error('加载准则设计器数据失败:', error)
    ElMessage.error('加载关联数据失败，请重试')
  } finally {
    loading.value = false
  }
}

// 递归收集树上所有节点的 uid
function collectUids(nodes, uids = []) {
  if (!Array.isArray(nodes)) return uids
  nodes.forEach(node => {
    if (node.uid) {
      uids.push(String(node.uid))
    }
    if (node.children && node.children.length > 0) {
      collectUids(node.children, uids)
    }
  })
  return uids
}

// 递归计算节点层级
function assignNodeLevels(nodes, level = 1) {
  if (!Array.isArray(nodes)) return
  nodes.forEach(node => {
    node.level = level
    if (node.children && node.children.length > 0) {
      assignNodeLevels(node.children, level + 1)
    }
  })
}

// 统计节点总数
function countTreeNodes(nodes) {
  if (!Array.isArray(nodes)) return 0
  let count = 0
  nodes.forEach(node => {
    count++
    if (node.children?.length) {
      count += countTreeNodes(node.children)
    }
  })
  return count
}

// 递归寻找第一个配置了准则的节点
function findFirstConfiguredNode(nodes) {
  if (!Array.isArray(nodes)) return null
  for (const node of nodes) {
    if (hasCriterion(node.uid)) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findFirstConfiguredNode(node.children)
      if (found) return found
    }
  }
  return null
}

// 树过滤算法
function filterNode(value, data) {
  if (!value) return true
  const name = data.indicatorName || data.label || ''
  return name.indexOf(value) !== -1
}

// 查找匹配的评估准则（采用严格字符串匹配，大数在解析和加载时已做高精度自愈对齐）
function findMatchedCriterion(uid) {
  if (uid == null || uid === '') return null
  return evalCriteriaMap.value[String(uid)] || null
}

// 左侧辅助判断：某指标是否有配置准则
function hasCriterion(uid) {
  const item = findMatchedCriterion(uid)
  return !!(item && item.ruleType)
}

// 切换树节点
function handleNodeClick(data) {
  isInitializing.value = true

  // 1. 先把当前已修改的节点数据同步回全局暂存 Map 中
  saveActiveNodeToMap()

  // 2. 激活新节点
  activeNode.value = data

  // 3. 反序列化该节点的准则配置
  const criterion = findMatchedCriterion(data.uid)
  if (criterion) {
    ruleForm.id = criterion.id
    ruleForm.valueCategory = criterion.valueCategory || '效益型'
    ruleForm.bestValue = criterion.bestValue
    ruleForm.worstValue = criterion.worstValue
    ruleForm.isMandatory = criterion.isMandatory || 0
    ruleForm.vetoAction = criterion.vetoAction || 'DIRECT_ZERO'
    ruleForm.ruleType = criterion.ruleType
    ruleForm.conclusionTemplate = criterion.conclusionTemplate

    // 解析 ruleJson
    deserializeRuleJson(criterion.ruleType, criterion.ruleJson)
  } else {
    // 初始化空表单
    ruleForm.id = undefined
    ruleForm.valueCategory = data.valueCategory || '效益型'
    ruleForm.bestValue = undefined
    ruleForm.worstValue = undefined
    ruleForm.isMandatory = 0
    ruleForm.vetoAction = 'DIRECT_ZERO'
    ruleForm.vetoScoreDeduct = 20
    ruleForm.vetoLevelLimit = '不及格'
    ruleForm.ruleType = undefined
    ruleForm.conclusionTemplate = undefined

    // 默认重置所有规则表单配置
    resetAllRuleForms()
  }
  
  // 同步初始化等级名称快照，避免切换节点时触发 watch 的误动作
  lastTiersNames = levelMapForm.tiers.map(t => t.name || '')

  nextTick(() => {
    isInitializing.value = false
  })
}

// 重置所有规则子表单的响应式状态，避免跨节点或跨规则类型的交叉污染
function resetAllRuleForms() {
  levelMapForm.tiers = []
  shortBoardForm.lambda = 0.5
  conditionForm.relation = 'AND'
  conditionForm.conditions = []
  conditionForm.action = { type: 'SET_SCORE', value: 0 }
  
  trendStabilityForm.settlingTimeMax = 3.0
  trendStabilityForm.overshootMax = 5.0
  trendStabilityForm.targetRangePercent = 2.0
  
  dynamicEnvelopeForm.xVariable = 'height'
  dynamicEnvelopeForm.yVariable = 'velocity'
  dynamicEnvelopeForm.lookupTable = []
  
  relativeCompareForm.baselineSource = 'MODEL_B'
  relativeCompareForm.compareType = 'PERCENTAGE'
  relativeCompareForm.targetImprovement = 20.0
  
  robustnessForm.disturbanceSource = 'wind'
  robustnessForm.slopeLimit = 0.15
  
  probabilityConfidenceForm.confidenceLevel = 0.90
  probabilityConfidenceForm.lowerLimitThreshold = 0.80
  
  intervalOverlapForm.requiredMin = 60.0
  intervalOverlapForm.requiredMax = 100.0
  intervalOverlapForm.overlapRatioLimit = 0.95
  
  monotonicityForm.monotonicDirection = 'INCREASING'
  monotonicityForm.ratioLimit = 0.95
  
  vetoControlForm.vetoScoreThreshold = 60.0
  vetoControlForm.vetoAction = 'DIRECT_ZERO'
  vetoControlForm.vetoScoreDeduct = 20
  vetoControlForm.vetoLevelLimit = '不及格'
}

// 选择不同的规则类型时的默认结论文案推荐及表单默认值补充
function handleRuleTypeChange(type) {
  if (!ruleForm.conclusionTemplate) {
    if (type === 'LEVEL_MAP') {
      ruleForm.conclusionTemplate = '指标 {name} 评分为 {score}，定级为 {grade}。'
    } else if (type === 'SHORT_BOARD_COMP') {
      ruleForm.conclusionTemplate = '指标 {name} 触发短板补偿计算，最终得分评定为 {score}。'
    } else {
      ruleForm.conclusionTemplate = '指标 {name} 评分为 {score}。'
    }
  }

  // 赋对应规则类型的默认初始数据
  if (type === 'LEVEL_MAP' && levelMapForm.tiers.length === 0) {
    levelMapForm.tiers = [
      { name: '优秀', valueMin: 90, valueMax: 100, conclusion: '指标表现极其优异' },
      { name: '良好', valueMin: 80, valueMax: 90, conclusion: '指标表现良好' },
      { name: '及格', valueMin: 60, valueMax: 80, conclusion: '指标表现基本及格' },
      { name: '不及格', valueMin: 0, valueMax: 60, conclusion: '指标未达到合格要求，需重点改进' }
    ]
  } else if (type === 'CONDITION' && conditionForm.conditions.length === 0) {
    conditionForm.relation = 'AND'
    conditionForm.conditions = [
      { field: 'value', operator: '>=', threshold: 0 }
    ]
    conditionForm.action = { type: 'SET_SCORE', value: 0 }
  } else if (type === 'DYNAMIC_ENVELOPE' && dynamicEnvelopeForm.lookupTable.length === 0) {
    dynamicEnvelopeForm.lookupTable = [
      { x: 10000, y: 340, limit: 80 }
    ]
  }

  // 同步初始化等级名称快照
  lastTiersNames = levelMapForm.tiers.map(t => t.name || '')
}

// 等级映射 tiers 动态表格交互
function addLevelTier() {
  levelMapForm.tiers.push({
    name: '',
    valueMin: 0,
    valueMax: 100,
    conclusion: ''
  })
}

function removeLevelTier(index) {
  levelMapForm.tiers.splice(index, 1)
}

// 清除当前准则
function handleRemoveCriterion() {
  if (!activeNode.value) return
  ElMessageBox.confirm('是否确认清除该指标的全部评估准则？', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isInitializing.value = true
    const matched = findMatchedCriterion(activeNode.value.uid)
    if (matched && matched.indicatorId) {
      delete evalCriteriaMap.value[String(matched.indicatorId)]
    }
    delete evalCriteriaMap.value[String(activeNode.value.uid)]
    
    // 初始化空表单
    ruleForm.id = undefined
    ruleForm.valueCategory = activeNode.value.valueCategory || '效益型'
    ruleForm.bestValue = undefined
    ruleForm.worstValue = undefined
    ruleForm.isMandatory = 0
    ruleForm.vetoAction = 'DIRECT_ZERO'
    ruleForm.vetoScoreDeduct = 20
    ruleForm.vetoLevelLimit = '不及格'
    ruleForm.ruleType = undefined
    ruleForm.conclusionTemplate = undefined

    // 重置所有表单
    resetAllRuleForms()

    lastTiersNames = []
    
    nextTick(() => {
      isInitializing.value = false
    })
    ElMessage.success('已清除该节点准则（尚未保存，需在右上角保存生效）')
  }).catch(() => {})
}

// 缓存右侧属性表单到全局 Map
function saveActiveNodeToMap() {
  if (!activeNode.value) return

  // 如果根本没有选规则类型，就不写入 Map（代表未配置）
  if (!ruleForm.ruleType) {
    return
  }

  // 构造 ruleJson 字符串
  let ruleObj = {}
  if (ruleForm.ruleType === 'LEVEL_MAP') {
    ruleObj = {
      tiers: levelMapForm.tiers
    }
  } else if (ruleForm.ruleType === 'SHORT_BOARD_COMP') {
    ruleObj = {
      lambda: shortBoardForm.lambda
    }
  } else if (ruleForm.ruleType === 'CONDITION') {
    ruleObj = {
      relation: conditionForm.relation,
      conditions: conditionForm.conditions,
      action: {
        type: conditionForm.action.type,
        value: conditionForm.action.value
      }
    }
  } else if (ruleForm.ruleType === 'TREND_STABILITY') {
    ruleObj = {
      settlingTimeMax: trendStabilityForm.settlingTimeMax,
      overshootMax: trendStabilityForm.overshootMax,
      targetRangePercent: trendStabilityForm.targetRangePercent
    }
  } else if (ruleForm.ruleType === 'DYNAMIC_ENVELOPE') {
    ruleObj = {
      xVariable: dynamicEnvelopeForm.xVariable,
      yVariable: dynamicEnvelopeForm.yVariable,
      lookupTable: dynamicEnvelopeForm.lookupTable
    }
  } else if (ruleForm.ruleType === 'RELATIVE_COMPARE') {
    ruleObj = {
      baselineSource: relativeCompareForm.baselineSource,
      compareType: relativeCompareForm.compareType,
      targetImprovement: relativeCompareForm.targetImprovement
    }
  } else if (ruleForm.ruleType === 'ROBUSTNESS') {
    ruleObj = {
      disturbanceSource: robustnessForm.disturbanceSource,
      slopeLimit: robustnessForm.slopeLimit
    }
  } else if (ruleForm.ruleType === 'PROBABILITY_CONFIDENCE') {
    ruleObj = {
      confidenceLevel: probabilityConfidenceForm.confidenceLevel,
      lowerLimitThreshold: probabilityConfidenceForm.lowerLimitThreshold
    }
  } else if (ruleForm.ruleType === 'INTERVAL_OVERLAP') {
    ruleObj = {
      requiredMin: intervalOverlapForm.requiredMin,
      requiredMax: intervalOverlapForm.requiredMax,
      overlapRatioLimit: intervalOverlapForm.overlapRatioLimit
    }
  } else if (ruleForm.ruleType === 'MONOTONICITY') {
    ruleObj = {
      monotonicDirection: monotonicityForm.monotonicDirection,
      ratioLimit: monotonicityForm.ratioLimit
    }
  } else if (ruleForm.ruleType === 'VETO_CONTROL') {
    ruleObj = {
      vetoScoreThreshold: vetoControlForm.vetoScoreThreshold,
      vetoAction: vetoControlForm.vetoAction,
      vetoScoreDeduct: vetoControlForm.vetoScoreDeduct,
      vetoLevelLimit: vetoControlForm.vetoLevelLimit
    }
  }

  // 写入一票否决自定义参数
  if (ruleForm.isMandatory === 1) {
    ruleObj.vetoParams = {
      vetoScoreDeduct: ruleForm.vetoScoreDeduct,
      vetoLevelLimit: ruleForm.vetoLevelLimit
    }
  }

  const ruleJson = JSON.stringify(ruleObj)

  // 先寻找原先有大数精度丢失的重复 key，防重复覆盖时产生脏数据残留
  const matched = findMatchedCriterion(activeNode.value.uid)
  if (matched && matched.indicatorId && String(matched.indicatorId) !== String(activeNode.value.uid)) {
    delete evalCriteriaMap.value[String(matched.indicatorId)]
  }

  // 将编辑后的数据暂存入 Map
  evalCriteriaMap.value[String(activeNode.value.uid)] = {
    id: ruleForm.id,
    setId: setId.value,
    indicatorSystemId: indicatorSystemId.value,
    indicatorId: String(activeNode.value.uid),
    indicatorLevel: activeNode.value.level,
    criterionCode: criterionSet.value.setCode + '_' + activeNode.value.uid,
    criterionName: (activeNode.value.indicatorName || activeNode.value.label || '') + '评估准则',
    valueCategory: ruleForm.valueCategory,
    bestValue: ruleForm.bestValue,
    worstValue: ruleForm.worstValue,
    isMandatory: ruleForm.isMandatory,
    vetoAction: ruleForm.isMandatory === 1 ? ruleForm.vetoAction : null,
    ruleType: ruleForm.ruleType,
    ruleJson: ruleJson,
    conclusionTemplate: ruleForm.conclusionTemplate,
    status: '1'
  }
}

// 反序列化规则 JSON 到表单
function deserializeRuleJson(type, jsonStr) {
  // 先把所有规则表单重置，确保干净
  resetAllRuleForms()

  if (!jsonStr) {
    return
  }
  try {
    const obj = JSON.parse(jsonStr)
    if (type === 'LEVEL_MAP') {
      levelMapForm.tiers = Array.isArray(obj.tiers) ? obj.tiers : []
    } else if (type === 'SHORT_BOARD_COMP') {
      shortBoardForm.lambda = obj.lambda != null ? obj.lambda : 0.5
    } else if (type === 'CONDITION') {
      conditionForm.relation = obj.relation || 'AND'
      conditionForm.conditions = Array.isArray(obj.conditions) ? obj.conditions : []
      conditionForm.action = obj.action || { type: 'SET_SCORE', value: 0 }
    } else if (type === 'TREND_STABILITY') {
      trendStabilityForm.settlingTimeMax = obj.settlingTimeMax != null ? obj.settlingTimeMax : 3.0
      trendStabilityForm.overshootMax = obj.overshootMax != null ? obj.overshootMax : 5.0
      trendStabilityForm.targetRangePercent = obj.targetRangePercent != null ? obj.targetRangePercent : 2.0
    } else if (type === 'DYNAMIC_ENVELOPE') {
      dynamicEnvelopeForm.xVariable = obj.xVariable || 'height'
      dynamicEnvelopeForm.yVariable = obj.yVariable || 'velocity'
      dynamicEnvelopeForm.lookupTable = Array.isArray(obj.lookupTable) ? obj.lookupTable : []
    } else if (type === 'RELATIVE_COMPARE') {
      relativeCompareForm.baselineSource = obj.baselineSource || 'MODEL_B'
      relativeCompareForm.compareType = obj.compareType || 'PERCENTAGE'
      relativeCompareForm.targetImprovement = obj.targetImprovement != null ? obj.targetImprovement : 20.0
    } else if (type === 'ROBUSTNESS') {
      robustnessForm.disturbanceSource = obj.disturbanceSource || 'wind'
      robustnessForm.slopeLimit = obj.slopeLimit != null ? obj.slopeLimit : 0.15
    } else if (type === 'PROBABILITY_CONFIDENCE') {
      probabilityConfidenceForm.confidenceLevel = obj.confidenceLevel != null ? obj.confidenceLevel : 0.90
      probabilityConfidenceForm.lowerLimitThreshold = obj.lowerLimitThreshold != null ? obj.lowerLimitThreshold : 0.80
    } else if (type === 'INTERVAL_OVERLAP') {
      intervalOverlapForm.requiredMin = obj.requiredMin != null ? obj.requiredMin : 60.0
      intervalOverlapForm.requiredMax = obj.requiredMax != null ? obj.requiredMax : 100.0
      intervalOverlapForm.overlapRatioLimit = obj.overlapRatioLimit != null ? obj.overlapRatioLimit : 0.95
    } else if (type === 'MONOTONICITY') {
      monotonicityForm.monotonicDirection = obj.monotonicDirection || 'INCREASING'
      monotonicityForm.ratioLimit = obj.ratioLimit != null ? obj.ratioLimit : 0.95
    } else if (type === 'VETO_CONTROL') {
      vetoControlForm.vetoScoreThreshold = obj.vetoScoreThreshold != null ? obj.vetoScoreThreshold : 60.0
      vetoControlForm.vetoAction = obj.vetoAction || 'DIRECT_ZERO'
      vetoControlForm.vetoScoreDeduct = obj.vetoScoreDeduct != null ? obj.vetoScoreDeduct : 20
      vetoControlForm.vetoLevelLimit = obj.vetoLevelLimit || '不及格'
    }

    // 解析一票否决参数并还原至 ruleForm
    if (obj.vetoParams) {
      ruleForm.vetoScoreDeduct = obj.vetoParams.vetoScoreDeduct != null ? obj.vetoParams.vetoScoreDeduct : 20
      ruleForm.vetoLevelLimit = obj.vetoParams.vetoLevelLimit || '不及格'
    } else {
      ruleForm.vetoScoreDeduct = 20
      ruleForm.vetoLevelLimit = '不及格'
    }
  } catch (e) {
    // 异常情况下回填默认值
    ruleForm.vetoScoreDeduct = 20
    ruleForm.vetoLevelLimit = '不及格'
  }
}

function handleBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push({ name: 'TemplateCriterion' })
  }
}



// 批量覆盖保存准则集
function handleBatchSave() {
  // 1. 同步当前右侧表单里的改动
  saveActiveNodeToMap()

  // 2. 搜集待保存准则列表
  const criterionList = []
  for (const uid in evalCriteriaMap.value) {
    const item = evalCriteriaMap.value[uid]
    if (item && item.ruleType) {
      criterionList.push(item)
    }
  }

  saveLoading.value = true
  batchSaveCriteria(setId.value, criterionList)
    .then((res) => {
      ElMessage.success(res.msg || `覆盖保存准则成功，共保存 ${criterionList.length} 条指标准则！`)
    })
    .finally(() => {
      saveLoading.value = false
    })
}
</script>

<style scoped>
.criterion-designer-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  padding: 16px;
  overflow: hidden;
  box-sizing: border-box;
}

.designer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 12px;
}

.header-left {
  display: flex;
  align-items: center;
}

.title-area {
  display: flex;
  flex-direction: column;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.set-switcher-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.set-switcher-divider {
  margin: 0 4px;
  color: var(--el-border-color);
}

.highlight-text {
  font-weight: 550;
  color: var(--el-color-primary);
}

.designer-main {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0; /* 核心：限制高度，内部滚动 */
}

.tree-panel-wrapper {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.form-panel-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}

.full-height-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-radius: 8px;
}

.box-card :deep(.el-card__header) {
  padding: 12px 16px;
  flex-shrink: 0;
  background-color: var(--el-fill-color-light);
}

.box-card :deep(.el-card__body) {
  padding: 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.scrollable-card :deep(.el-card__body) {
  overflow-y: auto;
}

.card-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tree-scrollbar {
  flex: 1;
  min-height: 0;
}

.custom-indicator-tree {
  background: transparent;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  font-size: 13px;
  padding-right: 8px;
}

.node-icon {
  margin-right: 6px;
  display: flex;
  align-items: center;
  color: var(--el-text-color-secondary);
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-tags-area {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.node-tag {
  height: 18px;
  line-height: 16px;
  padding: 0 4px;
  font-size: 10px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.info-banner {
  display: flex;
  gap: 24px;
  background-color: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-light);
  border-left: 4px solid var(--el-color-primary);
  padding: 12px 16px;
  border-radius: 4px;
  flex-wrap: wrap;
}

.banner-item {
  font-size: 13px;
}

.banner-item .label {
  color: var(--el-text-color-secondary);
}

.banner-item .value {
  color: var(--el-text-color-primary);
}

.form-section {
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section-header .section-title {
  margin-bottom: 0;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.red-tip {
  color: var(--el-color-danger);
}

.rule-detail-card {
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  padding: 14px;
  border: 1px dashed var(--el-border-color-darker);
}

.rule-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rule-title {
  margin: 0 0 10px 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.no-rule-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.font-semibold {
  font-weight: 600;
}
.ml12 { margin-left: 12px; }
.mb12 { margin-bottom: 12px; }
.mb16 { margin-bottom: 16px; }
.mt12 { margin-top: 12px; }
.mt16 { margin-top: 16px; }
.ml4 { margin-left: 4px; }

/* 算法原理与公式说明卡片样式 */
.formula-intro-panel {
  background-color: var(--el-color-primary-light-9);
  border-left: 4px solid var(--el-color-primary);
  border-radius: 4px;
  padding: 12px 14px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.formula-intro-panel .intro-title {
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.formula-intro-panel .intro-content code {
  background-color: var(--el-fill-color-darker);
  color: var(--el-color-danger);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: Consolas, monospace;
}

.formula-intro-panel .intro-content p {
  margin: 4px 0;
}

.formula-intro-panel .intro-formula {
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 8px;
  margin: 6px 0;
  font-family: "Times New Roman", Times, serif;
  font-size: 13px;
  font-style: italic;
  text-align: center;
}
</style>

<style>
/* 准则规则类型下拉选择分组标题样式美化 */
.criterion-select-popper .el-select-group__title {
  font-weight: 700 !important;
  color: var(--el-color-primary) !important;
  font-size: 12px !important;
  padding: 8px 16px !important;
  background-color: var(--el-color-primary-light-9) !important;
  border-top: 1px solid var(--el-border-color-lighter) !important;
  border-bottom: 1px solid var(--el-border-color-lighter) !important;
  display: flex !important;
  align-items: center !important;
  gap: 6px !important;
  margin-bottom: 4px !important;
}

.criterion-select-popper .el-select-group__title::before {
  content: "" !important;
  display: inline-block !important;
  width: 4px !important;
  height: 12px !important;
  background-color: var(--el-color-primary) !important;
  border-radius: 2px !important;
}

.criterion-select-popper .el-select-group:first-child .el-select-group__title {
  border-top: none !important;
}
</style>
