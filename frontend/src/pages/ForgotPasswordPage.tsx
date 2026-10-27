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
        <Link className="auth-back-link" to="/">OOAD Cosmetics</Link>
        <p className="eyebrow">Account recovery</p>
        <h1>Forgot password?</h1>
        <p className="page-intro">Enter your email to request a password reset.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="forgotEmail">Email</label>
          <input autoComplete="email" id="forgotEmail" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Sending…' : 'Request reset'}
          </button>
        </form>
        {result && (
          <div className="recovery-result" role="status">
            <p>{result.message}</p>
            {resetToken && (
              <>
                <p><strong>Demo reset token:</strong></p>
                <code className="reset-token">{resetToken}</code>
                {resetUrl && <Link className="button button-secondary button-full" to={resetUrl}>Continue to reset password</Link>}
              </>
            )}
          </div>
        )}
        <p className="auth-secondary-link"><Link className="text-link" to="/login">Back to sign in</Link></p>
      </section>
    </main>
  )
}
