<template>
  <div class="manual-build-page">
    <!-- TOP BAR -->
    <header class="tactical-header">
      <div class="header-left">
        <div class="divider"></div>
        <div class="title-area">
          <h2 class="project-title">{{ skeletonInfo.name }}</h2>
        </div>
      </div>
      <div class="header-right">
        <div class="action-buttons">
          <el-button type="warning" plain class="cyber-btn" @click="showSim">
            <template #icon><el-icon>
                <VideoPlay />
              </el-icon></template>
            模拟生成
          </el-button>
          <el-button type="primary" plain class="cyber-btn" @click="saveDraft">
            <template #icon><el-icon>
                <DocumentChecked />
              </el-icon></template>
            保存草稿
          </el-button>
          <el-button type="success" class="glow-btn">
            <template #icon><el-icon>
                <Upload />
              </el-icon></template>
            发布生效
          </el-button>
        </div>
      </div>
    </header>

    <div class="main-content">
      <SideLayout>
        <!-- LEFT PANEL -->
        <template #left>
          <div class="side-panel-inner">
            <div class="panel-section">
              <div class="section-title">基本信息</div>
              <div class="fld">
                <div class="fld-l"><span class="rq">*</span>骨架名称</div>
                <el-input v-model="skeletonInfo.name" placeholder="请输入骨架名称" />
              </div>
              <div class="fld">
                <div class="fld-l"><span class="rq">*</span>版本</div>
                <el-input v-model="skeletonInfo.version" style="width:120px" placeholder="v1.0.0" />
              </div>
              <div class="fld">
                <div class="fld-l">评估目标描述</div>
                <el-input v-model="skeletonInfo.desc" type="textarea" :rows="3" placeholder="简述骨架评估目标..." />
              </div>
            </div>

            <div class="panel-section">
              <div class="section-title">适用范围</div>
              <div class="fld">
                <div class="fld-l">业务领域</div>
                <div class="cyber-tags">
                  <span v-for="tag in ['电子对抗', '通信对抗', '雷达对抗']" :key="tag" class="cyber-tag"
                    :class="{ active: skeletonInfo.domains.includes(tag) }" @click="toggleTag('domains', tag)">{{ tag
                    }}</span>
                </div>
              </div>
              <div class="fld">
                <div class="fld-l">装备类型</div>
                <div class="cyber-tags">
                  <span v-for="tag in ['有源干扰机', '侦察干扰一体化', '宽带侦察']" :key="tag" class="cyber-tag highlight"
                    :class="{ active: skeletonInfo.eqTypes.includes(tag) }" @click="toggleTag('eqTypes', tag)">{{ tag
                    }}</span>
                </div>
              </div>
              <div class="fld">
                <div class="fld-l">评估层级</div>
                <el-checkbox-group v-model="skeletonInfo.levels" class="cyber-checkbox-group">
                  <el-checkbox-button label="设备级">设备级</el-checkbox-button>
                  <el-checkbox-button label="系统级">系统级</el-checkbox-button>
                  <el-checkbox-button label="体系级">体系级</el-checkbox-button>
                </el-checkbox-group>
              </div>
              <div class="fld">
                <div class="fld-l">指标任务类型</div>
                <el-checkbox-group v-model="skeletonInfo.taskTypes" class="cyber-checkbox-group highlight">
                  <el-checkbox-button label="效能评估">效能评估</el-checkbox-button>
                  <el-checkbox-button label="对比评估">对比评估</el-checkbox-button>
                  <el-checkbox-button label="性能测试">性能测试</el-checkbox-button>
                </el-checkbox-group>
              </div>
            </div>

            <!-- 评估准则 -->
            <div class="panel-section">
              <div class="section-title">评估准则</div>
              <div class="fld">
                <div class="fld-l">全局评分机制</div>
                <el-select v-model="skeletonInfo.scoreMethod" placeholder="请选择评分机制" class="cyber-select">
                  <el-option label="综合单一评分" value="综合单一评分" />
                  <el-option label="分维度独立评分" value="分维度独立评分" />
                  <el-option label="加权综合 + 维度诊断" value="加权综合 + 维度诊断" />
                </el-select>
              </div>
              <div class="fld">
                <div class="fld-l">全局默认汇聚算子 <span class="help-text">（各节点可覆盖）</span></div>
                <el-select v-model="skeletonInfo.globalAgg" class="cyber-select">
                  <el-option label="加权平均" value="加权平均" />
                  <el-option label="均值聚合" value="均值聚合" />
                  <el-option label="最大值聚合" value="最大值聚合" />
                </el-select>
              </div>
              <div class="fld">
                <div class="fld-l">全局默认权重策略</div>
                <el-select v-model="skeletonInfo.globalWeight" class="cyber-select">
                  <el-option label="AHP层次分析法" value="AHP层次分析法" />
                  <el-option label="等权重" value="等权重" />
                  <el-option label="熵权法" value="熵权法" />
                  <el-option label="专家打分" value="专家打分" />
                </el-select>
              </div>

              <div class="fld">
                <div class="fld-l">最低通过分值</div>
                <div class="flex-center-gap">
                  <el-input v-model="skeletonInfo.minPass" style="width:100px" placeholder="0.6" />
                  <span class="hint-text">低于此值输出警告</span>
                </div>
              </div>
            </div>

            <div class="panel-section stats-section">
              <div class="section-title">骨架统计信息</div>
              <div class="tactical-stats-grid">
                <div class="stat-item">
                  <div class="stat-value">{{ stats.mid }}</div>
                  <div class="stat-label">中间层</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value secondary">{{ stats.leaf }}</div>
                  <div class="stat-label">叶子层</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value accent">{{ stats.br }}</div>
                  <div class="stat-label">总分支</div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- CENTER PANEL -->
        <template #middle>
          <div class="metric-tree-container">
            <!-- Tactical Viewer Corners -->
            <div class="viewer-corner top-left"></div>
            <div class="viewer-corner top-right"></div>
            <div class="viewer-corner bottom-left"></div>
            <div class="viewer-corner bottom-right"></div>

            <div class="panel-header-tactical">
              <div class="header-left">
                <h3 class="panel-title">指标体系树 (Metric Hierarchy)</h3>
                <el-button-group class="cyber-button-group">
                  <el-button size="small" :type="isExpandAll ? 'primary' : ''" plain
                    @click="toggleAll(true)">展开</el-button>
                  <el-button size="small" :type="!isExpandAll ? 'primary' : ''" plain
                    @click="toggleAll(false)">折叠</el-button>
                </el-button-group>
              </div>
              <div class="header-right">
                <el-button type="primary" size="small" @click="openAddNode(null)">+ 添加根节点</el-button>
              </div>
            </div>

            <div class="tree-header-row">
              <div class="th-col th-name">节点 / 分支标识</div>
              <div class="th-col th-type">类型</div>
              <div class="th-col th-op">聚合算子 / 分支</div>
              <div class="th-col th-cond">激活逻辑</div>
              <div class="th-col th-actions">操作</div>
            </div>

            <div class="tree-body-viewport">
              <el-tree ref="treeRef" :data="treeData" node-key="id" :default-expand-all="true"
                :expand-on-click-node="false" empty-text="系统内暂无指标数据">
                <template #default="{ node, data }">
                  <div class="tactical-row"
                    :class="{ selected: selectedNodeId === data.id, 'status-hit': data._hit, 'status-miss': data._miss }"
                    @click="selRow(data.id)">
                    <!-- Indentation Lines -->
                    <div class="indent-guide" :style="{ width: (node.level - 1) * 20 + 'px' }">
                      <div class="guide-line" v-for="l in (node.level - 1)" :key="l"
                        :style="{ left: l * 20 - 10 + 'px' }"></div>
                    </div>

                    <!-- Toggle Arrow -->
                    <div class="row-toggle"
                      :class="{ collapsed: !node.expanded, 'is-leaf': !data.children || data.children.length === 0 }"
                      @click.stop="node.expanded = !node.expanded">
                      <el-icon v-if="data.children && data.children.length > 0">
                        <CaretBottom />
                      </el-icon>
                    </div>

                    <!-- Node Icon -->
                    <div class="node-icon" :class="data.type">
                      <span class="icon-char">{{ getIcoChar(data.type) }}</span>
                    </div>

                    <!-- Name & ID -->
                    <div class="node-main">
                      <span class="node-name">{{ data.name }}</span>
                      <span class="node-id" v-if="data.type === 'br'">#{{ data.id }}</span>
                    </div>

                    <!-- Type Badge -->
                    <div class="col-type">
                      <span class="type-tag" :class="data.type">{{ getTypeText(data.type) }}</span>
                    </div>

                    <!-- Operator / Branch Info -->
                    <div class="col-op">
                      <template v-if="data.type === 'mid'">
                        <span class="badge agg">{{ data.aggOp }}</span>
                        <span class="badge weight" :class="{ pass: data.wOp === '透传' }">{{ data.wOp }}</span>
                      </template>
                      <template v-else-if="data.type === 'leaf'">
                        <span class="branch-count">{{ data.children ? data.children.length : 0 }} 维度分支</span>
                      </template>
                      <template v-else-if="data.type === 'br'">
                        <span class="calc-text" :title="data.calc">{{ data.calc }}</span>
                        <span v-if="data.veto" class="veto-mini">V</span>
                      </template>
                    </div>

                    <!-- Condition Logic -->
                    <div class="col-cond">
                      <template v-if="data.type === 'mid'"><span class="auto-calc">自动级联</span></template>
                      <template v-else-if="data.type === 'leaf'"><span class="blank-dash">—</span></template>
                      <template v-else-if="data.type === 'br'">
                        <span v-if="!data.conds || data.conds.length === 0" class="cond-any">始终激活</span>
                        <div v-else class="cond-summary">
                          <span class="cond-tag">{{ data.conds[0].dim }}{{ data.conds[0].op }}{{ data.conds[0].val
                          }}</span>
                          <span v-if="data.conds.length > 1" class="cond-plus">+{{ data.conds.length - 1 }}</span>
                        </div>
                      </template>
                    </div>

                    <!-- Row Actions -->
                    <div class="row-actions">
                      <el-tooltip content="添加子项" placement="top" v-if="data.type !== 'br'">
                        <el-button link class="act-btn"
                          @click.stop="data.type === 'leaf' ? openAddBranch(data.id) : openAddNode(data.id)">
                          <el-icon>
                            <Plus />
                          </el-icon>
                        </el-button>
                      </el-tooltip>
                      <el-button link class="act-btn delete" @click.stop="deleteNode(data.id)">
                        <el-icon>
                          <Delete />
                        </el-icon>
                      </el-button>
                    </div>
                  </div>
                </template>
              </el-tree>
            </div>
          </div>
        </template>

        <!-- RIGHT PANEL -->
        <template #right>
          <div class="side-panel-inner">
            <div class="panel-header-tactical">
              <div class="header-left">
                <h3 class="panel-title">属性配置 (Attributes)</h3>
              </div>
              <div class="header-right" v-if="selectedNode">
                <span class="node-type-label" :class="selectedNode.type">{{ getTypeText(selectedNode.type) }}</span>
              </div>
            </div>

            <div class="panel-content rp-body">
              <div v-if="!selectedNode" class="empty-state">
                <div class="empty-icon">◈</div>
                <p>点击指标树节点<br>进行详细属性配置</p>
              </div>

              <!-- MID NODE CONFIG -->
              <template v-else-if="selectedNode.type === 'mid'">
                <div class="panel-section">
                  <div class="section-title">节点名称</div>
                  <el-input v-model="selectedNode.name" placeholder="输入节点名称" />
                </div>
                <div class="panel-section">
                  <div class="section-title">算子配置</div>
                  <div class="fld">
                    <div class="fld-l">汇聚算子</div>
                    <div class="cyber-radio-group mini">
                      <span v-for="op in ['加权平均', '均值聚合', '最大值聚合', '最小值聚合', '几何平均']" :key="op" class="cyber-radio-btn"
                        :class="{ active: selectedNode.aggOp === op, cyan: true }" @click="selectedNode.aggOp = op">{{
                          op }}</span>
                    </div>
                  </div>
                  <div class="fld" style="margin-top: 15px;">
                    <div class="fld-l">权重算子</div>
                    <div class="cyber-radio-group mini">
                      <span v-for="op in ['AHP权重', '等权重', '熵权法', '专家打分', '透传']" :key="op" class="cyber-radio-btn"
                        :class="{ active: selectedNode.wOp === op, purple: true }" @click="selectedNode.wOp = op">{{ op
                        }}</span>
                    </div>
                  </div>
                </div>
                <div class="panel-section">
                  <div class="section-title">关联子项</div>
                  <div class="child-node-list">
                    <div v-for="c in getChildren(selectedNode.id)" :key="c.id" class="child-node-card"
                      @click="selRow(c.id)">
                      <div class="child-info">
                        <span class="child-name">{{ c.name }}</span>
                        <span class="child-meta">{{ c.type === 'leaf' ? getChildren(c.id).length + ' 分支' : '中间层'
                        }}</span>
                      </div>
                      <el-icon>
                        <ArrowRight />
                      </el-icon>
                    </div>
                  </div>
                  <el-button block class="add-child-btn" @click="openAddNode(selectedNode.id)">+ 添加子节点</el-button>
                </div>
              </template>

              <!-- LEAF NODE CONFIG -->
              <template v-else-if="selectedNode.type === 'leaf'">
                <div class="panel-section">
                  <div class="section-title">泛化节点名称</div>
                  <el-input v-model="selectedNode.name" placeholder="输入分类名称" />
                  <div class="field-hint">此类节点作为容器，不直接参与计算，其下属命中的分支将直接出现在评估体系中。</div>
                </div>
                <div class="panel-section">
                  <div class="section-title">
                    下属分支列表
                    <span class="count-badge">{{ getChildren(selectedNode.id).length }}</span>
                  </div>
                  <div class="branch-list">
                    <div v-for="b in getChildren(selectedNode.id)" :key="b.id" class="branch-card"
                      @click="selRow(b.id)">
                      <div class="branch-top">
                        <span class="branch-name">{{ b.name }}</span>
                        <el-tag v-if="b.veto" size="small" type="danger" effect="dark" class="mini-veto">VETO</el-tag>
                      </div>
                      <div class="branch-meta">
                        <el-icon>
                          <Compass />
                        </el-icon> {{ b.norm }}
                      </div>
                      <div class="branch-conditions">
                        <span v-if="!b.conds || b.conds.length === 0" class="empty-cond">无激活条件</span>
                        <span v-else v-for="(c, i) in b.conds.slice(0, 2)" :key="i" class="cond-tag">{{ c.dim }}{{ c.op
                        }}{{ c.val }}</span>
                        <span v-if="b.conds && b.conds.length > 2" class="cond-more">...</span>
                      </div>
                    </div>
                  </div>
                  <div class="action-row-split">
                    <el-button block @click="openAddBranch(selectedNode.id)">+ 手动新增</el-button>
                    <el-button block type="success" plain @click="openAddBranch(selectedNode.id, 'lib')">
                      <el-icon>
                        <Collection />
                      </el-icon> 指标库导入
                    </el-button>
                  </div>
                </div>
              </template>

              <!-- BRANCH NODE CONFIG -->
              <template v-else-if="selectedNode.type === 'br'">
                <div class="panel-section">
                  <div class="section-title">所属上级容器</div>
                  <div class="parent-reference">
                    <el-icon>
                      <FolderOpened />
                    </el-icon>
                    {{ getNodeById(selectedNode.pid)?.name }}
                  </div>
                </div>
                <div class="panel-section">
                  <div class="section-title">指标名称 (具体分支)</div>
                  <el-input v-model="selectedNode.name" />
                </div>
                <div class="panel-section">
                  <div class="section-title">激活触发逻辑 (Activation Logic)</div>
                  <div class="logic-box">
                    <div v-if="!selectedNode.conds || selectedNode.conds.length === 0" class="logic-info">
                      <el-icon>
                        <CircleCheckFilled />
                      </el-icon> 默认激活：该指标始终参与当前任务评估
                    </div>
                    <div v-else class="condition-stack">
                      <div v-for="(c, i) in selectedNode.conds" :key="i" class="logic-row">
                        <span class="logic-op" v-if="i > 0">AND</span>
                        <div class="logic-bubble">
                          <span class="dim">{{ c.dim }}</span>
                          <span class="op">{{ c.op }}</span>
                          <span class="val">{{ c.val }}</span>
                          <el-icon class="close-icon" @click="removeCond(selectedNode, i)">
                            <Close />
                          </el-icon>
                        </div>
                      </div>
                    </div>
                    <el-button link type="primary" class="add-logic-btn" @click="addCondToNode(selectedNode)">
                      + 插入逻辑规则
                    </el-button>
                  </div>
                </div>
                <div class="panel-section">
                  <div class="section-title">数据采集与计算</div>
                  <div class="fld">
                    <div class="fld-l">原始数据源</div>
                    <div class="data-binding-bar" @click="ElMessage('数据源切换不可用')">
                      <div class="bind-status"></div>
                      <span class="bind-name">{{ selectedNode.data }}</span>
                      <el-icon class="chg-icon">
                        <Switch />
                      </el-icon>
                    </div>
                  </div>
                  <div class="fld">
                    <div class="fld-l">计算算子 (Calculator)</div>
                    <div class="data-binding-bar" @click="ElMessage('算子切换不可用')">
                      <el-icon class="math-icon">
                        <Cpu />
                      </el-icon>
                      <span class="bind-name">{{ selectedNode.calc }}</span>
                      <el-icon class="chg-icon">
                        <Switch />
                      </el-icon>
                    </div>
                  </div>
                  <div class="fld">
                    <div class="cyber-radio-group mini column">
                      <span v-for="op in ['线性归一化', '逆线性归一化', 'S型归一化', '比值归一化', '阶梯归一化']" :key="op"
                        class="cyber-radio-btn" :class="{ active: selectedNode.norm === op, green: true }"
                        @click="selectedNode.norm = op">{{ op
                        }}</span>
                    </div>
                  </div>
                </div>
                <div class="panel-section">
                  <div class="section-title">特殊处理规则</div>
                  <el-checkbox v-model="selectedNode.veto" class="veto-checkbox">
                    启用一票否决 (VETO MODE)
                  </el-checkbox>
                  <div v-if="selectedNode.veto" class="veto-config-box">
                    <div class="fld">
                      <div class="fld-l">否决阈值 (Threshold)</div>
                      <el-input-number v-model="selectedNode.vetoThr" :min="0" :max="1" :step="0.1" size="small" />
                    </div>
                    <div class="notice-bar warn">
                      警告：触发否决时，总评估分值将强制锁定为 <strong>0.00</strong>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </template>
      </SideLayout>
    </div>

    <!-- MODALS -->
    <!-- Add Node Modal -->
    <el-dialog v-model="addNodeDialog.visible" width="600px" class="tactical-modal" :show-close="false">
      <template #header>
        <div class="modal-header">
          <div class="header-left">
            <el-icon class="header-icon">
              <Plus />
            </el-icon>
            <span class="header-title">{{ addNodeDialog.pid ? `在「${getNodeById(addNodeDialog.pid)?.name}」下级添加新指标` :
              '创建顶级评估域' }}</span>
          </div>
          <el-button link class="modal-close" @click="addNodeDialog.visible = false"><el-icon>
              <Close />
            </el-icon></el-button>
        </div>
      </template>
      <div class="modal-body">
        <div class="fld">
          <div class="fld-l">拓扑层级类型</div>
          <div class="tactical-type-grid">
            <div class="type-option" :class="{ active: addNodeDialog.type === 'mid' }"
              @click="addNodeDialog.type = 'mid'">
              <span class="ico purple">◈</span>
              <div class="info">
                <div class="lbl">中间聚合层</div>
                <div class="sub">负责指标分组与权重聚合</div>
              </div>
            </div>
            <div class="type-option" :class="{ active: addNodeDialog.type === 'leaf' }"
              @click="addNodeDialog.type = 'leaf'">
              <span class="ico blue">◇</span>
              <div class="info">
                <div class="lbl">叶子分类层</div>
                <div class="sub">具体维度的评估指标容器</div>
              </div>
            </div>
          </div>
        </div>
        <div class="fld">
          <div class="fld-l"><span class="rq">*</span>指标/领域名称</div>
          <el-input v-model="addNodeDialog.name" placeholder="例如：干扰引导精度 / 侦察范围强度" />
        </div>
        <div v-show="addNodeDialog.type === 'mid'" class="expanded-config">
          <div class="fld">
            <div class="fld-l">默认汇聚算子</div>
            <div class="cyber-radio-group mini">
              <span v-for="op in ['加权平均', '均值聚合', '最大值聚合', '最小值聚合']" :key="op" class="cyber-radio-btn"
                :class="{ active: addNodeDialog.aggOp === op, cyan: true }" @click="addNodeDialog.aggOp = op">{{ op
                }}</span>
            </div>
          </div>
          <div class="fld" style="margin-top: 15px;">
            <div class="fld-l">默认权重策略</div>
            <div class="cyber-radio-group mini">
              <span v-for="op in ['AHP权重', '等权重', '熵权法', '专家打分', '透传']" :key="op" class="cyber-radio-btn"
                :class="{ active: addNodeDialog.wOp === op, purple: true }" @click="addNodeDialog.wOp = op">{{ op
                }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="modal-footer">
          <el-button @click="addNodeDialog.visible = false">取消操作</el-button>
          <el-button type="primary" class="glow-btn" @click="confirmAddNode">确认添加至指标树</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Add Branch Modal -->
    <el-dialog v-model="addBranchDialog.visible" width="800px" class="tactical-modal" :show-close="false">
      <template #header>
        <div class="modal-header">
          <div class="header-left">
            <el-icon class="header-icon">
              <Connection />
            </el-icon>
            <span class="header-title">配置叶子分支 (Branch Configuration)</span>
          </div>
          <el-button link class="modal-close" @click="addBranchDialog.visible = false"><el-icon>
              <Close />
            </el-icon></el-button>
        </div>
      </template>
      <div class="modal-body no-padding">
        <div class="modal-tabs-header">
          <div class="tab-item" :class="{ active: addBranchDialog.tab === 'manual' }"
            @click="addBranchDialog.tab = 'manual'">
            <el-icon>
              <EditPen />
            </el-icon> 自定义录入
          </div>
          <div class="tab-item" :class="{ active: addBranchDialog.tab === 'lib' }" @click="addBranchDialog.tab = 'lib'">
            <el-icon>
              <Management />
            </el-icon> 从评估指标库同步
          </div>
        </div>

        <div class="tab-viewport">
          <!-- Manual -->
          <div v-show="addBranchDialog.tab === 'manual'" class="tab-pane">
            <div class="fld">
              <div class="fld-l"><span class="rq">*</span>分支名称 (具体评估指标)</div>
              <el-input v-model="addBranchDialog.manual.name" placeholder="如：压制干扰频率覆盖率" />
            </div>
            <div class="fld">
              <div class="fld-l">激活逻辑配置 (无配置则始终激活)</div>
              <div class="condition-builder">
                <div v-for="(c, index) in addBranchDialog.manual.conds" :key="index" class="builder-row">
                  <el-select v-model="c.dim" placeholder="维度" style="width: 140px">
                    <el-option label="任务类型" value="任务类型" /><el-option label="评估层级" value="评估层级" />
                    <el-option label="场景环境" value="场景" /><el-option label="装备型号" value="装备型号" />
                  </el-select>
                  <el-select v-model="c.op" placeholder="算子" style="width: 100px">
                    <el-option label="=" value="=" /><el-option label="包含" value="包含" /><el-option label="!="
                      value="!=" />
                  </el-select>
                  <el-input v-model="c.val" placeholder="匹配值" style="flex: 1" />
                  <el-button link type="danger" @click="addBranchDialog.manual.conds.splice(index, 1)">
                    <el-icon>
                      <CloseBold />
                    </el-icon>
                  </el-button>
                </div>
                <el-button link type="primary"
                  @click="addBranchDialog.manual.conds.push({ dim: '任务类型', op: '=', val: '' })">
                  + 添加触发条件
                </el-button>
              </div>
            </div>
            <div class="grid-2-col">
              <div class="fld">
                <div class="fld-l"><span class="rq">*</span>底层数据源</div>
                <el-select v-model="addBranchDialog.manual.data" style="width: 100%">
                  <el-option v-for="d in ['宽带信号采集数据', '频谱扫描数据', '雷达回波数据', '信号强度测量数据']" :key="d" :label="d" :value="d" />
                </el-select>
              </div>
              <div class="fld">
                <div class="fld-l"><span class="rq">*</span>主要计算算子</div>
                <el-select v-model="addBranchDialog.manual.calc" style="width: 100%">
                  <el-option v-for="c in ['覆盖带宽比值算子', '频段完整度算子', '虚假目标计数算子', '干信比计算算子']" :key="c" :label="c"
                    :value="c" />
                </el-select>
              </div>
            </div>
            <div class="fld">
              <div class="fld-l"><span class="rq">*</span>归一化算法策略</div>
              <div class="cyber-radio-group mini">
                <span v-for="op in ['线性归一化', '逆线性归一化', 'S型归一化', '比值归一化', '阶梯归一化']" :key="op" class="cyber-radio-btn"
                  :class="{ active: addBranchDialog.manual.norm === op, green: true }"
                  @click="addBranchDialog.manual.norm = op">{{ op }}</span>
              </div>
            </div>
            <div class="fld">
              <el-checkbox v-model="addBranchDialog.manual.veto">开启一票否决模式 (Critical Metric)</el-checkbox>
            </div>
          </div>

          <!-- Library -->
          <div v-show="addBranchDialog.tab === 'lib'" class="tab-pane">
            <div class="library-filter-bar">
              <div class="cat-tags">
                <span v-for="cat in ['all', '干扰效能', '侦察能力', '响应时序', '对抗效果']" :key="cat" class="cat-tag"
                  :class="{ active: addBranchDialog.lib.cat === cat }" @click="addBranchDialog.lib.cat = cat">{{ cat ===
                    'all' ? '全部领域' : cat }}</span>
              </div>
              <el-input v-model="addBranchDialog.lib.search" placeholder="检索系统预置指标..." class="lib-search-input">
                <template #prefix><el-icon>
                    <Search />
                  </el-icon></template>
              </el-input>
            </div>
            <div class="library-grid-container">
              <div v-for="item in filteredLibItems" :key="item.name" class="library-card"
                :class="{ selected: item._picked }" @click="item._picked = !item._picked">
                <div class="card-head">
                  <span class="metric-name">{{ item.name }}</span>
                  <div class="check-box" :class="{ active: item._picked }">
                    <el-icon v-if="item._picked">
                      <Check />
                    </el-icon>
                  </div>
                </div>
                <div class="card-meta">
                  <span class="lib-badge">{{ item.cat }}</span>
                  <span class="lib-calc">{{ item.calc }}</span>
                </div>
              </div>
            </div>
            <div class="library-summary">
              当前已选择 <strong class="highlight">{{ selectedLibCount }}</strong> 项指标分支
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="modal-footer">
          <el-button @click="addBranchDialog.visible = false">放弃修改</el-button>
          <el-button type="primary" class="glow-btn" @click="confirmAddBranch">保存并接入指标树</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Simulate Modal -->
    <el-dialog v-model="simDialog.visible" width="750px" class="tactical-modal" :show-close="false">
      <template #header>
        <div class="modal-header">
          <div class="header-left">
            <el-icon class="header-icon">
              <Cpu />
            </el-icon>
            <span class="header-title">骨架任务生成模拟 (Skeleton Generation Simulator)</span>
          </div>
          <el-button link class="modal-close" @click="simDialog.visible = false"><el-icon>
              <Close />
            </el-icon></el-button>
        </div>
      </template>
      <div class="modal-body">
        <div class="simulator-guide">
          输入测试任务的环境属性，系统将根据骨架配置的激活逻辑，预测并生成最终应用于该任务的指标树结构。
        </div>

        <div class="simulator-controls grid-4-col">
          <div class="sim-fld">
            <div class="fld-l">任务类型</div>
            <el-select v-model="simDialog.attrs['任务类型']" placeholder="不限" clearable>
              <el-option label="效能评估" value="效能评估" /><el-option label="性能测试" value="性能测试" /><el-option label="对比评估"
                value="对比评估" />
            </el-select>
          </div>
          <div class="sim-fld">
            <div class="fld-l">评估层级</div>
            <el-select v-model="simDialog.attrs['评估层级']" placeholder="不限" clearable>
              <el-option label="设备级" value="设备级" /><el-option label="系统级" value="系统级" /><el-option label="体系级"
                value="体系级" />
            </el-select>
          </div>
          <div class="sim-fld">
            <div class="fld-l">场景环境</div>
            <el-select v-model="simDialog.attrs['场景']" placeholder="不限" clearable>
              <el-option label="跳频信号" value="跳频信号" /><el-option label="宽频谱覆盖" value="宽频谱覆盖" /><el-option label="复杂电磁环境"
                value="复杂电磁环境" />
            </el-select>
          </div>
          <div class="sim-fld">
            <div class="fld-l">装备类型</div>
            <el-select v-model="simDialog.attrs['装备型号']" placeholder="不限" clearable>
              <el-option label="欺骗干扰型" value="欺骗干扰型" /><el-option label="压制干扰型" value="压制干扰型" /><el-option
                label="侦察干扰一体化" value="侦察干扰一体化" />
            </el-select>
          </div>
        </div>

        <el-button block type="primary" class="glow-btn simulate-trigger" @click="runSim">
          <el-icon style="margin-right: 8px">
            <VideoPlay />
          </el-icon> 执行逻辑演算
        </el-button>

        <div v-if="simDialog.hasRun" class="simulation-results-viewport">
          <div class="sim-section">
            <div class="section-title flex-between">
              <span>分支命中心电图</span>
              <span class="hit-summary">命中: <strong class="green">{{ simDialog.hitCount }}</strong> / 总计: {{ stats.br
              }}</span>
            </div>
            <div class="hit-map">
              <div v-for="b in branchNodes" :key="b.id" class="hit-indicator-node" :class="{ hit: b._hit }">
                <div class="hit-status-dot"></div>
                <span class="hit-name">{{ b.name }}</span>
                <span class="hit-logic">{{(!b.conds || b.conds.length === 0) ? 'ALWAYS' : b.conds.map(c =>
                  `${c.dim}${c.op}${c.val}`).join(' & ')}}</span>
              </div>
            </div>
          </div>

          <div class="sim-grid-split">
            <div class="sim-section" v-if="vetosHit.length > 0">
              <div class="section-title danger">一票否决(VETO)触发概览</div>
              <div class="veto-list">
                <div v-for="v in vetosHit" :key="v.id" class="veto-hit-item">
                  <el-icon>
                    <WarningFilled />
                  </el-icon>
                  <span class="veto-name">{{ v.name }}</span>
                  <span class="veto-desc">触发：整体评估结果将强制归零</span>
                </div>
              </div>
            </div>

            <div class="sim-section">
              <div class="section-title">演算生成的拓扑结构 (Tree Trace)</div>
              <div class="tree-trace-code" v-html="simTreeText"></div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="modal-footer">
          <el-button @click="simDialog.visible = false">退出分析</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import SideLayout from '@/layout/SideLayout.vue'

// --- State ---
const skeletonInfo = reactive({
  name: '通信对抗装备效能评估骨架',
  version: 'v1.0.0',
  desc: '适用于通信对抗装备的干扰效果、侦察能力、响应时序综合效能评估',
  domains: ['电子对抗', '通信对抗'],
  eqTypes: ['有源干扰机', '侦察干扰一体化'],
  levels: ['设备级', '系统级'],
  taskTypes: ['效能评估', '对比评估'],
  scoreMethod: '综合单一评分',
  globalAgg: '加权平均',
  globalWeight: 'AHP层次分析法',
  minPass: '0.60',
  vetos: [
    { name: '全链路端到端响应时延', thr: '0.3' }
  ]
})

// Flat list of nodes
const nodesList = ref([
  { id: 'root', pid: null, type: 'mid', name: '通信对抗综合效能', aggOp: '加权平均', wOp: 'AHP权重' },
  { id: 'n1', pid: 'root', type: 'mid', name: '干扰效能', aggOp: '加权平均', wOp: '等权重' },
  { id: 'n1a', pid: 'n1', type: 'leaf', name: '干扰覆盖性' },
  { id: 'b1', pid: 'n1a', type: 'br', name: '压制干扰覆盖率', conds: [{ dim: '任务类型', op: '=', val: '效能评估' }], data: '宽带信号采集数据', calc: '覆盖带宽比值算子', norm: '线性归一化', veto: false },
  { id: 'b2', pid: 'n1a', type: 'br', name: '频段覆盖完整度', conds: [{ dim: '任务类型', op: '=', val: '效能评估' }, { dim: '场景', op: '包含', val: '宽频谱覆盖' }], data: '频谱扫描数据', calc: '频段完整度算子', norm: '线性归一化', veto: false },
  { id: 'b3', pid: 'n1a', type: 'br', name: '欺骗目标数量', conds: [{ dim: '装备型号', op: '包含', val: '欺骗干扰型' }], data: '雷达回波数据', calc: '虚假目标计数算子', norm: '比值归一化', veto: false },
  { id: 'n1b', pid: 'n1', type: 'leaf', name: '干扰功率效能' },
  { id: 'b4', pid: 'n1b', type: 'br', name: '干信比 J/S', conds: [], data: '信号强度测量数据', calc: '干信比计算算子', norm: 'S型归一化', veto: false },
  { id: 'b5', pid: 'n1b', type: 'br', name: '有效干扰功率占比', conds: [{ dim: '评估层级', op: '=', val: '系统级' }], data: '功率监测数据', calc: '功率比值算子', norm: '线性归一化', veto: false },
  { id: 'n2', pid: 'root', type: 'mid', name: '侦察能力', aggOp: '均值聚合', wOp: '等权重' },
  { id: 'n2a', pid: 'n2', type: 'leaf', name: '截获能力' },
  { id: 'b6', pid: 'n2a', type: 'br', name: '信号截获率', conds: [{ dim: '任务类型', op: '=', val: '效能评估' }], data: '宽带采集数据', calc: '截获率统计算子', norm: '线性归一化', veto: false },
  { id: 'b7', pid: 'n2a', type: 'br', name: '跳频截获成功率', conds: [{ dim: '场景', op: '包含', val: '跳频信号' }], data: '跳频专项采集数据', calc: '跳频截获算子', norm: '线性归一化', veto: false },
  { id: 'n2b', pid: 'n2', type: 'leaf', name: '测向精度' },
  { id: 'b8', pid: 'n2b', type: 'br', name: '测向均方根误差', conds: [{ dim: '评估层级', op: '=', val: '设备级' }], data: '测向原始数据', calc: '均方根误差算子', norm: '逆线性归一化', veto: false },
  { id: 'b9', pid: 'n2b', type: 'br', name: '多站融合测向精度', conds: [{ dim: '评估层级', op: '=', val: '系统级' }], data: '多站融合数据', calc: '融合精度算子', norm: '逆线性归一化', veto: false },
  { id: 'n3', pid: 'root', type: 'mid', name: '响应时序', aggOp: '最小值聚合', wOp: '透传' },
  { id: 'n3a', pid: 'n3', type: 'leaf', name: '时延指标' },
  { id: 'b10', pid: 'n3a', type: 'br', name: '目指接收时延', conds: [], data: '目标指示报文数据', calc: '报文时延算子', norm: '逆线性归一化', veto: false },
  { id: 'b11', pid: 'n3a', type: 'br', name: '干扰响应时延', conds: [], data: '干扰触发时序数据', calc: '响应时延算子', norm: '逆线性归一化', veto: false },
  { id: 'b12', pid: 'n3a', type: 'br', name: '全链路端到端响应时延', conds: [{ dim: '评估层级', op: '=', val: '系统级' }], data: '全链路时序数据', calc: '端到端时延算子', norm: '逆线性归一化', veto: true },
])

// 24 mock items for library import to meet requirements
const libItemsRaw = ref([
  { name: '压制干扰覆盖率', cat: '干扰效能', calc: '覆盖带宽比值算子', data: '宽带信号采集数据', norm: '线性归一化' },
  { name: '频段覆盖完整度', cat: '干扰效能', calc: '频段完整度算子', data: '频谱扫描数据', norm: '线性归一化' },
  { name: '欺骗目标数量', cat: '干扰效能', calc: '虚假目标计数算子', data: '雷达回波数据', norm: '比值归一化' },
  { name: '干信比 J/S', cat: '干扰效能', calc: '干信比计算算子', data: '信号强度测量数据', norm: 'S型归一化' },
  { name: '有效干扰功率占比', cat: '干扰效能', calc: '功率比值算子', data: '功率监测数据', norm: '线性归一化' },
  { name: '干扰建立有效时延', cat: '干扰效能', calc: '时延统计算子', data: '干扰触发时序数据', norm: '逆线性归一化' },
  { name: '全频谱压制效率', cat: '干扰效能', calc: '宽带压制比算子', data: '宽带对消数据', norm: '线性归一化' },
  { name: '精准引导干扰成功率', cat: '干扰效能', calc: '锁定命中率算子', data: '目标锁定日志数据', norm: '线性归一化' },
  { name: '目标误判率', cat: '干扰效能', calc: '误判分类统计', data: '跟踪确认数据', norm: '逆线性归一化' },
  { name: '多目标同时干扰能力', cat: '干扰效能', calc: '多目标数量结算', data: '并发处理能力数据', norm: '阶梯归一化' },

  { name: '信号截获率', cat: '侦察能力', calc: '截获率统计算子', data: '宽带采集数据', norm: '线性归一化' },
  { name: '跳频截获成功率', cat: '侦察能力', calc: '跳频截获算子', data: '跳频专项采集数据', norm: '线性归一化' },
  { name: '信号识别率', cat: '侦察能力', calc: '识别率统计算子', data: '信号特征数据', norm: '线性归一化' },
  { name: '测向均方根误差', cat: '侦察能力', calc: '均方根误差算子', data: '测向原始数据', norm: '逆线性归一化' },
  { name: '多站融合测向精度', cat: '侦察能力', calc: '融合精度算子', data: '多站融合数据', norm: '逆线性归一化' },
  { name: '极弱信号发现概率', cat: '侦察能力', calc: '信噪比检测算子', data: '底噪提取数据', norm: 'S型归一化' },
  { name: '未知调制方式识别率', cat: '侦察能力', calc: '未知分类模型算子', data: '原始IQ数据', norm: '线性归一化' },

  { name: '目指接收时延', cat: '响应时序', calc: '报文时延算子', data: '目标指示报文数据', norm: '逆线性归一化' },
  { name: '干扰响应时延', cat: '响应时序', calc: '响应时延算子', data: '干扰触发时序数据', norm: '逆线性归一化' },
  { name: '全链路端到端响应时延', cat: '响应时序', calc: '端到端时延算子', data: '全链路时序数据', norm: '逆线性归一化' },
  { name: '跳频响应时延', cat: '响应时序', calc: '跳频时延算子', data: '跳频时序数据', norm: '逆线性归一化' },
  { name: '系统冷启动就绪时延', cat: '响应时序', calc: '启动时序列分析', data: '系统日志数据', norm: '逆线性归一化' },

  { name: '干扰空窗期占比', cat: '对抗效果', calc: '空窗期统计算子', data: '干扰时序数据', norm: '逆线性归一化' },
  { name: '干扰成功率', cat: '对抗效果', calc: '干扰成功率算子', data: '对抗结果数据', norm: '线性归一化' },
  { name: '频率占空比', cat: '对抗效果', calc: '占空比计算算子', data: '频率使用数据', norm: '线性归一化' }
].map(item => ({ ...item, _picked: false })))

const selectedNodeId = ref(null)
const isExpandAll = ref(true)
const treeRef = ref(null)

let idCounter = 100
const getUid = () => 'n' + (++idCounter)

// --- Computed ---
const selectedNode = computed(() => nodesList.value.find(n => n.id === selectedNodeId.value) || null)
const branchNodes = computed(() => nodesList.value.filter(n => n.type === 'br'))

const stats = computed(() => {
  return {
    mid: nodesList.value.filter(n => n.type === 'mid').length,
    leaf: nodesList.value.filter(n => n.type === 'leaf').length,
    br: nodesList.value.filter(n => n.type === 'br').length
  }
})

// Build nested tree for el-tree
const treeData = computed(() => {
  const map = {}
  const list = []
  nodesList.value.forEach(item => {
    map[item.id] = { ...item, children: [] }
  })
  nodesList.value.forEach(item => {
    if (item.pid && map[item.pid]) {
      map[item.pid].children.push(map[item.id])
    } else {
      list.push(map[item.id])
    }
  })
  return list
})

// --- Dialogs ---
const addNodeDialog = reactive({ visible: false, pid: null, type: 'mid', name: '', aggOp: '加权平均', wOp: 'AHP权重' })
const addBranchDialog = reactive({
  visible: false, pid: null, tab: 'manual',
  manual: { name: '', conds: [], data: '宽带信号采集数据', calc: '覆盖带宽比值算子', norm: '线性归一化', veto: false },
  lib: { cat: 'all', search: '' }
})
const simDialog = reactive({ visible: false, hasRun: false, attrs: { '任务类型': '', '评估层级': '', '场景': '', '装备型号': '' }, hitCount: 0 })

// --- Helper Functions ---
const getNodeById = (id) => nodesList.value.find(n => n.id === id)
const getChildren = (pid) => nodesList.value.filter(n => n.pid === pid)

const toggleTag = (category, val) => {
  const arr = skeletonInfo[category]
  if (arr.includes(val)) arr.splice(arr.indexOf(val), 1)
  else arr.push(val)
}

const addVetoRow = () => { skeletonInfo.vetos.push({ name: branchNodes.value[0]?.name || '', thr: '0.3' }) }
const removeVetoRow = (index) => { skeletonInfo.vetos.splice(index, 1) }
const addCondToNode = (node) => {
  if (!node.conds) node.conds = []
  node.conds.push({ dim: '任务类型', op: '=', val: '' })
}
const removeCond = (node, i) => { node.conds.splice(i, 1) }

// --- Tree UI formatting ---
const getIcoClass = (type) => ({ 'mid': 'ico-mid', 'leaf': 'ico-lf', 'br': 'ico-br' }[type])
const getIcoChar = (type) => ({ 'mid': '◈', 'leaf': '◇', 'br': '•' }[type])
const getTypeBadge = (type) => ({ 'mid': 'tb-mid', 'leaf': 'tb-lf', 'br': 'tb-br' }[type])
const getTypeText = (type) => ({ 'mid': '中间层', 'leaf': '叶子层', 'br': '分支' }[type])

const toggleAll = (exp) => {
  isExpandAll.value = exp
  // Using el-tree store methods requires slightly hacky access, or just re-assigning data keys.
  // We're letting el-tree handle it automatically based on default-expand-all property via remount if necessary,
  // but it's simpler to traverse the actual store nodes.
  if (treeRef.value) {
    const nodesMap = treeRef.value.store.nodesMap;
    for (let i in nodesMap) {
      nodesMap[i].expanded = exp;
    }
  }
}

const selRow = (id) => { selectedNodeId.value = id }

const deleteNode = (id) => {
  const toDelete = [id]
  const findDescendants = (pid) => {
    nodesList.value.filter(n => n.pid === pid).forEach(child => {
      toDelete.push(child.id)
      findDescendants(child.id)
    })
  }
  findDescendants(id)
  nodesList.value = nodesList.value.filter(n => !toDelete.includes(n.id))
  if (toDelete.includes(selectedNodeId.value)) selectedNodeId.value = null
  ElMessage.success('已删除节点')
}

// --- Add Node Handlers ---
const openAddNode = (pid) => {
  addNodeDialog.pid = pid
  addNodeDialog.name = ''
  addNodeDialog.type = 'mid'
  addNodeDialog.aggOp = '加权平均'
  addNodeDialog.wOp = 'AHP权重'
  addNodeDialog.visible = true
}

const confirmAddNode = () => {
  if (!addNodeDialog.name.trim()) return ElMessage.warning('请填写节点名称')
  const newNode = {
    id: getUid(),
    pid: addNodeDialog.pid,
    type: addNodeDialog.type,
    name: addNodeDialog.name.trim()
  }
  if (addNodeDialog.type === 'mid') {
    newNode.aggOp = addNodeDialog.aggOp
    newNode.wOp = addNodeDialog.wOp
  }

  // Try to insert after the last sibling, or at end
  const siblings = nodesList.value.filter(n => n.pid === addNodeDialog.pid)
  const lastSibling = siblings[siblings.length - 1]
  if (lastSibling) {
    const lastIdx = nodesList.value.findIndex(n => n.id === lastSibling.id)
    nodesList.value.splice(lastIdx + 1, 0, newNode)
  } else {
    nodesList.value.push(newNode)
  }

  addNodeDialog.visible = false
  selRow(newNode.id)
  ElMessage.success('添加成功')
}

// --- Add Branch Handlers ---
const openAddBranch = (pid, tab = 'manual') => {
  addBranchDialog.pid = pid
  addBranchDialog.tab = tab
  addBranchDialog.manual = { name: '', conds: [], data: '宽带信号采集数据', calc: '覆盖带宽比值算子', norm: '线性归一化', veto: false }
  libItemsRaw.value.forEach(i => i._picked = false)
  addBranchDialog.lib.search = ''
  addBranchDialog.visible = true
}

const filteredLibItems = computed(() => {
  return libItemsRaw.value.filter(i => {
    const matchCat = addBranchDialog.lib.cat === 'all' || i.cat === addBranchDialog.lib.cat
    const matchSearch = i.name.toLowerCase().includes(addBranchDialog.lib.search.toLowerCase())
    return matchCat && matchSearch
  })
})

const selectedLibCount = computed(() => libItemsRaw.value.filter(i => i._picked).length)

const confirmAddBranch = () => {
  if (addBranchDialog.tab === 'manual') {
    if (!addBranchDialog.manual.name.trim()) return ElMessage.warning('请填写分支名称')
    nodesList.value.push({
      id: getUid(),
      pid: addBranchDialog.pid,
      type: 'br',
      name: addBranchDialog.manual.name.trim(),
      conds: [...addBranchDialog.manual.conds],
      data: addBranchDialog.manual.data,
      calc: addBranchDialog.manual.calc,
      norm: addBranchDialog.manual.norm,
      veto: addBranchDialog.manual.veto
    })
  } else {
    const picked = libItemsRaw.value.filter(i => i._picked)
    if (picked.length === 0) return ElMessage.warning('请选择至少一个指标')
    picked.forEach(item => {
      nodesList.value.push({
        id: getUid(),
        pid: addBranchDialog.pid,
        type: 'br',
        name: item.name,
        conds: [],
        data: item.data,
        calc: item.calc,
        norm: item.norm,
        veto: false
      })
    })
  }
  addBranchDialog.visible = false
  selRow(addBranchDialog.pid)
  ElMessage.success('添加成功')
}

// --- Simulate Logic ---
const showSim = () => { simDialog.visible = true; simDialog.hasRun = false }
const saveDraft = () => { ElMessage.success('草稿已保存') }

const evalCond = (conds, attrs) => {
  if (!conds || conds.length === 0) return true
  return conds.every(c => {
    const av = attrs[c.dim] || ''
    if (c.op === '=') return av === c.val
    if (c.op === '包含') return av.includes(c.val) || c.val.includes(av)
    if (c.op === '!=') return av !== c.val
    return false
  })
}

// Sim tree text rendering
const simTreeText = ref('')
const vetosHit = ref([])

const buildSimTreeText = (nodeId, attrs, depth = 0) => {
  const n = getNodeById(nodeId)
  if (!n) return ''
  const pfx = '  '.repeat(depth)

  if (n.type === 'leaf') {
    const matched = getChildren(nodeId).filter(b => b._hit)
    if (matched.length === 0) return ''
    return matched.map(b => `${pfx}● ${b.name}  <span style="color:#5a6a85">${b.data}</span>`).join('\n')
  }

  const childrenTexts = getChildren(nodeId).map(c => buildSimTreeText(c.id, attrs, depth + 1)).filter(Boolean)
  if (childrenTexts.length === 0) return ''

  const ico = depth === 0 ? '◈' : '◈'
  const color = depth === 0 ? '#eef3fb' : '#dde4f2'
  const head = `${pfx}${ico} <span style="color:${color}">${n.name}</span>  <span style="color:#5a6a85;font-size:10px">[${n.aggOp || ''}]</span>`
  return head + '\n' + childrenTexts.join('\n')
}

const runSim = () => {
  let hitCt = 0
  vetosHit.value = []

  nodesList.value.forEach(n => {
    if (n.type === 'br') {
      const ok = evalCond(n.conds, simDialog.attrs)
      n._hit = ok
      n._miss = !ok
      if (ok) {
        hitCt++
        if (n.veto) vetosHit.value.push(n)
      }
    } else {
      n._hit = false
      n._miss = false
    }
  })

  simDialog.hitCount = hitCt

  const root = nodesList.value.find(n => !n.pid)
  if (root) {
    const t = buildSimTreeText(root.id, simDialog.attrs)
    simTreeText.value = t || '<span style="color:#3c4b62">无任何分支命中，指标树为空</span>'
  }

  simDialog.hasRun = true
}

</script>

<style lang="scss" scoped>
@import "@/styles/variables.scss";

$b0: #0a111a;
$b1: #0d1624;
$b2: #121d2f;
$b3: #1a283e;
$b4: #24344d;
$bd: rgba(52, 120, 240, 0.15);
$bd-high: rgba(52, 120, 240, 0.3);
$wh: #e5e9ef;
$t1: #c5d1e3;
$t2: #a0adc3;
$t3: #7e8da8;
$t4: #5a6a85;
$bl: #3478f0;
$bl-glow: rgba(52, 120, 240, 0.4);
$cy: #08a8c5;
$pp: #9855e0;
$gn: #0faa78;
$am: #e89a08;
$rd: #e04040;

.manual-build-page {
  height: 100%;
  background: $b0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  min-height: 0;
}

// Side Layout Overrides
:deep(.side-layout) {
  .left-side {
    border-right: 1px solid $bd;
  }

  .right-side {
    border-left: 1px solid $bd;
  }
}

.side-panel-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
  background: $b0;
  overflow-y: auto;

}

