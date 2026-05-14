<template>
  <div class="reward-mall-page">
    <div class="reward-mall-shell">
      <div class="reward-mall-phone">
        <ASpin :spinning="loading && Boolean(rewardCenter)">
          <header class="mall-hero">
            <div class="mall-hero__glow"></div>

            <div class="mall-hero__top">
              <div class="mall-hero__copy">
                <h1>积分兑换商城</h1>
              </div>

              <div class="mall-hero__search">
                <AInput v-model:value="keyword" size="large" allow-clear placeholder="搜索奖励名称">
                  <template #prefix>
                    <SearchOutlined />
                  </template>
                </AInput>
              </div>
            </div>
          </header>

          <AAlert
            v-if="errorMessage"
            class="mall-error"
            type="error"
            show-icon
            :message="errorMessage"
          />

          <section v-else-if="loading && !rewardCenter" class="mall-loading-state" aria-live="polite">
            <div class="mall-loading-state__header">
              <span class="mall-loading-state__spinner" aria-hidden="true"></span>
              <div>
                <strong>正在加载积分商城</strong>
                <p>奖励列表和兑换记录马上就绪</p>
              </div>
            </div>

            <div class="view-strip view-strip--skeleton">
              <span class="skeleton-line skeleton-line--tab"></span>
              <span class="skeleton-line skeleton-line--tab"></span>
            </div>

            <section class="summary-row">
              <article class="summary-pill summary-pill--skeleton">
                <span class="skeleton-line skeleton-line--short"></span>
                <strong class="skeleton-line skeleton-line--value"></strong>
              </article>
            </section>

            <section class="card-list">
              <article v-for="index in 3" :key="index" class="reward-card reward-card--skeleton">
                <div class="reward-card__media reward-card__media--skeleton">
                  <span class="mall-loading-state__spinner mall-loading-state__spinner--small" aria-hidden="true"></span>
                </div>

                <div class="reward-card__content">
                  <span class="skeleton-line skeleton-line--title"></span>
                  <span class="skeleton-line skeleton-line--desc"></span>
                  <div class="reward-card__meta">
                    <span class="skeleton-line skeleton-line--chip"></span>
                    <span class="skeleton-line skeleton-line--chip skeleton-line--chip-wide"></span>
                  </div>
                  <div class="reward-card__bottom">
                    <span class="skeleton-line skeleton-line--points"></span>
                    <span class="skeleton-line skeleton-line--button"></span>
                  </div>
                </div>
              </article>
            </section>
          </section>

          <template v-else-if="rewardCenter">
            <section class="mall-toolbar">
              <article class="summary-pill summary-pill--balance">
                <span>当前积分</span>
                <strong>{{ currentPoints }}</strong>
                <small>最近可兑换：{{ nearestRewardName }}</small>
              </article>

              <section class="view-strip">
                <button
                  v-for="view in viewOptions"
                  :key="view.value"
                  type="button"
                  class="view-chip"
                  :class="{ 'view-chip--active': activeView === view.value }"
                  @click="activeView = view.value"
                >
                  {{ view.label }}
                </button>
              </section>
            </section>

            <section v-if="activeView === 'rewards'" class="card-list">
              <article v-for="item in pagedRewards" :key="item.rewardId" class="reward-card">
                <div
                  class="reward-card__media"
                  :class="{ 'reward-card__media--loading': !isRewardImageReady(item.rewardId) }"
                >
                  <span
                    v-if="!isRewardImageReady(item.rewardId)"
                    class="reward-card__image-spinner"
                    aria-hidden="true"
                  ></span>
                  <img
                    :src="safeImageUrl(item.imageUrl)"
                    :alt="item.rewardName"
                    class="reward-card__image"
                    :class="{ 'reward-card__image--ready': isRewardImageReady(item.rewardId) }"
                    loading="lazy"
                    @load="markRewardImageReady(item.rewardId)"
                    @error="handleImageError($event, item.rewardId)"
                  />
                </div>

                <div class="reward-card__content">
                  <h3 class="reward-card__title">{{ item.rewardName }}</h3>
                  <p class="reward-card__desc">{{ item.rewardDesc || '暂无奖励说明' }}</p>

                  <div class="reward-card__meta">
                    <span class="reward-card__meta-chip">库存 {{ Math.max(item.stock, 0) }}</span>
                    <span
                      class="reward-card__meta-chip"
                      :class="{ 'reward-card__meta-chip--warn': !item.canExchange }"
                    >
                      {{ item.exchangeTip }}
                    </span>
                  </div>

                  <div class="reward-card__bottom">
                    <span class="reward-card__points">{{ item.pointsCost }} 积分</span>

                    <AButton
                      type="primary"
                      size="large"
                      class="exchange-button"
                      :disabled="item.disabled"
                      :loading="submittingRewardId === item.rewardId"
                      @click="openExchangeModal(item)"
                    >
                      <template #icon>
                        <ShoppingCartOutlined />
                      </template>
                      {{ item.actionText }}
                    </AButton>
                  </div>
                </div>
              </article>

              <AEmpty v-if="!displayRewards.length" class="empty-block" description="没有匹配到可展示的奖励" />

              <div v-else class="list-pagination">
                <div class="pagination-text">
                  <strong>{{ rewardPage }} / {{ rewardTotalPages }}</strong>
                  <span>当前 {{ rewardPageRangeText }} / 共 {{ displayRewards.length }} 项</span>
                </div>

                <button
                  type="button"
                  class="pagination-button"
                  :disabled="rewardPage <= 1"
                  @click="changeRewardPage(-1)"
                >
                  上一页
                </button>

                <button
                  type="button"
                  class="pagination-button"
                  :disabled="rewardPage >= rewardTotalPages"
                  @click="changeRewardPage(1)"
                >
                  下一页
                </button>
              </div>
            </section>

            <section v-else class="record-list">
              <article v-for="record in pagedRecords" :key="record.recordId" class="record-card">
                <div class="record-card__head">
                  <div>
                    <h3>{{ record.rewardName }}</h3>
                    <p>{{ record.remark || '个人积分兑换成功，等待发放。' }}</p>
                  </div>
                  <ATag color="green">-{{ record.exchangePoints }} 积分</ATag>
                </div>

                <div class="record-card__footer">
                  <span>
                    <ClockCircleOutlined />
                    {{ formatDateTime(record.exchangeTime) }}
                  </span>
                </div>
              </article>

              <AEmpty v-if="!displayRecords.length" class="empty-block" description="暂无兑换记录" />

              <div v-else class="list-pagination">
                <div class="pagination-text">
                  <strong>{{ recordPage }} / {{ recordTotalPages }}</strong>
                  <span>当前 {{ recordPageRangeText }} / 共 {{ displayRecords.length }} 项</span>
                </div>

                <button
                  type="button"
                  class="pagination-button"
                  :disabled="recordPage <= 1"
                  @click="changeRecordPage(-1)"
                >
                  上一页
                </button>

                <button
                  type="button"
                  class="pagination-button"
                  :disabled="recordPage >= recordTotalPages"
                  @click="changeRecordPage(1)"
                >
                  下一页
                </button>
              </div>
            </section>
          </template>
        </ASpin>
      </div>
    </div>

    <Teleport to="body">
      <transition name="sheet-fade">
        <div v-if="exchangeModalOpen" class="sheet-mask" @click.self="closeExchangeModal">
          <transition name="sheet-rise">
            <div v-if="selectedReward" class="exchange-sheet">
              <div class="exchange-sheet__handle"></div>

              <div class="exchange-sheet__header">
                <div>
                  <p class="exchange-sheet__eyebrow">确认兑换</p>
                  <h3>{{ selectedReward.rewardName }}</h3>
                </div>
                <button type="button" class="exchange-sheet__close" @click="closeExchangeModal">关闭</button>
              </div>

              <p class="exchange-sheet__desc">{{ selectedReward.rewardDesc || '请再次确认当前奖励信息。' }}</p>

              <div class="exchange-sheet__brief">
                <div class="exchange-sheet__brief-item">
                  <span>本次扣除</span>
                  <strong>{{ selectedReward.pointsCost }} 积分</strong>
                </div>
                <div class="exchange-sheet__brief-item">
                  <span>兑换后剩余</span>
                  <strong>{{ exchangeRemainingPoints }}</strong>
                </div>
                <div class="exchange-sheet__brief-item">
                  <span>剩余库存</span>
                  <strong>{{ Math.max(selectedReward.stock, 0) }} 件</strong>
                </div>
              </div>

              <p class="exchange-sheet__summary">确认后会直接提交兑换申请，默认单次兑换 1 件。</p>

              <div class="exchange-sheet__actions">
                <button type="button" class="sheet-button sheet-button--ghost" @click="closeExchangeModal">取消</button>
                <button
                  type="button"
                  class="sheet-button sheet-button--primary"
                  :disabled="exchangeConfirmLoading"
                  @click="confirmExchange"
                >
                  {{ exchangeConfirmLoading ? '提交中...' : '立即兑换' }}
                </button>
              </div>
            </div>
          </transition>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch, type ComputedRef } from 'vue'
