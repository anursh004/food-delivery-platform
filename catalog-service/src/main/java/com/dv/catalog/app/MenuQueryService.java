package com.dv.catalog.app;

import com.dv.catalog.domain.MenuItem;
import com.dv.catalog.repo.MenuItemRepository;
import com.dv.catalog.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MenuQueryService {
  private final MenuItemRepository repo;
  public MenuQueryService(MenuItemRepository repo){ this.repo = repo; }

  @Cacheable(cacheNames = CacheConfig.MENU_CACHE, key = "#restaurantId")
  public List<MenuItem> getMenu(UUID restaurantId){
    return repo.findByRestaurantId(restaurantId);
  }

  @CacheEvict(cacheNames = CacheConfig.MENU_CACHE, key = "#restaurantId")
  public void evictMenu(UUID restaurantId){ /* no-op */ }
}
