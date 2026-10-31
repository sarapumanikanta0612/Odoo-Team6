import { Button, Result } from 'antd'
import { Outlet, useNavigate } from 'react-router'
import { useAuth } from './AuthContext'
import { roleLabel } from './roles'

/**
 * Hides a page from roles that cannot use it. This is a UX guard only: the
 * backend must still reject the API calls for those roles.
 */
export default function RequireRole({ roles, children }) {
  const { user, hasRole } = useAuth()
  const navigate = useNavigate()

  if (roles?.length && !hasRole(roles)) {
    return (
      <Result
        status="403"
        title="Access denied"
        subTitle={`Your role (${roleLabel(user?.role)}) cannot open this page.`}
        extra={
          <Button type="primary" onClick={() => navigate('/app')}>
            Back to workspace
          </Button>
        }
      />
    )
  }
  return children ?? <Outlet />
}