import { useRoute } from 'vue-router'
import {
  Alert as AAlert,
  Button as AButton,
  Empty as AEmpty,
  Input as AInput,
  Spin as ASpin,
  Tag as ATag,
  message,
} from 'ant-design-vue'
import {
  ClockCircleOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons-vue'
import 'ant-design-vue/dist/reset.css'
import { storeToRefs } from 'pinia'
import { exchangeReward, fetchRewardCenter, type RewardCenter, type RewardItem } from '@/api/modules/reward'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
import { formatDateTime } from '@/utils/formatters'
import { useStudentTokenStore } from '@/stores/student-token'

type RewardView = 'rewards' | 'records'

interface DisplayReward extends RewardItem {
  actionText: string
  disabled: boolean
}

const FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240"><rect width="240" height="240" rx="48" fill="%23eef7f1"/><circle cx="120" cy="92" r="34" fill="%23dcefe2"/><rect x="68" y="136" width="104" height="20" rx="10" fill="%2322c55e" opacity="0.18"/><rect x="84" y="88" width="72" height="72" rx="18" fill="%23ffffff"/><path d="M102 136h36" stroke="%2322c55e" stroke-width="12" stroke-linecap="round"/><path d="M120 100v28" stroke="%2322c55e" stroke-width="12" stroke-linecap="round"/></svg>'
const MISSING_STUDENT_MESSAGE = '未检测到学生学号，请重新登录或在地址中传入 stuNum'

const route = useRoute()
const studentTokenStore = useStudentTokenStore()
const { stuNum } = storeToRefs(studentTokenStore)

const viewOptions = [
  { label: '奖励列表', value: 'rewards' as const },
  { label: '兑换记录', value: 'records' as const },
]

const keyword = ref('')
const activeView = ref<RewardView>('rewards')
const viewportWidth = ref(typeof window === 'undefined' ? 393 : window.innerWidth)
const viewportHeight = ref(typeof window === 'undefined' ? 852 : window.innerHeight)
const loading = ref(false)
const errorMessage = ref('')
const rewardCenter = ref<RewardCenter | null>(null)
const submittingRewardId = ref<number | null>(null)
const selectedReward = ref<DisplayReward | null>(null)
const exchangeModalOpen = ref(false)
const exchangeConfirmLoading = ref(false)
const readyRewardImageIds = ref(new Set<number>())

const routeStuNum = computed(() => {
  const value = route.query.stuNum
  return typeof value === 'string' ? value.trim() : ''
})
const resolvedStuNum = computed(() => routeStuNum.value || stuNum.value || '')
const publicAccessMode = computed(() => Boolean(routeStuNum.value) && routeStuNum.value !== stuNum.value)
const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
const rewardItems = computed(() => rewardCenter.value?.rewardItems || [])
const exchangeRecords = computed(() => rewardCenter.value?.exchangeRecords || [])
const nearestRewardName = computed(() => rewardCenter.value?.dormScoreSummary.nearestRewardName || '暂无')
const currentPoints = computed(() => rewardCenter.value?.currentPoints ?? 0)
const exchangeRemainingPoints = computed(() =>
  Math.max(currentPoints.value - (selectedReward.value?.pointsCost ?? 0), 0),
)
const isCompactViewport = computed(() => viewportWidth.value <= 640 || viewportHeight.value <= 820)
const isUltraCompactViewport = computed(() => viewportWidth.value <= 390 || viewportHeight.value <= 740)
const rewardPageSize = computed(() => (isUltraCompactViewport.value ? 3 : isCompactViewport.value ? 4 : 5))
const recordPageSize = computed(() => (isUltraCompactViewport.value ? 4 : isCompactViewport.value ? 5 : 6))

const displayRewards = computed<DisplayReward[]>(() =>
  rewardItems.value.filter((item) => matchesKeyword(item.rewardName, item.rewardDesc)).map(toDisplayReward),
)

const displayRecords = computed(() =>
  exchangeRecords.value.filter((record) => matchesKeyword(record.rewardName, record.remark || '')),
)

const {
  page: rewardPage,
  totalPages: rewardTotalPages,
  pagedItems: pagedRewards,
  rangeText: rewardPageRangeText,
  resetPage: resetRewardPage,
  changePage: changeRewardPage,
} = createPager(displayRewards, rewardPageSize)

const {
  page: recordPage,
  totalPages: recordTotalPages,
  pagedItems: pagedRecords,
  rangeText: recordPageRangeText,
  resetPage: resetRecordPage,
  changePage: changeRecordPage,
} = createPager(displayRecords, recordPageSize)

function matchesKeyword(...texts: string[]) {
  if (!normalizedKeyword.value) {
    return true
  }

  return texts.some((text) => text.toLowerCase().includes(normalizedKeyword.value))
}

function toDisplayReward(item: RewardItem): DisplayReward {
  return {
    ...item,
    actionText: item.canExchange ? '兑换' : item.stock <= 0 ? '已兑完' : '积分不足',
    disabled: !item.canExchange || submittingRewardId.value === item.rewardId,
  }
}

function getTotalPages(total: number, pageSize: number) {
  return Math.max(1, Math.ceil(total / pageSize))
}

function clampPage(page: number, total: number, pageSize: number) {
  return Math.min(Math.max(1, page), getTotalPages(total, pageSize))
}

function slicePageItems<T>(items: T[], page: number, pageSize: number) {
  const currentPage = clampPage(page, items.length, pageSize)
  const start = (currentPage - 1) * pageSize
  return items.slice(start, start + pageSize)
}

function getPageRangeText(total: number, page: number, pageSize: number) {
  if (!total) {
    return '0-0'
  }

  const currentPage = clampPage(page, total, pageSize)
  const start = (currentPage - 1) * pageSize + 1
  const end = Math.min(currentPage * pageSize, total)
  return `${start}-${end}`
}

function createPager<T>(items: ComputedRef<T[]>, pageSize: ComputedRef<number>) {
  const page = ref(1)
  const totalPages = computed(() => getTotalPages(items.value.length, pageSize.value))
  const pagedItems = computed(() => slicePageItems(items.value, page.value, pageSize.value))
  const rangeText = computed(() => getPageRangeText(items.value.length, page.value, pageSize.value))

  function resetPage() {
    page.value = 1
  }

  function changePage(direction: number) {
    page.value = clampPage(page.value + direction, items.value.length, pageSize.value)
  }

  watch(items, (nextItems) => {
    page.value = clampPage(page.value, nextItems.length, pageSize.value)
  })

  watch(pageSize, () => {
    page.value = clampPage(page.value, items.value.length, pageSize.value)
  })

  return {
    page,
    totalPages,
    pagedItems,
    rangeText,
    resetPage,
    changePage,
  }
}

function updateViewportSize() {
  viewportWidth.value = window.innerWidth
  viewportHeight.value = window.innerHeight
}

async function loadRewardCenter() {
  if (!resolvedStuNum.value) {
    errorMessage.value = MISSING_STUDENT_MESSAGE
    return null
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const { data } = await fetchRewardCenter(resolvedStuNum.value)
    const result = requireApiData(data, '获取奖励中心失败')
    rewardCenter.value = result

    if (!publicAccessMode.value) {
      studentTokenStore.updateCarbonScore(result.currentPoints)
    }

    return result
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, '获取奖励中心失败，请稍后重试')
    return null
  } finally {
    loading.value = false
  }
}

