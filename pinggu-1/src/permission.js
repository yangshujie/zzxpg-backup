import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, setToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./views/**/*.vue')

NProgress.configure({ showSpinner: false })

// 静态菜单数据 - 结合自定义系统Plus和原有系统管理菜单
const staticMenuData = []
// const staticMenuData = [
//   // {
//   //   name: 'Overview',
//   //   path: '/overview',
//   //   hidden: false,
//   //   component: 'systemPlus/overview/Index',
//   //   meta: { title: '系统概览', icon: 'monitor', noCache: false }
//   // },
//   {
//     name: 'Task',
//     path: '/project',
//     hidden: false,
//     redirect: '/project/index',
//     component: 'Layout',
//     meta: { title: '工程子系统', icon: 'list', noCache: false },
//     children: [
//       {
//         name: 'ProjectManagement',
//         path: 'index',
//         hidden: false,
//         component: 'systemPlus/projectManagement/index',
//         meta: { title: '评估工程管理', icon: 'list', noCache: false }
//       },
//       // {
//       //   name: 'ProjectBuild',
//       //   path: 'build',
//       //   hidden: false,
//       //   component: 'systemPlus/projectManagement/ProjectDetail',
//       //   meta: { title: '评估工程构建', icon: 'build', noCache: false }
//       // }
//     ]
//   },

//   // 能力资源菜单
//   {
//     name: 'CapabilityResource',
//     path: '/capability-resource',
//     hidden: false,
//     redirect: '/capability-resource/indicator',
//     component: 'Layout',
//     meta: { title: '能力资源', icon: 'skill', noCache: false },
//     children: [
//       {
//         name: 'IndicatorManagement',
//         path: 'indicator',
//         hidden: false,
//         component: 'systemPlus/capabilityResource/indicator/index',
//         meta: { title: '指标管理', icon: 'chart', noCache: false }
//       },
//       {
//         name: 'IndicatorSystemManagement',
//         path: 'indicator-system',
//         hidden: false,
//         component: 'systemPlus/capabilityResource/indicatorSystem/index',
//         meta: { title: '指标体系管理', icon: 'component', noCache: false }
//       },
//       {
//         name: 'EvaluationReportManagement',
//         path: 'evaluation-report',
//         hidden: false,
//         component: 'systemPlus/capabilityResource/evaluationReport/index',
//         meta: { title: '评估报告管理', icon: 'documentation', noCache: false }
//       }
//     ]
//   },

//   // 模版库管理菜单
//   {
//     name: 'TemplateLibrary',
//     path: '/template-library',
//     hidden: false,
//     redirect: '/template-library/analysis-process',
//     component: 'Layout',
//     meta: { title: '模版库管理', icon: 'template', noCache: false },
//     children: [
//       {
//         name: 'AnalysisProcessTemplate',
//         path: 'analysis-process',
//         hidden: false,
//         component: 'systemPlus/templateLibrary/analysisProcess/index',
//         meta: { title: '综合分析流程模版管理', icon: 'flow', noCache: false }
//       },
//       {
//         name: 'EvaluationReportTemplate',
//         path: 'evaluation-report',
//         hidden: false,
//         component: 'systemPlus/templateLibrary/evaluationReport/index',
//         meta: { title: '评估报告模版管理', icon: 'report', noCache: false }
//       }
//     ]
//   },

