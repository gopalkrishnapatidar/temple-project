# Temple Digital Services Platform

# Module Status

Concise implementation progress tracker for the Temple Digital Services Platform.

Cursor must update this file after completing each module.

---

## Project Status

| Field | Value |
|-------|-------|
| Project | Temple Digital Services Platform |
| Total Modules | 44 |
| Completed | 13 / 44 |
| Current Phase | Phase 1 - Application |
| Current Module | Module 13 - Payments & Donations |
| Current Module Status | COMPLETED |

### Completed Modules

- [x] Module 00 - Project Architecture & Foundation
- [x] Module 01 - Git, GitHub & Local Development
- [x] Module 02 - Linux & Networking Foundation
- [x] Module 03 - Spring Boot Backend Foundation
- [x] Module 04 - Next.js Frontend Foundation
- [x] Module 05 - PostgreSQL & Database Engineering
- [x] Module 06 - Authentication & Authorization
- [x] Module 07 - Temple & Event Management
- [x] Module 08 - Darshan & Slot Management
- [x] Module 09 - Havan & Puja Booking
- [x] Module 10 - Booking / Concurrency
- [x] Module 11 - Redis & Caching
- [x] Module 12 - Real-Time Availability
- [x] Module 13 - Payments & Donations

---

## Module Status Values

Use only: `NOT STARTED`, `IN PROGRESS`, `BLOCKED`, `TESTING`, `COMPLETED`

---

## Module 03 - Spring Boot Backend Foundation

**Status:** COMPLETED

### Implementation

- `backend/` Spring Boot 3.4.4 (Java 21, Maven)
- Dependencies: Web, Validation, Actuator, JDBC, Flyway, PostgreSQL driver
- `GET /api/v1/system/ping`, `GET /api/v1/system/info`
- Actuator: health, liveness, readiness (health and info exposed only)
- Flyway `V1__baseline.sql` — `application_metadata` table
- Global JSON error handler; no stack traces to clients
- `backend/.env.example`; password via `SPRING_DATASOURCE_PASSWORD` only
- `docs/backend/BACKEND_FOUNDATION.md`

### Database

- PostgreSQL database: `temple_platform_dev`
- Application database user: `temple_app`
- Flyway V1 baseline migration applied successfully
- PostgreSQL 18.6 produced a non-blocking Flyway compatibility warning because
  the bundled Flyway version officially reports support through PostgreSQL 17.
  Do not change dependency versions as part of unrelated work.

### Automated Validation

`mvn test`

| Metric | Result |
|--------|--------|
| Tests run | 2 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

### Manual Runtime Validation

| Endpoint | Result |
|----------|--------|
| `GET /api/v1/system/ping` | SUCCESS — status UP |
| `GET /api/v1/system/info` | SUCCESS — application `temple-platform`, version `0.0.1-SNAPSHOT`, active profile `dev` |
| `GET /actuator/health` | UP — PostgreSQL database health UP |
| `GET /actuator/health/liveness` | UP |
| `GET /actuator/health/readiness` | UP |

### Problems Encountered

None.

---

## Module 04 - Next.js Frontend Foundation

**Status:** COMPLETED

### Implementation

- `frontend/` Next.js 15 (React 19, TypeScript, App Router)
- Routes: `GET /` home page
- Shared root layout with minimal header/footer structure
- `lib/api.ts` and `lib/types.ts` for backend ping integration
- Server Component `BackendStatus` with Suspense loading fallback
- `frontend/.env.example` with `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`
- Server-side fetch to `GET /api/v1/system/ping` (no direct PostgreSQL access)
- Controlled loading, success, and error UI states
- No UI framework, state management, auth, Docker, CI/CD, or AWS added
- No backend CORS change required (browser requests are not used for ping)

### Automated Validation

`npm install`, `npm run lint`, `npm run build`

| Metric | Result |
|--------|--------|
| ESLint | PASS — no warnings or errors |
| Production build | SUCCESS |
| Type check (build) | PASS |

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| `npm run dev` | SUCCESS — served on `http://localhost:3000` |
| Home page title/content | SUCCESS |
| Backend status section (backend stopped) | SUCCESS — controlled unavailable message shown |
| `GET http://localhost:8080/api/v1/system/ping` | NOT TESTED — backend not running in agent environment |
| Backend status success state | NOT TESTED — requires running Spring Boot backend |

### Problems Encountered

- Node.js/npm were not initially available in the agent shell PATH; Node.js LTS was installed via `winget` to run frontend validation.
- Backend integration success state could not be verified because the Spring Boot backend was not running and database credentials were not configured in the agent environment.

