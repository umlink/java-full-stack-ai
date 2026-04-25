import { type FC } from 'react'

interface LoadingProps {
  size?: 'sm' | 'md' | 'lg'
  text?: string
}

const sizeClasses = {
  sm: 'h-6 w-6 border-2',
  md: 'h-10 w-10 border-3',
  lg: 'h-14 w-14 border-4',
}

export const Loading: FC<LoadingProps> = ({ size = 'md', text }) => {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div
        className={`animate-spin rounded-full border-gray-300 border-t-blue-600 ${sizeClasses[size]}`}
      />
      {text && <p className="mt-4 text-sm text-gray-500">{text}</p>}
    </div>
  )
}
