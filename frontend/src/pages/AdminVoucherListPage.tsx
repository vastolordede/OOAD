import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listVouchersApi, updateVoucherStatusApi } from '../api/vouchers.api'
import type { PageResponse } from '../types/api'
import type { CatalogStatus, VoucherResponse } from '../types/voucher'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

interface VoucherListState {
  key: string
  data?: PageResponse<VoucherResponse>
  error?: string
}

export function AdminVoucherListPage() {
  const toast = useToast()
  const [searchDraft, setSearchDraft] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [status, setStatus] = useState<CatalogStatus | ''>('')
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<VoucherListState | null>(null)
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const pageSize = 20
  const requestKey = `${searchQuery}:${status}:${pageNumber}:${reloadVersion}`
  const pageData = result?.key === requestKey ? result.data ?? null : null
  const pageError = result?.key === requestKey ? result.error ?? null : null
  const isLoading = result?.key !== requestKey

  useEffect(() => {
    let isCurrent = true

    listVouchersApi({
      ...(searchQuery ? { q: searchQuery } : {}),
      ...(status ? { status } : {}),
      page: pageNumber,
      size: pageSize,
    })
      .then((data) => {
        if (isCurrent) {
          setResult({ key: requestKey, data })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setResult({ key: requestKey, error: getErrorMessage(error) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [pageNumber, pageSize, requestKey, searchQuery, status])

  function handleSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSearchQuery(searchDraft.trim())
    setPageNumber(0)
  }

  async function handleStatusUpdate(voucher: VoucherResponse) {
    const nextStatus = voucher.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    setUpdatingId(voucher.id)

    try {
      await updateVoucherStatusApi(voucher.id, nextStatus)
      toast.success(`Voucher ${voucher.code} set to ${nextStatus.toLowerCase()}.`)
      setReloadVersion((version) => version + 1)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setUpdatingId(null)
    }
  }

  function handleStatusFilter(value: string) {
    setStatus(value === 'ACTIVE' || value === 'INACTIVE' ? value : '')
    setPageNumber(0)
  }

  return (
    <section className="page-section admin-voucher-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>Voucher management</h1>
        </div>
        <Link className="button button-primary button-link" to="/admin/vouchers/new">
          Create voucher
        </Link>
      </header>

      <form className="voucher-filters" onSubmit={handleSearch}>
        <label>
          Search code or description
          <input
            onChange={(event) => setSearchDraft(event.target.value)}
            value={searchDraft}
          />
        </label>
        <label>
          Status
          <select onChange={(event) => handleStatusFilter(event.target.value)} value={status}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </label>
        <button className="button button-secondary" type="submit">Search</button>
      </form>

      {isLoading && <p className="loading-state" role="status">Loading vouchers…</p>}
      {pageError && (
        <div className="form-error" role="alert">
          {pageError}
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
        </div>
      )}
      {!isLoading && !pageError && pageData?.content.length === 0 && (
        <p className="empty-state">No vouchers match these filters.</p>
      )}

      {!isLoading && !pageError && pageData && pageData.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Discount</th>
                  <th>Minimum order</th>
                  <th>Usage</th>
                  <th>Validity</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((voucher) => (
                  <tr key={voucher.id}>
                    <td>
                      <strong>{voucher.code}</strong>
                      {voucher.description && <span className="table-subtext">{voucher.description}</span>}
                    </td>
                    <td>
                      {voucher.discountType === 'PERCENTAGE'
                        ? `${voucher.discountValue}%`
                        : formatCurrency(voucher.discountValue)}
                      {voucher.maxDiscount !== null && (
                        <span className="table-subtext">Max {formatCurrency(voucher.maxDiscount)}</span>
                      )}
                    </td>
                    <td>{formatCurrency(voucher.minOrderValue)}</td>
                    <td>{voucher.usedCount} / {voucher.usageLimit ?? '∞'}</td>
                    <td>
                      <span>{new Date(voucher.startDate).toLocaleDateString()}</span>
                      <span className="table-subtext">to {new Date(voucher.endDate).toLocaleDateString()}</span>
                    </td>
                    <td><span className={`status-badge status-${voucher.status.toLowerCase()}`}>{voucher.status}</span></td>
                    <td>
                      <div className="table-actions">
                        <Link className="text-link" to={`/admin/vouchers/${voucher.id}/edit`}>Edit</Link>
                        <button
                          className="text-button"
                          disabled={updatingId === voucher.id}
                          onClick={() => void handleStatusUpdate(voucher)}
                          type="button"
                        >
                          {updatingId === voucher.id ? 'Updating…' : voucher.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav aria-label="Voucher pages" className="pagination">
            <button
              className="button button-secondary"
              disabled={pageData.first}
              onClick={() => setPageNumber((current) => Math.max(0, current - 1))}
              type="button"
            >
              Previous
            </button>
            <span>Page {pageData.page + 1} of {Math.max(pageData.totalPages, 1)}</span>
            <button
              className="button button-secondary"
              disabled={pageData.last}
              onClick={() => setPageNumber((current) => current + 1)}
              type="button"
            >
              Next
            </button>
          </nav>
        </>
      )}
    </section>
  )
}