---

## Module 05 - PostgreSQL & Database Engineering

**Status:** COMPLETED

### Implementation

- Explicit HikariCP pool settings (env-overridable); leak detection on in `dev`, off in `prod`
- Optional Flyway credentials (`SPRING_FLYWAY_USERNAME` / `SPRING_FLYWAY_PASSWORD`)
- Flyway `V2__database_engineering.sql` — `updated_at` trigger, CHECK constraints
- Flyway `V3__fix_application_metadata_updated_at_timestamp.sql` — `clock_timestamp()` trigger fix
- JDBC `ApplicationMetadataRepository` and `GET /api/v1/system/database`
- Local bootstrap SQL: `backend/db/01_create_database.sql`, `backend/db/02_roles_and_grants.sql` (reference-only)
- `docs/database/DATABASE_ENGINEERING.md`
- No domain tables (auth, temple, booking), JPA, Redis, Docker, or Testcontainers

### Database

- PostgreSQL database: `temple_platform_dev`
- Application database user: `temple_app` (datasource and Flyway for local development)
- Flyway V1, V2, and V3 applied successfully; `schema_version` = `3`
- V3 required because PostgreSQL `NOW()` is transaction-stable; forward migration uses `clock_timestamp()`
- Optional `temple_migrator` role bootstrap remains reference-only and was not executed
- PostgreSQL 18.6 produced a non-blocking Flyway tested-support warning (official support through PostgreSQL 17)

### Automated Validation

`mvn test`

| Metric | Result |
|--------|--------|
| Tests run | 8 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| PostgreSQL connectivity | SUCCESS |
| HikariCP startup | SUCCESS |
| Application startup | SUCCESS |
| `GET /api/v1/system/ping` | SUCCESS |
| `GET /api/v1/system/info` | SUCCESS |
| `GET /api/v1/system/database` | SUCCESS — `schemaVersion` `3`, `flywayVersion` `3` |
| `GET /actuator/health` | UP |

### Direct PostgreSQL Validation (`psql`)

| Check | Result |
|-------|--------|
| Database `temple_platform_dev` | CONFIRMED |
| User `temple_app` | CONFIRMED |
| Schema `public` | CONFIRMED |
| Flyway V1/V2/V3 success | CONFIRMED |
| `schema_version` = `3` | CONFIRMED |
| PK, UNIQUE, CHECK constraints and `updated_at` trigger | CONFIRMED |

### Problems Encountered

- V2 `updated_at` trigger used `NOW()` (transaction-stable); fixed in V3 with `clock_timestamp()` without modifying applied migrations.
- Agent environment initially lacked `SPRING_DATASOURCE_PASSWORD`; resolved during local developer verification.

---

## Module 06 - Authentication & Authorization

**Status:** COMPLETED

### Implementation

- Flyway `V4__account.sql` — `account` table with unique normalized email and role/status CHECKs
- Spring Security filter chain (stateless JWT, default deny)
- `POST /api/v1/auth/register` — public; always creates `DEVOTEE` + `ACTIVE`; duplicate email → 409; client cannot self-assign admin role
- `POST /api/v1/auth/login` — public; BCrypt authentication; generic invalid-credentials response
- `GET /api/v1/auth/me` — protected; identity from JWT `sub` / SecurityContext only
- JWT HS256 access tokens (`JWT_SECRET` required, no insecure production default); 15-minute lifetime
- Missing/invalid/expired/tampered token → 401; insufficient role → 403
- `GET /api/v1/system/database` and `/actuator/info` require `PLATFORM_ADMIN`
- Public: register, login, ping, info, health/liveness/readiness
- Authorization probes: `/api/v1/internal/temple-admin`, `/api/v1/internal/platform-admin`
- `docs/security/AUTHENTICATION.md`

### Database

- Flyway V4 applied; `schema_version` = `4`
- `account` roles: `DEVOTEE`, `TEMPLE_ADMIN`, `PLATFORM_ADMIN`; statuses: `ACTIVE`, `DISABLED`

### Automated Validation

`mvn clean test`

| Metric | Result |
|--------|--------|
| Tests run | 32 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| Registration → `DEVOTEE` / `ACTIVE` | SUCCESS |
| Duplicate registration | SUCCESS — 409 |
| Self-assigned `PLATFORM_ADMIN` in JSON | SUCCESS — still `DEVOTEE` |
| Valid login → JWT (900s expiry) | SUCCESS |
| `GET /api/v1/auth/me` with valid token | SUCCESS |
| Missing JWT | SUCCESS — 401 JSON |
| `DEVOTEE` → `GET /api/v1/system/database` | SUCCESS — 403 JSON |
| `GET /actuator/health` (no auth) | SUCCESS — UP |
| Wrong password / unknown user login | SUCCESS — 401 |

