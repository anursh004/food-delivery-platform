package com.dv.logistics.repo;
import com.dv.logistics.domain.DriverLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface DriverLocationRepository extends JpaRepository<DriverLocationEntity, UUID> {
  List<DriverLocationEntity> findTop10ByOrderIdOrderByUpdatedAtDesc(UUID orderId);
}
