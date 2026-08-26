# Hands-On Exercises

Safe local labs for Module 02. Run on Windows; Linux equivalents noted. No destructive changes.

## Exercises completed (Module 02 implementation)

Environment: Windows 10, Python 3.14.7, project root `C:\DevOps\Temple-Digital-Platform`.

### 1. Inspect IP configuration

```powershell
ipconfig
```

**Result:** WiFi adapter active with IPv4, subnet mask, default gateway, DNS server (1.1.1.1). Several adapters show Media disconnected.

**Linux:** `ip addr`

### 2. Resolve a public hostname

```powershell
nslookup github.com
```

**Result:** `github.com` → `20.207.73.82` via resolver `1.1.1.1`.

**Linux:** `dig github.com +short`

### 3. Ping and interpret

```powershell
ping -n 2 8.8.8.8
```

**Result:** 0% loss, ~17–20 ms RTT — IP-layer reachability to Google DNS.

**Note:** Ping success does not prove HTTP/443 works.

### 4. curl public HTTPS endpoint

```powershell
curl.exe -I -s -m 10 https://example.com
```

**Result:** `HTTP/1.1 200 OK`, headers include `Server: cloudflare`.

**Checks:** DNS + TCP 443 + TLS + HTTP in one command.

### 5. Check local listening port

After starting temporary server (exercise 6):

```powershell
netstat -ano | findstr ":8888"
```

**Result:**

```text
TCP    0.0.0.0:8888    LISTENING    16516
TCP    [::]:8888       LISTENING    16516
```

Server bound **all interfaces** (`0.0.0.0`), not loopback-only.

**Linux:** `ss -tlnp | grep 8888`

### 6. Temporary local HTTP server

```powershell
python -m http.server 8888
```

Python 3.14.7 was already installed. Server started in background; stopped gracefully in exercise 9.

### 7. Access via localhost

```powershell
curl.exe -s -m 5 http://127.0.0.1:8888/
```

**Result:** HTML directory listing (`<!DOCTYPE HTML>…`) — loopback reachability confirmed.

Compare: `http://localhost:8888` would behave the same (localhost → 127.0.0.1).

### 8. Process owning the port

```powershell
netstat -ano | findstr ":8888"
Get-Process -Id 16516
```

**Result:** PID 16516 = `python` listening on 8888.

**Linux:** `ss -tlnp` shows `pid=`

### 9. Stop process gracefully

```powershell
Stop-Process -Id 16516
```

SIGTERM equivalent on Windows. Port 8888 no longer LISTENING (only TIME_WAIT entries briefly).

**Linux:** `kill <pid>` (SIGTERM), then `kill -9` only if needed.

### 10. Connection refused (safe)

With nothing on port 59999:

```powershell
curl.exe -s -m 3 http://127.0.0.1:59999/
Test-NetConnection -ComputerName 127.0.0.1 -Port 59999
```

**Result:** curl exit code 7; `TcpTestSucceeded : False` — immediate failure, nothing listening.

### 11. DNS failure (safe)

```powershell
nslookup this-hostname-does-not-exist.invalid
```

**Result:** `Non-existent domain` — resolution failed before any TCP attempt.

### 12. Timeout vs refused vs DNS failure

| Symptom | Layer | Module 02 demo |
|---------|-------|----------------|
| DNS failure | DNS | Exercise 11 — NXDOMAIN |
| Connection refused | TCP | Exercise 10 — instant fail, port closed |
| Connection timeout | Network/firewall | Use unreachable IP behind drop rule or blocked host (document only; do not attack external hosts) |
| TLS failure | TLS | `curl -vI https://expired.badssl.com` (optional self-study) |
| HTTP 4xx/5xx | HTTP | `curl -I` returns status after TLS succeeds |

---

## Troubleshooting mindset: “Temple website is down”

Work **top to bottom**. At each step, try **hostname**, **IP**, and **localhost** where relevant (localhost only for on-box checks).

| Step | Question | Tools |
|------|----------|-------|
| 1 | DNS resolving? | `nslookup temple.example.com` |
| 2 | Host reachable? | `ping`, `tracert` (limited) |
| 3 | Port 443 open? | `Test-NetConnection -Port 443` |
| 4 | TLS working? | `curl -vI https://…` |
| 5 | Load balancer healthy? | AWS/K8s consoles (later) |
| 6 | App process running? | `ps`, `Get-Process`, pod status |
| 7 | App listening on expected port? | `ss -tlnp`, `netstat -ano` — expect `0.0.0.0:8080` in containers |
| 8 | Backend dependency reachable? | from app network: Postgres `:5432` |
| 9 | Logs showing errors? | `journalctl`, app log files |
| 10 | CPU/memory/disk exhausted? | `top`, `df -h`, `free -h` |

### Failure types vs layers

- **Port misconfiguration** — refused or wrong service; check bind address and port env vars.
- **Application crash** — refused or 502; process gone; check logs and restarts.
- **Dependency down** — 5xx; app logs show DB connection errors.
- **Resource exhaustion** — timeouts, OOM; check `df`, `free`, load average.

Do not skip layers: a 502 from ALB still requires checking whether the backend pod listens on `8080` and binds `0.0.0.0`, not only `127.0.0.1`.

---

## Self-study (optional, not run in Module 02)

- `Resolve-DnsName example.com`
- `tracert github.com`
- `route print`
- Linux VM or WSL: `ip addr`, `ss`, `journalctl`

No new software installed during Module 02.
