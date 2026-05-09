import type { DormItem } from '@/api/modules/dashboard'

export type DormStatusKey = 'excellent' | 'good' | 'warning' | 'abnormal'

export interface DormStatusResult {
  key: DormStatusKey
  text: string
}

export const DORM_STATUS_TEXT: Record<DormStatusKey, string> = {
  excellent: '优秀',
  good: '良好',
  warning: '预警',
  abnormal: '异常',
}

export function getDormStatus(dorm: Pick<DormItem, 'carbonScore' | 'totalFee' | 'totalCarbon'>): DormStatusResult {
  const score = Number(dorm.carbonScore) || 0
  const totalFee = Number(dorm.totalFee) || 0
  const totalCarbon = Number(dorm.totalCarbon) || 0

  if (score < 0 || totalFee >= 10 || totalCarbon >= 15) {
    return { key: 'abnormal', text: DORM_STATUS_TEXT.abnormal }
  }

  if (score < 10 || totalFee >= 0.8 || totalCarbon >= 7) {
    return { key: 'warning', text: DORM_STATUS_TEXT.warning }
  }

  if (score >= 55 && totalFee <= 0.3 && totalCarbon <= 4.5) {
    return { key: 'excellent', text: DORM_STATUS_TEXT.excellent }
  }

  return { key: 'good', text: DORM_STATUS_TEXT.good }
}
