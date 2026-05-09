<template>
  <div class="reward-mall-page">
    <div class="reward-mall-shell">
      <div class="reward-mall-phone">
        <ASpin :spinning="loading">
          <header class="mall-hero">
            <div class="mall-hero__glow"></div>

            <div class="mall-hero__top">
              <div class="mall-hero__copy">
                <h1>碳积分商店</h1>
                <p>用积分兑换专属奖励</p>
              </div>

              <div class="points-badge">
                <TrophyOutlined />
                <strong>{{ currentPoints }}</strong>
                <span>分</span>
              </div>
            </div>

            <div class="mall-hero__search">
              <AInput v-model:value="keyword" size="large" allow-clear placeholder="搜索商品...">
                <template #prefix>
                  <SearchOutlined />
                </template>
              </AInput>
            </div>

            <div class="mall-hero__actions">
              <button type="button" class="ghost-action" @click="goBack">
                <LeftOutlined />
                返回
              </button>
              <button type="button" class="ghost-action" @click="loadRewardCenter">
                <ReloadOutlined />
                刷新
              </button>
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

            <section class="promo-card" :class="{ 'promo-card--ready': promoReady }">
              <div class="promo-card__icon">
                <GiftOutlined />
              </div>

              <div class="promo-card__body">
                <div class="promo-card__head">
                  <span class="promo-card__tag">{{ promoTag }}</span>
                  <strong>{{ promoTitle }}</strong>
                </div>
                <p>{{ promoDescription }}</p>
              </div>

              <div class="promo-card__side">{{ promoHint }}</div>
            </section>

            <section class="summary-row">
              <article class="summary-pill">
                <span>当前积分</span>
                <strong>{{ currentPoints }}</strong>
              </article>
              <article class="summary-pill">
                <span>奖励状态</span>
                <strong>{{ rewardStatusText }}</strong>
              </article>
              <article class="summary-pill">
                <span>推荐奖励</span>
                <strong>{{ nearestRewardName }}</strong>
              </article>
            </section>

            <section v-if="activeView === 'rewards'" class="card-list">
              <article v-for="item in displayRewards" :key="item.rewardId" class="reward-card">
                <div class="reward-card__media">
                  <span class="reward-card__stock">{{ item.stockText }}</span>
                  <img
                    :src="safeImageUrl(item.imageUrl)"
                    :alt="item.rewardName"
                    class="reward-card__image"
                    @error="handleImageError"
                  />
                </div>

                <div class="reward-card__content">
                  <div class="reward-card__head">
                    <div>
                      <h3>{{ item.rewardName }}</h3>
                      <div v-if="item.badge" class="reward-card__tags">
                        <ATag :color="item.badge.color">{{ item.badge.text }}</ATag>
                      </div>
                    </div>
                  </div>

                  <p class="reward-card__desc">{{ item.rewardDesc }}</p>

                  <div class="reward-card__bottom">
                    <div class="reward-card__meta">
                      <span class="reward-card__points">
                        <GiftOutlined />
                        {{ item.pointsCost }}积分
                      </span>
                      <small>{{ item.helperText }}</small>
                      <small class="reward-card__submeta">{{ item.stockText }} · {{ item.exchangeUnitText }}</small>
                    </div>

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

              <AEmpty
                v-if="!displayRewards.length"
                class="empty-block"
                description="没有匹配到可展示的奖励"
              />
            </section>

            <section v-else class="record-list">
              <article v-for="record in displayRecords" :key="record.recordId" class="record-card">
                <div class="record-card__head">
                  <div>
                    <h3>{{ record.rewardName }}</h3>
                    <p>{{ record.remark || '个人积分兑换成功，等待发放。' }}</p>
                  </div>
                  <ATag color="green">-{{ record.exchangePoints }}积分</ATag>
                </div>

                <div class="record-card__footer">
                  <span>
                    <ClockCircleOutlined />
                    {{ formatDateTime(record.exchangeTime) }}
                  </span>
                </div>
              </article>

              <AEmpty v-if="!displayRecords.length" class="empty-block" description="暂无兑换记录" />
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
                  <span>单次兑换</span>
                  <strong>1 件</strong>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
  ReloadOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
  TrophyOutlined,
} from '@ant-design/icons-vue'
import 'ant-design-vue/dist/reset.css'
import type { RewardItem } from '@/api/modules/reward'
import { resolveErrorMessage } from '@/utils/api-response'
import { formatDateTime } from '@/utils/formatters'
import { useRewardCenter } from './use-reward-center'

type RewardView = 'rewards' | 'records'

interface RewardBadge {
  text: string
  color: string
}

interface DisplayReward extends RewardItem {
  badge: RewardBadge | null
  helperText: string
  actionText: string
  disabled: boolean
  stockText: string
  exchangeUnitText: string
}

const FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240"><rect width="240" height="240" rx="48" fill="%23eef7f1"/><circle cx="120" cy="92" r="34" fill="%23dcefe2"/><rect x="68" y="136" width="104" height="20" rx="10" fill="%2322c55e" opacity="0.18"/><rect x="84" y="88" width="72" height="72" rx="18" fill="%23ffffff"/><path d="M102 136h36" stroke="%2322c55e" stroke-width="12" stroke-linecap="round"/><path d="M120 100v28" stroke="%2322c55e" stroke-width="12" stroke-linecap="round"/></svg>'

