import { useEffect, useState, type FC } from 'react'
import { Link } from 'react-router-dom'
import { Loading } from '@/components/Loading'
import { Empty } from '@/components/Empty'
import { useCountdown } from '@/hooks/useCountdown'
import { getActiveEvents, type FlashSaleEventVO } from '@/api/flash-sale'

const eventStatusLabels: Record<number, { label: string; color: string }> = {
  0: { label: '即将开始', color: 'text-blue-600 bg-blue-50 ring-1 ring-blue-600/20' },
  1: { label: '进行中', color: 'text-red-600 bg-red-50 ring-1 ring-red-600/20' },
  2: { label: '已结束', color: 'text-gray-400 bg-gray-50 ring-1 ring-gray-400/20' },
}

function EventCard({ event }: { event: FlashSaleEventVO }) {
  const isUpcoming = event.status === 0
  const isOngoing = event.status === 1
  const showCountdown = isUpcoming || isOngoing
  const targetTime = isUpcoming ? event.startTime : event.endTime
  const { formatted, isEnded } = useCountdown(
    targetTime,
    isUpcoming ? () => {} : undefined,
  )

  const statusInfo = eventStatusLabels[event.status] ?? {
    label: '未知',
    color: 'text-gray-400 bg-gray-50',
  }

  return (
    <Link
      to={`/flash-sale/${event.id}`}
      className="block bg-white rounded-xl border border-gray-200 overflow-hidden hover:shadow-md transition-shadow"
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold text-gray-900 truncate">{event.name}</h3>
          <span className={`inline-flex items-center text-xs font-medium px-2.5 py-1 rounded-full shrink-0 ${statusInfo.color}`}>
            {statusInfo.label}
          </span>
        </div>

        {/* Countdown */}
        {showCountdown && !isEnded && (
          <div className="mb-3 text-center">
            <p className="text-xs text-gray-400 mb-1">
              {isUpcoming ? '距开始' : '距结束'}
            </p>
            <p className="text-xl font-mono font-bold text-red-500">{formatted}</p>
          </div>
        )}

        {/* Product preview */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {event.items?.slice(0, 4).map((item) => (
            <div key={item.id} className="shrink-0">
              <img
                src={item.productImage}
                alt={item.productName}
                className="h-16 w-16 object-cover rounded-lg border border-gray-100"
              />
            </div>
          ))}
        </div>
      </div>
    </Link>
  )
}

const FlashSaleListPage: FC = () => {
  const [events, setEvents] = useState<FlashSaleEventVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchEvents = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getActiveEvents()
      if (res.code === 200) {
        setEvents(res.data ?? [])
      } else {
        setError(res.message ?? '获取活动列表失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchEvents()
  }, [])

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="grid grid-cols-2 gap-4">
          {Array.from({ length: 2 }).map((_, i) => (
            <div key={i} className="h-48 bg-gray-100 rounded-xl animate-pulse" />
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 text-center">
        <p className="text-red-500 mb-4">{error}</p>
        <button onClick={fetchEvents} className="px-4 py-2 bg-blue-600 text-white rounded-lg">
          重新加载
        </button>
      </div>
    )
  }

  const ongoing = events.filter((e) => e.status === 1)
  const upcoming = events.filter((e) => e.status === 0)
  const ended = events.filter((e) => e.status === 2)

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">限时秒杀</h1>

      {events.length === 0 ? (
        <Empty title="暂无秒杀活动" description="敬请期待" />
      ) : (
        <div className="space-y-8">
          {ongoing.length > 0 && (
            <section>
              <h2 className="text-lg font-medium text-red-600 mb-3">正在抢购</h2>
              <div className="grid grid-cols-2 gap-4">
                {ongoing.map((e) => <EventCard key={e.id} event={e} />)}
              </div>
            </section>
          )}

          {upcoming.length > 0 && (
            <section>
              <h2 className="text-lg font-medium text-blue-600 mb-3">即将开始</h2>
              <div className="grid grid-cols-2 gap-4">
                {upcoming.map((e) => <EventCard key={e.id} event={e} />)}
              </div>
            </section>
          )}

          {ended.length > 0 && (
            <section>
              <h2 className="text-lg font-medium text-gray-500 mb-3">已结束</h2>
              <div className="grid grid-cols-2 gap-4">
                {ended.map((e) => <EventCard key={e.id} event={e} />)}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  )
}

export default FlashSaleListPage
