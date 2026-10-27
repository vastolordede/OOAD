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

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  phone?: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ForgotPasswordResponse {
  message: string
  resetToken?: string | null
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface UpdateProfileRequest {
  fullName: string
  phone?: string | null
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}
