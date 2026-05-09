<template>
  <div class="dashboard-page">
    <div class="dashboard-shell" v-loading="loading">
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />

      <el-empty
        v-else-if="!loading && !dashboard"
        class="empty-state"
        description="当前暂无个人低碳看板数据"
      />

      <template v-else-if="dashboard">
        <section class="hero">
          <div class="hero__content">
            <div class="hero__tag-row">
              <span class="hero__eyebrow">个人低碳看板</span>
              <span class="status-chip" :class="heroStatus.className">{{ heroStatus.text }}</span>
            </div>

            <h1>{{ dashboard.dormAnchor.dormLabel }}</h1>
            <p class="hero__subtitle">
              {{ dashboard.dormAnchor.building }}
              <span v-if="dashboard.dormAnchor.college"> · {{ dashboard.dormAnchor.college }}</span>
            </p>

            <div class="hero__meta">
                <span>成员 {{ dashboard.dormAnchor.residentCount }} 人</span>
                <span>最后更新 {{ formatDateTime(dashboard.dormAnchor.dataUpdatedAt || dashboard.lastUpdatedAt) }}</span>
              <span>{{ dashboard.periodLabel }}</span>
            </div>

            <div v-if="residentNames.length" class="resident-list">
              <span v-for="name in residentNames" :key="name">{{ name }}</span>
            </div>
          </div>

          <div class="hero__score-card">
                  <small>根据本周期宿舍能耗与碳排表现计算</small>
            <strong>{{ formatScore(dashboard.scoreSummary.currentPeriodScore) }}</strong>
            <span>{{ dashboard.dormAnchor.carbonLevel }}</span>
            <div class="hero__score-note">
              {{ dashboard.scoreSummary.currentPeriodParticipating ? currentRankText : '本周期仅展示上次结果' }}
            </div>
          </div>
        </section>

        <section class="metric-grid">
          <article v-for="item in summaryCards" :key="item.label" class="metric-card">
            <span class="metric-card__label">{{ item.label }}</span>
            <strong class="metric-card__value">{{ item.value }}</strong>
            <small class="metric-card__note">{{ item.note }}</small>
          </article>
        </section>

        <section class="dual-grid dual-grid--compact">
          <article class="panel">
            <div class="panel__head">
              <div>
                <div class="panel__eyebrow">排名对比</div>
                <h2>楼栋 / 学院 / 学校</h2>
              </div>
            </div>

            <div class="ranking-grid">
              <div v-for="item in rankingCards" :key="item.title" class="ranking-card">
                <div class="ranking-card__head">
                  <span>{{ item.title }}</span>
                  <strong>{{ item.rankText }}</strong>
                </div>
                <div class="progress">
                  <div class="progress__bar" :style="{ width: item.progress + '%' }"></div>
                </div>
                <small>{{ item.note }}</small>
              </div>
            </div>
          </article>

          <article class="panel">
            <div class="panel__head">
              <div>
                <div class="panel__eyebrow">费用构成</div>
                <h2>本周期扣费占比</h2>
              </div>
            </div>

            <div class="pie-section">
              <div class="pie-card">
                <div class="pie-card__title">扣费构成</div>
                <div class="pie-visual">
                  <div class="donut" :style="{ background: feePieBackground }">
                    <div class="donut__center">
                  <small>电费占比</small>
                      <strong>{{ formatCurrency(dashboard.overview.totalFee) }}</strong>
                    </div>
                  </div>
                </div>
                <div class="legend-list">
                  <div v-for="item in feeLegend" :key="item.label" class="legend-item">
                    <span class="legend-item__dot" :style="{ background: item.color }"></span>
                    <div class="legend-item__text">
                      <strong>{{ item.label }}</strong>
                      <small>{{ item.value }} · {{ item.percent }}</small>
                    </div>
                  </div>
                </div>
              </div>

              <div class="pie-card">
                <div class="pie-card__title">碳排构成</div>
                <div class="pie-visual">
                  <div class="donut" :style="{ background: carbonPieBackground }">
                    <div class="donut__center">
                  <small>用水占比</small>
                      <strong>{{ formatCarbon(dashboard.overview.totalCarbon) }}</strong>
                    </div>
                  </div>
                </div>
                <div class="legend-list">
                  <div v-for="item in carbonLegend" :key="item.label" class="legend-item">
                    <span class="legend-item__dot" :style="{ background: item.color }"></span>
                    <div class="legend-item__text">
                      <strong>{{ item.label }}</strong>
                      <small>{{ item.value }} · {{ item.percent }}</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="detail-grid detail-grid--summary">
              <div v-for="item in feeAndCarbonCards" :key="item.label" class="detail-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </article>
        </section>

        <section class="dual-grid">
          <article class="panel">
            <div class="panel__head">
              <div>
                <div class="panel__eyebrow">历史趋势</div>
                <h2>历史周期积分变化</h2>
              </div>
            </div>

            <div class="trend-chart">
              <div v-for="item in trendBars" :key="item.label" class="trend-bar">
                <div class="trend-bar__track">
                  <div class="trend-bar__fill" :style="{ height: item.height + '%' }"></div>
                </div>
                <strong>{{ item.shortScore }}</strong>
                <span>{{ item.label }}</span>
                <small>{{ item.rankText }}</small>
              </div>
            </div>
          </article>

          <article class="panel">
            <div class="panel__head">
              <div>
                <div class="panel__eyebrow">榜样宿舍</div>
                <h2>本楼栋优秀宿舍</h2>
              </div>
            </div>

            <div class="benchmark-list">
              <div
                v-for="item in buildingBenchmarks"
                :key="'building-' + item.dormLabel"
                class="benchmark-item"
              >
                <div>
                  <strong>{{ item.dormLabel }}</strong>
                  <small>第 {{ item.rank }} 名</small>
                </div>
                <div class="benchmark-item__metrics">
                  <span>{{ formatScore(item.carbonScore) }}</span>
                  <span>{{ formatCarbon(item.totalCarbon) }}</span>
                </div>
              </div>

              <el-empty
                v-if="!buildingBenchmarks.length"
                :image-size="72"
              description="当前暂无楼栋榜样宿舍数据"
              />
            </div>
          </article>
        </section>

        <section class="dual-grid">
          <article class="panel">
            <div class="panel__head">
              <div>
              <div class="panel__eyebrow">学院榜样</div>
                <h2>学院优秀宿舍</h2>
              </div>
            </div>

            <div class="benchmark-list">
              <div
                v-for="item in collegeBenchmarks"
                :key="'college-' + item.dormLabel"
                class="benchmark-item"
              >
                <div>
                  <strong>{{ item.dormLabel }}</strong>
                  <small>第 {{ item.rank }} 名</small>
                </div>
                <div class="benchmark-item__metrics">
                  <span>{{ formatScore(item.carbonScore) }}</span>
                  <span>{{ formatCarbon(item.totalCarbon) }}</span>
                </div>
              </div>

              <el-empty
                v-if="!collegeBenchmarks.length"
                :image-size="72"
              description="当前暂无学院优秀宿舍数据"
              />
            </div>
          </article>

          <article class="panel">
            <div class="panel__head">
              <div>
                <div class="panel__eyebrow">规则说明</div>
              <h2>积分计算</h2>
              </div>
              <el-button text @click="goRulePage">查看规则</el-button>
            </div>

            <div class="rule-grid">
              <div class="detail-item">
                <span>电碳系数</span>
                <strong>{{ formatNumber(dashboard.ruleSummary.baseScore, 0) }}</strong>
              </div>
              <div class="detail-item">
                <span>电碳系数</span>
                <strong>{{ formatNumber(dashboard.ruleSummary.electricCarbonFactor, 4) }}</strong>
              </div>
              <div class="detail-item">
                <span>水碳系数</span>
                <strong>{{ formatNumber(dashboard.ruleSummary.waterCarbonFactor, 4) }}</strong>
              </div>
              <div class="detail-item">
                <span>扣分系数</span>
                <strong>{{ formatNumber(dashboard.ruleSummary.carbonPenaltyFactor, 4) }}</strong>
              </div>
            </div>

            <p class="rule-text">{{ dashboard.ruleSummary.formulaText }}</p>
            <p class="rule-note">{{ dashboard.ruleSummary.rankingUpdateNote }}</p>
          </article>
        </section>

        <section class="panel panel--suggestions">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">建议</div>
              <h2>低碳优化建议</h2>
            </div>
          </div>

          <div class="suggestion-list">
            <div v-for="item in recommendationList" :key="item.title" class="suggestion-item">
              <div class="suggestion-item__head">
                <strong>{{ item.title }}</strong>
                <span>{{ item.priority }}</span>
              </div>
              <p>{{ item.detail }}</p>
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { fetchPersonalDashboard, type PersonalDashboardData, type SimpleDormBenchmark } from '@/api/modules/dashboard'
import { formatCarbon, formatCurrency, formatDateTime, formatNumber, formatScore } from '@/utils/formatters'
import { useStudentTokenStore } from '@/stores/student-token'

