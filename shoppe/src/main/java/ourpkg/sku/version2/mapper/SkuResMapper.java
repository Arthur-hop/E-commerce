package ourpkg.sku.version2.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import ourpkg.sku.Sku;
import ourpkg.sku.version2.dto.SkuResDTO;

/**
 * SKU響應映射類：將Sku實體映射為SkuResDTO
 */
@Component
public class SkuResMapper {

	/**
	 * 將實體轉換為DTO
	 * 
	 * @param entity SKU實體
	 * @return SKU響應DTO
	 */
	public SkuResDTO toDto(Sku entity) {
		if (entity == null) {
			return null;
		}

		SkuResDTO dto = new SkuResDTO();

		dto.setSkuId(entity.getSkuId());
		dto.setProductId(entity.getProduct().getProductId());
		dto.setProductName(entity.getProduct().getProductName());
		dto.setStock(entity.getStock());
		dto.setPrice(entity.getPrice());
		dto.setIsDeleted(entity.getIsDeleted());

		// 設置規格鍵值對
		Map<String, String> specPairs = entity.getSpecPairsAsMap();
		dto.setSpecPairs(specPairs);

		// 生成規格描述
		if (specPairs != null && !specPairs.isEmpty()) {
			String specDescription = specPairs.values().stream().collect(Collectors.joining("/"));
			dto.setSpecDescription(specDescription);
		}

		return dto;
	}

	/**
	 * 將實體列表轉換為DTO列表
	 * 
	 * @param entities SKU實體列表
	 * @return SKU DTO列表
	 */
	public List<SkuResDTO> toDtoList(List<Sku> entities) {
		if (entities == null) {
			return null;
		}

		return entities.stream().map(this::toDto).collect(Collectors.toList());
	}

	/**
	 * 將實體分頁轉換為DTO分頁
	 * 
	 * @param entityPage SKU實體分頁
	 * @return SKU DTO分頁
	 */
	public Page<SkuResDTO> toDtoPage(Page<Sku> entityPage) {
		if (entityPage == null) {
			return null;
		}

		List<SkuResDTO> dtos = entityPage.getContent().stream().map(this::toDto).collect(Collectors.toList());

		return new PageImpl<>(dtos, entityPage.getPageable(), entityPage.getTotalElements());
	}
}
