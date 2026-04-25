import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getProducts, getCategories } from '@/api/product'
import type { ProductVO, CategoryVO } from '@/types/product'

// --- Skeleton sub-component ---
function HomeSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 py-8 animate-pulse">
      {/* Hero skeleton */}
      <div className="bg-gray-200 rounded-xl h-48 mb-8" />

      {/* Category skeleton */}
      <div className="flex gap-4 mb-8">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex flex-col items-center gap-2">
            <div className="w-16 h-16 bg-gray-200 rounded-full" />
            <div className="w-12 h-3 bg-gray-200 rounded" />
          </div>
        ))}
      </div>

      {/* Flash sale skeleton */}
      <div className="bg-gray-100 rounded-xl h-24 mb-8" />

      {/* Product grid skeleton */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="bg-white rounded-lg shadow-sm p-4">
            <div className="aspect-square bg-gray-200 rounded-lg mb-3" />
            <div className="h-4 bg-gray-200 rounded w-3/4 mb-2" />
            <div className="h-4 bg-gray-200 rounded w-1/2" />
          </div>
        ))}
      </div>
    </div>
  )
}

// --- Category icon mapping (emoji fallback for display) ---
const CATEGORY_ICONS: Record<string, string> = {
  '手机': '📱',
  '电脑': '💻',
  '家电': '🏠',
  '服饰': '👕',
  '食品': '🍜',
  '图书': '📚',
  '美妆': '💄',
  '运动': '⚽',
  '母婴': '🍼',
  '家居': '🛋️',
  '数码': '📷',
  '配件': '🔌',
  '办公': '📎',
  '汽车': '🚗',
  '宠物': '🐾',
}

function getCategoryIcon(name: string): string {
  for (const [key, icon] of Object.entries(CATEGORY_ICONS)) {
    if (name.includes(key)) return icon
  }
  return '🏷️'
}

// --- Product Card ---
function ProductCard({ product }: { product: ProductVO }) {
  return (
    <Link
      to={`/products/${product.id}`}
      className="group bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md hover:border-gray-200 transition-all duration-200"
    >
      <div className="aspect-square bg-gray-50 flex items-center justify-center overflow-hidden">
        {product.mainImage ? (
          <img
            src={product.mainImage}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <span className="text-gray-300 text-4xl">🖼️</span>
        )}
      </div>
      <div className="p-4">
        <h3 className="text-sm font-medium text-gray-900 line-clamp-2 mb-2 min-h-[2.5rem]">
          {product.name}
        </h3>
        <div className="flex items-center justify-between">
          <span className="text-lg font-bold text-red-600">
            ¥{Number(product.price).toFixed(2)}
          </span>
          {product.sales > 0 && (
            <span className="text-xs text-gray-400">
              已售 {product.sales}
            </span>
          )}
        </div>
      </div>
    </Link>
  )
}

// --- Home Page ---
export default function Home() {
  const navigate = useNavigate()
  const [categories, setCategories] = useState<CategoryVO[]>([])
  const [hotProducts, setHotProducts] = useState<ProductVO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getCategories(),
      getProducts({ sort: 'sales_desc', pageSize: 8 }),
    ])
      .then(([catRes, prodRes]) => {
        setCategories(catRes.data || [])
        setHotProducts(prodRes.data?.records || [])
      })
      .catch(() => {
        // Silent fail — empty data acts as graceful degradation
      })
      .finally(() => setLoading(false))
  }, [])

  // Top-level categories for the entry grid
  const topCategories = categories.filter(
    (c) => c.level === 1 || c.parentId === 0,
  )

  if (loading) return <HomeSkeleton />

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-6 space-y-8">
        {/* ===== Hero Banner ===== */}
        <section className="bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-500 rounded-2xl p-8 md:p-12 text-white relative overflow-hidden">
          {/* Decorative circles */}
          <div className="absolute -top-10 -right-10 w-40 h-40 bg-white/10 rounded-full" />
          <div className="absolute -bottom-8 -left-8 w-32 h-32 bg-white/5 rounded-full" />

          <div className="relative z-10">
            <h1 className="text-3xl md:text-4xl font-extrabold mb-3 tracking-tight">
              全栈商城
            </h1>
            <p className="text-lg md:text-xl text-white/80 mb-6">
              发现好物，畅享购物
            </p>
            <div className="relative max-w-md">
              <input
                type="text"
                placeholder="搜索你想要的商品..."
                className="w-full px-5 py-3 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-white/50 shadow-lg"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    const value = (e.target as HTMLInputElement).value.trim()
                    if (value) {
                      navigate(`/products?keyword=${encodeURIComponent(value)}`)
                    }
                  }
                }}
              />
              <svg
                className="absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
          </div>
        </section>

        {/* ===== Category Entry ===== */}
        {topCategories.length > 0 && (
          <section>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-gray-900">商品分类</h2>
              <Link
                to="/products"
                className="text-sm text-blue-600 hover:text-blue-700 transition-colors"
              >
                查看全部 &rarr;
              </Link>
            </div>
            <div className="flex flex-wrap gap-4 md:gap-6">
              {topCategories.slice(0, 10).map((cat) => (
                <Link
                  key={cat.id}
                  to={`/products?categoryId=${cat.id}`}
                  className="flex flex-col items-center gap-2 group w-[18%] sm:w-[12%] min-w-[72px]"
                >
                  <span className="w-14 h-14 md:w-16 md:h-16 rounded-2xl bg-white shadow-sm border border-gray-100 flex items-center justify-center text-2xl group-hover:shadow-md group-hover:border-blue-200 group-hover:bg-blue-50 transition-all duration-200">
                    {getCategoryIcon(cat.name)}
                  </span>
                  <span className="text-xs text-gray-600 group-hover:text-blue-600 transition-colors truncate w-full text-center">
                    {cat.name}
                  </span>
                </Link>
              ))}
            </div>
          </section>
        )}

        {/* ===== Flash Sale Entry ===== */}
        <Link
          to="/flash-sale"
          className="block bg-gradient-to-r from-red-500 to-rose-600 rounded-2xl p-6 hover:from-red-600 hover:to-rose-700 transition-all duration-200 shadow-md hover:shadow-lg"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
                <span className="text-2xl">⚡</span>
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">限时秒杀</h2>
                <p className="text-sm text-white/80 mt-0.5">
                  超值好物，限时抢购
                </p>
              </div>
            </div>
            <span className="inline-flex items-center gap-1 bg-white/20 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-white/30 transition-colors">
              立即前往
              <svg
                className="w-4 h-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </span>
          </div>
        </Link>

        {/* ===== Hot Products ===== */}
        <section>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-bold text-gray-900">热门商品</h2>
            <Link
              to="/products?sort=sales_desc"
              className="text-sm text-blue-600 hover:text-blue-700 transition-colors"
            >
              查看更多 &rarr;
            </Link>
          </div>

          {hotProducts.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-xl border border-dashed border-gray-200">
              <span className="text-4xl">📦</span>
              <p className="mt-3 text-gray-500">暂无商品</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-5">
              {hotProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
