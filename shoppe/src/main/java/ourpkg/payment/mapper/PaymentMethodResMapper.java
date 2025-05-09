package ourpkg.payment.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ourpkg.payment.PaymentMethod;
import ourpkg.payment.dto.PaymentMethodResDTO;

@Mapper(componentModel = "spring")
public interface PaymentMethodResMapper {

	PaymentMethodResDTO toDto(PaymentMethod entity);

	List<PaymentMethodResDTO> toDtoList(List<PaymentMethod> entityList);
	
}
