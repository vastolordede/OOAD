import { useEffect, useState } from 'react'
import { getDashboardSummaryApi } from '../api/dashboard.api'
import type { DashboardSummaryResponse } from '../types/dashboard'
import type { OrderStatus } from '../types/commerce'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

const orderStatuses: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'SHIPPING',
  'DELIVERED',
  'CANCELLED',
]

interface DashboardState {
  key: number
  summary?: DashboardSummaryResponse
  error?: string
}

export function AdminDashboardPage() {
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<DashboardState | null>(null)
  const summary = result?.key === reloadVersion ? result.summary ?? null : null
  const error = result?.key === reloadVersion ? result.error ?? null : null
  const isLoading = result?.key !== reloadVersion

  useEffect(() => {
    let isCurrent = true
    getDashboardSummaryApi()
      .then((data) => {
        if (isCurrent) {
          setResult({ key: reloadVersion, summary: data })
        }
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setResult({ key: reloadVersion, error: getErrorMessage(requestError) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [reloadVersion])

  return (
    <section className="page-section admin-dashboard-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>Dashboard</h1>
        </div>
        <button
          className="button button-secondary"
          disabled={isLoading}
          onClick={() => setReloadVersion((version) => version + 1)}
          type="button"
        >
          Refresh
        </button>
      </header>

      {isLoading && <p className="loading-state" role="status">Loading dashboard…</p>}
      {error && (
        <div>
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
        </div>
      )}

      {summary && (
        <>
          <div className="dashboard-cards">
            <article className="dashboard-card">
              <span>Total customers</span>
              <strong>{summary.totalCustomers.toLocaleString()}</strong>
            </article>
            <article className="dashboard-card">
              <span>Total products</span>
              <strong>{summary.totalProducts.toLocaleString()}</strong>
            </article>
            <article className="dashboard-card">
              <span>Total orders</span>
              <strong>{summary.totalOrders.toLocaleString()}</strong>
            </article>
            <article className="dashboard-card">
              <span>Revenue · delivered orders</span>
              <strong>{formatCurrency(summary.totalRevenue)}</strong>
            </article>
          </div>

          <div className="dashboard-widgets">
            <section className="dashboard-widget">
              <h2>Orders by status</h2>
              <div className="status-counts">
                {orderStatuses.map((status) => (
                  <div className="status-count" key={status}>
                    <span className={`status-badge status-${status.toLowerCase()}`}>{status}</span>
                    <strong>{(summary.ordersByStatus[status] ?? 0).toLocaleString()}</strong>
                  </div>
                ))}
              </div>
            </section>

            <section className="dashboard-widget">
              <header className="section-heading">
                <h2>Low stock</h2>
                <span className="muted-text">At or below {summary.lowStockThreshold} units</span>
              </header>
              {summary.lowStockVariants.length === 0 ? (
                <p className="empty-state">No active variants are below the threshold.</p>
              ) : (
                <div className="table-scroll">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>SKU</th>
                        <th>Stock</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.lowStockVariants.map((variant) => (
                        <tr key={variant.id}>
                          <td>{variant.productName}</td>
                          <td>{variant.sku}</td>
                          <td><strong className="stock-warning">{variant.stock}</strong></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </section>
  )
}
