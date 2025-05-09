package ourpkg.attribute;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

@Component
public class AttributeValueMapper {

	public AttributeValueDTO toDto(AttributeValue entity) {
		return new AttributeValueDTO(entity.getId(), entity.getName(), entity.getAttribute().getId());
	}

	public AttributeValue toEntity(AttributeValueDTO dto, Attribute attribute) {
		return new AttributeValue(
				dto.getId(), 
				dto.getName(), 
				attribute, // attribute 需要在Service內處理(DTO只有id，需要先在service 透過id查詢物件，再呼叫toEntity)
				new ArrayList<>()); // product 需要在Service內處理
	}

}