async function exchangeRewardById(rewardId: number) {
  if (!resolvedStuNum.value) {
    throw new Error(MISSING_STUDENT_MESSAGE)
  }

  submittingRewardId.value = rewardId

  try {
    const { data } = await exchangeReward(resolvedStuNum.value, rewardId)
    return requireApiData(data, '兑换失败')
  } finally {
    submittingRewardId.value = null
  }
}

function safeImageUrl(imageUrl?: string) {
  return imageUrl && imageUrl.trim() ? imageUrl : FALLBACK_IMAGE
}

function isRewardImageReady(rewardId: number) {
  return readyRewardImageIds.value.has(rewardId)
}

function markRewardImageReady(rewardId: number) {
  if (readyRewardImageIds.value.has(rewardId)) {
    return
  }

  readyRewardImageIds.value = new Set([...readyRewardImageIds.value, rewardId])
}

function handleImageError(event: Event, rewardId: number) {
  const target = event.target as HTMLImageElement | null

  if (target && target.src !== FALLBACK_IMAGE) {
    target.src = FALLBACK_IMAGE
    return
  }

  markRewardImageReady(rewardId)
}

function openExchangeModal(reward: DisplayReward) {
  if (!reward.canExchange || submittingRewardId.value) {
    return
  }

  if (typeof window !== 'undefined' && window.navigator?.vibrate) {
    window.navigator.vibrate(10)
  }

  selectedReward.value = reward
  exchangeModalOpen.value = true
}

