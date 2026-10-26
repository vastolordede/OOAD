import { useEffect, useState, type FormEvent } from 'react'
import { listLowStockVariantsApi, updateVariantStockApi } from '../api/admin-product.api'
import { ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { Pagination } from '../components/common/Pagination'
import { useToast } from '../hooks/useToast'
import type { PageResponse } from '../types/api'
import type { ProductVariantResponse } from '../types/product'
import { getErrorMessage } from '../utils/getErrorMessage'

interface InventoryState {
  key: string
  page?: PageResponse<ProductVariantResponse>
  error?: string
}

export function AdminInventoryPage() {
  const toast = useToast()
  const [thresholdDraft, setThresholdDraft] = useState('10')
  const [threshold, setThreshold] = useState(10)
  const [pageNumber, setPageNumber] = useState(0)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<InventoryState | null>(null)
  const [stockDrafts, setStockDrafts] = useState<Record<number, string>>({})
  const [workingId, setWorkingId] = useState<number | null>(null)
  const pageSize = 20
  const requestKey = `${threshold}:${pageNumber}:${reloadVersion}`
  const currentResult = result?.key === requestKey ? result : null

  useEffect(() => {
    let isCurrent = true
    listLowStockVariantsApi({ threshold, page: pageNumber, size: pageSize })
      .then((page) => {
        if (isCurrent) setResult({ key: requestKey, page })
      })
      .catch((error: unknown) => {
        if (isCurrent) setResult({ key: requestKey, error: getErrorMessage(error) })
      })
    return () => {
      isCurrent = false
    }
  }, [pageNumber, pageSize, requestKey, threshold])

  function handleThreshold(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextThreshold = Number(thresholdDraft)
    if (!Number.isInteger(nextThreshold) || nextThreshold < 0) {
      toast.error('Threshold must be a non-negative whole number.')
      return
    }
    setThreshold(nextThreshold)
    setPageNumber(0)
  }

  async function handleStockUpdate(variant: ProductVariantResponse) {
    const stock = Number(stockDrafts[variant.id] ?? String(variant.stock))
    if (!Number.isInteger(stock) || stock < 0) {
      toast.error('Stock must be a non-negative whole number.')
      return
    }
    setWorkingId(variant.id)
    try {
      await updateVariantStockApi(variant.id, { stock })
      toast.success(`Stock updated for ${variant.sku}.`)
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
          <h1>Inventory</h1>
        </div>
      </header>
      <form className="admin-filter-form" onSubmit={handleThreshold}>
        <label>
          Low-stock threshold
          <input
            min="0"
            onChange={(event) => setThresholdDraft(event.target.value)}
            step="1"
            type="number"
            value={thresholdDraft}
          />
        </label>
        <button className="button button-secondary" type="submit">Apply threshold</button>
      </form>
      {currentResult === null && <LoadingState message="Loading low-stock variants…" />}
      {currentResult?.error && (
        <ErrorState
          message={currentResult.error}
          onRetry={() => setReloadVersion((version) => version + 1)}
        />
      )}
      {currentResult?.page && (
        <>
          {currentResult.page.content.length === 0 ? (
            <p className="empty-state">No active variants are at or below {threshold} units.</p>
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Product</th><th>SKU</th><th>Current stock</th><th>New stock</th><th>Action</th></tr></thead>
                <tbody>
                  {currentResult.page.content.map((variant) => (
                    <tr key={variant.id}>
                      <td>{variant.productName}</td>
                      <td>{variant.sku}</td>
                      <td><strong className="stock-warning">{variant.stock}</strong></td>
                      <td>
                        <input
                          aria-label={`New stock for ${variant.sku}`}
                          min="0"
                          onChange={(event) => setStockDrafts((current) => ({ ...current, [variant.id]: event.target.value }))}
                          step="1"
                          type="number"
                          value={stockDrafts[variant.id] ?? String(variant.stock)}
                        />
                      </td>
                      <td>
                        <button
                          className="button button-secondary"
                          disabled={workingId === variant.id}
                          onClick={() => void handleStockUpdate(variant)}
                          type="button"
                        >
                          {workingId === variant.id ? 'Updating…' : 'Update stock'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
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
