package ourpkg.coupon.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AdminCouponDTO {
	private Integer couponId;
    private String couponCode;
    private String couponName;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private String startDate; // Format as String
    private String endDate;   // Format as String
    private Integer usageLimit;
    private Integer usagePerUser;
    private Boolean redeemed; // Admin might want to see this
    private String createdAt; // Format as String
    private String updatedAt; // Format as String
    private Integer shopId;
    private String shopName; // Include shop name
}
