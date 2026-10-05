# Catalog / Product / Inventory (P01-P40)

This cluster implements the main backend catalog domain.

## Checklist coverage

- P01-P06: Category, Brand, Product, ProductVariant, ProductImage schema/entities/mapping
- P07-P10: Category CRUD/list/status
- P11-P14: Brand CRUD/list/status
- P15-P17: Product create/update/status
- P18-P23: Variant create/update/status + SKU/price/stock validation
- P24-P26: Product image URL/remove/primary
- P27-P29: Public product list/detail/variants/images
- P30-P36: Search/filter/sort
- P37: Public API returns ACTIVE catalog only
- P38-P39: Stock update + low-stock API
- P40: Admin product list/search/pagination

## Flyway

`V3__catalog_product_inventory_schema.sql` creates:

- `categories`
- `brands`
- `products`
- `product_variants`
- `product_images`

Do not create these tables manually in DBeaver.

## Public APIs

- `GET /api/public/categories`
- `GET /api/public/brands`
- `GET /api/public/products`
- `GET /api/public/products/{id}`

Product list supports:

- `q`
- `categoryId`
- `brandId`
- `minPrice`
- `maxPrice`
- `sort=NEWEST|PRICE_ASC|PRICE_DESC`
- `page`
- `size`

## Admin APIs

Categories:
- `POST /api/admin/categories`
- `PUT /api/admin/categories/{id}`
- `PATCH /api/admin/categories/{id}/status`
- `GET /api/admin/categories`

Brands:
- `POST /api/admin/brands`
- `PUT /api/admin/brands/{id}`
- `PATCH /api/admin/brands/{id}/status`
- `GET /api/admin/brands`

Products:
- `POST /api/admin/products`
- `PUT /api/admin/products/{id}`
- `PATCH /api/admin/products/{id}/status`
- `GET /api/admin/products`

Variants:
- `POST /api/admin/products/{productId}/variants`
- `PUT /api/admin/variants/{id}`
- `PATCH /api/admin/variants/{id}/status`

Images:
- `POST /api/admin/products/{productId}/images`
- `DELETE /api/admin/products/{productId}/images/{imageId}`
- `PUT /api/admin/products/{productId}/images/{imageId}/primary`

Inventory:
- `PUT /api/admin/inventory/variants/{variantId}/stock`
- `GET /api/admin/inventory/low-stock`

`ProductVariant` includes an optimistic-lock `version` column now so later Checkout/Order code can safely build concurrency handling on top of the catalog model.
