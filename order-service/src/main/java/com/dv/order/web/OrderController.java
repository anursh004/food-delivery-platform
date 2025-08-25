package com.dv.order.web;
import com.dv.common.domain.OrderId;
import com.dv.order.app.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order creation and management")
public class OrderController {
  private final OrderService orderService;
  public OrderController(OrderService orderService){ this.orderService = orderService; }

  @Operation(summary = "Create an order", description = "Idempotent order creation using Idempotency-Key header.")
  @PostMapping
  public ResponseEntity<?> create(@RequestHeader("Idempotency-Key") @NotBlank String key){
    OrderId id = orderService.createOrder(key);
    return ResponseEntity.created(URI.create("/orders/" + id.get())).body(new CreateOrderResponse(id.get()));
  }
  public record CreateOrderResponse(String orderId) {}
}
