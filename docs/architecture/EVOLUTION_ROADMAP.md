# Evolution Roadmap

This is a staged target, not a build list for Module 00. Do not implement these technologies until their modules are approved.

Current system:

```text
User → Frontend → Backend (modular monolith) → PostgreSQL
```

## Application and data

- **Redis:** Cache and short-lived coordination later. Never the authoritative store for booking capacity.
- **Kafka:** Async notifications and integration later. Never the authoritative store for transactional booking state.

## Packaging and runtime

- **Docker:** Repeatable app images and local parity.
- **Docker Compose:** Local multi-container stack (app, database, later Redis/Kafka).
- **Kubernetes:** Orchestration, probes, resources, rolling deploys.
- **Helm:** Packaged Kubernetes releases.
- **Load balancing:** Service/Ingress locally; ALB in AWS later.
- **TLS / certificates:** Local HTTPS → cluster TLS → cert-manager/ACM as modules require.

## Delivery

- **GitHub Actions:** Build, test, scan, image publish.
- **DevSecOps:** Secret scanning, SAST, image/IaC scanning.
- **Argo CD:** GitOps sync to Kubernetes.

## Cloud

- **AWS:** Temporary, justified resources only (VPC, EKS, RDS, etc.).
- **Terraform:** IaC with approval before apply/destroy.
- **EKS:** Production-like Kubernetes on AWS after local Kubernetes is understood.

## Observability and SRE

Metrics, logs, traces (Prometheus, Loki, Grafana, OpenTelemetry), SLIs/SLOs, alerting, incident/RCA, load testing, backup/DR, chaos, and cost optimization in later phases.

## Rule

Each technology is added when a module explains the problem it solves, alternatives, failure modes, operations, and cost. Module 00 only records the path.
