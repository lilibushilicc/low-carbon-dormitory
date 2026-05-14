import axios from 'axios'

const STUDENT_TOKEN_KEY = 'studentToken'
const ADMIN_TOKEN_KEY = 'adminToken'
const PUBLIC_STUDENT_MOBILE_PATHS = new Set([
  '/reward-exchange-antd',
  '/reward-exchange-antd/',
  '/water-electricity-antd',
  '/water-electricity-antd/',
  '/pay-up-antd',
  '/pay-up-antd/',
  '/history-fee-antd',
  '/history-fee-antd/',
])

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  if (configuredBaseUrl) {
    return configuredBaseUrl === '/' ? '' : configuredBaseUrl.replace(/\/+$/, '')
  }

  return import.meta.env.DEV ? '/api/dorm' : ''
}

function isPublicStudentMobileAccess() {
  if (typeof window === 'undefined') {
    return false
  }

  const pathname = window.location.pathname
  const searchParams = new URLSearchParams(window.location.search)
  const routeStuNum = searchParams.get('stuNum')?.trim()
  return Boolean(routeStuNum) && PUBLIC_STUDENT_MOBILE_PATHS.has(pathname)
}

export const http = axios.create({
  baseURL: resolveApiBaseUrl(),
  timeout: 10000,
  headers: {
    'X-Requested-With': 'XMLHttpRequest',
  },
})

http.interceptors.request.use((config) => {
  if (config.url?.startsWith('/api/')) {
    config.url = config.url.slice('/api'.length)
  }

  const url = config.url || ''
  const isLoginRequest = url === '/student/login' || url === '/admin/login'
  if (isLoginRequest) {
    return config
  }

  const isAdminRequest = url.startsWith('/admin/')
  const token = isAdminRequest
    ? localStorage.getItem(ADMIN_TOKEN_KEY)
    : isPublicStudentMobileAccess()
      ? ''
      : localStorage.getItem(STUDENT_TOKEN_KEY) || localStorage.getItem(ADMIN_TOKEN_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
)
