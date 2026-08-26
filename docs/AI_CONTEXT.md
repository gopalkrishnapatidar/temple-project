# Temple Digital Services Platform

## Permanent AI Project Context

This document is the permanent source of truth for AI-assisted development

of the Temple Digital Services Platform.

Cursor must read this document before working on any project module.

---

# 1. Project Goal

Build a realistic, production-oriented Temple Digital Services Platform

while using the project as a complete hands-on DevOps/SRE learning project.

This project has TWO equally important goals:

1. Build a real working Temple Digital Services Platform.

2. Gain production-level DevOps/SRE hands-on experience for interviews.

The goal is NOT to let AI generate the entire project automatically.

The required learning workflow is:

LEARN

→ DESIGN

→ IMPLEMENT

→ TEST

→ BREAK

→ TROUBLESHOOT

→ FIX

→ DOCUMENT

→ EXPLAIN

I must understand every important implementation well enough to explain,

operate and troubleshoot it myself.

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

# 3. DevOps/SRE Learning Scope

The complete project will progressively provide hands-on experience with:

## Development

- Java

- Spring Boot

- Maven

- REST APIs

- Next.js

- React

- TypeScript

- PostgreSQL

- Flyway

- Redis

- Kafka

## Source Control

- Git

- GitHub

- Branching

- Pull requests

- Versioning

## Linux and Networking

- Linux administration

- Processes

- Services

- Permissions

- TCP/IP

- DNS

- HTTP/HTTPS

- Ports

- Troubleshooting

## Containers

- Docker

- Dockerfile

- Multi-stage builds

- Docker networking

- Docker volumes

- Docker Compose

- Image optimization

- Container security

## Kubernetes

- Pods

- ReplicaSets

- Deployments

- Services

- Namespaces

- ConfigMaps

- Secrets

- Ingress

- Storage

- StatefulSets

- RBAC

- ServiceAccounts

- NetworkPolicy

- SecurityContext

- Probes

- Resource requests and limits

- HPA

- PDB

- Affinity

- Topology spread

## Packaging

- Helm

## Load Balancing

- Layer 4

- Layer 7

- Kubernetes Services

- Ingress

- AWS ALB

- Health checks

- Traffic distribution

- Connection draining

## TLS and Certificates

- Public/private keys

- CSR

- Certificate Authorities

- Certificate chains

- TLS handshake

- Self-signed certificates

- Kubernetes TLS Secrets

- cert-manager

- Let's Encrypt

- AWS ACM

- Certificate renewal

- Certificate rotation

## CI/CD

- GitHub Actions

- Continuous Integration

- Continuous Delivery

- Build pipelines

- Testing

- Container registry

- Deployment

- Rollback

## DevSecOps

- Gitleaks

- Semgrep

- Trivy

- Dependency scanning

- Checkov

- Container scanning

- IaC scanning

- Secret scanning

## AWS

- IAM

- VPC

- CIDR

- Public/private subnets

- Route tables

- Internet Gateway

- NAT

- Security Groups

- NACL

- ECR

- EKS

- ALB

- Route 53

- ACM

- RDS

- Redis/ElastiCache

- Secrets Manager

- CloudWatch

## Infrastructure as Code

- Terraform

- Providers

- Resources

- Variables

- Outputs

- Modules

- State

- Remote state

- Locking

- Drift

- Import

## GitOps

- Argo CD

- Desired state

- Synchronization

- Drift detection

- Rollback

## Observability

- Prometheus

- Grafana

- Loki

- OpenTelemetry

- Metrics

- Logs

- Traces

- Dashboards

- Alerting

## SRE

- SLI

- SLO

- SLA

- Error budgets

- Incident management

- RCA

- Runbooks

- Reliability engineering

## Performance

- k6

- Load testing

- Stress testing

- Spike testing

- RPS

- p50

- p95

- p99

- Bottleneck analysis

## Resilience

- High availability

- Scalability

- Backup

- Restore

- RPO

- RTO

- Disaster recovery

- Chaos engineering

- Failure testing

## FinOps

- AWS cost optimization

- Kubernetes cost optimization

- Resource rightsizing

- Autoscaling

- Storage optimization

- Logging cost

- Data-transfer cost

---

# 4. Module Isolation Rule

The project must be implemented MODULE BY MODULE.

Only implement the module explicitly requested.

NEVER automatically implement future modules.

For example:

If the current module is Spring Boot:

Do NOT automatically create:

- Kubernetes manifests

- Terraform

- AWS infrastructure

- Argo CD

- Kafka

- Prometheus

