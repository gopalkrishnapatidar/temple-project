# Troubleshooting Commands

Windows-first (this dev machine); Linux equivalents for container/EKS work.

For each: **what it checks**, **example**, **interpret output**, **when SRE uses it**.

## IP configuration

| Windows | Linux | Checks |
|---------|-------|--------|
| `ipconfig` | `ip addr` / `ip a` | Local IPs, gateways, DNS servers |

**Example (this machine):** WiFi adapter shows IPv4, subnet mask, default gateway, DNS (e.g. 1.1.1.1).

**SRE use:** verify pod/node has expected IP; wrong subnet breaks routing.

## Reachability

| Command | Checks |
|---------|--------|
| `ping 8.8.8.8` | ICMP to IP (may be blocked; not proof HTTP works) |
| `ping hostname` | DNS + ICMP |

**Module 02 result:** `Reply from 8.8.8.8 … time=17–20ms` — host reachable at IP layer.

**Interpret:** request timeout → routing/firewall/host down; "could not find host" → DNS issue before ping.

## DNS

| Windows | Linux |
|---------|-------|
| `nslookup github.com` | `dig github.com` / `nslookup` |
| `Resolve-DnsName github.com` | `host github.com` |

**Module 02 result:** `github.com` → `20.207.73.82` via resolver 1.1.1.1.

**Failure example:** `Non-existent domain` for `this-hostname-does-not-exist.invalid`.

## HTTP/HTTPS

| Command | Checks |
|---------|--------|
| `curl -I https://example.com` | TLS + HTTP headers + status |

**Module 02 result:** `HTTP/1.1 200 OK`, `Server: cloudflare`.

**SRE use:** verify LB/ingress returns expected status without a browser.

## Port connectivity

| Windows | Linux |
|---------|-------|
| `Test-NetConnection -ComputerName host -Port 443` | `nc -zv host 443` |
| `netstat -ano` | `ss -tlnp` |

**Listening (Module 02):** `TCP 0.0.0.0:8888 LISTENING PID 16516` — Python HTTP server.

**Closed port:** `TcpTestSucceeded : False` on 127.0.0.1:59999 — nothing listening (connection refused at TCP layer).

## Route

| Windows | Linux |
|---------|-------|
| `route print` | `ip route` |
| `tracert host` | `traceroute host` / `tracepath` |

**SRE use:** packet path, stuck hop, asymmetric routing.

## Process on port

Windows:

```powershell
netstat -ano | findstr :8888
Get-Process -Id <pid>
```

Linux:

```bash
ss -tlnp | grep 8080
ps -p <pid> -o pid,cmd
```

## wget / telnet / nc (concepts)

- **wget** — download URL (like curl for files).
- **telnet host 443** — raw TCP connect test (often disabled; use Test-NetConnection/nc).
- **nc (netcat)** — send/receive TCP/UDP; port probe: `nc -zv host port`.

---

## Error classification

Use **localhost vs hostname vs IP** in every check: wrong Host header, cert mismatch, or `/etc/hosts` override can make `localhost` work while production hostname fails.

| Error | Layer failing | Usually means | Check commands | Investigate next |
|-------|---------------|---------------|----------------|------------------|
| **DNS resolution failure** | DNS / app config | Bad name, NXDOMAIN, resolver down | `nslookup`, `dig`, `Resolve-DnsName` | DNS records, Route 53, typo in URL, client resolver |
| **Connection refused** | TCP (no listener) | Nothing listening on port; wrong port; process crashed | `Test-NetConnection`, `curl`, `ss -tlnp` | App running? correct port? bind 127.0.0.1 vs 0.0.0.0? |
| **Connection timeout** | Network / firewall | SYN no reply; ACL, security group, routing, host down | `ping`, `tracert`, `Test-NetConnection` | Firewall rules, SG/NACL, wrong IP, overloaded LB |
| **TLS / certificate failure** | TLS (above TCP) | Expired cert, hostname mismatch, untrusted CA | `curl -vI https://…`, browser cert details | ACM/cert-manager renewal, SNI, clock skew |
| **HTTP 4xx** | HTTP / application logic | Client error: auth, not found, bad input | `curl -I`, access logs | 401/403 → auth; 404 → routing/path; compare Host header |
| **HTTP 5xx** | Server / upstream | App crash, DB down, LB cannot reach backend | `curl`, app logs, `kubectl logs` (later) | Stack trace, dependency health, recent deploy |

### Distinguishing quickly

- **DNS failure** — never get an IP; `nslookup` fails first.
- **Refused** — IP/port reached quickly; TCP RST (curl exit 7 on Windows).
- **Timeout** — waits then fails; often firewall or dead route.
- **TLS error** — TCP works; handshake fails before HTTP status.
- **4xx/5xx** — TLS OK; HTTP status in response (problem is app or gateway config).

---

## Temple “site down” command sequence

1. `nslookup temple.example.com` — DNS?
2. `ping` / path — host alive? (ICMP optional)
3. `Test-NetConnection temple.example.com -Port 443` — port open?
4. `curl -vI https://temple.example.com` — TLS + HTTP status?
5. Check LB target health (AWS/K8s — later modules)
6. `ss -tlnp` / logs on backend pod — app listening on 8080?
7. Test Postgres from backend network — 5432 reachable?
8. `journalctl` / app logs — errors?
9. `df -h`, `free -h`, `top` — resource exhaustion?
