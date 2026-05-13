import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createApp } from 'vue'
import Cookies from 'js-cookie'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 移除默认的 dark css vars，我们将完全通过自定义 CSS 变量控制主题
// import 'element-plus/theme-chalk/dark/css-vars.css'
import locale from 'element-plus/es/locale/lang/zh-cn'

import '@fortawesome/fontawesome-free/css/all.min.css' // Font Awesome 图标库
import '@vue-flow/core/dist/style.css' // Vue Flow 核心样式
import '@vue-flow/core/dist/theme-default.css' // Vue Flow 默认主题
import '@/assets/styles/index.scss' // global css
import './styles/index.scss'
import microApp from '@micro-zoe/micro-app'

// 引入主题 Store 并立即初始化，避免闪烁
import { useThemeStore } from './store/theme'

microApp.start()

import App from './App'
import store from './store'
import router from './router'
import directive from './directive' // directive
import { getToken, setToken, removeToken } from '@/utils/auth'
// 注册指令
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

// svg 图标
import 'virtual:svg-icons-register'
import SvgIcon from '@/components/SvgIcon'
import elementIcons from '@/components/SvgIcon/svgicon'

import './permission' // permission control

import { useDict } from '@/utils/dict'
import { getConfigKey } from "@/api/system/config"
import { parseTime, resetForm, addDateRange, handleTree, selectDictLabel, selectDictLabels } from '@/utils/ruoyi'

// 分页组件
import Pagination from '@/components/Pagination'
// 自定义表格工具组件
import RightToolbar from '@/components/RightToolbar'
// 富文本组件
import Editor from "@/components/Editor"
// 文件上传组件
import FileUpload from "@/components/FileUpload"
// 图片上传组件
import ImageUpload from "@/components/ImageUpload"
// 图片预览组件
import ImagePreview from "@/components/ImagePreview"
// 字典标签组件
import DictTag from '@/components/DictTag'
import LineChart from '@/components/LineChart'

const app = createApp(App)
// Polyfill for Object.hasOwn (for Node < 16.9.0)
if (!Object.hasOwn) {
    Object.hasOwn = function(obj, prop) {
      return Object.prototype.hasOwnProperty.call(obj, prop);
    };
  }

// 全局方法挂载
app.config.globalProperties.useDict = useDict
app.config.globalProperties.download = download
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.handleTree = handleTree
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.getConfigKey = getConfigKey
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels

// 全局组件挂载
app.component('DictTag', DictTag)
app.component('Pagination', Pagination)
app.component('FileUpload', FileUpload)
app.component('ImageUpload', ImageUpload)
app.component('ImagePreview', ImagePreview)
app.component('RightToolbar', RightToolbar)
app.component('Editor', Editor)
app.component('LineChart', LineChart)

const pinia = createPinia();
app.use(pinia)
app.use(router)
app.use(store)
app.use(plugins)
app.use(elementIcons)
app.component('svg-icon', SvgIcon)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
directive(app)

// 使用 element-plus 并且设置全局的大小
app.use(ElementPlus, {
    locale: locale,
    // 支持 large、default、small
    size: Cookies.get('size') || 'default'
})

// 【重要】在 Pinia 和 Router 注册后，初始化主题 Store
// 这会读取 localStorage 并应用对应的 CSS 变量
const themeStore = useThemeStore();
// 如果 localStorage 有值，store.initTheme() 会应用它；否则应用默认值
// 由于我们在上面已经读取了 savedTheme，这里直接调用 initTheme 即可，它会处理逻辑
themeStore.initTheme();
console.log('init theme ', themeStore.currentThemeName)

// 从主应用获取传递的数据（token、userInfo、baseUrl、path 等）
if (window.__MICRO_APP_ENVIRONMENT__) {
    const data = window.microApp?.getData()
    console.log('[requirementPortal] 主应用传递的数据:', data)

    if (data) {
        // 可根据需要使用 data.token, data.userInfo, data.baseUrl, data.path 等
        console.log('[requirementPortal] token:', data.token)
        console.log('[requirementPortal] userInfo:', data.userInfo)
        console.log('[requirementPortal] baseUrl:', data.baseUrl)
        console.log('[requirementPortal] path:', data.path)
        setToken(data.token)
    }

    // 监听主应用后续数据变化（如 token 刷新、路由同步等）
    window.microApp?.addDataListener((newData) => {
        console.log('[requirementPortal] 主应用数据更新:', newData)
    })
}

app.mount('#app')
window.unmount = () => {
    app.unmount()
}
