package ourpkg.product.version2.product_sales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuSalesDTO {

	private Integer skuId;
    private Integer productId;
    private String productName;
    private String specPairs;
    private Integer soldCount;
}
