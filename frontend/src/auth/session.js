/**
 * Browser storage for the signed-in session.
 *
 * sessionStorage keeps the token for the current tab only and clears it when the
 * tab closes. The backend issues stateless JWTs, so signing out only removes the
 * token here; it stays valid on the server until it expires.
 */
const STORAGE_KEY = 'wams.session'

// Sign out slightly before the server rejects the token.
const EXPIRY_SKEW_MS = 30_000

function storage() {
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

export function clearSession() {
  storage()?.removeItem(STORAGE_KEY)
}

/** Returns `{ token, expiresAt, user }`, or null when missing, malformed or expired. */
export function loadSession() {
  const raw = storage()?.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const session = JSON.parse(raw)
    const valid =
      typeof session?.token === 'string' &&
      typeof session?.expiresAt === 'number' &&
      session.expiresAt > Date.now() &&
      session.user &&
      typeof session.user.role === 'string'
    if (valid) return session
  } catch {
    // Fall through and discard unreadable data.
  }
  clearSession()
  return null
}

export function saveSession(session) {
  storage()?.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function getToken() {
  return loadSession()?.token ?? null
}

/** Converts the backend's AuthResponse into the stored session shape. */
export function sessionFromAuthResponse(auth) {
  if (!auth || typeof auth.token !== 'string' || !auth.user) {
    throw new Error('The server returned an unexpected sign-in response.')
  }
  const lifetimeMs = Math.max(Number(auth.expiresInSeconds) * 1000 - EXPIRY_SKEW_MS, 0)
  return {
    token: auth.token,
    expiresAt: Date.now() + lifetimeMs,
    user: auth.user,
  }
}
