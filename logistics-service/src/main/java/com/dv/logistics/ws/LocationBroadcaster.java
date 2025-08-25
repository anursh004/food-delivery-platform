package com.dv.logistics.ws;
import com.dv.logistics.messaging.DriverLocationEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class LocationBroadcaster {
  private final SimpMessagingTemplate template;
  public LocationBroadcaster(SimpMessagingTemplate template){ this.template=template; }
  public void broadcast(DriverLocationEvent evt){
    String destination = "/topic/track/" + evt.orderId;
    template.convertAndSend(destination, evt);
  }
}
