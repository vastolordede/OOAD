import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clearCartApi, getCartApi, removeCartItemApi, updateCartItemApi } from '../api/cart.api'
import type { CartResponse } from '../types/commerce'
import { useToast } from '../hooks/useToast'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'

interface CartResult {
  key: number
  cart?: CartResponse
  error?: string
}

export function CartPage() {
  const toast = useToast()
  const [reloadVersion, setReloadVersion] = useState(0)
  const [result, setResult] = useState<CartResult | null>(null)
  const [quantityDrafts, setQuantityDrafts] = useState<Record<number, string>>({})
  const [busyItemId, setBusyItemId] = useState<number | null>(null)
  const [isClearing, setIsClearing] = useState(false)
  const cart = result?.key === reloadVersion ? result.cart ?? null : null
  const error = result?.key === reloadVersion ? result.error ?? null : null
  const isLoading = result?.key !== reloadVersion
  const hasPendingQuantityChanges =
    cart !== null &&
    Object.entries(quantityDrafts).some(([itemId, value]) => {
      const item = cart.items.find((cartItem) => cartItem.id === Number(itemId))
      return item !== undefined && value !== String(item.quantity)
    })
  const checkoutBlocked = Boolean(cart?.hasUnavailableItems || hasPendingQuantityChanges)

  useEffect(() => {
    let isCurrent = true
    getCartApi()
      .then((loadedCart) => {
        if (isCurrent) {
          setResult({ key: reloadVersion, cart: loadedCart })
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

  async function handleUpdate(itemId: number, currentQuantity: number, stock: number) {
    const quantity = Number(quantityDrafts[itemId] ?? currentQuantity)
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 1000) {
      toast.error('Số lượng phải là số nguyên từ 1 đến 1.000.')
      return
    }
    if (quantity > stock) {
      toast.error(`Hiện chỉ còn ${stock} sản phẩm trong kho.`)
      return
    }
    if (quantity === currentQuantity) {
      return
    }

    setBusyItemId(itemId)
    try {
      const updatedCart = await updateCartItemApi(itemId, { quantity })
      setResult({ key: reloadVersion, cart: updatedCart })
      setQuantityDrafts((drafts) => {
        const next = { ...drafts }
        delete next[itemId]
        return next
      })
      toast.success('Đã cập nhật giỏ hàng.')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setBusyItemId(null)
    }
  }

  async function handleRemove(itemId: number) {
    setBusyItemId(itemId)
    try {
      const updatedCart = await removeCartItemApi(itemId)
      setResult({ key: reloadVersion, cart: updatedCart })
      setQuantityDrafts((drafts) => {
        const next = { ...drafts }
        delete next[itemId]
        return next
      })
      toast.success('Đã xóa sản phẩm khỏi giỏ hàng.')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setBusyItemId(null)
    }
  }

  async function handleClear() {
    if (!window.confirm('Bạn có chắc muốn xóa tất cả sản phẩm khỏi giỏ hàng?')) {
      return
    }

    setIsClearing(true)
    try {
      const updatedCart = await clearCartApi()
      setResult({ key: reloadVersion, cart: updatedCart })
      setQuantityDrafts({})
      toast.success('Đã làm trống giỏ hàng.')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setIsClearing(false)
    }
  }

  return (
    <main className="customer-page">
      <header className="customer-page-header">
        <div>
          <p className="eyebrow">Mua sắm</p>
          <h1>Giỏ hàng</h1>
        </div>
        <Link className="text-link" to="/">Tiếp tục mua sắm</Link>
      </header>

      {isLoading && <p className="loading-state" role="status">Đang tải giỏ hàng...</p>}
      {error && (
        <section className="customer-panel">
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
        </section>
      )}

      {!isLoading && !error && cart && cart.items.length === 0 && (
        <section className="customer-panel empty-orders">
          <h2>Giỏ hàng đang trống</h2>
          <p className="page-intro">Chọn sản phẩm để bắt đầu mua sắm.</p>
          <Link className="button button-primary button-link" to="/">Tiếp tục mua sắm</Link>
        </section>
      )}

      {!isLoading && !error && cart && cart.items.length > 0 && (
        <div className="cart-layout">
          <section className="customer-panel cart-items-panel">
            {cart.hasUnavailableItems && (
              <p className="form-error" role="alert">
                Một số sản phẩm hiện không khả dụng hoặc vượt quá số lượng tồn kho. Vui lòng cập nhật hoặc xóa trước khi thanh toán.
              </p>
            )}
            <div className="table-scroll">
              <table className="data-table cart-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Đơn giá</th>
                    <th>Số lượng</th>
                    <th>Thành tiền</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {cart.items.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.productName}</strong>
                        <span className="table-subtext">Mã SKU: {item.sku}</span>
                        {!item.available && <span className="cart-unavailable">Không khả dụng</span>}
                      </td>
                      <td>{formatCurrency(item.unitPrice)}</td>
                      <td>
                        <input
                          aria-label={`Số lượng ${item.productName}`}
                          className="cart-quantity"
                          max={Math.min(1000, item.stock)}
                          min={1}
                          onChange={(event) =>
                            setQuantityDrafts((drafts) => ({
                              ...drafts,
                              [item.id]: event.target.value,
                            }))
                          }
                          type="number"
                          value={quantityDrafts[item.id] ?? String(item.quantity)}
                        />
                        <span className="table-subtext">Tồn kho: {item.stock}</span>
                      </td>
                      <td>{formatCurrency(item.lineTotal)}</td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="text-button"
                            disabled={busyItemId === item.id || !item.available}
                            onClick={() => void handleUpdate(item.id, item.quantity, item.stock)}
                            type="button"
                          >
                            {busyItemId === item.id ? 'Đang lưu...' : 'Cập nhật'}
                          </button>
                          <button
                            className="text-button text-button-danger"
                            disabled={busyItemId === item.id}
                            onClick={() => void handleRemove(item.id)}
                            type="button"
                          >
                            Xóa
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <button
              className="text-button text-button-danger clear-cart-button"
              disabled={isClearing}
              onClick={() => void handleClear()}
              type="button"
            >
              {isClearing ? 'Đang xóa...' : 'Xóa giỏ hàng'}
            </button>
          </section>

          <aside className="customer-panel cart-summary">
            <h2>Tóm tắt đơn hàng</h2>
            <div><span>Tổng số sản phẩm</span><span>{cart.totalQuantity}</span></div>
            <div className="summary-total"><strong>Tạm tính</strong><strong>{formatCurrency(cart.subtotal)}</strong></div>
            <p className="muted-text">Phí vận chuyển và ưu đãi sẽ được tính khi thanh toán.</p>
            {hasPendingQuantityChanges && (
              <p className="form-error" role="alert">Vui lòng lưu thay đổi số lượng trước khi thanh toán.</p>
            )}
            <Link
              aria-disabled={checkoutBlocked}
              className={`button button-primary button-link button-full${checkoutBlocked ? ' is-disabled' : ''}`}
              onClick={(event) => {
                if (checkoutBlocked) {
                  event.preventDefault()
                }
              }}
              to="/checkout"
            >
              Tiến hành thanh toán
            </Link>
          </aside>
        </div>
      )}
    </main>
  )
}
