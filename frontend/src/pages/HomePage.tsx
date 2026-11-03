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
          <p className="eyebrow">Mỹ Phẩm Tơ Ràn</p>
          <h1>{user ? `Xin chào, ${user.fullName}` : 'Chăm sóc làn da, tôn vinh vẻ đẹp của bạn'}</h1>
          <p className="page-intro">
            {user
              ? `Chào mừng bạn quay trở lại. Bạn đang đăng nhập với vai trò ${user.role === 'ADMIN' ? 'quản trị viên' : 'khách hàng'}.`
              : 'Khám phá các sản phẩm chăm sóc da và làm đẹp được tuyển chọn cho thói quen hằng ngày của bạn.'}
          </p>
          <div className="home-hero-actions">
            <Link className="button button-primary" to="/products">Khám phá sản phẩm</Link>
            {!user && (
              <>
                <Link className="button button-secondary" to="/login">Đăng nhập</Link>
                <Link className="button button-secondary" to="/register">Tạo tài khoản</Link>
              </>
            )}
            {user?.role === 'ADMIN' && (
              <Link className="button button-secondary" to="/admin/dashboard">Mở trang quản trị</Link>
            )}
            {user?.role === 'CUSTOMER' && (
              <>
                <Link className="button button-secondary" to="/cart">Xem giỏ hàng</Link>
                <Link className="button button-secondary" to="/checkout">Tiến hành đặt hàng</Link>
                <Link className="button button-secondary" to="/my-orders">Xem đơn hàng</Link>
              </>
            )}
            {user && (
              <button className="button button-secondary" onClick={logout} type="button">
                Đăng xuất
              </button>
            )}
          </div>
        </div>
      </section>

      <section aria-labelledby="new-arrivals-heading" className="featured-section">
        <header className="featured-header">
          <div>
            <p className="eyebrow">Vừa cập nhật</p>
            <h2 id="new-arrivals-heading">Sản phẩm mới</h2>
            <p className="page-intro">Khám phá những sản phẩm mới nhất trong bộ sưu tập.</p>
          </div>
          <Link className="button button-secondary" to="/products">Xem tất cả sản phẩm</Link>
        </header>
        {isLoading && <LoadingState message="Đang tải sản phẩm mới..." />}
        {error && (
          <ErrorState
            message={error}
            onRetry={handleRetry}
          />
        )}
        {!isLoading && !error && products.length === 0 && (
          <EmptyState
            action={<Link className="button button-secondary" to="/products">Xem danh sách sản phẩm</Link>}
            description="Hiện chưa có sản phẩm nào để giới thiệu."
            title="Chưa có sản phẩm mới"
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
