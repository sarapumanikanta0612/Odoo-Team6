import { UserOutlined } from '@ant-design/icons'
import { SECTIONS } from '../../app/moduleSections'
import { ROLES } from '../../auth/roles'
import UsersPage from './UsersPage'

export const usersModule = {
  key: 'users',
  label: 'Users',
  description: 'Accounts, roles and access status.',
  icon: <UserOutlined />,
  section: SECTIONS.OVERVIEW,
  path: 'users',
  roles: [ROLES.ADMIN],
  order: 20,
  routes: [{ index: true, element: <UsersPage /> }],
}

export default usersModule
