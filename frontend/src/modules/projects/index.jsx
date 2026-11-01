import { ProjectOutlined } from '@ant-design/icons'
import { SECTIONS } from '../../app/moduleSections'
import { ROLES } from '../../auth/roles'
import RequireRole from '../../auth/RequireRole'
import ProjectDetailPage from './ProjectDetailPage'
import ProjectOperationsPage from './ProjectOperationsPage'
import ProjectQuotationsPage from './ProjectQuotationsPage'
import ProjectsPage from './ProjectsPage'
import QualificationPage from './QualificationPage'

export const projectsModule = {
  key: 'projects',
  label: 'Wedding projects',
  description: 'Qualified weddings, dates and project status.',
  icon: <ProjectOutlined />,
  section: SECTIONS.OPERATIONS,
  path: 'projects',
  roles: [ROLES.ADMIN, ROLES.LEAD_PLANNER, ROLES.PRIMARY_CLIENT, ROLES.PARTNER_CLIENT],
  order: 10,
  routes: [
    { index: true, element: <ProjectsPage /> },
    {
      path: 'qualification',
      element: <RequireRole roles={[ROLES.ADMIN, ROLES.LEAD_PLANNER]}><QualificationPage /></RequireRole>,
    },
    {
      path: 'qualification/:enquiryId',
      element: <RequireRole roles={[ROLES.ADMIN, ROLES.LEAD_PLANNER]}><QualificationPage /></RequireRole>,
    },
    { path: ':projectId/operations', element: <RequireRole roles={[ROLES.ADMIN, ROLES.LEAD_PLANNER]}><ProjectOperationsPage /></RequireRole> },
    { path: ':projectId/quotations', element: <RequireRole roles={[ROLES.ADMIN, ROLES.LEAD_PLANNER, ROLES.PRIMARY_CLIENT]}><ProjectQuotationsPage /></RequireRole> },
    { path: ':projectId', element: <ProjectDetailPage /> },
  ],
}

export default projectsModule
