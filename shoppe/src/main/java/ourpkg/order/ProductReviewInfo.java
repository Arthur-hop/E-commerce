package ourpkg.order;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ProductReviewInfo {
    private Integer productId;
    private String productName;
    private String description;
    private String orderItemId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private Date updatedAt;

    public ProductReviewInfo(Integer productId, String productName, String description, String orderItemId, Date updatedAt) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.orderItemId = orderItemId;
        this.updatedAt = updatedAt;
    }

	public Integer getProductId() {
		return productId;
	}

	public void setProductId(Integer productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOrderItemId() {
		return orderItemId;
	}

	public void setOrderItemId(String orderItemId) {
		this.orderItemId = orderItemId;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

    // Getters & Setters
}
