# OOAD Cosmetics E-commerce

Repository structure:

- `backend/`: Java 21 + Spring Boot 3.x + PostgreSQL/Flyway foundation.
- `frontend/`: React + TypeScript + Vite customer and admin application.
- `backend/work/`: local patch helper directory; ignored by Git.

## Local frontend configuration

1. Copy `frontend\.env.example` to `frontend\.env`. The default points to the backend's local `/api` base URL.
2. From `frontend/`, run `npm install` and `npm run dev`.
3. Validate with `npm run build` and `npm run lint`.

Frontend routes and API integration are documented in [frontend/README.md](frontend/README.md). Wireframes and account/catalog/admin integration checks are available in [docs/frontend-account-catalog-wireframes.svg](docs/frontend-account-catalog-wireframes.svg) and [docs/frontend-account-catalog-admin-test-plan.md](docs/frontend-account-catalog-admin-test-plan.md).

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

## Temporary demo database

The demo dataset is isolated in the `demo` profile; normal and test profiles do not load it.
All seeded accounts use the temporary password `DemoOnly123!`:

- Admin: `admin@ooad.demo`
- Customers: `anh@ooad.demo`, `minh@ooad.demo`, `linh@ooad.demo`

From `backend/`, point `DB_URL` to a dedicated demo database and start the profile:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/ooad_cosmetics_demo"
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

To reset only that demo database, run these commands from a PostgreSQL-capable terminal,
then start the app with the demo profile again so Flyway recreates the schema and seed:

```powershell
psql -U postgres -d postgres -c "DROP DATABASE IF EXISTS ooad_cosmetics_demo WITH (FORCE);"
psql -U postgres -d postgres -c "CREATE DATABASE ooad_cosmetics_demo;"
```

The seed is temporary and should be replaced with Đặng's canonical demo data when available.

## Project documents

- [Order & business wireframes](docs/frontend-order-business-wireframes.svg)
- [Account, catalog, and admin wireframes](docs/frontend-account-catalog-wireframes.svg)
- [Account, catalog, and admin integration checks](docs/frontend-account-catalog-admin-test-plan.md)
- [Frontend demo script](docs/frontend-order-business-demo.md)
- [Cart, order, voucher, review, and dashboard tests](docs/testing.md)
