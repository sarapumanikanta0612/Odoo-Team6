import { Spin } from 'antd'

export default function FullPageSpinner({ label = 'Loading' }) {
  return (
    <div className="wams-centered" role="status" aria-live="polite">
      <Spin size="large" />
      <span className="wams-centered__label">{label}</span>
    </div>
  )
}
