import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type { CatalogStatus, VoucherRequest, VoucherResponse } from '../types/voucher'

export async function listVouchersApi(params: {
  q?: string
  status?: CatalogStatus
  page: number
  size: number
}): Promise<PageResponse<VoucherResponse>> {
  const response = await api.get<ApiResponse<PageResponse<VoucherResponse>>>(
    '/admin/vouchers',
    { params },
  )
  const page = response.data.data

  if (!response.data.success || !page) {
    throw new Error(response.data.message || 'Unable to load vouchers.')
  }

  return page
}

export async function getVoucherApi(id: number): Promise<VoucherResponse> {
  const response = await api.get<ApiResponse<VoucherResponse>>(`/admin/vouchers/${id}`)
  const voucher = response.data.data

  if (!response.data.success || !voucher) {
    throw new Error(response.data.message || 'Unable to load voucher.')
  }

  return voucher
}

export async function createVoucherApi(payload: VoucherRequest): Promise<VoucherResponse> {
  const response = await api.post<ApiResponse<VoucherResponse>>('/admin/vouchers', payload)
  const voucher = response.data.data

  if (!response.data.success || !voucher) {
    throw new Error(response.data.message || 'Unable to create voucher.')
  }

  return voucher
}

export async function updateVoucherApi(
  id: number,
  payload: VoucherRequest,
): Promise<VoucherResponse> {
  const response = await api.put<ApiResponse<VoucherResponse>>(
    `/admin/vouchers/${id}`,
    payload,
  )
  const voucher = response.data.data

  if (!response.data.success || !voucher) {
    throw new Error(response.data.message || 'Unable to update voucher.')
  }

  return voucher
}

export async function updateVoucherStatusApi(
  id: number,
  status: CatalogStatus,
): Promise<VoucherResponse> {
  const response = await api.patch<ApiResponse<VoucherResponse>>(
    `/admin/vouchers/${id}/status`,
    null,
    { params: { status } },
  )
  const voucher = response.data.data

  if (!response.data.success || !voucher) {
    throw new Error(response.data.message || 'Unable to update voucher status.')
  }

  return voucher
}
