import { useState, useRef, useEffect } from 'react'
import { useAuthStore } from '@/stores/authStore'
import { updateProfileApi } from '@/api/auth'
import { getUploadToken } from '@/api/upload'
import { showToast } from '@/utils/toast'
import { Loading } from '@/components/Loading'

export default function ProfilePage() {
  const { user, fetchProfile } = useAuthStore()

  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [avatar, setAvatar] = useState('')
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [loading, setLoading] = useState(true)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const hasChanges =
    phone !== (user?.phone ?? '') ||
    email !== (user?.email ?? '') ||
    avatar !== (user?.avatar ?? '')

  useEffect(() => {
    if (user) {
      setPhone(user.phone ?? '')
      setEmail(user.email ?? '')
      setAvatar(user.avatar ?? '')
      setLoading(false)
    } else {
      // 如果 user 为 null，尝试拉取 profile
      fetchProfile().finally(() => setLoading(false))
    }
  }, [user, fetchProfile])

  /** 将文件上传到七牛云，返回可公开访问的 URL */
  const uploadToQiniu = async (file: File): Promise<string> => {
    setUploading(true)
    try {
      // 1. 获取上传凭证
      const tokenRes = await getUploadToken('avatar')
      const { token, key, domain } = tokenRes.data

      // 2. 构造 FormData 直传七牛云
      const formData = new FormData()
      formData.append('token', token)
      formData.append('key', key)
      formData.append('file', file)

      const uploadRes = await fetch('https://upload.qiniup.com', {
        method: 'POST',
        body: formData,
      })

      if (!uploadRes.ok) {
        const errText = await uploadRes.text()
        throw new Error(errText || '上传失败')
      }

      // 3. 返回完整 URL
      return `${domain}/${key}`
    } finally {
      setUploading(false)
    }
  }

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    // 简单校验文件类型和大小
    if (!file.type.startsWith('image/')) {
      showToast('请选择图片文件')
      return
    }
    if (file.size > 2 * 1024 * 1024) {
      showToast('图片大小不能超过 2MB')
      return
    }

    try {
      const url = await uploadToQiniu(file)
      setAvatar(url)
      showToast('头像上传成功')
    } catch {
      showToast('头像上传失败')
    } finally {
      // 重置 file input 以便重复选择同一文件
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const handleSave = async () => {
    setSaving(true)
    try {
      await updateProfileApi({ phone, email, avatar })
      await fetchProfile()
      showToast('保存成功')
    } catch {
      showToast('保存失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <Loading text="加载中..." />
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-6">个人信息</h2>

      <div className="space-y-6 max-w-md">
        {/* Avatar */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">头像</label>
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
            className="group relative inline-block"
          >
            {avatar ? (
              <img
                src={avatar}
                alt="头像"
                className="h-20 w-20 rounded-full object-cover border-2 border-gray-200 group-hover:border-blue-400 transition-colors"
              />
            ) : (
              <div className="h-20 w-20 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-2xl font-semibold border-2 border-gray-200 group-hover:border-blue-400 transition-colors">
                {user?.username?.charAt(0).toUpperCase()}
              </div>
            )}
            {/* Upload overlay */}
            <div className="absolute inset-0 rounded-full bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
              <span className="text-white text-xs font-medium">
                {uploading ? '上传中...' : '更换'}
              </span>
            </div>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleAvatarChange}
            className="hidden"
          />
          <p className="mt-1 text-xs text-gray-400">支持 JPG/PNG，不超过 2MB</p>
        </div>

        {/* Username (readonly) */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">昵称</label>
          <input
            type="text"
            value={user?.username ?? ''}
            readOnly
            className="w-full px-3 py-2.5 border border-gray-200 rounded-lg text-sm bg-gray-50 text-gray-500 cursor-not-allowed"
          />
          <p className="mt-1 text-xs text-gray-400">昵称为注册用户名，暂不支持修改</p>
        </div>

        {/* Phone */}
        <div>
          <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1.5">
            手机号
          </label>
          <input
            id="phone"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="请输入手机号"
            maxLength={11}
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder:text-gray-400"
          />
        </div>

        {/* Email */}
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1.5">
            邮箱
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="请输入邮箱地址"
            className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder:text-gray-400"
          />
        </div>

        {/* Save button */}
        <div className="pt-2">
          <button
            type="button"
            onClick={handleSave}
            disabled={saving || !hasChanges}
            className="px-6 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? (
              <span className="flex items-center gap-2">
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                保存中...
              </span>
            ) : (
              '保存'
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