const viewOptions = [
  { label: '商品列表', value: 'rewards' as const },
  { label: '兑换记录', value: 'records' as const },
]

const router = useRouter()
const keyword = ref('')
const activeView = ref<RewardView>('rewards')
const { stuNum, loading, errorMessage, rewardCenter, submittingRewardId, currentPoints, loadRewardCenter, exchangeRewardById } =
  useRewardCenter()
const selectedReward = ref<DisplayReward | null>(null)
const exchangeModalOpen = ref(false)
const exchangeConfirmLoading = ref(false)

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
const rewardItems = computed(() => rewardCenter.value?.rewardItems || [])
const exchangeRecords = computed(() => rewardCenter.value?.exchangeRecords || [])
const nearestRewardName = computed(() => rewardCenter.value?.dormScoreSummary.nearestRewardName || '暂无')
const gapToNearestReward = computed(() => rewardCenter.value?.dormScoreSummary.gapToNearestReward || 0)
const promoReady = computed(() => gapToNearestReward.value <= 0)

const promoTitle = computed(() => {
  if (nearestRewardName.value === '暂无') {
    return '积分兑换专区'
  }

  return promoReady.value ? `${nearestRewardName.value} 已可兑换` : `${nearestRewardName.value} 即将解锁`
})

const promoDescription = computed(() => {
  if (nearestRewardName.value === '暂无') {
    return '浏览当前奖励内容，挑选最适合你的积分兑换项。'
  }

  return promoReady.value
    ? '积分条件已经满足，现在就可以直接发起兑换。'
    : '继续积累积分，就能解锁这份推荐奖励。'
})

const promoTag = computed(() => (promoReady.value ? '立即可兑' : '积分推荐'))

const promoHint = computed(() => {
  if (nearestRewardName.value === '暂无') {
    return '去看看'
  }

  return promoReady.value ? '马上兑换' : `还差${gapToNearestReward.value}分`
})

const rewardStatusText = computed(() => {
  if (!rewardItems.value.length) {
    return '暂无奖励'
  }

  return rewardItems.value.some((item) => item.canExchange) ? '可立即兑换' : '继续攒分'
})

const displayRewards = computed<DisplayReward[]>(() => {
  return rewardItems.value
    .filter((item) => matchesKeyword(item.rewardName, item.rewardDesc))
    .map(toDisplayReward)
})

const displayRecords = computed(() => {
  return exchangeRecords.value.filter((record) => matchesKeyword(record.rewardName, record.remark || ''))
})

function matchesKeyword(...texts: string[]) {
  if (!normalizedKeyword.value) {
    return true
  }

  return texts.some((text) => text.toLowerCase().includes(normalizedKeyword.value))
}

function toDisplayReward(item: RewardItem): DisplayReward {
  return {
    ...item,
    badge: resolveBadge(item),
    helperText: item.canExchange ? '满足条件即可发起兑换' : '积分满足后可兑换',
    actionText: item.canExchange ? '兑换' : item.stock <= 0 ? '已兑完' : '积分不足',
    disabled: !item.canExchange || submittingRewardId.value === item.rewardId,
    stockText: `剩余 ${Math.max(item.stock, 0)} 件`,
    exchangeUnitText: '单次兑换 1 件',
  }
}

function goBack() {
  router.push('/index-student')
}

function getRewardCategory(item: RewardItem) {
  const source = `${item.rewardName} ${item.rewardDesc}`.toLowerCase()

  if (/(公益|捐|植树|树|助力|沙漠|环保捐)/.test(source)) {
    return '公益捐助'
  }

  if (/(头像框|徽章|装扮|皮肤|称号|虚拟)/.test(source)) {
    return '虚拟装扮'
  }

  if (/(图书馆|时长|延时|服务|权益|特权|课程|资格|体验|使用权)/.test(source)) {
    return '权益服务'
  }

  return ''
}

function resolveBadge(item: RewardItem): RewardBadge | null {
  const category = getRewardCategory(item)

  if (category === '公益捐助') {
    return { text: '公益', color: 'blue' }
  }

  if (category === '虚拟装扮') {
    return { text: '装饰', color: 'gold' }
  }

  if (category === '权益服务') {
    return { text: '权益', color: 'cyan' }
  }

  return null
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

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})

onMounted(loadRewardCenter)
</script>

<style scoped>
.reward-mall-page {
  min-height: 100vh;
  padding: 24px 12px calc(40px + env(safe-area-inset-bottom, 0px));
  background:
    radial-gradient(circle at top left, rgba(101, 183, 126, 0.16), transparent 28%),
    linear-gradient(180deg, #edf8f1 0%, #f7fbf8 100%);
}

.reward-mall-shell {
  display: flex;
  justify-content: center;
}

.reward-mall-phone {
  width: min(100%, 420px);
}

.mall-hero {
  position: relative;
  overflow: hidden;
  padding: 28px 24px 20px;
  border-radius: 32px 32px 20px 20px;
  background: linear-gradient(160deg, #157338 0%, #1ba34b 100%);
  box-shadow: 0 22px 48px rgba(21, 115, 56, 0.22);
}

.mall-hero__glow {
  position: absolute;
  right: -26px;
  top: -48px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.14), transparent 68%);
  pointer-events: none;
}

