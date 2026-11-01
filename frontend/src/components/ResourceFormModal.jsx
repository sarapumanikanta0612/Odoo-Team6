import { Form, Modal } from 'antd'
import ApiErrorAlert from './ApiErrorAlert'

/** Shared modal frame for create/edit forms; domain fields remain feature-local. */
export default function ResourceFormModal(props) {
  // Do not create a Form instance while a destroy-on-close Modal has no content;
  // Ant Design correctly warns that such an instance is disconnected.
  return props.open ? <OpenResourceFormModal {...props} /> : null
}

function OpenResourceFormModal({
  title,
  okText = 'Save',
  initialValues,
  loading,
  error,
  onSubmit,
  onCancel,
  children,
}) {
  const [form] = Form.useForm()

  return (
    <Modal
      open
      title={title}
      okText={okText}
      confirmLoading={loading}
      onOk={() => form.submit()}
      onCancel={onCancel}
      destroyOnHidden
      mask={{ closable: !loading }}
    >
      {error && <ApiErrorAlert error={error} style={{ marginBottom: 16 }} />}
      <Form
        form={form}
        initialValues={initialValues}
        layout="vertical"
        requiredMark={false}
        disabled={loading}
        scrollToFirstError={{ focus: true }}
        onFinish={onSubmit}
      >
        {children}
      </Form>
    </Modal>
  )
}
