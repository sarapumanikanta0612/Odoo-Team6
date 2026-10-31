/**
 * Role names as issued by the backend (com.wams.enums.Role).
 *
 * Frontend role checks only decide what to show. The backend must authorize
 * every request; never rely on these checks for security.
 */
export const ROLES = Object.freeze({
  ADMIN: 'ADMIN',
  LEAD_PLANNER: 'LEAD_PLANNER',
  EMPLOYEE: 'EMPLOYEE',
  PRIMARY_CLIENT: 'PRIMARY_CLIENT',
  PARTNER_CLIENT: 'PARTNER_CLIENT',
  VENDOR: 'VENDOR',
})

export const STAFF_ROLES = Object.freeze([ROLES.ADMIN, ROLES.LEAD_PLANNER, ROLES.EMPLOYEE])
export const CLIENT_ROLES = Object.freeze([ROLES.PRIMARY_CLIENT, ROLES.PARTNER_CLIENT])

const ROLE_LABELS = Object.freeze({
  [ROLES.ADMIN]: 'Admin',
  [ROLES.LEAD_PLANNER]: 'Lead planner',
  [ROLES.EMPLOYEE]: 'Employee',
  [ROLES.PRIMARY_CLIENT]: 'Client',
  [ROLES.PARTNER_CLIENT]: 'Partner client',
  [ROLES.VENDOR]: 'Vendor',
})

export function roleLabel(role) {
  return ROLE_LABELS[role] ?? role ?? 'Unknown role'
}

/**
 * True when `user` has one of `roles`. An empty or missing list means
 * "any signed-in user".
 */
export function hasAnyRole(user, roles) {
  if (!user) return false
  if (!roles || roles.length === 0) return true
  return roles.includes(user.role)
}
