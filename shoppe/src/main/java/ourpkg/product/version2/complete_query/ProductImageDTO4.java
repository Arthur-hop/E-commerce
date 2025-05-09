package ourpkg.product.version2.complete_query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDTO4 {

	private Integer imageId;
    private String imagePath;
    private Boolean isPrimary;
    private Integer displayOrder;
}
