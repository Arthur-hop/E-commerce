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
public class ProductSpecDTO2 {

	private String specName;
    private List<SpecValueDTO2> values;
}
