package com.dv.catalog.web;
import com.dv.catalog.repo.MenuItemRepository;
import com.dv.catalog.domain.MenuItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
@RestController
@RequestMapping("/restaurants")
public class MenuController {
  private final MenuItemRepository repo;
  public MenuController(MenuItemRepository repo){ this.repo = repo; }
  @GetMapping("/{id}/menu")
  public ResponseEntity<List<MenuItem>> menu(@PathVariable UUID id, @RequestHeader(name="If-None-Match", required=false) String inm) {
    List<MenuItem> items = repo.findByRestaurantId(id);
    String etag = computeEtag(items);
    if (inm != null && inm.equals(etag)) return ResponseEntity.status(304).eTag(etag).build();
    return ResponseEntity.ok().eTag(etag).body(items);
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
