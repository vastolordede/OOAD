import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

export function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleSignOut() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="site-header">
      <nav aria-label="Điều hướng chính" className="site-nav">
        <Link className="site-brand" to="/">
          Mỹ Phẩm Tơ Ràn
        </Link>
        <div className="site-nav-links">
          <NavLink className={({ isActive }) => isActive ? 'site-nav-link is-active' : 'site-nav-link'} to="/products">
            Sản phẩm
          </NavLink>
          {!user && (
            <>
              <Link className="button button-secondary" to="/login">Đăng nhập</Link>
              <Link className="button button-primary" to="/register">Đăng ký</Link>
            </>
          )}
          {user?.role === 'CUSTOMER' && (
            <>
              <Link className="site-nav-link" to="/cart">Giỏ hàng</Link>
              <Link className="site-nav-link" to="/my-orders">Đơn hàng</Link>
              <Link className="site-nav-link" to="/wishlist">Yêu thích</Link>
              <Link className="site-nav-link" to="/profile">Tài khoản</Link>
              <Link className="site-nav-link" to="/addresses">Địa chỉ</Link>
              <span className="site-user-name">{user.fullName}</span>
              <button className="button button-secondary" onClick={handleSignOut} type="button">
                Đăng xuất
              </button>
            </>
          )}
          {user?.role === 'ADMIN' && (
            <>
              <Link className="site-nav-link" to="/admin/dashboard">Trang quản trị</Link>
              <span className="site-user-name">{user.fullName}</span>
              <button className="button button-secondary" onClick={handleSignOut} type="button">
                Đăng xuất
              </button>
            </>
          )}
        </div>
      </nav>
    </header>
  )
}

export function Footer() {
  return (
    <footer className="site-footer">
      <span>Mỹ Phẩm Tơ Ràn</span>
      <span>© {new Date().getFullYear()} — Chăm sóc làn da, tôn vinh vẻ đẹp của bạn.</span>
    </footer>
  )
}

export function MainLayout() {
  return (
    <div className="main-layout">
      <Navbar />
      <div className="main-layout-content">
        <Outlet />
      </div>
      <Footer />
    </div>
  )
}
