<template>
  <div class="pay-mobile-page">
    <div class="pay-mobile-shell">
      <div class="pay-mobile-phone">
        <ASpin :spinning="loading || isPaying || checkingStatus || simulatingSuccess">
          <header class="pay-hero">
            <div class="pay-hero__glow pay-hero__glow--left"></div>
            <div class="pay-hero__glow pay-hero__glow--right"></div>

            <div class="pay-hero__top">
              <button type="button" class="hero-icon-button" @click="goPrevious">
                <LeftOutlined />
              </button>

              <div class="pay-hero__copy">
                <p class="pay-hero__eyebrow">宿舍水电费</p>
                <h1>移动缴费</h1>
                <span>{{ currentDormLabel }}</span>
              </div>

              <button type="button" class="hero-text-button" @click="goHistory">历史订单</button>
            </div>

            <div class="hero-rate-grid">
              <article class="hero-rate-card">
                <span>水费单价</span>
                <strong>{{ formatUnitPrice(rateInfo?.waterUnitPrice, '吨水') }}</strong>
              </article>
              <article class="hero-rate-card">
                <span>电费单价</span>
                <strong>{{ formatUnitPrice(rateInfo?.electricityUnitPrice, '度电') }}</strong>
              </article>
            </div>
          </header>

          <AAlert
            v-if="errorMessage"
            class="pay-error"
            type="error"
            show-icon
            :message="errorMessage"
          />

          <template v-else>
            <section class="pay-card">
              <div class="section-head">
                <div>
                  <span class="section-tag">创建订单</span>
                  <h2>选择费用类型与金额</h2>
                </div>
                <button type="button" class="section-link" @click="goUtilityHome">账单主页</button>
              </div>

              <div class="fee-switcher">
                <button
                  type="button"
                  class="fee-chip"
                  :class="{ 'fee-chip--active': feeType === 'water' }"
                  @click="switchFeeType('water')"
                >
                  水费
                </button>
                <button
                  type="button"
                  class="fee-chip"
                  :class="{ 'fee-chip--active': feeType === 'electric' }"
                  @click="switchFeeType('electric')"
                >
                  电费
                </button>
              </div>

              <div class="amount-panel">
                <span class="amount-panel__prefix">¥</span>
                <AInput
                  :value="amount"
                  size="large"
                  class="amount-input"
                  :bordered="false"
                  :placeholder="`请输入${feeLabel}金额`"
                  inputmode="decimal"
                  @update:value="handleAmountChange"
                />
              </div>

              <div class="preset-grid">
                <button
                  v-for="value in presetAmounts"
                  :key="value"
                  type="button"
                  class="preset-chip"
                  @click="applyPresetAmount(value)"
                >
                  {{ value }} 元
                </button>
              </div>

              <div class="estimate-grid">
                <article class="estimate-card">
                  <span>预计可购</span>
                  <strong>{{ estimatedQuantityText || '-' }}</strong>
                </article>
                <article class="estimate-card">
                  <span>支付方式</span>
                  <strong>{{ paymentMethodLabel }}</strong>
                </article>
              </div>

              <div class="method-block">
                <p>选择支付方式</p>

                <div class="method-grid">
                  <button
                    v-for="method in payMethods"
                    :key="method.value"
                    type="button"
                    class="method-item"
                    :class="{ 'method-item--active': selectedMethod === method.value }"
                    @click="selectedMethod = method.value"
                  >
                    <span>{{ method.name }}</span>
                    <small>{{ method.desc }}</small>
                  </button>
                </div>

                <AButton
                  type="primary"
                  size="large"
                  block
                  class="pay-submit"
                  :disabled="!isPayable"
                  :loading="isPaying"
                  @click="handlePay"
                >
                  生成{{ feeLabel }}订单
                </AButton>
              </div>
            </section>
          </template>
        </ASpin>
      </div>
    </div>

    <Teleport to="body">
      <transition name="sheet-fade">
        <div v-if="paymentDialogVisible" class="sheet-mask" @click.self="closePaymentSheet">
          <transition name="sheet-rise">
            <section v-if="paymentOrder" class="payment-sheet">
              <div class="payment-sheet__handle"></div>

              <div class="payment-sheet__head">
                <div>
                  <p>扫码支付</p>
                  <h3>{{ feeLabel }}订单</h3>
                </div>
                <button type="button" class="sheet-close" @click="closePaymentSheet">关闭</button>
              </div>

              <div class="payment-sheet__summary">
                <div class="summary-item">
                  <span>订单号</span>
                  <strong>{{ paymentOrder.orderNo }}</strong>
                </div>
                <div class="summary-item">
                  <span>支付金额</span>
                  <strong>{{ formatCurrency(paymentOrder.amount) }}</strong>
                </div>
                <div class="summary-item">
                  <span>支付方式</span>
                  <strong>{{ paymentMethodLabel }}</strong>
                </div>
                <div class="summary-item">
                  <span>当前状态</span>
                  <strong :class="paymentStatusClass">{{ paymentStatusText }}</strong>
                </div>
              </div>

              <div class="payment-sheet__qr">
                <img
                  v-if="paymentOrder.qrCodeImageUrl"
                  :src="paymentOrder.qrCodeImageUrl"
                  alt="支付二维码"
                  class="qr-image"
                />
                <AEmpty v-else description="二维码生成失败" />
              </div>

              <p class="payment-sheet__tip">支付成功后会自动同步宿舍余额和低碳积分。</p>

              <div class="payment-sheet__actions">
                <AButton size="large" block :loading="checkingStatus" @click="checkPaymentStatus">
                  刷新状态
                </AButton>
                <AButton size="large" block type="primary" :loading="simulatingSuccess" @click="simulatePaymentSuccess">
                  模拟支付成功
                </AButton>
              </div>
            </section>
          </transition>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Alert as AAlert, Button as AButton, Empty as AEmpty, Input as AInput, Spin as ASpin, message } from 'ant-design-vue'
import { LeftOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import 'ant-design-vue/dist/reset.css'
import { formatCurrency, formatDormLabel, formatNumber, formatUnitPrice } from '@/utils/formatters'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
import { buildUtilityMobileQuery, parsePositiveId, readQueryText } from '@/utils/mobile-billing'
import { useStudentTokenStore } from '@/stores/student-token'
import {
  createStudentPaymentOrder,
  fetchStudentPaymentOrder,
  fetchWaterElectricity,
  simulateStudentPaymentSuccess,
  type PaymentOrder,
  type StudentWaterElectricity,
} from '@/api/modules/student'

type PayMethodValue = 'ALIPAY' | 'WECHAT'
type FeeType = 'water' | 'electric'

const route = useRoute()
const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { studentInfo, stuNum, dormId, dormLabel } = storeToRefs(studentTokenStore)

const loading = ref(true)
const errorMessage = ref('')
const amount = ref('')
const selectedMethod = ref<PayMethodValue>('ALIPAY')
const feeType = ref<FeeType>('water')
const isPaying = ref(false)
const checkingStatus = ref(false)
const simulatingSuccess = ref(false)
const rateInfo = ref<StudentWaterElectricity | null>(null)
const paymentDialogVisible = ref(false)
const paymentOrder = ref<PaymentOrder | null>(null)
const pollingTimer = ref<number | null>(null)

const routeStuNum = computed(() => readQueryText(route.query.stuNum))

const routeDormId = computed(() => parsePositiveId(route.query.dormId))
const storeDormId = computed(() => parsePositiveId(dormId.value))
const resolvedStuNum = computed(() => routeStuNum.value || stuNum.value || '')
const resolvedDormId = computed(() => rateInfo.value?.dormId ?? routeDormId.value ?? storeDormId.value)
const currentDormLabel = computed(() => formatDormLabel(rateInfo.value, dormLabel.value))
const publicAccessMode = computed(() => Boolean(routeStuNum.value))
const feeLabel = computed(() => (feeType.value === 'water' ? '水费' : '电费'))
const paymentMethodLabel = computed(() => (selectedMethod.value === 'WECHAT' ? '微信支付' : '支付宝'))

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

const isPayable = computed(() => {
  return Boolean(amount.value) && Number.parseFloat(amount.value) > 0 && Boolean(resolvedStuNum.value) && Boolean(resolvedDormId.value)
})

const paymentStatusText = computed(() => {
  if (!paymentOrder.value) return '未创建'
  if (paymentOrder.value.status === 'SUCCESS') return '支付成功'
  if (paymentOrder.value.status === 'PENDING') return '待支付'
  return paymentOrder.value.status || '未知'
})

const paymentStatusClass = computed(() => ({
  'status-text': true,
  'status-text--success': paymentOrder.value?.status === 'SUCCESS',
  'status-text--pending': paymentOrder.value?.status === 'PENDING',
}))

const presetAmounts = [20, 30, 50, 100]

const payMethods: Array<{ name: string; value: PayMethodValue; desc: string }> = [
  { name: '支付宝', value: 'ALIPAY', desc: '推荐日常扫码' },
  { name: '微信支付', value: 'WECHAT', desc: '适合移动端支付' },
]

function buildMobileQuery() {
  return buildUtilityMobileQuery(routeStuNum.value, resolvedDormId.value, true)
}

function buildUtilityTarget() {
  return {
    path: '/water-electricity-antd',
    query: buildMobileQuery(),
  }
}

function buildHistoryTarget() {
  return {
    path: '/history-fee-antd',
    query: buildMobileQuery(),
  }
}

function switchFeeType(type: FeeType) {
  feeType.value = type
}

function normalizeAmountInput(value: string) {
  const cleaned = String(value || '')
    .replace(/[^\d.]/g, '')
    .replace(/(\..*)\./g, '$1')
    .replace(/^(\d+)\.(\d{0,2}).*$/, '$1.$2')
  return cleaned
}

function handleAmountChange(value: string) {
  amount.value = normalizeAmountInput(value)
}

function applyPresetAmount(value: number) {
  amount.value = String(value)
}

function goPrevious() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push(buildUtilityTarget())
}

