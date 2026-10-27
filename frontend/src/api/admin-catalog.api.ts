import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type { BrandResponse, CategoryResponse } from '../types/product'
import type { CatalogStatus } from '../types/voucher'

export interface CatalogNameRequest {
  name: string
}

export async function listAdminCategoriesApi(params: {
  page: number
  size: number
}): Promise<PageResponse<CategoryResponse>> {
  const response = await api.get<ApiResponse<PageResponse<CategoryResponse>>>(
    '/admin/categories',
    { params },
  )
  const categories = response.data.data
  if (!response.data.success || !categories) {
    throw new Error(response.data.message || 'Unable to load categories.')
  }
  return categories
}

export async function createAdminCategoryApi(
  payload: CatalogNameRequest,
): Promise<CategoryResponse> {
  const response = await api.post<ApiResponse<CategoryResponse>>('/admin/categories', payload)
  const category = response.data.data
  if (!response.data.success || !category) {
    throw new Error(response.data.message || 'Unable to create category.')
  }
  return category
}

export async function updateAdminCategoryApi(
  id: number,
  payload: CatalogNameRequest,
): Promise<CategoryResponse> {
  const response = await api.put<ApiResponse<CategoryResponse>>(
    `/admin/categories/${id}`,
    payload,
  )
  const category = response.data.data
  if (!response.data.success || !category) {
    throw new Error(response.data.message || 'Unable to update category.')
  }
  return category
}

export async function updateAdminCategoryStatusApi(
  id: number,
  status: CatalogStatus,
): Promise<CategoryResponse> {
  const response = await api.patch<ApiResponse<CategoryResponse>>(
    `/admin/categories/${id}/status`,
    null,
    { params: { status } },
  )
  const category = response.data.data
  if (!response.data.success || !category) {
    throw new Error(response.data.message || 'Unable to update category status.')
  }
  return category
}

export async function listAdminBrandsApi(params: {
  page: number
  size: number
}): Promise<PageResponse<BrandResponse>> {
  const response = await api.get<ApiResponse<PageResponse<BrandResponse>>>('/admin/brands', {
    params,
  })
  const brands = response.data.data
  if (!response.data.success || !brands) {
    throw new Error(response.data.message || 'Unable to load brands.')
  }
  return brands
}

export async function createAdminBrandApi(payload: CatalogNameRequest): Promise<BrandResponse> {
  const response = await api.post<ApiResponse<BrandResponse>>('/admin/brands', payload)
  const brand = response.data.data
  if (!response.data.success || !brand) {
    throw new Error(response.data.message || 'Unable to create brand.')
  }
  return brand
}

export async function updateAdminBrandApi(
  id: number,
  payload: CatalogNameRequest,
): Promise<BrandResponse> {
  const response = await api.put<ApiResponse<BrandResponse>>(`/admin/brands/${id}`, payload)
  const brand = response.data.data
  if (!response.data.success || !brand) {
    throw new Error(response.data.message || 'Unable to update brand.')
  }
  return brand
}

export async function updateAdminBrandStatusApi(
  id: number,
  status: CatalogStatus,
): Promise<BrandResponse> {
  const response = await api.patch<ApiResponse<BrandResponse>>(
    `/admin/brands/${id}/status`,
    null,
    { params: { status } },
  )
  const brand = response.data.data
  if (!response.data.success || !brand) {
    throw new Error(response.data.message || 'Unable to update brand status.')
  }
  return brand
}
