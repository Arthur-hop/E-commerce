package ourpkg.product.version2.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDTO {

	@Size(min = 2, max = 100, message = "商品名稱長度必須在2到100個字符之間")
	private String productName;

	@Size(max = 2000, message = "商品描述不能超過2000個字符")
	private String description;

	private Boolean active;

}
