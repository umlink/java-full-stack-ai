import { useState, useEffect, useMemo, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { getProductDetail } from '@/api/product'
import { addToCart } from '@/api/cart'
import { useAuthStore } from '@/stores/authStore'
import type { ProductDetailVO, SkuVO } from '@/api/product'
import { Loading } from '@/components/Loading'
import { showToast } from '@/utils/toast'

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  const [product, setProduct] = useState<ProductDetailVO | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [selectedAttrs, setSelectedAttrs] = useState<Record<string, string>>({})
  const [currentImage, setCurrentImage] = useState(0)
  const [addToCartLoading, setAddToCartLoading] = useState(false)
  const [buyNowLoading, setBuyNowLoading] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    getProductDetail(Number(id))
      .then((res) => {
        if (res.data) {
          setProduct(res.data)
          setError(false)
        } else {
          setError(true)
        }
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [id])

  // 解析 SKU attrs JSON 字符串为 { key: value } 格式
  const parseSkuAttrs = (attrsStr: string): Record<string, string> => {
    try {
      const parsed = JSON.parse(attrsStr)
      if (Array.isArray(parsed)) {
        const result: Record<string, string> = {}
        parsed.forEach((item: { k: string; v: string }) => { result[item.k] = item.v })
        return result
      }
      return parsed
    } catch {
      return {}
    }
  }

  // 匹配当前选中的 SKU
  const matchedSku = useMemo<SkuVO | null>(() => {
    if (!product || !product.skus || product.skus.length === 0) return null
    const attrKeys = Object.keys(selectedAttrs)
    if (attrKeys.length === 0) return null
    return (
      product.skus.find((sku) => {
        if (!sku.attrs) return false
        const parsed = parseSkuAttrs(sku.attrs)
        return attrKeys.every((key) => parsed[key] === selectedAttrs[key])
      }) || null
    )
  }, [product, selectedAttrs])

  // 当前显示的图片 URL
  const currentImageSrc = useMemo(() => {
    if (!product) return ''
    if (matchedSku && matchedSku.image) return matchedSku.image
    if (product.images && product.images.length > 0) {
      return product.images[currentImage] || product.images[0]
    }
    return product.mainImage || ''
  }, [product, matchedSku, currentImage])

  // 所有展示图片（优先 SKU 图片，否则用商品图片列表）
  const displayImages = useMemo(() => {
    if (!product) return []
    if (matchedSku && matchedSku.image) return [matchedSku.image]
    return product.images?.length ? product.images : product.mainImage ? [product.mainImage] : []
  }, [product, matchedSku])

  // 库存状态
  const stockStatus = useMemo(() => {
    if (!product) return { stock: 0, outOfStock: true }
    if (product.hasSku && matchedSku) {
      return { stock: matchedSku.stock, outOfStock: matchedSku.stock <= 0 }
    }
    if (!product.hasSku) {
      return { stock: product.totalStock, outOfStock: product.totalStock <= 0 }
    }
    // 有 SKU 但未选择
    return { stock: 0, outOfStock: true }
  }, [product, matchedSku])

  const handleAddToCart = useCallback(async () => {
    if (!isAuthenticated) {
      navigate('/login?redirect=' + encodeURIComponent(window.location.pathname))
      return
    }
    if (!product) return
    if (product.hasSku && !matchedSku) return

    setAddToCartLoading(true)
    try {
      await addToCart({
        productId: product.id,
        skuId: matchedSku?.id,
        quantity: 1,
      })
      showToast('已加入购物车')
    } catch {
      showToast('加入购物车失败，请重试')
    } finally {
      setAddToCartLoading(false)
    }
  }, [product, matchedSku, isAuthenticated, navigate])

  const handleBuyNow = useCallback(async () => {
    if (!isAuthenticated) {
      navigate('/login?redirect=' + encodeURIComponent(window.location.pathname))
      return
    }
    if (!product) return
    if (product.hasSku && !matchedSku) return

    setBuyNowLoading(true)
    try {
      await addToCart({
        productId: product.id,
        skuId: matchedSku?.id,
        quantity: 1,
      })
      navigate('/checkout')
    } catch {
      showToast('操作失败，请重试')
    } finally {
      setBuyNowLoading(false)
    }
  }, [product, matchedSku, isAuthenticated, navigate])

  // 计算按钮状态文案
  const buttonState = useMemo(() => {
    if (addToCartLoading) return { disabled: true, text: '添加中...', style: 'bg-gray-400 cursor-not-allowed' }
    if (product?.hasSku && !matchedSku) return { disabled: true, text: '请选择规格', style: 'bg-gray-400 cursor-not-allowed' }
    if (stockStatus.outOfStock) return { disabled: true, text: '暂时缺货', style: 'bg-gray-400 cursor-not-allowed' }
    return { disabled: false, text: '加入购物车', style: 'bg-red-600 hover:bg-red-700' }
  }, [addToCartLoading, product, matchedSku, stockStatus])

  // 加载中状态
  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <Loading text="商品加载中..." />
      </div>
    )
  }

  // 错误 / 不存在
  if (error || !product) {
    return (
      <div className="text-center py-20">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">商品已下架或不存在</h1>
        <Link to="/products" className="text-blue-600 hover:underline">返回商品列表</Link>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* 左侧图片区 */}
        <div>
          <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden mb-4 flex items-center justify-center">
            {currentImageSrc ? (
              <img src={currentImageSrc} alt={product.name} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full bg-gray-200 flex items-center justify-center text-gray-400">
                暂无图片
              </div>
            )}
          </div>
          {/* 缩略图列表 */}
          {displayImages.length > 1 && (
            <div className="flex gap-2">
              {displayImages.map((img, idx) => (
                <button
                  key={idx}
                  onClick={() => setCurrentImage(idx)}
                  className={`w-16 h-16 rounded border-2 overflow-hidden flex-shrink-0 ${
                    currentImage === idx ? 'border-blue-500' : 'border-transparent'
                  }`}
                >
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 右侧信息区 */}
        <div>
          <h1 className="text-2xl font-bold mb-2">{product.name}</h1>
          {product.brief && <p className="text-gray-500 mb-4">{product.brief}</p>}

          {/* 品牌和标签 */}
          <div className="flex items-center gap-2 mb-4">
            {product.brandName && (
              <span className="text-sm text-gray-500">{product.brandName}</span>
            )}
            {product.tags?.map((tag) => (
              <span key={tag} className="px-2 py-0.5 bg-red-50 text-red-600 text-xs rounded">
                {tag}
              </span>
            ))}
          </div>

          {/* 价格 */}
          <div className="text-3xl text-red-600 font-bold mb-4">
            {'¥'}{matchedSku ? matchedSku.price : product.price}
            {product.hasSku && !matchedSku && product.maxPrice && product.minPrice !== product.maxPrice && (
              <span className="text-lg text-gray-400"> - {'¥'}{product.maxPrice}</span>
            )}
          </div>

          {/* 已售 */}
          <div className="text-sm text-gray-500 mb-6">已售 {product.sales} 件</div>

          {/* SKU 选择器 */}
          {product.hasSku &&
            product.specs?.map((spec) => (
              <div key={spec.name} className="mb-4">
                <div className="text-sm text-gray-600 mb-2">{spec.name}</div>
                <div className="flex flex-wrap gap-2">
                  {spec.values.map((val) => (
                    <button
                      key={val}
                      onClick={() =>
                        setSelectedAttrs((prev) => ({ ...prev, [spec.name]: val }))
                      }
                      className={`px-4 py-2 rounded border text-sm transition-colors ${
                        selectedAttrs[spec.name] === val
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400'
                      }`}
                    >
                      {val}
                    </button>
                  ))}
                </div>
              </div>
            ))}

          {/* 库存信息 */}
          <div className="text-sm text-gray-500 mb-6">
            库存: {stockStatus.stock} 件
          </div>

          {/* 操作按钮 */}
          <div className="mt-6 flex gap-3">
            <button
              onClick={handleAddToCart}
              disabled={addToCartLoading || buyNowLoading || buttonState.disabled}
              className={`flex-1 py-3 rounded-lg text-white font-bold text-lg transition-colors ${
                addToCartLoading
                  ? 'bg-gray-400 cursor-not-allowed'
                  : buttonState.disabled
                    ? 'bg-gray-400 cursor-not-allowed'
                    : 'bg-orange-500 hover:bg-orange-600'
              }`}
            >
              {addToCartLoading ? '添加中...' : buttonState.disabled ? buttonState.text : '加入购物车'}
            </button>
            <button
              onClick={handleBuyNow}
              disabled={buyNowLoading || addToCartLoading || buttonState.disabled}
              className={`flex-1 py-3 rounded-lg text-white font-bold text-lg transition-colors ${
                buyNowLoading
                  ? 'bg-gray-400 cursor-not-allowed'
                  : buttonState.disabled
                    ? 'bg-gray-400 cursor-not-allowed'
                    : 'bg-red-600 hover:bg-red-700'
              }`}
            >
              {buyNowLoading ? '跳转中...' : '立即购买'}
            </button>
          </div>

          {/* 参数属性表 */}
          {product.attrs?.length > 0 && (
            <div className="mt-8">
              <h3 className="font-bold mb-3">商品参数</h3>
              <div className="border rounded divide-y">
                {product.attrs.map((attr) => (
                  <div key={attr.name} className="flex px-4 py-2 text-sm">
                    <span className="text-gray-500 w-32 flex-shrink-0">{attr.name}</span>
                    <span>{attr.value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
