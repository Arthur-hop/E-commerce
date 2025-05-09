package ourpkg.product.version2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateDTO {

	@NotBlank(message = "商品名稱不能為空")
	@Size(min = 2, max = 100, message = "商品名稱長度必須在2到100個字符之間")
	private String productName;

	@Size(max = 2000, message = "商品描述不能超過2000個字符")
	private String description;

	@NotNull(message = "一級分類ID不能為空")
	private Integer category1Id;

	@NotNull(message = "二級分類ID不能為空")
	private Integer category2Id;

	private Boolean active = false;

}