function closeExchangeModal() {
  exchangeModalOpen.value = false
  selectedReward.value = null
}

async function confirmExchange() {
  if (!selectedReward.value || !resolvedStuNum.value) {
    return
  }

  exchangeConfirmLoading.value = true

  try {
    await exchangeRewardById(selectedReward.value.rewardId)
    message.success('兑换成功')
    closeExchangeModal()
    await loadRewardCenter()
  } catch (error) {
    message.error(resolveErrorMessage(error, '兑换失败，请稍后重试'))
  } finally {
    exchangeConfirmLoading.value = false
  }
}

watch(exchangeModalOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

watch(keyword, () => {
  resetRewardPage()
  resetRecordPage()
})

watch(activeView, (view) => {
  if (view === 'rewards') {
    resetRewardPage()
    return
  }

  resetRecordPage()
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('resize', updateViewportSize)
})

onMounted(() => {
  updateViewportSize()
  window.addEventListener('resize', updateViewportSize)
  loadRewardCenter()
})
</script>

<style scoped>
.reward-mall-page {
  --mall-phone-max: 860px;
  --mall-content-max: 560px;
  --mall-content-inset: 20px;
  --mall-font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  --mall-ink-strong: #18372a;
  --mall-ink: #2f5140;
  --mall-ink-soft: #62806f;
  --mall-accent: #22b357;
  --mall-accent-strong: #158f46;
  min-height: 100dvh;
  padding: 12px 12px calc(12px + env(safe-area-inset-bottom, 0px));
  font-family: var(--mall-font-family);
  background:
    linear-gradient(180deg, rgba(248, 255, 249, 0.12) 0%, rgba(241, 252, 244, 0.28) 100%),
    url('/images/reward-exchange-mobile-bg.jpg') center top / cover no-repeat;
}

.reward-mall-shell {
  display: flex;
  justify-content: center;
  width: 100%;
  min-height: calc(100dvh - 24px);
}

.reward-mall-phone {
  width: min(100%, var(--mall-phone-max));
  min-height: calc(100dvh - 24px);
  border-radius: 32px;
  overflow: hidden;
  background: rgba(250, 255, 250, 0.14);
  box-shadow: 0 22px 48px rgba(53, 109, 73, 0.14);
}

.mall-hero {
  position: relative;
  padding: 18px var(--mall-content-inset) 12px;
  background: linear-gradient(180deg, rgba(16, 66, 43, 0.34), rgba(16, 66, 43, 0.08));
}

.mall-hero__glow {
  position: absolute;
  top: -40px;
  right: -10px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(104, 255, 161, 0.32), transparent 68%);
  pointer-events: none;
}

