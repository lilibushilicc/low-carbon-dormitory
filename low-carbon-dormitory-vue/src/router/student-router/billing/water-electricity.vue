<template>
  <div class="page">
    <div class="panel">
      <div class="header">
        <div class="header-main">
          <div class="hero-icon">水电</div>
          <div class="header-copy">
            <h2>宿舍水电费</h2>
            <p>查看当前水电余额、可用量和低碳积分结算情况。</p>
          </div>
        </div>

        <div class="header-actions">
          <el-button class="action-nav" @click="goPrevious">
            <el-icon><ArrowLeft /></el-icon>
            返回上一页
          </el-button>
          <el-button type="primary" plain class="action-nav action-nav--home" @click="goBack">
            返回首页
          </el-button>
        </div>
      </div>

      <el-skeleton :loading="loading" animated :rows="8">
        <template #default>
          <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />

          <div v-else-if="waterElectricity" class="board-layout">
            <section class="board-main">
              <div v-if="latestSettlementSummary" class="event-banner">
                <strong>最近一次事件已结算</strong>
                <span>{{ latestSettlementSummary }}</span>
              </div>

              <div class="balance-grid">
                <article class="info-card info-card--electric info-card--featured" :class="statusInfo.electricClass">
                  <span class="info-card__label">电费余额</span>
                  <strong>{{ formatCurrency(waterElectricity.electricityBalance) }}</strong>
                  <small>预计可用电量 {{ formatAvailable(waterElectricity.electricityAvailable, waterElectricity.electricityUnitName) }}</small>
                </article>

                <article class="info-card info-card--water info-card--featured" :class="statusInfo.waterClass">
                  <span class="info-card__label">水费余额</span>
                  <strong>{{ formatCurrency(waterElectricity.waterBalance) }}</strong>
                  <small>预计可用水量 {{ formatAvailable(waterElectricity.waterAvailable, waterElectricity.waterUnitName) }}</small>
                </article>
              </div>

              <div class="identity-strip">
                <div class="identity-chip">
                  <span>学号</span>
                  <strong>{{ waterElectricity.stuNum || stuNum || '-' }}</strong>
                </div>
                <div class="identity-chip">
                  <span>宿舍</span>
                  <strong>{{ currentDormLabel }}</strong>
                </div>
              </div>

              <div class="card-grid">
                <article class="detail-card">
                  <span class="detail-card__label">电费单价</span>
                  <strong>{{ electricityRateText }}</strong>
                </article>

                <article class="detail-card">
                  <span class="detail-card__label">水费单价</span>
                  <strong>{{ waterRateText }}</strong>
                </article>

                <article class="detail-card">
                  <span class="detail-card__label">最后扣费时间</span>
                  <strong>{{ formatDateTime(waterElectricity.lastDeductTime, '-') }}</strong>
                </article>

                <article class="detail-card">
                  <span class="detail-card__label">个人积分</span>
                  <strong>{{ formatNumber(waterElectricity.personalCarbonScore, 0) }}</strong>
                </article>

                <article class="detail-card">
                  <span class="detail-card__label">宿舍周期积分</span>
                  <strong>{{ formatNumber(waterElectricity.dormCarbonScore, 0) }}</strong>
                </article>

                <article class="detail-card detail-card--status" :class="statusInfo.overallClass">
                  <span class="detail-card__label">余额状态</span>
                  <strong>{{ statusInfo.title }}</strong>
                  <small>{{ statusInfo.description }}</small>
                </article>
              </div>
            </section>

            <aside class="board-side">
              <article class="side-panel">
                <div class="side-panel__title">快捷操作</div>
                <div class="quick-actions">
                  <button type="button" class="quick-action quick-action--amber" :disabled="refreshing" @click="handleRefresh">
                    <span class="quick-action__icon">↻</span>
                    <strong>{{ refreshing ? '刷新中' : '刷新数据' }}</strong>
                    <small>更新当前余额，记录刷新账单，并按本次事件结算积分</small>
                  </button>

                  <button type="button" class="quick-action quick-action--green" @click="goPay">
                    <span class="quick-action__icon">¥</span>
                    <strong>立即充值</strong>
                    <small>支付成功后会更新余额，并同步本次低碳积分。</small>
                  </button>

                  <button type="button" class="quick-action quick-action--blue" @click="goHistory">
                    <span class="quick-action__icon">≡</span>
                    <strong>查看账单</strong>
                    <small>查看充值、刷新和周期扣费记录</small>
                  </button>
                </div>
              </article>

              <article class="side-panel side-panel--tips">
                <div class="side-panel__title">事件说明</div>
                <ul class="tips-list">
                  <li>刷新会创建 0 元事件，用于同步当前余额与积分。</li>
                  <li>充值成功后会自动写入费用流水。</li>
                  <li>系统会根据费用变化结算宿舍和个人低碳积分。</li>
                </ul>
              </article>
            </aside>
          </div>

          <div v-else class="empty-holder"></div>
        </template>
      </el-skeleton>

      <div v-if="waterElectricity" class="footer-tip">
        <div>页面数据来自当前登录学生绑定的宿舍。</div>
        <div>刷新水电费会同步余额与积分。</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, HomeFilled } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { formatCurrency, formatDateTime, formatDormLabel, formatNumber, formatUnitPrice } from '@/utils/formatters'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
