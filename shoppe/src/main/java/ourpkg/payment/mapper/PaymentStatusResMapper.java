package ourpkg.payment.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ourpkg.payment.PaymentStatus;
import ourpkg.payment.dto.PaymentStatusResDTO;

@Mapper(componentModel = "spring")
public interface PaymentStatusResMapper {

	PaymentStatusResDTO toDto(PaymentStatus entity);

	List<PaymentStatusResDTO> toDtoList(List<PaymentStatus> entityList);
}
