const DEFAULT_MAX_TICKS = 5

export function clampChartTranslateX(x, chart, scale) {
  const plotWidth = Math.max(0, Number(chart?.right || 0) - Number(chart?.left || 0))
  const safeScale = Math.max(1, Number(scale) || 1)
  const minX = Math.round(plotWidth * (1 - safeScale))
  return Math.min(0, Math.max(minX, Number(x) || 0))
}

export function clampChartTranslateY(y, chart, scale) {
  const plotHeight = Math.max(0, Number(chart?.bottom || 0) - Number(chart?.top || 0))
  const safeScale = Math.max(1, Number(scale) || 1)
  const maxY = Math.round(plotHeight * (safeScale - 1))
  return Math.max(0, Math.min(maxY, Number(y) || 0))
}

export function getChartDataTransform(viewport = {}, chart = {}) {
  const left = Number(chart?.left ?? 54)
  const bottom = Number(chart?.bottom ?? 246)
  const scale = Number((Number(viewport.scale) || 1).toFixed(3))
  const x = Number(((Number(viewport.x) || 0) + left * (1 - scale)).toFixed(2))
  const y = Number(((Number(viewport.y) || 0) + bottom * (1 - scale)).toFixed(2))
  return `translate(${x} ${y}) scale(${scale} ${scale})`
}

export function buildViewportXTicks(chart, viewport = {}, maxTicks = DEFAULT_MAX_TICKS) {
  const sampleCount = Number(chart?.sampleCount) || 0
  if (!sampleCount) return []

  const left = Number(chart.left)
  const right = Number(chart.right)
  const scale = Math.max(1, Number(viewport.scale) || 1)
  const translateX = clampChartTranslateX(viewport.x, chart, scale)
  const plotWidth = Math.max(1, right - left)
  const visibleStartX = left + Math.max(0, -translateX) / scale
  const visibleEndX = left + Math.min(plotWidth, plotWidth - translateX) / scale
  const startIndex = Math.max(0, Math.floor(chartXToIndex(visibleStartX, sampleCount, left, right)))
  const endIndex = Math.min(sampleCount - 1, Math.ceil(chartXToIndex(visibleEndX, sampleCount, left, right)))
  const visibleCount = Math.max(1, endIndex - startIndex + 1)
  const step = Math.max(1, Math.ceil(visibleCount / Math.max(2, maxTicks - 1)))
  const indexes = []

  for (let index = startIndex; index <= endIndex; index += step) {
    indexes.push(index)
  }
  if (indexes[indexes.length - 1] !== endIndex) indexes.push(endIndex)

  return indexes.map(index => {
    const sourceX = indexToChartX(index, sampleCount, left, right)
    return {
      index,
      label: formatChartTickLabel(chart.records?.[index], index, chart.records || []),
      sourceX,
      x: Number((left + (sourceX - left) * scale + translateX).toFixed(2))
    }
  }).filter(tick => tick.x >= left - 0.5 && tick.x <= right + 0.5)
}

export function buildViewportYTicks(chart, viewport = {}, tickCount = 5) {
  const top = Number(chart?.top)
  const bottom = Number(chart?.bottom)
  const min = Number(chart?.min)
  const max = Number(chart?.max)
  if (![top, bottom, min, max].every(Number.isFinite)) return chart?.yTicks || []

  const scale = Math.max(1, Number(viewport.scale) || 1)
  const translateY = clampChartTranslateY(viewport.y, chart, scale)
  const adjustedY = translateY + bottom * (1 - scale)
  const count = Math.max(2, tickCount)

  return Array.from({ length: count }, (_, index) => {
    const ratio = index / (count - 1)
    const y = Number((top + (bottom - top) * ratio).toFixed(2))
    const sourceY = (y - adjustedY) / scale
    const value = chartYToValue(sourceY, min, max, top, bottom)
    return {
      value,
      label: formatTickNumber(value),
      y
    }
  })
}

export function indexToChartX(index, count, left, right) {
  const step = count > 1 ? (right - left) / (count - 1) : right - left
  return Number((left + index * step).toFixed(2))
}

function chartXToIndex(x, count, left, right) {
  if (count <= 1) return 0
  return ((x - left) / Math.max(1, right - left)) * (count - 1)
}

function chartYToValue(y, min, max, top, bottom) {
  const range = max - min || 1
  return max - ((y - top) / Math.max(1, bottom - top)) * range
}

function formatTickNumber(value) {
  const abs = Math.abs(value)
  if (abs >= 100) return Number(value.toFixed(0))
  if (abs >= 10) return Number(value.toFixed(1))
  return Number(value.toFixed(2))
}

function formatChartTickLabel(record, index, records = []) {
  const timestamp = record?.timestamp || record?.time || ''
  if (!timestamp) return `S${index + 1}`

  if (timestamp.includes('T')) {
    const [date = '', time = ''] = timestamp.split('T')
    const dateStr = date.substring(5)
    const timeStr = time.substring(0, 8)
    const dates = new Set(records.map(item => (item?.timestamp || item?.time || '').split('T')[0]).filter(Boolean))
    return dates.size > 1 ? `${dateStr} ${timeStr}` : timeStr
  }

  return timestamp.length > 10 ? timestamp.substring(11, 19) : timestamp
}
