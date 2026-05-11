import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import {
  exchangeReward,
  fetchRewardCenter,
  type RewardCenter,
  type RewardExchangeResult,
} from '@/api/modules/reward'
import { useStudentTokenStore } from '@/stores/student-token'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'

const MISSING_STUDENT_MESSAGE = '未检测到学生学号，请重新登录或在地址中传入 stuNum'

export function useRewardCenter() {
  const route = useRoute()
  const studentTokenStore = useStudentTokenStore()
  const { stuNum } = storeToRefs(studentTokenStore)

  const loading = ref(false)
  const errorMessage = ref('')
  const rewardCenter = ref<RewardCenter | null>(null)
  const submittingRewardId = ref<number | null>(null)

  const routeStuNum = computed(() => {
    const value = route.query.stuNum
    return typeof value === 'string' ? value.trim() : ''
  })
  const resolvedStuNum = computed(() => routeStuNum.value || stuNum.value || '')
  const publicAccessMode = computed(() => Boolean(routeStuNum.value) && routeStuNum.value !== stuNum.value)
  const currentPoints = computed(() => rewardCenter.value?.currentPoints ?? 0)

  async function loadRewardCenter() {
    if (!resolvedStuNum.value) {
      errorMessage.value = MISSING_STUDENT_MESSAGE
      return null
    }

    loading.value = true
    errorMessage.value = ''

    try {
      const { data } = await fetchRewardCenter(resolvedStuNum.value)
      const result = requireApiData(data, '获取奖励中心失败')
      rewardCenter.value = result
      if (!publicAccessMode.value) {
        studentTokenStore.updateCarbonScore(result.currentPoints)
      }
      return result
    } catch (error) {
      errorMessage.value = resolveErrorMessage(error, '获取奖励中心失败，请稍后重试')
      return null
    } finally {
      loading.value = false
    }
  }

  async function exchangeRewardById(rewardId: number): Promise<RewardExchangeResult> {
    if (!resolvedStuNum.value) {
      throw new Error(MISSING_STUDENT_MESSAGE)
    }

    submittingRewardId.value = rewardId

    try {
      const { data } = await exchangeReward(resolvedStuNum.value, rewardId)
      return requireApiData(data, '兑换失败')
    } finally {
      submittingRewardId.value = null
    }
  }

  return {
    stuNum: resolvedStuNum,
    routeStuNum,
    publicAccessMode,
    loading,
    errorMessage,
    rewardCenter,
    submittingRewardId,
    currentPoints,
    loadRewardCenter,
    exchangeRewardById,
  }
}
