package ourpkg.shipment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentMethodRepository extends JpaRepository<ShipmentMethod, Integer>{
    Optional<ShipmentMethod> findByName(String name);

}
