import { TeamOutlined } from '@ant-design/icons'
import { SECTIONS } from '../../app/moduleSections'
import { ROLES } from '../../auth/roles'
import EmployeesPage from './EmployeesPage'

export const employeesModule = {
  key: 'employees',
  label: 'Employees',
  description: 'Staff profiles and planning team details.',
  icon: <TeamOutlined />,
  section: SECTIONS.OVERVIEW,
  path: 'employees',
  roles: [ROLES.ADMIN, ROLES.LEAD_PLANNER],
  order: 30,
  routes: [{ index: true, element: <EmployeesPage /> }],
}

export default employeesModule
