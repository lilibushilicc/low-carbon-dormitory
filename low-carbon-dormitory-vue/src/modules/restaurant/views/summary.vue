<template>
  <div class="summary-page">
    <el-card shadow="hover" class="query-card">
      <div class="query-form">
        <el-select v-model="query.periodType" placeholder="请选择周期类型" class="field">
          <el-option label="按周查询" value="week" />
          <el-option label="按月查询" value="month" />
          <el-option label="按学期查询" value="term" />
        </el-select>

        <el-date-picker
          v-model="query.year"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          class="field"
        />

        <el-select
          v-if="query.periodType === 'week'"
          v-model="query.periodValue"
          placeholder="选择周次"
          class="field"
        >
          <el-option v-for="i in 52" :key="i" :label="`第 ${i} 周`" :value="String(i)" />
        </el-select>

        <el-select
          v-else-if="query.periodType === 'month'"
          v-model="query.periodValue"
          placeholder="选择月份"
          class="field"
        >
          <el-option v-for="i in 12" :key="i" :label="`${i} 月`" :value="String(i)" />
        </el-select>

        <el-select
          v-else
          v-model="query.periodValue"
          placeholder="选择学期"
          class="field"
        >
          <el-option label="春季学期" value="1" />
          <el-option label="秋季学期" value="2" />
        </el-select>

        <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
      </div>
    </el-card>

    <el-card shadow="hover" class="chart-card" v-loading="loading">
      <div class="chart-header">
        <div>
          <h2>餐厅首页统计</h2>
          <p>当前参数：{{ periodLabel }}</p>
        </div>
        <el-tag :type="chartData.length ? 'success' : 'info'" effect="light">
          {{ chartData.length ? `共 ${chartData.length} 个学院` : '当前参数暂无数据' }}
        </el-tag>
      </div>

      <div ref="chartRef" class="chart-box"></div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '../util/request.js'

const chartRef = ref(null)
const loading = ref(false)
const chartData = ref([])

function getCurrentYear() {
  return new Date().getFullYear().toString()
}

function getCurrentMonth() {
  return String(new Date().getMonth() + 1)
}

function getCurrentTerm() {
  const month = new Date().getMonth() + 1
  return month >= 2 && month <= 7 ? '1' : '2'
}

function getCurrentWeek() {
  const now = new Date()
  const date = new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()))
  const day = date.getUTCDay() || 7
  date.setUTCDate(date.getUTCDate() + 4 - day)
  const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1))
  const weekNo = Math.ceil((((date - yearStart) / 86400000) + 1) / 7)
  return String(weekNo)
}

function resolveDefaultPeriodValue(periodType) {
  if (periodType === 'month') return getCurrentMonth()
  if (periodType === 'term') return getCurrentTerm()
  return getCurrentWeek()
}

const query = ref({
  periodType: 'week',
  year: getCurrentYear(),
  periodValue: resolveDefaultPeriodValue('week'),
})

let chart = null

const periodLabel = computed(() => {
  const { periodType, year, periodValue } = query.value
  if (periodType === 'week') return `${year} 年第 ${periodValue} 周`
  if (periodType === 'month') return `${year} 年 ${periodValue} 月`
  return `${year} 年 ${periodValue === '1' ? '春季学期' : '秋季学期'}`
})

function ensureChart() {
  if (!chartRef.value) return null
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  return chart
}

function buildGraphicText() {
  return chartData.value.length ? '' : `${periodLabel.value} 暂无数据`
}

function renderChart() {
  const instance = ensureChart()
  if (!instance) return

  instance.setOption({
    title: {
      text: '各学院碳排放量统计',
      subtext: periodLabel.value,
      left: 'center',
    },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: {
      left: '4%',
      right: '4%',
      bottom: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: chartData.value.map((item) => item.orgName || '未命名学院'),
      axisLabel: {
        interval: 0,
        rotate: chartData.value.length > 6 ? 25 : 0,
      },
    },
    yAxis: {
      type: 'value',
      name: '碳排放量(kg)',
    },
    series: [
      {
        name: '碳排放量',
        type: 'bar',
        barMaxWidth: 52,
        data: chartData.value.map((item) => Number(item.totalCarbonEmission || 0)),
        itemStyle: { color: '#409eff' },
      },
    ],
    graphic: buildGraphicText()
      ? [
          {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: buildGraphicText(),
              fill: '#909399',
              fontSize: 16,
              fontWeight: 500,
            },
          },
        ]
      : [],
  })
}

async function loadData() {
  loading.value = true
  try {
    const response = await request.post('/summary', query.value)
    chartData.value = Array.isArray(response?.data) ? response.data : []
    await nextTick()
    renderChart()
  } catch (error) {
    chartData.value = []
    await nextTick()
    renderChart()
    ElMessage.error('餐厅首页数据加载失败，请检查接口配置或查询参数')
  } finally {
    loading.value = false
  }
}

function resizeChart() {
  chart?.resize()
}

watch(
  () => query.value.periodType,
  (periodType) => {
    query.value.periodValue = resolveDefaultPeriodValue(periodType)
  },
)

onMounted(async () => {
  await nextTick()
  renderChart()
  await loadData()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.summary-page {
  display: grid;
  gap: 20px;
  padding: 20px;
}

.query-card,
.chart-card {
  border-radius: 20px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.field {
  width: 160px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.chart-header h2 {
  margin: 0;
  color: #303133;
  font-size: 20px;
}

.chart-header p {
  margin: 6px 0 0;
  color: #606266;
  font-size: 14px;
}

.chart-box {
  width: 100%;
  height: 460px;
}

@media (max-width: 768px) {
  .summary-page {
    padding: 12px;
  }

  .field {
    width: 100%;
  }

  .chart-header {
    flex-direction: column;
  }
}
</style>
