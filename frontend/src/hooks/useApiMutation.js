import { useCallback, useState } from 'react'

/** Tracks one API mutation without hiding its result or error from the caller. */
export function useApiMutation() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const run = useCallback(async (operation) => {
    setLoading(true)
    setError(null)
    try {
      return await operation()
    } catch (caught) {
      setError(caught)
      throw caught
    } finally {
      setLoading(false)
    }
  }, [])

  const clearError = useCallback(() => setError(null), [])
  return { run, loading, error, clearError }
}
