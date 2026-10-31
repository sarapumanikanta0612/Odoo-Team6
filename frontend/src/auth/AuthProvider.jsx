import { App } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, setUnauthorizedHandler } from '../services/apiClient'
import { AuthContext } from './AuthContext'
import { hasAnyRole } from './roles'
import { clearSession, loadSession, saveSession, sessionFromAuthResponse } from './session'

// setTimeout stores delays as a 32-bit integer.
const MAX_TIMER_MS = 2 ** 31 - 1

/**
 * Owns the signed-in session. Uses DEV 1's endpoints:
 *   POST /api/auth/login  -> { token, tokenType, expiresInSeconds, user }
 *   GET  /api/auth/me     -> user (validates a stored token on page load)
 */
export default function AuthProvider({ children }) {
  const { message } = App.useApp()
  const [session, setSession] = useState(() => loadSession())
  const [status, setStatus] = useState(() => (loadSession() ? 'checking' : 'anonymous'))

  const endSession = useCallback(
    (notice) => {
      clearSession()
      setSession(null)
      setStatus('anonymous')
      if (notice) message.warning(notice)
    },
    [message],
  )

  // A 401 on any authenticated request means the server no longer accepts the token.
  useEffect(() => {
    setUnauthorizedHandler(() => endSession('Your session has ended. Please sign in again.'))
    return () => setUnauthorizedHandler(null)
  }, [endSession])

  // Validate a stored token once and refresh the user record from the server.
  useEffect(() => {
    if (status !== 'checking') return undefined
    const controller = new AbortController()
    api
      .get('/api/auth/me', { signal: controller.signal })
      .then((user) => {
        setSession((current) => {
          if (!current) return current
          const next = { ...current, user }
          saveSession(next)
          return next
        })
        setStatus('authenticated')
      })
      .catch((error) => {
        if (controller.signal.aborted || error?.status === 401) return // 401 handled by endSession
        // Server unreachable: keep the stored session. Each API call reports its own error.
        setStatus('authenticated')
      })
    return () => controller.abort()
  }, [status])

  // Sign out when the token expires.
  useEffect(() => {
    if (!session) return undefined
    const remaining = Math.min(Math.max(session.expiresAt - Date.now(), 0), MAX_TIMER_MS)
    const timer = window.setTimeout(() => endSession('Your session has expired. Please sign in again.'), remaining)
    return () => window.clearTimeout(timer)
  }, [session, endSession])

  const login = useCallback(async (email, password) => {
    const auth = await api.post('/api/auth/login', { email, password }, { auth: false })
    const next = sessionFromAuthResponse(auth)
    saveSession(next)
    setSession(next)
    setStatus('authenticated')
    return next.user
  }, [])

  const logout = useCallback(() => endSession(), [endSession])

  const value = useMemo(() => {
    const user = session?.user ?? null
    return {
      status,
      user,
      isAuthenticated: status === 'authenticated' && Boolean(user),
      hasRole: (...roles) => hasAnyRole(user, roles.flat()),
      login,
      logout,
    }
  }, [status, session, login, logout])

  return <AuthContext value={value}>{children}</AuthContext>
}
