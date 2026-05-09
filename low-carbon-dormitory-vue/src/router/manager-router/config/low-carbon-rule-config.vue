<template>
  <section class="dorm-page panel" v-loading="loading">
    <section class="hero-card">
      <div class="hero-card__head">
        <div>
          <div class="hero-card__eyebrow">宿舍管理 / 基础配置</div>
          <h1>低碳积分规则配置</h1>
          <p>配置宿舍低碳积分、碳排放折算和荣誉规则。</p>
        </div>
        <div class="header__actions">
          <el-button @click="loadConfig">刷新</el-button>
          <el-button type="primary" @click="saveConfig">保存规则</el-button>
        </div>
      </div>
    </section>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />

    <template v-if="form">
      <section class="grid">
        <article class="content-card">
          <div class="tag">评分标准配置</div>
          <el-form label-position="top">
            <div class="form-grid">
              <el-form-item label="基础分">
                <el-input-number v-model="form.scoreRule.baseScore" :min="0" :step="1" />
              </el-form-item>
              <el-form-item label="电碳排系数">
                <el-input-number v-model="form.scoreRule.electricCarbonFactor" :min="0" :step="0.0001" :precision="4" />
              </el-form-item>
              <el-form-item label="水碳排系数">
                <el-input-number v-model="form.scoreRule.waterCarbonFactor" :min="0" :step="0.0001" :precision="4" />
              </el-form-item>
              <el-form-item label="碳排扣分系数">
                <el-input-number v-model="form.scoreRule.carbonPenaltyFactor" :min="0" :step="0.1" :precision="2" />
              </el-form-item>
            </div>
            <el-form-item label="周积分说明">
              <el-input v-model="form.scoreRule.weeklyDescription" />
            </el-form-item>
            <el-form-item label="月积分说明">
              <el-input v-model="form.scoreRule.monthlyDescription" />
            </el-form-item>
            <el-form-item label="排名更新说明">
              <el-input v-model="form.scoreRule.rankingUpdateNote" />
            </el-form-item>
          </el-form>
        </article>

        <article class="content-card">
          <div class="tag">规则预览</div>
          <div class="form-grid">
            <el-form-item label="示例电费">
              <el-input-number v-model="previewForm.electricFee" :min="0" :step="1" />
            </el-form-item>
            <el-form-item label="示例水费">
              <el-input-number v-model="previewForm.waterFee" :min="0" :step="1" />
            </el-form-item>
          </div>
          <el-button type="primary" plain @click="runPreview">预览结果</el-button>

          <div v-if="preview" class="preview-box">
            <div class="preview-stats">
              <div><label>电量折算</label><strong>{{ preview.electricUsage }}</strong></div>
              <div><label>水量折算</label><strong>{{ preview.waterUsage }}</strong></div>
              <div><label>总碳排放</label><strong>{{ preview.totalCarbon }}</strong></div>
              <div><label>积分结果</label><strong>{{ preview.score }}</strong></div>
            </div>
            <p class="formula">{{ preview.formulaText }}</p>
            <div class="preview-rules">
              <div v-for="item in preview.honorPreviewTexts" :key="item" class="preview-rule">{{ item }}</div>
            </div>
          </div>
        </article>
      </section>

      <section class="content-card">
        <div class="table-header">
          <div>
            <div class="tag">荣誉规则配置</div>
            <h2>配置各范围的低碳荣誉</h2>
          </div>
          <el-button type="primary" plain @click="addRule">新增规则</el-button>
        </div>

        <el-table :data="form.honorRules" border>
          <el-table-column label="周期" min-width="110">
            <template #default="{ row }">
              <el-select v-model="row.periodType">
                <el-option label="周榜" value="weekly" />
                <el-option label="月榜" value="monthly" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="范围" min-width="120">
            <template #default="{ row }">
              <el-select v-model="row.scopeType">
                <el-option label="楼栋" value="building" />
                <el-option label="学院" value="college" />
                <el-option label="学校" value="school" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="荣誉名称" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.honorTitle" />
            </template>
          </el-table-column>
          <el-table-column label="徽章标识" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.badge" />
            </template>
          </el-table-column>
          <el-table-column label="判定方式" min-width="120">
            <template #default="{ row }">
              <el-select v-model="row.rankType">
                <el-option label="名次" value="RANK" />
                <el-option label="百分比" value="PERCENT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="阈值" min-width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.rankValue" :min="0" :step="1" />
            </template>
          </el-table-column>
          <el-table-column label="排序" min-width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :min="0" :step="10" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ $index }">
              <el-button text type="danger" @click="removeRule($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchLowCarbonRuleConfig,
  previewLowCarbonRule,
  updateLowCarbonRuleConfig,
  type HonorRuleConfig,
  type LowCarbonRuleConfig,
  type LowCarbonRulePreview,
} from '@/api/modules/low-carbon-rule'

