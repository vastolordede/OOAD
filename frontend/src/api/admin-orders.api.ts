import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  AdminOrderResponse,
  OrderResponse,
  OrderStatus,
  OrderSummaryResponse,
} from '../types/commerce'

export interface AdminOrderListParams {
  page: number
  size: number
  status?: OrderStatus
  keyword?: string
  fromDate?: string
  toDate?: string
  sortBy?: 'createdAt' | 'totalAmount' | 'status' | 'id'
  direction?: 'asc' | 'desc'
}

export async function listAdminOrdersApi(
  params: AdminOrderListParams,
): Promise<PageResponse<OrderSummaryResponse>> {
  const response = await api.get<ApiResponse<PageResponse<OrderSummaryResponse>>>(
    '/admin/orders',
    { params },
  )
  const orders = response.data.data

  if (!response.data.success || !orders) {
    throw new Error(response.data.message || 'Unable to load orders.')
  }

  return orders
}

export async function getAdminOrderDetailApi(id: number): Promise<AdminOrderResponse> {
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
