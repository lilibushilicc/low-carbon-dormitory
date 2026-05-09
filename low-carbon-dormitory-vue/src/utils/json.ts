export function parseJsonSafely<T>(value: unknown, onError?: () => void): T | null {
  if (typeof value !== 'string' || !value) {
    return null
  }

  try {
    return JSON.parse(value) as T
  } catch {
    onError?.()
    return null
  }
}
