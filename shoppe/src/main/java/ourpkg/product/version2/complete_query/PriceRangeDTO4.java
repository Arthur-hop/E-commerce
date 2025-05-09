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
public class PriceRangeDTO4 {

	private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
