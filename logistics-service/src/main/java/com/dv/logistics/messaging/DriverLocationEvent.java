package com.dv.logistics.messaging;
import java.time.OffsetDateTime;
import java.util.UUID;
public class DriverLocationEvent {
  public UUID orderId;
  public double lat;
  public double lon;
  public OffsetDateTime updatedAt;
  public DriverLocationEvent(){}
  public DriverLocationEvent(UUID orderId, double lat, double lon, OffsetDateTime updatedAt){
    this.orderId=orderId; this.lat=lat; this.lon=lon; this.updatedAt=updatedAt;
  }
}
