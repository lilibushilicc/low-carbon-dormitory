import type { ApiResponse } from '@/types/api'
import { http } from '@/api/http'

export interface AdminLoginPayload {
  token: string
  adminId: number
  username: string
  displayName: string
}

export interface UtilityRateItem {
  feeType: 'ELECTRIC' | 'WATER' | string
  unitPrice: number
  unitName: string
  enabled: boolean
}

export interface AdminCreateStudentRequest {
  stuNum: string
  name: string
  password: string
  dormBuilding: string
  dormRoom: string
  bedTotal?: number
  gender?: number
  idCard?: string
  phone?: string
  college?: string
  major?: string
  className?: string
  grade?: string
  carbonScore?: number
}

export interface AdminCreateStudentResponse {
  studentId: number
  stuNum: string
  dormId: number
}

export interface AdminStudentListItem {
  studentId: number
  stuNum: string
  name: string
  gender?: number
  phone?: string
  college?: string
  major?: string
  className?: string
  grade?: string
  carbonScore?: number
  dormId?: number
  dormBuilding?: string
  dormRoom?: string
  createTime?: string
  updateTime?: string
}

export interface AdminStudentDeleteCheckResponse {
  studentId: number
  stuNum: string
  deletable: boolean
  rewardExchangeCount: number
  feeHistoryCount: number
  paymentOrderCount: number
  reason: string
}

export interface AdminRewardItem {
  rewardId: number
  rewardName: string
  rewardDesc: string
  pointsCost: number
  imageUrl?: string
  stock: number
  status: number
  sortOrder: number
  createTime?: string
  updateTime?: string
}

export interface AdminCreateRewardRequest {
  rewardName: string
  rewardDesc: string
  pointsCost: number
  imageUrl?: string
  stock: number
  sortOrder?: number
  status?: number
}

export interface AdminRewardImageUploadResponse {
  imageUrl: string
  originalName: string
  size: number
}

export interface AdminR2StorageConfig {
  endpoint: string
  accessKeyId: string
  secretAccessKey: string
  bucket: string
  publicBaseUrl: string
  region: string
  configured: boolean
}

export interface AdminDeductDormFeeRequest {
  amount: number
  feeType: 'ELECTRIC' | 'WATER'
  payType?: 'SYSTEM' | 'ALIPAY' | 'WECHAT' | 'CASH'
  payerName?: string
  payerAccount?: string
  remark?: string
}

export interface DormFeeInfo {
  dormId: number
  electricityBalance: number
  waterBalance: number
  lastDeductTime?: string
}

export function adminLogin(username: string, password: string) {
  return http.post<ApiResponse<AdminLoginPayload>>('/api/admin/login', { username, password })
}

export function fetchUtilityRates() {
  return http.get<ApiResponse<UtilityRateItem[]>>('/api/admin/utility-rates')
}

export function updateUtilityRate(feeType: string, payload: { unitPrice: number; unitName: string; enabled: boolean }) {
  return http.put<ApiResponse<UtilityRateItem>>(`/api/admin/utility-rates/${feeType}`, payload)
}

export function createStudentByAdmin(payload: AdminCreateStudentRequest) {
  return http.post<ApiResponse<AdminCreateStudentResponse>>('/api/admin/students', payload)
}

export function fetchStudentsByAdmin() {
  return http.get<ApiResponse<AdminStudentListItem[]>>('/api/admin/students')
}

export function fetchStudentDeleteCheckByAdmin(studentId: number) {
  return http.get<ApiResponse<AdminStudentDeleteCheckResponse>>(`/api/admin/students/${studentId}/delete-check`)
}

export function deleteStudentByAdmin(studentId: number) {
  return http.delete<ApiResponse<boolean>>(`/api/admin/students/${studentId}`)
}

export function fetchRewardsByAdmin() {
  return http.get<ApiResponse<AdminRewardItem[]>>('/api/admin/rewards')
}

export function fetchR2StorageConfigByAdmin() {
  return http.get<ApiResponse<AdminR2StorageConfig>>('/api/admin/storage/r2')
}

export function updateR2StorageConfigByAdmin(payload: Omit<AdminR2StorageConfig, 'configured'>) {
  return http.put<ApiResponse<AdminR2StorageConfig>>('/api/admin/storage/r2', payload)
}

export function testR2StorageConfigByAdmin(payload: Omit<AdminR2StorageConfig, 'configured'>) {
  return http.post<ApiResponse<boolean>>('/api/admin/storage/r2/test', payload)
}

export function createRewardByAdmin(payload: AdminCreateRewardRequest) {
  return http.post<ApiResponse<AdminRewardItem>>('/api/admin/rewards', payload)
}

export function uploadRewardImageByAdmin(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<ApiResponse<AdminRewardImageUploadResponse>>('/api/admin/rewards/upload-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export function updateRewardStockByAdmin(rewardId: number, stock: number) {
  return http.put<ApiResponse<AdminRewardItem>>(`/api/admin/rewards/${rewardId}/stock`, { stock })
}

export function deleteRewardByAdmin(rewardId: number) {
  return http.delete<ApiResponse<boolean>>(`/api/admin/rewards/${rewardId}`)
}

export function deductDormFeeByAdmin(dormId: number, payload: AdminDeductDormFeeRequest) {
  return http.post<ApiResponse<DormFeeInfo>>(`/api/admin/dorms/${dormId}/fees/deduct`, payload)
}
