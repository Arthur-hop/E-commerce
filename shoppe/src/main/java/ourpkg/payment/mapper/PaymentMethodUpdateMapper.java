package ourpkg.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import ourpkg.payment.PaymentMethod;
import ourpkg.payment.dto.PaymentMethodUpdateDTO;

@Mapper(componentModel = "spring")
public interface PaymentMethodUpdateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "paymentList", ignore = true)
	PaymentMethod toEntity(@MappingTarget PaymentMethod paymentMethod, PaymentMethodUpdateDTO dto);
}
