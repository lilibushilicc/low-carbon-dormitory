import axios from 'axios'

function resolveRestaurantApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_RESTAURANT_API_BASE_URL?.trim()
  if (configuredBaseUrl) {
    return configuredBaseUrl === '/' ? '' : configuredBaseUrl.replace(/\/+$/, '')
  }

  return import.meta.env.DEV ? '/api/restaurant' : 'http://39.98.69.153:8081/api'
}

const instance = axios.create({
  baseURL: resolveRestaurantApiBaseUrl(),
  timeout: 5000,
})
instance.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)
 instance.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    return Promise.reject(error)
  },
)

export default instance