.mall-hero__top {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
}

.mall-hero__copy h1 {
  margin: 0;
  color: #ffffff;
  font-size: 26px;
  line-height: 1.1;
  font-weight: 900;
}

.mall-hero__copy p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
  line-height: 1.5;
}

.mall-hero__search {
  width: 100%;
}

.mall-toolbar,
.summary-row,
.card-list,
.record-list,
.list-pagination,
.mall-loading-state,
.mall-error,
.empty-block {
  width: min(calc(100% - var(--mall-content-inset) * 2), var(--mall-content-max));
  margin-left: auto;
  margin-right: auto;
}

.mall-toolbar {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.view-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
  border: 1px solid rgba(70, 123, 92, 0.12);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(252, 254, 252, 0.96), rgba(241, 247, 243, 0.92));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 18px rgba(31, 74, 52, 0.08);
}

.view-chip {
  min-height: 44px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: #edf7ef;
  color: var(--mall-ink-soft);
  font: inherit;
  font-size: 14px;
  font-weight: 700;
}

.view-chip--active {
  background: linear-gradient(135deg, #20b457 0%, #3acb72 100%);
  color: #ffffff;
  box-shadow: 0 10px 18px rgba(45, 189, 97, 0.18);
}

.mall-loading-state {
  margin-top: 16px;
}

.mall-loading-state__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(218, 239, 224, 0.92);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12px 26px rgba(44, 114, 67, 0.08);
}

