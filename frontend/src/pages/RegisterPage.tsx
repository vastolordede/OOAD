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
      toast.error('Passwords do not match.')
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
      toast.success('Account created successfully.')
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
        <Link className="auth-back-link" to="/">OOAD Cosmetics</Link>
        <p className="eyebrow">Account</p>
        <h1>Create account</h1>
        <p className="page-intro">Create an account to shop with OOAD Cosmetics.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="fullName">Full name</label>
          <input autoComplete="name" id="fullName" onChange={(event) => setFullName(event.target.value)} required value={fullName} />
          <label htmlFor="registerEmail">Email</label>
          <input autoComplete="email" id="registerEmail" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
          <label htmlFor="phone">Phone (optional)</label>
          <input autoComplete="tel" id="phone" onChange={(event) => setPhone(event.target.value)} type="tel" value={phone} />
          <label htmlFor="registerPassword">Password</label>
          <input autoComplete="new-password" id="registerPassword" minLength={8} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          <label htmlFor="confirmPassword">Confirm password</label>
          <input autoComplete="new-password" id="confirmPassword" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} />
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <p className="auth-secondary-link">Already registered? <Link className="text-link" to="/login">Sign in</Link></p>
      </section>
    </main>
  )
}
