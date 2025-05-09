package ourpkg.attribute;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class AttributeValueService {

	private final AttributeValueRepo attributeValueRepo;
	private final AttributeRepo attributeRepo;
	private final AttributeValueMapper attributeValueMapper;

	public List<AttributeValueDTO> getAllAttributeValues(Integer attributeId) {
		if (!attributeRepo.existsById(attributeId)) {
			throw new ResourceNotFoundException("屬性不存在");
		}

		return attributeValueRepo.findByAttribute_Id(attributeId).stream().map(e -> attributeValueMapper.toDto(e))
				.collect(Collectors.toList());
	}

	public AttributeValueDTO getAttributeValue(Integer attributeId, Integer valueId) {
		// 直接判斷屬性值存在，不用先判斷屬性存在(如果屬性值存在，屬性一定存在)(在我的設計中，新增一筆屬性值時，一定要掛在某個屬性下)
		AttributeValue attributeValue = attributeValueRepo.findById(valueId)
				.orElseThrow(() -> new ResourceNotFoundException("屬性值不存在"));

		if (!attributeValue.getAttribute().getId().equals(attributeId)) {
			throw new IllegalArgumentException("屬性值不屬於該屬性，請確認輸入的參數是否正確");
		}

		return attributeValueMapper.toDto(attributeValue);
	}

	public AttributeValueDTO createAttributeValue(Integer attributeId, AttributeValueDTO dto) {
		Attribute attribute = attributeRepo.findById(attributeId)
				.orElseThrow(() -> new ResourceNotFoundException("屬性不存在"));

		AttributeValue attributeValue = attributeValueMapper.toEntity(dto, attribute);
		AttributeValue savedValue = attributeValueRepo.save(attributeValue);

		return attributeValueMapper.toDto(savedValue);
	}

	public AttributeValueDTO updateAttributeValue(Integer attributeId, Integer valueId, AttributeValueDTO dto) {
		AttributeValue attributeValue = attributeValueRepo.findById(valueId)
				.orElseThrow(() -> new ResourceNotFoundException("屬性值不存在"));

		if (!attributeValue.getAttribute().getId().equals(attributeId)) {
			throw new IllegalArgumentException("屬性值不屬於該屬性");
		}

		attributeValue.setName(dto.getName());
		AttributeValue updatedValue = attributeValueRepo.save(attributeValue);

		return attributeValueMapper.toDto(updatedValue);

	}

	public void deleteAttributeValue(Integer attributeId, Integer valueId) {
		AttributeValue attributeValue = attributeValueRepo.findById(valueId)
				.orElseThrow(() -> new ResourceNotFoundException("屬性值不存在"));

		if (!attributeValue.getAttribute().getId().equals(attributeId)) {
			throw new IllegalArgumentException("屬性值不屬於該屬性");
		}

		attributeValueRepo.delete(attributeValue);
	}

}
