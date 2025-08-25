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
