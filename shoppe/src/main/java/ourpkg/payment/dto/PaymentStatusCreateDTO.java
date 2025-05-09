package ourpkg.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusCreateDTO {

	@NotBlank(message = "支付狀態名稱不能為空")
    @Size(max = 100, message = "支付狀態名稱長度不能超過 100 個字元")
	private String name;
}
