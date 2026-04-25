import client from './client'
import type { ApiResponse, PageResult } from '@/types/api'

export interface ProductQuery {
  categoryId?: number
  brandId?: number
  minPrice?: number
  maxPrice?: number
  keyword?: string
  sort?: string
  page?: number
  pageSize?: number
}

export interface ProductVO {
  id: number
  name: string
  brief: string
  categoryId: number
  brandId: number
  brandName?: string
  categoryName?: string
  hasSku: boolean
  price: number
  minPrice?: number
  maxPrice?: number
  totalStock: number
  sales: number
  mainImage: string
  tags: string[]
  status: number
}

export interface ProductDetailVO extends ProductVO {
  description: string
  images: string[]
  videoUrl?: string
  specs: SpecDef[]
  attrs: AttrDef[]
  skus: SkuVO[]
}

export interface SpecDef {
  name: string
  values: string[]
}

export interface AttrDef {
  name: string
  value: string
}

export interface SkuVO {
  id: number
  name: string
  attrs: string
  price: number
  stock: number
  image: string
  code: string
}

export interface CategoryVO {
  id: number
  name: string
  children: CategoryVO[]
}

export interface BrandVO {
  id: number
  name: string
  logo?: string
}

export const getProducts = (params: ProductQuery) =>
  client.get<any, ApiResponse<PageResult<ProductVO>>>('/products', { params })

export const getProductDetail = (id: number) =>
  client.get<any, ApiResponse<ProductDetailVO>>(`/products/${id}`)

export const getCategories = () =>
  client.get<any, ApiResponse<CategoryVO[]>>('/categories')

export const getBrands = () =>
  client.get<any, ApiResponse<BrandVO[]>>('/brands')
