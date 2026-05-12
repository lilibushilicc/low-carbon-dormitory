export const UTILITY_MOBILE_SOURCE = 'utility-mobile'

export function readQueryText(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

export function parsePositiveId(value: unknown) {
  if (typeof value !== 'string' && typeof value !== 'number') {
    return null
  }

  const normalized = Number(value)
  if (!Number.isFinite(normalized) || normalized <= 0) {
    return null
  }

  return normalized
}

export function buildUtilityMobileQuery(
  stuNum: string,
  dormId?: number | string | null,
  includeSource = false,
) {
  if (!stuNum && !includeSource) {
    return undefined
  }

  return {
    ...(stuNum ? { stuNum } : {}),
    source: UTILITY_MOBILE_SOURCE,
    ...(dormId ? { dormId: String(dormId) } : {}),
  }
}
