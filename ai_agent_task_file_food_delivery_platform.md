# AI Agent Task File — End‑to‑End Implementation (No‑Hallucination Spec)

> **Goal:** Implement a fully working, Dockerized **Java 21 + Spring Boot 3.x** multi‑module system for a Food Delivery Platform with **unit + integration + e2e tests**, **Hexagonal/SOLID** architecture, and **observability**. This task file is the single source of truth. If any information is missing, **do not invent**; follow the fallback rules defined below.

---

## 0) Operating Principles (No‑Hallucination Rules)
1. **Single Source of Truth:** Use only this Task File and the "Technical Implementation Guide" document already provided. Do **not** pull external frameworks or patterns unless explicitly stated.
2. **No Fabrication:** If something is undefined, follow the **Fallback Hierarchy**:
   - (a) Prefer the defaults stated in this Task File.
   - (b) Choose the simplest, standard Spring approach consistent with Hexagonal/SOLID.
   - (c) Leave a `// TODO:` with exact missing detail and proceed only if non‑blocking.
3. **Version Integrity:** Pin versions and verify availability in Maven Central during generation. If a coordinate fails to resolve, **fallback** to the previous patch version (see Version Selection Algorithm).
4. **Deterministic Outputs:** Before writing code, generate the **final file tree**; after code generation, re‑print the tree. Trees **must match** except for build outputs (e.g., `target/`).
5. **Self‑Testing:** All tests must pass locally via the commands specified in **Section 10**. Capture real logs/outputs; never claim results not actually produced.

---

## 1) Target Deliverable
A monorepo with these modules and characteristics:

- `common-domain`: domain entities, value objects, errors, **ports** (interfaces). **No Spring imports.**
- `order-service`: order creation, idempotency via `Idempotency-Key`, transactional outbox → Kafka `orders.created`.
- `catalog-service`: restaurants & menus; **L1 Caffeine** + **L2 Redis** caching; `ETag` + `If-None-Match`.
- `logistics-service`: consumes `driver.locations` from Kafka, batch persistence, WebSocket `/ws/track/{orderId}`.
- `api-gateway`: lightweight aggregation and routing; OpenAPI exposure; no business logic.
- `e2e-tests`: black‑box flows with Testcontainers‑Compose/`docker compose` profile.

Each service ships a **Dockerfile**, Flyway migrations (if DB‑backed), OpenAPI definition, unit & integration tests.

---

## 2) Pinned Tech & Versions (with Fallback)
- **Java**: 21 (Temurin).
- **Spring Boot**: **3.3.4**. *If not resolvable →* 3.3.3 → 3.3.2.
- **Spring Cloud**: Only if required; prefer none for simplicity.
- **PostgreSQL**: 16
- **Redis**: 7
- **Kafka/Zookeeper**: Bitnami images latest stable tags.
- **Build**: Maven multi‑module; use Spring Boot parent for dependency management.
- **Testing**: JUnit 5 (5.10.x), Mockito, AssertJ, REST Assured, **Testcontainers** (1.20.x). *If a lib fails to resolve, decrement patch.*
- **Mapping**: MapStruct (latest 1.6.x stable); processor configured for incremental builds.
- **Observability**: Actuator + Micrometer Prometheus.
- **Docs**: springdoc-openapi v2.x for Spring Boot 3.

---

## 3) Repository Structure (Authoritative)
```
food-delivery/
  pom.xml                      # parent aggregator (packaging=pom)
  docker-compose.yml
  prometheus/
    prometheus.yml
  common-domain/
    pom.xml
    src/main/java/...          # domain model & ports (no Spring)
    src/test/java/...
  order-service/
    pom.xml
    src/main/java/...
    src/main/resources/db/migration/   # Flyway
    src/test/java/...                  # unit + integration (Testcontainers: Postgres, Kafka)
    Dockerfile
  catalog-service/
    pom.xml
    src/main/java/...
    src/main/resources/db/migration/
    src/test/java/...                  # Postgres + Redis containers
    Dockerfile
  logistics-service/
    pom.xml
    src/main/java/...
    src/main/resources/db/migration/
    src/test/java/...                  # Kafka + Postgres + WebSocket test
    Dockerfile
  api-gateway/
    pom.xml
    src/main/java/...
    src/test/java/...
    Dockerfile
  e2e-tests/
    pom.xml
    src/test/java/...                  # REST Assured + Testcontainers-Compose
  README.md
  Makefile (optional)
```

