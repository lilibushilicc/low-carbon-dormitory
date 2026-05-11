<template>
  <div class="pay-up">
    <header class="top-bar">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <el-button link type="primary" @click="$router.push('/index-student')">回到首页</el-button>
    </header>

    <main class="main-content">
      <div class="pay-card">
        <h1>费用缴纳</h1>
        <p class="sub-title">创建支付订单后扫码支付。支付成功后系统会更新余额，并返回本次结算的积分结果。</p>

        <div class="dorm-info">
          <span>当前宿舍</span>
          <strong>{{ currentDormLabel }}</strong>
        </div>

        <div v-if="rateInfo" class="rate-panel">
          <div class="rate-item">
            <span>水费单价</span>
            <strong>{{ formatUnitPrice(rateInfo.waterUnitPrice, '吨水') }}</strong>
          </div>
          <div class="rate-item">
            <span>电费单价</span>
            <strong>{{ formatUnitPrice(rateInfo.electricityUnitPrice, '度电') }}</strong>
          </div>
        </div>

        <div class="fee-switcher">
          <div class="slider-bg" :style="sliderStyle"></div>
          <div class="switch-item" :class="{ 'is-active': feeType === 'water' }" @click="switchFeeType('water')">
            <el-icon><Coffee /></el-icon>
            水费
          </div>
          <div class="switch-item" :class="{ 'is-active': feeType === 'electric' }" @click="switchFeeType('electric')">
            <el-icon><Sunny /></el-icon>
            电费
          </div>
        </div>

        <div class="input-wrapper">
          <span class="currency">¥</span>
          <input v-model="amount" type="text" :placeholder="'请输入' + feeLabel + '金额'" @input="formatAmountInput" />
        </div>

        <div v-if="estimatedQuantityText" class="estimate-box">
          <span>预计可购</span>
          <strong>{{ estimatedQuantityText }}</strong>
        </div>

        <div class="method-section">
          <p>请选择缴费方式</p>
          <div class="pay-methods">
            <div
              v-for="method in payMethods"
              :key="method.value"
              class="method-item"
              :class="{ 'is-active': selectedMethod === method.value }"
              @click="selectedMethod = method.value"
            >
              <span class="method-name">{{ method.name }}</span>
              <el-icon class="check-icon"><Check /></el-icon>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="pay-action-btn"
            :disabled="!isPayable"
            :loading="isPaying"
            @click="handlePay"
          >
            缴纳 {{ feeLabel }}
          </el-button>
        </div>
      </div>
    </main>

    <footer class="action-buttons">
      <el-button size="large" @click="$router.back()">返回</el-button>
      <el-button size="large" type="primary" plain @click="goHistory">缴费历史</el-button>
    </footer>

    <el-dialog
      v-model="paymentDialogVisible"
      title="扫码支付"
      width="420px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="stopPolling"
    >
      <div v-if="paymentOrder" class="payment-dialog">
        <div class="payment-dialog__summary">
          <strong>{{ feeLabel }}</strong>
          <span>订单号：{{ paymentOrder.orderNo }}</span>
          <span>支付方式：{{ paymentMethodLabel }}</span>
          <span>支付金额：{{ Number(paymentOrder.amount || 0).toFixed(2) }}</span>
        </div>

        <div class="qr-box">
          <img v-if="paymentOrder.qrCodeImageUrl" :src="paymentOrder.qrCodeImageUrl" alt="支付二维码" class="qr-image" />
          <el-empty v-else description="二维码生成失败" />
        </div>

        <div class="payment-status" :class="paymentStatusClass">当前状态：{{ paymentStatusText }}</div>

        <p class="payment-tip">支付成功后会自动更新余额并结算低碳积分。</p>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="paymentDialogVisible = false">取消</el-button>
          <el-button :loading="checkingStatus" @click="checkPaymentStatus">刷新状态</el-button>
          <el-button type="success" :loading="simulatingSuccess" @click="simulatePaymentSuccess">模拟支付成功</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Check, Coffee, Sunny } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { formatDormLabel, formatNumber, formatUnitPrice } from '@/utils/formatters'