import { useStudentTokenStore } from '@/stores/student-token'
import { fetchWaterElectricity, refreshWaterElectricity, type StudentWaterElectricity } from '@/api/modules/student'

const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { stuNum, dormId, dormLabel } = storeToRefs(studentTokenStore)

const loading = ref(true)
const refreshing = ref(false)
const errorMessage = ref('')
const waterElectricity = ref<StudentWaterElectricity | null>(null)

const currentDormLabel = computed(() => formatDormLabel(waterElectricity.value, dormLabel.value))
const electricityRateText = computed(() =>
  waterElectricity.value ? formatUnitPrice(waterElectricity.value.electricityUnitPrice, '度电') : '-',
)
const waterRateText = computed(() =>
  waterElectricity.value ? formatUnitPrice(waterElectricity.value.waterUnitPrice, '吨水') : '-',
)

const latestSettlementSummary = computed(() => {
  const dormAdded = Number(waterElectricity.value?.carbonPointsAdded || 0)
  const personalAdded = Number(waterElectricity.value?.personalPointsAdded || 0)
  if (dormAdded <= 0 && personalAdded <= 0) return ''
  return `最近结算：宿舍积分 +${dormAdded}，个人积分 +${personalAdded}`
})

const statusInfo = computed(() => {
  const electric = Number(waterElectricity.value?.electricityBalance || 0)
  const water = Number(waterElectricity.value?.waterBalance || 0)

  if (electric <= 20 || water <= 40) {
    return {
      title: '余额偏低',
      description: '建议尽快刷新并充值，避免影响宿舍正常使用。',
      overallClass: 'is-danger',
      electricClass: electric <= 20 ? 'is-danger' : '',
      waterClass: water <= 40 ? 'is-danger' : '',
    }
  }

  if (electric <= 80 || water <= 100) {
    return {
      title: '余额提醒',
      description: '当前仍可使用，但建议尽快处理后续扣费。',
      overallClass: 'is-warning',
      electricClass: electric <= 80 ? 'is-warning' : '',
      waterClass: water <= 100 ? 'is-warning' : '',
    }
  }

  return {
    title: '余额正常',
    description: '当前水电余额充足，可以继续正常使用。',
    overallClass: 'is-safe',
    electricClass: '',
    waterClass: '',
  }
})

function goBack() {
  router.push('/index-student')
}

function goPrevious() {
  router.back()
}

function goPay() {
  router.push('/pay-up')
}

function goHistory() {
  router.push('/history-fee')
}

