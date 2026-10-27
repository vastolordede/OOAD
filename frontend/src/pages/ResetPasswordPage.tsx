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
      toast.error('Passwords do not match.')
      return
    }

    const payload: ResetPasswordRequest = { token: token.trim(), newPassword }
    setIsSubmitting(true)
    try {
      await resetPasswordApi(payload)
      toast.success('Password reset successfully. Please sign in.')
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
        <Link className="auth-back-link" to="/">OOAD Cosmetics</Link>
        <p className="eyebrow">Account recovery</p>
        <h1>Reset password</h1>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="resetToken">Reset token</label>
          <input autoComplete="one-time-code" id="resetToken" onChange={(event) => setToken(event.target.value)} required value={token} />
          <label htmlFor="newPassword">New password</label>
          <input autoComplete="new-password" id="newPassword" minLength={8} onChange={(event) => setNewPassword(event.target.value)} required type="password" value={newPassword} />
          <label htmlFor="confirmNewPassword">Confirm new password</label>
          <input autoComplete="new-password" id="confirmNewPassword" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} />
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Resetting…' : 'Reset password'}
          </button>
        </form>
        <p className="auth-secondary-link"><Link className="text-link" to="/login">Back to sign in</Link></p>
      </section>
    </main>
  )
}