import { useStudentTokenStore } from '@/stores/student-token'
import {
  createStudentPaymentOrder,
  fetchStudentPaymentOrder,
  fetchWaterElectricity,
  simulateStudentPaymentSuccess,
  type PaymentOrder,
  type StudentWaterElectricity as WaterElectricityRateInfo,
} from '@/api/modules/student'

const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { studentInfo, stuNum, dormId, dormLabel } = storeToRefs(studentTokenStore)

const amount = ref('')
const selectedMethod = ref('ALIPAY')
const feeType = ref<'water' | 'electric'>('water')
const isPaying = ref(false)
const checkingStatus = ref(false)
const simulatingSuccess = ref(false)
const rateInfo = ref<WaterElectricityRateInfo | null>(null)
const paymentDialogVisible = ref(false)
const paymentOrder = ref<PaymentOrder | null>(null)
let pollingTimer: number | null = null

const feeLabel = computed(() => (feeType.value === 'water' ? '水费' : '电费'))
const currentDormLabel = computed(() => formatDormLabel(rateInfo.value, dormLabel.value))

const sliderStyle = computed(() => ({
  transform: feeType.value === 'water' ? 'translateX(0)' : 'translateX(100%)',
}))

const currentUnitPrice = computed(() => {
  if (!rateInfo.value) return null
  return feeType.value === 'water' ? rateInfo.value.waterUnitPrice : rateInfo.value.electricityUnitPrice
})

const currentUnitName = computed(() => {
  if (!rateInfo.value) return ''
  return feeType.value === 'water' ? rateInfo.value.waterUnitName || '吨' : rateInfo.value.electricityUnitName || '度'
})

const estimatedQuantityText = computed(() => {
  const parsedAmount = Number.parseFloat(amount.value)
  if (!parsedAmount || parsedAmount <= 0 || !currentUnitPrice.value || currentUnitPrice.value <= 0) {
    return ''
  }
  return `${formatNumber(parsedAmount / currentUnitPrice.value, 2)} ${currentUnitName.value}`
})

const payMethods = [
  { name: '支付宝', value: 'ALIPAY' },
  { name: '微信支付', value: 'WECHAT' },
]

const isPayable = computed(() => {
  return Boolean(amount.value) && Number.parseFloat(amount.value) > 0 && Boolean(stuNum.value) && Boolean(dormId.value)
})

const paymentMethodLabel = computed(() => (selectedMethod.value === 'WECHAT' ? '微信支付' : '支付宝'))

const paymentStatusText = computed(() => {
  if (!paymentOrder.value) return '未创建'
  if (paymentOrder.value.status === 'SUCCESS') return '支付成功'
  if (paymentOrder.value.status === 'PENDING') return '等待支付'
  return paymentOrder.value.status
})

const paymentStatusClass = computed(() => ({
  'is-success': paymentOrder.value?.status === 'SUCCESS',
  'is-pending': paymentOrder.value?.status === 'PENDING',
}))

function buildSettlementMessage(order?: PaymentOrder | null) {
  const dormAdded = Number(order?.carbonPointsAdded || 0)
  const personalAdded = Number(order?.personalPointsAdded || 0)
  if (dormAdded <= 0 && personalAdded <= 0) {
    return '支付成功，系统已完成充值。'
  }
  return `支付成功，系统已完成充值。本次宿舍积分 +${dormAdded}，个人积分 +${personalAdded}。`
}

function switchFeeType(type: 'water' | 'electric') {
  feeType.value = type
}

function formatAmountInput() {
  amount.value = amount.value.replace(/[^\d.]/g, '').replace(/(\..*)\./g, '$1')
}

function goHistory() {
  router.push('/history-fee')
}

