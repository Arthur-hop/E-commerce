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
public class ProductSpecDTO3 {

	private String specName;
    private List<SpecValueDTO3> values;
}
