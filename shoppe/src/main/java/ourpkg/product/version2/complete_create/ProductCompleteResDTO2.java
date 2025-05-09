package ourpkg.product.version2.complete_create;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompleteResDTO2 {

	private ProductResDTO2 product;
    private List<SkuResDTO2> skus;
    private List<ProductSpecDTO2> specifications;
}
