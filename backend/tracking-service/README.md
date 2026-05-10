# tracking-service

Node 20 + TypeScript + Fastify + KafkaJS. Ingests truck GPS at ~30s intervals, fans out live positions over WebSocket, and publishes to Kafka for downstream consumers.

## Why Node here (and only here)

- I/O-bound: thousands of concurrent WebSocket connections.
- Different release cadence than the Java services (faster iteration on ETA / geofencing logic).
- Different scaling axis: scales with truck count, not user count.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET  | `/actuator/health` | Liveness / readiness |
| POST | `/ingest` | One sample or batch (≤ 500). Auth: bearer JWT. |
| WS   | `/ws/positions` | Live position stream for the caller's tenant. |

## Data flow on ingest

1. Validate (Zod) → multi-row INSERT into `tracking.truck_position` hypertable.
2. Upsert `tracking.truck_last_position` (one row per truck) — drives the live map.
3. Publish to Kafka topic `truck.position.v1` (consumers: dispatch, compliance).
4. Publish to Redis channel `tenant:<id>:positions` (drives WebSocket fanout).

## Running locally

```bash
make up                        # from repo root, brings up postgres+kafka+redis
pnpm install
pnpm dev                       # tsx watch
```

## Adaptive sampling (planned)

The 30s default is enough for ETA + state-mile calc. Burst to 5s when:
- Truck within 10 miles of pickup or delivery
- Truck within 1 mile of state border at >55 mph
- Dispatcher actively viewing this truck on the map

Implementation lives in this service, not the device — the device pushes whatever it can; we down-sample on ingest.
