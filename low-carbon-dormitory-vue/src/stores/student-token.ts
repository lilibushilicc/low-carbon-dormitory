import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { StudentProfile } from '@/types/student'
import { formatDormLabel } from '@/utils/formatters'
import {
  persistJsonStorage,
  persistStorageValue,
  readJsonStorage,
  readStorageString,
} from '@/utils/storage'

const STUDENT_TOKEN_KEY = 'studentToken'
const STUDENT_STU_NUM_KEY = 'studentStuNum'
const DORM_ID_KEY = 'dormId'
const STUDENT_INFO_KEY = 'studentInfo'

function loadStoredStudentInfo() {
  return readJsonStorage<StudentProfile>(STUDENT_INFO_KEY)
}

export const useStudentTokenStore = defineStore('studentToken', () => {
  const token = ref(readStorageString(STUDENT_TOKEN_KEY))
  const stuNum = ref(readStorageString(STUDENT_STU_NUM_KEY))
  const dormId = ref(readStorageString(DORM_ID_KEY))
  const studentInfo = ref<StudentProfile | null>(loadStoredStudentInfo())

  const isLoggedIn = computed(() => Boolean(token.value))
  const dormLabel = computed(() =>
    formatDormLabel(studentInfo.value, dormId.value ? `宿舍ID：${dormId.value}` : '-'),
  )

  function persistState() {
    persistStorageValue(STUDENT_TOKEN_KEY, token.value)
    persistStorageValue(STUDENT_STU_NUM_KEY, stuNum.value)
    persistStorageValue(DORM_ID_KEY, dormId.value)
    persistJsonStorage(STUDENT_INFO_KEY, studentInfo.value)
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
