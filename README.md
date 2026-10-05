# OOAD Cosmetics E-commerce

Repository structure at initialization:

- `backend/`: Java 21 + Spring Boot 3.x + PostgreSQL/Flyway foundation.
- `frontend/`: intentionally empty for the frontend members to initialize later.
- `backend/work/`: local patch helper directory; ignored by Git.

## Local backend configuration

1. Copy/edit `backend/.env` locally. It is ignored by Git.
2. Create PostgreSQL database `ooad_cosmetics` (or change `DB_URL`).
3. Put the real PostgreSQL password in `DB_PASSWORD`.
4. Generate the Gradle wrapper once with `backend\bootstrap-gradle-wrapper.bat`.
5. Run from `backend/`:
   - `gradlew.bat clean test`
   - `gradlew.bat bootRun`

Schema changes are managed by Flyway migrations in `backend/src/main/resources/db/migration/`.
