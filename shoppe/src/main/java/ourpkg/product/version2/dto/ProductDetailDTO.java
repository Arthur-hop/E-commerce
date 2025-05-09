package ourpkg.product.version2.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.sku.version2.dto.SkuResDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {

	private ProductResDTO product;
    private List<SkuResDTO> skus;
    private List<ProductSpecDTO> specifications;
}