.panel-section {
  padding: 16px;
  border-bottom: 1px solid $bd;

  &:last-child {
    border-bottom: none;
  }

  .section-title {
    font-size: 11px;
    font-weight: 700;
    color: $t4;
    text-transform: uppercase;
    letter-spacing: 1.5px;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .count-badge {
      background: $b3;
      color: $t2;
      padding: 0 6px;
      border-radius: 10px;
      font-size: 10px;
      font-family: "Courier New", monospace;
    }
  }
}

.fld {
  margin-bottom: 14px;

  .fld-l {
    font-size: 11px;
    color: $t3;
    margin-bottom: 6px;
    font-weight: 500;

    .rq {
      color: $rd;
      margin-right: 2px;
    }
  }

  .help-text {
    font-size: 10px;
    color: $t4;
    font-weight: 400;
    margin-left: 4px;
  }
}

// Tactical Header Component
.tactical-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(90deg, #0d1624 0%, rgba(13, 22, 36, 0.4) 100%);
  border-bottom: 1px solid $bd;
  flex-shrink: 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  z-index: 100;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .divider {
      width: 3px;
      height: 20px;
      background: $bl;
      box-shadow: 0 0 8px $bl;
    }

    .project-title {
      font-size: 16px;
      font-weight: 700;
      color: #fff;
      margin: 0;
      letter-spacing: 0.5px;
    }
  }

  .header-right {
    .action-buttons {
      display: flex;
      gap: 12px;
    }
  }
}

