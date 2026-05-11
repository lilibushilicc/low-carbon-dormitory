<template>
  <div class="reward-mall-page">
    <div class="reward-mall-shell">
      <div class="reward-mall-phone">
        <ASpin :spinning="loading">
          <header class="mall-hero">
            <div class="mall-hero__glow"></div>

            <div class="mall-hero__top">
              <div v-if="publicAccessMode" class="public-entry-chip public-entry-chip--inline">
                <span class="public-entry-chip__label">访问学号</span>
                <strong class="public-entry-chip__value">{{ stuNum }}</strong>
              </div>

              <div class="mall-hero__copy">
                <h1>积分兑换商城</h1>
              </div>

              <div class="mall-hero__action-spacer" aria-hidden="true"></div>
            </div>

            <button type="button" class="ghost-action ghost-action--top" @click="goBack">
              <LeftOutlined />
              返回
            </button>

            <div class="mall-hero__search">
              <AInput v-model:value="keyword" size="large" allow-clear placeholder="搜索奖励名称">
                <template #prefix>
                  <SearchOutlined />
                </template>
              </AInput>
            </div>

          </header>

          <AAlert
            v-if="errorMessage"
            class="mall-error"
            type="error"
            show-icon
            :message="errorMessage"
          />

          <template v-else-if="rewardCenter">
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

            <section class="summary-row">
              <article class="summary-pill">
                <span>当前积分</span>
                <strong>{{ currentPoints }}</strong>
              </article>
              <article class="summary-pill">
                <span>推荐奖励</span>
                <strong>{{ nearestRewardName }}</strong>
              </article>
            </section>

            <section v-if="activeView === 'rewards'" class="card-list">
              <article v-for="item in pagedRewards" :key="item.rewardId" class="reward-card">
                <div class="reward-card__media">
                  <img
                    :src="safeImageUrl(item.imageUrl)"
                    :alt="item.rewardName"
                    class="reward-card__image"
                    loading="lazy"
                    @error="handleImageError"
                  />
                </div>

                <div class="reward-card__content">
                  <h3 class="reward-card__title">{{ item.rewardName }}</h3>

                  <div class="reward-card__bottom">
                    <span class="reward-card__points">
                      <GiftOutlined />
                      {{ item.pointsCost }} 积分
                    </span>

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
                  <strong>第 {{ rewardPage }} / {{ rewardTotalPages }} 页</strong>
                  <span>共 {{ displayRewards.length }} 项，当前显示 {{ rewardPageRangeText }}</span>
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
                  <strong>第 {{ recordPage }} / {{ recordTotalPages }} 页</strong>
                  <span>共 {{ displayRecords.length }} 项，当前显示 {{ recordPageRangeText }}</span>
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

              <p class="exchange-sheet__desc">{{ selectedReward.rewardDesc }}</p>

              <div class="exchange-sheet__stats">
                <div class="exchange-sheet__stat">
                  <span>消耗积分</span>
                  <strong>{{ selectedReward.pointsCost }}</strong>
                </div>
                <div class="exchange-sheet__stat">
                  <span>当前积分</span>
                  <strong>{{ currentPoints }}</strong>
                </div>
                <div class="exchange-sheet__stat">
                  <span>剩余数量</span>
                  <strong>{{ Math.max(selectedReward.stock, 0) }} 件</strong>
                </div>
                <div class="exchange-sheet__stat">
                  <span>兑换说明</span>
                  <strong>单次兑换 1 件</strong>
                </div>
              </div>

              <p class="exchange-sheet__summary">确认后将立即提交兑换申请，请核对奖励内容。</p>

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
import { useRouter } from 'vue-router'
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
  GiftOutlined,
  LeftOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons-vue'
import 'ant-design-vue/dist/reset.css'
import type { RewardItem } from '@/api/modules/reward'
import { resolveErrorMessage } from '@/utils/api-response'
import { formatDateTime } from '@/utils/formatters'
import { useRewardCenter } from './use-reward-center'

type RewardView = 'rewards' | 'records'

interface DisplayReward extends RewardItem {
  actionText: string
  disabled: boolean
}

const FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240"><rect width="240" height="240" rx="48" fill="%23eef7f1"/><circle cx="120" cy="92" r="34" fill="%23dcefe2"/><rect x="68" y="136" width="104" height="20" rx="10" fill="%2322c55e" opacity="0.18"/><rect x="84" y="88" width="72" height="72" rx="18" fill="%23ffffff"/><path d="M102 136h36" stroke="%2322c55e" stroke-width="12" stroke-linecap="round"/><path d="M120 100v28" stroke="%2322c55e" stroke-width="12" stroke-linecap="round"/></svg>'

