package ourpkg.product.version2.complete_query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesInfoDTO4 {

	private Integer totalSoldCount;
    private BigDecimal totalSalesAmount;
    private Integer totalViewCount;
    private Double conversionRate;
    private List<SkuSalesDTO4> skuSalesDetails;
    private Map<String, Integer> dailySalesLast30Days;
    private LocalDateTime lastUpdated;
}
