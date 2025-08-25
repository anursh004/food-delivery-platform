package com.dv.order.repo;
import com.dv.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
  Optional<OrderEntity> findByIdempotencyKey(String key);
}
