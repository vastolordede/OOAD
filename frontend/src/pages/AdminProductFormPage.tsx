import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  addProductImageApi,
  createAdminProductApi,
  createProductVariantApi,
  deleteProductImageApi,
  listAdminProductsApi,
  setPrimaryProductImageApi,
  updateAdminProductApi,
  updateAdminProductStatusApi,
  updateProductVariantApi,
  updateProductVariantStatusApi,
  updateVariantStockApi,
  uploadProductImageFileApi,
} from '../api/admin-product.api'
import { listAdminBrandsApi, listAdminCategoriesApi } from '../api/admin-catalog.api'
import { getPublicProductDetailApi } from '../api/products.api'
import { ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { useToast } from '../hooks/useToast'
import type { BrandResponse, CategoryResponse, ProductDetailResponse, ProductImageResponse, ProductSummaryResponse, ProductVariantResponse } from '../types/product'
import type { CatalogStatus } from '../types/voucher'
import { formatCurrency } from '../utils/formatCurrency'
import { getErrorMessage } from '../utils/getErrorMessage'
import { catalogStatusLabel } from '../utils/localization'

const emptyProduct = {
  name: '',
  description: '',
  categoryId: '',
  brandId: '',
}

const variantCacheKey = (id: number) => `admin-product-detail:${id}`

interface LoadedProduct {
  key: string
  summary?: ProductSummaryResponse
  detail?: ProductDetailResponse
  error?: string
}

interface CatalogOptions {
  key: number
  categories?: CategoryResponse[]
  brands?: BrandResponse[]
  error?: string
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isVariant(value: unknown): value is ProductVariantResponse {
  return isObject(value) &&
    typeof value.id === 'number' &&
    typeof value.productId === 'number' &&
    typeof value.productName === 'string' &&
    typeof value.sku === 'string' &&
    typeof value.price === 'number' &&
    typeof value.stock === 'number' &&
    (value.status === 'ACTIVE' || value.status === 'INACTIVE')
}

function isImage(value: unknown): value is ProductImageResponse {
  return isObject(value) &&
    typeof value.id === 'number' &&
    typeof value.imageUrl === 'string' &&
    typeof value.primaryImage === 'boolean' &&
    typeof value.sortOrder === 'number'
}

function isProductDetail(value: unknown): value is ProductDetailResponse {
  return isObject(value) &&
    typeof value.id === 'number' &&
    typeof value.name === 'string' &&
    (typeof value.description === 'string' || value.description === null) &&
    typeof value.categoryId === 'number' &&
    typeof value.categoryName === 'string' &&
    typeof value.brandId === 'number' &&
    typeof value.brandName === 'string' &&
    (value.status === 'ACTIVE' || value.status === 'INACTIVE') &&
    (typeof value.minPrice === 'number' || value.minPrice === null) &&
    Array.isArray(value.variants) && value.variants.every(isVariant) &&
    Array.isArray(value.images) && value.images.every(isImage) &&
    typeof value.createdAt === 'string' &&
    typeof value.updatedAt === 'string'
}

function readCachedDetail(id: number): ProductDetailResponse | null {
  const cacheValue = sessionStorage.getItem(variantCacheKey(id))
  if (!cacheValue) return null
  try {
    const parsed: unknown = JSON.parse(cacheValue)
    return isProductDetail(parsed) && parsed.id === id ? parsed : null
  } catch (error) {
    if (!(error instanceof SyntaxError)) throw error
    sessionStorage.removeItem(variantCacheKey(id))
    return null
  }
}

function toSummary(product: ProductDetailResponse): ProductSummaryResponse {
  const primaryImage = product.images.find((image) => image.primaryImage) ?? product.images[0]
  return {
    id: product.id,
    name: product.name,
    categoryId: product.categoryId,
    categoryName: product.categoryName,
    brandId: product.brandId,
    brandName: product.brandName,
    status: product.status,
    minPrice: product.minPrice,
    primaryImageUrl: primaryImage?.imageUrl ?? null,
    createdAt: product.createdAt,
  }
}

async function loadAdminProduct(id: number): Promise<LoadedProduct> {
  try {
    const detail = await getPublicProductDetailApi(id)
    sessionStorage.setItem(variantCacheKey(id), JSON.stringify(detail))
    return { key: String(id), summary: toSummary(detail), detail }
  } catch (publicError) {
    try {
      let pageNumber = 0
      let lastPage = false
      let summary: ProductSummaryResponse | undefined
      while (!lastPage && !summary) {
        const page = await listAdminProductsApi({ page: pageNumber, size: 100 })
        summary = page.content.find((item) => item.id === id)
        lastPage = page.last
        pageNumber += 1
      }
      if (!summary) {
        throw new Error(`Product ${id} was not found in the admin product list.`, {
          cause: publicError,
        })
      }
      const cachedDetail = readCachedDetail(id)
      const cachedProduct = cachedDetail
        ? {
            ...cachedDetail,
            name: summary.name,
            categoryId: summary.categoryId,
            categoryName: summary.categoryName,
            brandId: summary.brandId,
            brandName: summary.brandName,
            status: summary.status,
            minPrice: summary.minPrice,
          }
        : undefined
      return { key: String(id), summary, detail: cachedProduct }
    } catch (adminError) {
      throw new Error(
        `Unable to load product details (${getErrorMessage(publicError)}). ` +
        `Admin fallback failed (${getErrorMessage(adminError)}).`,
        { cause: adminError },
      )
    }
  }
}

function ProductLoadResult({
  isLoading,
  error,
  onRetry,
}: {
  isLoading: boolean
  error: string | undefined
  onRetry: () => void
}) {
  if (isLoading) return <LoadingState message="Đang tải sản phẩm..." />
  if (error) return <ErrorState message={error} onRetry={onRetry} />
  return null
}

export function AdminProductFormPage() {
  const { productId } = useParams()
  const id = productId ? Number(productId) : null
  const validId = id !== null && Number.isSafeInteger(id) && id > 0
  const navigate = useNavigate()
  const toast = useToast()
  const [reloadVersion, setReloadVersion] = useState(0)
  const [loadedProduct, setLoadedProduct] = useState<LoadedProduct | null>(null)
  const [optionsVersion, setOptionsVersion] = useState(0)
  const [options, setOptions] = useState<CatalogOptions | null>(null)
  const [formState, setFormState] = useState<{
    key: string
    values: typeof emptyProduct
  } | null>(null)
  const [isSavingProduct, setIsSavingProduct] = useState(false)
  const [variantForm, setVariantForm] = useState({ sku: '', price: '', stock: '0' })
  const [editingVariantId, setEditingVariantId] = useState<number | null>(null)
  const [stockDrafts, setStockDrafts] = useState<Record<number, string>>({})
  const [isSavingVariant, setIsSavingVariant] = useState(false)
  const [imageUrl, setImageUrl] = useState('')
  const [imageSortOrder, setImageSortOrder] = useState('0')
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [isSavingImage, setIsSavingImage] = useState(false)
  const requestKey = `${id ?? 'new'}:${reloadVersion}`
  const currentProduct = loadedProduct?.key === requestKey ? loadedProduct : null
  const currentOptions = options?.key === optionsVersion ? options : null
  const productSummary = currentProduct?.summary
  const productDetail = currentProduct?.detail
  const productLoading = id !== null && currentProduct === null
  const defaultForm = productSummary
    ? {
        name: productSummary.name,
        description: productDetail?.description ?? '',
        categoryId: String(productSummary.categoryId),
        brandId: String(productSummary.brandId),
      }
    : emptyProduct
  const form = formState?.key === requestKey ? formState.values : defaultForm

  useEffect(() => {
    if (!validId || id === null) return
    let isCurrent = true
    loadAdminProduct(id)
      .then((loaded) => {
        if (isCurrent) setLoadedProduct({ ...loaded, key: requestKey })
      })
      .catch((error: unknown) => {
        if (isCurrent) setLoadedProduct({ key: requestKey, error: getErrorMessage(error) })
      })
    return () => {
      isCurrent = false
    }
  }, [id, requestKey, validId])

  useEffect(() => {
    let isCurrent = true
    Promise.all([
      listAdminCategoriesApi({ page: 0, size: 100 }),
      listAdminBrandsApi({ page: 0, size: 100 }),
    ])
      .then(([categoryPage, brandPage]) => {
        if (isCurrent) {
          setOptions({
            key: optionsVersion,
            categories: categoryPage.content,
            brands: brandPage.content,
          })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) setOptions({ key: optionsVersion, error: getErrorMessage(error) })
      })
    return () => {
      isCurrent = false
    }
  }, [optionsVersion])

  function saveProductState(detail: ProductDetailResponse) {
    sessionStorage.setItem(variantCacheKey(detail.id), JSON.stringify(detail))
    setLoadedProduct({
      key: requestKey,
      summary: toSummary(detail),
      detail,
    })
  }

  async function handleProductSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const category = Number(form.categoryId)
    const brand = Number(form.brandId)
    if (!Number.isInteger(category) || category <= 0 || !Number.isInteger(brand) || brand <= 0) {
      toast.error('Vui lòng chọn danh mục và thương hiệu hợp lệ.')
      return
    }
    setIsSavingProduct(true)
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || null,
        categoryId: category,
        brandId: brand,
      }
      if (id === null) {
        const created = await createAdminProductApi(payload)
        sessionStorage.setItem(variantCacheKey(created.id), JSON.stringify(created))
        toast.success('Đã tạo sản phẩm. Bạn có thể thêm phân loại và hình ảnh bên dưới.')
        navigate(`/admin/products/${created.id}/edit`, { replace: true })
      } else {
        const updated = await updateAdminProductApi(id, payload)
        saveProductState(updated)
        toast.success('Đã cập nhật thông tin sản phẩm.')
      }
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSavingProduct(false)
    }
  }

  async function handleProductStatus() {
    if (!id || !productSummary) return
    const nextStatus: CatalogStatus = productSummary.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      const updated = await updateAdminProductStatusApi(id, nextStatus)
      saveProductState(updated)
      toast.success(`Sản phẩm đã chuyển sang trạng thái ${catalogStatusLabel(nextStatus).toLowerCase()}.`)
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  }

  function updateProductDetail(update: (current: ProductDetailResponse) => ProductDetailResponse) {
    if (!productDetail) return
    saveProductState(update(productDetail))
  }

  async function handleVariantSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!id || !productDetail) return
    const price = Number(variantForm.price)
    const stock = Number(variantForm.stock)
    if (!variantForm.sku.trim() || !Number.isFinite(price) || price < 0 || !Number.isInteger(stock) || stock < 0) {
      toast.error('Vui lòng nhập mã SKU, giá không âm và số lượng tồn kho là số nguyên không âm.')
      return
    }
    setIsSavingVariant(true)
    try {
      const payload = { sku: variantForm.sku.trim(), price, stock }
      const updated = editingVariantId === null
        ? await createProductVariantApi(id, payload)
        : await updateProductVariantApi(editingVariantId, payload)
      updateProductDetail((current) => ({
        ...current,
        variants: editingVariantId === null
          ? [...current.variants, updated]
          : current.variants.map((variant) => variant.id === updated.id ? updated : variant),
      }))
      setVariantForm({ sku: '', price: '', stock: '0' })
      setEditingVariantId(null)
      toast.success(editingVariantId === null ? 'Đã thêm phân loại sản phẩm.' : 'Đã cập nhật phân loại sản phẩm.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSavingVariant(false)
    }
  }

  function beginVariantEdit(variant: ProductVariantResponse) {
    setEditingVariantId(variant.id)
    setVariantForm({
      sku: variant.sku,
      price: String(variant.price),
      stock: String(variant.stock),
    })
  }

  async function handleVariantStatus(variant: ProductVariantResponse) {
    const nextStatus: CatalogStatus = variant.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      const updated = await updateProductVariantStatusApi(variant.id, nextStatus)
      updateProductDetail((current) => ({
        ...current,
        variants: current.variants.map((item) => item.id === updated.id ? updated : item),
      }))
      toast.success(`Phân loại sản phẩm đã chuyển sang trạng thái ${catalogStatusLabel(nextStatus).toLowerCase()}.`)
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  }

  async function handleStockUpdate(variant: ProductVariantResponse) {
    const stock = Number(stockDrafts[variant.id] ?? String(variant.stock))
    if (!Number.isInteger(stock) || stock < 0) {
      toast.error('Tồn kho phải là số nguyên không âm.')
      return
    }
    try {
      const updated = await updateVariantStockApi(variant.id, { stock })
      updateProductDetail((current) => ({
        ...current,
        variants: current.variants.map((item) => item.id === updated.id ? updated : item),
      }))
      setStockDrafts((current) => ({ ...current, [variant.id]: String(updated.stock) }))
      toast.success('Đã cập nhật tồn kho.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  }

  async function handleImageSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!id || !productDetail) return
    const sortOrder = Number(imageSortOrder)
    if (!imageUrl.trim() || !Number.isInteger(sortOrder) || sortOrder < 0) {
      toast.error('Vui lòng nhập đường dẫn hình ảnh và thứ tự hiển thị không âm.')
      return
    }
    setIsSavingImage(true)
    try {
      const image = await addProductImageApi(id, {
        imageUrl: imageUrl.trim(),
        sortOrder,
      })
      updateProductDetail((current) => ({ ...current, images: [...current.images, image] }))
      setImageUrl('')
      toast.success('Đã thêm hình ảnh sản phẩm.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSavingImage(false)
    }
  }

  async function handleImageUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!id || !productDetail || !imageFile) {
      toast.error('Vui lòng chọn tệp hình ảnh cần tải lên.')
      return
    }
    const sortOrder = Number(imageSortOrder)
    if (!Number.isInteger(sortOrder) || sortOrder < 0) {
      toast.error('Thứ tự hiển thị phải là số nguyên không âm.')
      return
    }
    setIsSavingImage(true)
    try {
      const upload = await uploadProductImageFileApi(id, imageFile, sortOrder)
      updateProductDetail((current) => ({
        ...current,
        images: [...current.images, upload.productImage],
      }))
      setImageFile(null)
      const fileInput = document.getElementById('productImageFile')
      if (fileInput instanceof HTMLInputElement) fileInput.value = ''
      toast.success('Đã tải hình ảnh sản phẩm lên.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSavingImage(false)
    }
  }

  async function handleSetPrimary(image: ProductImageResponse) {
    if (!id) return
    try {
      const updated = await setPrimaryProductImageApi(id, image.id)
      updateProductDetail((current) => ({
        ...current,
        images: current.images.map((item) => ({ ...item, primaryImage: item.id === updated.id })),
      }))
      toast.success('Đã cập nhật hình ảnh chính.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  }

  async function handleDeleteImage(image: ProductImageResponse) {
    if (!id || !window.confirm('Bạn có chắc muốn xóa hình ảnh sản phẩm này?')) return
    try {
      await deleteProductImageApi(id, image.id)
      updateProductDetail((current) => ({
        ...current,
        images: current.images.filter((item) => item.id !== image.id),
      }))
      toast.success('Đã xóa hình ảnh sản phẩm.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    }
  }

  if (productId && !validId) {
    return <section className="page-section"><ErrorState message="Mã sản phẩm không hợp lệ." /></section>
  }

  const productError = loadedProduct?.key === requestKey ? loadedProduct.error : undefined
  const isLoadingCurrentProduct = id !== null && (productLoading || (currentProduct !== null && !productSummary && !productError))

  return (
    <section className="page-section">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Quản trị</p>
          <h1>{id === null ? 'Thêm sản phẩm' : `Quản lý ${productSummary?.name ?? 'sản phẩm'}`}</h1>
        </div>
        <Link className="button button-secondary" to="/admin/products">Quay lại danh sách sản phẩm</Link>
      </header>
      {currentOptions === null && <LoadingState message="Đang tải danh mục và thương hiệu..." />}
      {currentOptions?.error && (
        <ErrorState
          message={currentOptions.error}
          onRetry={() => setOptionsVersion((version) => version + 1)}
        />
      )}
      <ProductLoadResult
        error={productError}
        isLoading={isLoadingCurrentProduct}
        onRetry={() => setReloadVersion((version) => version + 1)}
      />
      {productSummary && (
        <p className="admin-product-status-line">
          Trạng thái <span className={`status-badge status-${productSummary.status.toLowerCase()}`}>{catalogStatusLabel(productSummary.status)}</span>
          <button className="button button-secondary" onClick={() => void handleProductStatus()} type="button">
            {productSummary.status === 'ACTIVE' ? 'Ngừng hoạt động' : 'Kích hoạt sản phẩm'}
          </button>
        </p>
      )}
      <section className="admin-detail-panel">
        <h2>Thông tin sản phẩm</h2>
        <form className="admin-form-grid" onSubmit={handleProductSubmit}>
          <label>
            Tên sản phẩm
            <input
              maxLength={200}
              onChange={(event) => setFormState({
                key: requestKey,
                values: { ...form, name: event.target.value },
              })}
              required
              value={form.name}
            />
          </label>
          <label>
            Category
            <select
              disabled={!currentOptions?.categories}
              onChange={(event) => setFormState({
                key: requestKey,
                values: { ...form, categoryId: event.target.value },
              })}
              required
              value={form.categoryId}
            >
              <option value="">Chọn danh mục</option>
              {currentOptions?.categories?.map((category) => (
                <option disabled={category.status !== 'ACTIVE'} key={category.id} value={category.id}>
                  {category.name}{category.status === 'INACTIVE' ? ' (ngừng hoạt động)' : ''}
                </option>
              ))}
            </select>
          </label>
          <label>
            Brand
            <select
              disabled={!currentOptions?.brands}
              onChange={(event) => setFormState({
                key: requestKey,
                values: { ...form, brandId: event.target.value },
              })}
              required
              value={form.brandId}
            >
              <option value="">Chọn thương hiệu</option>
              {currentOptions?.brands?.map((brand: BrandResponse) => (
                <option disabled={brand.status !== 'ACTIVE'} key={brand.id} value={brand.id}>
                  {brand.name}{brand.status === 'INACTIVE' ? ' (ngừng hoạt động)' : ''}
                </option>
              ))}
            </select>
          </label>
          <label className="admin-form-wide">
            Mô tả
            <textarea
              maxLength={10000}
              onChange={(event) => setFormState({
                key: requestKey,
                values: { ...form, description: event.target.value },
              })}
              rows={4}
              value={form.description}
            />
          </label>
          <div className="admin-form-wide">
            <button className="button button-primary" disabled={isSavingProduct || !currentOptions?.categories || !currentOptions.brands} type="submit">
              {isSavingProduct ? 'Đang lưu...' : id === null ? 'Tạo sản phẩm' : 'Lưu sản phẩm'}
            </button>
          </div>
        </form>
      </section>

      {id !== null && productSummary && (
        <>
          {!productDetail && (
            <p className="admin-info-note" role="status">
              API hiện chưa cung cấp endpoint quản trị riêng để tải phân loại và hình ảnh.
              Hãy kích hoạt sản phẩm để tải thông tin công khai hoặc tiếp tục quản lý dữ liệu mới trong phiên này.
            </p>
          )}
          <section className="admin-detail-panel">
            <h2>Phân loại sản phẩm</h2>
            <form className="admin-inline-form" onSubmit={(event) => void handleVariantSubmit(event)}>
              <label>SKU<input maxLength={100} onChange={(event) => setVariantForm((current) => ({ ...current, sku: event.target.value }))} required value={variantForm.sku} /></label>
              <label>Giá bán<input min="0" onChange={(event) => setVariantForm((current) => ({ ...current, price: event.target.value }))} required step="any" type="number" value={variantForm.price} /></label>
              <label>Tồn kho<input min="0" onChange={(event) => setVariantForm((current) => ({ ...current, stock: event.target.value }))} required step="1" type="number" value={variantForm.stock} /></label>
              <button className="button button-primary" disabled={isSavingVariant || !productDetail} type="submit">
                {isSavingVariant ? 'Đang lưu...' : editingVariantId === null ? 'Thêm phân loại' : 'Lưu phân loại'}
              </button>
              {editingVariantId !== null && (
                <button className="button button-secondary" onClick={() => { setEditingVariantId(null); setVariantForm({ sku: '', price: '', stock: '0' }) }} type="button">
                  Hủy chỉnh sửa
                </button>
              )}
            </form>
            {productDetail?.variants.length === 0 && <p className="empty-state">Chưa có phân loại sản phẩm.</p>}
            {productDetail && productDetail.variants.length > 0 && (
              <div className="table-scroll">
                <table className="data-table">
                  <thead><tr><th>Mã SKU</th><th>Giá bán</th><th>Tồn kho</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                  <tbody>
                    {productDetail.variants.map((variant) => (
                      <tr key={variant.id}>
                        <td>{variant.sku}</td>
                        <td>{formatCurrency(variant.price)}</td>
                        <td>
                          <div className="stock-edit">
                            <input
                              aria-label={`Tồn kho của ${variant.sku}`}
                              min="0"
                              onChange={(event) => setStockDrafts((current) => ({ ...current, [variant.id]: event.target.value }))}
                              step="1"
                              type="number"
                              value={stockDrafts[variant.id] ?? String(variant.stock)}
                            />
                            <button className="text-button" onClick={() => void handleStockUpdate(variant)} type="button">Cập nhật</button>
                          </div>
                        </td>
                        <td><span className={`status-badge status-${variant.status.toLowerCase()}`}>{catalogStatusLabel(variant.status)}</span></td>
                        <td>
                          <div className="table-actions">
                            <button className="text-button" onClick={() => beginVariantEdit(variant)} type="button">Chỉnh sửa</button>
                            <button className="text-button" onClick={() => void handleVariantStatus(variant)} type="button">
                              {variant.status === 'ACTIVE' ? 'Ngừng hoạt động' : 'Kích hoạt'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <section className="admin-detail-panel">
            <h2>Hình ảnh sản phẩm</h2>
            <form className="admin-inline-form" onSubmit={(event) => void handleImageSubmit(event)}>
              <label>Đường dẫn hình ảnh<input onChange={(event) => setImageUrl(event.target.value)} required type="url" value={imageUrl} /></label>
              <label>Thứ tự hiển thị<input min="0" onChange={(event) => setImageSortOrder(event.target.value)} required step="1" type="number" value={imageSortOrder} /></label>
              <button className="button button-primary" disabled={isSavingImage || !productDetail} type="submit">Thêm đường dẫn ảnh</button>
            </form>
            <form className="admin-inline-form" onSubmit={(event) => void handleImageUpload(event)}>
              <label>Tải hình ảnh lên<input accept="image/*" id="productImageFile" onChange={(event) => setImageFile(event.target.files?.[0] ?? null)} type="file" /></label>
              <button className="button button-secondary" disabled={isSavingImage || !productDetail || !imageFile} type="submit">
                {isSavingImage ? 'Đang tải lên...' : 'Tải tệp lên'}
              </button>
            </form>
            {productDetail?.images.length === 0 && <p className="empty-state">Chưa có hình ảnh sản phẩm.</p>}
            {productDetail && productDetail.images.length > 0 && (
              <div className="admin-image-grid">
                {productDetail.images.map((image) => (
                  <article className="admin-image-card" key={image.id}>
                    <img alt="" loading="lazy" src={image.imageUrl} />
                    <p>{image.primaryImage ? 'Hình ảnh chính' : `Thứ tự hiển thị ${image.sortOrder}`}</p>
                    <div className="table-actions">
                      {!image.primaryImage && (
                        <button className="text-button" onClick={() => void handleSetPrimary(image)} type="button">Đặt làm ảnh chính</button>
                      )}
                      <button className="text-button danger-text" onClick={() => void handleDeleteImage(image)} type="button">Xóa</button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </section>
  )
}
