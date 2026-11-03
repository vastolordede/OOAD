import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { listAdminReviewsApi, updateAdminReviewStatusApi } from '../api/reviews.api'
import type { PageResponse } from '../types/api'
import type { AdminReviewResponse, ReviewStatus } from '../types/review'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatDate, reviewStatusLabel } from '../utils/localization'

interface ReviewListState {
  key: string
  data?: PageResponse<AdminReviewResponse>
  error?: string
}

export function AdminReviewsPage() {
  const toast = useToast()
  const [status, setStatus] = useState<ReviewStatus | ''>('')
  const [productIdDraft, setProductIdDraft] = useState('')
  const [productId, setProductId] = useState<number | null>(null)
  const [filterError, setFilterError] = useState<string | null>(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<ReviewListState | null>(null)
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const pageSize = 20
  const requestKey = `${status}:${productId}:${pageNumber}:${reloadVersion}`
  const pageData = result?.key === requestKey ? result.data ?? null : null
  const pageError = result?.key === requestKey ? result.error ?? null : null
  const isLoading = result?.key !== requestKey

  useEffect(() => {
    let isCurrent = true
    listAdminReviewsApi({
      ...(status ? { status } : {}),
      ...(productId === null ? {} : { productId }),
      page: pageNumber,
      size: pageSize,
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
  }, [pageNumber, pageSize, productId, requestKey, status])

  function handleFilter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedId = productIdDraft.trim()
    if (trimmedId && (!/^\d+$/.test(trimmedId) || Number(trimmedId) < 1)) {
      setFilterError('Mã sản phẩm phải là số nguyên dương.')
      return
    }
    setFilterError(null)
    setProductId(trimmedId ? Number(trimmedId) : null)
    setPageNumber(0)
  }

  function handleStatusFilter(value: string) {
    setStatus(value === 'VISIBLE' || value === 'HIDDEN' ? value : '')
    setPageNumber(0)
  }

  async function handleStatusUpdate(review: AdminReviewResponse) {
    const nextStatus = review.status === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE'
    setUpdatingId(review.id)

    try {
      await updateAdminReviewStatusApi(review.id, nextStatus)
      toast.success(`Đánh giá đã chuyển sang trạng thái ${reviewStatusLabel(nextStatus).toLowerCase()}.`)
      setReloadVersion((version) => version + 1)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <section className="page-section admin-reviews-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Quản trị</p>
          <h1>Quản lý đánh giá</h1>
        </div>
      </header>

      <form className="review-filters" onSubmit={handleFilter}>
        <label>
          Trạng thái
          <select onChange={(event) => handleStatusFilter(event.target.value)} value={status}>
            <option value="">Tất cả trạng thái</option>
            <option value="VISIBLE">Đang hiển thị</option>
            <option value="HIDDEN">Đã ẩn</option>
          </select>
        </label>
        <label>
          Mã sản phẩm
          <input
            inputMode="numeric"
            onChange={(event) => setProductIdDraft(event.target.value)}
            value={productIdDraft}
          />
        </label>
        <button className="button button-secondary" type="submit">Áp dụng bộ lọc</button>
      </form>

      {filterError && <p className="form-error" role="alert">{filterError}</p>}
      {isLoading && <p className="loading-state" role="status">Đang tải đánh giá...</p>}
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
        <p className="empty-state">Không có đánh giá phù hợp với bộ lọc.</p>
      )}
      {!isLoading && !pageError && pageData && pageData.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Khách hàng</th>
                  <th>Đánh giá</th>
                  <th>Nhận xét</th>
                  <th>Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((review) => (
                  <tr key={review.id}>
                    <td>
                      <Link className="text-link" to={`/products/${review.productId}`}>
                        {review.productName}
                      </Link>
                      <span className="table-subtext">Mã sản phẩm #{review.productId}</span>
                    </td>
                    <td>
                      <strong>{review.customerName}</strong>
                      <span className="table-subtext">{review.customerEmail}</span>
                    </td>
                    <td>{review.rating} / 5</td>
                    <td>{review.comment || '—'}</td>
                    <td><span className={`status-badge status-${review.status.toLowerCase()}`}>{reviewStatusLabel(review.status)}</span></td>
                    <td>{formatDate(review.createdAt)}</td>
                    <td>
                      <button
                        className="text-button"
                        disabled={updatingId === review.id}
                        onClick={() => void handleStatusUpdate(review)}
                        type="button"
                      >
                        {updatingId === review.id ? 'Đang cập nhật...' : review.status === 'VISIBLE' ? 'Ẩn đánh giá' : 'Hiện đánh giá'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav aria-label="Các trang đánh giá" className="pagination">
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
