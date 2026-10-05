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
| Completed | 23 / 44 |
| Current Phase | Phase 3 - Kubernetes |
| Current Module | Module 23 - Helm Package Management |
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
- [x] Module 14 - Notifications & Kafka
- [x] Module 15 - Testing & Quality Engineering
- [x] Module 16 - Docker Fundamentals & Production Images
- [x] Module 17 - Docker Compose & Local Production Stack
- [x] Module 18 - Container Security & Optimization

---

## Module 19 - Kubernetes Fundamentals

**Status:** COMPLETED

### Implementation

- `k8s/namespace.yaml` — namespace `temple`
- `k8s/priorityclass.yaml` — `temple-critical` (value 100000, `preemptionPolicy: Never`)
- `k8s/daemonset.yaml` — `temple-node-agent` (pause infra agent, `RollingUpdate`, `maxUnavailable: 1`)
- `k8s/job.yaml` — `temple-validation-job` (`restartPolicy: Never`, `backoffLimit: 3`, `ttlSecondsAfterFinished: 3600`)
- `k8s/cronjob.yaml` — hourly `:15` schedule, `concurrencyPolicy: Forbid`, history limits 3/1
- `k8s/pdb.yaml` — `temple-backend-pdb` (`policy/v1`, `minAvailable: 0` for single-replica local drain)
- `k8s/backend-deployment.yaml` — minimal backend Deployment (1 replica, probes, grace period 30s, `priorityClassName: temple-critical`, Secret `temple-backend-env`)

### Independent Cluster Validation

| Check | Result |
|-------|--------|
| Docker Desktop Kubernetes | SUCCESS - kind cluster running Kubernetes v1.36.1; node Ready |
| Final server-side dry-run | SUCCESS - all 7 manifests accepted by Kubernetes API server |
| Namespace | SUCCESS - `temple` Active with expected labels |
| PriorityClass | SUCCESS - `temple-critical`, value 100000, `preemptionPolicy: Never`; backend Pod assigned priority 100000 |
| DaemonSet | SUCCESS - 1/1 Ready; deleted Pod automatically recreated; security/resources/10s termination grace verified |
| Job | SUCCESS - deterministic Job completed; controlled temporary failure confirmed `restartPolicy: Never` and bounded backoff; temporary resources removed |
| CronJob | SUCCESS - manual trigger and real `15 * * * *` scheduled execution completed with `deterministic-output=temple-cronjob-ok` |
| Backend Deployment | SUCCESS - 1/1 Ready/Available, probes healthy, resources and `/tmp` EmptyDir verified, PriorityClass active |
| Backend security | SUCCESS - `runAsNonRoot: true`, UID 100, GID 101, read-only root filesystem, no privilege escalation, RuntimeDefault seccomp, all capabilities dropped |
| Graceful termination / recovery | SUCCESS - live Pod uses 30s termination grace; Pod deletion triggered automatic replacement which returned Ready with 0 restarts |
| PodDisruptionBudget | SUCCESS - selector matches backend; `minAvailable: 0` intentional for local single replica; healthy state reports 1 allowed disruption |

### Validation Issues Resolved

- Docker Desktop Kubernetes v1.37.0-rc.1 failed cluster initialization because its kubeadm configuration used an incompatible API version; selecting stable Kubernetes v1.36.1 recreated a healthy local kind cluster.
- Backend initially failed with `CreateContainerConfigError` because the image declares named user `app` while `runAsNonRoot` requires a verifiable numeric user. Image inspection confirmed UID 100 / GID 101; `runAsUser: 100` and `runAsGroup: 101` were added without weakening the security policy.
- Runtime Secret `temple-backend-env` was created from local environment values for validation only; no secret values were committed to Git.

### Assumptions / Limitations

- PDB `minAvailable: 0` is intentional for one-replica local clusters; increase replicas and tighten PDB for HA drills
- Backend Deployment is minimal (no Service/Ingress/ConfigMap stack); Modules 20+ extend networking and configuration
- DaemonSet uses pause (not a logging/metrics platform)

---

## Module 20 - Kubernetes Networking & Ingress

**Status:** COMPLETED

Independent local Kubernetes validation completed successfully: Services and EndpointSlices, CoreDNS service discovery, frontend-to-backend HTTP, ingress-nginx routing, end-to-end frontend/backend status, backend and frontend Pod recreation, wrong-selector/no-endpoint failure, invalid-DNS failure, wrong-targetPort connection failure, broken-Ingress-backend detection, server-side dry-runs, security/resource regression checks, and repository integrity checks all passed.

### Implementation

- `k8s/backend-service.yaml` — ClusterIP `temple-backend`, port `http` 8080 → container port `http` (8080); selector matches the backend Deployment
- `k8s/frontend-deployment.yaml` — `temple-frontend`, 1 replica, image `temple-frontend:module16` (`IfNotPresent`), container port 3000, `BACKEND_API_BASE_URL=http://temple-backend:8080`, UID/GID 1001, read-only root, probes on existing `GET /`
- `k8s/frontend-service.yaml` — ClusterIP `temple-frontend`, port `http` 3000 → container port `http` (3000)
- `k8s/ingress.yaml` — Ingress `temple`, `networking.k8s.io/v1`, `ingressClassName: nginx`, host `temple.local`, path `/` Prefix to Service `temple-frontend` port `http`; HTTP only

### Runtime prerequisites

- Frontend image must already be present for the Docker Desktop Kubernetes node: `docker build -t temple-frontend:module16 ./frontend` (same tag as `compose.yml`). This repository does not load or import the image.
- Backend image and Secret `temple-backend-env` remain the Module 19 prerequisites. PostgreSQL, Redis, and Kafka are still outside the cluster; the backend Pod cannot become Ready without them.
- Docker Desktop Kubernetes does not include an Ingress controller. Install ingress-nginx separately so class `nginx` exists. Do not add the controller manifests to `k8s/`.
- Local validation used `kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8088:80` because the ingress-nginx LoadBalancer external IP remained `<pending>`. Requests to `http://127.0.0.1:8088/` with host `temple.local` successfully exercised the Ingress route. No TLS listener is configured.