const viewOptions = [
  { label: '奖励列表', value: 'rewards' as const },
  { label: '兑换记录', value: 'records' as const },
]

const router = useRouter()
const keyword = ref('')
const activeView = ref<RewardView>('rewards')
const viewportWidth = ref(typeof window === 'undefined' ? 393 : window.innerWidth)
const viewportHeight = ref(typeof window === 'undefined' ? 852 : window.innerHeight)
const {
  stuNum,
  publicAccessMode,
  loading,
  errorMessage,
  rewardCenter,
  submittingRewardId,
  currentPoints,
  loadRewardCenter,
  exchangeRewardById,
} = useRewardCenter()
const selectedReward = ref<DisplayReward | null>(null)
const exchangeModalOpen = ref(false)
const exchangeConfirmLoading = ref(false)

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
const rewardItems = computed(() => rewardCenter.value?.rewardItems || [])
const exchangeRecords = computed(() => rewardCenter.value?.exchangeRecords || [])
const nearestRewardName = computed(() => rewardCenter.value?.dormScoreSummary.nearestRewardName || '暂无')
const isCompactViewport = computed(() => viewportWidth.value <= 640 || viewportHeight.value <= 820)
const isUltraCompactViewport = computed(() => viewportWidth.value <= 390 || viewportHeight.value <= 740)
const rewardPageSize = computed(() => (isUltraCompactViewport.value ? 2 : isCompactViewport.value ? 3 : 4))
const recordPageSize = computed(() => (isUltraCompactViewport.value ? 3 : isCompactViewport.value ? 4 : 5))

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

function goBack() {
  if (publicAccessMode.value) {
    router.back()
    return
  }
  router.push('/index-student')
}

function safeImageUrl(imageUrl?: string) {
  return imageUrl && imageUrl.trim() ? imageUrl : FALLBACK_IMAGE
}

function handleImageError(event: Event) {
  const target = event.target as HTMLImageElement | null
  if (target && target.src !== FALLBACK_IMAGE) {
    target.src = FALLBACK_IMAGE
  }
}

function openExchangeModal(reward: DisplayReward) {
  if (!reward.canExchange || submittingRewardId.value) {
    return
  }

  if (typeof window !== 'undefined' && window.navigator && window.navigator.vibrate) {
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
  if (!selectedReward.value || !stuNum.value) {
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
  --mall-content-width: 360px;
  --mall-hero-height: 156px;
  height: 100dvh;
  min-height: 100dvh;
  padding: 12px 12px calc(12px + env(safe-area-inset-bottom, 0px));
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(101, 183, 126, 0.16), transparent 28%),
    linear-gradient(180deg, #edf8f1 0%, #f7fbf8 100%);
}

.reward-mall-shell {
  display: flex;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.reward-mall-phone {
  width: min(100%, 404px);
  height: 100%;
  min-height: 0;
  position: relative;
}

.mall-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  height: var(--mall-hero-height);
  padding: 22px 20px 16px;
  border-radius: 28px 28px 18px 18px;
  background: linear-gradient(160deg, #157338 0%, #1ba34b 100%);
  box-shadow: 0 18px 34px rgba(21, 115, 56, 0.2);
  box-sizing: border-box;
}

.mall-hero__glow {
  position: absolute;
  right: -28px;
  top: -42px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.14), transparent 68%);
  pointer-events: none;
}

.mall-hero__top {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, auto) minmax(0, 1fr) 86px;
  align-items: center;
  width: min(100%, var(--mall-content-width));
  margin: 0 auto;
  gap: 10px;
}

.mall-hero__copy {
  min-width: 0;
  display: flex;
  justify-content: center;
  text-align: center;
  box-sizing: border-box;
}

.mall-hero h1 {
  margin: 0;
  color: #ffffff;
  font-size: 24px;
  line-height: 1.12;
  font-weight: 900;
  letter-spacing: -0.02em;
  white-space: nowrap;
}

.mall-hero__action-spacer {
  width: 86px;
  min-width: 86px;
}

.mall-hero__search {
  position: relative;
  z-index: 1;
  width: min(100%, var(--mall-content-width));
  margin: 14px auto 0;
}

.ghost-action {
  min-height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.ghost-action--top {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
}

.public-entry-chip {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: fit-content;
  max-width: min(100%, var(--mall-content-width));
  margin: 8px auto 0;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.82);
  font-size: 10px;
  font-weight: 600;
  text-align: center;
  letter-spacing: 0.01em;
  backdrop-filter: blur(10px);
}

.public-entry-chip--inline {
  margin: 0;
  justify-self: start;
  max-width: 108px;
}

.public-entry-chip::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.public-entry-chip__label {
  white-space: nowrap;
}

