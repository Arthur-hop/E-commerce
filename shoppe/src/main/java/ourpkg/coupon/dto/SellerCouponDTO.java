package ourpkg.coupon.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SellerCouponDTO {
	private Integer couponId;
	private String couponCode;
	private String couponName;
	private String description;
	private String discountType;
	private BigDecimal discountValue;
	private String startDate;
	private String endDate;
	private Integer usageLimit;
	private Integer usagePerUser;
	private Boolean redeemed; // Seller might want to see this too
}
