import type { ApiResponse } from '@/types/api'

export function requireApiData<T>(response: ApiResponse<T>, fallbackMessage: string): T {
  if (response.code !== 200 || response.data === null || response.data === undefined) {
    throw new Error(response.msg || fallbackMessage)
  }
  return response.data
}

export function resolveErrorMessage(error: unknown, fallbackMessage: string): string {
  const maybeAxiosMessage = (error as any)?.response?.data?.msg
  if (typeof maybeAxiosMessage === 'string' && maybeAxiosMessage.trim()) {
    return maybeAxiosMessage
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return fallbackMessage
}
