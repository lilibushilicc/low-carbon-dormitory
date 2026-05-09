import { normalizeAppRoutePath } from '@/utils/app-navigation'

interface UnifiedLoginOptions {
  redirectPath?: string
  role?: 'student' | 'admin'
}

function normalizeBasePath(path: string | undefined, fallback: string) {
  const normalizedPath = (path || fallback).trim()
  if (!normalizedPath) {
    return fallback
  }

  const withLeadingSlash = normalizedPath.startsWith('/') ? normalizedPath : `/${normalizedPath}`
  return withLeadingSlash.replace(/\/+$/, '') || fallback
}

function resolveLoginBaseUrl() {
  const loginUrl = import.meta.env.VITE_UNIFIED_LOGIN_URL?.trim()
  if (loginUrl) {
    return loginUrl.endsWith('/') ? loginUrl : `${loginUrl}/`
  }

  const loginOrigin = import.meta.env.VITE_UNIFIED_LOGIN_ORIGIN?.trim()
  const loginBasePath = normalizeBasePath(import.meta.env.VITE_UNIFIED_LOGIN_BASE_PATH, '/login')
  if (loginOrigin) {
    return `${loginOrigin.replace(/\/+$/, '')}${loginBasePath}/`
  }

  const appBaseUrl = new URL(import.meta.env.BASE_URL || '/', window.location.origin)
  return appBaseUrl.toString()
}

export function buildUnifiedLoginUrl(options: UnifiedLoginOptions = {}) {
  const url = new URL(resolveLoginBaseUrl())
  const redirectPath = normalizeAppRoutePath(options.redirectPath)

  if (redirectPath) {
    url.searchParams.set('redirect', redirectPath)
  }
  if (options.role) {
    url.searchParams.set('role', options.role)
  }

  return url.toString()
}

export function redirectToUnifiedLogin(replace = false, options: UnifiedLoginOptions = {}) {
  const target = buildUnifiedLoginUrl(options)
  if (replace) {
    window.location.replace(target)
    return
  }
  window.location.assign(target)
}
