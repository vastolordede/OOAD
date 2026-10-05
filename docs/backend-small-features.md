# Small backend features

## A41 - Forgot / Reset Password (SHOULD)

Endpoints:

- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Reset tokens are generated with `SecureRandom`, but only a SHA-256 hash of
the token is stored in PostgreSQL.

The forgot-password endpoint deliberately returns a generic message for both
existing and non-existing email addresses.

The current course project does not contain a mail subsystem. For local manual
testing only, set:

```properties
RESET_EXPOSE_TOKEN=true
```

The raw reset token will then be included in the forgot-password response.
Keep this disabled outside local development.

Flyway migration:

- `V4__password_reset_tokens.sql`

## P61 - Upload image to Cloud Storage (BONUS)

Cloudinary support is optional and disabled by default, so the backend can
still run without cloud credentials.

Enable it in local `.env` only when you want to test the BONUS feature:

```properties
CLOUDINARY_ENABLED=true
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=ooad-cosmetics
```

Endpoint:

- `POST /api/admin/products/{productId}/images/upload`

Multipart fields:

- `file`: image file
- `sortOrder`: optional integer, default `0`

The upload result's secure URL is saved through the existing ProductImage
domain flow.

Do not commit Cloudinary credentials.
