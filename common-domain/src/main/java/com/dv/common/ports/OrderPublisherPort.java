package com.dv.common.ports;
import com.dv.common.domain.OrderId;
public interface OrderPublisherPort {
  void publishOrderCreated(OrderId orderId);
}
