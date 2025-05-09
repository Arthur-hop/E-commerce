package ourpkg.order;

import java.util.List;

import jakarta.validation.constraints.Min;

public class OrderRequest {
    // 現有屬性
    @Min(value = 1, message = "金額必須大於0")
    private int amount;
    
    private String description;
    
    private String itemName;
    
    private String merchantTradeNo;
    
    private String returnUrl;
    private String clientBackUrl;
    
  
    private Integer productId;
    
    @Min(value = 1, message = "商品數量必須大於0")
    private Integer quantity;
    
    private Integer skuId;

    
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String paymentMethod;
    private Double price;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverZipCode;
    private String receiverStreet;
    private List<OrderItemRequest> items;
    private Integer userCouponId;

    public Integer getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Integer userCouponId) {
        this.userCouponId = userCouponId;
    }
    

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
    
    
    public String getReceiverCity() {
		return receiverCity;
	}

	public void setReceiverCity(String receiverCity) {
		this.receiverCity = receiverCity;
	}

	public String getReceiverDistrict() {
		return receiverDistrict;
	}

	public void setReceiverDistrict(String receiverDistrict) {
		this.receiverDistrict = receiverDistrict;
	}

	public String getReceiverZipCode() {
		return receiverZipCode;
	}

	public void setReceiverZipCode(String receiverZipCode) {
		this.receiverZipCode = receiverZipCode;
	}

	public String getReceiverStreet() {
		return receiverStreet;
	}

	public void setReceiverStreet(String receiverStreet) {
		this.receiverStreet = receiverStreet;
	}

	public Integer getSkuId() {
		return skuId;
	}

	public void setSkuId(Integer skuId) {
		this.skuId = skuId;
	}

	public String getReceiverName() {
		return receiverName;
	}

	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}

	public String getReceiverPhone() {
		return receiverPhone;
	}

	public void setReceiverPhone(String receiverPhone) {
		this.receiverPhone = receiverPhone;
	}

	public String getReceiverAddress() {
		return receiverAddress;
	}

	public void setReceiverAddress(String receiverAddress) {
		this.receiverAddress = receiverAddress;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getClientBackUrl() {
        return clientBackUrl;
    }
    
    public void setClientBackUrl(String clientBackUrl) {
        this.clientBackUrl = clientBackUrl;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getMerchantTradeNo() {
        return merchantTradeNo;
    }
    
    public void setMerchantTradeNo(String merchantTradeNo) {
        this.merchantTradeNo = merchantTradeNo;
    }
    
    public String getReturnUrl() {
        return returnUrl;
    }
    
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
    
    // 新增 getters 和 setters
    public Integer getProductId() {
        return productId;
    }
    
    public void setProductId(Integer productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Double getPrice() {
        return this.price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}