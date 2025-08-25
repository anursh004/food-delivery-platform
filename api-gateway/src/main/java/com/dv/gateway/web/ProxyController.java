package com.dv.gateway.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "API Gateway", description = "Thin HTTP proxy to backend services")
public class ProxyController {

  private final WebClient orderClient;
  private final WebClient catalogClient;

  public ProxyController(WebClient orderClient, WebClient catalogClient) {
    this.orderClient = orderClient;
    this.catalogClient = catalogClient;
  }

  @Operation(summary = "Create order (idempotent)", description = "Proxies to order-service /orders. Requires Idempotency-Key header.")
  @PostMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> createOrder(@RequestHeader("Idempotency-Key") String key) {
    return orderClient.post()
        .uri("/orders")
        .header("Idempotency-Key", key)
        .exchangeToMono(resp -> passthrough(resp));
  }

  @Operation(summary = "Fetch menu by restaurant", description = "Proxies to catalog-service /restaurants/{id}/menu, preserving ETag semantics.")
  @GetMapping(value = "/restaurants/{id}/menu", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> getMenu(@PathVariable UUID id, @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String inm) {
    WebClient.RequestHeadersSpec<?> req = catalogClient.get().uri("/restaurants/{id}/menu", id);
    if (inm != null) req = req.header(HttpHeaders.IF_NONE_MATCH, inm);
    return req.exchangeToMono(this::passthrough);
  }

  private Mono<ResponseEntity<String>> passthrough(ClientResponse resp) {
    HttpStatus status = resp.statusCode();
    return resp.bodyToMono(String.class)
        .defaultIfEmpty("")
        .map(body -> {
          ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
          resp.headers().asHttpHeaders().forEach((k,v) -> {
            if (HttpHeaders.ETAG.equalsIgnoreCase(k) || HttpHeaders.LOCATION.equalsIgnoreCase(k) || HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(k)) {
              builder.header(k, v.toArray(new String[0]));
            }
          });
          return builder.body(body);
        });
  }
}
