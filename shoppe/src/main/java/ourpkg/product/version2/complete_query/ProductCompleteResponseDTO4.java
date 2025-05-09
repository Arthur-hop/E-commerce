package ourpkg.product.version2.complete_query;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompleteResponseDTO4 {

	private ProductBasicInfoDTO4 basicInfo;
    private ShopInfoDTO4 shop;
    private List<ProductImageDTO4> images;
    private ProductImageDTO4 primaryImage;
    private List<SkuInfoDTO4> skus;
    private List<ProductSpecDTO4> specifications;
    private int totalStock;
    private SalesInfoDTO4 salesInfo;
}
