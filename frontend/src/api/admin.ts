import client from './client'
import type { ApiResponse, PageResult } from '@/types/api'
import type { UserInfo } from '@/types/auth'

// Statistics
export const getOverview = () =>
  client.get<any, ApiResponse<{ totalOrders: number; totalSales: number; todayOrders: number; todaySales: number }>>('/admin/statistics/overview')

export const getProductRanking = (limit = 10) =>
  client.get<any, ApiResponse<Array<{ rank: number; productId: number; productName: string; sales: number; price: number }>>>('/admin/statistics/product-ranking', { params: { limit } })

export const getOrderTrend = (days = 7) =>
  client.get<any, ApiResponse<Array<{ date: string; orderCount: number; amount: number }>>>('/admin/statistics/order-trend', { params: { days } })

// Users (admin)
export const getUserList = (params: { keyword?: string; role?: number; page?: number; pageSize?: number }) =>
  client.get<any, ApiResponse<PageResult<UserInfo>>>('/admin/users', { params })

export const updateUserRole = (id: number, role: number) =>
  client.put<any, ApiResponse<null>>(`/admin/users/${id}/role`, { role })

export const toggleUserStatus = (id: number, enabled: boolean) =>
  client.put<any, ApiResponse<null>>(`/admin/users/${id}/status`, { enabled })

// Operation logs
export const getOperationLogs = (params: { module?: string; action?: string; operatorName?: string; page?: number; pageSize?: number }) =>
  client.get<any, ApiResponse<PageResult<any>>>('/admin/operation-logs', { params })

// Flash sales (admin)
export const getAdminFlashSales = (params: { status?: number; page?: number; pageSize?: number }) =>
  client.get<any, ApiResponse<PageResult<any>>>('/admin/flash-sales', { params })
