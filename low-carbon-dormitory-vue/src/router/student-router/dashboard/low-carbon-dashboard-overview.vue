<template>
  <div class="board-page">
    <section class="board-content" v-loading="loading">
      <el-alert
        v-if="errorMessage"
        class="feedback-card"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />

      <el-empty
        v-else-if="!loading && !dashboard"
        class="feedback-card feedback-card--empty"
        description="当前暂无全局低碳看板数据"
      />

      <template v-else-if="dashboard">
        <section class="hero card">
          <div>
            <div class="hero__eyebrow">{{ props.adminMode ? '管理员低碳总览' : '宿舍低碳总览' }}</div>
            <h1>宿舍低碳积分看板</h1>
            <p>这里汇总展示当前宿舍排名、碳排放、扣费与入住学生信息，方便你快速查看每间宿舍的最新状态。</p>
          </div>
          <div class="hero__meta">
            <span>宿舍总数 {{ dashboard.overview.dormCount }}</span>
            <span>参排宿舍 {{ dashboard.overview.rankedDormCount }}</span>
            <span>最后更新 {{ formatDateTime(dashboard.lastUpdatedAt) }}</span>
          </div>
        </section>

        <section class="stats">
          <article class="stat card">
            <span>平均积分</span>
            <strong>{{ formatScore(dashboard.overview.averageScore) }}</strong>
            <small>按当前参排宿舍计算</small>
          </article>
          <article class="stat card">
            <span>总碳排放</span>
            <strong>{{ formatCarbon(dashboard.overview.totalCarbon) }}</strong>
            <small>展示最新累计结果</small>
          </article>
          <article class="stat card">
            <span>最佳宿舍</span>
            <strong>{{ dashboard.overview.bestDormLabel || '-' }}</strong>
            <small>当前排名第一</small>
          </article>
          <article class="stat card">
            <span>高碳排宿舍</span>
            <strong>{{ dashboard.overview.highestCarbonDormLabel || '-' }}</strong>
            <small>便于重点关注</small>
          </article>
        </section>

        <section class="toolbar card">
          <el-input v-model="searchKeyword" class="toolbar__search" placeholder="搜索宿舍或学生姓名" clearable />
          <el-select v-model="buildingFilter" class="toolbar__select">
            <el-option v-for="item in buildingOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="periodStatusFilter" class="toolbar__select">
            <el-option label="全部状态" value="all" />
            <el-option label="本周期参与排名" value="current" />
            <el-option label="仅展示历史结果" value="stale" />
          </el-select>
        </section>

        <section class="card-grid">
          <article
            v-for="dorm in topDorms"
            :key="dorm.dormId"
            class="dorm-card card"
            :class="{ 'dorm-card--stale': !dorm.currentPeriodParticipating }"
          >
            <div class="dorm-card__head">
              <strong>{{ dorm.label }}</strong>
              <span class="status-badge" :class="'status-badge--' + dorm.statusKey">{{ dorm.statusText }}</span>
            </div>

            <div class="dorm-card__period">
              <span class="period-badge" :class="{ 'period-badge--stale': !dorm.currentPeriodParticipating }">
                {{ dorm.currentPeriodParticipating ? '本周期参与排名' : '仅展示历史结果' }}
              </span>
            </div>

            <div class="dorm-card__metrics">
              <span>积分 {{ formatScore(dorm.carbonScore) }}</span>
              <span>扣费 {{ formatCurrency(dorm.totalFee) }}</span>
              <span>总碳 {{ formatCarbon(dorm.totalCarbon) }}</span>
              <span>人数 {{ dorm.residentCount }}</span>
            </div>

            <div class="resident-list" v-if="dorm.residents?.length">
              <span v-for="resident in dorm.residents" :key="resident">{{ resident }}</span>
            </div>

            <small>最后更新 {{ formatDateTime(dorm.dataUpdatedAt) }}</small>
            <button type="button" class="detail-link" @click="openDormDetail(dorm)">查看详情</button>
          </article>
        </section>

        <section class="panel card">
          <div class="panel__head">
            <div>
              <div class="panel__eyebrow">宿舍明细</div>
              <h2>排名与入住信息</h2>
            </div>
          </div>

          <el-table :data="filteredDorms" stripe>
            <el-table-column prop="label" label="宿舍" min-width="120" />
            <el-table-column prop="building" label="楼栋" min-width="90" />
            <el-table-column label="入住人数" min-width="100">
              <template #default="{ row }">{{ row.residentCount }}</template>
            </el-table-column>
            <el-table-column label="入住学生" min-width="240">
              <template #default="{ row }">{{ row.residents?.length ? row.residents.join('、') : '-' }}</template>
            </el-table-column>
            <el-table-column label="当前积分" min-width="120">
              <template #default="{ row }">{{ formatScore(row.carbonScore) }}</template>
            </el-table-column>
            <el-table-column label="周期扣费" min-width="120">
              <template #default="{ row }">{{ formatCurrency(row.totalFee) }}</template>
            </el-table-column>
            <el-table-column label="总碳排放" min-width="120">
              <template #default="{ row }">{{ formatCarbon(row.totalCarbon) }}</template>
            </el-table-column>
            <el-table-column label="状态" min-width="140">
              <template #default="{ row }">
                <span class="period-badge" :class="{ 'period-badge--stale': !row.currentPeriodParticipating }">
                  {{ row.currentPeriodParticipating ? '本周期参与排名' : '仅展示历史结果' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="最近更新" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.dataUpdatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="110" fixed="right">
              <template #default="{ row }">
                <button type="button" class="detail-link" @click="openDormDetail(row)">查看</button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { fetchGlobalDashboard, type DormItem, type GlobalDashboardData } from '@/api/modules/dashboard'
import { useStudentTokenStore } from '@/stores/student-token'
import { getDormStatus, type DormStatusKey } from '@/utils/dashboard-status'
import { formatCarbon, formatCurrency, formatDateTime, formatScore } from '@/utils/formatters'

type DashboardOverviewProps = {
  adminMode?: boolean
}

type DecoratedDorm = DormItem & {
  statusKey: DormStatusKey
  statusText: string
}

const props = withDefaults(defineProps<DashboardOverviewProps>(), {
  adminMode: false,
})

const loading = ref(false)
const errorMessage = ref('')
const dashboard = ref<GlobalDashboardData | null>(null)
const searchKeyword = ref('')
const buildingFilter = ref('all')
const periodStatusFilter = ref<'all' | 'current' | 'stale'>('all')

const studentTokenStore = useStudentTokenStore()
const router = useRouter()
const { stuNum, dormId } = storeToRefs(studentTokenStore)

const decoratedDorms = computed<DecoratedDorm[]>(() =>
  (dashboard.value?.dorms ?? []).map((dorm) => {
    const status = getDormStatus(dorm)
    return {
      ...dorm,
      statusKey: status.key,
      statusText: status.text,
    }
  }),
)

const buildingOptions = computed(() => {
  const buildings = Array.from(new Set((dashboard.value?.dorms ?? []).map((item) => item.building).filter(Boolean)))
  return [{ value: 'all', label: '全部楼栋' }, ...buildings.map((item) => ({ value: item, label: item }))]
})

function compareDormRanking(left: DecoratedDorm, right: DecoratedDorm) {
  if (left.currentPeriodParticipating !== right.currentPeriodParticipating) {
    return left.currentPeriodParticipating ? -1 : 1
  }
  if (left.currentPeriodParticipating && right.currentPeriodParticipating) {
    const leftRank = left.carbonRank || Number.MAX_SAFE_INTEGER
    const rightRank = right.carbonRank || Number.MAX_SAFE_INTEGER
    return leftRank - rightRank || left.totalCarbon - right.totalCarbon || left.label.localeCompare(right.label)
  }
  return right.carbonScore - left.carbonScore || left.totalCarbon - right.totalCarbon || left.label.localeCompare(right.label)
}

const filteredDorms = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return decoratedDorms.value
    .filter((item) => {
      const residentText = item.residents?.join(' ').toLowerCase() || ''
      const matchesKeyword =
        !keyword || item.label.toLowerCase().includes(keyword) || item.building.toLowerCase().includes(keyword) || residentText.includes(keyword)
      const matchesBuilding = buildingFilter.value === 'all' || item.building === buildingFilter.value
      const matchesPeriod =
        periodStatusFilter.value === 'all' ||
        (periodStatusFilter.value === 'current' && item.currentPeriodParticipating) ||
        (periodStatusFilter.value === 'stale' && !item.currentPeriodParticipating)
      return matchesKeyword && matchesBuilding && matchesPeriod
    })
    .sort(compareDormRanking)
})

