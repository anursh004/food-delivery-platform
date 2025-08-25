package com.dv.catalog.web;
import com.dv.catalog.app.MenuQueryService;
import com.dv.catalog.domain.MenuItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants")
@Tag(name = "Catalog", description = "Restaurants and Menus")
public class MenuController {
  private final MenuQueryService service;
  public MenuController(MenuQueryService service){ this.service = service; }

  @Operation(summary = "Get restaurant menu", description = "Returns menu items with ETag for caching. Supports If-None-Match → 304.")
  @GetMapping("/{id}/menu")
  public ResponseEntity<List<MenuItem>> menu(@PathVariable UUID id, @RequestHeader(name="If-None-Match", required=false) String inm) {
    List<MenuItem> items = service.getMenu(id);
    String etag = computeEtag(items);
    if (inm != null && inm.equals(etag)) return ResponseEntity.status(304).eTag(etag).build();
    return ResponseEntity.ok().eTag(etag).body(items);
  }

  @Operation(summary = "Refresh menu cache", description = "Evicts L1+L2 caches for the restaurant id.")
  @PostMapping("/{id}/menu/_refresh")
  public ResponseEntity<Void> refresh(@PathVariable UUID id) {
    service.evictMenu(id);
    return ResponseEntity.accepted().build();
  }

  private String computeEtag(List<MenuItem> items){
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      for (MenuItem i: items){
        md.update((i.getItemName()+i.getPrice()+i.isAvailable()).getBytes(StandardCharsets.UTF_8));
      }
      return '"' + Base64.getEncoder().encodeToString(md.digest()) + '"';
    } catch(Exception e){ return ""0""; }
  }
}
