import { EditOutlined, PlusOutlined } from '@ant-design/icons'
import { App, Button, DatePicker, Form, Input, Select, Table } from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { ROLES, roleLabel } from '../../auth/roles'
import ApiErrorAlert from '../../components/ApiErrorAlert'
import PageHeader from '../../components/PageHeader'
import ResourceFormModal from '../../components/ResourceFormModal'
import StatusTag from '../../components/StatusTag'
import { useApiMutation } from '../../hooks/useApiMutation'
import { useApiQuery } from '../../hooks/useApiQuery'
import { api } from '../../services/apiClient'
import { formatDate } from '../../utils/format'

const EMPLOYEE_ROLES = [ROLES.ADMIN, ROLES.LEAD_PLANNER, ROLES.EMPLOYEE]

export default function EmployeesPage() {
  const { hasRole } = useAuth()
  const isAdmin = hasRole(ROLES.ADMIN)
  const { message } = App.useApp()
  const query = useApiQuery((signal) => api.get('/api/employees', { signal }), [])
  const usersQuery = useApiQuery(
    async (signal) => {
      if (!isAdmin) return []
      const groups = await Promise.all(
        EMPLOYEE_ROLES.map((role) => api.get('/api/users', { query: { role }, signal })),
      )
      return groups.flat()
    },
    [isAdmin],
  )
  const mutation = useApiMutation()
  const [search, setSearch] = useState('')
  const [editor, setEditor] = useState(null)

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return (query.data ?? []).filter((record) => {
      const text = `${record.user.fullName} ${record.user.email} ${record.employeeCode} ${record.designation ?? ''}`
      return !needle || text.toLowerCase().includes(needle)
    })
  }, [query.data, search])

  const candidates = useMemo(() => {
    const linked = new Set((query.data ?? []).map((record) => record.user.id))
    return (usersQuery.data ?? [])
      .filter((user) => user.active && !linked.has(user.id))
      .map((user) => ({ value: user.id, label: `${user.fullName} · ${roleLabel(user.role)} · ${user.email}` }))
  }, [query.data, usersQuery.data])

  const initialValues = useMemo(() => {
    if (!editor?.record) return { active: true }
    const record = editor.record
    return {
      designation: record.designation,
      department: record.department,
      joiningDate: record.joiningDate ? dayjs(record.joiningDate) : undefined,
      active: record.active,
    }
  }, [editor])

  const openEditor = (record = null) => {
    mutation.clearError()
    setEditor({ record })
  }

  const submit = async (values) => {
    const body = {
      ...values,
      joiningDate: values.joiningDate?.format('YYYY-MM-DD') ?? null,
    }
    try {
      if (editor.record) {
        await mutation.run(() => api.put(`/api/employees/${editor.record.id}`, body))
        message.success('Employee profile updated.')
      } else {
        await mutation.run(() => api.post('/api/employees', body))
        message.success('Employee profile created.')
      }
      setEditor(null)
      query.reload()
      usersQuery.reload()
    } catch {
      // Modal renders the API error.
    }
  }

  const columns = [
    {
      title: 'Employee',
      key: 'employee',
      render: (_, record) => (
        <div>
          <strong>{record.user.fullName}</strong>
          <div className="wams-resource-summary">{record.user.email}</div>
        </div>
      ),
    },
    { title: 'Code', dataIndex: 'employeeCode', width: 130 },
    { title: 'Role', key: 'role', render: (_, record) => roleLabel(record.user.role), width: 140 },
    { title: 'Designation', dataIndex: 'designation', responsive: ['md'] },
    { title: 'Department', dataIndex: 'department', responsive: ['lg'] },
    { title: 'Joined', dataIndex: 'joiningDate', render: formatDate, responsive: ['xl'], width: 130 },
    { title: 'Status', dataIndex: 'active', render: (active) => <StatusTag value={active} />, width: 100 },
    ...(isAdmin
      ? [
          {
            title: 'Actions',
            key: 'actions',
            width: 100,
            render: (_, record) => (
              <Button size="small" icon={<EditOutlined />} onClick={() => openEditor(record)}>
                Edit
              </Button>
            ),
          },
        ]
      : []),
  ]

  return (
    <>
      <PageHeader
        eyebrow="Team"
        title="Employees"
        subtitle={isAdmin ? 'Create and maintain staff profiles.' : 'Agency staff directory (read-only).'}
        extra={
          isAdmin ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor()}>
              New employee profile
            </Button>
          ) : null
        }
      />
      {query.error || usersQuery.error ? (
        <ApiErrorAlert error={query.error || usersQuery.error} onRetry={() => { query.reload(); usersQuery.reload() }} />
      ) : (
        <section className="wams-panel">
          <div className="wams-resource-toolbar">
            <div className="wams-resource-toolbar__filters">
              <Input.Search
                allowClear
                placeholder="Search name, email, code or designation"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
            <span className="wams-resource-summary">{filtered.length} employees</span>
          </div>
          <Table rowKey="id" loading={query.loading || usersQuery.loading} dataSource={filtered} columns={columns} scroll={{ x: 900 }} />
        </section>
      )}

      {isAdmin && (
        <ResourceFormModal
          open={Boolean(editor)}
          title={editor?.record ? 'Edit employee' : 'Create employee profile'}
          okText={editor?.record ? 'Save changes' : 'Create profile'}
          initialValues={initialValues}
          loading={mutation.loading}
          error={mutation.error}
          onSubmit={submit}
          onCancel={() => setEditor(null)}
        >
          {!editor?.record && (
            <>
              <Form.Item
                label="User account"
                name="userId"
                rules={[{ required: true, message: 'Select an unlinked staff account.' }]}
                extra="Create the user account in Users first."
              >
                <Select showSearch optionFilterProp="label" options={candidates} />
              </Form.Item>
              <Form.Item label="Employee code" name="employeeCode" rules={[{ required: true }, { max: 50 }]}>
                <Input />
              </Form.Item>
            </>
          )}
          <div className="wams-form-grid">
            <Form.Item label="Designation" name="designation" rules={[{ max: 100 }]}>
              <Input />
            </Form.Item>
            <Form.Item label="Department" name="department" rules={[{ max: 100 }]}>
              <Input />
            </Form.Item>
            <Form.Item label="Joining date" name="joiningDate">
              <DatePicker style={{ width: '100%' }} format="DD MMM YYYY" />
            </Form.Item>
            <Form.Item label="Profile status" name="active" rules={[{ required: true }]}>
              <Select options={[{ value: true, label: 'Active' }, { value: false, label: 'Inactive' }]} />
            </Form.Item>
          </div>
        </ResourceFormModal>
      )}
    </>
  )
}
