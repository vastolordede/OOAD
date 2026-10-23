import { Link } from 'react-router-dom'

export function AccessDeniedPage() {
  return (
    <main className="auth-page">
      <section className="auth-card">
        <p className="eyebrow">403</p>
        <h1>Access denied</h1>
        <p className="page-intro">This area is available to administrators only.</p>
        <Link className="button button-primary button-link" to="/">
          Return home
        </Link>
      </section>
    </main>
  )
}