.mall-loading-state__header strong {
  display: block;
  color: var(--mall-ink-strong);
  font-size: 15px;
  line-height: 1.3;
  font-weight: 900;
}

.mall-loading-state__header p {
  margin: 4px 0 0;
  color: var(--mall-ink-soft);
  font-size: 12px;
  line-height: 1.5;
}

.mall-loading-state__spinner,
.reward-card__image-spinner {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 3px solid rgba(34, 179, 87, 0.16);
  border-top-color: var(--mall-accent);
  animation: mallSpin 0.8s linear infinite;
  flex: 0 0 auto;
}

.mall-loading-state__spinner--small {
  width: 24px;
  height: 24px;
  border-width: 2px;
}

.view-strip--skeleton {
  margin-top: 10px;
  box-shadow: none;
}

.summary-row {
  margin-top: 10px;
}

.summary-pill {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
  min-height: 82px;
  padding: 14px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 24px rgba(36, 69, 54, 0.06);
  backdrop-filter: blur(10px);
}

.summary-pill span {
  color: #31a34e;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
}

.summary-pill strong {
  color: #31a34e;
  font-size: clamp(18px, 5vw, 24px);
  line-height: 1.1;
  font-weight: 900;
}

.summary-pill small {
  color: var(--mall-ink-soft);
  font-size: 12px;
  line-height: 1.5;
}

.summary-pill--skeleton,
.reward-card--skeleton {
  pointer-events: none;
}

.skeleton-line {
  display: block;
  overflow: hidden;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(225, 243, 231, 0.88), rgba(248, 255, 250, 0.96), rgba(225, 243, 231, 0.88));
  background-size: 220% 100%;
  animation: mallSkeleton 1.2s ease-in-out infinite;
}

.skeleton-line--tab {
  min-height: 42px;
}

.skeleton-line--short {
  width: 64px;
  height: 14px;
}

.skeleton-line--value {
  width: 48px;
  height: 20px;
}

.skeleton-line--title {
  width: 72%;
  height: 18px;
}

.skeleton-line--desc {
  width: 92%;
  height: 14px;
  margin-top: 10px;
}

.skeleton-line--chip {
  width: 68px;
  height: 28px;
}

.skeleton-line--chip-wide {
  width: 112px;
}

.skeleton-line--points {
  width: 68px;
  height: 18px;
}

.skeleton-line--button {
  width: 112px;
  height: 42px;
}

.card-list,
.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.reward-card,
.record-card {
  padding: 16px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 16px 34px rgba(33, 83, 52, 0.06);
  border: 1px solid rgba(248, 255, 249, 0.78);
  backdrop-filter: blur(12px);
}

.reward-card {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 14px;
  align-items: flex-start;
}

