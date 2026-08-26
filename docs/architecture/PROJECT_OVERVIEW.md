# Project Overview

Temple Digital Services Platform is a production-oriented temple operations and devotee services system. It is also a staged DevOps/SRE learning project.

## Dual goals

1. Build a working platform for temple information, bookings, services, donations, and administration.
2. Learn production engineering through progressive modules, not by generating the full stack at once.

## What the platform will eventually do

Devotees can register, view temple information and events, book Darshan/Havan/Puja, donate, and receive notifications. Administrators can manage temples, schedules, capacity, reports, and audit trails.

Capability list and technology scope live in `/docs/AI_CONTEXT.md`. This document does not repeat them.

## Initial technical shape

- Architecture: modular monolith (one backend, one frontend, one transactional database)
- Backend: Java / Spring Boot (later modules)
- Frontend: Next.js / React / TypeScript (later modules)
- Data: PostgreSQL as the authoritative transactional store; Flyway for migrations
- Delivery: local-first, then Docker, Kubernetes, CI/CD, AWS as later modules require

## Non-goals for Module 00

Do not implement application code, Docker, Kubernetes, Helm, Terraform, AWS, CI/CD, Redis, Kafka, databases, or monitoring.

## Guiding constraints

- Implement one approved module at a time.
- Prefer local infrastructure until a paid cloud resource is justified.
- Never hard-code secrets.
- Booking capacity and payments must be correct under failure and concurrency; exact algorithms belong in later modules.
