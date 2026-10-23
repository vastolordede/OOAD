import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { deleteMyReviewApi, listMyReviewsApi, updateMyReviewApi } from '../api/reviews.api'
import type { PageResponse } from '../types/api'
import type { ReviewResponse } from '../types/review'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'

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
      toast.error('Choose a rating from 1 to 5.')
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
      toast.success('Review updated.')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setBusyReviewId(null)
    }
  }

  async function handleDelete(review: ReviewResponse) {
    if (!window.confirm('Delete this review?')) {
      return
    }
    setBusyReviewId(review.id)
    try {
      await deleteMyReviewApi(review.id)
      toast.success('Review deleted.')
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
          <p className="eyebrow">Customer</p>
          <h1>My reviews</h1>
        </div>
        <Link className="text-link" to="/my-orders">View my orders</Link>
      </header>

      {isLoading && <p className="loading-state" role="status">Loading reviews…</p>}
      {error && (
        <section className="customer-panel">
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
        </section>
      )}
      {!isLoading && !error && page?.content.length === 0 && (
        <section className="customer-panel empty-orders">
          <h2>No reviews yet</h2>
          <p className="page-intro">Reviews you submit for delivered orders will appear here.</p>
          <Link className="button button-primary button-link" to="/my-orders">View my orders</Link>
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
                      View reviewed product
                    </Link>
                    <p className="muted-text">
                      {new Date(review.createdAt).toLocaleString()} · {review.status}
                    </p>
                  </div>
                  <strong>{review.rating} / 5</strong>
                </div>
                {editingReviewId === review.id ? (
                  <form className="order-review-form" onSubmit={(event) => void handleUpdate(event, review.id)}>
                    <label>
                      Rating
                      <select onChange={(event) => setRating(event.target.value)} value={rating}>
                        {[1, 2, 3, 4, 5].map((value) => (
                          <option key={value} value={value}>{value} / 5</option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Comment (optional)
                      <textarea
                        maxLength={2000}
                        onChange={(event) => setComment(event.target.value)}
                        rows={3}
                        value={comment}
                      />
                    </label>
                    <div className="customer-review-actions">
                      <button className="button button-primary" disabled={busyReviewId === review.id} type="submit">
                        {busyReviewId === review.id ? 'Saving…' : 'Save changes'}
                      </button>
                      <button
                        className="button button-secondary"
                        onClick={() => setEditingReviewId(null)}
                        type="button"
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <p>{review.comment || 'No comment provided.'}</p>
                    <div className="customer-review-actions">
                      <button className="button button-secondary" onClick={() => beginEdit(review)} type="button">
                        Edit
                      </button>
                      <button
                        className="button button-danger"
                        disabled={busyReviewId === review.id}
                        onClick={() => void handleDelete(review)}
                        type="button"
                      >
                        {busyReviewId === review.id ? 'Deleting…' : 'Delete'}
                      </button>
                    </div>
                  </>
                )}
              </article>
            ))}
          </div>
          <nav aria-label="Review pages" className="pagination">
            <button
              className="button button-secondary"
              disabled={page.first}
              onClick={() => setPageNumber((current) => Math.max(0, current - 1))}
              type="button"
            >
              Previous
            </button>
            <span>Page {page.page + 1} of {Math.max(page.totalPages, 1)}</span>
            <button
              className="button button-secondary"
              disabled={page.last}
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
