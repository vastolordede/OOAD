# Frontend — OOAD Cosmetics

React + TypeScript + Vite frontend for customer account, catalog, checkout/order/review flows, and admin management screens.

## Local development

1. Copy `.env.example` to `.env` in this directory. The supplied local value targets the Spring Boot API at `http://localhost:8080/api`.
2. Install packages and start Vite:

   ```powershell
   npm install
   npm run dev
   ```

3. Run the production build and lint checks:

   ```powershell
   npm run build
   npm run lint
   ```

`VITE_API_URL` must point to the API root that includes `/api`. No API URL or token is embedded in feature code. When the variable is missing, UI routes remain available and API requests fail with an explicit configuration error.

## Routes

| Route | Access | Purpose |
|---|---|---|
| `/login` | Public | Sign in using `POST /api/auth/login` |
| `/register` | Public | Create an account |
| `/forgot-password` | Public | Request a password reset |
| `/reset-password` | Public | Set a new password with a reset token |
| `/` | Public | Welcome page and newest product cards |
| `/products` | Public | Search, filter, sort, and paginate public products |
| `/products/:productId` | Public | Product detail and visible reviews |
| `/profile` | CUSTOMER | View/update profile and change password |
| `/addresses` | CUSTOMER | Add, edit, delete, and set a default delivery address |
| `/cart` | CUSTOMER | View, update, and remove cart items |
| `/checkout` | CUSTOMER | Select/create shipping address, preview totals, apply/remove voucher, and create COD order |
| `/my-orders` | CUSTOMER | Filter and paginate the current user's orders |
| `/my-orders/:orderId` | CUSTOMER | Order detail, permitted cancellation, and delivered-order review form |
| `/my-orders/:orderId/payment` | CUSTOMER | Simulated payment status and success/failure actions for ONLINE orders |
| `/my-reviews` | CUSTOMER | Edit or delete the current user's reviews |
| `/wishlist` | CUSTOMER | View and remove saved products |
| `/admin/dashboard` | ADMIN | Summary cards, order status counts, and low-stock list |
| `/admin/users` | ADMIN | Search users, inspect details, and enable/disable accounts |
| `/admin/categories` | ADMIN | Create, rename, and activate/deactivate categories |
| `/admin/brands` | ADMIN | Create, rename, and activate/deactivate brands |
| `/admin/products` | ADMIN | Search, filter, paginate, and manage product status |
| `/admin/products/new` | ADMIN | Create a product |
| `/admin/products/:productId/edit` | ADMIN | Edit product, variants, images, and stock |
| `/admin/inventory` | ADMIN | Review low-stock variants and update stock |
| `/admin/orders` | ADMIN | Existing placeholder; the backend Admin Order API is not yet wired into this frontend screen |
| `/admin/vouchers` | ADMIN | Voucher list, filters, create/edit, and activation status |
| `/admin/reviews` | ADMIN | Review moderation |

The reusable layouts are defined in `src/components/common/MainLayout.tsx` and `src/components/common/AdminLayout.tsx`. API requests stay in `src/api/*.api.ts` and share `src/api/axios.ts`. Backend authorization remains the source of truth; route guards are a UI aid. Admin product detail uses the public detail endpoint while active and the Admin product list as a fallback for inactive products, because the backend does not expose an Admin product-detail GET endpoint.

## Backend contracts and business rules

- All responses use the Spring Boot `ApiResponse<T>` envelope; paginated endpoints use `PageResponse<T>`.
- The Axios request interceptor reads `accessToken` and sends `Authorization: Bearer ...`.
- Checkout totals and voucher validity come from `POST /api/checkout/preview`; the frontend does not calculate replacement totals.
- Checkout supports COD and the backend's explicitly simulated `ONLINE` method; the latter does not connect to a real provider or charge money.
- Customer cancellation is shown only for `PENDING` and `CONFIRMED`, matching `OrderStatus.canTransitionTo`.
- Review creation is offered only for `DELIVERED` orders and remains subject to backend rating and duplicate-item validation.
- Admin APIs are protected by the backend's `ADMIN` role. Image file upload requires the backend Cloudinary feature to be enabled. The backend Admin Order API exists, but its frontend screen remains a placeholder.

## Design and test references

- Shared wireframes: [../docs/frontend-order-business-wireframes.svg](../docs/frontend-order-business-wireframes.svg)
- Account/catalog/admin wireframes: [../docs/frontend-account-catalog-wireframes.svg](../docs/frontend-account-catalog-wireframes.svg)
- Frontend/business handoff: [../docs/frontend-order-business-handoff.md](../docs/frontend-order-business-handoff.md)
- Demo flow: [../docs/frontend-order-business-demo.md](../docs/frontend-order-business-demo.md)
- Test instructions: [../docs/testing.md](../docs/testing.md)
- Account/catalog/admin integration checks: [../docs/frontend-account-catalog-admin-test-plan.md](../docs/frontend-account-catalog-admin-test-plan.md)