### Intentionally deferred

- TLS, certificates, cert-manager, and HTTPS Ingress — Module 26
- NetworkPolicy, RBAC, ServiceAccount hardening, and ConfigMap/Secret configuration design — Module 21
- Helm, HPA, persistent storage, StatefulSets, AWS load balancers, and Argo CD — later modules
- Browser-direct `/api` Ingress path — current frontend calls the backend only from the server

---

## Module 21 - Kubernetes Configuration & Security

**Status:** COMPLETED

Independent local Kubernetes validation completed successfully. ConfigMap/Secret separation, dedicated ServiceAccounts, RBAC least privilege, NetworkPolicy enforcement, workload recreation, and end-to-end application connectivity were verified.

### Implementation

- `k8s/backend-configmap.yaml` — `temple-backend-config`: non-sensitive Spring, Redis, and Kafka runtime configuration; JDBC, Redis, and Kafka use the existing Compose DNS names (`postgres`, `redis`, `kafka`) through the Kubernetes node's established attachment to the Compose network
- `k8s/frontend-configmap.yaml` — `temple-frontend-config`: `BACKEND_API_BASE_URL=http://temple-backend:8080`
- `k8s/serviceaccounts.yaml` — dedicated `temple-backend` and `temple-frontend` ServiceAccounts with `automountServiceAccountToken: false`
- `k8s/networkpolicy.yaml` — frontend ingress from ingress-nginx controller Pods; frontend egress to backend TCP 8080 and kube-system DNS TCP/UDP 53; backend ingress from frontend TCP 8080 only; backend egress remains unrestricted for external PostgreSQL, Redis, and Kafka
- `k8s/backend-deployment.yaml` — uses `serviceAccountName: temple-backend` and `envFrom` ConfigMap followed by the existing runtime Secret; inline `SPRING_PROFILES_ACTIVE` removed
- `k8s/frontend-deployment.yaml` — uses `serviceAccountName: temple-frontend` and `envFrom` ConfigMap; inline `BACKEND_API_BASE_URL` removed

### Secret boundary

- No Secret manifest is committed to Git.
- The validated runtime Secret `temple-backend-env` contains only `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_DATASOURCE_USERNAME`.
- Non-sensitive deployment configuration is supplied by ConfigMaps. Secret values were not printed or committed during final validation.

### RBAC

- No Role or RoleBinding was added because neither application requires Kubernetes API access.
- `kubectl auth can-i` confirmed denial for Pods, Secrets, and ConfigMaps for both dedicated ServiceAccounts: all 6 checks returned `no`.
- Effective Pod configuration confirmed no projected ServiceAccount API-token volume or `/var/run/secrets/kubernetes.io/serviceaccount` mount.

### NetworkPolicy

- Baseline unrelated test Pod could reach backend readiness before NetworkPolicy enforcement.
- After applying NetworkPolicy, the same unrelated Pod-to-backend request timed out, confirming enforcement by the local cluster networking implementation.
- Authorized frontend-to-backend traffic remained allowed.
- Frontend DNS resolution remained functional through the explicit kube-system DNS egress rule.
- ingress-nginx controller traffic to the frontend remained allowed.
- Backend egress is intentionally unrestricted because PostgreSQL, Redis, and Kafka remain outside Kubernetes in the local Compose environment.

### Validation Results

- ConfigMaps, ServiceAccounts, NetworkPolicies, and updated Deployments applied successfully
- Backend and frontend Pods used their dedicated ServiceAccounts with no automatic Kubernetes API token mount
- RBAC least privilege: 6/6 `kubectl auth can-i` checks returned `no`
- Unauthorized unrelated Pod → backend: reachable before NetworkPolicy; blocked by timeout after policy activation
- Authorized frontend → backend readiness: HTTP 200 with PostgreSQL UP
- Frontend DNS resolution of `temple-backend`: SUCCESS
- ingress-nginx controller → frontend Pod: HTTP 200
- External `temple.local` Ingress path through local port-forward: HTTP 200
- Backend Pod deletion/recreation: replacement Ready with 0 restarts; configuration and readiness revalidated
- Frontend Pod deletion/recreation: replacement Ready with 0 restarts; frontend → backend and external Ingress paths revalidated
- Final Kubernetes server-side dry-run: SUCCESS for all Module 21 resources
- `git diff --check`: PASS
- Repository Kubernetes manifests contain no committed `kind: Secret`

### Intentionally Deferred

- Secret-management integrations such as External Secrets or Vault
- Backend egress allowlisting for external dependency addresses; local Docker/Compose addressing is not treated as a stable production allowlist
- Persistent storage and StatefulSets — Module 22
- Helm — Module 23
- HPA — Module 24
- Production load balancing — Module 25
- TLS/cert-manager — Module 26
- High availability architecture — Module 27

---
## Module 22 - Kubernetes Storage & Stateful Workloads

**Status:** COMPLETED

Independent local Kubernetes validation completed successfully. Dynamic persistent-volume provisioning, StatefulSet identity, PVC retention, persistent data across Pod recreation and scale cycles, headless-Service DNS, non-root execution, read-only root filesystem behavior, and service-account-token hardening were verified.

### Implementation

