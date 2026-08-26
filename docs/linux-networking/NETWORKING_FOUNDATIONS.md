# Networking Foundations

How traffic reaches the Temple Digital Services Platform — from browser to database.

## IP addressing

- **IPv4** — 32-bit addresses (e.g. `20.207.73.82`).
- **127.0.0.1** — loopback; traffic never leaves the host.
- **localhost** — hostname; typically resolves to `127.0.0.1` via `/etc/hosts` or DNS.
- **0.0.0.0** — used when a **server binds/listens** on all available network interfaces. It is **not** an address clients connect to.
- **Private IP** — RFC1918 ranges (`10.x`, `172.16–31.x`, `192.168.x`) used inside VPC/home networks; not routable on the public internet.
- **Public IP** — globally routable; assigned to load balancers, NAT gateways, etc.
- **Subnet** — IP range within a VPC (e.g. `10.0.1.0/24`); routing and security boundaries.

## Ports and sockets

- **Port** — 0–65535; identifies a service on a host (TCP/UDP).
- **Socket** — IP + port + protocol endpoint for communication.

Temple project ports (future, not implemented yet):

| Service | Port |
|---------|------|
| Next.js frontend | 3000 |
| Spring Boot backend | 8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 (typical) |
| HTTPS (public) | 443 |

## TCP and UDP

- **TCP** — reliable, ordered, connection-oriented (HTTP, Postgres, most APIs).
- **UDP** — lightweight, no guaranteed delivery (DNS queries, some streaming).

### Three-way handshake (TCP)

1. Client → SYN  
2. Server → SYN-ACK  
3. Client → ACK  

Connection established. **Teardown:** FIN/ACK sequence closes gracefully.

### Connection lifecycle

`CLOSED → SYN_SENT → ESTABLISHED → FIN_WAIT → CLOSED` (simplified). Timeouts occur when SYN never gets SYN-ACK (firewall, dead host, wrong IP).

## DNS

- **DNS** — maps hostnames to IP addresses.
- **Hostname** — e.g. `temple.example.com`.
- **Resolution flow:** browser → OS resolver → recursive DNS (1.1.1.1) → authoritative DNS → IP returned.

Temple path: user enters domain → DNS → ALB public IP → TLS → ingress → backend service.

## HTTP and HTTPS

- **HTTP** — application protocol over TCP; request/response, text headers + body.
- **HTTPS** — HTTP over **TLS** (encrypted, authenticated server).

Common **methods:** GET (read), POST (create/action), PUT/PATCH (update), DELETE.

Common **status codes:**

| Code | Meaning |
|------|---------|
| 200 | OK |
| 301/302 | Redirect |
| 400 | Bad request (client) |
| 401/403 | Auth failure / forbidden |
| 404 | Not found |
| 500 | Server error |
| 502/503/504 | Gateway/upstream failure |

**Headers:** `Host`, `Authorization`, `Content-Type`, `X-Request-Id` — routing, auth, tracing.

## TCP vs HTTP

TCP delivers bytes between ports. HTTP defines **what** those bytes mean (method, path, headers, body). You can have TCP without HTTP (Postgres wire protocol); HTTP always runs over TCP (or QUIC for HTTP/3).

## TLS basics

- Encrypts traffic; server presents certificate.
- Handshake negotiates cipher; client verifies cert chain and hostname.
- Failures: expired cert, wrong hostname (SNI), untrusted CA, clock skew.

Public temple site: browser → port 443 → TLS → HTTP inside encrypted tunnel.

## Client/server model

- **Client** initiates (browser, curl, Spring Boot calling Postgres).
- **Server** listens on a port and accepts connections.

## localhost vs 0.0.0.0 (critical for DevOps)

| | 127.0.0.1 / localhost | 0.0.0.0 |
|---|------------------------|---------|
| **Purpose** | Reach service on **same machine** | **Bind** server on **all interfaces** |
| **Client use** | Yes — `curl http://127.0.0.1:8080` | **No** — clients use real hostname/IP |
| **From another host** | Not reachable | Reachable if firewall/routing allow |

**Why it matters for Docker/Kubernetes (later):**

- App bound to `127.0.0.1:8080` inside a container → only reachable **inside that container**; kube probes and other pods cannot connect.
- App bound to `0.0.0.0:8080` → listens on container eth0; Service/Ingress can forward traffic to pod IP:8080.

Module 02 exercise: `python -m http.server 8888` showed `TCP 0.0.0.0:8888 LISTENING` — accessible via `127.0.0.1:8888` locally and would accept external interface traffic if firewall allowed.

## Temple request path (target architecture)

```text
User browser
  → DNS lookup (temple.example.com → IP)
  → TCP :443
  → TLS handshake
  → HTTP request
  → AWS ALB (load balancer)
  → Kubernetes Ingress
  → Service → Backend pod :8080 (Spring Boot)
  → PostgreSQL :5432
  → (later) Redis :6379, Kafka :9092
```

Each hop is a separate troubleshooting layer (see HANDS_ON_EXERCISES.md).