const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { stuNum, dormId } = storeToRefs(studentTokenStore)

const loading = ref(false)
const errorMessage = ref('')
const dashboard = ref<PersonalDashboardData | null>(null)

type PieLegendItem = {
  label: string
  value: string
  percent: string
  color: string
}

const residentNames = computed(() => (dashboard.value?.dormAnchor?.residents ?? []).filter(Boolean))

const heroStatus = computed(() => {
  if (!dashboard.value?.dormAnchor.currentPeriodParticipating) {
    return { text: '展示上次结算数据', className: 'status-chip--stale' }
  }
  return { text: '本周参与排名', className: 'status-chip--current' }
})

const currentRankText = computed(() => {
  if (!dashboard.value) return '-'
  const rank = dashboard.value.scoreSummary.currentSchoolRank || 0
  const total = dashboard.value.scoreSummary.rankedSchoolDormCount || 0
  return total > 0 ? `全校第 ${rank} / ${total} 名` : '本周暂无可排名数据'
})

function formatDeltaScore(value?: number | null) {
  const num = Number(value || 0)
  if (num > 0) return `提升 ${formatNumber(num, 1)} 分`
  if (num < 0) return `下降 ${formatNumber(Math.abs(num), 1)} 分`
  return '保持不变'
}

const summaryCards = computed(() => {
  if (!dashboard.value) return []

  return [
    {
      label: '个人低碳积分',
      value: formatScore(dashboard.value.scoreSummary.dormPoints),
      note: `较上周期 ${formatDeltaScore(dashboard.value.scoreSummary.scoreDelta)}`,
    },
    {
      label: '当前学校排名',
      value: dashboard.value.scoreSummary.currentPeriodParticipating
        ? `${dashboard.value.scoreSummary.currentSchoolRank || '-'}`
        : '暂不参排',
      note: dashboard.value.scoreSummary.currentPeriodParticipating
        ? `共 ${dashboard.value.scoreSummary.rankedSchoolDormCount || 0} 个参排名宿舍`
        : '本周无新数据，不参与正式排名',
    },
    {
      label: '总碳排放',
      value: formatCarbon(dashboard.value.overview.totalCarbon),
      note: `人均 ${formatCarbon(dashboard.value.overview.perCapitaCarbon)}`,
    },
    {
      label: '周期总扣费',
      value: formatCurrency(dashboard.value.overview.totalFee),
      note: `电费 ${formatCurrency(dashboard.value.overview.electricFee)} · 水费 ${formatCurrency(dashboard.value.overview.waterFee)}`,
    },
  ]
})

