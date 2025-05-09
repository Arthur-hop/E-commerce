package ourpkg.product.version2.complete_query;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuSalesDTO4 {

	private Integer skuId;
    private Integer soldCount;
    private BigDecimal totalAmount;
    private BigDecimal currentPrice;
    private Integer stockRemaining;
}