// Sub Panel Header Component
.panel-header-tactical {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: linear-gradient(90deg, #0d1624 0%, rgba(13, 22, 36, 0.4) 100%);
  border-bottom: 1px solid $bd;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .panel-title {
    font-size: 13px;
    font-weight: 600;
    color: $wh;
    margin: 0;
  }
}

// Cyber Elements
.cyber-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  .cyber-tag {
    padding: 3px 10px;
    font-size: 11px;
    border-radius: 4px;
    background: $b2;
    border: 1px solid $bd;
    color: $t3;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: $t4;
      color: $t2;
    }

    &.active {
      border-color: $bl;
      color: $bl;
      background: rgba(52, 120, 240, 0.08);
      box-shadow: 0 0 10px rgba(52, 120, 240, 0.15);
    }

    &.highlight.active {
      border-color: $cy;
      color: $cy;
      background: rgba(8, 168, 197, 0.08);
    }
  }
}

.tactical-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;

  .stat-item {
    background: $b2;
    border: 1px solid $bd;
    border-radius: 4px;
    padding: 10px;
    text-align: center;
    position: relative;
    overflow: hidden;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 2px;
      height: 100%;
      background: $bl;
      opacity: .4;
    }

    .stat-value {
      font-size: 20px;
      font-weight: 800;
      color: $wh;
      font-family: "Courier New", monospace;
      line-height: 1;

      &.secondary {
        color: $cy;
      }

      &.accent {
        color: $pp;
      }
    }

    .stat-label {
      font-size: 10px;
      color: $t4;
      margin-top: 4px;
      text-transform: uppercase;
    }
  }
}

