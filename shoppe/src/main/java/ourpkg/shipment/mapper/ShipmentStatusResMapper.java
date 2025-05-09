package ourpkg.shipment.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ourpkg.shipment.ShipmentStatus;
import ourpkg.shipment.dto.ShipmentStatusResDTO;

@Mapper(componentModel = "spring")
public interface ShipmentStatusResMapper {

	ShipmentStatusResDTO toDto(ShipmentStatus entity);

	List<ShipmentStatusResDTO> toDtoList(List<ShipmentStatus> entityList);
}
