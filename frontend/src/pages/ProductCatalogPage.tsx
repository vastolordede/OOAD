import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { listPublicBrandsApi, listPublicCategoriesApi, listPublicProductsApi } from '../api/products.api'
import { EmptyState, ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { Pagination } from '../components/common/Pagination'
import { ProductCard } from '../components/common/ProductCard'
import type {
  BrandResponse,
  CategoryResponse,
  ProductSort,
  ProductSummaryResponse,
  PublicProductQueryParams,
} from '../types/product'
import type { PageResponse } from '../types/api'
import { getErrorMessage } from '../utils/getErrorMessage'

const productsPerPage = 12
const defaultSort: ProductSort = 'NEWEST'

interface ProductResult {
  key: string
  page?: PageResponse<ProductSummaryResponse>
  error?: string
}

interface CatalogFilters {
  q: string
  categoryId: string
  brandId: string
  minPrice: string
  maxPrice: string
  sort: ProductSort
}

function parsePositiveInteger(value: string | null): number | undefined {
  if (!value || !/^\d+$/.test(value)) {
    return undefined
  }
  const parsed = Number(value)
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : undefined
}

function parseNonNegativeNumber(value: string | null): number | undefined {
  if (!value || value.trim() === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : undefined
}

function parseSort(value: string | null): ProductSort {
  return value === 'PRICE_ASC' || value === 'PRICE_DESC' ? value : defaultSort
}

function createQuery(searchParams: URLSearchParams): PublicProductQueryParams {
  const pageValue = searchParams.get('page')
  const page = pageValue && /^\d+$/.test(pageValue) ? Number(pageValue) : 0
  const query: PublicProductQueryParams = {
    page: Number.isSafeInteger(page) ? page : 0,
    size: productsPerPage,
    sort: parseSort(searchParams.get('sort')),
  }
  const q = searchParams.get('q')?.trim()
  const categoryId = parsePositiveInteger(searchParams.get('categoryId'))
  const brandId = parsePositiveInteger(searchParams.get('brandId'))
  const minPrice = parseNonNegativeNumber(searchParams.get('minPrice'))
  const maxPrice = parseNonNegativeNumber(searchParams.get('maxPrice'))

  if (q) query.q = q
  if (categoryId !== undefined) query.categoryId = categoryId
  if (brandId !== undefined) query.brandId = brandId
  if (minPrice !== undefined) query.minPrice = minPrice
  if (maxPrice !== undefined) query.maxPrice = maxPrice
  return query
}

function setOptionalParam(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim()
  if (trimmed) {
    params.set(key, trimmed)
  }
}

function createFilters(searchParams: URLSearchParams): CatalogFilters {
  return {
    q: searchParams.get('q') ?? '',
    categoryId: searchParams.get('categoryId') ?? '',
    brandId: searchParams.get('brandId') ?? '',
    minPrice: searchParams.get('minPrice') ?? '',
    maxPrice: searchParams.get('maxPrice') ?? '',
    sort: parseSort(searchParams.get('sort')),
  }
}

export function ProductCatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryString = searchParams.toString()
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [brands, setBrands] = useState<BrandResponse[]>([])
  const [optionsError, setOptionsError] = useState<string | null>(null)
  const [isLoadingOptions, setIsLoadingOptions] = useState(true)
  const [optionsReloadVersion, setOptionsReloadVersion] = useState(0)
  const [productResult, setProductResult] = useState<ProductResult | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [draft, setDraft] = useState(() => ({
    key: queryString,
    values: createFilters(new URLSearchParams(queryString)),
  }))
  const [priceErrorState, setPriceErrorState] = useState<{
    key: string
    message: string
  } | null>(null)
  const filters = draft.key === queryString
    ? draft.values
    : createFilters(new URLSearchParams(queryString))
  const priceError = priceErrorState?.key === queryString ? priceErrorState.message : null
  const requestKey = `${queryString}:${reloadVersion}`
  const currentResult = productResult?.key === requestKey ? productResult : null
  const isLoadingProducts = currentResult === null
  const pageData = currentResult?.page
  const productError = currentResult?.error

  useEffect(() => {
    let isCurrent = true
    Promise.all([listPublicCategoriesApi(), listPublicBrandsApi()])
      .then(([loadedCategories, loadedBrands]) => {
        if (isCurrent) {
          setCategories(loadedCategories.filter((category) => category.status === 'ACTIVE'))
          setBrands(loadedBrands.filter((brand) => brand.status === 'ACTIVE'))
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) setOptionsError(getErrorMessage(error))
      })
      .finally(() => {
        if (isCurrent) setIsLoadingOptions(false)
      })
    return () => {
      isCurrent = false
    }
  }, [optionsReloadVersion])

  useEffect(() => {
    let isCurrent = true
    const query = createQuery(new URLSearchParams(queryString))
    Promise.resolve()
      .then(() => {
        if (
          query.minPrice !== undefined &&
          query.maxPrice !== undefined &&
          query.minPrice > query.maxPrice
        ) {
          throw new Error('Minimum price cannot be greater than maximum price.')
        }
        return listPublicProductsApi(query)
      })
      .then((loadedPage) => {
        if (isCurrent) setProductResult({ key: requestKey, page: loadedPage })
      })
      .catch((error: unknown) => {
        if (isCurrent) setProductResult({ key: requestKey, error: getErrorMessage(error) })
      })
    return () => {
      isCurrent = false
    }
  }, [queryString, reloadVersion, requestKey])

  function updateFilter<K extends keyof CatalogFilters>(key: K, value: CatalogFilters[K]) {
    setDraft({
      key: queryString,
      values: { ...filters, [key]: value },
    })
  }

  function retryOptions() {
    setOptionsError(null)
    setIsLoadingOptions(true)
    setOptionsReloadVersion((version) => version + 1)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const parsedMin = filters.minPrice.trim() === '' ? undefined : Number(filters.minPrice)
    const parsedMax = filters.maxPrice.trim() === '' ? undefined : Number(filters.maxPrice)

    if (
      (parsedMin !== undefined && (!Number.isFinite(parsedMin) || parsedMin < 0)) ||
      (parsedMax !== undefined && (!Number.isFinite(parsedMax) || parsedMax < 0))
    ) {
      setPriceErrorState({ key: queryString, message: 'Prices must be zero or greater.' })
      return
    }
    if (parsedMin !== undefined && parsedMax !== undefined && parsedMin > parsedMax) {
      setPriceErrorState({
        key: queryString,
        message: 'Minimum price cannot be greater than maximum price.',
      })
      return
    }

    setPriceErrorState(null)
    const nextParams = new URLSearchParams()
    setOptionalParam(nextParams, 'q', filters.q)
    if (parsePositiveInteger(filters.categoryId) !== undefined) nextParams.set('categoryId', filters.categoryId)
    if (parsePositiveInteger(filters.brandId) !== undefined) nextParams.set('brandId', filters.brandId)
    if (parsedMin !== undefined) nextParams.set('minPrice', String(parsedMin))
    if (parsedMax !== undefined) nextParams.set('maxPrice', String(parsedMax))
    nextParams.set('sort', filters.sort)
    nextParams.set('page', '0')
    setSearchParams(nextParams)
  }

  function handleReset() {
    setPriceErrorState(null)
    setSearchParams(new URLSearchParams())
  }

  function handlePageChange(newPage: number) {
    const nextParams = new URLSearchParams(queryString)
    nextParams.set('page', String(newPage))
    setSearchParams(nextParams)
  }

  return (
    <main className="customer-page catalog-page">
      <header className="catalog-header">
        <div>
          <p className="eyebrow">OOAD Cosmetics</p>
          <h1>Product catalog</h1>
          <p className="page-intro">Find your next favorite in our beauty collection.</p>
        </div>
      </header>

      {optionsError && (
        <ErrorState
          message={`Unable to load category and brand filters: ${optionsError}`}
          onRetry={retryOptions}
        />
      )}
      <form className="catalog-filters" onSubmit={handleSubmit}>
        <label className="catalog-search">
          Search products
          <input
            onChange={(event) => updateFilter('q', event.target.value)}
            placeholder="Search by product name"
            type="search"
            value={filters.q}
          />
        </label>
        <label>
          Category
          <select
            disabled={isLoadingOptions || optionsError !== null}
            onChange={(event) => updateFilter('categoryId', event.target.value)}
            value={filters.categoryId}
          >
            <option value="">All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
        </label>
        <label>
          Brand
          <select
            disabled={isLoadingOptions || optionsError !== null}
            onChange={(event) => updateFilter('brandId', event.target.value)}
            value={filters.brandId}
          >
            <option value="">All brands</option>
            {brands.map((brand) => (
              <option key={brand.id} value={brand.id}>{brand.name}</option>
            ))}
          </select>
        </label>
        <label>
          Minimum price
          <input
            min="0"
            onChange={(event) => updateFilter('minPrice', event.target.value)}
            type="number"
            value={filters.minPrice}
          />
        </label>
        <label>
          Maximum price
          <input
            min="0"
            onChange={(event) => updateFilter('maxPrice', event.target.value)}
            type="number"
            value={filters.maxPrice}
          />
        </label>
        <label>
          Sort by
          <select
            onChange={(event) => {
              const selectedSort = event.target.value
              if (
                selectedSort === 'NEWEST' ||
                selectedSort === 'PRICE_ASC' ||
                selectedSort === 'PRICE_DESC'
              ) {
                updateFilter('sort', selectedSort)
              }
            }}
            value={filters.sort}
          >
            <option value="NEWEST">Newest arrivals</option>
            <option value="PRICE_ASC">Price: Low to High</option>
            <option value="PRICE_DESC">Price: High to Low</option>
          </select>
        </label>
        <div className="catalog-filter-actions">
          <button className="button button-primary" type="submit">Search</button>
          <button className="button button-secondary" onClick={handleReset} type="button">
            Reset filters
          </button>
        </div>
        {priceError && <p className="form-error catalog-price-error" role="alert">{priceError}</p>}
      </form>

      {isLoadingProducts && <LoadingState message="Loading products…" />}
      {productError && (
        <ErrorState
          message={productError}
          onRetry={productError.startsWith('Minimum price') ? undefined : () => setReloadVersion((version) => version + 1)}
        />
      )}
      {!isLoadingProducts && !productError && pageData?.content.length === 0 && (
        <EmptyState
          action={<button className="button button-secondary" onClick={handleReset} type="button">Clear filters</button>}
          description="Try adjusting your search or filter selections."
          title="No products found"
        />
      )}
      {!isLoadingProducts && !productError && pageData && pageData.content.length > 0 && (
        <>
          <section aria-label="Products" className="product-grid">
            {pageData.content.map((product) => <ProductCard key={product.id} product={product} />)}
          </section>
          <Pagination
            onPageChange={handlePageChange}
            page={pageData.page}
            totalElements={pageData.totalElements}
            totalPages={pageData.totalPages}
          />
        </>
      )}
      {!isLoadingProducts && !productError && pageData?.content.length === 0 && pageData.totalPages > 0 && (
        <Pagination
          onPageChange={handlePageChange}
          page={pageData.page}
          totalElements={pageData.totalElements}
          totalPages={pageData.totalPages}
        />
      )}
    </main>
  )
}
