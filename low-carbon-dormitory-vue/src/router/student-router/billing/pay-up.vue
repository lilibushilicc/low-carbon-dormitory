<template>
  <div class="page">
    <div class="page-shell">
      <header class="top-bar">
        <el-button class="top-action" text @click="goPrevious">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="top-links">
          <el-button class="top-action" text @click="goUtilityHome">水电费主页</el-button>
          <el-button class="top-action" text @click="goHistory">缴费历史</el-button>
        </div>
      </header>

      <el-skeleton :loading="loading" animated :rows="8">
        <template #default>
          <el-alert
            v-if="errorMessage"
            class="page-alert"
            :title="errorMessage"
            type="error"
            :closable="false"
            show-icon
          />

          <section v-else class="pay-card">
            <div class="hero-copy">
              <h1>费用缴纳</h1>
              <p>创建支付订单后扫码支付。支付成功后系统会更新余额，并返回本次结算的积分结果。</p>
            </div>

            <div class="dorm-info">
              <span>当前宿舍</span>
              <strong>{{ currentDormLabel }}</strong>
            </div>

            <div class="rate-panel">
              <article class="rate-item">
                <span>水费单价</span>
                <strong>{{ formatUnitPrice(rateInfo?.waterUnitPrice, '吨水') }}</strong>
              </article>
              <article class="rate-item">
                <span>电费单价</span>
                <strong>{{ formatUnitPrice(rateInfo?.electricityUnitPrice, '度电') }}</strong>
              </article>
            </div>

            <div class="fee-switcher">
              <div class="slider-bg" :style="sliderStyle"></div>
              <button
                type="button"
                class="switch-item"
                :class="{ 'is-active': feeType === 'water' }"
                @click="switchFeeType('water')"
              >
                <el-icon><Coffee /></el-icon>
                <span>水费</span>
              </button>
              <button
                type="button"
                class="switch-item"
                :class="{ 'is-active': feeType === 'electric' }"
                @click="switchFeeType('electric')"
              >
                <el-icon><Sunny /></el-icon>
                <span>电费</span>
              </button>
            </div>

            <div class="amount-section">
              <div class="amount-panel">
                <span class="currency">¥</span>
                <input
                  v-model="amount"
                  type="text"
                  :placeholder="`请输入${feeLabel}金额`"
                  @input="formatAmountInput"
                />
              </div>
              <div class="balance-text">当前余额 {{ currentBalanceText }}</div>
            </div>

            <article class="estimate-card">
              <span>预计可购</span>
              <strong>{{ estimatedQuantityText || '-' }}</strong>
            </article>

            <div class="method-section">
              <p>请选择缴费方式</p>
              <div class="pay-methods">
                <button
                  v-for="method in payMethods"
                  :key="method.value"
                  type="button"
                  class="method-item"
                  :class="{ 'is-active': selectedMethod === method.value }"
                  @click="selectedMethod = method.value"
                >
                  <span class="method-name">{{ method.name }}</span>
                  <el-icon class="check-icon"><Check /></el-icon>
                </button>
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
          </section>
        </template>
      </el-skeleton>
    </div>

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
import { formatCurrency, formatDormLabel, formatNumber, formatUnitPrice } from '@/utils/formatters'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
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

const loading = ref(true)
const errorMessage = ref('')
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

const payMethods = [
  { name: '支付宝', value: 'ALIPAY' },
  { name: '微信支付', value: 'WECHAT' },
]

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

const currentBalanceText = computed(() => {
  if (!rateInfo.value) return '-'
  return feeType.value === 'water'
    ? formatCurrency(rateInfo.value.waterBalance)
    : formatCurrency(rateInfo.value.electricityBalance)
})

const estimatedQuantityText = computed(() => {
  const parsedAmount = Number.parseFloat(amount.value)
  if (!parsedAmount || parsedAmount <= 0 || !currentUnitPrice.value || currentUnitPrice.value <= 0) {
    return ''
  }
  return `${formatNumber(parsedAmount / currentUnitPrice.value, 2)} ${currentUnitName.value}`
})

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

function goPrevious() {
  router.back()
}

function goUtilityHome() {
  router.push('/water-electricity')
}

function goHistory() {
  router.push('/history-fee')
}

function switchFeeType(type: 'water' | 'electric') {
  feeType.value = type
}

function formatAmountInput() {
  amount.value = amount.value.replace(/[^\d.]/g, '').replace(/(\..*)\./g, '$1')
}

