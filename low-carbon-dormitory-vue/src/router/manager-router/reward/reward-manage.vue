<template>
  <section class="dorm-page panel" v-loading="loading">
    <section class="hero-card">
      <div class="hero-card__head">
        <div>
          <div class="hero-card__eyebrow">宿舍管理 / 运营管理</div>
          <h1>奖励与库存管理</h1>
          <p>统一维护水电费单价、奖励商品、库存与兑换配置。</p>
        </div>
        <div class="actions">
          <el-button @click="goHome">返回管理首页</el-button>
          <el-button @click="loadAll">刷新数据</el-button>
        </div>
      </div>
    </section>

    <section class="content-card">
      <h2>水电费单价配置</h2>
      <div class="rate-grid">
        <div v-for="rate in rates" :key="rate.feeType" class="rate-item">
          <h3>{{ rate.feeType }}</h3>
          <el-form label-position="top">
            <el-form-item label="单价">
              <el-input-number v-model="rate.unitPrice" :min="0.0001" :precision="4" :step="0.0001" />
            </el-form-item>
            <el-form-item label="单位名称">
              <el-input v-model="rate.unitName" />
            </el-form-item>
            <el-form-item label="计费开关">
              <el-switch v-model="rate.enabled" active-text="参与计费" inactive-text="不计费" />
            </el-form-item>
            <el-button type="primary" @click="saveRate(rate)">保存单价</el-button>
          </el-form>
        </div>
      </div>
    </section>

    <section class="content-card">
      <h2>新增奖励</h2>
      <el-form label-position="top">
        <div class="reward-grid">
          <el-form-item label="奖励名称">
            <el-input v-model="rewardForm.rewardName" />
          </el-form-item>
          <el-form-item label="奖励描述">
            <el-input v-model="rewardForm.rewardDesc" />
          </el-form-item>
          <el-form-item label="积分消耗">
            <el-input-number v-model="rewardForm.pointsCost" :min="0" />
          </el-form-item>
          <el-form-item label="库存">
            <el-input-number v-model="rewardForm.stock" :min="0" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="rewardForm.sortOrder" :min="0" />
          </el-form-item>
          <el-form-item label="图片URL(可选)">
            <el-input v-model="rewardForm.imageUrl" />
          </el-form-item>
        </div>
        <el-button type="primary" @click="createReward">创建奖励</el-button>
      </el-form>
    </section>

    <section class="content-card">
      <h2>奖励库存修改</h2>
      <el-table :data="rewards" border>
        <el-table-column prop="rewardId" label="ID" width="80" />
        <el-table-column prop="rewardName" label="奖励名称" min-width="160" />
        <el-table-column prop="pointsCost" label="所需积分" width="110" />
        <el-table-column label="库存" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" :step="1" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text @click="saveStock(row)">保存库存</el-button>
            <el-button type="danger" text @click="removeReward(row)">删除奖品</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createRewardByAdmin,
  deleteRewardByAdmin,
  fetchRewardsByAdmin,
  fetchUtilityRates,
  updateRewardStockByAdmin,
  updateUtilityRate,
  type AdminRewardItem,
  type UtilityRateItem,
} from '@/api/modules/admin'

const router = useRouter()
const loading = ref(false)
const rates = ref<UtilityRateItem[]>([])
const rewards = ref<AdminRewardItem[]>([])
const rewardForm = reactive({
  rewardName: '',
  rewardDesc: '',
  pointsCost: 20,
  stock: 10,
  sortOrder: 100,
  imageUrl: '',
})

function goHome() {
  router.push('/manager/home')
}

async function loadAll() {
  loading.value = true
  try {
    const [ratesRes, rewardsRes] = await Promise.all([fetchUtilityRates(), fetchRewardsByAdmin()])
    if (ratesRes.data.code === 200 && ratesRes.data.data) {
      rates.value = ratesRes.data.data.map((item) => ({ ...item, enabled: item.enabled !== false }))
    }
    if (rewardsRes.data.code === 200 && rewardsRes.data.data) {
      rewards.value = rewardsRes.data.data.map((item) => ({ ...item }))
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载管理数据失败')
  } finally {
    loading.value = false
  }
}

async function saveRate(item: UtilityRateItem) {
  try {
    const { data } = await updateUtilityRate(item.feeType, {
      unitPrice: Number(item.unitPrice),
      unitName: item.unitName,
      enabled: item.enabled !== false,
    })
    if (data.code !== 200) {
      ElMessage.error(data.msg || `保存${item.feeType}单价失败`)
      return
    }
    ElMessage.success(`${item.feeType}单价已保存`)
  } catch (error) {
    console.error(error)
    ElMessage.error('保存单价失败')
  }
}

async function createReward() {
  if (!rewardForm.rewardName.trim() || !rewardForm.rewardDesc.trim()) {
    ElMessage.error('请填写奖励名称和描述')
    return
  }

  try {
    const { data } = await createRewardByAdmin({
      rewardName: rewardForm.rewardName.trim(),
      rewardDesc: rewardForm.rewardDesc.trim(),
      pointsCost: rewardForm.pointsCost,
      stock: rewardForm.stock,
      sortOrder: rewardForm.sortOrder,
      imageUrl: rewardForm.imageUrl.trim() || undefined,
      status: 1,
    })

    if (data.code !== 200) {
      ElMessage.error(data.msg || '新增奖励失败')
      return
    }

    ElMessage.success('奖励新增成功')
    rewardForm.rewardName = ''
    rewardForm.rewardDesc = ''
    rewardForm.imageUrl = ''
    await loadAll()
  } catch (error) {
    console.error(error)
    ElMessage.error('新增奖励失败')
  }
}

async function saveStock(item: AdminRewardItem) {
  try {
    const { data } = await updateRewardStockByAdmin(item.rewardId, item.stock)
    if (data.code !== 200) {
      ElMessage.error(data.msg || '库存更新失败')
      return
    }
    ElMessage.success('库存更新成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('库存更新失败')
  }
}

async function removeReward(item: AdminRewardItem) {
  try {
    await ElMessageBox.confirm(`确认删除奖品“${item.rewardName}”吗？删除后将不能恢复。`, '删除奖品', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    const { data } = await deleteRewardByAdmin(item.rewardId)
    if (data.code !== 200) {
      ElMessage.error(data.msg || '删除奖品失败')
      return
    }
    ElMessage.success('奖品已删除')
    await loadAll()
  } catch (error) {
    console.error(error)
    ElMessage.error('删除奖品失败')
  }
}

loadAll()
</script>

<style scoped>
.panel {
  display: grid;
  gap: 18px;
}

.hero-card,
.content-card {
  padding: 24px;
}

.hero-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.hero-card h1 {
  margin: 12px 0 8px;
  color: #244536;
}

.hero-card p,
h2 {
  color: #244536;
}

.hero-card p {
  margin: 0;
  color: #5f776b;
}

h2 {
  margin: 0 0 12px;
}

.rate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.reward-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.rate-item {
  border: 1px solid #e4ece8;
  border-radius: 12px;
  padding: 12px;
}

.rate-item :deep(.el-switch) {
  --el-switch-on-color: #4b9b6e;
}

@media (max-width: 1000px) {
  .rate-grid,
  .reward-grid {
    grid-template-columns: 1fr;
  }

  .hero-card__head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