### Problems Encountered

- Stale `target/test-classes/application.yml` from a deleted test resource shadowed main `application.yml`; `mvn clean test` resolved it.
- Runtime startup failure was caused by an incorrect local `SPRING_DATASOURCE_PASSWORD`, not application code.

---

## Module 07 - Temple & Event Management

**Status:** COMPLETED

### Implementation

- Flyway `V5__temple_and_event.sql` — `temple`, `temple_admin_assignment`, `temple_event`
- Temple CRUD (create `PLATFORM_ADMIN` only; update assigned `TEMPLE_ADMIN` or `PLATFORM_ADMIN`)
- Temple admin assignment management (`PLATFORM_ADMIN` only; duplicate → 409)
- Temple event CRUD with bounded pagination (default 20, max 100); safe page-offset validation
- Event create status server-owned — always `DRAFT`; lifecycle transitions enforced on update
- Centralized `TempleAuthorizationService` for resource-level checks (assignments from DB, not JWT)
- Public read visibility: `ACTIVE` temples and `PUBLISHED` events only for `DEVOTEE`
- `docs/temple/TEMPLE_AND_EVENT_MANAGEMENT.md`

### Database

- Flyway V5 applied; `schema_version` = `5` (no migration changes required during hardening)
- FK integrity, unique assignment, `end_at > start_at`, status CHECK constraints

### Automated Validation

`mvn clean test`

| Metric | Result |
|--------|--------|
| Tests run | 70 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| Event create defaults to `DRAFT` | SUCCESS |
| `DRAFT` → `PUBLISHED` transition | SUCCESS |
| `PUBLISHED` → `DRAFT` rejected | SUCCESS — HTTP 400 |
| Invalid schedule (`endAt` ≤ `startAt`) | SUCCESS — HTTP 400, message `Event end time must be after start time` |
| DB-backed authorization after assignment removal | SUCCESS — existing `TEMPLE_ADMIN` JWT immediately receives HTTP 403 |

### Problems Encountered

- One test run failed due to incorrect local `SPRING_DATASOURCE_PASSWORD` (environment configuration, not code).
- Runtime startup failed once because `JWT_SECRET` was missing (environment configuration, not code).
- `curl.exe` JSON quoting produced generic `Invalid request` during one manual schedule test; PowerShell `ConvertTo-Json` confirmed the intended validation response.

---

## Module 08 - Darshan & Slot Management

**Status:** COMPLETED

### Implementation

- Flyway `V6__darshan_and_slot.sql` — `darshan`, `darshan_slot`, overlap EXCLUDE constraint (`btree_gist`)
- Darshan CRUD nested under temples; slot CRUD nested under darshans
- Darshan lifecycle: `ACTIVE` / `INACTIVE`; slot lifecycle: `AVAILABLE` / `CANCELLED` (create defaults server-owned)
- Reuses `TempleAuthorizationService` and DB-backed temple assignment checks; nested Temple → Darshan → Slot BOLA protection
- Devotee visibility: `ACTIVE` temple + `ACTIVE` darshan; non-cancelled slots with `end_at > now()`
- Slot listing: temple-timezone `date` filter, optional `from`/`to` instant range (max 90 days), pagination
- No booking, Redis, Kafka, payments, notifications, or real-time capacity engine

### Database

- Flyway V6 applied; `schema_version` = `6`
- FK `darshan.temple_id` → `temple`, `darshan_slot.darshan_id` → `darshan`
- CHECK: `capacity > 0`, `end_at > start_at`, status enums
- GiST EXCLUDE on `tstzrange(start_at, end_at, '[)')` for `AVAILABLE` slots per `darshan_id` (adjacent allowed; `CANCELLED` excluded from overlap set)
- Reuses `set_updated_at()` trigger (`clock_timestamp()`)

### Automated Validation

`mvn clean test`

