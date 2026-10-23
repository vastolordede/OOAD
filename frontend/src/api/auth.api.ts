import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type { AuthResponse, LoginRequest } from '../types/auth'

export async function loginApi(payload: LoginRequest): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', payload)
  const authResponse = response.data.data

  if (!response.data.success || !authResponse) {
    throw new Error(response.data.message || 'Login failed.')
  }

  return authResponse
}
