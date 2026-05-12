<template>
  <div class="history-mobile-page">
    <div class="history-mobile-shell">
      <div class="history-mobile-phone">
        <ASpin :spinning="loading">
          <header class="history-hero">
            <div class="history-hero__glow history-hero__glow--left"></div>
            <div class="history-hero__glow history-hero__glow--right"></div>

            <div class="history-hero__top">
              <button type="button" class="hero-icon-button" @click="goPrevious">
                <LeftOutlined />
              </button>

              <div class="history-hero__copy">
                <p class="history-hero__eyebrow">最近 30 条</p>
                <h1>历史订单</h1>
                <span>{{ currentDormLabel }}</span>
              </div>

              <button type="button" class="hero-text-button" @click="goPay">去缴费</button>
            </div>

            <div class="summary-grid">
              <article class="summary-card">
                <span>记录数量</span>
                <strong>{{ filteredHistory.length }}</strong>
              </article>
              <article class="summary-card">
                <span>累计充值</span>
                <strong>{{ totalRechargeAmount }}</strong>
              </article>
              <article class="summary-card">
                <span>最近支付</span>
                <strong>{{ latestPayTime }}</strong>
              </article>
            </div>
          </header>

          <AAlert
            v-if="errorMessage"
            class="history-error"
            type="error"
            show-icon
            :message="errorMessage"
          />

          <template v-else>
            <section class="filter-strip">
              <button
                v-for="item in filterOptions"
                :key="item.value"
                type="button"
                class="filter-chip"
                :class="{ 'filter-chip--active': activeFilter === item.value }"
                @click="activeFilter = item.value"
              >
                {{ item.label }}
              </button>
            </section>

            <section v-if="filteredHistory.length" class="record-list">
              <article v-for="item in filteredHistory" :key="item.historyId" class="record-card">
                <div class="record-card__top">
                  <div class="record-card__title">
                    <ATag :color="item.tagColor">{{ item.eventLabel }}</ATag>
                    <strong>{{ item.title }}</strong>
                    <span class="record-card__type-inline">{{ item.feeTypeText }}</span>
                  </div>
                  <span class="record-card__time">{{ item.payTime }}</span>
                </div>

                <div class="record-card__amount" :class="item.amountClass">{{ item.amountText }}</div>

                <button type="button" class="detail-toggle" @click="toggleDetail(item.historyId)">
                  {{ expandedHistoryId === item.historyId ? '收起详情' : '查看详情' }}
                </button>

                <div v-if="expandedHistoryId === item.historyId" class="record-card__detail">
                  <p class="record-card__desc">{{ item.description }}</p>

                  <div class="record-card__meta">
                    <div class="meta-pill">
                      <span>宿舍</span>
                      <strong>{{ item.dormNo }}</strong>
                    </div>
                    <div class="meta-pill">
                      <span>支付方式</span>
                      <strong>{{ item.payTypeText }}</strong>
                    </div>
                    <div class="meta-pill">
                      <span>余额</span>
                      <strong>{{ item.balanceAfterText }}</strong>
                    </div>
                    <div class="meta-pill">
                      <span>付款人</span>
                      <strong>{{ item.payerName }}</strong>
                    </div>
                    <div class="meta-pill">
                      <span>学号</span>
                      <strong>{{ item.payerStuNum }}</strong>
                    </div>
                    <div class="meta-pill">
                      <span>流水号</span>
                      <strong>{{ item.historyId }}</strong>
                    </div>
                  </div>
                </div>
              </article>
            </section>

            <AEmpty v-else class="history-empty" description="当前筛选条件下暂无历史订单" />
          </template>
        </ASpin>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Alert as AAlert, Empty as AEmpty, Spin as ASpin, Tag as ATag, message } from 'ant-design-vue'
import { LeftOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import 'ant-design-vue/dist/reset.css'
import { formatCurrency, formatDateTime, formatDormLabel, formatFeeTypeText, formatPayTypeText } from '@/utils/formatters'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
import {
  buildUtilityMobileQuery,
  parsePositiveId,
  readQueryText,
  UTILITY_MOBILE_SOURCE,
} from '@/utils/mobile-billing'
import { useStudentTokenStore } from '@/stores/student-token'
import { fetchFeeHistory, fetchWaterElectricity, type StudentWaterElectricity } from '@/api/modules/student'

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
  tagColor: string
  amountClass: string
  title: string
  description: string
}

const route = useRoute()
const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { stuNum, dormId, dormLabel } = storeToRefs(studentTokenStore)

const utilityInfo = ref<StudentWaterElectricity | null>(null)
const historyList = ref<FeeHistoryRow[]>([])
const loading = ref(false)
const errorMessage = ref('')
const activeFilter = ref<EventFilter>('all')
const expandedHistoryId = ref<number | null>(null)

