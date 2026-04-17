# Code Review 文档：center-Vue3-master 变更汇总

> 项目：基于 RuoYi-Vue3 的前后端分离管理系统
> 生成日期：2026-04-07
> Git 分支：main
> 最近提交：54de122 原型初版（暂存）

---

## 一、变更概况

| 指标 | 数值 |
|------|------|
| 变更文件数 | 66 个 |
| 新增行数 | +1,648 |
| 删除行数 | -24,476 |
| **净变化** | **-22,828 行** |

### 核心趋势
1. **大量清理**：删除了整个 `src/views/systemPlus/` 下的旧业务页面（约 -23,000 行）
2. **图编辑器重构**：GraphEditor 模块核心组件重写
3. **新增 zhpg 模块**：侦察目标相关的新业务模块
4. **样式主题更新**：新增科幻主题、指挥控制主题样式

---

## 二、新增内容

### 2.1 新增 API 文件

| 文件路径 | 说明 |
|----------|------|
| `src/api/graph.js` | 图编辑器数据接口 |
| `src/api/zhpg/` | zhpg 模块 API（目录） |
| `src/api/systemPlus/systemCooperation/taskCriterion.js` | 任务准则 API |
| `src/api/systemPlus/systemCooperation/taskManagement.js` | 任务管理 API |
| `src/api/systemPlus/systemCooperation/taskNetwork.js` | 任务网络 API |
| `src/api/capabilityResource/` | 能力资源 API（目录） |

### 2.2 新增组件

| 文件路径 | 说明 |
|----------|------|
| `src/components/CesiumViewer.vue` | Cesium 三维地球查看器组件 |

### 2.3 新增工具类

| 文件路径 | 说明 |
|----------|------|
| `src/utils/zhpg/` | zhpg 模块工具函数（目录） |
| `src/utils/zhpgConductionAlgorithms.js` | 传导算法实现 |
| `src/utils/zhpgIndicatorTreeJson.js` | 指标树 JSON 数据 |
| `src/constants/` | 常量定义目录 |

### 2.4 新增样式文件

| 文件路径 | 说明 |
|----------|------|
| `src/assets/styles/mission-control.scss` | 指挥控制主题样式 |
| `src/assets/styles/sci-fi-theme.scss` | 科幻主题样式 |
| `src/assets/styles/v2/` | V2 版本样式目录 |

### 2.5 新增页面

| 文件路径 | 说明 |
|----------|------|
| `src/views/zhpg/` | zhpg 业务页面目录 |
| `src/views/systemUpdate/` | 系统更新页面目录 |

---

## 三、重构/修改内容

### 3.1 图编辑器模块（核心改动）

| 文件 | 变更行数 | 主要改动 |
|------|----------|----------|
| `src/components/GraphEditor/GraphContainer.vue` | ~253 | 图编辑器容器组件重构 |
| `src/components/GraphEditor/core/adapters/G6Adapter.js` | ~403 | AntV G6 适配器核心逻辑重写 |
| `src/components/GraphEditor/core/adapters/X6Adapter.js` | ~2 | X6 适配器微调 |
| `src/components/GraphEditor/core/registry/NodeRegistry.js` | ~101 | 节点注册表重构 |
| `src/components/GraphEditor/utils/dataConverter.js` | ~255 | 数据转换工具重写 |
| `src/components/GraphEditor/config/edges.js` | ~14 | 边配置调整 |
| `src/components/GraphEditor/config/nodes.js` | ~6 | 节点配置微调 |
| `src/components/GraphEditor/Toolbar.vue` | ~24 | 工具栏调整 |
| `src/store/modules/graph.js` | ~351 | 图状态管理重构 |

### 3.2 业务页面修改

| 文件 | 变更行数 | 主要改动 |
|------|----------|----------|
| `src/views/index.vue` | ~1467 → 精简 | 首页大幅精简，移除大量内嵌模块 |
| `src/views/login.vue` | ~98 | 登录页调整 |

### 3.3 布局与样式调整

| 文件 | 主要改动 |
|------|----------|
| `src/assets/styles/variables.module.scss` | 主题变量重构（106 行变更） |
| `src/assets/styles/element-ui.scss` | Element Plus 样式覆盖（107 行变更） |
| `src/assets/styles/ruoyi.scss` | 若依框架样式调整（66 行变更） |
| `src/assets/styles/index.scss` | 全局样式调整（22 行变更） |
| `src/assets/styles/sidebar.scss` | 侧边栏样式调整（21 行变更） |
| `src/assets/styles/transition.scss` | 过渡动画样式（6 行变更） |
| `src/layout/components/Navbar.vue` | 导航栏调整（45 行变更） |
| `src/layout/components/TagsView/index.vue` | 标签页视图调整（70 行变更） |
| `src/layout/components/AppMain.vue` | 主内容区调整（27 行变更） |
| `src/layout/components/Sidebar/Logo.vue` | 侧边栏 Logo 调整（29 行变更） |
| `src/layout/components/TopBar/index.vue` | 顶部栏微调（6 行变更） |
| `src/layout/components/Settings/index.vue` | 设置组件微调（2 行变更） |

