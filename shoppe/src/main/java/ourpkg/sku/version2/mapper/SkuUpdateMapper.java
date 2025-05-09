package ourpkg.sku.version2.mapper;

import org.springframework.stereotype.Component;

import ourpkg.sku.Sku;
import ourpkg.sku.version2.dto.SkuUpdateDTO;

/**
 * 將SkuUpdateDTO應用到Sku實體
 */
@Component
public class SkuUpdateMapper {

	/**
	 * 將DTO的值應用到實體
	 * 
	 * @param entity 現有的SKU實體
	 * @param dto    SKU更新DTO
	 * @return 更新後的SKU實體
	 */
	public Sku toEntity(Sku entity, SkuUpdateDTO dto) {
		if (entity == null || dto == null) {
			return entity;
		}

		// 只更新非null的屬性
		if (dto.getStock() != null) {
			entity.setStock(dto.getStock());
		}

		if (dto.getPrice() != null) {
			entity.setPrice(dto.getPrice());
		}

		// 更新規格鍵值對
		if (dto.getSpecPairs() != null) {
			entity.setSpecPairsFromMap(dto.getSpecPairs());
		}

		return entity;
	}
}
