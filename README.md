# Food Delivery Platform — Skeleton

> Java 21 • Spring Boot 3.3.4 • Maven • Postgres 16 • Redis 7 • Kafka (Bitnami) • Docker Compose • Actuator + Prometheus

This is a starter skeleton aligned with the AI Agent Task File. It compiles cleanly and provides the structure for domain, services, infra, and tests.

## Resolved Versions
- Spring Boot: 3.3.4
- Testcontainers: 1.20.1
- MapStruct: 1.6.2
- REST Assured: 5.5.0

## Build & Run
```bash
./mvnw -q -DskipTests=false clean verify   # add Maven wrapper or run with your local mvn
docker compose up --build -d
curl -sf http://localhost:8080/actuator/health
```

> Note: Ensure Docker is running. The service Dockerfiles expect built JARs; run Maven build first.

## Modules
- common-domain
- order-service (8082)
- catalog-service (8083)
- logistics-service (8084)
- api-gateway (8080)
- e2e-tests

## Next Steps
- Implement outbox publishing, Redis/Caffeine cache configuration, Kafka consumers/producers, and WebSocket broadcasting with tests (as in your Task File).
- Replace placeholder logic in `OrderService#createOrder` with real persistence + outbox TX.


## Part (1): Order Outbox + Idempotency
- `order-service` now persists orders with unique `idempotency_key` and writes an outbox event within the same transaction.
- `OutboxRelay` publishes `ORDER_CREATED` events to Kafka topic `orders.created` and marks events `PUBLISHED` (or `FAILED`).
- Integration test `OrderServiceIdempotencyIT` spins up **Postgres + Kafka** using Testcontainers, posts the API twice with the same key, runs the relay, and asserts a Kafka record exists.

> For production‑grade exactly‑once, consider a background relay with retry/backoff and DLQ on failures.


## Part (2): Catalog L1+L2 Caching + ETag
- Added a **TwoLevelCacheManager** combining **Caffeine (L1)** and **Redis (L2)** for cache `menuByRestaurant`.
- `MenuQueryService#getMenu` is `@Cacheable`; `POST /restaurants/{id}/menu/_refresh` performs `@CacheEvict`.
- `MenuCachingIT` spins **Postgres + Redis** containers, verifies `ETag/304`, then updates DB and refreshes cache to force a new ETag.

Run catalog tests:
```bash
mvn -q -pl catalog-service -am -DskipTests=false test
```


## Part (3): Logistics Kafka → DB (batch) → WebSocket broadcast
- `DriverLocationConsumer` consumes `driver.locations` in **batches**, persists via `saveAll`, and broadcasts each event to **/topic/track/{orderId}** via STOMP.
- `WebSocketConfig` exposes endpoint **/ws** (with SockJS) and topic prefix **/topic**.
- Integration test `DriverLocationFlowIT` starts **Postgres + Kafka**, opens a **STOMP** subscription, produces a Kafka event, and asserts the WebSocket receives it.
- Topic configurable: `app.kafka.driverLocationsTopic` (default `driver.locations`).

Run logistics tests:
```bash
mvn -q -pl logistics-service -am -DskipTests=false test
```


## API Gateway + OpenAPI + Live UI
- **Gateway** (port 8080) proxies:
  - `POST /api/orders` → order-service `/orders` (idempotent)
  - `GET /api/restaurants/{id}/menu` → catalog-service `/restaurants/{id}/menu` (preserves ETag/304)
- Configure downstream URIs via env vars: `ORDER_SERVICE_BASE`, `CATALOG_SERVICE_BASE`, `LOGISTICS_SERVICE_BASE`.
- **OpenAPI**: Controllers annotated with `@Tag` and `@Operation`. Visit `/swagger-ui.html` per service.
- **Live UI**: Open `http://localhost:8080/` → enter an `orderId` to subscribe to `/topic/track/{orderId}` served by logistics-service.

> Tip: produce a test location by sending a Kafka message to `driver.locations` with key=orderId.


## Makefile Targets
- `make build` — full Maven verify
- `make up` — docker compose up (build + detach)
- `make down` — docker compose down -v
- `make e2e` — run only the e2e black-box tests

## GitHub Actions
A minimal workflow is provided at `.github/workflows/build.yml` that runs `mvn verify` with tests on Ubuntu.

## E2E (Black-box) Test
The `e2e-tests` module uses **Testcontainers DockerComposeContainer** to bring up the whole stack from `docker-compose.yml` and exercises flows **via the API Gateway**:
1. `POST /api/orders` with `Idempotency-Key` → returns `orderId` and is idempotent.
2. `GET /api/restaurants/{id}/menu` → captures `ETag`, then repeats with `If-None-Match` → `304 Not Modified`.


## Grafana + Prometheus (Dashboards)
A pre-wired **Grafana** instance is included. It auto-provisions a Prometheus datasource and loads a dashboard.

- Grafana: http://localhost:3000  (user: **admin**, pass: **admin**)
- Prometheus: http://localhost:9090
- Dashboard folder: **Food Delivery** → *Food Delivery — Service Overview*

Bring up with:
```bash
docker compose up --build -d prometheus grafana
# or the whole stack:
docker compose up --build -d
```

The dashboard shows:
- **Targets UP** (Prometheus `up`)
- **HTTP rate** (`http_server_requests_seconds_count`)
- **HTTP P95** via histogram quantile
- **JVM heap** (`jvm_memory_used_bytes{area="heap"}`)
- **Kafka consumption rate** (if metrics present)
- **Cache hit rate** (if metrics present)

> Spring Boot exposes metrics on `/actuator/prometheus`, already scraped by Prometheus. Panels that reference optional metrics will render when those appear.
