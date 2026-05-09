import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { parseJsonSafely } from '@/utils/json'

const ADMIN_PROFILE_KEY = 'adminProfile'
const ADMIN_TOKEN_KEY = 'adminToken'

interface AdminTokenProfile {
  token: string
  adminId: number
  username: string
  displayName: string
}

function loadStoredAdminProfile() {
  return parseJsonSafely<AdminTokenProfile>(localStorage.getItem(ADMIN_PROFILE_KEY), () => {
    localStorage.removeItem(ADMIN_PROFILE_KEY)
  })
}

export const useAdminTokenStore = defineStore('adminToken', () => {
  const profile = ref<AdminTokenProfile | null>(loadStoredAdminProfile())
  const token = ref(localStorage.getItem(ADMIN_TOKEN_KEY) || profile.value?.token || '')

  const isAdminLoggedIn = computed(() => Boolean(profile.value?.adminId && token.value))

  function setAdminToken(payload: AdminTokenProfile) {
    profile.value = payload
    token.value = payload.token
    localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(payload))
    localStorage.setItem(ADMIN_TOKEN_KEY, payload.token)
  }

  function clearAdminToken() {
    profile.value = null
    token.value = ''
    localStorage.removeItem(ADMIN_PROFILE_KEY)
    localStorage.removeItem(ADMIN_TOKEN_KEY)
  }

  return {
    profile,
    token,
    isAdminLoggedIn,
    setAdminToken,
    clearAdminToken,
  }
})
