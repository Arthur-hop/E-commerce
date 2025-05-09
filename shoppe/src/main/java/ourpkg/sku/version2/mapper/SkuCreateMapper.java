package ourpkg.sku.version2.mapper;

import org.springframework.stereotype.Component;

import ourpkg.sku.Sku;
import ourpkg.sku.version2.dto.SkuCreateDTO;

/**
 * 將SkuCreateDTO映射為Sku實體
 */
@Component
public class SkuCreateMapper {

	/**
	 * 將DTO轉換為實體
	 * 
	 * @param dto SKU創建DTO
	 * @return SKU實體
	 */
	public Sku toEntity(SkuCreateDTO dto) {
		if (dto == null) {
			return null;
		}

		Sku entity = new Sku();

		entity.setStock(dto.getStock());
		entity.setPrice(dto.getPrice());

		// 設置規格鍵值對
		if (dto.getSpecPairs() != null) {
			entity.setSpecPairsFromMap(dto.getSpecPairs());
		}

		entity.setIsDeleted(false);

		return entity;
	}
}
