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
