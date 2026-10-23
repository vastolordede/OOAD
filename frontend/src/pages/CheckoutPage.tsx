import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
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
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatCurrency } from '../utils/formatCurrency'

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
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [addresses, setAddresses] = useState<AddressResponse[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [previewState, setPreviewState] = useState<{
    key: string
    status: 'loading' | 'success' | 'error'
    data?: CheckoutPreviewResponse
  } | null>(null)
  const [voucherCode, setVoucherCode] = useState('')
  const [voucherDraft, setVoucherDraft] = useState('')
  const [note, setNote] = useState('')
  const [addressForm, setAddressForm] = useState<AddressRequest>(emptyAddress)
  const [showAddressForm, setShowAddressForm] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isCreatingAddress, setIsCreatingAddress] = useState(false)
  const [isSubmittingOrder, setIsSubmittingOrder] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [createdOrder, setCreatedOrder] = useState<OrderResponse | null>(null)
  const submitLock = useRef(false)
  const previewKey =
    selectedAddressId === null ? null : `${selectedAddressId}:${voucherCode}`
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
    const requestKey = `${selectedAddressId}:${voucherCode}`

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
  }, [selectedAddressId, toast, voucherCode])

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
      toast.success('Shipping address saved.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsCreatingAddress(false)
    }
  }

  function applyVoucher() {
    const normalizedCode = voucherDraft.trim()

    if (!normalizedCode) {
      toast.error('Enter a voucher code first.')
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
      toast.error('Select a shipping address and wait for the checkout preview.')
      return
    }
    if (cart.hasUnavailableItems) {
      toast.error('Remove unavailable items from the cart before ordering.')
      return
    }

    submitLock.current = true
    setIsSubmittingOrder(true)

    try {
      const order = await createOrderApi({
        addressId: selectedAddressId,
        voucherCode: voucherCode || null,
        paymentMethod: 'COD',
        note: note.trim() || null,
      })
      setCreatedOrder(order)
      toast.success(`Order #${order.id} created successfully.`)
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      submitLock.current = false
      setIsSubmittingOrder(false)
    }
  }

  if (isLoading) {
    return <main className="checkout-page"><p role="status">Loading checkout…</p></main>
  }

  if (loadError) {
    return (
      <main className="checkout-page">
        <section className="checkout-panel">
          <h1>Checkout</h1>
          <p className="form-error" role="alert">{loadError}</p>
        </section>
      </main>
    )
  }

  if (createdOrder) {
    return (
      <main className="checkout-page">
        <section className="checkout-panel order-success">
          <p className="eyebrow">Order confirmed</p>
          <h1>Thank you for your order</h1>
          <p>
            Order <strong>#{createdOrder.id}</strong> is now {createdOrder.status.toLowerCase()}.
          </p>
          <p>Total: <strong>{formatCurrency(createdOrder.totalAmount)}</strong></p>
          <Link className="button button-primary button-link" to="/">
            Continue shopping
          </Link>
        </section>
      </main>
    )
  }

  if (!cart || cart.items.length === 0) {
    return (
      <main className="checkout-page">
        <section className="checkout-panel">
          <h1>Checkout</h1>
          <p>Your cart is empty.</p>
          <Link className="button button-primary button-link" to="/">
            Continue shopping
          </Link>
        </section>
      </main>
    )
  }

  return (
    <main className="checkout-page">
      <header className="checkout-header">
        <div>
          <p className="eyebrow">Customer</p>
          <h1>Checkout</h1>
        </div>
        <Link className="text-link" to="/">Continue shopping</Link>
      </header>

      {cart.hasUnavailableItems && (
        <p className="form-error" role="alert">
          One or more cart items are unavailable or have insufficient stock. Update the cart before checkout.
        </p>
      )}

      <div className="checkout-grid">
        <div className="checkout-main-column">
          <section className="checkout-panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Step 1</p>
                <h2>Shipping address</h2>
              </div>
              <button
                className="button button-secondary"
                onClick={() => setShowAddressForm((visible) => !visible)}
                type="button"
              >
                {showAddressForm ? 'Close form' : 'Add address'}
              </button>
            </div>

            {addresses.length === 0 && !showAddressForm ? (
              <p className="empty-state">Add a shipping address to continue.</p>
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
                      {address.defaultAddress && <span className="default-tag">Default</span>}
                      <span>{address.phone}</span>
                      <span>{addressLabel(address)}</span>
                    </span>
                  </label>
                ))}
              </div>
            )}

            {showAddressForm && (
              <form className="address-form-wrap" onSubmit={handleCreateAddress}>
                <h3>New shipping address</h3>
                <div className="address-form-grid">
                  <label>
                    Recipient name
                    <input
                      maxLength={150}
                      onChange={(event) => setAddressForm({ ...addressForm, receiverName: event.target.value })}
                      required
                      value={addressForm.receiverName}
                    />
                  </label>
                  <label>
                    Phone
                    <input
                      maxLength={30}
                      onChange={(event) => setAddressForm({ ...addressForm, phone: event.target.value })}
                      required
                      value={addressForm.phone}
                    />
                  </label>
                  <label className="address-form-wide">
                    Address
                    <input
                      maxLength={255}
                      onChange={(event) => setAddressForm({ ...addressForm, addressLine: event.target.value })}
                      required
                      value={addressForm.addressLine}
                    />
                  </label>
                  <label>
                    Ward
                    <input
                      maxLength={120}
                      onChange={(event) => setAddressForm({ ...addressForm, ward: event.target.value })}
                      value={addressForm.ward ?? ''}
                    />
                  </label>
                  <label>
                    District
                    <input
                      maxLength={120}
                      onChange={(event) => setAddressForm({ ...addressForm, district: event.target.value })}
                      value={addressForm.district ?? ''}
                    />
                  </label>
                  <label>
                    City
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
                    Set as default address
                  </label>
                </div>
                <button className="button button-primary" disabled={isCreatingAddress} type="submit">
                  {isCreatingAddress ? 'Saving…' : 'Save address'}
                </button>
              </form>
            )}
          </section>

          <section className="checkout-panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Step 2</p>
                <h2>Items</h2>
              </div>
              <span className="muted-text">{cart.totalQuantity} items</span>
            </div>
            <ul className="checkout-items">
              {cart.items.map((item) => (
                <li className="checkout-item" key={item.id}>
                  <div className="item-image">
                    {item.imageUrl ? <img alt="" src={item.imageUrl} /> : <span>No image</span>}
                  </div>
                  <div className="item-description">
                    <strong>{item.productName}</strong>
                    <span>SKU: {item.sku}</span>
                    <span>Quantity: {item.quantity}</span>
                  </div>
                  <strong>{formatCurrency(item.lineTotal)}</strong>
                </li>
              ))}
            </ul>
          </section>

          <section className="checkout-panel">
            <label className="note-field" htmlFor="order-note">Order note (optional)</label>
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
          <p className="eyebrow">Step 3</p>
          <h2>Order summary</h2>
          <div className="voucher-entry">
            <label htmlFor="voucher-code">Voucher code</label>
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
                  Remove
                </button>
              ) : (
                <button className="button button-secondary" onClick={applyVoucher} type="button">
                  Apply
                </button>
              )}
            </div>
            {preview?.voucherCode && (
              <p className="voucher-applied">Applied: {preview.voucherCode}</p>
            )}
          </div>

          <div aria-live="polite" className="summary-lines">
            <div><span>Subtotal</span><span>{preview ? formatCurrency(preview.subtotal) : '—'}</span></div>
            <div><span>Discount</span><span>{preview ? `− ${formatCurrency(preview.discountAmount)}` : '—'}</span></div>
            <div><span>Shipping</span><span>{preview ? formatCurrency(preview.shippingFee) : '—'}</span></div>
            <div className="summary-total"><strong>Total</strong><strong>{preview ? formatCurrency(preview.totalAmount) : '—'}</strong></div>
          </div>

          {isPreviewLoading && <p className="muted-text" role="status">Updating totals…</p>}
          {!isPreviewLoading && !preview && selectedAddressId && (
            <p className="form-error" role="alert">Checkout preview is not available. Review the selected address or voucher.</p>
          )}

          <p className="payment-note">Payment method: Cash on delivery (COD)</p>
          <button
            className="button button-primary button-full"
            disabled={!preview || isPreviewLoading || isSubmittingOrder || cart.hasUnavailableItems}
            onClick={() => void handleCreateOrder()}
            type="button"
          >
            {isSubmittingOrder ? 'Creating order…' : 'Confirm order'}
          </button>
        </aside>
      </div>
    </main>
  )
}
