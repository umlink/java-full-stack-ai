import client from './client'
import type { ApiResponse } from '@/types/api'
import type { LoginRequest, RegisterRequest, LoginResponse, UserInfo } from '@/types/auth'

// Individual API functions
export const loginApi = (data: LoginRequest) =>
  client.post<any, ApiResponse<LoginResponse>>('/auth/login', data)

export const registerApi = (data: RegisterRequest) =>
  client.post<any, ApiResponse<null>>('/auth/register', data)

export const getProfileApi = () =>
  client.get<any, ApiResponse<UserInfo>>('/user/profile')

export const updateProfileApi = (data: Partial<UserInfo>) =>
  client.put<any, ApiResponse<UserInfo>>('/user/profile', data)

export const changePasswordApi = (data: { oldPassword: string; newPassword: string }) =>
  client.put<any, ApiResponse<null>>('/user/password', data)

// Namespaced API object (kept for backward compatibility with authStore)
export const authApi = {
  login: loginApi,
  register: registerApi,
  getProfile: getProfileApi,
  updateProfile: updateProfileApi,
  changePassword: changePasswordApi,
}