| Metric | Result |
|--------|--------|
| Tests run | 91 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| `GET /actuator/health` | UP — PostgreSQL UP |
| `PLATFORM_ADMIN` creates `ACTIVE` darshan | SUCCESS |
| `AVAILABLE` slot creation | SUCCESS |
| Adjacent slots | SUCCESS |
| Overlapping slot | SUCCESS — HTTP 409, business error (no raw DB leak) |
| Temple-timezone `date` slot query | SUCCESS |
| `DEVOTEE` read | SUCCESS |
| `DEVOTEE` slot create | SUCCESS — HTTP 403 |
| Cross-temple darshan BOLA | SUCCESS — HTTP 404 |
| Cross-darshan slot BOLA | SUCCESS — HTTP 404 |
| Slot cancellation | SUCCESS |
| `PLATFORM_ADMIN` sees `CANCELLED` history | SUCCESS |
| `DEVOTEE` does not see `CANCELLED` slot | SUCCESS |
| `capacity=0` | SUCCESS — HTTP 400 |
| `endAt <= startAt` | SUCCESS — HTTP 400 |
| Expired JWT | SUCCESS — HTTP 401; re-login restored access |

### Problems Encountered

- Stale `AuthApiTest` expected `schemaVersion`/`flywayVersion` `5` after V6; updated to `6`.
- Agent environment initially lacked `SPRING_DATASOURCE_PASSWORD` / `JWT_SECRET` for integration tests (local configuration, not code).

---

## Module 09 - Havan & Puja Booking

**Status:** COMPLETED

### Implementation

- Flyway `V7__ritual_and_slot.sql` — `ritual`, `ritual_slot` (no overlap EXCLUDE)
- Shared Ritual bounded context: Temple → Ritual (PUJA/HAVAN) → RitualSlot
- Ritual lifecycle: `ACTIVE` / `INACTIVE`; slot lifecycle: `AVAILABLE` / `CANCELLED` (create defaults server-owned)
- `durationMinutes` is current offering configuration; existing slot `startAt`/`endAt` are not rewritten
- `price` is `NUMERIC(12,2)` / `BigDecimal`; currency `INR` only; zero allowed; negative rejected
- Domain/API absolute timestamps are `Instant`; PostgreSQL `TIMESTAMPTZ`; JDBC maps via `OffsetDateTime`
- Temple-local `date` queries use IANA ZoneId `[startOfDay, nextStartOfDay)`; `date` combined with `from`/`to` → 400
- Overlapping Ritual slots intentionally allowed (no Darshan-style GiST EXCLUDE)
- Authorization: `PLATFORM_ADMIN` global; `TEMPLE_ADMIN` DB assignment only; `DEVOTEE` hierarchical read
- Nested Temple → Ritual → Slot BOLA → 404
- No booking, capacity, priest/hall assignment, Redis, Kafka, payments, or notifications

### Database

- Flyway V7 applied; `schema_version` = `7`
- FK `ritual.temple_id` → `temple`, `ritual_slot.ritual_id` → `ritual` (`ON DELETE RESTRICT`)
- CHECK: type PUJA/HAVAN, duration > 0, price >= 0, currency INR, statuses, `end_at > start_at`
- Reuses `set_updated_at()` trigger (`clock_timestamp()`)
- Indexes: `(temple_id, type, status)` on `ritual`; `(ritual_id, start_at, id)` on `ritual_slot`

### Automated Validation

Focused Module 09 tests:

| Metric | Result |
|--------|--------|
| Tests run | 25 |
| Failures | 0 |
| Errors | 0 |
| Build | SUCCESS |

Full backend regression (`mvn clean test`):

| Metric | Result |
|--------|--------|
| Tests run | 117 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| Actuator overall health | UP |
| PostgreSQL health | UP |
| Liveness / readiness | UP |
| PUJA / HAVAN create | SUCCESS |
| Type filter PUJA / HAVAN | SUCCESS |
| Ritual slot create | SUCCESS |
| Overlapping Ritual slots | SUCCESS — both created |
| Temple-local date filter | SUCCESS |
| Invalid duration, price, schedule, currency | SUCCESS — HTTP 400 |
| Ambiguous `date` + `from`/`to` | SUCCESS — HTTP 400 |
| DEVOTEE read | SUCCESS |
| DEVOTEE write | SUCCESS — HTTP 403 |
| Cross-Temple Ritual BOLA | SUCCESS — HTTP 404 |
| Cross-Ritual Slot BOLA | SUCCESS — HTTP 404 |
| Slot AVAILABLE → CANCELLED | SUCCESS |
| Admin sees CANCELLED history | SUCCESS |
| DEVOTEE does not see CANCELLED slot | SUCCESS |
| CANCELLED → AVAILABLE | SUCCESS — HTTP 400 |
| Ritual ACTIVE → INACTIVE | SUCCESS |
| Admin sees INACTIVE Ritual | SUCCESS |
| DEVOTEE INACTIVE Ritual / its slots | SUCCESS — HTTP 404 |
| Expired JWT | SUCCESS — HTTP 401; re-login restored access |

### Problems Encountered

