package com.dv.order.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="outbox_events")
public class OutboxEventEntity {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name="aggregate_id", columnDefinition = "uuid")
  private UUID aggregateId;

  private String type;

  @Column(columnDefinition = "text")
  private String payload; // JSON

  private String status = "NEW";

  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name="published_at")
  private OffsetDateTime publishedAt;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id = id; }

  public UUID getAggregateId(){ return aggregateId; }
  public void setAggregateId(UUID a){ this.aggregateId = a; }

  public String getType(){ return type; }
  public void setType(String t){ this.type=t; }

  public String getPayload(){ return payload; }
  public void setPayload(String p){ this.payload=p; }

  public String getStatus(){ return status; }
  public void setStatus(String s){ this.status=s; }

  public OffsetDateTime getCreatedAt(){ return createdAt; }
  public void setCreatedAt(OffsetDateTime c){ this.createdAt=c; }

  public OffsetDateTime getPublishedAt(){ return publishedAt; }
  public void setPublishedAt(OffsetDateTime p){ this.publishedAt=p; }
}
