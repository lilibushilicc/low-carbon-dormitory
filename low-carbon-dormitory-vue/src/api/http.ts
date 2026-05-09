import axios from 'axios'

const STUDENT_TOKEN_KEY = 'studentToken'
const ADMIN_TOKEN_KEY = 'adminToken'

export const http = axios.create({
  baseURL: '/api/dorm',
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

  const token = url.startsWith('/admin/')
    ? localStorage.getItem(ADMIN_TOKEN_KEY)
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
