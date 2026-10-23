# OOAD Cosmetics E-commerce

Repository structure:

- `backend/`: Java 21 + Spring Boot 3.x + PostgreSQL/Flyway foundation.
- `frontend/`: React + TypeScript + Vite customer and admin application.
- `backend/work/`: local patch helper directory; ignored by Git.

## Local frontend configuration

1. Copy `frontend\.env.example` to `frontend\.env`. The default points to the backend's local `/api` base URL.
2. From `frontend/`, run `npm install` and `npm run dev`.
3. Validate with `npm run build` and `npm run lint`.

Frontend routes, API layering, and the current integration gap are documented in [frontend/README.md](frontend/README.md) and [docs/frontend-order-business-handoff.md](docs/frontend-order-business-handoff.md).

## Local backend configuration

1. Copy/edit `backend/.env` locally. It is ignored by Git.
2. Create PostgreSQL database `ooad_cosmetics` (or change `DB_URL`).
3. Put the real PostgreSQL password in `DB_PASSWORD`.
4. Generate the Gradle wrapper once with `backend\bootstrap-gradle-wrapper.bat`.
5. Run from `backend/`:
   - Run the focused database-independent unit tests listed in [docs/testing.md](docs/testing.md).
   - Configure a dedicated test database before running integration tests or the complete `test` task.
   - `gradlew.bat bootRun`

Schema changes are managed by Flyway migrations in `backend/src/main/resources/db/migration/`.

## Project documents

- [Order & business wireframes](docs/frontend-order-business-wireframes.svg)
- [Frontend demo script](docs/frontend-order-business-demo.md)
- [Cart, order, voucher, review, and dashboard tests](docs/testing.md)
