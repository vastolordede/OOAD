import { createContext } from 'react'
import type { LoginRequest, UserResponse, UserRole } from '../types/auth'

export interface AuthContextValue {
  accessToken: string | null
  user: UserResponse | null
  login: (payload: LoginRequest) => Promise<UserRole>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
