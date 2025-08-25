# ARCHITECTURE

## Style
- **Microservices** with **Hexagonal** boundaries.
- **Event-driven** via Kafka for decoupling and backpressure.
- **Caching** (Caffeine L1 + Redis L2) for hot browse paths.
- **Outbox pattern** for reliable order events.

## Rationale
- **Scalability**: Independent services scale horizontally (order, catalog, logistics). Kafka absorbs spikes (2k msg/s target design).
- **Resilience**: Order creation is independent of payment; payment is mocked + async worker so 3rd-party instability doesn't block orders. Outbox prevents event loss.
- **Performance**: Catalog read is cached (warm path sub-10ms locally). ETag/304 reduces payloads. DBs indexed for hot queries.
- **Maintainability**: Multi-module, strict layering, tests per service, shared domain in `common-domain`.

## Key Flows
- **Order**: HTTP → TX (order+outbox) → async `OutboxRelay` → Kafka → downstream consumers.
- **Payment (Mock)**: **Async** worker scans `CREATED` orders and marks `PAID`. In real world a separate payment service would consume `orders.created`.
- **Browse**: GET menu → cache L1/L2 → DB (on miss) → ETag support.
- **Logistics**: Kafka `driver.locations` → batch persist → broadcast `/topic/track/{orderId}`.

## Scale Targets (Design)
- Orders: **500/min** (~8.3/s). Single instance is fine; horizontal scale + Postgres connection pool to grow.
- Browse: P99 **<200ms** from cache; Redis ensures cross-instance consistency.
- Logistics: Drivers **10k** @ 5s → **2k msg/s**. Kafka partitions + consumer concurrency (increase partition count) + batch `saveAll` keep up. WebSocket broker is in-memory for local; scale with external broker or sticky sessions in prod.

## Components
- **Redis**: shared cache layer.
- **Kafka**: durable event streaming, partitions=6 (example) for parallelism.
- **Prometheus/Grafana**: metrics and dashboards.
