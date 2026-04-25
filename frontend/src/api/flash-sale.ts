import client from './client'
import type { ApiResponse } from '@/types/api'

export interface FlashSaleItemVO {
  id: number
  eventId: number
  productId: number
  flashPrice: number
  flashStock: number
  limitPerUser: number
  version: number
  productName: string
  productImage: string
  originalPrice: number
}

export interface FlashSaleEventVO {
  id: number
  name: string
  startTime: string
  endTime: string
  status: number
  remark: string
  createdAt: string
  items: FlashSaleItemVO[]
}

export const getActiveEvents = () =>
  client.get<any, ApiResponse<FlashSaleEventVO[]>>('/flash-sale/events')

export const getEventDetail = (id: number) =>
  client.get<any, ApiResponse<FlashSaleEventVO>>(`/flash-sale/events/${id}`)

export const flashOrder = (eventId: number, itemId: number) =>
  client.post<any, ApiResponse<{ requestId: string }>>('/flash-sale/order', null, {
    params: { eventId, itemId },
  })

export const getOrderStatus = (requestId: string) =>
  client.get<any, ApiResponse<{ status: string; orderNo?: string }>>('/flash-sale/order/status', {
    params: { requestId },
  })