---

## 4) Architecture & Design Constraints
- **Hexagonal** (Ports & Adapters). Layers in services:
  - `web` (controllers) → `app` (use cases) → `domain` (entities/VOs/interfaces) → `adapters` (db/kafka/redis/http)
- **SOLID**: thin controllers; services per use case; DIP via domain ports; mappers isolate DTO ↔ domain.
- **Validation**: Jakarta annotations at input boundary; domain invariants enforced in constructors/factories.
- **Transactions**: Order creation is atomic with outbox write (TX publish or outbox relay).

---

## 5) Data Model (Minimum Viable)
- `orders(id, customer_id, restaurant_id, total_amount, status, idempotency_key UNIQUE, created_at)`
- `outbox_events(id, aggregate_id, type, payload JSONB, status, created_at, published_at)`
- `restaurants(id, name, city)`
- `menus(id, restaurant_id FK, item_name, price, is_available)` (index on `restaurant_id`)
- `driver_locations(id, order_id, lat, lon, updated_at)` (index `(order_id, updated_at)`)

Seed 2 restaurants, 10 items each.

---

## 6) Messaging & Caching
- **Kafka Topics**: `orders.created`, `driver.locations`.
- **Order Flow**: REST → app service → DB (orders + outbox) → relay publishes `orders.created`.
- **Logistics**: Kafka listener (concurrency>1) batches and persists; emits WS updates.
- **Catalog Cache**: `@Cacheable` using Caffeine (L1) + Redis (L2). Explicit invalidation endpoint for tests.

---

## 7) API Contracts (Documented via OpenAPI)
- `POST /orders` → 201; body returns order id; **Idempotency-Key** header required; duplicate key → 200 with same order id.
- `GET /restaurants/{id}/menu` → 200 with `ETag`; if `If-None-Match` matches → 304.
- `POST /restaurants/{id}/menu/_refresh` → 202; test/admin only.
- `WS /ws/track/{orderId}` → streams location updates.
- Actuator: `/actuator/health`, `/actuator/prometheus`.

Include examples in OpenAPI, response schemas, and error model (problem+json).

---

## 8) Docker & Infra
- **docker-compose.yml** includes: `postgres:16`, `redis:7`, `bitnami/zookeeper`, `bitnami/kafka`, `prom/prometheus`, all services.
- Healthchecks for infra (pg_isready, redis-cli ping, kafka ready script) and apps (Actuator `/health`).
- Shared network; sensible ports (avoid local collisions).
- **Prometheus** scrapes each service `/actuator/prometheus`.

---

## 9) Testing Requirements (Must Pass)
**Unit**
- Domain services with mocked ports; >80% coverage on domain + app modules of each service.

**Integration**
- `order-service`: Postgres + Kafka containers; test idempotent `POST /orders`; verify outbox → one published event per order.
- `catalog-service`: Postgres + Redis containers; assert cache hit/miss + ETag/304 behaviour.
- `logistics-service`: Kafka producer pushes `driver.locations`; verify batch persistence + WS emits (Stomp client test + Awaitility).

**E2E (e2e-tests module)**
- Bring up infra + services (Testcontainers‑Compose or `docker compose` profile) and run REST Assured:
  1) Create order → 201 → follow with same Idempotency‑Key → 200 same id.
  2) Fetch menu → 200 with ETag → repeat with `If-None-Match` → 304.
  3) Produce driver updates → receive WS messages for `orderId`.

---

