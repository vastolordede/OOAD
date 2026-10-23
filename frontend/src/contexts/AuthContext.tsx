import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { loginApi } from '../api/auth.api'
import { AuthContext } from './auth-context'
import type { LoginRequest, UserResponse, UserRole } from '../types/auth'

function isUserResponse(value: unknown): value is UserResponse {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const user = value as Record<string, unknown>
  return (
    typeof user.id === 'number' &&
    typeof user.email === 'string' &&
    typeof user.fullName === 'string' &&
    (user.role === 'CUSTOMER' || user.role === 'ADMIN') &&
    (user.status === 'ACTIVE' || user.status === 'DISABLED') &&
    typeof user.createdAt === 'string' &&
    typeof user.updatedAt === 'string' &&
    (typeof user.phone === 'string' || user.phone === null)
  )
}

function loadStoredUser(): UserResponse | null {
  const storedUser = localStorage.getItem('authUser')
  if (!storedUser) {
    return null
  }

  try {
    const parsed: unknown = JSON.parse(storedUser)
    if (isUserResponse(parsed)) {
      return parsed
    }
  } catch (error) {
    if (!(error instanceof SyntaxError)) {
      throw error
    }
  }

  localStorage.removeItem('authUser')
  localStorage.removeItem('accessToken')
  return null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(loadStoredUser)
  const [accessToken, setAccessToken] = useState<string | null>(() =>
    localStorage.getItem('accessToken'),
  )

  const login = useCallback(async (payload: LoginRequest): Promise<UserRole> => {
    const response = await loginApi(payload)
    localStorage.setItem('accessToken', response.accessToken)
    localStorage.setItem('authUser', JSON.stringify(response.user))
    setAccessToken(response.accessToken)
    setUser(response.user)
    return response.user.role
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('authUser')
    setAccessToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ accessToken, user, login, logout }),
    [accessToken, user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
