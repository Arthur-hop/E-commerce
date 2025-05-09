package ourpkg.shipment.dto;

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
public class ShipmentStatusCreateDTO {

	@NotBlank(message = "配送狀態名稱不能為空")
    @Size(max = 100, message = "配送狀態名稱長度不能超過 100 個字元")
	private String name;
}
