import type { CatalogStatus } from './voucher'

export type ProductSort = 'NEWEST' | 'PRICE_ASC' | 'PRICE_DESC'

export interface CategoryResponse {
  id: number
  name: string
  status: CatalogStatus
}

export interface BrandResponse {
  id: number
  name: string
  status: CatalogStatus
}

export interface ProductSummaryResponse {
  id: number
  name: string
  categoryId: number
  categoryName: string
  brandId: number
  brandName: string
  status: CatalogStatus
  minPrice: number | null
  primaryImageUrl: string | null
  createdAt: string
}

export interface PublicProductQueryParams {
  q?: string
  categoryId?: number
  brandId?: number
  minPrice?: number
  maxPrice?: number
  sort?: ProductSort
  page?: number
  size?: number
}

export interface ProductRequest {
  name: string
  description: string | null
  categoryId: number
  brandId: number
}

export interface ProductVariantRequest {
  sku: string
  price: number
  stock: number
}

export interface ProductImageRequest {
  imageUrl: string
  sortOrder: number
}

export interface StockUpdateRequest {
  stock: number
}

export interface CloudImageUploadResponse {
  imageUrl: string
  publicId: string
  productImage: ProductImageResponse
}

export interface ProductVariantResponse {
  id: number
  productId: number
  productName: string
  sku: string
  price: number
  stock: number
  status: CatalogStatus
}

export interface ProductImageResponse {
  id: number
  imageUrl: string
  primaryImage: boolean
  sortOrder: number
}

export interface ProductDetailResponse {
  id: number
  name: string
  description: string | null
  categoryId: number
  categoryName: string
  brandId: number
  brandName: string
  status: CatalogStatus
  minPrice: number | null
  variants: ProductVariantResponse[]
  images: ProductImageResponse[]
  createdAt: string
  updatedAt: string
}
