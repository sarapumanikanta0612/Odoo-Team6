import { ArrowLeftOutlined, CheckCircleOutlined, LoginOutlined } from '@ant-design/icons'
import { App, Button, DatePicker, Form, Input, InputNumber, Result, Select, Space } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import ApiErrorAlert from '../../components/ApiErrorAlert'
import { TempleMark } from '../../components/TempleArt'
import { useApiMutation } from '../../hooks/useApiMutation'
import { api } from '../../services/apiClient'
import { formatDateTime } from '../../utils/format'

const PHONE_PATTERN = /^\+?[0-9]([ -]?[0-9]){6,14}$/
const today = () => dayjs().startOf('day')
const notPast = (date) => date && date.startOf('day').isBefore(today())

const SERVICE_OPTIONS = [
  'Complete wedding planning',
  'Venue and function coordination',
  'Decor and production',
  'Vendor coordination',
  'Hospitality',
  'Other',
].map((value) => ({ value, label: value }))

/** Public form backed by POST /api/public/enquiries. */
export default function PublicEnquiryPage() {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const { message } = App.useApp()
  const mutation = useApiMutation()
  const [created, setCreated] = useState(null)

  const submit = async (values) => {
    const [weddingStartDate, weddingEndDate] = values.weddingDates ?? []
    const body = {
      contactName: values.contactName.trim(),
      email: values.email.trim(),
      phone: values.phone.trim(),
      weddingStartDate: weddingStartDate?.format('YYYY-MM-DD'),
      weddingEndDate: weddingEndDate?.format('YYYY-MM-DD'),
      destination: values.destination?.trim() || null,
      estimatedGuestCount: values.estimatedGuestCount,
      budgetMin: values.budgetMin,
      budgetMax: values.budgetMax,
      servicesOfInterest: values.servicesOfInterest?.join(', ') || null,
      themeStyle: values.themeStyle?.trim() || null,
      functionsRequired: values.functionsRequired?.trim() || null,
      specialRequests: values.specialRequests?.trim() || null,
      message: values.message?.trim() || null,
      preferredConsultationDate: values.preferredConsultationDate?.format('YYYY-MM-DD'),
    }
    try {
      const response = await mutation.run(() => api.post('/api/public/enquiries', body, { auth: false }))
      setCreated(response)
      message.success('Your enquiry has been recorded.')
    } catch {
      // ApiErrorAlert renders the server response.
    }
  }

  if (created) {
    return (
      <main className="wams-public-form-page">
        <title>Enquiry received · WAMS</title>
        <div className="wams-public-form">
          <Result
            status="success"
            icon={<CheckCircleOutlined />}
            title="Enquiry received"
            subTitle={
              <>
                Your reference is <strong>#{created.id}</strong>. The agency can use this ID to record your
                consultation, agreement and booking deposit. Submitted {formatDateTime(created.createdAt)}.
              </>
            }
            extra={[
              <Button
                key="again"
                onClick={() => {
                  setCreated(null)
                  mutation.clearError()
                  form.resetFields()
                }}
              >
                Submit another enquiry
              </Button>,
              <Button key="login" type="primary" icon={<LoginOutlined />} onClick={() => navigate('/login')}>
                Staff or client sign in
              </Button>,
            ]}
          />
        </div>
      </main>
    )
  }

  return (
    <main className="wams-public-form-page">
      <title>Submit an enquiry · WAMS</title>
      <div className="wams-public-form">
        <div className="wams-public-form__head">
          <Link to="/" className="wams-back-link">
            <ArrowLeftOutlined /> WAMS overview
          </Link>
          <TempleMark size={38} className="wams-brand__mark" />
          <div className="wams-eyebrow">Consultation request</div>
          <h1>Tell the agency about your wedding</h1>
          <p>This form creates a real enquiry record. It does not confirm a booking or take payment.</p>
        </div>

        {mutation.error && <ApiErrorAlert error={mutation.error} style={{ marginBottom: 20 }} />}

        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
          disabled={mutation.loading}
          scrollToFirstError={{ focus: true }}
          onFinish={submit}
        >
          <div className="wams-form-grid">
            <Form.Item
              label="Contact name"
              name="contactName"
              rules={[
                { required: true, message: 'Enter your name.' },
                { max: 120, message: 'Use 120 characters or fewer.' },
              ]}
            >
              <Input autoComplete="name" />
            </Form.Item>
            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: 'Enter your email.' },
                { type: 'email', message: 'Enter a valid email.' },
                { max: 254 },
              ]}
            >
              <Input type="email" autoComplete="email" />
            </Form.Item>
            <Form.Item
              label="Phone"
              name="phone"
              rules={[
                { required: true, message: 'Enter your phone number.' },
                { pattern: PHONE_PATTERN, message: 'Use 7–15 digits, optionally with +, spaces or hyphens.' },
              ]}
            >
              <Input autoComplete="tel" placeholder="+91 98765 43210" />
            </Form.Item>
            <Form.Item label="Preferred consultation date" name="preferredConsultationDate">
              <DatePicker style={{ width: '100%' }} disabledDate={notPast} format="DD MMM YYYY" />
            </Form.Item>
            <Form.Item label="Wedding dates" name="weddingDates" className="wams-form-grid__wide">
              <DatePicker.RangePicker allowEmpty={[true, true]} style={{ width: '100%' }} disabledDate={notPast} format="DD MMM YYYY" />
            </Form.Item>
            <Form.Item label="Destination or city" name="destination" rules={[{ max: 150 }]}>
              <Input />
            </Form.Item>
            <Form.Item label="Estimated guests" name="estimatedGuestCount">
              <InputNumber min={1} max={100000} precision={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Minimum budget (INR)" name="budgetMin">
              <InputNumber min={0} max={9999999999.99} precision={2} style={{ width: '100%' }} prefix="₹" />
            </Form.Item>
            <Form.Item
              label="Maximum budget (INR)"
              name="budgetMax"
              dependencies={['budgetMin']}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    const minimum = getFieldValue('budgetMin')
                    if (value === undefined || minimum === undefined || Number(value) >= Number(minimum)) {
                      return Promise.resolve()
                    }
                    return Promise.reject(new Error('Maximum budget cannot be below the minimum.'))
                  },
                }),
              ]}
            >
              <InputNumber min={0} max={9999999999.99} precision={2} style={{ width: '100%' }} prefix="₹" />
            </Form.Item>
            <Form.Item label="Services of interest" name="servicesOfInterest" className="wams-form-grid__wide">
              <Select mode="multiple" options={SERVICE_OPTIONS} placeholder="Select any that apply" />
            </Form.Item>
            <Form.Item label="Theme or style" name="themeStyle" rules={[{ max: 200 }]}>
              <Input />
            </Form.Item>
            <Form.Item label="Functions required" name="functionsRequired" rules={[{ max: 500 }]}>
              <Input placeholder="For example: mehendi, sangeet, wedding" />
            </Form.Item>
            <Form.Item label="Special requests" name="specialRequests" className="wams-form-grid__wide" rules={[{ max: 2000 }]}>
              <Input.TextArea rows={3} showCount maxLength={2000} />
            </Form.Item>
            <Form.Item label="Message" name="message" className="wams-form-grid__wide" rules={[{ max: 2000 }]}>
              <Input.TextArea rows={4} showCount maxLength={2000} />
            </Form.Item>
          </div>
          <Space wrap>
            <Button type="primary" htmlType="submit" size="large" loading={mutation.loading}>
              Submit enquiry
            </Button>
            <Button size="large" onClick={() => navigate('/')}>
              Cancel
            </Button>
          </Space>
        </Form>
      </div>
    </main>
  )
}
