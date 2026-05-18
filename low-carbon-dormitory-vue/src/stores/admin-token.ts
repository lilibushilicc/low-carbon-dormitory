import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  persistJsonStorage,
  persistStorageValue,
  readJsonStorage,
  readStorageString,
} from '@/utils/storage'

const ADMIN_PROFILE_KEY = 'adminProfile'
const ADMIN_TOKEN_KEY = 'adminToken'

interface AdminTokenProfile {
  token: string
  adminId: number
  username: string
  displayName: string
}

function loadStoredAdminProfile() {
  return readJsonStorage<AdminTokenProfile>(ADMIN_PROFILE_KEY)
}

export const useAdminTokenStore = defineStore('adminToken', () => {
  const profile = ref<AdminTokenProfile | null>(loadStoredAdminProfile())
  const token = ref(readStorageString(ADMIN_TOKEN_KEY) || profile.value?.token || '')

  const isAdminLoggedIn = computed(() => Boolean(profile.value?.adminId && token.value))

  function setAdminToken(payload: AdminTokenProfile) {
    profile.value = payload
    token.value = payload.token
    persistJsonStorage(ADMIN_PROFILE_KEY, payload)
    persistStorageValue(ADMIN_TOKEN_KEY, payload.token)
  }

  function clearAdminToken() {
    profile.value = null
    token.value = ''
    persistJsonStorage(ADMIN_PROFILE_KEY, null)
    persistStorageValue(ADMIN_TOKEN_KEY, '')
  }

  return {
    profile,
    token,
    isAdminLoggedIn,
    setAdminToken,
    clearAdminToken,
  }
})
