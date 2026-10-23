import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { CreateOrderRequest, OrderResponse } from '../types/commerce'

export async function createOrderApi(payload: CreateOrderRequest): Promise<OrderResponse> {
  const response = await api.post<ApiResponse<OrderResponse>>('/orders', payload)
  const order = response.data.data

  if (!response.data.success || !order) {
    throw new Error(response.data.message || 'Unable to create order.')
  }

  return order
}
