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
public class ProductSpecDTO4 {
	
	private String specName;
    private List<SpecValueDTO4> values;
}