function getProgress(rank?: number, total?: number, participating = true) {
  if (!participating || !rank || !total) return 0
  const betterThan = Math.max(total - rank + 1, 0)
  return Math.max(12, Math.min(100, Math.round((betterThan / total) * 100)))
}

const rankingCards = computed(() => {
  if (!dashboard.value) return []

  const items = [
    { title: '楼栋排名', data: dashboard.value.comparisons.building },
    { title: '学院排名', data: dashboard.value.comparisons.college },
    { title: '学校排名', data: dashboard.value.comparisons.school },
  ]

  return items.map((item) => ({
    title: item.title,
    rankText: item.data.currentPeriodParticipating ? `第 ${item.data.rank} / ${item.data.totalDormCount} 名` : '暂不参排',
    progress: getProgress(item.data.rank, item.data.totalDormCount, item.data.currentPeriodParticipating),
    note: item.data.currentPeriodParticipating ? item.data.percentileText : '本周无新数据，当前仅展示上次结果',
  }))
})

const feeAndCarbonCards = computed(() => {
  if (!dashboard.value) return []
  const overview = dashboard.value.overview
  return [
    { label: '电量折算', value: formatNumber(overview.electricUsage, 2) },
    { label: '水量折算', value: formatNumber(overview.waterUsage, 2) },
    { label: '宿舍积分', value: formatScore(overview.carbonScore) },
    { label: '人均碳排放', value: formatCarbon(overview.perCapitaCarbon) },
  ]
})

