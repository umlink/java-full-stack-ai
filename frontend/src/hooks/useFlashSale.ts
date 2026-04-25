import { useState, useEffect, useRef } from 'react'
import { getOrderStatus } from '@/api/flash-sale'

interface FlashSaleState {
  status: 'idle' | 'pending' | 'success' | 'failed' | 'timeout'
  orderNo: string | null
  orderId: number | null
  error: string | null
}

export function useFlashSale(requestId: string | null) {
  const [state, setState] = useState<FlashSaleState>({
    status: 'idle',
    orderNo: null,
    orderId: null,
    error: null,
  })
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const retryCount = useRef(0)

  useEffect(() => {
    if (!requestId) {
      setState({ status: 'idle', orderNo: null, orderId: null, error: null })
      return
    }

    setState({ status: 'pending', orderNo: null, orderId: null, error: null })
    retryCount.current = 0

    pollingRef.current = setInterval(async () => {
      try {
        const res = await getOrderStatus(requestId)
        if (res.code === 200 && res.data) {
          if (res.data.status === 'success') {
            setState({
              status: 'success',
              orderNo: res.data.orderNo ?? null,
              orderId: res.data.orderId ?? null,
              error: null,
            })
            clearInterval(pollingRef.current!)
            return
          }
          if (res.data.status === 'failed') {
            setState({ status: 'failed', orderNo: null, orderId: null, error: '抢购失败' })
            clearInterval(pollingRef.current!)
            return
          }
        }
      } catch {
        // continue polling
      }

      retryCount.current++
      if (retryCount.current >= 10) {
        setState({ status: 'timeout', orderNo: null, orderId: null, error: '请求超时' })
        clearInterval(pollingRef.current!)
      }
    }, 3000)

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current)
      }
    }
  }, [requestId])

  return state
}
