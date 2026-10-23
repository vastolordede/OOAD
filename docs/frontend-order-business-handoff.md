# Frontend Order & Business — Handoff

## Delivered core UI

- Shared responsive Admin Layout/Sidebar and dependency-free Toast/Notification.
- Spring Boot-backed auth login and role-aware route guards.
- Cart item add/update/remove/clear, checkout address selection/creation, server preview, voucher apply/remove, and COD order creation.
- Customer order list/detail and cancellation only for `PENDING` or `CONFIRMED`.
- Product detail with paginated public reviews; review form only for delivered order items; Admin review moderation.
- Admin voucher list/filter/create/edit/status and Dashboard summary/status/low-stock widgets.
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
| Voucher | `GET/POST /api/admin/vouchers`; `GET/PUT /api/admin/vouchers/{id}`; `PATCH /api/admin/vouchers/{id}/status` |
| Product/review | `GET /api/public/products/{id}`; `GET /api/public/products/{productId}/reviews`; `POST /api/reviews`; `GET /api/reviews/mine` |
| Review moderation | `GET /api/admin/reviews`; `PATCH /api/admin/reviews/{id}/status` |
| Dashboard | `GET /api/admin/dashboard/summary?lowStockThreshold=10` |

DTOs follow the backend records and response envelope. Monetary totals at checkout/order detail come from the backend; the browser only formats VND for display.

## Blocked Admin Order scope

The repository contains `OrderService.adminList`, `adminDetail`, and `updateStatus`, `AdminOrderResponse`, and `UpdateOrderStatusRequest`, but it has no Admin Order controller. Therefore no verified HTTP method/path/query/body contract or controller-level role declaration exists for O40–O42. `/admin/orders` remains an explicit placeholder and does not issue guessed requests. The backend-wide security configuration protects `/api/admin/**` with `ADMIN`; that does not establish the missing route contract.

To finish O40–O42 and T06, provide/add the backend Admin Order controller and confirm its endpoint parameters/status contract. The frontend can then add the list/filter, detail, and status transition UI without guessing.

## Intentionally not included

- Revenue/top-product charts (SHOULD; no chart library was approved).
- Online-payment flow (BONUS; external gateway is not integrated).
- Review edit/delete (SHOULD).
- Wishlist.

## Verification record

- `npm run build`: passed after each implementation group.
- `npm run lint`: passed after each implementation group.
- Focused backend business unit tests passed using the database-independent class filters in [testing.md](testing.md).
- The unfiltered backend `test` task also discovers integration tests; the run was not successful because PostgreSQL was unavailable. See [testing.md](testing.md) for the current Gradle task behavior and safe test-database setup.
- Browser check: login form and Admin layout rendered; desktop navigation verified; at 390px viewport the Admin layout had no horizontal overflow.
- Live API integration was not verified: the local Spring Boot API was not running at `localhost:8080` during browser checks. The UI surfaced connection errors rather than successful responses. Run the manual flows in [testing.md](testing.md) with a configured backend/database before marking O46, V17, R15, T05–T09, or T13 complete.
