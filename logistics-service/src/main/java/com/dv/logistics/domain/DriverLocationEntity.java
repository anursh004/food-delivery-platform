package com.dv.logistics.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_locations")
public class DriverLocationEntity {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name="order_id", columnDefinition = "uuid")
  private UUID orderId;

  private double lat;
  private double lon;

  @Column(name="updated_at")
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public UUID getOrderId(){ return orderId; }
  public void setOrderId(UUID orderId){ this.orderId=orderId; }
  public double getLat(){ return lat; }
  public void setLat(double lat){ this.lat=lat; }
  public double getLon(){ return lon; }
  public void setLon(double lon){ this.lon=lon; }
  public OffsetDateTime getUpdatedAt(){ return updatedAt; }
  public void setUpdatedAt(OffsetDateTime u){ this.updatedAt=u; }
}
