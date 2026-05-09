<template>
  <div class="page">
    <div class="header">
      <div>
        <h2>水电费用历史</h2>
        <p>查看宿舍最近 30 条费用充值、刷新结算和扣费记录。</p>
      </div>
      <div class="actions">
        <el-button @click="goPrevious">返回上一页</el-button>
        <el-button type="primary" plain @click="goBack">返回首页</el-button>
        <el-button type="primary" @click="goPay">去充值</el-button>
      </div>
    </div>

    <div class="summary">
      <div class="summary-card">
        <span>最近记录</span>
        <strong>{{ historyList.length }}</strong>
      </div>
      <div class="summary-card">
        <span>累计充值</span>
        <strong>{{ totalRechargeAmount }}</strong>
      </div>
      <div class="summary-card">
        <span>最近支付时间</span>
        <strong>{{ latestPayTime }}</strong>
      </div>
    </div>

    <div class="filters">
      <el-segmented v-model="activeFilter" :options="filterOptions" />
    </div>

    <div v-loading="loading" class="timeline-shell">
      <template v-if="filteredHistory.length">
        <article v-for="item in filteredHistory" :key="item.historyId" class="event-card">
          <div class="event-card__top">
            <div class="event-card__title">
              <span class="event-badge" :class="item.eventClass">{{ item.eventLabel }}</span>
              <strong>{{ item.title }}</strong>
            </div>
            <span class="event-time">{{ item.payTime }}</span>
          </div>

          <div class="event-card__body">
            <div class="event-main">
              <div class="event-amount" :class="item.amountClass">{{ item.amountText }}</div>
              <div class="event-balance">余额：{{ item.balanceAfterText }}</div>
              <p class="event-description">{{ item.description }}</p>
            </div>

            <div class="event-meta">
              <div class="meta-item">
                <span>宿舍</span>
                <strong>{{ item.dormNo }}</strong>
              </div>
              <div class="meta-item">
                <span>费用类型</span>
                <strong>{{ item.feeTypeText }}</strong>
              </div>
              <div class="meta-item">
                <span>付款人</span>
                <strong>{{ item.payerName }}</strong>
              </div>
              <div class="meta-item">
                <span>学号</span>
                <strong>{{ item.payerStuNum }}</strong>
              </div>
              <div class="meta-item">
                <span>支付类型</span>
                <strong>{{ item.payTypeText }}</strong>
              </div>
              <div class="meta-item">
                <span>流水ID</span>
                <strong>{{ item.historyId }}</strong>
              </div>
            </div>
          </div>
        </article>
      </template>

      <el-empty v-else-if="!loading" description="当前筛选条件下暂无事件记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { formatCurrency, formatDateTime, formatFeeTypeText, formatPayTypeText } from '@/utils/formatters'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
import { useStudentTokenStore } from '@/stores/student-token'
import { fetchFeeHistory } from '@/api/modules/student'

type EventFilter = 'all' | 'recharge' | 'refresh' | 'deduct'

interface FeeHistoryRow {
  historyId: number
  dormNo: string
  payerName: string
  payerStuNum: string
  feeTypeText: string
  payTypeText: string
  amountText: string
  balanceAfterText: string
  payTime: string
  rawAmount: number
  operationType: string
  eventType: EventFilter
  eventLabel: string
  eventClass: string
  amountClass: string
  title: string
  description: string
}

const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { dormId, dormLabel } = storeToRefs(studentTokenStore)

const historyList = ref<FeeHistoryRow[]>([])
const loading = ref(false)
const activeFilter = ref<EventFilter>('all')

const filterOptions = [
  { label: '全部记录', value: 'all' },
  { label: '充值结算', value: 'recharge' },
  { label: '刷新结算', value: 'refresh' },
  { label: '周期扣费', value: 'deduct' },
]

const filteredHistory = computed(() => {
  if (activeFilter.value === 'all') return historyList.value
  return historyList.value.filter((item) => item.eventType === activeFilter.value)
})

const totalRechargeAmount = computed(() => {
  const total = historyList.value
    .filter((item) => item.operationType === 'RECHARGE')
    .reduce((sum, item) => sum + Number(item.rawAmount || 0), 0)
  return formatCurrency(total)
})

const latestPayTime = computed(() => historyList.value[0]?.payTime || '-')

function goBack() {
  router.push('/index-student')
}

function goPrevious() {
  router.back()
}

function goPay() {
  router.push('/pay-up')
}

function formatHistoryAmount(operationType: string, amount: number) {
  const normalized = operationType.toUpperCase()
  if (normalized === 'RECHARGE') return `+${formatCurrency(amount)}`
  if (normalized === 'DEDUCT') return `-${formatCurrency(amount)}`
  return formatCurrency(amount)
}

function resolveEventMeta(operationType: string, feeTypeText: string, payTypeText: string) {
  const normalized = operationType.toUpperCase()
  if (normalized === 'RECHARGE') {
    return {
      eventType: 'recharge' as const,
      eventLabel: '充值结算',
      eventClass: 'event-badge--recharge',
      amountClass: 'event-amount--positive',
      title: `${feeTypeText}充值`,
      description: `本次通过${payTypeText}完成充值，系统会同步更新余额与积分。`,
    }
  }
  if (normalized === 'REFRESH') {
    return {
      eventType: 'refresh' as const,
      eventLabel: '刷新结算',
      eventClass: 'event-badge--refresh',
      amountClass: 'event-amount--neutral',
      title: `${feeTypeText}刷新`,
      description: '本次刷新会提交 0 元事件，并按当前消耗同步余额与积分状态。',
    }
  }
  return {
    eventType: 'deduct' as const,
    eventLabel: '周期扣费',
    eventClass: 'event-badge--deduct',
    amountClass: 'event-amount--negative',
    title: `${feeTypeText}扣费`,
    description: '这是系统根据一段时间内的使用情况自动完成的费用扣减记录。',
  }
}

