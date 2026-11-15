import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { verifyVnPayReturnApi, type VnPayReturnResponse } from '../api/payment.api'
import { getErrorMessage } from '../utils/getErrorMessage'

type ReturnState =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'success'; result: VnPayReturnResponse }

export function VnPayReturnPage() {
  const { search } = useLocation()
  const [state, setState] = useState<ReturnState>({ status: 'loading' })

  useEffect(() => {
    let active = true
    if (!new URLSearchParams(search).has('vnp_SecureHash')) {
      return
    }
    verifyVnPayReturnApi(search)
      .then((result) => {
        if (active) setState({ status: 'success', result })
      })
      .catch((error: unknown) => {
        if (active) setState({ status: 'error', error: getErrorMessage(error) })
      })
    return () => { active = false }
  }, [search])

  const missingSignature = !new URLSearchParams(search).has('vnp_SecureHash')
  const result = state.status === 'success' ? state.result : null
  const validOrderId = result?.orderId != null && Number.isSafeInteger(result.orderId)
    && result.orderId > 0

  return (
    <main className="customer-page payment-page">
      <section className="customer-panel payment-panel">
        <p className="eyebrow">Kết quả từ VNPay</p>
        <h1>Xác minh thanh toán</h1>
        {missingSignature && (
          <p className="form-error" role="alert">Liên kết không chứa chữ ký của VNPay.</p>
        )}
        {!missingSignature && state.status === 'loading' && (
          <p className="loading-state" role="status">Đang xác minh giao dịch với máy chủ...</p>
        )}
        {state.status === 'error' && <p className="form-error" role="alert">{state.error}</p>}
        {result && (
          result.code === '97' || result.code === '01' || result.code === '04' ? (
            <p className="form-error" role="alert">
              Không xác minh được giao dịch. {result.message}
            </p>
          ) : result.paid ? (
            <p role="status">Thanh toán đã được xác nhận thành công.</p>
          ) : (
            <p role="status">
              Chưa ghi nhận thanh toán thành công. Kiểm tra lại trạng thái đơn hàng trước khi thử lại.
            </p>
          )
        )}
        {validOrderId && (
          <Link className="button button-primary" to={`/my-orders/${result!.orderId}`}>
            Xem đơn hàng
          </Link>
        )}
        <Link className="text-link" to="/my-orders">Quay lại đơn hàng của tôi</Link>
      </section>
    </main>
  )
}
