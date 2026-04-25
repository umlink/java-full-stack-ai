import { useEffect, useState, type FC } from 'react'
import { Loading } from '@/components/Loading'
import { getOverview, getProductRanking, getOrderTrend } from '@/api/admin'

const DashboardPage: FC = () => {
  const [overview, setOverview] = useState<any>(null)
  const [ranking, setRanking] = useState<any[]>([])
  const [trend, setTrend] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async () => {
    setLoading(true)
    setError(null)
    try {
      const [ovRes, rankRes, trendRes] = await Promise.all([
        getOverview(),
        getProductRanking(),
        getOrderTrend(7),
      ])
      if (ovRes.code === 200) setOverview(ovRes.data)
      if (rankRes.code === 200) setRanking(rankRes.data ?? [])
      if (trendRes.code === 200) setTrend(trendRes.data ?? [])
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  if (loading) return <Loading text="加载数据看板..." />
  if (error) return (
    <div className="text-center py-12">
      <p className="text-red-500 mb-4">{error}</p>
      <button onClick={fetchData} className="px-4 py-2 bg-blue-600 text-white rounded-lg">重新加载</button>
    </div>
  )

  const cards = [
    { label: '总订单数', value: overview?.totalOrders ?? 0, color: 'text-blue-600' },
    { label: '总销售额', value: `¥${(overview?.totalSales ?? 0).toFixed(2)}`, color: 'text-green-600' },
    { label: '今日订单', value: overview?.todayOrders ?? 0, color: 'text-orange-600' },
    { label: '今日销售额', value: `¥${(overview?.todaySales ?? 0).toFixed(2)}`, color: 'text-red-600' },
  ]

  return (
    <div>
      <h2 className="text-xl font-semibold text-gray-900 mb-6">数据看板</h2>

      {/* Stat cards */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {cards.map((card) => (
          <div key={card.label} className="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md hover:border-gray-300 transition-all duration-200">
            <p className="text-sm text-gray-500 mb-1">{card.label}</p>
            <p className={`text-2xl font-bold ${card.color}`}>{card.value}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-6">
        {/* Order trend */}
        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <h3 className="text-sm font-medium text-gray-900 mb-4">近 7 天订单趋势</h3>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-400">
                <th className="pb-2">日期</th>
                <th className="pb-2">订单数</th>
                <th className="pb-2">金额</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {trend.map((t) => (
                <tr key={t.date}>
                  <td className="py-2 text-gray-600">{t.date}</td>
                  <td className="py-2">{t.orderCount}</td>
                  <td className="py-2">¥{t.amount.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Product ranking */}
        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <h3 className="text-sm font-medium text-gray-900 mb-4">商品销量 Top 10</h3>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-400">
                <th className="pb-2">#</th>
                <th className="pb-2">商品</th>
                <th className="pb-2">销量</th>
                <th className="pb-2">价格</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {ranking.map((item) => (
                <tr key={item.productId}>
                  <td className="py-2 text-gray-400">{item.rank}</td>
                  <td className="py-2 text-gray-900 truncate max-w-[200px]">{item.productName}</td>
                  <td className="py-2">{item.sales}</td>
                  <td className="py-2">¥{item.price.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
