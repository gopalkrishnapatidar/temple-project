# Initial Architecture

## Shape

Start as a **modular monolith**.

```text
User
  → Frontend (Next.js)
    → Backend (Spring Boot, in-process domain modules)
      → PostgreSQL (transactional source of truth)
```

Frontend and backend are separate applications later, but they are still one product: a single backend process with internal module boundaries, not a mesh of microservices.

## Why not microservices first

- Booking and payment need strong transactional consistency. One database and in-process calls are simpler and safer to get right.
- Independent deployability is not a current requirement.
- Microservices add network failure, distributed transactions, ops cost, and observability burden before the domain is proven.
- The learning roadmap already introduces distribution later (Redis, Kafka, Kubernetes, AWS) when there is a justified reason.

## Internal backend modules

Logical packages (not separate services): identity, user, temple, event, darshan, booking, puja, havan, payment, donation, prasadam, notification, administration, audit.

Rules:

- Domains communicate through explicit APIs/services, not by reaching into each other's tables ad hoc.
- PostgreSQL is authoritative for transactional data.
- Redis must not become the source of truth for booking capacity (when introduced later).
- Kafka must not become the source of truth for transactional booking state (when introduced later).

## Request path (initial)

1. User uses the frontend.
2. Frontend calls backend HTTP APIs.
3. Backend authenticates/authorizes the request (later module).
4. Domain services apply business rules.
5. PostgreSQL commits or rolls back the unit of work.
6. Audit records significant actions.
7. Notifications may be triggered after state is committed; they must not replace the database as truth.

## Reliability requirements (design only)

These are future implementation requirements, not Module 00 work:

- **Concurrency:** Slot capacity must remain correct under concurrent booking attempts.
- **Idempotency:** Booking and payment APIs must be safe to retry.
- **Retries / timeouts:** Outbound calls (payment mock, later notifications) need bounded retries and timeouts.
- **Health checks:** Liveness/readiness for later container and Kubernetes modules.
- **Graceful shutdown:** In-flight requests should complete or fail cleanly on process stop.
- **Scalability / HA:** Multiple app instances later; database remains the consistency boundary for bookings.
- **Backup / DR:** PostgreSQL backup, RPO/RTO, and restore drills in later modules.

### Booking and payment consistency (architecture constraints only)

- PostgreSQL is the transactional source of truth.
- Booking capacity must be concurrency-safe.
- Booking operations must be idempotent.
- Payment operations must be idempotent.
- Failed or abandoned payments must not permanently consume capacity.
- The exact reservation, hold, or confirmation strategy is **not** decided here. It will be designed in the booking and payment modules.

Do not assume capacity is permanently decremented before payment or only after payment.

Capacity example (requirement, not implementation): Darshan slot capacity = 100. Even if 500 users attempt booking at once, no more than 100 successful bookings may exist.

## Security requirements (design only)

- Authentication and authorization for users and admins.
- RBAC for administration vs devotee actions.
- Secrets in environment or secret stores, never in source.
- TLS in later hosted environments.
- Encryption of sensitive data at rest/in transit as modules require.
- Audit logging of privileged and booking/payment-significant actions.
- Mock payments first; no real payment credentials in source or logs.
- Least privilege for app, database, and later cloud/IAM roles.

## Cost

Module 00 adds no cloud cost. Paid AWS resources require explicit justification in later modules. Prefer local development until then.
