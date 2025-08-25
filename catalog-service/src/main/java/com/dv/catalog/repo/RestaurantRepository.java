package com.dv.catalog.repo;
import com.dv.catalog.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {}
