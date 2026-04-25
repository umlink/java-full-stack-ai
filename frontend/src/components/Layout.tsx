import { type FC } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

export const Layout: FC = () => {
  const { user, isAuthenticated, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      {/* Header */}
      <header className="bg-white/90 backdrop-blur-md shadow-sm border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          {/* Logo + Navigation */}
          <div className="flex items-center gap-8">
            <Link to="/" className="text-xl font-bold text-blue-600 shrink-0">
              全栈商城
            </Link>
            <nav className="hidden md:flex items-center gap-6">
              <Link to="/" className="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                首页
              </Link>
              <Link to="/products" className="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                商品
              </Link>
              <Link to="/flash-sale" className="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                秒杀
              </Link>
            </nav>
          </div>

          {/* Search + Auth */}
          <div className="flex items-center gap-4">
            {/* Auth section */}
            {isAuthenticated && user ? (
              <div className="flex items-center gap-3">
                <Link
                  to="/user"
                  className="flex items-center gap-2 text-sm text-gray-600 hover:text-blue-600 transition-colors"
                >
                  {user.avatar ? (
                    <img src={user.avatar} alt="" className="h-8 w-8 rounded-full object-cover" />
                  ) : (
                    <div className="h-8 w-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-sm font-medium">
                      {user.username.charAt(0).toUpperCase()}
                    </div>
                  )}
                  <span className="hidden sm:inline">{user.username}</span>
                </Link>
                <Link
                  to="/user"
                  className="text-xs text-gray-400 hover:text-blue-600 transition-colors"
                >
                  个人中心
                </Link>
                <button
                  onClick={handleLogout}
                  className="text-xs text-gray-400 hover:text-red-500 transition-colors"
                >
                  退出
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="px-4 py-1.5 text-sm text-blue-600 border border-blue-600 rounded-lg hover:bg-blue-50 transition-colors"
                >
                  登录
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-1.5 text-sm text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
                >
                  注册
                </Link>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 py-6">
        <div className="max-w-7xl mx-auto px-4 text-center text-sm text-gray-400">
          <p>&copy; {new Date().getFullYear()} 全栈商城. All rights reserved.</p>
        </div>
      </footer>
    </div>
  )
}
