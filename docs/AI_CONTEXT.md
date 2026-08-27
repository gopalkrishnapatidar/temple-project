# Temple Digital Services Platform

## Permanent AI Project Context

This document is the permanent source of truth for AI-assisted development
of the Temple Digital Services Platform.

Cursor must read this document before working on any project module.

---

# 1. Project Goal

Build a realistic, production-oriented Temple Digital Services Platform.

This repository (`temple-project`) is the **implementation repository** for:

- application code
- configuration
- tests
- build and run
- debugging
- architecture decisions
- concise operational and project documentation

Detailed learning notes, concept explanations, interview preparation, and
scenario-based educational material are maintained separately in the
`temple-project-learning` repository. Do not generate that material here
unless explicitly requested.

The goal is NOT to let AI generate the entire project automatically.
Implementation must be production-oriented, testable, and maintainable.

---

# 2. Business Capabilities

The platform will eventually support:

- User registration and login
- User profiles
- Temple information
- Temple timings
- Festivals and events
- Darshan types
- Darshan schedules
- Darshan slot management
- Darshan booking
- Darshan cancellation
- Real-time Darshan availability
- Puja services
- Puja booking
- Havan services
- Havan booking
- Donations
- Payment processing abstraction
- Prasadam services
- Notifications
- Administration
- Reports
- Audit logging

Do not implement all capabilities at once.
They must be introduced according to the module roadmap.

---

# 3a. Application Stack (Current)

## Backend

- `backend/` — Spring Boot 3.4 (Java 21, Maven)
- REST API base path: `/api/v1`
- Local default: `http://localhost:8080`
- PostgreSQL via Flyway migrations (backend only; frontend never connects to DB)

## Frontend

- `frontend/` — Next.js 15 (React 19, TypeScript, App Router)
- Local default: `http://localhost:3000`
- Backend API URL via `NEXT_PUBLIC_API_BASE_URL` (browser-visible; no secrets)
- Server Components by default; Client Components only when interactivity requires it
- Backend integration uses server-side fetch for foundation endpoints (no CORS required for current ping check)

---

# 3. Technology Scope

The 44-module roadmap progressively introduces:

## Development

Java, Spring Boot, Maven, REST APIs, Next.js, React, TypeScript, PostgreSQL,
Flyway, Redis, Kafka

## Source Control

Git, GitHub, branching, pull requests, versioning

## Linux and Networking

Linux administration, processes, services, permissions, TCP/IP, DNS,
HTTP/HTTPS, ports, troubleshooting

## Containers

Docker, Dockerfile, multi-stage builds, Docker networking, Docker volumes,
Docker Compose, image optimization, container security

## Kubernetes

Pods, ReplicaSets, Deployments, Services, Namespaces, ConfigMaps, Secrets,
Ingress, Storage, StatefulSets, RBAC, ServiceAccounts, NetworkPolicy,
SecurityContext, probes, resource requests and limits, HPA, PDB, affinity,
topology spread

## Packaging

Helm

## Load Balancing

Layer 4, Layer 7, Kubernetes Services, Ingress, AWS ALB, health checks,
traffic distribution, connection draining

## TLS and Certificates

Public/private keys, CSR, Certificate Authorities, certificate chains,
TLS handshake, self-signed certificates, Kubernetes TLS Secrets,
cert-manager, Let's Encrypt, AWS ACM, certificate renewal, certificate rotation

## CI/CD

GitHub Actions, continuous integration, continuous delivery, build pipelines,
testing, container registry, deployment, rollback

## DevSecOps

Gitleaks, Semgrep, Trivy, dependency scanning, Checkov, container scanning,
IaC scanning, secret scanning

## AWS

IAM, VPC, CIDR, public/private subnets, route tables, Internet Gateway, NAT,
Security Groups, NACL, ECR, EKS, ALB, Route 53, ACM, RDS, Redis/ElastiCache,
Secrets Manager, CloudWatch

## Infrastructure as Code

Terraform, providers, resources, variables, outputs, modules, state,
remote state, locking, drift, import

## GitOps

Argo CD, desired state, synchronization, drift detection, rollback

## Observability

Prometheus, Grafana, Loki, OpenTelemetry, metrics, logs, traces, dashboards,
alerting

## SRE

SLI, SLO, SLA, error budgets, incident management, RCA, runbooks,
reliability engineering

## Performance

k6, load testing, stress testing, spike testing, RPS, p50, p95, p99,
bottleneck analysis

## Resilience

High availability, scalability, backup, restore, RPO, RTO, disaster recovery,
chaos engineering, failure testing

## FinOps

AWS cost optimization, Kubernetes cost optimization, resource rightsizing,
autoscaling, storage optimization, logging cost, data-transfer cost

---

# 4. Module Isolation Rule

The project must be implemented MODULE BY MODULE.

Only implement the module explicitly requested.
NEVER automatically implement future modules.

