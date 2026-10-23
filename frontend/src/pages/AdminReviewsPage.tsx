import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { listAdminReviewsApi, updateAdminReviewStatusApi } from '../api/reviews.api'
import type { PageResponse } from '../types/api'
import type { AdminReviewResponse, ReviewStatus } from '../types/review'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'

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
      setFilterError('Product ID must be a positive whole number.')
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
      toast.success(`Review set to ${nextStatus.toLowerCase()}.`)
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
          <p className="eyebrow">Admin</p>
          <h1>Review moderation</h1>
        </div>
      </header>

      <form className="review-filters" onSubmit={handleFilter}>
        <label>
          Status
          <select onChange={(event) => handleStatusFilter(event.target.value)} value={status}>
            <option value="">All statuses</option>
            <option value="VISIBLE">Visible</option>
            <option value="HIDDEN">Hidden</option>
          </select>
        </label>
        <label>
          Product ID
          <input
            inputMode="numeric"
            onChange={(event) => setProductIdDraft(event.target.value)}
            value={productIdDraft}
          />
        </label>
        <button className="button button-secondary" type="submit">Apply filters</button>
      </form>

      {filterError && <p className="form-error" role="alert">{filterError}</p>}
      {isLoading && <p className="loading-state" role="status">Loading reviews…</p>}
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
        <p className="empty-state">No reviews match these filters.</p>
      )}
      {!isLoading && !pageError && pageData && pageData.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Customer</th>
                  <th>Rating</th>
                  <th>Comment</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((review) => (
                  <tr key={review.id}>
                    <td>
                      <Link className="text-link" to={`/products/${review.productId}`}>
                        {review.productName}
                      </Link>
                      <span className="table-subtext">Product #{review.productId}</span>
                    </td>
                    <td>
                      <strong>{review.customerName}</strong>
                      <span className="table-subtext">{review.customerEmail}</span>
                    </td>
                    <td>{review.rating} / 5</td>
                    <td>{review.comment || '—'}</td>
                    <td><span className={`status-badge status-${review.status.toLowerCase()}`}>{review.status}</span></td>
                    <td>{new Date(review.createdAt).toLocaleDateString()}</td>
                    <td>
                      <button
                        className="text-button"
                        disabled={updatingId === review.id}
                        onClick={() => void handleStatusUpdate(review)}
                        type="button"
                      >
                        {updatingId === review.id ? 'Updating…' : review.status === 'VISIBLE' ? 'Hide' : 'Show'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav aria-label="Review pages" className="pagination">
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
