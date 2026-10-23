import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function HomePage() {
  const { user, logout } = useAuth()

  if (!user) {
    return (
      <main className="auth-page">
        <section className="auth-card">
          <p className="eyebrow">OOAD Cosmetics</p>
          <h1>Welcome</h1>
          <p className="page-intro">Sign in to continue to your account.</p>
          <Link className="button button-primary button-link" to="/login">
            Sign in
          </Link>
        </section>
      </main>
    )
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <p className="eyebrow">OOAD Cosmetics</p>
        <h1>Hello, {user.fullName}</h1>
        <p className="page-intro">Signed in as {user.role.toLowerCase()}.</p>
        {user.role === 'ADMIN' && (
          <Link className="button button-primary button-link" to="/admin/dashboard">
            Open administration
          </Link>
        )}
        {user.role === 'CUSTOMER' && (
          <>
            <Link className="button button-secondary button-link" to="/cart">
              View cart
            </Link>
            <Link className="button button-primary button-link" to="/checkout">
              Go to checkout
            </Link>
            <Link className="button button-secondary button-link" to="/my-orders">
              View my orders
            </Link>
          </>
        )}
        <button className="button button-secondary button-full" onClick={logout} type="button">
          Sign out
        </button>
      </section>
    </main>
  )
}