async function loadRateInfo() {
  if (!stuNum.value && !dormId.value) return
  try {
    const { data } = await fetchWaterElectricity({
      stuNum: stuNum.value || undefined,
      dormId: dormId.value || undefined,
    })
    if (data.code === 200 && data.data) {
      rateInfo.value = data.data
      studentTokenStore.updateCarbonScore(data.data.personalCarbonScore)
    }
  } catch (error) {
    console.error('获取单价信息失败:', error)
  }
}

async function handlePay() {
  if (!isPayable.value) {
    ElMessage.warning('请先完善缴费信息')
    return
  }

  isPaying.value = true
  try {
    const { data } = await createStudentPaymentOrder({
      stuNum: stuNum.value,
      dormId: Number(dormId.value),
      amount: Number.parseFloat(amount.value),
      feeType: feeType.value,
      payType: selectedMethod.value,
      payerAccount: studentInfo.value?.username || stuNum.value,
    })

    if (data.code !== 200 || !data.data) {
      ElMessage.error(data.msg || '创建支付订单失败')
      return
    }

    paymentOrder.value = data.data
    paymentDialogVisible.value = true
    startPolling()
  } catch (error: any) {
    console.error('创建支付订单失败:', error)
    ElMessage.error(error.response?.data?.msg || '创建支付订单失败，请稍后重试')
  } finally {
    isPaying.value = false
  }
}

async function checkPaymentStatus() {
  if (!paymentOrder.value?.orderNo || !stuNum.value) return

  checkingStatus.value = true
  try {
    const { data } = await fetchStudentPaymentOrder(paymentOrder.value.orderNo, stuNum.value)
    if (data.code === 200 && data.data) {
      paymentOrder.value = data.data
      if (data.data.status === 'SUCCESS') {
        stopPolling()
        ElMessage.success(buildSettlementMessage(data.data))
        amount.value = ''
        paymentDialogVisible.value = false
        router.push('/water-electricity')
      }
    }
  } catch (error) {
    console.error('查询支付状态失败:', error)
  } finally {
    checkingStatus.value = false
  }
}

async function simulatePaymentSuccess() {
  if (!paymentOrder.value?.orderNo || !stuNum.value) return

  simulatingSuccess.value = true
  try {
    const { data } = await simulateStudentPaymentSuccess(paymentOrder.value.orderNo, stuNum.value)
    if (data.code !== 200 || !data.data) {
      ElMessage.error(data.msg || '模拟支付失败')
      return
    }

    paymentOrder.value = data.data
    stopPolling()
    ElMessage.success(buildSettlementMessage(data.data))
    amount.value = ''
    paymentDialogVisible.value = false
    router.push('/water-electricity')
  } catch (error: any) {
    console.error('模拟支付失败:', error)
    ElMessage.error(error.response?.data?.msg || '模拟支付失败，请稍后重试')
  } finally {
    simulatingSuccess.value = false
  }
}

function startPolling() {
  stopPolling()
  pollingTimer = window.setInterval(() => {
    checkPaymentStatus()
  }, 3000)
}