//   // 体系协同菜单
//   {
//     name: 'SystemCooperation',
//     path: '/system-cooperation',
//     hidden: false,
//     redirect: '/system-cooperation/combat-profile',
//     component: 'Layout',
//     meta: { title: '体系协同', icon: 'cooperation', noCache: false },
//     children: [
//       // 体系作战剖面
//       {
//         name: 'CombatProfile',
//         path: 'combat-profile',
//         hidden: false,
//         component: 'systemPlus/systemCooperation/combatProfile',
//         meta: { title: '体系作战剖面', icon: 'profile', noCache: false }
//       },
//       {
//         name: 'CombatProfileAdd',
//         path: 'combat-profile/add',
//         hidden: true,
//         component: 'systemPlus/systemCooperation/CombatProfileAdd',
//         meta: { title: '新增体系作战剖面', icon: 'profile', noCache: false }
//       },
//       // 装备体系力量
//       {
//         name: 'EquipmentSystemPower',
//         path: 'equipment-system-power',
//         hidden: false,
//         redirect: '/system-cooperation/equipment-system-power/structure-configuration',
//         component: 'ParentView',
//         meta: { title: '装备体系力量', icon: 'power', noCache: false },
//         children: [
//           {
//             name: 'StructureConfiguration',
//             path: 'structure-configuration',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/equipmentSystemPower/structureConfiguration',
//             meta: { title: '结构构型管理', icon: 'structure', noCache: false }
//           },
//           {
//             name: 'PowerStructureNetwork',
//             path: 'power-structure-network',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/equipmentSystemPower/powerStructureNetwork',
//             meta: { title: '力量结构网管理', icon: 'network', noCache: false }
//           }
//         ]
//       },
//       // 体系作战任务
//       {
//         name: 'SystemCombatTask',
//         path: 'system-combat-task',
//         hidden: false,
//         redirect: '/system-cooperation/system-combat-task/task-criterion',
//         component: 'ParentView',
//         meta: { title: '体系作战任务', icon: 'task', noCache: false },
//         children: [
//           {
//             name: 'TaskCriterion',
//             path: 'task-criterion',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/systemCombatTask/taskCriterion',
//             meta: { title: '任务判据管理', icon: 'criterion', noCache: false }
//           },
//           {
//             name: 'TaskManagement',
//             path: 'task-management',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/systemCombatTask/taskManagement',
//             meta: { title: '任务管理', icon: 'management', noCache: false }
//           },
//           {
//             name: 'TaskNetwork',
//             path: 'task-network',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/systemCombatTask/taskNetwork',
//             meta: { title: '任务网管理', icon: 'network', noCache: false }
//           }
//         ]
//       },
//       // OODA链路
//       {
//         name: 'OodaLink',
//         path: 'ooda-link',
//         hidden: false,
//         redirect: '/system-cooperation/ooda-link/combat-activity',
//         component: 'ParentView',
//         meta: { title: 'OODA链路', icon: 'link', noCache: false },
//         children: [
//           {
//             name: 'CombatActivity',
//             path: 'combat-activity',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/oodaLink/combatActivity',
//             meta: { title: '作战活动管理', icon: 'activity', noCache: false }
//           },
//           {
//             name: 'EquipmentCoordinationStrategy',
//             path: 'equipment-coordination-strategy',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/oodaLink/equipmentCoordinationStrategy',
//             meta: { title: '装备协同组合运用策略管理', icon: 'strategy', noCache: false }
//           },
//           {
//             name: 'CooperationOodaLink',
//             path: 'cooperation-ooda-link',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/oodaLink/cooperationOodaLink',
//             meta: { title: '协同作战OODA链路管理', icon: 'cooperation', noCache: false }
//           },
//           {
//             name: 'OodaNetwork',
//             path: 'ooda-network',
//             hidden: false,
//             component: 'systemPlus/systemCooperation/oodaLink/oodaNetwork',
//             meta: { title: 'OODA网管理', icon: 'network', noCache: false }
//           }
//         ]
//       },
//       // 指标网管理
//       {
//         name: 'IndicatorNetwork',
//         path: 'indicator-network',
//         hidden: false,
//         component: 'systemPlus/systemCooperation/indicatorNetwork',
//         meta: { title: '指标网管理', icon: 'indicator', noCache: false }
//       },
//       // 算法网管理
//       {
//         name: 'AlgorithmNetwork',
//         path: 'algorithm-network',
//         hidden: false,
//         component: 'systemPlus/systemCooperation/algorithmNetwork',
//         meta: { title: '算法网管理', icon: 'algorithm', noCache: false }
//       },
//       // 评估方案管理
//       {
//         name: 'EvaluationPlan',
//         path: 'evaluation-plan',
//         hidden: false,
//         component: 'systemPlus/systemCooperation/evaluationPlan',
//         meta: { title: '评估方案管理', icon: 'plan', noCache: false }
//       },
//       // 效能射表管理
//       {
//         name: 'EffectivenessTable',
//         path: 'effectiveness-table',
//         hidden: false,
//         component: 'systemPlus/systemCooperation/effectivenessTable',
//         meta: { title: '效能射表管理', icon: 'table', noCache: false }
//       },
//       // 装备体系作战运用策略管理
//       {
//         name: 'EquipmentSystemStrategy',
//         path: 'equipment-system-strategy',
//         hidden: false,
//         component: 'systemPlus/systemCooperation/equipmentSystemStrategy',
//         meta: { title: '装备体系作战运用策略管理', icon: 'strategy', noCache: false }
//       }
//     ]
//   },

