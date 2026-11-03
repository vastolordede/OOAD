import { Link } from 'react-router-dom'

export function AccessDeniedPage() {
  return (
    <main className="auth-page">
      <section className="auth-card">
        <p className="eyebrow">403</p>
        <h1>Bạn không có quyền truy cập</h1>
        <p className="page-intro">Khu vực này chỉ dành cho quản trị viên.</p>
        <Link className="button button-primary button-link" to="/">
          Về trang chủ
        </Link>
      </section>
    </main>
  )
}
