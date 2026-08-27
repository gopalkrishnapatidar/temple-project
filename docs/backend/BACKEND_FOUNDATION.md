# Backend Foundation

Spring Boot backend foundation for the Temple Digital Services Platform (Module 03).

## Architecture

```text
Client
  |
  v
Spring Boot REST API :8080
  |
  v
PostgreSQL :5432
```

Base package: `com.temple.platform`

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 18+ running locally
- Database: `temple_platform_dev`
- Application user: `temple_app` (with access to the database)

## Environment Variables

| Variable | Description | Dev default |
|----------|-------------|-------------|
| `SPRING_PROFILES_ACTIVE` | Active profile (`dev` or `prod`) | `dev` |
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/temple_platform_dev` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `temple_app` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | **required** — no default in Git |

Copy `backend/.env.example` to `backend/.env` (or set variables in your shell). Never commit real passwords.

## Build

```bash
cd backend
mvn clean package
```

## Run (dev profile)

Set the database password, then start the application:

**PowerShell:**

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
mvn spring-boot:run
```

**Bash:**

```bash
cd backend
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_PASSWORD=your_password
mvn spring-boot:run
```

The API listens on `http://0.0.0.0:8080`.

## Test

```bash
cd backend
$env:SPRING_DATASOURCE_PASSWORD="your_password"   # PowerShell
mvn test
```

The full context test requires a reachable PostgreSQL instance. The `SystemController` ping test runs without a database (`@WebMvcTest`).

## Health Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/health` | Overall health |
| `GET /actuator/health/liveness` | Kubernetes liveness probe |
| `GET /actuator/health/readiness` | Kubernetes readiness probe |

Only `health` and `info` actuator endpoints are exposed.

## System Endpoints

| Endpoint | Response |
|----------|----------|
| `GET /api/v1/system/ping` | `{"status":"UP","message":"Temple Platform API is running"}` |
| `GET /api/v1/system/info` | Application name, version, active profiles (no secrets) |

## Common Startup Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `Connection refused` to PostgreSQL | PostgreSQL not running or wrong host/port | Start PostgreSQL; verify `SPRING_DATASOURCE_URL` |
| `password authentication failed` | Wrong or missing password | Set `SPRING_DATASOURCE_PASSWORD` |
| `database "temple_platform_dev" does not exist` | Database not created | Create the database and grant access to `temple_app` |
| `Port 8080 already in use` | Another process on 8080 | Stop the other process or change `server.port` |
| Flyway migration failure | Schema conflict or permissions | Check DB user privileges; inspect `flyway_schema_history` |

Errors return JSON with `timestamp`, `status`, `error`, `message`, and `path`. Stack traces are not sent to clients.
