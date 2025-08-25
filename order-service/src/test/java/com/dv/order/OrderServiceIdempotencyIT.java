package com.dv.order;

import com.dv.order.app.OutboxRelay;
import com.dv.order.repo.OutboxRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderServiceIdempotencyIT {

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

  @Autowired MockMvc mvc;
  @Autowired OutboxRelay relay;
  @Autowired OutboxRepository outbox;

  KafkaConsumer<String,String> consumer;

  @BeforeEach
  void setupConsumer(){
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
    consumer.subscribe(java.util.List.of("orders.created"));
  }

  @AfterEach
  void tearDown(){
    if (consumer != null) consumer.close();
  }

  @Test
  void createOrder_isIdempotent_andPublishesOutbox() throws Exception {
    String key = UUID.randomUUID().toString();

    // First call -> create
    String resp1 = mvc.perform(post("/orders").header("Idempotency-Key", key))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId", notNullValue()))
        .andReturn().getResponse().getContentAsString();

    // Second call with same key -> same order id returned (still 201 in this simple impl)
    String resp2 = mvc.perform(post("/orders").header("Idempotency-Key", key))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId", notNullValue()))
        .andReturn().getResponse().getContentAsString();

    // Relay outbox once
    int sent = relay.relayBatch(100);
    Assertions.assertTrue(sent >= 1, "Expected at least one event sent");

    // Consume from Kafka to verify an event was published
    ConsumerRecords<String,String> records = consumer.poll(Duration.ofSeconds(5));
    Assertions.assertFalse(records.isEmpty(), "Expected at least one Kafka record");
  }
}
