package ourpkg.shipment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import ourpkg.shipment.ShipmentMethod;
import ourpkg.shipment.dto.ShipmentMethodUpdateDTO;

@Mapper(componentModel = "spring")
public interface ShipmentMethodUpdateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "shipmentList", ignore = true)
	ShipmentMethod toEntity(@MappingTarget ShipmentMethod shipmentMethod, ShipmentMethodUpdateDTO dto);
}
