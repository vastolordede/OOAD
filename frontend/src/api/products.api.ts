import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { ProductDetailResponse } from '../types/product'

export async function getProductDetailApi(id: number): Promise<ProductDetailResponse> {
  const response = await api.get<ApiResponse<ProductDetailResponse>>(`/public/products/${id}`)
  const product = response.data.data

  if (!response.data.success || !product) {
    throw new Error(response.data.message || 'Unable to load product.')
  }

  return product
}
