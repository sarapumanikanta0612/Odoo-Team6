import { EditOutlined, PlusOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, InputNumber, Select, Table } from 'antd'
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

export default function ClientsPage() {
  const { hasRole } = useAuth()
  const isAdmin = hasRole(ROLES.ADMIN)
  const { message } = App.useApp()
  const query = useApiQuery((signal) => api.get('/api/clients', { signal }), [])
  const usersQuery = useApiQuery(
    async (signal) => {
      if (!isAdmin) return []
      const [primary, partners] = await Promise.all([
        api.get('/api/users', { query: { role: ROLES.PRIMARY_CLIENT }, signal }),
        api.get('/api/users', { query: { role: ROLES.PARTNER_CLIENT }, signal }),
      ])
      return [...primary, ...partners]
    },
    [isAdmin],
  )
  const mutation = useApiMutation()
  const [search, setSearch] = useState('')
  const [editor, setEditor] = useState(null)

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return (query.data ?? []).filter((record) => {
      const text = `${record.user.fullName} ${record.user.email} ${record.phoneNumber ?? ''} ${record.address ?? ''}`
      return !needle || text.toLowerCase().includes(needle)
    })
  }, [query.data, search])

  const candidates = useMemo(() => {
    const linked = new Set((query.data ?? []).map((record) => record.user.id))
    return (usersQuery.data ?? [])
      .filter((user) => user.active && !linked.has(user.id))
      .map((user) => ({ value: user.id, label: `${user.fullName} · ${roleLabel(user.role)} · ${user.email}` }))
  }, [query.data, usersQuery.data])

  const initialValues = useMemo(
    () =>
      editor?.record
        ? { phoneNumber: editor.record.phoneNumber, address: editor.record.address }
        : {},
    [editor],
  )

  const openEditor = (record = null) => {
    mutation.clearError()
    setEditor({ record })
  }

  const submit = async (values) => {
    try {
      if (editor.record) {
        await mutation.run(() => api.put(`/api/clients/${editor.record.id}`, values))
        message.success('Client profile updated.')
      } else {
        await mutation.run(() => api.post('/api/clients', values))
        message.success('Client profile created.')
      }
      setEditor(null)
      query.reload()
      if (isAdmin) usersQuery.reload()
    } catch {
      // Modal renders the API error.
    }
  }

  const columns = [
    {
      title: 'Client',
      key: 'client',
      render: (_, record) => (
        <div>
          <strong>{record.user.fullName}</strong>
          <div className="wams-resource-summary">{record.user.email}</div>
        </div>
      ),
    },
    { title: 'Type', key: 'type', render: (_, record) => roleLabel(record.user.role), width: 140 },
    { title: 'Phone', dataIndex: 'phoneNumber', render: (value) => value || '—', width: 160 },
    { title: 'Address', dataIndex: 'address', render: (value) => value || '—', responsive: ['lg'] },
    { title: 'Account', key: 'active', render: (_, record) => <StatusTag value={record.user.active} />, width: 105 },
    { title: 'Updated', dataIndex: 'updatedAt', render: formatDateTime, responsive: ['xl'], width: 190 },
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

  return (
    <>
      <PageHeader
        eyebrow="Relationships"
        title="Clients"
        subtitle="Contact profiles linked to primary or partner client accounts."
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor()}>
            New client profile
          </Button>
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
                placeholder="Search name, email, phone or address"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
            <span className="wams-resource-summary">{filtered.length} clients</span>
          </div>
          <Table rowKey="id" loading={query.loading || usersQuery.loading} dataSource={filtered} columns={columns} scroll={{ x: 860 }} />
        </section>
      )}

      <ResourceFormModal
        open={Boolean(editor)}
        title={editor?.record ? 'Edit client' : 'Create client profile'}
        okText={editor?.record ? 'Save changes' : 'Create profile'}
        initialValues={initialValues}
        loading={mutation.loading}
        error={mutation.error}
        onSubmit={submit}
        onCancel={() => setEditor(null)}
      >
        {!editor?.record && (
          <Form.Item
            label="Client user account"
            name="userId"
            rules={[{ required: true, message: isAdmin ? 'Select an unlinked client account.' : 'Enter a user ID.' }]}
            extra={
              isAdmin
                ? 'Public registration already creates a profile. This list shows admin-created client accounts without one.'
                : 'The user directory is admin-only. Enter the existing primary/partner client User ID.'
            }
          >
            {isAdmin ? (
              <Select showSearch optionFilterProp="label" options={candidates} />
            ) : (
              <InputNumber min={1} precision={0} style={{ width: '100%' }} />
            )}
          </Form.Item>
        )}
        <Form.Item label="Phone number" name="phoneNumber" rules={[{ max: 30 }]}>
          <Input />
        </Form.Item>
        <Form.Item label="Address" name="address" rules={[{ max: 500 }]}>
          <Input.TextArea rows={4} showCount maxLength={500} />
        </Form.Item>
      </ResourceFormModal>
    </>
  )
}