// Metric Tree Component
.metric-tree-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #090e16;
  position: relative;

  .viewer-corner {
    position: absolute;
    width: 10px;
    height: 10px;
    border: 1px solid rgba(52, 120, 240, 0.3);
    z-index: 5;

    &.top-left {
      top: 4px;
      left: 4px;
      border-right: none;
      border-bottom: none;
    }

    &.top-right {
      top: 4px;
      right: 4px;
      border-left: none;
      border-bottom: none;
    }

    &.bottom-left {
      bottom: 4px;
      left: 4px;
      border-right: none;
      border-top: none;
    }

    &.bottom-right {
      bottom: 4px;
      right: 4px;
      border-left: none;
      border-top: none;
    }
  }
}

.tree-header-row {
  display: flex;
  height: 32px;
  background: #0c1521;
  border-bottom: 1px solid $bd;
  align-items: center;
  font-size: 10px;
  font-weight: 700;
  color: $t4;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding-right: 12px;

  .th-col {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .th-name {
    flex: 1;
    justify-content: flex-start;
    padding-left: 20px;
  }

  .th-type {
    width: 80px;
  }

  .th-op {
    width: 140px;
  }

  .th-cond {
    width: 160px;
  }

  .th-actions {
    width: 80px;
  }
}

.tree-body-viewport {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 40px;

  :deep(.el-tree) {
    background: transparent;

    .el-tree-node__content {
      height: auto !important;
      padding: 0 !important;
      background: transparent !important;

      &:hover {
        background: transparent !important;
      }
    }

    .el-tree-node:focus>.el-tree-node__content {
      background: transparent !important;
    }
  }
}

.tactical-row {
  display: flex;
  align-items: center;
  height: 36px;
  width: 100%;
  border-bottom: 1px solid rgba(52, 120, 240, 0.05);
  transition: all 0.2s;
  cursor: default;
  position: relative;

  &:hover {
    background: rgba(52, 120, 240, 0.06);

    .row-actions {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &.selected {
    background: rgba(52, 120, 240, 0.12);

    &::after {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 2px;
      background: $bl;
      box-shadow: 0 0 8px $bl;
    }
  }

  &.status-hit {
    background: rgba(15, 170, 120, 0.08);

    .node-name {
      color: $gn;
    }
  }

  &.status-miss {
    opacity: 0.4;
  }

  .indent-guide {
    height: 100%;
    position: relative;
    flex-shrink: 0;

    .guide-line {
      position: absolute;
      top: 0;
      bottom: 0;
      width: 1px;
      background: rgba(52, 120, 240, 0.15);
    }
  }

  .row-toggle {
    width: 20px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: $t3;
    transition: transform 0.2s;

    &.collapsed {
      transform: rotate(-90deg);
    }

    &.is-leaf {
      visibility: hidden;
    }
  }

  .node-icon {
    width: 20px;
    height: 20px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-right: 8px;
    background: $b3;
    border: 1px solid $bd;

    .icon-char {
      font-size: 11px;
      font-weight: 800;
      font-family: "Courier New", monospace;
    }

    &.mid {
      color: $pp;
      border-color: rgba($pp, 0.3);
      background: rgba($pp, 0.1);
    }

    &.leaf {
      color: $bl;
      border-color: rgba($bl, 0.3);
      background: rgba($bl, 0.1);
    }

    &.br {
      color: $am;
      border-color: rgba($am, 0.3);
      background: rgba($am, 0.1);
    }
  }

  .node-main {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .node-name {
    font-size: 13px;
    font-weight: 500;
    color: $wh;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .node-id {
    font-size: 10px;
    color: $t4;
    font-family: "Courier New", monospace;
    opacity: 0.6;
  }

  .col-type {
    width: 80px;
    display: flex;
    justify-content: center;
  }

  .type-tag {
    font-size: 9px;
    padding: 2px 6px;
    border-radius: 4px;
    font-weight: 700;
    background: $b3;
    border: 1px solid $bd;
    color: $t4;
    text-transform: uppercase;
  }

  .col-op {
    width: 140px;
    display: flex;
    gap: 4px;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }

  .badge {
    font-size: 10px;
    padding: 1px 6px;
    border-radius: 4px;
    border: 1px solid transparent;
  }

  .badge.agg {
    background: rgba($cy, 0.1);
    color: $cy;
    border-color: rgba($cy, 0.2);
  }

  .badge.weight {
    background: rgba($pp, 0.1);
    color: $pp;
    border-color: rgba($pp, 0.2);

    &.pass {
      color: $t4;
      background: transparent;
      border-color: $bd;
    }
  }

  .branch-count {
    font-size: 10px;
    color: $t4;
    font-family: "Courier New", monospace;
  }

  .calc-text {
    font-size: 10px;
    color: $t3;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 120px;
  }

  .veto-mini {
    background: $rd;
    color: #fff;
    width: 12px;
    height: 12px;
    font-size: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 2px;
    font-weight: 900;
    margin-left: 4px;
  }

  .col-cond {
    width: 160px;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .auto-calc,
  .blank-dash {
    font-size: 10px;
    color: $t4;
    font-family: "Courier New", monospace;
  }

  .cond-summary {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .cond-tag {
    background: rgba($am, 0.1);
    color: $am;
    border: 1px solid rgba($am, 0.2);
    padding: 1px 5px;
    border-radius: 3px;
    font-size: 10px;
  }

  .cond-any {
    font-size: 10px;
    color: $gn;
    opacity: 0.6;
  }

  .row-actions {
    width: 80px;
    display: flex;
    gap: 4px;
    justify-content: flex-end;
    padding-right: 8px;
    opacity: 0;
    transform: translateX(10px);
    transition: all 0.2s;
  }

  .act-btn {
    padding: 4px;
    color: $t3;

    &:hover {
      color: $bl;
    }

    &.delete:hover {
      color: $rd;
    }
  }
}

// Right Panel Components
.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $t4;
  text-align: center;

  .empty-icon {
    font-size: 32px;
    margin-bottom: 20px;
    opacity: 0.2;
  }

  p {
    font-size: 13px;
    font-weight: 500;
    line-height: 1.6;
  }
}

.cyber-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  &.column {
    flex-direction: column;
  }

  .cyber-radio-btn {
    padding: 6px 12px;
    font-size: 11px;
    border-radius: 4px;
    border: 1px solid $bd;
    color: $t3;
    cursor: pointer;
    transition: all 0.2s;
    background: $b2;

    &:hover {
      border-color: $t4;
      color: $t2;
    }

    &.active {
      border-color: $bl;
      color: $wh;
      background: rgba(52, 120, 240, 0.2);

      &.cyan {
        border-color: $cy;
        background: rgba($cy, 0.2);
      }

      &.purple {
        border-color: $pp;
        background: rgba($pp, 0.2);
      }

      &.green {
        border-color: $gn;
        background: rgba($gn, 0.2);
      }
    }
  }

  &.mini .cyber-radio-btn {
    padding: 3px 8px;
    font-size: 10px;
  }
}

.child-node-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;

  .child-node-card {
    background: $b2;
    border: 1px solid $bd;
    border-radius: 4px;
    padding: 10px 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: $t4;
      background: $b3;
    }

    .child-name {
      font-size: 12px;
      font-weight: 600;
      color: $wh;
      display: block;
    }

    .child-meta {
      font-size: 10px;
      color: $t4;
      margin-top: 2px;
    }
  }
}

.branch-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;

  .branch-card {
    background: $b2;
    border: 1px solid $bd;
    border-radius: 6px;
    padding: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: $t4;
      background: $b3;
    }

    .branch-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 6px;
    }

    .branch-name {
      font-size: 12px;
      font-weight: 700;
      color: $wh;
    }

    .branch-meta {
      font-size: 10px;
      color: $t3;
      display: flex;
      align-items: center;
      gap: 4px;
      margin-bottom: 8px;
    }

    .branch-conditions {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;

      .cond-tag {
        font-size: 9px;
        padding: 1px 4px;
        border-radius: 3px;
        background: rgba($am, 0.1);
        color: $am;
        border: 1px solid rgba($am, 0.2);
      }
    }

    .empty-cond {
      font-size: 10px;
      color: $t4;
      font-style: italic;
    }
  }
}

