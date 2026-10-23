# Frontend — OOAD Cosmetics

React + TypeScript + Vite frontend for customer checkout/order/review flows and shared admin business screens.

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
| `/products/:productId` | Public | Product detail and visible reviews |
| `/cart` | CUSTOMER | View, update, and remove cart items |
| `/checkout` | CUSTOMER | Select/create shipping address, preview totals, apply/remove voucher, and create COD order |
| `/my-orders` | CUSTOMER | Filter and paginate the current user's orders |
| `/my-orders/:orderId` | CUSTOMER | Order detail, permitted cancellation, and delivered-order review form |
| `/admin/dashboard` | ADMIN | Summary cards, order status counts, and low-stock list |
| `/admin/orders` | ADMIN | Placeholder pending the missing backend Admin Order controller |
| `/admin/vouchers` | ADMIN | Voucher list, filters, create/edit, and activation status |
| `/admin/reviews` | ADMIN | Review moderation |

The reusable layout is defined in `src/components/common/AdminLayout.tsx`. API requests stay in `src/api/*.api.ts` and share `src/api/axios.ts`. Backend authorization remains the source of truth; route guards are a UI aid.

## Backend contracts and business rules

- All responses use the Spring Boot `ApiResponse<T>` envelope; paginated endpoints use `PageResponse<T>`.
- The Axios request interceptor reads `accessToken` and sends `Authorization: Bearer ...`.
- Checkout totals and voucher validity come from `POST /api/checkout/preview`; the frontend does not calculate replacement totals.
- Create order currently uses the supported COD payment method. Online payment remains outside the core UI scope.
- Customer cancellation is shown only for `PENDING` and `CONFIRMED`, matching `OrderStatus.canTransitionTo`.
- Review creation is offered only for `DELIVERED` orders and remains subject to backend rating and duplicate-item validation.
- Admin APIs are protected by the backend's `ADMIN` role. The Admin Order UI is blocked until its controller/HTTP contract is present.

## Design and test references

- Shared wireframes: [../docs/frontend-order-business-wireframes.svg](../docs/frontend-order-business-wireframes.svg)
- Frontend/business handoff: [../docs/frontend-order-business-handoff.md](../docs/frontend-order-business-handoff.md)
- Demo flow: [../docs/frontend-order-business-demo.md](../docs/frontend-order-business-demo.md)
- Test instructions: [../docs/testing.md](../docs/testing.md)
