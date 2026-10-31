import { Button, Result } from 'antd'
import { isRouteErrorResponse, useRouteError } from 'react-router'
import NotFoundPage from './NotFoundPage'

/** Shown when a page throws while rendering or loading. */
export default function RouteErrorPage({ embedded = false }) {
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage embedded={embedded} />

  if (import.meta.env.DEV) console.error(error)

  const result = (
    <Result
      status="error"
      title="This page could not be displayed"
      subTitle="An unexpected error occurred. Reload the page and try again."
      extra={
        <Button type="primary" onClick={() => window.location.reload()}>
          Reload page
        </Button>
      }
    />
  )
  return embedded ? result : <main className="wams-centered">{result}</main>
}