.logic-box {
  background: #090e16;
  border: 1px solid $bd;
  border-radius: 4px;
  padding: 12px;

  .logic-info {
    font-size: 11px;
    color: $gn;
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.condition-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.logic-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logic-op {
  font-size: 9px;
  font-weight: 900;
  color: $bl;
  width: 30px;
  text-align: center;
}

.logic-bubble {
  flex: 1;
  min-width: 0;
  background: $b2;
  border: 1px solid rgba($am, 0.3);
  border-radius: 4px;
  padding: 4px 10px;
  display: flex;
  align-items: center;
  gap: 8px;

  .dim {
    color: $wh;
    font-size: 11px;
  }

  .op {
    color: $am;
    font-family: "Courier New", monospace;
    font-weight: 800;
  }

  .val {
    color: $gn;
    font-size: 11px;
    flex: 1;
  }

  .close-icon {
    cursor: pointer;
    color: $t4;
    font-size: 12px;

    &:hover {
      color: $rd;
    }
  }
}

.data-binding-bar {
  background: $b2;
  border: 1px solid $bd;
  border-radius: 4px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $bl;

    .chg-icon {
      opacity: 1;
    }
  }

  .bind-status {
    width: 6px;
    height: 6px;
    background: $gn;
    border-radius: 50%;
    margin-right: 10px;
    box-shadow: 0 0 6px rgba($gn, 0.5);
  }

  .bind-name {
    font-size: 11px;
    color: $wh;
    flex: 1;
    font-weight: 600;
  }

  .chg-icon {
    color: $t4;
    font-size: 12px;
    opacity: 0;
  }

  .math-icon {
    color: $cy;
    margin-right: 8px;
    font-size: 14px;
  }
}

.veto-config-box {
  margin-top: 10px;
  border-left: 2px solid $rd;
  padding: 10px 12px;
  background: rgba($rd, 0.05);

  .notice-bar {
    font-size: 10px;
    margin-top: 8px;
    color: $rd;

    strong {
      color: $wh;
    }
  }
}

// Modal Tactics
:deep(.tactical-modal) {
  background: $b1;
  border: 1px solid $bd-high;
  border-radius: 8px;
  box-shadow: 0 30px 100px rgba(0, 0, 0, 0.6);

  .el-dialog__header,
  .el-dialog__body,
  .el-dialog__footer {
    padding: 0 !important;
  }

  .el-dialog__headerbtn {
    display: none;
  }
}

.modal-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid $bd;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .header-icon {
      font-size: 18px;
      color: $bl;
    }

    .header-title {
      color: $wh;
      font-size: 14px;
      font-weight: 700;
    }
  }

  .modal-close {
    font-size: 18px;
    color: $t4;

    &:hover {
      color: $rd;
    }
  }
}

