import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { changeMyPasswordApi, getMyProfileApi, updateMyProfileApi } from '../api/user.api'
import { ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { UserResponse } from '../types/auth'
import { getErrorMessage } from '../utils/getErrorMessage'
import { roleLabel, userStatusLabel } from '../utils/localization'

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
      toast.success('Cập nhật thông tin thành công.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSavingProfile(false)
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (newPassword !== confirmPassword) {
      toast.error('Mật khẩu mới xác nhận không khớp.')
      return
    }

    setIsChangingPassword(true)
    try {
      await changeMyPasswordApi({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      toast.success('Đổi mật khẩu thành công.')
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
          <p className="eyebrow">Tài khoản</p>
          <h1>Thông tin cá nhân</h1>
          <p className="page-intro">Quản lý thông tin cá nhân và mật khẩu của bạn.</p>
        </div>
        <Link className="button button-secondary" to="/addresses">Quản lý địa chỉ</Link>
      </header>
      {isLoading && <LoadingState message="Đang tải thông tin tài khoản..." />}
      {loadError && <ErrorState message={loadError} onRetry={handleProfileRetry} />}
      {!isLoading && !loadError && profile && (
        <div className="account-page-grid">
          <section className="customer-panel">
            <h2>Thông tin cá nhân</h2>
            <form className="auth-form account-form" onSubmit={handleProfileSubmit}>
              <label htmlFor="profileEmail">Địa chỉ email</label>
              <input id="profileEmail" readOnly value={profile.email} />
              <div className="profile-meta">
                <span>Vai trò <strong>{roleLabel(profile.role)}</strong></span>
                <span>Trạng thái <strong>{userStatusLabel(profile.status)}</strong></span>
              </div>
              <label htmlFor="profileFullName">Họ và tên</label>
              <input autoComplete="name" id="profileFullName" onChange={(event) => setFullName(event.target.value)} required value={fullName} />
              <label htmlFor="profilePhone">Số điện thoại</label>
              <input autoComplete="tel" id="profilePhone" onChange={(event) => setPhone(event.target.value)} type="tel" value={phone} />
              <button className="button button-primary" disabled={isSavingProfile} type="submit">
                {isSavingProfile ? 'Đang lưu...' : 'Lưu thông tin'}
              </button>
            </form>
          </section>
          <section className="customer-panel">
            <h2>Đổi mật khẩu</h2>
            <form className="auth-form account-form" onSubmit={handlePasswordSubmit}>
              <label htmlFor="currentPassword">Mật khẩu hiện tại</label>
              <input autoComplete="current-password" id="currentPassword" onChange={(event) => setCurrentPassword(event.target.value)} required type="password" value={currentPassword} />
              <label htmlFor="profileNewPassword">Mật khẩu mới (ít nhất 8 ký tự)</label>
              <input autoComplete="new-password" id="profileNewPassword" minLength={8} onChange={(event) => setNewPassword(event.target.value)} required type="password" value={newPassword} />
              <label htmlFor="profileConfirmPassword">Xác nhận mật khẩu mới</label>
              <input autoComplete="new-password" id="profileConfirmPassword" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} />
              <button className="button button-primary" disabled={isChangingPassword} type="submit">
                {isChangingPassword ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
              </button>
            </form>
          </section>
        </div>
      )}
    </main>
  )
}
