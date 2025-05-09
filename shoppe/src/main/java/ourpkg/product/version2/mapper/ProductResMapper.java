package ourpkg.product.version2.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.product.version2.dto.CategorySimpleDTO;
import ourpkg.product.version2.dto.ProductImageDTO;
import ourpkg.product.version2.dto.ProductResDTO;
import ourpkg.product.version2.dto.ProductResDTO.PriceRangeDTO;
import ourpkg.product.version2.dto.ShopSimpleDTO;
import ourpkg.sku.version2.dto.SkuResDTO;

/**
 * 商品響應映射類：將Product實體映射為ProductResDTO
 */
@Component
@RequiredArgsConstructor
public class ProductResMapper {

	/**
	 * 將實體轉換為DTO
	 * 
	 * @param entity 商品實體
	 * @return 商品響應DTO
	 */
	public ProductResDTO toDto(Product entity) {
		if (entity == null) {
			return null;
		}

		ProductResDTO dto = new ProductResDTO();

		// 基本屬性
		dto.setProductId(entity.getProductId());
		dto.setProductName(entity.getProductName());
		dto.setDescription(entity.getDescription());
		dto.setActive(entity.getActive());
		dto.setReviewStatus(entity.getReviewStatus());
		dto.setReviewComment(entity.getReviewComment());
		dto.setCreatedAt(entity.getCreatedAt());
		dto.setUpdatedAt(entity.getUpdatedAt());

		// 店鋪資訊
		if (entity.getShop() != null) {
			ShopSimpleDTO shopDto = new ShopSimpleDTO();
			shopDto.setShopId(entity.getShop().getShopId());
			shopDto.setShopName(entity.getShop().getShopName());
			dto.setShop(shopDto);
		}

		// 分類資訊
		if (entity.getCategory1() != null) {
			CategorySimpleDTO category1Dto = new CategorySimpleDTO();
			category1Dto.setId(entity.getCategory1().getId());
			category1Dto.setName(entity.getCategory1().getName());
			dto.setCategory1(category1Dto);
		}

		if (entity.getCategory2() != null) {
			CategorySimpleDTO category2Dto = new CategorySimpleDTO();
			category2Dto.setId(entity.getCategory2().getId());
			category2Dto.setName(entity.getCategory2().getName());
			dto.setCategory2(category2Dto);
		}

		// 圖片資訊
		if (entity.getProductImages() != null) {
			List<ProductImageDTO> imageDtos = entity.getProductImages().stream().map(this::convertToImageDto)
					.collect(Collectors.toList());

			dto.setProductImages(imageDtos);

			// 設置主圖URL
			entity.getProductImages().stream().filter(ProductImage::getIsPrimary).findFirst()
					.ifPresent(primaryImage -> dto.setPrimaryImageUrl(primaryImage.getImagePath()));
		}

		// SKU數量
		if (entity.getSkuList() != null) {
			dto.setSkuCount(entity.getSkuList().size());

			// 價格範圍
			if (!entity.getSkuList().isEmpty()) {
				double minPrice = entity.getSkuList().stream().mapToDouble(sku -> sku.getPrice().doubleValue()).min()
						.orElse(0.0);

				double maxPrice = entity.getSkuList().stream().mapToDouble(sku -> sku.getPrice().doubleValue()).max()
						.orElse(0.0);

				dto.setPriceRange(new PriceRangeDTO(minPrice, maxPrice));
			}
		} else {
			dto.setSkuCount(0);
		}
		
		if (entity.getSkuList() != null) {
		    List<SkuResDTO> skuDtos = entity.getSkuList().stream()
		        .filter(sku -> sku.getIsDeleted() == null || !sku.getIsDeleted()) // 過濾未刪除
		        .map(sku -> {
		            SkuResDTO skuDto = new SkuResDTO();
		            skuDto.setSkuId(sku.getSkuId());
		            skuDto.setProductId(entity.getProductId());
		            skuDto.setProductName(entity.getProductName());
		            skuDto.setPrice(sku.getPrice());
		            skuDto.setStock(sku.getStock());
		            skuDto.setSpecPairs(sku.getSpecPairsAsMap());
//		            skuDto.setSpecDescription(sku.getSpecDescription());
		            skuDto.setIsDeleted(sku.getIsDeleted());
		            return skuDto;
		        })
		        .collect(Collectors.toList());

		    dto.setSkuList(skuDtos);
		}


		return dto;
	}

	/**
	 * 將商品圖片實體轉換為DTO
	 */
	private ProductImageDTO convertToImageDto(ProductImage image) {
		ProductImageDTO imageDto = new ProductImageDTO();
		imageDto.setImageId(image.getImageId());
		imageDto.setProductId(image.getProduct().getProductId());
		imageDto.setImagePath(image.getImagePath());
		imageDto.setIsPrimary(image.getIsPrimary());
		imageDto.setDisplayOrder(image.getDisplayOrder());
		imageDto.setCreatedAt(image.getCreatedAt());
		imageDto.setUpdatedAt(image.getUpdatedAt());
		return imageDto;
	}

	/**
	 * 將實體分頁轉換為DTO分頁
	 * 
	 * @param entityPage 商品實體分頁
	 * @return 商品DTO分頁
	 */
	public Page<ProductResDTO> toDtoList(Page<Product> entityPage) {
		List<ProductResDTO> dtos = entityPage.getContent().stream().map(this::toDto).collect(Collectors.toList());

		return new PageImpl<>(dtos, entityPage.getPageable(), entityPage.getTotalElements());
	}
}