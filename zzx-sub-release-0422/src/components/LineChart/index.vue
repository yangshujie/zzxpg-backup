<template>
  <div class="line-chart" :style="wrapperStyle">
    <el-empty
      v-if="showEmpty"
      :description="emptyText"
      class="line-chart__empty"
    />
    <v-chart
      v-else
      ref="chartRef"
      class="line-chart__canvas"
      :option="chartOption"
      :loading="loading"
      autoresize
    />
  </div>
</template>

<script setup name="LineChart">
import { computed, ref } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart as EchartsLineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([
  CanvasRenderer,
  EchartsLineChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
])

const DEFAULT_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']

const props = defineProps({
  xAxisData: {
    type: Array,
    default: () => []
  },
  series: {
    type: Array,
    default: () => []
  },
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  height: {
    type: String,
    default: '320px'
  },
  width: {
    type: String,
    default: '100%'
  },
  loading: {
    type: Boolean,
    default: false
  },
  colors: {
    type: Array,
    default: () => ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']
  },
  smooth: {
    type: Boolean,
    default: true
  },
  showSymbol: {
    type: Boolean,
    default: false
  },
  symbol: {
    type: String,
    default: 'circle'
  },
  symbolSize: {
    type: Number,
    default: 6
  },
  area: {
    type: Boolean,
    default: false
  },
  legend: {
    type: Boolean,
    default: true
  },
  boundaryGap: {
    type: Boolean,
    default: false
  },
  xAxisName: {
    type: String,
    default: ''
  },
  yAxisName: {
    type: String,
    default: ''
  },
  yAxisMin: {
    type: [Number, String],
    default: null
  },
  yAxisMax: {
    type: [Number, String],
    default: null
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  tooltipFormatter: {
    type: Function,
    default: null
  },
  xAxisLabelFormatter: {
    type: Function,
    default: null
  },
  yAxisLabelFormatter: {
    type: Function,
    default: null
  },
  grid: {
    type: Object,
    default: () => ({
      left: 24,
      right: 24,
      top: 56,
      bottom: 24,
      containLabel: true
    })
  },
  customOption: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)

const wrapperStyle = computed(() => ({
  width: props.width,
  height: props.height
}))

const normalizedSeries = computed(() =>
  props.series.map((item, index) => {
    const color = item.color || props.colors[index] || DEFAULT_COLORS[index % DEFAULT_COLORS.length]
    const useArea = item.area ?? props.area
    const useSymbol = item.showSymbol ?? props.showSymbol

    return {
      ...item,
      type: 'line',
      smooth: item.smooth ?? props.smooth,
      showSymbol: useSymbol,
      symbol: item.symbol || props.symbol,
      symbolSize: item.symbolSize ?? props.symbolSize,
      lineStyle: {
        width: 2,
        color,
        ...(item.lineStyle || {})
      },
      itemStyle: {
        color,
        ...(item.itemStyle || {})
      },
      areaStyle: useArea
        ? {
            opacity: item.areaOpacity ?? 0.15,
            ...(item.areaStyle || {})
          }
        : item.areaStyle,
      emphasis: item.emphasis || { focus: 'series' }
    }
  })
)

const showEmpty = computed(() => {
  if (props.loading) {
    return false
  }

  if (!props.xAxisData.length || !normalizedSeries.value.length) {
    return true
  }

  return !normalizedSeries.value.some((item) => Array.isArray(item.data) && item.data.length)
})

const baseOption = computed(() => ({
  color: props.colors,
  title: {
    show: Boolean(props.title || props.subtitle),
    text: props.title,
    subtext: props.subtitle,
    left: 'center',
    textStyle: {
      fontSize: 16,
      fontWeight: 600,
      color: '#303133'
    },
    subtextStyle: {
      color: '#909399'
    }
  },
  tooltip: {
    trigger: 'axis',
    formatter: props.tooltipFormatter || undefined,
    axisPointer: {
      type: 'line'
    }
  },
  legend: {
    show: props.legend,
    top: 8,
    icon: 'circle'
  },
  grid: props.grid,
  xAxis: {
    type: 'category',
    boundaryGap: props.boundaryGap,
    name: props.xAxisName,
    data: props.xAxisData,
    axisTick: {
      show: false
    },
    axisLine: {
      lineStyle: {
        color: '#dcdfe6'
      }
    },
    axisLabel: {
      color: '#606266',
      formatter: props.xAxisLabelFormatter || undefined
    }
  },
  yAxis: {
    type: 'value',
    name: props.yAxisName,
    min: props.yAxisMin ?? undefined,
    max: props.yAxisMax ?? undefined,
    axisLine: {
      show: false
    },
    splitLine: {
      lineStyle: {
        type: 'dashed',
        color: '#ebeef5'
      }
    },
    axisLabel: {
      color: '#606266',
      formatter: props.yAxisLabelFormatter || undefined
    }
  },
  series: normalizedSeries.value
}))

const chartOption = computed(() => mergeChartOption(baseOption.value, props.customOption))

defineExpose({
  resize: () => chartRef.value?.resize?.(),
  getChart: () => chartRef.value?.chart
})

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function mergeChartOption(base, extra) {
  if (!isPlainObject(extra)) {
    return base
  }

  const output = { ...base }

  Object.keys(extra).forEach((key) => {
    const baseValue = output[key]
    const extraValue = extra[key]

    if (Array.isArray(extraValue)) {
      output[key] = extraValue
      return
    }

    if (isPlainObject(baseValue) && isPlainObject(extraValue)) {
      output[key] = mergeChartOption(baseValue, extraValue)
      return
    }

    output[key] = extraValue
  })

  return output
}
</script>

<style scoped lang="scss">
.line-chart {
  min-width: 0;
}

.line-chart__canvas,
.line-chart__empty {
  width: 100%;
  height: 100%;
}
</style>
