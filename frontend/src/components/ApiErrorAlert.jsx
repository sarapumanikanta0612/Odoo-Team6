import { Alert, Button } from 'antd'

function defaultTitle(status) {
  if (status === 0 || status === 502 || status === 503 || status === 504) return 'Server unavailable'
  if (status === 403) return 'Access denied'
  if (status === 404) return 'Not found'
  if (status === 400 || status === 422) return 'Please check the details'
  return 'Something went wrong'
}

/**
 * Shows an ApiError (or any Error) with the backend's message and field details.
 *
 *   <ApiErrorAlert error={error} onRetry={reload} />
 */
export default function ApiErrorAlert({ error, title, onRetry, className, style }) {
  if (!error) return null
  const details = Array.isArray(error.details) ? error.details : []

  return (
    <Alert
      type="error"
      showIcon
      className={className}
      style={style}
      title={title ?? defaultTitle(error.status)}
      description={
        <>
          {error.message || 'The request failed.'}
          {details.length > 0 && (
            <ul className="wams-error-details">
              {details.map((detail) => (
                <li key={detail}>{detail}</li>
              ))}
            </ul>
          )}
        </>
      }
      action={
        onRetry ? (
          <Button size="small" onClick={onRetry}>
            Retry
          </Button>
        ) : undefined
      }
    />
  )
}
