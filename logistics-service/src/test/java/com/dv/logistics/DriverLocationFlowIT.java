package com.dv.logistics;

import com.dv.logistics.messaging.DriverLocationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DriverLocationFlowIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
  @Container
  static KafkaContainer kafka = new KafkaContainer(org.testcontainers.utility.DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> postgres.getJdbcUrl());
    r.add("spring.datasource.username", () -> postgres.getUsername());
    r.add("spring.datasource.password", () -> postgres.getPassword());
    r.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
  }

  @LocalServerPort int port;
  @Autowired ObjectMapper om;

  @Test
  void kafkaToWebSocket_flow_works() throws Exception {
    UUID orderId = UUID.randomUUID();

    // STOMP client subscribe to topic
    List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
    SockJsClient sockJsClient = new SockJsClient(transports);
    WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

    BlockingQueue<DriverLocationEvent> queue = new ArrayBlockingQueue<>(1);

    StompSession session = stompClient.connect(new URI("ws://localhost:" + port + "/ws"), new StompSessionHandlerAdapter(){}).get(5, TimeUnit.SECONDS);
    session.subscribe("/topic/track/" + orderId, new StompFrameHandler() {
      @Override public Type getPayloadType(StompHeaders headers) { return DriverLocationEvent.class; }
      @Override public void handleFrame(StompHeaders headers, Object payload) {
        try {
          // SockJS + Jackson may deliver as LinkedHashMap; convert
          DriverLocationEvent evt = om.convertValue(payload, DriverLocationEvent.class);
          queue.offer(evt);
        } catch (Exception e){}
      }
    });

    // Produce a Kafka message
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    try (KafkaProducer<String,String> producer = new KafkaProducer<>(props)) {
      DriverLocationEvent evt = new DriverLocationEvent(orderId, 18.5204, 73.8567, OffsetDateTime.now());
      String json = om.writeValueAsString(evt);
      producer.send(new ProducerRecord<>("driver.locations", orderId.toString(), json)).get(5, TimeUnit.SECONDS);
    }

    DriverLocationEvent received = queue.poll(10, TimeUnit.SECONDS);
    assertNotNull(received, "Expected to receive a WebSocket message");
    assertTrue(orderId.equals(received.orderId), "OrderId should match");
  }
}
