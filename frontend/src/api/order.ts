import client from './client'
import type { ApiResponse, PageResult } from '@/types/api'

export interface OrderItemVO {
  id: number
  productId: number
  productName: string
  productImage: string
  skuId: number | null
  skuName: string | null
  price: number
  quantity: number
  subtotal: number
}

export interface AddressSnapshotVO {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
}

export interface OrderVO {
  id: number
  orderNo: string
  totalAmount: number
  status: number
  paymentMethod: string | null
  paidAt: string | null
  remark: string | null
  cancelReason: string | null
  canceledAt: string | null
  shippedAt: string | null
  completedAt: string | null
  isFlashSale: number
  createdAt: string
  items: OrderItemVO[]
  addressSnapshot: AddressSnapshotVO | null
}

export interface CreateOrderReq {
  cartItemIds: number[]
  addressId: number
  remark?: string
}

export const createOrder = (data: CreateOrderReq) =>
  client.post<any, ApiResponse<OrderVO>>('/orders', data)

export const getOrderList = (params: { status?: number; page?: number; pageSize?: number }) =>
  client.get<any, ApiResponse<PageResult<OrderVO>>>('/orders', { params })

export const getOrderDetail = (id: number) =>
  client.get<any, ApiResponse<OrderVO>>(`/orders/${id}`)

export const payOrder = (id: number) =>
  client.post<any, ApiResponse<null>>(`/orders/${id}/pay`)

export const cancelOrder = (id: number) =>
  client.post<any, ApiResponse<null>>(`/orders/${id}/cancel`)

export const completeOrder = (id: number) =>
  client.post<any, ApiResponse<null>>(`/orders/${id}/complete`)
