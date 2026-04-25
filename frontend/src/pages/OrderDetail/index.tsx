import { useEffect, useState, type FC } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Loading } from '@/components/Loading'
import {
  getOrderDetail,
  payOrder,
  cancelOrder,
  completeOrder,
  type OrderVO,
} from '@/api/order'

const statusLabels: Record<number, { label: string; color: string }> = {
  0: { label: '待支付', color: 'text-orange-600 bg-orange-50 ring-1 ring-orange-600/20' },
  1: { label: '已支付', color: 'text-blue-600 bg-blue-50 ring-1 ring-blue-600/20' },
  2: { label: '已发货', color: 'text-purple-600 bg-purple-50 ring-1 ring-purple-600/20' },
  3: { label: '已完成', color: 'text-green-600 bg-green-50 ring-1 ring-green-600/20' },
  4: { label: '已取消', color: 'text-gray-400 bg-gray-50 ring-1 ring-gray-400/20' },
}

const statusSteps = [
  { key: 0, label: '提交订单' },
  { key: 1, label: '已支付' },
  { key: 2, label: '已发货' },
  { key: 3, label: '已完成' },
]

const OrderDetailPage: FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [order, setOrder] = useState<OrderVO | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [countdown, setCountdown] = useState<number | null>(null)

  const fetchDetail = async () => {
    if (!id) return
    setLoading(true)
    setError(null)
    try {
      const res = await getOrderDetail(Number(id))
      if (res.code === 200 && res.data) {
        setOrder(res.data)
        // 30 min countdown for unpaid orders
        if (res.data.status === 0) {
          const created = new Date(res.data.createdAt).getTime()
          const deadline = created + 30 * 60 * 1000
          const remaining = Math.max(0, Math.floor((deadline - Date.now()) / 1000))
          setCountdown(remaining)
        }
      } else {
        setError(res.message ?? '获取订单失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDetail()
  }, [id])

  // Countdown timer
  useEffect(() => {
    if (countdown === null || countdown <= 0) return
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev === null || prev <= 1) {
          clearInterval(timer)
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [countdown !== null])

  const handleAction = async (action: string, fn: () => Promise<any>) => {
    setActionLoading(action)
    try {
      const res = await fn()
      if (res.code === 200) {
        await fetchDetail()
      } else {
        setError(res.message ?? '操作失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setActionLoading(null)
    }
  }

  const formatCountdown = (seconds: number) => {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-20 bg-gray-100 rounded-lg animate-pulse" />
          ))}
        </div>
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 text-center">
        <p className="text-red-500 mb-4">{error ?? '订单不存在'}</p>
        <button
          onClick={() => navigate('/orders')}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg"
        >
          返回订单列表
        </button>
      </div>
    )
  }

  const currentStepIndex = order.status === 4 ? -1 : order.status

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Back button */}
      <button
        onClick={() => navigate('/orders')}
        className="text-sm text-gray-400 hover:text-gray-600 mb-4"
      >
        &larr; 返回订单列表
      </button>

      {/* Status header */}
      <div className="bg-white rounded-lg border border-gray-200 p-6 mb-4">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-xl font-semibold text-gray-900">
              {statusLabels[order.status]?.label ?? '未知状态'}
            </h1>
            {order.status === 0 && countdown !== null && countdown > 0 && (
              <p className="text-sm text-orange-500 mt-1">
                还剩 {formatCountdown(countdown)} 自动取消
              </p>
            )}
          </div>
          <p className="text-2xl font-bold text-red-500">
            ¥{order.totalAmount.toFixed(2)}
          </p>
        </div>

        {/* Status timeline */}
        {order.status !== 4 && (
          <div className="flex items-center gap-0">
            {statusSteps.map((step, i) => (
              <div key={step.key} className="flex-1 flex items-center">
                <div className="flex items-center gap-2">
                  <div
                    className={`h-6 w-6 rounded-full flex items-center justify-center text-xs font-medium ${
                      currentStepIndex >= step.key
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-200 text-gray-400'
                    }`}
                  >
                    {currentStepIndex > step.key ? '✓' : step.key + 1}
                  </div>
                  <span
                    className={`text-xs ${
                      currentStepIndex >= step.key ? 'text-blue-600' : 'text-gray-400'
                    }`}
                  >
                    {step.label}
                  </span>
                </div>
                {i < statusSteps.length - 1 && (
                  <div
                    className={`flex-1 h-px mx-2 ${
                      currentStepIndex > step.key ? 'bg-blue-600' : 'bg-gray-200'
                    }`}
                  />
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Address */}
      {order.addressSnapshot && (
        <div className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
          <h2 className="text-sm font-medium text-gray-900 mb-2">收货信息</h2>
          <p className="text-sm text-gray-600">
            {order.addressSnapshot.receiverName} {order.addressSnapshot.receiverPhone}
          </p>
          <p className="text-sm text-gray-500">
            {order.addressSnapshot.province} {order.addressSnapshot.city}{' '}
            {order.addressSnapshot.district} {order.addressSnapshot.detail}
          </p>
        </div>
      )}

      {/* Items */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
        <h2 className="text-sm font-medium text-gray-900 mb-3">商品信息</h2>
        <div className="divide-y divide-gray-100">
          {order.items?.map((item) => (
            <div key={item.id} className="flex items-center gap-4 py-3">
              <img
                src={item.productImage}
                alt={item.productName}
                className="h-16 w-16 object-cover rounded-lg border border-gray-100 shrink-0"
              />
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-900 truncate">{item.productName}</p>
                {item.skuName && (
                  <p className="text-xs text-gray-400 mt-0.5">{item.skuName}</p>
                )}
              </div>
              <div className="text-sm text-gray-500">¥{item.price.toFixed(2)}</div>
              <div className="text-sm text-gray-500">×{item.quantity}</div>
              <div className="text-sm font-semibold text-gray-900 w-20 text-right">
                ¥{item.subtotal.toFixed(2)}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Order info */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
        <h2 className="text-sm font-medium text-gray-900 mb-2">订单信息</h2>
        <div className="space-y-1 text-xs text-gray-500">
          <p>订单编号: {order.orderNo}</p>
          <p>创建时间: {new Date(order.createdAt).toLocaleString('zh-CN')}</p>
          {order.paidAt && <p>支付时间: {new Date(order.paidAt).toLocaleString('zh-CN')}</p>}
          {order.shippedAt && <p>发货时间: {new Date(order.shippedAt).toLocaleString('zh-CN')}</p>}
          {order.completedAt && <p>完成时间: {new Date(order.completedAt).toLocaleString('zh-CN')}</p>}
          {order.canceledAt && <p>取消时间: {new Date(order.canceledAt).toLocaleString('zh-CN')}</p>}
          {order.cancelReason && <p>取消原因: {order.cancelReason}</p>}
        </div>
      </div>

      {/* Error toast */}
      {error && (
        <div className="bg-red-50 text-red-600 text-sm rounded-lg px-4 py-2 mb-4">{error}</div>
      )}

      {/* Action buttons */}
      <div className="flex justify-end gap-3">
        {order.status === 0 && (
          <>
            <button
              onClick={() => handleAction('pay', () => payOrder(order.id))}
              disabled={actionLoading === 'pay'}
              className="px-6 py-2 bg-red-500 text-white rounded-lg text-sm hover:bg-red-600 disabled:bg-red-300"
            >
              {actionLoading === 'pay' ? '处理中...' : '立即支付'}
            </button>
            <button
              onClick={() => handleAction('cancel', () => cancelOrder(order.id))}
              disabled={actionLoading === 'cancel'}
              className="px-4 py-2 text-sm text-gray-500 border border-gray-200 rounded-lg hover:text-gray-700 disabled:text-gray-300"
            >
              {actionLoading === 'cancel' ? '处理中...' : '取消订单'}
            </button>
          </>
        )}
        {order.status === 2 && (
          <button
            onClick={() => handleAction('complete', () => completeOrder(order.id))}
            disabled={actionLoading === 'complete'}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 disabled:bg-blue-300"
          >
            {actionLoading === 'complete' ? '处理中...' : '确认收货'}
          </button>
        )}
      </div>
    </div>
  )
}

export default OrderDetailPage