function stopPolling() {
  if (pollingTimer !== null) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

onMounted(loadRateInfo)
onBeforeUnmount(stopPolling)
</script>

<style scoped>
.pay-up {
  --primary-gradient: linear-gradient(135deg, #2b6a4f 0%, #5ea982 100%);
  --shadow-card: 0 18px 40px rgba(36, 69, 54, 0.1);
  --text-main: #1f372c;
  --text-sub: #5f776b;
  --bg-page: linear-gradient(180deg, #eef6f1 0%, #f8fbf9 100%);
  --radius-card: 20px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 24px rgba(36, 69, 54, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(10px);
}

:deep(.top-bar .el-button) {
  font-size: 15px;
  font-weight: 600;
}

.main-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 30px 20px;
}

.pay-card {
  width: 100%;
  max-width: 520px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: var(--radius-card);
  padding: 32px;
  box-shadow: var(--shadow-card);
  border: 1px solid #e5efe9;
}

.pay-card h1 {
  font-size: 26px;
  color: #244536;
  margin: 0 0 8px;
  font-weight: 700;
  text-align: center;
}

.sub-title {
  margin: 0 0 16px;
  text-align: center;
  color: var(--text-sub);
  line-height: 1.7;
}

.dorm-info,
.rate-panel,
.estimate-box {
  margin-bottom: 20px;
  border-radius: 14px;
}

.dorm-info {
  padding: 14px 16px;
  background: #fcfefd;
  border: 1px solid #e5efe9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.dorm-info span {
  color: var(--text-sub);
}

.dorm-info strong {
  color: var(--text-main);
}

.rate-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.rate-item,
.estimate-box {
  padding: 14px 16px;
  background: #eef6f1;
  border: 1px solid #e5efe9;
}

.rate-item span,
.estimate-box span {
  display: block;
  font-size: 13px;
  color: var(--text-sub);
  margin-bottom: 6px;
}

.rate-item strong,
.estimate-box strong {
  color: var(--text-main);
}

.fee-switcher {
  position: relative;
  display: flex;
  background: #f1f7f3;
  border: 1px solid #e5efe9;
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 32px;
  overflow: hidden;
}

.slider-bg {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(36, 69, 54, 0.08);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.switch-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-sub);
  cursor: pointer;
  position: relative;
  z-index: 2;
  transition: color 0.3s;
  user-select: none;
}

.switch-item.is-active {
  color: var(--text-main);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  border-bottom: 2px solid #dce9e1;
  padding-bottom: 10px;
  margin-bottom: 20px;
  transition: border-color 0.3s;
}

.input-wrapper:focus-within {
  border-bottom-color: #4f8f6e;
}

.currency {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  margin-right: 8px;
  padding-bottom: 4px;
}

.input-wrapper input[type='text'] {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 42px;
  font-weight: 700;
  color: var(--text-main);
  caret-color: #4f8f6e;
  line-height: 1;
}

.input-wrapper input[type='text']::placeholder {
  color: #c9cdd4;
  font-size: 18px;
  font-weight: 400;
}

.method-section p {
  color: var(--text-sub);
  font-size: 14px;
  margin: 0 0 16px;
  font-weight: 500;
}

.pay-methods {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 30px;
}

.method-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  border-radius: 16px;
  border: 1px solid #e5efe9;
  background: #fcfefd;
  cursor: pointer;
  transition: all 0.25s ease;
  user-select: none;
}

.method-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.check-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%) scale(0);
  color: #2b6a4f;
  transition: transform 0.2s ease;
}

.method-item.is-active {
  border-color: #8bb99f;
  background: #eef6f1;
}

.method-item.is-active .check-icon {
  transform: translateY(-50%) scale(1);
}

.pay-action-btn {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 16px;
  background: var(--primary-gradient);
  font-size: 16px;
  font-weight: 700;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 0 20px 24px;
}

.payment-dialog {
  display: grid;
  gap: 16px;
}

.payment-dialog__summary {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f6fbf8;
  border: 1px solid #e5efe9;
}

.qr-box {
  display: flex;
  justify-content: center;
}

.qr-image {
  width: 220px;
  height: 220px;
  object-fit: contain;
}

.payment-status {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f6fbf8;
  color: #56666a;
}

.payment-status.is-success {
  background: #ecfdf3;
  color: #147a46;
}

.payment-status.is-pending {
  background: #fff7ed;
  color: #b45309;
}

.payment-tip {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.7;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-button) {
  min-height: 42px;
  border-radius: 12px;
  border-color: #d6e5dc;
  font-weight: 600;
}

:deep(.el-dialog) {
  border-radius: 20px;
}

@media (max-width: 640px) {
  .main-content {
    padding: 20px 14px;
  }

  .pay-card {
    padding: 28px 20px;
  }

  .rate-panel,
  .pay-methods,
  .action-buttons {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>


