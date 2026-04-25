import client from '@/api/client'
import type { ApiResponse } from '@/types/api'

export interface UploadToken {
  token: string
  key: string
  domain: string
}

export const getUploadToken = (type: 'product' | 'avatar') =>
  client.get<any, ApiResponse<UploadToken>>('/upload/token', { params: { type } })
