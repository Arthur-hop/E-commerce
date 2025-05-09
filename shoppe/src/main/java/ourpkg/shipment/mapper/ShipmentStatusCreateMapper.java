package ourpkg.shipment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ourpkg.shipment.ShipmentStatus;
import ourpkg.shipment.dto.ShipmentStatusCreateDTO;

@Mapper(componentModel = "spring")
public interface ShipmentStatusCreateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "shipmentList", ignore = true)
	ShipmentStatus toEntity(ShipmentStatusCreateDTO dto);
}
