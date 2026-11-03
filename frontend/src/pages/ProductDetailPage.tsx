import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getProductDetailApi } from '../api/products.api'
import { addCartItemApi } from '../api/cart.api'
import {
  addWishlistItemApi,
  listWishlistProductIdsApi,
  removeWishlistItemApi,
} from '../api/wishlist.api'
import { ProductReviewsSection } from '../components/common/ProductReviewsSection'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { ProductDetailResponse } from '../types/product'
import { getErrorMessage } from '../utils/getErrorMessage'
import { formatCurrency } from '../utils/formatCurrency'
import { catalogStatusLabel } from '../utils/localization'

interface ProductState {
  key: string
  product?: ProductDetailResponse
  error?: string
}

export function ProductDetailPage() {
  const { productId } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const id = Number(productId)
  const isValidId = Number.isInteger(id) && id > 0
  const [result, setResult] = useState<ProductState | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [addingVariantId, setAddingVariantId] = useState<number | null>(null)
  const [wishlistResult, setWishlistResult] = useState<{ key: string; isWishlisted: boolean } | null>(null)
  const [isUpdatingWishlist, setIsUpdatingWishlist] = useState(false)
  const wishlistKey = `${user?.id ?? 'anonymous'}:${id}`
  const isWishlisted = wishlistResult?.key === wishlistKey && wishlistResult.isWishlisted
  const requestKey = `${id}:${reloadVersion}`
  const currentResult = result?.key === requestKey ? result : null
  const product = currentResult?.product ?? null
  const error = currentResult?.error ?? null
  const isLoading = currentResult === null

  useEffect(() => {
    if (!isValidId) {
      return
    }

    let isCurrent = true
    getProductDetailApi(id)
      .then((loadedProduct) => {
        if (isCurrent) {
          setResult({ key: requestKey, product: loadedProduct })
        }
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setResult({ key: requestKey, error: getErrorMessage(requestError) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [id, isValidId, requestKey])

  useEffect(() => {
    if (!isValidId || user?.role !== 'CUSTOMER') {
      return
    }

    let isCurrent = true
    listWishlistProductIdsApi()
      .then((productIds) => {
        if (isCurrent) {
          setWishlistResult({ key: wishlistKey, isWishlisted: productIds.includes(id) })
        }
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setWishlistResult({ key: wishlistKey, isWishlisted: false })
          toast.error(getErrorMessage(requestError))
        }
      })

    return () => {
      isCurrent = false
    }
  }, [id, isValidId, toast, user?.role, wishlistKey])

  if (!isValidId) {
    return (
      <main className="customer-page">
        <section className="customer-panel">
          <h1>Không tìm thấy sản phẩm</h1>
          <p className="form-error" role="alert">Mã sản phẩm không hợp lệ.</p>
          <Link className="text-link" to="/">Về trang chủ</Link>
        </section>
      </main>
    )
  }

  if (isLoading) {
    return <main className="customer-page"><p className="loading-state" role="status">Đang tải sản phẩm...</p></main>
  }

  if (error) {
    return (
      <main className="customer-page">
        <section className="customer-panel">
          <h1>Không thể tải sản phẩm</h1>
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Thử lại
          </button>
        </section>
      </main>
    )
  }

  if (!product) {
    return <main className="customer-page"><p className="loading-state" role="status">Đang tải sản phẩm...</p></main>
  }

  const primaryImage =
    product.images.find((image) => image.primaryImage) ?? product.images[0] ?? null

  async function handleAddToCart(variantId: number) {
    setAddingVariantId(variantId)
    try {
      await addCartItemApi({ variantId, quantity: 1 })
      toast.success('Đã thêm sản phẩm vào giỏ hàng.')
      navigate('/cart')
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setAddingVariantId(null)
    }
  }

  async function handleToggleWishlist(productId: number) {
    setIsUpdatingWishlist(true)
    try {
      if (isWishlisted) {
        await removeWishlistItemApi(productId)
        setWishlistResult({ key: wishlistKey, isWishlisted: false })
        toast.success('Đã xóa sản phẩm khỏi danh sách yêu thích.')
      } else {
        await addWishlistItemApi(productId)
        setWishlistResult({ key: wishlistKey, isWishlisted: true })
        toast.success('Đã thêm sản phẩm vào danh sách yêu thích.')
      }
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setIsUpdatingWishlist(false)
    }
  }

  return (
    <main className="customer-page product-page">
      <Link className="text-link" to="/">Về trang chủ</Link>
      <section className="customer-panel product-summary">
        <div className="product-photo">
          {primaryImage ? (
            <img alt={product.name} src={primaryImage.imageUrl} />
          ) : (
            <span>Chưa có hình ảnh</span>
          )}
        </div>
        <div className="product-information">
          <p className="eyebrow">{product.brandName} · {product.categoryName}</p>
          <h1>{product.name}</h1>
          {product.description && <p>{product.description}</p>}
          <p className="product-price">
            {product.minPrice === null ? 'Chưa có giá' : `Từ ${formatCurrency(product.minPrice)}`}
          </p>
          <h2>Phân loại sản phẩm</h2>
          <ul className="product-variants">
            {product.variants.map((variant) => (
              <li key={variant.id}>
                <div>
                  <span>{variant.sku}</span>
                  <strong>{formatCurrency(variant.price)}</strong>
                  <span>Tồn kho: {variant.stock}</span>
                </div>
                <span className={`status-badge status-${variant.status.toLowerCase()}`}>{catalogStatusLabel(variant.status)}</span>
                {user?.role === 'CUSTOMER' && (
                  <button
                    className="button button-secondary"
                    disabled={addingVariantId !== null || variant.status !== 'ACTIVE' || variant.stock < 1}
                    onClick={() => void handleAddToCart(variant.id)}
                    type="button"
                  >
                    {addingVariantId === variant.id ? 'Đang thêm...' : 'Thêm vào giỏ hàng'}
                  </button>
                )}
                {!user && (
                  <Link className="text-link" to="/login">Đăng nhập để mua hàng</Link>
                )}
              </li>
            ))}
          </ul>
          {user?.role === 'CUSTOMER' && (
            <div className="product-actions">
              <button
                className="button button-secondary"
                disabled={isUpdatingWishlist || wishlistResult?.key !== wishlistKey}
                onClick={() => void handleToggleWishlist(product.id)}
                type="button"
              >
                {isUpdatingWishlist
                  ? 'Đang cập nhật...'
                  : wishlistResult?.key !== wishlistKey
                    ? 'Đang tải danh sách yêu thích...'
                    : isWishlisted
                      ? 'Xóa khỏi danh sách yêu thích'
                      : 'Thêm vào danh sách yêu thích'}
              </button>
              <Link className="text-link" to="/cart">Xem giỏ hàng</Link>
              <Link className="text-link" to="/wishlist">Xem sản phẩm yêu thích</Link>
            </div>
          )}
        </div>
      </section>
      <ProductReviewsSection productId={product.id} />
    </main>
  )
}