### 3.4 路由与状态管理

| 文件 | 主要改动 |
|------|----------|
| `src/router/index.js` | 路由配置调整（21 行变更） |
| `src/store/modules/settings.js` | 设置状态调整（6 行变更） |
| `src/store/modules/user.js` | 用户状态调整（10 行变更） |

### 3.5 工具模块调整

| 文件 | 主要改动 |
|------|----------|
| `src/utils/request.js` | 请求封装调整（13 行变更） |
| `src/utils/auth.js` | 认证工具微调（2 行变更） |
| `src/api/tool/gen.js` | 代码生成工具调整（9 行变更） |
| `vite.config.js` | Vite 构建配置修改（4 行变更） |

---

## 四、删除内容

### 4.1 已删除页面（大量清理）

以下 `src/views/systemPlus/` 下的页面已被删除：

| 目录 | 删除的页面/组件 |
|------|-----------------|
| `projectManagement/` | ProjectBuild.vue, ProjectDetail.vue, ComprehensiveAnalysis.vue, index.vue, HyperNetwork.vue, NetworkGraph.vue |
| `systemCooperation/` | combatProfile.vue, CombatProfileAdd.vue, algorithmNetwork.vue, effectivenessTable.vue, equipmentSystemStrategy.vue, evaluationPlan.vue, evaluationPlanCreate.vue, indicatorNetwork.vue |
| `systemCooperation/equipment-system/` | EquipmentSystemX6.vue, SubCanvasComponent.vue |
| `systemCooperation/equipmentSystemPower/` | powerStructureNetwork.vue, PowerStructureNetworkDetail.vue, structureConfiguration.vue |
| `systemCooperation/oodaLink/` | combatActivity.vue, cooperationOodaLink.vue, equipmentCoordinationStrategy.vue, oodaNetwork.vue, demo/ |
| `systemCooperation/systemCombatTask/` | taskCriterion.vue, taskManagement.vue, taskNetwork.vue |
| `capabilityResource/` | indicatorManagement.vue, indicatorSystem.vue, evaluationReport.vue |
| `templateLibrary/` | analysisProcess/index.vue, evaluationReport/index.vue |

### 4.2 其他删除

| 文件 | 说明 |
|------|------|
| `.github/FUNDING.yml` | GitHub 赞助配置 |
| `src/views/systemPlus/projectManagement/component/NetworkGraph.vue` | 网络图组件（已移至 GraphEditor） |

---

## 五、合代码建议

### 5.1 高优先级 Review 项

| 优先级 | 模块 | 原因 |
|--------|------|------|
| **高** | G6Adapter.js | 核心适配器重写，变更量最大（403 行），需确保图编辑功能正常 |
| **高** | views/index.vue | 首页大幅精简（-1300+ 行），需确认功能完整性 |
| **高** | GraphContainer.vue | 图编辑器主容器重构，需确认各功能入口正常 |
| **高** | dataConverter.js | 数据转换逻辑重写，影响数据流，需验证数据正确性 |

### 5.2 中优先级 Review 项

| 优先级 | 模块 | 原因 |
|--------|------|------|
| **中** | store/modules/graph.js | 状态管理重构，可能影响全局状态 |
| **中** | NodeRegistry.js | 节点注册表变更，影响节点渲染 |
| **中** | router/index.js | 路由调整，需确认导航正常 |

### 5.3 需确认的问题

1. **删除的 systemPlus 页面**：确认这些页面确实不再需要，避免误删重要功能
2. **新增的 zhpg 模块**：确认是否与现有功能冲突或重复
3. **样式变更**：v2 样式、科幻主题可能影响整体 UI，需全屏回归测试
4. **CesiumViewer.vue**：新增的三维组件，需确认依赖是否完整引入
5. **GraphEditor 双适配器**：同时保留 G6 和 X6 适配器，需确认切换逻辑

### 5.4 建议的测试场景

1. 登录/登出流程
2. 首页各模块入口跳转
3. 图编辑器新增/编辑/删除节点和边
4. 切换 G6/X6 适配器
5. zhpg 模块各页面功能
6. 整体 UI 样式是否正常

---

## 六、技术依赖变更

### 6.1 新增依赖

无新增 npm 依赖（从 package.json 未见变化）

### 6.2 已有依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| @antv/g6 | ^5.0.51 | 图编辑器 G6 引擎 |
| @antv/x6 | ^3.1.7 | 图编辑器 X6 引擎 |
| element-plus | 2.13.1 | UI 组件库 |
| vue | 3.5.26 | 核心框架 |

---

## 七、附录

### Git 提交历史

```
54de122 原型初版（暂存
c1999d2 图编辑器组件
2630690 init01
```

### 相关文档

- 配套后端：RuoYi-Vue / RuoYi-Vue-fast
- 官方文档：http://doc.ruoyi.vip
- 演示地址：http://vue.ruoyi.vip

---

*本文档由 Claude Code 自动生成，如有疑问请联系项目负责人。*
