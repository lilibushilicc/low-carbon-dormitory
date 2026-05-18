import type { Router } from 'vue-router'
import { useAdminTokenStore } from '@/stores/admin-token'
import { useStudentTokenStore } from '@/stores/student-token'
import { persistJsonStorage, removeStorageItem } from './storage'

const LOGIN_USER_KEY = 'loginUser'

interface LegacyAdminLoginUserPayload {
  token: string
  username: string
  displayName: string
  adminId: number
}

export function clearLegacyLoginUser() {
  removeStorageItem(LOGIN_USER_KEY)
}

export function persistLegacyLoginUser(payload: Record<string, unknown>) {
  persistJsonStorage(LOGIN_USER_KEY, payload)
}

export function buildLegacyAdminLoginUser(payload: LegacyAdminLoginUserPayload) {
  return {
    token: payload.token,
    username: payload.username,
    displayName: payload.displayName || payload.username,
    adminId: payload.adminId || 0,
    role: 'ADMIN',
  }
}

export async function logoutAndRedirect(router: Router) {
  useStudentTokenStore().clearStudentToken()
  useAdminTokenStore().clearAdminToken()
  clearLegacyLoginUser()
  await router.replace('/login')
}
