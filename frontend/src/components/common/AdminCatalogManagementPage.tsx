import { useEffect, useState, type FormEvent } from 'react'
import type { PageResponse } from '../../types/api'
import type { CatalogStatus } from '../../types/voucher'
import { useToast } from '../../hooks/useToast'
import { Pagination } from './Pagination'
import { ErrorState, LoadingState } from './FeedbackStates'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { catalogStatusLabel } from '../../utils/localization'

export interface CatalogEntry {
  id: number
  name: string
  status: CatalogStatus
}

export interface AdminCatalogConfig {
  entityName: string
  list: (page: number, size: number) => Promise<PageResponse<CatalogEntry>>
  create: (name: string) => Promise<CatalogEntry>
  update: (id: number, name: string) => Promise<CatalogEntry>
  setStatus: (id: number, status: CatalogStatus) => Promise<CatalogEntry>
}

interface CatalogListState {
  key: string
  page?: PageResponse<CatalogEntry>
  error?: string
}

export function AdminCatalogManagementPage({ config }: { config: AdminCatalogConfig }) {
  const toast = useToast()
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<CatalogListState | null>(null)
  const [name, setName] = useState('')
  const [editing, setEditing] = useState<CatalogEntry | null>(null)
  const [saving, setSaving] = useState(false)
  const [workingId, setWorkingId] = useState<number | null>(null)
  const pageSize = 20
  const requestKey = `${config.entityName}:${pageNumber}:${reloadVersion}`
  const currentResult = result?.key === requestKey ? result : null

  useEffect(() => {
    let isCurrent = true
    config.list(pageNumber, pageSize)
      .then((page) => {
        if (isCurrent) setResult({ key: requestKey, page })
      })
      .catch((error: unknown) => {
        if (isCurrent) setResult({ key: requestKey, error: getErrorMessage(error) })
      })
    return () => {
      isCurrent = false
    }
  }, [config, pageNumber, pageSize, requestKey])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const cleanedName = name.trim()
    if (!cleanedName) return
    setSaving(true)
    try {
      if (editing) {
        await config.update(editing.id, cleanedName)
        toast.success(`Đã cập nhật ${config.entityName.toLowerCase()}.`)
      } else {
        await config.create(cleanedName)
        toast.success(`Đã tạo ${config.entityName.toLowerCase()}.`)
      }
      setName('')
      setEditing(null)
      setPageNumber(0)
      setReloadVersion((version) => version + 1)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  async function handleStatus(entry: CatalogEntry) {
    const nextStatus: CatalogStatus = entry.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    setWorkingId(entry.id)
    try {
      await config.setStatus(entry.id, nextStatus)
      toast.success(`${config.entityName} đã chuyển sang trạng thái ${catalogStatusLabel(nextStatus).toLowerCase()}.`)
      setReloadVersion((version) => version + 1)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setWorkingId(null)
    }
  }

  function beginEdit(entry: CatalogEntry) {
    setEditing(entry)
    setName(entry.name)
  }

  function cancelEdit() {
    setEditing(null)
    setName('')
  }

  return (
    <section className="page-section">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Quản trị</p>
          <h1>Quản lý {config.entityName.toLowerCase()}</h1>
        </div>
      </header>
      <form className="admin-inline-form" onSubmit={handleSubmit}>
        <label>
          {editing ? `Chỉnh sửa ${config.entityName.toLowerCase()}` : `Tên ${config.entityName.toLowerCase()} mới`}
          <input
            maxLength={120}
            onChange={(event) => setName(event.target.value)}
            required
            value={name}
          />
        </label>
        <button className="button button-primary" disabled={saving} type="submit">
          {saving ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : `Thêm ${config.entityName.toLowerCase()}`}
        </button>
        {editing && (
          <button className="button button-secondary" onClick={cancelEdit} type="button">
            Hủy
          </button>
        )}
      </form>
      {currentResult === null && <LoadingState message={`Đang tải ${config.entityName.toLowerCase()}...`} />}
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
                <tr><th>Mã</th><th>Tên</th><th>Trạng thái</th><th>Thao tác</th></tr>
              </thead>
              <tbody>
                {currentResult.page.content.map((entry) => (
                  <tr key={entry.id}>
                    <td>{entry.id}</td>
                    <td>{entry.name}</td>
                    <td><span className={`status-badge status-${entry.status.toLowerCase()}`}>{catalogStatusLabel(entry.status)}</span></td>
                    <td>
                      <div className="table-actions">
                        <button className="text-button" onClick={() => beginEdit(entry)} type="button">
                          Chỉnh sửa
                        </button>
                        <button
                          className="text-button"
                          disabled={workingId === entry.id}
                          onClick={() => void handleStatus(entry)}
                          type="button"
                        >
                          {workingId === entry.id ? 'Đang cập nhật...' : entry.status === 'ACTIVE' ? 'Ngừng hoạt động' : 'Kích hoạt'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {currentResult.page.content.length === 0 && <p className="empty-state">Chưa có {config.entityName.toLowerCase()} nào.</p>}
          <Pagination
            onPageChange={setPageNumber}
            page={currentResult.page.page}
            totalElements={currentResult.page.totalElements}
            totalPages={currentResult.page.totalPages}
          />
        </>
      )}
    </section>
  )
}
