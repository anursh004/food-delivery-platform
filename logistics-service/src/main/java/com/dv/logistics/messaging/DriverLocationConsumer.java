package com.dv.logistics.messaging;

import com.dv.logistics.domain.DriverLocationEntity;
import com.dv.logistics.repo.DriverLocationRepository;
import com.dv.logistics.ws.LocationBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DriverLocationConsumer {
  private static final Logger log = LoggerFactory.getLogger(DriverLocationConsumer.class);
  private final ObjectMapper om;
  private final DriverLocationRepository repo;
  private final LocationBroadcaster broadcaster;

  public DriverLocationConsumer(ObjectMapper om, DriverLocationRepository repo, LocationBroadcaster broadcaster){
    this.om = om; this.repo = repo; this.broadcaster = broadcaster;
  }

  @KafkaListener(topics = "${app.kafka.driverLocationsTopic:driver.locations}", groupId = "${spring.application.name}", batch = true)
  @Transactional
  public void onBatch(List<ConsumerRecord<String,String>> records, Acknowledgment ack){
    List<DriverLocationEntity> batch = new ArrayList<>(records.size());
    for (ConsumerRecord<String,String> rec : records){
      try {
        DriverLocationEvent evt = om.readValue(rec.value(), DriverLocationEvent.class);
        DriverLocationEntity e = new DriverLocationEntity();
        e.setId(UUID.randomUUID());
        e.setOrderId(evt.orderId);
        e.setLat(evt.lat);
        e.setLon(evt.lon);
        e.setUpdatedAt(evt.updatedAt != null ? evt.updatedAt : OffsetDateTime.now());
        batch.add(e);
        // Async broadcast to WS subscribers
        broadcaster.broadcast(evt);
      } catch (Exception ex){
        log.error("Failed to parse message {}", rec.value(), ex);
      }
    }
    if (!batch.isEmpty()) {
      repo.saveAll(batch);
    }
    if (ack != null) ack.acknowledge();
  }
}