async function loadRateInfo() {
  if (!stuNum.value && !dormId.value) {
    errorMessage.value = '未检测到登录信息，请重新登录'
    loading.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const { data } = await fetchWaterElectricity({
      stuNum: stuNum.value || undefined,
      dormId: dormId.value || undefined,
    })
    rateInfo.value = requireApiData(data, '获取单价信息失败')
    studentTokenStore.updateCarbonScore(rateInfo.value.personalCarbonScore)
  } catch (error) {
    console.error('获取单价信息失败:', error)
    errorMessage.value = resolveErrorMessage(error, '获取单价信息失败，请稍后重试')
  } finally {
    loading.value = false
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

    paymentOrder.value = requireApiData(data, '创建支付订单失败')
    paymentDialogVisible.value = true
    startPolling()
  } catch (error) {
    console.error('创建支付订单失败:', error)
    ElMessage.error(resolveErrorMessage(error, '创建支付订单失败，请稍后重试'))
  } finally {
    isPaying.value = false
  }
}

async function checkPaymentStatus() {
  if (!paymentOrder.value?.orderNo || !stuNum.value) return

  checkingStatus.value = true
  try {
    const { data } = await fetchStudentPaymentOrder(paymentOrder.value.orderNo, stuNum.value)
    paymentOrder.value = requireApiData(data, '查询支付状态失败')
    if (paymentOrder.value.status === 'SUCCESS') {
      stopPolling()
      ElMessage.success(buildSettlementMessage(paymentOrder.value))
      amount.value = ''
      paymentDialogVisible.value = false
      router.push('/water-electricity')
    }
  } catch (error) {
    console.error('查询支付状态失败:', error)
    ElMessage.error(resolveErrorMessage(error, '查询支付状态失败，请稍后重试'))
  } finally {
    checkingStatus.value = false
  }
}

async function simulatePaymentSuccess() {
  if (!paymentOrder.value?.orderNo || !stuNum.value) return

  simulatingSuccess.value = true
  try {
    const { data } = await simulateStudentPaymentSuccess(paymentOrder.value.orderNo, stuNum.value)
    paymentOrder.value = requireApiData(data, '模拟支付失败')
    stopPolling()
    ElMessage.success(buildSettlementMessage(paymentOrder.value))
    amount.value = ''
    paymentDialogVisible.value = false
    router.push('/water-electricity')
  } catch (error) {
    console.error('模拟支付失败:', error)
    ElMessage.error(resolveErrorMessage(error, '模拟支付失败，请稍后重试'))
  } finally {
    simulatingSuccess.value = false
  }
}

