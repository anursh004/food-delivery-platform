package com.dv.sim;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DriverSimulatorApplication implements CommandLineRunner {
  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper om = new ObjectMapper();

  @Value("${app.kafka.bootstrap-servers:kafka:9092}")
  String bootstrap;
  @Value("${app.topic:driver.locations}")
  String topic;
  @Value("${sim.drivers:50}")
  int drivers;
  @Value("${sim.periodMs:5000}")
  int periodMs;

  public DriverSimulatorApplication(KafkaTemplate<String, String> kafka){
    this.kafka = kafka;
  }

  public static void main(String[] args){ SpringApplication.run(DriverSimulatorApplication.class, args); }

  @Override
  public void run(String... args) throws Exception {
    ScheduledExecutorService ses = Executors.newScheduledThreadPool(Math.min(8, drivers));
    Random rnd = new Random();
    UUID[] ids = new UUID[drivers];
    for (int i=0;i<drivers;i++) ids[i] = UUID.randomUUID();
    for (int i=0;i<drivers;i++){
      final int idx = i;
      ses.scheduleAtFixedRate(() -> {
        try {
          UUID orderId = ids[idx];
          double lat = 18.5 + rnd.nextDouble();
          double lon = 73.7 + rnd.nextDouble();
          String json = om.writeValueAsString(new Event(orderId, lat, lon, OffsetDateTime.now()));
          kafka.send(new ProducerRecord<>(topic, orderId.toString(), json));
        } catch (Exception e){ e.printStackTrace(); }
      }, 1000, periodMs, TimeUnit.MILLISECONDS);
    }
  }

  public record Event(UUID orderId, double lat, double lon, OffsetDateTime updatedAt){}
}
