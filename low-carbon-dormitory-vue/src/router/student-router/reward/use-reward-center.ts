import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import {
  exchangeReward,
  fetchRewardCenter,
  type RewardCenter,
  type RewardExchangeResult,
} from '@/api/modules/reward'
import { useStudentTokenStore } from '@/stores/student-token'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'

const MISSING_STUDENT_MESSAGE = '未检测到登录学生信息，请重新登录'

export function useRewardCenter() {
  const studentTokenStore = useStudentTokenStore()
  const { stuNum } = storeToRefs(studentTokenStore)

  const loading = ref(false)
  const errorMessage = ref('')
  const rewardCenter = ref<RewardCenter | null>(null)
  const submittingRewardId = ref<number | null>(null)

  const currentPoints = computed(() => rewardCenter.value?.currentPoints ?? 0)

  async function loadRewardCenter() {
    if (!stuNum.value) {
      errorMessage.value = MISSING_STUDENT_MESSAGE
      return null
    }

    loading.value = true
    errorMessage.value = ''

    try {
      const { data } = await fetchRewardCenter(stuNum.value)
      const result = requireApiData(data, '获取奖励中心失败')
      rewardCenter.value = result
      return result
    } catch (error) {
      errorMessage.value = resolveErrorMessage(error, '获取奖励中心失败，请稍后重试')
      return null
    } finally {
      loading.value = false
    }
  }

  async function exchangeRewardById(rewardId: number): Promise<RewardExchangeResult> {
    if (!stuNum.value) {
      throw new Error(MISSING_STUDENT_MESSAGE)
    }

    submittingRewardId.value = rewardId

    try {
      const { data } = await exchangeReward(stuNum.value, rewardId)
      return requireApiData(data, '兑换失败')
    } finally {
      submittingRewardId.value = null
    }
  }

  return {
    stuNum,
    loading,
    errorMessage,
    rewardCenter,
    submittingRewardId,
    currentPoints,
    loadRewardCenter,
    exchangeRewardById,
  }
}
