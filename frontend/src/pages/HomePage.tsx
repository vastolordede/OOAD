import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listPublicProductsApi } from '../api/products.api'
import { EmptyState, ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { ProductCard } from '../components/common/ProductCard'
import { useAuth } from '../hooks/useAuth'
import type { ProductSummaryResponse } from '../types/product'
import { getErrorMessage } from '../utils/getErrorMessage'

export function HomePage() {
  const { user, logout } = useAuth()
  const [products, setProducts] = useState<ProductSummaryResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    let isCurrent = true
    listPublicProductsApi({ sort: 'NEWEST', page: 0, size: 8 })
      .then((page) => {
        if (isCurrent) setProducts(page.content)
      })
      .catch((requestError: unknown) => {
        if (isCurrent) setError(getErrorMessage(requestError))
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })
    return () => {
      isCurrent = false
    }
  }, [reloadVersion])

  function handleRetry() {
    setIsLoading(true)
    setError(null)
    setReloadVersion((version) => version + 1)
  }

  return (
    <main className="customer-page home-page">
      <section className="customer-panel home-hero">
        <div>
          <p className="eyebrow">OOAD Cosmetics</p>
          <h1>{user ? `Hello, ${user.fullName}` : 'Beauty, thoughtfully curated'}</h1>
          <p className="page-intro">
            {user
              ? `Welcome back. You're signed in as ${user.role.toLowerCase()}.`
              : 'Discover skincare and beauty essentials selected for your everyday routine.'}
          </p>
          <div className="home-hero-actions">
            <Link className="button button-primary" to="/products">Browse Catalog</Link>
            {!user && (
              <>
                <Link className="button button-secondary" to="/login">Sign in</Link>
                <Link className="button button-secondary" to="/register">Create an account</Link>
              </>
            )}
            {user?.role === 'ADMIN' && (
              <Link className="button button-secondary" to="/admin/dashboard">Open administration</Link>
            )}
            {user?.role === 'CUSTOMER' && (
              <>
                <Link className="button button-secondary" to="/cart">View cart</Link>
                <Link className="button button-secondary" to="/checkout">Go to checkout</Link>
                <Link className="button button-secondary" to="/my-orders">View my orders</Link>
              </>
            )}
            {user && (
              <button className="button button-secondary" onClick={logout} type="button">
                Sign out
              </button>
            )}
          </div>
        </div>
      </section>

      <section aria-labelledby="new-arrivals-heading" className="featured-section">
        <header className="featured-header">
          <div>
            <p className="eyebrow">Just added</p>
            <h2 id="new-arrivals-heading">Featured / New Arrivals</h2>
            <p className="page-intro">Explore the latest additions to our collection.</p>
          </div>
          <Link className="button button-secondary" to="/products">View all products</Link>
        </header>
        {isLoading && <LoadingState message="Loading new arrivals…" />}
        {error && (
          <ErrorState
            message={error}
            onRetry={handleRetry}
          />
        )}
        {!isLoading && !error && products.length === 0 && (
          <EmptyState
            action={<Link className="button button-secondary" to="/products">Browse catalog</Link>}
            description="There are no products to feature right now."
            title="No new arrivals yet"
          />
        )}
        {!isLoading && !error && products.length > 0 && (
          <div className="product-grid">
            {products.map((product) => <ProductCard key={product.id} product={product} />)}
          </div>
        )}
      </section>
    </main>
  )
}
