import { ArrowLeftOutlined, EditOutlined, PlusOutlined, RocketOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { App, Button, DatePicker, Descriptions, Form, Input, InputNumber, Select, Skeleton, Table } from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { ROLES } from '../../auth/roles'
import ApiErrorAlert from '../../components/ApiErrorAlert'
import PageHeader from '../../components/PageHeader'
import ResourceFormModal from '../../components/ResourceFormModal'
import StatusTag from '../../components/StatusTag'
import { useApiMutation } from '../../hooks/useApiMutation'
import { useApiQuery } from '../../hooks/useApiQuery'
import { api } from '../../services/apiClient'
import { formatDate, formatDateTime, formatINR, humanizeEnum } from '../../utils/format'

const PAYMENT_METHODS = ['CASH', 'BANK_TRANSFER', 'UPI', 'CHEQUE', 'CARD', 'OTHER']
const futureDate = (date) => date && date.startOf('day').isAfter(dayjs().startOf('day'))
const pastDate = (date) => date && date.startOf('day').isBefore(dayjs().startOf('day'))

export default function EnquiryDetailPage() {
  const { enquiryId } = useParams()
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const isAdmin = hasRole(ROLES.ADMIN)
  const { message } = App.useApp()
  const [editor, setEditor] = useState(null)
  const mutation = useApiMutation()

  const detailQuery = useApiQuery((signal) => api.get(`/api/enquiries/${enquiryId}`, { signal }), [enquiryId])
  const followUpsQuery = useApiQuery(
    (signal) => api.get(`/api/enquiries/${enquiryId}/follow-ups`, { query: { page: 0, size: 50 }, signal }),
    [enquiryId],
  )
  const employeesQuery = useApiQuery(
    (signal) => isAdmin ? api.get('/api/employees', { signal }) : Promise.resolve([]),
    [isAdmin],
  )
  const clientsQuery = useApiQuery((signal) => api.get('/api/clients', { signal }), [])
  const agreementQuery = useApiQuery(
    (signal) => detailQuery.data?.agreementStatus
      ? api.get(`/api/enquiries/${enquiryId}/agreement`, { signal })
      : Promise.resolve(null),
    [detailQuery.data?.agreementStatus, enquiryId],
  )
  const depositQuery = useApiQuery(
    (signal) => detailQuery.data?.bookingDepositStatus
      ? api.get(`/api/enquiries/${enquiryId}/booking-deposit`, { signal })
      : Promise.resolve(null),
    [detailQuery.data?.bookingDepositStatus, enquiryId],
  )

  const detail = detailQuery.data
  const agreement = agreementQuery.data
  const deposit = depositQuery.data
  const terminal = detail?.status === 'CONVERTED' || detail?.status === 'LOST'
  const plannerOptions = useMemo(
    () => (employeesQuery.data ?? [])
      .filter((record) => record.active && record.user.active && record.user.role === ROLES.LEAD_PLANNER)
      .map((record) => ({ value: record.id, label: `${record.user.fullName} · ${record.employeeCode}` })),
    [employeesQuery.data],
  )
  const primaryClientOptions = useMemo(
    () => (clientsQuery.data ?? [])
      .filter((record) => record.user.active && record.user.role === ROLES.PRIMARY_CLIENT)
      .map((record) => ({ value: record.id, label: `${record.user.fullName} · ${record.user.email}` })),
    [clientsQuery.data],
  )
  const partnerClientOptions = useMemo(
    () => (clientsQuery.data ?? [])
      .filter((record) => record.user.active && record.user.role === ROLES.PARTNER_CLIENT)
      .map((record) => ({ value: record.id, label: `${record.user.fullName} · ${record.user.email}` })),
    [clientsQuery.data],
  )

  const agreementStatusOptions = useMemo(() => {
    if (!agreement) return ['PENDING', 'SIGNED']
    if (agreement.status === 'PENDING') return ['PENDING', 'SIGNED', 'VOIDED']
    if (agreement.status === 'SIGNED') return ['SIGNED', 'VOIDED']
    return ['VOIDED']
  }, [agreement])
  const depositStatusOptions = useMemo(() => {
    if (!deposit) return ['PENDING', 'PAID']
    if (deposit.status === 'PENDING') return ['PENDING', 'PAID']
    if (deposit.status === 'PAID') return ['PAID', 'REFUNDED']
    return ['REFUNDED']
  }, [deposit])

  const editorInitialValues = useMemo(() => {
    switch (editor) {
      case 'assignment': return { assignedLeadPlannerId: detail?.assignedLeadPlannerId }
      case 'client-link': return {
        primaryClientId: detail?.primaryClientId,
        expectedCurrentPrimaryClientId: detail?.primaryClientId,
      }
      case 'status': return { status: detail?.allowedStatusTransitions?.[0] }
      case 'follow-up': return { occurredAt: dayjs() }
      case 'commercial': return {
        selectedPackage: detail?.selectedPackage,
        packageDescription: detail?.packageDescription,
        agreedAmount: detail?.agreedAmount,
      }
      case 'agreement': return agreement ? {
        agreementReference: agreement.agreementReference,
        status: agreement.status,
        signedByName: agreement.signedByName,
        signedAt: agreement.signedAt ? dayjs(agreement.signedAt) : undefined,
      } : { status: 'PENDING' }
      case 'deposit': return deposit ? {
        amount: deposit.amount,
        status: deposit.status,
        paidOn: deposit.paidOn ? dayjs(deposit.paidOn) : undefined,
        paymentMethod: deposit.paymentMethod,
        paymentReference: deposit.paymentReference,
      } : { status: 'PENDING' }
      case 'convert': return {
        projectName: `${detail?.contactName ?? ''} Wedding`,
        weddingDate: detail?.weddingStartDate ? dayjs(detail.weddingStartDate) : undefined,
        venue: detail?.destination,
      }
      default: return {}
    }
  }, [agreement, deposit, detail, editor])

  const openEditor = (type) => {
    mutation.clearError()
    setEditor(type)
  }

  const refresh = () => {
    detailQuery.reload()
    followUpsQuery.reload()
    agreementQuery.reload()
    depositQuery.reload()
  }

  const submit = async (values) => {
    try {
      let success = 'Enquiry updated.'
      if (editor === 'client-link') {
        await mutation.run(() => api.patch(`/api/enquiries/${enquiryId}/primary-client`, values))
        success = 'Primary client linked.'
      } else if (editor === 'assignment') {
        await mutation.run(() => api.patch(`/api/enquiries/${enquiryId}/assignment`, values))
        success = 'Lead planner assigned.'
      } else if (editor === 'status') {
        await mutation.run(() => api.patch(`/api/enquiries/${enquiryId}/status`, values))
        success = 'Enquiry status updated.'
      } else if (editor === 'follow-up') {
        await mutation.run(() => api.post(`/api/enquiries/${enquiryId}/follow-ups`, {
          occurredAt: values.occurredAt.toISOString(),
          notes: values.notes,
          nextFollowUpDate: values.nextFollowUpDate?.format('YYYY-MM-DD'),
        }))
        success = 'Follow-up recorded.'
      } else if (editor === 'commercial') {
        await mutation.run(() => api.patch(`/api/enquiries/${enquiryId}/commercial`, values))
        success = 'Package and agreed amount recorded.'
      } else if (editor === 'agreement') {
        await mutation.run(() => api.put(`/api/enquiries/${enquiryId}/agreement`, {
          agreementReference: values.agreementReference,
          status: values.status,
          signedByName: values.signedByName,
          signedAt: values.signedAt?.toISOString(),
        }))
        success = 'Agreement updated.'
      } else if (editor === 'deposit') {
        await mutation.run(() => api.put(`/api/enquiries/${enquiryId}/booking-deposit`, {
          amount: values.amount,
          status: values.status,
          paidOn: values.paidOn?.format('YYYY-MM-DD'),
          paymentMethod: values.paymentMethod,
          paymentReference: values.paymentReference,
        }))
        success = 'Booking deposit updated.'
      } else if (editor === 'convert') {
        const project = await mutation.run(() => api.post(`/api/enquiries/${enquiryId}/convert`, {
          projectName: values.projectName,
          partnerClientId: values.partnerClientId,
          weddingDate: values.weddingDate.format('YYYY-MM-DD'),
          venue: values.venue,
        }))
        message.success('Enquiry converted to a wedding project.')
        navigate(`/app/projects/${project.id}`)
        return
      }
      message.success(success)
      setEditor(null)
      refresh()
    } catch {
      // ResourceFormModal renders the API error.
    }
  }

  const loadError = detailQuery.error || followUpsQuery.error || employeesQuery.error
    || clientsQuery.error || agreementQuery.error || depositQuery.error
  if (loadError) return <ApiErrorAlert error={loadError} onRetry={refresh} />

  if (!detail || detailQuery.loading) {
    return <section className="wams-panel"><Skeleton active /></section>
  }

  const qualificationReady = detail.assignedLeadPlannerId
    && detail.agreementStatus === 'SIGNED'
    && detail.bookingDepositStatus === 'PAID'
    && !terminal

  const followUpColumns = [
    { title: 'Contacted', dataIndex: 'occurredAt', render: formatDateTime, width: 190 },
    { title: 'Notes', dataIndex: 'notes' },
    { title: 'Next follow-up', dataIndex: 'nextFollowUpDate', render: formatDate, width: 150, responsive: ['md'] },
    { title: 'Recorded by', dataIndex: 'recordedByName', width: 180, responsive: ['lg'] },
  ]

  return (
    <>
      <PageHeader
        eyebrow={`Enquiry #${detail.id}`}
        title={detail.contactName}
        subtitle={`${detail.email} · ${detail.phone}`}
        extra={<Link to="/app/enquiries"><Button icon={<ArrowLeftOutlined />}>Back to enquiries</Button></Link>}
      />
      <div className="wams-stack">
        <section className="wams-panel">
          <div className="wams-resource-toolbar">
            <h2 className="wams-panel__title">Overview</h2>
            <div className="wams-inline-actions">
              {isAdmin && <Button icon={<UserSwitchOutlined />} onClick={() => openEditor('client-link')}>{detail.primaryClientId ? 'Change' : 'Link'} client</Button>}
              {isAdmin && !terminal && <Button icon={<UserSwitchOutlined />} onClick={() => openEditor('assignment')}>{detail.assignedLeadPlannerId ? 'Reassign' : 'Assign'} planner</Button>}
              {detail.allowedStatusTransitions?.length > 0 && <Button onClick={() => openEditor('status')}>Change status</Button>}
            </div>
          </div>
          <Descriptions
            column={{ xs: 1, sm: 2, lg: 3 }}
            items={[
              { key: 'status', label: 'Status', children: <StatusTag value={detail.status} /> },
              { key: 'source', label: 'Source', children: humanizeEnum(detail.source) },
              { key: 'planner', label: 'Assigned planner', children: detail.assignedLeadPlannerName || 'Unassigned' },
              {
                key: 'client',
                label: 'Associated client',
                children: detail.primaryClientId
                  ? `${detail.primaryClientName} · ${detail.primaryClientEmail}`
                  : 'Unlinked — conversion will try the normalized enquiry email',
              },
              { key: 'linkSource', label: 'Client link source', children: detail.clientLinkSource ? humanizeEnum(detail.clientLinkSource) : 'Not linked' },
              { key: 'dates', label: 'Wedding dates', children: `${formatDate(detail.weddingStartDate)}${detail.weddingEndDate ? ` – ${formatDate(detail.weddingEndDate)}` : ''}` },
              { key: 'destination', label: 'Destination', children: detail.destination || '—' },
              { key: 'guests', label: 'Estimated guests', children: detail.estimatedGuestCount ?? '—' },
              { key: 'budget', label: 'Budget range', children: `${formatINR(detail.budgetMin, { whole: true })} – ${formatINR(detail.budgetMax, { whole: true })}` },
              { key: 'consultation', label: 'Consultation request', children: formatDate(detail.preferredConsultationDate) },
              { key: 'updated', label: 'Updated', children: formatDateTime(detail.updatedAt) },
            ]}
          />
        </section>

        <section className="wams-panel">
          <h2 className="wams-panel__title">Requirements</h2>
          <Descriptions
            column={{ xs: 1, lg: 2 }}
            items={[
              { key: 'services', label: 'Services', children: detail.servicesOfInterest || '—' },
              { key: 'theme', label: 'Theme / style', children: detail.themeStyle || '—' },
              { key: 'functions', label: 'Functions', children: detail.functionsRequired || '—' },
              { key: 'requests', label: 'Special requests', children: detail.specialRequests || '—' },
              { key: 'message', label: 'Message', children: detail.message || '—' },
            ]}
          />
        </section>

        <section className="wams-panel">
          <div className="wams-resource-toolbar">
            <h2 className="wams-panel__title">Follow-ups</h2>
            {!terminal && <Button icon={<PlusOutlined />} onClick={() => openEditor('follow-up')}>Record follow-up</Button>}
          </div>
          <Table rowKey="id" size="small" loading={followUpsQuery.loading} dataSource={followUpsQuery.data?.content ?? []} columns={followUpColumns} pagination={false} scroll={{ x: 700 }} />
        </section>

        <section className="wams-panel">
          <div className="wams-resource-toolbar">
            <h2 className="wams-panel__title">Package and agreed amount</h2>
            {!terminal && detail.agreementStatus !== 'SIGNED' && <Button icon={<EditOutlined />} onClick={() => openEditor('commercial')}>{detail.selectedPackage ? 'Edit terms' : 'Record terms'}</Button>}
          </div>
          <Descriptions
            column={{ xs: 1, md: 2 }}
            items={[
              { key: 'package', label: 'Selected package', children: detail.selectedPackage || 'Not recorded' },
              { key: 'amount', label: 'Agreed amount', children: formatINR(detail.agreedAmount) },
              { key: 'description', label: 'Customization', children: detail.packageDescription || '—' },
            ]}
          />
        </section>

        <section className="wams-panel">
          <h2 className="wams-panel__title">Booking qualification</h2>
          <div className="wams-detail-grid">
            <div className="wams-qualification-status__item">
              <span>Agreement</span><StatusTag value={detail.agreementStatus} />
              {agreement && <p className="wams-resource-summary">{agreement.agreementReference}{agreement.signedAt ? ` · ${formatDateTime(agreement.signedAt)}` : ''}</p>}
              {!terminal && detail.agreementStatus !== 'VOIDED' && <Button size="small" onClick={() => openEditor('agreement')}>{agreement ? 'Update agreement' : 'Record agreement'}</Button>}
            </div>
            <div className="wams-qualification-status__item">
              <span>Booking deposit</span><StatusTag value={detail.bookingDepositStatus} />
              {deposit && <p className="wams-resource-summary">{formatINR(deposit.amount)}{deposit.paymentReference ? ` · ${deposit.paymentReference}` : ''}</p>}
              {!terminal && detail.bookingDepositStatus !== 'REFUNDED' && <Button size="small" onClick={() => openEditor('deposit')}>{deposit ? 'Update deposit' : 'Record deposit'}</Button>}
            </div>
          </div>
          {detail.convertedProjectId ? (
            <Link to={`/app/projects/${detail.convertedProjectId}`}><Button type="primary" style={{ marginTop: 16 }}>Open project #{detail.convertedProjectId}</Button></Link>
          ) : (
            <Button type="primary" icon={<RocketOutlined />} style={{ marginTop: 16 }} disabled={!qualificationReady} onClick={() => openEditor('convert')}>
              Convert to wedding project
            </Button>
          )}
          {!detail.convertedProjectId && !qualificationReady && <p className="wams-resource-summary">Assign a planner and record a signed agreement and paid deposit to enable conversion.</p>}
        </section>
      </div>

      <ResourceFormModal
        open={Boolean(editor)}
        title={editorTitle(editor)}
        okText={editor === 'convert' ? 'Convert enquiry' : 'Save'}
        initialValues={editorInitialValues}
        loading={mutation.loading}
        error={mutation.error}
        onSubmit={submit}
        onCancel={() => setEditor(null)}
      >
        {editor === 'client-link' && <><Form.Item label="Primary client" name="primaryClientId" rules={[{ required: true }]}><Select showSearch optionFilterProp="label" options={primaryClientOptions} /></Form.Item><Form.Item name="expectedCurrentPrimaryClientId" hidden><InputNumber /></Form.Item><p className="wams-resource-summary">Only active PRIMARY_CLIENT profiles are listed. Changing a converted enquiry updates its project in the same transaction and does not change partner access.</p></>}
        {editor === 'assignment' && <Form.Item label="Lead planner" name="assignedLeadPlannerId" rules={[{ required: true }]}><Select showSearch optionFilterProp="label" options={plannerOptions} /></Form.Item>}
        {editor === 'status' && <Form.Item label="Next status" name="status" rules={[{ required: true }]}><Select options={(detail.allowedStatusTransitions ?? []).map((value) => ({ value, label: humanizeEnum(value) }))} /></Form.Item>}
        {editor === 'follow-up' && <><Form.Item label="Contacted at" name="occurredAt" rules={[{ required: true }]}><DatePicker showTime style={{ width: '100%' }} disabledDate={futureDate} /></Form.Item><Form.Item label="Notes" name="notes" rules={[{ required: true }, { max: 2000 }]}><Input.TextArea rows={4} maxLength={2000} showCount /></Form.Item><Form.Item label="Next follow-up date" name="nextFollowUpDate"><DatePicker style={{ width: '100%' }} disabledDate={pastDate} /></Form.Item></>}
        {editor === 'commercial' && <><Form.Item label="Selected package" name="selectedPackage" rules={[{ required: true }, { max: 200 }]}><Input /></Form.Item><Form.Item label="Customization summary" name="packageDescription" rules={[{ max: 1000 }]}><Input.TextArea rows={3} maxLength={1000} /></Form.Item><Form.Item label="Agreed amount (INR)" name="agreedAmount" rules={[{ required: true }]}><InputNumber min={0.01} precision={2} prefix="₹" style={{ width: '100%' }} /></Form.Item></>}
        {editor === 'agreement' && <><Form.Item label="Agreement reference" name="agreementReference" rules={[{ required: true }, { max: 100 }]}><Input /></Form.Item><Form.Item label="Status" name="status" rules={[{ required: true }]}><Select options={agreementStatusOptions.map((value) => ({ value, label: humanizeEnum(value) }))} /></Form.Item><Form.Item label="Signed by" name="signedByName" dependencies={['status']} rules={[({ getFieldValue }) => ({ validator(_, value) { return getFieldValue('status') !== 'SIGNED' || value ? Promise.resolve() : Promise.reject(new Error('Signed by is required for SIGNED status.')) } })]}><Input /></Form.Item><Form.Item label="Signed at" name="signedAt" dependencies={['status']} rules={[({ getFieldValue }) => ({ validator(_, value) { return getFieldValue('status') !== 'SIGNED' || value ? Promise.resolve() : Promise.reject(new Error('Signed date is required for SIGNED status.')) } })]}><DatePicker showTime style={{ width: '100%' }} disabledDate={futureDate} /></Form.Item></>}
        {editor === 'deposit' && <><Form.Item label="Amount (INR)" name="amount" rules={[{ required: true }]}><InputNumber min={0.01} precision={2} prefix="₹" style={{ width: '100%' }} /></Form.Item><Form.Item label="Status" name="status" rules={[{ required: true }]}><Select options={depositStatusOptions.map((value) => ({ value, label: humanizeEnum(value) }))} /></Form.Item><Form.Item label="Payment date" name="paidOn" dependencies={['status']} rules={[({ getFieldValue }) => ({ validator(_, value) { return getFieldValue('status') !== 'PAID' || value ? Promise.resolve() : Promise.reject(new Error('Payment date is required for PAID status.')) } })]}><DatePicker style={{ width: '100%' }} disabledDate={futureDate} /></Form.Item><Form.Item label="Payment method" name="paymentMethod" dependencies={['status']} rules={[({ getFieldValue }) => ({ validator(_, value) { return getFieldValue('status') !== 'PAID' || value ? Promise.resolve() : Promise.reject(new Error('Payment method is required for PAID status.')) } })]}><Select allowClear options={PAYMENT_METHODS.map((value) => ({ value, label: humanizeEnum(value) }))} /></Form.Item><Form.Item label="Payment reference" name="paymentReference" dependencies={['status']} rules={[({ getFieldValue }) => ({ validator(_, value) { return getFieldValue('status') !== 'PAID' || value ? Promise.resolve() : Promise.reject(new Error('Payment reference is required for PAID status.')) } }), { max: 100 }]}><Input /></Form.Item></>}
        {editor === 'convert' && <><Form.Item label="Project name" name="projectName" rules={[{ required: true }, { max: 200 }]}><Input /></Form.Item><Form.Item label="Partner client" name="partnerClientId"><Select allowClear showSearch optionFilterProp="label" options={partnerClientOptions} /></Form.Item><Form.Item label="Wedding date" name="weddingDate" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} disabledDate={pastDate} /></Form.Item><Form.Item label="Venue" name="venue" rules={[{ max: 300 }]}><Input /></Form.Item><p className="wams-resource-summary">Primary client: {detail.primaryClientName || `automatic match for ${detail.email}`}. Lead planner: {detail.assignedLeadPlannerName}. The backend verifies client identity, agreement and deposit in one transaction.</p></>}
      </ResourceFormModal>
    </>
  )
}

function editorTitle(editor) {
  return {
    'client-link': 'Link primary client',
    assignment: 'Assign lead planner',
    status: 'Change enquiry status',
    'follow-up': 'Record follow-up',
    commercial: 'Package and agreed amount',
    agreement: 'Agreement details',
    deposit: 'Booking deposit',
    convert: 'Convert to wedding project',
  }[editor] || 'Update enquiry'
}
