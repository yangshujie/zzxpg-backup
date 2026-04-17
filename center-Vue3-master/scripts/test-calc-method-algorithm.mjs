import assert from 'node:assert/strict'
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const projectRoot = resolve(__dirname, '..')
const sourcePath = resolve(projectRoot, 'src/utils/zhpg/calcMethodAlgorithm.js')
const tmpDir = resolve(projectRoot, '.tmp-tests')
const tmpPath = resolve(tmpDir, 'calcMethodAlgorithm.mjs')

await mkdir(tmpDir, { recursive: true })

const source = await readFile(sourcePath, 'utf8')
await writeFile(
  tmpPath,
  source.replace(
    "import { ZHPG_WORK_MODE, normalizeZhpgWorkMode } from '@/constants/zhpgWorkMode'",
    "import { ZHPG_WORK_MODE, normalizeZhpgWorkMode } from '../src/constants/zhpgWorkMode.js'"
  ),
  'utf8'
)

try {
  const {
    parseLeafCalcToWorkspace,
    serializeWorkspaceToComputeRule,
    validateCalcWorkspace
  } = await import(`${pathToFileURL(tmpPath).href}?t=${Date.now()}`)

  const computeRule = {
    data: 'leaf-a',
    method: {
      node: [
        {
          type: 'start',
          id: 'ds-a',
          value: 'dir-a',
          name: '数据源A',
          fields: ['field_a'],
          x: 80,
          y: 120
        },
        {
          type: 'algo',
          id: 'algo-a',
          value: 101,
          config: [{ field: 'k', defaultValue: 'v' }],
          x: 360,
          y: 130
        },
        {
          type: 'algo',
          id: 'algo-b',
          value: 102,
          config: [{ field: 'threshold', defaultValue: '5' }],
          x: 360,
          y: 300
        },
        { type: 'result', id: 'result-a', value: '', x: 680, y: 200 }
      ],
      lineList: [
        { sourceId: 'ds-a', targetId: 'algo-b' },
        { sourceId: 'algo-b', targetId: 'result-a' },
        { sourceId: 'ds-a', targetId: 'algo-a' }
      ]
    }
  }

  const workspace = parseLeafCalcToWorkspace(computeRule)
  assert.deepEqual(workspace.flowEdges, computeRule.method.lineList)
  assert.equal(workspace.dataSources[0].x, 80)
  assert.equal(workspace.dataSources[0].y, 120)
  assert.equal(workspace.algorithmSteps.find(s => s.flowNodeId === 'algo-b')?.y, 300)
  assert.equal(workspace.resultPosition.x, 680)

  const validationError = validateCalcWorkspace({
    workMode: workspace.workMode,
    dataSources: workspace.dataSources,
    algorithmSteps: workspace.algorithmSteps,
    resultFlowNodeId: workspace.resultFlowNodeId,
    flowEdges: workspace.flowEdges
  })
  assert.equal(validationError, null)

  const serialized = serializeWorkspaceToComputeRule(
    {
      ...workspace,
      flowEdges: [
        { sourceId: 'ds-a', targetId: 'algo-b' },
        { sourceId: 'algo-b', targetId: 'result-a' },
        { sourceId: 'ds-a', targetId: 'algo-a' }
      ]
    },
    'leaf-a'
  )
  assert.deepEqual(serialized.method.lineList, [
    { sourceId: 'ds-a', targetId: 'algo-b' },
    { sourceId: 'algo-b', targetId: 'result-a' },
    { sourceId: 'ds-a', targetId: 'algo-a' }
  ])
  assert.equal(serialized.method.node.find(n => n.id === 'algo-b')?.y, 300)
} finally {
  await rm(tmpDir, { recursive: true, force: true })
}
