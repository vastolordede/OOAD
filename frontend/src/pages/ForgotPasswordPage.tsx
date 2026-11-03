import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { forgotPasswordApi } from '../api/auth.api'
import type { ForgotPasswordResponse } from '../types/auth'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'

export function ForgotPasswordPage() {
  const toast = useToast()
  const [email, setEmail] = useState('')
  const [result, setResult] = useState<ForgotPasswordResponse | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setResult(null)
    try {
      const response = await forgotPasswordApi({ email: email.trim() })
      setResult(response)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const resetToken = result?.resetToken
  const resetUrl = resetToken
    ? `/reset-password?token=${encodeURIComponent(resetToken)}`
    : null

  return (
    <main className="auth-page">
      <section className="auth-card">
        <Link className="auth-back-link" to="/">Mỹ Phẩm Tơ Ràn</Link>
        <p className="eyebrow">Khôi phục tài khoản</p>
        <h1>Quên mật khẩu?</h1>
        <p className="page-intro">Nhập email để yêu cầu đặt lại mật khẩu.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="forgotEmail">Địa chỉ email</label>
          <input autoComplete="email" id="forgotEmail" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Đang gửi...' : 'Gửi yêu cầu'}
          </button>
        </form>
        {result && (
          <div className="recovery-result" role="status">
            <p>{result.message}</p>
            {resetToken && (
              <>
                <p><strong>Mã đặt lại mật khẩu (bản thử nghiệm):</strong></p>
                <code className="reset-token">{resetToken}</code>
                {resetUrl && <Link className="button button-secondary button-full" to={resetUrl}>Tiếp tục đặt lại mật khẩu</Link>}
              </>
            )}
          </div>
        )}
        <p className="auth-secondary-link"><Link className="text-link" to="/login">Quay lại đăng nhập</Link></p>
      </section>
    </main>
  )
}
