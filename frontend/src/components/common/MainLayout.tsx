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
      <nav aria-label="Main navigation" className="site-nav">
        <Link className="site-brand" to="/">
          OOAD Cosmetics
        </Link>
        <div className="site-nav-links">
          <NavLink className={({ isActive }) => isActive ? 'site-nav-link is-active' : 'site-nav-link'} to="/products">
            Catalog
          </NavLink>
          {!user && (
            <>
              <Link className="button button-secondary" to="/login">Sign in</Link>
              <Link className="button button-primary" to="/register">Register</Link>
            </>
          )}
          {user?.role === 'CUSTOMER' && (
            <>
              <Link className="site-nav-link" to="/cart">Cart</Link>
              <Link className="site-nav-link" to="/my-orders">Orders</Link>
              <Link className="site-nav-link" to="/wishlist">Wishlist</Link>
              <Link className="site-nav-link" to="/profile">Profile</Link>
              <Link className="site-nav-link" to="/addresses">Addresses</Link>
              <span className="site-user-name">{user.fullName}</span>
              <button className="button button-secondary" onClick={handleSignOut} type="button">
                Sign out
              </button>
            </>
          )}
          {user?.role === 'ADMIN' && (
            <>
              <Link className="site-nav-link" to="/admin/dashboard">Admin Panel</Link>
              <span className="site-user-name">{user.fullName}</span>
              <button className="button button-secondary" onClick={handleSignOut} type="button">
                Sign out
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
      <span>OOAD Cosmetics</span>
      <span>© {new Date().getFullYear()} — Care, confidence, and beauty.</span>
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
