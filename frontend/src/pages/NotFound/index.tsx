import { type FC } from 'react'
import { Link } from 'react-router-dom'

const NotFound: FC = () => {
  return (
    <div className="flex flex-col items-center justify-center py-24">
      <h1 className="text-6xl font-bold text-gray-300 mb-4">404</h1>
      <p className="text-xl text-gray-600 mb-8">页面不存在</p>
      <Link
        to="/"
        className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
      >
        返回首页
      </Link>
    </div>
  )
}

export default NotFound
