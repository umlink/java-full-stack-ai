import client from './client'
import type { ApiResponse } from '@/types/api'

export interface CartItemVO {
  id: number
  productId: number
  productName: string
  productImage: string
  skuId: number | null
  skuName: string | null
  price: number
  quantity: number
  stock: number  // 当前 SKU 库存
  selected: boolean
  isOffline?: boolean  // 是否已下架
}

export interface AddCartRequest {
  productId: number
  skuId?: number
  quantity: number
}

export const addToCart = (data: AddCartRequest) =>
  client.post<any, ApiResponse<null>>('/cart/add', data)

export const getCartList = () =>
  client.get<any, ApiResponse<CartItemVO[]>>('/cart')

export const updateCartItem = (id: number, quantity: number) =>
  client.put<any, ApiResponse<null>>(`/cart/${id}`, { quantity })

export const deleteCartItem = (id: number) =>
  client.delete<any, ApiResponse<null>>(`/cart/${id}`)

export const toggleCartItem = (id: number, selected: boolean) =>
  client.put<any, ApiResponse<null>>(`/cart/${id}/selected`, { selected })
