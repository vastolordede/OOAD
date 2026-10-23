import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getProductDetailApi } from '../api/products.api'
import { addCartItemApi } from '../api/cart.api'
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
            <Link className="text-link product-cart-link" to="/cart">View cart</Link>
          )}
        </div>
      </section>
      <ProductReviewsSection productId={product.id} />
    </main>
  )
}
