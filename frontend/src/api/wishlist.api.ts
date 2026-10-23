import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'

export interface WishlistItemResponse {
  productId: number
  productName: string
  brandName: string
  minPrice: number | null
  imageUrl: string | null
  available: boolean
  addedAt: string
}

export async function listWishlistApi(page: number, size: number): Promise<PageResponse<WishlistItemResponse>> {
  const response = await api.get<ApiResponse<PageResponse<WishlistItemResponse>>>('/wishlist', {
    params: { page, size },
  })
  const wishlist = response.data.data

  if (!response.data.success || !wishlist) {
    throw new Error(response.data.message || 'Unable to load wishlist.')
  }

  return wishlist
}

export async function listWishlistProductIdsApi(): Promise<number[]> {
  const response = await api.get<ApiResponse<number[]>>('/wishlist/ids')
  const productIds = response.data.data

  if (!response.data.success || !productIds) {
    throw new Error(response.data.message || 'Unable to load wishlist products.')
  }

  return productIds
}

export async function addWishlistItemApi(productId: number): Promise<WishlistItemResponse> {
  const response = await api.post<ApiResponse<WishlistItemResponse>>('/wishlist/items', { productId })
  const item = response.data.data

  if (!response.data.success || !item) {
    throw new Error(response.data.message || 'Unable to add product to wishlist.')
  }

  return item
}

export async function removeWishlistItemApi(productId: number): Promise<void> {
  const response = await api.delete<ApiResponse<null>>(`/wishlist/items/${productId}`)

  if (!response.data.success) {
    throw new Error(response.data.message || 'Unable to remove product from wishlist.')
  }
}