const filterOptions: Array<{ label: string; value: EventFilter }> = [
  { label: '全部', value: 'all' },
  { label: '充值', value: 'recharge' },
  { label: '刷新', value: 'refresh' },
  { label: '扣费', value: 'deduct' },
]

const routeStuNum = computed(() => readQueryText(route.query.stuNum))

const routeSource = computed(() => readQueryText(route.query.source))

const publicAccessMode = computed(() => Boolean(routeStuNum.value))
const currentDormLabel = computed(() => formatDormLabel(utilityInfo.value, dormLabel.value))
const resolvedStuNum = computed(() => routeStuNum.value || stuNum.value || '')
const resolvedDormId = computed(() => utilityInfo.value?.dormId ?? parsePositiveId(route.query.dormId) ?? parsePositiveId(dormId.value))

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

function buildMobileQuery() {
  return buildUtilityMobileQuery(routeStuNum.value, resolvedDormId.value, routeSource.value === UTILITY_MOBILE_SOURCE)
}

function goPrevious() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push({
    path: '/water-electricity-antd',
    query: buildMobileQuery(),
  })
}

function goPay() {
  void router.push({
    path: '/pay-up-antd',
    query: buildMobileQuery(),
  })
}

function formatHistoryAmount(operationType: string, amount: number) {
  const normalized = operationType.toUpperCase()
  if (normalized === 'RECHARGE') return `+${formatCurrency(amount)}`
  if (normalized === 'DEDUCT') return `-${formatCurrency(amount)}`
  return formatCurrency(amount)
}

function toggleDetail(historyId: number) {
  expandedHistoryId.value = expandedHistoryId.value === historyId ? null : historyId
}

function resolveEventMeta(operationType: string, feeTypeText: string, payTypeText: string) {
  const normalized = operationType.toUpperCase()
  if (normalized === 'RECHARGE') {
    return {
      eventType: 'recharge' as const,
      eventLabel: '充值订单',
      tagColor: 'green',
      amountClass: 'record-card__amount--positive',
      title: `${feeTypeText}充值`,
      description: `通过${payTypeText}完成支付，宿舍余额和低碳积分会在支付成功后同步更新。`,
    }
  }
  if (normalized === 'REFRESH') {
    return {
      eventType: 'refresh' as const,
      eventLabel: '刷新结算',
      tagColor: 'blue',
      amountClass: 'record-card__amount--neutral',
      title: `${feeTypeText}刷新`,
      description: '系统按当前宿舍消耗同步余额与积分状态，本次记录金额通常为 0 元。',
    }
  }
  return {
    eventType: 'deduct' as const,
    eventLabel: '周期扣费',
    tagColor: 'orange',
    amountClass: 'record-card__amount--negative',
    title: `${feeTypeText}扣费`,
    description: '这是系统根据一段时间内的真实消耗自动生成的扣费记录。',
  }
}

async function loadUtilityInfo() {
  if (!resolvedStuNum.value && !resolvedDormId.value) {
    throw new Error('未检测到学生学号或宿舍信息，请重新进入页面。')
  }

  const { data } = await fetchWaterElectricity({
    stuNum: resolvedStuNum.value || undefined,
    dormId: resolvedDormId.value || undefined,
  })
  const result = requireApiData(data, '获取宿舍水电信息失败')
  utilityInfo.value = result

  if (!publicAccessMode.value) {
    studentTokenStore.updateCarbonScore(result.personalCarbonScore)
  }

  return result
}