//   // 原有的若依系统管理菜单
//   {
//     name: 'System',
//     path: '/system',
//     hidden: false,
//     redirect: 'noRedirect',
//     component: 'Layout',
//     meta: { title: '系统管理', icon: 'system', noCache: false },
//     children: [
//       {
//         name: 'User',
//         path: 'user',
//         hidden: false,
//         component: 'system/user/index',
//         meta: { title: '用户管理', icon: 'user', noCache: false }
//       },
//       {
//         name: 'Role',
//         path: 'role',
//         hidden: false,
//         component: 'system/role/index',
//         meta: { title: '角色管理', icon: 'peoples', noCache: false }
//       },
//       {
//         name: 'Menu',
//         path: 'menu',
//         hidden: false,
//         component: 'system/menu/index',
//         meta: { title: '菜单管理', icon: 'tree-table', noCache: false }
//       },
//       {
//         name: 'Dept',
//         path: 'dept',
//         hidden: false,
//         component: 'system/dept/index',
//         meta: { title: '部门管理', icon: 'tree', noCache: false }
//       },
//       {
//         name: 'Post',
//         path: 'post',
//         hidden: false,
//         component: 'system/post/index',
//         meta: { title: '岗位管理', icon: 'post', noCache: false }
//       },
//       {
//         name: 'Dict',
//         path: 'dict',
//         hidden: false,
//         component: 'system/dict/index',
//         meta: { title: '字典管理', icon: 'dict', noCache: false }
//       },
//       {
//         name: 'Config',
//         path: 'config',
//         hidden: false,
//         component: 'system/config/index',
//         meta: { title: '参数设置', icon: 'edit', noCache: false }
//       },
//       {
//         name: 'Notice',
//         path: 'notice',
//         hidden: false,
//         component: 'system/notice/index',
//         meta: { title: '通知公告', icon: 'message', noCache: false }
//       }
//     ]
//   },
//   {
//     name: 'Monitor',
//     path: '/monitor',
//     hidden: false,
//     redirect: 'noRedirect',
//     component: 'Layout',
//     meta: { title: '系统监控', icon: 'monitor', noCache: false },
//     children: [
//       {
//         name: 'Online',
//         path: 'online',
//         hidden: false,
//         component: 'monitor/online/index',
//         meta: { title: '在线用户', icon: 'online', noCache: false }
//       },
//       {
//         name: 'Job',
//         path: 'job',
//         hidden: false,
//         component: 'monitor/job/index',
//         meta: { title: '定时工程', icon: 'job', noCache: false }
//       },
//       {
//         name: 'Druid',
//         path: 'druid',
//         hidden: false,
//         component: 'monitor/druid/index',
//         meta: { title: '数据监控', icon: 'druid', noCache: false }
//       },
//       {
//         name: 'Server',
//         path: 'server',
//         hidden: false,
//         component: 'monitor/server/index',
//         meta: { title: '服务监控', icon: 'server', noCache: false }
//       },
//       {
//         name: 'Cache',
//         path: 'cache',
//         hidden: false,
//         component: 'monitor/cache/index',
//         meta: { title: '缓存监控', icon: 'redis', noCache: false }
//       }
//     ]
//   },
//   {
//     name: 'Tool',
//     path: '/tool',
//     hidden: false,
//     redirect: 'noRedirect',
//     component: 'Layout',
//     meta: { title: '系统工具', icon: 'tool', noCache: false },
//     children: [
//       {
//         name: 'Build',
//         path: 'build',
//         hidden: false,
//         component: 'tool/build/index',
//         meta: { title: '表单构建', icon: 'build', noCache: false }
//       },
//       {
//         name: 'Gen',
//         path: 'gen',
//         hidden: false,
//         component: 'tool/gen/index',
//         meta: { title: '代码生成', icon: 'code', noCache: false }
//       },
//       {
//         name: 'Swagger',
//         path: 'swagger',
//         hidden: false,
//         component: 'tool/swagger/index',
//         meta: { title: '系统接口', icon: 'swagger', noCache: false }
//       }
//     ]
//   }
// ]