- PostgreSQL JDBC does not support `ResultSet.getObject(..., Instant.class)` for `timestamptz`. Repository maps `OffsetDateTime` ↔ `Instant`; domain/API remain `Instant`.
- Java Instant nanoseconds vs PostgreSQL microsecond `TIMESTAMPTZ` rounded a test fixture; the duration-independence assertion was kept (deterministic microsecond Instant).
- DST API fixture `2026-03-08` was already past; DEVOTEE correctly hid ended slots. Fixture moved to future America/New_York spring-forward `2027-03-14`.

### Final Review

- MUST FIX: NONE
- Module 09 approved for completion
- SHOULD FIX LATER items are non-blocking and were not implemented

---

## Module 10 - Darshan Booking & Concurrency

**Status:** COMPLETED

### Implementation

- Flyway `V8__booking_and_ritual_slot_capacity.sql` — positive `ritual_slot.capacity`; `booking` table
- REST: `POST/GET /api/v1/bookings`, `GET/PATCH /api/v1/bookings/{bookingReference}`
- PostgreSQL authoritative for booking and capacity truth (no derived `available_capacity`)
- Pessimistic slot-row `SELECT … FOR UPDATE` on booking create, cancel, and Darshan/Ritual capacity updates
- `Idempotency-Key` required on create; `(account_id, idempotency_key)` uniqueness with `ON CONFLICT DO NOTHING`
- Darshan and Ritual slot capacity cannot be reduced below confirmed booking quantity (409)
- Capacity equal to or above confirmed quantity allowed
- No payments, Redis, Kafka, AWS, Kubernetes, or physical booking deletes

### Database

- Flyway V8 applied; `schema_version` = `8`
- `booking`: exactly-one slot FK CHECK; `CONFIRMED`/`CANCELLED`; UNIQUE `booking_reference` and `(account_id, idempotency_key)`
- Indexes for owner listing and confirmed quantity SUM by slot

### Automated Validation

`mvn clean test`

| Metric | Result |
|--------|--------|
| Tests run | 146 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

Coverage includes Darshan/Ritual booking, idempotency, BOLA, capacity invariant, repository constraints, and concurrent booking/capacity races.

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| Booking concurrency (no overselling) | SUCCESS |
| Darshan capacity invariant | SUCCESS |
| Ritual capacity invariant | SUCCESS |

### Problems Encountered

- `BookingRepositoryTest` PostgreSQL `25P02` after multiple constraint violations in one `@Transactional` test — fixed by isolating each DB constraint assertion in its own test method.
- `concurrentCapacityReductionAndBookingRespectInvariant` failed with HTTP 400 when racing capacity reduction to zero — test redesigned to race valid operations (capacity 2→1 vs booking quantity 1).

### Final Review

- MUST FIX: NONE
- Module 10 approved for completion

---

## Implementation Artifacts by Module

### Module 00

- `/docs/architecture/PROJECT_OVERVIEW.md`
- `/docs/architecture/BUSINESS_DOMAINS.md`
- `/docs/architecture/INITIAL_ARCHITECTURE.md`
- `/docs/architecture/EVOLUTION_ROADMAP.md`

### Module 01

- `/docs/git/GIT_WORKFLOW.md`
- `/docs/git/GIT_COMMANDS.md`
- `/docs/git/GITHUB_PULL_REQUESTS.md`
- `/docs/git/GIT_TROUBLESHOOTING.md`

### Module 02

- `/docs/linux-networking/LINUX_FOUNDATIONS.md`
- `/docs/linux-networking/NETWORKING_FOUNDATIONS.md`
- `/docs/linux-networking/TROUBLESHOOTING_COMMANDS.md`
- `/docs/linux-networking/HANDS_ON_EXERCISES.md`

### Module 03

- `backend/` Spring Boot backend foundation
- No Docker, Kubernetes, Helm, Terraform, AWS, CI/CD, Redis, Kafka, frontend,
  auth, or business domain logic

### Module 04

- `frontend/` Next.js frontend foundation
- No UI framework, Redux/Zustand, auth, Docker, Kubernetes, CI/CD, AWS, Redis,
  Kafka, or business domain features

### Module 05

- Database engineering: HikariCP, Flyway V1–V3, optional migrator role (reference-only), JDBC metadata API
- No auth, temple/event APIs, booking, Redis, Kafka, Docker, Kubernetes, CI/CD, or AWS

### Module 06

- Authentication and authorization: `account` table, Spring Security, JWT access tokens
- No Temple/Event APIs, booking, Redis, Kafka, Docker, Kubernetes, CI/CD, or AWS

### Module 08

