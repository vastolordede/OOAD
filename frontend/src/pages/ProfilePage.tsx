import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { changeMyPasswordApi, getMyProfileApi, updateMyProfileApi } from '../api/user.api'
import { ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { UserResponse } from '../types/auth'
import { getErrorMessage } from '../utils/getErrorMessage'

export function ProfilePage() {
  const { updateUserState } = useAuth()
  const toast = useToast()
  const [profile, setProfile] = useState<UserResponse | null>(null)
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    let isCurrent = true
    getMyProfileApi()
      .then((loadedProfile) => {
        if (isCurrent) {
          setProfile(loadedProfile)
          setFullName(loadedProfile.fullName)
          setPhone(loadedProfile.phone ?? '')
          updateUserState(loadedProfile)
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) setLoadError(getErrorMessage(error))
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })
    return () => {
      isCurrent = false
    }
  }, [reloadVersion, updateUserState])

  function handleProfileRetry() {
    setIsLoading(true)
    setLoadError(null)
    setReloadVersion((version) => version + 1)
  }

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSavingProfile(true)
    try {
      const updated = await updateMyProfileApi({
        fullName: fullName.trim(),
        phone: phone.trim() || null,
      })
      setProfile(updated)
      setFullName(updated.fullName)
      setPhone(updated.phone ?? '')
      updateUserState(updated)
      toast.success('Profile updated.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSavingProfile(false)
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match.')
      return
    }

    setIsChangingPassword(true)
    try {
      await changeMyPasswordApi({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      toast.success('Password changed.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsChangingPassword(false)
    }
  }

  return (
    <main className="customer-page">
      <header className="customer-page-header">
        <div>
          <p className="eyebrow">Account</p>
          <h1>My profile</h1>
          <p className="page-intro">Manage your personal details and password.</p>
        </div>
        <Link className="button button-secondary" to="/addresses">Manage addresses</Link>
      </header>
      {isLoading && <LoadingState message="Loading your profile…" />}
      {loadError && <ErrorState message={loadError} onRetry={handleProfileRetry} />}
      {!isLoading && !loadError && profile && (
        <div className="account-page-grid">
          <section className="customer-panel">
            <h2>Personal information</h2>
            <form className="auth-form account-form" onSubmit={handleProfileSubmit}>
              <label htmlFor="profileEmail">Email</label>
              <input id="profileEmail" readOnly value={profile.email} />
              <div className="profile-meta">
                <span>Role <strong>{profile.role}</strong></span>
                <span>Status <strong>{profile.status}</strong></span>
              </div>
              <label htmlFor="profileFullName">Full name</label>
              <input autoComplete="name" id="profileFullName" onChange={(event) => setFullName(event.target.value)} required value={fullName} />
              <label htmlFor="profilePhone">Phone</label>
              <input autoComplete="tel" id="profilePhone" onChange={(event) => setPhone(event.target.value)} type="tel" value={phone} />
              <button className="button button-primary" disabled={isSavingProfile} type="submit">
                {isSavingProfile ? 'Saving…' : 'Save profile'}
              </button>
            </form>
          </section>
          <section className="customer-panel">
            <h2>Change password</h2>
            <form className="auth-form account-form" onSubmit={handlePasswordSubmit}>
              <label htmlFor="currentPassword">Current password</label>
              <input autoComplete="current-password" id="currentPassword" onChange={(event) => setCurrentPassword(event.target.value)} required type="password" value={currentPassword} />
              <label htmlFor="profileNewPassword">New password</label>
              <input autoComplete="new-password" id="profileNewPassword" minLength={8} onChange={(event) => setNewPassword(event.target.value)} required type="password" value={newPassword} />
              <label htmlFor="profileConfirmPassword">Confirm new password</label>
              <input autoComplete="new-password" id="profileConfirmPassword" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} />
              <button className="button button-primary" disabled={isChangingPassword} type="submit">
                {isChangingPassword ? 'Changing…' : 'Change password'}
              </button>
            </form>
          </section>
        </div>
      )}
    </main>
  )
}
