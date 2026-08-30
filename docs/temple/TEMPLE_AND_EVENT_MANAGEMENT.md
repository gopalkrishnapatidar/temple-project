# Temple and Event Management

Module 07 implementation for the Temple Digital Services Platform.

## Schema (Flyway V5)

| Table | Purpose |
|-------|---------|
| `temple` | Temple profile, location, timezone, status |
| `temple_admin_assignment` | Links `TEMPLE_ADMIN` accounts to temples |
| `temple_event` | Events scoped to a temple |

Statuses:
- Temple: `ACTIVE`, `INACTIVE`
- Event: `DRAFT`, `PUBLISHED`, `CANCELLED`

Constraints: non-blank names, valid statuses, `end_at > start_at`, FK integrity, unique `(account_id, temple_id)` assignment.

Indexes: `temple(status)`, `temple_admin_assignment(temple_id)`, `temple_event(temple_id, start_at, id)`.

## API

Base path: `/api/v1/temples`

| Method | Path | Access |
|--------|------|--------|
| POST | `/api/v1/temples` | `PLATFORM_ADMIN` |
| GET | `/api/v1/temples` | Authenticated (visibility filtered) |
| GET | `/api/v1/temples/{templeId}` | Authenticated (visibility filtered) |
| PATCH | `/api/v1/temples/{templeId}` | `PLATFORM_ADMIN` or assigned `TEMPLE_ADMIN` |
| POST | `/api/v1/temples/{templeId}/admins` | `PLATFORM_ADMIN` |
| DELETE | `/api/v1/temples/{templeId}/admins/{accountId}` | `PLATFORM_ADMIN` |
| POST | `/api/v1/temples/{templeId}/events` | `PLATFORM_ADMIN` or assigned `TEMPLE_ADMIN` |
| GET | `/api/v1/temples/{templeId}/events` | Authenticated (paginated, visibility filtered) |
| GET | `/api/v1/temples/{templeId}/events/{eventId}` | Authenticated (visibility filtered) |
| PATCH | `/api/v1/temples/{templeId}/events/{eventId}` | `PLATFORM_ADMIN` or assigned `TEMPLE_ADMIN` |

Pagination: `page` (default 0), `size` (default 20, max 100). Ordered by `start_at`, then `id`. Page offsets are validated to prevent integer overflow; unsafe `page`/`size` combinations return 400.

## Event lifecycle

- Event creation always sets `status` to `DRAFT` server-side; clients cannot supply `status` on create.
- Allowed status transitions on update:
  - `DRAFT` → `PUBLISHED`
  - `DRAFT` → `CANCELLED`
  - `PUBLISHED` → `CANCELLED`
  - same-status updates (no-op transition)
- Rejected transitions return 400: `PUBLISHED` → `DRAFT`, `CANCELLED` → `DRAFT`, `CANCELLED` → `PUBLISHED`.
- Field-only PATCH updates (name, description, schedule) are allowed without changing status when `status` is omitted.

## Authorization

Role checks plus resource-level checks via `TempleAuthorizationService`:
- `PLATFORM_ADMIN`: all temples and assignments
- `TEMPLE_ADMIN`: only assigned temples (from `temple_admin_assignment`, not JWT)
- `DEVOTEE`: read-only, filtered public visibility

Assignment rules:
- target account must exist, be `ACTIVE`, and have role `TEMPLE_ADMIN`
- duplicate assignment → 409
- temple management authorization is resolved from `temple_admin_assignment` on each request; removing an assignment immediately revokes access (existing JWT unchanged, DB lookup fails → 403)

## Public read visibility

- Devotee reads: `ACTIVE` temples only; `PUBLISHED` events only
- `INACTIVE` temples and `DRAFT`/`CANCELLED` events return 404 on individual reads and are excluded from public list filters
- Admins with management access see full data for assigned/all temples

## Validation

- IANA timezone (`ZoneId`) on create/update
- Event schedule: `endAt` must be after `startAt` (service + DB CHECK); invalid schedule returns 400 with message `Event end time must be after start time`
- Invalid event status transitions return 400 with message `Invalid event status transition`
- Clients cannot set IDs, timestamps, `templeId`, or create-time `status` in event body (URL is authoritative for `templeId`)

## Configuration

No new secrets. Uses existing JWT and PostgreSQL configuration from Modules 05–06.

## Tests

`mvn clean test` — 70 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS.

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_PASSWORD="<local password>"
$env:JWT_SECRET="<local secret>"
mvn clean test
```

Privileged local test accounts: insert `TEMPLE_ADMIN` / `PLATFORM_ADMIN` rows directly (tests use `AccountRepository.insert` with BCrypt password). Public registration remains `DEVOTEE` only.

## Manual verification (completed)

| Check | Result |
|-------|--------|
| Event create defaults to `DRAFT` | Verified |
| `DRAFT` → `PUBLISHED` | Verified |
| `PUBLISHED` → `DRAFT` | Rejected — HTTP 400 |
| Invalid schedule | HTTP 400 — `Event end time must be after start time` |
| Assignment removal revokes access immediately | Existing `TEMPLE_ADMIN` JWT → HTTP 403 (DB lookup, not JWT claims) |

PostgreSQL schema version remains `5`; no migration changes were required during hardening.

### Troubleshooting notes

- Incorrect `SPRING_DATASOURCE_PASSWORD` caused a test-run failure; missing `JWT_SECRET` caused a runtime startup failure — both environment configuration issues, not code defects.
- `curl.exe` JSON quoting can produce generic `Invalid request`; use PowerShell `ConvertTo-Json` for reliable schedule-validation manual tests.

## Known limitations / deferred

- Internal RBAC probe endpoints from Module 06 remain
- No Redis auth cache, JWT revocation, refresh tokens, notifications, booking, or audit history
- JWT remains valid until expiry after account disable
- Remove `/api/v1/internal/*` probes before production (Module 41)
