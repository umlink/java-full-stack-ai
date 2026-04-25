import { useState, useEffect, useCallback } from 'react'

interface UseCountdownReturn {
  seconds: number
  formatted: string
  isEnded: boolean
}

export function useCountdown(targetTime: string | number | Date, onEnd?: () => void): UseCountdownReturn {
  const target = new Date(targetTime).getTime()

  const calcRemaining = useCallback(() => {
    return Math.max(0, Math.floor((target - Date.now()) / 1000))
  }, [target])

  const [seconds, setSeconds] = useState(calcRemaining)

  useEffect(() => {
    setSeconds(calcRemaining)

    const timer = setInterval(() => {
      const remaining = calcRemaining()
      setSeconds(remaining)
      if (remaining <= 0) {
        clearInterval(timer)
        onEnd?.()
      }
    }, 1000)

    return () => clearInterval(timer)
  }, [target, calcRemaining, onEnd])

  const format = (totalSec: number): string => {
    if (totalSec <= 0) return '00:00:00'
    const h = Math.floor(totalSec / 3600)
    const m = Math.floor((totalSec % 3600) / 60)
    const s = totalSec % 60
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  return {
    seconds,
    formatted: format(seconds),
    isEnded: seconds <= 0,
  }
}