const topDorms = computed(() => filteredDorms.value.slice(0, 6))

function openDormDetail(dorm: Pick<DecoratedDorm, 'dormId' | 'label'>) {
  if (props.adminMode && dorm.dormId) {
    router.push(`/manager/low-carbon-overview/dorm/${dorm.dormId}`)
    return
  }
  ElMessage.info(`当前版本暂未接入 ${dorm.label} 的宿舍详情跳转`)
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''

  try {
    const { data } = await fetchGlobalDashboard({
      stuNum: stuNum.value || undefined,
      dormId: dormId.value ? Number(dormId.value) : undefined,
    })

    if (data.code !== 200 || !data.data) {
      errorMessage.value = data.msg || '获取全局低碳看板失败'
      dashboard.value = null
      return
    }

    dashboard.value = data.data
  } catch (error) {
    console.error(error)
    errorMessage.value = '获取全局低碳看板失败，请稍后重试'
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.board-page {
  min-height: auto;
}

.board-content {
  min-width: 0;
  display: grid;
  gap: 18px;
}

.feedback-card--empty {
  padding: 36px 0;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 22px;
}

.hero h1,
.panel h2 {
  margin: 12px 0 8px;
  color: #244536;
}

.hero p,
.hero__meta {
  color: #5f776b;
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

.hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat {
  padding: 16px;
  display: grid;
  gap: 8px;
}

.stat span,
.stat small {
  color: #5f776b;
}

.stat strong {
  color: #244536;
  font-size: 24px;
}

.toolbar {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 180px 220px;
  gap: 12px;
}

.toolbar__search,
.toolbar__select {
  width: 100%;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.dorm-card {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.dorm-card--stale {
  background: #fcfcfa;
}

.dorm-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.dorm-card__head strong,
.panel h2 {
  color: #244536;
}

.dorm-card__metrics {
  display: grid;
  gap: 6px;
  color: #5f776b;
}

.dorm-card small {
  color: #5f776b;
  line-height: 1.6;
}

.resident-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.resident-list span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f2f8f4;
  color: #2d6d4a;
  font-size: 12px;
}

.panel {
  padding: 18px;
}

.panel__head {
  margin-bottom: 14px;
}

.status-badge,
.period-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.status-badge--excellent {
  background: rgba(46, 125, 67, 0.16);
  color: #2e7d43;
}

.status-badge--good {
  background: rgba(37, 97, 180, 0.14);
  color: #2561b4;
}

.status-badge--warning {
  background: rgba(180, 130, 22, 0.14);
  color: #a06d08;
}

.status-badge--abnormal {
  background: rgba(182, 73, 48, 0.14);
  color: #b64930;
}

.period-badge {
  background: rgba(42, 138, 91, 0.12);
  color: #2a8a5b;
}

.period-badge--stale {
  background: rgba(140, 120, 70, 0.12);
  color: #8b6f2e;
}

.detail-link {
  width: fit-content;
  border: 1px solid rgba(42, 138, 91, 0.28);
  background: #f7fbf8;
  color: #2a8a5b;
  border-radius: 10px;
  padding: 8px 12px;
  cursor: pointer;
}

@media (max-width: 1100px) {
  .stats,
  .card-grid,
  .toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .hero,
  .stats,
  .card-grid,
  .toolbar {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero__meta {
    justify-content: flex-start;
  }
}
</style>
