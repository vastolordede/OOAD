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

function formatDate(value: string): string {
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString()
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
      toast.success(`User ${updated.status === 'ACTIVE' ? 'enabled' : 'disabled'}.`)
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
          <p className="eyebrow">Admin</p>
          <h1>User management</h1>
        </div>
      </header>
      <form className="admin-filter-form" onSubmit={handleSearch}>
        <label>
          Search users
          <input
            onChange={(event) => setQueryDraft(event.target.value)}
            placeholder="Name or email"
            value={queryDraft}
          />
        </label>
        <button className="button button-secondary" type="submit">Search</button>
      </form>
      {currentResult === null && <LoadingState message="Loading users…" />}
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
                <tr><th>User</th><th>Role</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {currentResult.page.content.map((user) => (
                  <tr key={user.id}>
                    <td><strong>{user.fullName}</strong><span className="table-subtext">{user.email}</span></td>
                    <td>{user.role}</td>
                    <td><span className={`status-badge status-${user.status.toLowerCase()}`}>{user.status}</span></td>
                    <td>
                      <div className="table-actions">
                        <button className="text-button" onClick={() => setSelectedUserId(user.id)} type="button">
                          Details
                        </button>
                        <button
                          className="text-button"
                          disabled={workingId === user.id}
                          onClick={() => void handleToggleStatus(user)}
                          type="button"
                        >
                          {workingId === user.id ? 'Updating…' : user.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {currentResult.page.content.length === 0 && <p className="empty-state">No users found.</p>}
          <Pagination
            onPageChange={setPageNumber}
            page={currentResult.page.page}
            totalElements={currentResult.page.totalElements}
            totalPages={currentResult.page.totalPages}
          />
        </>
      )}
      {selectedUserId !== null && (
        <section aria-label="User details" className="admin-detail-panel">
          <header className="section-heading">
            <h2>User details</h2>
            <button className="button button-secondary" onClick={() => setSelectedUserId(null)} type="button">
              Close
            </button>
          </header>
          {!currentDetail && <LoadingState message="Loading user details…" />}
          {currentDetail?.error && (
            <ErrorState
              message={currentDetail.error}
              onRetry={() => setReloadVersion((version) => version + 1)}
            />
          )}
          {currentDetail?.user && (
            <dl className="admin-detail-grid">
              <div><dt>ID</dt><dd>{currentDetail.user.id}</dd></div>
              <div><dt>Email</dt><dd>{currentDetail.user.email}</dd></div>
              <div><dt>Full name</dt><dd>{currentDetail.user.fullName}</dd></div>
              <div><dt>Phone</dt><dd>{currentDetail.user.phone || '—'}</dd></div>
              <div><dt>Role</dt><dd>{currentDetail.user.role}</dd></div>
              <div><dt>Status</dt><dd>{currentDetail.user.status}</dd></div>
              <div><dt>Created</dt><dd>{formatDate(currentDetail.user.createdAt)}</dd></div>
              <div><dt>Updated</dt><dd>{formatDate(currentDetail.user.updatedAt)}</dd></div>
            </dl>
          )}
        </section>
      )}
    </section>
  )
}