- `k8s/stateful-storage-service.yaml` - headless Service `temple-stateful-storage` providing stable StatefulSet network identity on TCP 8080
- `k8s/stateful-storage-statefulset.yaml` - isolated single-replica `temple-stateful-storage` StatefulSet using `python:3.12-alpine` for storage validation
- `volumeClaimTemplates` creates the per-Pod `data` claim and mounts it at `/data`
- Storage request: `128Mi`, access mode `ReadWriteOnce`; no standalone PVC, manually provisioned PV, or new StorageClass is committed
- The cluster default `standard` StorageClass (`rancher.io/local-path`) dynamically provisions the PV with `WaitForFirstConsumer` binding and `Delete` reclaim policy
- Container runs as UID/GID 1000 with `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`, `RuntimeDefault` seccomp, and all capabilities dropped
- `/data` remains writable as the persistent volume while the image root filesystem remains read-only
- `automountServiceAccountToken: false` prevents an unnecessary Kubernetes API credential from being projected into the demonstration Pod
- PostgreSQL, Redis, and Kafka remain outside Kubernetes in the current local architecture; this workload is intentionally isolated from the Temple frontend/backend business path

### Validation

- Both Module 22 manifests passed Kubernetes server-side dry-run
- StatefulSet reached `1/1` Ready with 0 restarts
- `volumeClaimTemplates` created PVC `data-temple-stateful-storage-0`; it became `Bound` to a dynamically provisioned PV through StorageClass `standard`
- Persistent test data written to `/data/persistence-test.txt` survived explicit Pod deletion and StatefulSet recreation
- StatefulSet Pod retained stable ordinal identity `temple-stateful-storage-0` while receiving a new Pod IP after recreation
- Headless Service reported `clusterIP: None`; EndpointSlice tracked the active Pod; `temple-stateful-storage-0.temple-stateful-storage` resolved successfully through cluster DNS
- Pod-template hardening rollout completed successfully and persistent data remained available afterward
- Effective Pod configuration confirmed `automountServiceAccountToken: false` with no projected `kube-api-access-*` volume or service-account-token mount
- Scaling StatefulSet from 1 to 0 removed the Pod while the PVC remained `Bound`; scaling back to 1 recreated ordinal 0 and preserved the original persistent data
- Runtime write test proved `/data` is writable while a write to the container root filesystem was blocked
- Runtime identity confirmed UID/GID 1000
- Local-path storage exposed the underlying host filesystem capacity from inside `/data`; the `128Mi` PVC request must not be interpreted as proof of a filesystem-level 128Mi quota in this local provisioner
- Historical startup probe connection-refused events occurred briefly while the Python HTTP server started; the final Pod was Ready with 0 restarts, so no probe change was required
- Existing backend and frontend workloads remained healthy throughout Module 22 validation
- Manifest whitespace validation passed before staging; staged Git whitespace validation passed

### Lifecycle Boundary

- Pod deletion and StatefulSet scale-down were tested without deleting the PVC
- PVC deletion and PV reclaim/deletion behavior were intentionally not executed because that would destroy the validation data; the observed PV reclaim policy is `Delete`
- A PVC provides persistent workload storage but is not a backup or disaster-recovery strategy

### Intentionally Deferred

- Helm packaging - Module 23
- Horizontal Pod Autoscaling - Module 24
- Production load balancing - Module 25
- TLS/cert-manager - Module 26
- High availability and production stateful architecture - Module 27
- Production cloud storage, backup/restore, snapshots, and disaster-recovery implementation are outside this local Module 22 demonstration

---
## Module 23 - Helm Package Management

**Status:** COMPLETED

Independent Helm implementation and runtime validation completed successfully. The existing stateless Temple Kubernetes resources were packaged into a Helm application chart and adopted into release `temple` without deleting or recreating the existing resources.

### Implementation

- Added application chart `helm/temple` with `Chart.yaml`, `values.yaml`, `values.schema.json`, `.helmignore`, shared helpers, and templates
- Helm manages exactly nine existing stateless resources: two ConfigMaps, two ServiceAccounts, two Deployments, two Services, and the Ingress
- Existing selector contracts remain stable and independent of Helm release/chart names so Services, PDB, and NetworkPolicies continue matching the intended workloads
- Backend and frontend configuration is exposed through chart values and validated by JSON Schema
- Backend continues to reference existing runtime Secret `temple-backend-env`; secret values are not stored in the chart
- ConfigMap checksum annotations trigger workload rollouts when the corresponding configuration changes
- Existing raw Kubernetes manifests remain in the repository as implementation history/reference and must not be blindly reapplied over Helm-managed resources
- Namespace, PriorityClass, PDB, NetworkPolicies, runtime Secret, ingress-nginx, Module 19 auxiliary workloads, Module 22 StatefulSet/storage resources, and PostgreSQL/Redis/Kafka remain outside this Helm release

### Validation

- Helm lint and JSON Schema validation passed for default and valid override values; invalid replica count, pull policy, path type, and empty Secret name were rejected
- Default rendering produced exactly nine intended resources with no Secret, HPA, Certificate, or Issuer
- All rendered resources passed Kubernetes server-side dry-run
- ConfigMap checksum validation proved backend and frontend configuration changes independently alter only the corresponding workload checksum
- Pre-adoption validation confirmed rendered selectors, ServiceAccounts, images, ConfigMaps, external Secret reference, and Module 22 persistence matched the live contracts
- Helm 4 `install --take-ownership` adopted the nine existing resources without delete/recreate or force replacement
- Post-adoption validation confirmed all nine resources carry Helm release ownership while excluded resources remain outside Helm
- A normal replica upgrade exposed a real server-side-apply conflict on frontend `.spec.replicas` with historical `kubectl-client-side-apply` field ownership
- Helm rollback successfully restored the healthy release, but a subsequent normal upgrade reproduced the same field conflict
- A deliberate one-time `--force-conflicts` upgrade transferred the conflicting desired-state field ownership to Helm without replacing the Deployment
- After field ownership migration, a normal Helm upgrade with `forceConflicts=false` succeeded and reset the frontend from two replicas to the chart default of one
- Final release revision 6 is deployed with backend and frontend both `1/1`, healthy Service endpoints, and no user-supplied value overrides
- Module 22 persistent data remained intact throughout Helm adoption, failed upgrades, rollback, ownership migration, and final upgrade
- Final Helm lint and Git whitespace validation passed

