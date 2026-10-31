# Frontend Order & Business — Handoff

## Delivered core UI

- Shared responsive Admin Layout/Sidebar and dependency-free Toast/Notification.
- Spring Boot-backed auth login and role-aware route guards.
- Cart item add/update/remove/clear, checkout address selection/creation, server preview, voucher apply/remove, and COD order creation.
- Customer order list/detail and cancellation only for `PENDING` or `CONFIRMED`.
- Product detail with paginated public reviews; review form only for delivered order items; Admin review moderation.
- Customer wishlist list/add/remove, and customer review edit/delete using the existing backend contracts.
- Checkout supports COD and the backend's explicitly simulated ONLINE payment flow; this does not connect to a payment provider or charge money.
- Admin voucher list/filter/create/edit/status and Dashboard summary/status/low-stock widgets.
- Admin Order list/filter, detail, and permitted status updates.
- Dashboard delivered-order revenue and top-selling product charts, rendered without an added chart dependency.
- Cart, Checkout, Order, and Dashboard wireframes: [frontend-order-business-wireframes.svg](frontend-order-business-wireframes.svg).

## API contract map

The frontend base URL is `VITE_API_URL` and should include `/api`. `src/api/axios.ts` is the only shared Axios configuration/token layer; feature requests live in `src/api/*.api.ts`.

| Feature | Spring Boot endpoint(s) used |
|---|---|
| Authentication | `POST /api/auth/login` |
| Cart | `GET /api/cart`; `POST /api/cart/items`; `PUT /api/cart/items/{itemId}`; `DELETE /api/cart/items/{itemId}`; `DELETE /api/cart` |
| Address | `GET /api/users/me/addresses`; `POST /api/users/me/addresses` |
| Checkout | `POST /api/checkout/preview` |
| Customer order | `POST /api/orders`; `GET /api/orders`; `GET /api/orders/{id}`; `PUT /api/orders/{id}/cancel` |
| Admin order | `GET /api/admin/orders`; `GET /api/admin/orders/{id}`; `PATCH /api/admin/orders/{id}/status` |
| Voucher | `GET/POST /api/admin/vouchers`; `GET/PUT /api/admin/vouchers/{id}`; `PATCH /api/admin/vouchers/{id}/status` |
| Product/review | `GET /api/public/products/{id}`; `GET /api/public/products/{productId}/reviews`; `POST /api/reviews`; `GET /api/reviews/mine` |
| Review moderation | `GET /api/admin/reviews`; `PATCH /api/admin/reviews/{id}/status` |
| Dashboard | `GET /api/admin/dashboard/summary?lowStockThreshold=10`; `GET /api/admin/dashboard/revenue`; `GET /api/admin/dashboard/top-products` |
| Customer review management | `GET /api/reviews/mine`; `PUT /api/reviews/{id}`; `DELETE /api/reviews/{id}` |
| Wishlist | `GET /api/wishlist`; `GET /api/wishlist/ids`; `POST /api/wishlist/items`; `DELETE /api/wishlist/items/{productId}` |
| Simulated payment | `GET /api/orders/{orderId}/payment`; `POST /api/orders/{orderId}/payment/confirm`; `POST /api/orders/{orderId}/payment/fail` |

DTOs follow the backend records and response envelope. Monetary totals at checkout/order detail come from the backend; the browser only formats VND for display.

## Intentionally not included

- Real online-payment gateway: the implemented O49 UI only uses backend simulation endpoints and must not be presented as a real payment integration.

## Verification record

- `npm run build`: passed after each implementation group.
- `npm run lint`: passed after each implementation group.
- Focused backend business unit tests passed using the database-independent class filters in [testing.md](testing.md).
- The unfiltered backend `test` task also discovers integration tests; the run was not successful because PostgreSQL was unavailable. See [testing.md](testing.md) for the current Gradle task behavior and safe test-database setup.
- Browser check: login form and Admin layout rendered; desktop navigation verified; at 390px viewport the Admin layout had no horizontal overflow.
- The sheet's O36, R13, and DB09 rows are stale: their implementation is already present in commits `cd1eb12`, `4052afc`, and `583e25c`, respectively. Verify/update those sheet entries rather than duplicating the work.
- R17, W05, and the simulated O49 UI now have frontend implementations against existing backend contracts, but still require a live end-to-end check.
- Backend Admin Order HTTP behavior is covered by `DemoFlowApiIntegrationTest`; a live frontend-to-backend browser regression still requires a configured local database and credentials.
- Manual frontend integration flows have not been verified in this handoff. Run the cases in [testing.md](testing.md) before marking O46, V17, R15, T05–T09, or T13 complete.
- The demo profile provides synthetic data; provision/reset disposable Customer/Admin credentials locally. Do not store passwords, tokens, or real personal data in Git.
