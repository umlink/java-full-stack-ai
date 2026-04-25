import axios from 'axios'
import type { ApiResponse } from '@/types/api'
import { showToast } from '@/utils/toast'

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：附加 token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一错误处理
client.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          // 已在登录页时，401 表示凭证错误，不跳转
          if (window.location.pathname === '/login' || window.location.pathname === '/register') {
            break
          }
          // 清除 token，跳转登录
          localStorage.removeItem('token')
          window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
          break
        case 403:
          showToast('没有权限访问')
          break
        case 429:
          showToast('操作太频繁，请稍后再试')
          break
        case 422:
          // 字段校验错误，返回 data 让表单处理
          return Promise.reject({ status, ...data })
        default:
          showToast(data?.message || '系统繁忙，请稍后重试')
      }
    } else if (error.code === 'ECONNABORTED') {
      showToast('请求超时，请检查网络')
    } else {
      showToast('网络连接失败')
    }
    return Promise.reject(error)
  },
)

export default client
