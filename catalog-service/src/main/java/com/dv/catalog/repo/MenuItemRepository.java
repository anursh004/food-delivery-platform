package com.dv.catalog.repo;
import com.dv.catalog.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
  List<MenuItem> findByRestaurantId(UUID restaurantId);
}
