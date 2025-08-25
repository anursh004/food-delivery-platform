package com.dv.order.app;
import com.dv.common.domain.OrderId;
import com.dv.order.domain.OrderEntity;
import com.dv.order.domain.OrderStatus;
import com.dv.order.domain.OutboxEventEntity;
import com.dv.order.repo.OrderRepository;
import com.dv.order.repo.OutboxRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
  private final OrderRepository orders;
  private final OutboxRepository outbox;

  public OrderService(OrderRepository orders, OutboxRepository outbox) {
    this.orders = orders; this.outbox = outbox;
  }

  @Transactional
  public OrderId createOrder(String idempotencyKey) {
    Optional<OrderEntity> existing = orders.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) return new OrderId(existing.get().getId().toString());

    try {
      UUID orderId = UUID.randomUUID();
      OrderEntity e = new OrderEntity();
      e.setId(orderId);
      e.setStatus(OrderStatus.CREATED);
      e.setIdempotencyKey(idempotencyKey);
      e.setTotalAmount(new BigDecimal("0.00"));
      e.setCreatedAt(OffsetDateTime.now());
      orders.saveAndFlush(e);

      OutboxEventEntity ev = new OutboxEventEntity();
      ev.setId(UUID.randomUUID());
      ev.setAggregateId(orderId);
      ev.setType("ORDER_CREATED");
      String payload = "{\"orderId\":\"" + orderId + "\"}";
      ev.setPayload(payload);
      outbox.save(ev);

      return new OrderId(orderId.toString());
    } catch (DataIntegrityViolationException dive) {
      // Unique idempotency_key constraint hit: return existing
      OrderEntity ex = orders.findByIdempotencyKey(idempotencyKey)
                             .orElseThrow(() -> dive);
      return new OrderId(ex.getId().toString());
    }
  }
}
