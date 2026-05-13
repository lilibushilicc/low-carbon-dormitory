<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { monthCarbonService } from '../api/studentMealWaste.js'

const selectedYear = ref(new Date().getFullYear())
const chartRef = ref(null)
const xAxisLabels = ref([])
const carbonSeriesData = ref([])
const loading = ref(false)
let chartInstance = null

const hasData = computed(() => carbonSeriesData.value.length > 0)
const singlePoint = computed(() => carbonSeriesData.value.length === 1)

const fetchMonthData = async (year) => {
  loading.value = true
  try {
    const res = await monthCarbonService({ yearCode: year })
    const payload = res?.data
    const list = Array.isArray(payload) ? payload : (payload?.list || [])
    if (Array.isArray(list) && list.length > 0) {
      xAxisLabels.value = list.map((item, index) => {
        if (typeof item === 'string' || typeof item === 'number') return String(item)
        return String(item.monthName || item.monthLabel || item.monthCode || item.month || `${index + 1} 月`)
      })
      carbonSeriesData.value = list.map((item) => {
        if (typeof item === 'number') return item
        const value = Number(item.totalCarbonEmission ?? item.monthCarbonEmission ?? item.value ?? 0)
        return Number.isNaN(value) ? 0 : value
      })
      return
    }

    xAxisLabels.value = []
    carbonSeriesData.value = []
  } catch (error) {
    xAxisLabels.value = []
    carbonSeriesData.value = []
    ElMessage.error('按月碳排放数据加载失败')
  } finally {
    loading.value = false
  }
}

const renderChart = () => {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  chartInstance.setOption({
    title: {
      text: `${selectedYear.value} 年按月碳排放变化`,
      left: 'center',
    },
    tooltip: {
      trigger: hasData.value ? 'axis' : 'item',
    },
    grid: {
      left: '4%',
      right: '4%',
      bottom: '6%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: singlePoint.value,
      data: xAxisLabels.value,
    },
    yAxis: {
      type: 'value',
      name: '碳排放(kgCO2)',
    },
    series: hasData.value
      ? [
          {
            name: '月度碳排放',
            type: singlePoint.value ? 'bar' : 'line',
            smooth: !singlePoint.value,
            symbol: 'circle',
            symbolSize: singlePoint.value ? 16 : 10,
            barMaxWidth: 72,
            areaStyle: singlePoint.value
              ? undefined
              : {
                  opacity: 0.12,
                },
            lineStyle: singlePoint.value
              ? undefined
              : {
                  width: 3,
                },
            itemStyle: {
              color: '#409eff',
            },
            label: {
              show: true,
              position: 'top',
              formatter: ({ value }) => `${value}`,
            },
            data: carbonSeriesData.value,
          },
        ]
      : [],
    graphic: hasData.value
      ? []
      : [
          {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: `${selectedYear.value} 年暂无月度碳排放数据`,
              fill: '#909399',
              fontSize: 16,
              fontWeight: 500,
            },
          },
        ],
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
    <el-card v-loading="loading">
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

      <div class="summary-tip">
        <el-tag v-if="singlePoint" type="warning" effect="light">当前年份仅有 1 条月度数据，已切换为柱状展示</el-tag>
        <el-tag v-else-if="hasData" type="success" effect="light">已加载 {{ carbonSeriesData.length }} 条月度数据</el-tag>
        <el-tag v-else type="info" effect="light">当前年份暂无月度数据</el-tag>
      </div>

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

.summary-tip {
  margin-bottom: 14px;
}

.chart-box {
  width: 100%;
  height: 460px;
}
</style>
