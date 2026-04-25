import { create } from 'zustand'
import { getCartList, updateCartItem, deleteCartItem } from '@/api/cart'
import type { CartItemVO } from '@/api/cart'

interface CartState {
  items: CartItemVO[]
  loading: boolean
  error: string | null
  selectedIds: Set<number>
  fetchCart: () => Promise<void>
  toggleSelect: (id: number) => void
  selectAll: (selected: boolean) => void
  updateQuantity: (id: number, quantity: number) => Promise<void>
  removeItem: (id: number) => Promise<void>
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],
  loading: false,
  error: null,
  selectedIds: new Set<number>(),

  fetchCart: async () => {
    set({ loading: true, error: null })
    try {
      const res = await getCartList()
      if (res.code === 200) {
        const items = res.data ?? []
        set({
          items,
          selectedIds: new Set(items.filter((i) => i.selected).map((i) => i.id)),
        })
      } else {
        set({ error: res.message ?? '获取购物车失败' })
      }
    } catch {
      set({ error: '网络错误，请稍后重试' })
    } finally {
      set({ loading: false })
    }
  },

  toggleSelect: (id: number) => {
    set((state) => {
      const next = new Set(state.selectedIds)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return { selectedIds: next }
    })
  },

  selectAll: (selected: boolean) => {
    set((state) => ({
      selectedIds: selected
        ? new Set(state.items.map((i) => i.id))
        : new Set<number>(),
    }))
  },

  updateQuantity: async (id: number, quantity: number) => {
    const prev = get().items.find((i) => i.id === id)
    // Optimistic update
    set((state) => ({
      items: state.items.map((i) => (i.id === id ? { ...i, quantity } : i)),
    }))
    try {
      const res = await updateCartItem(id, quantity)
      if (res.code !== 200) {
        // Rollback
        set((state) => ({
          items: state.items.map((i) =>
            i.id === id ? { ...i, quantity: prev?.quantity ?? quantity } : i,
          ),
        }))
      }
    } catch {
      set((state) => ({
        items: state.items.map((i) =>
          i.id === id ? { ...i, quantity: prev?.quantity ?? quantity } : i,
        ),
      }))
    }
  },

  removeItem: async (id: number) => {
    const prev = get().items
    set((state) => ({
      items: state.items.filter((i) => i.id !== id),
      selectedIds: new Set(
        [...state.selectedIds].filter((sid) => sid !== id),
      ),
    }))
    try {
      const res = await deleteCartItem(id)
      if (res.code !== 200) {
        set({ items: prev })
      }
    } catch {
      set({ items: prev })
    }
  },
}))

// Computed values hook
export const useCartComputed = () => {
  const items = useCartStore((s) => s.items)
  const selectedIds = useCartStore((s) => s.selectedIds)

  const selectedItems = items.filter((i) => selectedIds.has(i.id))
  const totalAmount = selectedItems.reduce(
    (sum, i) => sum + i.price * i.quantity,
    0,
  )
  const selectedCount = selectedItems.reduce((sum, i) => sum + i.quantity, 0)
  const isAllSelected =
    items.length > 0 && selectedIds.size === items.length

  return { selectedItems, totalAmount, selectedCount, isAllSelected }
}
