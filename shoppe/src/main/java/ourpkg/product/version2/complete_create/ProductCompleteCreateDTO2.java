package ourpkg.product.version2.complete_create;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.sku.version2.dto.SkuCreateDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompleteCreateDTO2 {

private String productName;
    
    private String description;
    
    private Integer category1Id;
    
    private Integer category2Id;
    
    private Boolean active = false;
    
    private List<SkuCreateDTO2> skus;
}