function createPieBackground(firstValue: number, secondValue: number, firstColor: string, secondColor: string) {
  const total = firstValue + secondValue
  if (total <= 0) {
    return `conic-gradient(${firstColor} 0deg 180deg, ${secondColor} 180deg 360deg)`
  }
  const firstDeg = Math.round((firstValue / total) * 360)
  return `conic-gradient(${firstColor} 0deg ${firstDeg}deg, ${secondColor} ${firstDeg}deg 360deg)`
}

function getPercentText(value: number, total: number) {
  if (total <= 0) return '0%'
  return `${((value / total) * 100).toFixed(1).replace(/\.0$/, '')}%`
}

const feeLegend = computed<PieLegendItem[]>(() => {
  if (!dashboard.value) return []
  const overview = dashboard.value.overview
  const total = overview.electricFee + overview.waterFee
  return [
    {
      label: '电费扣费',
      value: formatCurrency(overview.electricFee),
      percent: getPercentText(overview.electricFee, total),
      color: '#3ba272',
    },
    {
      label: '水费扣费',
      value: formatCurrency(overview.waterFee),
      percent: getPercentText(overview.waterFee, total),
      color: '#69c0ff',
    },
  ]
})

const feePieBackground = computed(() => {
  if (!dashboard.value) return createPieBackground(1, 1, '#3ba272', '#69c0ff')
  return createPieBackground(
    dashboard.value.overview.electricFee,
    dashboard.value.overview.waterFee,
    '#3ba272',
    '#69c0ff',
  )
})

const carbonBreakdown = computed(() => {
  if (!dashboard.value) {
    return { electricCarbon: 0, waterCarbon: 0 }
  }
  return {
    electricCarbon: dashboard.value.overview.electricUsage * dashboard.value.ruleSummary.electricCarbonFactor,
    waterCarbon: dashboard.value.overview.waterUsage * dashboard.value.ruleSummary.waterCarbonFactor,
  }
})

const carbonLegend = computed<PieLegendItem[]>(() => {
  if (!dashboard.value) return []
  const electricCarbon = carbonBreakdown.value.electricCarbon
  const waterCarbon = carbonBreakdown.value.waterCarbon
  const total = electricCarbon + waterCarbon
  return [
    {
      label: '电力碳排放',
      value: formatCarbon(electricCarbon),
      percent: getPercentText(electricCarbon, total),
      color: '#f6bd16',
    },
    {
      label: '用水碳排放',
      value: formatCarbon(waterCarbon),
      percent: getPercentText(waterCarbon, total),
      color: '#5b8ff9',
    },
  ]
})

const carbonPieBackground = computed(() => {
  return createPieBackground(
    carbonBreakdown.value.electricCarbon,
    carbonBreakdown.value.waterCarbon,
    '#f6bd16',
    '#5b8ff9',
  )
})

const trendBars = computed(() => {
  const points = dashboard.value?.trends?.points ?? []
  if (!points.length) return []
  const maxScore = Math.max(...points.map((item) => item.carbonScore), 1)
  return points.map((item) => ({
    ...item,
    shortScore: formatNumber(item.carbonScore, 0),
    height: Math.max(14, Math.round((item.carbonScore / maxScore) * 100)),
    rankText: item.schoolRank ? `全校第 ${item.schoolRank} 名` : '该周期未参排',
  }))
})

