package ourpkg.product.version2.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.sku.version2.dto.SkuResDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResDTO {

	private Integer productId;

	private String productName;

	private String description;

	private Boolean active;

	private Boolean reviewStatus;

	private String reviewComment;

	// 商店資訊 (簡化版)
	private ShopSimpleDTO shop;

	// 分類資訊 (簡化版)
	private CategorySimpleDTO category1;
	private CategorySimpleDTO category2;

	// 商品圖片列表
	private List<ProductImageDTO> productImages = new ArrayList<>();

	// 主圖URL (方便前端使用)
	private String primaryImageUrl;

	// SKU數量
	private Integer skuCount;


	// 價格範圍
	private PriceRangeDTO priceRange;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8")
	private Date createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8")
	private Date updatedAt;
	
	private List<SkuResDTO> skuList = new ArrayList<>();
	
	private Double rating;         // 平均評價星數
	
	private Integer reviewCount;   // 評論數

	/**
	 * 內部類：價格範圍
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PriceRangeDTO {
		private Double minPrice;
		private Double maxPrice;
	}

}