### Helm Migration Lesson

Helm release ownership metadata and Kubernetes managed-field ownership are separate concerns. `--take-ownership` successfully transferred the existing resources into the Helm release, but it did not automatically remove historical field ownership created by `kubectl` client-side apply. The conflicting `.spec.replicas` field required one controlled `--force-conflicts` migration. After Helm became the authoritative server-side-apply manager for that field, subsequent normal upgrades worked without force. `--force-conflicts` is therefore documented as a one-time migration action here, not a routine deployment flag.

### Intentionally Deferred

- Horizontal Pod Autoscaling - Module 24
- Production load balancing - Module 25
- TLS/cert-manager - Module 26
- High availability and zero-downtime architecture - Module 27
- CI/CD, GitOps/Argo CD, container registry, and EKS deployment remain later-module concerns
- PostgreSQL, Redis, and Kafka remain outside Kubernetes in the current local architecture

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
- Flyway `V1__baseline.sql` â€” `application_metadata` table
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
| `GET /api/v1/system/ping` | SUCCESS â€” status UP |
| `GET /api/v1/system/info` | SUCCESS â€” application `temple-platform`, version `0.0.1-SNAPSHOT`, active profile `dev` |
| `GET /actuator/health` | UP â€” PostgreSQL database health UP |
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
| ESLint | PASS â€” no warnings or errors |
| Production build | SUCCESS |
| Type check (build) | PASS |

### Manual Runtime Validation

| Check | Result |
|-------|--------|
| `npm run dev` | SUCCESS â€” served on `http://localhost:3000` |
| Home page title/content | SUCCESS |
| Backend status section (backend stopped) | SUCCESS â€” controlled unavailable message shown |
| `GET http://localhost:8080/api/v1/system/ping` | NOT TESTED â€” backend not running in agent environment |
| Backend status success state | NOT TESTED â€” requires running Spring Boot backend |

### Problems Encountered

- Node.js/npm were not initially available in the agent shell PATH; Node.js LTS was installed via `winget` to run frontend validation.
- Backend integration success state could not be verified because the Spring Boot backend was not running and database credentials were not configured in the agent environment.

---

## Module 05 - PostgreSQL & Database Engineering

**Status:** COMPLETED

### Implementation

- Explicit HikariCP pool settings (env-overridable); leak detection on in `dev`, off in `prod`
- Optional Flyway credentials (`SPRING_FLYWAY_USERNAME` / `SPRING_FLYWAY_PASSWORD`)
- Flyway `V2__database_engineering.sql` â€” `updated_at` trigger, CHECK constraints
- Flyway `V3__fix_application_metadata_updated_at_timestamp.sql` â€” `clock_timestamp()` trigger fix
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
| `GET /api/v1/system/database` | SUCCESS â€” `schemaVersion` `3`, `flywayVersion` `3` |
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

- Flyway `V4__account.sql` â€” `account` table with unique normalized email and role/status CHECKs
- Spring Security filter chain (stateless JWT, default deny)
- `POST /api/v1/auth/register` â€” public; always creates `DEVOTEE` + `ACTIVE`; duplicate email â†’ 409; client cannot self-assign admin role
- `POST /api/v1/auth/login` â€” public; BCrypt authentication; generic invalid-credentials response
- `GET /api/v1/auth/me` â€” protected; identity from JWT `sub` / SecurityContext only
- JWT HS256 access tokens (`JWT_SECRET` required, no insecure production default); 15-minute lifetime
- Missing/invalid/expired/tampered token â†’ 401; insufficient role â†’ 403
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
| Registration â†’ `DEVOTEE` / `ACTIVE` | SUCCESS |
| Duplicate registration | SUCCESS â€” 409 |
| Self-assigned `PLATFORM_ADMIN` in JSON | SUCCESS â€” still `DEVOTEE` |
| Valid login â†’ JWT (900s expiry) | SUCCESS |
| `GET /api/v1/auth/me` with valid token | SUCCESS |
| Missing JWT | SUCCESS â€” 401 JSON |
| `DEVOTEE` â†’ `GET /api/v1/system/database` | SUCCESS â€” 403 JSON |
| `GET /actuator/health` (no auth) | SUCCESS â€” UP |
| Wrong password / unknown user login | SUCCESS â€” 401 |

### Problems Encountered

- Stale `target/test-classes/application.yml` from a deleted test resource shadowed main `application.yml`; `mvn clean test` resolved it.
- Runtime startup failure was caused by an incorrect local `SPRING_DATASOURCE_PASSWORD`, not application code.

---

## Module 07 - Temple & Event Management

**Status:** COMPLETED

### Implementation

- Flyway `V5__temple_and_event.sql` â€” `temple`, `temple_admin_assignment`, `temple_event`
- Temple CRUD (create `PLATFORM_ADMIN` only; update assigned `TEMPLE_ADMIN` or `PLATFORM_ADMIN`)
- Temple admin assignment management (`PLATFORM_ADMIN` only; duplicate â†’ 409)
- Temple event CRUD with bounded pagination (default 20, max 100); safe page-offset validation
- Event create status server-owned â€” always `DRAFT`; lifecycle transitions enforced on update
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
| `DRAFT` â†’ `PUBLISHED` transition | SUCCESS |
| `PUBLISHED` â†’ `DRAFT` rejected | SUCCESS â€” HTTP 400 |
| Invalid schedule (`endAt` â‰¤ `startAt`) | SUCCESS â€” HTTP 400, message `Event end time must be after start time` |
| DB-backed authorization after assignment removal | SUCCESS â€” existing `TEMPLE_ADMIN` JWT immediately receives HTTP 403 |

