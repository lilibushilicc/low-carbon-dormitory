<template>
  <div class="page">
    <div class="panel" v-loading="loading">
      <div class="header">
        <div>
        <h1>宿舍低碳积分说明</h1>
        <p>查看当前低碳积分计算公式与荣誉判定规则。</p>
        </div>
        <el-button @click="goBack">返回</el-button>
      </div>

      <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />

      <template v-else-if="config">
        <section class="grid">
          <article class="card">
            <div class="tag">评分规则</div>
        <h2>积分计算公式</h2>
            <div class="stats">
        <div><label>基础分</label><strong>{{ config.scoreRule.baseScore }}</strong></div>
        <div><label>电碳排系数</label><strong>{{ config.scoreRule.electricCarbonFactor }}</strong></div>
        <div><label>水碳排系数</label><strong>{{ config.scoreRule.waterCarbonFactor }}</strong></div>
              <div><label>扣分系数</label><strong>{{ config.scoreRule.carbonPenaltyFactor }}</strong></div>
            </div>
            <p class="formula">{{ config.scoreRule.formulaText }}</p>
            <p>{{ config.scoreRule.weeklyDescription }}</p>
            <p>{{ config.scoreRule.monthlyDescription }}</p>
            <p>{{ config.scoreRule.rankingUpdateNote }}</p>
          </article>

          <article class="card">
        <div class="tag">荣誉判定</div>
            <h2>排名区间规则</h2>
            <div class="rule-list">
              <div v-for="item in config.honorRules" :key="ruleKey(item)" class="rule-item">
                <strong>{{ periodText(item.periodType) }} / {{ scopeText(item.scopeType) }}</strong>
                <span>{{ item.honorTitle }}</span>
                <small>{{ rankText(item.rankType, item.rankValue) }}</small>
              </div>
            </div>
          </article>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentLowCarbonRules, type HonorRuleConfig, type LowCarbonRuleConfig } from '@/api/modules/low-carbon-rule'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const config = ref<LowCarbonRuleConfig | null>(null)

function goBack() {
  router.back()
}

function periodText(value: string) {
  return value === 'monthly' ? '月榜' : '周榜'
}

function scopeText(value: string) {
  if (value === 'building') return '楼栋'
  if (value === 'college') return '学院'
  return '学校'
}

function rankText(type: string, value: number) {
  return type === 'RANK' ? `第 ${value} 名` : `前 ${value}%`
}

function ruleKey(item: HonorRuleConfig) {
  return `${item.periodType}-${item.scopeType}-${item.honorTitle}-${item.sortOrder}`
}

async function loadRules() {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchStudentLowCarbonRules()
    if (data.code !== 200 || !data.data) {
      errorMessage.value = data.msg || '获取积分规则失败'
      return
    }
    config.value = data.data
  } catch (error) {
    console.error(error)
    errorMessage.value = '获取积分规则失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(loadRules)
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px 16px 40px;
  background: linear-gradient(180deg, #eef6f1 0%, #f8fbf9 100%);
}

.panel {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 20px 50px rgba(36, 69, 54, 0.1);
}

.header,
.grid,
.stats,
.rule-list {
  display: grid;
  gap: 16px;
}

.header {
  grid-template-columns: 1fr auto;
  align-items: start;
  margin-bottom: 24px;
}

.grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.card,
.rule-item {
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e6efe9;
}

.card {
  padding: 20px;
}

.tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(21, 128, 61, 0.1);
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.stats {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 16px 0;
}

.stats div,
.rule-item {
  padding: 14px 16px;
  background: #f7fbf8;
  border-radius: 16px;
}

.stats label,
.rule-item small,
.card p {
  display: block;
  color: #5f776b;
}

.formula {
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #eef7f1;
  color: #244536;
}

@media (max-width: 900px) {
  .header,
  .grid,
  .stats {
    grid-template-columns: 1fr;
  }
}
</style>