// 组件加载函数
const loadView = (view) => {
  let res
  let found = false

  // 调试信息：显示要查找的组件
  console.log(`Looking for component: ${view}`)

  // 先检查modules对象是否为空
  if (Object.keys(modules).length === 0) {
    console.warn('modules对象为空，无法加载组件')
  }

  for (const path in modules) {
    // 改进路径匹配逻辑 - 更精确的路径提取
    const normalizedPath = path.replace(/^.*?views\//, '').replace(/\.vue$/, '')

    // 调试信息：显示每个路径的匹配情况


    // 更宽松的匹配逻辑，支持部分匹配
    if (normalizedPath === view || normalizedPath.endsWith(`/${view}`)) {
      console.log(`✓ Found match for ${view} at ${path}`)
      res = () => modules[path]()
      found = true
      break // 找到匹配后立即返回
    }
  }

  // 如果没找到匹配的组件，返回一个默认组件或记录警告
  if (!found) {
    console.warn(`✗ Component not found for view: ${view}`)
    console.warn(`Available modules:`, Object.keys(modules))
    // 返回一个简单的默认组件
    res = () => import('@/views/error/404')
  }

  return res
}

// 过滤异步路由
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 生成静态路由（跳过登录模式使用）
function generateStaticRoutes() {
  return new Promise(resolve => {
    console.log('开始生成静态路由...')

    const sdata = JSON.parse(JSON.stringify(staticMenuData))
    const rdata = JSON.parse(JSON.stringify(staticMenuData))
    const defaultData = JSON.parse(JSON.stringify(staticMenuData))

    // 过滤异步路由
    console.log('过滤异步路由...')
    const sidebarRoutes = filterAsyncRouter(sdata)
    const rewriteRoutes = filterAsyncRouter(rdata, false, true)
    const defaultRoutes = filterAsyncRouter(defaultData)

    console.log('过滤后的路由:', rewriteRoutes)

    // 设置权限store的状态
    const permissionStore = usePermissionStore()
    permissionStore.setRoutes(rewriteRoutes)
    permissionStore.setSidebarRouters(sidebarRoutes)
    permissionStore.setDefaultRoutes(defaultRoutes)
    permissionStore.setTopbarRoutes(defaultRoutes)

    // 添加404路由
    rewriteRoutes.push({ path: '/:pathMatch(.*)*', redirect: '/404', hidden: true })

    console.log('静态路由生成完成')
    resolve(rewriteRoutes)
  })
}

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

router.beforeEach((to, from, next) => {
  NProgress.start()

  // 跳过登录验证，直接允许访问所有页面
  if (to.meta.title) {
    useSettingsStore().setTitle(to.meta.title)
  }

  // 如果访问登录页，直接跳转到系统概览页面
  if (to.path === '/login') {
    next({ path: '/overview' })
    NProgress.done()
  } else {
    // 模拟已登录状态，设置默认token和用户信息
    if (!getToken()) {
      // 设置一个默认token
      setToken('demo-token-skip-login')
    }

    // 如果用户信息为空，设置默认信息
    if (useUserStore().roles.length === 0) {
      // 设置默认用户信息
      const defaultUserInfo = {
        roles: ['admin'],
        permissions: ['*:*:*'],
        user: {
          userId: 1,
          userName: 'demo',
          nickName: '演示用户'
        }
      }

      // 直接设置用户信息，跳过API调用
      useUserStore().roles = defaultUserInfo.roles
      useUserStore().permissions = defaultUserInfo.permissions
      useUserStore().id = defaultUserInfo.user.userId
      useUserStore().name = defaultUserInfo.user.userName
      useUserStore().nickName = defaultUserInfo.user.nickName

      // 生成静态路由（跳过API调用）
      generateStaticRoutes().then(accessRoutes => {
        // 根据静态菜单数据生成可访问的路由表
        accessRoutes.forEach(route => {
          if (!isHttp(route.path)) {
            router.addRoute(route) // 动态添加可访问路由表
          }
        })
        next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
      })
    } else {
      next()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