### Problems Encountered

- One test run failed due to incorrect local `SPRING_DATASOURCE_PASSWORD` (environment configuration, not code).
- Runtime startup failed once because `JWT_SECRET` was missing (environment configuration, not code).
- `curl.exe` JSON quoting produced generic `Invalid request` during one manual schedule test; PowerShell `ConvertTo-Json` confirmed the intended validation response.

---

## Module 08 - Darshan & Slot Management

**Status:** COMPLETED

### Implementation

- Flyway `V6__darshan_and_slot.sql` â€” `darshan`, `darshan_slot`, overlap EXCLUDE constraint (`btree_gist`)
- Darshan CRUD nested under temples; slot CRUD nested under darshans
- Darshan lifecycle: `ACTIVE` / `INACTIVE`; slot lifecycle: `AVAILABLE` / `CANCELLED` (create defaults server-owned)
- Reuses `TempleAuthorizationService` and DB-backed temple assignment checks; nested Temple â†’ Darshan â†’ Slot BOLA protection
- Devotee visibility: `ACTIVE` temple + `ACTIVE` darshan; non-cancelled slots with `end_at > now()`
- Slot listing: temple-timezone `date` filter, optional `from`/`to` instant range (max 90 days), pagination
- No booking, Redis, Kafka, payments, notifications, or real-time capacity engine

### Database

- Flyway V6 applied; `schema_version` = `6`
- FK `darshan.temple_id` â†’ `temple`, `darshan_slot.darshan_id` â†’ `darshan`
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
| `GET /actuator/health` | UP â€” PostgreSQL UP |
| `PLATFORM_ADMIN` creates `ACTIVE` darshan | SUCCESS |
| `AVAILABLE` slot creation | SUCCESS |
| Adjacent slots | SUCCESS |
| Overlapping slot | SUCCESS â€” HTTP 409, business error (no raw DB leak) |
| Temple-timezone `date` slot query | SUCCESS |
| `DEVOTEE` read | SUCCESS |
| `DEVOTEE` slot create | SUCCESS â€” HTTP 403 |
| Cross-temple darshan BOLA | SUCCESS â€” HTTP 404 |
| Cross-darshan slot BOLA | SUCCESS â€” HTTP 404 |
| Slot cancellation | SUCCESS |
| `PLATFORM_ADMIN` sees `CANCELLED` history | SUCCESS |
| `DEVOTEE` does not see `CANCELLED` slot | SUCCESS |
| `capacity=0` | SUCCESS â€” HTTP 400 |
| `endAt <= startAt` | SUCCESS â€” HTTP 400 |
| Expired JWT | SUCCESS â€” HTTP 401; re-login restored access |

### Problems Encountered

- Stale `AuthApiTest` expected `schemaVersion`/`flywayVersion` `5` after V6; updated to `6`.
- Agent environment initially lacked `SPRING_DATASOURCE_PASSWORD` / `JWT_SECRET` for integration tests (local configuration, not code).

---

## Module 09 - Havan & Puja Booking

**Status:** COMPLETED

### Implementation

- Flyway `V7__ritual_and_slot.sql` â€” `ritual`, `ritual_slot` (no overlap EXCLUDE)
- Shared Ritual bounded context: Temple â†’ Ritual (PUJA/HAVAN) â†’ RitualSlot
- Ritual lifecycle: `ACTIVE` / `INACTIVE`; slot lifecycle: `AVAILABLE` / `CANCELLED` (create defaults server-owned)
- `durationMinutes` is current offering configuration; existing slot `startAt`/`endAt` are not rewritten
- `price` is `NUMERIC(12,2)` / `BigDecimal`; currency `INR` only; zero allowed; negative rejected
- Domain/API absolute timestamps are `Instant`; PostgreSQL `TIMESTAMPTZ`; JDBC maps via `OffsetDateTime`
- Temple-local `date` queries use IANA ZoneId `[startOfDay, nextStartOfDay)`; `date` combined with `from`/`to` â†’ 400
- Overlapping Ritual slots intentionally allowed (no Darshan-style GiST EXCLUDE)
- Authorization: `PLATFORM_ADMIN` global; `TEMPLE_ADMIN` DB assignment only; `DEVOTEE` hierarchical read
- Nested Temple â†’ Ritual â†’ Slot BOLA â†’ 404
- No booking, capacity, priest/hall assignment, Redis, Kafka, payments, or notifications

### Database

- Flyway V7 applied; `schema_version` = `7`
- FK `ritual.temple_id` â†’ `temple`, `ritual_slot.ritual_id` â†’ `ritual` (`ON DELETE RESTRICT`)
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
| Overlapping Ritual slots | SUCCESS â€” both created |
| Temple-local date filter | SUCCESS |
| Invalid duration, price, schedule, currency | SUCCESS â€” HTTP 400 |
| Ambiguous `date` + `from`/`to` | SUCCESS â€” HTTP 400 |
| DEVOTEE read | SUCCESS |
| DEVOTEE write | SUCCESS â€” HTTP 403 |
| Cross-Temple Ritual BOLA | SUCCESS â€” HTTP 404 |
| Cross-Ritual Slot BOLA | SUCCESS â€” HTTP 404 |
| Slot AVAILABLE â†’ CANCELLED | SUCCESS |
| Admin sees CANCELLED history | SUCCESS |
| DEVOTEE does not see CANCELLED slot | SUCCESS |
| CANCELLED â†’ AVAILABLE | SUCCESS â€” HTTP 400 |
| Ritual ACTIVE â†’ INACTIVE | SUCCESS |
| Admin sees INACTIVE Ritual | SUCCESS |
| DEVOTEE INACTIVE Ritual / its slots | SUCCESS â€” HTTP 404 |
| Expired JWT | SUCCESS â€” HTTP 401; re-login restored access |

