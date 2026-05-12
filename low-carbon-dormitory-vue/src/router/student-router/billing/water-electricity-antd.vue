<template>
  <div class="utility-mobile-page">
    <div class="utility-mobile-shell">
      <div class="utility-mobile-phone">
        <ASpin :spinning="loading || refreshing">
          <header class="utility-hero">
            <div class="utility-hero__glow utility-hero__glow--left"></div>
            <div class="utility-hero__glow utility-hero__glow--right"></div>

            <div class="utility-hero__top">
              <button type="button" class="hero-back" @click="goPrevious">
                <LeftOutlined />
              </button>

              <div class="utility-hero__copy">
                <h1>宿舍水电费</h1>
                <p>{{ currentDormLabel }}</p>
              </div>

              <button type="button" class="hero-switch" @click="goPay">
                立即充值
              </button>
            </div>

            <div class="hero-balance-grid">
              <article class="hero-balance-card hero-balance-card--electric" :class="statusInfo.electricClass">
                <div class="hero-balance-card__label">
                  <span class="hero-balance-card__icon">⚡</span>
                  <span>电费余额</span>
                </div>
                <strong>{{ formatCurrency(waterElectricity?.electricityBalance) }}</strong>
                <small>预计可用 {{ formatAvailable(waterElectricity?.electricityAvailable, waterElectricity?.electricityUnitName || null) }}</small>
              </article>

              <article class="hero-balance-card hero-balance-card--water" :class="statusInfo.waterClass">
                <div class="hero-balance-card__label">
                  <span class="hero-balance-card__icon">💧</span>
                  <span>水费余额</span>
                </div>
                <strong>{{ formatCurrency(waterElectricity?.waterBalance) }}</strong>
                <small>预计可用 {{ formatAvailable(waterElectricity?.waterAvailable, waterElectricity?.waterUnitName || null) }}</small>
              </article>
            </div>
          </header>

          <AAlert
            v-if="errorMessage"
            class="utility-error"
            type="error"
            show-icon
            :message="errorMessage"
          />

          <template v-else-if="waterElectricity">
            <div class="chart-row">
              <section class="section-card section-card--chart">
                <div class="section-head">
                  <div>
                    <span class="section-tag">近七天</span>
                    <h2>费用构成占比</h2>
                  </div>
                  <div class="section-head__value">{{ formatCurrency(recentSummary.totalCost) }}</div>
                </div>

                <div ref="donutChartRef" class="chart-surface chart-surface--donut"></div>

                <div class="legend-grid">
                  <article class="legend-card">
                    <span class="legend-card__dot legend-card__dot--electric"></span>
                    <div>
                      <strong>电费</strong>
                      <small>{{ formatCurrency(recentSummary.electricCost) }} · {{ recentSummary.electricPercent }}</small>
                    </div>
                  </article>
                  <article class="legend-card">
                    <span class="legend-card__dot legend-card__dot--water"></span>
                    <div>
                      <strong>水费</strong>
                      <small>{{ formatCurrency(recentSummary.waterCost) }} · {{ recentSummary.waterPercent }}</small>
                    </div>
                  </article>
                </div>
              </section>

              <section class="section-card section-card--chart">
                <div class="section-head">
                  <div>
                    <span class="section-tag">近七天</span>
                    <h2>费用趋势折线</h2>
                  </div>
                  <div class="section-head__value">{{ recentSummary.latestDayLabel }}</div>
                </div>

                <div ref="lineChartRef" class="chart-surface chart-surface--line"></div>
              </section>
            </div>

            <section class="section-card section-card--records">
              <div class="section-head">
                <div>
                  <span class="section-tag">最近记录</span>
                  <h2>缴费记录</h2>
                </div>
                <button type="button" class="section-link" @click="goHistory">查看账单</button>
              </div>

              <div v-if="recentRecords.length" class="record-list">
                <article v-for="item in recentRecords" :key="item.id" class="record-item">
                  <div class="record-item__icon" :class="`record-item__icon--${item.accent}`">
                    {{ item.icon }}
                  </div>
                  <div class="record-item__content">
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.time }}</small>
                  </div>
                  <div class="record-item__amount" :class="item.amountClass">
                    <strong>{{ item.amountText }}</strong>
                    <small>{{ item.subtitle }}</small>
                  </div>
                </article>
              </div>
              <AEmpty v-else description="暂无账单记录" />
            </section>
          </template>
        </ASpin>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Alert as AAlert, Empty as AEmpty, Spin as ASpin, message } from 'ant-design-vue'
import { LeftOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import * as echarts from 'echarts'
import 'ant-design-vue/dist/reset.css'
import {
  fetchFeeHistory,
  fetchWaterElectricity,
  refreshWaterElectricity,
  type FeeHistoryRecord,
  type StudentWaterElectricity,
} from '@/api/modules/student'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
import {
  formatCurrency,
  formatDateTime,
  formatDormLabel,
  formatFeeTypeText,
  formatNumber,
  formatOperationTypeText,
} from '@/utils/formatters'
import { useStudentTokenStore } from '@/stores/student-token'
import { buildUtilityMobileQuery, readQueryText } from '@/utils/mobile-billing'

type RecentDayItem = {
  key: string
  label: string
  electric: number
  water: number
}

type RecentRecordItem = {
  id: number
  title: string
  time: string
  amountText: string
  subtitle: string
  amountClass: string
  accent: 'green' | 'blue' | 'amber'
  icon: string
}

const route = useRoute()
const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { stuNum, dormId, dormLabel } = storeToRefs(studentTokenStore)

const loading = ref(true)
const refreshing = ref(false)
const errorMessage = ref('')
const waterElectricity = ref<StudentWaterElectricity | null>(null)
const feeHistory = ref<FeeHistoryRecord[]>([])
const donutChartRef = ref<HTMLDivElement | null>(null)
const lineChartRef = ref<HTMLDivElement | null>(null)

let donutChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

const currentDormLabel = computed(() => formatDormLabel(waterElectricity.value, dormLabel.value))
const resolvedDormId = computed(() => waterElectricity.value?.dormId ?? dormId.value ?? null)
const routeStuNum = computed(() => readQueryText(route.query.stuNum))
const activeStuNum = computed(() => routeStuNum.value || stuNum.value || '')

const statusInfo = computed(() => {
  const electric = Number(waterElectricity.value?.electricityBalance || 0)
  const water = Number(waterElectricity.value?.waterBalance || 0)

  if (electric <= 20 || water <= 40) {
    return {
      electricClass: electric <= 20 ? 'is-danger' : '',
      waterClass: water <= 40 ? 'is-danger' : '',
    }
  }

  if (electric <= 80 || water <= 100) {
    return {
      electricClass: electric <= 80 ? 'is-warning' : '',
      waterClass: water <= 100 ? 'is-warning' : '',
    }
  }

  return {
    electricClass: '',
    waterClass: '',
  }
})

const recentDailySeries = computed<RecentDayItem[]>(() => buildRecentDailySeries(feeHistory.value))

const recentSummary = computed(() => {
  const electricCost = recentDailySeries.value.reduce((sum, item) => sum + item.electric, 0)
  const waterCost = recentDailySeries.value.reduce((sum, item) => sum + item.water, 0)
  const totalCost = electricCost + waterCost
  return {
    electricCost,
    waterCost,
    totalCost,
    electricPercent: totalCost > 0 ? `${Math.round((electricCost / totalCost) * 100)}%` : '0%',
    waterPercent: totalCost > 0 ? `${Math.round((waterCost / totalCost) * 100)}%` : '0%',
    latestDayLabel: recentDailySeries.value[recentDailySeries.value.length - 1]?.label || '-',
  }
})

const recentRecords = computed<RecentRecordItem[]>(() =>
  feeHistory.value.slice(0, 5).map((item) => {
    const operationType = String(item.operationType || '').toUpperCase()
    const amount = Number(item.amount || 0)
    const isRecharge = operationType === 'RECHARGE'
    const isRefresh = operationType === 'REFRESH'

    return {
      id: item.id,
      title: `${formatFeeTypeText(item.feeType)}${formatOperationTypeText(operationType)}`,
      time: formatDateTime(item.createTime, '-'),
      amountText: `${isRecharge ? '+' : operationType === 'DEDUCT' ? '-' : ''}${formatCurrency(amount)}`,
      subtitle: isRefresh ? '系统同步' : `${item.payerName || '-'} · ${formatFeeTypeText(item.feeType)}`,
      amountClass: isRecharge
        ? 'record-item__amount--plus'
        : isRefresh
          ? 'record-item__amount--neutral'
          : 'record-item__amount--minus',
      accent: isRecharge ? 'green' : isRefresh ? 'blue' : 'amber',
      icon: isRecharge ? '¥' : String(item.feeType || '').toUpperCase() === 'WATER' ? '💧' : '⚡',
    }
  }),
)

function goPrevious() {
  router.back()
}

function goPay() {
  router.push({
    path: '/pay-up-antd',
    query: buildMobileQuery(),
  })
}

function goHistory() {
  router.push({
    path: '/history-fee-antd',
    query: buildMobileQuery(),
  })
}

function buildMobileQuery() {
  return buildUtilityMobileQuery(routeStuNum.value, resolvedDormId.value)
}

function normalizeUnit(unitName: string | null) {
  const normalized = String(unitName || '').toLowerCase()
  if (normalized.includes('kwh') || normalized.includes('电')) return '度'
  if (normalized.includes('ton') || normalized.includes('水')) return '吨'
  return unitName || ''
}

function formatAvailable(value: number | null | undefined, unitName: string | null) {
  if (value === null || value === undefined) return '-'
  return `${formatNumber(value, 2)} ${normalizeUnit(unitName)}`.trim()
}

function isRecentCostRecord(item: FeeHistoryRecord) {
  const operationType = String(item.operationType || '').toUpperCase()
  return operationType === 'DEDUCT' || operationType === 'CONSUME' || operationType === 'EXPENSE'
}

function buildRecentDailySeries(records: FeeHistoryRecord[]) {
  const dayMap = new Map<string, RecentDayItem>()
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - offset)
    const key = toDateKey(date)
    dayMap.set(key, {
      key,
      label: `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`,
      electric: 0,
      water: 0,
    })
  }

  records.forEach((item) => {
    if (!isRecentCostRecord(item)) return
    const parsedDate = new Date(item.createTime)
    if (Number.isNaN(parsedDate.getTime())) return
    const key = toDateKey(parsedDate)
    const day = dayMap.get(key)
    if (!day) return
    const amount = Number(item.amount || 0)
    if (String(item.feeType || '').toUpperCase() === 'WATER') {
      day.water += amount
    } else {
      day.electric += amount
    }
  })

  return Array.from(dayMap.values())
}

function toDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

async function loadPageData() {
  if (!activeStuNum.value && !dormId.value) {
    errorMessage.value = '未检测到登录信息，请重新登录'
    loading.value = false
    return
  }

  loading.value = true
  try {
    const utilityResponse = await fetchWaterElectricity({
      stuNum: activeStuNum.value || undefined,
      dormId: dormId.value || undefined,
    })
    waterElectricity.value = requireApiData(utilityResponse.data, '获取水电信息失败')
    studentTokenStore.updateCarbonScore(waterElectricity.value.personalCarbonScore)

    if (resolvedDormId.value) {
      const historyResponse = await fetchFeeHistory({ dormId: resolvedDormId.value, pageNum: 1, pageSize: 50 })
      const historyPage = requireApiData(historyResponse.data, '获取历史记录失败')
      feeHistory.value = historyPage.records || []
    } else {
      feeHistory.value = []
    }

    errorMessage.value = ''
    await nextTick()
    renderCharts()
  } catch (error) {
    console.error('加载水电费页面失败:', error)
    errorMessage.value = resolveErrorMessage(error, '服务异常或网络错误')
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  if (!activeStuNum.value && !dormId.value) {
    message.warning('未检测到登录信息，请重新登录')
    return
  }

  refreshing.value = true
  try {
    const { data } = await refreshWaterElectricity({
      stuNum: activeStuNum.value || undefined,
      dormId: dormId.value || undefined,
    })
    waterElectricity.value = requireApiData(data, '刷新水电费失败')
    studentTokenStore.updateCarbonScore(waterElectricity.value.personalCarbonScore)
    message.success('刷新完成，余额与积分已同步。')
    await loadHistoryOnly()
    await nextTick()
    renderCharts()
  } catch (error) {
    console.error('刷新水电费失败:', error)
    message.error(resolveErrorMessage(error, '刷新失败，请稍后重试'))
  } finally {
    refreshing.value = false
  }
}

async function loadHistoryOnly() {
  if (!resolvedDormId.value) {
    feeHistory.value = []
    return
  }
  const { data } = await fetchFeeHistory({ dormId: resolvedDormId.value, pageNum: 1, pageSize: 50 })
  const historyPage = requireApiData(data, '获取历史记录失败')
  feeHistory.value = historyPage.records || []
}

function renderCharts() {
  renderDonutChart()
  renderLineChart()
}

function renderDonutChart() {
  if (!donutChartRef.value) return
  donutChart = donutChart || echarts.init(donutChartRef.value)
  donutChart.setOption({
    animationDuration: 500,
    color: ['#29b36a', '#5aaef6'],
    tooltip: {
      trigger: 'item',
      formatter: (params: { name: string; value: number }) => `${params.name}：${formatCurrency(params.value)}`,
    },
    series: [
      {
        type: 'pie',
        radius: ['62%', '82%'],
        center: ['50%', '50%'],
        startAngle: 90,
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: {
          borderRadius: 10,
          borderColor: '#ffffff',
          borderWidth: 4,
        },
        data: [
          { name: '电费', value: recentSummary.value.electricCost },
          { name: '水费', value: recentSummary.value.waterCost },
        ],
      },
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '43%',
        style: {
          text: '近七天总费用',
          fill: '#7a9487',
          fontSize: 12,
          fontWeight: 500,
        },
      },
      {
        type: 'text',
        left: 'center',
        top: '52%',
        style: {
          text: formatCurrency(recentSummary.value.totalCost),
          fill: '#193d2d',
          fontSize: 22,
          fontWeight: 700,
        },
      },
    ],
  })
}

