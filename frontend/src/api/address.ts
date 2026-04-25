import client from './client'
import type { ApiResponse } from '@/types/api'

export interface AddressVO {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

export interface AddressForm {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
}

export const getAddressList = () =>
  client.get<any, ApiResponse<AddressVO[]>>('/addresses')

export const createAddress = (data: AddressForm) =>
  client.post<any, ApiResponse<AddressVO>>('/addresses', data)

export const updateAddress = (id: number, data: AddressForm) =>
  client.put<any, ApiResponse<AddressVO>>(`/addresses/${id}`, data)

export const setDefaultAddress = (id: number) =>
  client.put<any, ApiResponse<null>>(`/addresses/${id}/default`)

export const deleteAddress = (id: number) =>
  client.delete<any, ApiResponse<null>>(`/addresses/${id}`)
