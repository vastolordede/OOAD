import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { CheckoutPreviewRequest, CheckoutPreviewResponse } from '../types/commerce'

export async function previewCheckoutApi(
  payload: CheckoutPreviewRequest,
): Promise<CheckoutPreviewResponse> {
  const response = await api.post<ApiResponse<CheckoutPreviewResponse>>(
    '/checkout/preview',
    payload,
  )
  const preview = response.data.data

  if (!response.data.success || !preview) {
    throw new Error(response.data.message || 'Unable to preview checkout.')
  }

  return preview
}
