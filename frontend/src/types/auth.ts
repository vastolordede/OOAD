export type UserRole = 'CUSTOMER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED'

export interface UserResponse {
  id: number
  email: string
  fullName: string
  phone: string | null
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInMs: number
  user: UserResponse
}

export interface LoginRequest {
  email: string
  password: string
}