function goUtilityHome() {
  void router.push(buildUtilityTarget())
}

function goHistory() {
  void router.push(buildHistoryTarget())
}

function closePaymentSheet() {
  paymentDialogVisible.value = false
  stopPolling()
}

function buildSettlementMessage(order?: PaymentOrder | null) {
  const dormAdded = Number(order?.carbonPointsAdded || 0)
  const personalAdded = Number(order?.personalPointsAdded || 0)
  if (dormAdded <= 0 && personalAdded <= 0) {
    return '支付成功，余额已同步更新。'
  }
  return `支付成功，宿舍积分 +${dormAdded}，个人积分 +${personalAdded}。`
}

async function loadRateInfo() {
  if (!resolvedStuNum.value && !resolvedDormId.value) {
    errorMessage.value = '未检测到学生学号或宿舍信息，请重新进入页面。'
    loading.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const { data } = await fetchWaterElectricity({
      stuNum: resolvedStuNum.value || undefined,
      dormId: resolvedDormId.value || undefined,
    })
    const result = requireApiData(data, '获取宿舍水电信息失败')
    rateInfo.value = result

    if (!publicAccessMode.value) {
      studentTokenStore.updateCarbonScore(result.personalCarbonScore)
    }
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, '获取宿舍水电信息失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  if (!isPayable.value || !resolvedDormId.value) {
    message.warning('请先完善缴费信息')
    return
  }

  isPaying.value = true
  try {
    const { data } = await createStudentPaymentOrder({
      stuNum: resolvedStuNum.value,
      dormId: resolvedDormId.value,
      amount: Number.parseFloat(amount.value),
      feeType: feeType.value,
      payType: selectedMethod.value,
      payerAccount: studentInfo.value?.username || resolvedStuNum.value,
    })

    paymentOrder.value = requireApiData(data, '创建支付订单失败')
    paymentDialogVisible.value = true
    startPolling()
  } catch (error) {
    message.error(resolveErrorMessage(error, '创建支付订单失败，请稍后重试'))
  } finally {
    isPaying.value = false
  }
}

