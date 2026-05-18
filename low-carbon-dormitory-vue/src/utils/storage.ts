import { parseJsonSafely } from './json'

export function readStorageString(key: string) {
  return localStorage.getItem(key) || ''
}

export function removeStorageItem(key: string) {
  localStorage.removeItem(key)
}

export function readJsonStorage<T>(key: string) {
  return parseJsonSafely<T>(localStorage.getItem(key), () => {
    removeStorageItem(key)
  })
}

export function persistStorageValue(key: string, value: string | null | undefined) {
  if (value) {
    localStorage.setItem(key, value)
    return
  }

  removeStorageItem(key)
}

export function persistJsonStorage(key: string, value: unknown) {
  if (value === null || value === undefined) {
    removeStorageItem(key)
    return
  }

  localStorage.setItem(key, JSON.stringify(value))
}
