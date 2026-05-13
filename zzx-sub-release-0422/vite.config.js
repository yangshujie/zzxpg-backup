import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

const kexiUrl = 'http://127.0.0.1:9401/' // 后端接口
// const kexiUrl = 'http://10.150.166.150:9401' // 后端接口
const huageUrl = "http://172.16.2.89:9501"
// const huageUrl = "http://10.150.166.222:9501"
const xiaoyangUrl = "http://127.0.0.1:9303" // 

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        external: ['web-worker'],
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置
    server: {
      port: 2025,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        '/kexi-api': {
          target: kexiUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/kexi-api/, '')
        },
        '/dev-api': {
          target: 'http://192.168.0.157:9201',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-api/, '')
        },
        '/task-api': {
          target: 'http://192.168.0.69:9301', //任务和作战剖面
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/task-api/, '')
        },
        '/equ-api': {
          target: 'http://192.168.0.23:9302', //装备管理
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/equ-api/, '')
        },
        '/huage-api': {
          target: huageUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/huage-api/, '')
        },
        '/xiaoyang-api': {
          target: xiaoyangUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/xiaoyang-api/, '')
        },
        '/zhpg-api': {
          target: xiaoyangUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/zhpg-api/, '')
        },
        // springdoc proxy
        '^/v3/api-docs/(.*)': {
          target: "",
          changeOrigin: true,
        }
      }
    },
    // Cesium配置
    optimizeDeps: {
      include: ['cesium', 'elkjs']
    },
    define: {
      // 定义全局变量，避免Cesium的web worker错误
      'process.env': {},
      CESIUM_BASE_URL: JSON.stringify('/node_modules/cesium/Build/Cesium/')
    },
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})
