package ourpkg.attribute;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/attributes/{attributeId}/attribute-values")
@RequiredArgsConstructor
public class AttributeValueController {

	private final AttributeValueService attributeValueService;

	// 查詢 指定屬性下的 所有 屬性值（管理員、賣家）
	@GetMapping
	public ResponseEntity<List<AttributeValueDTO>> getAll(@PathVariable Integer attributeId) {
		return ResponseEntity.ok(attributeValueService.getAllAttributeValues(attributeId));
	}

	// 查詢 指定屬性下的 單一 屬性值（管理員）
	@GetMapping("/{valueId}")
	public ResponseEntity<AttributeValueDTO> get(@PathVariable Integer attributeId, @PathVariable Integer valueId) {
		return ResponseEntity.ok(attributeValueService.getAttributeValue(attributeId, valueId));
	}

	// 新增 指定屬性下的 屬性值（管理員）
	@PostMapping
	public ResponseEntity<AttributeValueDTO> create(@PathVariable Integer attributeId,
			@RequestBody AttributeValueDTO dto) {
		return ResponseEntity.ok(attributeValueService.createAttributeValue(attributeId, dto));
	}

	// 更新 指定屬性下的 屬性值（管理員）
	@PutMapping("/{valueId}")
	public ResponseEntity<AttributeValueDTO> update(@PathVariable Integer attributeId, @PathVariable Integer valueId,
			@RequestBody AttributeValueDTO dto) {
		return ResponseEntity.ok(attributeValueService.updateAttributeValue(attributeId, valueId, dto));
	}

	// 刪除 指定屬性下的 屬性值（管理員）
	@DeleteMapping("/{valueId}")
	public ResponseEntity<Void> delete(@PathVariable Integer attributeId, @PathVariable Integer valueId) {
		attributeValueService.deleteAttributeValue(attributeId, valueId);
		return ResponseEntity.noContent().build();
	}

}
