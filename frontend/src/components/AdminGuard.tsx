import { type FC, type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

interface AdminGuardProps {
  children: ReactNode
  /** 最低角色等级，默认为 2（运营） */
  minRole?: number
}

export const AdminGuard: FC<AdminGuardProps> = ({ children, minRole = 2 }) => {
  const { user, isAuthenticated } = useAuthStore()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!user || user.role < minRole) {
    return <Navigate to="/403" replace />
  }

  return <>{children}</>
}