.public-entry-chip__value {
  margin: 0;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.view-strip {
  width: min(100%, var(--mall-content-width));
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 14px auto 0;
  padding: 8px;
  border: 1px solid rgba(70, 123, 92, 0.12);
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(252, 254, 252, 0.94), rgba(241, 247, 243, 0.9));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 18px rgba(31, 74, 52, 0.08);
  backdrop-filter: blur(18px);
  scrollbar-width: none;
}

.view-strip::-webkit-scrollbar {
  display: none;
}

.view-chip {
  min-height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: #edf4ef;
  color: #72857a;
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.view-chip--active {
  background: linear-gradient(135deg, #1aae4b 0%, #1fbe57 100%);
  color: #ffffff;
  box-shadow: 0 10px 18px rgba(34, 197, 94, 0.18);
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.summary-pill {
  padding: 14px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 24px rgba(36, 69, 54, 0.08);
  text-align: center;
}

.summary-pill span {
  display: block;
  color: #86a092;
  font-size: 12px;
  font-weight: 700;
}

.summary-pill strong {
  display: block;
  margin-top: 6px;
  color: #224636;
  font-size: 16px;
  line-height: 1.35;
}

.card-list,
.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
  min-height: 0;
}

.reward-card,
.record-card {
  padding: 16px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 34px rgba(33, 83, 52, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.reward-card {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.reward-card__media {
  width: 76px;
  height: 76px;
  border-radius: 20px;
  overflow: hidden;
  background: #eef7f1;
}

.reward-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reward-card__content {
  min-width: 0;
}

.reward-card__title {
  margin: 0;
  color: #234936;
  font-size: 19px;
  line-height: 1.3;
  font-weight: 800;
}

.reward-card__bottom {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reward-card__points {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #14a54f;
  font-size: 18px;
  font-weight: 900;
  white-space: nowrap;
}

.exchange-button {
  min-width: 112px;
  height: 42px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
  box-shadow: none;
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
  color: #234936;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
}

.record-card__head p {
  margin: 8px 0 0;
  color: #7f968a;
  font-size: 14px;
  line-height: 1.6;
}

.record-card__footer {
  margin-top: 14px;
  color: #7d9387;
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
  display: grid;
  gap: 4px;
  text-align: center;
}

.pagination-text strong {
  color: #1a7331;
  font-size: 14px;
  line-height: 1.3;
}

.pagination-text span {
  color: #7a9186;
  font-size: 12px;
  line-height: 1.45;
}

.pagination-button {
  min-height: 40px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: #ffffff;
  color: #1aae4b;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
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
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
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
  color: #7f9489;
  font-size: 12px;
  font-weight: 700;
}

.exchange-sheet__header h3 {
  margin: 0;
  color: #224636;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 900;
}

.exchange-sheet__close {
  min-width: 56px;
  min-height: 34px;
  border: none;
  border-radius: 999px;
  background: #eef5f0;
  color: #54715f;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
}

.exchange-sheet__desc {
  margin: 12px 0 0;
  color: #6f877b;
  font-size: 14px;
  line-height: 1.7;
}

.exchange-sheet__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.exchange-sheet__stat {
  padding: 14px 12px;
  border-radius: 18px;
  background: #f5faf7;
}

.exchange-sheet__stat span {
  display: block;
  color: #7f9489;
  font-size: 12px;
  font-weight: 700;
}

.exchange-sheet__stat strong {
  display: block;
  margin-top: 8px;
  color: #1e4734;
  font-size: 22px;
  font-weight: 900;
}

.exchange-sheet__summary {
  margin: 16px 0 0;
  color: #3a5f4c;
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
  background: #eef5f0;
  color: #365644;
}

.sheet-button--primary {
  background: linear-gradient(135deg, #15b54d 0%, #1ec45a 100%);
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
  min-height: 46px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  box-shadow: none;
}

:deep(.ant-input) {
  font-size: 15px;
}

:deep(.ant-alert) {
  border-radius: 16px;
}

:deep(.ant-tag) {
  margin-inline-end: 0;
  border-radius: 999px;
  font-weight: 700;
}

:deep(.ant-spin-nested-loading),
:deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

:deep(.ant-spin-nested-loading) {
  height: 100%;
}

:deep(.ant-spin-container) {
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
  padding-bottom: calc(98px + env(safe-area-inset-bottom, 0px));
}

:deep(.ant-spin-container::-webkit-scrollbar) {
  display: none;
}

:deep(.ant-spin-blur) {
  overflow: hidden;
}

@media (max-width: 640px) {
  .reward-mall-page {
    --mall-content-width: 330px;
    --mall-hero-height: 148px;
    padding: 8px 8px calc(8px + env(safe-area-inset-bottom, 0px));
  }

  .reward-mall-phone {
    width: min(100%, 430px);
  }

  .mall-hero {
    padding: 18px 16px 14px;
    border-radius: 24px 24px 16px 16px;
  }

  .mall-hero h1 {
    font-size: 22px;
  }

  .mall-hero__top {
    grid-template-columns: minmax(0, auto) minmax(0, 1fr) 78px;
  }

  .mall-hero__action-spacer {
    width: 78px;
    min-width: 78px;
  }

  .ghost-action {
    min-height: 32px;
    padding: 0 12px;
    font-size: 11px;
  }

  .public-entry-chip {
    gap: 7px;
    min-height: 22px;
    margin-top: 7px;
    padding: 0 9px;
    font-size: 9px;
  }

  .public-entry-chip__value {
    font-size: 10px;
  }

  .view-strip {
    margin-top: 12px;
  }

  .view-chip {
    min-height: 46px;
    padding: 0 12px;
    border-radius: 18px;
    font-size: 13px;
  }

  .summary-row {
    margin-top: 10px;
  }

  .summary-pill {
    padding: 12px 10px;
  }

  .card-list,
  .record-list {
    gap: 10px;
    margin-top: 10px;
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

  .reward-card__title {
    font-size: 16px;
  }

  .reward-card__bottom {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .reward-card__points {
    font-size: 16px;
  }

  .exchange-button {
    width: 100%;
    min-width: 0;
    height: 40px;
    font-size: 13px;
  }

  .record-card__head {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .list-pagination {
    gap: 8px;
  }

  .pagination-text strong {
    font-size: 13px;
  }

  .pagination-text span {
    font-size: 11px;
  }

  .exchange-sheet {
    width: 100%;
    border-radius: 24px 24px 16px 16px;
  }

  .exchange-sheet__header h3 {
    font-size: 20px;
  }

  .exchange-sheet__desc,
  .exchange-sheet__summary {
    font-size: 13px;
    line-height: 1.6;
  }

  .exchange-sheet__stat strong {
    font-size: 20px;
  }
}

@media (max-width: 520px) {
  .reward-mall-page {
    --mall-content-width: 316px;
    --mall-hero-height: 142px;
    padding: 6px 6px calc(6px + env(safe-area-inset-bottom, 0px));
  }

  .reward-mall-phone {
    width: 100%;
  }

  .mall-hero {
    padding: 16px 14px 12px;
    border-radius: 22px 22px 15px 15px;
  }

  .mall-hero__top {
    grid-template-columns: minmax(0, auto) minmax(0, 1fr) 72px;
    gap: 8px;
  }

  .public-entry-chip--inline {
    max-width: 90px;
  }

  .mall-hero__action-spacer {
    width: 72px;
    min-width: 72px;
  }

  .mall-hero h1 {
    font-size: 20px;
  }

  .view-strip {
    border-radius: 24px;
  }

  .view-chip {
    min-height: 44px;
    font-size: 12px;
  }

  .summary-pill strong {
    font-size: 15px;
  }

  .reward-card {
    grid-template-columns: 58px minmax(0, 1fr);
  }

  .reward-card__media {
    width: 58px;
    height: 58px;
  }

  .reward-card__title {
    font-size: 15px;
  }

  .reward-card__points {
    font-size: 15px;
  }

  .pagination-button {
    min-height: 38px;
    font-size: 12px;
  }

  :deep(.ant-input-affix-wrapper) {
    min-height: 42px;
    padding: 0 12px;
  }

  :deep(.ant-input) {
    font-size: 14px;
  }
}

@media (max-width: 390px) {
  .reward-mall-page {
    --mall-content-width: 296px;
    --mall-hero-height: 136px;
  }

  .mall-hero {
    padding: 14px 12px 10px;
  }

  .mall-hero__top {
    grid-template-columns: minmax(0, auto) minmax(0, 1fr) 68px;
    gap: 6px;
  }

  .public-entry-chip--inline {
    max-width: 82px;
  }

  .mall-hero__action-spacer {
    width: 68px;
    min-width: 68px;
  }

  .ghost-action--top {
    top: 12px;
    right: 12px;
  }

  .mall-hero h1 {
    font-size: 18px;
  }

  .view-strip {
    gap: 6px;
    padding: 6px;
  }

  .summary-row {
    gap: 8px;
  }

  .reward-card,
  .record-card {
    padding: 12px;
  }

  .reward-card {
    grid-template-columns: 52px minmax(0, 1fr);
    gap: 10px;
  }

  .reward-card__media {
    width: 52px;
    height: 52px;
    border-radius: 14px;
  }

  .exchange-sheet__stats,
  .exchange-sheet__actions {
    grid-template-columns: 1fr;
  }
}
</style>
