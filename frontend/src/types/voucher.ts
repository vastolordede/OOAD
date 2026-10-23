export type DiscountType = 'FIXED' | 'PERCENTAGE'
export type CatalogStatus = 'ACTIVE' | 'INACTIVE'

export interface VoucherRequest {
  code: string
  description: string | null
  discountType: DiscountType
  discountValue: number
  minOrderValue: number | null
  maxDiscount: number | null
  usageLimit: number | null
  startDate: string
  endDate: string
}

export interface VoucherResponse {
  id: number
  code: string
  description: string | null
  discountType: DiscountType
  discountValue: number
  minOrderValue: number
  maxDiscount: number | null
  usageLimit: number | null
  usedCount: number
  startDate: string
  endDate: string
  status: CatalogStatus
  createdAt: string
  updatedAt: string
}
