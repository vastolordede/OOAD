import { useEffect, useState, type FormEvent } from 'react'
import {
  disableAdminUserApi,
  enableAdminUserApi,
  getAdminUserDetailApi,
  listAdminUsersApi,
} from '../api/admin-user.api'
import { ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { Pagination } from '../components/common/Pagination'
import { useToast } from '../hooks/useToast'
import type { PageResponse } from '../types/api'
import type { UserResponse } from '../types/auth'
import { getErrorMessage } from '../utils/getErrorMessage'
import { roleLabel, userStatusLabel } from '../utils/localization'

interface UserListState {
  key: string
  page?: PageResponse<UserResponse>
  error?: string
}

interface UserDetailState {
  id: number
  user?: UserResponse
  error?: string
}

export function AdminUsersPage() {
  const toast = useToast()
  const [queryDraft, setQueryDraft] = useState('')
  const [query, setQuery] = useState('')
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<UserListState | null>(null)
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null)
  const [detail, setDetail] = useState<UserDetailState | null>(null)
  const [workingId, setWorkingId] = useState<number | null>(null)
  const pageSize = 20
  const requestKey = `${query}:${pageNumber}:${reloadVersion}`
  const currentResult = result?.key === requestKey ? result : null
  const currentDetail = detail?.id === selectedUserId ? detail : null

  useEffect(() => {
    let isCurrent = true
    listAdminUsersApi({
      ...(query ? { q: query } : {}),
      page: pageNumber,
      size: pageSize,
    })
      .then((page) => {
        if (isCurrent) setResult({ key: requestKey, page })
      })
      .catch((error: unknown) => {
        if (isCurrent) setResult({ key: requestKey, error: getErrorMessage(error) })
      })
    return () => {
      isCurrent = false
    }
  }, [pageNumber, pageSize, query, requestKey])

  useEffect(() => {
    if (selectedUserId === null) return
    const userId = selectedUserId
    let isCurrent = true
    getAdminUserDetailApi(userId)
      .then((user) => {
        if (isCurrent) setDetail({ id: userId, user })
      })
      .catch((error: unknown) => {
        if (isCurrent) setDetail({ id: userId, error: getErrorMessage(error) })
      })
    return () => {
      isCurrent = false
    }
  }, [selectedUserId, reloadVersion])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setQuery(queryDraft.trim())
    setPageNumber(0)
  }

  async function handleToggleStatus(user: UserResponse) {
    setWorkingId(user.id)
    try {
      const updated = user.status === 'ACTIVE'
        ? await disableAdminUserApi(user.id)
        : await enableAdminUserApi(user.id)
      setDetail({ id: updated.id, user: updated })
      setResult((current) => current?.key === requestKey && current.page
        ? {
            ...current,
            page: {
              ...current.page,
              content: current.page.content.map((item) => item.id === updated.id ? updated : item),
            },
          }
        : current)
      toast.success(updated.status === 'ACTIVE' ? 'Đã mở khóa tài khoản.' : 'Đã khóa tài khoản.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setWorkingId(null)
    }
  }

  return (
    <section className="page-section">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Quản trị</p>
          <h1>Quản lý tài khoản</h1>
        </div>
      </header>
      <form className="admin-filter-form" onSubmit={handleSearch}>
        <label>
          Tìm kiếm tài khoản
          <input
            onChange={(event) => setQueryDraft(event.target.value)}
            placeholder="Nhập tên hoặc email"
            value={queryDraft}
          />
        </label>
        <button className="button button-secondary" type="submit">Tìm kiếm</button>
      </form>
      {currentResult === null && <LoadingState message="Đang tải danh sách tài khoản..." />}
      {currentResult?.error && (
        <ErrorState
          message={currentResult.error}
          onRetry={() => setReloadVersion((version) => version + 1)}
        />
      )}
      {currentResult?.page && (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr><th>Người dùng</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr>
              </thead>
              <tbody>
                {currentResult.page.content.map((user) => (
                  <tr key={user.id}>
                    <td><strong>{user.fullName}</strong><span className="table-subtext">{user.email}</span></td>
                    <td>{roleLabel(user.role)}</td>
                    <td><span className={`status-badge status-${user.status.toLowerCase()}`}>{userStatusLabel(user.status)}</span></td>
                    <td>
                      <div className="table-actions">
                        <button className="text-button" onClick={() => setSelectedUserId(user.id)} type="button">
                          Chi tiết
                        </button>
                        <button
                          className="text-button"
                          disabled={workingId === user.id}
                          onClick={() => void handleToggleStatus(user)}
                          type="button"
                        >
                          {workingId === user.id ? 'Đang cập nhật...' : user.status === 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {currentResult.page.content.length === 0 && <p className="empty-state">Không tìm thấy tài khoản phù hợp.</p>}
          <Pagination
            onPageChange={setPageNumber}
            page={currentResult.page.page}
            totalElements={currentResult.page.totalElements}
            totalPages={currentResult.page.totalPages}
          />
        </>
      )}
      {selectedUserId !== null && (
        <section aria-label="Chi tiết người dùng" className="admin-detail-panel">
          <header className="section-heading">
            <h2>Chi tiết tài khoản</h2>
            <button className="button button-secondary" onClick={() => setSelectedUserId(null)} type="button">
              Đóng
            </button>
          </header>
          {!currentDetail && <LoadingState message="Đang tải chi tiết tài khoản..." />}
          {currentDetail?.error && (
            <ErrorState
              message={currentDetail.error}
              onRetry={() => setReloadVersion((version) => version + 1)}
            />
          )}
          {currentDetail?.user && (
            <dl className="admin-detail-grid">
              <div><dt>Mã tài khoản</dt><dd>{currentDetail.user.id}</dd></div>
              <div><dt>Email</dt><dd>{currentDetail.user.email}</dd></div>
              <div><dt>Họ và tên</dt><dd>{currentDetail.user.fullName}</dd></div>
              <div><dt>Số điện thoại</dt><dd>{currentDetail.user.phone || 'Chưa cập nhật'}</dd></div>
              <div><dt>Vai trò</dt><dd>{roleLabel(currentDetail.user.role)}</dd></div>
              <div><dt>Trạng thái</dt><dd>{userStatusLabel(currentDetail.user.status)}</dd></div>
              <div><dt>Ngày tạo</dt><dd>{new Date(currentDetail.user.createdAt).toLocaleString('vi-VN')}</dd></div>
              <div><dt>Cập nhật lần cuối</dt><dd>{new Date(currentDetail.user.updatedAt).toLocaleString('vi-VN')}</dd></div>
            </dl>
          )}
        </section>
      )}
    </section>
  )
}
