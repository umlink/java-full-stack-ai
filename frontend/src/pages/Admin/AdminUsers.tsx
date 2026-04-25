import { useEffect, useState, type FC } from 'react'
import { Loading } from '@/components/Loading'
import { getUserList, updateUserRole, toggleUserStatus } from '@/api/admin'
import type { UserInfo } from '@/types/auth'
import { Role } from '@/types/auth'

const roleLabels: Record<number, string> = {
  [Role.USER]: '用户',
  [Role.OPERATOR]: '运营',
  [Role.ADMIN]: '管理员',
}

const roleColors: Record<number, string> = {
  [Role.USER]: 'bg-gray-100 text-gray-600',
  [Role.OPERATOR]: 'bg-blue-50 text-blue-600',
  [Role.ADMIN]: 'bg-purple-50 text-purple-600',
}

const AdminUsersPage: FC = () => {
  const [users, setUsers] = useState<UserInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchUsers = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getUserList({ page: 1, pageSize: 50 })
      if (res.code === 200 && res.data) {
        setUsers(res.data.records ?? [])
      } else {
        setError(res.message ?? '获取用户失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchUsers() }, [])

  if (loading) return <Loading text="加载用户列表..." />
  if (error) return (
    <div className="text-center py-12">
      <p className="text-red-500 mb-4">{error}</p>
      <button onClick={fetchUsers} className="px-4 py-2 bg-blue-600 text-white rounded-lg">重新加载</button>
    </div>
  )

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">用户管理</h2>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">用户名</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">邮箱</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">角色</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">注册时间</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {users.map((u) => (
              <tr key={u.id} className="hover:bg-gray-50 transition-colors duration-150">
                <td className="px-4 py-3.5 text-gray-400 text-xs">{u.id}</td>
                <td className="px-4 py-3.5 text-gray-900">{u.username}</td>
                <td className="px-4 py-3.5 text-gray-500">{u.email || '-'}</td>
                <td className="px-4 py-3.5">
                  <span className={`inline-flex items-center text-xs font-medium px-2.5 py-1 rounded-full ${roleColors[u.role] ?? ''}`}>
                    {roleLabels[u.role] ?? '未知'}
                  </span>
                </td>
                <td className="px-4 py-3.5 text-gray-400 text-xs">{u.createdAt ? new Date(u.createdAt).toLocaleString('zh-CN') : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {users.length === 0 && <p className="text-center py-8 text-gray-400">暂无用户</p>}
      </div>
    </div>
  )
}

export default AdminUsersPage
