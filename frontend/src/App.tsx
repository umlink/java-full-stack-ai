import { ErrorBoundary } from '@/components/ErrorBoundary'
import { AppRouterProvider } from '@/router'
import { Loading } from '@/components/Loading'
import { useEffect } from 'react'
import { useAuthStore } from '@/stores/authStore'

function App() {
  const fetchProfile = useAuthStore((s) => s.fetchProfile)
  const token = useAuthStore((s) => s.token)
  const isInitialized = useAuthStore((s) => s.isInitialized)

  useEffect(() => {
    if (token) {
      fetchProfile()
    } else {
      useAuthStore.setState({ isInitialized: true })
    }
  }, [])

  if (!isInitialized) {
    return <Loading size="lg" text="加载中..." />
  }

  return (
    <ErrorBoundary>
      <AppRouterProvider />
    </ErrorBoundary>
  )
}

export default App
