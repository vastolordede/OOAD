# Account / Authentication / Authorization

Checklist coverage: A01-A26.

## Main endpoints

### Public
- `POST /api/auth/register`
- `POST /api/auth/login`

### Authenticated customer/admin
- `GET /api/users/me`
- `PUT /api/users/me`
- `PUT /api/users/me/password`
- `GET /api/users/me/addresses`
- `POST /api/users/me/addresses`
- `PUT /api/users/me/addresses/{id}`
- `DELETE /api/users/me/addresses/{id}`
- `PUT /api/users/me/addresses/{id}/default`

### Admin only
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `PUT /api/admin/users/{id}/disable`
- `PUT /api/admin/users/{id}/enable`

## Security rules
- Passwords are BCrypt hashes only.
- JWT secret comes from `.env` through `JWT_SECRET`; it is never hard-coded.
- `/api/admin/**` requires role `ADMIN`.
- Disabled accounts cannot authenticate.
- Address operations always query by both address id and current user id to enforce ownership.

## Local admin test

Register a normal account first, then promote it locally in PostgreSQL:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@example.com';
```

Log in again to receive a token with the current authorities loaded for the promoted account.

## Flyway

`V2__account_auth_schema.sql` creates:
- `users`
- `addresses`

Do not create or alter these tables manually in DBeaver.
