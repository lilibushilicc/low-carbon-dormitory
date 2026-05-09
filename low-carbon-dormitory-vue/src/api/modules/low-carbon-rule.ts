import type { ApiResponse } from '@/types/api'
import { http } from '@/api/http'

export interface ScoreRuleConfig {
  ruleId?: number
  baseScore: number
  electricCarbonFactor: number
  waterCarbonFactor: number
  carbonPenaltyFactor: number
  weeklyDescription: string
  monthlyDescription: string
  rankingUpdateNote: string
  formulaText?: string
  updatedAt?: string
}

export interface HonorRuleConfig {
  honorRuleId?: number
  periodType: 'weekly' | 'monthly'
  scopeType: 'building' | 'college' | 'school'
  honorTitle: string
  badge: string
  rankType: 'RANK' | 'PERCENT'
  rankValue: number
  sortOrder: number
  status: number
}

export interface LowCarbonRuleConfig {
  scoreRule: ScoreRuleConfig
  honorRules: HonorRuleConfig[]
}

export interface LowCarbonRulePreview {
  electricFee: number
  waterFee: number
  electricUsage: number
  waterUsage: number
  electricCarbon: number
  waterCarbon: number
  totalCarbon: number
  score: number
  formulaText: string
  honorPreviewTexts: string[]
}

export function fetchLowCarbonRuleConfig() {
  return http.get<ApiResponse<LowCarbonRuleConfig>>('/api/admin/low-carbon-rules')
}

export function updateLowCarbonRuleConfig(payload: LowCarbonRuleConfig) {
  return http.put<ApiResponse<LowCarbonRuleConfig>>('/api/admin/low-carbon-rules', payload)
}

export function previewLowCarbonRule(payload: {
  electricFee: number
  waterFee: number
  scoreRule?: ScoreRuleConfig
}) {
  return http.post<ApiResponse<LowCarbonRulePreview>>('/api/admin/low-carbon-rules/preview', payload)
}

export function fetchStudentLowCarbonRules() {
  return http.get<ApiResponse<LowCarbonRuleConfig>>('/api/student/low-carbon-rules')
}
