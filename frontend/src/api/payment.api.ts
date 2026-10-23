import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { PaymentResponse } from '../types/commerce'

export async function getPaymentApi(orderId: number): Promise<PaymentResponse> {
  const response = await api.get<ApiResponse<PaymentResponse>>(`/orders/${orderId}/payment`)
  const payment = response.data.data

  if (!response.data.success || !payment) {
    throw new Error(response.data.message || 'Unable to load payment status.')
  }

  return payment
}

export async function confirmPaymentApi(orderId: number): Promise<PaymentResponse> {
  const response = await api.post<ApiResponse<PaymentResponse>>(
    `/orders/${orderId}/payment/confirm`,
  )
  const payment = response.data.data

  if (!response.data.success || !payment) {
    throw new Error(response.data.message || 'Unable to confirm payment.')
  }

  return payment
}

export async function failPaymentApi(orderId: number): Promise<PaymentResponse> {
  const response = await api.post<ApiResponse<PaymentResponse>>(
    `/orders/${orderId}/payment/fail`,
  )
  const payment = response.data.data

  if (!response.data.success || !payment) {
    throw new Error(response.data.message || 'Unable to mark payment as failed.')
  }

  return payment
}