async function loadHistory() {
  if (!dormId.value) {
    ElMessage.warning('未识别到宿舍信息')
    return
  }

  loading.value = true
  try {
    const { data } = await fetchFeeHistory({
      dormId: dormId.value,
      pageNum: 1,
      pageSize: 30,
    })

    const pageData = requireApiData(data, '获取历史记录失败')
    historyList.value = (pageData.records || []).map((item) => {
      const operationType = String(item.operationType || '').toUpperCase()
      const feeTypeText = formatFeeTypeText(item.feeType)
      const payTypeText = formatPayTypeText(item.payType)
      const eventMeta = resolveEventMeta(operationType, feeTypeText, payTypeText)

      return {
        historyId: item.id,
        dormNo: dormLabel.value,
        payerName: item.payerName || '-',
        payerStuNum: item.payerStuNum || '-',
        feeTypeText,
        payTypeText,
        amountText: formatHistoryAmount(operationType, Number(item.amount || 0)),
        balanceAfterText: formatCurrency(item.balanceAfter),
        payTime: formatDateTime(item.createTime, '-'),
        rawAmount: Number(item.amount || 0),
        operationType,
        ...eventMeta,
      }
    })
  } catch (error) {
    console.error('获取历史记录失败:', error)
    ElMessage.error(resolveErrorMessage(error, '获取历史记录失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: clamp(20px, 3vw, 36px) 16px;
  background:
    radial-gradient(circle at 8% 0%, rgba(135, 205, 156, 0.22), transparent 28%),
    radial-gradient(circle at 90% 12%, rgba(177, 224, 144, 0.18), transparent 26%),
    linear-gradient(180deg, #edf8f0 0%, #f8fcf6 100%);
}

.header,
.summary,
.filters,
.timeline-shell {
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

.header {
  margin-bottom: 18px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.header h2 {
  margin: 0 0 8px;
  color: #244536;
  font-size: 28px;
}

.header p {
  margin: 0;
  color: #5f776b;
  line-height: 1.7;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.summary {
  margin-bottom: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.summary-card {
  min-height: 76px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f8fef8 0%, #eef9ef 100%);
  border: 1px solid #cfe8d4;
  box-shadow: 0 8px 18px rgba(37, 102, 62, 0.08);
}

.summary-card span {
  display: block;
  color: #5f776b;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 6px;
  color: #244536;
  font-size: 20px;
  line-height: 1.25;
}

.filters {
  margin-bottom: 18px;
}

.timeline-shell {
  display: grid;
  gap: 14px;
}

.event-card {
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fcf8 100%);
  border: 1px solid #d5ead9;
  box-shadow: 0 18px 40px rgba(36, 92, 55, 0.1);
  padding: 18px;
}

.event-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.event-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.event-card__title strong {
  color: #244536;
  font-size: 20px;
}

.event-time {
  color: #698074;
  font-size: 13px;
}

.event-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.event-badge--recharge {
  background: rgba(40, 123, 76, 0.12);
  color: #1f7b4e;
}

.event-badge--refresh {
  background: rgba(96, 171, 84, 0.14);
  color: #347b33;
}

.event-badge--deduct {
  background: rgba(173, 94, 39, 0.12);
  color: #ad5e27;
}

.event-card__body {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr);
  gap: 18px;
}

.event-main {
  display: grid;
  gap: 10px;
}

.event-amount {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.1;
}

.event-amount--positive {
  color: #1e7a4c;
}

.event-amount--negative {
  color: #c25a2a;
}

.event-amount--neutral {
  color: #3f7d3f;
}

.event-balance,
.event-description {
  color: #5f776b;
}

.event-description {
  margin: 0;
  line-height: 1.7;
}

.event-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid #dcefe0;
  background: #f8fdf8;
  display: grid;
  gap: 6px;
}

.meta-item span {
  color: #698074;
  font-size: 12px;
}

.meta-item strong {
  color: #244536;
  font-size: 15px;
}

:deep(.el-button) {
  min-height: 42px;
  border-radius: 12px;
  border-color: #cde5d3;
  font-weight: 600;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #2f9b59;
  --el-button-border-color: #2f9b59;
  --el-button-hover-bg-color: #25894c;
  --el-button-hover-border-color: #25894c;
  --el-button-active-bg-color: #1f7541;
  --el-button-active-border-color: #1f7541;
}

:deep(.el-button--primary.is-plain) {
  --el-button-bg-color: #eef9f0;
  --el-button-border-color: #b9dfc2;
  --el-button-text-color: #257447;
  --el-button-hover-bg-color: #2f9b59;
  --el-button-hover-border-color: #2f9b59;
}

:deep(.el-segmented) {
  --el-segmented-item-selected-bg-color: #2f9b59;
  --el-segmented-item-selected-color: #ffffff;
  --el-segmented-item-hover-bg-color: #dff2e3;
  background: #e9f7ec;
  padding: 4px;
  border-radius: 14px;
}

@media (max-width: 920px) {
  .header,
  .event-card__top {
    flex-direction: column;
  }

  .event-card__body,
  .event-meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .summary {
    grid-template-columns: 1fr;
  }
}
</style>


