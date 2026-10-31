import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAdminOrderDetailApi, updateAdminOrderStatusApi } from '../api/admin-orders.api'
import type { AdminOrderResponse } from '../types/commerce'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

interface AdminOrderDetailState {
  key: string
  data?: AdminOrderResponse
  error?: string
}

const nextStatuses: Record<AdminOrderResponse['order']['status'], AdminOrderResponse['order']['status'][]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SHIPPING', 'CANCELLED'],
  SHIPPING: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
}

export function AdminOrderDetailPage() {
  const { orderId } = useParams()
  const toast = useToast()
  const id = Number(orderId)
  const isValidId = Number.isSafeInteger(id) && id > 0
  const [result, setResult] = useState<AdminOrderDetailState | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [isUpdating, setIsUpdating] = useState(false)
  const requestKey = `${id}:${reloadVersion}`
  const currentResult = result?.key === requestKey ? result : null
  const data = currentResult?.data ?? null
  const error = currentResult?.error ?? null
  const isLoading = isValidId && currentResult === null

  useEffect(() => {
    if (!isValidId) return

    let isCurrent = true
    getAdminOrderDetailApi(id)
      .then((order) => {
        if (isCurrent) setResult({ key: requestKey, data: order })
      })
      .catch((requestError: unknown) => {
        if (isCurrent) setResult({ key: requestKey, error: getErrorMessage(requestError) })
      })
    return () => {
      isCurrent = false
    }
  }, [id, isValidId, requestKey])

  async function updateStatus(nextStatus: AdminOrderResponse['order']['status']) {
    if (!data || !nextStatuses[data.order.status].includes(nextStatus)) return

    if (nextStatus === 'CANCELLED' && !window.confirm('Cancel this order? Stock and voucher usage will be restored.')) {
      return
    }

    setIsUpdating(true)
    try {
      const order = await updateAdminOrderStatusApi(id, nextStatus)
      setResult({ key: requestKey, data: { ...data, order } })
      toast.success(`Order #${id} updated to ${nextStatus}.`)
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setIsUpdating(false)
    }
  }

  return (
    <section className="page-section admin-order-detail-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Admin · Order management</p>
          <h1>Order #{isValidId ? id : '—'}</h1>
        </div>
        <Link className="button button-secondary button-link" to="/admin/orders">Back to orders</Link>
      </header>

      {!isValidId && <p className="form-error" role="alert">The order ID is invalid.</p>}
      {isLoading && <p className="loading-state" role="status">Loading order details…</p>}
      {error && (
        <div className="form-error" role="alert">
          {error}
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
        </div>
      )}
      {data && (
        <>
          <section className="customer-panel admin-order-summary">
            <div className="section-heading">
              <div>
                <span className={`status-badge status-${data.order.status.toLowerCase()}`}>
                  {data.order.status}
                </span>
                <p className="muted-text">Placed {new Date(data.order.createdAt).toLocaleString()}</p>
              </div>
              <div className="admin-order-status-actions">
                {nextStatuses[data.order.status].map((nextStatus) => (
                  <button
                    className={nextStatus === 'CANCELLED' ? 'button button-secondary text-button-danger' : 'button button-primary'}
                    disabled={isUpdating}
                    key={nextStatus}
                    onClick={() => void updateStatus(nextStatus)}
                    type="button"
                  >
                    {isUpdating ? 'Updating…' : `Set ${nextStatus}`}
                  </button>
                ))}
              </div>
            </div>
            <dl className="payment-details">
              <div><dt>Customer</dt><dd>{data.customerName}</dd></div>
              <div><dt>Email</dt><dd>{data.customerEmail}</dd></div>
              <div><dt>Customer ID</dt><dd>{data.customerId}</dd></div>
              <div><dt>Recipient</dt><dd>{data.order.receiverName}</dd></div>
              <div><dt>Phone</dt><dd>{data.order.receiverPhone}</dd></div>
              <div>
                <dt>Shipping address</dt>
                <dd>
                  {[data.order.shippingAddressLine, data.order.shippingWard, data.order.shippingDistrict, data.order.shippingCity]
                    .filter(Boolean)
                    .join(', ')}
                </dd>
              </div>
              <div><dt>Payment</dt><dd>{data.order.payment?.method ?? '—'} · {data.order.payment?.status ?? '—'}</dd></div>
            </dl>
          </section>

          <section className="customer-panel admin-order-items">
            <h2>Items</h2>
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>Product</th><th>SKU</th><th>Unit price</th><th>Quantity</th><th>Line total</th></tr>
                </thead>
                <tbody>
                  {data.order.items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.productName}</td>
                      <td>{item.sku}</td>
                      <td>{formatCurrency(item.unitPrice)}</td>
                      <td>{item.quantity}</td>
                      <td>{formatCurrency(item.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <dl className="admin-order-totals">
              <div><dt>Subtotal</dt><dd>{formatCurrency(data.order.subtotal)}</dd></div>
              <div><dt>Shipping</dt><dd>{formatCurrency(data.order.shippingFee)}</dd></div>
              <div><dt>Discount{data.order.voucherCode ? ` · ${data.order.voucherCode}` : ''}</dt><dd>−{formatCurrency(data.order.discountAmount)}</dd></div>
              <div><dt>Total</dt><dd>{formatCurrency(data.order.totalAmount)}</dd></div>
            </dl>
            {data.order.note && <p><strong>Customer note:</strong> {data.order.note}</p>}
          </section>
        </>
      )}
    </section>
  )
}
