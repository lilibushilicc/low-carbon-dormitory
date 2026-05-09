<template>
  <div v-if="rewardList.length" class="reward-grid">
    <div v-for="item in rewardList" :key="item.rewardId" class="reward-card">
      <div class="reward-image-wrap">
        <img :src="safeImageUrl(item.imageUrl)" :alt="item.rewardName" class="reward-image" />
        <span class="reward-stock">库存 {{ item.stock }}</span>
      </div>

      <div class="reward-body">
        <div class="reward-title-row">
          <h3>{{ item.rewardName }}</h3>
          <el-tag type="success" effect="light">{{ item.pointsCost }} 积分</el-tag>
        </div>

        <p class="reward-desc">{{ item.rewardDesc }}</p>

        <div class="reward-footer">
          <div class="reward-meta">
            <span>当前个人积分：{{ safeCurrentPoints }}</span>
            <small :class="{ disabled: !item.canExchange }">{{ item.exchangeTip }}</small>
          </div>
          <el-button
            type="primary"
            :disabled="!item.canExchange || submittingRewardId === item.rewardId"
            :loading="submittingRewardId === item.rewardId"
            @click="$emit('exchange', item)"
          >
            立即兑换
          </el-button>
        </div>
      </div>
    </div>
  </div>

  <el-empty v-else description="当前没有可兑换奖励" />
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface RewardItem {
  rewardId: number
  rewardName: string
  rewardDesc: string
  pointsCost: number
  imageUrl: string
  stock: number
  canExchange: boolean
  exchangeTip: string
}

const props = defineProps<{
  rewardList: RewardItem[]
  currentPoints: number
  submittingRewardId: number | null
}>()

const safeCurrentPoints = computed(() => {
  return typeof props.currentPoints === 'number' && Number.isFinite(props.currentPoints) ? props.currentPoints : 0
})

function safeImageUrl(imageUrl?: string) {
  const fallback =
    'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=900&q=80'
  return imageUrl && imageUrl.trim() ? imageUrl : fallback
}

defineEmits<{
  exchange: [reward: RewardItem]
}>()
</script>

<style scoped>
.reward-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 18px;
}

.reward-card {
  overflow: hidden;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e6efe9;
  box-shadow: 0 10px 24px rgba(32, 67, 52, 0.08);
  display: flex;
  flex-direction: column;
}

.reward-image-wrap {
  position: relative;
  height: 180px;
  background: #f3f8f5;
}

.reward-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reward-stock {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(20, 39, 30, 0.72);
  color: #fff;
  font-size: 12px;
}

.reward-body {
  padding: 18px;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
}

.reward-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.reward-title-row h3 {
  margin: 0;
  color: #244536;
  font-size: 18px;
}

.reward-desc {
  margin: 0;
  color: #60776b;
  line-height: 1.6;
  min-height: 44px;
}

.reward-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.reward-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #5f776b;
}

.reward-meta small {
  color: #1f7a51;
}

.reward-meta small.disabled {
  color: #d96c52;
}

@media (max-width: 768px) {
  .reward-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>