const loading = ref(false)
const errorMessage = ref('')
const preview = ref<LowCarbonRulePreview | null>(null)
const previewForm = reactive({
  electricFee: 30,
  waterFee: 12,
})

const form = ref<LowCarbonRuleConfig | null>(null)

function createEmptyRule(): HonorRuleConfig {
  return {
    periodType: 'weekly',
    scopeType: 'building',
    honorTitle: '低碳先锋宿舍',
    badge: 'custom-badge',
    rankType: 'RANK',
    rankValue: 1,
    sortOrder: 999,
    status: 1,
  }
}

async function loadConfig() {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchLowCarbonRuleConfig()
    if (data.code !== 200 || !data.data) {
      errorMessage.value = data.msg || '获取配置失败'
      return
    }
    form.value = {
      scoreRule: { ...data.data.scoreRule },
      honorRules: data.data.honorRules.map((item) => ({ ...item })),
    }
    await runPreview()
  } catch (error) {
    console.error(error)
    errorMessage.value = '获取配置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function runPreview() {
  if (!form.value) return
  const { data } = await previewLowCarbonRule({
    electricFee: previewForm.electricFee,
    waterFee: previewForm.waterFee,
    scoreRule: form.value.scoreRule,
  })
  if (data.code === 200 && data.data) {
    preview.value = data.data
  }
}

async function saveConfig() {
  if (!form.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const payload: LowCarbonRuleConfig = {
      scoreRule: { ...form.value.scoreRule },
      honorRules: form.value.honorRules.map((item) => ({ ...item })),
    }
    const { data } = await updateLowCarbonRuleConfig(payload)
    if (data.code !== 200 || !data.data) {
      errorMessage.value = data.msg || '保存低碳积分规则失败'
      return
    }
    form.value = {
      scoreRule: { ...data.data.scoreRule },
      honorRules: data.data.honorRules.map((item) => ({ ...item })),
    }
    await runPreview()
    ElMessage.success('低碳积分规则已保存')
  } catch (error) {
    console.error(error)
    errorMessage.value = '保存配置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function addRule() {
  form.value?.honorRules.push(createEmptyRule())
}

function removeRule(index: number) {
  form.value?.honorRules.splice(index, 1)
}

loadConfig()
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

.hero-card__head,
.header__actions,
.grid,
.form-grid,
.preview-stats,
.preview-rules {
  display: grid;
  gap: 16px;
}

.hero-card__head {
  grid-template-columns: 1fr auto;
  align-items: start;
}

.hero-card h1 {
  margin: 12px 0 8px;
  color: #244536;
}

.hero-card p {
  margin: 0;
  color: #5f776b;
}

.header__actions {
  grid-auto-flow: column;
}

.grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.tag {
  margin-bottom: 12px;
}

.form-grid,
.preview-stats {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.preview-box {
  margin-top: 16px;
}

.preview-stats div,
.preview-rule {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f7fbf8;
}

.preview-stats label {
  display: block;
  color: #5f776b;
  font-size: 13px;
}

.preview-stats strong {
  color: #244536;
}

.formula {
  margin: 16px 0;
  padding: 14px 16px;
  border-radius: 16px;
  background: #eef7f1;
  color: #244536;
}

.table-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .hero-card__head,
  .grid,
  .form-grid,
  .preview-stats {
    grid-template-columns: 1fr;
  }

  .header__actions {
    grid-auto-flow: row;
  }
}
</style>
