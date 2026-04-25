/**
 * Toast 回调机制
 *
 * 实际项目中替换为 Toast 组件（如 shadcn/ui 的 useToast / sonner 等）。
 * 在 App 初始化时注册 toast 函数：
 *
 *   import { toast } from 'sonner'
 *   import { setToastFn } from '@/utils/toast'
 *   setToastFn((msg) => toast(msg))
 *
 * 之后在拦截器或业务代码中调用 showToast / showError 即可。
 */

type ToastFn = (message: string) => void

let toastFn: ToastFn | null = null

export function setToastFn(fn: ToastFn) {
  toastFn = fn
}

export function showToast(message: string) {
  if (toastFn) {
    toastFn(message)
  } else {
    console.log('[Toast]', message)
  }
}

export function showError(message: string) {
  showToast(message)
}
