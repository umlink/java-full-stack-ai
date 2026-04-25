import { useAuthStore } from '@/stores/authStore'
import { Role } from '@/types/auth'

export function useAuth() {
  const store = useAuthStore()

  const hasRole = (minRole: Role) => {
    return store.user ? store.user.role >= minRole : false
  }

  const isAdmin = () => hasRole(Role.ADMIN)
  const isOperator = () => hasRole(Role.OPERATOR)

  return { ...store, hasRole, isAdmin, isOperator }
}
