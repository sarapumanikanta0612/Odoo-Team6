/**
 * Module registry: the one place where feature modules plug into the signed-in app.
 *
 * Each module lives in src/modules/<name>/ and default-exports a descriptor from
 * src/modules/<name>/index.jsx:
 *
 *   export default {
 *     key: 'finance',                            // unique id
 *     label: 'Finance',                          // sidebar and workspace label
 *     description: 'Budgets, payments, expenses and approvals.',
 *     icon: <WalletOutlined />,                  // @ant-design/icons element
 *     section: SECTIONS.FINANCE,                 // sidebar group
 *     path: 'finance',                           // URL segment under /app
 *     roles: [ROLES.ADMIN, ROLES.LEAD_PLANNER],  // who sees it (UX only; backend enforces)
 *     order: 10,                                 // optional, sorts within the section
 *     routes: [                                  // react-router routes under /app/<path>
 *       { index: true, element: <FinanceOverviewPage /> },
 *       { path: ':projectId', element: <ProjectFinancePage /> },
 *     ],
 *   }
 *
 * To register a module, import its descriptor below and add it to `modules`.
 * Only register modules whose pages call real APIs.
 */
import clientsModule from '../modules/clients'
import employeesModule from '../modules/employees'
import enquiriesModule from '../modules/enquiries'
import projectsModule from '../modules/projects'
import quotationsModule from '../modules/quotations'
import tasksModule from '../modules/tasks'
import usersModule from '../modules/users'
import vendorsModule from '../modules/vendors'
import { hasAnyRole } from '../auth/roles'
import { SECTIONS, SECTION_ORDER } from './moduleSections'

export { SECTIONS } from './moduleSections'

export const modules = [
  usersModule,
  employeesModule,
  enquiriesModule,
  projectsModule,
  quotationsModule,
  tasksModule,
  clientsModule,
  vendorsModule,
]

/** Modules the user may see, sorted by section and order. */
export function modulesForUser(user) {
  return modules
    .filter((module) => hasAnyRole(user, module.roles))
    .toSorted(
      (a, b) =>
        sectionIndex(a.section) - sectionIndex(b.section) ||
        (a.order ?? 100) - (b.order ?? 100) ||
        a.label.localeCompare(b.label),
    )
}

/** Visible modules grouped for the sidebar: [{ section, modules: [...] }]. */
export function groupBySection(visibleModules) {
  return SECTION_ORDER.map((section) => ({
    section,
    modules: visibleModules.filter((module) => normalizeSection(module.section) === section),
  })).filter((group) => group.modules.length > 0)
}

function normalizeSection(section) {
  return SECTION_ORDER.includes(section) ? section : SECTIONS.OTHER
}

function sectionIndex(section) {
  return SECTION_ORDER.indexOf(normalizeSection(section))
}

// Catch registration mistakes early in development.
if (import.meta.env.DEV) {
  const required = ['key', 'label', 'path', 'roles', 'routes']
  const seenKeys = new Set()
  const seenPaths = new Set()
  for (const module of modules) {
    const missing = required.filter((field) => module?.[field] === undefined)
    if (missing.length) console.error(`[modules] "${module?.key ?? '?'}" is missing: ${missing.join(', ')}`)
    if (seenKeys.has(module.key)) console.error(`[modules] duplicate key "${module.key}"`)
    if (seenPaths.has(module.path)) console.error(`[modules] duplicate path "${module.path}"`)
    if (module.path?.startsWith('/')) console.error(`[modules] path "${module.path}" must be relative (no leading "/")`)
    seenKeys.add(module.key)
    seenPaths.add(module.path)
  }
}
