import { useEffect, useState, type FC } from 'react'
import { useNavigate } from 'react-router-dom'
import { Loading } from '@/components/Loading'
import { useCartStore, useCartComputed } from '@/stores/cartStore'
import { getAddressList, type AddressVO } from '@/api/address'
import { createOrder } from '@/api/order'

const CheckoutPage: FC = () => {
  const navigate = useNavigate()
  const { fetchCart, loading: cartLoading } = useCartStore()
  const { selectedItems, totalAmount, selectedCount } = useCartComputed()
  const [addresses, setAddresses] = useState<AddressVO[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [addressLoading, setAddressLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchCart()
    loadAddresses()
  }, [])

  const loadAddresses = async () => {
    setAddressLoading(true)
    try {
      const res = await getAddressList()
      if (res.code === 200) {
        const list = res.data ?? []
        setAddresses(list)
        const defaultAddr = list.find((a) => a.isDefault)
        if (defaultAddr) setSelectedAddressId(defaultAddr.id)
        else if (list.length > 0) setSelectedAddressId(list[0].id)
      }
    } catch {
      // silent
    } finally {
      setAddressLoading(false)
    }
  }

  const handleSubmit = async () => {
    if (!selectedAddressId) {
      setError('请选择收货地址')
      return
    }
    if (selectedItems.length === 0) {
      setError('请选择要购买的商品')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      const res = await createOrder({
        cartItemIds: selectedItems.map((i) => i.id),
        addressId: selectedAddressId,
      })
      if (res.code === 200 && res.data) {
        navigate(`/orders/${res.data.id}`)
      } else {
        setError(res.message ?? '下单失败')
      }
    } catch {
      setError('网络错误，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  if (cartLoading || addressLoading) {
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

  if (selectedItems.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 text-center">
        <p className="text-gray-500 mb-4">没有选中商品</p>
        <button
          onClick={() => navigate('/cart')}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg"
        >
          返回购物车
        </button>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">确认订单</h1>

      {/* Address selection */}
      <section className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
        <h2 className="text-sm font-medium text-gray-900 mb-3">收货地址</h2>
        {addresses.length === 0 ? (
          <p className="text-sm text-gray-400">
            暂无地址，请先在
            <button
              onClick={() => navigate('/user/addresses')}
              className="text-blue-600 hover:underline mx-1"
            >
              地址管理
            </button>
            中添加
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {addresses.map((addr) => (
              <div
                key={addr.id}
                onClick={() => setSelectedAddressId(addr.id)}
                className={`border rounded-lg p-3 cursor-pointer transition-colors ${
                  selectedAddressId === addr.id
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-medium text-gray-900">{addr.receiverName}</span>
                  <span className="text-xs text-gray-400">
                    {addr.receiverPhone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')}
                  </span>
                  {addr.isDefault && (
                    <span className="text-xs text-blue-600 bg-blue-100 px-1.5 py-0.5 rounded">默认</span>
                  )}
                </div>
                <p className="text-xs text-gray-500">
                  {addr.province} {addr.city} {addr.district} {addr.detail}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Order items */}
      <section className="bg-white rounded-lg border border-gray-200 p-4 mb-4">
        <h2 className="text-sm font-medium text-gray-900 mb-3">商品清单</h2>
        <div className="divide-y divide-gray-100">
          {selectedItems.map((item) => (
            <div key={item.id} className="flex items-center gap-4 py-3">
              <img
                src={item.productImage}
                alt={item.productName}
                className="h-16 w-16 object-cover rounded-lg border border-gray-100 shrink-0"
              />
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-900 truncate">{item.productName}</p>
                {item.skuName && (
                  <p className="text-xs text-gray-400 mt-0.5">{item.skuName}</p>
                )}
              </div>
              <div className="text-sm text-gray-500 shrink-0">×{item.quantity}</div>
              <div className="text-sm font-semibold text-gray-900 shrink-0 w-20 text-right">
                ¥{(item.price * item.quantity).toFixed(2)}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Error */}
      {error && (
        <div className="bg-red-50 text-red-600 text-sm rounded-lg px-4 py-2 mb-4">{error}</div>
      )}

      {/* Submit bar */}
      <div className="sticky bottom-0 bg-white/90 backdrop-blur-md rounded-lg border border-gray-200 p-4 flex items-center justify-between shadow-lg">
        <div className="text-sm text-gray-500">
          共 <span className="text-blue-600 font-semibold">{selectedCount}</span> 件商品
        </div>
        <div className="flex items-center gap-6">
          <div className="text-sm">
            合计:{' '}
            <span className="text-lg font-bold text-red-500">
              ¥{totalAmount.toFixed(2)}
            </span>
          </div>
          <button
            onClick={handleSubmit}
            disabled={submitting || !selectedAddressId}
            className="px-8 py-2 bg-red-500 text-white rounded-lg text-sm font-medium hover:bg-red-600 disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            {submitting ? '提交中...' : '提交订单'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default CheckoutPage
