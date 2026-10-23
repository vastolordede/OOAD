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
