<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { monthCarbonService } from '../api/studentMealWaste.js'

const selectedYear = ref(2026)
const chartRef = ref(null)
const xAxisLabels = ref([])
const carbonSeriesData = ref([])
let chartInstance = null

const fetchMonthData = async (year) => {
  try {
    const res = await monthCarbonService({ yearCode: year })
    const payload = res?.data
    const list = Array.isArray(payload) ? payload : (payload?.list || [])
    if (Array.isArray(list) && list.length > 0) {
      xAxisLabels.value = list.map((item, index) => {
        if (typeof item === 'string' || typeof item === 'number') return String(item)
        return String(item.monthName || item.monthLabel || item.monthCode || item.month || `${index + 1}月`)
      })
      const parsedSeries = list.map((item) => {
        if (typeof item === 'number') return item
        const value = Number(
          item.totalCarbonEmission ??
          item.monthCarbonEmission ??
          item.value ??
          0
        )
        return Number.isNaN(value) ? 0 : value
      })
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
      text: `${selectedYear.value}年每月碳排放变化`,
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
        name: '每月碳排放',
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
  await fetchMonthData(selectedYear.value)
  await nextTick()
  renderChart()
})

onMounted(async () => {
  await fetchMonthData(selectedYear.value)
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
  <div class="month-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <span class="title">按月碳排放趋势</span>
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
.month-page {
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
