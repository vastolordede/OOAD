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

/** Backend controls which payment modes are enabled for the active environment. */
export interface PaymentModesResponse {
  vnpayAvailable: boolean
  simulationEnabled: boolean
}

export interface VnPayReturnResponse {
  paid: boolean
  code: string
  message: string
  orderId: number | null
}

export async function getPaymentModesApi(): Promise<PaymentModesResponse> {
  const response = await api.get<ApiResponse<PaymentModesResponse>>('/public/payment/modes')
  const modes = response.data.data
  if (!response.data.success || !modes) {
    throw new Error(response.data.message || 'Unable to load available payment methods.')
  }
  return modes
}

export async function createVnPayPaymentUrlApi(orderId: number): Promise<string> {
  const response = await api.post<ApiResponse<{ paymentUrl: string }>>(
    `/orders/${orderId}/payment/vnpay`,
  )
  const result = response.data.data
  if (!response.data.success || !result?.paymentUrl) {
    throw new Error(response.data.message || 'Unable to create VNPay payment link.')
  }
  // Avoid navigating to an insecure or malformed payment URL.
  const url = new URL(result.paymentUrl)
  if (url.protocol !== 'https:') {
    throw new Error('Payment provider URL must use HTTPS.')
  }
  return url.toString()
}

export async function verifyVnPayReturnApi(query: string): Promise<VnPayReturnResponse> {
  const response = await api.get<ApiResponse<VnPayReturnResponse>>(
    `/public/payment/vnpay/return${query}`,
  )
  const result = response.data.data
  if (!response.data.success || !result) {
    throw new Error(response.data.message || 'Unable to verify VNPay response.')
  }
  return result
}