## 10) Build, Run, Verify (Authoritative Commands)
```bash
# 1) Build + tests
./mvnw -q -DskipTests=false clean verify

# 2) Run stack
docker compose up --build -d

# 3) Health gate (should be UP within 90s)
curl -sf http://localhost:8080/actuator/health

# 4) Smoke
curl -s http://localhost:8080/v3/api-docs | head -n 20

# 5) Tail logs (example)
docker compose logs --tail=200 api-gateway
```
**Deliver actual terminal output in README** (copy/paste); do not paraphrase.

---

## 11) Static & Style Gates
- Checkstyle/Spotless (or `maven-checkstyle-plugin`) with Google or Spring style.
- MapStruct mappers for DTO↔domain.
- No `@Autowired` field injection; constructor injection only.
- Controller methods small; business logic lives in app services.

---

## 12) CI (Local Parity)
- Provide GitHub Actions workflow (`.github/workflows/build.yml`) running `./mvnw -DskipTests=false verify` and caching `~/.m2`.
- Job must fail on any test failure or Checkstyle issue.

---

## 13) Version Selection Algorithm (To Avoid Missing Artifacts)
1. Try target version (e.g., Spring Boot **3.3.4**). If dependency resolution fails (404 from Maven Central), decrement patch → 3.3.3 → 3.3.2.
2. For Testcontainers/MapStruct/REST Assured, pick latest known stable minor (1.20.x / 1.6.x / 5.5.x respectively). If resolution fails, decrement patch.
3. **Record chosen versions** in README under “Resolved Versions.”

---

## 14) Definition of Done (DoD)
- ✅ `./mvnw clean verify` passes with tests **not skipped**.
- ✅ `docker compose up --build -d` → all containers **healthy**.
- ✅ Functional checks in **Section 9** succeed.
- ✅ Domain modules import **no Spring**.
- ✅ Coverage report shows ≥ **80%** lines for domain + app services.
- ✅ README includes: run commands, resolved versions, sample curl outputs, screenshots or logs from health + e2e flows.

---

## 15) Delivery Artifacts
- Full repo with code, tests, Docker assets, OpenAPI, Flyway migrations, Prometheus config.
- `README.md` with exact runbook and captured outputs.
- Optional `Makefile` (`make build`, `make up`, `make down`, `make test`).

---

## 16) Execution Plan (Strict Order)
1. Generate **file tree** exactly as in Section 3.
2. Create parent POM + module POMs with dependency management and plugins (Surefire/Failsafe, JaCoCo, Checkstyle/Spotless).
3. Implement `common-domain` (entities, value objects, ports, errors, factories).
4. Implement `order-service` (use case, persistence adapter, outbox, controller, tests).
5. Implement `catalog-service` (read model, caching, ETag path, tests).
6. Implement `logistics-service` (consumer, batch repo, WebSocket config, tests).
7. Implement `api-gateway` + OpenAPI exposure/aggregation.
8. Add Flyway migrations + seed data; verify schema.
9. Add Dockerfiles per service; write `docker-compose.yml` with healthchecks and Prometheus scrape config.
10. Implement `e2e-tests` flows.
11. Run Section 10 commands; fix regressions; repeat until **DoD** met.

---

## 17) Guardrails & Common Pitfalls
- Do **not** introduce security/auth now (out of scope for local run).
- Avoid circular deps between modules.
- Ensure Kafka producer and DB write are coordinated (TX or outbox).
- Use **constructor** injection; avoid static singletons.
- For WebSocket tests, use STOMP client + Awaitility timeouts.

---

## 18) What to Output When Finished
- `Resolved Versions` table.
- File tree (final).
- `mvn verify` summary with SUCCESS lines.
- Health check `curl` output and sample e2e responses.
- Short note on how caches behaved (hit/miss logs) and idempotency demo.

---

**End of Task File.** Follow exactly. If a step cannot be completed without external info, place a precise `// TODO:` with the missing key and proceed with the remaining tasks that are unblocked.

