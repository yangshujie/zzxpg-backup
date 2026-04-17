<template>
  <div class="manual-build">
    <!-- TOP BAR -->
    <div class="top">
      <div class="tl">
        <span class="logo">EVAL·SYS</span>
        <div class="tsep"></div>
        <span class="tname">{{ skeletonInfo.name }}</span>
        <span class="tid">SKL-COMJAM-EQ-001</span>
        <span class="tbadge draft">草稿</span>
      </div>
      <div class="tr">
        <button class="b b-am" @click="showSim">▶ 模拟生成</button>
        <button class="b" @click="saveDraft">✓ 保存</button>
        <button class="b b-g">↑ 发布</button>
      </div>
    </div>

    <div class="main">
      <!-- LEFT PANEL -->
      <div class="lp">
        <div class="sec">
          <div class="sec-t">基本信息</div>
          <div class="fld">
            <div class="fld-l"><span class="rq">*</span>骨架名称</div>
            <input class="inp" v-model="skeletonInfo.name">
          </div>
          <div class="fld">
            <div class="fld-l"><span class="rq">*</span>版本</div>
            <input class="inp" v-model="skeletonInfo.version" style="width:90px">
          </div>
          <div class="fld">
            <div class="fld-l">评估目标描述</div>
            <textarea class="inp" v-model="skeletonInfo.desc"></textarea>
          </div>
        </div>

        <div class="sec">
          <div class="sec-t">适用范围</div>
          <div class="fld">
            <div class="fld-l">领域</div>
            <div class="tags">
              <button class="tag" :class="{ on: skeletonInfo.domains.includes('电子对抗') }" @click="toggleTag('domains', '电子对抗')">电子对抗</button>
              <button class="tag" :class="{ on: skeletonInfo.domains.includes('通信对抗') }" @click="toggleTag('domains', '通信对抗')">通信对抗</button>
              <button class="tag" :class="{ on: skeletonInfo.domains.includes('雷达对抗') }" @click="toggleTag('domains', '雷达对抗')">雷达对抗</button>
            </div>
          </div>
          <div class="fld">
            <div class="fld-l">装备类型</div>
            <div class="tags">
              <button class="tag" :class="{ on2: skeletonInfo.eqTypes.includes('有源干扰机') }" @click="toggleTag('eqTypes', '有源干扰机')">有源干扰机</button>
              <button class="tag" :class="{ on2: skeletonInfo.eqTypes.includes('侦察干扰一体化') }" @click="toggleTag('eqTypes', '侦察干扰一体化')">侦察干扰一体化</button>
              <button class="tag" :class="{ on2: skeletonInfo.eqTypes.includes('宽带侦察') }" @click="toggleTag('eqTypes', '宽带侦察')">宽带侦察</button>
            </div>
          </div>
          <div class="fld">
            <div class="fld-l">评估层级</div>
            <div class="tags">
              <button class="tag" :class="{ on: skeletonInfo.levels.includes('设备级') }" @click="toggleTag('levels', '设备级')">设备级</button>
              <button class="tag" :class="{ on: skeletonInfo.levels.includes('系统级') }" @click="toggleTag('levels', '系统级')">系统级</button>
              <button class="tag" :class="{ on: skeletonInfo.levels.includes('体系级') }" @click="toggleTag('levels', '体系级')">体系级</button>
            </div>
          </div>
          <div class="fld">
            <div class="fld-l">任务类型</div>
            <div class="tags">
              <button class="tag" :class="{ on2: skeletonInfo.taskTypes.includes('效能评估') }" @click="toggleTag('taskTypes', '效能评估')">效能评估</button>
              <button class="tag" :class="{ on2: skeletonInfo.taskTypes.includes('对比评估') }" @click="toggleTag('taskTypes', '对比评估')">对比评估</button>
              <button class="tag" :class="{ on2: skeletonInfo.taskTypes.includes('性能测试') }" @click="toggleTag('taskTypes', '性能测试')">性能测试</button>
            </div>
          </div>
        </div>

        <!-- 评估准则 -->
        <div class="sec">
          <div class="sec-t">评估准则</div>
          <div class="fld">
            <div class="fld-l">全局评分机制</div>
            <select class="inp sel" v-model="skeletonInfo.scoreMethod">
              <option>综合单一评分</option>
              <option>分维度独立评分</option>
              <option>加权综合 + 维度诊断</option>
            </select>
          </div>
          <div class="fld">
            <div class="fld-l">全局默认汇聚算子 <span style="color:#5a6a85;font-size:9px">（各节点可覆盖）</span></div>
            <select class="inp sel" v-model="skeletonInfo.globalAgg">
              <option>加权平均</option><option>均值聚合</option><option>最大值聚合</option>
            </select>
          </div>
          <div class="fld">
            <div class="fld-l">全局默认权重策略</div>
            <select class="inp sel" v-model="skeletonInfo.globalWeight">
              <option>AHP层次分析法</option><option>等权重</option><option>熵权法</option><option>专家打分</option>
            </select>
          </div>
          <div class="fld">
            <div class="fld-l" style="display:flex;justify-content:space-between">
              <span>一票否决规则 <span class="veto-badge">VETO</span></span>
              <button class="add-link" @click="addVetoRow">+ 添加</button>
            </div>
            <div>
              <div v-for="(v, index) in skeletonInfo.vetos" :key="index" class="crit-row">
                <span class="veto-badge">否决</span>
                <select class="inp sel cr-nm" v-model="v.name" style="flex:1;padding:2px 20px 2px 5px;font-size:10px;border:none;background:transparent;">
                  <option v-for="b in branchNodes" :key="b.id" :value="b.name">{{ b.name }}</option>
                </select>
                <span style="font-size:9px;color:#5a6a85">得分&lt;</span>
                <input class="cr-thr" v-model="v.thr">
                <button class="cr-del" @click="removeVetoRow(index)">✕</button>
              </div>
            </div>
          </div>
          <div class="fld">
            <div class="fld-l">最低通过分值</div>
            <div style="display:flex;align-items:center;gap:8px">
              <input class="inp" v-model="skeletonInfo.minPass" style="width:70px;font-family:'Courier New',monospace">
              <span style="font-size:10px;color:#5a6a85">低于此值输出警告</span>
            </div>
          </div>
        </div>

        <div class="sec">
          <div class="sec-t">骨架统计</div>
          <div class="st-grid">
            <div class="st"><div class="st-v">{{ stats.mid }}</div><div class="st-l">中间层</div></div>
            <div class="st"><div class="st-v">{{ stats.leaf }}</div><div class="st-l">叶子层</div></div>
            <div class="st"><div class="st-v">{{ stats.br }}</div><div class="st-l">具体分支</div></div>
          </div>
        </div>
      </div>

      <!-- CENTER PANEL -->
      <div class="cp">
        <div class="cp-bar">
          <div class="cp-bar-l">
            <h3>指标树</h3>
            <button class="cp-btn" :class="{ on: isExpandAll }" @click="toggleAll(true)">全部展开</button>
            <button class="cp-btn" :class="{ on: !isExpandAll }" @click="toggleAll(false)">全部折叠</button>
          </div>
          <div class="cp-btns">
            <button class="b" @click="openAddNode(null)">+ 添加根节点</button>
          </div>
        </div>
        <div class="col-hdr">
          <div class="ch-name">节点 / 分支名称</div>
          <div class="ch-type">类型</div>
          <div class="ch-op">算子 / 分支数</div>
          <div class="ch-cond">激活条件</div>
          <div class="ch-act"></div>
        </div>
        
        <div class="tree-scroll">
          <div class="tree-ct">
            <el-tree 
              ref="treeRef"
              :data="treeData"
              node-key="id"
              :default-expand-all="true"
              :expand-on-click-node="false"
              empty-text="暂无节点"
            >
              <template #default="{ node, data }">
                 <div class="r" 
                      :class="{ sel: selectedNodeId === data.id, hit: data._hit, miss: data._miss }" 
                      @click="selRow(data.id)"
                 >
                    <div class="pad-area" :style="{ width: (node.level - 1) * 20 + 6 + 'px' }">
                      <div class="pad-line" v-for="l in (node.level - 1)" :key="l" :style="{ left: l*20 - 4 + 'px' }"></div>
                    </div>
                    
                    <div class="tg" 
                         :class="{ shut: !node.expanded, no: !data.children || data.children.length === 0 }"
                         @click.stop="node.expanded = !node.expanded"
                    >
                      ▼
                    </div>
                    
                    <div class="ico" :class="getIcoClass(data.type)">{{ getIcoChar(data.type) }}</div>
                    
                    <div class="nm"><span>{{ data.name }}</span></div>
                    
                    <div class="type-badge" :class="getTypeBadge(data.type)">{{ getTypeText(data.type) }}</div>
                    
                    <div class="op-cell" :style="data.type === 'br' ? 'justify-content: flex-start; padding-left: 4px;' : ''">
                       <template v-if="data.type === 'mid'">
                         <span class="op-tag op-agg">{{ data.aggOp }}</span>
                         <span class="op-tag" :class="data.wOp === '透传' ? 'op-pass' : 'op-w'">{{ data.wOp }}</span>
                       </template>
                       <template v-else-if="data.type === 'leaf'">
                         <span style="font-size:9px;color:#5a6a85">{{ data.children ? data.children.length : 0 }}个分支</span>
                       </template>
                       <template v-else-if="data.type === 'br'">
                         <span style="font-size:9px;color:#3c4b62;overflow:hidden;text-overflow:ellipsis;max-width:110px">{{ data.calc }}</span>
                         <span v-if="data.veto" class="veto-badge" style="font-size:7px">V</span>
                       </template>
                    </div>
                    
                    <div class="cond-cell">
                      <template v-if="data.type === 'mid'"><span style="font-size:9px;color:#3c4b62">自动推导</span></template>
                      <template v-else-if="data.type === 'leaf'"><span style="font-size:9px;color:#3c4b62">—</span></template>
                      <template v-else-if="data.type === 'br'">
                         <span v-if="!data.conds || data.conds.length === 0" class="ct-free">无条件</span>
                         <template v-else>
                           <span class="ct">{{ data.conds[0].dim }}{{ data.conds[0].op }}{{ data.conds[0].val }}</span>
                           <span v-if="data.conds.length > 1" class="ct-more">+{{ data.conds.length - 1 }}</span>
                         </template>
                      </template>
                    </div>
                    
                    <div class="r-act">
                      <button v-if="data.type === 'leaf'" class="ab" title="添加分支" @click.stop="openAddBranch(data.id)">+</button>
                      <button v-if="data.type === 'mid'" class="ab" title="添加子节点" @click.stop="openAddNode(data.id)">+</button>
                      <button class="ab del" title="删除" @click.stop="deleteNode(data.id)">✕</button>
                    </div>
                 </div>
              </template>
            </el-tree>
          </div>
        </div>
      </div>

      <!-- RIGHT PANEL -->
      <div class="rp">
        <div class="rp-hdr">
          <div class="rp-title">
            <span v-if="selectedNode" class="ico" :class="getIcoClass(selectedNode.type)" style="width:15px;height:15px;font-size:9px;display:inline-flex;align-items:center;justify-content:center;margin-right:4px;">{{ getIcoChar(selectedNode.type) }}</span>
            <span v-if="selectedNode">{{ selectedNode.name }}</span>
            <span v-else style="color:#3c4b62">属性配置</span>
          </div>
          <div class="rp-sub" v-if="selectedNode">{{ getTypeText(selectedNode.type) }}</div>
        </div>
        <div class="rp-body">
          <div v-if="!selectedNode" class="rp-empty">
            <div style="font-size:26px;opacity:.3;margin-bottom:6px">◈</div>
            <div>点击树中节点<br>查看并编辑属性</div>
          </div>
          
          <!-- MID NODE CONFIG -->
          <template v-else-if="selectedNode.type === 'mid'">
            <div class="rp-sec">
              <div class="rp-sec-t">节点名称</div>
              <input class="inp" v-model="selectedNode.name">
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">算子配置</div>
              <div class="rp-fld">
                <div class="rp-fld-l">汇聚算子</div>
                <div class="op-sel">
                  <button v-for="op in ['加权平均','均值聚合','最大值聚合','最小值聚合','几何平均']" :key="op"
                          class="op-btn" :class="{'on-cy': selectedNode.aggOp === op}"
                          @click="selectedNode.aggOp = op">{{ op }}</button>
                </div>
              </div>
              <div class="rp-fld">
                <div class="rp-fld-l">权重算子</div>
                <div class="op-sel">
                  <button v-for="op in ['AHP权重','等权重','熵权法','专家打分','透传']" :key="op"
                          class="op-btn" :class="{'on-pp': selectedNode.wOp === op}"
                          @click="selectedNode.wOp = op">{{ op }}</button>
                </div>
              </div>
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">激活规则</div>
              <div class="info-box">本节点<span class="hl">无需配置条件</span>，激活状态由子节点自动推导。<br>子节点有任意分支命中 → 本节点自动激活；所有子节点均无命中 → 本节点从生成结果中移除。</div>
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">子节点 <span style="font-family:'Courier New',monospace;color:#5a6a85;font-weight:400">{{ getChildren(selectedNode.id).length }}</span></div>
              <div v-for="c in getChildren(selectedNode.id)" :key="c.id" class="bc" @click="selRow(c.id)">
                <div class="bc-name">
                  <span>{{ c.name }}</span>
                  <span style="font-size:9px;color:#3c4b62">{{ c.type === 'leaf' ? getChildren(c.id).length + '个分支' : '中间层' }}</span>
                </div>
              </div>
              <div class="bc-add" @click="openAddNode(selectedNode.id)">+ 添加子节点</div>
            </div>
          </template>

          <!-- LEAF NODE CONFIG -->
          <template v-else-if="selectedNode.type === 'leaf'">
            <div class="rp-sec">
              <div class="rp-sec-t">泛化节点名称</div>
              <input class="inp" v-model="selectedNode.name">
              <div style="font-size:10px;color:#3c4b62;margin-top:5px">此名称是类别容器，不直接出现在生成的指标体系中；出现的是命中的具体分支名称。</div>
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">具体分支 <span style="font-family:'Courier New',monospace;color:#5a6a85;font-weight:400">{{ getChildren(selectedNode.id).length }}</span></div>
              <div v-for="b in getChildren(selectedNode.id)" :key="b.id" class="bc" @click="selRow(b.id)">
                <div class="bc-name">
                  <span>{{ b.name }}</span>
                  <span v-if="b.veto" class="veto-badge">VETO</span>
                  <span style="font-size:9px;color:#3c4b62">{{ b.norm }}</span>
                </div>
                <div class="bc-conds">
                  <span v-if="!b.conds || b.conds.length === 0" class="free-tag" style="font-size:10px;padding:1px 7px">无条件</span>
                  <span v-else v-for="(c, i) in b.conds" :key="i" class="ct">{{ c.dim }}{{ c.op }}{{ c.val }}</span>
                </div>
              </div>
              <div class="bc-add-row">
                <div class="bc-add" @click="openAddBranch(selectedNode.id)">+ 手动添加分支</div>
                <div class="bc-add bc-add-lib" @click="openAddBranch(selectedNode.id, 'lib')">☰ 从指标库</div>
              </div>
            </div>
          </template>

          <!-- BRANCH NODE CONFIG -->
          <template v-else-if="selectedNode.type === 'br'">
            <div class="rp-sec">
              <div class="rp-sec-t">所属叶子节点</div>
              <div style="font-size:11px;color:#5a6a85">{{ getNodeById(selectedNode.pid)?.name }}</div>
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">分支名称（具体指标）</div>
              <input class="inp" v-model="selectedNode.name">
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">激活条件</div>
              <div v-if="!selectedNode.conds || selectedNode.conds.length === 0" class="free-tag">✓ 无条件 — 始终激活</div>
              <div v-else v-for="(c, i) in selectedNode.conds" :key="i" class="cond-row">
                <span v-if="i > 0" style="font-size:9px;color:#3c4b62">AND</span>
                <span class="cond-dim">{{ c.dim }}</span>
                <span class="cond-op-badge">{{ c.op }}</span>
                <span class="cond-val">{{ c.val }}</span>
                <button class="cond-del-btn" @click="removeCond(selectedNode, i)">✕</button>
              </div>
              <button class="add-cond-btn" style="margin-top:6px" @click="addCondToNode(selectedNode)">+ 添加条件</button>
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">数据绑定</div>
              <div class="data-bind"><div class="db-dot"></div><span>{{ selectedNode.data }}</span><span class="db-chg">切换 ▼</span></div>
            </div>
            <div class="rp-sec">
              <div class="rp-sec-t">算子绑定</div>
              <div class="rp-fld">
                <div class="rp-fld-l">指标值计算算子</div>
                <div class="data-bind" style="cursor:pointer"><span style="color:#08a8c5;font-size:10px">◈</span><span>{{ selectedNode.calc }}</span><span class="db-chg">切换 ▼</span></div>
              </div>
              <div class="rp-fld">
                <div class="rp-fld-l">归一化算子</div>
                <div class="op-sel">
                  <button v-for="op in ['线性归一化','逆线性归一化','S型归一化','比值归一化','阶梯归一化']" :key="op"
                          class="op-btn" :class="{'on-gn': selectedNode.norm === op}"
                          @click="selectedNode.norm = op">{{ op }}</button>
                </div>
              </div>
            </div>
            <div class="rp-sec">
               <div class="rp-sec-t">特殊规则</div>
               <label style="display:flex;align-items:center;gap:8px;font-size:11px;color:#90a0be;cursor:pointer">
                 <input type="checkbox" v-model="selectedNode.veto">
                 <span>一票否决 <span class="veto-badge">VETO</span></span>
                 <span style="font-size:10px;color:#5a6a85;margin-left:4px">否决阈值：</span>
                 <input class="inp" value="0.3" style="width:56px">
               </label>
               <div class="info-box" style="margin-top:8px;font-size:10px">若此分支得分低于阈值，<span class="hl-am">整体评估总分强制归零</span>，输出否决警告。</div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- MODALS -->
    <!-- Add Node Modal -->
    <el-dialog v-model="addNodeDialog.visible" :title="addNodeDialog.pid ? `在「${getNodeById(addNodeDialog.pid)?.name}」下添加子节点` : '添加根节点'" width="520px" class="cyber-dialog" :show-close="false">
      <template #header="{ close }">
        <div class="md-h">
          <h3>{{ addNodeDialog.pid ? `在「${getNodeById(addNodeDialog.pid)?.name}」下添加子节点` : '添加根节点' }}</h3>
          <button class="md-x" @click="close">✕</button>
        </div>
      </template>
      <div class="md-b">
        <div class="fld">
          <div class="fld-l">选择节点类型</div>
          <div class="type-sel">
            <div class="type-opt" :class="{ on: addNodeDialog.type === 'mid' }" @click="addNodeDialog.type = 'mid'">
              <div class="to-ico" style="color:#9855e0">◈</div>
              <div class="to-lbl">中间层节点</div>
              <div class="to-sub">结构分组 + 聚合计算</div>
            </div>
            <div class="type-opt" :class="{ on: addNodeDialog.type === 'leaf' }" @click="addNodeDialog.type = 'leaf'">
              <div class="to-ico" style="color:#5b9cf5">◇</div>
              <div class="to-lbl">叶子节点</div>
              <div class="to-sub">持有具体分支的末端</div>
            </div>
          </div>
        </div>
        <div class="fld">
          <div class="fld-l"><span class="rq">*</span>泛化名称</div>
          <input class="inp" v-model="addNodeDialog.name" placeholder="如：干扰覆盖性 / 测向精度">
        </div>
        <div v-show="addNodeDialog.type === 'mid'">
          <div class="fld">
             <div class="fld-l">汇聚算子</div>
             <div class="op-sel">
                <button v-for="op in ['加权平均','均值聚合','最大值聚合','最小值聚合']" :key="op"
                        class="op-btn" :class="{'on-cy': addNodeDialog.aggOp === op}"
                        @click="addNodeDialog.aggOp = op">{{ op }}</button>
             </div>
          </div>
          <div class="fld">
             <div class="fld-l">权重算子</div>
             <div class="op-sel">
                <button v-for="op in ['AHP权重','等权重','熵权法','透传']" :key="op"
                        class="op-btn" :class="{'on-pp': addNodeDialog.wOp === op}"
                        @click="addNodeDialog.wOp = op">{{ op }}</button>
             </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="md-f">
          <button class="b" @click="addNodeDialog.visible = false">取消</button>
          <button class="b b-p" @click="confirmAddNode">✓ 确认添加</button>
        </div>
      </template>
    </el-dialog>

    <!-- Add Branch Modal -->
    <el-dialog v-model="addBranchDialog.visible" title="添加具体分支" width="700px" class="cyber-dialog" :show-close="false">
      <template #header="{ close }">
        <div class="md-h">
          <h3>添加具体分支</h3>
          <button class="md-x" @click="close">✕</button>
        </div>
      </template>
      <div class="md-b">
        <div class="md-tabs">
          <button class="md-tab" :class="{ on: addBranchDialog.tab === 'manual' }" @click="addBranchDialog.tab = 'manual'">✎ 手动填写</button>
          <button class="md-tab" :class="{ on: addBranchDialog.tab === 'lib' }" @click="addBranchDialog.tab = 'lib'">☰ 从指标库选择</button>
        </div>
        
        <!-- Manual -->
        <div v-show="addBranchDialog.tab === 'manual'" class="md-panel vis">
          <div class="fld">
            <div class="fld-l"><span class="rq">*</span>分支名称（具体指标名称）</div>
            <input class="inp" v-model="addBranchDialog.manual.name" placeholder="如：压制干扰覆盖率">
          </div>
          <div class="fld">
            <div class="fld-l">激活条件 <span style="color:#3c4b62;font-size:9px;margin-left:4px">（留空 = 始终激活）</span></div>
            <div>
              <div v-for="(c, index) in addBranchDialog.manual.conds" :key="index" class="cond-builder-row">
                <select class="cond-s" v-model="c.dim">
                  <option>任务类型</option><option>评估层级</option><option>场景</option>
                  <option>装备型号</option><option>电磁环境</option><option>数据可用性</option>
                </select>
                <select class="cond-s" style="max-width:60px" v-model="c.op">
                  <option>=</option><option>包含</option><option>!=</option>
                </select>
                <input class="inp" style="flex:1" placeholder="值" v-model="c.val">
                <button class="cond-del-sm" @click="addBranchDialog.manual.conds.splice(index, 1)">✕</button>
              </div>
            </div>
            <button class="add-cond-btn" @click="addBranchDialog.manual.conds.push({dim: '任务类型', op: '=', val: ''})">+ 添加条件</button>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
            <div class="fld">
              <div class="fld-l"><span class="rq">*</span>数据源</div>
              <select class="inp sel" v-model="addBranchDialog.manual.data">
                <option>宽带信号采集数据</option><option>频谱扫描数据</option><option>雷达回波数据</option>
                <option>信号强度测量数据</option><option>功率监测数据</option><option>多站融合数据</option>
              </select>
            </div>
            <div class="fld">
              <div class="fld-l"><span class="rq">*</span>计算算子</div>
              <select class="inp sel" v-model="addBranchDialog.manual.calc">
                <option>覆盖带宽比值算子</option><option>频段完整度算子</option><option>虚假目标计数算子</option>
                <option>干信比计算算子</option><option>功率比值算子</option><option>均方根误差算子</option>
              </select>
            </div>
          </div>
          <div class="fld">
            <div class="fld-l"><span class="rq">*</span>归一化算子</div>
            <div class="op-sel">
                <button v-for="op in ['线性归一化','逆线性归一化','S型归一化','比值归一化','阶梯归一化']" :key="op"
                        class="op-btn" :class="{'on-gn': addBranchDialog.manual.norm === op}"
                        @click="addBranchDialog.manual.norm = op">{{ op }}</button>
            </div>
          </div>
          <div class="fld">
            <div class="fld-l">是否设为一票否决指标</div>
            <label style="display:flex;align-items:center;gap:7px;font-size:11px;color:#90a0be;cursor:pointer">
              <input type="checkbox" v-model="addBranchDialog.manual.veto">
              <span>启用 <span class="veto-badge">VETO</span></span>
            </label>
          </div>
        </div>

        <!-- Library -->
        <div v-show="addBranchDialog.tab === 'lib'" class="md-panel vis">
           <div class="lib-cats">
              <button class="lib-cat" :class="{ on: addBranchDialog.lib.cat === 'all' }" @click="addBranchDialog.lib.cat = 'all'">全部</button>
              <button class="lib-cat" :class="{ on: addBranchDialog.lib.cat === '干扰效能' }" @click="addBranchDialog.lib.cat = '干扰效能'">干扰效能</button>
              <button class="lib-cat" :class="{ on: addBranchDialog.lib.cat === '侦察能力' }" @click="addBranchDialog.lib.cat = '侦察能力'">侦察能力</button>
              <button class="lib-cat" :class="{ on: addBranchDialog.lib.cat === '响应时序' }" @click="addBranchDialog.lib.cat = '响应时序'">响应时序</button>
              <button class="lib-cat" :class="{ on: addBranchDialog.lib.cat === '对抗效果' }" @click="addBranchDialog.lib.cat = '对抗效果'">对抗效果</button>
           </div>
           <input class="inp lib-search" v-model="addBranchDialog.lib.search" placeholder="搜索指标名称...">
           <div class="lib-grid">
              <div v-for="item in filteredLibItems" :key="item.name" 
                   class="lib-item" :class="{ picked: item._picked }" @click="item._picked = !item._picked">
                 <div class="li-top">
                    <span class="li-nm">{{ item.name }}</span>
                    <span class="li-ck">{{ item._picked ? '✓' : '' }}</span>
                 </div>
                 <div class="li-meta">
                    <span class="li-cat-badge">{{ item.cat }}</span>
                    <span style="color:#3c4b62">{{ item.calc }}</span>
                 </div>
              </div>
           </div>
           <div class="li-count">已选 <span style="color:#30d49a;font-weight:600">{{ selectedLibCount }}</span> 项</div>
        </div>
      </div>
      <template #footer>
        <div class="md-f">
          <button class="b" @click="addBranchDialog.visible = false">取消</button>
          <button class="b b-p" @click="confirmAddBranch">✓ 保存</button>
        </div>
      </template>
    </el-dialog>

    <!-- Simulate Modal -->
    <el-dialog v-model="simDialog.visible" title="模拟生成 — 输入任务属性" width="700px" class="cyber-dialog" :show-close="false">
      <template #header="{ close }">
        <div class="md-h">
          <h3>模拟生成 — 输入任务属性</h3>
          <button class="md-x" @click="close">✕</button>
        </div>
      </template>
      <div class="md-b">
        <div style="font-size:11px;color:#5a6a85;margin-bottom:11px">输入假设任务的属性，预览哪些具体分支会被选入该任务的指标体系。</div>
        <div class="sim-attrs">
          <div class="fld">
            <div class="fld-l">任务类型</div>
            <select class="inp sel" v-model="simDialog.attrs['任务类型']">
              <option value="">（不指定）</option><option>效能评估</option><option>性能测试</option><option>对比评估</option>
            </select>
          </div>
          <div class="fld">
            <div class="fld-l">评估层级</div>
            <select class="inp sel" v-model="simDialog.attrs['评估层级']">
               <option value="">（不指定）</option><option>设备级</option><option>系统级</option><option>体系级</option>
            </select>
          </div>
          <div class="fld">
            <div class="fld-l">场景</div>
            <select class="inp sel" v-model="simDialog.attrs['场景']">
               <option value="">（不指定）</option><option>跳频信号</option><option>宽频谱覆盖</option><option>复杂电磁环境</option>
            </select>
          </div>
          <div class="fld">
            <div class="fld-l">装备型号</div>
            <select class="inp sel" v-model="simDialog.attrs['装备型号']">
               <option value="">（不指定）</option><option>欺骗干扰型</option><option>压制干扰型</option><option>侦察干扰一体化</option>
            </select>
          </div>
        </div>
        <button class="b b-p" @click="runSim" style="width:100%;justify-content:center;margin-bottom:13px">▶ 生成并预览</button>
        
        <div v-if="simDialog.hasRun" id="sim-result">
          <div class="sim-result">
            <div class="sim-sec-t">命中分支 <span style="color:#30d49a;font-family:'Courier New',monospace;font-weight:400">{{ simDialog.hitCount }} / {{ stats.br }}</span></div>
            <div>
               <div v-for="b in branchNodes" :key="b.id" class="sim-match">
                  <div :class="b._hit ? 'sim-dot-hit' : 'sim-dot-miss'"></div>
                  <span :class="b._hit ? 'sim-nm-hit' : 'sim-nm-miss'">{{ b.name }}</span>
                  <span class="sim-cond-txt">
                     {{ (!b.conds || b.conds.length === 0) ? '无条件' : b.conds.map(c => `${c.dim}${c.op}${c.val}`).join(' & ') }}
                  </span>
               </div>
            </div>
          </div>
          
          <div class="sim-result" style="margin-top:8px" v-if="vetosHit.length > 0">
             <div class="sim-sec-t" style="color:#e04040">一票否决指标 <span class="veto-badge">VETO</span></div>
             <div v-for="v in vetosHit" :key="v.id" class="sim-match">
                <div class="sim-dot-hit" style="background:#e04040"></div>
                <span style="color:#e04040">{{ v.name }} — 若得分低于阈值，总分归零</span>
             </div>
          </div>
          
          <div class="sim-result" style="margin-top:8px">
            <div class="sim-sec-t">生成的指标树结构</div>
            <div style="font-family:'Courier New',monospace;font-size:11px;color:#90a0be;line-height:1.8;white-space:pre" v-html="simTreeText"></div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="md-f"><button class="b" @click="simDialog.visible = false">关闭</button></div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

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
  {id:'root',pid:null,type:'mid',name:'通信对抗综合效能',aggOp:'加权平均',wOp:'AHP权重'},
  {id:'n1',pid:'root',type:'mid',name:'干扰效能',aggOp:'加权平均',wOp:'等权重'},
  {id:'n1a',pid:'n1',type:'leaf',name:'干扰覆盖性'},
  {id:'b1',pid:'n1a',type:'br',name:'压制干扰覆盖率',conds:[{dim:'任务类型',op:'=',val:'效能评估'}],data:'宽带信号采集数据',calc:'覆盖带宽比值算子',norm:'线性归一化',veto:false},
  {id:'b2',pid:'n1a',type:'br',name:'频段覆盖完整度',conds:[{dim:'任务类型',op:'=',val:'效能评估'},{dim:'场景',op:'包含',val:'宽频谱覆盖'}],data:'频谱扫描数据',calc:'频段完整度算子',norm:'线性归一化',veto:false},
  {id:'b3',pid:'n1a',type:'br',name:'欺骗目标数量',conds:[{dim:'装备型号',op:'包含',val:'欺骗干扰型'}],data:'雷达回波数据',calc:'虚假目标计数算子',norm:'比值归一化',veto:false},
  {id:'n1b',pid:'n1',type:'leaf',name:'干扰功率效能'},
  {id:'b4',pid:'n1b',type:'br',name:'干信比 J/S',conds:[],data:'信号强度测量数据',calc:'干信比计算算子',norm:'S型归一化',veto:false},
  {id:'b5',pid:'n1b',type:'br',name:'有效干扰功率占比',conds:[{dim:'评估层级',op:'=',val:'系统级'}],data:'功率监测数据',calc:'功率比值算子',norm:'线性归一化',veto:false},
  {id:'n2',pid:'root',type:'mid',name:'侦察能力',aggOp:'均值聚合',wOp:'等权重'},
  {id:'n2a',pid:'n2',type:'leaf',name:'截获能力'},
  {id:'b6',pid:'n2a',type:'br',name:'信号截获率',conds:[{dim:'任务类型',op:'=',val:'效能评估'}],data:'宽带采集数据',calc:'截获率统计算子',norm:'线性归一化',veto:false},
  {id:'b7',pid:'n2a',type:'br',name:'跳频截获成功率',conds:[{dim:'场景',op:'包含',val:'跳频信号'}],data:'跳频专项采集数据',calc:'跳频截获算子',norm:'线性归一化',veto:false},
  {id:'n2b',pid:'n2',type:'leaf',name:'测向精度'},
  {id:'b8',pid:'n2b',type:'br',name:'测向均方根误差',conds:[{dim:'评估层级',op:'=',val:'设备级'}],data:'测向原始数据',calc:'均方根误差算子',norm:'逆线性归一化',veto:false},
  {id:'b9',pid:'n2b',type:'br',name:'多站融合测向精度',conds:[{dim:'评估层级',op:'=',val:'系统级'}],data:'多站融合数据',calc:'融合精度算子',norm:'逆线性归一化',veto:false},
  {id:'n3',pid:'root',type:'mid',name:'响应时序',aggOp:'最小值聚合',wOp:'透传'},
  {id:'n3a',pid:'n3',type:'leaf',name:'时延指标'},
  {id:'b10',pid:'n3a',type:'br',name:'目指接收时延',conds:[],data:'目标指示报文数据',calc:'报文时延算子',norm:'逆线性归一化',veto:false},
  {id:'b11',pid:'n3a',type:'br',name:'干扰响应时延',conds:[],data:'干扰触发时序数据',calc:'响应时延算子',norm:'逆线性归一化',veto:false},
  {id:'b12',pid:'n3a',type:'br',name:'全链路端到端响应时延',conds:[{dim:'评估层级',op:'=',val:'系统级'}],data:'全链路时序数据',calc:'端到端时延算子',norm:'逆线性归一化',veto:true},
])

