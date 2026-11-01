import { ArrowLeftOutlined, CalendarOutlined, DollarOutlined, SaveOutlined } from '@ant-design/icons'
import { App, Button, DatePicker, Descriptions, Form, Input, Select, Skeleton } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useMemo } from 'react'
import { Link, useParams } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { ROLES } from '../../auth/roles'
import ApiErrorAlert from '../../components/ApiErrorAlert'
import PageHeader from '../../components/PageHeader'
import StatusTag from '../../components/StatusTag'
import { useApiMutation } from '../../hooks/useApiMutation'
import { useApiQuery } from '../../hooks/useApiQuery'
import { api } from '../../services/apiClient'
import { formatDate, formatDateTime, formatINR } from '../../utils/format'

const PROJECT_STATUSES = ['PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'POSTPONED']

export default function ProjectDetailPage() {
  const { projectId } = useParams()
  const { hasRole } = useAuth()
  const isAdmin = hasRole(ROLES.ADMIN)
  const isPrimaryClient = hasRole(ROLES.PRIMARY_CLIENT)
  const isStaff = hasRole(ROLES.ADMIN, ROLES.LEAD_PLANNER)
  const query = useApiQuery(async (signal) => {
    const project = await api.get(`/api/projects/${projectId}`, { signal })
    if (!isStaff) return { project, clients: [], employees: [] }
    const [clients, employees] = await Promise.all([
      api.get('/api/clients', { signal }),
      api.get('/api/employees', { signal }),
    ])
    return { project, clients, employees }
  }, [isStaff, projectId])

  const clients = query.data?.clients ?? []
  const employees = query.data?.employees ?? []
  const project = query.data?.project
  const primary = clients.find((record) => record.id === project?.primaryClientId)
  const partner = clients.find((record) => record.id === project?.partnerClientId)
  const lead = employees.find((record) => record.id === project?.leadPlannerId)

  if (query.error) return <ApiErrorAlert error={query.error} onRetry={query.reload} />

  return (
    <>
      <PageHeader
        eyebrow="Wedding project"
        title={project?.projectName || 'Project details'}
        subtitle={project ? `${formatDate(project.weddingDate)} · ${project.venue || 'Venue not recorded'}` : 'Loading project'}
        extra={<div className="wams-inline-actions">{(isStaff||isPrimaryClient)&&<Link to={`/app/projects/${projectId}/quotations`}><Button icon={<DollarOutlined/>}>Quotations</Button></Link>}{isStaff&&<Link to={`/app/projects/${projectId}/operations`}><Button type="primary" icon={<CalendarOutlined/>}>Functions & tasks</Button></Link>}<Link to="/app/projects"><Button icon={<ArrowLeftOutlined />}>Back to projects</Button></Link></div>}
      />
      {query.loading || !project ? (
        <section className="wams-panel"><Skeleton active /></section>
      ) : (
        <div className="wams-stack">
          <section className="wams-panel">
            <h2 className="wams-panel__title">Overview</h2>
            <Descriptions
              column={{ xs: 1, sm: 2, lg: 3 }}
              items={[
                { key: 'status', label: 'Status', children: <StatusTag value={project.projectStatus} /> },
                { key: 'date', label: 'Wedding date', children: formatDate(project.weddingDate) },
                { key: 'venue', label: 'Venue', children: project.venue || 'Not recorded' },
                ...(isStaff
                  ? [
                      { key: 'client', label: 'Primary client', children: primary?.user.fullName || `Client #${project.primaryClientId}` },
                      { key: 'partner', label: 'Partner client', children: partner?.user.fullName || 'Not assigned' },
                      { key: 'lead', label: 'Lead planner', children: lead?.user.fullName || `Employee #${project.leadPlannerId}` },
                    ]
                  : []),
                { key: 'package', label: 'Selected package', children: project.selectedPackage || 'Not recorded' },
                { key: 'amount', label: 'Agreed amount', children: formatINR(project.agreedAmount) },
                { key: 'deposit', label: 'Booking deposit', children: formatINR(project.bookingDepositAmount) },
                { key: 'depositRef', label: 'Deposit reference', children: project.bookingDepositReference || '—' },
                {
                  key: 'enquiry',
                  label: 'Enquiry',
                  children: project.enquiryId
                    ? (isStaff ? <Link to={`/app/enquiries/${project.enquiryId}`}>#{project.enquiryId}</Link> : `#${project.enquiryId}`)
                    : 'Legacy project',
                },
                { key: 'created', label: 'Created', children: formatDateTime(project.createdAt) },
              ]}
            />
          </section>

          {isStaff && (
            <ProjectEditor
              project={project}
              clients={clients}
              employees={employees}
              isAdmin={isAdmin}
              onUpdated={query.reload}
            />
          )}
        </div>
      )}
    </>
  )
}

function ProjectEditor({ project, clients, employees, isAdmin, onUpdated }) {
  const { message } = App.useApp()
  const [form] = Form.useForm()
  const mutation = useApiMutation()
  const partnerOptions = useMemo(
    () => clients
      .filter((record) => record.user.role === ROLES.PARTNER_CLIENT && record.user.active)
      .map((record) => ({ value: record.id, label: `${record.user.fullName} · ${record.user.email}` })),
    [clients],
  )
  const leadOptions = useMemo(
    () => employees
      .filter((record) => record.active && record.user.active && [ROLES.ADMIN, ROLES.LEAD_PLANNER].includes(record.user.role))
      .map((record) => ({ value: record.id, label: `${record.user.fullName} · ${record.employeeCode}` })),
    [employees],
  )

  useEffect(() => {
    form.setFieldsValue({
      projectName: project.projectName,
      weddingDate: project.weddingDate ? dayjs(project.weddingDate) : undefined,
      venue: project.venue,
      projectStatus: project.projectStatus,
      partnerClientId: project.partnerClientId,
      leadPlannerId: project.leadPlannerId,
    })
  }, [form, project])

  const submit = async (values) => {
    const body = {
      projectName: values.projectName,
      weddingDate: values.weddingDate?.format('YYYY-MM-DD'),
      venue: values.venue,
      projectStatus: values.projectStatus,
      partnerClientId: values.partnerClientId,
      ...(isAdmin ? { leadPlannerId: values.leadPlannerId } : {}),
    }
    try {
      await mutation.run(() => api.put(`/api/projects/${project.id}`, body))
      message.success('Project updated.')
      onUpdated()
    } catch {
      // ApiErrorAlert renders the error.
    }
  }

  return (
    <section className="wams-panel">
      <h2 className="wams-panel__title">Update project</h2>
      {mutation.error && <ApiErrorAlert error={mutation.error} style={{ marginBottom: 16 }} />}
      <Form form={form} layout="vertical" requiredMark={false} disabled={mutation.loading} onFinish={submit}>
        <div className="wams-form-grid">
          <Form.Item label="Project name" name="projectName" rules={[{ required: true }, { max: 200 }]}><Input /></Form.Item>
          <Form.Item label="Wedding date" name="weddingDate" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} format="DD MMM YYYY" /></Form.Item>
          <Form.Item label="Venue" name="venue" rules={[{ max: 300 }]}><Input /></Form.Item>
          <Form.Item label="Project status" name="projectStatus"><Select options={PROJECT_STATUSES.map((value) => ({ value, label: value.replaceAll('_', ' ') }))} /></Form.Item>
          <Form.Item label="Partner client" name="partnerClientId" extra="An existing partner cannot be removed with the current API."><Select showSearch optionFilterProp="label" options={partnerOptions} /></Form.Item>
          {isAdmin && <Form.Item label="Lead planner" name="leadPlannerId" rules={[{ required: true }]}><Select showSearch optionFilterProp="label" options={leadOptions} /></Form.Item>}
        </div>
        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={mutation.loading}>Save project</Button>
      </Form>
    </section>
  )
}
