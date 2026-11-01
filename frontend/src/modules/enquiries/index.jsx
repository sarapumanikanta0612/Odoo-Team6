import { InboxOutlined } from '@ant-design/icons'
import { SECTIONS } from '../../app/moduleSections'
import { ROLES } from '../../auth/roles'
import EnquiriesPage from './EnquiriesPage'
import EnquiryDetailPage from './EnquiryDetailPage'

export const enquiriesModule = {
  key: 'enquiries',
  label: 'Enquiries',
  description: 'Assignment, follow-ups, qualification and conversion.',
  icon: <InboxOutlined />,
  section: SECTIONS.OPERATIONS,
  path: 'enquiries',
  roles: [ROLES.ADMIN, ROLES.LEAD_PLANNER],
  order: 5,
  routes: [
    { index: true, element: <EnquiriesPage /> },
    { path: ':enquiryId', element: <EnquiryDetailPage /> },
  ],
}

export default enquiriesModule
