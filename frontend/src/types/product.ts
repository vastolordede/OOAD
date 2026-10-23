import type { CatalogStatus } from './voucher'

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
