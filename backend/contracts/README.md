# contracts — shared cross-service schemas

Single source of truth for inter-service contracts so producers and consumers can never drift.

## Layout

```
contracts/
├── kafka/             # Avro schemas for every Kafka topic
│   ├── tenant.created.v1.avsc
│   ├── user.created.v1.avsc
│   ├── load.created.v1.avsc
│   ├── load.assigned.v1.avsc
│   ├── truck.position.v1.avsc
│   ├── invoice.created.v1.avsc
│   └── ...
├── openapi/           # OpenAPI 3.1 specs for every service's REST API
│   ├── identity.yaml
│   ├── operations.yaml
│   └── ...
└── proto/             # Optional: gRPC for low-latency intra-cluster calls
```

## How services consume these

- **Java services** depend on `:contracts` and use `avro-tools` Gradle task to generate Java classes for the topics they produce/consume.
- **Frontend** generates TypeScript clients from the OpenAPI specs into `frontend/shared/src/api/<service>.ts`.
- **Node tracking-service** uses `@kafkajs/avro` with the schemas at runtime and generates TS types via `avsc`.

## Versioning rules

1. **Never edit a published schema.** Add `vN+1` and dual-publish until consumers migrate.
2. **Backward-compatible changes only** within a major version (add optional fields, never remove or rename).
3. **Topic names** carry the version: `truck.position.v1`, `truck.position.v2`. CI fails the PR if a version bump is missing.

This directory is non-code; it's consumed by build tasks in each service.
