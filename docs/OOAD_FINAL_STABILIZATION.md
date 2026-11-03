# OOAD - Final stabilization gate

Do not claim the project is complete merely because ZIP patches applied or unit tests pass.

## Required environment checks

- Verify Git is on a feature/fix branch based on the latest origin/dev; never patch a dirty tree.
- Run `git status --short`, `git diff --check` after each patch.
- Frontend commands must run inside the **real** `frontend` folder containing `package.json`.
- `npm ci` requires `package-lock.json`; if absent, investigate repository checkout. Do **not** auto-generate a lockfile in this stabilization PR.
- Backend 72 failed integration tests are not established as 72 logic bugs: the first failures indicate Flyway/PostgreSQL context boot errors.
- Before running integration tests, configure two isolated databases `ooad_cosmetics_test` and `ooad_cosmetics_demo_test` as documented in `docs/testing.md`.
- Never use real/demo app data for destructive integration tests. Never automatically drop/reset any database.
- Read the *first* full `PSQLException` in `backend/build/test-results/test/*.xml` (connection refused, password denied, database missing, permissions, checksum, etc.), fix the environment root cause, then rerun.

## Acceptance cases

1. Login Customer/Admin; Admin cannot access Customer-only cart, orders, wishlist, checkout, reviews, address APIs (403). Anonymous access yields 401.
2. Admin sees inactive variants after refreshing `/admin/products/:id/edit`; public detail does not show inactive variant.
3. Expired JWT gets 401 and frontend clears displayed auth state; failed `/auth/login` does not erroneously log out a new authenticated session.
4. ONLINE pending payment cannot be confirmed in Admin order detail until PAID; COD status flow remains available.
5. Checkout preview error can be retried without changing voucher/address; create order remains locked while preview unavailable.
6. Run backend unit tests, backend integration tests with dedicated disposable PostgreSQL test DBs, frontend build/lint, and a browser regression across desktop/mobile before marking Complete.

## Intentional non-feature

Forgot Password does not send reset email in the current default backend configuration. Do not claim it is production-ready. Do not enable exposing raw reset tokens on a public deployment just to make the demo work. Adding SMTP / transactional email requires its own approved scope and secrets outside Git.

## PR / sheet rules

One branch, N logical commits, one push, one user-created PR. Track the PR number(s) in Google Sheets; do not replace older PRs or mark Complete until tests, manual acceptance and merge have passed. No automated push/merge in this kit.
