import { useState, useEffect, useCallback, type FC } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { Empty } from '@/components/Empty'
import type { ProductVO, CategoryVO, BrandVO, ProductQuery } from '@/api/product'
import { getProducts, getCategories, getBrands } from '@/api/product'

// ---------- helpers ----------

function flattenCategories(tree: CategoryVO[], prefix = ''): { id: number; name: string }[] {
  const result: { id: number; name: string }[] = []
  for (const node of tree) {
    const label = prefix ? `${prefix} > ${node.name}` : node.name
    result.push({ id: node.id, name: label })
    if (node.children && node.children.length > 0) {
      result.push(...flattenCategories(node.children, label))
    }
  }
  return result
}

/** Number formatting with Chinese unit for sales */
function formatSales(n: number): string {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return n.toLocaleString()
}

function formatPrice(n: number): string {
  return `¥${n.toFixed(2)}`
}

const sortOptions = [
  { value: '', label: '综合排序' },
  { value: 'price_asc', label: '价格升序' },
  { value: 'price_desc', label: '价格降序' },
  { value: 'sales_desc', label: '销量优先' },
  { value: 'newest', label: '最新上架' },
]

const PAGE_SIZE = 20

// ---------- sub components ----------

interface ProductCardProps {
  product: ProductVO
}

const ProductCard: FC<ProductCardProps> = ({ product }) => {
  const [imgError, setImgError] = useState(false)
  const hasRange =
    product.hasSku &&
    product.minPrice != null &&
    product.maxPrice != null &&
    product.minPrice < product.maxPrice

  return (
    <Link
      to={`/products/${product.id}`}
      className="group bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md hover:border-gray-200 transition-all duration-200 flex flex-col"
    >
      {/* Image */}
      <div className="aspect-square bg-gray-100 relative overflow-hidden">
        {product.mainImage && !imgError ? (
          <img
            src={product.mainImage}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            onError={() => setImgError(true)}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-300 text-sm">
            暂无图片
          </div>
        )}
        {product.tags && product.tags.length > 0 && (
          <span className="absolute top-2 left-2 bg-red-500 text-white text-xs px-1.5 py-0.5 rounded">
            {product.tags[0]}
          </span>
        )}
      </div>

      {/* Body */}
      <div className="p-4 flex flex-col flex-1 gap-1.5">
        <h3 className="text-sm font-medium text-gray-900 line-clamp-2 leading-snug min-h-0 group-hover:text-blue-600 transition-colors">
          {product.name}
        </h3>

        {product.brandName && (
          <span className="text-xs text-gray-400">{product.brandName}</span>
        )}

        {product.brief && (
          <p className="text-xs text-gray-400 line-clamp-1">{product.brief}</p>
        )}

        <div className="mt-auto pt-2 flex items-baseline justify-between">
          <div className="flex items-baseline gap-0.5">
            {hasRange ? (
              <span className="text-base font-bold text-red-600">
                {formatPrice(product.minPrice!)} - {formatPrice(product.maxPrice!)}
              </span>
            ) : (
              <span className="text-base font-bold text-red-600">
                {formatPrice(product.price)}
              </span>
            )}
          </div>
          {product.sales > 0 && (
            <span className="text-xs text-gray-400">已售 {formatSales(product.sales)}</span>
          )}
        </div>
      </div>
    </Link>
  )
}

/** Skeleton card shown during loading */
const SkeletonCard: FC = () => (
  <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden animate-pulse">
    <div className="aspect-square bg-gray-200" />
    <div className="p-4 space-y-3">
      <div className="h-4 bg-gray-200 rounded w-3/4" />
      <div className="h-3 bg-gray-200 rounded w-1/3" />
      <div className="h-3 bg-gray-200 rounded w-1/2" />
      <div className="pt-2 flex justify-between">
        <div className="h-5 bg-gray-200 rounded w-1/3" />
        <div className="h-3 bg-gray-200 rounded w-1/4" />
      </div>
    </div>
  </div>
)