### Problems Encountered

- PostgreSQL JDBC does not support `ResultSet.getObject(..., Instant.class)` for `timestamptz`. Repository maps `OffsetDateTime` â†” `Instant`; domain/API remain `Instant`.
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

- Flyway `V8__booking_and_ritual_slot_capacity.sql` â€” positive `ritual_slot.capacity`; `booking` table
- REST: `POST/GET /api/v1/bookings`, `GET/PATCH /api/v1/bookings/{bookingReference}`
- PostgreSQL authoritative for booking and capacity truth (no derived `available_capacity`)
- Pessimistic slot-row `SELECT â€¦ FOR UPDATE` on booking create, cancel, and Darshan/Ritual capacity updates
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

- `BookingRepositoryTest` PostgreSQL `25P02` after multiple constraint violations in one `@Transactional` test â€” fixed by isolating each DB constraint assertion in its own test method.
- `concurrentCapacityReductionAndBookingRespectInvariant` failed with HTTP 400 when racing capacity reduction to zero â€” test redesigned to race valid operations (capacity 2â†’1 vs booking quantity 1).

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

- Database engineering: HikariCP, Flyway V1â€“V3, optional migrator role (reference-only), JDBC metadata API
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

- Start as a modular monolith (frontend â†’ backend â†’ PostgreSQL).
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

## Module 14 - Notifications & Kafka

**Status:** COMPLETED

### Implementation

- Flyway `V10__notifications_and_outbox.sql` - durable `outbox_event` and `notification` tables
- Transactional outbox integrated with booking confirmation/cancellation and terminal payment transitions
- Kafka domain-event publishing through scheduled bounded outbox polling
- Kafka topic `temple.domain.events` with stable aggregate-reference message keys
- Consumer group `temple-notification-consumer`
- Dead-letter topic `temple.domain.events.DLT` for unrecoverable events
- Versioned event envelope with stable UUID `eventId`
- Idempotent notification consumer using unique `source_event_id`
- Mock email notification delivery with `PENDING`, `SENT`, and `FAILED` lifecycle
- Notification APIs: `GET /api/v1/notifications` and `GET /api/v1/notifications/{notificationReference}`
- DEVOTEE ownership/BOLA protection; PLATFORM_ADMIN may list all notifications
- Kafka configuration externalized through environment variables
- PostgreSQL remains transactional source of truth; Kafka is asynchronous event transport

### Database

- Flyway V10 applied successfully; `schema_version` = `10`
- `outbox_event.event_id` is unique and created inside the same PostgreSQL transaction as the business state change
- `notification.source_event_id` is unique for durable consumer idempotency
- Expected duplicate events use `INSERT ... ON CONFLICT DO NOTHING`
- Outbox tracks `published_at`, `publish_attempts`, and sanitized `last_error`

### Automated Validation

Full backend regression (`mvn clean test`, Kafka disabled for deterministic automated testing):

| Metric | Result |
|--------|--------|
| Tests run | 228 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

Focused Module 14 suite: 25 tests, 0 failures, 0 errors.

### Manual Runtime Validation

- Local Apache Kafka 4.3.1 KRaft broker/controller started successfully on ports 9092/9093
- `temple.domain.events`: 3 partitions, replication factor 1 for local development
- `temple.domain.events.DLT`: 1 partition, replication factor 1
- Booking confirmation created an outbox event and returned successfully
- Scheduled publisher published the outbox event to Kafka
- Notification consumer processed the event and persisted exactly one `SENT` EMAIL_MOCK notification
- Owner notification API returned the generated notification
- Consumer-group lag reached 0
- Kafka outage did not roll back booking: business transaction succeeded and durable outbox row remained unpublished
- Publisher retry attempts and sanitized `Kafka is unavailable` error were persisted while Kafka was unavailable
- After Kafka restart, pending outbox event was published automatically and notification transitioned to `SENT`
- Exact event replay produced no duplicate notification because `source_event_id` is unique
- Deliberately malformed Kafka record was routed to DLT and confirmed by console consumer
- Final consumer-group lag was 0

### Problems Encountered

- Initial V10 migration omitted the `application_metadata.schema_version = 10` update. Because the original V10 had already run locally, the local V10 tables/history entry were deliberately reset and the corrected migration reapplied rather than using Flyway repair
- Existing schema-version tests still expected V9 and were updated to V10
- `DomainEventProcessor` originally attempted delivery and failure persistence in one transaction; rethrow rolled back FAILED state. Processing was redesigned using separate `REQUIRES_NEW` database transactions around preparation and final status persistence, with external delivery outside the transaction
- Focused tests initially observed historical local database rows; test setup was isolated without changing production behavior
- PowerShell requires quoting Maven `-Dtest` expressions containing commas
- Kafka Windows startup script depended on removed `wmic`; explicitly setting `KAFKA_HEAP_OPTS=-Xmx1G -Xms1G` bypassed that legacy detection
- Kafka CLI emitted a non-blocking Log4j reconfiguration warning
- Legacy `kafka.tools.GetOffsetShell` is unavailable in Kafka 4.3.1; runtime DLT verification used the console consumer instead

### Reliability / Security Review

- PostgreSQL-to-Kafka dual-write risk is addressed with the transactional outbox pattern
- Business transactions do not depend on Kafka availability
- Publisher retries unpublished events after broker recovery
- Duplicate publication is tolerated through stable event IDs and durable consumer idempotency
- Poison/unrecoverable events are isolated through DLT
- Error text stored in outbox/notification records is sanitized and bounded
- Notification ownership/BOLA protections are enforced
- Secrets remain externalized
- Kafka is not transactional booking/payment authority
- Multi-instance publisher duplicate publication remains safe through consumer idempotency; publisher row claiming can be introduced later if scaling requires it
- External notification providers should eventually support idempotency because delivery success followed by a crash before `markSent` can otherwise cause duplicate external delivery

