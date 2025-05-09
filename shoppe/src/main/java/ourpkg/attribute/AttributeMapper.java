package ourpkg.attribute;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class AttributeMapper {

	public AttributeDTO toDto(Attribute attribute) {
		return new AttributeDTO(attribute.getId(), attribute.getName(),
				attribute.getCategory2List().stream().map(e -> e.getId()) // 只接收&返回二級分類的 id(不是物件喔!) 列表
						.collect(Collectors.toList()));
	}

	public Attribute toEntity(AttributeDTO dto) {
		return new Attribute(
				dto.getId(), 
				dto.getName(), 
				new ArrayList<>(), // category2List 需要在Service內處理
				new ArrayList<>()); // attributevalueList 需要在Service內處理
	}

}
