package ourpkg.product.version2.complete_create;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResDTO2 {

	private Integer productId;
	private String productName;
	private String description;
	private Boolean active;
	private Integer category1Id;
	private String category1Name;
	private Integer category2Id;
	private String category2Name;
	private Integer shopId;
	private String shopName;
	private String primaryImageUrl;
	private List<String> imageUrls;
	private Boolean isDeleted;
	private PriceRangeDTO2 priceRange;
	private Integer skuCount;
	
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	private Date createdAt;
	
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	private Date updatedAt;

	// 價格範圍內部類
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PriceRangeDTO2 {
		private double min;
		private double max;

	}
}
