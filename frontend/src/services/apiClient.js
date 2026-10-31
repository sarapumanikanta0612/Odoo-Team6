/**
 * Shared REST client for the WAMS backend. Every module should call the API
 * through `api` so authentication and error handling behave the same everywhere.
 *
 * - Paths are relative (`/api/...`). In development Vite proxies them to Spring Boot.
 * - The bearer token from the current session is attached unless `auth: false`.
 * - Non-2xx responses and network failures throw `ApiError`. Its message comes
 *   from the backend's ApiError envelope when one is returned.
 * - A 401 on a request that carried a token ends the session only if the token is
 *   really rejected (checked with GET /api/auth/me). Otherwise it is reported as 403,
 *   because the backend currently answers role-denied requests with 401.
 *
 *   const projects = await api.get('/api/finance/projects', { query: { status: 'PLANNING' }, signal })
 *   await api.post('/api/finance/projects/12/expenses', body)
 */
import { getToken } from '../auth/session'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

const FALLBACK_MESSAGES = {
  400: 'The request was not valid.',
  401: 'Please sign in to continue.',
  403: 'You do not have permission to do this.',
  404: 'The requested record was not found.',
  409: 'This conflicts with an existing record.',
  422: 'This action is not allowed by the business rules.',
  500: 'The server could not complete the request.',
}

const SERVER_UNAVAILABLE = 'Cannot reach the WAMS server. Check that the backend is running.'
// The dev proxy (and most gateways) answer 502-504 when the backend is down.
const GATEWAY_STATUSES = new Set([502, 503, 504])

export class ApiError extends Error {
  constructor({ status, message, details = [], path, body = null }) {
    super(message)
    this.name = 'ApiError'
    /** HTTP status, or 0 when the server could not be reached. */
    this.status = status
    /** Field-level messages from the backend, e.g. "email: must not be blank". */
    this.details = details
    this.path = path
    this.body = body
  }
}

let unauthorizedHandler = null

/** Registered by AuthProvider. Called when the server rejects the session token. */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

function buildUrl(path, query) {
  const url = `${BASE_URL}${path}`
  if (!query) return url
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    const values = Array.isArray(value) ? value : [value]
    values.forEach((item) => params.append(key, String(item)))
  }
  const queryString = params.toString()
  return queryString ? `${url}?${queryString}` : url
}

async function readBody(response) {
  if (response.status === 204) return null
  const text = await response.text()
  if (!text) return null
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('json')) {
    try {
      return JSON.parse(text)
    } catch {
      return text
    }
  }
  return text
}

/**
 * Low-level request. Prefer the `api.get/post/put/patch/delete` helpers.
 *
 * @param {string} path      `/api/...`
 * @param {object} options   { method, body, query, signal, auth = true, headers }
 *                           `body` is sent as JSON unless it is FormData.
 */
export async function apiRequest(path, { method = 'GET', body, query, signal, auth = true, headers = {} } = {}) {
  const token = auth ? getToken() : null
  const init = { method, signal, headers: { Accept: 'application/json', ...headers } }
  if (token) init.headers.Authorization = `Bearer ${token}`
  if (body !== undefined) {
    if (body instanceof FormData) {
      init.body = body
    } else {
      init.headers['Content-Type'] = 'application/json'
      init.body = JSON.stringify(body)
    }
  }

  let response
  try {
    response = await fetch(buildUrl(path, query), init)
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    throw new ApiError({ status: 0, message: SERVER_UNAVAILABLE, path })
  }

  const data = await readBody(response)
  if (response.ok) return data

  const envelope = data && typeof data === 'object' ? data : {}
  const fallback = GATEWAY_STATUSES.has(response.status)
    ? SERVER_UNAVAILABLE
    : FALLBACK_MESSAGES[response.status] || `Request failed (${response.status}).`
  const error = new ApiError({
    status: response.status,
    message: envelope.message || fallback,
    details: Array.isArray(envelope.details) ? envelope.details : [],
    path,
    body: data,
  })

  if (response.status === 401 && token) {
    const accepted = path === SESSION_CHECK_PATH ? false : await tokenStillAccepted(token)
    if (accepted) {
      // The token is valid, so this was an authorization failure the backend reported as 401
      // (role-denied requests currently surface as 401; see GitHub issue #3). Keep the session.
      throw new ApiError({ status: 403, message: FALLBACK_MESSAGES[403], path, body: data })
    }
    if (accepted === false) unauthorizedHandler?.(error)
  }
  throw error
}

const SESSION_CHECK_PATH = '/api/auth/me'

/** true = token accepted, false = token rejected, null = could not tell (server unreachable). */
async function tokenStillAccepted(token) {
  try {
    const check = await fetch(buildUrl(SESSION_CHECK_PATH), {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    })
    if (check.ok) return true
    return check.status === 401 ? false : null
  } catch {
    return null
  }
}

export const api = {
  get: (path, options) => apiRequest(path, { ...options, method: 'GET' }),
  post: (path, body, options) => apiRequest(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => apiRequest(path, { ...options, method: 'PUT', body }),
  patch: (path, body, options) => apiRequest(path, { ...options, method: 'PATCH', body }),
  delete: (path, options) => apiRequest(path, { ...options, method: 'DELETE' }),
}
