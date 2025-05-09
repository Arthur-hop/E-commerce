package ourpkg.shipment.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ourpkg.shipment.ShipmentMethod;
import ourpkg.shipment.dto.ShipmentMethodResDTO;

@Mapper(componentModel = "spring")
public interface ShipmentMethodResMapper {

	ShipmentMethodResDTO toDto(ShipmentMethod entity);

	List<ShipmentMethodResDTO> toDtoList(List<ShipmentMethod> entityList);
	
}
