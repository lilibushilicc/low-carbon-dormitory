export interface StudentProfile {
  studentId: number
  dormId: number | null
  stuNum: string
  dormNo?: string
  name: string
  gender: number | null
  idCard: string
  phone: string
  college: string
  major: string
  className: string
  grade: string
  username: string
  signature: string
  admissionDate: string
  dormBuilding: string
  dormRoom: string
  dormType: string
  bedTotal: number | null
  bedAvailable: number | null
  carbonScore: number | null
}
