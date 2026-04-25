import { useEffect, useState, type FC } from 'react'
import { Loading } from '@/components/Loading'
import { getOrderList, type OrderVO } from '@/api/order'

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待支付', color: 'text-orange-500 bg-orange-50' },
  1: { label: '已支付', color: 'text-blue-600 bg-blue-50' },
  2: { label: '已发货', color: 'text-purple-600 bg-purple-50' },
  3: { label: '已完成', color: 'text-green-600 bg-green-50' },
  4: { label: '已取消', color: 'text-gray-400 bg-gray-50' },
}

const AdminOrdersPage: FC = () => {
  const [orders, setOrders] = useState<OrderVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchOrders()
  }, [])

  const fetchOrders = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getOrderList({ page: 1, pageSize: 50 })
      if (res.code === 200 && res.data) {
        setOrders(res.data.records ?? [])
      } else {
        setError(res.message ?? '获取订单失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  if (loading) return <Loading text="加载订单列表..." />
  if (error) return (
    <div className="text-center py-12">
      <p className="text-red-500 mb-4">{error}</p>
      <button onClick={fetchOrders} className="px-4 py-2 bg-blue-600 text-white rounded-lg">重新加载</button>
    </div>
  )

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">订单管理</h2>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">订单号</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">金额</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">时间</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {orders.map((o) => {
              const st = statusMap[o.status] ?? { label: '未知', color: 'text-gray-400 bg-gray-50' }
              return (
                <tr key={o.id} className="hover:bg-gray-50 transition-colors duration-150">
                  <td className="px-4 py-3.5 text-gray-600 font-mono text-xs">{o.orderNo}</td>
                  <td className="px-4 py-3.5 font-medium">¥{o.totalAmount.toFixed(2)}</td>
                  <td className="px-4 py-3.5">
                    <span className={`inline-flex items-center text-xs font-medium px-2.5 py-1 rounded-full ${st.color}`}>{st.label}</span>
                  </td>
                  <td className="px-4 py-3.5 text-gray-400 text-xs">{new Date(o.createdAt).toLocaleString('zh-CN')}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {orders.length === 0 && <p className="text-center py-8 text-gray-400">暂无订单</p>}
      </div>
    </div>
  )
}

export default AdminOrdersPage