.modal-body {
  padding: 20px;

  &.no-padding {
    padding: 0;
  }
}

.tactical-type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 10px;

  .type-option {
    background: $b2;
    border: 1px solid $bd;
    border-radius: 6px;
    padding: 12px;
    display: flex;
    align-items: flex-start;
    gap: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: $t4;
    }

    &.active {
      border-color: $bl;
      background: rgba(52, 120, 240, 0.1);
      box-shadow: 0 0 15px rgba(52, 120, 240, 0.1);
    }

    .ico {
      font-size: 20px;
      font-weight: 700;
      line-height: 1;

      &.purple {
        color: $pp;
      }

      &.blue {
        color: $bl;
      }
    }

    .lbl {
      color: $wh;
      font-size: 13px;
      font-weight: 700;
    }

    .sub {
      color: $t4;
      font-size: 10px;
      margin-top: 4px;
      line-height: 1.4;
    }
  }
}

.modal-tabs-header {
  display: flex;
  background: $b2;
  border-bottom: 1px solid $bd;

  .tab-item {
    flex: 1;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: $t3;
    font-weight: 600;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    transition: all 0.2s;

    &:hover {
      color: $t2;
    }

    &.active {
      color: $bl;
      border-bottom-color: $bl;
      background: rgba(52, 120, 240, 0.05);
    }

    i {
      margin-right: 8px;
      font-size: 14px;
    }
  }
}

