package ourpkg.attribute;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.category.Category2;
import ourpkg.category.repo.Category2Repository;
import ourpkg.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class AttributeService {

	private final AttributeRepo attributeRepo;
	private final Category2Repository category2Repo;
	private final AttributeMapper attributeMapper;

	public List<AttributeDTO> getAllAttributes() {
		return attributeRepo.findAll().stream().map(e -> attributeMapper.toDto(e)).collect(Collectors.toList());
	}

	public AttributeDTO getAttributeById(Integer id) {
		Attribute attribute = attributeRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("屬性不存在"));
		return attributeMapper.toDto(attribute);
	}

	public AttributeDTO createAttribute(AttributeDTO attributeDTO) {
		Attribute attribute = attributeMapper.toEntity(attributeDTO);

		// **查詢 category2Ids 並設定到 attribute**
		// 新增時 -- null: 不行!設為 new ArrayList<>() []: 設為 new ArrayList<>()
		if (attributeDTO.getCategory2Ids() != null && !attributeDTO.getCategory2Ids().isEmpty()) {
			List<Category2> category2List = category2Repo.findAllById(attributeDTO.getCategory2Ids());
			attribute.setCategory2List(category2List);
		}

		Attribute savedAttribute = attributeRepo.save(attribute);
		return attributeMapper.toDto(savedAttribute);
	}

	public AttributeDTO updateAttribute(Integer id, AttributeDTO attributeDTO) {
		Attribute attribute = attributeRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("屬性不存在"));

		attribute.setName(attributeDTO.getName()); // 更新名稱

		// **更新 category2List** 更新關聯
		// 更新時 -- null:不變更 []:清空關聯
		if (attributeDTO.getCategory2Ids() != null) {
			List<Category2> category2List = category2Repo.findAllById(attributeDTO.getCategory2Ids());
			attribute.setCategory2List(category2List);
		}

		Attribute updatedAttribute = attributeRepo.save(attribute);
		return attributeMapper.toDto(updatedAttribute);
	}

	public void deleteAttribute(Integer id) {
		if (!attributeRepo.existsById(id)) {
			throw new ResourceNotFoundException("屬性不存在");
		}
		attributeRepo.deleteById(id);
	}

	public List<AttributeDTO> getAttributesByCategory2(Integer category2Id) {
		if (!category2Repo.existsById(category2Id)) {
			throw new ResourceNotFoundException("二級分類不存在");
		}

		List<Attribute> attributes = attributeRepo.findByCategory2List_Id(category2Id);
		return attributes.stream().map(e -> attributeMapper.toDto(e)).collect(Collectors.toList());
	}

}
