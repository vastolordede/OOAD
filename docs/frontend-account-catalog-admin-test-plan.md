# Frontend Account, Catalog, and Admin Integration Checks

Run the checks against a configured backend with a test/demo database. Use a non-production environment and accounts only.

## T02 — Account registration and customer profile

1. Open `/register`, create an account with valid full name, email, phone, and matching passwords.
2. Verify a success toast, customer session, home redirect, and updated Navbar.
3. Open `/profile`; confirm server profile values and that email, role, and status are read-only.
4. Change full name or phone, save, and verify the Navbar updates without signing in again.
5. Change the password with the correct current password and matching new passwords; confirm success and cleared inputs.
6. Sign out and sign in with the new credentials.

## T03 — Customer address management

1. Open `/addresses`, add an address and set it as default.
2. Verify the Default badge, edit the saved address, then add another address.
3. Set the second address as default and confirm the badge moves.
4. Cancel the delete confirmation once, then confirm deletion and verify the address is removed.

## T04 — Admin users and catalog setup

1. Sign in as an Admin, open `/admin/users`, search by email/name, and inspect user details.
2. Disable a test customer and re-enable it; confirm the status and success toast update.
3. Create a Category and Brand under their respective Admin routes, rename both, and toggle each status.
4. Confirm inactive catalog values cannot be selected for new products.

## P60 — Admin product, variant, image, and inventory

1. Create a product with an active Category and Brand. Confirm the editor opens for the created product.
2. Add a variant with non-negative price and stock; edit its SKU/price and update its stock.
3. Toggle variant status, then add an image URL and set it as primary.
4. When Cloudinary is configured, upload an image file; otherwise confirm the disabled/missing service is surfaced as an error.
5. Deactivate the product, reload its editor, and confirm Admin-list fallback still loads the product. Reactivate it and confirm public product detail is available again.
6. Open `/admin/inventory`, change the threshold, and update stock on a low-stock row.

## T12 — Public product catalog and detail

1. Open `/products` as a guest and verify product cards render with image or placeholder, brand/category, and price.
2. Search by product name; filter by Category, Brand, and non-negative price range; verify search/filter/sort state remains in the URL.
3. Submit an invalid price range and verify no invalid catalog request is made.
4. Move between pages and reload; verify filters remain and pagination follows the URL.
5. Clear filters from the empty state and open a card to `/products/:productId`.
6. Confirm the home page shows the eight newest products and links to the full catalog.

## Regression checks

- Existing customer routes: cart, checkout, orders, reviews, wishlist, and profile/address pages.
- Existing Admin routes: dashboard, orders, vouchers, and reviews.
- Run `npm run lint` and `npm run build` in `frontend/` after changes.
