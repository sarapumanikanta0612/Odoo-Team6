import { ShopOutlined } from '@ant-design/icons'
import { SECTIONS } from '../../app/moduleSections'
import { ROLES } from '../../auth/roles'
import VendorsPage from './VendorsPage'

export const vendorsModule = {
  key: 'vendors',
  label: 'Vendors',
  description: 'Vendor directory and linked account profile.',
  icon: <ShopOutlined />,
  section: SECTIONS.PARTNERS,
  path: 'vendors',
  roles: [ROLES.ADMIN, ROLES.LEAD_PLANNER, ROLES.VENDOR],
  order: 20,
  routes: [{ index: true, element: <VendorsPage /> }],
}

export default vendorsModule
