package ourpkg.shipment;


import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Integer>{

	boolean existsByShipmentMethod_Id(Integer id);

	boolean existsByShipmentStatus_Id(Integer id);

}

