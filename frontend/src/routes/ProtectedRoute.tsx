import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import type { UserRole } from '../types/auth'

export function ProtectedRoute({ requiredRole }: { requiredRole?: UserRole }) {
  const { accessToken, user } = useAuth()

  if (!accessToken || !user) {
    return <Navigate to="/login" replace />
  }

  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/access-denied" replace />
  }

  return <Outlet />
}
