import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type {
  AuthResponse,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from '../types/auth'

export async function loginApi(payload: LoginRequest): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', payload)
  const authResponse = response.data.data

  if (!response.data.success || !authResponse) {
    throw new Error(response.data.message || 'Login failed.')
  }

  return authResponse
}

export async function registerApi(payload: RegisterRequest): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>('/auth/register', payload)
  const authResponse = response.data.data

  if (!response.data.success || !authResponse) {
    throw new Error(response.data.message || 'Registration failed.')
  }

  return authResponse
}

export async function forgotPasswordApi(
  payload: ForgotPasswordRequest,
): Promise<ForgotPasswordResponse> {
  const response = await api.post<ApiResponse<ForgotPasswordResponse>>(
    '/auth/forgot-password',
    payload,
  )
  const result = response.data.data

  if (!response.data.success || !result) {
    throw new Error(response.data.message || 'Unable to request a password reset.')
  }

  return result
}

export async function resetPasswordApi(payload: ResetPasswordRequest): Promise<void> {
  const response = await api.post<ApiResponse<null>>('/auth/reset-password', payload)

  if (!response.data.success) {
    throw new Error(response.data.message || 'Unable to reset password.')
  }
}
