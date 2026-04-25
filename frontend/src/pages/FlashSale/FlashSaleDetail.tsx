import { useEffect, useState, type FC } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Loading } from '@/components/Loading'
import { useCountdown } from '@/hooks/useCountdown'
import { useFlashSale } from '@/hooks/useFlashSale'
import { useAuthStore } from '@/stores/authStore'
import { getEventDetail, flashOrder, type FlashSaleEventVO } from '@/api/flash-sale'

type ButtonState = 'upcoming' | 'ready' | 'pending' | 'sold_out' | 'ended'

const FlashSaleDetailPage: FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const [event, setEvent] = useState<FlashSaleEventVO | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedItemId, setSelectedItemId] = useState<number | null>(null)
  const [requestId, setRequestId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const fetchDetail = async () => {
    if (!id) return
    setLoading(true)
    setError(null)
    try {
      const res = await getEventDetail(Number(id))
      if (res.code === 200 && res.data) {
        setEvent(res.data)
        if (res.data.items?.length > 0) {
          setSelectedItemId(res.data.items[0].id)
        }
      } else {
        setError(res.message ?? '获取活动详情失败')
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

  // Countdown for event start/end
  const targetTime = event
    ? event.status === 0
      ? event.startTime
      : event.status === 1
        ? event.endTime
        : null
    : null
  const { formatted, isEnded } = useCountdown(targetTime ?? new Date().toISOString(), () => {
    if (event?.status === 0) {
      // Event just started, refresh
      fetchDetail()
    }
  })

  // Flash sale polling
  const flashState = useFlashSale(requestId)

  useEffect(() => {
    if (flashState.status === 'success' && flashState.orderNo) {
      setMessage(`抢购成功！订单号: ${flashState.orderNo}`)
    } else if (flashState.status === 'failed' || flashState.status === 'timeout') {
      setMessage(flashState.error)
    }
  }, [flashState.status])

  const handleFlashOrder = async () => {
    if (!event || !selectedItemId) return

    if (!isAuthenticated) {
      navigate('/login?redirect=' + encodeURIComponent(window.location.pathname))
      return
    }

    const item = event.items?.find((i) => i.id === selectedItemId)
    if (!item || event.status !== 1) return

    setMessage(null)
    try {
      const res = await flashOrder(event.id, selectedItemId)
      if (res.code === 200 && res.data?.requestId) {
        setRequestId(res.data.requestId)
        setMessage('排队中，请稍候...')
      } else {
        setMessage(res.message ?? '抢购失败')
      }
    } catch {
      setMessage('网络错误')
    }
  }

  // Determine button state
  const getButtonState = (): ButtonState => {
    if (!event) return 'ended'
    if (event.status === 0) return 'upcoming'
    if (event.status === 2 || (event.status === 1 && isEnded)) return 'ended'
    if (requestId && flashState.status === 'pending') return 'pending'
    const item = event.items?.find((i) => i.id === selectedItemId)
    if (!item || item.flashStock <= 0) return 'sold_out'
    if (event.status === 1) return 'ready'
    return 'ended'
  }

  const buttonState = getButtonState()
  const selectedItem = event?.items?.find((i) => i.id === selectedItemId)

  const buttonConfig: Record<ButtonState, { text: string; disabled: boolean; className: string }> = {
    upcoming: {
      text: `距开始 ${formatted}`,
      disabled: true,
      className: 'bg-gray-300 text-gray-500 cursor-not-allowed',
    },
    ready: {
      text: '立即秒杀',
      disabled: false,
      className: 'bg-red-500 text-white hover:bg-red-600 cursor-pointer',
    },
    pending: {
      text: '排队中...',
      disabled: true,
      className: 'bg-orange-400 text-white cursor-wait animate-pulse',
    },
    sold_out: {
      text: '已售罄',
      disabled: true,
      className: 'bg-gray-300 text-gray-500 cursor-not-allowed',
    },
    ended: {
      text: '已结束',
      disabled: true,
      className: 'bg-gray-300 text-gray-500 cursor-not-allowed',
    },
  }

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-40 bg-gray-100 rounded-xl animate-pulse" />
          ))}
        </div>
      </div>
    )
  }

  if (error || !event) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 text-center">
        <p className="text-red-500 mb-4">{error ?? '活动不存在'}</p>
        <button onClick={() => navigate('/flash-sale')} className="px-4 py-2 bg-blue-600 text-white rounded-lg">
          返回秒杀列表
        </button>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate('/flash-sale')}
        className="text-sm text-gray-400 hover:text-gray-600 mb-4"
      >
        &larr; 返回秒杀列表
      </button>

      {/* Event info header */}
      <div className="bg-gradient-to-r from-red-500 to-orange-400 rounded-xl p-6 text-white mb-6">
        <h1 className="text-xl font-bold mb-2">{event.name}</h1>
        {event.status === 1 && !isEnded && (
          <div>
            <p className="text-sm opacity-80">距结束</p>
            <p className="text-3xl font-mono font-bold">{formatted}</p>
          </div>
        )}
        {event.status === 0 && (
          <div>
            <p className="text-sm opacity-80">距开始</p>
            <p className="text-3xl font-mono font-bold">{formatted}</p>
          </div>
        )}
        {(event.status === 2 || isEnded) && (
          <p className="text-lg font-medium opacity-80">已结束</p>
        )}
      </div>

      {/* Product grid */}
      <div className="space-y-4">
        {event.items?.map((item) => (
          <div
            key={item.id}
            className={`bg-white rounded-xl border p-4 flex items-center gap-4 ${
              selectedItemId === item.id ? 'border-red-300 ring-2 ring-red-100' : 'border-gray-200'
            }`}
            onClick={() => setSelectedItemId(item.id)}
          >
            <img
              src={item.productImage}
              alt={item.productName}
              className="h-24 w-24 object-cover rounded-lg border border-gray-100 shrink-0"
            />
            <div className="flex-1 min-w-0">
              <p className="font-medium text-gray-900">{item.productName}</p>
              <div className="flex items-center gap-3 mt-2">
                <span className="text-2xl font-bold text-red-500">
                  ¥{item.flashPrice.toFixed(2)}
                </span>
                <span className="text-sm text-gray-400 line-through">
                  ¥{item.originalPrice.toFixed(2)}
                </span>
              </div>
              <div className="flex items-center gap-3 mt-2 text-xs text-gray-500">
                <span>限购 {item.limitPerUser} 件</span>
                <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden max-w-[120px]">
                  <div
                    className="h-full bg-red-400 rounded-full transition-all"
                    style={{ width: `${item.flashStock > 0 ? 100 : 0}%` }}
                  />
                </div>
                <span>已售 {(item.flashStock > 0 ? 0 : 1)}%</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Flash button */}
      <div className="sticky bottom-0 mt-6 bg-white/90 backdrop-blur-md rounded-xl border border-gray-200 p-4 shadow-lg">
        {message && (
          <p className="text-sm text-center mb-3 text-gray-600">{message}</p>
        )}
        <button
          onClick={handleFlashOrder}
          disabled={buttonConfig[buttonState].disabled}
          className={`w-full py-3 rounded-xl text-lg font-bold transition-all ${buttonConfig[buttonState].className}`}
        >
          {buttonConfig[buttonState].text}
        </button>

        {flashState.status === 'success' && flashState.orderId && (
          <div className="mt-3 text-center">
            <button
              onClick={() => navigate(`/orders/${flashState.orderId}`)}
              className="text-sm text-blue-600 hover:underline"
            >
              查看订单 &rarr;
            </button>
          </div>
        )}

        {flashState.status === 'failed' && (
          <div className="mt-3 text-center">
            <button
              onClick={() => {
                setRequestId(null)
                setMessage(null)
              }}
              className="text-sm text-blue-600 hover:underline"
            >
              重新尝试
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default FlashSaleDetailPage
