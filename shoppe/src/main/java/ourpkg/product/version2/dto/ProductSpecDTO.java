package ourpkg.product.version2.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecDTO {

	private String specName;  // 例如："顏色", "尺寸"
    private List<SpecValueDTO> values;
}
