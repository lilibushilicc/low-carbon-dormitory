<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { weekXAxisService } from '../api/studentMealWaste.js'

const selectedYear = ref(2026)
const chartRef = ref(null)
const xAxisLabels = ref([])
const carbonSeriesData = ref([])
let chartInstance = null

// 示例数据：可替换为后端接口返回的数据
const weeklyCarbonByYear = {
  2024: [18, 17, 16, 15, 16, 14, 15, 16, 15, 14, 13, 13, 14, 15, 16, 16, 17, 18, 17, 16, 15, 14, 14, 13, 12, 12, 11, 11, 12, 13, 13, 14, 15, 15, 16, 17, 18, 17, 16, 15, 14, 13, 13, 12, 12, 11, 10, 10, 11, 12, 12, 13],
  2025: [15, 14, 13, 13, 12, 12, 11, 10, 10, 11, 12, 12, 13, 14, 14, 15, 16, 15, 14, 13, 12, 11, 10, 10, 9, 9, 10, 10, 11, 12, 13, 13, 14, 15, 15, 14, 13, 12, 11, 10, 10, 9, 9, 8, 8, 9, 10, 10, 11, 12, 12, 13],
  2026: [13, 12, 12, 11, 10, 10, 9, 9, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 11, 11, 10, 9, 9, 8, 8, 7, 7, 8, 8, 9, 10, 10, 11, 11, 12, 12, 11, 10, 10, 9, 9, 8, 8, 7, 7, 8, 8, 9, 9, 10, 10, 11]
}

const generateYearData = (year) => {
  const seed = year % 17
  const data = []
  for (let week = 1; week <= 52; week += 1) {
    const seasonal = Math.sin((week / 52) * Math.PI * 2) * 1.8
    const trend = ((year - 2024) % 5) * -0.25
    const noise = ((week * (seed + 3)) % 7) * 0.22 - 0.66
    const value = Number((12 + seasonal + trend + noise).toFixed(2))
    data.push(Math.max(6, value))
  }
  return data
}

const currentYearData = computed(() => {
  const year = selectedYear.value
  if (!weeklyCarbonByYear[year]) {
    weeklyCarbonByYear[year] = generateYearData(year)
  }
  return weeklyCarbonByYear[year]
})
const fallbackWeekLabels = computed(() => currentYearData.value.map((_, index) => `第${index + 1}周`))

const fetchXAxisLabels = async (year) => {
  try {
    const res = await weekXAxisService({ yearCode: year })
    const payload = res?.data
    const list = Array.isArray(payload) ? payload : (payload?.list || [])
    if (Array.isArray(list) && list.length > 0) {
      xAxisLabels.value = list.map((item, index) => {
        if (typeof item === 'string' || typeof item === 'number') return String(item)
        return String(item.weekLabel || item.weekName || item.weekCode || item.week || `第${index + 1}周`)
      })
      const parsedSeries = list.map((item, index) => {
        if (typeof item === 'number') return item
        const value = Number(
          item.totalCarbonEmission ??
          item.carbonEmission ??
          item.weekCarbonEmission ??
          item.value ??
          currentYearData.value[index] ??
          0
        )
        return Number.isNaN(value) ? 0 : value
      })
      // 保证点位数量与 X 轴完全一致，避免“数据对不上”
      carbonSeriesData.value = parsedSeries.slice(0, xAxisLabels.value.length)
      return
    }
    xAxisLabels.value = []
    carbonSeriesData.value = []
  } catch (error) {
    xAxisLabels.value = []
    carbonSeriesData.value = []
  }
}

const renderChart = () => {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  chartInstance.setOption({
    title: {
      text: `${selectedYear.value}年每周碳排放变化`,
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisLabels.value
    },
    yAxis: {
      type: 'value',
      name: '碳排放(kgCO2)'
    },
    series: [
      {
        name: '每周碳排放',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        areaStyle: {
          opacity: 0.08
        },
        lineStyle: {
          width: 3
        },
        data: carbonSeriesData.value
      }
    ]
  })
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(selectedYear, async () => {
  await fetchXAxisLabels(selectedYear.value)
  await nextTick()
  renderChart()
})

onMounted(async () => {
  await fetchXAxisLabels(selectedYear.value)
  await nextTick()
  renderChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<template>
  <div class="week-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <span class="title">按周碳排放趋势</span>
          <div class="controls">
            <span>年份：</span>
            <el-input-number
              v-model="selectedYear"
              :min="2000"
              :max="2100"
              :step="1"
              :precision="0"
              controls-position="right"
              style="width: 160px"
            />
          </div>
        </div>
      </template>

      <div ref="chartRef" class="chart-box"></div>
    </el-card>
  </div>
</template>

<style scoped>
.week-page {
  padding: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-weight: 600;
  font-size: 16px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-box {
  width: 100%;
  height: 460px;
}
</style>
