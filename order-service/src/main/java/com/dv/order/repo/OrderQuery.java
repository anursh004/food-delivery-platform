package com.dv.order.repo;
import com.dv.order.domain.OrderEntity;
import com.dv.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;
public interface OrderQuery extends JpaRepository<OrderEntity, UUID> {
  @Query("select o from OrderEntity o where o.status = com.dv.order.domain.OrderStatus.CREATED order by o.createdAt")
  List<OrderEntity> findCreated();
}