### Final Review

- MUST FIX: NONE
- Focused Module 14 tests: 25 PASS
- Full backend regression: 228 tests, 0 failures, 0 errors
- Real Kafka publish/consume, outage recovery, duplicate-event idempotency, DLT routing, and zero consumer lag verified
- Module 14 approved for completion

---

## Module 15 - Testing & Quality Engineering

**Status:** COMPLETED

Implementation and independent local validation completed successfully.

### Implementation

- Testcontainers PostgreSQL (`postgres:16-alpine`) shared for the Maven JVM; one disposable database per test run
- `IsolatedPostgres` + `IsolatedPostgresInitializer` + `@IsolatedPostgresIntegrationTest` apply Testcontainers datasource/Flyway at context startup for opted-in integration tests only (production datasource unchanged)
- Existing `@SpringBootTest` DB integration tests migrated to `@IsolatedPostgresIntegrationTest`; `@WebMvcTest` and unit tests unchanged
- Flyway V1â€“V10 applied to the test container; `application_metadata.schema_version` = 10
- JaCoCo agent + HTML report on `verify`; bootstrap `TemplePlatformApplication` excluded from the report; no minimum coverage percentage
- Frontend `typecheck` script added (`tsc --noEmit`); `lint` and `build` already existed
- No CI/CD workflows, Docker production images, Kafka broker in the Maven suite, or H2

### Quality commands

| Purpose | Command |
|---------|---------|
| Backend tests | `mvn test` (from `backend/`) |
| Backend verify + coverage | `mvn clean verify` (from `backend/`; report `target/site/jacoco/index.html`) |
| Frontend lint | `npm run lint` (from `frontend/`) |
| Frontend type check | `npm run typecheck` (from `frontend/`) |
| Frontend production build | `npm run build` (from `frontend/`) |

### Automated Validation

Focused implementation checks (agent environment):

| Check | Result |
|-------|--------|
| `mvn test-compile` | SUCCESS |
| `mvn -Dtest=JwtPropertiesTest,MockPaymentProviderTest test` | 7 tests, 0 failures |
| `mvn -Dtest=FlywayMigrationQualityTest,NotificationSchemaRepositoryTest,ApplicationMetadataRepositoryTest,BookingRepositoryTest test` | 18 errors â€” Docker engine not available in agent shell (`Could not find a valid Docker environment`) |
| `npm run typecheck` (frontend) | SUCCESS |

Independent local validation completed: `FlywayMigrationQualityTest` — 3 tests, 0 failures, 0 errors; `mvn clean verify` — BUILD SUCCESS; JaCoCo HTML report generated successfully with 206 classes analyzed; frontend `lint` — SUCCESS with no warnings/errors; `typecheck` — SUCCESS; production `build` — SUCCESS. Initial Docker/Testcontainers validation failed because Testcontainers 1.20.6 attempted an unsupported Docker API against Docker Engine 29; upgrading to Testcontainers 2.0.5 resolved the compatibility issue.

### Known limitations

- Backend tests that use `@IsolatedPostgresIntegrationTest` require a working Docker engine for Testcontainers.
- `@WebMvcTest` slice tests and pure unit tests do not require Docker.
- Maven suite does not start a real Kafka broker.

### Final Review

- COMPLETED — backend regression, isolated PostgreSQL/Flyway validation, JaCoCo reporting, frontend lint/typecheck/build, and final implementation review passed.

---
## Module 16 - Docker Fundamentals & Production Images

**Status:** COMPLETED

### Implementation

- Backend production image uses a multi-stage Maven/Java 21 build so Maven and build tooling are not carried into the runtime image.
- Backend runtime uses a lightweight JRE Alpine image and executes as non-root user `app`.
- Frontend production image uses a multi-stage Node.js 22 Alpine build with Next.js standalone output.
- Frontend runtime executes as non-root user `nextjs` with UID 1001.
- Docker build contexts are constrained using `.dockerignore`.
- Runtime configuration and secrets remain externalized rather than embedded into container images.
- Docker Compose foundation defines PostgreSQL, Redis, Kafka, backend, and frontend services.
- PostgreSQL uses the named volume `temple-postgres-data` for persistent database storage.
- Backend resource controls include `cpus: 1.0` and `mem_limit: 768m`.
- Backend uses bounded Docker `json-file` logging with `max-size: 10m` and `max-file: 3`.
- Healthchecks are configured for infrastructure and application services.
- Required Compose secrets/configuration use required-variable interpolation instead of insecure runtime defaults.
- Backend and frontend containers remain stateless and replaceable.

### Runtime / Image Validation

| Check | Result |
|-------|--------|
| Backend image build | SUCCESS |
| Frontend image build | SUCCESS |
| Backend non-root runtime | CONFIRMED |
| Frontend non-root runtime | CONFIRMED |
| Backend HTTP service | SUCCESS |
| Frontend HTTP service | SUCCESS |
| PostgreSQL connectivity | SUCCESS |
| Redis connectivity | SUCCESS |
| Kafka connectivity | SUCCESS |
| Docker Compose stack startup | SUCCESS |
| Healthchecks | SUCCESS |
| PostgreSQL named-volume persistence | CONFIRMED |
| Backend CPU/memory controls | CONFIRMED |
| Backend bounded logging | CONFIRMED |

### Security / Reliability Review

- Build-time tooling is separated from runtime images through multi-stage builds.
- Application containers do not require root execution.
- Secrets remain externalized and `.env` is excluded from source control.
- PostgreSQL, Redis, and Kafka do not require host-port publication for application-to-service communication.
- PostgreSQL is persistent while backend/frontend remain replaceable.
- Redis is treated as cache infrastructure rather than transactional storage.
- Kafka is local single-node infrastructure at this stage and is not a production HA design.
- Additional container security hardening is intentionally deferred to Module 18.

