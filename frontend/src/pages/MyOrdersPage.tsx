import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listMyOrdersApi } from '../api/orders.api'
import type { OrderStatus, OrderSummaryResponse } from '../types/commerce'
import type { PageResponse } from '../types/api'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatCurrency } from '../utils/formatCurrency'

const orderStatuses: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'SHIPPING',
  'DELIVERED',
  'CANCELLED',
]

interface OrdersResult {
  key: string
  data?: PageResponse<OrderSummaryResponse>
  error?: string
}

export function MyOrdersPage() {
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<OrdersResult | null>(null)
  const size = 10
  const requestKey = `${status}:${pageNumber}:${reloadVersion}`
  const pageData = result?.key === requestKey ? result.data ?? null : null
  const pageError = result?.key === requestKey ? result.error ?? null : null
  const isLoading = result?.key !== requestKey

  useEffect(() => {
    let isCurrent = true

    listMyOrdersApi({
      page: pageNumber,
      size,
      ...(status ? { status } : {}),
    })
      .then((data) => {
        if (isCurrent) {
          setResult({ key: requestKey, data })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setResult({ key: requestKey, error: getErrorMessage(error) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [pageNumber, requestKey, size, status])

  function handleStatusChange(value: string) {
    setStatus(orderStatuses.find((orderStatus) => orderStatus === value) ?? '')
    setPageNumber(0)
  }

  return (
    <main className="customer-page">
      <header className="customer-page-header">
        <div>
          <p className="eyebrow">Customer</p>
          <h1>My orders</h1>
        </div>
        <div className="customer-page-actions">
          <Link className="text-link" to="/my-reviews">My reviews</Link>
          <Link className="text-link" to="/wishlist">Wishlist</Link>
          <label className="filter-control">
            Filter by status
            <select onChange={(event) => handleStatusChange(event.target.value)} value={status}>
              <option value="">All orders</option>
              {orderStatuses.map((orderStatus) => (
                <option key={orderStatus} value={orderStatus}>{orderStatus}</option>
              ))}
            </select>
          </label>
        </div>
      </header>

      {isLoading && <p className="loading-state" role="status">Loading orders…</p>}

      {pageError && (
        <section className="customer-panel">
          <p className="form-error" role="alert">{pageError}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
        </section>
      )}

      {!isLoading && !pageError && pageData?.content.length === 0 && (
        <section className="customer-panel empty-orders">
          <h2>No orders found</h2>
          <p className="page-intro">
            {status ? `There are no ${status.toLowerCase()} orders.` : 'Your orders will appear here after checkout.'}
          </p>
          <Link className="button button-primary button-link" to="/">Continue shopping</Link>
        </section>
      )}

      {!isLoading && !pageError && pageData && pageData.content.length > 0 && (
        <>
          <div className="order-list">
            {pageData.content.map((order) => (
              <article className="customer-panel order-card" key={order.id}>
                <div className="order-card-main">
                  <div>
                    <p className="eyebrow">Order #{order.id}</p>
                    <h2>{order.firstItemName ?? 'Order items'}</h2>
                    <p className="muted-text">
                      {new Date(order.createdAt).toLocaleString()} · {order.itemCount} items
                    </p>
                  </div>
                  <span className={`status-badge status-${order.status.toLowerCase()}`}>
                    {order.status}
                  </span>
                </div>
                <div className="order-card-footer">
                  <div>
                    <span className="muted-text">Total</span>
                    <strong>{formatCurrency(order.totalAmount)}</strong>
                  </div>
                  <Link className="button button-secondary button-link" to={`/my-orders/${order.id}`}>
                    View details
                  </Link>
                </div>
              </article>
            ))}
          </div>

          <nav aria-label="Order pages" className="pagination">
            <button
              className="button button-secondary"
              disabled={pageData.first}
              onClick={() => setPageNumber((current) => Math.max(0, current - 1))}
              type="button"
            >
              Previous
            </button>
            <span>Page {pageData.page + 1} of {Math.max(pageData.totalPages, 1)}</span>
            <button
              className="button button-secondary"
              disabled={pageData.last}
              onClick={() => setPageNumber((current) => current + 1)}
              type="button"
            >
              Next
            </button>
          </nav>
        </>
      )}
    </main>
  )
}
