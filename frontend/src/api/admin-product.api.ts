import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type { CatalogStatus } from '../types/voucher'
import type {
  CloudImageUploadResponse,
  ProductDetailResponse,
  ProductImageRequest,
  ProductImageResponse,
  ProductRequest,
  ProductSummaryResponse,
  ProductVariantRequest,
  ProductVariantResponse,
  StockUpdateRequest,
} from '../types/product'

export async function listAdminProductsApi(params: {
  q?: string
  status?: CatalogStatus
  page: number
  size: number
}): Promise<PageResponse<ProductSummaryResponse>> {
  const response = await api.get<ApiResponse<PageResponse<ProductSummaryResponse>>>(
    '/admin/products',
    {
      params: {
        ...(params.q?.trim() ? { q: params.q.trim() } : {}),
        ...(params.status ? { status: params.status } : {}),
        page: params.page,
        size: params.size,
      },
    },
  )
  const products = response.data.data
  if (!response.data.success || !products) {
    throw new Error(response.data.message || 'Unable to load products.')
  }
  return products
}

export async function createAdminProductApi(payload: ProductRequest): Promise<ProductDetailResponse> {
  const response = await api.post<ApiResponse<ProductDetailResponse>>('/admin/products', payload)
  const product = response.data.data
  if (!response.data.success || !product) {
    throw new Error(response.data.message || 'Unable to create product.')
  }
  return product
}

export async function updateAdminProductApi(
  id: number,
  payload: ProductRequest,
): Promise<ProductDetailResponse> {
  const response = await api.put<ApiResponse<ProductDetailResponse>>(
    `/admin/products/${id}`,
    payload,
  )
  const product = response.data.data
  if (!response.data.success || !product) {
    throw new Error(response.data.message || 'Unable to update product.')
  }
  return product
}

export async function updateAdminProductStatusApi(
  id: number,
  status: CatalogStatus,
): Promise<ProductDetailResponse> {
  const response = await api.patch<ApiResponse<ProductDetailResponse>>(
    `/admin/products/${id}/status`,
    null,
    { params: { status } },
  )
  const product = response.data.data
  if (!response.data.success || !product) {
    throw new Error(response.data.message || 'Unable to update product status.')
  }
  return product
}

export async function createProductVariantApi(
  productId: number,
  payload: ProductVariantRequest,
): Promise<ProductVariantResponse> {
  const response = await api.post<ApiResponse<ProductVariantResponse>>(
    `/admin/products/${productId}/variants`,
    payload,
  )
  const variant = response.data.data
  if (!response.data.success || !variant) {
    throw new Error(response.data.message || 'Unable to create variant.')
  }
  return variant
}

export async function updateProductVariantApi(
  variantId: number,
  payload: ProductVariantRequest,
): Promise<ProductVariantResponse> {
  const response = await api.put<ApiResponse<ProductVariantResponse>>(
    `/admin/variants/${variantId}`,
    payload,
  )
  const variant = response.data.data
  if (!response.data.success || !variant) {
    throw new Error(response.data.message || 'Unable to update variant.')
  }
  return variant
}

export async function updateProductVariantStatusApi(
  variantId: number,
  status: CatalogStatus,
): Promise<ProductVariantResponse> {
  const response = await api.patch<ApiResponse<ProductVariantResponse>>(
    `/admin/variants/${variantId}/status`,
    null,
    { params: { status } },
  )
  const variant = response.data.data
  if (!response.data.success || !variant) {
    throw new Error(response.data.message || 'Unable to update variant status.')
  }
  return variant
}

export async function addProductImageApi(
  productId: number,
  payload: ProductImageRequest,
): Promise<ProductImageResponse> {
  const response = await api.post<ApiResponse<ProductImageResponse>>(
    `/admin/products/${productId}/images`,
    payload,
  )
  const image = response.data.data
  if (!response.data.success || !image) {
    throw new Error(response.data.message || 'Unable to add product image.')
  }
  return image
}

export async function deleteProductImageApi(productId: number, imageId: number): Promise<void> {
  const response = await api.delete<ApiResponse<null>>(
    `/admin/products/${productId}/images/${imageId}`,
  )
  if (!response.data.success) {
    throw new Error(response.data.message || 'Unable to delete product image.')
  }
}

export async function setPrimaryProductImageApi(
  productId: number,
  imageId: number,
): Promise<ProductImageResponse> {
  const response = await api.put<ApiResponse<ProductImageResponse>>(
    `/admin/products/${productId}/images/${imageId}/primary`,
  )
  const image = response.data.data
  if (!response.data.success || !image) {
    throw new Error(response.data.message || 'Unable to set primary image.')
  }
  return image
}

export async function uploadProductImageFileApi(
  productId: number,
  file: File,
  sortOrder: number,
): Promise<CloudImageUploadResponse> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await api.post<ApiResponse<CloudImageUploadResponse>>(
    `/admin/products/${productId}/images/upload`,
    formData,
    { params: { sortOrder } },
  )
  const upload = response.data.data
  if (!response.data.success || !upload) {
    throw new Error(response.data.message || 'Unable to upload product image.')
  }
  return upload
}

export async function updateVariantStockApi(
  variantId: number,
  payload: StockUpdateRequest,
): Promise<ProductVariantResponse> {
  const response = await api.put<ApiResponse<ProductVariantResponse>>(
    `/admin/inventory/variants/${variantId}/stock`,
    payload,
  )
  const variant = response.data.data
  if (!response.data.success || !variant) {
    throw new Error(response.data.message || 'Unable to update stock.')
  }
  return variant
}

export async function listLowStockVariantsApi(params: {
  threshold: number
  page: number
  size: number
}): Promise<PageResponse<ProductVariantResponse>> {
  const response = await api.get<ApiResponse<PageResponse<ProductVariantResponse>>>(
    '/admin/inventory/low-stock',
    { params },
  )
  const variants = response.data.data
  if (!response.data.success || !variants) {
    throw new Error(response.data.message || 'Unable to load low-stock variants.')
  }
  return variants
}
