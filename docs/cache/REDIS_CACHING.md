# Redis & Caching (Module 11)

Optional Redis cache-aside for shared catalog reads. PostgreSQL remains authoritative.

## Local Redis

```bash
docker run --name temple-redis -p 6379:6379 redis:7-alpine --maxmemory 64mb --maxmemory-policy allkeys-lru
```

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `CACHE_ENABLED` | `true` | Enable Redis catalog cache |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_TIMEOUT` | `200ms` | Command timeout |
| `REDIS_PASSWORD` | empty | Optional password |

## Cached keys

- `temple-platform:v1:temple:id:{id}` — TTL 10m
- `temple-platform:v1:temple:list:public` — TTL 60s
- `temple-platform:v1:darshan:id:{id}` — TTL 5m
- `temple-platform:v1:darshan:list:temple:{templeId}:public` — TTL 5m
- `temple-platform:v1:ritual:id:{id}` — TTL 5m
- `temple-platform:v1:event:id:{id}` — TTL 2m

Values are JSON (inspectable via `redis-cli GET`).

## Invalidation

After successful PostgreSQL commit on catalog mutations:

- Temple create/update → temple id + public temple list
- Darshan create/update → darshan id + public darshan list for temple
- Ritual create/update → ritual id
- Event create/update → event id

Booking, slot, and admin-assignment changes do not invalidate catalog cache.

## Failure behavior

- Redis GET/SET/DEL failures log WARN and fall back to PostgreSQL.
- Application starts without Redis.
- Readiness depends on PostgreSQL only; Redis health is observable separately on `/actuator/health`.

## Not cached

Bookings, capacity, availability, auth, assignments, slot lists, admin views.