.mall-hero__top {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.mall-hero__copy {
  min-width: 0;
}

.mall-hero h1 {
  margin: 0;
  color: #ffffff;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 900;
}

.mall-hero p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.88);
  font-size: 18px;
  line-height: 1.5;
}

.points-badge {
  position: relative;
  z-index: 1;
  min-height: 62px;
  padding: 0 18px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.24);
  backdrop-filter: blur(8px);
  flex-shrink: 0;
}

.points-badge strong {
  font-size: 32px;
  line-height: 1;
}

.points-badge span {
  font-size: 16px;
  opacity: 0.88;
}

.mall-hero__search {
  position: relative;
  z-index: 1;
  margin-top: 24px;
}

.mall-hero__actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.ghost-action {
  min-height: 38px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.view-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 16px 0 4px;
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

.promo-card {
  margin-top: 18px;
  min-height: 92px;
  padding: 14px 16px;
  border-radius: 22px;
  display: flex;
  gap: 12px;
  align-items: center;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 30%),
    linear-gradient(135deg, #ffab00 0%, #ffc636 100%);
  box-shadow: 0 16px 28px rgba(255, 178, 36, 0.18);
}

.promo-card--ready {
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 30%),
    linear-gradient(135deg, #ff9800 0%, #ffbe2e 100%);
}

.promo-card__icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-size: 20px;
  flex-shrink: 0;
}

.promo-card__body {
  min-width: 0;
  flex: 1;
}

.promo-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.promo-card__tag {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.96);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.promo-card__body strong {
  display: block;
  color: #ffffff;
  font-size: 18px;
  line-height: 1.25;
}

.promo-card__body p {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  line-height: 1.45;
}

.promo-card__side {
  min-width: 72px;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.22);
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  flex-shrink: 0;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.summary-pill {
  padding: 14px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
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
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.reward-card,
.record-card {
  padding: 18px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 34px rgba(33, 83, 52, 0.1);
}

.reward-card {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.reward-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 38px rgba(33, 83, 52, 0.14);
}

.reward-card__media {
  position: relative;
  width: 82px;
  height: 82px;
  border-radius: 22px;
  overflow: hidden;
  background: #eef7f1;
}

.reward-card__stock {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.72);
  color: #ffffff;
  font-size: 11px;
  line-height: 1.2;
  font-weight: 700;
  backdrop-filter: blur(6px);
}

.reward-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reward-card__content {
  min-width: 0;
}

.reward-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.reward-card__head h3,
.record-card__head h3 {
  margin: 0;
  color: #234936;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
}

.reward-card__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.reward-card__desc,
.record-card__head p {
  margin: 8px 0 0;
  color: #7f968a;
  font-size: 14px;
  line-height: 1.6;
}

.reward-card__bottom {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.reward-card__meta {
  display: grid;
  gap: 8px;
}

.reward-card__points {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #14a54f;
  font-size: 18px;
  font-weight: 900;
}

.reward-card__meta small {
  color: #90a699;
  font-size: 13px;
  line-height: 1.5;
}

.reward-card__submeta {
  color: #688072;
}

.exchange-button {
  min-width: 108px;
  height: 46px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #15b54d 0%, #1ec45a 100%);
  box-shadow: none;
  font-weight: 800;
}

.record-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
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
  min-height: 56px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  box-shadow: none;
}

:deep(.ant-input) {
  font-size: 16px;
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
  display: block;
}

@media (max-width: 520px) {
  .reward-mall-page {
    padding-top: 14px;
    padding-inline: 10px;
  }

  .mall-hero {
    padding: 24px 18px 18px;
  }

  .mall-hero__top {
    flex-direction: column;
  }

  .points-badge {
    align-self: flex-start;
  }

  .summary-row {
    display: grid;
    grid-auto-flow: column;
    grid-auto-columns: minmax(112px, 1fr);
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .summary-row::-webkit-scrollbar {
    display: none;
  }

  .promo-card {
    align-items: flex-start;
  }

  .promo-card__head {
    flex-wrap: wrap;
  }

  .promo-card__side {
    margin-left: 54px;
  }

  .reward-card {
    grid-template-columns: 1fr;
  }

  .reward-card__media {
    width: 88px;
    height: 88px;
  }

  .reward-card__bottom,
  .record-card__head {
    flex-direction: column;
    align-items: stretch;
  }

  .exchange-sheet {
    width: 100%;
    border-radius: 26px 26px 18px 18px;
  }

  .exchange-sheet__stats,
  .exchange-sheet__actions {
    grid-template-columns: 1fr;
  }

  .exchange-button {
    width: 100%;
  }
}
</style>
