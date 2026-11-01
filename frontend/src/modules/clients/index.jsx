import { ContactsOutlined } from '@ant-design/icons'
import { SECTIONS } from '../../app/moduleSections'
import { ROLES } from '../../auth/roles'
import ClientsPage from './ClientsPage'

export const clientsModule = {
  key: 'clients',
  label: 'Clients',
  description: 'Primary and partner client profiles.',
  icon: <ContactsOutlined />,
  section: SECTIONS.PARTNERS,
  path: 'clients',
  roles: [ROLES.ADMIN, ROLES.LEAD_PLANNER],
  order: 10,
  routes: [{ index: true, element: <ClientsPage /> }],
}

export default clientsModule
