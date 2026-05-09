import type { ApiResponse } from '@/types/api'
import { http } from '@/api/http'

export type SortKey = 'carbonScore' | 'electricFee' | 'waterFee'

export interface DormAnchor {
  dormLabel: string
  building: string
  college: string | null
  residentCount: number
  residents: string[]
  carbonLevel: string
  currentPeriodParticipating: boolean
  dataUpdatedAt: string | null
}

export interface PersonalOverview {
  electricFee: number
  waterFee: number
  totalFee: number
  electricUsage: number
  waterUsage: number
  totalCarbon: number
  perCapitaCarbon: number
  carbonScore: number
}

export interface ComparisonLevel {
  rank: number
  totalDormCount: number
  currentPeriodParticipating: boolean
  percentileText: string
}

export interface Recommendation {
  title: string
  detail: string
  priority: string
}

export interface TrendPoint {
  label: string
  carbonScore: number
  schoolRank: number
}

export interface SimpleDormBenchmark {
  dormLabel: string
  totalCarbon: number
  carbonScore: number
  rank: number
}

export interface PersonalDashboardData {
  periodLabel: string
  lastUpdatedAt: string
  scoreSummary: {
    dormPoints: number
    currentPeriodScore: number
    currentSchoolRank: number
    rankedSchoolDormCount: number
    currentPeriodParticipating: boolean
    scoreDelta: number
  }
  ruleSummary: {
    baseScore: number
    electricCarbonFactor: number
    waterCarbonFactor: number
    carbonPenaltyFactor: number
    rankingUpdateNote: string
    formulaText: string
  }
  dormAnchor: DormAnchor
  overview: PersonalOverview
  comparisons: {
    building: ComparisonLevel
    college: ComparisonLevel
    school: ComparisonLevel
  }
  recommendations: Recommendation[]
  trends: {
    points: TrendPoint[]
    buildingTopDorms: SimpleDormBenchmark[]
    collegeTopDorms: SimpleDormBenchmark[]
  }
}

export interface BuildingItem {
  rank: number
  name: string
  dormCount: number
  rankedDormCount: number
  totalFee: number
  avgFee: number
  totalCarbon: number
  avgScore: number
}

export interface DormItem {
  dormId: number
  building: string
  room: string
  label: string
  residentCount: number
  residents: string[]
  currentDorm: boolean
  electricFee: number
  waterFee: number
  totalFee: number
  electricUsage: number
  waterUsage: number
  electricCarbon: number
  waterCarbon: number
  totalCarbon: number
  carbonScore: number
  energyRank: number
  carbonRank: number
  currentPeriodParticipating: boolean
  currentPeriodStatus: string
  dataUpdatedAt: string | null
  currentPeriodUpdatedAt: string | null
}

export interface GlobalDashboardData {
  period: string
  periodLabel: string
  currentDormId: number | null
  currentDormLabel: string | null
  rangeStart: string
  rangeEnd: string
  lastUpdatedAt: string
  electricUnitPrice: number
  waterUnitPrice: number
  electricCarbonFactor: number
  waterCarbonFactor: number
  overview: {
    dormCount: number
    rankedDormCount: number
    totalElectricUsage: number
    totalWaterUsage: number
    totalCarbon: number
    averageScore: number
    bestDormLabel: string | null
    highestCarbonDormLabel: string | null
  }
  buildings: BuildingItem[]
  dorms: DormItem[]
}

export function fetchPersonalDashboard(params: { stuNum?: string; dormId?: number }) {
  return http.get<ApiResponse<PersonalDashboardData>>('/api/student/low-carbon-dashboard-personal', {
    params,
  })
}

export function fetchGlobalDashboard(params: { stuNum?: string; dormId?: number }) {
  return http.get<ApiResponse<GlobalDashboardData>>('/api/student/low-carbon-dashboard', {
    params,
  })
}
