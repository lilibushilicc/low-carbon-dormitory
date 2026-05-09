export type AppRole = 'student' | 'admin'

const DEFAULT_HOME_PATH: Record<AppRole, string> = {
  student: '/index-student',
  admin: '/manager/home',
}

export function getDefaultHomePath(role: AppRole) {
  return DEFAULT_HOME_PATH[role]
}

export function normalizeAppRoutePath(path?: string | null) {
  if (typeof path !== 'string') {
    return ''
  }

  const normalizedPath = path.trim()
  if (!normalizedPath.startsWith('/')) {
    return ''
  }

  if (
    normalizedPath === '/' ||
    normalizedPath.startsWith('/?') ||
    normalizedPath.startsWith('//') ||
    normalizedPath.startsWith('/login')
  ) {
    return ''
  }

  return normalizedPath
}

export function resolvePostLoginPath(_path: string | null | undefined, role: AppRole) {
  return getDefaultHomePath(role)
}