- Darshan and slot domain: Flyway V6, JDBC repositories, nested REST API, overlap constraint
- No booking, Redis, Kafka, payments, notifications, Docker, Kubernetes, CI/CD, or AWS

### Module 09

- Ritual (PUJA/HAVAN) domain: Flyway V7, JDBC repositories, nested REST API, no overlap constraint
- No booking, Redis, Kafka, payments, notifications, Docker, Kubernetes, CI/CD, or AWS

### Module 10

- Booking domain: Flyway V8, JDBC repository, REST API, slot-row pessimistic locking
- No payments, Redis, Kafka, notifications, Docker, Kubernetes, CI/CD, or AWS

---

## Architecture Decisions

- Start as a modular monolith (frontend → backend → PostgreSQL).
- Do not start with microservices.
- PostgreSQL is the transactional source of truth.
- Defer Redis, Kafka, Docker, Kubernetes, CI/CD, AWS, Terraform, and
  observability to their modules.
- Do not decide the booking reservation/hold/confirmation vs payment ordering
  in Module 00.
- Module 06: short-lived JWT access tokens (no refresh tokens); `GET /api/v1/system/database` is `PLATFORM_ADMIN`-only.
- Module 07: temple admin assignments stored relationally; resource-level authorization enforced per temple; event create status server-owned (`DRAFT`); lifecycle transitions validated on update.
- Module 08: darshan/slot nested under temples; PostgreSQL EXCLUDE overlap for available slots; devotee visibility filters; temple-timezone date queries.
- Module 09: PUJA and HAVAN share one `ritual` bounded context (separate from Darshan); PostgreSQL `NUMERIC` price; no same-ritual overlap constraint; slot times are scheduled boundaries independent of `durationMinutes`; current price is configuration only (no booking snapshot yet).
- Module 10: PostgreSQL is the booking/capacity authority; pessimistic slot-row locking on booking create/cancel and capacity updates; idempotency uniqueness per account at DB; RitualSlot explicit positive capacity (V8); confirmed quantity must not exceed slot capacity.

---

## Module 11 - Redis & Caching

**Status:** COMPLETED

### Implementation

- Spring Data Redis with Lettuce and fail-open cache-aside catalog caching
- Cached temple, darshan, ritual, and event catalog reads with bounded TTLs
- PostgreSQL remains authoritative for booking, capacity, availability, idempotency, and authentication
- Cache invalidation executes only after successful database commit
- Redis is not a readiness dependency

### Validation

- Full regression: 160 tests, 0 failures, 0 errors, 0 skipped
- Verified cache MISS -> DB -> SET -> HIT and TTL behavior
- Verified successful PATCH invalidates entity and public-list keys after commit
- Verified subsequent GET repopulates Redis from PostgreSQL
- Verified Redis outage fail-open: catalog API remained HTTP 200
- Verified application readiness remained UP during Redis outage
- Verified Redis recovery after restart
- Verified no booking, capacity, authentication, or idempotency data was cached
- Local Redis-compatible Memurai listener verified on 127.0.0.1:6379

### Final Review

- MUST FIX: NONE

---

## Module 12 - Real-Time Availability

**Status:** COMPLETED

### Implementation

- PostgreSQL read-time availability projection for Darshan and Ritual slots
- Nested paginated list and slot-detail `GET .../availability` endpoints
- `CONFIRMED` booking quantities aggregated with `LEFT JOIN` + `GROUP BY`; no per-slot N+1 SUM loop
- `remainingCapacity` derived at read time and clamped to zero
- `available` requires an AVAILABLE future slot with remaining capacity
- Existing hierarchical authorization and slot visibility rules reused
- PostgreSQL remains authoritative; no Redis availability cache, write lock, mutable availability counter, or booking write-path change
- No new Flyway migration; existing V8 confirmed-booking partial indexes reused
- `docs/availability/REAL_TIME_AVAILABILITY.md`

### Automated Validation

- `mvn compile`: SUCCESS
- Module 12 availability suite: 18 tests, 0 failures, 0 errors
- Timezone regression tests: 2 tests, 0 failures, 0 errors
- Full `mvn clean test`: 178 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS

### Manual Runtime Validation

- Darshan detail: baseline, confirmed booking, cancellation, and full-capacity `available=false` verified
- Ritual detail: baseline, confirmed booking, and cancellation restoration verified
- Darshan availability list: pagination and aggregate values verified for controlled slots
- Ritual availability list: pagination and aggregate values verified for controlled slot
- Anonymous availability request returned HTTP 401
- Cross-hierarchy/BOLA request returned HTTP 404
- Redis/Memurai outage: availability remained functional from PostgreSQL
- Aggregate health returned HTTP 503 while Redis was down, while readiness remained HTTP 200 / UP with PostgreSQL UP
- Redis recovery verified after restart
- Capacity-1 concurrency race: two concurrent bookings produced exactly one HTTP 201 CONFIRMED and one HTTP 409; cancellation restored capacity

