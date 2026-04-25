import { useEffect, useState, type FC } from 'react'
import { Loading } from '@/components/Loading'
import { getProducts, type ProductVO } from '@/api/product'

const AdminProductsPage: FC = () => {
  const [products, setProducts] = useState<ProductVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchProducts()
  }, [])

  const fetchProducts = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getProducts({ page: 1, pageSize: 50 })
      if (res.code === 200 && res.data) {
        setProducts(res.data.records ?? [])
      } else {
        setError(res.message ?? '获取商品失败')
      }
    } catch {
      setError('网络错误')
    } finally {
      setLoading(false)
    }
  }

  if (loading) return <Loading text="加载商品列表..." />
  if (error) return (
    <div className="text-center py-12">
      <p className="text-red-500 mb-4">{error}</p>
      <button onClick={fetchProducts} className="px-4 py-2 bg-blue-600 text-white rounded-lg">重新加载</button>
    </div>
  )

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">商品管理</h2>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">商品名称</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">价格</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">库存</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">销量</th>
              <th className="text-left px-4 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {products.map((p) => (
              <tr key={p.id} className="hover:bg-gray-50 transition-colors duration-150">
                <td className="px-4 py-3.5 text-gray-400 text-xs">{p.id}</td>
                <td className="px-4 py-3.5 text-gray-900 max-w-[300px] truncate">{p.name}</td>
                <td className="px-4 py-3.5 text-gray-900 font-medium">¥{p.price.toFixed(2)}</td>
                <td className="px-4 py-3.5 text-gray-600">{p.totalStock}</td>
                <td className="px-4 py-3.5 text-gray-600">{p.sales}</td>
                <td className="px-4 py-3.5">
                  <span className={`inline-flex items-center text-xs font-medium px-2.5 py-1 rounded-full ${p.status === 1 ? 'bg-green-50 text-green-700 ring-1 ring-green-600/20' : 'bg-gray-50 text-gray-500 ring-1 ring-gray-600/20'}`}>
                    {p.status === 1 ? '上架' : '下架'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {products.length === 0 && <p className="text-center py-8 text-gray-400">暂无商品</p>}
      </div>
    </div>
  )
}

export default AdminProductsPage
