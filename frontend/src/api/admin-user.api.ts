import { api } from './axios'
import type { ApiResponse, PageResponse } from '../types/api'
import type { UserResponse } from '../types/auth'

export async function listAdminUsersApi(params: {
  q?: string
  page: number
  size: number
}): Promise<PageResponse<UserResponse>> {
  const response = await api.get<ApiResponse<PageResponse<UserResponse>>>('/admin/users', {
    params: {
      ...(params.q?.trim() ? { q: params.q.trim() } : {}),
      page: params.page,
      size: params.size,
    },
  })
  const users = response.data.data

  if (!response.data.success || !users) {
    throw new Error(response.data.message || 'Unable to load users.')
  }
  return users
}

export async function getAdminUserDetailApi(id: number): Promise<UserResponse> {
  const response = await api.get<ApiResponse<UserResponse>>(`/admin/users/${id}`)
  const user = response.data.data

  if (!response.data.success || !user) {
    throw new Error(response.data.message || 'Unable to load user details.')
  }
  return user
}

async function updateAdminUserStatus(
  id: number,
  status: 'enable' | 'disable',
): Promise<UserResponse> {
  const response = await api.put<ApiResponse<UserResponse>>(`/admin/users/${id}/${status}`)
  const user = response.data.data

  if (!response.data.success || !user) {
    throw new Error(response.data.message || `Unable to ${status} user.`)
  }
  return user
}

export function enableAdminUserApi(id: number): Promise<UserResponse> {
  return updateAdminUserStatus(id, 'enable')
}

export function disableAdminUserApi(id: number): Promise<UserResponse> {
  return updateAdminUserStatus(id, 'disable')
}
