import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildViewportYTicks,
  buildViewportXTicks,
  clampChartTranslateX,
  clampChartTranslateY,
  getChartDataTransform
} from '../src/utils/zhpg/preprocessChartViewport.js'

const chart = {
  left: 54,
  right: 1170,
  top: 26,
  bottom: 246,
  min: 0,
  max: 40,
  sampleCount: 12,
  records: Array.from({ length: 12 }, (_, index) => ({
    timestamp: `2026-05-${String(index + 1).padStart(2, '0')}T02:00:00`
  }))
}

test('buildViewportXTicks keeps ticks in the fixed plot area after zooming and panning', () => {
  const ticks = buildViewportXTicks(chart, { scale: 2, x: -360 })

  assert.ok(ticks.length >= 3)
  assert.ok(ticks.every(tick => tick.x >= chart.left && tick.x <= chart.right))
  assert.ok(ticks[0].index > 0)
  assert.match(ticks[0].label, /^05-/)
})

test('clampChartTranslateX constrains pan to the fixed plot window', () => {
  assert.equal(clampChartTranslateX(80, chart, 2), 0)
  assert.equal(clampChartTranslateX(-3000, chart, 2), -(chart.right - chart.left))
})

test('clampChartTranslateY constrains vertical pan to the fixed plot window', () => {
  assert.equal(clampChartTranslateY(-80, chart, 2), 0)
  assert.equal(clampChartTranslateY(3000, chart, 2), chart.bottom - chart.top)
})

test('getChartDataTransform scales both horizontal and vertical data layers', () => {
  assert.equal(getChartDataTransform({ scale: 1.5, x: -120, y: 40 }, chart), 'translate(-147 -83) scale(1.5 1.5)')
})

test('getChartDataTransform aligns data points with fixed viewport ticks', () => {
  const viewport = { scale: 2, x: -360, y: 80 }
  const tick = buildViewportXTicks(chart, viewport).find(item => item.index === 5)
  const transform = getChartDataTransform(viewport, chart)
  const [, translateX] = transform.match(/translate\((-?\d+(?:\.\d+)?) /)
  const transformedX = Number((tick.sourceX * viewport.scale + Number(translateX)).toFixed(2))

  assert.equal(transformedX, tick.x)
})

test('buildViewportYTicks relabels the fixed y-axis after vertical zooming and panning', () => {
  const ticks = buildViewportYTicks(chart, { scale: 2, y: 80 })

  assert.equal(ticks.length, 5)
  assert.deepEqual(ticks.map(tick => tick.y), [26, 81, 136, 191, 246])
  assert.notDeepEqual(ticks.map(tick => tick.label), [40, 30, 20, 10, 0])
})
