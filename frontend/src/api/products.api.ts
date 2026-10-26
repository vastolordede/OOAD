import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  BrandResponse,
  CategoryResponse,
  ProductDetailResponse,
  ProductSort,
  ProductSummaryResponse,
  PublicProductQueryParams,
} from '../types/product'

function isValidProductSort(value: ProductSort | undefined): value is ProductSort {
  return value === 'NEWEST' || value === 'PRICE_ASC' || value === 'PRICE_DESC'
}

function buildPublicProductQuery(params: PublicProductQueryParams = {}): Record<string, string | number> {
  const query: Record<string, string | number> = {}
  const searchTerm = params.q?.trim()

  if (searchTerm) {
    query.q = searchTerm
  }
  if (Number.isInteger(params.categoryId) && params.categoryId !== undefined && params.categoryId > 0) {
    query.categoryId = params.categoryId
  }
  if (Number.isInteger(params.brandId) && params.brandId !== undefined && params.brandId > 0) {
    query.brandId = params.brandId
  }
  if (Number.isFinite(params.minPrice) && params.minPrice !== undefined && params.minPrice >= 0) {
    query.minPrice = params.minPrice
  }
  if (Number.isFinite(params.maxPrice) && params.maxPrice !== undefined && params.maxPrice >= 0) {
    query.maxPrice = params.maxPrice
  }
  if (isValidProductSort(params.sort)) {
    query.sort = params.sort
  }
  if (Number.isInteger(params.page) && params.page !== undefined && params.page >= 0) {
    query.page = params.page
  }
  if (Number.isInteger(params.size) && params.size !== undefined && params.size > 0) {
    query.size = params.size
  }

  return query
}

export async function listPublicCategoriesApi(): Promise<CategoryResponse[]> {
  const response = await api.get<ApiResponse<CategoryResponse[]>>('/public/categories')
  const categories = response.data.data

  if (!response.data.success || !categories) {
    throw new Error(response.data.message || 'Unable to load categories.')
  }

  return categories
}

export async function listPublicBrandsApi(): Promise<BrandResponse[]> {
  const response = await api.get<ApiResponse<BrandResponse[]>>('/public/brands')
  const brands = response.data.data

  if (!response.data.success || !brands) {
    throw new Error(response.data.message || 'Unable to load brands.')
  }

  return brands
}

export async function listPublicProductsApi(
  params?: PublicProductQueryParams,
): Promise<PageResponse<ProductSummaryResponse>> {
  const response = await api.get<ApiResponse<PageResponse<ProductSummaryResponse>>>(
    '/public/products',
    { params: buildPublicProductQuery(params) },
  )
  const products = response.data.data

  if (!response.data.success || !products) {
    throw new Error(response.data.message || 'Unable to load products.')
  }

  return products
}

export async function getProductDetailApi(id: number): Promise<ProductDetailResponse> {
  const response = await api.get<ApiResponse<ProductDetailResponse>>(`/public/products/${id}`)
  const product = response.data.data

  if (!response.data.success || !product) {
    throw new Error(response.data.message || 'Unable to load product.')
  }

  return product
}

export const getPublicProductDetailApi = getProductDetailApi
