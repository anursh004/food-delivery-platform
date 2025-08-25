package com.dv.order.web;
import com.dv.common.domain.OrderId;
import com.dv.order.app.OrderService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
@RestController
@RequestMapping("/orders")
public class OrderController {
  private final OrderService orderService;
  public OrderController(OrderService orderService){ this.orderService = orderService; }
  @PostMapping
  public ResponseEntity<?> create(@RequestHeader("Idempotency-Key") @NotBlank String key){
    OrderId id = orderService.createOrder(key);
    return ResponseEntity.created(URI.create("/orders/" + id.get())).body(new CreateOrderResponse(id.get()));
  }
  public record CreateOrderResponse(String orderId) {}
}
