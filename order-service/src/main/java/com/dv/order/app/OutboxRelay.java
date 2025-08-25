package com.dv.order.app;
import com.dv.order.domain.OutboxEventEntity;
import com.dv.order.repo.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxRelay {
  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
  private final OutboxRepository outbox;
  private final KafkaTemplate<String, String> kafka;
  private final String topic;

  public OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka,
                     @Value("${app.kafka.ordersCreatedTopic:orders.created}") String topic) {
    this.outbox = outbox; this.kafka = kafka; this.topic = topic;
  }

  @Scheduled(fixedDelayString = "PT5S")
  @Transactional
  public void scheduledRelay() {
    relayBatch(100);
  }

  @Transactional
  public int relayBatch(int limit) {
    List<OutboxEventEntity> events = outbox.fetchBatchForUpdate(limit);
    int sent = 0;
    for (OutboxEventEntity ev : events) {
      try {
        kafka.send(new ProducerRecord<>(topic, ev.getAggregateId().toString(), ev.getPayload())).get();
        ev.setStatus("PUBLISHED");
        ev.setPublishedAt(OffsetDateTime.now());
        sent++;
      } catch (Exception ex) {
        log.error("Failed to publish outbox event {}", ev.getId(), ex);
        ev.setStatus("FAILED");
      }
    }
    return sent;
  }
}
