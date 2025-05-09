package ourpkg.shipment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import ourpkg.shipment.ShipmentStatus;
import ourpkg.shipment.dto.ShipmentStatusUpdateDTO;

@Mapper(componentModel = "spring")
public interface ShipmentStatusUpdateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "shipmentList", ignore = true)
	ShipmentStatus toEntity(@MappingTarget ShipmentStatus shipmentStatus, ShipmentStatusUpdateDTO dto);
}
