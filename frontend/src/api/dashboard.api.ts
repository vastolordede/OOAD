import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { DashboardSummaryResponse } from '../types/dashboard'

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
