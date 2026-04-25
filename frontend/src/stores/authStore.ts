import { create } from 'zustand'
import type { UserInfo } from '@/types/auth'
import { authApi } from '@/api/auth'

interface AuthState {
  user: UserInfo | null
  token: string | null
  isAuthenticated: boolean
  isInitialized: boolean
  login: (token: string, user: UserInfo) => void
  logout: () => void
  fetchProfile: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: localStorage.getItem('token'),
  isAuthenticated: false,
  isInitialized: false,
  login: (token, user) => {
    localStorage.setItem('token', token)
    set({ token, user, isAuthenticated: true })
  },
  logout: () => {
    localStorage.removeItem('token')
    set({ token: null, user: null, isAuthenticated: false })
  },
  fetchProfile: async () => {
    try {
      const res = await authApi.getProfile()
      set({ user: res.data, isAuthenticated: true, isInitialized: true })
    } catch {
      get().logout()
      set({ isInitialized: true })
    }
  },
}))