### Problems Encountered

- Agent integration tests initially lacked database credentials; secure local-shell execution completed them successfully
- Two historical fixed-date timezone fixtures were changed to dynamically future Asia/Kolkata dates; full regression then passed
- Initial PowerShell Darshan slot timestamp lost its offset through `DateTimeOffset.Date`; offset-preserving serialization resolved HTTP 400
- Expected 15-minute JWT expiration occurred during extended runtime verification; secure re-login restored access
- Memurai service stop required an elevated PowerShell session
- Redis health briefly remained unavailable immediately after service restart before recovering

### Final Review

- BLOCKER/HIGH findings: NONE
- PostgreSQL authority, authorization hierarchy, concurrency safety, SQL aggregation, Redis independence, and scope boundaries reviewed
- Non-blocking future maintainability items: duplicated visibility logic, mixed time sources, and duplicated timestamptz mapping helper
- Module 12 approved for completion

---

## Module 13 - Payments & Donations

**Status:** COMPLETED

### Implementation

- Flyway `V9__payments_and_donations.sql` - `donation`, `payment`, `payment_webhook_event`
- PostgreSQL-backed booking and donation payment flows
- Mock `PaymentProvider` with deterministic PENDING / SUCCEEDED / FAILED outcomes
- Provider initiation idempotent by `paymentReference` with deterministic provider reference
- Booking payment amount derived server-side from Ritual price x quantity; Darshan payment intentionally unsupported
- Donation creation validates amount and INR currency
- Payment read/reconciliation endpoints with resource-level authorization
- HMAC-SHA256 webhook verification over exact raw request bytes
- Payment state machine: `PENDING -> SUCCEEDED | FAILED`
- Donation state machine: `PENDING -> COMPLETED | FAILED`
- Per-account request idempotency, webhook-event idempotency, provider-reference uniqueness, and active booking-payment uniqueness
- Donation GET returns its linked payment reference
- Provider calls execute outside the payment-preparation DB transaction
- `docs/payment/PAYMENTS_AND_DONATIONS.md`

### Database

- Flyway V9 applied successfully; `schema_version` = `9`
- Monetary values use PostgreSQL `NUMERIC(12,2)`
- DB constraints enforce currency, state, purpose/target integrity, donation amount, and idempotency rules
- Partial unique index prevents multiple PENDING/SUCCEEDED payments for the same booking
- Unique provider event IDs protect webhook processing
- Expected duplicate webhook events use `INSERT ... ON CONFLICT DO NOTHING`

### Automated Validation

Full backend regression (`mvn clean test`):

| Metric | Result |
|--------|--------|
| Tests run | 203 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

Additional validation:

- Payment/Donation/Mock Provider suite: 25 tests, all PASS
- Duplicate webhook focused test: PASS
- Database status/schema tests: 6 tests, all PASS
- Flyway V9 and application schema version 9 confirmed

### Manual Runtime Validation

- Application startup with PostgreSQL/Flyway V9: SUCCESS
- Booking payment creation: SUCCESS
- Server-authoritative Ritual booking amount: SUCCESS
- Same idempotency key replay returned same payment: SUCCESS
- Different idempotency key against existing active booking payment: HTTP 409
- Donation `100.50 INR` produced PENDING donation/payment: SUCCESS
- Donation GET returned linked payment reference: SUCCESS
- Reconciliation preserved PENDING provider state: SUCCESS
- Correctly signed HMAC webhook: HTTP 204
- Payment transitioned `PENDING -> SUCCEEDED`: SUCCESS
- Donation transitioned `PENDING -> COMPLETED`: SUCCESS
- Duplicate identical webhook event: HTTP 204
- Final payment/donation linkage remained correct

### Problems Encountered

- Duplicate webhook initially produced HTTP 500 because PostgreSQL unique violation `23505` aborted the transaction (`25P02`) even though the Java exception was caught. Fixed with `INSERT ... ON CONFLICT DO NOTHING`
- Concurrent booking-payment creation with different idempotency keys could race on the active-payment unique index; the integrity race is translated to HTTP 409
- Mock provider initiation was made idempotent using `paymentReference`
- Donation GET initially omitted its linked payment reference; repository/service lookup was added
- Mock `.50` / `.99` amount behavior was normalized to scale 2
- Stale V8 schema-version test data was updated to V9
- Runtime verification exposed local environment issues involving database credentials, JWT configuration, webhook-secret process inheritance, and a stale Java process on port 8080; application behavior was verified after correcting the runtime environment

