import { useCallback, useEffect, useState } from 'react'

/**
 * Loads data and tracks loading and error state. The request is aborted when
 * `deps` change or the component unmounts.
 *
 *   const { data, loading, error, reload } = useApiQuery(
 *     (signal) => api.get(`/api/finance/projects/${projectId}/summary`, { signal }),
 *     [projectId],
 *   )
 *
 * `error` is an ApiError (see services/apiClient.js); render it with <ApiErrorAlert />.
 */
export function useApiQuery(fetcher, deps = []) {
  const [state, setState] = useState({ data: undefined, loading: true, error: null })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setState((previous) => ({ ...previous, loading: true, error: null }))
    Promise.resolve()
      .then(() => fetcher(controller.signal))
      .then((data) => {
        if (!controller.signal.aborted) setState({ data, loading: false, error: null })
      })
      .catch((error) => {
        if (controller.signal.aborted || error?.name === 'AbortError') return
        setState((previous) => ({ ...previous, loading: false, error }))
      })
    return () => controller.abort()
    // The caller decides when to re-fetch through `deps`; `fetcher` is usually an inline function.
    // oxlint-disable-next-line react/exhaustive-deps
  }, [...deps, version])

  const reload = useCallback(() => setVersion((current) => current + 1), [])
  return { ...state, reload }
}
