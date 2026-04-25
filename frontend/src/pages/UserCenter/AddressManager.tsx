import { useEffect, useState, type FC } from 'react'
import { Loading } from '@/components/Loading'
import { Empty } from '@/components/Empty'
import {
  getAddressList,
  createAddress,
  updateAddress,
  setDefaultAddress,
  deleteAddress,
  type AddressVO,
  type AddressForm,
} from '@/api/address'

const emptyForm: AddressForm = {
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
}

const AddressManager: FC = () => {
  const [addresses, setAddresses] = useState<AddressVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<AddressForm>(emptyForm)
  const [formErrors, setFormErrors] = useState<Partial<Record<keyof AddressForm, string>>>({})
  const [saving, setSaving] = useState(false)
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)

  const fetchAddresses = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getAddressList()
      if (res.code === 200) {
        setAddresses(res.data ?? [])
      } else {
        setError(res.message ?? '获取地址失败')
      }
    } catch {
      setError('网络错误，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchAddresses()
  }, [])

  const openCreate = () => {
    setForm(emptyForm)
    setEditingId(null)
    setFormErrors({})
    setShowForm(true)
  }

  const openEdit = (addr: AddressVO) => {
    setForm({
      receiverName: addr.receiverName,
      receiverPhone: addr.receiverPhone,
      province: addr.province,
      city: addr.city,
      district: addr.district,
      detail: addr.detail,
    })
    setEditingId(addr.id)
    setFormErrors({})
    setShowForm(true)
  }

  const validate = (): boolean => {
    const errors: Partial<Record<keyof AddressForm, string>> = {}
    if (!form.receiverName.trim()) errors.receiverName = '请输入收货人姓名'
    if (!/^1\d{10}$/.test(form.receiverPhone)) errors.receiverPhone = '请输入正确的手机号'
    if (!form.province.trim()) errors.province = '请输入省份'
    if (!form.city.trim()) errors.city = '请输入城市'
    if (!form.district.trim()) errors.district = '请输入区/县'
    if (!form.detail.trim()) errors.detail = '请输入详细地址'
    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSave = async () => {
    if (!validate()) return
    setSaving(true)
    try {
      const res = editingId
        ? await updateAddress(editingId, form)
        : await createAddress(form)
      if (res.code === 200) {
        setShowForm(false)
        await fetchAddresses()
      } else {
        setFormErrors({ receiverName: res.message ?? '保存失败' })
      }
    } catch {
      setFormErrors({ receiverName: '网络错误' })
    } finally {
      setSaving(false)
    }
  }

  const handleSetDefault = async (id: number) => {
    try {
      await setDefaultAddress(id)
      setAddresses((prev) =>
        prev.map((a) => ({ ...a, isDefault: a.id === id })),
      )
    } catch {
      // silent
    }
  }

  const handleDelete = async (id: number) => {
    setDeleting(true)
    try {
      const res = await deleteAddress(id)
      if (res.code === 200) {
        setAddresses((prev) => prev.filter((a) => a.id !== id))
      }
    } catch {
      // silent
    } finally {
      setDeleting(false)
      setDeleteConfirmId(null)
    }
  }

  if (loading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 2 }).map((_, i) => (
          <div key={i} className="h-28 bg-gray-100 rounded-lg animate-pulse" />
        ))}
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-500 mb-4">{error}</p>
        <button
          onClick={fetchAddresses}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          重新加载
        </button>
      </div>
    )
  }

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-gray-900">收货地址</h2>
        <button
          onClick={openCreate}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700"
        >
          添加地址
        </button>
      </div>

      {/* Address list */}
      {addresses.length === 0 ? (
        <Empty title="暂无收货地址" description="点击上方按钮添加" />
      ) : (
        <div className="space-y-3">
          {addresses.map((addr) => (
            <div
              key={addr.id}
              className="bg-white rounded-lg border border-gray-200 p-4"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-sm font-medium text-gray-900">
                      {addr.receiverName}
                    </span>
                    <span className="text-sm text-gray-400">
                      {addr.receiverPhone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')}
                    </span>
                    {addr.isDefault && (
                      <span className="inline-block text-xs text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                        默认
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-500">
                    {addr.province} {addr.city} {addr.district} {addr.detail}
                  </p>
                </div>
                <div className="flex items-center gap-2 ml-4 shrink-0">
                  {!addr.isDefault && (
                    <button
                      onClick={() => handleSetDefault(addr.id)}
                      className="text-xs text-blue-600 hover:text-blue-700"
                    >
                      设为默认
                    </button>
                  )}
                  <button
                    onClick={() => openEdit(addr)}
                    className="text-xs text-gray-500 hover:text-gray-700"
                  >
                    编辑
                  </button>
                  {deleteConfirmId === addr.id ? (
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => handleDelete(addr.id)}
                        disabled={deleting}
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
                      onClick={() => setDeleteConfirmId(addr.id)}
                      className="text-xs text-gray-400 hover:text-red-500"
                    >
                      删除
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit form dialog */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              {editingId ? '编辑地址' : '新增地址'}
            </h3>
            <div className="space-y-4">
              {/* Receiver name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  收货人
                </label>
                <input
                  value={form.receiverName}
                  onChange={(e) =>
                    setForm({ ...form, receiverName: e.target.value })
                  }
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="请输入收货人姓名"
                />
                {formErrors.receiverName && (
                  <p className="text-xs text-red-500 mt-1">{formErrors.receiverName}</p>
                )}
              </div>

              {/* Phone */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  手机号
                </label>
                <input
                  value={form.receiverPhone}
                  onChange={(e) =>
                    setForm({ ...form, receiverPhone: e.target.value })
                  }
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="请输入11位手机号"
                  maxLength={11}
                />
                {formErrors.receiverPhone && (
                  <p className="text-xs text-red-500 mt-1">{formErrors.receiverPhone}</p>
                )}
              </div>

              {/* Province / City / District */}
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    省
                  </label>
                  <input
                    value={form.province}
                    onChange={(e) =>
                      setForm({ ...form, province: e.target.value })
                    }
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="省份"
                  />
                  {formErrors.province && (
                    <p className="text-xs text-red-500 mt-1">{formErrors.province}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    市
                  </label>
                  <input
                    value={form.city}
                    onChange={(e) =>
                      setForm({ ...form, city: e.target.value })
                    }
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="城市"
                  />
                  {formErrors.city && (
                    <p className="text-xs text-red-500 mt-1">{formErrors.city}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    区
                  </label>
                  <input
                    value={form.district}
                    onChange={(e) =>
                      setForm({ ...form, district: e.target.value })
                    }
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="区/县"
                  />
                  {formErrors.district && (
                    <p className="text-xs text-red-500 mt-1">{formErrors.district}</p>
                  )}
                </div>
              </div>

              {/* Detail */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  详细地址
                </label>
                <input
                  value={form.detail}
                  onChange={(e) =>
                    setForm({ ...form, detail: e.target.value })
                  }
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="街道、门牌号等"
                />
                {formErrors.detail && (
                  <p className="text-xs text-red-500 mt-1">{formErrors.detail}</p>
                )}
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setShowForm(false)}
                className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800"
              >
                取消
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 disabled:bg-blue-300"
              >
                {saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AddressManager
