import { Link } from 'react-router-dom'
import type { ProductSummaryResponse } from '../../types/product'
import { formatCurrency } from '../../utils/formatCurrency'

export function ProductCard({ product }: { product: ProductSummaryResponse }) {
  return (
    <article className="product-card">
      <Link
        aria-label={`View ${product.name}`}
        className="product-card-link"
        to={`/products/${product.id}`}
      >
        <div className="product-card-image">
          {product.primaryImageUrl ? (
            <img alt={product.name} loading="lazy" src={product.primaryImageUrl} />
          ) : (
            <div aria-label={`${product.brandName} ${product.name}`} className="product-card-placeholder">
              <span>{product.brandName}</span>
              <strong>{product.name}</strong>
            </div>
          )}
        </div>
        <p className="product-card-meta">{product.brandName} · {product.categoryName}</p>
        <h2>{product.name}</h2>
        <p className="product-card-price">
          {product.minPrice === null ? 'Contact for price' : formatCurrency(product.minPrice)}
        </p>
        <span className="button button-secondary product-card-action">View details</span>
      </Link>
    </article>
  )
}
