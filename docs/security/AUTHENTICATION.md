# Authentication and Authorization

Module 06 security foundation for the Temple Digital Services Platform.

## Endpoints

| Method | Path | Access |
|--------|------|--------|
| POST | `/api/v1/auth/register` | Public. Always creates `DEVOTEE` / `ACTIVE`. |
| POST | `/api/v1/auth/login` | Public. Returns a short-lived JWT access token. |
| GET | `/api/v1/auth/me` | Authenticated. Identity from SecurityContext/JWT `sub` only. |
| GET | `/api/v1/internal/temple-admin` | `TEMPLE_ADMIN` or `PLATFORM_ADMIN` (authorization probe) |
| GET | `/api/v1/internal/platform-admin` | `PLATFORM_ADMIN` (authorization probe) |
| GET | `/api/v1/system/ping` | Public (frontend/ops liveness-style check) |
| GET | `/api/v1/system/info` | Public (existing contract; non-sensitive app metadata) |
| GET | `/api/v1/system/database` | `PLATFORM_ADMIN` |
| GET | `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | Public (probes) |
| GET | `/actuator/info` | `PLATFORM_ADMIN` |

Default deny for any other endpoint.

## JWT

- HS256 access token signed with `JWT_SECRET` (required, minimum 32 bytes).
- Claims: `sub` (account id), `role`, `iss`, `iat`, `exp`.
- No refresh tokens.
- Missing, malformed, tampered, or expired tokens return 401.
- Authenticated caller with insufficient role returns 403.

## Configuration

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | HMAC signing secret. Required at startup. No insecure default. |
| `JWT_ISSUER` | Expected issuer. Default `temple-platform`. |
| `JWT_ACCESS_TOKEN_TTL` | Access token lifetime. Default `15m`. |

Passwords are stored with BCrypt. Public registration cannot assign `TEMPLE_ADMIN` or `PLATFORM_ADMIN`.
