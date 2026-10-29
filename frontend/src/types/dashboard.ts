import type { OrderStatus } from './commerce'
import type { ProductVariantResponse } from './product'

export interface DashboardSummaryResponse {
  totalCustomers: number
  totalProducts: number
  totalOrders: number
  totalRevenue: number
  ordersByStatus: Record<OrderStatus, number>
  lowStockThreshold: number
  lowStockVariants: ProductVariantResponse[]
}

export type RevenueGrouping = 'DAY' | 'MONTH'

export interface RevenuePointResponse {
  period: string
  revenue: number
  orderCount: number
}

export interface TopProductResponse {
  productId: number
  productName: string
  quantitySold: number
  revenue: number
}