interface PaginationProps {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

const Pagination: FC<PaginationProps> = ({ page, totalPages, onChange }) => {
  if (totalPages <= 1) return null

  const pages: (number | 'ellipsis')[] = []
  const delta = 2
  const left = Math.max(2, page - delta)
  const right = Math.min(totalPages - 1, page + delta)

  pages.push(1)
  if (left > 2) pages.push('ellipsis')
  for (let i = left; i <= right; i++) pages.push(i)
  if (right < totalPages - 1) pages.push('ellipsis')
  if (totalPages > 1) pages.push(totalPages)

  const btnCls =
    'inline-flex items-center justify-center min-w-[2.25rem] h-9 px-3 py-1.5 text-sm rounded-lg border border-gray-200 bg-white hover:bg-gray-50 hover:border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-150'
  const activeCls = 'inline-flex items-center justify-center min-w-[2.25rem] h-9 px-3 py-1.5 text-sm rounded-lg bg-blue-600 text-white shadow-sm shadow-blue-200 font-medium'

  return (
    <div className="flex items-center justify-center gap-1.5 mt-8">
      <button className={btnCls} disabled={page <= 1} onClick={() => onChange(page - 1)}>
        上一页
      </button>
      {pages.map((p, idx) =>
        p === 'ellipsis' ? (
          <span key={`e-${idx}`} className="px-2 text-gray-400">
            ...
          </span>
        ) : (
          <button
            key={p}
            className={p === page ? activeCls : btnCls}
            onClick={() => onChange(p)}
          >
            {p}
          </button>
        ),
      )}
      <button className={btnCls} disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
        下一页
      </button>
    </div>
  )
}

// ---------- main component ----------

const ProductList: FC = () => {
  const [searchParams, setSearchParams] = useSearchParams()

  // Derived filter state from URL params
  const keyword = searchParams.get('keyword') || ''
  const categoryId = searchParams.get('categoryId') || ''
  const brandId = searchParams.get('brandId') || ''
  const minPrice = searchParams.get('minPrice') || ''
  const maxPrice = searchParams.get('maxPrice') || ''
  const sort = searchParams.get('sort') || ''
  const page = parseInt(searchParams.get('page') || '1', 10)

  // Data state
  const [products, setProducts] = useState<ProductVO[]>([])
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [categories, setCategories] = useState<CategoryVO[]>([])
  const [brands, setBrands] = useState<BrandVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Search input value (for controlled input)
  const [searchInput, setSearchInput] = useState(keyword)

  // Sync searchInput when keyword changes from URL
  useEffect(() => {
    setSearchInput(keyword)
  }, [keyword])

  // Fetch categories & brands on mount
  useEffect(() => {
    Promise.all([getCategories(), getBrands()])
      .then(([catRes, brandRes]) => {
        if (catRes.code === 200) setCategories(catRes.data)
        if (brandRes.code === 200) setBrands(brandRes.data)
      })
      .catch(() => {
        // Non-critical, silently ignore
      })
  }, [])

  // Fetch products when any filter changes
  const fetchProducts = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params: ProductQuery = { page, pageSize: PAGE_SIZE }
      if (categoryId) params.categoryId = Number(categoryId)
      if (brandId) params.brandId = Number(brandId)
      if (minPrice) params.minPrice = Number(minPrice)
      if (maxPrice) params.maxPrice = Number(maxPrice)
      if (keyword) params.keyword = keyword
      if (sort) params.sort = sort

      const res = await getProducts(params)
      if (res.code === 200) {
        setProducts(res.data.records)
        setTotal(res.data.total)
        setTotalPages(res.data.totalPages)
      } else {
        setError(res.message || '获取商品列表失败')
      }
    } catch {
      setError('网络连接失败，请检查网络后重试')
    } finally {
      setLoading(false)
    }
  }, [categoryId, brandId, minPrice, maxPrice, keyword, sort, page])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  // Filter update helpers
  const updateFilter = useCallback(
    (key: string, value: string) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        if (value === '' || value === 'all') {
          next.delete(key)
        } else {
          next.set(key, value)
        }
        // Reset page when any filter changes (except page itself)
        if (key !== 'page') {
          next.delete('page')
        }
        // Clean up empty values
        ;['keyword', 'categoryId', 'brandId', 'minPrice', 'maxPrice', 'sort'].forEach((k) => {
          if (!next.get(k)) next.delete(k)
        })
        return next
      })
    },
    [setSearchParams],
  )

  const handleSearch = useCallback(() => {
    updateFilter('keyword', searchInput.trim())
  }, [searchInput, updateFilter])

  const handleSearchKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') handleSearch()
    },
    [handleSearch],
  )

  const handlePageChange = useCallback(
    (p: number) => {
      updateFilter('page', String(p))
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    [updateFilter],
  )

  const handleRetry = useCallback(() => {
    fetchProducts()
  }, [fetchProducts])

  // Flattened category options
  const categoryOptions = flattenCategories(categories)

  // Determine empty state copy
  const hasActiveFilters = !!(categoryId || brandId || minPrice || maxPrice || keyword)

  // ---- render ----

  return (
    <div className="max-w-7xl mx-auto px-4 py-6">
      {/* ---------- Filter bar ---------- */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-6 space-y-4">
        {/* Search row */}
        <div className="flex gap-2">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={handleSearchKeyDown}
            placeholder="搜索商品名称…"
            className="flex-1 h-10 px-4 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow duration-150"
          />
          <button
            onClick={handleSearch}
            className="h-10 px-6 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 active:bg-blue-800 transition-colors duration-150"
          >
            搜索
          </button>
        </div>

        {/* Filter row */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Category */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs text-gray-500 shrink-0">分类</label>
            <select
              value={categoryId}
              onChange={(e) => updateFilter('categoryId', e.target.value)}
              className="h-9 px-3 text-sm border border-gray-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer hover:border-gray-300 transition-colors duration-150"
            >
              <option value="all">全部分类</option>
              {categoryOptions.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {/* Brand */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs text-gray-500 shrink-0">品牌</label>
            <select
              value={brandId}
              onChange={(e) => updateFilter('brandId', e.target.value)}
              className="h-9 px-3 text-sm border border-gray-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer hover:border-gray-300 transition-colors duration-150"
            >
              <option value="all">全部品牌</option>
              {brands.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>

          {/* Price range */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs text-gray-500 shrink-0">价格</label>
            <input
              type="number"
              min={0}
              placeholder="最低"
              value={minPrice}
              onChange={(e) => updateFilter('minPrice', e.target.value)}
              className="w-20 h-9 px-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
            />
            <span className="text-gray-400">-</span>
            <input
              type="number"
              min={0}
              placeholder="最高"
              value={maxPrice}
              onChange={(e) => updateFilter('maxPrice', e.target.value)}
              className="w-20 h-9 px-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
            />
          </div>

          {/* Sort */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs text-gray-500 shrink-0">排序</label>
            <select
              value={sort}
              onChange={(e) => updateFilter('sort', e.target.value)}
              className="h-9 px-3 text-sm border border-gray-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer hover:border-gray-300 transition-colors duration-150"
            >
              {sortOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Clear filter button */}
          {(hasActiveFilters) && (
            <button
              onClick={() => setSearchParams({})}
              className="h-9 px-3 text-xs text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors duration-150"
            >
              清除筛选
            </button>
          )}
        </div>
      </div>

      {/* ---------- Result summary ---------- */}
      {!loading && !error && (
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm text-gray-500">
            {keyword && (
              <span>
                关键词 &ldquo;<span className="text-gray-700 font-medium">{keyword}</span>&rdquo;
                &nbsp;的搜索结果&nbsp;
              </span>
            )}
            <span>
              共 <strong className="text-gray-700">{total}</strong> 件商品
            </span>
          </p>
        </div>
      )}

      {/* ---------- Loading state ---------- */}
      {loading && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-5">
          {Array.from({ length: 8 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      )}

      {/* ---------- Error state ---------- */}
      {!loading && error && (
        <div className="flex flex-col items-center justify-center py-20">
          <div className="text-5xl text-gray-300 mb-4">!</div>
          <p className="text-gray-500 mb-4">{error}</p>
          <button
            onClick={handleRetry}
            className="px-5 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors"
          >
            重新加载
          </button>
        </div>
      )}

      {/* ---------- Empty state ---------- */}
      {!loading && !error && products.length === 0 && (
        <>
          {keyword ? (
            <Empty
              title="未找到相关商品"
              description="没有找到相关商品，试试其他关键词"
              action={{ text: '清除筛选', onClick: () => setSearchParams({}) }}
            />
          ) : hasActiveFilters ? (
            <Empty
              title="该分类暂无商品"
              description="该分类暂无商品，去看看别的分类"
              action={{ text: '清除筛选', onClick: () => setSearchParams({}) }}
            />
          ) : (
            <Empty
              title="暂无商品"
              description="商品列表暂无数据"
              action={{ text: '刷新', onClick: handleRetry }}
            />
          )}
        </>
      )}

      {/* ---------- Product grid ---------- */}
      {!loading && !error && products.length > 0 && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-5">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>

          <Pagination page={page} totalPages={totalPages} onChange={handlePageChange} />
        </>
      )}
    </div>
  )
}

export default ProductList
