import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { listAdminOrdersApi } from '../api/admin-orders.api'
import type { PageResponse } from '../types/api'
import type { OrderStatus, OrderSummaryResponse } from '../types/commerce'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatDateTime, orderStatusLabel, paymentMethodLabel, paymentStatusLabel } from '../utils/localization'

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
      setFilterError('Ngày bắt đầu phải bằng hoặc trước ngày kết thúc.')
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
          <p className="eyebrow">Quản trị</p>
          <h1>Quản lý đơn hàng</h1>
        </div>
      </header>

      <form className="admin-order-filters" onSubmit={applyFilters}>
        <label>
          Tìm kiếm
          <input
            onChange={(event) => setKeywordDraft(event.target.value)}
            placeholder="Mã đơn, email, người nhận hoặc số điện thoại"
            value={keywordDraft}
          />
        </label>
        <label>
          Trạng thái
          <select onChange={(event) => updateStatusFilter(event.target.value)} value={status}>
            <option value="">Tất cả trạng thái</option>
            {orderStatuses.map((value) => <option key={value} value={value}>{orderStatusLabel(value)}</option>)}
          </select>
        </label>
        <label>
          Từ ngày
          <input
            onChange={(event) => setFromDateDraft(event.target.value)}
            type="date"
            value={fromDateDraft}
          />
        </label>
        <label>
          Đến ngày
          <input
            onChange={(event) => setToDateDraft(event.target.value)}
            type="date"
            value={toDateDraft}
          />
        </label>
        <label>
          Sắp xếp theo
          <select
            onChange={(event) =>
              setSortBy(event.target.value as 'createdAt' | 'totalAmount' | 'status' | 'id')}
            value={sortBy}
          >
            <option value="createdAt">Ngày tạo</option>
            <option value="totalAmount">Tổng tiền</option>
            <option value="status">Trạng thái</option>
            <option value="id">Mã đơn hàng</option>
          </select>
        </label>
        <label>
          Thứ tự
          <select
            onChange={(event) => setDirection(event.target.value === 'asc' ? 'asc' : 'desc')}
            value={direction}
          >
            <option value="desc">Giảm dần</option>
            <option value="asc">Tăng dần</option>
          </select>
        </label>
        <button className="button button-secondary" type="submit">Áp dụng bộ lọc</button>
      </form>

      {filterError && <p className="form-error" role="alert">{filterError}</p>}
      {isLoading && <p className="loading-state" role="status">Đang tải đơn hàng...</p>}
      {pageError && (
        <div className="form-error" role="alert">
          {pageError}
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
        </div>
      )}
      {!isLoading && !pageError && pageData?.content.length === 0 && (
        <p className="empty-state">Không có đơn hàng phù hợp với bộ lọc.</p>
      )}

      {!isLoading && !pageError && pageData && pageData.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Đơn hàng</th>
                  <th>Khách hàng / người nhận</th>
                  <th>Sản phẩm</th>
                  <th>Tổng tiền</th>
                  <th>Thanh toán</th>
                  <th>Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th>Chi tiết</th>
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
                    <td>{paymentMethodLabel(order.paymentMethod)} · {paymentStatusLabel(order.paymentStatus)}</td>
                    <td>
                      <span className={`status-badge status-${order.status.toLowerCase()}`}>
                        {orderStatusLabel(order.status)}
                      </span>
                    </td>
                    <td>{formatDateTime(order.createdAt)}</td>
                    <td><Link className="text-link" to={`/admin/orders/${order.id}`}>Mở</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav aria-label="Các trang đơn hàng" className="pagination">
            <button
              className="button button-secondary"
              disabled={pageData.first}
              onClick={() => setPageNumber((current) => Math.max(0, current - 1))}
              type="button"
            >
              Trước
            </button>
            <span>Trang {pageData.page + 1} / {Math.max(pageData.totalPages, 1)}</span>
            <button
              className="button button-secondary"
              disabled={pageData.last}
              onClick={() => setPageNumber((current) => current + 1)}
              type="button"
            >
              Sau
            </button>
          </nav>
        </>
      )}
    </section>
  )
}
