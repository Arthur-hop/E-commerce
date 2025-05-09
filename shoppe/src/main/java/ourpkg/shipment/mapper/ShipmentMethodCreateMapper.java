package ourpkg.shipment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ourpkg.shipment.ShipmentMethod;
import ourpkg.shipment.dto.ShipmentMethodCreateDTO;

@Mapper(componentModel = "spring")
public interface ShipmentMethodCreateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "shipmentList", ignore = true)
	ShipmentMethod toEntity(ShipmentMethodCreateDTO dto);
}

