# Business Domains

Logical domains inside the initial modular monolith. They are not separate services yet.

## Identity

- **Responsibility:** Registration, login, sessions/tokens, password handling.
- **Entities:** Credentials, session or token records, auth events.
- **Operations:** Register, login, logout, credential change, session invalidation.
- **Depends on:** User (profile creation).
- **Production concerns:** Credential hashing, brute-force protection, secret storage, TLS in later environments.

## User

- **Responsibility:** Devotee and staff profiles.
- **Entities:** User profile, contact details, preferences.
- **Operations:** Create/update profile, deactivate account.
- **Depends on:** Identity.
- **Production concerns:** PII handling, least-privilege access to personal data.

## Temple

- **Responsibility:** Temple identity, location, contact, timings, rules.
- **Entities:** Temple, timing window, contact, status.
- **Operations:** CRUD temple records, publish timings.
- **Depends on:** Administration for privileged writes.
- **Production concerns:** Public read vs admin write; consistent timezone handling.

## Event

- **Responsibility:** Festivals and special events.
- **Entities:** Event, date range, linked temple, published status.
- **Operations:** Schedule, update, publish, cancel events.
- **Depends on:** Temple.
- **Production concerns:** Overlapping events; schedules that affect Darshan/Puja/Havan availability.

## Darshan

- **Responsibility:** Darshan types, schedules, and slot capacity.
- **Entities:** Darshan type, schedule, slot, capacity.
- **Operations:** Define types, generate/update slots, set capacity, query availability.
- **Depends on:** Temple, Event (when events change schedules).
- **Production concerns:** Capacity is a critical invariant. PostgreSQL is the transactional source of truth. Slot capacity must remain concurrency-safe. Exact reservation/hold/confirmation design is deferred to booking/payment modules.

## Booking

- **Responsibility:** Create, confirm, cancel, and query bookings for Darshan, Havan, and Puja.
- **Entities:** Booking, booking type, status, correlation/idempotency key, linked slot or service.
- **Operations:** Request booking, cancel, list by user/admin, status transitions.
- **Depends on:** User, Darshan, Puja, Havan, Payment.
- **Production concerns:**
  - Booking operations must be idempotent.
  - Capacity must be concurrency-safe.
  - Failed or abandoned payments must not permanently consume capacity.
  - Do not decide here whether capacity is reserved before payment, after payment, or via a hold/confirm pattern. That strategy is designed in later booking/payment modules.
  - Example requirement (not implemented here): if slot capacity is 100, at most 100 successful bookings may exist even under 500 concurrent attempts.

## Puja

- **Responsibility:** Puja catalog, rules, and availability.
- **Entities:** Puja type, offering details, schedule/availability, pricing reference.
- **Operations:** Catalog CRUD, availability queries, booking hand-off.
- **Depends on:** Temple, Event, Booking, Payment.
- **Production concerns:** Same booking/payment invariants as Darshan bookings.

## Havan

- **Responsibility:** Havan catalog, rules, and availability.
- **Entities:** Havan type, schedule/availability, capacity or participation rules, pricing reference.
- **Operations:** Catalog CRUD, availability queries, booking hand-off.
- **Depends on:** Temple, Event, Booking, Payment.
- **Production concerns:** Same booking/payment invariants as Darshan bookings.

## Donation

- **Responsibility:** Donation intents and receipts.
- **Entities:** Donation, amount, purpose, status, receipt reference.
- **Operations:** Initiate donation, confirm, fail, receipt lookup.
- **Depends on:** User (optional for guest rules later), Payment, Audit.
- **Production concerns:** Payment idempotency; failed payments must not look successful.

## Payment

- **Responsibility:** Payment abstraction over a mock provider first. Real providers only when explicitly requested.
- **Entities:** Payment intent, status, provider reference, idempotency key, linked booking or donation.
- **Operations:** Initiate, confirm, fail, refund (later), reconcile.
- **Depends on:** Booking, Donation.
- **Production concerns:**
  - Payment operations must be idempotent.
  - Never log sensitive payment data.
  - Failed/abandoned payments must not permanently consume booking capacity.
  - Exact consistency algorithm with booking is deferred to booking/payment modules.

## Prasadam

- **Responsibility:** Prasadam offerings tied to bookings or donations as rules require.
- **Entities:** Prasadam item, eligibility, fulfillment status.
- **Operations:** Configure offerings, record fulfillment.
- **Depends on:** Temple, Booking, Donation.
- **Production concerns:** Fulfillment must not invent bookings that did not succeed.

## Notification

- **Responsibility:** User-facing messages for booking, payment, and admin events.
- **Entities:** Notification, channel, template, delivery status.
- **Operations:** Enqueue/send, retry, mark delivered/failed.
- **Depends on:** User, Booking, Payment, Event.
- **Production concerns:** Notifications are not the source of truth. Delivery can fail and retry. Kafka is a later option, not required initially.

## Administration

- **Responsibility:** Privileged operations: temple config, schedules, capacity, user roles, operational reports access.
- **Entities:** Admin actions, configuration records, role assignments.
- **Operations:** Manage catalogs, slots, users/roles, operational settings.
- **Depends on:** All domain write paths that are admin-controlled.
- **Production concerns:** RBAC, least privilege, audit every privileged change.

## Reports

- **Responsibility:** Operational and business reporting.
- **Entities:** Report query definitions, generated snapshots (later).
- **Operations:** Booking counts, payment outcomes, donation totals, capacity utilization.
- **Depends on:** Booking, Payment, Donation, Temple, Audit (read-only).
- **Production concerns:** Reports must not lock hot booking tables unnecessarily.

## Audit

- **Responsibility:** Tamper-evident record of security and business-significant actions.
- **Entities:** Actor, action, resource, timestamp, outcome.
- **Operations:** Append audit events, query for investigation.
- **Depends on:** Identity and all write-heavy domains.
- **Production concerns:** Append-oriented logging; do not store secrets or payment credentials in audit payloads.
