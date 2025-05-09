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
public class ProductCompleteUpdateDTO3 {

	private String productName;
    private String description;
    private Boolean active;
    private List<Integer> deleteImageIds;
    private Integer primaryImageId;
    private List<SkuUpdateDTO3> updateSkus;
    private List<SkuCreateDTO3> createSkus;
    private List<Integer> deleteSkuIds;
}
