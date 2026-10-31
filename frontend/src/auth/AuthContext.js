import { createContext, useContext } from 'react'

export const AuthContext = createContext(null)

/**
 * Current authentication state:
 * {
 *   status: 'checking' | 'authenticated' | 'anonymous',
 *   user: UserResponse | null,          // { id, fullName, email, role, active, ... }
 *   isAuthenticated: boolean,
 *   hasRole(...roles): boolean,         // UX only; the backend enforces access
 *   login(email, password): Promise<UserResponse>,
 *   logout(): void,
 * }
 */
export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth() must be used inside <AuthProvider>.')
  return value
}
