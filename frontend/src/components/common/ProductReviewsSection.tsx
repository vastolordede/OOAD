import { useEffect, useState } from 'react'
import { getProductReviewsApi } from '../../api/reviews.api'
import type { ProductReviewsResponse } from '../../types/review'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { formatDate } from '../../utils/localization'

interface ProductReviewState {
  key: string
  data?: ProductReviewsResponse
  error?: string
}

export function ProductReviewsSection({ productId }: { productId: number }) {
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<ProductReviewState | null>(null)
  const pageSize = 10
  const requestKey = `${productId}:${pageNumber}:${reloadVersion}`
  const data = result?.key === requestKey ? result.data ?? null : null
  const error = result?.key === requestKey ? result.error ?? null : null
  const isLoading = result?.key !== requestKey

  useEffect(() => {
    let isCurrent = true
    getProductReviewsApi(productId, pageNumber, pageSize)
      .then((reviews) => {
        if (isCurrent) {
          setResult({ key: requestKey, data: reviews })
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
  }, [pageNumber, pageSize, productId, requestKey])

  return (
    <section className="product-reviews customer-panel">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Phản hồi khách hàng</p>
          <h2>Đánh giá sản phẩm</h2>
        </div>
        {data && (
          <p className="review-summary">
            <strong>{data.averageRating.toFixed(1)} / 5</strong>
            <span>{data.reviewCount} đánh giá</span>
          </p>
        )}
      </header>

      {isLoading && <p className="loading-state" role="status">Đang tải đánh giá...</p>}
      {error && (
        <div>
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
        </div>
      )}
      {!isLoading && !error && data?.reviews.content.length === 0 && (
        <p className="empty-state">Chưa có đánh giá nào.</p>
      )}
      {!isLoading && !error && data && data.reviews.content.length > 0 && (
        <>
          <ul className="product-review-list">
            {data.reviews.content.map((review) => (
              <li key={review.id}>
                <div className="review-meta">
                  <strong>{review.reviewerName}</strong>
                  <span>{review.rating} / 5</span>
                  <time dateTime={review.createdAt}>
                    {formatDate(review.createdAt)}
                  </time>
                </div>
                {review.comment && <p>{review.comment}</p>}
              </li>
            ))}
          </ul>
          <nav aria-label="Các trang đánh giá" className="pagination">
            <button
              className="button button-secondary"
              disabled={data.reviews.first}
              onClick={() => setPageNumber((current) => Math.max(0, current - 1))}
              type="button"
            >
              Trước
            </button>
            <span>Trang {data.reviews.page + 1} / {Math.max(data.reviews.totalPages, 1)}</span>
            <button
              className="button button-secondary"
              disabled={data.reviews.last}
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
