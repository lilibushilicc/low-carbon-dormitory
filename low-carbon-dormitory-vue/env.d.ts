/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_UNIFIED_LOGIN_URL?: string
  readonly VITE_UNIFIED_LOGIN_ORIGIN?: string
  readonly VITE_UNIFIED_LOGIN_BASE_PATH?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