.reward-card__media {
  position: relative;
  display: grid;
  place-items: center;
  width: 76px;
  height: 76px;
  border-radius: 20px;
  overflow: hidden;
  background: #eef7f1;
}

.reward-card__media--loading,
.reward-card__media--skeleton {
  background:
    radial-gradient(circle at 35% 30%, rgba(255, 255, 255, 0.92), transparent 34%),
    linear-gradient(135deg, #edf8f1, #dff2e7);
}

.reward-card__image-spinner {
  position: absolute;
  inset: 0;
  margin: auto;
  z-index: 1;
}

.reward-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.22s ease;
}

.reward-card__image--ready {
  opacity: 1;
}

.reward-card__content {
  min-width: 0;
}

.reward-card__title {
  margin: 0;
  color: var(--mall-ink-strong);
  font-size: 18px;
  line-height: 1.28;
  font-weight: 900;
}

.reward-card__desc {
  margin: 8px 0 0;
  color: var(--mall-ink-soft);
  font-size: 13px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.reward-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.reward-card__meta-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f1fbf4;
  color: var(--mall-accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.reward-card__meta-chip--warn {
  background: #fff2ee;
  color: #d76b4d;
}

.reward-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.reward-card__points {
  color: #31a34e;
  font-size: 17px;
  line-height: 1.3;
  font-weight: 900;
  white-space: nowrap;
}

.exchange-button {
  min-width: 108px;
  height: 42px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #26c15f 0%, #19a84e 100%);
  box-shadow: 0 10px 18px rgba(32, 182, 87, 0.16);
  font-weight: 800;
}

.record-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.record-card__head h3 {
  margin: 0;
  color: var(--mall-ink-strong);
  font-size: 18px;
  line-height: 1.35;
  font-weight: 900;
}

.record-card__head p {
  margin: 8px 0 0;
  color: var(--mall-ink-soft);
  font-size: 14px;
  line-height: 1.6;
}

.record-card__footer {
  margin-top: 14px;
  color: var(--mall-ink-soft);
  font-size: 13px;
}

.record-card__footer span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.list-pagination {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 4px;
}

.pagination-text {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  text-align: center;
}

.pagination-text strong {
  color: var(--mall-accent-strong);
  font-size: 14px;
  line-height: 1.3;
  font-weight: 900;
}

.pagination-text span {
  color: var(--mall-ink-soft);
  font-size: 12px;
  line-height: 1.45;
}

.pagination-button {
  min-height: 40px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--mall-accent-strong);
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 8px 16px rgba(72, 120, 90, 0.08);
}

.pagination-button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
  box-shadow: none;
}

.mall-error {
  margin-top: 18px;
  border-radius: 18px;
  overflow: hidden;
}

.empty-block {
  padding: 36px 0 10px;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 18px 12px calc(18px + env(safe-area-inset-bottom, 0px));
}

.exchange-sheet {
  width: min(100%, 440px);
  border-radius: 28px 28px 20px 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
  backdrop-filter: blur(14px);
  padding: 12px 16px calc(16px + env(safe-area-inset-bottom, 0px));
}

.exchange-sheet__handle {
  width: 56px;
  height: 6px;
  border-radius: 999px;
  margin: 0 auto 16px;
  background: #dbe7df;
}

.exchange-sheet__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.exchange-sheet__eyebrow {
  margin: 0 0 6px;
  color: var(--mall-ink-soft);
  font-size: 12px;
  font-weight: 700;
}

.exchange-sheet__header h3 {
  margin: 0;
  color: var(--mall-ink-strong);
  font-size: 22px;
  line-height: 1.25;
  font-weight: 900;
}

.exchange-sheet__close {
  min-width: 56px;
  min-height: 34px;
  border: none;
  border-radius: 999px;
  background: #eef9f1;
  color: var(--mall-ink);
  font: inherit;
  font-size: 13px;
  font-weight: 700;
}

.exchange-sheet__desc {
  margin: 12px 0 0;
  color: var(--mall-ink-soft);
  font-size: 14px;
  line-height: 1.7;
}

.exchange-sheet__brief {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.exchange-sheet__brief-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 12px;
  border-radius: 18px;
  background: #f4fcf6;
}

.exchange-sheet__brief-item span {
  color: var(--mall-ink-soft);
  font-size: 12px;
  font-weight: 700;
}

