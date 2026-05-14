import type { ApiResponse } from '@/types/api'
import type { StudentProfile } from '@/types/student'
import { http } from '@/api/http'

export interface LoginPayload {
  token: string
  stuNum: string
  username: string
  studentInfo: StudentProfile
}

export interface StudentWaterElectricity {
  stuNum: string
  dormId: number | null
  dormNo?: string
  dormBuilding: string
  dormRoom: string
  electricityBalance: number | null
  waterBalance: number | null
  electricityAvailable: number | null
  waterAvailable: number | null
  electricityUnitPrice: number | null
  waterUnitPrice: number | null
  electricityUnitName: string | null
  waterUnitName: string | null
  electricityBillingEnabled?: boolean | null
  waterBillingEnabled?: boolean | null
  lastDeductTime: string | null
  dormCarbonScore?: number | null
  personalCarbonScore?: number | null
  carbonPointsAdded?: number | null
  personalPointsAdded?: number | null
}

export interface FeeHistoryRecord {
  id: number
  feeType: string
  operationType: string
  payType: string
  payerName: string
  payerStuNum: string
  amount: number
  balanceAfter: number
  createTime: string
}

export interface FeeHistoryPage {
  records: FeeHistoryRecord[]
}

export interface PayRequest {
  stuNum: string
  dormId: number
  amount: number
  feeType: 'water' | 'electric'
  payType: string
  payerAccount: string
}

export interface PaymentOrder {
  orderNo: string
  status: string
  amount: number
  feeType: string
  payType: string
  qrCodeContent: string | null
  qrCodeImageUrl: string | null
  thirdTradeNo: string | null
  createTime: string | null
  paidTime: string | null
  carbonPointsAdded?: number | null
  personalPointsAdded?: number | null
}

export function loginStudent(username: string, password: string) {
  return http.post<ApiResponse<LoginPayload>>('/api/student/login', {
    username,
    password,
  })
}

export function fetchStudentProfile(stuNum: string) {
  return http.get<ApiResponse<StudentProfile>>('/api/student/profile', {
    params: { stuNum },
  })
}

export function fetchWaterElectricity(params: { stuNum?: string; dormId?: string | number }) {
  return http.get<ApiResponse<StudentWaterElectricity>>('/api/student/water-electricity', {
    params,
  })
}

export function refreshWaterElectricity(params: { stuNum?: string; dormId?: string | number }) {
  return http.post<ApiResponse<StudentWaterElectricity>>('/api/student/water-electricity/refresh', null, {
    params,
  })
}

export function fetchFeeHistory(params: { dormId: string | number; pageNum?: number; pageSize?: number }) {
  return http.get<ApiResponse<FeeHistoryPage>>('/api/student/fee-history', {
    params,
  })
}

export function submitStudentPayment(payload: PayRequest) {
  return http.post<ApiResponse<unknown>>('/api/student/pay', payload)
}

export function createStudentPaymentOrder(payload: PayRequest) {
  return http.post<ApiResponse<PaymentOrder>>('/api/student/payments/orders', payload)
}

export function fetchStudentPaymentOrder(orderNo: string, stuNum: string) {
  return http.get<ApiResponse<PaymentOrder>>(`/api/student/payments/orders/${orderNo}`, {
    params: { stuNum },
  })
}

export function simulateStudentPaymentSuccess(orderNo: string, stuNum: string) {
  return http.post<ApiResponse<PaymentOrder>>(`/api/student/payments/orders/${orderNo}/simulate-success`, null, {
    params: { stuNum },
  })
}
