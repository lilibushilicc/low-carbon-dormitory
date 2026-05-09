export interface DormLabelSource {
  dormNo?: string | null
  dormBuilding?: string | null
  dormRoom?: string | null
}

export function formatNumber(value?: number | null, digits = 1, fallback = '0') {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return fallback
  }

  return Number(value).toFixed(digits).replace(/\.0+$|(\.\d*[1-9])0+$/, '$1')
}

export function formatCurrency(value?: number | null, digits = 2) {
  return `${formatNumber(value, digits)} 元`
}

export function formatScore(value?: number | null, digits = 1) {
  return `${formatNumber(value, digits)} 分`
}

export function formatCarbon(value?: number | null, digits = 2) {
  return `${formatNumber(value, digits)} kg`
}

export function formatDateTime(value?: string | null, fallback = '暂无') {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ')
  }

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(
    date.getHours(),
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function formatGender(value?: number | null) {
  if (value === 1) return '男'
  if (value === 2) return '女'
  return '-'
}

export function formatDormLabel(source?: DormLabelSource | null, fallback = '-') {
  if (!source) {
    return fallback
  }

  return source.dormNo || [source.dormBuilding, source.dormRoom].filter(Boolean).join('-') || fallback
}

export function formatFeeTypeText(value?: string | null) {
  return String(value || '').toUpperCase() === 'WATER' ? '水费' : '电费'
}

export function formatOperationTypeText(value?: string | null) {
  const normalized = String(value || '').toUpperCase()
  if (normalized === 'RECHARGE') return '充值'
  if (normalized === 'REFRESH') return '刷新'
  if (normalized === 'DEDUCT') return '周期扣费'
  return '消费'
}

export function formatPayTypeText(value?: string | null) {
  const normalized = String(value || '').toUpperCase()
  if (normalized === 'ALIPAY') return '支付宝'
  if (normalized === 'WECHAT') return '微信'
  if (normalized === 'CASH') return '现金'
  if (normalized === 'SYSTEM') return '系统'
  return '-'
}

export function formatUnitPrice(value?: number | null, unitName = '') {
  if (value === null || value === undefined) {
    return '-'
  }

  return `1${unitName} = ${formatNumber(value, 4)} 元`
}