### Security / Reliability Review

- No card/CVV data stored
- Booking payment amount is server-authoritative
- HMAC-SHA256 authenticates webhook requests
- Constant-time signature comparison used
- Payment/donation state transitions are controlled and auditable
- Duplicate requests/provider events are idempotent
- BOLA/resource ownership protections enforced
- Secrets remain externalized
- PostgreSQL remains transactional source of truth
- No Kafka, AWS, Kubernetes, or other future-module technologies introduced

### Final Review

- MUST FIX: NONE
- Full regression: 203 tests, 0 failures, 0 errors
- End-to-end payment/donation/webhook lifecycle verified at runtime
- Module 13 approved for completion

---

## Next Module

**Module 14 - Notifications & Kafka**

Status: NOT STARTED

Do not automatically implement Module 14.

---

# Module Roadmap

## Phase 0 - Foundation

- [x] Module 00 - Project Architecture & Foundation
- [x] Module 01 - Git, GitHub & Local Development
- [x] Module 02 - Linux & Networking Foundation

## Phase 1 - Application

- [x] Module 03 - Spring Boot Backend Foundation
- [x] Module 04 - Next.js Frontend Foundation
- [x] Module 05 - PostgreSQL & Database Engineering
- [x] Module 06 - Authentication & Authorization
- [x] Module 07 - Temple & Event Management
- [x] Module 08 - Darshan & Slot Management
- [x] Module 09 - Havan & Puja Booking
- [x] Module 10 - Darshan Booking & Concurrency
- [x] Module 11 - Redis & Caching
- [x] Module 12 - Real-Time Availability
- [x] Module 13 - Payments & Donations
- [x] Module 12 - Real-Time Availability
- [x] Module 13 - Payments & Donations
- [ ] Module 14 - Notifications & Kafka
- [ ] Module 15 - Testing & Quality Engineering

## Phase 2 - Containers

- [ ] Module 16 - Docker Fundamentals & Production Images
- [ ] Module 17 - Docker Compose & Local Production Stack
- [ ] Module 18 - Container Security & Optimization

## Phase 3 - Kubernetes

- [ ] Module 19 - Kubernetes Fundamentals
- [ ] Module 20 - Kubernetes Networking & Ingress
- [ ] Module 21 - Kubernetes Configuration & Security
- [ ] Module 22 - Kubernetes Storage & Stateful Workloads
- [ ] Module 23 - Helm
- [ ] Module 24 - Kubernetes Scalability
- [ ] Module 25 - Load Balancing
- [ ] Module 26 - TLS & Certificate Management
- [ ] Module 27 - High Availability & Zero Downtime

## Phase 4 - CI/CD & DevSecOps

- [ ] Module 28 - GitHub Actions CI
- [ ] Module 29 - CD & Container Registry
- [ ] Module 30 - DevSecOps
- [ ] Module 31 - GitOps with Argo CD

## Phase 5 - AWS & Infrastructure

- [ ] Module 32 - AWS Networking & Architecture
- [ ] Module 33 - Terraform
- [ ] Module 34 - EKS Production Deployment

## Phase 6 - Observability & SRE

- [ ] Module 35 - Observability
- [ ] Module 36 - SRE: SLI/SLO/Error Budget
- [ ] Module 37 - Incident Management & RCA
- [ ] Module 38 - Load & Performance Testing

## Phase 7 - Production Engineering

- [ ] Module 39 - Backup & Disaster Recovery
- [ ] Module 40 - Chaos Engineering
- [ ] Module 41 - Advanced Security Hardening
- [ ] Module 42 - Cloud Cost Optimization
- [ ] Module 43 - Production Simulation & Interview Readiness

---

# Module Completion Checklist

Before marking any module COMPLETED, verify:

- [ ] Required implementation is working
- [ ] Tests pass
- [ ] Documentation is updated
- [ ] Security considerations reviewed where applicable
- [ ] Scalability/availability reviewed where applicable
- [ ] Cost implications reviewed where applicable
- [ ] `MODULE_STATUS.md` updated
- [ ] Git commit ready

---

# Cursor Instructions

After completing a module:

1. Update current module status.
2. Mark the completed module with `[x]` in the roadmap.
3. Record implementation details and validation results.
4. Record problems encountered.
5. Set the next module.

Do not automatically implement the next module.
