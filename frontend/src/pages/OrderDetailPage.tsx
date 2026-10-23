import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cancelOrderApi, getOrderDetailApi } from '../api/orders.api'
import type { OrderResponse, OrderStatus } from '../types/commerce'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatCurrency } from '../utils/formatCurrency'

type OrderDetailState =
  | { key: string; status: 'success'; order: OrderResponse }
  | { key: string; status: 'error'; error: string }

function canCancelOrder(status: OrderStatus): boolean {
  return status === 'PENDING' || status === 'CONFIRMED'
}

function formatAddress(order: OrderResponse): string {
  return [
    order.shippingAddressLine,
    order.shippingWard,
    order.shippingDistrict,
    order.shippingCity,
  ]
    .filter((part): part is string => Boolean(part))
    .join(', ')
}

export function OrderDetailPage() {
  const { orderId } = useParams()
  const toast = useToast()
  const id = Number(orderId)
  const isValidId = Number.isInteger(id) && id > 0
  const [detailState, setDetailState] = useState<OrderDetailState | null>(null)
  const [isCancelling, setIsCancelling] = useState(false)
  const [reloadVersion, setReloadVersion] = useState(0)
  const requestKey = `${id}:${reloadVersion}`
  const currentDetailState = detailState?.key === requestKey ? detailState : null
  const order = currentDetailState?.status === 'success' ? currentDetailState.order : null
  const loadError = currentDetailState?.status === 'error' ? currentDetailState.error : null
  const isLoading = currentDetailState === null

  useEffect(() => {
    if (!isValidId) {
      return
    }

    let isCurrent = true

    getOrderDetailApi(id)
      .then((loadedOrder) => {
        if (isCurrent) {
          setDetailState({ key: requestKey, status: 'success', order: loadedOrder })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setDetailState({ key: requestKey, status: 'error', error: getErrorMessage(error) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [id, isValidId, reloadVersion, requestKey])

  async function handleCancel() {
    if (!order || !canCancelOrder(order.status)) {
      return
    }
    if (!window.confirm(`Cancel order #${order.id}?`)) {
      return
    }

    setIsCancelling(true)
    try {
      const cancelledOrder = await cancelOrderApi(order.id)
      setDetailState({ key: requestKey, status: 'success', order: cancelledOrder })
      toast.success(`Order #${order.id} cancelled.`)

      try {
        const refreshedOrder = await getOrderDetailApi(order.id)
        setDetailState({ key: requestKey, status: 'success', order: refreshedOrder })
      } catch (refreshError) {
        toast.error(`Cancellation succeeded, but the order could not be refreshed: ${getErrorMessage(refreshError)}`)
      }
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsCancelling(false)
    }
  }

  if (!isValidId) {
    return (
      <main className="customer-page">
        <section className="customer-panel">
          <h1>Order not found</h1>
          <p className="form-error" role="alert">The order ID is invalid.</p>
          <Link className="text-link" to="/my-orders">Back to my orders</Link>
        </section>
      </main>
    )
  }

  if (isLoading) {
    return <main className="customer-page"><p className="loading-state" role="status">Loading order…</p></main>
  }

  if (loadError) {
    return (
      <main className="customer-page">
        <section className="customer-panel">
          <h1>Unable to load order</h1>
          <p className="form-error" role="alert">{loadError}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
          <Link className="text-link" to="/my-orders">Back to my orders</Link>
        </section>
      </main>
    )
  }

  if (!order) return null

  return (
    <main className="customer-page">
      <Link className="text-link" to="/my-orders">← Back to my orders</Link>
      <header className="customer-page-header order-detail-header">
        <div>
          <p className="eyebrow">Order #{order.id}</p>
          <h1>Order details</h1>
          <p className="muted-text">Placed {new Date(order.createdAt).toLocaleString()}</p>
        </div>
        <span className={`status-badge status-${order.status.toLowerCase()}`}>
          {order.status}
        </span>
      </header>

      <div className="order-detail-grid">
        <div className="order-detail-main">
          <section className="customer-panel">
            <h2>Items</h2>
            <ul className="detail-items">
              {order.items.map((item) => (
                <li key={item.id}>
                  <div>
                    <strong>{item.productName}</strong>
                    <span>SKU: {item.sku} · Quantity: {item.quantity}</span>
                  </div>
                  <span>{formatCurrency(item.lineTotal)}</span>
                </li>
              ))}
            </ul>
          </section>
          <section className="customer-panel">
            <h2>Shipping</h2>
            <p><strong>{order.receiverName}</strong></p>
            <p>{order.receiverPhone}</p>
            <p>{formatAddress(order)}</p>
          </section>
        </div>

        <aside className="customer-panel order-totals">
          <h2>Summary</h2>
          <div><span>Subtotal</span><span>{formatCurrency(order.subtotal)}</span></div>
          <div><span>Discount</span><span>− {formatCurrency(order.discountAmount)}</span></div>
          <div><span>Shipping</span><span>{formatCurrency(order.shippingFee)}</span></div>
          <div className="summary-total"><strong>Total</strong><strong>{formatCurrency(order.totalAmount)}</strong></div>
          <div><span>Payment</span><span>{order.payment?.method ?? '—'}</span></div>
          {order.voucherCode && <div><span>Voucher</span><span>{order.voucherCode}</span></div>}
          {order.note && <p className="order-note">Note: {order.note}</p>}
          {canCancelOrder(order.status) && (
            <button
              className="button button-danger button-full"
              disabled={isCancelling}
              onClick={() => void handleCancel()}
              type="button"
            >
              {isCancelling ? 'Cancelling…' : 'Cancel order'}
            </button>
          )}
        </aside>
      </div>
    </main>
  )
}
