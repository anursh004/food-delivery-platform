package com.dv.e2e;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Testcontainers
class GatewayBlackBoxIT {

  // Compose from repo root; when running in module dir, "../docker-compose.yml" points to root
  @Container
  static final DockerComposeContainer<?> ENV = new DockerComposeContainer<>(
      new File("../docker-compose.yml"))
      .withBuild(true)
      .withLocalCompose(true)
      .withExposedService("api-gateway", 8080,
          Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(6)));

  @Test
  void endToEnd_viaGateway_orderIdempotency_andMenuEtag() {
    int gatewayPort = ENV.getServicePort("api-gateway", 8080);
    RestAssured.baseURI = "http://localhost:" + gatewayPort;

    // 1) Create an order via gateway (idempotent)
    String key = UUID.randomUUID().toString();
    String orderId =
        given()
            .header("Idempotency-Key", key)
            .when()
            .post("/api/orders")
            .then()
            .statusCode(anyOf(is(201), is(200)))
            .body("orderId", notNullValue())
            .extract().path("orderId");

    // Repeat same key → still succeeds & same id (status may be 201 in current impl)
    String orderId2 =
        given()
            .header("Idempotency-Key", key)
            .when().post("/api/orders")
            .then().statusCode(anyOf(is(201), is(200)))
            .extract().path("orderId");
    org.junit.jupiter.api.Assertions.assertEquals(orderId, orderId2);

    // 2) Fetch menu for seeded restaurant id, get ETag, then re-fetch with If-None-Match → 304
    String restId = "00000000-0000-0000-0000-000000000001";
    String etag =
        given().when().get("/api/restaurants/{id}/menu", restId)
            .then().statusCode(200)
            .header("ETag", notNullValue())
            .extract().header("ETag");

    given().header("If-None-Match", etag)
        .when().get("/api/restaurants/{id}/menu", restId)
        .then().statusCode(304)
        .header("ETag", equalTo(etag));
  }
}
