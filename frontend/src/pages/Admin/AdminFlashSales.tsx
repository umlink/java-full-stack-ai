import { useEffect, useState, type FC } from 'react'
import { Loading } from '@/components/Loading'
import { getAdminFlashSales } from '@/api/admin'

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待开始', color: 'text-blue-600 bg-blue-50' },
  1: { label: '进行中', color: 'text-red-600 bg-red-50' },
  2: { label: '已结束', color: 'text-gray-400 bg-gray-50' },
}

const AdminFlashSalesPage: FC = () => {
  const [events, setEvents] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchEvents = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getAdminFlashSales({ page: 1, pageSize: 20 })
      if (res.code === 200 && res.data) {
        setEvents(res.data.records ?? [])
      } else {
        setError(res.message ?? '获取活动失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchEvents() }, [])

  if (loading) return <Loading text="加载秒杀活动..." />
  if (error) return (
    <div className="text-center py-12">
      <p className="text-red-500 mb-4">{error}</p>
      <button onClick={fetchEvents} className="px-4 py-2 bg-blue-600 text-white rounded-lg">重新加载</button>
    </div>
  )

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">秒杀管理</h2>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">活动名称</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">开始时间</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">结束时间</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {events.map((e: any) => {
              const st = statusMap[e.status] ?? { label: '未知', color: 'text-gray-400' }
              return (
                <tr key={e.id} className="hover:bg-gray-50 transition-colors duration-150">
                  <td className="px-4 py-3.5 text-gray-400 text-xs">{e.id}</td>
                  <td className="px-4 py-3.5 text-gray-900">{e.name}</td>
                  <td className="px-4 py-3.5 text-gray-500 text-xs">{new Date(e.startTime).toLocaleString('zh-CN')}</td>
                  <td className="px-4 py-3.5 text-gray-500 text-xs">{new Date(e.endTime).toLocaleString('zh-CN')}</td>
                  <td className="px-4 py-3.5">
                    <span className={`inline-flex items-center text-xs font-medium px-2.5 py-1 rounded-full ${st.color}`}>{st.label}</span>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {events.length === 0 && <p className="text-center py-8 text-gray-400">暂无活动</p>}
      </div>
    </div>
  )
}

export default AdminFlashSalesPage