function renderLineChart() {
  if (!lineChartRef.value) return
  lineChart = lineChart || echarts.init(lineChartRef.value)
  lineChart.setOption({
    animationDuration: 500,
    color: ['#f59e0b', '#2f80ed'],
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: number) => formatCurrency(Number(value || 0)),
    },
    grid: {
      left: 8,
      right: 8,
      top: 22,
      bottom: 12,
      containLabel: true,
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 8,
      itemHeight: 8,
      textStyle: {
        color: '#5c756a',
        fontSize: 10,
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: recentDailySeries.value.map((item) => item.label),
      axisLine: { lineStyle: { color: '#d9ebe0' } },
      axisTick: { show: false },
      axisLabel: { color: '#81988d', fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(100, 140, 118, 0.14)' } },
      axisLabel: {
        color: '#81988d',
        fontSize: 10,
        formatter: (value: number) => `${value}元`,
      },
    },
    series: [
      {
        name: '电费',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3 },
        areaStyle: { color: 'rgba(245, 158, 11, 0.12)' },
        data: recentDailySeries.value.map((item) => Number(item.electric.toFixed(2))),
      },
      {
        name: '水费',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3 },
        areaStyle: { color: 'rgba(47, 128, 237, 0.10)' },
        data: recentDailySeries.value.map((item) => Number(item.water.toFixed(2))),
      },
    ],
  })
}

function handleResize() {
  donutChart?.resize()
  lineChart?.resize()
}

watch(recentDailySeries, async () => {
  await nextTick()
  renderCharts()
})

onMounted(async () => {
  await loadPageData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  donutChart?.dispose()
  lineChart?.dispose()
  donutChart = null
  lineChart = null
})
</script>

