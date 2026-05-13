export const INPUT_PORT_COUNT = 7

const NODE_WIDTH = {
  data: 240,
  algorithm: 240,
  result: 150
}

const NODE_HEIGHT = {
  data: 88,
  algorithm: 88,
  result: 64
}

const LEFT = 80
const TOP = 96
const COLUMN_GAP = 360
const ROW_GAP = 156
const MIN_EDGE_LANE_GAP = 48

function byOrder(a, b) {
  return (a.order || 0) - (b.order || 0)
}

function nodeLevel(kind) {
  if (kind === 'data') return 0
  if (kind === 'result') return Number.MAX_SAFE_INTEGER
  return 1
}

function buildNodeMap(nodes) {
  return new Map((nodes || []).map(node => [node.id, node]))
}

function buildIncoming(edges) {
  const incoming = new Map()
  for (const edge of edges || []) {
    if (!edge?.sourceId || !edge?.targetId) continue
    if (!incoming.has(edge.targetId)) incoming.set(edge.targetId, [])
    incoming.get(edge.targetId).push(edge.sourceId)
  }
  return incoming
}

function resolveLevels(nodes, edges) {
  const nodeMap = buildNodeMap(nodes)
  const incoming = buildIncoming(edges)
  const levels = new Map()
  for (const node of nodes || []) levels.set(node.id, nodeLevel(node.kind))

  const algorithmNodes = (nodes || []).filter(node => node.kind === 'algorithm')
  for (let pass = 0; pass < Math.max(algorithmNodes.length, 1); pass++) {
    let changed = false
    for (const node of algorithmNodes) {
      const prevIds = incoming.get(node.id) || []
      const prevLevels = prevIds
        .map(id => nodeMap.get(id))
        .filter(Boolean)
        .map(prev => prev.kind === 'data' ? 0 : levels.get(prev.id))
        .filter(level => Number.isFinite(level) && level < Number.MAX_SAFE_INTEGER)
      const nextLevel = Math.max(1, ...prevLevels.map(level => level + 1))
      if (nextLevel > levels.get(node.id)) {
        levels.set(node.id, nextLevel)
        changed = true
      }
    }
    if (!changed) break
  }

  const maxWorkLevel = Math.max(
    0,
    ...(nodes || [])
      .filter(node => node.kind !== 'result')
      .map(node => levels.get(node.id) || 0)
  )

  for (const node of nodes || []) {
    if (node.kind === 'result') levels.set(node.id, maxWorkLevel + 1)
  }

  return levels
}

function sortLayer(layer, incoming, orderIndex) {
  return [...layer].sort((a, b) => {
    const aPrev = incoming.get(a.id) || []
    const bPrev = incoming.get(b.id) || []
    const aScore = aPrev.length
      ? aPrev.reduce((sum, id) => sum + (orderIndex.get(id) ?? a.order ?? 0), 0) / aPrev.length
      : (a.order || 0)
    const bScore = bPrev.length
      ? bPrev.reduce((sum, id) => sum + (orderIndex.get(id) ?? b.order ?? 0), 0) / bPrev.length
      : (b.order || 0)
    return aScore - bScore || byOrder(a, b)
  })
}

export function assignCalcAlgorithmLayout(nodes, edges) {
  const safeNodes = (nodes || []).filter(node => node?.id)
  const incoming = buildIncoming(edges)
  const levels = resolveLevels(safeNodes, edges)
  const layers = new Map()
  for (const node of safeNodes) {
    const level = levels.get(node.id) || 0
    if (!layers.has(level)) layers.set(level, [])
    layers.get(level).push(node)
  }

  const orderedLevels = [...layers.keys()].sort((a, b) => a - b)
  const maxRows = Math.max(1, ...orderedLevels.map(level => layers.get(level).length))
  const orderIndex = new Map()
  const result = new Map()

  for (const level of orderedLevels) {
    const sorted = sortLayer(layers.get(level).sort(byOrder), incoming, orderIndex)
    const layerTop = TOP + ((maxRows - sorted.length) * ROW_GAP) / 2
    sorted.forEach((node, index) => {
      orderIndex.set(node.id, index)
      result.set(node.id, {
        x: LEFT + level * COLUMN_GAP,
        y: layerTop + index * ROW_GAP,
        width: NODE_WIDTH[node.kind] || NODE_WIDTH.algorithm,
        height: NODE_HEIGHT[node.kind] || NODE_HEIGHT.algorithm,
        level
      })
    })
  }

  return result
}

