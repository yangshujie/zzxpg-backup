import assert from 'node:assert/strict'
import {
  anchorArgsForSide,
  assignCalcAlgorithmLayout,
  buildOrthogonalEdgeVertices,
  chooseConnectionSides,
  distributeIncomingPorts,
  getTargetAnchorDy,
  INPUT_PORT_COUNT
} from '../src/utils/zhpg/calcAlgorithmLayout.js'

const nodes = [
  { id: 'data-1', kind: 'data', order: 0 },
  { id: 'data-2', kind: 'data', order: 1 },
  { id: 'algo-1', kind: 'algorithm', order: 2 },
  { id: 'algo-2', kind: 'algorithm', order: 3 },
  { id: 'result-1', kind: 'result', order: 4 }
]

const edges = [
  { sourceId: 'data-1', targetId: 'algo-1' },
  { sourceId: 'data-2', targetId: 'algo-1' },
  { sourceId: 'algo-1', targetId: 'algo-2' },
  { sourceId: 'algo-2', targetId: 'result-1' }
]

const layout = assignCalcAlgorithmLayout(nodes, edges)

assert.equal(layout.get('data-1').x, layout.get('data-2').x)
assert.ok(layout.get('algo-1').x > layout.get('data-1').x)
assert.ok(layout.get('algo-2').x > layout.get('algo-1').x)
assert.ok(layout.get('result-1').x > layout.get('algo-2').x)
assert.notEqual(layout.get('data-1').y, layout.get('data-2').y)

const distributed = distributeIncomingPorts(edges)
assert.equal(distributed[0].targetPortId, 'in_2')
assert.equal(distributed[1].targetPortId, 'in_4')
assert.equal(distributed[2].targetPortId, 'in_3')
assert.equal(distributed[3].targetPortId, 'in_3')
assert.equal(INPUT_PORT_COUNT, 7)

const vertices = buildOrthogonalEdgeVertices(
  { x: 80, y: 100, width: 220, height: 80 },
  { x: 440, y: 220, width: 220, height: 80, portId: 'in_4' },
  0
)

const fullPath = [
  { x: 300, y: 140 },
  ...vertices,
  { x: 440, y: 220 + 40 + getTargetAnchorDy({ height: 80 }, 'in_4') }
]

for (let i = 1; i < fullPath.length; i++) {
  const prev = fullPath[i - 1]
  const curr = fullPath[i]
  assert.ok(prev.x === curr.x || prev.y === curr.y, `segment ${i} is not orthogonal`)
}

assert.ok(vertices[0].x > 300)
assert.ok(vertices[0].x < 440)
assert.equal(vertices[0].x, vertices[1].x)
assert.equal(getTargetAnchorDy({ height: 80 }, 'in_3'), 0)
assert.ok(getTargetAnchorDy({ height: 80 }, 'in_5') > 0)
assert.ok(getTargetAnchorDy({ height: 80 }, 'in_1') < 0)

assert.deepEqual(
  chooseConnectionSides(
    { x: 100, y: 100, width: 120, height: 80 },
    { x: 420, y: 110, width: 120, height: 80 }
  ),
  { sourceSide: 'right', targetSide: 'left' }
)
assert.deepEqual(
  chooseConnectionSides(
    { x: 100, y: 100, width: 120, height: 80 },
    { x: 120, y: 320, width: 120, height: 80 }
  ),
  { sourceSide: 'bottom', targetSide: 'top' }
)

const verticalVertices = buildOrthogonalEdgeVertices(
  { x: 100, y: 100, width: 120, height: 80 },
  {
    x: 120,
    y: 320,
    width: 120,
    height: 80,
    portId: 'in_3',
    sides: { sourceSide: 'bottom', targetSide: 'top' }
  },
  1
)
const verticalPath = [
  { x: 160, y: 180 },
  ...verticalVertices,
  { x: 180, y: 320 }
]
for (let i = 1; i < verticalPath.length; i++) {
  const prev = verticalPath[i - 1]
  const curr = verticalPath[i]
  assert.ok(prev.x === curr.x || prev.y === curr.y, `vertical segment ${i} is not orthogonal`)
}
assert.deepEqual(anchorArgsForSide({ width: 120, height: 80 }, 'top', 'in_3'), { dx: 0 })
assert.ok(anchorArgsForSide({ width: 120, height: 80 }, 'right', 'in_5').dy > 0)
