# Linux Foundations

Core OS concepts for DevOps/SRE. Linux commands apply inside Docker containers, Kubernetes pods, and AWS EC2/EKS nodes. Windows equivalents are noted for local development on this machine.

## Filesystem layout

| Path | Purpose |
|------|---------|
| `/` | Root of the filesystem |
| `/home` | User home directories |
| `/etc` | Configuration files (nginx, postgres, systemd) |
| `/var` | Variable data: logs (`/var/log`), caches, spool |
| `/tmp` | Temporary files (often cleared on reboot) |
| `/opt` | Optional/third-party software |
| `/usr` | User programs, libraries, documentation |
| `/proc` | Virtual FS exposing process/kernel info (`/proc/<pid>`) |
| `/dev` | Device files (disks, terminals) |

Windows analogy: `C:\Users` ≈ `/home`, `C:\Windows\System32\config` ≈ `/etc`, event logs ≈ `/var/log`.

## Files and directories

| Task | Linux | Windows (PowerShell) |
|------|-------|----------------------|
| Current dir | `pwd` | `Get-Location` |
| List | `ls -la` | `Get-ChildItem` |
| Change dir | `cd /path` | `Set-Location` |
| Create dir | `mkdir -p dir` | `New-Item -ItemType Directory` |
| Create file | `touch file` | `New-Item file` |
| Copy/move | `cp`, `mv` | `Copy-Item`, `Move-Item` |
| Remove | `rm` (careful) | `Remove-Item` |
| Find | `find /path -name "*.log"` | `Get-ChildItem -Recurse -Filter` |
| View | `cat`, `less`, `head`, `tail` | `Get-Content`, `more` |
| Search | `grep pattern file` | `Select-String` |

## Permissions

- **r** read, **w** write, **x** execute (for files; for dirs, x = enter/list).
- **Owner / group / others** — three permission triples.
- Numeric: `chmod 640 file` → rw-r-----.
- `chown user:group file` — change owner (Linux).
- **Least privilege:** app runs as non-root; config readable only by service user; secrets not world-readable.

Temple project (later): Spring Boot process should not run as root in containers; DB credentials via env/secrets with minimal file permissions.

## Processes

- **Process** — running program instance.
- **PID** — unique process ID.
- **Parent/child** — fork/spawn tree (PID 1 is init/systemd on Linux).

| Task | Linux | Windows |
|------|-------|---------|
| List | `ps aux`, `ps -ef` | `Get-Process` |
| Live view | `top`, `htop` | Task Manager |
| Stop graceful | `kill <pid>` (SIGTERM) | `Stop-Process` |
| Force kill | `kill -9 <pid>` (SIGKILL) | `Stop-Process -Force` |

**SIGTERM (15):** request shutdown — app can flush logs, close connections.  
**SIGKILL (9):** immediate termination — no cleanup. Prefer SIGTERM first (Kubernetes pod termination uses SIGTERM then SIGKILL).

## Services (systemd)

Linux production hosts use **systemd** to manage long-running services.

```bash
systemctl status nginx
systemctl start|stop|restart myapp
journalctl -u myapp -f
```

Windows equivalent: `Get-Service`, `Start-Service`, Event Viewer / `Get-WinEvent`.

## Logs

- Linux: `/var/log/*`, `journalctl -u service -f`
- Windows: Event Viewer, application logs

```bash
tail -f /var/log/nginx/access.log
grep ERROR /var/log/app.log
journalctl -u temple-backend --since "1 hour ago"
```

Temple project (later): correlate nginx/ingress access logs with Spring Boot application logs during incidents.

## Environment variables

```bash
export DATABASE_URL=postgres://...
echo $PATH
```

- **PATH** — directories searched for executables.
- Apps read config from env at startup (12-factor style).
- Never commit secrets; use env or secret stores (see AI_CONTEXT.md).

Windows: `$env:VAR = "value"`, `[Environment]::GetEnvironmentVariable("PATH")`.

## Disk, memory, CPU

| Command | Checks |
|---------|--------|
| `df -h` | Filesystem disk usage |
| `du -sh /path` | Directory size |
| `free -h` | Memory |
| `top` / `uptime` | CPU load, uptime |

Windows: `Get-PSDrive`, `Get-Counter '\Memory\Available MBytes'`.

**Troubleshooting:** full disk → logs fail, DB cannot write; high memory → OOM kill; high load → slow booking API.

## Bind addresses (preview for containers)

| Address | Role |
|---------|------|
| **127.0.0.1** | Loopback — only this machine |
| **localhost** | Hostname that normally resolves to 127.0.0.1 |
| **0.0.0.0** | **Listen/bind only** — “all interfaces”; not a client destination |

Binding Spring Boot to `127.0.0.1:8080` makes it unreachable from another container/pod. Binding to `0.0.0.0:8080` accepts traffic on the pod network interface. Clients connect using a **real IP or hostname**, never `0.0.0.0`.

See NETWORKING_FOUNDATIONS.md for full explanation.
