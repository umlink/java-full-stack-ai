// 商品分类视图对象
export interface CategoryVO {
  id: number
  name: string
  parentId: number
  level: number
  sortOrder: number
  status: number
  children?: CategoryVO[]
  createdAt?: string
}

// 商品视图对象
export interface ProductVO {
  id: number
  name: string
  brief?: string
  description?: string
  categoryId: number
  brandId?: number
  unit: string
  weight?: number
  hasSku: boolean
  price: number
  minPrice?: number
  maxPrice?: number
  totalStock: number
  sales: number
  mainImage: string
  images?: string[]
  videoUrl?: string
  specs?: SpecTemplate[]
  attrs?: AttributeParam[]
  tags?: string[]
  keywords?: string
  sortOrder: number
  status: number
  createdAt: string
  updatedAt: string
}

// 规格模板
export interface SpecTemplate {
  name: string
  values: string[]
}

// 属性参数
export interface AttributeParam {
  name: string
  value: string
}

// 商品 SKU 视图对象
export interface SkuVO {
  id: number
  productId: number
  name: string
  attrs: Record<string, string>
  price: number
  stock: number
  code?: string
  image?: string
  weight?: number
  status: number
  sortOrder: number
}

// 商品查询参数
export interface ProductQueryParams {
  page?: number
  pageSize?: number
  categoryId?: number
  brandId?: number
  keyword?: string
  sort?: 'price_asc' | 'price_desc' | 'sales_desc' | 'newest'
  minPrice?: number
  maxPrice?: number
}
