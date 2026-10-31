import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type {
  DashboardSummaryResponse,
  RevenueGrouping,
  RevenuePointResponse,
  TopProductResponse,
} from '../types/dashboard'

export async function getDashboardSummaryApi(
  lowStockThreshold = 10,
): Promise<DashboardSummaryResponse> {
  const response = await api.get<ApiResponse<DashboardSummaryResponse>>(
    '/admin/dashboard/summary',
    { params: { lowStockThreshold } },
  )
  const summary = response.data.data

  if (!response.data.success || !summary) {
    throw new Error(response.data.message || 'Unable to load dashboard summary.')
  }

  return summary
}

export async function getDashboardRevenueApi(params: {
  from?: string
  to?: string
  groupBy?: RevenueGrouping
} = {}): Promise<RevenuePointResponse[]> {
  const response = await api.get<ApiResponse<RevenuePointResponse[]>>(
    '/admin/dashboard/revenue',
    { params },
  )
  const revenue = response.data.data

  if (!response.data.success || !revenue) {
    throw new Error(response.data.message || 'Unable to load revenue report.')
  }

  return revenue
}

export async function getDashboardTopProductsApi(limit = 5): Promise<TopProductResponse[]> {
  const response = await api.get<ApiResponse<TopProductResponse[]>>(
    '/admin/dashboard/top-products',
    { params: { limit } },
  )
  const products = response.data.data

  if (!response.data.success || !products) {
    throw new Error(response.data.message || 'Unable to load top-selling products.')
  }

  return products
}
