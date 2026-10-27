import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { listAdminProductsApi, updateAdminProductStatusApi } from '../api/admin-product.api'
import { ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { Pagination } from '../components/common/Pagination'
import { useToast } from '../hooks/useToast'
import type { PageResponse } from '../types/api'
import type { ProductSummaryResponse } from '../types/product'
import type { CatalogStatus } from '../types/voucher'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

interface ProductListState {
  key: string
  page?: PageResponse<ProductSummaryResponse>
  error?: string
}

export function AdminProductListPage() {
  const toast = useToast()
  const [queryDraft, setQueryDraft] = useState('')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<CatalogStatus | ''>('')
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<ProductListState | null>(null)
  const [workingId, setWorkingId] = useState<number | null>(null)
  const pageSize = 20
  const requestKey = `${query}:${status}:${pageNumber}:${reloadVersion}`
  const currentResult = result?.key === requestKey ? result : null

  useEffect(() => {
    let isCurrent = true
    listAdminProductsApi({
      ...(query ? { q: query } : {}),
      ...(status ? { status } : {}),
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
  }, [pageNumber, pageSize, query, requestKey, status])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setQuery(queryDraft.trim())
    setPageNumber(0)
  }

  function handleStatusFilter(value: string) {
    setStatus(value === 'ACTIVE' || value === 'INACTIVE' ? value : '')
    setPageNumber(0)
  }

  async function handleToggleStatus(product: ProductSummaryResponse) {
    const nextStatus: CatalogStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    setWorkingId(product.id)
    try {
      await updateAdminProductStatusApi(product.id, nextStatus)
      toast.success(`${product.name} set to ${nextStatus.toLowerCase()}.`)
      setReloadVersion((version) => version + 1)
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
          <h1>Product management</h1>
        </div>
        <Link className="button button-primary" to="/admin/products/new">Create product</Link>
      </header>
      <form className="admin-filter-form" onSubmit={handleSearch}>
        <label>
          Search products
          <input onChange={(event) => setQueryDraft(event.target.value)} value={queryDraft} />
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
      {currentResult === null && <LoadingState message="Loading products…" />}
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
                <tr><th>Product</th><th>Category</th><th>Brand</th><th>Price</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {currentResult.page.content.map((product) => (
                  <tr key={product.id}>
                    <td>
                      <div className="admin-product-cell">
                        {product.primaryImageUrl
                          ? <img alt="" loading="lazy" src={product.primaryImageUrl} />
                          : <span className="admin-product-placeholder" aria-hidden="true">—</span>}
                        <strong>{product.name}</strong>
                      </div>
                    </td>
                    <td>{product.categoryName}</td>
                    <td>{product.brandName}</td>
                    <td>{product.minPrice === null ? 'Contact for price' : formatCurrency(product.minPrice)}</td>
                    <td><span className={`status-badge status-${product.status.toLowerCase()}`}>{product.status}</span></td>
                    <td>
                      <div className="table-actions">
                        <Link className="text-link" to={`/admin/products/${product.id}/edit`}>Edit / Manage</Link>
                        <button
                          className="text-button"
                          disabled={workingId === product.id}
                          onClick={() => void handleToggleStatus(product)}
                          type="button"
                        >
                          {workingId === product.id ? 'Updating…' : product.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {currentResult.page.content.length === 0 && <p className="empty-state">No products match these filters.</p>}
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
