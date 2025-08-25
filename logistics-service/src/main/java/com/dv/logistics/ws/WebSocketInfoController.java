package com.dv.logistics.ws;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws-info")
@Tag(name = "WebSocket", description = "Live tracking WebSocket information")
public class WebSocketInfoController {
  @Operation(summary = "WebSocket details", description = "Connect to /ws using STOMP/SockJS and subscribe to /topic/track/{orderId}")
  @GetMapping
  public ResponseEntity<?> info(){
    return ResponseEntity.ok(new Info("ws", "/topic/track/{orderId}"));
  }
  public record Info(String endpoint, String topic) {}
}
