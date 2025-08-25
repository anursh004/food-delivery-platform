package com.dv.order.web;
import com.dv.order.domain.OrderEntity;
import com.dv.order.repo.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/orders")
@Tag(name = "Orders")
public class OrderQueryController {
  private final OrderRepository repo;
  public OrderQueryController(OrderRepository repo){ this.repo = repo; }

  @Operation(summary="Get order status")
  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable UUID id){
    return repo.findById(id)
        .map(e -> ResponseEntity.ok(new OrderView(e.getId().toString(), e.getStatus().name())))
        .orElse(ResponseEntity.notFound().build());
  }
  public record OrderView(String id, String status){}
}
