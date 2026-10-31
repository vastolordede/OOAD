# Frontend Order & Business — Demo Script

## Before the demo

1. Start PostgreSQL and the Spring Boot backend using the local configuration in the root README.
2. Copy `frontend/.env.example` to `frontend/.env`, then start the frontend from `frontend/` with `npm run dev`.
3. Prepare a test Customer account and an Admin account. Promote an Admin only in a local database following [account-auth.md](account-auth.md). Never write the credentials, JWT, or production account data into Git.
4. Prepare:
   - An active public product with an active variant and enough stock.
   - A Customer shipping address, or be ready to add one in Checkout.
   - Optionally, an active voucher within its validity period and unused by the demo Customer.
   - For the Review portion, a delivered test order with an order item that has not already been reviewed.
   - For Wishlist, an active public product with an active variant.
   - For payment simulation, a separate disposable ONLINE order in `PENDING` state. No external provider or real charge is involved.

Use disposable local test accounts and records. Keep any credentials/data checklist outside Git; never commit passwords, tokens, or personal customer information.

## Demo sequence

### Customer

1. Sign in at `/login` as the test Customer.
2. Open `/products/{productId}` for the prepared active product and inspect the backend-backed variant and public review list.
3. Add an available variant to the cart. In `/cart`, change quantity and save it; verify the subtotal comes from the Cart API.
4. Continue to `/checkout`, choose an address or add one, and inspect the server-provided subtotal, discount, shipping fee, and total.
5. Apply an eligible voucher and show the updated preview; remove it and show the preview without a voucher. An invalid or ineligible code should display the backend error.
6. Confirm a COD order. Open `/my-orders` and its detail; demonstrate cancellation only on a cancellable order if the demo data includes one.
7. Open the delivered test order, submit a rating/comment for an unreviewed item, and show that the item is then marked reviewed.
8. Open `/my-reviews`, edit a review and delete a separate test review after confirmation.
9. Add a product to the wishlist from Product Detail, open `/wishlist`, and remove the test product.
10. To demonstrate O49, open the prepared pending online order and run either simulated success or failure. Do not represent this as a real payment.

### Admin

11. Sign in as the test Admin and show Dashboard cards, order-status counts, low-stock widget, Admin Orders filters/details/status transitions, voucher list/form, and review moderation.
12. Update a test order through valid status transitions in `/admin/orders`, then return to the Customer session and confirm the status in My Orders.
13. Hide or show a review and verify the returned status in the moderation list.

The Admin Order API enforces valid transitions. Use a disposable COD test order for the full PENDING → CONFIRMED → SHIPPING → DELIVERED sequence. ONLINE orders must be paid before confirmation.

## Reset and safety

- Use local test data only; the integration test guidance in [testing.md](testing.md) applies to a dedicated database whose name contains `test`.
- Do not run destructive cleanup against the application database.
- Do not save demo passwords, tokens, or personal customer information in this repository.
