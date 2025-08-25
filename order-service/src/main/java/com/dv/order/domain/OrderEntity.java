package com.dv.order.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="orders", uniqueConstraints = @UniqueConstraint(name="uk_orders_idempotency_key", columnNames = "idempotency_key"))
public class OrderEntity {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name="customer_id")
  private String customerId;

  @Column(name="restaurant_id")
  private String restaurantId;

  @Column(name="total_amount")
  private java.math.BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  @Column(name="idempotency_key", nullable=false, length=128)
  private String idempotencyKey;

  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }

  public String getCustomerId(){ return customerId; }
  public void setCustomerId(String c){ this.customerId=c; }

  public String getRestaurantId(){ return restaurantId; }
  public void setRestaurantId(String r){ this.restaurantId=r; }

  public java.math.BigDecimal getTotalAmount(){ return totalAmount; }
  public void setTotalAmount(java.math.BigDecimal t){ this.totalAmount=t; }

  public OrderStatus getStatus(){ return status; }
  public void setStatus(OrderStatus s){ this.status=s; }

  public String getIdempotencyKey(){ return idempotencyKey; }
  public void setIdempotencyKey(String k){ this.idempotencyKey=k; }

  public OffsetDateTime getCreatedAt(){ return createdAt; }
  public void setCreatedAt(OffsetDateTime t){ this.createdAt=t; }
}
