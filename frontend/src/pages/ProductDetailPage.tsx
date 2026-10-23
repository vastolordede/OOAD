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
          <h1>Product not found</h1>
          <p className="form-error" role="alert">The product ID is invalid.</p>
          <Link className="text-link" to="/">Return home</Link>
        </section>
      </main>
    )
  }

  if (isLoading) {
    return <main className="customer-page"><p className="loading-state" role="status">Loading product…</p></main>
  }

  if (error) {
    return (
      <main className="customer-page">
        <section className="customer-panel">
          <h1>Unable to load product</h1>
          <p className="form-error" role="alert">{error}</p>
          <button
            className="button button-secondary"
            onClick={() => setReloadVersion((version) => version + 1)}
            type="button"
          >
            Retry
          </button>
        </section>
      </main>
    )
  }

  if (!product) {
    return <main className="customer-page"><p className="loading-state" role="status">Loading product…</p></main>
  }

  const primaryImage =
    product.images.find((image) => image.primaryImage) ?? product.images[0] ?? null

  async function handleAddToCart(variantId: number) {
    setAddingVariantId(variantId)
    try {
      await addCartItemApi({ variantId, quantity: 1 })
      toast.success('Product added to cart.')
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
        toast.success('Product removed from wishlist.')
      } else {
        await addWishlistItemApi(productId)
        setWishlistResult({ key: wishlistKey, isWishlisted: true })
        toast.success('Product added to wishlist.')
      }
    } catch (requestError) {
      toast.error(getErrorMessage(requestError))
    } finally {
      setIsUpdatingWishlist(false)
    }
  }

  return (
    <main className="customer-page product-page">
      <Link className="text-link" to="/">Return home</Link>
      <section className="customer-panel product-summary">
        <div className="product-photo">
          {primaryImage ? (
            <img alt={product.name} src={primaryImage.imageUrl} />
          ) : (
            <span>No image available</span>
          )}
        </div>
        <div className="product-information">
          <p className="eyebrow">{product.brandName} · {product.categoryName}</p>
          <h1>{product.name}</h1>
          {product.description && <p>{product.description}</p>}
          <p className="product-price">
            {product.minPrice === null ? 'Price unavailable' : `From ${formatCurrency(product.minPrice)}`}
          </p>
          <h2>Variants</h2>
          <ul className="product-variants">
            {product.variants.map((variant) => (
              <li key={variant.id}>
                <div>
                  <span>{variant.sku}</span>
                  <strong>{formatCurrency(variant.price)}</strong>
                  <span>{variant.stock} in stock</span>
                </div>
                {user?.role === 'CUSTOMER' && (
                  <button
                    className="button button-secondary"
                    disabled={addingVariantId !== null || variant.status !== 'ACTIVE' || variant.stock < 1}
                    onClick={() => void handleAddToCart(variant.id)}
                    type="button"
                  >
                    {addingVariantId === variant.id ? 'Adding…' : 'Add to cart'}
                  </button>
                )}
                {!user && (
                  <Link className="text-link" to="/login">Sign in to buy</Link>
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
                  ? 'Updating…'
                  : wishlistResult?.key !== wishlistKey
                    ? 'Loading wishlist…'
                    : isWishlisted
                      ? 'Remove from wishlist'
                      : 'Add to wishlist'}
              </button>
              <Link className="text-link" to="/cart">View cart</Link>
              <Link className="text-link" to="/wishlist">View wishlist</Link>
            </div>
          )}
        </div>
      </section>
      <ProductReviewsSection productId={product.id} />
    </main>
  )
}
