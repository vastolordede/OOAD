import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { listAdminOrdersApi } from '../api/admin-orders.api'
import type { PageResponse } from '../types/api'
import type { OrderStatus, OrderSummaryResponse } from '../types/commerce'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

const orderStatuses: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'SHIPPING',
  'DELIVERED',
  'CANCELLED',
]

interface AdminOrderListState {
  key: string
  data?: PageResponse<OrderSummaryResponse>
  error?: string
}

export function AdminOrdersPage() {
  const [keywordDraft, setKeywordDraft] = useState('')
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [fromDateDraft, setFromDateDraft] = useState('')
  const [toDateDraft, setToDateDraft] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [sortBy, setSortBy] = useState<'createdAt' | 'totalAmount' | 'status' | 'id'>('createdAt')
  const [direction, setDirection] = useState<'asc' | 'desc'>('desc')
  const [filterError, setFilterError] = useState<string | null>(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const pageSize = 20
  const requestKey = [
    keyword,
    status,
    fromDate,
    toDate,
    sortBy,
    direction,
    pageNumber,
    reloadVersion,
  ].join(':')
  const [result, setResult] = useState<AdminOrderListState | null>(null)
  const pageData = result?.key === requestKey ? result.data ?? null : null
  const pageError = result?.key === requestKey ? result.error ?? null : null
  const isLoading = result?.key !== requestKey

  useEffect(() => {
    let isCurrent = true
    listAdminOrdersApi({
      page: pageNumber,
      size: pageSize,
      ...(keyword ? { keyword } : {}),
      ...(status ? { status } : {}),
      ...(fromDate ? { fromDate } : {}),
      ...(toDate ? { toDate } : {}),
      sortBy,
      direction,
    })
      .then((data) => {
        if (isCurrent) setResult({ key: requestKey, data })
      })
      .catch((error: unknown) => {
        if (isCurrent) setResult({ key: requestKey, error: getErrorMessage(error) })
      })

    return () => {
      isCurrent = false
    }
  }, [direction, fromDate, keyword, pageNumber, pageSize, requestKey, sortBy, status, toDate])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (fromDateDraft && toDateDraft && fromDateDraft > toDateDraft) {
      setFilterError('The start date must be on or before the end date.')
      return
    }

    setFilterError(null)
    setKeyword(keywordDraft.trim())
    setFromDate(fromDateDraft)
    setToDate(toDateDraft)
    setPageNumber(0)
  }

  function updateStatusFilter(value: string) {
    setStatus(orderStatuses.includes(value as OrderStatus) ? value as OrderStatus : '')
    setPageNumber(0)
  }

  return (
    <section className="page-section admin-orders-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>Order management</h1>
        </div>
      </header>

      <form className="admin-order-filters" onSubmit={applyFilters}>
        <label>
          Search
          <input
            onChange={(event) => setKeywordDraft(event.target.value)}
            placeholder="Order ID, email, recipient or phone"
            value={keywordDraft}
          />
        </label>
        <label>
          Status
          <select onChange={(event) => updateStatusFilter(event.target.value)} value={status}>
            <option value="">All statuses</option>
            {orderStatuses.map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
        </label>
        <label>
          From
          <input
            onChange={(event) => setFromDateDraft(event.target.value)}
            type="date"
            value={fromDateDraft}
          />
        </label>
        <label>
          To
          <input
            onChange={(event) => setToDateDraft(event.target.value)}
            type="date"
            value={toDateDraft}
          />
        </label>
        <label>
          Sort by
          <select
            onChange={(event) =>
              setSortBy(event.target.value as 'createdAt' | 'totalAmount' | 'status' | 'id')}
            value={sortBy}
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
            onChange={(event) => setDirection(event.target.value === 'asc' ? 'asc' : 'desc')}
            value={direction}
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
                  <th>Customer / recipient</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Payment</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((order) => (
                  <tr key={order.id}>
                    <td>#{order.id}</td>
                    <td>
                      <strong>{order.receiverName}</strong>
                      <span className="table-subtext">{order.customerEmail}</span>
                    </td>
                    <td>
                      {order.itemCount}
                      {order.firstItemName && <span className="table-subtext">{order.firstItemName}</span>}
                    </td>
                    <td>{formatCurrency(order.totalAmount)}</td>
                    <td>{order.paymentMethod} · {order.paymentStatus}</td>
                    <td>
                      <span className={`status-badge status-${order.status.toLowerCase()}`}>
                        {order.status}
                      </span>
                    </td>
                    <td>{new Date(order.createdAt).toLocaleString()}</td>
                    <td><Link className="text-link" to={`/admin/orders/${order.id}`}>Open</Link></td>
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
    </section>
  )
}