function normalizeUnit(unitName: string | null) {
  const normalized = String(unitName || '').toLowerCase()
  if (normalized.includes('kwh') || normalized.includes('电')) return '度'
  if (normalized.includes('ton') || normalized.includes('水')) return '吨'
  return unitName || ''
}

function formatAvailable(value: number | null, unitName: string | null) {
  if (value === null || value === undefined) return '-'
  return `${formatNumber(value, 2)} ${normalizeUnit(unitName)}`.trim()
}

async function loadWaterElectricity() {
  if (!stuNum.value && !dormId.value) {
    errorMessage.value = '未检测到登录信息，请重新登录'
    loading.value = false
    return
  }

  try {
    const { data } = await fetchWaterElectricity({
      stuNum: stuNum.value || undefined,
      dormId: dormId.value || undefined,
    })
    waterElectricity.value = requireApiData(data, '获取水电信息失败')
    studentTokenStore.updateCarbonScore(waterElectricity.value.personalCarbonScore)
    errorMessage.value = ''
  } catch (error) {
    console.error('获取水电信息失败:', error)
    errorMessage.value = resolveErrorMessage(error, '服务异常或网络错误')
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  if (!stuNum.value && !dormId.value) {
    ElMessage.warning('未检测到登录信息，请重新登录')
    return
  }

  refreshing.value = true
  try {
    const { data } = await refreshWaterElectricity({
      stuNum: stuNum.value || undefined,
      dormId: dormId.value || undefined,
    })
    waterElectricity.value = requireApiData(data, '刷新水电费失败')
    studentTokenStore.updateCarbonScore(waterElectricity.value.personalCarbonScore)
    errorMessage.value = ''
    ElMessage.success(
      latestSettlementSummary.value
        ? `刷新完成，${latestSettlementSummary.value}`
        : '刷新完成，余额与积分已同步。',
    )
  } catch (error) {
    console.error('刷新水电费失败:', error)
    ElMessage.error(resolveErrorMessage(error, '刷新失败，请稍后重试'))
  } finally {
    refreshing.value = false
  }
}

onMounted(loadWaterElectricity)
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: clamp(20px, 3vw, 36px) 16px 40px;
  background: linear-gradient(180deg, #eef6f1 0%, #f8fbf9 100%);
}

.panel {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e5efe9;
  box-shadow: 0 18px 40px rgba(36, 69, 54, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 800;
  color: #2b6a4f;
  background: #eef6f1;
  border: 1px solid #d6e5dc;
}

.header-copy h2 {
  margin: 0 0 8px;
  color: #244536;
  font-size: 28px;
  line-height: 1.25;
}

.header-copy p {
  margin: 0;
  color: #5f776b;
  font-size: 14px;
  line-height: 1.7;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-nav {
  min-width: 140px;
  min-height: 42px;
  padding: 0 18px;
  border-radius: 12px;
  border: 1px solid #d6e5dc;
  background: #fff;
  color: #244536;
  font-size: 14px;
  font-weight: 600;
}

.action-nav :deep(.el-icon) {
  margin-right: 8px;
}

.action-nav--home {
  border-color: #c7ddd0;
  color: #244536;
  background: #eef6f1;
}

.board-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.85fr);
  gap: 16px;
}

.balance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.identity-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.identity-chip {
  min-height: 86px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #e5efe9;
  background: #f6faf8;
  display: grid;
  gap: 8px;
}

.identity-chip span {
  color: #5f776b;
  font-size: 13px;
  font-weight: 600;
}

.identity-chip strong {
  color: #244536;
  font-size: 18px;
  line-height: 1.2;
}

.event-banner {
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(42, 104, 76, 0.2);
  background: linear-gradient(135deg, rgba(37, 99, 70, 0.1), rgba(116, 180, 149, 0.08));
  display: grid;
  gap: 6px;
}

.event-banner strong {
  color: #214935;
}

.event-banner span {
  color: #547064;
  font-size: 14px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.info-card,
.detail-card,
.side-panel {
  border-radius: 16px;
  border: 1px solid #e5efe9;
  background: #fcfefd;
}

.info-card,
.detail-card {
  min-height: 128px;
  padding: 16px;
  display: grid;
  align-content: start;
  gap: 10px;
}

.info-card__label,
.detail-card__label,
.side-panel__title {
  color: #5f776b;
  font-size: 13px;
  font-weight: 600;
}

.info-card strong {
  color: #244536;
  font-size: 28px;
  line-height: 1.15;
}

.info-card small,
.detail-card small,
.quick-action small,
.footer-tip,
.tips-list {
  color: #5f776b;
  font-size: 13px;
  line-height: 1.6;
}

.detail-card strong {
  color: #244536;
  font-size: 18px;
}

.info-card--meta {
  background: #f6faf8;
}

.info-card--compact {
  min-height: 92px;
}

.info-card--featured {
  min-height: 148px;
}

.info-card--electric {
  background: #fcfaf4;
  border-color: #eadfbf;
}

.info-card--water {
  background: #f6faf8;
  border-color: #d9e8e1;
}

.detail-card--status {
  grid-column: span 2;
}

.is-warning {
  box-shadow: inset 0 0 0 1px rgba(255, 179, 64, 0.32);
}

.is-danger {
  background: #fff6f3;
  border-color: #efc2b5;
}

.is-safe {
  background: #f6faf8;
}

.board-side {
  display: grid;
  gap: 12px;
}

.side-panel {
  padding: 16px;
}

.quick-actions {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.quick-action {
  min-height: 96px;
  padding: 14px 12px;
  border: 1px solid rgba(64, 122, 105, 0.38);
  border-radius: 18px;
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 8px;
  color: #eff8f4;
  cursor: pointer;
  transition: transform 0.18s ease;
}

.quick-action:hover {
  transform: translateY(-2px);
}

.quick-action:disabled {
  cursor: wait;
  opacity: 0.72;
  transform: none;
}

.quick-action__icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 700;
}

.quick-action strong {
  font-size: 16px;
  color: #f2fbf6;
}

.quick-action--green {
  background: rgba(20, 78, 58, 0.52);
}

.quick-action--green .quick-action__icon {
  color: #5df0c8;
  background: rgba(30, 102, 77, 0.62);
}

.quick-action--blue {
  background: rgba(24, 60, 92, 0.48);
}

.quick-action--blue .quick-action__icon {
  color: #61c2ff;
  background: rgba(22, 86, 133, 0.52);
}

.quick-action--amber {
  background: rgba(87, 66, 17, 0.58);
}

.quick-action--amber .quick-action__icon {
  color: #ffd36b;
  background: rgba(130, 96, 22, 0.56);
}

.tips-list {
  margin: 12px 0 0;
  padding-left: 18px;
}

.footer-tip {
  margin-top: 18px;
  min-height: 68px;
  padding: 18px 24px;
  border-radius: 20px;
  border: 1px solid rgba(28, 81, 69, 0.32);
  background: rgba(7, 28, 24, 0.82);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  color: #86c8b1;
}

.empty-holder {
  min-height: 240px;
}

@media (max-width: 1180px) {
  .board-layout {
    grid-template-columns: 1fr;
  }

  .balance-grid,
  .identity-strip,
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-card--status {
    grid-column: span 2;
  }
}

@media (max-width: 860px) {
  .panel {
    padding: 22px 18px 20px;
  }

  .header {
    flex-direction: column;
  }

  .balance-grid,
  .identity-strip,
  .card-grid {
    grid-template-columns: 1fr;
  }

  .info-card,
  .detail-card {
    min-height: 96px;
    padding: 14px;
  }

  .info-card strong {
    font-size: 22px;
  }

  .detail-card strong {
    font-size: 17px;
  }

  .detail-card--status {
    grid-column: span 1;
  }

  .footer-tip {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-main {
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .action-nav {
    min-width: 0;
    width: 100%;
  }
}
</style>


