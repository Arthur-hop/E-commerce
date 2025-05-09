package ourpkg.attribute;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDTO {

	private Integer id;
	private String name;
	private List<Integer> category2Ids; // 只接收&返回二級分類的 id(不是物件喔!) 列表
	
	
}
