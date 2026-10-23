import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type {
  AddCartItemRequest,
  CartResponse,
  UpdateCartItemRequest,
} from '../types/commerce'

async function unwrapCartResponse(response: {
  data: ApiResponse<CartResponse>
}): Promise<CartResponse> {
  const cart = response.data.data

  if (!response.data.success || !cart) {
    throw new Error(response.data.message || 'Unable to load cart.')
  }

  return cart
}

export async function getCartApi(): Promise<CartResponse> {
  return unwrapCartResponse(await api.get<ApiResponse<CartResponse>>('/cart'))
}

export async function addCartItemApi(payload: AddCartItemRequest): Promise<CartResponse> {
  return unwrapCartResponse(await api.post<ApiResponse<CartResponse>>('/cart/items', payload))
}

export async function updateCartItemApi(
  itemId: number,
  payload: UpdateCartItemRequest,
): Promise<CartResponse> {
  return unwrapCartResponse(
    await api.put<ApiResponse<CartResponse>>(`/cart/items/${itemId}`, payload),
  )
}

export async function removeCartItemApi(itemId: number): Promise<CartResponse> {
  return unwrapCartResponse(
    await api.delete<ApiResponse<CartResponse>>(`/cart/items/${itemId}`),
  )
}

export async function clearCartApi(): Promise<CartResponse> {
  return unwrapCartResponse(await api.delete<ApiResponse<CartResponse>>('/cart'))
}
