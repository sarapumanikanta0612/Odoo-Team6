import dayjs from 'dayjs'

/** Placeholder shown when a value is missing. */
export const EMPTY_VALUE = '—'

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const inrWhole = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
})

/** 1234567.5 -> "₹12,34,567.50" (Indian digit grouping). `whole: true` drops paise. */
export function formatINR(value, { whole = false } = {}) {
  if (value === null || value === undefined || value === '') return EMPTY_VALUE
  const number = Number(value)
  if (!Number.isFinite(number)) return EMPTY_VALUE
  return (whole ? inrWhole : inr).format(number)
}

/** "2026-12-04" -> "04 Dec 2026" */
export function formatDate(value) {
  if (!value) return EMPTY_VALUE
  const date = dayjs(value)
  return date.isValid() ? date.format('DD MMM YYYY') : EMPTY_VALUE
}

/** ISO timestamp -> "04 Dec 2026, 18:30" in the browser's time zone. */
export function formatDateTime(value) {
  if (!value) return EMPTY_VALUE
  const date = dayjs(value)
  return date.isValid() ? date.format('DD MMM YYYY, HH:mm') : EMPTY_VALUE
}
