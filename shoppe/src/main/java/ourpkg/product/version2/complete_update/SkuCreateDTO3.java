package ourpkg.product.version2.complete_update;

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
public class SkuCreateDTO3 {

	private BigDecimal price;
    private Integer stock;
    private Map<String, String> specPairs;
}
