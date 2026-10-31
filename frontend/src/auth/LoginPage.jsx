import { ArrowLeftOutlined, LockOutlined, MailOutlined } from '@ant-design/icons'
import { Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import ApiErrorAlert from '../components/ApiErrorAlert'
import FullPageSpinner from '../components/FullPageSpinner'
import { GopuramIllustration, TempleMark } from '../components/TempleArt'
import { useAuth } from './AuthContext'
import './login.css'

/** Only return to pages inside the signed-in app after login. */
function returnPath(from) {
  const pathname = from?.pathname
  if (typeof pathname === 'string' && (pathname === '/app' || pathname.startsWith('/app/'))) {
    return `${pathname}${from.search ?? ''}`
  }
  return '/app'
}

function signInError(error) {
  if (error?.status === 401) {
    return {
      ...error,
      message: 'The email or password is incorrect, or the account is disabled.',
      details: [],
    }
  }
  return error
}

export default function LoginPage() {
  const { status, login } = useAuth()
  const location = useLocation()
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (status === 'checking') return <FullPageSpinner label="Checking your session" />
  // After a successful login the status changes and this redirect takes over.
  if (status === 'authenticated') return <Navigate to={returnPath(location.state?.from)} replace />

  const onFinish = async ({ email, password }) => {
    setSubmitting(true)
    setError(null)
    try {
      await login(email.trim(), password)
    } catch (caught) {
      setError(signInError(caught))
      setSubmitting(false)
    }
  }

  return (
    <div className="wams-login">
      <title>Sign in · WAMS</title>

      <aside className="wams-login__panel">
        <Link to="/" className="wams-login__brand">
          <TempleMark size={34} />
          <span>WAMS</span>
        </Link>
        <GopuramIllustration className="wams-login__art" />
        <div className="wams-login__panel-copy">
          <h2>One workspace for the whole agency.</h2>
          <p>Staff, clients and vendors sign in here. Each account opens only the weddings and records its role allows.</p>
        </div>
      </aside>

      <main className="wams-login__main">
        <div className="wams-login__card">
          <div className="wams-eyebrow">Wedding Agency Management System</div>
          <h1 className="wams-login__title">Sign in</h1>
          <p className="wams-login__hint">Use the email address registered with your agency.</p>

          {error && <ApiErrorAlert error={error} title="Sign-in failed" className="wams-login__error" />}

          <Form layout="vertical" requiredMark={false} onFinish={onFinish} disabled={submitting} size="large">
            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: 'Enter your email address.' },
                { type: 'email', message: 'Enter a valid email address.' },
              ]}
            >
              <Input prefix={<MailOutlined />} type="email" autoComplete="username" autoFocus />
            </Form.Item>
            <Form.Item label="Password" name="password" rules={[{ required: true, message: 'Enter your password.' }]}>
              <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={submitting}>
              Sign in
            </Button>
          </Form>

          <Link to="/" className="wams-login__back">
            <ArrowLeftOutlined /> Back to overview
          </Link>
        </div>
      </main>
    </div>
  )
}
