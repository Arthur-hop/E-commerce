package ourpkg.product.version2.complete_create;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuResDTO2 {

	private Integer skuId;
	private Integer productId;
	private String productName;
	private Integer stock;
	private BigDecimal price;
	private Map<String, String> specPairs;
	private String specDescription;
	private Boolean isDeleted;
}
