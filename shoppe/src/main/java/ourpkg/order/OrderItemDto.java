package ourpkg.order;

import java.math.BigDecimal;

public class OrderItemDto {
	private String productName;
	private int quantity;
	private BigDecimal unitPrice;
	private String imageUrl; // 新增圖片欄位
	private Integer productId;

	public OrderItemDto() {
	}

	public OrderItemDto(String productName, int quantity, BigDecimal unitPrice) {
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;

	}

	public OrderItemDto(String productName, int quantity, BigDecimal unitPrice, String imageUrl, Integer productId) {
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.imageUrl = imageUrl;
		this.productId = productId;
	}

	public OrderItemDto(Integer productId, String productName, Integer quantity, BigDecimal unitPrice,
			String imageUrl) {
		this.productId = productId;
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.imageUrl = imageUrl;
	}

	public Integer getProductId() {
		return productId;
	}

	public void setProductId(Integer productId) {
		this.productId = productId;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}
}
