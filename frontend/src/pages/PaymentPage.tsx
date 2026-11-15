import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  confirmPaymentApi,
  createVnPayPaymentUrlApi,
  failPaymentApi,
  getPaymentApi,
  getPaymentModesApi,
  type PaymentModesResponse,
} from '../api/payment.api'
import { getOrderDetailApi } from '../api/orders.api'
import type { OrderResponse, PaymentResponse } from '../types/commerce'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'
import { orderStatusLabel, paymentMethodLabel, paymentStatusLabel } from '../utils/localization'

interface PaymentState {
  payment: PaymentResponse
  order: OrderResponse
  modes: PaymentModesResponse
}

type PaymentLoadState =
  | { key: string; status: 'loading' }
  | { key: string; status: 'success'; result: PaymentState }
  | { key: string; status: 'error'; error: string }

export function PaymentPage() {
  const { orderId } = useParams()
  const toast = useToast()
  const id = Number(orderId)
  const isValidId = Number.isInteger(id) && id > 0
  const [loadState, setLoadState] = useState<PaymentLoadState | null>(null)
  const [isWorking, setIsWorking] = useState(false)
  const [reloadVersion, setReloadVersion] = useState(0)
  const requestKey = `${id}:${reloadVersion}`
  const currentLoadState = loadState?.key === requestKey ? loadState : null
  const result = currentLoadState?.status === 'success' ? currentLoadState.result : null
  const error = currentLoadState?.status === 'error' ? currentLoadState.error : null
  const isLoading = isValidId && currentLoadState?.status !== 'success' && currentLoadState?.status !== 'error'

  useEffect(() => {
    if (!isValidId) {
      return
    }
    let isCurrent = true
    Promise.all([getPaymentApi(id), getOrderDetailApi(id), getPaymentModesApi()])
      .then(([payment, order, modes]) => {
        if (isCurrent) {
          setLoadState({ key: requestKey, status: 'success', result: { payment, order, modes } })
        }
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setLoadState({ key: requestKey, status: 'error', error: getErrorMessage(requestError) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [id, isValidId, reloadVersion, requestKey])

  async function processPayment(action: 'confirm' | 'fail') {
    if (!result || result.order.status !== 'PENDING' || result.payment.status === 'PAID'
        || !result.modes.simulationEnabled
        || !result.payment.transactionRef?.startsWith('MOCK-')) {
      return
    }
    setIsWorking(true)
    try {
      const payment = action === 'confirm'
        ? await confirmPaymentApi(id)
        : await failPaymentApi(id)
      setLoadState({ key: requestKey, status: 'success', result: { ...result, payment } })
      toast.success(action === 'confirm' ? 'Đã xác nhận thanh toán mô phỏng.' : 'Đã đánh dấu thanh toán thất bại.')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setIsWorking(false)
    }
  }

  async function goToVnPay() {
    if (!result || !result.modes.vnpayAvailable || isWorking
        || result.payment.status === 'PAID' || result.order.status !== 'PENDING') {
      return
    }
    setIsWorking(true)
    try {
      const paymentUrl = await createVnPayPaymentUrlApi(id)
      window.location.assign(paymentUrl)
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
      setIsWorking(false)
    }
  }

  return (
    <main className="customer-page payment-page">
      <Link className="text-link" to={`/my-orders/${id}`}>← Quay lại chi tiết đơn hàng</Link>
      <section className="customer-panel payment-panel">
        <p className="eyebrow">Thanh toán trực tuyến</p>
        <h1>Thanh toán đơn hàng #{isValidId ? id : '—'}</h1>
        {!isValidId && <p className="form-error" role="alert">Mã đơn hàng không hợp lệ.</p>}
        {isLoading && <p className="loading-state" role="status">Đang tải thông tin thanh toán...</p>}
        {error && (
          <>
            <p className="form-error" role="alert">{error}</p>
            <button
              className="button button-secondary"
              onClick={() => setReloadVersion((version) => version + 1)}
              type="button"
            >
              Thử lại
            </button>
          </>
        )}
        {!isLoading && !error && result && (
          <>
            {result.modes.simulationEnabled && (
              <p className="payment-simulation-note">
                Chế độ demo đang bật: nút mô phỏng không thu tiền thật.
              </p>
            )}
            <dl className="payment-details">
              <div><dt>Số tiền</dt><dd>{formatCurrency(result.payment.amount)}</dd></div>
              <div><dt>Phương thức</dt><dd>{paymentMethodLabel(result.payment.method)}</dd></div>
              <div><dt>Trạng thái thanh toán</dt><dd>{paymentStatusLabel(result.payment.status)}</dd></div>
              <div><dt>Trạng thái đơn hàng</dt><dd>{orderStatusLabel(result.order.status)}</dd></div>
              <div><dt>Mã giao dịch</dt><dd>{result.payment.transactionRef ?? 'Chưa có'}</dd></div>
            </dl>
            {result.order.status === 'PENDING' && result.payment.status !== 'PAID' ? (
              <div className="customer-review-actions">
                {result.modes.vnpayAvailable && (
                  <button
                    className="button button-primary"
                    disabled={isWorking}
                    onClick={() => void goToVnPay()}
                    type="button"
                  >
                    {isWorking ? 'Đang xử lý...' : 'Thanh toán qua VNPay'}
                  </button>
                )}
                {result.modes.simulationEnabled && result.payment.transactionRef?.startsWith('MOCK-') && (
                  <>
                    <button
                      className="button button-secondary"
                      disabled={isWorking}
                      onClick={() => void processPayment('confirm')}
                      type="button"
                    >
                      Mô phỏng thanh toán thành công
                    </button>
                    <button
                      className="button button-secondary"
                      disabled={isWorking}
                      onClick={() => void processPayment('fail')}
                      type="button"
                    >
                      Mô phỏng thanh toán thất bại
                    </button>
                  </>
                )}
                {!result.modes.vnpayAvailable && !result.modes.simulationEnabled && (
                  <p className="form-error" role="status">
                    Chưa cấu hình phương thức thanh toán online. Vui lòng liên hệ quản trị viên.
                  </p>
                )}
              </div>
            ) : (
              <p className="muted-text">
                {result.payment.status === 'PAID'
                  ? 'Đơn hàng đã được thanh toán.'
                  : 'Đơn hàng này không còn chờ thanh toán.'}
              </p>
            )}
            <Link className="text-link payment-order-link" to={`/my-orders/${id}`}>
              Xem chi tiết đơn hàng
            </Link>
          </>
        )}
      </section>
    </main>
  )
}
