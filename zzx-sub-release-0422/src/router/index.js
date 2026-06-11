import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import TopNavLayout from '@/layout/TopNavLayout.vue'
import GlobalAppLayout from '@/layout/GlobalAppLayout.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/portal/MainControlCenter.vue'),
    meta: { title: '系统首页' }
  },
  {
    path: '/process',
    component: GlobalAppLayout,
    children: [
      {
        path: 'portal',
        name: 'Portal',
        component: () => import('@/views/portal/index.vue'),
        meta: { title: '系统门户' }
      },
      {
        path: 'reception-sys',
        component: TopNavLayout,
        redirect: '/process/reception-sys/overview',
        children: [
          {
            path: 'overview',
            name: 'ReceptionOverview',
            component: () => import('@/views/processSystem/reception/Overview.vue'),
            meta: { title: '状态概览' }
          },
          {
            path: 'requirements',
            name: 'ReceptionRequirements',
            component: () => import('@/views/processSystem/reception/Requirements.vue'),
            meta: { title: '评估需求列表' }
          },
          {
            path: 'templates',
            name: 'ReceptionTemplates',
            component: () => import('@/views/processSystem/reception/Templates.vue'),
            meta: { title: '采集表单模板库' }
          },
          {
            path: 'forms',
            name: 'ReceptionForms',
            component: () => import('@/views/processSystem/reception/Forms.vue'),
            meta: { title: '采集表单列表' }
          },
          {
            path: 'designer',
            name: 'ReceptionDesigner',
            component: () => import('@/views/processSystem/reception/TableDesigner.vue'),
            meta: { title: '表单设计器' }
          },
          {
            path: 'flowRunner',
            name: 'FlowRunner',
            component: () => import('@/views/flowRunner.vue'),
            meta: { title: '流程管理' }
          }
        ]
      },
      {
        path: 'analysis-sys',
        component: TopNavLayout,
        redirect: '/process/analysis-sys/overview',
        children: [
          {
            path: 'overview',
            name: 'AnalysisOverview',
            component: () => import('@/views/processSystem/analysis/Overview.vue'),
            meta: { title: '概览' }
          },
          {
            path: 'tasks',
            name: 'AnalysisTasks',
            component: () => import('@/views/processSystem/analysis/TaskManagement.vue'),
            meta: { title: '汇总整编任务管理' }
          },
          {
            path: 'task-detail',
            name: 'AnalysisTaskDetail',
            component: () => import('@/views/processSystem/analysis/TaskDetail.vue'),
            meta: { title: '汇总任务详情', activeMenu: '/process/analysis-sys/tasks' }
          },
          {
            path: 'interfaces',
            name: 'AnalysisInterfaces',
            component: () => import('@/views/processSystem/analysis/InterfaceManagement.vue'),
            meta: { title: '数据源接口管理' }
          },
          {
            path: 'catalogs',
            name: 'AnalysisCatalogs',
            component: () => import('@/views/processSystem/analysis/CatalogManagement.vue'),
            meta: { title: '数据目录管理' }
          },
          {
            path: 'datasets',
            name: 'AnalysisDatasets',
            component: () => import('@/views/processSystem/analysis/DatasetManagement.vue'),
            meta: { title: '数据集管理' }
          }
        ]
      },
      {
        path: 'process-sys',
        component: TopNavLayout,
        redirect: '/process/process-sys/tasks',
        children: [
          {
            path: 'tasks',
            name: 'ProcessTasks',
            component: () => import('@/views/processSystem/process/TaskManagement.vue'),
            meta: { title: '预处理任务管理' }
          },
          {
            path: 'operators',
            name: 'ProcessOperators',
            component: () => import('@/views/processSystem/process/OperatorManagement.vue'),
            meta: { title: '算子管理' }
          },
          {
            path: 'algorithm',
            name: 'ProcessAlgorithm',
            component: () => import('@/views/zhpg/sfmx/algorithm/index.vue'),
            meta: { title: '算法管理' }
          },
          {
            path: 'templates',
            name: 'ProcessTemplates',
            component: () => import('@/views/processSystem/process/WorkflowTemplate.vue'),
            meta: { title: '预处理流程模板' }
          },

        ]
      },
      {
        path: 'mining-sys',
        component: TopNavLayout,
        redirect: '/process/mining-sys/tasks',
        children: [
          {
            path: 'tasks',
            name: 'MiningTasks',
            component: () => import('@/views/processSystem/mining/TaskManagement.vue'),
            meta: { title: '数据分析挖掘任务管理' }
          },
          {
            path: 'models',
            name: 'MiningModels',
            component: () => import('@/views/processSystem/mining/ModelManagement.vue'),
            meta: { title: '算法模型管理' }
          }
        ]
      },
      {
        path: 'resource-sys',
        component: TopNavLayout,
        redirect: '/process/resource-sys/overview',
        children: [
          {
            path: 'overview',
            name: 'ResourceOverview',
            component: () => import('@/views/processSystem/resource/Overview.vue'),
            meta: { title: '资源池' }
          }
        ]
      },
      {
        path: 'knowledge-sys',
        component: TopNavLayout,
        redirect: '/process/knowledge-sys/ontology',
        children: [
          {
            path: 'ontology',
            name: 'OntologyBuilder',
            component: () => import('@/views/processSystem/knowledge/OntologyBuilder.vue')
          },
          {
            path: 'management',
            name: 'KnowledgeManager',
            component: () => import('@/views/processSystem/knowledge/KnowledgeManager.vue')
          },
          {
            path: 'association',
            name: 'AssociationAnalysis',
            component: () => import('@/views/processSystem/knowledge/AssociationAnalysis.vue')
          },
          {
            path: 'situation',
            name: 'SituationManager',
            component: () => import('@/views/processSystem/knowledge/SituationManager.vue')
          },
          {
            path: 'traceability',
            name: 'TraceabilityManager',
            component: () => import('@/views/processSystem/knowledge/TraceabilityManager.vue')
          },
          {
            path: 'lifecycle',
            name: 'LifecycleOverview',
            component: () => import('@/views/processSystem/knowledge/LifecycleOverview.vue')
          }
        ]
      },
      {
        path: 'skeleton-sys',
        name: 'SkeletonSys',
        meta: { title: '任务骨架管理' },
        children: [
          {
            path: 'overview',
            name: 'SkeletonOverview',
            component: () => import('@/views/processSystem/skeleton/Overview.vue'),
            meta: { title: '骨架总览' }
          },
          {
            path: 'builder',
            name: 'SkeletonBuilder',
            component: () => import('@/views/processSystem/skeleton/Builder.vue'),
            meta: { title: '骨架构建' }
          },
          {
            path: 'manual-build',
            name: 'ManualSkeletonBuilder',
            component: () => import('@/views/processSystem/skeleton/manualBuild.vue'),
            meta: { title: '手动构建骨架' }
          }
        ]
      }
    ]
  },
  {
    path: '/major',
    component: GlobalAppLayout,
    children: [
      {
        path: 'new-portal',
        name: 'NewPortal',
        component: () => import('@/views/portal/systemAnalysis.vue'),
        meta: { title: '综合管控门户' }
      },
      {
        path: 'requirement-sys',
        component: TopNavLayout,
        redirect: '/major/requirement-sys/portal',
        children: [
          {
            path: 'portal',
            name: 'RequirementPortal',
            component: () => import('@/views/portal/requirementPortal.vue'),
            meta: { title: '评估需求导航' }
          },
          {
            path: 'import',
            name: 'RequirementImport',
            component: () => import('@/views/requirements/ImportData.vue'),
            meta: { title: '导入一案三纲' }
          },
          {
            path: 'skeleton-match',
            name: 'SkeletonMatch',
            component: () => import('@/views/requirements/SkeletonMatch.vue'),
            meta: { title: '匹配任务骨架' }
          },
          {
            path: 'management',
            name: 'RequirementManagement',
            component: () => import('@/views/requirements/newRequire.vue'),
            meta: { title: '评估需求管理' }
          },
          {
            path: 'generate',
            name: 'RequirementGenerate',
            component: () => import('@/views/requirements/GenerateReq.vue'),
            meta: { title: '生成评估需求' }
          },
          {
            path: 'adjust',
            name: 'RequirementAdjust',
            component: () => import('@/views/requirements/AdjustReq.vue'),
            meta: { title: '需求调整优化' }
          },
          {
            path: 'distribute',
            name: 'RequirementDistribute',
            component: () => import('@/views/requirements/DistributeReq.vue'),
            meta: { title: '需求下发与集汇' }
          },
          {
            path: 'complete',
            name: 'RequirementComplete',
            component: () => import('@/views/requirements/CompleteReq.vue'),
            meta: { title: '完整评估需求' }
          },
          {
            path: 'completeness',
            name: 'RequirementCompleteness',
            component: () => import('@/views/requirements/CompletenessAnalysis.vue'),
            meta: { title: '完备度分析' }
          },
          {
            path: 'template/flow',
            name: 'FlowTemplate',
            component: () => import('@/views/requirements/template/FlowTemplate.vue'),
            meta: { title: '需求设计流程模板管理' }
          },
          {
            path: 'template/completeness',
            name: 'CompletenessTemplate',
            component: () => import('@/views/requirements/template/CompletenessTemplate.vue'),
            meta: { title: '完备度分析模板管理' }
          },
          {
            path: 'template/report',
            name: 'ReportTemplate',
            component: () => import('@/views/requirements/template/ReportTemplate.vue'),
            meta: { title: '需求报告模板管理' }
          }
        ]
      },
      {
        path: 'program-mgmt',
        component: TopNavLayout,
        redirect: '/major/program-mgmt/list',
        children: [
          {
            path: 'list',
            name: 'ProgramList',
            component: () => import('@/views/projectManagement/index.vue'),
            meta: { title: '评估工程管理' }
          },
          {
            path: 'equipment',
            name: 'EquipmentManagement',
            component: () => import('@/views/systemUpdate/capabilityResource/equipmentManagement.vue'),
            meta: { title: '装备管理' }
          },
          {
            path: 'super-network',
            name: 'SuperNetwork',
            component: () => import('@/views/systemUpdate/projectManagement/component/superNetworkBuild.vue'),
            meta: { title: '超网络构建' }
          },
          {
            path: 'detail/:id',
            name: 'ProgramDetail',
            component: () => import('@/views/projectManagement/ProjectDetail.vue'),
            meta: { title: '工程详情' }
          },
          {
            path: 'build',
            name: 'ProgramBuild',
            component: () => import('@/views/projectManagement/ProjectBuild.vue'),
            meta: { title: '工程构建' }
          },
          {
            path: 'eval-result',
            name: 'ProgramEvalResult',
            component: () => import('@/views/zhpg/pgjg/index.vue'),
            meta: { title: '评估计算及结果管理' }
          }
        ]
      },
      {
        path: 'capability-resource',
        component: TopNavLayout,
        redirect: '/major/capability-resource/indicator',
        children: [
          {
            path: 'indicator',
            name: 'ResourceIndicator',
            component: () => import('@/views/zhpg/zbgj/indicator/index.vue'),
            meta: { title: '指标管理' }
          },
          {
            path: 'indicator-system',
            name: 'ResourceIndicatorSystem',
            component: () => import('@/views/zhpg/zbgj/indicatorSystem/index.vue'),
            meta: { title: '指标体系管理' }
          },
          {
            path: 'algorithm',
            name: 'ResourceAlgorithm',
            component: () => import('@/views/zhpg/sfmx/algorithm/index.vue'),
            meta: { title: '算法管理' }
          },
          {
            path: 'report',
            name: 'ResourceReport',
            component: () => import('@/views/zhpg/report/instances.vue'),
            meta: { title: '评估报告管理' }
          }
        ]
      },
      {
        path: 'template-library',
        component: TopNavLayout,
        redirect: '/major/template-library/calc-flow',
        children: [
          {
            path: 'calc-flow',
            name: 'TemplateCalcFlow',
            component: () => import('@/views/zhpg/zhfx/calcFlow/index.vue'),
            meta: { title: '综合评估流程模板管理' }
          },
          {
            path: 'task',
            name: 'TemplateTask',
            component: () => import('@/views/zhpg/rwgj/taskTemplate/index.vue'),
            meta: { title: '任务模板管理' }
          },
          {
            path: 'report',
            name: 'TemplateReport',
            component: () => import('@/views/zhpg/report/manage.vue'),
            meta: { title: '评估报告模板管理' }
          },
          {
            path: 'criterion',
            name: 'TemplateCriterion',
            component: () => import('@/views/zhpg/rwgj/criterion/index.vue'),
            meta: { title: '评估准则管理' }
          },
          {
            path: 'criterion-designer',
            name: 'TemplateCriterionDesigner',
            component: () => import('@/views/zhpg/rwgj/criterion/designer.vue'),
            meta: { title: '评估准则设计' }
          }
        ]
      },
      {
        path: 'system-cooperation',
        component: TopNavLayout,
        redirect: '/major/system-cooperation/combat-profile',
        children: [
          {
            path: 'combat-profile',
            name: 'CombatProfile',
            component: () => import('@/views/systemUpdate/systemCooperation/combatProfile.vue'),
            meta: { title: '体系作战剖面' }
          },
          {
            path: 'detail',
            name: 'CombatProfileDetail',
            component: () => import('@/views/systemUpdate/systemCooperation/CombatProfileAdd.vue'),
            meta: { title: '作战剖面详情' }
          },
          {
            path: 'evaluation-plan',
            name: 'EvaluationPlan',
            component: () => import('@/views/systemUpdate/systemCooperation/evaluationManagement.vue'),
            meta: { title: '评估方案管理' }
          },
          // {
          //   path: 'evaluation-plan-create',
          //   name: 'EvaluationPlanCreate',
          //   component: () => import('@/views/systemUpdate/systemCooperation/evaluationPlanCreate.vue'),
          //   meta: { title: '创建评估方案' }
          // },
          // {
          //   path: 'indicator-network',
          //   name: 'IndicatorNetwork',
          //   component: () => import('@/views/systemCooperation/indicatorNetwork.vue'),
          //   meta: { title: '指标网管理' }
          // },
          // {
          //   path: 'algorithm-network',
          //   name: 'AlgorithmNetwork',
          //   component: () => import('@/views/systemCooperation/algorithmNetwork.vue'),
          //   meta: { title: '算法网管理' }
          // },
          // {
          //   path: 'effectiveness-table',
          //   name: 'EffectivenessTable',
          //   component: () => import('@/views/systemCooperation/effectivenessTable.vue'),
          //   meta: { title: '效能射表管理' }
          // },
          // {
          //   path: 'equipment-strategy',
          //   name: 'EquipmentStrategy',
          //   component: () => import('@/views/systemCooperation/equipmentSystemStrategy.vue'),
          //   meta: { title: '装备体系作战运用策略管理' }
          // },
          {
            path: 'power-network',
            name: 'PowerNetwork',
            component: () => import('@/views/systemUpdate/systemCooperation/equipmentSystemPower/powerStructureNetwork.vue'),
            meta: { title: '力量结构网管理' }
          },
          {
            path: 'power-network-detail',
            name: 'PowerNetworkDetail',
            component: () => import('@/views/systemUpdate/systemCooperation/equipmentSystemPower/powerStructureNetwork.vue'),
            meta: { title: '力量结构网详情' }
          },
          {
            path: 'structure-config',
            name: 'StructureConfig',
            component: () => import('@/views/systemUpdate/systemCooperation/equipmentSystemPower/structureConfiguration.vue'),
            meta: { title: '结构构型管理' }
          },
          {
            path: 'combat-activity',
            name: 'CombatActivity',
            component: () => import('@/views/systemUpdate/systemCooperation/oodaLink/combatActivity.vue'),
            meta: { title: '作战活动管理' }
          },
          {
            path: 'ooda-link',
            name: 'OodaLink',
            component: () => import('@/views/systemUpdate/systemCooperation/oodaLink/cooperationOodaLink.vue'),
            meta: { title: '协同作战OODA链路管理' }
          },
          {
            path: 'coordination-strategy',
            name: 'CoordinationStrategy',
            component: () => import('@/views/systemUpdate/systemCooperation/oodaLink/equipmentCoordinationStrategy.vue'),
            meta: { title: '装备协同组合运用策略管理' }
          },
          {
            path: 'ooda-network',
            name: 'OodaNetwork',
            component: () => import('@/views/systemUpdate/systemCooperation/oodaLink/oodaNetwork.vue'),
            meta: { title: 'OODA网管理' }
          },
          {
            path: 'task-criterion',
            name: 'MajorTaskCriterion',
            component: () => import('@/views/systemUpdate/systemCooperation/systemCombatTask/taskCriterion.vue'),
            meta: { title: '任务判据管理' }
          },
          {
            path: 'task-mgmt',
            name: 'MajorTaskMgmt',
            component: () => import('@/views/systemUpdate/systemCooperation/systemCombatTask/taskManagement.vue'),
            meta: { title: '任务管理' }
          },
          // {
          //   path: 'task-network',
          //   name: 'MajorTaskNetwork',
          //   component: () => import('@/views/systemUpdate/systemCooperation/systemCombatTask/taskNetwork.vue'),
          //   meta: { title: '任务网管理' }
          // },
          {
            path: 'sub-app/:appName/:pathMatch(.*)*',
            name: 'SubAppHost',
            component: () => import('@/views/SubAppHost.vue'),
            meta: { title: '外部子系统' }
          }
        ]
      }
    ]
  },
  {
    path: '/unified-home',
    component: GlobalAppLayout,
    children: [
      {
        path: '',
        name: 'UnifiedHome',
        component: () => import('@/views/portal/UnifiedHome.vue'),
        meta: { title: '系统总体首页' }
      }
    ]
  },
  {
    path: '/',
    redirect: '/unified-home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: routes,
})
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/unified-home',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [

  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  },
]
export default router
