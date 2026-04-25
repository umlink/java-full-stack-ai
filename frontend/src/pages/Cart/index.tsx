import { useEffect, useState, type FC } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Loading } from '@/components/Loading'
import { Empty } from '@/components/Empty'
import { useCartStore, useCartComputed } from '@/stores/cartStore'
import { addToCart } from '@/api/cart'

const CartPage: FC = () => {
  const navigate = useNavigate()
  const { items, loading, error, fetchCart, toggleSelect, selectAll, updateQuantity, removeItem } =
    useCartStore()
  const { totalAmount, selectedCount, isAllSelected } = useCartComputed()
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    fetchCart()
  }, [])

  const handleCheckout = () => {
    if (selectedCount === 0) return
    navigate('/checkout')
  }

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-24 bg-gray-100 rounded-lg animate-pulse" />
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="text-center py-12">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            onClick={fetchCart}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            重新加载
          </button>
        </div>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <Empty
          title="购物车是空的"
          description="快去挑选心仪的商品吧"
          action={
            <Link
              to="/products"
              className="inline-block px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              去逛逛
            </Link>
          }
        />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">购物车</h1>

      {/* Select all */}
      <div className="bg-white rounded-lg border border-gray-200 px-4 py-3 mb-4 flex items-center gap-3">
        <input
          type="checkbox"
          checked={isAllSelected}
          onChange={(e) => selectAll(e.target.checked)}
          className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
        />
        <span className="text-sm text-gray-700">全选</span>
        <span className="text-sm text-gray-400 ml-auto">
          共 {items.length} 件商品
        </span>
      </div>

      {/* Cart items */}
      <div className="space-y-3">
        {items.map((item) => {
          const isOffline = item.isOffline
          const outOfStock = !isOffline && item.stock === 0
          const maxQty = Math.min(item.stock, 99)

          return (
            <div
              key={item.id}
              className={`bg-white rounded-lg border border-gray-200 p-4 flex items-center gap-4 ${
                isOffline ? 'opacity-60' : ''
              }`}
            >
              {/* Checkbox */}
              <input
                type="checkbox"
                checked={useCartStore.getState().selectedIds.has(item.id)}
                onChange={() => toggleSelect(item.id)}
                disabled={isOffline}
                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 disabled:cursor-not-allowed"
              />

              {/* Product image */}
              <Link to={`/products/${item.productId}`} className="shrink-0">
                <img
                  src={item.productImage}
                  alt={item.productName}
                  className="h-20 w-20 object-cover rounded-lg border border-gray-100"
                />
              </Link>

              {/* Product info */}
              <div className="flex-1 min-w-0">
                <Link
                  to={`/products/${item.productId}`}
                  className="text-sm font-medium text-gray-900 hover:text-blue-600 line-clamp-2"
                >
                  {item.productName}
                </Link>
                {item.skuName && (
                  <p className="text-xs text-gray-400 mt-1">{item.skuName}</p>
                )}
                {isOffline && (
                  <span className="inline-block mt-1 text-xs text-red-500 bg-red-50 px-2 py-0.5 rounded">
                    该商品已下架
                  </span>
                )}
                {outOfStock && !isOffline && (
                  <span className="inline-block mt-1 text-xs text-orange-500 bg-orange-50 px-2 py-0.5 rounded">
                    库存不足
                  </span>
                )}
              </div>

              {/* Price */}
              <div className="text-sm font-semibold text-red-500 shrink-0">
                ¥{item.price.toFixed(2)}
              </div>

              {/* Quantity selector */}
              <div className="flex items-center border border-gray-200 rounded-lg shrink-0">
                <button
                  onClick={() => {
                    if (item.quantity > 1) updateQuantity(item.id, item.quantity - 1)
                  }}
                  disabled={isOffline || item.quantity <= 1}
                  className="px-3 py-1.5 text-gray-500 hover:text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  -
                </button>
                <span className="px-3 py-1.5 text-sm text-gray-900 min-w-[2rem] text-center border-x border-gray-200">
                  {item.quantity}
                </span>
                <button
                  onClick={() => {
                    if (item.quantity < maxQty) updateQuantity(item.id, item.quantity + 1)
                  }}
                  disabled={isOffline || item.quantity >= maxQty}
                  className="px-3 py-1.5 text-gray-500 hover:text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  +
                </button>
              </div>

              {/* Subtotal */}
              <div className="text-sm font-semibold text-gray-900 shrink-0 w-20 text-right">
                ¥{(item.price * item.quantity).toFixed(2)}
              </div>

              {/* Delete */}
              <div className="shrink-0">
                {deleteConfirmId === item.id ? (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={async () => {
                        await removeItem(item.id)
                        setDeleteConfirmId(null)
                      }}
                      className="text-xs text-red-600 hover:text-red-700"
                    >
                      确认
                    </button>
                    <button
                      onClick={() => setDeleteConfirmId(null)}
                      className="text-xs text-gray-400 hover:text-gray-600"
                    >
                      取消
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => setDeleteConfirmId(item.id)}
                    className="text-sm text-gray-400 hover:text-red-500"
                  >
                    删除
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>

      {/* Bottom checkout bar */}
      <div className="sticky bottom-0 mt-6 bg-white/90 backdrop-blur-md rounded-lg border border-gray-200 px-4 py-3 flex items-center justify-between shadow-lg">
        <div className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={isAllSelected}
            onChange={(e) => selectAll(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <span className="text-sm text-gray-700">全选</span>
        </div>
        <div className="flex items-center gap-6">
          <div className="text-sm text-gray-500">
            已选 <span className="text-blue-600 font-semibold">{selectedCount}</span> 件
          </div>
          <div className="text-sm text-gray-500">
            合计:{' '}
            <span className="text-lg font-bold text-red-500">
              ¥{totalAmount.toFixed(2)}
            </span>
          </div>
          <button
            onClick={handleCheckout}
            disabled={selectedCount === 0 || submitting}
            className="px-8 py-2 bg-red-500 text-white rounded-lg text-sm font-medium hover:bg-red-600 disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            去结算
          </button>
        </div>
      </div>
    </div>
  )
}

export default CartPage