unless the current module specifically requires them.

When the current module is complete:

STOP.

Wait for the next module instruction.

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

Only introduce distributed architecture when there is a justified

engineering reason.

Whenever introducing a major technology explain:

1. What problem does it solve?

2. Why do we need it?

3. What alternatives exist?

4. Why was this solution selected?

5. What complexity does it introduce?

6. What can fail?

7. How will it be monitored?

8. What does it cost?

---

# 6. Cursor Token and Cost Optimization

Cursor usage must be optimized.

Before modifying anything:

1. Read this file.

2. Read /docs/MODULE_[STATUS.md](http://STATUS.md).

3. Inspect only files relevant to the current task.

4. Produce a concise implementation plan.

Do NOT:

- repeatedly scan the entire repository

- regenerate working files

- rewrite unchanged code

- perform unnecessary refactoring

- generate duplicate documentation

- implement future functionality

- repeatedly explain information already documented

- add unnecessary dependencies

Prefer:

- small targeted changes

- existing components

- concise explanations

- reusable implementations

- minimum necessary context

Use the least expensive suitable model for routine work.

Use stronger reasoning models only for complex architecture,

security, concurrency, infrastructure or troubleshooting.

---

# 7. Learning-First Rule

AI must act as a technical mentor, not only a coding agent.

For every important module I should understand:

1. What problem are we solving?

2. Why is this technology needed?

3. How does it work internally?

4. How do components communicate?

5. How do we configure it?

6. How do we test it?

7. What happens when it fails?

8. How do we troubleshoot it?

9. How do we secure it?

10. How does it scale?

11. How does it affect availability?

12. What does it cost?

13. What are the production trade-offs?

14. How would I explain it in an interview?

Do not hide important implementation details behind AI-generated code.

---

# 8. Production Engineering Rule

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

Do not add unnecessary complexity just to make the architecture

look more advanced.

---

# 9. Security Rules

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

# 10. Cloud Safety and Cost Rules

Cloud cost optimization is mandatory.

Preferred progression:

Local Development

→ Docker

→ Docker Compose

→ Local Kubernetes

→ CI/CD

→ Temporary AWS Infrastructure

→ Production Simulation

Use local infrastructure whenever possible.

Before introducing a paid AWS resource explain:

1. Why it is required.

2. What AWS service will be used.

3. Major cost drivers.

4. Lower-cost alternatives.

5. How to shut it down.

6. How to verify that it was removed.

NEVER automatically run:

terraform apply

terraform destroy

aws resource deletion commands

production deployments

or other destructive infrastructure commands without explicit approval.

Do not keep expensive AWS infrastructure running unnecessarily.

---

# 11. Database Rules

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

# 12. Booking Reliability

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

Example:

If a Darshan slot capacity is 100,

a maximum of 100 bookings may succeed.

Booking 101 must fail correctly even under concurrent traffic.

Concurrency tests must prove this behavior.

---

# 13. Payment Safety

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

# 14. Testing Rule

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

Before completing a module:

BUILD

→ TEST

→ VERIFY

→ DOCUMENT

---

# 15. Failure-First Learning

Successful deployment alone is not sufficient.

When appropriate, create SAFE failure scenarios.

Examples:

- application crash

- container crash

- pod crash

- Redis unavailable

- database unavailable

- Kafka consumer failure

- high CPU

- high memory

- high latency

- bad deployment

- DNS problem

- certificate problem

- network problem

For each failure follow:

HYPOTHESIS

→ REPRODUCE

→ OBSERVE

→ INVESTIGATE

→ ROOT CAUSE

→ FIX

→ VERIFY

→ PREVENT

Never run destructive experiments against production.

---

# 16. Kubernetes Rules

Introduce Kubernetes only in its planned modules.

Eventually use appropriate:

- Deployments

- Services

- ConfigMaps

- Secrets

- Ingress

- probes

- requests

- limits

- RBAC

- NetworkPolicy

- SecurityContext

- HPA

- PDB

- scheduling controls

Explain Kubernetes YAML instead of blindly generating it.

---

# 17. TLS and Certificate Rules

Certificate learning must include:

- TLS handshake

- public/private key

- CSR

- CA

- root certificate

- intermediate certificate

- certificate chain

- certificate expiration

- certificate renewal

- certificate rotation

Progression should be:

Self-Signed Certificate

→ Local HTTPS

→ Kubernetes TLS Secret

→ HTTPS Ingress

→ cert-manager

→ Let's Encrypt

→ AWS ACM

→ AWS ALB

Never commit private keys.

---

# 18. Scalability and Availability

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

# 19. CI/CD Rules

CI/CD should evolve progressively.

Initial CI:

Code

→ Build

→ Test

→ Security Scan

→ Docker Build

→ Image Scan

Later:

Container Registry

→ GitOps

→ Argo CD

→ Kubernetes

Use immutable artifact/image versions.

Do not use `latest` for production deployment.

---

# 20. Observability Rules

Eventually implement:

METRICS

+

LOGS

+

TRACES

Monitor infrastructure, Kubernetes, application and business signals.

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

# 21. SRE Rules

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

# 22. Documentation Rules

Important engineering decisions must be documented.

Maintain at minimum:

- /docs/AI_[CONTEXT.md](http://CONTEXT.md)

- /docs/MODULE_[STATUS.md](http://STATUS.md)

- /docs/LEARNING_[PROMPT.md](http://PROMPT.md)

Additional documentation should be created only when needed.

Use ADRs for important architecture decisions.

Avoid duplicate documentation.

---

# 23. Error Handling Rule

If something fails:

DO NOT apply random fixes.

Follow:

ERROR

→ UNDERSTAND

→ INVESTIGATE

→ ROOT CAUSE

→ FIX

→ RETEST

Explain important failures because troubleshooting is part of the

learning objective.

---

# 24. Module Workflow

For EVERY module:

READ CONTEXT

↓

UNDERSTAND

↓

PLAN

↓

IMPLEMENT

↓

TEST

↓

VERIFY

↓

BREAK SAFELY

↓

TROUBLESHOOT

↓

DOCUMENT

↓

INTERVIEW REVIEW

↓

UPDATE MODULE STATUS

↓

GIT COMMIT

↓

STOP

Never automatically proceed to the next module.

---

# 25. Before Implementation

Before making significant changes:

1. Read /docs/AI_[CONTEXT.md](http://CONTEXT.md).

2. Read /docs/MODULE_[STATUS.md](http://STATUS.md).

3. Inspect relevant existing files.

4. Explain the module objective.

5. Explain the architecture.

6. Explain what will change.

7. List files expected to change.

8. Explain important risks.

9. Explain testing strategy.

10. Provide a concise implementation plan.

For large changes, wait for approval before implementation.

---

# 26. After Implementation

After completing a module provide:

## Summary

## Architecture

## Files Changed

## Important Commands

## Tests Performed

## Validation

## Security Considerations

## Scalability / Availability

## Cost Considerations

## Failure Scenarios

## Troubleshooting

## What I Should Learn

## Interview Questions

## Suggested Git Commit

## Next Module

Then update /docs/MODULE_[STATUS.md](http://STATUS.md).

Do NOT automatically start the next module.

---

# 27. Module Completion Criteria

A module is complete only when:

- implementation works

- required tests pass

- important functionality is verified

- documentation is updated

- security has been reviewed

- scalability/availability has been considered where applicable

- cost has been considered where applicable

- important failure scenarios are understood

- troubleshooting steps are understood

- interview concepts are understood

- MODULE_[STATUS.md](http://STATUS.md) is updated

- Git commit is ready

Generated code alone does NOT mean the module is complete.

---

# 28. Final Target Architecture

The architecture will evolve toward approximately:

Users

  ↓

Route 53

  ↓

HTTPS / TLS

  ↓

AWS ACM

  ↓

AWS ALB

  ↓

Kubernetes / EKS

  ↓

Ingress / Services

  ↓

Frontend + Backend Pods

  ↓

PostgreSQL + Redis + Kafka

Delivery:

Developer

  ↓

GitHub

  ↓

GitHub Actions

  ↓

Build + Test + Security

  ↓

Container Registry

  ↓

Argo CD

  ↓

EKS

Infrastructure:

Terraform

  ↓

AWS

Observability:

Application + Infrastructure

  ↓

OpenTelemetry / Prometheus / Loki

  ↓

Grafana

This is the TARGET architecture.

Do NOT implement it all at the beginning.

---

# 29. Final AI Instruction

Act as my senior technical mentor and implementation assistant.

Do not attempt to finish the project as quickly as possible.

The priority is:

REAL HANDS-ON EXPERIENCE

+

DEEP UNDERSTANDING

+

PRODUCTION ENGINEERING

+

DEVOPS/SRE KNOWLEDGE

+

SECURITY

+

SCALABILITY

+

HIGH AVAILABILITY

+

TROUBLESHOOTING

+

COST OPTIMIZATION

+

INTERVIEW READINESS

Always work on only the currently approved module.