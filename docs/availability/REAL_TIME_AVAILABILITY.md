# Real-Time Slot Availability (Module 12)

PostgreSQL-derived read model for Darshan and Ritual slot capacity. Availability is a projection only; booking create/cancel remains authoritative via slot-row `SELECT … FOR UPDATE`.

## Endpoints

All endpoints require authentication.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/temples/{templeId}/darshans/{darshanId}/availability` | Paginated Darshan slot availability (optional `date`, `from`, `to`, `page`, `size`) |
| `GET` | `/api/v1/temples/{templeId}/darshans/{darshanId}/slots/{slotId}/availability` | Single Darshan slot availability |
| `GET` | `/api/v1/temples/{templeId}/rituals/{ritualId}/availability` | Paginated Ritual slot availability |
| `GET` | `/api/v1/temples/{templeId}/rituals/{ritualId}/slots/{slotId}/availability` | Single Ritual slot availability |

## Response

```json
{
  "slotId": 42,
  "capacity": 100,
  "bookedQuantity": 37,
  "remainingCapacity": 63,
  "available": true
}
```

- `bookedQuantity`: `SUM(quantity)` for `CONFIRMED` bookings only.
- `remainingCapacity`: `max(0, capacity - bookedQuantity)`.
- `available`: slot is bookable now (`AVAILABLE`, not ended, remaining capacity > 0). Parent temple/darshan/ritual visibility rules apply before the response is returned.

## Rules

- PostgreSQL is authoritative; availability is not cached in Redis.
- Read queries do not acquire write locks.
- No booking or user details are exposed.
- List queries aggregate confirmed bookings with a single `LEFT JOIN` + `GROUP BY` per page (no per-slot SUM loop).

## Indexes

Uses existing partial indexes from V8:

- `booking_darshan_slot_confirmed_idx`
- `booking_ritual_slot_confirmed_idx`

No new migration was added for Module 12.
