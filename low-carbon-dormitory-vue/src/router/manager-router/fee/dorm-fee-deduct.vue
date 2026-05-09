<template>
  <section class="dorm-page panel">
    <section class="hero-card">
      <div class="hero-card__head">
        <div>
          <div class="hero-card__eyebrow">宿舍管理 / 运营管理</div>
          <h1>宿舍水电扣费</h1>
          <p>按宿舍 ID 扣减水费或电费，扣减后会自动写入费用流水。</p>
        </div>
        <el-button @click="goHome">返回管理首页</el-button>
      </div>
    </section>

    <section class="content-card">
      <el-form label-position="top" class="deduct-form">
        <div class="form-grid">
          <el-form-item label="宿舍ID">
            <el-input-number v-model="deductForm.dormId" :min="1" :step="1" :precision="0" />
          </el-form-item>

          <el-form-item label="费用类型">
            <el-select v-model="deductForm.feeType">
              <el-option v-for="item in feeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="扣减金额">
            <el-input-number v-model="deductForm.amount" :min="0.01" :step="0.1" :precision="2" />
          </el-form-item>

          <el-form-item label="支付类型">
            <el-select v-model="deductForm.payType">
              <el-option v-for="item in payTypeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item label="付款人姓名（可选）">
            <el-input v-model="deductForm.payerName" />
          </el-form-item>

          <el-form-item label="付款账号（可选）">
            <el-input v-model="deductForm.payerAccount" />
          </el-form-item>
        </div>

        <el-form-item label="备注（可选）">
          <el-input v-model="deductForm.remark" />
        </el-form-item>

        <div class="deduct-actions">
          <el-button type="danger" :loading="deductLoading" @click="submitDeduct">确认扣减</el-button>
        </div>
      </el-form>

      <div v-if="lastResult" class="result">
        <strong>最新余额：</strong>
        <span>宿舍ID {{ lastResult.dormId }}</span>
        <span>电费 {{ Number(lastResult.electricityBalance).toFixed(2) }}</span>
        <span>水费 {{ Number(lastResult.waterBalance).toFixed(2) }}</span>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { deductDormFeeByAdmin, type DormFeeInfo } from '@/api/modules/admin'

const router = useRouter()
const route = useRoute()
const deductLoading = ref(false)
const lastResult = ref<DormFeeInfo | null>(null)
const feeTypeOptions = [
  { label: '电费 (ELECTRIC)', value: 'ELECTRIC' },
  { label: '水费 (WATER)', value: 'WATER' },
] as const
const payTypeOptions = ['SYSTEM', 'ALIPAY', 'WECHAT', 'CASH'] as const

const deductForm = reactive({
  dormId: Number(route.query.dormId) > 0 ? Number(route.query.dormId) : 1,
  feeType: 'ELECTRIC' as 'ELECTRIC' | 'WATER',
  amount: 1,
  payType: 'SYSTEM' as 'SYSTEM' | 'ALIPAY' | 'WECHAT' | 'CASH',
  payerName: '',
  payerAccount: '',
  remark: '',
})

function goHome() {
  router.push('/manager/home')
}

async function submitDeduct() {
  if (!deductForm.dormId || deductForm.dormId < 1) {
    ElMessage.error('请填写正确的宿舍ID')
    return
  }
  if (!deductForm.amount || deductForm.amount <= 0) {
    ElMessage.error('扣减金额必须大于 0')
    return
  }

  deductLoading.value = true
  try {
    const { data } = await deductDormFeeByAdmin(deductForm.dormId, buildDeductPayload())

    if (data.code !== 200 || !data.data) {
      ElMessage.error(data.msg || '扣减失败')
      return
    }

    lastResult.value = data.data
    ElMessage.success('扣减成功')
  } catch (error) {
    console.error('扣减宿舍水电失败:', error)
    ElMessage.error('扣减失败，请检查余额或参数')
  } finally {
    deductLoading.value = false
  }
}

function buildDeductPayload() {
  return {
    amount: Number(deductForm.amount),
    feeType: deductForm.feeType,
    payType: deductForm.payType,
    payerName: deductForm.payerName.trim() || undefined,
    payerAccount: deductForm.payerAccount.trim() || undefined,
    remark: deductForm.remark.trim() || undefined,
  }
}
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
  align-items: flex-start;
  gap: 12px;
}

.hero-card h1 {
  margin: 12px 0 8px;
  color: #244536;
}

.hero-card p {
  margin: 0;
  color: #5f776b;
}

.deduct-form {
  margin-top: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.deduct-actions {
  margin-top: 4px;
}

.result {
  margin-top: 12px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: #2f4f3f;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .hero-card__head {
    flex-direction: column;
  }
}
</style>
