import { Navigate, Outlet, useLocation } from 'react-router'
import FullPageSpinner from '../components/FullPageSpinner'
import { useAuth } from './AuthContext'

/** Sends anonymous visitors to /login and returns them to this page after sign-in. */
export default function RequireAuth({ children }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'checking') return <FullPageSpinner label="Checking your session" />
  if (status !== 'authenticated') return <Navigate to="/login" replace state={{ from: location }} />
  return children ?? <Outlet />
}
