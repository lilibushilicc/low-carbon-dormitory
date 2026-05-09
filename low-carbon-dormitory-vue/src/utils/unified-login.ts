interface UnifiedLoginOptions {
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

  return new URL('/login', window.location.origin).toString()
}

export function buildUnifiedLoginUrl(options: UnifiedLoginOptions = {}) {
  const url = new URL(resolveLoginBaseUrl())

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