export function distributeIncomingPorts(edges) {
  const targetGrouped = new Map()
  const sourceGrouped = new Map()
  for (const edge of edges || []) {
    if (!edge?.sourceId || !edge?.targetId) continue
    
    const targetKey = `${edge.targetId}_${edge.targetSide || 'left'}`
    if (!targetGrouped.has(targetKey)) targetGrouped.set(targetKey, [])
    targetGrouped.get(targetKey).push(edge)

    const sourceKey = `${edge.sourceId}_${edge.sourceSide || 'right'}`
    if (!sourceGrouped.has(sourceKey)) sourceGrouped.set(sourceKey, [])
    sourceGrouped.get(sourceKey).push(edge)
  }

  const nextEdges = (edges || []).map(edge => ({ ...edge }))
  const edgeIndex = new Map((edges || []).map((edge, index) => [edge, index]))

  for (const group of targetGrouped.values()) {
    const count = group.length
    group.forEach((edge, index) => {
      const portIndex = count === 1
        ? Math.floor(INPUT_PORT_COUNT / 2)
        : Math.round(((index + 1) * (INPUT_PORT_COUNT - 1)) / (count + 1))
      nextEdges[edgeIndex.get(edge)].targetPortId = `in_${portIndex}`
    })
  }

  for (const group of sourceGrouped.values()) {
    const count = group.length
    group.forEach((edge, index) => {
      const portIndex = count === 1
        ? Math.floor(INPUT_PORT_COUNT / 2)
        : Math.round(((index + 1) * (INPUT_PORT_COUNT - 1)) / (count + 1))
      nextEdges[edgeIndex.get(edge)].sourcePortId = `out_${portIndex}`
    })
  }

  return nextEdges
}

export function getInputPortRatio(portId) {
  const rawIndex = Number(String(portId || '').replace(/^(in|out)_/, ''))
  const index = Number.isFinite(rawIndex)
    ? Math.max(0, Math.min(INPUT_PORT_COUNT - 1, rawIndex))
    : Math.floor(INPUT_PORT_COUNT / 2)
  return (index + 0.5) / INPUT_PORT_COUNT
}

export function buildOrthogonalEdgeVertices(sourceBox, targetBox, edgeIndex = 0) {
  const sides = targetBox.sides || chooseConnectionSides(sourceBox, targetBox)
  const source = anchorPoint(sourceBox, sides.sourceSide)
  const target = anchorPoint(targetBox, sides.targetSide, targetBox.portId)
  const available = Math.max(MIN_EDGE_LANE_GAP, Math.abs(target.x - source.x) + Math.abs(target.y - source.y))
  const laneStep = Math.min(20, Math.max(10, available / 12))
  const laneOffset = (edgeIndex % 5 - 2) * laneStep

  if (isHorizontalSide(sides.sourceSide) && isHorizontalSide(sides.targetSide)) {
    const midX = Math.round(source.x + (target.x - source.x) / 2 + laneOffset)
    return compactPoints([
      { x: midX, y: source.y },
      { x: midX, y: target.y }
    ])
  }

  if (isVerticalSide(sides.sourceSide) && isVerticalSide(sides.targetSide)) {
    const midY = Math.round(source.y + (target.y - source.y) / 2 + laneOffset)
    return compactPoints([
      { x: source.x, y: midY },
      { x: target.x, y: midY }
    ])
  }

  const sourceLead = translateOutward(source, sides.sourceSide, 40 + Math.abs(laneOffset))
  const targetLead = translateOutward(target, sides.targetSide, 40 + Math.abs(laneOffset))
  return compactPoints([
    sourceLead,
    { x: targetLead.x, y: sourceLead.y },
    targetLead
  ])
}

export function getTargetAnchorDy(targetBox, portId) {
  return Math.round(targetBox.height * (getInputPortRatio(portId) - 0.5))
}

export function chooseConnectionSides(sourceBox, targetBox) {
  const sourceCenter = centerPoint(sourceBox)
  const targetCenter = centerPoint(targetBox)
  const dx = targetCenter.x - sourceCenter.x
  const dy = targetCenter.y - sourceCenter.y
  if (Math.abs(dx) >= Math.abs(dy)) {
    return dx >= 0
      ? { sourceSide: 'right', targetSide: 'left' }
      : { sourceSide: 'left', targetSide: 'right' }
  }
  return dy >= 0
    ? { sourceSide: 'bottom', targetSide: 'top' }
    : { sourceSide: 'top', targetSide: 'bottom' }
}

export function anchorArgsForSide(box, side, portId) {
  const offset = Math.round((isHorizontalSide(side) ? box.height : box.width) * (getInputPortRatio(portId) - 0.5))
  return isHorizontalSide(side) ? { dy: offset } : { dx: offset }
}

function centerPoint(box) {
  return { x: box.x + box.width / 2, y: box.y + box.height / 2 }
}

function anchorPoint(box, side, portId) {
  const args = portId ? anchorArgsForSide(box, side, portId) : {}
  if (side === 'left') return { x: box.x, y: box.y + box.height / 2 + (args.dy || 0) }
  if (side === 'right') return { x: box.x + box.width, y: box.y + box.height / 2 + (args.dy || 0) }
  if (side === 'top') return { x: box.x + box.width / 2 + (args.dx || 0), y: box.y }
  return { x: box.x + box.width / 2 + (args.dx || 0), y: box.y + box.height }
}

function translateOutward(point, side, distance) {
  if (side === 'left') return { x: point.x - distance, y: point.y }
  if (side === 'right') return { x: point.x + distance, y: point.y }
  if (side === 'top') return { x: point.x, y: point.y - distance }
  return { x: point.x, y: point.y + distance }
}

function isHorizontalSide(side) {
  return side === 'left' || side === 'right'
}

function isVerticalSide(side) {
  return side === 'top' || side === 'bottom'
}

function compactPoints(points) {
  return points.filter((point, index) => {
    if (index === 0) return true
    const prev = points[index - 1]
    return point.x !== prev.x || point.y !== prev.y
  })
}