function startPolling() {
  stopPolling()
  pollingTimer = window.setInterval(() => {
    void checkPaymentStatus()
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
.page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at 3% 4%, rgba(163, 223, 190, 0.24), transparent 18%),
    radial-gradient(circle at 96% 8%, rgba(143, 211, 178, 0.22), transparent 20%),
    linear-gradient(180deg, #eef7f2 0%, #f7fbf9 100%);
}

.page-shell {
  max-width: 900px;
  margin: 0 auto;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  max-width: 740px;
  margin: 0 auto 12px;
}

.top-links {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.top-action {
  color: #5f776b;
  font-size: 14px;
  font-weight: 600;
}

.page-alert {
  margin-bottom: 16px;
}

.pay-card {
  width: min(100%, 740px);
  margin: 0 auto;
  padding: 30px 30px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #e1ece4;
  box-shadow: 0 22px 54px rgba(42, 86, 61, 0.1);
}

.hero-copy {
  display: grid;
  justify-items: center;
  text-align: center;
  gap: 10px;
  margin-bottom: 22px;
}

.hero-copy h1 {
  margin: 0;
  color: #234635;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
}

.hero-copy p {
  margin: 0;
  max-width: 620px;
  color: #6d8477;
  font-size: 15px;
  line-height: 1.75;
}

.dorm-info {
  min-height: 64px;
  margin-bottom: 16px;
  padding: 0 20px;
  border-radius: 18px;
  border: 1px solid #dce9e0;
  background: #fbfefd;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.dorm-info span {
  color: #6d8477;
  font-size: 16px;
  font-weight: 600;
}

.dorm-info strong {
  color: #244536;
  font-size: 18px;
  font-weight: 800;
}

.rate-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.rate-item {
  min-height: 92px;
  padding: 16px 18px 14px;
  background: #edf5f1;
  display: grid;
  align-content: start;
  gap: 8px;
}

.rate-item span {
  color: #5f776b;
  font-size: 15px;
  font-weight: 600;
}

.rate-item strong {
  color: #244536;
  font-size: 16px;
  line-height: 1.2;
  font-weight: 800;
}

.fee-switcher {
  position: relative;
  display: flex;
  padding: 6px;
  margin-bottom: 28px;
  border-radius: 20px;
  border: 1px solid #dce9e0;
  background: #eef5f1;
  overflow: hidden;
}

.slider-bg {
  position: absolute;
  top: 6px;
  left: 6px;
  width: calc(50% - 6px);
  height: calc(100% - 12px);
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(35, 70, 53, 0.08);
  transition: transform 0.28s ease;
  z-index: 1;
}

.switch-item {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 58px;
  border: 0;
  background: transparent;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  color: #5f776b;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
}

.switch-item.is-active {
  color: #244536;
}

.amount-section {
  margin-bottom: 14px;
}

.amount-panel {
  display: flex;
  align-items: center;
  padding: 6px 0 12px;
  border-bottom: 3px solid #63a37d;
}

.currency {
  margin-right: 14px;
  color: #244536;
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
}

.amount-panel input {
  flex: 1;
  border: 0;
  outline: none;
  background: transparent;
  color: #244536;
  font-size: 32px;
  font-weight: 800;
  line-height: 1.1;
}

.amount-panel input::placeholder {
  color: #c8d1cb;
  font-size: 16px;
  font-weight: 600;
}

.balance-text {
  margin-top: 10px;
  color: #6d8477;
  font-size: 13px;
}

.estimate-card {
  min-height: 76px;
  margin-bottom: 20px;
  padding: 14px 18px;
  border-radius: 16px;
  background: #edf5f1;
  display: grid;
  gap: 4px;
}

.estimate-card span {
  color: #6d8477;
  font-size: 14px;
  font-weight: 600;
}

.estimate-card strong {
  color: #244536;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 800;
}

.method-section {
  margin-bottom: 18px;
}

.method-section p {
  margin: 0 0 10px;
  color: #5f776b;
  font-size: 16px;
  line-height: 1.4;
}

.pay-methods {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.method-item {
  min-height: 62px;
  padding: 0 18px;
  border-radius: 16px;
  border: 1px solid #dce9e0;
  background: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #244536;
  cursor: pointer;
  transition: all 0.22s ease;
}

.method-item.is-active {
  background: #edf7f1;
  border-color: #7db696;
}

.method-name {
  font-size: 16px;
  font-weight: 800;
}

.check-icon {
  color: #2c7a56;
  font-size: 16px;
  opacity: 0;
}

.method-item.is-active .check-icon {
  opacity: 1;
}

.pay-action-btn {
  width: 100%;
  min-height: 64px;
  border: 0;
  border-radius: 16px;
  font-size: 18px;
  font-weight: 800;
  --el-button-bg-color: #4b9b6e;
  --el-button-border-color: #4b9b6e;
  --el-button-hover-bg-color: #3f895f;
  --el-button-hover-border-color: #3f895f;
  --el-button-active-bg-color: #35704e;
  --el-button-active-border-color: #35704e;
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
  color: #5f776b;
  line-height: 1.7;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-button) {
  border-radius: 12px;
  font-weight: 600;
}

:deep(.el-dialog) {
  border-radius: 20px;
}

@media (max-width: 1200px) {
  .hero-copy h1 {
    font-size: 28px;
  }

  .hero-copy p {
    font-size: 15px;
  }

  .currency,
  .amount-panel input {
    font-size: 30px;
  }
}

@media (max-width: 860px) {
  .page {
    padding: 16px 10px 28px;
  }

  .top-bar,
  .pay-card {
    max-width: none;
    width: 100%;
  }

  .top-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .top-links {
    width: 100%;
    justify-content: space-between;
  }

  .pay-card {
    padding: 28px 18px 24px;
    border-radius: 24px;
  }

  .hero-copy h1 {
    font-size: 32px;
  }

  .hero-copy p {
    font-size: 16px;
    line-height: 1.8;
  }

  .dorm-info {
    min-height: 74px;
    padding: 0 18px;
    border-radius: 18px;
  }

  .dorm-info span,
  .method-section p {
    font-size: 16px;
  }

  .dorm-info strong {
    font-size: 18px;
  }

  .rate-panel,
  .pay-methods {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .rate-item {
    min-height: 96px;
    padding: 18px;
  }

  .rate-item span {
    font-size: 15px;
  }

  .rate-item strong {
    font-size: 18px;
  }

  .switch-item {
    min-height: 62px;
    font-size: 17px;
  }

  .currency {
    font-size: 40px;
  }

  .amount-panel input {
    font-size: 38px;
  }

  .amount-panel input::placeholder {
    font-size: 18px;
  }

  .balance-text {
    font-size: 14px;
  }

  .estimate-card {
    padding: 16px 18px;
  }

  .estimate-card span {
    font-size: 14px;
  }

  .estimate-card strong {
    font-size: 18px;
  }

  .method-item {
    min-height: 68px;
    padding: 0 18px;
  }

  .method-name,
  .check-icon {
    font-size: 18px;
  }

  .pay-action-btn {
    min-height: 72px;
    font-size: 20px;
  }
}
</style>
