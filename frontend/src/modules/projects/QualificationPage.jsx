import { Navigate, useParams } from 'react-router'

/** Backward-compatible alias: enquiry qualification now lives in Enquiries. */
export default function QualificationPage() {
  const { enquiryId } = useParams()
  const id = Number(enquiryId)
  const target = Number.isSafeInteger(id) && id > 0 ? `/app/enquiries/${id}` : '/app/enquiries'
  return <Navigate to={target} replace />
}
