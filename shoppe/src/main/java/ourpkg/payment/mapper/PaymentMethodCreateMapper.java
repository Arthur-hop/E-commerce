package ourpkg.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ourpkg.payment.PaymentMethod;
import ourpkg.payment.dto.PaymentMethodCreateDTO;

@Mapper(componentModel = "spring")
public interface PaymentMethodCreateMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "paymentList", ignore = true)
	PaymentMethod toEntity(PaymentMethodCreateDTO dto);
}

