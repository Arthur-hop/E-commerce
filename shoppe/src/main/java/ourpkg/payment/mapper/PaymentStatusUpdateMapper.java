package ourpkg.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import ourpkg.payment.PaymentStatus;
import ourpkg.payment.dto.PaymentStatusUpdateDTO;

@Mapper(componentModel = "spring")
public interface PaymentStatusUpdateMapper {

	@Mapping(target = "id", ignore = true)
//	@Mapping(target = "paymentList", ignore = true)
	PaymentStatus toEntity(@MappingTarget PaymentStatus paymentStatus, PaymentStatusUpdateDTO dto);
}
