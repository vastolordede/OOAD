import { useEffect, useState, type FormEvent } from 'react'
import {
  getAdminOrderApi,
  listAdminOrdersApi,
  updateAdminOrderStatusApi,
} from '../api/admin-orders.api'
import type { PageResponse } from '../types/api'
import type {
  AdminOrderResponse,
  OrderStatus,
  OrderSummaryResponse,
} from '../types/commerce'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

const orderStatuses: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'SHIPPING',
  'DELIVERED',
  'CANCELLED',
]

const orderSortOptions: Filters['sortBy'][] = ['createdAt', 'totalAmount', 'status', 'id']

const statusTransitions: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SHIPPING', 'CANCELLED'],
  SHIPPING: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
}

interface Filters {
  keyword: string
  status: OrderStatus | ''
  fromDate: string
  toDate: string
  sortBy: 'createdAt' | 'totalAmount' | 'status' | 'id'
  direction: 'asc' | 'desc'
}

interface OrderListState {
  key: string
  data?: PageResponse<OrderSummaryResponse>
  error?: string
}

interface OrderDetailState {
  id: number
  data?: AdminOrderResponse
  error?: string
}

export function AdminOrdersPage() {
  const toast = useToast()
  const [draftFilters, setDraftFilters] = useState<Filters>({
    keyword: '',
    status: '',
    fromDate: '',
    toDate: '',
    sortBy: 'createdAt',
    direction: 'desc',
  })
  const [filters, setFilters] = useState<Filters>({
    keyword: '',
    status: '',
    fromDate: '',
    toDate: '',
    sortBy: 'createdAt',
    direction: 'desc',
  })
  const [filterError, setFilterError] = useState<string | null>(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [listResult, setListResult] = useState<OrderListState | null>(null)
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null)
  const [detailResult, setDetailResult] = useState<OrderDetailState | null>(null)
  const [detailReloadVersion, setDetailReloadVersion] = useState(0)
  const [updating, setUpdating] = useState(false)
  const pageSize = 20
  const requestKey = JSON.stringify({ ...filters, pageNumber, reloadVersion })
  const pageData = listResult?.key === requestKey ? listResult.data ?? null : null
  const pageError = listResult?.key === requestKey ? listResult.error ?? null : null
  const isLoading = listResult?.key !== requestKey
  const selectedOrder = detailResult?.id === selectedOrderId ? detailResult.data ?? null : null
  const detailError = detailResult?.id === selectedOrderId ? detailResult.error ?? null : null
  const isDetailLoading = selectedOrderId !== null && detailResult?.id !== selectedOrderId

  useEffect(() => {
    let isCurrent = true
    listAdminOrdersApi({
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.status ? { status: filters.status } : {}),
      ...(filters.fromDate ? { fromDate: filters.fromDate } : {}),
      ...(filters.toDate ? { toDate: filters.toDate } : {}),
      page: pageNumber,
      size: pageSize,
      sortBy: filters.sortBy,
      direction: filters.direction,
    })
      .then((data) => {
        if (isCurrent) {
          setListResult({ key: requestKey, data })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setListResult({ key: requestKey, error: getErrorMessage(error) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [filters, pageNumber, pageSize, requestKey])

  useEffect(() => {
    if (selectedOrderId === null) {
      return
    }

    let isCurrent = true
    getAdminOrderApi(selectedOrderId)
      .then((data) => {
        if (isCurrent) {
          setDetailResult({ id: selectedOrderId, data })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setDetailResult({ id: selectedOrderId, error: getErrorMessage(error) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [detailReloadVersion, selectedOrderId])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (draftFilters.fromDate && draftFilters.toDate && draftFilters.fromDate > draftFilters.toDate) {
      setFilterError('The start date must be on or before the end date.')
      return
    }

    setFilterError(null)
    setFilters({ ...draftFilters, keyword: draftFilters.keyword.trim() })
    setPageNumber(0)
  }

  async function changeStatus(order: AdminOrderResponse, nextStatus: OrderStatus) {
    if (nextStatus === 'CANCELLED' && !window.confirm(`Cancel order #${order.order.id}? Stock and voucher usage will be restored.`)) {
      return
    }

    setUpdating(true)
    try {
      const updatedOrder = await updateAdminOrderStatusApi(order.order.id, nextStatus)
      setDetailResult({
        id: order.order.id,
        data: { ...order, order: updatedOrder },
      })
      setReloadVersion((version) => version + 1)
      toast.success(`Order #${order.order.id} updated to ${nextStatus}.`)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setUpdating(false)
    }
  }

  return (
    <section className="page-section admin-orders-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>Order management</h1>
        </div>
      </header>

      <form className="voucher-filters" onSubmit={applyFilters}>
        <label>
          Search order or customer
          <input
            onChange={(event) => setDraftFilters((current) => ({ ...current, keyword: event.target.value }))}
            placeholder="Order ID, email, name, or phone"
            value={draftFilters.keyword}
          />
        </label>
        <label>
          Status
          <select
            onChange={(event) => setDraftFilters((current) => ({
              ...current,
              status: orderStatuses.find((status) => status === event.target.value) ?? '',
            }))}
            value={draftFilters.status}
          >
            <option value="">All statuses</option>
            {orderStatuses.map((status) => <option key={status} value={status}>{status}</option>)}
          </select>
        </label>
        <label>
          From date
          <input
            onChange={(event) => setDraftFilters((current) => ({ ...current, fromDate: event.target.value }))}
            type="date"
            value={draftFilters.fromDate}
          />
        </label>
        <label>
          To date
          <input
            onChange={(event) => setDraftFilters((current) => ({ ...current, toDate: event.target.value }))}
            type="date"
            value={draftFilters.toDate}
          />
        </label>
        <label>
          Sort by
          <select
            onChange={(event) => {
              const sortBy = orderSortOptions.find((value) => value === event.target.value)
              setDraftFilters((current) => ({ ...current, sortBy: sortBy ?? 'createdAt' }))
            }}
            value={draftFilters.sortBy}
          >
            <option value="createdAt">Created date</option>
            <option value="totalAmount">Total amount</option>
            <option value="status">Status</option>
            <option value="id">Order ID</option>
          </select>
        </label>
        <label>
          Direction
          <select
            onChange={(event) => setDraftFilters((current) => ({
              ...current,
              direction: event.target.value === 'asc' ? 'asc' : 'desc',
            }))}
            value={draftFilters.direction}
          >
            <option value="desc">Descending</option>
            <option value="asc">Ascending</option>
          </select>
        </label>
        <button className="button button-secondary" type="submit">Apply filters</button>
      </form>

      {filterError && <p className="form-error" role="alert">{filterError}</p>}
      {isLoading && <p className="loading-state" role="status">Loading orders…</p>}
      {pageError && (
        <div className="form-error" role="alert">
          {pageError}
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
        </div>
      )}
      {!isLoading && !pageError && pageData?.content.length === 0 && (
        <p className="empty-state">No orders match these filters.</p>
      )}
      {!isLoading && !pageError && pageData && pageData.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Customer</th>
                  <th>Created</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Payment</th>
                  <th>Status</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((order) => (
                  <tr key={order.id}>
                    <td><strong>#{order.id}</strong></td>
                    <td>
                      <strong>{order.receiverName}</strong>
                      <span className="table-subtext">{order.customerEmail}</span>
                    </td>
                    <td>{new Date(order.createdAt).toLocaleString()}</td>
                    <td>
                      {order.itemCount}
                      <span className="table-subtext">{order.firstItemName ?? 'Order items'}</span>
                    </td>
                    <td>{formatCurrency(order.totalAmount)}</td>
                    <td>
                      {order.paymentMethod}
                      <span className="table-subtext">{order.paymentStatus}</span>
                    </td>
                    <td>
                      <span className={`status-badge status-${order.status.toLowerCase()}`}>
                        {order.status}
                      </span>
                    </td>
                    <td>
                      <button
                        className="text-button"
                        onClick={() => {
                          setDetailResult(null)
                          setDetailReloadVersion((version) => version + 1)
                          setSelectedOrderId(order.id)
                        }}
                        type="button"
                      >
                        View details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
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

      {selectedOrderId !== null && (
        <section aria-labelledby="admin-order-detail-title" className="admin-detail-panel">
          <header className="section-heading">
            <h2 id="admin-order-detail-title">Order #{selectedOrderId}</h2>
            <button
              aria-label="Close order details"
              className="text-button"
              onClick={() => {
                setSelectedOrderId(null)
                setDetailResult(null)
              }}
              type="button"
            >
              Close
            </button>
          </header>
          {isDetailLoading && <p className="loading-state" role="status">Loading order details…</p>}
          {detailError && (
            <div className="form-error" role="alert">
              {detailError}
              <button
                className="button button-secondary"
                onClick={() => {
                  setDetailResult(null)
                  setDetailReloadVersion((version) => version + 1)
                }}
                type="button"
              >
                Retry
              </button>
            </div>
          )}
          {selectedOrder && (
            <>
              <dl className="admin-detail-grid">
                <div><dt>Customer</dt><dd>{selectedOrder.customerName} · {selectedOrder.customerEmail}</dd></div>
                <div><dt>Status</dt><dd>{selectedOrder.order.status}</dd></div>
                <div><dt>Receiver</dt><dd>{selectedOrder.order.receiverName} · {selectedOrder.order.receiverPhone}</dd></div>
                <div>
                  <dt>Shipping address</dt>
                  <dd>
                    {[selectedOrder.order.shippingAddressLine, selectedOrder.order.shippingWard,
                      selectedOrder.order.shippingDistrict, selectedOrder.order.shippingCity]
                      .filter(Boolean)
                      .join(', ')}
                  </dd>
                </div>
                <div>
                  <dt>Payment</dt>
                  <dd>
                    {selectedOrder.order.payment
                      ? `${selectedOrder.order.payment.method} · ${selectedOrder.order.payment.status}`
                      : 'Not available'}
                  </dd>
                </div>
                <div><dt>Created</dt><dd>{new Date(selectedOrder.order.createdAt).toLocaleString()}</dd></div>
              </dl>

              <h3>Items</h3>
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr><th>Product</th><th>SKU</th><th>Quantity</th><th>Unit price</th><th>Line total</th></tr>
                  </thead>
                  <tbody>
                    {selectedOrder.order.items.map((item) => (
                      <tr key={item.id}>
                        <td>{item.productName}</td>
                        <td>{item.sku}</td>
                        <td>{item.quantity}</td>
                        <td>{formatCurrency(item.unitPrice)}</td>
                        <td>{formatCurrency(item.lineTotal)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <dl className="admin-detail-grid">
                <div><dt>Subtotal</dt><dd>{formatCurrency(selectedOrder.order.subtotal)}</dd></div>
                <div><dt>Discount</dt><dd>{formatCurrency(selectedOrder.order.discountAmount)}</dd></div>
                <div><dt>Shipping</dt><dd>{formatCurrency(selectedOrder.order.shippingFee)}</dd></div>
                <div><dt>Total</dt><dd>{formatCurrency(selectedOrder.order.totalAmount)}</dd></div>
                {selectedOrder.order.voucherCode && (
                  <div><dt>Voucher</dt><dd>{selectedOrder.order.voucherCode}</dd></div>
                )}
                {selectedOrder.order.note && (
                  <div><dt>Customer note</dt><dd>{selectedOrder.order.note}</dd></div>
                )}
              </dl>

              <div aria-label="Available status updates" className="table-actions">
                {statusTransitions[selectedOrder.order.status].map((nextStatus) => {
                  const requiresOnlinePayment = nextStatus === 'CONFIRMED'
                    && selectedOrder.order.payment?.method === 'ONLINE'
                    && selectedOrder.order.payment.status !== 'PAID'

                  return (
                    <button
                      className={nextStatus === 'CANCELLED' ? 'button button-secondary' : 'button button-primary'}
                      disabled={updating || requiresOnlinePayment}
                      key={nextStatus}
                      onClick={() => void changeStatus(selectedOrder, nextStatus)}
                      title={requiresOnlinePayment ? 'Complete online payment before confirming this order.' : undefined}
                      type="button"
                    >
                      {updating ? 'Updating…' : `Set ${nextStatus}`}
                    </button>
                  )
                })}
                {selectedOrder.order.status === 'PENDING'
                  && selectedOrder.order.payment?.method === 'ONLINE'
                  && selectedOrder.order.payment.status !== 'PAID'
                  && <p className="muted-text">Online payment must be completed before confirming this order.</p>}
                {statusTransitions[selectedOrder.order.status].length === 0
                  && <p className="muted-text">This order is in a final status.</p>}
              </div>
            </>
          )}
        </section>
      )}
    </section>
  )
}