.tab-pane {
  padding: 20px;
  min-height: 400px;
}

.library-filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid $bd;
  padding-bottom: 16px;

  .cat-tags {
    display: flex;
    gap: 8px;
    flex: 1;

    .cat-tag {
      font-size: 11px;
      padding: 3px 10px;
      border-radius: 12px;
      background: $b3;
      border: 1px solid $bd;
      color: $t4;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: $t2;
      }

      &.active {
        background: $cy;
        color: #fff;
        border-color: $cy;
      }
    }
  }
}

.library-grid-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 5px;

  .library-card {
    background: $b2;
    border: 1px solid $bd;
    border-radius: 6px;
    padding: 12px;
    cursor: pointer;
    transition: all 0.15s;

    &:hover {
      border-color: $t3;
    }

    &.selected {
      border-color: $gn;
      background: rgba(15, 170, 120, 0.08);

      .check-box.active {
        background: $gn;
        border-color: $gn;
      }
    }

    .card-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
    }

    .metric-name {
      color: $wh;
      font-size: 12px;
      font-weight: 700;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 250px;
    }

    .check-box {
      width: 14px;
      height: 14px;
      border: 1px solid $bd;
      border-radius: 2px;
      color: #fff;
      font-size: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .card-meta {
      display: flex;
      gap: 8px;

      .lib-badge {
        font-size: 9px;
        padding: 1px 6px;
        border-radius: 3px;
        background: $b4;
        color: $t3;
      }

      .lib-calc {
        font-size: 9px;
        color: $t4;
        font-family: "Courier New", monospace;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

.modal-footer {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  border-top: 1px solid $bd;
  gap: 12px;
}

// Simulator Results Tactical UI
.simulation-results-viewport {
  margin-top: 20px;
  border-top: 2px dashed $bd;
  padding-top: 20px;
}

.sim-section {
  margin-bottom: 20px;

  .section-title {
    font-size: 11px;
    font-weight: 800;
    color: $t3;
    text-transform: uppercase;
    letter-spacing: 1.5px;
    margin-bottom: 12px;

    &.danger {
      color: $rd;
      border-left: 2px solid $rd;
      padding-left: 10px;
    }
  }
}

.hit-map {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;

  .hit-indicator-node {
    background: $b2;
    border: 1px solid $bd;
    border-radius: 4px;
    padding: 6px 10px;
    display: flex;
    align-items: center;
    gap: 8px;
    opacity: 0.4;

    &.hit {
      opacity: 1;
      border-color: rgba($gn, 0.4);

      .hit-status-dot {
        background: $gn;
        box-shadow: 0 0 6px $gn;
      }
    }

    .hit-status-dot {
      width: 6px;
      height: 6px;
      background: $t4;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .hit-name {
      font-size: 11px;
      color: $wh;
      font-weight: 600;
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .hit-logic {
      font-size: 9px;
      font-weight: 800;
      color: $t4;
      font-family: "Courier New", monospace;
      flex-shrink: 0;
    }
  }
}

.tree-trace-code {
  background: #050a10;
  border: 1px solid rgba($bl, 0.2);
  border-radius: 4px;
  padding: 15px;
  font-family: "Courier New", monospace;
  font-size: 11px;
  color: $bl;
  line-height: 1.8;
  overflow-x: auto;
}

.glow-btn {
  background: $bl;
  border: none;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 15px rgba($bl, 0.3);

  &:hover {
    box-shadow: 0 6px 20px rgba($bl, 0.5);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper) {
  background-color: $b2 !important;
  box-shadow: 0 0 0 1px $bd inset !important;
  border: none !important;

  .el-input__inner,
  .el-textarea__inner {
    color: $wh !important;
  }
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px $bl inset !important;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flex-center-gap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auto-calc {
  color: $cy;
  font-weight: 700;
}

.veto-badge {
  background: $rd;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 800;
  letter-spacing: 1px;
}

.veto-label {
  color: $rd;
  font-size: 10px;
  font-weight: 800;
  border-right: 1px solid rgba($rd, 0.3);
  padding-right: 8px;
  margin-right: 8px;
}

.crit-container {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.crit-card {
  display: flex;
  align-items: center;
  background: $b3;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid $bd;
}

.crit-select {
  flex: 1;
}

.crit-symbol {
  color: $t3;
  font-weight: 800;
  margin: 0 8px;
}

.crit-val-input {
  width: 50px !important;
}

.hint-text {
  font-size: 10px;
  color: $t4;
}
</style>
