import { Button, Result } from 'antd'
import { useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthContext'

/** `embedded` renders inside the signed-in shell instead of as a full page. */
export default function NotFoundPage({ embedded = false }) {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const target = isAuthenticated ? '/app' : '/'

  const result = (
    <Result
      status="404"
      title="Page not found"
      subTitle="This address does not match any WAMS page."
      extra={
        <Button type="primary" onClick={() => navigate(target)}>
          {isAuthenticated ? 'Back to workspace' : 'Back to home'}
        </Button>
      }
    />
  )

  if (embedded) return result
  return (
    <main className="wams-centered">
      <title>Page not found · WAMS</title>
      {result}
    </main>
  )
}