.exchange-sheet__brief-item strong {
  color: var(--mall-ink-strong);
  font-size: 18px;
  font-weight: 900;
  text-align: right;
}

.exchange-sheet__summary {
  margin: 16px 0 0;
  color: var(--mall-ink);
  font-size: 14px;
  line-height: 1.7;
}

.exchange-sheet__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.sheet-button {
  min-height: 48px;
  border: none;
  border-radius: 16px;
  font: inherit;
  font-size: 15px;
  font-weight: 800;
}

.sheet-button--ghost {
  background: #eef9f2;
  color: var(--mall-ink);
}

.sheet-button--primary {
  background: linear-gradient(135deg, #21ba59 0%, #35cb70 100%);
  color: #ffffff;
}

.sheet-button:disabled {
  opacity: 0.72;
}

.sheet-fade-enter-active,
.sheet-fade-leave-active {
  transition: opacity 0.2s ease;
}

.sheet-fade-enter-from,
.sheet-fade-leave-to {
  opacity: 0;
}

.sheet-rise-enter-active,
.sheet-rise-leave-active {
  transition:
    transform 0.22s ease,
    opacity 0.22s ease;
}

.sheet-rise-enter-from,
.sheet-rise-leave-to {
  opacity: 0;
  transform: translateY(18px);
}

:deep(.ant-input-affix-wrapper) {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 42px;
  height: 42px;
  padding: 0 12px;
  border: 1px solid rgba(213, 238, 219, 0.98);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 20px rgba(79, 126, 94, 0.06);
}

:deep(.ant-input) {
  height: 100%;
  line-height: normal;
  font-size: 15px;
  color: var(--mall-ink-strong);
  font-family: var(--mall-font-family);
}

:deep(.ant-input::placeholder) {
  color: #7ea08d;
}

:deep(.ant-input-prefix) {
  display: inline-flex;
  align-items: center;
  color: var(--mall-accent-strong);
}

:deep(.ant-alert) {
  border-radius: 16px;
}

:deep(.ant-tag) {
  margin-inline-end: 0;
  border-radius: 999px;
  font-weight: 700;
  border: none;
  background: #e8f8ec;
  color: var(--mall-accent-strong);
}

:deep(.ant-spin-nested-loading),
:deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
  min-height: calc(100dvh - 24px);
}

:deep(.ant-spin-container) {
  padding-bottom: calc(24px + env(safe-area-inset-bottom, 0px));
}

@keyframes mallSpin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes mallSkeleton {
  0% {
    background-position: 120% 0;
  }

  100% {
    background-position: -120% 0;
  }
}

@media (max-width: 640px) {
  .reward-mall-page {
    --mall-content-inset: 14px;
  }

  .reward-mall-phone {
    border-radius: 0;
    min-height: 100dvh;
  }

  .mall-hero {
    padding-top: 14px;
  }

  .mall-hero__copy h1 {
    font-size: 22px;
  }

  .reward-card,
  .record-card {
    padding: 14px;
    border-radius: 20px;
  }

  .reward-card {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 12px;
  }

  .reward-card__media {
    width: 64px;
    height: 64px;
    border-radius: 16px;
  }

  .reward-card__bottom {
    flex-direction: column;
    align-items: stretch;
  }

  .exchange-button {
    width: 100%;
    min-width: 0;
  }

  .record-card__head {
    flex-direction: column;
    align-items: stretch;
  }

  .exchange-sheet {
    width: 100%;
    border-radius: 24px 24px 16px 16px;
  }
}

@media (max-width: 390px) {
  .mall-hero__copy h1 {
    font-size: 20px;
  }

  .view-chip {
    min-height: 42px;
    font-size: 13px;
  }

  .reward-card {
    grid-template-columns: 56px minmax(0, 1fr);
    gap: 10px;
  }

  .reward-card__media {
    width: 56px;
    height: 56px;
    border-radius: 14px;
  }

  .reward-card__title {
    font-size: 16px;
  }

  .reward-card__points {
    font-size: 15px;
  }

  .exchange-sheet__actions {
    grid-template-columns: 1fr;
  }
}
</style>
