import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAdminOrderDetailApi, updateAdminOrderStatusApi } from '../api/admin-orders.api'
import type { AdminOrderResponse } from '../types/commerce'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatDateTime, orderStatusLabel, paymentMethodLabel, paymentStatusLabel } from '../utils/localization'

interface AdminOrderDetailState {
  key: string
  data?: AdminOrderResponse
  error?: string
}

const nextStatuses: Record<AdminOrderResponse['order']['status'], AdminOrderResponse['order']['status'][]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SHIPPING', 'CANCELLED'],
  SHIPPING: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
}

export function AdminOrderDetailPage() {
  const { orderId } = useParams()
  const toast = useToast()
  const id = Number(orderId)
  const isValidId = Number.isSafeInteger(id) && id > 0
  const [result, setResult] = useState<AdminOrderDetailState | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [isUpdating, setIsUpdating] = useState(false)
  const requestKey = `${id}:${reloadVersion}`
  const currentResult = result?.key === requestKey ? result : null
  const data = currentResult?.data ?? null
  const error = currentResult?.error ?? null
  const isLoading = isValidId && currentResult === null

  useEffect(() => {
    if (!isValidId) return

    let isCurrent = true
    getAdminOrderDetailApi(id)
      .then((order) => {
        if (isCurrent) setResult({ key: requestKey, data: order })
      })
      .catch((requestError: unknown) => {
        if (isCurrent) setResult({ key: requestKey, error: getErrorMessage(requestError) })
      })
    return () => {
      isCurrent = false
    }
  }, [id, isValidId, requestKey])

  async function updateStatus(nextStatus: AdminOrderResponse['order']['status']) {
    if (!data || !nextStatuses[data.order.status].includes(nextStatus)) return
    if (nextStatus === 'CONFIRMED' && data.order.payment?.method === 'ONLINE' && data.order.payment?.status !== 'PAID') {
      toast.error('Đơn ONLINE phải thanh toán thành công trước khi xác nhận.')
      return
    }

    if (nextStatus === 'CANCELLED' && !window.confirm('Bạn có chắc muốn hủy đơn hàng? Tồn kho và lượt sử dụng mã giảm giá sẽ được hoàn lại.')) {
      return
    }

    setIsUpdating(true)
    try {
      const order = await updateAdminOrderStatusApi(id, nextStatus)
      setResult({ key: requestKey, data: { ...data, order } })
      toast.success(`Đơn hàng #${id} đã chuyển sang trạng thái "${orderStatusLabel(nextStatus)}".`)
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setIsUpdating(false)
    }
  }

  return (
    <section className="page-section admin-order-detail-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Quản trị · Quản lý đơn hàng</p>
          <h1>Order #{isValidId ? id : '—'}</h1>
        </div>
        <Link className="button button-secondary button-link" to="/admin/orders">Quay lại danh sách đơn</Link>
      </header>

      {!isValidId && <p className="form-error" role="alert">Mã đơn hàng không hợp lệ.</p>}
      {isLoading && <p className="loading-state" role="status">Đang tải chi tiết đơn hàng...</p>}
      {error && (
        <div className="form-error" role="alert">
          {error}
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
        </div>
      )}
      {data && (
        <>
          <section className="customer-panel admin-order-summary">
            <div className="section-heading">
              <div>
                <span className={`status-badge status-${data.order.status.toLowerCase()}`}>
                  {orderStatusLabel(data.order.status)}
                </span>
                <p className="muted-text">Đặt lúc {formatDateTime(data.order.createdAt)}</p>
              </div>
              <div className="admin-order-status-actions">
                {nextStatuses[data.order.status].map((nextStatus) => (
                  <button
                    className={nextStatus === 'CANCELLED' ? 'button button-secondary text-button-danger' : 'button button-primary'}
                    disabled={isUpdating || (nextStatus === 'CONFIRMED' && data.order.payment?.method === 'ONLINE' && data.order.payment?.status !== 'PAID')}
                    key={nextStatus}
                    onClick={() => void updateStatus(nextStatus)}
                    type="button"
                  >
                    {isUpdating ? 'Đang cập nhật...' : `Chuyển sang: ${orderStatusLabel(nextStatus)}`}
                  </button>
                ))}
              </div>
            </div>
            {data.order.status === 'PENDING' && data.order.payment?.method === 'ONLINE' && data.order.payment?.status !== 'PAID' && (
              <p className="muted-text">Đơn ONLINE chưa được thanh toán: không thể xác nhận trước khi trạng thái thanh toán là PAID.</p>
            )}
            <dl className="payment-details">
              <div><dt>Khách hàng</dt><dd>{data.customerName}</dd></div>
              <div><dt>Email</dt><dd>{data.customerEmail}</dd></div>
              <div><dt>Mã khách hàng</dt><dd>{data.customerId}</dd></div>
              <div><dt>Người nhận</dt><dd>{data.order.receiverName}</dd></div>
              <div><dt>Số điện thoại</dt><dd>{data.order.receiverPhone}</dd></div>
              <div>
                <dt>Địa chỉ nhận hàng</dt>
                <dd>
                  {[data.order.shippingAddressLine, data.order.shippingWard, data.order.shippingDistrict, data.order.shippingCity]
                    .filter(Boolean)
                    .join(', ')}
                </dd>
              </div>
              <div><dt>Thanh toán</dt><dd>{data.order.payment ? `${paymentMethodLabel(data.order.payment.method)} · ${paymentStatusLabel(data.order.payment.status)}` : '—'}</dd></div>
            </dl>
          </section>

          <section className="customer-panel admin-order-items">
            <h2>Sản phẩm</h2>
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>Sản phẩm</th><th>Mã SKU</th><th>Đơn giá</th><th>Số lượng</th><th>Thành tiền</th></tr>
                </thead>
                <tbody>
                  {data.order.items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.productName}</td>
                      <td>{item.sku}</td>
                      <td>{formatCurrency(item.unitPrice)}</td>
                      <td>{item.quantity}</td>
                      <td>{formatCurrency(item.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <dl className="admin-order-totals">
              <div><dt>Tạm tính</dt><dd>{formatCurrency(data.order.subtotal)}</dd></div>
              <div><dt>Phí vận chuyển</dt><dd>{formatCurrency(data.order.shippingFee)}</dd></div>
              <div><dt>Giảm giá{data.order.voucherCode ? ` · ${data.order.voucherCode}` : ''}</dt><dd>−{formatCurrency(data.order.discountAmount)}</dd></div>
              <div><dt>Tổng thanh toán</dt><dd>{formatCurrency(data.order.totalAmount)}</dd></div>
            </dl>
            {data.order.note && <p><strong>Ghi chú của khách hàng:</strong> {data.order.note}</p>}
          </section>
        </>
      )}
    </section>
  )
}
