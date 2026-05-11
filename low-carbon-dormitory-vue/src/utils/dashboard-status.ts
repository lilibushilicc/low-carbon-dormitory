import type { DormItem } from '@/api/modules/dashboard'

export type DormStatusKey = 'excellent' | 'good' | 'warning' | 'abnormal'

export interface DormStatusResult {
  key: DormStatusKey
  text: string
}

export interface DormStatusContext {
  comparableDormCount: number
}

export const DORM_STATUS_TEXT: Record<DormStatusKey, string> = {
  excellent: '优秀',
  good: '良好',
  warning: '预警',
  abnormal: '异常',
}

function hasComparableDormData(dorm: Pick<DormItem, 'carbonScore' | 'totalFee' | 'totalCarbon' | 'dataUpdatedAt'>) {
  const score = Number(dorm.carbonScore) || 0
  const totalFee = Number(dorm.totalFee) || 0
  const totalCarbon = Number(dorm.totalCarbon) || 0
  return Boolean(dorm.dataUpdatedAt) || score > 0 || totalFee > 0 || totalCarbon > 0
}

function resolveRankCutoff(total: number, ratio: number) {
  return Math.max(1, Math.ceil(total * ratio))
}

export function getDormStatus(
  dorm: Pick<DormItem, 'carbonScore' | 'totalFee' | 'totalCarbon' | 'carbonRank' | 'dataUpdatedAt'>,
  context: DormStatusContext,
): DormStatusResult {
  const score = Number(dorm.carbonScore) || 0
  const rank = Number(dorm.carbonRank) || 0
  const comparableDormCount = Math.max(Number(context.comparableDormCount) || 0, 1)

  if (score < 0) {
    return { key: 'abnormal', text: DORM_STATUS_TEXT.abnormal }
  }

  if (!hasComparableDormData(dorm)) {
    return { key: 'warning', text: DORM_STATUS_TEXT.warning }
  }

  if (!rank) {
    return { key: 'good', text: DORM_STATUS_TEXT.good }
  }

  const excellentCutoff = resolveRankCutoff(comparableDormCount, 0.15)
  const goodCutoff = resolveRankCutoff(comparableDormCount, 0.6)
  const warningCutoff = resolveRankCutoff(comparableDormCount, 0.85)

  if (rank <= excellentCutoff) {
    return { key: 'excellent', text: DORM_STATUS_TEXT.excellent }
  }

  if (rank <= goodCutoff) {
    return { key: 'good', text: DORM_STATUS_TEXT.good }
  }

  if (rank <= warningCutoff) {
    return { key: 'warning', text: DORM_STATUS_TEXT.warning }
  }

  return { key: 'abnormal', text: DORM_STATUS_TEXT.abnormal }
}
