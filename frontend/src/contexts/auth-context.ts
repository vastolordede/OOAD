import { createContext } from 'react'
import type {
  LoginRequest,
  RegisterRequest,
  UserResponse,
  UserRole,
} from '../types/auth'

export interface AuthContextValue {
  accessToken: string | null
  user: UserResponse | null
  login: (payload: LoginRequest) => Promise<UserRole>
  register: (payload: RegisterRequest) => Promise<UserRole>
  logout: () => void
  updateUserState: (updatedUser: UserResponse) => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
