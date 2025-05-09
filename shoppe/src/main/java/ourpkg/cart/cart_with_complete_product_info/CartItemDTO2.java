package ourpkg.cart.cart_with_complete_product_info;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO2 {

	private Integer cartId;
	private Integer skuId;
	private String name;
	private Integer quantity;
	private BigDecimal price;

	// 新增的屬性
	private String imageUrl; // 商品主圖URL
	private Map<String, String> specInfo; // 規格信息，如 {"顏色": "紅色", "尺寸": "M"}
	private Integer productId; // 商品ID
	private String shopName; // 店鋪名稱
	private Integer shopId; // 店鋪ID
	private Integer stock; // 當前庫存，用於前端限制最大購買數量

}
