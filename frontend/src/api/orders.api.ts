import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  CreateOrderRequest,
  OrderResponse,
  OrderStatus,
  OrderSummaryResponse,
} from '../types/commerce'

export async function createOrderApi(payload: CreateOrderRequest): Promise<OrderResponse> {
  const response = await api.post<ApiResponse<OrderResponse>>('/orders', payload)
  const order = response.data.data

  if (!response.data.success || !order) {
    throw new Error(response.data.message || 'Unable to create order.')
  }

  return order
}

export async function listMyOrdersApi(params: {
  page: number
  size: number
  status?: OrderStatus
}): Promise<PageResponse<OrderSummaryResponse>> {
  const response = await api.get<ApiResponse<PageResponse<OrderSummaryResponse>>>('/orders', {
    params,
  })
  const page = response.data.data

  if (!response.data.success || !page) {
    throw new Error(response.data.message || 'Unable to load orders.')
  }

  return page
}

export async function getOrderDetailApi(id: number): Promise<OrderResponse> {
  const response = await api.get<ApiResponse<OrderResponse>>(`/orders/${id}`)
  const order = response.data.data

  if (!response.data.success || !order) {
    throw new Error(response.data.message || 'Unable to load order details.')
  }

  return order
}

export async function cancelOrderApi(id: number): Promise<OrderResponse> {
  const response = await api.put<ApiResponse<OrderResponse>>(`/orders/${id}/cancel`)
  const order = response.data.data

  if (!response.data.success || !order) {
    throw new Error(response.data.message || 'Unable to cancel order.')
  }

  return order
}