### Final Review

- MUST FIX: NONE
- Production-oriented Docker image foundation: PASS
- Non-root execution: PASS
- Runtime configuration externalization: PASS
- Compose foundation: PASS
- Module 16 approved for completion

---

## Module 17 - Docker Compose & Local Production Stack

**Status:** COMPLETED

### Implementation / Architecture Review

- Existing Module 16 Compose foundation was retained after Module 17 production-gap analysis; no unnecessary rewrite was introduced.
- Multi-container stack consists of frontend, backend, PostgreSQL, Redis, and Kafka.
- Compose service DNS is used for container-to-container communication.
- Only frontend (`3000`) and backend (`8080`) are published to the host.
- PostgreSQL uses named volume `temple-postgres-data`.
- Redis remains an intentionally disposable cache.
- Kafka remains an intentionally disposable single-node KRaft broker for local development.
- Backend is constrained to `1.0` CPU and `768m` memory.
- Backend bounded `json-file` logging is retained.
- Health-based startup dependencies were verified.
- PostgreSQL is readiness-critical.
- Redis is intentionally excluded from readiness.
- Kafka remains asynchronous infrastructure and is not an Actuator readiness dependency.
- No confirmed Compose implementation defect remained after Module 17 testing.

### Runtime Validation

| Check | Result |
|-------|--------|
| `docker compose config --quiet` | SUCCESS - exit code 0 |
| Backend container | HEALTHY |
| Frontend container | HEALTHY |
| PostgreSQL container | HEALTHY |
| Redis container | HEALTHY |
| Kafka container | HEALTHY |
| Backend aggregate health | HTTP 200 |
| Backend readiness | HTTP 200 |
| Backend liveness | HTTP 200 |
| Frontend | HTTP 200 |
| PostgreSQL | accepting connections |
| Redis | PONG |
| Kafka topic `temple.domain.events` | CONFIRMED |
| Backend non-root execution | CONFIRMED |
| Frontend non-root execution | CONFIRMED |
| Backend CPU/memory cgroup limits | CONFIRMED |
| PostgreSQL volume persistence | CONFIRMED |

### Controlled Failure Testing

- Redis outage: backend remained running; aggregate health returned HTTP 503 while readiness remained HTTP 200 as designed.
- Redis recovery: container/DNS/TCP connectivity was verified; Spring/Lettuce recovered without backend restart.
- PostgreSQL outage: backend remained alive but readiness returned HTTP 503 as designed.
- PostgreSQL recovery: backend recovered after database restart.
- Kafka outage: backend remained alive; Kafka client connection/DNS failures were observed and the client recovered after broker restoration.
- Backend termination test produced exit code 137 with `OOMKilled=false`, confirming that exit 137 alone does not prove an OOM condition.
- `restart: unless-stopped` behavior was reviewed during explicit operator-directed container termination.
- Final full-stack health returned to green after failure testing.

### Production Gap Analysis

#### Confirmed Defects

- NONE.

#### Intentional Local Architecture

- PostgreSQL is the persistent transactional source of truth.
- Redis is a disposable cache.
- Kafka is local single-node disposable infrastructure.
- Backend/frontend are stateless and replaceable.
- PostgreSQL participates in readiness; Redis/Kafka do not based on current business semantics.

#### Hardening Opportunities

- Standardize bounded container logging across frontend, PostgreSQL, Redis, and Kafka.
- Continue graceful-shutdown validation.
- Add richer dependency and business telemetry in later observability modules.

#### Future Architecture

- Kubernetes orchestration and rolling deployments.
- Multi-node/high-availability architecture.
- Production Kafka persistence, replication, ISR, and disaster recovery.
- Vault/AWS Secrets Manager or equivalent production secret management.
- Prometheus/Grafana and centralized logging.
- OpenTelemetry tracing.
- Production load/capacity engineering.
- Backup and disaster recovery architecture.

### Problems Encountered / Learning

- Host `5432` and `6379` listeners were native Windows services rather than Docker-published PostgreSQL/Redis ports.
- Frontend healthcheck required explicit `127.0.0.1` rather than `localhost`.
- Redis temporarily remained DOWN in Spring health after infrastructure recovery but subsequently recovered through the client connection lifecycle.
- `docker compose ps -q backend` returned no ID after backend exit; `ps -a` or a previously captured container ID was required for post-exit inspection.
- Exit code 137 was correctly distinguished from OOM by checking `OOMKilled=false` and the known operator action.
- Docker resource configuration was verified through both Docker inspection and Linux cgroup values.
- Idle resource observations were not used to invent arbitrary production limits.

### Final Review

- MUST FIX: NONE
- Compose validation: PASS
- Full stack: HEALTHY
- Controlled dependency failure/recovery testing: PASS
- Persistence validation: PASS
- Networking/service-discovery validation: PASS
- Resource-limit validation: PASS
- Security/non-root validation: PASS
- Module 17 approved for completion

---
## Next Module

**Module 20 - Kubernetes Networking & Ingress**

Status: COMPLETED

Independent implementation validation completed. Do not start Module 21 until Module 20 implementation Git/PR closeout and learning-repository documentation are complete.

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
- [x] Module 14 - Notifications & Kafka
- [x] Module 15 - Testing & Quality Engineering

## Phase 2 - Containers

- [x] Module 16 - Docker Fundamentals & Production Images
- [x] Module 17 - Docker Compose & Local Production Stack
- [x] Module 18 - Container Security & Optimization

## Phase 3 - Kubernetes

- [x] Module 19 - Kubernetes Fundamentals
- [x] Module 20 - Kubernetes Networking & Ingress
- [x] Module 21 - Kubernetes Configuration & Security
- [x] Module 22 - Kubernetes Storage & Stateful Workloads
- [x] Module 23 - Helm
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
