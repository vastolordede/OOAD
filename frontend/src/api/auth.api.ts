import { api } from './axios'
import type { ApiResponse, AuthResponse, LoginRequest } from '../types/auth'

export async function loginApi(payload: LoginRequest): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', payload)
  const authResponse = response.data.data

  if (!response.data.success || !authResponse) {
    throw new Error(response.data.message || 'Login failed.')
  }

  return authResponse
}
