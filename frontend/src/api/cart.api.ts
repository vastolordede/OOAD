import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { CartResponse } from '../types/commerce'

export async function getCartApi(): Promise<CartResponse> {
  const response = await api.get<ApiResponse<CartResponse>>('/cart')
  const cart = response.data.data

  if (!response.data.success || !cart) {
    throw new Error(response.data.message || 'Unable to load cart.')
  }

  return cart
}
