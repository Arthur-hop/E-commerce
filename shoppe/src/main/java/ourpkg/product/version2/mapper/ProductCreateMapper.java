package ourpkg.product.version2.mapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ourpkg.product.Product;
import ourpkg.product.version2.dto.ProductCreateDTO;

/**
 * 商品創建映射類：將ProductCreateDTO映射為Product實體
 */
@Component
@RequiredArgsConstructor
public class ProductCreateMapper {

	/**
	 * 將DTO轉換為實體
	 * 
	 * @param dto 商品創建的DTO
	 * @return 商品實體
	 */
	public Product toEntity(ProductCreateDTO dto) {
		Product entity = new Product();

		// 設置基本屬性
		entity.setProductName(dto.getProductName());
		entity.setDescription(dto.getDescription());
		entity.setActive(dto.getActive());
		entity.setReviewStatus(true); // 默認審核通過

		return entity;
	}
}
