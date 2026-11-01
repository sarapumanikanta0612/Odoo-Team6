import { Tag } from 'antd'

import { humanizeEnum } from '../utils/format'

const COLORS = {
  ACTIVE: 'green',
  APPROVED: 'green',
  COMPLETED: 'green',
  SIGNED: 'green',
  PAID: 'green',
  IN_PROGRESS: 'blue',
  CONTACTED: 'blue',
  CONSULTATION: 'cyan',
  PLANNING: 'gold',
  PROPOSAL: 'gold',
  PENDING: 'gold',
  PENDING_APPROVAL: 'gold',
  NEW: 'purple',
  INACTIVE: 'default',
  REJECTED: 'red',
  LOST: 'red',
  CANCELLED: 'red',
  VOIDED: 'red',
  REFUNDED: 'red',
  POSTPONED: 'orange',
  CONVERTED: 'green',
}

export default function StatusTag({ value }) {
  const normalized = typeof value === 'boolean' ? (value ? 'ACTIVE' : 'INACTIVE') : value
  return <Tag color={COLORS[normalized] ?? 'default'}>{humanizeEnum(normalized)}</Tag>
}