async function handlePaymentSuccess(order: PaymentOrder) {
  paymentOrder.value = order
  stopPolling()
  amount.value = ''
  paymentDialogVisible.value = false
  await loadRateInfo()
  message.success(buildSettlementMessage(order))
  await router.push(buildUtilityTarget())
}

async function checkPaymentStatus() {
  if (!paymentOrder.value?.orderNo || !resolvedStuNum.value) {
    return
  }

  checkingStatus.value = true
  try {
    const { data } = await fetchStudentPaymentOrder(paymentOrder.value.orderNo, resolvedStuNum.value)
    const result = requireApiData(data, '查询支付状态失败')
    if (result.status === 'SUCCESS') {
      await handlePaymentSuccess(result)
      return
    }
    paymentOrder.value = result
  } catch (error) {
    message.error(resolveErrorMessage(error, '查询支付状态失败，请稍后重试'))
  } finally {
    checkingStatus.value = false
  }
}

async function simulatePaymentSuccess() {
  if (!paymentOrder.value?.orderNo || !resolvedStuNum.value) {
    return
  }

  simulatingSuccess.value = true
  try {
    const { data } = await simulateStudentPaymentSuccess(paymentOrder.value.orderNo, resolvedStuNum.value)
    const result = requireApiData(data, '模拟支付失败')
    await handlePaymentSuccess(result)
  } catch (error) {
    message.error(resolveErrorMessage(error, '模拟支付失败，请稍后重试'))
  } finally {
    simulatingSuccess.value = false
  }
}

function startPolling() {
  stopPolling()
  pollingTimer.value = window.setInterval(() => {
    void checkPaymentStatus()
  }, 3000)
}

