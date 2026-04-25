import { useEffect, useState, type FC } from 'react'
import { Loading } from '@/components/Loading'
import { getOperationLogs } from '@/api/admin'

interface OpLog {
  id: number
  operatorName: string
  module: string
  action: string
  description: string
  targetId: string
  requestIp: string
  result: number
  durationMs: number
  createdAt: string
}

const AdminOperationLogsPage: FC = () => {
  const [logs, setLogs] = useState<OpLog[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchLogs = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getOperationLogs({ page: 1, pageSize: 50 })
      if (res.code === 200 && res.data) {
        setLogs(res.data.records ?? [])
      } else {
        setError(res.message ?? '获取日志失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchLogs() }, [])

  if (loading) return <Loading text="加载操作日志..." />
  if (error) return (
    <div className="text-center py-12">
      <p className="text-red-500 mb-4">{error}</p>
      <button onClick={fetchLogs} className="px-4 py-2 bg-blue-600 text-white rounded-lg">重新加载</button>
    </div>
  )

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">操作日志</h2>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">时间</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">操作人</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">模块</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">描述</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">结果</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">耗时</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {logs.map((log) => (
              <tr key={log.id} className="hover:bg-gray-50 transition-colors duration-150">
                <td className="px-4 py-3.5 text-gray-400 text-xs">{new Date(log.createdAt).toLocaleString('zh-CN')}</td>
                <td className="px-4 py-3.5 text-gray-900">{log.operatorName}</td>
                <td className="px-4 py-3.5 text-gray-600">{log.module}</td>
                <td className="px-4 py-3.5 text-gray-600">{log.action}</td>
                <td className="px-4 py-3.5 text-gray-500 max-w-[200px] truncate">{log.description || '-'}</td>
                <td className="px-4 py-3.5">
                  <span className={`inline-flex items-center text-xs font-medium px-2.5 py-1 rounded-full ${log.result === 1 ? 'bg-green-50 text-green-700 ring-1 ring-green-600/20' : 'bg-red-50 text-red-700 ring-1 ring-red-600/20'}`}>
                    {log.result === 1 ? '成功' : '失败'}
                  </span>
                </td>
                <td className="px-4 py-3.5 text-gray-400 text-xs">{log.durationMs}ms</td>
              </tr>
            ))}
          </tbody>
        </table>
        {logs.length === 0 && <p className="text-center py-8 text-gray-400">暂无日志</p>}
      </div>
    </div>
  )
}

export default AdminOperationLogsPage
