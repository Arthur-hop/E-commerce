package ourpkg.shipment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentStatusRepository extends JpaRepository<ShipmentStatus, Integer>{
    Optional<ShipmentStatus> findByName(String name);

}
