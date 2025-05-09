package ourpkg.product.version2.mapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ourpkg.product.Product;
import ourpkg.product.version2.dto.ProductUpdateDTO;

/**
 * 商品更新映射類：將ProductUpdateDTO應用到Product實體
 */
@Component
@RequiredArgsConstructor
public class ProductUpdateMapper {

	/**
	 * 將DTO的值應用到實體
	 * 
	 * @param entity 現有的商品實體
	 * @param dto    商品更新的DTO
	 * @return 更新後的商品實體
	 */
	public Product toEntity(Product entity, ProductUpdateDTO dto) {
		// 只更新非null的屬性
		if (dto.getProductName() != null) {
			entity.setProductName(dto.getProductName());
		}

		if (dto.getDescription() != null) {
			entity.setDescription(dto.getDescription());
		}

		if (dto.getActive() != null) {
			entity.setActive(dto.getActive());
		}

		return entity;
	}
}