For example, if the current module is Spring Boot, do NOT automatically create
Kubernetes manifests, Terraform, AWS infrastructure, Argo CD, Kafka, or Prometheus
unless the current module specifically requires them.

When the current module is complete: STOP. Wait for the next module instruction.

---

# 5. Architecture Strategy

Start with a modular monolith.
Do NOT start with microservices.

Initial logical domains may include:

- identity
- user
- temple
- event
- darshan
- booking
- puja
- havan
- payment
- donation
- prasadam
- notification
- administration
- audit

Only introduce distributed architecture when there is a justified engineering reason.

When introducing a major technology, document the engineering decision:
what problem it solves, why it was selected, what complexity it introduces,
what can fail, how it will be monitored, and what it costs.

---

# 6. Cursor Token and Cost Optimization

Cursor usage must be optimized.

Before modifying anything:

1. Read this file.
2. Read `/docs/MODULE_STATUS.md`.
3. Inspect only files relevant to the current task.
4. Produce a concise implementation plan.

Do NOT:

- repeatedly scan the entire repository
- regenerate working files
- rewrite unchanged code
- perform unnecessary refactoring
- generate duplicate documentation
- implement future functionality
- generate long tutorials or learning notes
- generate interview preparation material
- add unnecessary dependencies

Prefer:

- small targeted changes
- existing components
- concise explanations
- reusable implementations
- minimum necessary context

Use the least expensive suitable model for routine work.
Use stronger reasoning models only for complex architecture, security,
concurrency, infrastructure, or troubleshooting.

---

# 7. Production Engineering Rule

Use production-oriented engineering practices where appropriate.

Consider:

- validation
- error handling
- transactions
- idempotency
- retries
- timeouts
- health checks
- graceful shutdown
- logging
- monitoring
- alerting
- scalability
- high availability
- security
- resource limits
- rollback
- backup
- disaster recovery

Do not add unnecessary complexity just to make the architecture look more advanced.

---

# 8. Security Rules

Security is mandatory.

NEVER:

- hardcode passwords
- hardcode AWS credentials
- hardcode API keys
- commit tokens
- commit private keys
- expose secrets
- log sensitive credentials
- store real payment credentials in source code

Use appropriate:

- environment variables
- secret stores
- Kubernetes Secrets
- AWS Secrets Manager
- IAM
- RBAC
- encryption
- TLS
- least privilege

Containers should run as non-root where practical.

Kubernetes workloads should eventually use appropriate:

- securityContext
- RBAC
- ServiceAccounts
- NetworkPolicy
- resource limits

---

# 9. Cloud Safety and Cost Rules

Cloud cost optimization is mandatory.

Preferred progression:

Local Development → Docker → Docker Compose → Local Kubernetes → CI/CD →
Temporary AWS Infrastructure → Production Simulation

Use local infrastructure whenever possible.

Before introducing a paid AWS resource, document why it is required, what
service will be used, major cost drivers, lower-cost alternatives, how to
shut it down, and how to verify removal.

NEVER automatically run:

- terraform apply
- terraform destroy
- aws resource deletion commands
- production deployments

or other destructive infrastructure commands without explicit approval.

Do not keep expensive AWS infrastructure running unnecessarily.

---

# 10. Database Rules

PostgreSQL is the authoritative source of truth for transactional data.

Use Flyway for database migrations.

Use appropriate:

- primary keys
- foreign keys
- constraints
- indexes
- transactions

Redis must NOT become the authoritative source for booking capacity.
Kafka must NOT become the authoritative source for transactional booking state.

---

# 11. Booking Reliability

Booking is a critical business workflow.

The implementation must eventually address:

- concurrency
- race conditions
- transactions
- locking
- idempotency
- duplicate requests
- capacity
- cancellation
- retry behavior
- state transitions

Example: if a Darshan slot capacity is 100, a maximum of 100 bookings may
succeed. Booking 101 must fail correctly even under concurrent traffic.

Concurrency tests must prove this behavior.

---

# 12. Payment Safety

Initially use a MOCK payment provider.
Do not integrate real payment providers until explicitly requested.

Payment design must eventually handle:

- idempotency
- state transitions
- duplicate webhooks
- failures
- retries
- refunds

Never log sensitive payment information.

---

# 13. Testing Rule

Important functionality must be tested.

Testing may include:

- unit tests
- integration tests
- API tests
- database tests
- security tests
- concurrency tests
- container tests
- Kubernetes tests
- infrastructure validation
- load testing

Tests must validate real behavior.
Do not create meaningless tests only to increase coverage.

Before completing a module: BUILD → TEST → VERIFY → UPDATE PROJECT CONTEXT

---

# 14. Troubleshooting Rule

When something fails, follow:

ERROR → UNDERSTAND → INVESTIGATE → ROOT CAUSE → FIX → RETEST

Do NOT:

