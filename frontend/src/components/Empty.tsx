import { type FC, type ReactNode } from 'react'

interface EmptyProps {
  icon?: ReactNode
  title?: string
  description?: string
  action?: {
    text: string
    onClick: () => void
  }
}

export const Empty: FC<EmptyProps> = ({ icon, title, description, action }) => {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      {icon ? (
        <div className="mb-4 text-gray-300">{icon}</div>
      ) : (
        <div className="mb-4 text-gray-300">
          <svg className="w-16 h-16 mx-auto" fill="none" viewBox="0 0 64 64" stroke="currentColor" strokeWidth={1}>
            <circle cx="32" cy="32" r="24" stroke="currentColor" strokeDasharray="4 3" fill="none" />
            <path strokeLinecap="round" d="M32 22v12" />
            <path strokeLinecap="round" d="M32 40v2" />
          </svg>
        </div>
      )}
      {title && <h3 className="text-lg font-medium text-gray-600 mb-1">{title}</h3>}
      {description && <p className="text-sm text-gray-400 mb-6 text-center max-w-sm">{description}</p>}
      {action && (
        <button
          onClick={action.onClick}
          className="px-5 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors"
        >
          {action.text}
        </button>
      )}
    </div>
  )
}
