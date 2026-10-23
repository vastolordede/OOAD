import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listWishlistApi, removeWishlistItemApi, type WishlistItemResponse } from '../api/wishlist.api'
import type { PageResponse } from '../types/api'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

interface WishlistResult {
  key: string
  page?: PageResponse<WishlistItemResponse>
  error?: string
}

export function WishlistPage() {
  const toast = useToast()
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<WishlistResult | null>(null)
  const [removingProductId, setRemovingProductId] = useState<number | null>(null)
  const size = 12
  const requestKey = `${pageNumber}:${reloadVersion}`
  const page = result?.key === requestKey ? result.page ?? null : null
  const error = result?.key === requestKey ? result.error ?? null : null
  const isLoading = result?.key !== requestKey

  useEffect(() => {
    let isCurrent = true
    listWishlistApi(pageNumber, size)
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

  async function handleRemove(productId: number) {
    setRemovingProductId(productId)
    try {
      await removeWishlistItemApi(productId)
      toast.success('Product removed from wishlist.')
      if (page?.content.length === 1 && pageNumber > 0) {
        setPageNumber((current) => current - 1)
      } else {
        setReloadVersion((version) => version + 1)
      }
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setRemovingProductId(null)
    }
  }

  return (
    <main className="customer-page">
      <header className="customer-page-header">
        <div>
          <p className="eyebrow">Customer</p>
          <h1>Wishlist</h1>
        </div>
        <Link className="text-link" to="/">Continue shopping</Link>
      </header>

      {isLoading && <p className="loading-state" role="status">Loading wishlist…</p>}
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
          <h2>Your wishlist is empty</h2>
          <p className="page-intro">Save products you want to revisit here.</p>
          <Link className="button button-primary button-link" to="/">Browse products</Link>
        </section>
      )}
      {!isLoading && !error && page && page.content.length > 0 && (
        <>
          <div className="wishlist-grid">
            {page.content.map((item) => (
              <article className="customer-panel wishlist-card" key={item.productId}>
                <Link className="wishlist-image" to={`/products/${item.productId}`}>
                  {item.imageUrl
                    ? <img alt="" src={item.imageUrl} />
                    : <span>No image</span>}
                </Link>
                <p className="eyebrow">{item.brandName}</p>
                <h2><Link to={`/products/${item.productId}`}>{item.productName}</Link></h2>
                <p>{item.minPrice === null ? 'Price unavailable' : `From ${formatCurrency(item.minPrice)}`}</p>
                {!item.available && <p className="form-error">This product is currently unavailable.</p>}
                <div className="customer-review-actions">
                  <Link className="button button-primary" to={`/products/${item.productId}`}>View product</Link>
                  <button
                    className="button button-secondary"
                    disabled={removingProductId === item.productId}
                    onClick={() => void handleRemove(item.productId)}
                    type="button"
                  >
                    {removingProductId === item.productId ? 'Removing…' : 'Remove'}
                  </button>
                </div>
              </article>
            ))}
          </div>
          <nav aria-label="Wishlist pages" className="pagination">
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