const buildingBenchmarks = computed<SimpleDormBenchmark[]>(() => dashboard.value?.trends?.buildingTopDorms ?? [])
const collegeBenchmarks = computed<SimpleDormBenchmark[]>(() => dashboard.value?.trends?.collegeTopDorms ?? [])

const recommendationList = computed(() => {
  if (dashboard.value?.recommendations?.length) return dashboard.value.recommendations
  return [
    {
      title: '保持当前节奏',
      priority: '中优先级',
      detail: '当前没有额外预警项，继续保持稳定用电和节约用水即可。',
    },
  ]
})

function goRulePage() {
  router.push('/low-carbon-rule-readonly')
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''

  try {
    const { data } = await fetchPersonalDashboard({
      stuNum: stuNum.value || undefined,
      dormId: dormId.value ? Number(dormId.value) : undefined,
    })

    if (data.code !== 200 || !data.data) {
      errorMessage.value = data.msg || '获取个人低碳看板失败'
      dashboard.value = null
      return
    }

    dashboard.value = data.data
  } catch (error) {
    console.error(error)
    errorMessage.value = '获取个人低碳看板失败，请稍后重试'
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  padding: 24px 16px 40px;
  background:
    radial-gradient(circle at top left, rgba(69, 135, 101, 0.16), transparent 28%),
    radial-gradient(circle at right 20%, rgba(111, 174, 143, 0.12), transparent 30%),
    linear-gradient(180deg, #edf6f0 0%, #f8fbf9 100%);
}

.dashboard-shell {
  max-width: 1240px;
  margin: 0 auto;
  display: grid;
  gap: 14px;
}

.empty-state,
.panel,
.metric-card,
.hero {
  border: 1px solid rgba(39, 82, 61, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 20px 48px rgba(31, 63, 47, 0.08);
}

.hero,
.panel,
.metric-card {
  border-radius: 20px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  padding: 24px;
  overflow: hidden;
  position: relative;
}

.hero::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(39, 102, 72, 0.05), transparent 45%);
  pointer-events: none;
}

.hero__content,
.hero__score-card {
  position: relative;
  z-index: 1;
}

.hero__tag-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero__eyebrow,
.panel__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #ebf5ef;
  color: #43715b;
  font-size: 12px;
  font-weight: 700;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-chip--current {
  background: rgba(45, 122, 82, 0.12);
  color: #1f6f49;
}

.status-chip--stale {
  background: rgba(163, 118, 29, 0.14);
  color: #8a6110;
}

.hero h1,
.panel h2 {
  margin: 14px 0 8px;
  color: #1f3c2f;
  line-height: 1.2;
}

.hero h1 {
  font-size: clamp(28px, 4vw, 40px);
}

.hero__subtitle {
  margin: 0;
  color: #537364;
  font-size: 15px;
}

.hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
  color: #6d8679;
  font-size: 13px;
}

.resident-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.resident-list span {
  padding: 7px 12px;
  border-radius: 999px;
  background: #f4f9f6;
  color: #2b6046;
  font-size: 13px;
}

.hero__score-card {
  display: grid;
  align-content: center;
  gap: 10px;
  padding: 22px;
  border-radius: 18px;
  border: 1px solid rgba(214, 229, 220, 0.95);
  background:
    radial-gradient(circle at top right, rgba(145, 209, 171, 0.1), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 251, 249, 0.96) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    0 14px 28px rgba(31, 63, 47, 0.08);
  color: #244536;
}

.hero__score-card small,
.hero__score-card span,
.hero__score-note {
  color: #5f776b;
}

.hero__score-card strong {
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1;
}

.hero__score-note {
  font-size: 13px;
}

.metric-grid,
.dual-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dual-grid--compact {
  gap: 14px;
}

.metric-card,
.panel {
  padding: 18px;
}

.metric-card {
  display: grid;
  gap: 10px;
}

