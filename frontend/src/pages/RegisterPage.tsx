import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'

export function RegisterPage() {
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (password !== confirmPassword) {
      toast.error('Mật khẩu xác nhận không khớp.')
      return
    }

    setIsSubmitting(true)
    try {
      const role = await register({
        fullName: fullName.trim(),
        email: email.trim(),
        ...(phone.trim() ? { phone: phone.trim() } : {}),
        password,
      })
      toast.success('Tạo tài khoản thành công.')
      navigate(role === 'ADMIN' ? '/admin/dashboard' : '/', { replace: true })
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
        <p className="eyebrow">Tài khoản</p>
        <h1>Tạo tài khoản</h1>
        <p className="page-intro">Đăng ký tài khoản để mua sắm tại Mỹ Phẩm Tơ Ràn.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="fullName">Họ và tên</label>
          <input autoComplete="name" id="fullName" onChange={(event) => setFullName(event.target.value)} required value={fullName} />
          <label htmlFor="registerEmail">Địa chỉ email</label>
          <input autoComplete="email" id="registerEmail" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
          <label htmlFor="phone">Số điện thoại (không bắt buộc)</label>
          <input autoComplete="tel" id="phone" onChange={(event) => setPhone(event.target.value)} type="tel" value={phone} />
          <label htmlFor="registerPassword">Mật khẩu (ít nhất 8 ký tự)</label>
          <input autoComplete="new-password" id="registerPassword" minLength={8} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          <label htmlFor="confirmPassword">Xác nhận mật khẩu</label>
          <input autoComplete="new-password" id="confirmPassword" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} />
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Đang tạo tài khoản...' : 'Tạo tài khoản'}
          </button>
        </form>
        <p className="auth-secondary-link">Đã có tài khoản? <Link className="text-link" to="/login">Đăng nhập</Link></p>
      </section>
    </main>
  )
}
