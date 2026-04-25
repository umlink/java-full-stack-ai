// 通用 API 响应结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应结构
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// 字段级校验错误
export interface FieldError {
  field: string
  message: string
}

// 分页请求参数
export interface PageParams {
  page?: number
  pageSize?: number
}