async function loadHistory() {
  loading.value = true
  errorMessage.value = ''

  try {
    const info = utilityInfo.value || (await loadUtilityInfo())
    const historyDormId = info.dormId ?? resolvedDormId.value
    if (!historyDormId) {
      throw new Error('未识别到宿舍信息，无法加载历史订单。')
    }

    const { data } = await fetchFeeHistory({
      dormId: historyDormId,
      pageNum: 1,
      pageSize: 30,
    })

    const pageData = requireApiData(data, '获取历史订单失败')
    historyList.value = (pageData.records || []).map((item) => {
      const operationType = String(item.operationType || '').toUpperCase()
      const feeTypeText = formatFeeTypeText(item.feeType)
      const payTypeText = formatPayTypeText(item.payType)
      const eventMeta = resolveEventMeta(operationType, feeTypeText, payTypeText)

      return {
        historyId: item.id,
        dormNo: currentDormLabel.value,
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
    errorMessage.value = resolveErrorMessage(error, '获取历史订单失败，请稍后重试')
    message.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadHistory()
})
</script>

<style scoped>
.history-mobile-page {
  min-height: 100vh;
  padding: 16px 12px 24px;
  background:
    radial-gradient(circle at 12% 2%, rgba(170, 231, 205, 0.4), transparent 22%),
    radial-gradient(circle at 88% 0%, rgba(138, 190, 255, 0.22), transparent 24%),
    linear-gradient(180deg, #effbf3 0%, #fafffc 48%, #edf8f1 100%);
}

.history-mobile-shell {
  display: flex;
  justify-content: center;
}

.history-mobile-phone {
  width: min(100%, 430px);
  display: grid;
  gap: 12px;
}

.history-hero {
  position: relative;
  overflow: hidden;
  padding: 16px 14px 14px;
  border-radius: 26px;
  background:
    radial-gradient(circle at 50% 0%, rgba(255, 255, 255, 0.96), transparent 54%),
    linear-gradient(160deg, rgba(236, 252, 242, 0.98), rgba(222, 246, 233, 0.94));
  border: 1px solid rgba(189, 230, 204, 0.9);
  box-shadow: 0 24px 50px rgba(40, 104, 70, 0.14);
}

.history-hero__glow {
  position: absolute;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(65, 181, 117, 0.14);
}

.history-hero__glow--left {
  top: -42px;
  left: -60px;
}

.history-hero__glow--right {
  top: -58px;
  right: -30px;
}

.history-hero__top {
  position: relative;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}

.hero-icon-button,
.hero-text-button,
.filter-chip {
  border: 0;
  cursor: pointer;
}

.hero-icon-button {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.8);
  color: #2a7a53;
  box-shadow: inset 0 0 0 1px rgba(167, 222, 184, 0.8);
  justify-self: start;
}

.hero-text-button {
  min-width: 76px;
  padding: 8px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #246645;
  font-size: 11px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(169, 221, 186, 0.9);
  justify-self: end;
  white-space: nowrap;
}

.history-hero__copy {
  display: grid;
  gap: 3px;
  justify-items: center;
  text-align: center;
  min-width: 0;
}

.history-hero__eyebrow {
  margin: 0;
  color: #4c8d66;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.history-hero__copy h1 {
  margin: 0;
  color: #124b31;
  font-size: 24px;
  line-height: 1.1;
  font-weight: 800;
  white-space: nowrap;
}

.history-hero__copy span {
  color: #265f3f;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.summary-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.summary-card {
  padding: 11px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(216, 236, 222, 0.94);
  box-shadow: 0 14px 28px rgba(63, 118, 86, 0.08);
  display: grid;
  gap: 4px;
}

.summary-card span {
  color: #5e7f6d;
  font-size: 10px;
  font-weight: 700;
}

.summary-card strong {
  color: #173f2d;
  font-size: 13px;
  line-height: 1.35;
  word-break: break-word;
}

.history-error {
  border-radius: 18px;
}

.filter-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.filter-strip::-webkit-scrollbar {
  display: none;
}

.filter-chip {
  flex: 0 0 auto;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #557767;
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(214, 235, 220, 0.95);
}

.filter-chip--active {
  background: linear-gradient(135deg, #2f9c66, #4fbf86);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(54, 151, 103, 0.24);
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-card {
  padding: 14px 13px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(214, 235, 220, 0.94);
  box-shadow: 0 18px 40px rgba(45, 102, 68, 0.1);
  display: grid;
  gap: 10px;
}

.record-card__top {
  display: grid;
  gap: 6px;
}

.record-card__title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.record-card__title strong {
  color: #173f2d;
  font-size: 17px;
  font-weight: 800;
}

.record-card__type-inline {
  padding: 2px 8px;
  border-radius: 999px;
  background: #edf8f1;
  color: #4f7f61;
  font-size: 10px;
  font-weight: 700;
}

.record-card__time {
  color: #708c7b;
  font-size: 11px;
  font-weight: 600;
}

.record-card__amount {
  font-size: 28px;
  line-height: 1.1;
  font-weight: 800;
}

.detail-toggle {
  justify-self: start;
  padding: 0;
  border: 0;
  background: transparent;
  color: #2f9c66;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.record-card__detail {
  display: grid;
  gap: 10px;
  padding-top: 2px;
}

.record-card__amount--positive {
  color: #208851;
}

.record-card__amount--negative {
  color: #c46d27;
}

.record-card__amount--neutral {
  color: #2d81bf;
}

.record-card__desc {
  margin: 0;
  color: #648170;
  font-size: 12px;
  line-height: 1.65;
}

.record-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.meta-pill {
  padding: 10px 12px;
  border-radius: 14px;
  background: #f8fcf9;
  border: 1px solid rgba(220, 238, 225, 0.95);
  display: grid;
  gap: 4px;
}

.meta-pill span {
  color: #749081;
  font-size: 10px;
  font-weight: 700;
}

.meta-pill strong {
  color: #173f2d;
  font-size: 12px;
  line-height: 1.5;
}

.history-empty {
  padding: 28px 0;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(214, 235, 220, 0.94);
}

@media (max-width: 380px) {
  .summary-grid,
  .record-card__meta {
    grid-template-columns: 1fr;
  }

  .history-hero__copy h1 {
    font-size: 22px;
  }
}
</style>
