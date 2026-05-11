import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { StudentProfile } from '@/types/student'
import { formatDormLabel } from '@/utils/formatters'
import { parseJsonSafely } from '@/utils/json'

const STUDENT_TOKEN_KEY = 'studentToken'
const STUDENT_STU_NUM_KEY = 'studentStuNum'
const DORM_ID_KEY = 'dormId'
const STUDENT_INFO_KEY = 'studentInfo'

function loadStoredStudentInfo() {
  return parseJsonSafely<StudentProfile>(localStorage.getItem(STUDENT_INFO_KEY), () => {
    localStorage.removeItem(STUDENT_INFO_KEY)
  })
}

export const useStudentTokenStore = defineStore('studentToken', () => {
  const token = ref(localStorage.getItem(STUDENT_TOKEN_KEY) || '')
  const stuNum = ref(localStorage.getItem(STUDENT_STU_NUM_KEY) || '')
  const dormId = ref(localStorage.getItem(DORM_ID_KEY) || '')
  const studentInfo = ref<StudentProfile | null>(loadStoredStudentInfo())

  const isLoggedIn = computed(() => Boolean(token.value))
  const dormLabel = computed(() =>
    formatDormLabel(studentInfo.value, dormId.value ? `宿舍ID：${dormId.value}` : '-'),
  )

  function persistState() {
    if (token.value) {
      localStorage.setItem(STUDENT_TOKEN_KEY, token.value)
    } else {
      localStorage.removeItem(STUDENT_TOKEN_KEY)
    }

    if (stuNum.value) {
      localStorage.setItem(STUDENT_STU_NUM_KEY, stuNum.value)
    } else {
      localStorage.removeItem(STUDENT_STU_NUM_KEY)
    }

    if (dormId.value) {
      localStorage.setItem(DORM_ID_KEY, dormId.value)
    } else {
      localStorage.removeItem(DORM_ID_KEY)
    }

    if (studentInfo.value) {
      localStorage.setItem(STUDENT_INFO_KEY, JSON.stringify(studentInfo.value))
    } else {
      localStorage.removeItem(STUDENT_INFO_KEY)
    }
  }

  function setStudentToken(profile: StudentProfile, currentStuNum?: string, authToken?: string) {
    studentInfo.value = profile
    token.value = authToken || token.value
    stuNum.value = currentStuNum || profile.stuNum || ''
    dormId.value = profile.dormId === null || profile.dormId === undefined ? '' : String(profile.dormId)
    persistState()
  }

  function updateStudentProfile(profile: StudentProfile) {
    setStudentToken(profile, stuNum.value || profile.stuNum)
  }

  function updateCarbonScore(carbonScore: number | null | undefined) {
    if (!studentInfo.value) {
      return
    }

    const normalizedScore = carbonScore ?? null
    if (studentInfo.value.carbonScore === normalizedScore) {
      return
    }

    studentInfo.value = {
      ...studentInfo.value,
      carbonScore: normalizedScore,
    }
    persistState()
  }

  function clearStudentToken() {
    token.value = ''
    stuNum.value = ''
    dormId.value = ''
    studentInfo.value = null
    persistState()
  }

  return {
    token,
    stuNum,
    dormId,
    studentInfo,
    isLoggedIn,
    dormLabel,
    setStudentToken,
    updateStudentProfile,
    updateCarbonScore,
    clearStudentToken,
  }
})
