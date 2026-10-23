import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { confirmPaymentApi, failPaymentApi, getPaymentApi } from '../api/payment.api'
import { getOrderDetailApi } from '../api/orders.api'
import type { OrderResponse, PaymentResponse } from '../types/commerce'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

interface PaymentState {
  payment: PaymentResponse
  order: OrderResponse
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
    Promise.all([getPaymentApi(id), getOrderDetailApi(id)])
      .then(([payment, order]) => {
        if (isCurrent) {
          setLoadState({ key: requestKey, status: 'success', result: { payment, order } })
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
    if (!result || result.order.status !== 'PENDING' || result.payment.status === 'PAID') {
      return
    }
    setIsWorking(true)
    try {
      const payment = action === 'confirm'
        ? await confirmPaymentApi(id)
        : await failPaymentApi(id)
      setLoadState({ key: requestKey, status: 'success', result: { ...result, payment } })
      toast.success(action === 'confirm' ? 'Simulated payment confirmed.' : 'Simulated payment marked as failed.')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setIsWorking(false)
    }
  }

  return (
    <main className="customer-page payment-page">
      <Link className="text-link" to={`/my-orders/${id}`}>← Back to order details</Link>
      <section className="customer-panel payment-panel">
        <p className="eyebrow">Online payment · simulation</p>
        <h1>Payment for order #{isValidId ? id : '—'}</h1>
        {!isValidId && <p className="form-error" role="alert">The order ID is invalid.</p>}
        {isLoading && <p className="loading-state" role="status">Loading payment…</p>}
        {error && (
          <>
            <p className="form-error" role="alert">{error}</p>
            <button
              className="button button-secondary"
              onClick={() => setReloadVersion((version) => version + 1)}
              type="button"
            >
              Retry
            </button>
          </>
        )}
        {!isLoading && !error && result && (
          <>
            <p className="payment-simulation-note">
              This is a simulated payment flow. It does not charge a card or contact a payment provider.
            </p>
            <dl className="payment-details">
              <div><dt>Amount</dt><dd>{formatCurrency(result.payment.amount)}</dd></div>
              <div><dt>Method</dt><dd>{result.payment.method}</dd></div>
              <div><dt>Payment status</dt><dd>{result.payment.status}</dd></div>
              <div><dt>Order status</dt><dd>{result.order.status}</dd></div>
              <div><dt>Transaction reference</dt><dd>{result.payment.transactionRef ?? 'Not assigned'}</dd></div>
            </dl>
            {result.order.status === 'PENDING' && result.payment.status !== 'PAID' ? (
              <div className="customer-review-actions">
                <button
                  className="button button-primary"
                  disabled={isWorking}
                  onClick={() => void processPayment('confirm')}
                  type="button"
                >
                  {isWorking ? 'Processing…' : 'Simulate successful payment'}
                </button>
                <button
                  className="button button-secondary"
                  disabled={isWorking}
                  onClick={() => void processPayment('fail')}
                  type="button"
                >
                  Simulate failed payment
                </button>
              </div>
            ) : (
              <p className="muted-text">
                {result.payment.status === 'PAID'
                  ? 'Payment is complete.'
                  : 'This order is no longer awaiting payment.'}
              </p>
            )}
            <Link className="text-link payment-order-link" to={`/my-orders/${id}`}>
              View order details
            </Link>
          </>
        )}
      </section>
    </main>
  )
}
