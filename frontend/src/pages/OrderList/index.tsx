import { useEffect, useState, type FC } from 'react'
import { Link } from 'react-router-dom'
import { Loading } from '@/components/Loading'
import { Empty } from '@/components/Empty'
import { getOrderList, type OrderVO } from '@/api/order'

const statusLabels: Record<number, string> = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成',
  4: '已取消',
}

const statusColors: Record<number, string> = {
  0: 'text-orange-600 bg-orange-50 ring-1 ring-orange-600/20',
  1: 'text-blue-600 bg-blue-50 ring-1 ring-blue-600/20',
  2: 'text-purple-600 bg-purple-50 ring-1 ring-purple-600/20',
  3: 'text-green-600 bg-green-50 ring-1 ring-green-600/20',
  4: 'text-gray-400 bg-gray-50 ring-1 ring-gray-400/20',
}

const tabs = [
  { label: '全部', value: undefined },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已发货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
]

const OrderListPage: FC = () => {
  const [orders, setOrders] = useState<OrderVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<number | undefined>(undefined)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(true)
  const pageSize = 10

  const fetchOrders = async (pageNum: number, replace: boolean, status?: number) => {
    setLoading(true)
    setError(null)
    try {
      const res = await getOrderList({ status: status, page: pageNum, pageSize })
      if (res.code === 200 && res.data) {
        const list = res.data.records ?? []
        if (replace) {
          setOrders(list)
        } else {
          setOrders((prev) => [...prev, ...list])
        }
        setHasMore(pageNum < res.data.totalPages)
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
    setPage(1)
    fetchOrders(1, true, activeTab)
  }, [activeTab])

  const handleLoadMore = () => {
    const next = page + 1
    setPage(next)
    fetchOrders(next, false, activeTab)
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">我的订单</h1>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 border-b border-gray-200">
        {tabs.map((tab) => (
          <button
            key={tab.label}
            onClick={() => setActiveTab(tab.value)}
            className={`px-4 py-2 text-sm border-b-2 transition-colors ${
              activeTab === tab.value
                ? 'border-blue-600 text-blue-600 font-medium'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Order list */}
      {loading && orders.length === 0 ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-32 bg-gray-100 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : error && orders.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            onClick={() => fetchOrders(1, true, activeTab)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg"
          >
            重新加载
          </button>
        </div>
      ) : orders.length === 0 ? (
        <Empty
          title="暂无订单"
          description="快去挑选心仪的商品吧"
          action={
            <Link to="/products" className="text-blue-600 hover:underline text-sm">
              去购物
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              className="block bg-white rounded-lg border border-gray-200 p-4 hover:border-gray-300 transition-colors"
            >
              {/* Header */}
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs text-gray-400">
                  订单号: {order.orderNo}
                </span>
                <span className={`inline-flex items-center text-xs font-medium px-2.5 py-1 rounded-full ${statusColors[order.status] ?? ''}`}>
                  {statusLabels[order.status] ?? '未知'}
                </span>
              </div>

              {/* Items preview */}
              <div className="flex items-center gap-3">
                {order.items?.slice(0, 3).map((item) => (
                  <img
                    key={item.id}
                    src={item.productImage}
                    alt={item.productName}
                    className="h-16 w-16 object-cover rounded-lg border border-gray-100"
                  />
                ))}
                {(order.items?.length ?? 0) > 3 && (
                  <span className="text-xs text-gray-400">
                    +{order.items!.length - 3}件
                  </span>
                )}
                <div className="ml-auto text-right">
                  <p className="text-sm font-semibold text-gray-900">
                    ¥{order.totalAmount.toFixed(2)}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {new Date(order.createdAt).toLocaleString('zh-CN')}
                  </p>
                </div>
              </div>
            </Link>
          ))}

          {/* Load more */}
          {hasMore && (
            <div className="text-center pt-4">
              <button
                onClick={handleLoadMore}
                disabled={loading}
                className="px-6 py-2 text-sm text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-50 disabled:text-gray-400 disabled:border-gray-200"
              >
                {loading ? '加载中...' : '加载更多'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default OrderListPage
