# Database Engineering

PostgreSQL and Flyway conventions for the Temple Digital Services Platform (Module 05).

## Architecture

```text
Spring Boot (HikariCP pool)
  |
  v
PostgreSQL :5432 / temple_platform_dev
  - Flyway schema history
  - application_metadata (versioned by Flyway)
```

Frontend never connects to PostgreSQL.

## Conventions

- PostgreSQL is the transactional source of truth.
- Schema changes go through Flyway only (`backend/src/main/resources/db/migration/`).
- Use `TIMESTAMPTZ`, explicit constraints, and least-privilege roles.
- Do not store secrets in Git. Passwords come from the environment.
- Domain tables for auth, temples, slots, and bookings belong to later modules.

## Local database

| Item | Value |
|------|--------|
| Database | `temple_platform_dev` |
| Application user (DML) | `temple_app` |
| Optional Flyway/DDL user | `temple_migrator` |

Bootstrap SQL (superuser; passwords substituted locally only when a script requires them):

- `backend/db/01_create_database.sql` — creates `temple_platform_dev` only
- `backend/db/02_roles_and_grants.sql` — optional/reference hardened two-role bootstrap

### Local development default (Module 03 / Module 05)

Continue using `temple_app` for both datasource and Flyway unless you deliberately perform a migration-role transition later. Do not revoke `CREATE` from `temple_app` while it remains the Flyway user.

Do **not** run `02_roles_and_grants.sql` against an existing Module 03/05 local database and expect the app to work immediately. That script is reference material for a planned hardened setup, not Module 05 verification.

- Creating `temple_migrator` does not transfer ownership of existing objects.
- Existing tables, sequences, functions, and Flyway-managed objects may still be owned by `temple_app` or another existing owner.
- `REVOKE CREATE` from `temple_app` is safe only after the migration-owner model is correctly established.
- Setting `SPRING_FLYWAY_USERNAME` to `temple_migrator` requires appropriate ownership and privileges for existing and future Flyway migrations.

## Connection pool (HikariCP)

Defaults are sized for a single local backend process. Replica count × `HIKARI_MAXIMUM_POOL_SIZE` must stay below PostgreSQL `max_connections` (leave headroom for admin/maintenance).

| Variable | Default | Notes |
|----------|---------|--------|
| `HIKARI_MAXIMUM_POOL_SIZE` | 10 | Per application process |
| `HIKARI_MINIMUM_IDLE` | 2 | |
| `HIKARI_CONNECTION_TIMEOUT_MS` | 30000 | Fail fast if the pool is exhausted |
| `HIKARI_LEAK_DETECTION_THRESHOLD_MS` | 20000 in `dev`, 0 in `prod` | Dev-only leak logging |

## Flyway

| Migration | Purpose |
|-----------|---------|
| `V1__baseline.sql` | `application_metadata` |
| `V2__database_engineering.sql` | `updated_at` trigger, CHECK constraints, schema version `2` |

Optional separate migrator credentials: `SPRING_FLYWAY_USERNAME` and `SPRING_FLYWAY_PASSWORD`. If unset, Flyway uses the datasource user.

## API

`GET /api/v1/system/database` returns `schemaVersion` (from `application_metadata`) and `flywayVersion` (latest successful Flyway version). No connection strings or passwords.

## Verification

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
mvn test
```

With the API running:

```powershell
curl http://localhost:8080/api/v1/system/database
curl http://localhost:8080/actuator/health
```

## Cost

Local PostgreSQL only in this module. No AWS RDS or extra paid database services.
