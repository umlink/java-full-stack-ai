export interface UserInfo {
  id: number
  username: string
  phone?: string
  email?: string
  avatar?: string
  role: number
  status: number
  createdAt: string
}

export enum Role {
  USER = 1,
  OPERATOR = 2,
  ADMIN = 3,
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}
