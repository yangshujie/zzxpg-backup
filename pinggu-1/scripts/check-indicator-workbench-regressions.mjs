import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const workbench = readFileSync(
  resolve(root, 'src/views/zhpg/components/IndicatorTreeWorkbench.vue'),
  'utf8'
)
const calcFields = readFileSync(
  resolve(root, 'src/views/zhpg/components/ZhpgCalcMethodFields.vue'),
  'utf8'
)
const calcAlgorithmDialog = readFileSync(
  resolve(root, 'src/views/zhpg/components/ZhpgCalcAlgorithmDialog.vue'),
  'utf8'
)
const externalDataDialog = readFileSync(
  resolve(root, 'src/views/zhpg/components/ZhpgExternalDataDialog.vue'),
  'utf8'
)
const calcMethodAlgorithm = readFileSync(
  resolve(root, 'src/utils/zhpg/calcMethodAlgorithm.js'),
  'utf8'
)
const indicatorTreeJson = readFileSync(
  resolve(root, 'src/utils/zhpgIndicatorTreeJson.js'),
  'utf8'
)
const indicatorSystemConstants = readFileSync(
  resolve(root, 'src/constants/zhpgIndicatorSystem.js'),
  'utf8'
)

const checks = [
  {
    name: 'G6 compatibility wrapper must not override Graph.on and break built-in zoom behavior',
    pass: !/\n\s*on\(eventName,\s*handler\)\s*\{/.test(workbench)
  },
  {
    name: 'theme detection follows html.dark instead of forcing dark mode',
    pass:
      /function\s+isDarkMode\(\)\s*\{[\s\S]*?document\.documentElement\.classList\.contains\('dark'\)/.test(workbench) &&
      !/function\s+isDarkMode\(\)\s*\{\s*return\s+true\s*\}/.test(workbench)
  },
  {
    name: 'workbench has an explicit light theme token branch',
    pass: /:global\(html:not\(\.dark\).*?\)\s+\.indicator-tree-workbench/.test(workbench)
  },
  {
    name: 'calculation preview does not use fixed-width grid tracks that overflow narrow panes',
    pass: !/grid-template-columns:\s*minmax\(220px/.test(calcFields)
  },
  {
    name: 'leaf calculation editor can be hidden during main-branch initial draft workbench editing',
    pass: /selectedNodeIsLeaf\s*&&\s*!hideLeafCalcMethod/.test(workbench)
  },
  {
    name: 'equipment type constants use backend enum codes while preserving legacy aliases',
    pass:
      /value:\s*'space_recon'/.test(indicatorSystemConstants) &&
      /space_domain_awareness:\s*'太空态势感知'/.test(indicatorSystemConstants) &&
      /航天侦察:\s*'space_recon'/.test(indicatorTreeJson)
  },
  {
    name: 'calculation workspace serialization preserves canvas positions and flow edges',
    pass:
      /resultPosition/.test(calcMethodAlgorithm) &&
      /flowEdges/.test(calcMethodAlgorithm) &&
      /normalizeFlowEdges/.test(calcMethodAlgorithm) &&
      /normalizePosition/.test(calcMethodAlgorithm)
  },
  {
    name: 'algorithm configuration dialog uses X6 canvas interaction instead of simplified linear editor',
    pass:
      /import\s+\{\s*Graph\s*\}\s+from\s+'@antv\/x6'/.test(calcAlgorithmDialog) &&
      /ref="canvasRef"/.test(calcAlgorithmDialog) &&
      /onCanvasDrop/.test(calcAlgorithmDialog) &&
      /collectWorkspaceFromGraph/.test(calcAlgorithmDialog)
  },
  {
    name: 'algorithm configuration dialog supports external data selection and persisted custom edges',
    pass:
      /ZhpgExternalDataDialog/.test(calcAlgorithmDialog) &&
      /handleExternalDataSelected/.test(calcAlgorithmDialog) &&
      /workspace\.flowEdges/.test(calcAlgorithmDialog) &&
      /resultPosition/.test(calcAlgorithmDialog)
  },
  {
    name: 'algorithm configuration dialog is adapted to pinggu dark theme',
    pass:
      /function\s+graphTheme\(\)/.test(calcAlgorithmDialog) &&
      /canvasBg:\s*'#07111f'/.test(calcAlgorithmDialog) &&
      /background:\s*#050d18/.test(calcAlgorithmDialog) &&
      /rgba\(0,\s*242,\s*255,\s*0\.14\)/.test(calcAlgorithmDialog) &&
      /fill:\s*graphTheme\(\)\.label/.test(calcAlgorithmDialog)
  },
  {
    name: 'external data selector dialog is adapted to pinggu dark theme',
    pass:
      /class="zhpg-external-data-dialog"/.test(externalDataDialog) &&
      /\.selected-summary[\s\S]*background:\s*rgba\(5,\s*13,\s*24,\s*0\.92\)/.test(externalDataDialog) &&
      /\.summary-header[\s\S]*background:\s*rgba\(0,\s*242,\s*255,\s*0\.06\)/.test(externalDataDialog) &&
      /\.zhpg-external-data-dialog\s+\.el-table[\s\S]*--el-table-header-bg-color:\s*rgba\(0,\s*242,\s*255,\s*0\.08\)/.test(externalDataDialog)
  }
]

const failed = checks.filter(check => !check.pass)
if (failed.length) {
  console.error('Indicator workbench regression checks failed:')
  for (const check of failed) console.error(`- ${check.name}`)
  process.exit(1)
}

console.log('Indicator workbench regression checks passed.')
