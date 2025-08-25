package com.dv.order.app;
import com.dv.common.domain.OrderId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class OrderService {
  @Transactional
  public OrderId createOrder(String idempotencyKey) {
    // TODO: persist order + outbox within TX; for now return deterministic UUID from key
    String id = UUID.nameUUIDFromBytes(idempotencyKey.getBytes()).toString();
    return new OrderId(id);
  }
}
