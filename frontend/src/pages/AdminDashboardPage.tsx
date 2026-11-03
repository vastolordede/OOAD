import { useEffect, useState } from 'react'
import {
  getDashboardRevenueApi,
  getDashboardSummaryApi,
  getDashboardTopProductsApi,
} from '../api/dashboard.api'
import type { DashboardSummaryResponse } from '../types/dashboard'
import type { RevenuePointResponse, TopProductResponse } from '../types/dashboard'
import type { OrderStatus } from '../types/commerce'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatDate, orderStatusLabel } from '../utils/localization'

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

interface ReportState<T> {
  key: number
  data?: T
  error?: string
}

function revenueBarHeight(value: number, maximum: number): string {
  return `${maximum === 0 ? 2 : Math.max(4, (value / maximum) * 100)}%`
}

export function AdminDashboardPage() {
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<DashboardState | null>(null)
  const [revenueResult, setRevenueResult] = useState<ReportState<RevenuePointResponse[]> | null>(null)
  const [topProductsResult, setTopProductsResult] = useState<ReportState<TopProductResponse[]> | null>(null)
  const summary = result?.key === reloadVersion ? result.summary ?? null : null
  const error = result?.key === reloadVersion ? result.error ?? null : null
  const isLoading = result?.key !== reloadVersion
  const revenue = revenueResult?.key === reloadVersion ? revenueResult.data ?? null : null
  const revenueError = revenueResult?.key === reloadVersion ? revenueResult.error ?? null : null
  const revenueLoading = revenueResult?.key !== reloadVersion
  const topProducts = topProductsResult?.key === reloadVersion ? topProductsResult.data ?? null : null
  const topProductsError = topProductsResult?.key === reloadVersion ? topProductsResult.error ?? null : null
  const topProductsLoading = topProductsResult?.key !== reloadVersion
  const maximumRevenue = revenue ? Math.max(...revenue.map((point) => point.revenue)) : 0
  const maximumProductSales = topProducts
    ? Math.max(...topProducts.map((product) => product.quantitySold))
    : 0

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

  useEffect(() => {
    let isCurrent = true
    getDashboardRevenueApi()
      .then((data) => {
        if (isCurrent) setRevenueResult({ key: reloadVersion, data })
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setRevenueResult({ key: reloadVersion, error: getErrorMessage(requestError) })
        }
      })
    return () => {
      isCurrent = false
    }
  }, [reloadVersion])

  useEffect(() => {
    let isCurrent = true
    getDashboardTopProductsApi()
      .then((data) => {
        if (isCurrent) setTopProductsResult({ key: reloadVersion, data })
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setTopProductsResult({ key: reloadVersion, error: getErrorMessage(requestError) })
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
          <p className="eyebrow">Quản trị</p>
          <h1>Tổng quan</h1>
        </div>
        <button
          className="button button-secondary"
          disabled={isLoading || revenueLoading || topProductsLoading}
          onClick={() => setReloadVersion((version) => version + 1)}
          type="button"
        >
          Làm mới
        </button>
      </header>

      {isLoading && <p className="loading-state" role="status">Đang tải dữ liệu tổng quan...</p>}
      {error && (
        <div>
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
        </div>
      )}

      {summary && (
        <>
          <div className="dashboard-cards">
            <article className="dashboard-card">
              <span>Tổng khách hàng</span>
              <strong>{summary.totalCustomers.toLocaleString()}</strong>
            </article>
            <article className="dashboard-card">
              <span>Tổng sản phẩm</span>
              <strong>{summary.totalProducts.toLocaleString()}</strong>
            </article>
            <article className="dashboard-card">
              <span>Tổng đơn hàng</span>
              <strong>{summary.totalOrders.toLocaleString()}</strong>
            </article>
            <article className="dashboard-card">
              <span>Doanh thu · đơn đã giao</span>
              <strong>{formatCurrency(summary.totalRevenue)}</strong>
            </article>
          </div>

          <div className="dashboard-widgets">
            <section className="dashboard-widget">
              <h2>Đơn hàng theo trạng thái</h2>
              <div className="status-counts">
                {orderStatuses.map((status) => (
                  <div className="status-count" key={status}>
                    <span className={`status-badge status-${status.toLowerCase()}`}>{orderStatusLabel(status)}</span>
                    <strong>{(summary.ordersByStatus[status] ?? 0).toLocaleString()}</strong>
                  </div>
                ))}
              </div>
            </section>

            <section className="dashboard-widget">
              <header className="section-heading">
                <h2>Sắp hết hàng</h2>
                <span className="muted-text">Còn không quá {summary.lowStockThreshold} sản phẩm</span>
              </header>
              {summary.lowStockVariants.length === 0 ? (
                <p className="empty-state">Không có phân loại đang hoạt động nào dưới ngưỡng tồn kho.</p>
              ) : (
                <div className="table-scroll">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Sản phẩm</th>
                        <th>Mã SKU</th>
                        <th>Tồn kho</th>
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

          <div className="dashboard-widgets dashboard-report-widgets">
            <section className="dashboard-widget">
              <h2>Doanh thu · 30 ngày gần đây</h2>
              {revenueLoading && <p className="loading-state" role="status">Đang tải doanh thu...</p>}
              {revenueError && (
                <div>
                  <p className="form-error" role="alert">{revenueError}</p>
                  <button
                    className="button button-secondary"
                    onClick={() => setReloadVersion((version) => version + 1)}
                    type="button"
                  >
                    Thử tải lại báo cáo
                  </button>
                </div>
              )}
              {revenue && revenue.length === 0 && <p className="empty-state">Chưa có dữ liệu doanh thu.</p>}
              {revenue && revenue.length > 0 && (
                <>
                  <div
                    aria-label="Biểu đồ doanh thu đơn hàng đã giao theo ngày"
                    className="revenue-chart"
                    role="img"
                  >
                    {revenue.map((point) => (
                      <div
                        className="revenue-chart-column"
                        key={point.period}
                        title={`${formatDate(point.period)}: ${formatCurrency(point.revenue)} · ${point.orderCount} đơn hàng`}
                      >
                        <span className="revenue-chart-bar" style={{ height: revenueBarHeight(point.revenue, maximumRevenue) }} />
                        <span className="revenue-chart-label">{point.period.slice(-2)}</span>
                      </div>
                    ))}
                  </div>
                  <p className="muted-text">Doanh thu đơn hàng đã giao theo ngày; di chuột để xem tổng tiền.</p>
                </>
              )}
            </section>

            <section className="dashboard-widget">
              <h2>Sản phẩm bán chạy</h2>
              {topProductsLoading && <p className="loading-state" role="status">Đang tải sản phẩm bán chạy...</p>}
              {topProductsError && <p className="form-error" role="alert">{topProductsError}</p>}
              {topProducts && topProducts.length === 0 && (
                <p className="empty-state">Chưa có sản phẩm nào được giao thành công.</p>
              )}
              {topProducts && topProducts.length > 0 && (
                <ol className="top-products-chart">
                  {topProducts.map((product) => {
                    return (
                      <li key={product.productId}>
                        <div className="top-product-label">
                          <strong>{product.productName}</strong>
                          <span>Đã bán {product.quantitySold.toLocaleString()} · {formatCurrency(product.revenue)}</span>
                        </div>
                        <span
                          aria-hidden="true"
                          className="top-product-bar"
                          style={{
                            width: `${maximumProductSales === 0 ? 0 : (product.quantitySold / maximumProductSales) * 100}%`,
                          }}
                        />
                      </li>
                    )
                  })}
                </ol>
              )}
              {topProductsError && (
                <button
                  className="button button-secondary"
                  onClick={() => setReloadVersion((version) => version + 1)}
                  type="button"
                >
                  Thử tải lại báo cáo
                </button>
              )}
            </section>
          </div>
        </>
      )}
    </section>
  )
}
