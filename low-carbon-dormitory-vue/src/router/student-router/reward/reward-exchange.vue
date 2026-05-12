<template>
  <div class="page">
    <div class="panel" v-loading="loading">
      <div class="header">
        <div>
          <h1>奖励兑换中心</h1>
          <p>使用个人低碳积分兑换奖励，兑换后请等待发放。</p>
        </div>
        <div class="header-actions">
          <el-button @click="goBack">返回首页</el-button>
          <el-button type="primary" plain @click="loadRewardCenter">刷新数据</el-button>
        </div>
      </div>

      <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />

      <template v-else-if="rewardCenter">
        <section class="summary-grid">
          <article class="summary-card summary-card--main">
            <span>当前个人积分</span>
            <strong>{{ currentPoints }}</strong>
            <small>积分越高，可兑换的奖励越多</small>
          </article>

          <article class="summary-card">
            <span>最近可兑换奖励</span>
            <strong>{{ rewardCenter.dormScoreSummary.nearestRewardName || '暂无' }}</strong>
            <small>还差 {{ rewardCenter.dormScoreSummary.gapToNearestReward || 0 }} 积分</small>
          </article>

          <article class="summary-card">
            <span>个人兑换记录</span>
            <strong>{{ rewardCenter.exchangeCount }}</strong>
            <small>仅展示当前学生的兑换记录</small>
          </article>
        </section>

        <el-tabs v-model="activeTab" class="tabs">
          <el-tab-pane label="奖励列表" name="rewards">
            <RewardExchangeSon
              :reward-list="rewardCenter.rewardItems"
              :current-points="currentPoints"
              :submitting-reward-id="submittingRewardId"
              @exchange="handleExchange"
            />
          </el-tab-pane>

          <el-tab-pane label="个人兑换记录" name="records">
            <div v-if="rewardCenter.exchangeRecords.length" class="record-list">
              <div v-for="record in rewardCenter.exchangeRecords" :key="record.recordId" class="record-item">
                <div>
                  <strong>{{ record.rewardName }}</strong>
                  <p>{{ record.remark || '个人积分兑换成功，等待发放。' }}</p>
                </div>
                <div class="record-meta">
                  <span>消耗 {{ record.exchangePoints }} 积分</span>
                  <span>{{ formatDateTime(record.exchangeTime) }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无兑换记录" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import RewardExchangeSon from './reward-exchange-son.vue'
import type { RewardItem } from '@/api/modules/reward'
import { formatDateTime } from '@/utils/formatters'
import { resolveErrorMessage } from '@/utils/api-response'
import { useRewardCenter } from './use-reward-center'

const router = useRouter()
const activeTab = ref('rewards')
const {
  loading,
  errorMessage,
  rewardCenter,
  submittingRewardId,
  currentPoints,
  loadRewardCenter,
  exchangeRewardById,
} = useRewardCenter()

function goBack() {
  router.push('/index-student')
}

async function handleExchange(reward: RewardItem) {
  if (!reward.canExchange || submittingRewardId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认使用个人积分兑换“${reward.rewardName}”吗？本次会扣除 ${reward.pointsCost} 积分。`,
      '确认兑换',
      {
        type: 'warning',
        confirmButtonText: '立即兑换',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  try {
    const result = await exchangeRewardById(reward.rewardId)
    ElMessage.success(`兑换成功，剩余 ${result.remainingPoints} 积分`)
    await loadRewardCenter()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '兑换失败，请稍后重试'))
  }
}

onMounted(loadRewardCenter)
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
  box-shadow: 0 18px 40px rgba(36, 69, 54, 0.1);
}

.header,
.header-actions {
  display: flex;
  gap: 12px;
}

.header {
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.header h1 {
  margin: 0 0 8px;
  color: #244536;
  font-size: 28px;
}

.header p {
  margin: 0;
  color: #5f776b;
  line-height: 1.7;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0;
}

.summary-card {
  border: 1px solid #e5efe9;
  border-radius: 16px;
  padding: 14px;
  background: #fcfefd;
}

.summary-card--main {
  background: linear-gradient(135deg, #2b6a4f 0%, #5ea982 100%);
  color: #fff;
}

.summary-card strong {
  display: block;
  margin: 8px 0;
  font-size: 24px;
}

.summary-card small {
  color: inherit;
  opacity: 0.9;
}

.record-list {
  display: grid;
  gap: 10px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e5efe9;
  border-radius: 12px;
  padding: 12px;
}

.record-item p {
  margin: 6px 0 0;
  color: #5f776b;
}

.record-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #5f776b;
}

:deep(.el-button) {
  min-height: 42px;
  border-radius: 12px;
  border-color: #d6e5dc;
  font-weight: 600;
}

:deep(.el-tabs__item.is-active) {
  color: #244536;
}

:deep(.el-tabs__active-bar) {
  background-color: #244536;
}

@media (max-width: 1000px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .record-item {
    flex-direction: column;
  }
}
</style>
