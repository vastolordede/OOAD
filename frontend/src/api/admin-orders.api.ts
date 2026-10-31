import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type { AdminOrderResponse, OrderResponse, OrderStatus, OrderSummaryResponse } from '../types/commerce'

export interface AdminOrderFilters {
  status?: OrderStatus
  keyword?: string
  fromDate?: string
  toDate?: string
  page: number
  size: number
  sortBy?: 'createdAt' | 'totalAmount' | 'status' | 'id'
  direction?: 'asc' | 'desc'
}

export async function listAdminOrdersApi(
  filters: AdminOrderFilters,
): Promise<PageResponse<OrderSummaryResponse>> {
  const response = await api.get<ApiResponse<PageResponse<OrderSummaryResponse>>>(
    '/admin/orders',
    { params: filters },
  )
  const page = response.data.data

  if (!response.data.success || !page) {
    throw new Error(response.data.message || 'Unable to load orders.')
  }

  return page
}

export async function getAdminOrderApi(id: number): Promise<AdminOrderResponse> {
  const response = await api.get<ApiResponse<AdminOrderResponse>>(`/admin/orders/${id}`)
  const order = response.data.data

  if (!response.data.success || !order) {
    throw new Error(response.data.message || 'Unable to load order details.')
  }

  return order
}

export async function updateAdminOrderStatusApi(
  id: number,
  status: OrderStatus,
): Promise<OrderResponse> {
  const response = await api.patch<ApiResponse<OrderResponse>>(
    `/admin/orders/${id}/status`,
    { status },
  )
  const order = response.data.data

  if (!response.data.success || !order) {
    throw new Error(response.data.message || 'Unable to update order status.')
  }

  return order
}
