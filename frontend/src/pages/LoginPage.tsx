import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'

export function LoginPage() {
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)

    try {
      const role = await login({ email, password })
      toast.success('Đăng nhập thành công.')
      navigate(role === 'ADMIN' ? '/admin/dashboard' : '/', { replace: true })
    } catch (loginError) {
      toast.error(getErrorMessage(loginError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <Link className="auth-back-link" to="/">
          Mỹ Phẩm Tơ Ràn
        </Link>
        <p className="eyebrow">Tài khoản</p>
        <h1>Đăng nhập</h1>
        <p className="page-intro">Nhập thông tin tài khoản để tiếp tục.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="email">Địa chỉ email</label>
          <input
            autoComplete="email"
            id="email"
            onChange={(event) => setEmail(event.target.value)}
            required
            type="email"
            value={email}
          />
          <label htmlFor="password">Mật khẩu</label>
          <input
            autoComplete="current-password"
            id="password"
            onChange={(event) => setPassword(event.target.value)}
            required
            type="password"
            value={password}
          />
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>
        <div className="auth-links">
          <Link className="text-link" to="/register">Tạo tài khoản mới</Link>
          <Link className="text-link" to="/forgot-password">Quên mật khẩu?</Link>
        </div>
      </section>
    </main>
  )
}
