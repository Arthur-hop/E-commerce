package ourpkg.attribute;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attributes")
public class AttributeController {

	@Autowired
	private AttributeService attributeService;

	// 查詢 所有 屬性（管理員）
	@GetMapping
	public ResponseEntity<List<AttributeDTO>> getAllAttributes() {
		List<AttributeDTO> attributes = attributeService.getAllAttributes();
		return ResponseEntity.ok(attributes);
	}
	
	// 查詢 單一 屬性（管理員）
	@GetMapping("/{id}")
	public ResponseEntity<AttributeDTO> getAttributeById(@PathVariable Integer id) {
		AttributeDTO attribute = attributeService.getAttributeById(id);
		return ResponseEntity.ok(attribute);
	}

	// 新增 屬性（管理員）
	@PostMapping
	public ResponseEntity<AttributeDTO> createAttribute(@RequestBody AttributeDTO attributeDTO) {
		AttributeDTO createdAttribute = attributeService.createAttribute(attributeDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdAttribute);
	}

	// 更新 屬性（管理員）
	@PutMapping("/{id}")
	public ResponseEntity<AttributeDTO> updateAttribute(@PathVariable Integer id,
			@RequestBody AttributeDTO attributeDTO) {
		AttributeDTO updatedAttribute = attributeService.updateAttribute(id, attributeDTO);
		return ResponseEntity.ok(updatedAttribute);
	}

	// 刪除 屬性（管理員）
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAttribute(@PathVariable Integer id) {
		attributeService.deleteAttribute(id);
		return ResponseEntity.noContent().build();
	}

	// 查詢 某個二級分類的 所有 屬性（賣家上架商品時用）
	@GetMapping("/category2/{category2Id}")
	public ResponseEntity<List<AttributeDTO>> getAttributesByCategory2(@PathVariable Integer category2Id) {
		List<AttributeDTO> attributes = attributeService.getAttributesByCategory2(category2Id);
		return ResponseEntity.ok(attributes);
	}

}
