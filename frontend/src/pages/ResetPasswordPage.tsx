import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPasswordApi } from '../api/auth.api'
import type { ResetPasswordRequest } from '../types/auth'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const toast = useToast()
  const [token, setToken] = useState(searchParams.get('token') ?? '')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (newPassword !== confirmPassword) {
      toast.error('Mật khẩu xác nhận không khớp.')
      return
    }

    const payload: ResetPasswordRequest = { token: token.trim(), newPassword }
    setIsSubmitting(true)
    try {
      await resetPasswordApi(payload)
      toast.success('Đặt lại mật khẩu thành công. Vui lòng đăng nhập.')
      navigate('/login', { replace: true })
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <Link className="auth-back-link" to="/">Mỹ Phẩm Tơ Ràn</Link>
        <p className="eyebrow">Khôi phục tài khoản</p>
        <h1>Đặt lại mật khẩu</h1>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="resetToken">Mã đặt lại mật khẩu</label>
          <input autoComplete="one-time-code" id="resetToken" onChange={(event) => setToken(event.target.value)} required value={token} />
          <label htmlFor="newPassword">Mật khẩu mới</label>
          <input autoComplete="new-password" id="newPassword" minLength={8} onChange={(event) => setNewPassword(event.target.value)} required type="password" value={newPassword} />
          <label htmlFor="confirmNewPassword">Xác nhận mật khẩu mới</label>
          <input autoComplete="new-password" id="confirmNewPassword" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} />
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Đang cập nhật...' : 'Đặt lại mật khẩu'}
          </button>
        </form>
        <p className="auth-secondary-link"><Link className="text-link" to="/login">Quay lại đăng nhập</Link></p>
      </section>
    </main>
  )
}