.metric-card__label,
.metric-card__note,
.ranking-card small,
.detail-item span,
.benchmark-item small,
.rule-text,
.rule-note,
.suggestion-item p,
.legend-item__text small {
  color: #667f72;
}

.metric-card__value,
.detail-item strong,
.benchmark-item strong,
.ranking-card strong,
.suggestion-item strong,
.legend-item__text strong {
  color: #204132;
}

.metric-card__value {
  font-size: 30px;
  line-height: 1.1;
}

.panel__head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.panel__head h2 {
  font-size: 22px;
}

.ranking-grid,
.benchmark-list,
.suggestion-list {
  display: grid;
  gap: 12px;
}

.ranking-card,
.benchmark-item,
.suggestion-item,
.detail-item,
.pie-card {
  border-radius: 16px;
  border: 1px solid #e4eee8;
  background: #f9fcfa;
}

.ranking-card {
  padding: 14px;
}

.ranking-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.ranking-card__head strong {
  font-size: 22px;
}

.progress {
  height: 10px;
  border-radius: 999px;
  background: #e7f0ea;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress__bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2f7d55 0%, #7bc49b 100%);
}

.pie-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.pie-card {
  padding: 16px;
}

.pie-card__title {
  color: #325a46;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
}

.pie-visual {
  display: grid;
  place-items: center;
  margin-bottom: 16px;
}

.donut {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
}

.donut::after {
  content: "";
  position: absolute;
  inset: 20px;
  border-radius: 50%;
  background: #f9fcfa;
  box-shadow: inset 0 0 0 1px #e8f0ea;
}

.donut__center {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 6px;
  text-align: center;
  padding: 0 14px;
}

.donut__center small {
  color: #6a8376;
  font-size: 12px;
}

.donut__center strong {
  color: #1f3f31;
  font-size: 22px;
  line-height: 1.2;
}

.legend-list {
  display: grid;
  gap: 10px;
}

.legend-item {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.legend-item__dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  margin-top: 4px;
}

.legend-item__text {
  display: grid;
  gap: 2px;
}

.detail-grid,
.rule-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid--summary {
  margin-top: 2px;
}

.detail-item {
  padding: 14px;
  display: grid;
  gap: 8px;
}

.detail-item strong {
  font-size: 20px;
}

.trend-chart {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(88px, 1fr));
  gap: 12px;
  align-items: end;
  min-height: 260px;
}

.trend-bar {
  display: grid;
  justify-items: center;
  gap: 8px;
}

.trend-bar strong {
  color: #1f4c37;
  font-size: 18px;
}

.trend-bar span,
.trend-bar small {
  color: #688376;
  text-align: center;
}

.trend-bar__track {
  width: 100%;
  max-width: 72px;
  height: 168px;
  padding: 6px;
  border-radius: 18px;
  background: linear-gradient(180deg, #eef5f0 0%, #e4efe8 100%);
  display: flex;
  align-items: end;
}

.trend-bar__fill {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(180deg, #91d1ab 0%, #2f7d55 100%);
}

.benchmark-item,
.suggestion-item {
  padding: 14px;
}

.benchmark-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.benchmark-item__metrics {
  display: grid;
  justify-items: end;
  gap: 6px;
  color: #315743;
  font-size: 13px;
}

.rule-text,
.rule-note {
  margin: 8px 0 0;
  line-height: 1.7;
}

.panel--suggestions .panel__head {
  margin-bottom: 10px;
}

.suggestion-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.suggestion-item__head span {
  color: #3c7057;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .hero,
  .dual-grid,
  .metric-grid,
  .detail-grid,
  .rule-grid,
  .pie-section {
    grid-template-columns: 1fr;
  }

  .hero__score-card {
    min-height: 180px;
  }

  .trend-chart {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .dashboard-page {
    padding: 16px 12px 28px;
  }

  .hero,
  .panel,
  .metric-card {
    border-radius: 16px;
  }

  .trend-chart {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .benchmark-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .benchmark-item__metrics {
    justify-items: start;
  }

  .donut {
    width: 156px;
    height: 156px;
  }

  .donut::after {
    inset: 18px;
  }
}
</style>


