package ourpkg.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ourpkg.payment.PaymentStatus;
import ourpkg.payment.dto.PaymentStatusCreateDTO;

@Mapper(componentModel = "spring")
public interface PaymentStatusCreateMapper {

	@Mapping(target = "id", ignore = true)
//	@Mapping(target = "paymentList", ignore = true)
	PaymentStatus toEntity(PaymentStatusCreateDTO dto);
}
