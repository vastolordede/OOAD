import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  AdminReviewResponse,
  CreateReviewRequest,
  ProductReviewsResponse,
  ReviewResponse,
  ReviewStatus,
  UpdateReviewRequest,
} from '../types/review'

export async function getProductReviewsApi(
  productId: number,
  page: number,
  size: number,
): Promise<ProductReviewsResponse> {
  const response = await api.get<ApiResponse<ProductReviewsResponse>>(
    `/public/products/${productId}/reviews`,
    { params: { page, size } },
  )
  const reviews = response.data.data

  if (!response.data.success || !reviews) {
    throw new Error(response.data.message || 'Unable to load product reviews.')
  }

  return reviews
}

export async function createReviewApi(payload: CreateReviewRequest): Promise<ReviewResponse> {
  const response = await api.post<ApiResponse<ReviewResponse>>('/reviews', payload)
  const review = response.data.data

  if (!response.data.success || !review) {
    throw new Error(response.data.message || 'Unable to submit review.')
  }

  return review
}

export async function listMyReviewsApi(
  page: number,
  size: number,
): Promise<PageResponse<ReviewResponse>> {
  const response = await api.get<ApiResponse<PageResponse<ReviewResponse>>>('/reviews/mine', {
    params: { page, size },
  })
  const reviews = response.data.data

  if (!response.data.success || !reviews) {
    throw new Error(response.data.message || 'Unable to load your reviews.')
  }

  return reviews
}

export async function updateMyReviewApi(
  id: number,
  payload: UpdateReviewRequest,
): Promise<ReviewResponse> {
  const response = await api.put<ApiResponse<ReviewResponse>>(`/reviews/${id}`, payload)
  const review = response.data.data

  if (!response.data.success || !review) {
    throw new Error(response.data.message || 'Unable to update review.')
  }

  return review
}

export async function deleteMyReviewApi(id: number): Promise<void> {
  const response = await api.delete<ApiResponse<null>>(`/reviews/${id}`)

  if (!response.data.success) {
    throw new Error(response.data.message || 'Unable to delete review.')
  }
}

export async function listMyReviewedOrderItemIdsApi(
  orderItemIds: number[],
): Promise<number[]> {
  if (orderItemIds.length === 0) {
    return []
  }

  const orderItemIdSet = new Set(orderItemIds)
  const reviewedIds: number[] = []
  let pageNumber = 0
  let isLastPage = false

  while (!isLastPage && reviewedIds.length < orderItemIdSet.size) {
    const page = await listMyReviewsApi(pageNumber, 100)
    for (const review of page.content) {
      if (orderItemIdSet.has(review.orderItemId)) {
        reviewedIds.push(review.orderItemId)
      }
    }
    isLastPage = page.last
    pageNumber += 1
  }

  return reviewedIds
}

export async function listAdminReviewsApi(params: {
  status?: ReviewStatus
  productId?: number
  page: number
  size: number
}): Promise<PageResponse<AdminReviewResponse>> {
  const response = await api.get<ApiResponse<PageResponse<AdminReviewResponse>>>(
    '/admin/reviews',
    { params },
  )
  const reviews = response.data.data

  if (!response.data.success || !reviews) {
    throw new Error(response.data.message || 'Unable to load reviews.')
  }

  return reviews
}

export async function updateAdminReviewStatusApi(
  id: number,
  status: ReviewStatus,
): Promise<AdminReviewResponse> {
  const response = await api.patch<ApiResponse<AdminReviewResponse>>(
    `/admin/reviews/${id}/status`,
    null,
    { params: { status } },
  )
  const review = response.data.data

  if (!response.data.success || !review) {
    throw new Error(response.data.message || 'Unable to update review status.')
  }

  return review
}