- apply random fixes
- randomly change configuration
- reinstall tools without evidence
- suppress errors just to make tests pass
- disable important tests
- hide failures
- perform destructive actions without approval

Troubleshooting is an engineering responsibility. Do not generate detailed
troubleshooting learning material in this repository.

---

# 15. Kubernetes Rules

Introduce Kubernetes only in its planned modules.

Eventually use appropriate:

- Deployments
- Services
- ConfigMaps
- Secrets
- Ingress
- probes
- requests and limits
- RBAC
- NetworkPolicy
- SecurityContext
- HPA
- PDB
- scheduling controls

---

# 16. TLS and Certificate Rules

Certificate management progression:

Self-Signed Certificate → Local HTTPS → Kubernetes TLS Secret → HTTPS Ingress →
cert-manager → Let's Encrypt → AWS ACM → AWS ALB

Never commit private keys.

---

# 17. Scalability and Availability

The final platform must demonstrate:

- horizontal scaling
- vertical scaling concepts
- Kubernetes HPA
- node scaling concepts
- load balancing
- multiple replicas
- pod distribution
- PDB
- readiness/liveness
- rolling deployment
- failover
- Multi-AZ architecture
- graceful shutdown

Scaling must be demonstrated using measurable load.

---

# 18. CI/CD Rules

CI/CD should evolve progressively.

Initial CI:

Code → Build → Test → Security Scan → Docker Build → Image Scan

Later:

Container Registry → GitOps → Argo CD → Kubernetes

Use immutable artifact/image versions.
Do not use `latest` for production deployment.

---

# 19. Observability Rules

Eventually implement: METRICS + LOGS + TRACES

Monitor infrastructure, Kubernetes, application, and business signals.

Important examples:

- request rate
- error rate
- latency
- CPU
- memory
- pod restarts
- database health
- Redis health
- Kafka lag
- booking success rate
- payment success rate

Observability must be used for actual troubleshooting.

---

# 20. SRE Rules

Eventually define:

- SLIs
- SLOs
- SLAs
- error budgets
- alerts
- runbooks
- incident response
- RCA

Reliability decisions should be based on measurable signals.

---

# 21. Documentation Rules

Important engineering decisions must be documented in this repository.

Maintain at minimum:

- `/docs/AI_CONTEXT.md`
- `/docs/MODULE_STATUS.md`

Update `AI_CONTEXT.md` only when architecture, conventions, important
implementation details, or project context changed.

Keep documentation concise and implementation-specific.
Do NOT write detailed educational explanations here.

Additional documentation should be created only when needed.
Use ADRs for important architecture decisions.
Avoid duplicate documentation.

---

# 22. Module Workflow

For every module:

READ CONTEXT → PLAN → IMPLEMENT → TEST → TROUBLESHOOT → FIX → VERIFY →
UPDATE PROJECT CONTEXT → COMMIT → STOP

Never automatically proceed to the next module.

---

# 23. Before Implementation

Before making significant changes:

1. Read `/docs/AI_CONTEXT.md`.
2. Read `/docs/MODULE_STATUS.md`.
3. Inspect relevant existing files.
4. Provide a concise implementation plan.
5. List files expected to change.

For large changes, wait for approval before implementation.

Do not provide a long conceptual lesson before implementation.

---

# 24. After Implementation

After completing a module, provide only:

1. Files Created/Modified
2. Implementation Summary
3. Commands Executed
4. Test Results
5. Problems Encountered
6. Manual Verification
7. Module Status (COMPLETE, TESTING, or BLOCKED)
8. Suggested Git Commit Message

Then update `/docs/MODULE_STATUS.md`.
Do NOT automatically start the next module.

---

# 25. Module Completion Criteria

A module is complete only when:

- implementation works
- required tests pass
- important functionality is verified
- documentation is updated
- security has been reviewed where applicable
- scalability/availability has been considered where applicable
- cost has been considered where applicable
- `MODULE_STATUS.md` is updated
- Git commit is ready

Generated code alone does NOT mean the module is complete.

---

# 26. Final Target Architecture

The architecture will evolve toward approximately:

Users → Route 53 → HTTPS / TLS → AWS ACM → AWS ALB → Kubernetes / EKS →
Ingress / Services → Frontend + Backend Pods → PostgreSQL + Redis + Kafka

Delivery:

Developer → GitHub → GitHub Actions → Build + Test + Security →
Container Registry → Argo CD → EKS

Infrastructure:

Terraform → AWS

Observability:

Application + Infrastructure → OpenTelemetry / Prometheus / Loki → Grafana

This is the TARGET architecture.
Do NOT implement it all at the beginning.

---

# 27. Final AI Instruction

Act as a senior implementation assistant for this repository.

Focus on:

- production engineering
- security
- scalability
- high availability
- troubleshooting
- cost optimization

Do not attempt to finish the project as quickly as possible.
Do not generate detailed learning material, tutorials, or interview content
unless explicitly requested.

Always work on only the currently approved module.
