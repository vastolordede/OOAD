import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { AddressRequest, AddressResponse } from '../types/commerce'

export async function listAddressesApi(): Promise<AddressResponse[]> {
  const response = await api.get<ApiResponse<AddressResponse[]>>('/users/me/addresses')
  const addresses = response.data.data

  if (!response.data.success || !addresses) {
    throw new Error(response.data.message || 'Unable to load addresses.')
  }

  return addresses
}

export async function createAddressApi(payload: AddressRequest): Promise<AddressResponse> {
  const response = await api.post<ApiResponse<AddressResponse>>(
    '/users/me/addresses',
    payload,
  )
  const address = response.data.data

  if (!response.data.success || !address) {
    throw new Error(response.data.message || 'Unable to create address.')
  }

  return address
}

export async function updateAddressApi(
  id: number,
  payload: AddressRequest,
): Promise<AddressResponse> {
  const response = await api.put<ApiResponse<AddressResponse>>(
    `/users/me/addresses/${id}`,
    payload,
  )
  const address = response.data.data

  if (!response.data.success || !address) {
    throw new Error(response.data.message || 'Unable to update address.')
  }

  return address
}

export async function deleteAddressApi(id: number): Promise<void> {
  const response = await api.delete<ApiResponse<null>>(`/users/me/addresses/${id}`)

  if (!response.data.success) {
    throw new Error(response.data.message || 'Unable to delete address.')
  }
}

export async function setDefaultAddressApi(id: number): Promise<AddressResponse> {
  const response = await api.put<ApiResponse<AddressResponse>>(
    `/users/me/addresses/${id}/default`,
  )
  const address = response.data.data

  if (!response.data.success || !address) {
    throw new Error(response.data.message || 'Unable to set default address.')
  }

  return address
}
