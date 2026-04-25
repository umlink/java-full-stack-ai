import { type FC } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { Role } from '@/types/auth'

interface NavItem {
  label: string
  path: string
  minRole: Role
}

const navItems: NavItem[] = [
  { label: '数据看板', path: '/admin/dashboard', minRole: Role.OPERATOR },
  { label: '商品管理', path: '/admin/products', minRole: Role.OPERATOR },
  { label: '订单管理', path: '/admin/orders', minRole: Role.ADMIN },
  { label: '秒杀管理', path: '/admin/flash-sales', minRole: Role.OPERATOR },
  { label: '用户管理', path: '/admin/users', minRole: Role.ADMIN },
  { label: '操作日志', path: '/admin/operation-logs', minRole: Role.ADMIN },
]

const AdminLayout: FC = () => {
  const { user } = useAuthStore()
  const navigate = useNavigate()

  const visibleItems = navItems.filter((item) => {
    if (!user) return false
    return (user.role as number) >= item.minRole
  })

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top bar */}
      <header className="bg-white/90 backdrop-blur-md border-b border-gray-200 sticky top-0 z-40">
        <div className="flex items-center justify-between px-6 h-14">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-semibold text-gray-900">管理后台</h1>
            <button
              onClick={() => navigate('/')}
              className="text-sm text-gray-400 hover:text-gray-600"
            >
              返回前台 &rarr;
            </button>
          </div>
          <div className="text-sm text-gray-500">
            {user?.username}
          </div>
        </div>
      </header>

      <div className="flex">
        {/* Sidebar */}
        <aside className="w-56 shrink-0 bg-white border-r border-gray-200 min-h-[calc(100vh-3.5rem)]">
          <nav className="py-2">
            {visibleItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `block px-5 py-2.5 text-sm transition-colors ${
                    isActive
                      ? 'text-blue-600 bg-blue-50 font-medium border-r-2 border-blue-600'
                      : 'text-gray-600 hover:text-blue-600 hover:bg-gray-50'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        {/* Content */}
        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
