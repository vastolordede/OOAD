import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { deleteMyReviewApi, listMyReviewsApi, updateMyReviewApi } from '../api/reviews.api'
import type { PageResponse } from '../types/api'
import type { ReviewResponse } from '../types/review'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatDateTime, reviewStatusLabel } from '../utils/localization'

interface ReviewsResult {
  key: string
  page?: PageResponse<ReviewResponse>
  error?: string
}

export function MyReviewsPage() {
  const toast = useToast()
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<ReviewsResult | null>(null)
  const [editingReviewId, setEditingReviewId] = useState<number | null>(null)
  const [rating, setRating] = useState('5')
  const [comment, setComment] = useState('')
  const [busyReviewId, setBusyReviewId] = useState<number | null>(null)
  const size = 10
  const requestKey = `${pageNumber}:${reloadVersion}`
  const page = result?.key === requestKey ? result.page ?? null : null
  const error = result?.key === requestKey ? result.error ?? null : null
  const isLoading = result?.key !== requestKey

  useEffect(() => {
    let isCurrent = true
    listMyReviewsApi(pageNumber, size)
      .then((loadedPage) => {
        if (isCurrent) {
          setResult({ key: requestKey, page: loadedPage })
        }
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setResult({ key: requestKey, error: getErrorMessage(requestError) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [pageNumber, reloadVersion, requestKey])

  function beginEdit(review: ReviewResponse) {
    setEditingReviewId(review.id)
    setRating(String(review.rating))
    setComment(review.comment ?? '')
  }

  async function handleUpdate(event: FormEvent<HTMLFormElement>, reviewId: number) {
    event.preventDefault()
    const numericRating = Number(rating)
    if (!Number.isInteger(numericRating) || numericRating < 1 || numericRating > 5) {
      toast.error('Vui lòng chọn mức đánh giá từ 1 đến 5 sao.')
      return
    }

    setBusyReviewId(reviewId)
    try {
      const updated = await updateMyReviewApi(reviewId, {
        rating: numericRating,
        comment: comment.trim() || null,
      })
      setResult((current) => current?.key === requestKey && current.page
        ? {
            ...current,
            page: {
              ...current.page,
              content: current.page.content.map((review) => review.id === updated.id ? updated : review),
            },
          }
        : current)
      setEditingReviewId(null)
      toast.success('Đã cập nhật đánh giá.')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setBusyReviewId(null)
    }
  }

  async function handleDelete(review: ReviewResponse) {
    if (!window.confirm('Bạn có chắc muốn xóa đánh giá này?')) {
      return
    }
    setBusyReviewId(review.id)
    try {
      await deleteMyReviewApi(review.id)
      toast.success('Đã xóa đánh giá.')
      if (page?.content.length === 1 && pageNumber > 0) {
        setPageNumber((current) => current - 1)
      } else {
        setReloadVersion((version) => version + 1)
      }
      setEditingReviewId(null)
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setBusyReviewId(null)
    }
  }

  return (
    <main className="customer-page">
      <header className="customer-page-header">
        <div>
          <p className="eyebrow">Tài khoản</p>
          <h1>Đánh giá của tôi</h1>
        </div>
        <Link className="text-link" to="/my-orders">Xem đơn hàng</Link>
      </header>

      {isLoading && <p className="loading-state" role="status">Đang tải đánh giá...</p>}
      {error && (
        <section className="customer-panel">
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
        </section>
      )}
      {!isLoading && !error && page?.content.length === 0 && (
        <section className="customer-panel empty-orders">
          <h2>Bạn chưa có đánh giá</h2>
          <p className="page-intro">Đánh giá sản phẩm trong đơn hàng đã giao sẽ hiển thị tại đây.</p>
          <Link className="button button-primary button-link" to="/my-orders">Xem đơn hàng</Link>
        </section>
      )}
      {!isLoading && !error && page && page.content.length > 0 && (
        <>
          <div className="customer-review-list">
            {page.content.map((review) => (
              <article className="customer-panel customer-review-card" key={review.id}>
                <div className="customer-review-heading">
                  <div>
                    <Link className="text-link" to={`/products/${review.productId}`}>
                      Xem sản phẩm đã đánh giá
                    </Link>
                    <p className="muted-text">
                      {formatDateTime(review.createdAt)} · {reviewStatusLabel(review.status)}
                    </p>
                  </div>
                  <strong>{review.rating} / 5</strong>
                </div>
                {editingReviewId === review.id ? (
                  <form className="order-review-form" onSubmit={(event) => void handleUpdate(event, review.id)}>
                    <label>
                      Mức đánh giá
                      <select onChange={(event) => setRating(event.target.value)} value={rating}>
                        {[1, 2, 3, 4, 5].map((value) => (
                          <option key={value} value={value}>{value} / 5</option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Nhận xét (không bắt buộc)
                      <textarea
                        maxLength={2000}
                        onChange={(event) => setComment(event.target.value)}
                        rows={3}
                        value={comment}
                      />
                    </label>
                    <div className="customer-review-actions">
                      <button className="button button-primary" disabled={busyReviewId === review.id} type="submit">
                        {busyReviewId === review.id ? 'Đang lưu...' : 'Lưu thay đổi'}
                      </button>
                      <button
                        className="button button-secondary"
                        onClick={() => setEditingReviewId(null)}
                        type="button"
                      >
                        Hủy
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <p>{review.comment || 'Không có nhận xét.'}</p>
                    <div className="customer-review-actions">
                      <button className="button button-secondary" onClick={() => beginEdit(review)} type="button">
                        Chỉnh sửa
                      </button>
                      <button
                        className="button button-danger"
                        disabled={busyReviewId === review.id}
                        onClick={() => void handleDelete(review)}
                        type="button"
                      >
                        {busyReviewId === review.id ? 'Đang xóa...' : 'Xóa'}
                      </button>
                    </div>
                  </>
                )}
              </article>
            ))}
          </div>
          <nav aria-label="Các trang đánh giá" className="pagination">
            <button
              className="button button-secondary"
              disabled={page.first}
              onClick={() => setPageNumber((current) => Math.max(0, current - 1))}
              type="button"
            >
              Trước
            </button>
            <span>Trang {page.page + 1} / {Math.max(page.totalPages, 1)}</span>
            <button
              className="button button-secondary"
              disabled={page.last}
              onClick={() => setPageNumber((current) => current + 1)}
              type="button"
            >
              Sau
            </button>
          </nav>
        </>
      )}
    </main>
  )
}
