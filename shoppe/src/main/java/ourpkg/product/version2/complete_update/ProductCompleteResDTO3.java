package ourpkg.product.version2.complete_update;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompleteResDTO3 {

	private ProductResDTO3 product;
    private List<SkuResDTO3> skus;
    private List<ProductSpecDTO3> specifications;
}
