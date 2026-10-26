import { api } from './axios'
import type { ApiResponse } from '../types/api'
import type {
  ChangePasswordRequest,
  UpdateProfileRequest,
  UserResponse,
} from '../types/auth'

export async function getMyProfileApi(): Promise<UserResponse> {
  const response = await api.get<ApiResponse<UserResponse>>('/users/me')
  const profile = response.data.data

  if (!response.data.success || !profile) {
    throw new Error(response.data.message || 'Unable to load profile.')
  }

  return profile
}

export async function updateMyProfileApi(
  payload: UpdateProfileRequest,
): Promise<UserResponse> {
  const response = await api.put<ApiResponse<UserResponse>>('/users/me', payload)
  const profile = response.data.data

  if (!response.data.success || !profile) {
    throw new Error(response.data.message || 'Unable to update profile.')
  }

  return profile
}

export async function changeMyPasswordApi(
  payload: ChangePasswordRequest,
): Promise<void> {
  const response = await api.put<ApiResponse<null>>('/users/me/password', payload)

  if (!response.data.success) {
    throw new Error(response.data.message || 'Unable to change password.')
  }
}