<style scoped>
.utility-mobile-page {
  min-height: 100vh;
  padding: 18px 12px 32px;
  background:
    radial-gradient(circle at 10% 5%, rgba(170, 243, 204, 0.6), transparent 26%),
    radial-gradient(circle at 90% 8%, rgba(134, 239, 172, 0.4), transparent 24%),
    radial-gradient(circle at 50% 0%, rgba(207, 250, 229, 0.9), transparent 38%),
    linear-gradient(180deg, #ebfff0 0%, #f7fff9 52%, #edfdf1 100%);
}

.utility-mobile-shell {
  display: flex;
  justify-content: center;
}

.utility-mobile-phone {
  width: min(100%, 430px);
  display: grid;
  gap: 14px;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.utility-hero {
  position: relative;
  overflow: hidden;
  padding: 16px 14px 14px;
  border-radius: 28px;
  background:
    radial-gradient(circle at 50% 0%, rgba(255, 255, 255, 0.95), transparent 48%),
    linear-gradient(160deg, rgba(237, 255, 244, 0.96), rgba(224, 250, 232, 0.88));
  box-shadow: 0 22px 44px rgba(44, 112, 70, 0.13);
  border: 1px solid rgba(188, 231, 200, 0.9);
}

.utility-hero__glow {
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(120, 210, 154, 0.16);
  filter: blur(2px);
}

.utility-hero__glow--left {
  top: -28px;
  left: -70px;
}

.utility-hero__glow--right {
  top: -56px;
  right: -36px;
}

.utility-hero__top {
  position: relative;
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.hero-back,
.hero-switch,
.section-link {
  border: 0;
  cursor: pointer;
}

.hero-back {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.72);
  color: #2d8a53;
  box-shadow: inset 0 0 0 1px rgba(164, 218, 180, 0.7);
}

.hero-switch {
  min-width: 88px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: #236c43;
  font-size: 13px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(171, 223, 186, 0.85);
}

.utility-hero__copy h1 {
  margin: 0;
  color: #0f5534;
  font-size: 26px;
  line-height: 1.1;
  font-weight: 800;
}

.utility-hero__copy p {
  margin: 4px 0 0;
  color: #205f3d;
  font-size: 13px;
  font-weight: 700;
}

.hero-balance-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-balance-card {
  min-height: 122px;
  padding: 14px 14px 12px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(238, 197, 185, 0.9);
  box-shadow: 0 14px 28px rgba(77, 140, 98, 0.08);
  display: grid;
  align-content: start;
  gap: 10px;
}

.hero-balance-card--water {
  border-color: rgba(238, 197, 185, 0.9);
}

.hero-balance-card__label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #4d8b68;
  font-size: 13px;
  font-weight: 700;
}

.hero-balance-card__icon {
  font-size: 16px;
}

.hero-balance-card strong {
  color: #2bb98a;
  font-size: 30px;
  line-height: 1.05;
  font-weight: 800;
}

.hero-balance-card small {
  color: #6f9380;
  font-size: 12px;
  line-height: 1.5;
}

.hero-balance-card.is-danger {
  background: #fff7f5;
  border-color: #f0d0c4;
}

.hero-balance-card.is-warning {
  background: #fffdf4;
  border-color: #efdfb0;
}

.utility-error {
  border-radius: 18px;
}

.section-card {
  padding: 10px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(214, 235, 220, 0.9);
  box-shadow: 0 18px 40px rgba(48, 101, 66, 0.1);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.section-tag {
  display: inline-block;
  margin-bottom: 4px;
  padding: 2px 7px;
  border-radius: 999px;
  background: #ecf8ef;
  color: #47815f;
  font-size: 10px;
  font-weight: 700;
}

.section-head h2 {
  margin: 0;
  color: #1b4732;
  font-size: 15px;
  line-height: 1.2;
  font-weight: 800;
}

.section-head__value {
  color: #315841;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.section-link {
  padding: 0;
  background: transparent;
  color: #4b7c64;
  font-size: 12px;
  font-weight: 700;
}

.chart-surface {
  width: 100%;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(244, 252, 247, 0.95), rgba(255, 255, 255, 0.98)),
    #fff;
}

.chart-surface--donut,
.chart-surface--line {
  height: 128px;
}

.legend-grid {
  margin-top: 8px;
  display: grid;
  gap: 6px;
}

.legend-card {
  padding: 7px 8px;
  border-radius: 14px;
  border: 1px solid #e1eee5;
  background: #f8fcf9;
  display: flex;
  gap: 8px;
  align-items: center;
}

.legend-card__dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.legend-card__dot--electric {
  background: #29b36a;
}

.legend-card__dot--water {
  background: #5aaef6;
}

.legend-card strong {
  color: #1b4732;
  font-size: 12px;
}

.legend-card small {
  color: #739284;
  font-size: 9px;
}

.record-list {
  display: grid;
  gap: 10px;
}

.record-item {
  padding: 12px 0;
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: 10px;
  align-items: center;
  border-bottom: 1px solid #edf4ef;
}

.record-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.record-item__icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 700;
}

.record-item__icon--green {
  color: #2aa05d;
  background: rgba(96, 214, 138, 0.18);
}

.record-item__icon--blue {
  color: #2a7be9;
  background: rgba(102, 175, 245, 0.18);
}

.record-item__icon--amber {
  color: #dd960a;
  background: rgba(247, 187, 73, 0.18);
}

.record-item__content {
  display: grid;
  gap: 4px;
}

.record-item__content strong,
.record-item__amount strong {
  color: #1b4732;
  font-size: 15px;
  font-weight: 800;
}

.record-item__content small,
.record-item__amount small {
  color: #739284;
  font-size: 11px;
}

.record-item__amount {
  display: grid;
  justify-items: end;
  gap: 4px;
}

.record-item__amount--plus strong {
  color: #2aa05d;
}

.record-item__amount--minus strong {
  color: #dd960a;
}

.record-item__amount--neutral strong {
  color: #2a7be9;
}

@media (max-width: 420px) {
  .utility-mobile-page {
    padding-left: 10px;
    padding-right: 10px;
  }

  .utility-hero__copy h1 {
    font-size: 24px;
  }
}

@media (max-width: 360px) {
  .hero-balance-grid,
  .chart-row {
    grid-template-columns: 1fr;
  }

  .record-item {
    grid-template-columns: 40px 1fr;
  }

  .record-item__amount {
    grid-column: 2;
    justify-items: start;
  }
}
</style>
