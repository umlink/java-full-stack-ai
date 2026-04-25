import { createBrowserRouter, Navigate, RouterProvider as RRouterProvider } from 'react-router-dom'
import { Suspense, lazy, type FC, type ReactNode } from 'react'
import { Layout } from '@/components/Layout'
import { Loading } from '@/components/Loading'
import { AuthGuard } from '@/components/AuthGuard'
import { AdminGuard } from '@/components/AdminGuard'

// 使用 lazy 懒加载页面组件
const Home = lazy(() => import('@/pages/Home'))
const Login = lazy(() => import('@/pages/Login'))
const Register = lazy(() => import('@/pages/Register'))
const NotFound = lazy(() => import('@/pages/NotFound'))
const Forbidden = lazy(() => import('@/pages/Forbidden'))
const ProductList = lazy(() => import('@/pages/ProductList'))
const ProductDetail = lazy(() => import('@/pages/ProductDetail'))
const Cart = lazy(() => import('@/pages/Cart'))
const Checkout = lazy(() => import('@/pages/Checkout'))
const OrderList = lazy(() => import('@/pages/OrderList'))
const OrderDetail = lazy(() => import('@/pages/OrderDetail'))
const UserCenter = lazy(() => import('@/pages/UserCenter'))
const Profile = lazy(() => import('@/pages/UserCenter/Profile'))
const ChangePassword = lazy(() => import('@/pages/UserCenter/ChangePassword'))
const AddressManager = lazy(() => import('@/pages/UserCenter/AddressManager'))
const FlashSaleList = lazy(() => import('@/pages/FlashSale/FlashSaleList'))
const FlashSaleDetail = lazy(() => import('@/pages/FlashSale/FlashSaleDetail'))
const AdminLayout = lazy(() => import('@/components/AdminLayout'))
const Dashboard = lazy(() => import('@/pages/Admin/Dashboard'))
const AdminProducts = lazy(() => import('@/pages/Admin/AdminProducts'))
const AdminOrders = lazy(() => import('@/pages/Admin/AdminOrders'))
const AdminFlashSales = lazy(() => import('@/pages/Admin/AdminFlashSales'))
const AdminUsers = lazy(() => import('@/pages/Admin/AdminUsers'))
const AdminOperationLogs = lazy(() => import('@/pages/Admin/AdminOperationLogs'))

interface LazyLoadProps {
  children: ReactNode
}

const LazyLoad: FC<LazyLoadProps> = ({ children }) => (
  <Suspense fallback={<Loading text="页面加载中..." />}>{children}</Suspense>
)

// 路由配置
const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <LazyLoad><Home /></LazyLoad> },
      { path: 'login', element: <LazyLoad><Login /></LazyLoad> },
      { path: 'register', element: <LazyLoad><Register /></LazyLoad> },
      { path: 'products', element: <LazyLoad><ProductList /></LazyLoad> },
      { path: 'products/:id', element: <LazyLoad><ProductDetail /></LazyLoad> },
      {
        path: 'cart',
        element: <AuthGuard><LazyLoad><Cart /></LazyLoad></AuthGuard>,
      },
      {
        path: 'checkout',
        element: <AuthGuard><LazyLoad><Checkout /></LazyLoad></AuthGuard>,
      },
      {
        path: 'orders',
        element: <AuthGuard><LazyLoad><OrderList /></LazyLoad></AuthGuard>,
      },
      {
        path: 'orders/:id',
        element: <AuthGuard><LazyLoad><OrderDetail /></LazyLoad></AuthGuard>,
      },
      { path: 'flash-sale', element: <LazyLoad><FlashSaleList /></LazyLoad> },
      { path: 'flash-sale/:id', element: <LazyLoad><FlashSaleDetail /></LazyLoad> },
      {
        path: 'user',
        element: <AuthGuard><LazyLoad><UserCenter /></LazyLoad></AuthGuard>,
        children: [
          { index: true, element: <Navigate to="/user/profile" replace /> },
          { path: 'profile', element: <LazyLoad><Profile /></LazyLoad> },
          { path: 'password', element: <LazyLoad><ChangePassword /></LazyLoad> },
          { path: 'addresses', element: <LazyLoad><AddressManager /></LazyLoad> },
        ],
      },
      { path: '403', element: <LazyLoad><Forbidden /></LazyLoad> },
      { path: '404', element: <LazyLoad><NotFound /></LazyLoad> },
      { path: '*', element: <Navigate to="/404" replace /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <AdminGuard>
        <Suspense fallback={<Loading text="管理后台加载中..." />}>
          <AdminLayout />
        </Suspense>
      </AdminGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/admin/dashboard" replace /> },
      { path: 'dashboard', element: <Suspense fallback={<Loading />}><Dashboard /></Suspense> },
      { path: 'products', element: <Suspense fallback={<Loading />}><AdminProducts /></Suspense> },
      { path: 'orders', element: <Suspense fallback={<Loading />}><AdminOrders /></Suspense> },
      { path: 'flash-sales', element: <Suspense fallback={<Loading />}><AdminFlashSales /></Suspense> },
      { path: 'users', element: <Suspense fallback={<Loading />}><AdminUsers /></Suspense> },
      { path: 'operation-logs', element: <Suspense fallback={<Loading />}><AdminOperationLogs /></Suspense> },
    ],
  },
]

const router = createBrowserRouter(routes)

export const AppRouterProvider: FC = () => {
  return (
    <Suspense fallback={<Loading size="lg" text="页面加载中..." />}>
      <RRouterProvider router={router} />
    </Suspense>
  )
}

export default routes
