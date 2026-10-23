import type { PageResponse } from './api'

export type ReviewStatus = 'VISIBLE' | 'HIDDEN'

export interface CreateReviewRequest {
  orderItemId: number
  rating: number
  comment: string | null
}

export interface UpdateReviewRequest {
  rating: number
  comment: string | null
}

export interface ReviewResponse {
  id: number
  productId: number
  orderItemId: number
  reviewerName: string
  rating: number
  comment: string | null
  status: ReviewStatus
  createdAt: string
  updatedAt: string
}

export interface ProductReviewsResponse {
  averageRating: number
  reviewCount: number
  reviews: PageResponse<ReviewResponse>
}

export interface AdminReviewResponse {
  id: number
  productId: number
  productName: string
  orderItemId: number
  customerId: number
  customerEmail: string
  customerName: string
  rating: number
  comment: string | null
  status: ReviewStatus
  createdAt: string
}
