<template>
  <section class="dorm-page panel" v-loading="loading">
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <template v-else-if="dorm">
      <section class="hero card">
        <div>
          <div class="eyebrow">宿舍管理 / 详情</div>
          <h1>{{ dorm.label }}</h1>
          <p>楼栋：{{ dorm.building }} · 宿舍ID：{{ dorm.dormId }}</p>
        </div>
        <div class="hero-actions">
          <el-button @click="goOverview">返回总览</el-button>
          <el-button type="primary" @click="goFeeDeduct">前往扣费管理</el-button>
        </div>
      </section>

      <section class="stats">
        <article class="stat card">
          <span>当前积分</span>
          <strong>{{ formatScore(dorm.carbonScore) }}</strong>
        </article>
        <article class="stat card">
          <span>周期扣费</span>
          <strong>{{ formatCurrency(dorm.totalFee) }}</strong>
        </article>
        <article class="stat card">
          <span>电费扣费</span>
          <strong>{{ formatCurrency(dorm.electricFee) }}</strong>
        </article>
        <article class="stat card">
          <span>水费扣费</span>
          <strong>{{ formatCurrency(dorm.waterFee) }}</strong>
        </article>
        <article class="stat card">
          <span>总碳排放</span>
          <strong>{{ formatCarbon(dorm.totalCarbon) }}</strong>
        </article>
        <article class="stat card">
          <span>排名</span>
          <strong>能耗 {{ dorm.energyRank }} / 积分 {{ dorm.carbonRank }}</strong>
        </article>
      </section>

      <section class="detail-grid">
        <article class="card panel-card">
          <h2>用量明细</h2>
          <div class="detail-list">
            <div><span>电量折算</span><strong>{{ formatNumber(dorm.electricUsage, 2) }}</strong></div>
            <div><span>水量折算</span><strong>{{ formatNumber(dorm.waterUsage, 2) }}</strong></div>
            <div><span>电碳排放</span><strong>{{ formatCarbon(dorm.electricCarbon) }}</strong></div>
            <div><span>水碳排放</span><strong>{{ formatCarbon(dorm.waterCarbon) }}</strong></div>
          </div>
        </article>

        <article class="card panel-card">
          <h2>全局信息</h2>
          <div class="detail-list">
            <div><span>周期标签</span><strong>{{ dashboard?.periodLabel || '-' }}</strong></div>
            <div><span>周期开始</span><strong>{{ formatDateTime(dashboard?.rangeStart) }}</strong></div>
            <div><span>周期结束</span><strong>{{ formatDateTime(dashboard?.rangeEnd) }}</strong></div>
            <div><span>最后更新</span><strong>{{ formatDateTime(dashboard?.lastUpdatedAt) }}</strong></div>
          </div>
        </article>

        <article class="card panel-card">
          <h2>入住学生</h2>
          <div class="resident-summary">
            <span>当前人数</span>
            <strong>{{ dorm.residentCount }}</strong>
          </div>
          <div v-if="dorm.residents.length" class="resident-list">
            <span v-for="resident in dorm.residents" :key="resident" class="resident-chip">{{ resident }}</span>
          </div>
          <p v-else class="resident-empty">当前宿舍还没有入住学生数据</p>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchGlobalDashboard, type DormItem, type GlobalDashboardData } from '@/api/modules/dashboard'
import { formatCarbon, formatCurrency, formatDateTime, formatNumber, formatScore } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const dashboard = ref<GlobalDashboardData | null>(null)

const dormId = computed(() => Number(route.params.dormId))
const dorm = computed<DormItem | null>(() => {
  if (!dashboard.value || !Number.isFinite(dormId.value)) return null
  return dashboard.value.dorms.find((item) => item.dormId === dormId.value) || null
})

function goOverview() {
  router.push('/manager/low-carbon-overview')
}

function goFeeDeduct() {
  router.push(`/manager/dorm-fee-deduct?dormId=${dormId.value}`)
}

async function loadDormDetail() {
  if (!Number.isFinite(dormId.value)) {
    errorMessage.value = '宿舍ID无效'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchGlobalDashboard({ dormId: dormId.value })
    if (data.code !== 200 || !data.data) {
      errorMessage.value = data.msg || '获取宿舍详情失败'
      dashboard.value = null
      return
    }
    dashboard.value = data.data
    if (!dorm.value) {
      errorMessage.value = '未找到对应宿舍详情'
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = '获取宿舍详情失败，请稍后重试'
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadDormDetail)
</script>

<style scoped>
.panel {
  display: grid;
  gap: 18px;
}

.hero {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.eyebrow {
  color: #5f776b;
  font-size: 14px;
}

.hero h1 {
  margin: 10px 0 8px;
  color: #244536;
  font-size: 36px;
}

.hero p {
  margin: 0;
  color: #5f776b;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stat {
  padding: 18px;
  display: grid;
  gap: 10px;
}

.stat span {
  color: #5f776b;
}

.stat strong {
  color: #244536;
  font-size: 28px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel-card {
  padding: 20px;
}

.panel-card h2 {
  margin: 0 0 16px;
  color: #244536;
}

.detail-list {
  display: grid;
  gap: 12px;
}

.detail-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5efe9;
}

.detail-list span {
  color: #5f776b;
}

.detail-list strong {
  color: #244536;
}

.resident-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  color: #5f776b;
}

.resident-summary strong {
  color: #244536;
  font-size: 24px;
}

.resident-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.resident-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: #edf5ef;
  color: #244536;
  font-weight: 600;
}

.resident-empty {
  margin: 0;
  color: #7c9085;
}

@media (max-width: 900px) {
  .hero,
  .detail-grid,
  .stats {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }
}
</style>