function stopPolling() {
  if (pollingTimer.value !== null) {
    window.clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

onMounted(() => {
  void loadRateInfo()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.pay-mobile-page {
  min-height: 100vh;
  padding: 16px 12px 24px;
  background:
    radial-gradient(circle at 8% 4%, rgba(173, 244, 197, 0.48), transparent 24%),
    radial-gradient(circle at 92% 0%, rgba(144, 210, 255, 0.28), transparent 22%),
    linear-gradient(180deg, #eefcf3 0%, #f9fffb 48%, #edf8f1 100%);
}

.pay-mobile-shell {
  display: flex;
  justify-content: center;
}

.pay-mobile-phone {
  width: min(100%, 430px);
  display: grid;
  gap: 12px;
}

.pay-hero {
  position: relative;
  overflow: hidden;
  padding: 16px 14px 14px;
  border-radius: 26px;
  background:
    radial-gradient(circle at 50% 0%, rgba(255, 255, 255, 0.95), transparent 52%),
    linear-gradient(160deg, rgba(233, 253, 241, 0.98), rgba(219, 245, 228, 0.94));
  border: 1px solid rgba(188, 231, 204, 0.9);
  box-shadow: 0 24px 50px rgba(40, 104, 70, 0.14);
}

.pay-hero__glow {
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(66, 186, 120, 0.14);
}

.pay-hero__glow--left {
  top: -50px;
  left: -60px;
}

.pay-hero__glow--right {
  top: -64px;
  right: -28px;
}

.pay-hero__top {
  position: relative;
  display: grid;
  grid-template-columns: 88px 1fr 88px;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.hero-icon-button,
.hero-text-button,
.section-link,
.fee-chip,
.preset-chip,
.method-item,
.sheet-close {
  border: 0;
  cursor: pointer;
}

.hero-icon-button {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.8);
  color: #2a7a53;
  box-shadow: inset 0 0 0 1px rgba(167, 222, 184, 0.8);
  justify-self: start;
}

.hero-text-button {
  min-width: 88px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #246645;
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(169, 221, 186, 0.9);
  justify-self: end;
}

.pay-hero__copy {
  display: grid;
  gap: 3px;
  justify-items: center;
  text-align: center;
}

.pay-hero__eyebrow {
  margin: 0;
  color: #4c8d66;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.pay-hero__copy h1 {
  margin: 0;
  color: #124b31;
  font-size: 24px;
  line-height: 1.1;
  font-weight: 800;
}

.pay-hero__copy span {
  color: #265f3f;
  font-size: 12px;
  font-weight: 700;
}

.hero-rate-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hero-rate-card {
  min-height: 84px;
  padding: 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(215, 236, 222, 0.9);
  box-shadow: 0 14px 28px rgba(65, 120, 88, 0.08);
  display: grid;
  align-content: start;
  gap: 6px;
}

.hero-rate-card span {
  color: #5c7f6a;
  font-size: 11px;
  font-weight: 700;
}

.hero-rate-card strong {
  color: #173f2d;
  font-size: 15px;
  line-height: 1.35;
}

.pay-error {
  border-radius: 18px;
}

.pay-card {
  padding: 14px 13px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(215, 236, 222, 0.94);
  box-shadow: 0 18px 40px rgba(45, 102, 68, 0.1);
  display: grid;
  gap: 14px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.section-tag {
  display: inline-block;
  margin-bottom: 4px;
  padding: 2px 7px;
  border-radius: 999px;
  background: #ebf8ef;
  color: #4f7f61;
  font-size: 9px;
  font-weight: 700;
}

.section-head h2 {
  margin: 0;
  color: #173f2d;
  font-size: 15px;
  line-height: 1.3;
  font-weight: 800;
}

.section-link {
  padding: 0;
  background: transparent;
  color: #4f7f61;
  font-size: 11px;
  font-weight: 700;
}

.fee-switcher {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.fee-chip {
  min-height: 44px;
  border-radius: 14px;
  background: #f3faf5;
  color: #557767;
  font-size: 14px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(213, 234, 220, 0.9);
  transition: all 0.22s ease;
}

.fee-chip--active {
  background: linear-gradient(135deg, #2f9c66, #4fbf86);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(54, 151, 103, 0.26);
}

.amount-panel {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
  align-items: center;
  padding: 8px 12px;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(241, 250, 244, 0.92), rgba(255, 255, 255, 0.96)),
    #fff;
  border: 1px solid rgba(219, 238, 225, 0.95);
}

.amount-panel__prefix {
  color: #1f5b3d;
  font-size: 24px;
  font-weight: 800;
}

.amount-input {
  font-size: 30px;
}

:deep(.amount-input .ant-input) {
  height: auto;
  padding: 0;
  background: transparent;
  color: #173f2d;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
}

:deep(.amount-input .ant-input::placeholder) {
  color: #bccbbf;
  font-size: 16px;
  font-weight: 500;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.preset-chip {
  min-height: 38px;
  border-radius: 12px;
  background: #f7fbf8;
  color: #325843;
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(219, 236, 224, 0.95);
}

.estimate-grid,
.method-grid,
.payment-sheet__summary {
  display: grid;
  gap: 10px;
}

.estimate-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.estimate-card,
.summary-item {
  padding: 10px 12px;
  border-radius: 16px;
  background: #f7fcf8;
  border: 1px solid rgba(220, 239, 226, 0.95);
  display: grid;
  gap: 4px;
}

.estimate-card span,
.summary-item span {
  color: #6f8e7a;
  font-size: 10px;
  font-weight: 700;
}

.estimate-card strong,
.summary-item strong {
  color: #173f2d;
  font-size: 14px;
  line-height: 1.4;
}

.method-block {
  display: grid;
  gap: 12px;
}

.method-block p {
  margin: 0;
  color: #5b7967;
  font-size: 12px;
  font-weight: 700;
}

.method-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.method-item {
  padding: 12px 12px;
  border-radius: 16px;
  background: #fbfefd;
  border: 1px solid rgba(217, 236, 223, 0.96);
  display: grid;
  gap: 4px;
  text-align: left;
  transition: all 0.22s ease;
}

.method-item span {
  color: #173f2d;
  font-size: 14px;
  font-weight: 800;
}

.method-item small {
  color: #789181;
  font-size: 10px;
}

.method-item--active {
  border-color: rgba(88, 178, 126, 0.8);
  background: linear-gradient(180deg, #eefaf2 0%, #ffffff 100%);
  box-shadow: 0 14px 28px rgba(58, 154, 106, 0.12);
}

.pay-submit {
  height: 46px;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, #2f9c66, #58be82);
  box-shadow: 0 16px 28px rgba(47, 156, 102, 0.28);
  font-weight: 800;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  padding: 24px 14px;
  background: rgba(15, 37, 26, 0.36);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.payment-sheet {
  width: min(100%, 430px);
  padding: 14px 14px 18px;
  border-radius: 28px 28px 0 0;
  background: linear-gradient(180deg, #ffffff 0%, #f7fcf8 100%);
  box-shadow: 0 -20px 48px rgba(16, 52, 34, 0.24);
  display: grid;
  gap: 14px;
}

.payment-sheet__handle {
  width: 56px;
  height: 5px;
  border-radius: 999px;
  background: #d6e8dc;
  margin: 0 auto;
}

.payment-sheet__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.payment-sheet__head p {
  margin: 0 0 4px;
  color: #6a8976;
  font-size: 12px;
  font-weight: 700;
}

.payment-sheet__head h3 {
  margin: 0;
  color: #173f2d;
  font-size: 22px;
  font-weight: 800;
}

.sheet-close {
  padding: 0;
  background: transparent;
  color: #5a7967;
  font-size: 13px;
  font-weight: 700;
}

.status-text--success {
  color: #238d57;
}

.status-text--pending {
  color: #c07a12;
}

.payment-sheet__qr {
  min-height: 240px;
  padding: 14px;
  border-radius: 24px;
  background: linear-gradient(180deg, #f4fbf6 0%, #ffffff 100%);
  border: 1px solid rgba(219, 236, 223, 0.95);
  display: flex;
  justify-content: center;
  align-items: center;
}

.qr-image {
  width: min(100%, 240px);
  aspect-ratio: 1;
  object-fit: contain;
}

.payment-sheet__tip {
  margin: 0;
  color: #6d8677;
  font-size: 12px;
  line-height: 1.7;
}

.payment-sheet__actions {
  display: grid;
  gap: 10px;
}

:deep(.payment-sheet__actions .ant-btn) {
  height: 46px;
  border-radius: 16px;
  font-weight: 700;
}

.sheet-fade-enter-active,
.sheet-fade-leave-active,
.sheet-rise-enter-active,
.sheet-rise-leave-active {
  transition: all 0.24s ease;
}

.sheet-fade-enter-from,
.sheet-fade-leave-to {
  opacity: 0;
}

.sheet-rise-enter-from,
.sheet-rise-leave-to {
  opacity: 0;
  transform: translateY(24px);
}

@media (max-width: 380px) {
  .hero-rate-grid,
  .estimate-grid,
  .method-grid,
  .preset-grid {
    grid-template-columns: 1fr 1fr;
  }

  .pay-hero__copy h1 {
    font-size: 22px;
  }
}
</style>
