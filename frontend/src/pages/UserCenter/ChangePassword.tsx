import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { changePasswordApi } from '@/api/auth'
import { showToast } from '@/utils/toast'
import { Eye, EyeOff } from 'lucide-react'

const passwordSchema = z
  .object({
    oldPassword: z.string().min(1, '请输入旧密码'),
    newPassword: z
      .string()
      .min(8, '密码至少 8 位')
      .regex(/[a-zA-Z]/, '密码必须包含字母')
      .regex(/[0-9]/, '密码必须包含数字'),
    confirmPassword: z.string().min(1, '请确认新密码'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: '两次输入的密码不一致',
    path: ['confirmPassword'],
  })

type PasswordForm = z.infer<typeof passwordSchema>

export default function ChangePasswordPage() {
  const [submitting, setSubmitting] = useState(false)
  const [showOld, setShowOld] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  })

  const onSubmit = async (data: PasswordForm) => {
    // 前端提前校验新旧密码是否相同
    if (data.oldPassword === data.newPassword) {
      showToast('新密码不能与旧密码相同')
      return
    }

    setSubmitting(true)
    try {
      await changePasswordApi({
        oldPassword: data.oldPassword,
        newPassword: data.newPassword,
      })
      showToast('密码修改成功')
      reset()
    } catch (err: any) {
      // 根据后端返回的错误信息提示
      const status = err?.response?.status
      if (status === 400) {
        showToast('旧密码错误')
      } else {
        showToast(err?.response?.data?.message || '密码修改失败')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-6">修改密码</h2>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 max-w-md">
        {/* Old password */}
        <div>
          <label htmlFor="oldPassword" className="block text-sm font-medium text-gray-700 mb-1.5">
            旧密码
          </label>
          <div className="relative">
            <input
              id="oldPassword"
              type={showOld ? 'text' : 'password'}
              autoComplete="current-password"
              {...register('oldPassword')}
              placeholder="请输入旧密码"
              className={`w-full px-3 py-2.5 pr-10 border rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder:text-gray-400 ${
                errors.oldPassword ? 'border-red-300' : 'border-gray-300'
              }`}
            />
            <button
              type="button"
              onClick={() => setShowOld((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
              tabIndex={-1}
              aria-label={showOld ? '隐藏密码' : '显示密码'}
            >
              {showOld ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          {errors.oldPassword && (
            <p className="mt-1 text-xs text-red-500">{errors.oldPassword.message}</p>
          )}
        </div>

        {/* New password */}
        <div>
          <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 mb-1.5">
            新密码
          </label>
          <div className="relative">
            <input
              id="newPassword"
              type={showNew ? 'text' : 'password'}
              autoComplete="new-password"
              {...register('newPassword')}
              placeholder="请输入新密码（至少 8 位，含字母和数字）"
              className={`w-full px-3 py-2.5 pr-10 border rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder:text-gray-400 ${
                errors.newPassword ? 'border-red-300' : 'border-gray-300'
              }`}
            />
            <button
              type="button"
              onClick={() => setShowNew((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
              tabIndex={-1}
              aria-label={showNew ? '隐藏密码' : '显示密码'}
            >
              {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          {errors.newPassword && (
            <p className="mt-1 text-xs text-red-500">{errors.newPassword.message}</p>
          )}
          <p className="mt-1 text-xs text-gray-400">至少 8 位，需包含字母和数字</p>
        </div>

        {/* Confirm new password */}
        <div>
          <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1.5">
            确认新密码
          </label>
          <div className="relative">
            <input
              id="confirmPassword"
              type={showConfirm ? 'text' : 'password'}
              autoComplete="new-password"
              {...register('confirmPassword')}
              placeholder="请再次输入新密码"
              className={`w-full px-3 py-2.5 pr-10 border rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder:text-gray-400 ${
                errors.confirmPassword ? 'border-red-300' : 'border-gray-300'
              }`}
            />
            <button
              type="button"
              onClick={() => setShowConfirm((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
              tabIndex={-1}
              aria-label={showConfirm ? '隐藏密码' : '显示密码'}
            >
              {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          {errors.confirmPassword && (
            <p className="mt-1 text-xs text-red-500">{errors.confirmPassword.message}</p>
          )}
        </div>

        {/* Submit */}
        <div className="pt-2">
          <button
            type="submit"
            disabled={submitting}
            className="px-6 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {submitting ? (
              <span className="flex items-center gap-2">
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                提交中...
              </span>
            ) : (
              '确认修改'
            )}
          </button>
        </div>
      </form>
    </div>
  )
}
