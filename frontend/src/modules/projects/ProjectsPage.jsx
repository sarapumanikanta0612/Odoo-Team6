import { EyeOutlined, InboxOutlined } from '@ant-design/icons'
import { Button, Input, Select, Table } from 'antd'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { ROLES } from '../../auth/roles'
import ApiErrorAlert from '../../components/ApiErrorAlert'
import PageHeader from '../../components/PageHeader'
import StatusTag from '../../components/StatusTag'
import { useApiQuery } from '../../hooks/useApiQuery'
import { api } from '../../services/apiClient'
import { formatDate } from '../../utils/format'

const PROJECT_STATUSES = ['PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'POSTPONED']

export default function ProjectsPage() {
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const isStaff = hasRole(ROLES.ADMIN, ROLES.LEAD_PLANNER)
  const query = useApiQuery(async (signal) => {
    const projects = await api.get('/api/projects', { signal })
    if (!isStaff) return { projects, clients: [], employees: [] }
    const [clients, employees] = await Promise.all([
      api.get('/api/clients', { signal }),
      api.get('/api/employees', { signal }),
    ])
    return { projects, clients, employees }
  }, [isStaff])
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState()

  const clientNames = useMemo(
    () => new Map((query.data?.clients ?? []).map((record) => [record.id, record.user.fullName])),
    [query.data],
  )
  const plannerNames = useMemo(
    () => new Map((query.data?.employees ?? []).map((record) => [record.id, record.user.fullName])),
    [query.data],
  )
  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return (query.data?.projects ?? []).filter((project) => {
      const text = `${project.projectName} ${project.venue ?? ''} ${clientNames.get(project.primaryClientId) ?? ''}`
      return (!status || project.projectStatus === status) && (!needle || text.toLowerCase().includes(needle))
    })
  }, [clientNames, query.data, search, status])

  const columns = [
    {
      title: 'Project',
      key: 'project',
      render: (_, project) => (
        <div>
          <Link to={`/app/projects/${project.id}`}><strong>{project.projectName}</strong></Link>
          <div className="wams-resource-summary">{project.venue || 'Venue not recorded'}</div>
        </div>
      ),
    },
    { title: 'Wedding date', dataIndex: 'weddingDate', render: formatDate, width: 145 },
    ...(isStaff
      ? [
          { title: 'Primary client', dataIndex: 'primaryClientId', render: (id) => clientNames.get(id) || `Client #${id}`, responsive: ['md'] },
          { title: 'Lead planner', dataIndex: 'leadPlannerId', render: (id) => plannerNames.get(id) || `Employee #${id}`, responsive: ['lg'] },
        ]
      : []),
    { title: 'Status', dataIndex: 'projectStatus', render: (value) => <StatusTag value={value} />, width: 125 },
    ...(isStaff
      ? [{
          title: 'Source enquiry',
          dataIndex: 'enquiryId',
          render: (id) => id ? <Link to={`/app/enquiries/${id}`}>#{id}</Link> : 'Legacy project',
          responsive: ['xl'],
          width: 130,
        }]
      : []),
    {
      title: 'Actions',
      key: 'actions',
      width: 90,
      render: (_, project) => (
        <Button size="small" icon={<EyeOutlined />} onClick={() => navigate(`/app/projects/${project.id}`)}>
          View
        </Button>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Operations"
        title={isStaff ? 'Wedding projects' : 'My wedding'}
        subtitle={isStaff ? 'Only projects within your backend-authorized scope are shown.' : 'Wedding details available to your client account.'}
        extra={
          isStaff ? (
            <Button type="primary" icon={<InboxOutlined />} onClick={() => navigate('/app/enquiries')}>
              Open enquiries
            </Button>
          ) : null
        }
      />
      {query.error ? (
        <ApiErrorAlert error={query.error} onRetry={query.reload} />
      ) : (
        <section className="wams-panel">
          <div className="wams-resource-toolbar">
            <div className="wams-resource-toolbar__filters">
              <Input.Search
                allowClear
                placeholder="Search project, venue or client"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              <Select
                allowClear
                placeholder="All project statuses"
                value={status}
                onChange={setStatus}
                options={PROJECT_STATUSES.map((value) => ({ value, label: value.replaceAll('_', ' ') }))}
              />
            </div>
            <span className="wams-resource-summary">{filtered.length} projects</span>
          </div>
          <Table rowKey="id" loading={query.loading} dataSource={filtered} columns={columns} scroll={{ x: 1000 }} />
        </section>
      )}
    </>
  )
}
