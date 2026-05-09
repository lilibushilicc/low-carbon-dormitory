import type { ApiResponse } from '@/types/api'
import { http } from '@/api/http'

export interface RewardItem {
  rewardId: number
  rewardName: string
  rewardDesc: string
  pointsCost: number
  imageUrl: string
  stock: number
  canExchange: boolean
  exchangeTip: string
}

export interface RewardRecord {
  recordId: number
  rewardName: string
  exchangePoints: number
  remark: string
  exchangeTime: string
}

export interface RewardCenter {
  currentPoints: number
  exchangeCount: number
  dormScoreSummary: {
    nearestRewardName: string | null
    gapToNearestReward: number
  }
  rewardItems: RewardItem[]
  exchangeRecords: RewardRecord[]
}

export interface RewardExchangeResult {
  rewardId: number
  rewardName: string
  spentPoints: number
  remainingPoints: number
  remainingStock: number
  exchangeTime: string
}

export function fetchRewardCenter(stuNum: string) {
  return http.get<ApiResponse<RewardCenter>>('/api/student/rewards', {
    params: { stuNum },
  })
}

export function exchangeReward(stuNum: string, rewardId: number) {
  return http.post<ApiResponse<RewardExchangeResult>>('/api/student/rewards/exchange', {
    stuNum,
    rewardId,
  })
}
