import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createAddressApi, listAddressesApi } from '../api/address.api'
import { getCartApi } from '../api/cart.api'
import { previewCheckoutApi } from '../api/checkout.api'
import { createOrderApi } from '../api/orders.api'
import type {
  AddressRequest,
  AddressResponse,
  CartResponse,
  CheckoutPreviewResponse,
  OrderResponse,
} from '../types/commerce'
import type { PaymentMethod } from '../types/order'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatCurrency } from '../utils/formatCurrency'
import { orderStatusLabel } from '../utils/localization'

const emptyAddress: AddressRequest = {
  receiverName: '',
  phone: '',
  addressLine: '',
  ward: '',
  district: '',
  city: '',
  defaultAddress: false,
}

function addressLabel(address: AddressResponse): string {
  return [
    address.addressLine,
    address.ward,
    address.district,
    address.city,
  ]
    .filter((part): part is string => Boolean(part))
    .join(', ')
}

export function CheckoutPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [addresses, setAddresses] = useState<AddressResponse[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [previewState, setPreviewState] = useState<{
    key: string
    status: 'loading' | 'success' | 'error'
    data?: CheckoutPreviewResponse
  } | null>(null)
  const [previewRetry, setPreviewRetry] = useState(0)
  const [voucherCode, setVoucherCode] = useState('')
  const [voucherDraft, setVoucherDraft] = useState('')
  const [note, setNote] = useState('')
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('COD')
  const [addressForm, setAddressForm] = useState<AddressRequest>(emptyAddress)
  const [showAddressForm, setShowAddressForm] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isCreatingAddress, setIsCreatingAddress] = useState(false)
  const [isSubmittingOrder, setIsSubmittingOrder] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [createdOrder, setCreatedOrder] = useState<OrderResponse | null>(null)
  const submitLock = useRef(false)
  const previewKey =
    selectedAddressId === null ? null : `${selectedAddressId}:${voucherCode}:${previewRetry}`
  const preview =
    previewKey !== null &&
    previewState?.key === previewKey &&
    previewState.status === 'success'
      ? previewState.data ?? null
      : null
  const isPreviewLoading =
    previewKey !== null &&
    (previewState?.key !== previewKey || previewState.status === 'loading')

  useEffect(() => {
    let isCurrent = true

    async function loadCheckout() {
      setIsLoading(true)
      setLoadError(null)

      try {
        const [loadedCart, loadedAddresses] = await Promise.all([
          getCartApi(),
          listAddressesApi(),
        ])

        if (!isCurrent) {
          return
        }

        setCart(loadedCart)
        setAddresses(loadedAddresses)
        const defaultAddress = loadedAddresses.find((address) => address.defaultAddress)
        setSelectedAddressId(defaultAddress?.id ?? loadedAddresses[0]?.id ?? null)
      } catch (error) {
        if (isCurrent) {
          setLoadError(getErrorMessage(error))
        }
      } finally {
        if (isCurrent) {
          setIsLoading(false)
        }
      }
    }

    void loadCheckout()
    return () => {
      isCurrent = false
    }
  }, [])

  useEffect(() => {
    if (selectedAddressId === null) {
      return
    }

    let isCurrent = true
    const requestKey = `${selectedAddressId}:${voucherCode}:${previewRetry}`

    previewCheckoutApi({
      addressId: selectedAddressId,
      voucherCode: voucherCode || null,
    })
      .then((checkoutPreview) => {
        if (isCurrent) {
          setPreviewState({ key: requestKey, status: 'success', data: checkoutPreview })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setPreviewState({ key: requestKey, status: 'error' })
          toast.error(getErrorMessage(error))
        }
      })

    return () => {
      isCurrent = false
    }
  }, [selectedAddressId, toast, voucherCode, previewRetry])

  async function handleCreateAddress(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsCreatingAddress(true)

    try {
      const address = await createAddressApi({
        ...addressForm,
        ward: addressForm.ward?.trim() || null,
        district: addressForm.district?.trim() || null,
      })
      setAddresses((current) => [
        address,
        ...current.map((existing) =>
          address.defaultAddress
            ? { ...existing, defaultAddress: false }
            : existing,
        ),
      ])
      setSelectedAddressId(address.id)
      setAddressForm(emptyAddress)
      setShowAddressForm(false)
      toast.success('Đã lưu địa chỉ nhận hàng.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsCreatingAddress(false)
    }
  }

  function applyVoucher() {
    const normalizedCode = voucherDraft.trim()

    if (!normalizedCode) {
      toast.error('Vui lòng nhập mã giảm giá.')
      return
    }

    setVoucherCode(normalizedCode)
  }

  function removeVoucher() {
    setVoucherCode('')
    setVoucherDraft('')
  }

  async function handleCreateOrder() {
    if (submitLock.current) {
      return
    }
    if (!selectedAddressId || !preview || !cart || cart.items.length === 0) {
      toast.error('Vui lòng chọn địa chỉ nhận hàng và chờ hệ thống tính đơn hàng.')
      return
    }
    if (cart.hasUnavailableItems) {
      toast.error('Vui lòng xóa sản phẩm không khả dụng khỏi giỏ hàng trước khi đặt.')
      return
    }

    submitLock.current = true
    setIsSubmittingOrder(true)

    try {
      const order = await createOrderApi({
        addressId: selectedAddressId,
        voucherCode: voucherCode || null,
        paymentMethod,
        note: note.trim() || null,
      })
      if (paymentMethod === 'ONLINE') {
        navigate(`/my-orders/${order.id}/payment`)
        return
      }
      setCreatedOrder(order)
      toast.success(`Đặt hàng thành công. Mã đơn hàng: #${order.id}.`)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      submitLock.current = false
      setIsSubmittingOrder(false)
    }
  }

  if (isLoading) {
    return <main className="checkout-page"><p role="status">Đang tải thông tin thanh toán...</p></main>
  }

  if (loadError) {
    return (
      <main className="checkout-page">
        <section className="checkout-panel">
          <h1>Thanh toán</h1>
          <p className="form-error" role="alert">{loadError}</p>
        </section>
      </main>
    )
  }

  if (createdOrder) {
    return (
      <main className="checkout-page">
        <section className="checkout-panel order-success">
          <p className="eyebrow">Đặt hàng thành công</p>
          <h1>Cảm ơn bạn đã đặt hàng</h1>
          <p>
            Đơn hàng <strong>#{createdOrder.id}</strong> hiện đang ở trạng thái {orderStatusLabel(createdOrder.status).toLowerCase()}.
          </p>
          <p>Tổng thanh toán: <strong>{formatCurrency(createdOrder.totalAmount)}</strong></p>
          <Link className="button button-primary button-link" to={`/my-orders/${createdOrder.id}`}>
            Xem chi tiết đơn hàng
          </Link>
        </section>
      </main>
    )
  }

  if (!cart || cart.items.length === 0) {
    return (
      <main className="checkout-page">
        <section className="checkout-panel">
          <h1>Thanh toán</h1>
          <p>Giỏ hàng của bạn đang trống.</p>
          <Link className="button button-primary button-link" to="/">
            Tiếp tục mua sắm
          </Link>
        </section>
      </main>
    )
  }

  return (
    <main className="checkout-page">
      <header className="checkout-header">
        <div>
          <p className="eyebrow">Mua sắm</p>
          <h1>Thanh toán</h1>
        </div>
        <Link className="text-link" to="/">Tiếp tục mua sắm</Link>
      </header>

      {cart.hasUnavailableItems && (
        <p className="form-error" role="alert">
          Một hoặc nhiều sản phẩm trong giỏ hàng không khả dụng hoặc không đủ tồn kho. Vui lòng cập nhật giỏ hàng trước khi thanh toán.
        </p>
      )}

      <div className="checkout-grid">
        <div className="checkout-main-column">
          <section className="checkout-panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Bước 1</p>
                <h2>Địa chỉ nhận hàng</h2>
              </div>
              <button
                className="button button-secondary"
                onClick={() => setShowAddressForm((visible) => !visible)}
                type="button"
              >
                {showAddressForm ? 'Đóng biểu mẫu' : 'Thêm địa chỉ'}
              </button>
            </div>

            {addresses.length === 0 && !showAddressForm ? (
              <p className="empty-state">Thêm địa chỉ nhận hàng để tiếp tục.</p>
            ) : (
              <div className="address-list">
                {addresses.map((address) => (
                  <label className="address-option" key={address.id}>
                    <input
                      checked={selectedAddressId === address.id}
                      name="shippingAddress"
                      onChange={() => setSelectedAddressId(address.id)}
                      type="radio"
                    />
                    <span>
                      <strong>{address.receiverName}</strong>
                      {address.defaultAddress && <span className="default-tag">Mặc định</span>}
                      <span>{address.phone}</span>
                      <span>{addressLabel(address)}</span>
                    </span>
                  </label>
                ))}
              </div>
            )}

            {showAddressForm && (
              <form className="address-form-wrap" onSubmit={handleCreateAddress}>
                <h3>Địa chỉ nhận hàng mới</h3>
                <div className="address-form-grid">
                  <label>
                    Tên người nhận
                    <input
                      maxLength={150}
                      onChange={(event) => setAddressForm({ ...addressForm, receiverName: event.target.value })}
                      required
                      value={addressForm.receiverName}
                    />
                  </label>
                  <label>
                    Số điện thoại
                    <input
                      maxLength={30}
                      onChange={(event) => setAddressForm({ ...addressForm, phone: event.target.value })}
                      required
                      value={addressForm.phone}
                    />
                  </label>
                  <label className="address-form-wide">
                    Địa chỉ
                    <input
                      maxLength={255}
                      onChange={(event) => setAddressForm({ ...addressForm, addressLine: event.target.value })}
                      required
                      value={addressForm.addressLine}
                    />
                  </label>
                  <label>
                    Phường / xã
                    <input
                      maxLength={120}
                      onChange={(event) => setAddressForm({ ...addressForm, ward: event.target.value })}
                      value={addressForm.ward ?? ''}
                    />
                  </label>
                  <label>
                    Quận / huyện
                    <input
                      maxLength={120}
                      onChange={(event) => setAddressForm({ ...addressForm, district: event.target.value })}
                      value={addressForm.district ?? ''}
                    />
                  </label>
                  <label>
                    Tỉnh / thành phố
                    <input
                      maxLength={120}
                      onChange={(event) => setAddressForm({ ...addressForm, city: event.target.value })}
                      required
                      value={addressForm.city}
                    />
                  </label>
                  <label className="checkbox-option">
                    <input
                      checked={addressForm.defaultAddress}
                      onChange={(event) => setAddressForm({ ...addressForm, defaultAddress: event.target.checked })}
                      type="checkbox"
                    />
                    Đặt làm địa chỉ mặc định
                  </label>
                </div>
                <button className="button button-primary" disabled={isCreatingAddress} type="submit">
                  {isCreatingAddress ? 'Đang lưu...' : 'Lưu địa chỉ'}
                </button>
              </form>
            )}
          </section>

          <section className="checkout-panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Bước 2</p>
                <h2>Sản phẩm</h2>
              </div>
              <span className="muted-text">{cart.totalQuantity} sản phẩm</span>
            </div>
            <ul className="checkout-items">
              {cart.items.map((item) => (
                <li className="checkout-item" key={item.id}>
                  <div className="item-image">
                    {item.imageUrl ? <img alt="" src={item.imageUrl} /> : <span>Chưa có hình ảnh</span>}
                  </div>
                  <div className="item-description">
                    <strong>{item.productName}</strong>
                    <span>Mã SKU: {item.sku}</span>
                    <span>Số lượng: {item.quantity}</span>
                  </div>
                  <strong>{formatCurrency(item.lineTotal)}</strong>
                </li>
              ))}
            </ul>
          </section>

          <section className="checkout-panel">
            <label className="note-field" htmlFor="order-note">Ghi chú đơn hàng (không bắt buộc)</label>
            <textarea
              id="order-note"
              maxLength={500}
              onChange={(event) => setNote(event.target.value)}
              rows={3}
              value={note}
            />
          </section>
        </div>

        <aside className="checkout-panel checkout-summary">
          <p className="eyebrow">Bước 3</p>
          <h2>Tóm tắt đơn hàng</h2>
          <div className="voucher-entry">
            <label htmlFor="voucher-code">Mã giảm giá</label>
            <div className="voucher-controls">
              <input
                disabled={Boolean(voucherCode)}
                id="voucher-code"
                maxLength={50}
                onChange={(event) => setVoucherDraft(event.target.value)}
                value={voucherDraft}
              />
              {voucherCode ? (
                <button className="button button-secondary" onClick={removeVoucher} type="button">
                  Xóa mã
                </button>
              ) : (
                <button className="button button-secondary" onClick={applyVoucher} type="button">
                  Áp dụng
                </button>
              )}
            </div>
            {preview?.voucherCode && (
              <p className="voucher-applied">Đã áp dụng: {preview.voucherCode}</p>
            )}
          </div>

          <div aria-live="polite" className="summary-lines">
            <div><span>Tạm tính</span><span>{preview ? formatCurrency(preview.subtotal) : '—'}</span></div>
            <div><span>Giảm giá</span><span>{preview ? `− ${formatCurrency(preview.discountAmount)}` : '—'}</span></div>
            <div><span>Phí vận chuyển</span><span>{preview ? formatCurrency(preview.shippingFee) : '—'}</span></div>
            <div className="summary-total"><strong>Tổng thanh toán</strong><strong>{preview ? formatCurrency(preview.totalAmount) : '—'}</strong></div>
          </div>

          {isPreviewLoading && <p className="muted-text" role="status">Đang cập nhật tổng tiền...</p>}
          {!isPreviewLoading && !preview && selectedAddressId && (
            <p className="form-error" role="alert">Chưa thể tính đơn hàng. Vui lòng kiểm tra địa chỉ nhận hàng hoặc mã giảm giá.</p>
          )}

          {previewState?.key === previewKey && previewState.status === 'error' && (
            <button
              className="button button-secondary"
              onClick={() => setPreviewRetry((value) => value + 1)}
              type="button"
            >
              Thử tính lại đơn hàng
            </button>
          )}
          <fieldset className="payment-method-options">
            <legend>Phương thức thanh toán</legend>
            <label className="address-option">
              <input
                checked={paymentMethod === 'COD'}
                name="paymentMethod"
                onChange={() => setPaymentMethod('COD')}
                type="radio"
              />
              <span>
                <strong>Thanh toán khi nhận hàng</strong>
                <span>Thanh toán khi đơn hàng được giao đến.</span>
              </span>
            </label>
            <label className="address-option">
              <input
                checked={paymentMethod === 'ONLINE'}
                name="paymentMethod"
                onChange={() => setPaymentMethod('ONLINE')}
                type="radio"
              />
              <span>
                <strong>Thanh toán trực tuyến (mô phỏng)</strong>
                <span>Không kết nối cổng thanh toán và không phát sinh giao dịch thật.</span>
              </span>
            </label>
          </fieldset>
          <button
            className="button button-primary button-full"
            disabled={!preview || isPreviewLoading || isSubmittingOrder || cart.hasUnavailableItems}
            onClick={() => void handleCreateOrder()}
            type="button"
          >
            {isSubmittingOrder ? 'Đang tạo đơn hàng...' : 'Xác nhận đặt hàng'}
          </button>
        </aside>
      </div>
    </main>
  )
}
