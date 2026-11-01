import { EyeOutlined, SearchOutlined } from '@ant-design/icons'
import { Button, Form, Input, Select, Table } from 'antd'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { ROLES } from '../../auth/roles'
import ApiErrorAlert from '../../components/ApiErrorAlert'
import PageHeader from '../../components/PageHeader'
import StatusTag from '../../components/StatusTag'
import { useApiQuery } from '../../hooks/useApiQuery'
import { api } from '../../services/apiClient'
import { formatDate, formatINR, humanizeEnum } from '../../utils/format'

const STATUSES = ['NEW', 'CONTACTED', 'CONSULTATION', 'PROPOSAL', 'CONVERTED', 'LOST']

export default function EnquiriesPage() {
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const isAdmin = hasRole(ROLES.ADMIN)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [filters, setFilters] = useState({ search: '', status: undefined, assignedLeadPlannerId: undefined })
  const query = useApiQuery(
    (signal) => api.get('/api/enquiries', { query: { ...filters, page, size: pageSize }, signal }),
    [filters, page, pageSize],
  )
  const employees = useApiQuery(
    (signal) => isAdmin ? api.get('/api/employees', { signal }) : Promise.resolve([]),
    [isAdmin],
  )

  const plannerOptions = useMemo(
    () => (employees.data ?? [])
      .filter((record) => record.active && record.user.active && record.user.role === ROLES.LEAD_PLANNER)
      .map((record) => ({ value: record.id, label: `${record.user.fullName} · ${record.employeeCode}` })),
    [employees.data],
  )

  const columns = [
    {
      title: 'Enquiry',
      key: 'enquiry',
      render: (_, record) => (
        <div>
          <Link to={`/app/enquiries/${record.id}`}><strong>{record.contactName}</strong></Link>
          <div className="wams-resource-summary">{record.email} · {record.phone}</div>
        </div>
      ),
    },
    { title: 'Wedding date', dataIndex: 'weddingStartDate', render: formatDate, width: 145 },
    { title: 'Destination', dataIndex: 'destination', render: (value) => value || '—', responsive: ['md'] },
    { title: 'Status', dataIndex: 'status', render: (value) => <StatusTag value={value} />, width: 125 },
    { title: 'Assigned planner', dataIndex: 'assignedLeadPlannerName', render: (value) => value || 'Unassigned', responsive: ['lg'] },
    { title: 'Associated client', dataIndex: 'primaryClientName', render: (value) => value || 'Unlinked', responsive: ['lg'] },
    { title: 'Package', dataIndex: 'selectedPackage', render: (value) => value || 'Not recorded', responsive: ['xl'] },
    { title: 'Agreed amount', dataIndex: 'agreedAmount', render: (value) => formatINR(value, { whole: true }), responsive: ['xl'], width: 150 },
    { title: 'Next follow-up', dataIndex: 'nextFollowUpDate', render: formatDate, responsive: ['lg'], width: 145 },
    {
      title: 'Actions', key: 'actions', width: 90,
      render: (_, record) => <Button size="small" icon={<EyeOutlined />} onClick={() => navigate(`/app/enquiries/${record.id}`)}>View</Button>,
    },
  ]

  const applyFilters = (values) => {
    setFilters({
      search: values.search?.trim() || '',
      status: values.status,
      assignedLeadPlannerId: isAdmin ? values.assignedLeadPlannerId : undefined,
    })
    setPage(0)
  }

  const error = query.error || employees.error
  return (
    <>
      <PageHeader
        eyebrow="Pipeline"
        title="Enquiries"
        subtitle={isAdmin ? 'All agency enquiries, assignment and conversion status.' : 'Only enquiries assigned to your planner profile.'}
      />
      {error ? (
        <ApiErrorAlert error={error} onRetry={() => { query.reload(); employees.reload() }} />
      ) : (
        <section className="wams-panel">
          <Form layout="inline" onFinish={applyFilters} className="wams-resource-toolbar" initialValues={{ search: '', status: undefined }}>
            <div className="wams-resource-toolbar__filters">
              <Form.Item name="search" noStyle><Input allowClear placeholder="Name, email, phone or destination" /></Form.Item>
              <Form.Item name="status" noStyle><Select allowClear placeholder="All statuses" options={STATUSES.map((value) => ({ value, label: humanizeEnum(value) }))} /></Form.Item>
              {isAdmin && <Form.Item name="assignedLeadPlannerId" noStyle><Select allowClear showSearch optionFilterProp="label" placeholder="All planners" options={plannerOptions} /></Form.Item>}
              <Button htmlType="submit" icon={<SearchOutlined />}>Apply filters</Button>
            </div>
            <span className="wams-resource-summary">{query.data?.page?.totalElements ?? 0} enquiries</span>
          </Form>
          <Table
            rowKey="id"
            loading={query.loading || employees.loading}
            dataSource={query.data?.content ?? []}
            columns={columns}
            scroll={{ x: 1200 }}
            pagination={{
              current: (query.data?.page?.number ?? page) + 1,
              pageSize: query.data?.page?.size ?? pageSize,
              total: query.data?.page?.totalElements ?? 0,
              showSizeChanger: true,
              showTotal: (total) => `${total} enquiries`,
              placement: ['bottomEnd'],
            }}
            onChange={(pagination) => {
              setPage((pagination.current ?? 1) - 1)
              setPageSize(pagination.pageSize ?? 20)
            }}
          />
        </section>
      )}
    </>
  )
}