// 24 mock items for library import to meet requirements
const libItemsRaw = ref([
  {name:'压制干扰覆盖率',cat:'干扰效能',calc:'覆盖带宽比值算子',data:'宽带信号采集数据',norm:'线性归一化'},
  {name:'频段覆盖完整度',cat:'干扰效能',calc:'频段完整度算子',data:'频谱扫描数据',norm:'线性归一化'},
  {name:'欺骗目标数量',cat:'干扰效能',calc:'虚假目标计数算子',data:'雷达回波数据',norm:'比值归一化'},
  {name:'干信比 J/S',cat:'干扰效能',calc:'干信比计算算子',data:'信号强度测量数据',norm:'S型归一化'},
  {name:'有效干扰功率占比',cat:'干扰效能',calc:'功率比值算子',data:'功率监测数据',norm:'线性归一化'},
  {name:'干扰建立有效时延',cat:'干扰效能',calc:'时延统计算子',data:'干扰触发时序数据',norm:'逆线性归一化'},
  {name:'全频谱压制效率',cat:'干扰效能',calc:'宽带压制比算子',data:'宽带对消数据',norm:'线性归一化'},
  {name:'精准引导干扰成功率',cat:'干扰效能',calc:'锁定命中率算子',data:'目标锁定日志数据',norm:'线性归一化'},
  {name:'目标误判率',cat:'干扰效能',calc:'误判分类统计',data:'跟踪确认数据',norm:'逆线性归一化'},
  {name:'多目标同时干扰能力',cat:'干扰效能',calc:'多目标数量结算',data:'并发处理能力数据',norm:'阶梯归一化'},
  
  {name:'信号截获率',cat:'侦察能力',calc:'截获率统计算子',data:'宽带采集数据',norm:'线性归一化'},
  {name:'跳频截获成功率',cat:'侦察能力',calc:'跳频截获算子',data:'跳频专项采集数据',norm:'线性归一化'},
  {name:'信号识别率',cat:'侦察能力',calc:'识别率统计算子',data:'信号特征数据',norm:'线性归一化'},
  {name:'测向均方根误差',cat:'侦察能力',calc:'均方根误差算子',data:'测向原始数据',norm:'逆线性归一化'},
  {name:'多站融合测向精度',cat:'侦察能力',calc:'融合精度算子',data:'多站融合数据',norm:'逆线性归一化'},
  {name:'极弱信号发现概率',cat:'侦察能力',calc:'信噪比检测算子',data:'底噪提取数据',norm:'S型归一化'},
  {name:'未知调制方式识别率',cat:'侦察能力',calc:'未知分类模型算子',data:'原始IQ数据',norm:'线性归一化'},
  
  {name:'目指接收时延',cat:'响应时序',calc:'报文时延算子',data:'目标指示报文数据',norm:'逆线性归一化'},
  {name:'干扰响应时延',cat:'响应时序',calc:'响应时延算子',data:'干扰触发时序数据',norm:'逆线性归一化'},
  {name:'全链路端到端响应时延',cat:'响应时序',calc:'端到端时延算子',data:'全链路时序数据',norm:'逆线性归一化'},
  {name:'跳频响应时延',cat:'响应时序',calc:'跳频时延算子',data:'跳频时序数据',norm:'逆线性归一化'},
  {name:'系统冷启动就绪时延',cat:'响应时序',calc:'启动时序列分析',data:'系统日志数据',norm:'逆线性归一化'},
  
  {name:'干扰空窗期占比',cat:'对抗效果',calc:'空窗期统计算子',data:'干扰时序数据',norm:'逆线性归一化'},
  {name:'干扰成功率',cat:'对抗效果',calc:'干扰成功率算子',data:'对抗结果数据',norm:'线性归一化'},
  {name:'频率占空比',cat:'对抗效果',calc:'占空比计算算子',data:'频率使用数据',norm:'线性归一化'}
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
const getIcoClass = (type) => ({'mid': 'ico-mid', 'leaf': 'ico-lf', 'br': 'ico-br'}[type])
const getIcoChar = (type) => ({'mid': '◈', 'leaf': '◇', 'br': '•'}[type])
const getTypeBadge = (type) => ({'mid': 'tb-mid', 'leaf': 'tb-lf', 'br': 'tb-br'}[type])
const getTypeText = (type) => ({'mid': '中间层', 'leaf': '叶子层', 'br': '分支'}[type])

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

<style scoped lang="scss">
$v: #060911;
$b0: #0a0e18;
$b1: #0e1322;
$b2: #131a2d;
$b3: #182038;
$b4: #1e2844;
$b5: #253050;
$bd: rgba(50,85,155,0.1);
$bd2: rgba(50,85,155,0.22);
$bd3: rgba(50,85,155,0.4);
$gl: rgba(50,120,240,0.12);
$bl: #3478f0;
$bl2: #5b9cf5;
$gn: #0faa78;
$gn2: #30d49a;
$am: #e89a08;
$am2: #f5b840;
$rd: #e04040;
$pp: #9855e0;
$cy: #08a8c5;
$pk: #e06090;
$t1: #dde4f2;
$t2: #90a0be;
$t3: #5a6a85;
$t4: #3c4b62;
$wh: #eef3fb;

.manual-build {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: $v;
  color: $t1;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
}

// TOP BAR
.top {
  height: 44px;
  background: $b0;
  border-bottom: 1px solid $bd;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  position: relative;
  z-index: 80;
  flex-shrink: 0;
  &::after {
    content:''; position:absolute; bottom:0; left:0; right:0; height:1px;
    background: linear-gradient(90deg, transparent, rgba(52,120,240,.15) 20%, rgba(52,120,240,.15) 80%, transparent);
  }
}
.tl { display: flex; align-items: center; gap: 10px; }
.logo { font-family: "Courier New", monospace; font-size: 11px; font-weight: 700; color: $bl; letter-spacing: 2px; }
.tsep { width: 1px; height: 14px; background: $bd2; }
.tname { font-size: 13px; font-weight: 600; color: $wh; }
.tid { font-family: "Courier New", monospace; font-size: 9px; padding: 1px 6px; border: 1px solid $bd2; border-radius: 2px; color: $t3; }
.tbadge {
  font-size: 9px; padding: 2px 7px; border-radius: 9px; display: flex; align-items: center; gap: 3px;
  &::before { content:''; width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
  &.draft { background: rgba(232,154,8,.1); color: $am; }
}
.tr { display: flex; gap: 5px; }

// BUTTONS
.b {
  padding: 4px 12px; font-size: 11px; border-radius: 3px; border: 1px solid $bd; background: transparent; color: $t2; cursor: pointer; transition: all .15s; display: inline-flex; align-items: center; gap: 4px; font-weight: 500;
  &:hover { border-color: $bd2; color: $t1; }
  &.b-p { background: $bl; border-color: $bl; color: #fff; &:hover { background: #2560d0; } }
  &.b-g { background: rgba(15,170,120,.1); border-color: rgba(15,170,120,.3); color: $gn2; &:hover { background: rgba(15,170,120,.18); } }
  &.b-am { background: rgba(232,154,8,.1); border-color: rgba(232,154,8,.3); color: $am2; &:hover { background: rgba(232,154,8,.18); } }
}

// MAIN LAYOUT
.main { flex: 1; display: grid; grid-template-columns: 272px 1fr 308px; overflow: hidden; }

// PANELS
.lp, .rp { background: $b0; display: flex; flex-direction: column; overflow-y: auto; overflow-x: hidden; }
.lp { border-right: 1px solid $bd; }
.rp { border-left: 1px solid $bd; }

.sec, .rp-sec { padding: 11px 13px; border-bottom: 1px solid $bd; }
.sec-t, .rp-sec-t { font-size: 9px; color: $t4; text-transform: uppercase; letter-spacing: 1.2px; font-weight: 700; margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }

.fld, .rp-fld { margin-bottom: 9px; }
.fld-l, .rp-fld-l { font-size: 10px; color: $t3; margin-bottom: 3px; font-weight: 500; display: flex; align-items: center; gap: 3px; }
.rq { color: $rd; font-size: 9px; }

.inp {
  width: 100%; padding: 5px 9px; background: $b2; border: 1px solid $bd; border-radius: 4px; color: $t1; font-size: 12px; font-family: inherit; transition: border-color .15s; outline: none;
  &:focus { border-color: $bl; box-shadow: 0 0 0 2px $gl; }
}
textarea.inp { resize: none; height: 48px; line-height: 1.5; }
.sel { appearance: none; background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1L5 5L9 1' stroke='%235a6a85' stroke-width='1.5' stroke-linecap='round'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 8px center; cursor: pointer; padding-right: 24px; }

// SCOPE TAGS
.tags { display: flex; flex-wrap: wrap; gap: 4px; }
.tag {
  padding: 2px 8px; font-size: 10px; border-radius: 3px; border: 1px solid $bd; color: $t3; cursor: pointer; transition: all .15s; background: transparent;
  &:hover { border-color: $bd2; color: $t2; }
  &.on { border-color: $bl; color: $bl2; background: rgba(52,120,240,.07); }
  &.on2 { border-color: $gn; color: $gn2; background: rgba(15,170,120,.07); }
}

// STATS
.st-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 5px; }
.st { padding: 7px 8px; background: $b2; border-radius: 4px; border: 1px solid $bd; }
.st-v { font-family: "Courier New", monospace; font-size: 17px; font-weight: 700; color: $wh; }
.st-l { font-size: 9px; color: $t4; margin-top: 1px; }

// CRITERIA / VETOS
.crit-row { display: flex; align-items: center; gap: 6px; background: $b2; border: 1px solid $bd; border-radius: 4px; padding: 5px 8px; margin-bottom: 5px; font-size: 11px; }
.cr-thr { width: 38px; font-family: "Courier New", monospace; font-size: 11px; text-align: center; padding: 1px 3px; background: $b3; border: 1px solid $bd; border-radius: 2px; color: $am2; outline: none; }
.cr-del { background: transparent; border: none; color: $t4; cursor: pointer; font-size: 10px; padding: 2px 3px; border-radius: 2px; flex-shrink: 0; &:hover { background: rgba(224,64,64,.08); color: $rd; } }
.add-link, .add-cond-btn { font-size: 10px; color: $bl; background: transparent; border: none; cursor: pointer; padding: 2px 0; display: inline-flex; align-items: center; gap: 3px; &:hover { color: $bl2; } }
.veto-badge { font-size: 8px; padding: 1px 5px; border-radius: 2px; background: rgba(224,64,64,.08); color: $rd; border: 1px solid rgba(224,64,64,.2); flex-shrink: 0; }

// CENTER PANEL
.cp { display: flex; flex-direction: column; overflow: hidden; background: $v; }
.cp-bar { display: flex; align-items: center; justify-content: space-between; padding: 5px 13px; background: $b0; border-bottom: 1px solid $bd; flex-shrink: 0; gap: 8px; }
.cp-bar-l { display: flex; align-items: center; gap: 6px; flex: 1; }
.cp-bar h3 { font-size: 11px; font-weight: 600; color: $t2; }
.cp-btn { padding: 3px 9px; font-size: 10px; border-radius: 3px; border: 1px solid $bd; background: transparent; color: $t3; cursor: pointer; transition: all .12s; &:hover { border-color: $bd2; color: $t2; } &.on { border-color: $bl; color: $bl2; background: rgba(52,120,240,.06); } }

// TREE HEADER
.col-hdr { display: flex; align-items: center; height: 24px; padding-right: 6px; background: $b1; border-bottom: 1px solid $bd; font-size: 9px; color: $t4; text-transform: uppercase; letter-spacing: .8px; font-weight: 600; flex-shrink: 0; }
.ch-name { flex: 1; padding-left: 10px; }
.ch-type { width: 64px; text-align: center; }
.ch-op   { width: 118px; text-align: center; }
.ch-cond { width: 138px; text-align: center; }
.ch-act  { width: 52px; }

.tree-scroll { flex: 1; overflow-y: auto; overflow-x: auto; }
.tree-ct { min-width: 660px; :deep(.el-tree) { background: transparent; color: $t1; } }

// CUSTOM ROW inside el-tree
.r { 
  display: flex; align-items: center; height: 28px; width: 100%; border-bottom: 1px solid rgba(50,85,155,.045); transition: background .1s; cursor: default;
  &:hover { background: rgba(52,120,240,.035); .r-act { opacity: 1; } }
  &.sel { background: rgba(52,120,240,.08); .r-act { opacity: 1; } }
  &.hit { background: rgba(15,170,120,.07); .nm { color: $gn2; } }
  &.miss { opacity:.4; }
  
  .pad-area { position: relative; height: 100%; flex-shrink: 0; }
  .pad-line { position: absolute; width: 1px; background: $bd2; top: 0; bottom: 0; }
  
  .tg { width: 14px; height: 14px; display: flex; align-items: center; justify-content: center; cursor: pointer; flex-shrink: 0; border-radius: 2px; color: $t3; font-size: 7px; margin-right: 2px; transition: transform .12s; &:hover { background: $b4; color: $t2; } &.shut { transform: rotate(-90deg); } &.no { visibility: hidden; } }
  
  .ico { width: 16px; height: 16px; display: flex; align-items: center; justify-content: center; border-radius: 3px; flex-shrink: 0; margin-right: 5px; font-size: 10px; font-weight: 700; }
  .ico-mid { background: rgba(152,85,224,.12); color: $pp; }
  .ico-lf { background: rgba(52,120,240,.1); color: $bl2; }
  .ico-br { background: rgba(232,154,8,.08); color: $am; }
  
  .nm { flex: 1; font-size: 12px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center; gap: 4px; min-width: 0; }
  
  .type-badge { font-size: 8px; padding: 1px 5px; border-radius: 2px; font-weight: 600; letter-spacing: .3px; flex-shrink: 0; width: 64px; text-align: center; }
  .tb-mid { background: rgba(152,85,224,.08); color: $pp; border: 1px solid rgba(152,85,224,.15); }
  .tb-lf { background: rgba(52,120,240,.07); color: $bl2; border: 1px solid rgba(52,120,240,.18); }
  .tb-br { background: rgba(232,154,8,.07); color: $am; border: 1px solid rgba(232,154,8,.15); }
  
  .op-cell { width: 118px; flex-shrink: 0; display: flex; gap: 2px; justify-content: center; overflow: hidden; padding: 0 3px; align-items: center; }
  .op-tag { font-size: 9px; padding: 1px 5px; border-radius: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .op-agg { background: rgba(8,168,197,.08); color: $cy; border: 1px solid rgba(8,168,197,.2); }
  .op-w { background: rgba(152,85,224,.07); color: $pp; border: 1px solid rgba(152,85,224,.15); }
  .op-pass { background: rgba(90,106,133,.1); color: $t3; border: 1px solid $bd; }
  
  .cond-cell { width: 138px; flex-shrink: 0; display: flex; gap: 2px; align-items: center; padding: 0 4px; overflow: hidden; justify-content: center; }
  .ct { font-size: 9px; padding: 1px 5px; border-radius: 2px; white-space: nowrap; border: 1px solid rgba(232,154,8,.25); color: $am; background: rgba(232,154,8,.06); flex-shrink: 0; }
  .ct-free { font-size: 9px; padding: 1px 5px; border-radius: 2px; color: $t4; border: 1px solid $bd; }
  .ct-more { font-size: 9px; color: $t4; flex-shrink: 0; }
  
  .r-act { width: 52px; flex-shrink: 0; display: flex; gap: 1px; opacity: 0; transition: opacity .12s; justify-content: flex-end; padding-right: 2px; }
  .ab { width: 18px; height: 18px; border: none; border-radius: 2px; background: transparent; color: $t4; cursor: pointer; font-size: 11px; display: flex; align-items: center; justify-content: center; font-weight: 500; &:hover { background: $b4; color: $t2; } &.del:hover { background: rgba(224,64,64,.08); color: $rd; } }
}

:deep(.el-tree-node__content) { height: auto !important; padding: 0 !important; background: transparent !important; }
:deep(.el-tree-node__expand-icon) { display: none !important; }

// RIGHT PANEL
.rp-hdr { padding: 8px 13px; border-bottom: 1px solid $bd; flex-shrink: 0; background: $b1; display: flex; align-items: center; justify-content: space-between; }
.rp-title { font-size: 11px; font-weight: 600; color: $wh; display: flex; align-items: center; }
.rp-sub { font-size: 10px; color: $t4; font-weight: 400; }
.rp-body { flex: 1; overflow-y: auto; }
.rp-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: $t4; font-size: 12px; gap: 8px; padding: 24px; text-align: center; }

.info-box { background: $b2; border-radius: 4px; border: 1px solid $bd; padding: 9px 11px; font-size: 11px; color: $t3; line-height: 1.7; .hl { color: $gn2; } .hl-am { color: $am2; } }

.bc { background: $b2; border: 1px solid $bd; border-radius: 4px; padding: 8px 10px; margin-bottom: 6px; cursor: pointer; transition: border-color .12s; &:hover { border-color: $bd2; } }
.bc-name { font-size: 12px; font-weight: 500; color: $t1; margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.bc-conds { display: flex; flex-wrap: wrap; gap: 3px; }
.bc-add-row { display: flex; gap: 5px; margin-top: 2px; }
.bc-add { flex: 1; border: 1px dashed $bd2; border-radius: 4px; padding: 6px 10px; text-align: center; font-size: 11px; color: $t3; cursor: pointer; transition: all .12s; &:hover { border-color: $bl; color: $bl2; } &.bc-add-lib { border-color: rgba(15,170,120,.3); color: $gn; &:hover { border-color: $gn; background: rgba(15,170,120,.04); } } }

.cond-row { display: flex; align-items: center; gap: 5px; background: $b2; border: 1px solid $bd; border-radius: 3px; padding: 5px 8px; margin-bottom: 5px; }
.cond-dim { background: rgba(52,120,240,.1); color: $bl2; padding: 1px 6px; border-radius: 2px; font-size: 10px; flex-shrink: 0; }
.cond-op-badge { background: rgba(232,154,8,.08); color: $am; padding: 1px 5px; border-radius: 2px; font-size: 10px; flex-shrink: 0; }
.cond-val { background: rgba(15,170,120,.08); color: $gn2; padding: 1px 6px; border-radius: 2px; font-size: 10px; flex: 1; min-width: 0; }
.cond-del-btn, .cond-del-sm { background: transparent; border: none; color: $t4; cursor: pointer; font-size: 10px; flex-shrink: 0; padding: 2px 4px; border-radius: 2px; &:hover { background: rgba(224,64,64,.08); color: $rd; } }

.free-tag { display: inline-flex; align-items: center; gap: 4px; background: rgba(15,170,120,.07); border: 1px solid rgba(15,170,120,.2); color: $gn2; padding: 3px 10px; border-radius: 3px; font-size: 11px; }

.data-bind { display: flex; align-items: center; gap: 6px; background: $b2; border: 1px solid $bd; border-radius: 4px; padding: 6px 9px; font-size: 11px; color: $t2; cursor: pointer; transition: border-color .12s; &:hover { border-color: $bd2; } }
.db-dot { width: 6px; height: 6px; border-radius: 50%; background: $gn; flex-shrink: 0; }
.db-chg { font-size: 9px; color: $t4; margin-left: auto; }

.op-sel { display: flex; gap: 4px; flex-wrap: wrap; }
.op-btn { padding: 3px 9px; font-size: 10px; border-radius: 3px; border: 1px solid $bd; background: transparent; color: $t3; cursor: pointer; transition: all .1s; &:hover { border-color: $bd2; color: $t2; } &.on-cy { border-color: $cy; color: $cy; background: rgba(8,168,197,.06); } &.on-pp { border-color: $pp; color: $pp; background: rgba(152,85,224,.06); } &.on-gn { border-color: $gn; color: $gn2; background: rgba(15,170,120,.06); } }

// DIALOGS / MODALS
:deep(.cyber-dialog) {
   background: $b1; border: 1px solid $bd2; border-radius: 6px; box-shadow: 0 20px 60px rgba(0,0,0,.5);
   .el-dialog__header, .el-dialog__body, .el-dialog__footer { padding: 0; margin: 0; }
   .el-dialog__headerbtn { display: none; }
}

.md-h { padding: 13px 16px; border-bottom: 1px solid $bd; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; h3 { font-size: 13px; font-weight: 600; color: $wh; margin:0; } }
.md-x { background: transparent; border: none; color: $t3; cursor: pointer; font-size: 15px; line-height: 1; padding: 2px 5px; border-radius: 2px; &:hover { background: $b3; color: $t1; } }
.md-b { padding: 14px 16px; min-height: 200px; max-height: 60vh; overflow-y: auto; flex: 1; }
.md-f { padding: 10px 16px; border-top: 1px solid $bd; display: flex; justify-content: flex-end; gap: 6px; flex-shrink: 0; }

.md-tabs { display: flex; gap: 0; border-bottom: 1px solid $bd; margin: -14px -16px 14px; padding: 0 16px; }
.md-tab { padding: 8px 14px; font-size: 11px; color: $t3; cursor: pointer; border-bottom: 2px solid transparent; transition: all .12s; background: transparent; border-top: none; border-left: none; border-right: none; &:hover { color: $t2; } &.on { color: $bl2; border-bottom-color: $bl; } }

.type-sel { display: flex; gap: 8px; margin-bottom: 12px; }
.type-opt { flex: 1; border: 1px solid $bd; border-radius: 5px; padding: 10px; cursor: pointer; text-align: center; transition: all .12s; background: $b2; &:hover { border-color: $bd2; } &.on { border-color: $bl; background: rgba(52,120,240,.06); } .to-ico { font-size: 20px; margin-bottom: 5px; } .to-lbl { font-size: 12px; font-weight: 500; color: $t1; } .to-sub { font-size: 10px; color: $t4; margin-top: 2px; } }

.cond-builder-row { display: flex; gap: 5px; margin-bottom: 5px; align-items: center; }
.cond-s { flex: 1; padding: 4px 8px; background: $b3; border: 1px solid $bd; border-radius: 3px; color: $t1; font-size: 11px; outline: none; &:focus { border-color: $bl; } }

.lib-cats { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 8px; }
.lib-cat { padding: 2px 8px; font-size: 10px; border-radius: 3px; border: 1px solid $bd; color: $t3; cursor: pointer; background: transparent; &:hover { border-color: $bd2; color: $t2; } &.on { border-color: $cy; color: $cy; background: rgba(8,168,197,.06); } }
.lib-search { margin-bottom: 10px; }
.lib-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5px; max-height: 320px; overflow-y: auto; }
.lib-item { border: 1px solid $bd; border-radius: 4px; padding: 7px 9px; cursor: pointer; transition: all .1s; background: $b2; &:hover { border-color: $bd2; } &.picked { border-color: $gn; background: rgba(15,170,120,.05); .li-ck { background: rgba(15,170,120,.15); border-color: $gn; } } }
.li-top { display: flex; align-items: center; justify-content: space-between; gap: 4px; margin-bottom: 3px; }
.li-nm { font-size: 12px; font-weight: 500; color: $t1; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.li-ck { width: 14px; height: 14px; border-radius: 2px; border: 1px solid $bd2; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 9px; color: $gn2; }
.li-meta { font-size: 9px; color: $t4; display: flex; gap: 4px; }
.li-cat-badge { background: rgba(8,168,197,.07); color: $cy; padding: 1px 5px; border-radius: 2px; border: 1px solid rgba(8,168,197,.15); }
.li-count { margin-top: 6px; font-size: 10px; color: $t3; }

.sim-attrs { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px; }
.sim-result { background: $b2; border-radius: 4px; border: 1px solid $bd; padding: 10px 12px; }
.sim-sec-t { font-size: 9px; color: $t4; text-transform: uppercase; letter-spacing: .8px; font-weight: 700; margin-bottom: 7px; }
.sim-match { display: flex; align-items: center; gap: 6px; padding: 4px 0; font-size: 11px; border-bottom: 1px solid rgba(50,85,155,.06); &:last-child { border: none; } }
.sim-dot-hit { width: 6px; height: 6px; border-radius: 50%; background: $gn; flex-shrink: 0; }
.sim-dot-miss { width: 6px; height: 6px; border-radius: 50%; background: $t4; flex-shrink: 0; }
.sim-nm-hit { color: $gn2; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sim-nm-miss { color: $t4; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sim-cond-txt { font-size: 9px; color: $t3; flex-shrink: 0; max-width: 150px; text-align: right; }
</style>
