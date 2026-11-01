import { EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, App, Button, Descriptions, Form, Input, Select, Skeleton, Table } from 'antd'
import { useMemo, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { ROLES } from '../../auth/roles'
import ApiErrorAlert from '../../components/ApiErrorAlert'
import PageHeader from '../../components/PageHeader'
import ResourceFormModal from '../../components/ResourceFormModal'
import StatusTag from '../../components/StatusTag'
import { useApiMutation } from '../../hooks/useApiMutation'
import { useApiQuery } from '../../hooks/useApiQuery'
import { api } from '../../services/apiClient'
import { humanizeEnum } from '../../utils/format'

const CATEGORIES = ['CATERING', 'DECOR', 'PHOTOGRAPHY', 'MAKEUP', 'MEHENDI', 'MUSIC_ENTERTAINMENT', 'LIGHTING_SOUND', 'INVITATIONS', 'OTHER']
const STATUSES = ['PENDING_APPROVAL', 'APPROVED', 'REJECTED']
const option = (value) => ({ value, label: humanizeEnum(value) })

export default function VendorsPage() {
  const { hasRole } = useAuth()
  const isAdmin = hasRole(ROLES.ADMIN)
  const isVendor = hasRole(ROLES.VENDOR)
  const { message } = App.useApp()
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState()
  const [status, setStatus] = useState()
  const [editor, setEditor] = useState(null)

  const directory = useApiQuery(
    (signal) => isVendor
      ? Promise.resolve(null)
      : api.get('/api/vendors', { query: { page, size: pageSize, search, category, status: isAdmin ? status : undefined }, signal }),
    [isVendor, isAdmin, page, pageSize, search, category, status],
  )
  const selfProfile = useApiQuery(
    (signal) => isVendor ? api.get('/api/vendors/me', { signal }) : Promise.resolve(null),
    [isVendor],
  )
  const usersQuery = useApiQuery(
    (signal) => isAdmin ? api.get('/api/users', { query: { role: ROLES.VENDOR }, signal }) : Promise.resolve([]),
    [isAdmin],
  )
  const mutation = useApiMutation()

  const linkedUserIds = useMemo(
    () => new Set((directory.data?.content ?? []).map((record) => record.userId).filter(Boolean)),
    [directory.data],
  )
  const vendorUserOptions = useMemo(
    () => (usersQuery.data ?? [])
      .filter((user) => user.active && (!linkedUserIds.has(user.id) || user.id === editor?.record?.userId))
      .map((user) => ({ value: user.id, label: `${user.fullName} · ${user.email}` })),
    [editor, linkedUserIds, usersQuery.data],
  )
  const initialValues = useMemo(() => {
    if (!editor?.record) return { category: 'OTHER' }
    const record = editor.record
    return {
      businessName: record.businessName,
      category: record.category,
      contactPerson: record.contactPerson,
      email: record.email,
      phone: record.phone,
      city: record.city,
      address: record.address,
      serviceDescription: record.serviceDescription,
      userId: record.userId,
    }
  }, [editor])

  const openEditor = (record = null) => {
    mutation.clearError()
    setEditor({ record })
  }

  const submit = async (values) => {
    try {
      if (editor.record) {
        await mutation.run(() => api.put(`/api/vendors/${editor.record.id}`, values))
        message.success('Vendor updated.')
      } else {
        await mutation.run(() => api.post('/api/vendors', values))
        message.success('Vendor created and approved.')
      }
      setEditor(null)
      directory.reload()
      usersQuery.reload()
    } catch {
      // Modal renders the API error.
    }
  }

  if (isVendor) {
    const vendor = selfProfile.data
    return (
      <>
        <PageHeader eyebrow="Vendor portal" title="My vendor profile" subtitle="This page is resolved from your signed-in account; it accepts no vendor ID." />
        {selfProfile.error ? (
          <ApiErrorAlert error={selfProfile.error} onRetry={selfProfile.reload} />
        ) : selfProfile.loading || !vendor ? (
          <section className="wams-panel"><Skeleton active /></section>
        ) : (
          <div className="wams-stack">
            <section className="wams-panel">
              <Descriptions
                title={vendor.businessName}
                column={{ xs: 1, sm: 2, lg: 3 }}
                items={[
                  { key: 'status', label: 'Status', children: <StatusTag value={vendor.status} /> },
                  { key: 'category', label: 'Category', children: humanizeEnum(vendor.category) },
                  { key: 'contact', label: 'Contact person', children: vendor.contactPerson || '—' },
                  { key: 'email', label: 'Contact email', children: vendor.email },
                  { key: 'phone', label: 'Phone', children: vendor.phone },
                  { key: 'city', label: 'City', children: vendor.city || '—' },
                  { key: 'address', label: 'Address', children: vendor.address || '—' },
                  { key: 'description', label: 'Services', children: vendor.serviceDescription || '—' },
                ]}
              />
            </section>
            <Alert type="info" showIcon title="Profile updates are handled by the agency" description="The current backend provides vendor self-read access but no vendor self-update endpoint." />
          </div>
        )}
      </>
    )
  }

  const columns = [
    {
      title: 'Vendor',
      key: 'vendor',
      render: (_, record) => (
        <div><strong>{record.businessName}</strong><div className="wams-resource-summary">{record.contactPerson || record.email}</div></div>
      ),
    },
    { title: 'Category', dataIndex: 'category', render: humanizeEnum, width: 170 },
    { title: 'City', dataIndex: 'city', render: (value) => value || '—', responsive: ['md'], width: 140 },
    { title: 'Phone', dataIndex: 'phone', responsive: ['lg'], width: 160 },
    { title: 'Account', dataIndex: 'userId', render: (value) => value ? `User #${value}` : 'Not linked', responsive: ['lg'], width: 120 },
    { title: 'Status', dataIndex: 'status', render: (value) => <StatusTag value={value} />, width: 130 },
    ...(isAdmin ? [{ title: 'Actions', key: 'actions', width: 95, render: (_, record) => <Button size="small" icon={<EditOutlined />} onClick={() => openEditor(record)}>Edit</Button> }] : []),
  ]

  return (
    <>
      <PageHeader
        eyebrow="Partners"
        title="Vendors"
        subtitle={isAdmin ? 'Create, link and maintain approved vendor directory entries.' : 'Approved vendor directory (read-only).'}
        extra={isAdmin ? <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor()}>New vendor</Button> : null}
      />
      {directory.error || usersQuery.error ? (
        <ApiErrorAlert error={directory.error || usersQuery.error} onRetry={() => { directory.reload(); usersQuery.reload() }} />
      ) : (
        <section className="wams-panel">
          <div className="wams-resource-toolbar">
            <div className="wams-resource-toolbar__filters">
              <Input.Search
                allowClear
                placeholder="Search vendor or city"
                value={search}
                onChange={(event) => { setSearch(event.target.value); setPage(0) }}
              />
              <Select allowClear placeholder="All categories" options={CATEGORIES.map(option)} value={category} onChange={(value) => { setCategory(value); setPage(0) }} />
              {isAdmin && <Select allowClear placeholder="All statuses" options={STATUSES.map(option)} value={status} onChange={(value) => { setStatus(value); setPage(0) }} />}
            </div>
          </div>
          <Table
            rowKey="id"
            loading={directory.loading || usersQuery.loading}
            dataSource={directory.data?.content ?? []}
            columns={columns}
            scroll={{ x: 900 }}
            pagination={{
              current: (directory.data?.page?.number ?? page) + 1,
              pageSize: directory.data?.page?.size ?? pageSize,
              total: directory.data?.page?.totalElements ?? 0,
              showSizeChanger: true,
              showTotal: (total) => `${total} vendors`,
              placement: ['bottomEnd'],
            }}
            onChange={(pagination) => {
              setPage((pagination.current ?? 1) - 1)
              setPageSize(pagination.pageSize ?? 20)
            }}
          />
        </section>
      )}

      {isAdmin && (
        <ResourceFormModal
          open={Boolean(editor)}
          title={editor?.record ? 'Edit vendor' : 'Create vendor'}
          okText={editor?.record ? 'Save changes' : 'Create vendor'}
          initialValues={initialValues}
          loading={mutation.loading}
          error={mutation.error}
          onSubmit={submit}
          onCancel={() => setEditor(null)}
        >
          <div className="wams-form-grid">
            <Form.Item label="Business name" name="businessName" rules={[{ required: true }, { max: 150 }]}><Input /></Form.Item>
            <Form.Item label="Category" name="category" rules={[{ required: true }]}><Select options={CATEGORIES.map(option)} /></Form.Item>
            <Form.Item label="Contact person" name="contactPerson" rules={[{ max: 100 }]}><Input /></Form.Item>
            <Form.Item label="Contact email" name="email" rules={[{ required: true }, { type: 'email' }, { max: 150 }]}><Input type="email" /></Form.Item>
            <Form.Item label="Phone" name="phone" rules={[{ required: true }, { min: 7 }, { max: 20 }, { pattern: /^[0-9+()\- ]+$/, message: 'Use digits and common phone punctuation only.' }]}><Input /></Form.Item>
            <Form.Item label="City" name="city" rules={[{ max: 100 }]}><Input /></Form.Item>
            <Form.Item label="Linked vendor user" name="userId" className="wams-form-grid__wide" extra="Optional. Create a VENDOR user first; one account can link to one vendor.">
              <Select allowClear showSearch optionFilterProp="label" options={vendorUserOptions} />
            </Form.Item>
            <Form.Item label="Address" name="address" className="wams-form-grid__wide" rules={[{ max: 255 }]}><Input.TextArea rows={2} maxLength={255} /></Form.Item>
            <Form.Item label="Service description" name="serviceDescription" className="wams-form-grid__wide" rules={[{ max: 2000 }]}><Input.TextArea rows={4} showCount maxLength={2000} /></Form.Item>
          </div>
        </ResourceFormModal>
      )}
    </>
  )
}
