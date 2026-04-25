import { type FC } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

const navItems = [
  { label: '个人信息', path: '/user/profile' },
  { label: '收货地址', path: '/user/addresses' },
  { label: '修改密码', path: '/user/password' },
  { label: '我的订单', path: '/orders' },
]

const UserCenterLayout: FC = () => {
  const { user } = useAuthStore()

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Page title */}
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">个人中心</h1>

      <div className="flex gap-8">
        {/* Sidebar */}
        <aside className="w-56 shrink-0">
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            {/* User info card */}
            <div className="p-5 border-b border-gray-100">
              <div className="flex items-center gap-3">
                {user?.avatar ? (
                  <img src={user.avatar} alt="" className="h-12 w-12 rounded-full object-cover" />
                ) : (
                  <div className="h-12 w-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-lg font-semibold">
                    {user?.username?.charAt(0).toUpperCase()}
                  </div>
                )}
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">{user?.username}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {user?.phone || '未绑定手机'}
                  </p>
                </div>
              </div>
            </div>

            {/* Navigation */}
            <nav className="py-2">
              {navItems.map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  end={item.path === '/user/profile'}
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
          </div>
        </aside>

        {/* Content area */}
        <main className="flex-1 min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default UserCenterLayout
