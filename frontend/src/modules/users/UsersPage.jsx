import { EditOutlined, PlusOutlined, StopOutlined, UnlockOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, Select, Space, Table, Tooltip } from 'antd'
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
import { formatDateTime } from '../../utils/format'

const ROLE_OPTIONS = Object.values(ROLES).map((role) => ({ value: role, label: roleLabel(role) }))

export default function UsersPage() {
  const { user: currentUser } = useAuth()
  const { message, modal } = App.useApp()
  const query = useApiQuery((signal) => api.get('/api/users', { signal }), [])
  const mutation = useApiMutation()
  const [search, setSearch] = useState('')
  const [role, setRole] = useState()
  const [editor, setEditor] = useState(null)

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return (query.data ?? []).filter(
      (record) =>
        (!role || record.role === role) &&
        (!needle || record.fullName.toLowerCase().includes(needle) || record.email.toLowerCase().includes(needle)),
    )
  }, [query.data, role, search])

  const initialValues = useMemo(
    () =>
      editor?.record
        ? { fullName: editor.record.fullName, email: editor.record.email }
        : { role: ROLES.EMPLOYEE, active: true },
    [editor],
  )

  const openEditor = (record = null) => {
    mutation.clearError()
    setEditor({ record })
  }

  const submit = async (values) => {
    try {
      if (editor.record) {
        await mutation.run(() => api.put(`/api/users/${editor.record.id}`, values))
        message.success('User details updated.')
      } else {
        await mutation.run(() => api.post('/api/users', values))
        message.success('User account created.')
      }
      setEditor(null)
      query.reload()
    } catch {
      // ResourceFormModal renders the error.
    }
  }

  const changeStatus = (record) => {
    const activating = !record.active
    modal.confirm({
      title: `${activating ? 'Activate' : 'Deactivate'} ${record.fullName}?`,
      content: activating
        ? 'The account will be able to authenticate again.'
        : 'Existing tokens stop working on the next protected request.',
      okText: activating ? 'Activate' : 'Deactivate',
      okButtonProps: activating ? {} : { danger: true },
      async onOk() {
        try {
          await api.patch(`/api/users/${record.id}/status`, { active: activating })
          message.success(`User ${activating ? 'activated' : 'deactivated'}.`)
          query.reload()
        } catch (error) {
          message.error(error.message)
          throw error
        }
      },
    })
  }

  const columns = [
    {
      title: 'User',
      key: 'user',
      render: (_, record) => (
        <div>
          <strong>{record.fullName}</strong>
          <div className="wams-resource-summary">{record.email}</div>
        </div>
      ),
    },
    { title: 'Role', dataIndex: 'role', render: roleLabel, width: 150 },
    { title: 'Status', dataIndex: 'active', render: (active) => <StatusTag value={active} />, width: 110 },
    { title: 'Updated', dataIndex: 'updatedAt', render: formatDateTime, width: 190, responsive: ['lg'] },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEditor(record)}>
            Edit
          </Button>
          <Tooltip title={record.id === currentUser.id ? 'You cannot change your own status here.' : undefined}>
            <Button
              size="small"
              type="text"
              danger={record.active}
              disabled={record.id === currentUser.id}
              icon={record.active ? <StopOutlined /> : <UnlockOutlined />}
              aria-label={record.active ? 'Deactivate user' : 'Activate user'}
              onClick={() => changeStatus(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Administration"
        title="Users"
        subtitle="Create accounts and control sign-in access. Roles cannot be changed after creation."
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor()}>
            New user
          </Button>
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
                placeholder="Search name or email"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              <Select allowClear placeholder="All roles" options={ROLE_OPTIONS} value={role} onChange={setRole} />
            </div>
            <span className="wams-resource-summary">{filtered.length} users</span>
          </div>
          <Table rowKey="id" loading={query.loading} dataSource={filtered} columns={columns} scroll={{ x: 780 }} />
        </section>
      )}

      <ResourceFormModal
        open={Boolean(editor)}
        title={editor?.record ? 'Edit user' : 'Create user'}
        okText={editor?.record ? 'Save changes' : 'Create user'}
        initialValues={initialValues}
        loading={mutation.loading}
        error={mutation.error}
        onSubmit={submit}
        onCancel={() => setEditor(null)}
      >
        <Form.Item label="Full name" name="fullName" rules={[{ required: true }, { max: 150 }]}>
          <Input />
        </Form.Item>
        <Form.Item
          label="Email"
          name="email"
          rules={[{ required: true }, { type: 'email' }, { max: 190 }]}
        >
          <Input type="email" autoComplete="off" />
        </Form.Item>
        {!editor?.record && (
          <>
            <Form.Item
              label="Temporary password"
              name="password"
              rules={[{ required: true }, { min: 8 }, { max: 72 }]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <Form.Item label="Role" name="role" rules={[{ required: true }]}>
              <Select options={ROLE_OPTIONS} />
            </Form.Item>
            <Form.Item label="Status" name="active" rules={[{ required: true }]}>
              <Select options={[{ value: true, label: 'Active' }, { value: false, label: 'Inactive' }]} />
            </Form.Item>
          </>
        )}
      </ResourceFormModal>
    </>
  )
}
