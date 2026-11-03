import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cancelOrderApi, getOrderDetailApi } from '../api/orders.api'
import { listMyReviewedOrderItemIdsApi } from '../api/reviews.api'
import { OrderReviewForm } from '../components/common/OrderReviewForm'
import type { OrderResponse, OrderStatus } from '../types/commerce'
import type { ReviewResponse } from '../types/review'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatCurrency } from '../utils/formatCurrency'
import { formatDateTime, orderStatusLabel, paymentMethodLabel } from '../utils/localization'

type OrderDetailState =
  | { key: string; status: 'success'; order: OrderResponse }
  | { key: string; status: 'error'; error: string }

interface ReviewLookupState {
  key: string
  orderItemIds: number[]
  error?: string
}

function canCancelOrder(status: OrderStatus): boolean {
  return status === 'PENDING' || status === 'CONFIRMED'
}

function formatAddress(order: OrderResponse): string {
  return [
    order.shippingAddressLine,
    order.shippingWard,
    order.shippingDistrict,
    order.shippingCity,
  ]
    .filter((part): part is string => Boolean(part))
    .join(', ')
}

export function OrderDetailPage() {
  const { orderId } = useParams()
  const toast = useToast()
  const id = Number(orderId)
  const isValidId = Number.isInteger(id) && id > 0
  const [detailState, setDetailState] = useState<OrderDetailState | null>(null)
  const [reviewLookup, setReviewLookup] = useState<ReviewLookupState | null>(null)
  const [isCancelling, setIsCancelling] = useState(false)
  const [reloadVersion, setReloadVersion] = useState(0)
  const requestKey = `${id}:${reloadVersion}`
  const currentDetailState = detailState?.key === requestKey ? detailState : null
  const order = currentDetailState?.status === 'success' ? currentDetailState.order : null
  const loadError = currentDetailState?.status === 'error' ? currentDetailState.error : null
  const isLoading = currentDetailState === null
  const currentReviewLookup = reviewLookup?.key === requestKey ? reviewLookup : null

  useEffect(() => {
    if (!isValidId) {
      return
    }

    let isCurrent = true

    getOrderDetailApi(id)
      .then(async (loadedOrder) => {
        if (isCurrent) {
          setDetailState({ key: requestKey, status: 'success', order: loadedOrder })
        }

        if (isCurrent && loadedOrder.status === 'DELIVERED') {
          try {
            const reviewedIds = await listMyReviewedOrderItemIdsApi(
              loadedOrder.items.map((item) => item.id),
            )
            if (isCurrent) {
              setReviewLookup({ key: requestKey, orderItemIds: reviewedIds })
            }
          } catch (error) {
            if (isCurrent) {
              setReviewLookup({
                key: requestKey,
                orderItemIds: [],
                error: getErrorMessage(error),
              })
              toast.error(`Không thể kiểm tra đánh giá đã gửi: ${getErrorMessage(error)}`)
            }
          }
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setDetailState({ key: requestKey, status: 'error', error: getErrorMessage(error) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [id, isValidId, reloadVersion, requestKey, toast])

  function handleReviewSubmitted(review: ReviewResponse) {
    setReviewLookup((current) => ({
      key: requestKey,
      orderItemIds: [
        ...(current?.key === requestKey ? current.orderItemIds : []),
        review.orderItemId,
      ],
    }))
  }

  async function handleCancel() {
    if (!order || !canCancelOrder(order.status)) {
      return
    }
    if (!window.confirm(`Bạn có chắc muốn hủy đơn hàng #${order.id}?`)) {
      return
    }

    setIsCancelling(true)
    try {
      const cancelledOrder = await cancelOrderApi(order.id)
      setDetailState({ key: requestKey, status: 'success', order: cancelledOrder })
      toast.success(`Đã hủy đơn hàng #${order.id}.`)

      try {
        const refreshedOrder = await getOrderDetailApi(order.id)
        setDetailState({ key: requestKey, status: 'success', order: refreshedOrder })
      } catch (refreshError) {
        toast.error(`Đã hủy đơn hàng nhưng không thể tải lại thông tin: ${getErrorMessage(refreshError)}`)
      }
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsCancelling(false)
    }
  }

  if (!isValidId) {
    return (
      <main className="customer-page">
        <section className="customer-panel">
          <h1>Không tìm thấy đơn hàng</h1>
          <p className="form-error" role="alert">Mã đơn hàng không hợp lệ.</p>
          <Link className="text-link" to="/my-orders">Quay lại đơn hàng</Link>
        </section>
      </main>
    )
  }

  if (isLoading) {
    return <main className="customer-page"><p className="loading-state" role="status">Đang tải đơn hàng...</p></main>
  }

  if (loadError) {
    return (
      <main className="customer-page">
        <section className="customer-panel">
          <h1>Không thể tải đơn hàng</h1>
          <p className="form-error" role="alert">{loadError}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
          <Link className="text-link" to="/my-orders">Quay lại đơn hàng</Link>
        </section>
      </main>
    )
  }

  if (!order) return null

  return (
    <main className="customer-page">
      <Link className="text-link" to="/my-orders">← Quay lại đơn hàng</Link>
      <header className="customer-page-header order-detail-header">
        <div>
          <p className="eyebrow">Order #{order.id}</p>
          <h1>Chi tiết đơn hàng</h1>
          <p className="muted-text">Đặt lúc {formatDateTime(order.createdAt)}</p>
        </div>
        <span className={`status-badge status-${order.status.toLowerCase()}`}>
          {orderStatusLabel(order.status)}
        </span>
      </header>

      <div className="order-detail-grid">
        <div className="order-detail-main">
          <section className="customer-panel">
            <h2>Sản phẩm</h2>
            <ul className="detail-items">
              {order.items.map((item) => (
                <li key={item.id}>
                  <div>
                    <strong>{item.productName}</strong>
                    <span>Mã SKU: {item.sku} · Số lượng: {item.quantity}</span>
                  </div>
                  <span>{formatCurrency(item.lineTotal)}</span>
                </li>
              ))}
            </ul>
            {order.status === 'DELIVERED' && (
              <div className="order-review-section">
                <h3>Đánh giá sản phẩm</h3>
                {isLoading || !currentReviewLookup ? (
                  <p className="muted-text" role="status">Đang kiểm tra đánh giá...</p>
                ) : (
                  order.items.map((item) => (
                    <div className="order-review-item" key={item.id}>
                      <strong>{item.productName}</strong>
                      {currentReviewLookup.orderItemIds.includes(item.id) ? (
                        <p className="review-complete">Bạn đã đánh giá sản phẩm này.</p>
                      ) : (
                        <>
                          {currentReviewLookup.error && (
                            <p className="form-error" role="alert">
                              Không thể xác minh đánh giá trước đó. Hệ thống sẽ ngăn gửi đánh giá trùng lặp.
                            </p>
                          )}
                          <OrderReviewForm
                            onSubmitted={handleReviewSubmitted}
                            orderItemId={item.id}
                          />
                        </>
                      )}
                    </div>
                  ))
                )}
              </div>
            )}
          </section>
          <section className="customer-panel">
            <h2>Thông tin nhận hàng</h2>
            <p><strong>{order.receiverName}</strong></p>
            <p>{order.receiverPhone}</p>
            <p>{formatAddress(order)}</p>
          </section>
        </div>

        <aside className="customer-panel order-totals">
          <h2>Tóm tắt đơn hàng</h2>
          <div><span>Tạm tính</span><span>{formatCurrency(order.subtotal)}</span></div>
          <div><span>Giảm giá</span><span>− {formatCurrency(order.discountAmount)}</span></div>
          <div><span>Phí vận chuyển</span><span>{formatCurrency(order.shippingFee)}</span></div>
          <div className="summary-total"><strong>Tổng thanh toán</strong><strong>{formatCurrency(order.totalAmount)}</strong></div>
          <div><span>Thanh toán</span><span>{order.payment?.method ? paymentMethodLabel(order.payment.method) : '—'}</span></div>
          {order.payment?.method === 'ONLINE' && order.payment.status !== 'PAID' && order.status === 'PENDING' && (
            <Link className="button button-primary button-full" to={`/my-orders/${order.id}/payment`}>
              Tiếp tục thanh toán
            </Link>
          )}
          {order.voucherCode && <div><span>Mã giảm giá</span><span>{order.voucherCode}</span></div>}
          {order.note && <p className="order-note">Ghi chú: {order.note}</p>}
          {canCancelOrder(order.status) && (
            <button
              className="button button-danger button-full"
              disabled={isCancelling}
              onClick={() => void handleCancel()}
              type="button"
            >
              {isCancelling ? 'Đang hủy...' : 'Hủy đơn hàng'}
            </button>
          )}
        </aside>
      </div>
    </main>
  )
}
